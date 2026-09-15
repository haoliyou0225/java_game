package Main.model;

import Main.config.GameConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PlayerImpl 单元测试：分数、炸药库存、短时道具、持续道具的入库/消耗/激活逻辑。
 */
class PlayerImplTest {

    private PlayerImpl player;

    @BeforeEach
    void setUp() {
        player = new PlayerImpl();
    }

    // ==================== 分数 ====================

    @Test
    void initialScore_shouldBeZero() {
        assertEquals(0, player.getScore());
    }

    @Test
    void addScore_shouldAccumulate() {
        player.addScore(100);
        player.addScore(50);
        assertEquals(150, player.getScore());
    }

    @Test
    void addScore_shouldAllowNegative() {
        player.addScore(100);
        player.addScore(-30);
        assertEquals(70, player.getScore());
    }

    // ==================== 炸药 ====================

    @Test
    void initialDynamite_shouldMatchConfig() {
        assertEquals(GameConfig.PLAYER_INIT_DYNAMITE_COUNT, player.getDynamiteCount());
    }

    @Test
    void useDynamite_shouldDecreaseAndReturnTrue_whenInStock() {
        int before = player.getDynamiteCount();
        assertTrue(player.useDynamite());
        assertEquals(before - 1, player.getDynamiteCount());
    }

    @Test
    void useDynamite_shouldReturnFalse_whenOutOfStock() {
        // 耗尽炸药
        while (player.useDynamite()) { /* noop */ }
        assertEquals(0, player.getDynamiteCount());
        assertFalse(player.useDynamite());
    }

    @Test
    void addDynamite_shouldRespectMaxLimit() {
        int max = GameConfig.PLAYER_MAX_DYNAMITE_COUNT;
        // 先清零
        while (player.useDynamite()) { /* noop */ }
        int added = player.addDynamite(max + 5);
        assertEquals(max, player.getDynamiteCount());
        assertEquals(max, added);
    }

    @Test
    void addDynamite_shouldReturnZero_whenCountNotPositive() {
        assertEquals(0, player.addDynamite(0));
        assertEquals(0, player.addDynamite(-3));
    }

    // ==================== 短时道具（强力药水 / 冰冻箱） ====================

    @Test
    void shortItem_shouldStartEmpty() {
        assertEquals(0, player.getPowerPotionCount());
        assertEquals(0, player.getFreezeBoxCount());
    }

    @Test
    void addPowerPotion_shouldRespectMaxLimit() {
        int max = GameConfig.PLAYER_MAX_SHORT_ITEM_COUNT;
        int added = player.addPowerPotion(max + 10);
        assertEquals(max, player.getPowerPotionCount());
        assertEquals(max, added);
    }

    @Test
    void consumePowerPotion_shouldWork() {
        player.addPowerPotion(2);
        assertTrue(player.consumePowerPotion());
        assertEquals(1, player.getPowerPotionCount());
        assertTrue(player.consumePowerPotion());
        assertFalse(player.consumePowerPotion());
    }

    @Test
    void addFreezeBox_andConsume_shouldWork() {
        assertEquals(0, player.addFreezeBox(0));
        player.addFreezeBox(1);
        assertTrue(player.consumeFreezeBox());
        assertFalse(player.consumeFreezeBox());
    }

    // ==================== 持续道具（幸运草 / 钻石升级 / 石头书） ====================

    @Test
    void persistItem_shouldStartInactiveAndEmpty() {
        assertFalse(player.hasLuckyClover());
        assertFalse(player.hasDiamondBoost());
        assertFalse(player.hasStoneBook());
        assertEquals(0, player.getLuckyCloverCount());
        assertEquals(0, player.getDiamondBoostCount());
        assertEquals(0, player.getStoneBookCount());
    }

    @Test
    void usePersistItem_shouldReturnNoStock_whenEmpty() {
        assertEquals(PersistItemUseResult.NO_STOCK, player.useLuckyClover());
        assertEquals(PersistItemUseResult.NO_STOCK, player.useDiamondBoost());
        assertEquals(PersistItemUseResult.NO_STOCK, player.useStoneBook());
    }

    @Test
    void useLuckyClover_shouldActivateOnFirstUse() {
        player.addLuckyClover(1);
        PersistItemUseResult result = player.useLuckyClover();
        assertEquals(PersistItemUseResult.ACTIVATED, result);
        assertTrue(player.hasLuckyClover());
        assertEquals(0, player.getLuckyCloverCount());
    }

    @Test
    void useLuckyClover_shouldConvertToGold_whenAlreadyActive() {
        player.addLuckyClover(2);
        player.useLuckyClover(); // 激活
        int scoreBefore = player.getScore();
        PersistItemUseResult result = player.useLuckyClover(); // 已激活
        assertEquals(PersistItemUseResult.DUPLICATE_GOLD, result);
        assertEquals(scoreBefore + GameConfig.ITEM_DUP_AUTO_GOLD, player.getScore());
    }

    @Test
    void addPersistItem_shouldRespectMaxLimit() {
        int max = GameConfig.PLAYER_MAX_PERSIST_ITEM_COUNT;
        int added = player.addStoneBook(max + 3);
        assertEquals(max, player.getStoneBookCount());
        assertEquals(max, added);
    }

    @Test
    void addPersistItem_shouldReturnZero_whenCountNotPositive() {
        assertEquals(0, player.addLuckyClover(0));
        assertEquals(0, player.addDiamondBoost(-1));
    }
}
