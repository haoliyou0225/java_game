// FR-11 钻石猪：生成时预携带 2~5 颗钻石，结算 = 颗数×600+10 金，收回耗时 3.0s（中档）
// 每 2~4 秒触发一次 0.5 秒加速（+50%）；钩爪碰撞仅约 40% 概率成功捕获，逃脱时立即冲刺
package Main.model;

import Main.config.Config;
import Main.config.GameConfig;

import java.util.concurrent.ThreadLocalRandom;

public class DiamondPig extends ItemImpl {

    /** 帧步长（秒），与 Mole 一致按 ~60fps 推进 */
    private static final double FRAME_STEP = 0.016;

    /** 携带钻石颗数（2~5，生成时预计算） */
    private final int diamonds;

    /** 移动方向：1 向右，-1 向左 */
    private int dir;
    /** 矿洞左右边界 */
    private final double minX;
    private final double maxX;
    /** 距下次定时加速的剩余秒数 */
    private double nextDashIn;
    /** 当前加速冲刺剩余秒数（>0 表示加速中） */
    private double dashTimer;

    public DiamondPig(double x, double y) {
        super(x, y, 0, 3.0);
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        this.diamonds = rnd.nextInt(
                GameConfig.PIG_DIAMOND_MIN, GameConfig.PIG_DIAMOND_MAX + 1);
        // FR-10：颗数×600 + 10 金（生成时预计算，计入地图公平性总价值）
        this.score = diamonds * GameConfig.PIG_DIAMOND_VALUE + GameConfig.PIG_BASE_GOLD;
        this.dir = rnd.nextBoolean() ? 1 : -1;
        this.minX = GameConfig.MINE_MIN_X;
        this.maxX = GameConfig.MINE_MAX_X;
        scheduleNextDash(rnd);
    }

    @Override
    public void onGrab(Hook hook) {
        // 是否能被抓住由 HookImpl 调用 tryCapture() 决定，此处不做处理
    }

    /**
     * 每帧推进（FR-11）：水平移动；每 2~4 秒触发一次 0.5 秒加速（速度×1.5）；
     * 撞矿洞边界自动反弹；被抓取后停止移动。
     */
    @Override
    public void updatePosition() {
        if (isGrabbed()) {
            return;
        }
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        nextDashIn -= FRAME_STEP;
        if (nextDashIn <= 0) {
            dashTimer = GameConfig.PIG_DASH_DURATION_SEC;
            scheduleNextDash(rnd);
        }
        double speed = GameConfig.PIG_MOVE_SPEED
                * (dashTimer > 0 ? GameConfig.PIG_DASH_MULTIPLIER : 1.0);
        if (dashTimer > 0) {
            dashTimer -= FRAME_STEP;
        }
        x += dir * speed * FRAME_STEP;
        if (x < minX) {
            x = minX;
            dir = 1;
        } else if (x > maxX) {
            x = maxX;
            dir = -1;
        }
    }

    /**
     * FR-11 捕获判定：钩爪碰到钻石猪时约 40% 概率成功捕获；
     * 失败时猪立即触发一次 0.5 秒加速冲刺逃离。
     *
     * @return true=本次被钩爪捕获；false=逃脱
     */
    public boolean tryCapture() {
        if (Math.random() < GameConfig.PIG_CAPTURE_RATE) {
            return true;
        }
        dash(); // 逃脱：立即加速
        return false;
    }

    /** 立即进入 0.5 秒加速冲刺（逃脱钩爪/定时触发共用） */
    public void dash() {
        dashTimer = GameConfig.PIG_DASH_DURATION_SEC;
    }

    /** 是否处于加速状态（渲染可用） */
    public boolean isDashing() {
        return dashTimer > 0;
    }

    /** 携带钻石颗数（2~5） */
    public int getDiamonds() {
        return diamonds;
    }

    @Override
    public double getRadius() {
        return 26;
    }

    /** 当前朝向：1 向右，-1 向左（渲染猪头/钻石图标朝向） */
    public int getFacing() {
        return dir;
    }

    /** 随机安排下一次定时加速（2~4 秒后） */
    private void scheduleNextDash(ThreadLocalRandom rnd) {
        nextDashIn = GameConfig.PIG_DASH_INTERVAL_MIN_SEC
                + rnd.nextDouble() * (GameConfig.PIG_DASH_INTERVAL_MAX_SEC
                - GameConfig.PIG_DASH_INTERVAL_MIN_SEC);
    }
}
