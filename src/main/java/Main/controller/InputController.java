// FR-UI InputController：双人输入控制器接口（P1/P2 释放钩爪 + ESC 暂停），来自 feature_ui 分支
package Main.controller;

/**
 * 双人输入控制器接口（FR-18 双人按键独立监听 + FR-19 暂停/恢复）。
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
     * 玩家1引爆炸药（按键 W）：炸毁当前钩上携带的物品，钩爪立即空钩收回，炸药库存减 1。
     * 仅在对局进行中、钩爪处于 GRABBING 携带状态、且炸药库存 > 0 时生效。
     */
    void player1UseDynamite();

    /**
     * 玩家2引爆炸药（按键 ↑）：逻辑与玩家1完全对称独立。
     */
    void player2UseDynamite();

    /**
     * 玩家1使用强力药水（按键 A，FR-16/FR-18）：
     * 消耗库存 1 瓶，自身钩爪收回速度 ×2 持续 10 秒；生效中再用仅刷新时长。
     */
    void player1UsePowerPotion();

    /**
     * 玩家2使用强力药水（按键 Num1）：逻辑与玩家1完全对称独立。
     */
    void player2UsePowerPotion();

    /**
     * 玩家1使用冰冻箱（按键 D，FR-15/FR-16）：
     * 消耗库存 1 个，仅对玩家2钩爪生效，冻结 3 秒（运动完全暂停）。
     */
    void player1UseFreezeBox();

    /**
     * 玩家2使用冰冻箱（按键 Num2）：仅对玩家1钩爪生效，逻辑对称独立。
     */
    void player2UseFreezeBox();

    /**
     * 暂停/恢复对局（按键 ESC，双方共用，FR-19）。
     * PLAYING 与 PAUSED 之间互相切换；READY/FINISHED 状态下忽略。
     * 倒计时与钩爪收回的冻结由各自持有方监听 GameState 实现。
     */
    void togglePause();
}
