package Main.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CollisionUtils 与 CollisionUtil 碰撞检测工具单元测试。
 * 全部为静态纯函数，无外部依赖。
 */
class CollisionUtilsTest {

    // ==================== circlesCollide ====================

    @ParameterizedTest
    @CsvSource({
            "0, 0, 5,  0, 0, 5, true",    // 同心重叠
            "0, 0, 5,  10, 0, 5, true",   // 恰好相切（圆心距 = r1+r2）
            "0, 0, 5,  3, 4, 5, true",    // 圆心距 5 < 10，相交
            "0, 0, 1,  0, 0, 1, true"     // 半径 1 相切重叠
    })
    void circlesCollide_shouldReturnTrue_whenOverlapOrTouch(
            double x1, double y1, double r1,
            double x2, double y2, double r2, boolean expected) {
        assertEquals(expected, CollisionUtils.circlesCollide(x1, y1, r1, x2, y2, r2));
    }

    @ParameterizedTest
    @CsvSource({
            "0, 0, 5,  20, 0, 5, false",  // 圆心距 20 > 10，分离
            "0, 0, 5,  11, 0, 5, false",  // 圆心距 11 > 10，分离
            "100, 100, 2, 200, 200, 2, false"
    })
    void circlesCollide_shouldReturnFalse_whenSeparated(
            double x1, double y1, double r1,
            double x2, double y2, double r2, boolean expected) {
        assertEquals(expected, CollisionUtils.circlesCollide(x1, y1, r1, x2, y2, r2));
    }

    @Test
    void circlesCollide_shouldBeSymmetric() {
        boolean a = CollisionUtils.circlesCollide(1, 2, 3, 4, 5, 6);
        boolean b = CollisionUtils.circlesCollide(4, 5, 6, 1, 2, 3);
        assertEquals(a, b);
    }

    // ==================== pointInCircle ====================

    @Test
    void pointInCircle_shouldReturnTrue_whenPointInside() {
        assertTrue(CollisionUtils.pointInCircle(0, 0, 0, 0, 10));
        assertTrue(CollisionUtils.pointInCircle(3, 4, 0, 0, 10)); // 距离 5
    }

    @Test
    void pointInCircle_shouldReturnTrue_whenPointOnBoundary() {
        // 距离恰好等于半径
        assertTrue(CollisionUtils.pointInCircle(6, 8, 0, 0, 10)); // 距离 10
    }

    @Test
    void pointInCircle_shouldReturnFalse_whenPointOutside() {
        assertFalse(CollisionUtils.pointInCircle(10, 10, 0, 0, 5));
        assertFalse(CollisionUtils.pointInCircle(0, 11, 0, 0, 10));
    }

    // ==================== isOutOfBounds ====================

    @Test
    void isOutOfBounds_shouldReturnFalse_whenInsideRegion() {
        // 区域 [0,100] x [0,200]
        assertFalse(CollisionUtils.isOutOfBounds(50, 100, 0, 0, 100, 200));
        assertFalse(CollisionUtils.isOutOfBounds(0, 0, 0, 0, 100, 200));
        assertFalse(CollisionUtils.isOutOfBounds(100, 200, 0, 0, 100, 200));
    }

    @Test
    void isOutOfBounds_shouldReturnTrue_whenOutsideRegion() {
        assertTrue(CollisionUtils.isOutOfBounds(-1, 50, 0, 0, 100, 200));   // 左越界
        assertTrue(CollisionUtils.isOutOfBounds(101, 50, 0, 0, 100, 200));  // 右越界
        assertTrue(CollisionUtils.isOutOfBounds(50, -1, 0, 0, 100, 200));   // 上越界
        assertTrue(CollisionUtils.isOutOfBounds(50, 201, 0, 0, 100, 200));  // 下越界
    }

    // ==================== CollisionUtil.circleCollision（旧版工具，共存验证） ====================

    @Test
    void circleCollision_shouldDetectOverlap() {
        assertTrue(CollisionUtil.circleCollision(0, 0, 5, 3, 4, 5));
        assertFalse(CollisionUtil.circleCollision(0, 0, 1, 10, 0, 1));
    }
}
