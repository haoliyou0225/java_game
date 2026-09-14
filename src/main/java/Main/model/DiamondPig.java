// FR-11 钻石猪：生成时预携带 2~5 颗钻石，结算 = 颗数×600+10 金，收回耗时 3.0s（中档）
// 四足匍匐爬行：爬动-停顿交替，爬动时水平移动并累积步态相位，停顿段原地伏身；
// 每 2~4 秒触发一次 0.5 秒加速（+50%，步频同比加快）；钩爪碰撞仅约 40% 概率成功捕获，逃脱时立即冲刺
package Main.model;

import Main.config.GameConfig;

import java.util.concurrent.ThreadLocalRandom;

public class DiamondPig extends ItemImpl {

    /** 帧步长（秒），与 Mole 一致按 ~60fps 推进 */
    private static final double FRAME_STEP = 0.016;

    /** 一次爬行持续时间下限（秒），猪比鼹鼠节奏更稳重 */
    private static final double CRAWL_MIN = 0.55;
    /** 一次爬行持续时间上限（秒） */
    private static final double CRAWL_MAX = 1.10;
    /** 一次停顿持续时间下限（秒） */
    private static final double PAUSE_MIN = 0.20;
    /** 一次停顿持续时间上限（秒） */
    private static final double PAUSE_MAX = 0.55;
    /** 爬行开始时换向的概率 */
    private static final double TURN_CHANCE = 0.30;
    /**
     * 单步步幅（像素）：每前进 STRIDE 像素相位走 π（一次身体起伏）。
     * 步态相位与真实位移严格匹配——冲刺提速时步频同比加快，不会打滑/倒步。
     */
    private static final double STRIDE = 15.0;
    /** 爬行姿态混合系数逼近速度（1/秒），仅渲染平滑用，不参与世界坐标 */
    private static final double BLEND_SPEED = 12.0;

    /** 爬行状态：爬动 / 停顿 */
    private enum CrawlState { CRAWL, PAUSE }

    /** 携带钻石颗数（2~5，生成时预计算） */
    private final int diamonds;

    /** 移动方向：1 向右，-1 向左 */
    private int dir;
    /** 矿洞左右边界 */
    private final double minX;
    private final double maxX;
    /** 当前爬行状态 */
    private CrawlState state = CrawlState.CRAWL;
    /** 当前状态剩余秒数 */
    private double stateIn;
    /** 步态相位（弧度）：仅爬动时随位移累积，停顿时冻结在中立点 */
    private double legPhase;
    /**
     * 爬行姿态混合量（0=伏身停顿，1=全力爬动），渲染层专用：
     * 状态切换时指数平滑，避免身体起伏/蹲伏突变。绝不参与 x/y 积分。
     */
    private double crawlBlend;
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
        startCrawl(rnd, false);
    }

    @Override
    public void onGrab(Hook hook) {
        // 是否能被抓住由 HookImpl 调用 tryCapture() 决定，此处不做处理
    }

    /**
     * 每帧推进（FR-11）：四足匍匐爬行——
     * 爬行段移动并累积步态相位，停顿段原地伏身；每段爬行开始有概率换向，撞边必然反弹；
     * 每 2~4 秒触发一次 0.5 秒加速（速度×1.5，步频同比加快）；被抓取后停止移动。
     */
    @Override
    public void updatePosition() {
        if (isGrabbed()) {
            // 被钩走：姿态平滑回落到伏身静止（不影响世界坐标，钩子负责位移）
            approachBlend(0.0);
            return;
        }
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        // 定时冲刺计时；触发时若正在停顿则立即开爬（突然加速逃跑）
        nextDashIn -= FRAME_STEP;
        if (nextDashIn <= 0) {
            dashTimer = GameConfig.PIG_DASH_DURATION_SEC;
            scheduleNextDash(rnd);
            if (state == CrawlState.PAUSE) {
                startCrawl(rnd, false);
            }
        }
        double speed = GameConfig.PIG_MOVE_SPEED
                * (dashTimer > 0 ? GameConfig.PIG_DASH_MULTIPLIER : 1.0);
        if (dashTimer > 0) {
            dashTimer -= FRAME_STEP;
        }

        // ===== 根运动：世界坐标只在爬行段按速度积分 =====
        boolean moving = state == CrawlState.CRAWL;
        if (moving) {
            x += dir * speed * FRAME_STEP;
            // 步态相位与位移匹配：每前进一个步幅相位走 π
            legPhase += Math.PI * speed / STRIDE * FRAME_STEP;

            // 撞边必然反弹并续上一段爬行
            if (x < minX) {
                x = minX;
                dir = 1;
                startCrawl(rnd, false);
            } else if (x > maxX) {
                x = maxX;
                dir = -1;
                startCrawl(rnd, false);
            }
        }

        // ===== 爬行/停顿状态切换（不改变坐标） =====
        stateIn -= FRAME_STEP;
        if (stateIn <= 0) {
            if (moving) {
                if (dashTimer > 0) {
                    // 冲刺尚未结束：不允许趴下，续一段爬行直到加速结束
                    stateIn = CRAWL_MIN;
                } else {
                    // 爬完一段 -> 短暂伏身停顿；相位吸附到步伐中立点（起伏为 0、前倾为 0）
                    state = CrawlState.PAUSE;
                    legPhase = Math.round(legPhase / Math.PI) * Math.PI;
                    stateIn = PAUSE_MIN + rnd.nextDouble() * (PAUSE_MAX - PAUSE_MIN);
                }
            } else {
                // 停顿结束 -> 重新开爬，有概率换向
                startCrawl(rnd, true);
            }
        }

        // ===== 渲染姿态混合量平滑（纯表现层） =====
        approachBlend(moving ? 1.0 : 0.0);
    }

    /**
     * FR-11 捕获判定：钩爪碰到钻石猪时约 40% 概率成功捕获；
     * 失败时猪立即触发一次 0.5 秒加速冲刺逃离（停顿中也会立刻惊起开爬并反向逃窜）。
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

    /** 立即进入 0.5 秒加速冲刺（逃脱钩爪/定时触发共用）；停顿中会立即惊起反向逃窜 */
    public void dash() {
        dashTimer = GameConfig.PIG_DASH_DURATION_SEC;
        if (state == CrawlState.PAUSE) {
            startCrawl(ThreadLocalRandom.current(), true);
        }
    }

    /** 是否处于加速状态（渲染可用） */
    public boolean isDashing() {
        return dashTimer > 0;
    }

    /** 是否正在爬动（false = 伏身停顿中），视图据此播放爬行动画 */
    public boolean isCrawling() {
        return state == CrawlState.CRAWL;
    }

    /** 步态相位（弧度）：驱动身体起伏与前后倾，停顿时冻结在中立点 */
    public double getLegPhase() {
        return legPhase;
    }

    /** 爬行姿态混合量（0=伏身停顿，1=爬动中），已做帧间平滑，视图直接使用 */
    public double getCrawlBlend() {
        return crawlBlend;
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

    /** 进入一段爬行；mayTurn=true 时按概率换向（停顿后出发/逃脱用，撞边反弹不换向） */
    private void startCrawl(ThreadLocalRandom rnd, boolean mayTurn) {
        state = CrawlState.CRAWL;
        if (mayTurn && rnd.nextDouble() < TURN_CHANCE) {
            dir = -dir;
        }
        stateIn = CRAWL_MIN + rnd.nextDouble() * (CRAWL_MAX - CRAWL_MIN);
    }

    /** 随机安排下一次定时加速（2~4 秒后） */
    private void scheduleNextDash(ThreadLocalRandom rnd) {
        nextDashIn = GameConfig.PIG_DASH_INTERVAL_MIN_SEC
                + rnd.nextDouble() * (GameConfig.PIG_DASH_INTERVAL_MAX_SEC
                - GameConfig.PIG_DASH_INTERVAL_MIN_SEC);
    }

    /** crawlBlend 指数逼近目标值（纯渲染平滑） */
    private void approachBlend(double target) {
        double k = Math.min(1.0, BLEND_SPEED * FRAME_STEP);
        crawlBlend += (target - crawlBlend) * k;
        if (Math.abs(target - crawlBlend) < 0.01) {
            crawlBlend = target;
        }
    }
}
