// 折绳验收自检：鼹鼠碰撞使钩爪偏转时绳子在碰撞点打折（锚点→折点→钩尖折线），
// 抛出段保留原方向；收回（空钩/携带物品）严格沿折线原路折返，折点按逆序消失，
// 最终绳子恢复为沿原抛出方向的直绳。
// 无 JUnit 依赖，直接 java Main.model.HookRopeBendCheck 运行。
package Main.model;

import Main.config.GameConfig;
import Main.util.SelfCheck;

import java.util.ArrayList;
import java.util.List;

public class HookRopeBendCheck extends SelfCheck {

    private static final double ANCHOR_X = GameConfig.HOOK_ANCHOR_X_P1;
    private static final double ANCHOR_Y = GameConfig.HOOK_ANCHOR_Y;
    private static final double DT = 1.0 / 240;

    private HookImpl hook;

    public static void main(String[] args) {
        HookRopeBendCheck check = new HookRopeBendCheck();
        check.straightFlightKeepsRopeStraight();
        check.moleCollisionBendsRopeAtTipAndDeflectsOuterSegment();
        check.emptyRetractFollowsBentPathBackAndStraightensRope();
        check.multipleBendsAreConsumedInReverseOrder();
        check.carryingItemRetracesBentPathBackToAnchor();
        check.finish();
    }

    private void resetHook() {
        hook = new HookImpl(1, new RopeImpl(GameConfig.ROPE_MAX_EXTEND_LENGTH));
    }

    private void straightFlightKeepsRopeStraight() {
        resetHook();
        hook.throwHook();
        hook.update(0.2, List.of(), null);
        checkTrue(hook.getBendPoints().isEmpty(), "未碰撞鼹鼠时绳子应为直绳（无折点）");
        checkEq(ANCHOR_X, hook.getX(), 1e-9, "垂直下抛钩尖 X 应与锚点一致");
        checkEq(ANCHOR_Y + 70 + 100, hook.getY(), 1e-9, "0.2 秒应下飞 100px");
    }

    private void moleCollisionBendsRopeAtTipAndDeflectsOuterSegment() {
        resetHook();
        hook.throwHook();
        hook.update(0.2, List.of(), null);
        // 钩尖此刻约在 (320,250)
        Mole mole = new Mole(hook.getX(), hook.getY());

        hook.update(0.001, List.of(mole), null);

        List<double[]> bends = hook.getBendPoints();
        checkEq(1, bends.size(), "鼹鼠碰撞偏转应产生 1 个折点");
        double[] bend = bends.get(0);
        checkEq(ANCHOR_X, bend[0], 1e-6, "折点 X 应在原直绳轨迹上");
        checkEq(ANCHOR_Y + 170.5, bend[1], 1.0, "折点应位于碰撞瞬间的钩尖处");

        // 折点之后的外段发生 15°~30° 偏转；锚点→折点段保持垂直向下
        double delta = Math.abs(hook.getAngle() - Math.PI / 2);
        checkTrue(delta >= Math.toRadians(14.9) && delta <= Math.toRadians(30.1),
                "外段偏转角应在 15°~30°，实际 " + Math.toDegrees(delta) + "°");
        checkEq(HookState.THROWING, hook.getState(), "碰撞鼹鼠不被抓住，应继续 THROWING");

        // 再飞 0.1 秒：钩尖应从折点沿偏转方向离开，X 不再等于锚点 X
        hook.update(0.1, List.of(mole), null);
        double distFromBend = Math.hypot(hook.getX() - bend[0], hook.getY() - bend[1]);
        checkEq(50.0, distFromBend, 1.0, "折点后外段 0.1 秒应飞行 50px");
        checkTrue(Math.abs(hook.getX() - bend[0]) > 5,
                "偏转后外段钩尖应离开原垂直轨迹");
    }

    private void emptyRetractFollowsBentPathBackAndStraightensRope() {
        resetHook();
        BendSetup setup = deflectOnce();

        // 折返前折线：锚点 → 折点 → 当前钩尖
        double[][] polyline = polyline(setup.bend);

        hook.retractHook();
        double minDistToBend = Double.MAX_VALUE;
        int frames = 0;
        while (hook.getState() != HookState.SWINGING && frames < 500) {
            hook.update(DT, List.of(setup.mole), null);
            double d = Math.hypot(hook.getX() - setup.bend[0], hook.getY() - setup.bend[1]);
            minDistToBend = Math.min(minDistToBend, d);
            // 钩尖必须始终落在原折线（锚点→折点→折返起点）上
            double onPath = distToPolyline(hook.getX(), hook.getY(), polyline);
            checkTrue(onPath < 1e-6, "收回钩尖偏离原折线路径 " + onPath);
            frames++;
        }

        checkEq(HookState.SWINGING, hook.getState(), "应收回到起点恢复钟摆");
        checkTrue(hook.getBendPoints().isEmpty(), "完全收回后折点应全部消失、绳子拉直");
        checkEq(70.0, hook.getRopeLength(), 1e-9, "收回后绳长应恢复为钟摆长度 70");
        checkEq(Math.PI / 2, hook.getAngle(), 1e-9,
                "拉直后方向应恢复为锚点→折点的原抛出方向（垂直向下）");
        checkTrue(minDistToBend < 6.0,
                "空钩收回必须途经折点（原路折返），最近距离 " + minDistToBend);
    }

    private void multipleBendsAreConsumedInReverseOrder() {
        resetHook();
        hook.throwHook();
        hook.update(0.2, List.of(), null);
        Mole mole1 = new Mole(hook.getX(), hook.getY());
        hook.update(0.001, List.of(mole1), null);
        double[] bend1 = hook.getBendPoints().get(0).clone();

        // 沿偏转方向飞 0.15 秒后让第二只鼹鼠再次碰撞
        hook.update(0.15, List.of(mole1), null);
        Mole mole2 = new Mole(hook.getX(), hook.getY());
        hook.update(0.001, List.of(mole2, mole1), null);
        checkEq(2, hook.getBendPoints().size(), "两次鼹鼠碰撞应产生 2 个折点");
        double[] bend2 = hook.getBendPoints().get(1).clone();

        double[][] polyline = polyline(bend1, bend2);

        hook.retractHook();
        int hitOuterFrame = -1;
        int hitInnerFrame = -1;
        int frames = 0;
        while (hook.getState() != HookState.SWINGING && frames < 800) {
            hook.update(DT, List.of(mole2, mole1), null);
            checkTrue(distToPolyline(hook.getX(), hook.getY(), polyline) < 1e-6,
                    "收回钩尖偏离原折线路径");
            if (hitOuterFrame < 0
                    && Math.hypot(hook.getX() - bend2[0], hook.getY() - bend2[1]) < 6) {
                hitOuterFrame = frames;
            }
            if (hitInnerFrame < 0
                    && Math.hypot(hook.getX() - bend1[0], hook.getY() - bend1[1]) < 6) {
                hitInnerFrame = frames;
            }
            frames++;
        }

        checkTrue(hitOuterFrame >= 0, "应收先途经外侧折点₂");
        checkTrue(hitInnerFrame >= 0, "应后途经内侧折点₁");
        checkTrue(hitOuterFrame < hitInnerFrame,
                "折点必须按逆序消失：先过折点₂再过折点₁");
        checkTrue(hook.getBendPoints().isEmpty(), "完全收回后不应残留折点");
        checkEq(Math.PI / 2, hook.getAngle(), 1e-9, "拉直后方向应恢复原抛出方向");
    }

    private void carryingItemRetracesBentPathBackToAnchor() {
        resetHook();
        BendSetup setup = deflectOnce();

        // 在偏转后的钩尖前方 20px 放小金块，下一帧即被抓住进入 GRABBING
        Gold gold = new Gold(
                hook.getX() + Math.cos(hook.getAngle()) * 20,
                hook.getY() + Math.sin(hook.getAngle()) * 20);
        hook.update(0.001, List.of(gold, setup.mole), null);
        checkEq(HookState.GRABBING, hook.getState(), "钩爪应抓住金块进入携带收回");
        double[][] polyline = polyline(setup.bend);

        int frames = 0;
        while (hook.getState() != HookState.SWINGING && frames < 800) {
            hook.update(DT, List.of(gold, setup.mole), null);
            // 携带的金块必须贴在钩尖上，一同沿折线原路返回
            checkEq(hook.getX(), gold.getX(), 1e-9, "携带物品 X 应跟随钩尖");
            checkEq(hook.getY(), gold.getY(), 1e-9, "携带物品 Y 应跟随钩尖");
            checkTrue(distToPolyline(hook.getX(), hook.getY(), polyline) < 1e-6,
                    "携带收回必须沿原折线路径");
            frames++;
        }

        checkEq(HookState.SWINGING, hook.getState(), "携带物品应收回到起点");
        checkTrue(hook.getBendPoints().isEmpty(), "携带收回后折点应全部消失");
        checkEq(Math.PI / 2, hook.getAngle(), 1e-9, "拉直后方向应恢复原抛出方向");
    }

    // ===== 辅助 =====

    private BendSetup deflectOnce() {
        hook.throwHook();
        hook.update(0.2, List.of(), null);
        Mole mole = new Mole(hook.getX(), hook.getY());
        hook.update(0.001, List.of(mole), null);
        BendSetup s = new BendSetup();
        s.mole = mole;
        s.bend = hook.getBendPoints().get(0).clone();
        return s;
    }

    private static class BendSetup {
        Mole mole;
        double[] bend;
    }

    /** 组装折返折线：锚点 → 折点们 → 折返起始钩尖 */
    private double[][] polyline(double[]... bends) {
        List<double[]> pts = new ArrayList<>();
        pts.add(new double[]{ANCHOR_X, ANCHOR_Y});
        for (double[] b : bends) {
            pts.add(b.clone());
        }
        pts.add(new double[]{hook.getX(), hook.getY()});
        return pts.toArray(new double[0][]);
    }

    private static double distToPolyline(double px, double py, double[][] poly) {
        double min = Double.MAX_VALUE;
        for (int i = 0; i + 1 < poly.length; i++) {
            min = Math.min(min, distToSegment(px, py, poly[i], poly[i + 1]));
        }
        return min;
    }

    private static double distToSegment(double px, double py, double[] a, double[] b) {
        double dx = b[0] - a[0];
        double dy = b[1] - a[1];
        double lenSq = dx * dx + dy * dy;
        double t = lenSq == 0 ? 0
                : Math.max(0, Math.min(1, ((px - a[0]) * dx + (py - a[1]) * dy) / lenSq));
        double cx = a[0] + t * dx;
        double cy = a[1] + t * dy;
        return Math.hypot(px - cx, py - cy);
    }
}
