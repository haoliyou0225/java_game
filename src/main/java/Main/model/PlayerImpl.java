// FR-UI PlayerImpl：玩家分数 + 炸药/短时道具库存 + 福袋持续道具效果，对齐 FR-14/FR-15/FR-18
package Main.model;

import Main.config.GameConfig;

public class PlayerImpl implements Player {

    /** 当前分数（FR-29） */
    private int score;
    /** 炸药库存（初始 1，上限 3，由 GameConfig 控制） */
    private int dynamiteCount = GameConfig.PLAYER_INIT_DYNAMITE_COUNT;
    /** 强力药水库存（短时道具，上限 5） */
    private int powerPotionCount;
    /** 冰冻箱库存（短时道具，上限 5） */
    private int freezeBoxCount;

    // ===== 福袋持续道具效果（单激活位，本局持续生效；FR-18 重复获得转 +50 金币） =====
    /** 幸运草：本局物品基础收益 ×1.5 */
    private boolean luckyClover;
    /** 钻石药水：后续抓取钻石价值 ×2 */
    private boolean diamondBoost;
    /** 石头书：抓取石头价值 ×3 */
    private boolean stoneBook;

    @Override
    public int getScore() {
        return score;
    }

    @Override
    public void addScore(int points) {
        this.score += points;
    }

    @Override
    public int getDynamiteCount() {
        return dynamiteCount;
    }

    @Override
    public boolean useDynamite() {
        if (dynamiteCount <= 0) {
            return false;
        }
        dynamiteCount--;
        return true;
    }

    @Override
    public int addDynamite(int count) {
        if (count <= 0) {
            return 0;
        }
        int before = dynamiteCount;
        dynamiteCount = Math.min(dynamiteCount + count, GameConfig.PLAYER_MAX_DYNAMITE_COUNT);
        return dynamiteCount - before; // 实际增加数量；溢出部分由调用方转金币
    }

    // ===== 短时道具库存（FR-15：单种上限 5） =====
    @Override public int getPowerPotionCount() { return powerPotionCount; }
    @Override public int getFreezeBoxCount() { return freezeBoxCount; }

    @Override
    public int addPowerPotion(int count) {
        if (count <= 0) {
            return 0;
        }
        int before = powerPotionCount;
        powerPotionCount = Math.min(powerPotionCount + count,
                GameConfig.PLAYER_MAX_SHORT_ITEM_COUNT);
        return powerPotionCount - before;
    }

    @Override
    public int addFreezeBox(int count) {
        if (count <= 0) {
            return 0;
        }
        int before = freezeBoxCount;
        freezeBoxCount = Math.min(freezeBoxCount + count,
                GameConfig.PLAYER_MAX_SHORT_ITEM_COUNT);
        return freezeBoxCount - before;
    }

    @Override
    public boolean consumePowerPotion() {
        if (powerPotionCount <= 0) {
            return false;
        }
        powerPotionCount--;
        return true;
    }

    @Override
    public boolean consumeFreezeBox() {
        if (freezeBoxCount <= 0) {
            return false;
        }
        freezeBoxCount--;
        return true;
    }

    // ===== 持续道具（grant 返回 false 表示已激活，调用方转 +50 金币，FR-18） =====
    @Override public boolean hasLuckyClover() { return luckyClover; }
    @Override
    public boolean grantLuckyClover() {
        if (luckyClover) return false;
        luckyClover = true;
        return true;
    }

    @Override public boolean hasDiamondBoost() { return diamondBoost; }
    @Override
    public boolean grantDiamondBoost() {
        if (diamondBoost) return false;
        diamondBoost = true;
        return true;
    }

    @Override public boolean hasStoneBook() { return stoneBook; }
    @Override
    public boolean grantStoneBook() {
        if (stoneBook) return false;
        stoneBook = true;
        return true;
    }
}
