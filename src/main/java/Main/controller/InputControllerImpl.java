// FR-18 InputController 实现：justPressed 防抖集合 + 动作派发给 GameActionHandler（薄层，无 JavaFX 依赖）
package Main.controller;

import Main.model.GameModel;
import Main.model.GameState;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 双人输入控制器实现（FR-18/FR-19）。
 * <p>
 * 仅承担两件事：
 * <ol>
 *   <li>justPressed 防抖：用并发集合记录“已按下未松开”的 (玩家,动作)，
 *       长按期间操作系统产生的重复 KEY_PRESSED 事件被直接忽略，单次按下只派发一次；</li>
 *   <li>纯派发：防抖通过后调用 {@link GameActionHandler#handleAction}，
 *       状态校验/物理推进/库存扣减/道具效果全部在逻辑层（GameManagerImpl）完成。</li>
 * </ol>
 * 本类不 import 任何 JavaFX 类型：View 层完成 KeyCode→语义动作映射后再调用本类，
 * 因此输入控制层可随 Model 一起在 headless main() 中直接驱动。
 */
public class InputControllerImpl implements InputController {

    /** 逻辑层只读模型引用（仅用于 pressPause 后返回最新对局状态） */
    private final GameModel model;

    /** 语义动作处理器（逻辑层，真正执行物理/库存/效果变更） */
    private final GameActionHandler actionHandler;

    /** 当前“已按下未松开”的动作键集合（justPressed 防抖，支持 FX 线程与游戏线程并发） */
    private final Set<Long> pressedActions = ConcurrentHashMap.newKeySet();

    /** ESC 防抖标志（双方共用，同帧双人按只触发一次暂停切换） */
    private volatile boolean pausePressed = false;

    public InputControllerImpl(GameModel model, GameActionHandler actionHandler) {
        this.model = model;
        this.actionHandler = actionHandler;
    }

    @Override
    public void pressAction(int playerId, ActionType action) {
        if (action == null || action == ActionType.TOGGLE_PAUSE) {
            return; // 暂停只走 pressPause 通道
        }
        long key = actionKey(playerId, action);
        if (!pressedActions.add(key)) {
            return; // 已按下未松开：长按/自动重复事件忽略，防止连发
        }
        actionHandler.handleAction(playerId, action);
    }

    @Override
    public void releaseAction(int playerId, ActionType action) {
        if (action == null) {
            return;
        }
        if (action == ActionType.TOGGLE_PAUSE) {
            pausePressed = false; // ESC 松开，允许下次按下再次切换
            return;
        }
        pressedActions.remove(actionKey(playerId, action));
    }

    @Override
    public GameState pressPause() {
        if (pausePressed) {
            return model.getState(); // 防抖：ESC 未松开前不重复切换
        }
        pausePressed = true;
        actionHandler.handleAction(0, ActionType.TOGGLE_PAUSE);
        return model.getState();
    }

    @Override
    public void resetPressedState() {
        pressedActions.clear();
        pausePressed = false;
    }

    /**
     * 生成 (玩家,动作) 的唯一长整型键：高 32 位玩家编号，低 32 位动作序号。
     */
    private long actionKey(int playerId, ActionType action) {
        return ((long) playerId << 32) | action.ordinal();
    }
}
