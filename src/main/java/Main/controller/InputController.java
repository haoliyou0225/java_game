// FR-18 双人输入控制器接口：justPressed 防抖 + 语义动作纯派发（不含任何物理/库存修改）
package Main.controller;

import Main.model.GameState;

/**
 * 双人输入控制器接口（FR-18 双人按键独立 + FR-19 暂停/恢复）。
 * <p>
 * 分层职责：View 层把物理键位映射为 (playerId, {@link ActionType}) 后调用本接口；
 * 本层只做 justPressed 防抖（长按/系统自动重复不连发）并把动作派发给
 * {@link GameActionHandler}（逻辑层）。本接口的任何方法都不直接修改钩爪物理、
 * 库存或道具效果，也不依赖 JavaFX 类型。
 */
public interface InputController {

    /**
     * 语义动作“按下”：同一 (玩家,动作) 在松开前只生效一次，随后派发给逻辑层。
     *
     * @param playerId 1=P1，2=P2
     * @param action   语义动作（THROW_HOOK / USE_DYNAMITE / 各道具，不含 TOGGLE_PAUSE）
     */
    void pressAction(int playerId, ActionType action);

    /**
     * 语义动作“松开”：清除对应防抖标志，使下次按下可以再次触发。
     * ESC 松开时传 (0, {@link ActionType#TOGGLE_PAUSE}) 复位暂停防抖标志。
     */
    void releaseAction(int playerId, ActionType action);

    /**
     * ESC“按下”（双方共用，justPressed 防抖：双人同帧按也只切换一次）。
     *
     * @return 切换后的对局状态（PAUSED/PLAYING），供 View 层显隐暂停遮罩；FINISHED 等状态原样返回
     */
    GameState pressPause();

    /**
     * 清空全部按键防抖标志（窗口失焦时调用，防止“卡键”）。
     */
    void resetPressedState();
}
