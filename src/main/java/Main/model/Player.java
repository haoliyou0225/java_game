// FR-UI Player：玩家接口（分数 + 炸药 + 道具效果），来自 feature_ui 分支
package Main.model;

public interface Player {

    /** 当前分数（FR-29：HUD 实时显示） */
    int getScore();

    /** 增加分数（抓到物品结算后调用） */
    void addScore(int points);

    /** 当前炸药库存数量 */
    int getBombCount();

    /**
     * 使用 1 个炸药。
     * @return true 成功使用（库存 > 0）；false 库存不足
     */
    boolean useBomb();

    /**
     * 增加炸药库存（上限由 GameConfig.PLAYER_MAX_DYNAMITE_COUNT 控制）。
     * 若库存已满则炸药自动转为金币（GameConfig.BOMB_FULL_AUTO_GOLD）。
     * @param count 增加数量（正数）
     * @return 实际增加的炸药数（因上限未满）；溢出部分转为金币不计入炸药数
     */
    int addBomb(int count);

    // ===== 道具效果标志（福袋抽取后触发，本局持续生效） =====

    /** 幸运草：本局所有抓取物品收益 +50% */
    boolean hasLuckyClover();
    void grantLuckyClover();

    /** 钻石升级药水：本局后续抓取钻石价值翻倍 */
    boolean hasDiamondBoost();
    void grantDiamondBoost();

    /** 石头收藏书：本局抓取石头消除惩罚，仅保留 +1 金币基础价值（与原版规则一致，预留） */
    boolean hasStoneBook();
    void grantStoneBook();
}
