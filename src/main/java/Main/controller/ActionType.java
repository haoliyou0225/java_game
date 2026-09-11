// 双人对局语义动作枚举（与物理键位解耦；KeyCode→ActionType 的映射只存在于 View 装配层）
package Main.controller;

/**
 * 对局语义动作类型（玩家无关）。
 * <p>
 * 分层约定：View 层（Main）负责把物理键位（S/W/A/D/F/G/H、方向键、小键盘等）
 * 映射为 (playerId, ActionType)；Controller 层（InputController）只做 justPressed 防抖与派发；
 * 真正的状态校验、物理推进、库存扣减、道具效果全部在逻辑层（GameActionHandler 实现）处理。
 */
public enum ActionType {
    /** 释放钩爪（仅 SWINGING 可触发，500px/s 直线抛出） */
    THROW_HOOK,
    /** 使用炸药（仅 GRABBING 携带收回可触发，炸毁携带物并空钩收回） */
    USE_DYNAMITE,
    /** 强力药水（自身钩爪收回速度 ×2，持续 10 秒） */
    USE_POWER_POTION,
    /** 冰冻箱（冻结对方钩爪 3 秒） */
    USE_FREEZE_BOX,
    /** 幸运草（本局自身物品收益 ×1.5，已激活再用折 50 金币） */
    USE_LUCKY_CLOVER,
    /** 钻石升级（本局自身钻石价值 ×2，已激活再用折 50 金币） */
    USE_DIAMOND_BOOST,
    /** 石头书（本局自身石头价值 ×3，已激活再用折 50 金币） */
    USE_STONE_BOOK,
    /** 暂停/恢复（双方共用，PLAYING↔PAUSED） */
    TOGGLE_PAUSE
}
