// FR-UI InputController：双人输入控制器接口（P1/P2 释放钩爪 + P1/P2 使用炸药 + ESC 暂停），来自 feature_ui 分支
package Main.controller;

/**
 * 双人输入控制器接口（FR-18 双人按键独立监听 + FR-19 暂停/恢复 + 炸药道具）。
 * 键盘事件由 View/Main 层监听后转发到本层，本层不依赖任何 JavaFX 类型。
 */
public interface InputController {

    /**
     * 玩家1释放钩爪（按键 S，FR-18）。
     * 仅在对局进行中（PLAYING）且玩家1钩爪处于 SWINGING 状态时生效；
     * 触发后钩爪置为 THROWING（抛出飞行），并安排 2 秒后自动收回（P0 模拟）。
     */
    void player1ReleaseHook();

    /**
     * 玩家2释放钩爪（按键 ↓，FR-18）。
     * 与玩家1逻辑完全独立、互不阻塞；
     * 仅在对局进行中且玩家2钩爪处于 SWINGING 状态时生效。
     */
    void player2ReleaseHook();

    /**
     * 玩家1使用炸药（按键 A）。
     * 消耗 1 个炸药库存；若钩爪正携带物品则炸掉物品使钩爪空钩快速收回。
     * 仅在对局进行中（PLAYING）且库存 > 0 时生效。
     */
    void player1UseBomb();

    /**
     * 玩家2使用炸药（按键 L）。
     * 与玩家1逻辑完全对称且独立。
     */
    void player2UseBomb();

    /**
     * 暂停/恢复对局（按键 ESC，双方共用，FR-19）。
     * PLAYING 与 PAUSED 之间互相切换；READY/FINISHED 状态下忽略。
     * 倒计时与钩爪收回的冻结由各自持有方监听 GameState 实现。
     */
    void togglePause();
}
