// FR-14/FR-15/FR-17/FR-18 福袋与结算验收自检：
// 加权抽奖 35%金币/35%炸药/20%短时道具/10%持续道具；
// 结算链 基础价值→石头×3→钻石×2→幸运草×1.5→四舍五入；
// 库存上限（炸药3/短时道具5/持续单激活位）溢出转 50 金币，持续道具重复获得转 50 金币。
// 与 GameManagerImpl 同包以访问包级静态方法；直接 java Main.controller.BagRewardAndSettlementCheck 运行。
package Main.controller;

import Main.config.GameConfig;
import Main.model.Diamond;
import Main.model.Gold;
import Main.model.Player;
import Main.model.PlayerImpl;
import Main.model.Stone;
import Main.util.SelfCheck;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

public class BagRewardAndSettlementCheck extends SelfCheck {

    public static void main(String[] args) {
        BagRewardAndSettlementCheck check = new BagRewardAndSettlementCheck();
        check.rollBagExtraRewardMatches35_35_20_10Weights();
        check.stoneBookTriplesStone();
        check.diamondBoostDoublesDiamond();
        check.luckyCloverMultipliesByOnePointFiveAndRounds();
        check.settlementChainAppliesStoneBookBeforeClover();
        check.noBonusReturnsBaseValue();
        check.goldRewardIsIn100To800Range();
        check.dynamiteOverflowConvertsTo50Gold();
        check.dynamiteNormalGainEntersInventory();
        check.shortItemCapFiveThenConvertsTo50Gold();
        check.shortItemNormalGainEntersInventory();
        check.persistentItemFirstGainActivatesSecondGainConvertsTo50Gold();
        check.playerStartsWithOneDynamite();
        check.finish();
    }

    // ===== FR-14 加权抽奖权重 =====

    private void rollBagExtraRewardMatches35_35_20_10Weights() {
        Random rnd = new Random(20260911L);
        int trials = 100000;
        Map<GameConfig.MysteryReward, Integer> counts =
                new EnumMap<>(GameConfig.MysteryReward.class);
        for (int i = 0; i < trials; i++) {
            GameConfig.MysteryReward r = GameManagerImpl.rollBagExtraReward(rnd);
            counts.merge(r, 1, Integer::sum);
        }

        double goldPct = pct(counts, GameConfig.MysteryReward.MYSTERY_GOLD, trials);
        double dynamitePct = pct(counts, GameConfig.MysteryReward.DYNAMITE, trials);
        double shortPct = pct(counts, GameConfig.MysteryReward.POWER_POTION, trials)
                + pct(counts, GameConfig.MysteryReward.FREEZE_BOX, trials);
        double persistPct = pct(counts, GameConfig.MysteryReward.LUCKY_CLOVER, trials)
                + pct(counts, GameConfig.MysteryReward.DIAMOND_BOOST, trials)
                + pct(counts, GameConfig.MysteryReward.STONE_BOOK, trials);

        checkTrue(Math.abs(goldPct - 35) < 3, "金币档应约 35%，实际 " + goldPct);
        checkTrue(Math.abs(dynamitePct - 35) < 3, "炸药档应约 35%，实际 " + dynamitePct);
        checkTrue(Math.abs(shortPct - 20) < 3, "短时道具档应约 20%，实际 " + shortPct);
        checkTrue(Math.abs(persistPct - 10) < 3, "持续道具档应约 10%，实际 " + persistPct);
    }

    private double pct(Map<GameConfig.MysteryReward, Integer> counts,
                       GameConfig.MysteryReward reward, int trials) {
        return counts.getOrDefault(reward, 0) * 100.0 / trials;
    }

    // ===== FR-17 结算链顺序与倍率 =====

    private void stoneBookTriplesStone() {
        Player player = new PlayerImpl();
        player.addStoneBook(1);
        checkTrue(player.useStoneBook() == Main.model.PersistItemUseResult.ACTIVATED, "首次使用石头书应激活成功");
        Stone stone = new Stone(0, 0);
        checkEq(GameConfig.STONE_BASE_GOLD, stone.getScore(), "石头基础价值应为 11");
        checkEq(GameConfig.STONE_BASE_GOLD * GameConfig.STONE_BOOK_MULTIPLIER,
                GameManagerImpl.settleNormalItem(stone, player),
                "石头书激活后石头价值应 ×3");
    }

    private void diamondBoostDoublesDiamond() {
        Player player = new PlayerImpl();
        player.addDiamondBoost(1);
        checkTrue(player.useDiamondBoost() == Main.model.PersistItemUseResult.ACTIVATED, "首次使用钻石药水应激活成功");
        Diamond diamond = new Diamond(0, 0);
        checkEq(600 * GameConfig.DIAMOND_BOOST_MULTIPLIER,
                GameManagerImpl.settleNormalItem(diamond, player),
                "钻石药水激活后钻石价值应 ×2");
    }

    private void luckyCloverMultipliesByOnePointFiveAndRounds() {
        Player player = new PlayerImpl();
        player.addLuckyClover(1);
        checkTrue(player.useLuckyClover() == Main.model.PersistItemUseResult.ACTIVATED, "首次使用幸运草应激活成功");
        Gold gold = new Gold(0, 0);
        checkEq(25, gold.getScore(), "小金块基础价值应为 25");
        // 25 × 1.5 = 37.5 → 四舍五入 38
        checkEq(38, GameManagerImpl.settleNormalItem(gold, player),
                "幸运草 ×1.5 后应四舍五入");
    }

    private void settlementChainAppliesStoneBookBeforeClover() {
        Player player = new PlayerImpl();
        player.addStoneBook(1);
        player.useStoneBook();
        player.addLuckyClover(1);
        player.useLuckyClover();
        Stone stone = new Stone(0, 0);
        // 顺序：11 ×3 = 33 → 33 ×1.5 = 49.5 → 四舍五入 50
        checkEq(50, GameManagerImpl.settleNormalItem(stone, player),
                "结算链应为 石头×3 后再 幸运草×1.5 四舍五入");
    }

    private void noBonusReturnsBaseValue() {
        Player player = new PlayerImpl();
        checkEq(11, GameManagerImpl.settleNormalItem(new Stone(0, 0), player),
                "无道具石头应得基础 11");
        checkEq(600, GameManagerImpl.settleNormalItem(new Diamond(0, 0), player),
                "无道具钻石应得基础 600");
        checkEq(25, GameManagerImpl.settleNormalItem(new Gold(0, 0), player),
                "无道具小金块应得基础 25");
    }

    // ===== FR-14 金币档 / FR-15 库存上限 / FR-18 重复转 50 金 =====

    private void goldRewardIsIn100To800Range() {
        Random rnd = new Random(7L);
        Player player = new PlayerImpl();
        for (int i = 0; i < 200; i++) {
            int gold = GameManagerImpl.grantBagExtra(
                    GameConfig.MysteryReward.MYSTERY_GOLD, player, rnd);
            checkTrue(gold >= GameConfig.MYSTERY_BAG_MIN_GOLD
                            && gold <= GameConfig.MYSTERY_BAG_MAX_GOLD,
                    "金币档应在 100~800 之间，实际 " + gold);
        }
    }

    private void dynamiteOverflowConvertsTo50Gold() {
        Player player = new PlayerImpl();
        // 开局自带 1 个炸药，补到上限 3
        player.addDynamite(10);
        checkEq(GameConfig.PLAYER_MAX_DYNAMITE_COUNT, player.getDynamiteCount(),
                "炸药库存应被限制在上限 3");

        int gold = GameManagerImpl.grantBagExtra(
                GameConfig.MysteryReward.DYNAMITE, player, new Random());
        checkEq(GameConfig.ITEM_DUP_AUTO_GOLD, gold, "炸药满 3 再获得应转 50 金币");
        checkEq(GameConfig.PLAYER_MAX_DYNAMITE_COUNT, player.getDynamiteCount(),
                "溢出后炸药库存不得超限");
    }

    private void dynamiteNormalGainEntersInventory() {
        Player player = new PlayerImpl();
        int gold = GameManagerImpl.grantBagExtra(
                GameConfig.MysteryReward.DYNAMITE, player, new Random());
        checkEq(0, gold, "炸药正常入库不附带金币");
        checkEq(2, player.getDynamiteCount(), "开局 1 个 + 福袋 1 个 = 2");
    }

    private void shortItemCapFiveThenConvertsTo50Gold() {
        Player player = new PlayerImpl();
        checkEq(5, player.addPowerPotion(10), "强力药水应入库到上限 5");
        checkEq(GameConfig.ITEM_DUP_AUTO_GOLD,
                GameManagerImpl.grantBagExtra(GameConfig.MysteryReward.POWER_POTION,
                        player, new Random()),
                "强力药水满 5 再获得应转 50 金币");
        checkEq(5, player.getPowerPotionCount(), "溢出后强力药水库存不得超限");

        checkEq(5, player.addFreezeBox(10), "冰冻箱应入库到上限 5");
        checkEq(GameConfig.ITEM_DUP_AUTO_GOLD,
                GameManagerImpl.grantBagExtra(GameConfig.MysteryReward.FREEZE_BOX,
                        player, new Random()),
                "冰冻箱满 5 再获得应转 50 金币");
        checkEq(5, player.getFreezeBoxCount(), "溢出后冰冻箱库存不得超限");
    }

    private void shortItemNormalGainEntersInventory() {
        Player player = new PlayerImpl();
        checkEq(0, GameManagerImpl.grantBagExtra(
                GameConfig.MysteryReward.POWER_POTION, player, new Random()),
                "强力药水正常入库不附带金币");
        checkEq(1, player.getPowerPotionCount(), "强力药水库存应为 1");
        checkEq(0, GameManagerImpl.grantBagExtra(
                GameConfig.MysteryReward.FREEZE_BOX, player, new Random()),
                "冰冻箱正常入库不附带金币");
        checkEq(1, player.getFreezeBoxCount(), "冰冻箱库存应为 1");
    }

    private void persistentItemFirstGainActivatesSecondGainConvertsTo50Gold() {
        Player player = new PlayerImpl();

        // FR-18：持续道具先入库存（grantBagExtra 返回 0=成功入库），玩家按键 use 激活
        checkEq(0, GameManagerImpl.grantBagExtra(
                GameConfig.MysteryReward.LUCKY_CLOVER, player, new Random()),
                "首次获得幸运草应入库，不转金币");
        checkEq(1, player.getLuckyCloverCount(), "幸运草库存应为 1");
        checkTrue(player.useLuckyClover() == Main.model.PersistItemUseResult.ACTIVATED, "按键使用幸运草应首次激活");
        checkTrue(player.hasLuckyClover(), "幸运草应处于激活状态");
        // FR-18：已激活再抽 → grantBagExtra 入库存（返回 0），按键 use 时转 50 金币
        checkEq(0, GameManagerImpl.grantBagExtra(
                GameConfig.MysteryReward.LUCKY_CLOVER, player, new Random()),
                "已激活再抽幸运草应入库不转金币");
        checkTrue(player.useLuckyClover() == Main.model.PersistItemUseResult.DUPLICATE_GOLD,
                "已激活再使用幸运草应转 50 金币");

        checkEq(0, GameManagerImpl.grantBagExtra(
                GameConfig.MysteryReward.DIAMOND_BOOST, player, new Random()),
                "首次获得钻石药水应入库");
        checkTrue(player.useDiamondBoost() == Main.model.PersistItemUseResult.ACTIVATED, "按键使用钻石药水应首次激活");
        checkTrue(player.hasDiamondBoost(), "钻石药水应处于激活状态");
        checkEq(0, GameManagerImpl.grantBagExtra(
                GameConfig.MysteryReward.DIAMOND_BOOST, player, new Random()),
                "已激活再抽钻石药水应入库不转金币");
        checkTrue(player.useDiamondBoost() == Main.model.PersistItemUseResult.DUPLICATE_GOLD,
                "已激活再使用钻石药水应转 50 金币");

        checkEq(0, GameManagerImpl.grantBagExtra(
                GameConfig.MysteryReward.STONE_BOOK, player, new Random()),
                "首次获得石头书应入库");
        checkTrue(player.useStoneBook() == Main.model.PersistItemUseResult.ACTIVATED, "按键使用石头书应首次激活");
        checkTrue(player.hasStoneBook(), "石头书应处于激活状态");
        checkEq(0, GameManagerImpl.grantBagExtra(
                GameConfig.MysteryReward.STONE_BOOK, player, new Random()),
                "已激活再抽石头书应入库不转金币");
        checkTrue(player.useStoneBook() == Main.model.PersistItemUseResult.DUPLICATE_GOLD,
                "已激活再使用石头书应转 50 金币");
    }

    private void playerStartsWithOneDynamite() {
        Player player = new PlayerImpl();
        checkEq(GameConfig.PLAYER_INIT_DYNAMITE_COUNT, player.getDynamiteCount(),
                "FR-15：开局每人 1 个炸药");
        checkFalse(player.hasLuckyClover(), "开局不应激活幸运草");
        checkFalse(player.hasDiamondBoost(), "开局不应激活钻石药水");
        checkFalse(player.hasStoneBook(), "开局不应激活石头书");
    }
}
