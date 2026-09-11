// FR-UI Player：玩家接口（分数 + 炸药/短时道具库存 + 福袋持续道具效果），对齐 FR-14/FR-15/FR-18
package Main.model;

public interface Player {

    /** 当前分数（FR-29：HUD 实时显示） */
    int getScore();

    /** 增加分数（抓到物品结算后调用） */
    void addScore(int points);

    /** 当前炸药库存（炸药键炸毁钩上携带物，库存减 1） */
    int getDynamiteCount();

    /**
     * 使用 1 个炸药（库存 > 0 时扣减并返回 true；库存为 0 返回 false）
     */
    boolean useDynamite();

    /**
     * 增加炸药库存（福袋开出炸药时调用）。
     * 库存上限 GameConfig.PLAYER_MAX_DYNAMITE_COUNT（3），
     * 因上限未能加入的部分由调用方转为金币（FR-15：+50）。
     *
     * @param count 增加数量（正数）
     * @return 实际增加的炸药数（0 表示库存已满）
     */
    int addDynamite(int count);

    // ===== 短时道具库存（FR-15：单种上限 5，福袋抽奖入库存，按键使用） =====

    /** 强力药水库存数量（FR-16：自身钩爪收回×2，持续 10 秒） */
    int getPowerPotionCount();

    /** 冰冻箱库存数量（FR-16：冻结对方钩爪 3 秒） */
    int getFreezeBoxCount();

    /**
     * 强力药水入库（福袋抽到短时道具时调用）。
     * 上限 GameConfig.PLAYER_MAX_SHORT_ITEM_COUNT（5），
     * @return 实际加入数量（0 表示已满，调用方转 +50 金币）
     */
    int addPowerPotion(int count);

    /** 冰冻箱入库，规则同 {@link #addPowerPotion(int)} */
    int addFreezeBox(int count);

    /** 取出 1 瓶强力药水使用（库存 > 0 扣减并返回 true） */
    boolean consumePowerPotion();

    /** 取出 1 个冰冻箱使用（库存 > 0 扣减并返回 true） */
    boolean consumeFreezeBox();

    // ===== 持续道具效果（FR-15：单激活位，抽到即激活；FR-18 已激活再抽转 +50 金币） =====

    /** 幸运草：本局物品基础收益 ×1.5；激活返回 true，已激活返回 false（调用方转 50 金币） */
    boolean hasLuckyClover();
    boolean grantLuckyClover();

    /** 钻石药水：本局抓取钻石价值 ×2；激活返回 true，已激活返回 false */
    boolean hasDiamondBoost();
    boolean grantDiamondBoost();

    /** 石头书：本局抓取石头价值 ×3；激活返回 true，已激活返回 false */
    boolean hasStoneBook();
    boolean grantStoneBook();
}
