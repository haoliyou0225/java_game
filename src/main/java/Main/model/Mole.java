// FR-07 鼢鼠：分数 10、重量 1.0、会左右移动的活物（贴近原版）
package Main.model;

import Main.config.GameConfig;

public class Mole extends ItemImpl {
<<<<<<< Updated upstream
    /** 移动相位 */
    private double phase;
    /** 原始 X 坐标（移动中心） */
    private final double originX;
    /** 振幅 */
    private final double amplitude;
    /** 角速度（弧度/秒） */
    private final double speed;
=======
    /** 爬行速度（像素/秒），短促快速爬行 */
    private static final double CRAWL_SPEED = 75;
    /** 一次爬行持续时间下限（秒） */
    private static final double CRAWL_MIN = 0.35;
    /** 一次爬行持续时间上限（秒） */
    private static final double CRAWL_MAX = 0.75;
    /** 一次停顿持续时间下限（秒） */
    private static final double PAUSE_MIN = 0.15;
    /** 一次停顿持续时间上限（秒） */
    private static final double PAUSE_MAX = 0.45;
    /** 爬行开始时换向的概率 */
    private static final double TURN_CHANCE = 0.35;
    /**
     * 腿部摆动角速度（弧度/秒）。与 CRAWL_SPEED 匹配：
     * 半周期位移 = 75 × π/14 ≈ 16.8px，视图里脚摆幅 2×0.35r(r=24) = 16.8px，
     * 保证撑地阶段脚相对地面不打滑。
     */
    private static final double LEG_ANGULAR_SPEED = 14;

    /** 爬行状态：爬动 / 停顿 */
    private enum CrawlState { CRAWL, PAUSE }

    /** 原始 Y 坐标（鼹鼠不上下移动，只水平爬） */
    private final double originY;
    /** 移动方向：1 = 向右，-1 = 向左 */
    private int dir;
    /** 当前爬行状态 */
    private CrawlState state = CrawlState.CRAWL;
    /** 当前状态剩余秒数 */
    private double stateIn;
    /** 腿部摆动相位（弧度），仅爬动时累积，停顿时冻结 */
    private double legPhase;
    /** 矿洞左边界 */
    private final double minX;
    /** 矿洞右边界 */
    private final double maxX;
>>>>>>> Stashed changes

    public Mole(double x, double y) {
        super(x, y, GameConfig.MOLE_CAPTURE_GOLD, 1.0);
        this.originX = x;
        this.phase = Math.random() * Math.PI * 2;
        this.amplitude = 30 + Math.random() * 50;
        this.speed = 1.0 + Math.random() * 1.5;
    }

    @Override
    public void onGrab(Hook hook) {
        // 被抓时停止移动（grabbed 字段由 setGrabbed 维护）
    }

    /** 每帧推进：未被抓取时左右振荡移动；被抓取后不再移动 */
    @Override
    public void updatePosition(double deltaTime) {
        if (isGrabbed()) {
            return;
        }
<<<<<<< Updated upstream
        // 假设 ~60fps，每帧推进 speed * 0.016 弧度
        phase += speed * 0.016;
        x = originX + Math.sin(phase) * amplitude;
=======

        if (state == CrawlState.CRAWL) {
            x += dir * CRAWL_SPEED * deltaTime;
            legPhase += LEG_ANGULAR_SPEED * deltaTime;

            // 撞边必然反弹并立即继续爬
            if (x < minX) {
                x = minX;
                dir = 1;
                stateIn = CRAWL_MIN + Math.random() * (CRAWL_MAX - CRAWL_MIN);
            } else if (x > maxX) {
                x = maxX;
                dir = -1;
                stateIn = CRAWL_MIN + Math.random() * (CRAWL_MAX - CRAWL_MIN);
            }
        }

        // 爬行/停顿状态切换
        stateIn -= deltaTime;
        if (stateIn <= 0) {
            if (state == CrawlState.CRAWL) {
                // 爬完一段 -> 短暂停顿；相位吸附到步伐中立点（双脚居中、伏身），避免停顿瞬间腿突变
                state = CrawlState.PAUSE;
                legPhase = Math.round(legPhase / Math.PI) * Math.PI;
                stateIn = PAUSE_MIN + Math.random() * (PAUSE_MAX - PAUSE_MIN);
            } else {
                // 停顿结束 -> 重新开爬，有概率换向
                state = CrawlState.CRAWL;
                if (Math.random() < TURN_CHANCE) {
                    dir = -dir;
                }
                stateIn = CRAWL_MIN + Math.random() * (CRAWL_MAX - CRAWL_MIN);
            }
        }

        // Y 保持不变（身体起伏仅为渲染表现）
        y = originY;
>>>>>>> Stashed changes
    }

    @Override
    public double getRadius() {
        return 16;
    }
}