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

    // ===== 持续道具：库存（按键消耗）+ 单激活位（本局持续生效，FR-18 已激活再用转 +50 金币） =====
    /** 幸运草库存（按键 F/Num3 使用） */
    private int luckyCloverCount;
    /** 钻石升级库存（按键 G/Num4 使用） */
    private int diamondBoostCount;
    /** 石头书库存（按键 H/Num5 使用） */
    private int stoneBookCount;
    /** 幸运草是否已激活：本局物品基础收益 ×1.5 */
    private boolean luckyClover;
    /** 钻石升级是否已激活：后续抓取钻石价值 ×2 */
    private boolean diamondBoost;
    /** 石头书是否已激活：抓取石头价值 ×3 */
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

    // ===== 持续道具（FR-18：福袋入库存，按键消耗；已激活再用折 50 金币） =====

    @Override public boolean hasLuckyClover() { return luckyClover; }
    @Override public boolean hasDiamondBoost() { return diamondBoost; }
    @Override public boolean hasStoneBook() { return stoneBook; }

    @Override public int getLuckyCloverCount() { return luckyCloverCount; }
    @Override public int getDiamondBoostCount() { return diamondBoostCount; }
    @Override public int getStoneBookCount() { return stoneBookCount; }

    @Override
    public int addLuckyClover(int count) {
        return addToStock(count, v -> luckyCloverCount = v, () -> luckyCloverCount);
    }

    @Override
    public int addDiamondBoost(int count) {
        return addToStock(count, v -> diamondBoostCount = v, () -> diamondBoostCount);
    }

    @Override
    public int addStoneBook(int count) {
        return addToStock(count, v -> stoneBookCount = v, () -> stoneBookCount);
    }

    @Override
    public PersistItemUseResult useLuckyClover() {
        return consumePersist(() -> luckyCloverCount, v -> luckyCloverCount = v,
                () -> luckyClover, v -> luckyClover = v);
    }

    @Override
    public PersistItemUseResult useDiamondBoost() {
        return consumePersist(() -> diamondBoostCount, v -> diamondBoostCount = v,
                () -> diamondBoost, v -> diamondBoost = v);
    }

    @Override
    public PersistItemUseResult useStoneBook() {
        return consumePersist(() -> stoneBookCount, v -> stoneBookCount = v,
                () -> stoneBook, v -> stoneBook = v);
    }

    /**
     * 持续道具入库公共流程：上限 PLAYER_MAX_PERSIST_ITEM_COUNT，
     * @return 实际加入数量（0 表示已满，调用方折 50 金币）
     */
    private int addToStock(int count,
                           java.util.function.IntConsumer setter,
                           java.util.function.IntSupplier getter) {
        if (count <= 0) {
            return 0;
        }
        int before = getter.getAsInt();
        setter.accept(Math.min(before + count, GameConfig.PLAYER_MAX_PERSIST_ITEM_COUNT));
        return getter.getAsInt() - before;
    }

    /**
     * 持续道具按键使用公共流程（FR-18）：
     * 库存 0 → NO_STOCK（不扣库存）；库存 -1 后，未激活 → ACTIVATED；已激活 → +50 金币（DUPLICATE_GOLD）。
     */
    private PersistItemUseResult consumePersist(java.util.function.IntSupplier countGetter,
                                                java.util.function.IntConsumer countSetter,
                                                java.util.function.BooleanSupplier activeGetter,
                                                java.util.function.Consumer<Boolean> activeSetter) {
        if (countGetter.getAsInt() <= 0) {
            return PersistItemUseResult.NO_STOCK;
        }
        countSetter.accept(countGetter.getAsInt() - 1);
        if (!activeGetter.getAsBoolean()) {
            activeSetter.accept(true);
            return PersistItemUseResult.ACTIVATED;
        }
        score += GameConfig.ITEM_DUP_AUTO_GOLD; // 已激活：折算 50 金币
        return PersistItemUseResult.DUPLICATE_GOLD;
    }
}
