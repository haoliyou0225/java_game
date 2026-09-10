// FR-13 鼹鼠验收：10 金 / 收回耗时 1.0s，全程在矿洞内部随机游走、撞边反弹、
// 水平移动且 Y 不变、被钩爪携带后立即静止
package Main.model;

import Main.config.Config;
import Main.config.GameConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoleTest {

    @Test
    void moleStatsMatchFr10Table() {
        Mole mole = new Mole(600, 400);
        assertEquals(GameConfig.MOLE_CAPTURE_GOLD, mole.getScore(), "鼹鼠基础分值应为 10");
        assertEquals(1.0, mole.getWeight(), 1e-9, "鼹鼠收回耗时应为 1.0s（最轻档）");
        assertEquals(1.0, mole.getRetractDuration(), 1e-9);
        assertEquals(24, mole.getRadius(), 1e-9);
    }

    @Test
    void moleRoamsInsideMineAndBouncesOffBounds() {
        Mole mole = new Mole(60, 400);
        double originY = mole.getY();
        boolean moved = false;
        // 推进约 160 秒（10000 帧），全程被限制在矿洞内部
        for (int frame = 0; frame < 10000; frame++) {
            double before = mole.getX();
            mole.updatePosition();
            moved |= Math.abs(mole.getX() - before) > 1e-9;

            assertTrue(mole.getX() >= GameConfig.MINE_MIN_X - 1e-9,
                    "鼹鼠越出左边界 x=" + mole.getX());
            assertTrue(mole.getX() <= Config.WIDTH - 50 + 1e-9,
                    "鼹鼠越出右边界 x=" + mole.getX());
            assertEquals(originY, mole.getY(), 1e-9, "鼹鼠只水平移动，Y 必须保持不变");
        }
        assertTrue(moved, "鼹鼠应全程游走而非静止");
    }

    @Test
    void grabbedMoleStopsMoving() {
        Mole mole = new Mole(600, 400);
        mole.setGrabbed(true);
        double x = mole.getX();
        for (int i = 0; i < 300; i++) {
            mole.updatePosition();
        }
        assertEquals(x, mole.getX(), 1e-9, "被钩爪携带后鼹鼠应停止移动");
    }
}
