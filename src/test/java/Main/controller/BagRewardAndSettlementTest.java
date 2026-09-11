// FR-14/FR-15/FR-17/FR-18 福袋与结算验收：
// 加权抽奖 35%金币/35%炸药/20%短时道具/10%持续道具；
// 结算链 基础价值→石头×3→钻石×2→幸运草×1.5→四舍五入；
// 库存上限（炸药3/短时道具5/持续单激活位）溢出转 50 金币，持续道具重复获得转 50 金币
package Main.controller;

import Main.config.GameConfig;
import Main.model.Diamond;
import Main.model.Gold;
import Main.model.Player;
import Main.model.PlayerImpl;
import Main.model.Stone;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BagRewardAndSettlementTest {

    // ===== FR-14 加权抽奖权重 =====

    @Test
    void rollBagExtraRewardMatches35_35_20_10Weights() {
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

        assertTrue(Math.abs(goldPct - 35) < 3, "金币档应约 35%，实际 " + goldPct);
        assertTrue(Math.abs(dynamitePct - 35) < 3, "炸药档应约 35%，实际 " + dynamitePct);
        assertTrue(Math.abs(shortPct - 20) < 3, "短时道具档应约 20%，实际 " + shortPct);
        assertTrue(Math.abs(persistPct - 10) < 3, "持续道具档应约 10%，实际 " + persistPct);
    }

    private double pct(Map<GameConfig.MysteryReward, Integer> counts,
                       GameConfig.MysteryReward reward, int trials) {
        return counts.getOrDefault(reward, 0) * 100.0 / trials;
    }

    // ===== FR-17 结算链顺序与倍率 =====

    @Test
    void stoneBookTriplesStone() {
        Player player = new PlayerImpl();
        assertTrue(player.grantStoneBook());
        Stone stone = new Stone(0, 0);
        assertEquals(GameConfig.STONE_BASE_GOLD, stone.getScore());
        assertEquals(GameConfig.STONE_BASE_GOLD * GameConfig.STONE_BOOK_MULTIPLIER,
                GameManagerImpl.settleNormalItem(stone, player),
                "石头书激活后石头价值应 ×3");
    }

    @Test
    void diamondBoostDoublesDiamond() {
        Player player = new PlayerImpl();
        assertTrue(player.grantDiamondBoost());
        Diamond diamond = new Diamond(0, 0);
        assertEquals(600 * GameConfig.DIAMOND_BOOST_MULTIPLIER,
                GameManagerImpl.settleNormalItem(diamond, player),
                "钻石药水激活后钻石价值应 ×2");
    }

    @Test
    void luckyCloverMultipliesByOnePointFiveAndRounds() {
        Player player = new PlayerImpl();
        assertTrue(player.grantLuckyClover());
        Gold gold = new Gold(0, 0);
        assertEquals(25, gold.getScore());
        // 25 × 1.5 = 37.5 → 四舍五入 38
        assertEquals(38, GameManagerImpl.settleNormalItem(gold, player),
                "幸运草 ×1.5 后应四舍五入");
    }

    @Test
    void settlementChainAppliesStoneBookBeforeClover() {
        Player player = new PlayerImpl();
        player.grantStoneBook();
        player.grantLuckyClover();
        Stone stone = new Stone(0, 0);
        // 顺序：11 ×3 = 33 → 33 ×1.5 = 49.5 → 四舍五入 50
        assertEquals(50, GameManagerImpl.settleNormalItem(stone, player),
                "结算链应为 石头×3 后再 幸运草×1.5 四舍五入");
    }

    @Test
    void noBonusReturnsBaseValue() {
        Player player = new PlayerImpl();
        assertEquals(11, GameManagerImpl.settleNormalItem(new Stone(0, 0), player));
        assertEquals(600, GameManagerImpl.settleNormalItem(new Diamond(0, 0), player));
        assertEquals(25, GameManagerImpl.settleNormalItem(new Gold(0, 0), player));
    }

    // ===== FR-14 金币档 / FR-15 库存上限 / FR-18 重复转 50 金 =====

    @Test
    void goldRewardIsIn100To800Range() {
        Random rnd = new Random(7L);
        Player player = new PlayerImpl();
        for (int i = 0; i < 200; i++) {
            int gold = GameManagerImpl.grantBagExtra(
                    GameConfig.MysteryReward.MYSTERY_GOLD, player, rnd);
            assertTrue(gold >= GameConfig.MYSTERY_BAG_MIN_GOLD
                            && gold <= GameConfig.MYSTERY_BAG_MAX_GOLD,
                    "金币档应在 100~800 之间，实际 " + gold);
        }
    }

    @Test
    void dynamiteOverflowConvertsTo50Gold() {
        Player player = new PlayerImpl();
        // 开局自带 1 个炸药，补到上限 3
        player.addDynamite(10);
        assertEquals(GameConfig.PLAYER_MAX_DYNAMITE_COUNT, player.getDynamiteCount());

        int gold = GameManagerImpl.grantBagExtra(
                GameConfig.MysteryReward.DYNAMITE, player, new Random());
        assertEquals(GameConfig.ITEM_DUP_AUTO_GOLD, gold, "炸药满 3 再获得应转 50 金币");
        assertEquals(GameConfig.PLAYER_MAX_DYNAMITE_COUNT, player.getDynamiteCount(),
                "溢出后炸药库存不得超限");
    }

    @Test
    void dynamiteNormalGainEntersInventory() {
        Player player = new PlayerImpl();
        int gold = GameManagerImpl.grantBagExtra(
                GameConfig.MysteryReward.DYNAMITE, player, new Random());
        assertEquals(0, gold, "炸药正常入库不附带金币");
        assertEquals(2, player.getDynamiteCount(), "开局 1 个 + 福袋 1 个 = 2");
    }

    @Test
    void shortItemCapFiveThenConvertsTo50Gold() {
        Player player = new PlayerImpl();
        assertEquals(5, player.addPowerPotion(10), "强力药水应入库到上限 5");
        assertEquals(GameConfig.ITEM_DUP_AUTO_GOLD,
                GameManagerImpl.grantBagExtra(GameConfig.MysteryReward.POWER_POTION,
                        player, new Random()),
                "强力药水满 5 再获得应转 50 金币");
        assertEquals(5, player.getPowerPotionCount());

        assertEquals(5, player.addFreezeBox(10), "冰冻箱应入库到上限 5");
        assertEquals(GameConfig.ITEM_DUP_AUTO_GOLD,
                GameManagerImpl.grantBagExtra(GameConfig.MysteryReward.FREEZE_BOX,
                        player, new Random()),
                "冰冻箱满 5 再获得应转 50 金币");
        assertEquals(5, player.getFreezeBoxCount());
    }

    @Test
    void shortItemNormalGainEntersInventory() {
        Player player = new PlayerImpl();
        assertEquals(0, GameManagerImpl.grantBagExtra(
                GameConfig.MysteryReward.POWER_POTION, player, new Random()));
        assertEquals(1, player.getPowerPotionCount());
        assertEquals(0, GameManagerImpl.grantBagExtra(
                GameConfig.MysteryReward.FREEZE_BOX, player, new Random()));
        assertEquals(1, player.getFreezeBoxCount());
    }

    @Test
    void persistentItemFirstGainActivatesSecondGainConvertsTo50Gold() {
        Player player = new PlayerImpl();

        assertEquals(0, GameManagerImpl.grantBagExtra(
                GameConfig.MysteryReward.LUCKY_CLOVER, player, new Random()),
                "首次获得幸运草应激活，不转金币");
        assertTrue(player.hasLuckyClover());
        assertEquals(GameConfig.ITEM_DUP_AUTO_GOLD, GameManagerImpl.grantBagExtra(
                GameConfig.MysteryReward.LUCKY_CLOVER, player, new Random()),
                "已激活再抽幸运草应转 50 金币");

        assertEquals(0, GameManagerImpl.grantBagExtra(
                GameConfig.MysteryReward.DIAMOND_BOOST, player, new Random()));
        assertTrue(player.hasDiamondBoost());
        assertEquals(GameConfig.ITEM_DUP_AUTO_GOLD, GameManagerImpl.grantBagExtra(
                GameConfig.MysteryReward.DIAMOND_BOOST, player, new Random()));

        assertEquals(0, GameManagerImpl.grantBagExtra(
                GameConfig.MysteryReward.STONE_BOOK, player, new Random()));
        assertTrue(player.hasStoneBook());
        assertEquals(GameConfig.ITEM_DUP_AUTO_GOLD, GameManagerImpl.grantBagExtra(
                GameConfig.MysteryReward.STONE_BOOK, player, new Random()));
    }

    @Test
    void playerStartsWithOneDynamite() {
        Player player = new PlayerImpl();
        assertEquals(GameConfig.PLAYER_INIT_DYNAMITE_COUNT, player.getDynamiteCount(),
                "FR-15：开局每人 1 个炸药");
        assertFalse(player.hasLuckyClover());
        assertFalse(player.hasDiamondBoost());
        assertFalse(player.hasStoneBook());
    }
}
