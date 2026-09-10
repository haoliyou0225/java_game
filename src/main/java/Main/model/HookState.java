// FR-07 钩子状态机：SWINGING 钟摆 / THROWING 抛出 / RETRACTING 空钩收回 / GRABBING 携带收回 / STUNNED 眩晕停滞 / FROZEN 冰冻停滞
package Main.model;

public enum HookState {
    /** 钟摆摆动中（唯一可抛出状态）；对应 FR-07 文档态 SWING */
    SWINGING,
    /** 抛出直线飞行中；对应 FR-07 文档态 SHOOT */
    THROWING,
    /** 空钩收回中（800px/s 固定速度）；对应 FR-07 文档态 RETRACT */
    RETRACTING,
    /** 携带物品收回中（按物品收回耗时计算速度）；FR-15 中"RETRACT 携带物品状态"即本态 */
    GRABBING,
    /** 眩晕停滞中（抢夺/碰撞后在碰撞点冻结，期间无法操作，倒计时结束自动空钩收回） */
    STUNNED,
    /** 冰冻停滞中（FR-07 第五态/FR-16：冰冻箱施加，运动完全暂停，3 秒后恢复冻结前状态，期间无法操作） */
    FROZEN
}
