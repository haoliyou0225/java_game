// FR-13 鼹鼠：10 金、收回耗时 1.0s（轻档最快）；全程匍匐爬行（爬动-停顿交替），
// 范围限制在矿洞内部，边界自动反弹
package Main.model;

import Main.config.Config;
import Main.config.GameConfig;

public class Mole extends ItemImpl {
    /** 帧步长（秒），按 ~60fps 推进 */
    private static final double FRAME_STEP = 0.016;
    /** 爬行速度（像素/秒），短促快速爬动 */
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

    public Mole(double x, double y) {
        super(x, y, GameConfig.MOLE_CAPTURE_GOLD, 1.0);
        this.originY = y;
        // 初始方向随机
        this.dir = Math.random() < 0.5 ? 1 : -1;
        // 矿洞边界（与 GameViewImpl / HookImpl 对齐）
        this.minX = GameConfig.MINE_MIN_X;
        this.maxX = Config.WIDTH - 50;
        scheduleState();
    }

    @Override
    public void onGrab(Hook hook) {
        // 被抓时停止移动（grabbed 字段由 setGrabbed 维护）
    }

    /**
     * FR-13 每帧推进：未被抓取时在矿洞内匍匐爬行——
     * 爬行段快速移动并摆动四肢，停顿段原地不动；每段爬行开始有概率换向，撞边必然反弹。
     */
    @Override
    public void updatePosition() {
        if (isGrabbed()) {
            return;
        }

        if (state == CrawlState.CRAWL) {
            x += dir * CRAWL_SPEED * FRAME_STEP;
            legPhase += LEG_ANGULAR_SPEED * FRAME_STEP;

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
        stateIn -= FRAME_STEP;
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
    }

    @Override
    public double getRadius() {
        return 24; // 横着的鼹鼠碰撞半径更大
    }

    /** 当前朝向：1 = 向右，-1 = 向左（渲染时用来翻转头部方向） */
    public int getFacing() {
        return dir;
    }

    /** 是否正在爬动（false = 停顿中），视图据此决定是否播放爬行动画 */
    public boolean isCrawling() {
        return state == CrawlState.CRAWL;
    }

    /** 腿部摆动相位（弧度）：前后腿反相摆动，停顿时冻结 */
    public double getLegPhase() {
        return legPhase;
    }

    /** 安排当前状态的持续时间 */
    private void scheduleState() {
        stateIn = CRAWL_MIN + Math.random() * (CRAWL_MAX - CRAWL_MIN);
    }
}
