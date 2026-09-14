// FR-14/FR-15/FR-17/FR-18 福袋与结算验收自检：
// 加权抽奖 炸药20%/其他5种道具各16%（仅道具，不开出金币）；
// 结算链 基础价值→石头×3→钻石×2→幸运草×1.5→四舍五入；
// 库存上限（炸药3/短时道具5/持续道具5）：福袋溢出时道具丢弃，不折算金币；
// 持续道具已激活时玩家主动按键再次使用才折 50 金币（玩家行为，非福袋发放）。
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
        check.rollBagExtraRewardMatches20_16_16_16_16_16Weights();
        check.stoneBookTriplesStone();
        check.diamondBoostDoublesDiamond();
        check.luckyCloverMultipliesByOnePointFiveAndRounds();
        check.settlementChainAppliesStoneBookBeforeClover();
        check.noBonusReturnsBaseValue();
        check.dynamiteOverflowDiscardedWithoutGold();
        check.dynamiteNormalGainEntersInventory();
        check.shortItemCapFiveThenDiscarded();
        check.shortItemNormalGainEntersInventory();
        check.persistentItemEntersInventoryAndReuseConvertsTo50Gold();
        check.playerStartsWithOneDynamite();
        check.finish();
    }

    // ===== FR-14 加权抽奖权重（炸药 20% / 其他 5 种各 16%，仅道具） =====

    private void rollBagExtraRewardMatches20_16_16_16_16_16Weights() {
        Random rnd = new Random(20260911L);
        int trials = 100000;
        Map<GameConfig.MysteryReward, Integer> counts =
                new EnumMap<>(GameConfig.MysteryReward.class);
        for (int i = 0; i < trials; i++) {
            GameConfig.MysteryReward r = GameManagerImpl.rollBagExtraReward(rnd);
            counts.merge(r, 1, Integer::sum);
        }

        // 六类奖励概率合计应为 100%（金币档已移除，枚举中不再存在）
        int total = counts.values().stream().mapToInt(Integer::intValue).sum();
        checkEq(trials, total, "所有抽奖结果都应落在六种道具奖励内");

        double dynamitePct = pct(counts, GameConfig.MysteryReward.DYNAMITE, trials);
        checkTrue(Math.abs(dynamitePct - 20) < 3, "炸药档应约 20%，实际 " + dynamitePct);

        double powerPct = pct(counts, GameConfig.MysteryReward.POWER_POTION, trials);
        checkTrue(Math.abs(powerPct - 16) < 3, "强力药水应约 16%，实际 " + powerPct);

        double freezePct = pct(counts, GameConfig.MysteryReward.FREEZE_BOX, trials);
        checkTrue(Math.abs(freezePct - 16) < 3, "冰冻箱应约 16%，实际 " + freezePct);

        double cloverPct = pct(counts, GameConfig.MysteryReward.LUCKY_CLOVER, trials);
        checkTrue(Math.abs(cloverPct - 16) < 3, "幸运草应约 16%，实际 " + cloverPct);

        double diamondPct = pct(counts, GameConfig.MysteryReward.DIAMOND_BOOST, trials);
        checkTrue(Math.abs(diamondPct - 16) < 3, "钻石升级应约 16%，实际 " + diamondPct);

        double stonePct = pct(counts, GameConfig.MysteryReward.STONE_BOOK, trials);
        checkTrue(Math.abs(stonePct - 16) < 3, "石头书应约 16%，实际 " + stonePct);
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

    // ===== FR-15 库存上限：福袋溢出道具直接丢弃，不折算金币 =====

    private void dynamiteOverflowDiscardedWithoutGold() {
        Player player = new PlayerImpl();
        int scoreBefore = player.getScore();
        // 开局自带 1 个炸药，补到上限 3
        player.addDynamite(10);
        checkEq(GameConfig.PLAYER_MAX_DYNAMITE_COUNT, player.getDynamiteCount(),
                "炸药库存应被限制在上限 3");

        // 满库存再从福袋获得：丢弃、库存不变、分数不变（无金币折算）
        GameManagerImpl.grantBagExtra(GameConfig.MysteryReward.DYNAMITE, player);
        checkEq(GameConfig.PLAYER_MAX_DYNAMITE_COUNT, player.getDynamiteCount(),
                "溢出后炸药库存不得超限");
        checkEq(scoreBefore, player.getScore(), "福袋溢出不得折算金币");
    }

    private void dynamiteNormalGainEntersInventory() {
        Player player = new PlayerImpl();
        GameManagerImpl.grantBagExtra(GameConfig.MysteryReward.DYNAMITE, player);
        checkEq(2, player.getDynamiteCount(), "开局 1 个 + 福袋 1 个 = 2");
        checkEq(0, player.getScore(), "福袋道具正常入库不附带金币");
    }

    private void shortItemCapFiveThenDiscarded() {
        Player player = new PlayerImpl();
        int scoreBefore = player.getScore();
        checkEq(5, player.addPowerPotion(10), "强力药水应入库到上限 5");
        GameManagerImpl.grantBagExtra(GameConfig.MysteryReward.POWER_POTION, player);
        checkEq(5, player.getPowerPotionCount(), "溢出后强力药水库存不得超限");

        checkEq(5, player.addFreezeBox(10), "冰冻箱应入库到上限 5");
        GameManagerImpl.grantBagExtra(GameConfig.MysteryReward.FREEZE_BOX, player);
        checkEq(5, player.getFreezeBoxCount(), "溢出后冰冻箱库存不得超限");

        checkEq(scoreBefore, player.getScore(), "短时道具福袋溢出不得折算金币");
    }

    private void shortItemNormalGainEntersInventory() {
        Player player = new PlayerImpl();
        GameManagerImpl.grantBagExtra(GameConfig.MysteryReward.POWER_POTION, player);
        checkEq(1, player.getPowerPotionCount(), "强力药水库存应为 1");
        GameManagerImpl.grantBagExtra(GameConfig.MysteryReward.FREEZE_BOX, player);
        checkEq(1, player.getFreezeBoxCount(), "冰冻箱库存应为 1");
        checkEq(0, player.getScore(), "短时道具正常入库不附带金币");
    }

    // ===== FR-18 持续道具：福袋只入库存；按键首次激活，已激活再使用才折 50 金币 =====

    private void persistentItemEntersInventoryAndReuseConvertsTo50Gold() {
        Player player = new PlayerImpl();

        // FR-18：持续道具从福袋获得只入库存，玩家再按键 use 激活
        GameManagerImpl.grantBagExtra(GameConfig.MysteryReward.LUCKY_CLOVER, player);
        checkEq(1, player.getLuckyCloverCount(), "幸运草库存应为 1");
        checkTrue(player.useLuckyClover() == Main.model.PersistItemUseResult.ACTIVATED, "按键使用幸运草应首次激活");
        checkTrue(player.hasLuckyClover(), "幸运草应处于激活状态");
        // 已激活再从福袋获得：仍入库存（库存 2），不发金币
        GameManagerImpl.grantBagExtra(GameConfig.MysteryReward.LUCKY_CLOVER, player);
        checkEq(2, player.getLuckyCloverCount(), "已激活再抽幸运草应继续入库存");
        checkEq(0, player.getScore(), "福袋发放幸运草不得附带金币");
        // 玩家主动再次按键使用 → 折 50 金币
        checkTrue(player.useLuckyClover() == Main.model.PersistItemUseResult.DUPLICATE_GOLD,
                "已激活再使用幸运草应转 50 金币");

        GameManagerImpl.grantBagExtra(GameConfig.MysteryReward.DIAMOND_BOOST, player);
        checkEq(1, player.getDiamondBoostCount(), "钻石药水库存应为 1");
        checkTrue(player.useDiamondBoost() == Main.model.PersistItemUseResult.ACTIVATED, "按键使用钻石药水应首次激活");
        checkTrue(player.hasDiamondBoost(), "钻石药水应处于激活状态");
        checkTrue(player.useDiamondBoost() == Main.model.PersistItemUseResult.DUPLICATE_GOLD,
                "已激活再使用钻石药水应转 50 金币");

        GameManagerImpl.grantBagExtra(GameConfig.MysteryReward.STONE_BOOK, player);
        checkEq(1, player.getStoneBookCount(), "石头书库存应为 1");
        checkTrue(player.useStoneBook() == Main.model.PersistItemUseResult.ACTIVATED, "按键使用石头书应首次激活");
        checkTrue(player.hasStoneBook(), "石头书应处于激活状态");
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
