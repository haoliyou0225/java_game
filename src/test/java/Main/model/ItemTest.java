package Main.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 物品子类（Gold / BigGold / MediumGold / Diamond / Stone / Bomb）构造与属性测试。
 * 验证 FR-10 数值表：分值、重量（收回耗时）、半径。
 */
class ItemTest {

    // ==================== Gold 小金块 ====================

    @Test
    void gold_shouldHaveCorrectProperties() {
        Gold gold = new Gold(10, 20);
        assertEquals(10, gold.getX());
        assertEquals(20, gold.getY());
        assertEquals(25, gold.getScore());
        assertEquals(1.2, gold.getWeight());
        assertEquals(25, gold.getSettlementGold()); // 默认 = score
        assertEquals(1.2, gold.getRetractDuration()); // 默认 = weight
        assertFalse(gold.isGrabbed());
    }

    // ==================== BigGold 大金块 ====================

    @Test
    void bigGold_shouldHaveCorrectProperties() {
        BigGold big = new BigGold(100, 200);
        assertEquals(100, big.getX());
        assertEquals(200, big.getY());
        assertEquals(400, big.getScore());
        assertEquals(5.0, big.getWeight());
        assertEquals(28, big.getRadius()); // 大金块半径最大
    }

    // ==================== MediumGold 中金块 ====================

    @Test
    void mediumGold_shouldHaveCorrectProperties() {
        MediumGold medium = new MediumGold(0, 0);
        assertEquals(120, medium.getScore());
        assertEquals(2.5, medium.getWeight());
        assertEquals(21, medium.getRadius());
    }

    // ==================== Diamond 钻石 ====================

    @Test
    void diamond_shouldHaveCorrectProperties() {
        Diamond diamond = new Diamond(50, 60);
        assertEquals(600, diamond.getScore());
        assertEquals(2.0, diamond.getWeight());
    }

    // ==================== Stone 石头 ====================

    @Test
    void stone_shouldHaveCorrectProperties() {
        Stone stone = new Stone(30, 40);
        assertEquals(11, stone.getScore());
        assertEquals(4.0, stone.getWeight());
    }

    @Test
    void stone_onGrab_shouldSetGrabbed() {
        Stone stone = new Stone(0, 0);
        stone.onGrab(null);
        assertTrue(stone.isGrabbed());
    }

    // ==================== Bomb 炸弹 ====================

    @Test
    void bomb_shouldHaveCorrectProperties() {
        Bomb bomb = new Bomb(0, 0);
        assertEquals(-150, bomb.getScore());
        assertEquals(1.5, bomb.getWeight());
        assertFalse(bomb.isExploded());
    }

    @Test
    void bomb_onGrab_shouldSetGrabbed() {
        Bomb bomb = new Bomb(0, 0);
        bomb.onGrab(null);
        assertTrue(bomb.isGrabbed());
    }

    @Test
    void bomb_triggerExplode_shouldMarkExploded() {
        Bomb bomb = new Bomb(0, 0);
        bomb.triggerExplode();
        assertTrue(bomb.isExploded());
    }

    // ==================== 通用 setX / setY / setGrabbed ====================

    @Test
    void item_setters_shouldUpdateState() {
        Gold gold = new Gold(0, 0);
        gold.setX(77);
        gold.setY(88);
        gold.setGrabbed(true);
        assertEquals(77, gold.getX());
        assertEquals(88, gold.getY());
        assertTrue(gold.isGrabbed());
    }

    @Test
    void defaultRadius_shouldBe15() {
        // Gold / Diamond 未重写 getRadius，使用默认值 15
        assertEquals(15, new Gold(0, 0).getRadius());
        assertEquals(15, new Diamond(0, 0).getRadius());
    }
}
