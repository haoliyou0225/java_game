package Main.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CollisionResultImpl 碰撞结果封装单元测试。
 */
class CollisionResultImplTest {

    @Test
    void hitResult_shouldReturnHitTrue_andItem() {
        Item item = new Gold(10, 20);
        CollisionResult result = new CollisionResultImpl(true, item);
        assertTrue(result.isHit());
        assertSame(item, result.getHitItem());
    }

    @Test
    void missResult_shouldReturnHitFalse_andNullItem() {
        CollisionResult result = new CollisionResultImpl(false, null);
        assertFalse(result.isHit());
        assertNull(result.getHitItem());
    }

    @Test
    void hitResult_canCarryNullItem() {
        // 允许命中标记为 true 但物品为 null 的边界情况
        CollisionResult result = new CollisionResultImpl(true, null);
        assertTrue(result.isHit());
        assertNull(result.getHitItem());
    }
}
