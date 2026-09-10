// FR-03 钩子状态机：SWINGING 钟摆 / THROWING 抛出 / RETRACTING 空钩收回 / GRABBING 携带收回 / STUNNED 眩晕停滞
package Main.model;

public enum HookState {
    /** 钟摆摆动中（唯一可抛出状态） */
    SWINGING,
    /** 抛出直线飞行中 */
    THROWING,
    /** 空钩收回中（800px/s 固定速度） */
    RETRACTING,
    /** 携带物品收回中（按重量档位计算速度） */
    GRABBING,
    /** 眩晕停滞中（抢夺/碰撞后在碰撞点冻结，期间无法操作，倒计时结束自动空钩收回） */
    STUNNED
}
