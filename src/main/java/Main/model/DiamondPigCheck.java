// FR-11 钻石猪验收自检：生成时预计算 2~5 颗钻石、分值=颗数×600+10、中档重量 3.0s、
// 捕获率约 40%、矿洞内部移动且边界反弹、逃脱/定时加速持续 0.5 秒。
// 无 JUnit 依赖，直接 java Main.model.DiamondPigCheck 运行。
package Main.model;

import Main.config.GameConfig;
import Main.util.SelfCheck;

public class DiamondPigCheck extends SelfCheck {

    public static void main(String[] args) {
        DiamondPigCheck check = new DiamondPigCheck();
        check.carriesTwoToFiveDiamondsAndScoreMatchesFormula();
        check.isMidTierWeightThreeSeconds();
        check.captureRateIsAroundFortyPercent();
        check.staysInsideMineAndBouncesOffBounds();
        check.dashLastsHalfSecond();
        check.grabbedPigStopsMoving();
        check.finish();
    }

    private void carriesTwoToFiveDiamondsAndScoreMatchesFormula() {
        for (int i = 0; i < 500; i++) {
            DiamondPig pig = new DiamondPig(600, 400);
            checkTrue(pig.getDiamonds() >= GameConfig.PIG_DIAMOND_MIN
                            && pig.getDiamonds() <= GameConfig.PIG_DIAMOND_MAX,
                    "钻石颗数应在 2~5 之间，实际 " + pig.getDiamonds());
            checkEq(pig.getDiamonds() * GameConfig.PIG_DIAMOND_VALUE + GameConfig.PIG_BASE_GOLD,
                    pig.getScore(), "分值应为 颗数×600+10");
        }
    }

    private void isMidTierWeightThreeSeconds() {
        DiamondPig pig = new DiamondPig(600, 400);
        checkEq(3.0, pig.getWeight(), 1e-9, "钻石猪重量应为中档 3.0s");
        checkEq(3.0, pig.getRetractDuration(), 1e-9, "钻石猪收回耗时应为 3.0s");
        checkEq(26.0, pig.getRadius(), 1e-9, "钻石猪碰撞半径应为 26");
    }

    private void captureRateIsAroundFortyPercent() {
        DiamondPig pig = new DiamondPig(600, 400);
        int trials = 20000;
        int captured = 0;
        for (int i = 0; i < trials; i++) {
            if (pig.tryCapture()) captured++;
        }
        double rate = (double) captured / trials;
        checkTrue(rate > 0.35 && rate < 0.45,
                "钻石猪目标捕获率约 40%，实际 " + rate);
    }

    private void staysInsideMineAndBouncesOffBounds() {
        DiamondPig pig = new DiamondPig(1220, 400);
        // 推进约 80 秒（5000 帧），多次横穿矿洞，全程不得越界
        for (int frame = 0; frame < 5000; frame++) {
            pig.updatePosition();
            checkTrue(pig.getX() >= GameConfig.MINE_MIN_X - 1e-9,
                    "钻石猪越出左边界 x=" + pig.getX());
            checkTrue(pig.getX() <= GameConfig.MINE_MAX_X + 1e-9,
                    "钻石猪越出右边界 x=" + pig.getX());
        }
    }

    private void dashLastsHalfSecond() {
        DiamondPig pig = new DiamondPig(600, 400);
        pig.dash();
        checkTrue(pig.isDashing(), "触发冲刺后应处于加速状态");
        // 0.5 秒 = 约 32 帧；推进 40 帧（0.64s）后加速必须结束，
        // 且定时加速间隔 ≥2 秒，此期间不会被定时触发刷新
        for (int i = 0; i < 40; i++) {
            pig.updatePosition();
        }
        checkFalse(pig.isDashing(), "冲刺应在 0.5 秒后结束");
    }

    private void grabbedPigStopsMoving() {
        DiamondPig pig = new DiamondPig(600, 400);
        pig.setGrabbed(true);
        double x = pig.getX();
        for (int i = 0; i < 300; i++) {
            pig.updatePosition();
        }
        checkEq(x, pig.getX(), 1e-9, "被钩爪携带后钻石猪应停止自主移动");
    }
}
