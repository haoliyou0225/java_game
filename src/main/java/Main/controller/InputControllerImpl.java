// FR-UI InputControllerImpl：双人输入实现（状态校验→置 THROWING→模拟收回，暂停切换，炸药使用），来自 feature_ui 分支
package Main.controller;

import Main.model.GameModel;
import Main.model.GameState;
import Main.model.Hook;
import Main.model.HookState;
import Main.model.Item;
import Main.util.LogUtils;

/**
 * FR-18 双人按键独立监听 —— 控制器实现。
 * <p>
 * 职责：接收 View 层转发的按键事件，校验对局状态后驱动对应玩家的钩爪或道具。
 * 两名玩家的处理逻辑完全独立（各自走独立分支），互不阻塞、互不干扰。
 * <p>
 * 分层约束：本类位于 controller 包，不导入任何 javafx.* 类；
 * 键盘事件由 View/Main 层监听后调用本类方法。
 */
public class InputControllerImpl implements InputController {

    /** 对局模型（通过接口依赖，不依赖具体实现类） */
    private final GameModel model;

    /**
     * 构造控制器。
     *
     * @param model 对局模型，用于读取游戏状态与双方钩爪
     */
    public InputControllerImpl(GameModel model) {
        this.model = model;
    }

    /**
     * 玩家1释放钩爪（按键 S）。
     * <p>
     * 与玩家2逻辑完全对称且独立，通过公共方法 releaseHook 复用同一流程；
     * 触发后钩爪置为 THROWING，并安排 2 秒后自动收回（P0 模拟）。
     */
    @Override
    public void player1ReleaseHook() {
        releaseHook(model.getHook1(), "玩家1");
    }

    /**
     * 玩家2释放钩爪（按键 ↓）。
     * <p>
     * 与玩家1逻辑完全对称且独立：处理玩家2时不影响玩家1正在执行的任何动作。
     */
    @Override
    public void player2ReleaseHook() {
        releaseHook(model.getHook2(), "玩家2");
    }

    /**
     * 玩家1使用炸药（按键 A）。
     * 消耗库存 → 若钩爪携带物品则炸掉 → 钩爪变空钩收回。
     */
    @Override
    public void player1UseBomb() {
        useBomb(1, model.getHook1(), model.getPlayer1(), "玩家1");
    }

    /**
     * 玩家2使用炸药（按键 L）。
     * 与玩家1逻辑完全对称且独立。
     */
    @Override
    public void player2UseBomb() {
        useBomb(2, model.getHook2(), model.getPlayer2(), "玩家2");
    }

    /**
     * 使用炸药的公共流程：状态校验 → 库存扣减 → 炸掉携带物品 → 钩爪空钩收回。
     *
     * @param playerId    玩家编号（1 或 2）
     * @param hook        目标钩爪
     * @param player      玩家实例（用于扣减炸药库存）
     * @param playerLabel 玩家标签（日志用）
     */
    private void useBomb(int playerId, Hook hook, Main.model.Player player, String playerLabel) {
        // 非对局中忽略
        if (model.getState() != GameState.PLAYING) {
            return;
        }
        if (hook == null || player == null) {
            return;
        }
        // 库存不足
        if (!player.useBomb()) {
            System.out.println(LogUtils.format(playerLabel + " 炸药不足"));
            return;
        }
        // 触发爆炸效果：若钩爪正携带物品则清除
        model.triggerExplosion(playerId);
        System.out.println(LogUtils.format(playerLabel + " 使用炸药！剩余 " + player.getBombCount() + " 个"));
    }

    /**
     * 释放指定钩爪的公共流程（FR-11/FR-18）：状态校验 → 调用 hook.throwHook() 触发真实物理抛出。
     * 真实抛出/收回/抓取状态机由 GameModelImpl.gameLoopTick 每帧推进。
     *
     * @param hook        目标钩爪（可为 null，null 时防御性忽略）
     * @param playerLabel 玩家标签（如 "玩家1"），用于日志输出
     */
    private void releaseHook(Hook hook, String playerLabel) {
        // 非对局中（暂停/结束/准备）忽略输入，防止误触发
        if (model.getState() != GameState.PLAYING) {
            return;
        }
        if (hook == null) {
            return;
        }
        // hook.throwHook() 内部已做状态校验（非 SWINGING 忽略），无需重复判定
        HookState before = hook.getState();
        hook.throwHook();
        if (hook.getState() == HookState.THROWING && before == HookState.SWINGING) {
            System.out.println(LogUtils.format(playerLabel + " 释放钩爪，角度: " + formatDegrees(hook.getAngle())));
        }
    }

    /**
     * 暂停/恢复对局（按键 ESC，双方共用）。
     * PLAYING 与 PAUSED 之间互相切换；其余状态（READY/FINISHED）忽略。
     * 倒计时/钩爪动画的暂停联动由 GameTimer/GameModel 的状态监听实现。
     */
    @Override
    public void togglePause() {
        if (model.getState() == GameState.PLAYING) {
            model.setState(GameState.PAUSED);
            System.out.println(LogUtils.format("游戏暂停"));
        } else if (model.getState() == GameState.PAUSED) {
            model.setState(GameState.PLAYING);
            System.out.println(LogUtils.format("游戏继续"));
        }
        // READY / FINISHED 状态下忽略暂停操作
    }

    /**
     * 将钩爪角度（弧度）格式化为日志用的角度字符串，如 "45.0°"。
     *
     * @param angleRad 钩爪角度（弧度，Hook 接口约定）
     * @return 度数文本，保留 1 位小数并带 ° 符号
     */
    private String formatDegrees(double angleRad) {
        return String.format("%.1f°", Math.toDegrees(angleRad));
    }
}
