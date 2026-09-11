// FR-11 钻石猪验收：生成时预计算 2~5 颗钻石、分值=颗数×600+10、中档重量 3.0s、
// 捕获率约 40%、矿洞内部移动且边界反弹、逃脱/定时加速持续 0.5 秒
package Main.model;

import Main.config.GameConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiamondPigTest {

    @Test
    void pigCarriesTwoToFiveDiamondsAndScoreMatchesFormula() {
        for (int i = 0; i < 500; i++) {
            DiamondPig pig = new DiamondPig(600, 400);
            assertTrue(pig.getDiamonds() >= GameConfig.PIG_DIAMOND_MIN
                            && pig.getDiamonds() <= GameConfig.PIG_DIAMOND_MAX,
                    "钻石颗数应在 2~5 之间，实际 " + pig.getDiamonds());
            assertEquals(pig.getDiamonds() * GameConfig.PIG_DIAMOND_VALUE + GameConfig.PIG_BASE_GOLD,
                    pig.getScore(), "分值应为 颗数×600+10");
        }
    }

    @Test
    void pigIsMidTierWeightThreeSeconds() {
        DiamondPig pig = new DiamondPig(600, 400);
        assertEquals(3.0, pig.getWeight(), 1e-9, "钻石猪重量应为中档 3.0s");
        assertEquals(3.0, pig.getRetractDuration(), 1e-9, "钻石猪收回耗时应为 3.0s");
        assertEquals(26, pig.getRadius(), 1e-9);
    }

    @Test
    void captureRateIsAroundFortyPercent() {
        DiamondPig pig = new DiamondPig(600, 400);
        int trials = 20000;
        int captured = 0;
        for (int i = 0; i < trials; i++) {
            if (pig.tryCapture()) captured++;
        }
        double rate = (double) captured / trials;
        assertTrue(rate > 0.35 && rate < 0.45,
                "钻石猪目标捕获率约 40%，实际 " + rate);
    }

    @Test
    void pigStaysInsideMineAndBouncesOffBounds() {
        DiamondPig pig = new DiamondPig(1220, 400);
        // 推进约 80 秒（5000 帧），多次横穿矿洞，全程不得越界
        for (int frame = 0; frame < 5000; frame++) {
            pig.updatePosition();
            assertTrue(pig.getX() >= GameConfig.MINE_MIN_X - 1e-9,
                    "钻石猪越出左边界 x=" + pig.getX());
            assertTrue(pig.getX() <= GameConfig.MINE_MAX_X + 1e-9,
                    "钻石猪越出右边界 x=" + pig.getX());
        }
    }

    @Test
    void dashLastsHalfSecond() {
        DiamondPig pig = new DiamondPig(600, 400);
        pig.dash();
        assertTrue(pig.isDashing(), "触发冲刺后应处于加速状态");
        // 0.5 秒 = 约 32 帧；推进 40 帧（0.64s）后加速必须结束，
        // 且定时加速间隔 ≥2 秒，此期间不会被定时触发刷新
        for (int i = 0; i < 40; i++) {
            pig.updatePosition();
        }
        assertFalse(pig.isDashing(), "冲刺应在 0.5 秒后结束");
    }

    @Test
    void grabbedPigStopsMoving() {
        DiamondPig pig = new DiamondPig(600, 400);
        pig.setGrabbed(true);
        double x = pig.getX();
        for (int i = 0; i < 300; i++) {
            pig.updatePosition();
        }
        assertEquals(x, pig.getX(), 1e-9, "被钩爪携带后钻石猪应停止自主移动");
    }
}
