// FR-13 鼹鼠：10 金、收回耗时 1.0s（轻档最快）；全程随机游走，范围限制在矿洞内部，边界自动反弹
package Main.model;

import Main.config.Config;
import Main.config.GameConfig;

public class Mole extends ItemImpl {
    /** 帧步长（秒），按 ~60fps 推进 */
    private static final double FRAME_STEP = 0.016;
    /** 水平移动速度（像素/秒），慢速全图跑 */
    private static final double MOVE_SPEED = 40;
    /** 随机变向间隔下限（秒） */
    private static final double TURN_INTERVAL_MIN = 0.8;
    /** 随机变向间隔上限（秒） */
    private static final double TURN_INTERVAL_MAX = 2.5;

    /** 原始 Y 坐标（鼹鼠不上下移动，只水平跑） */
    private final double originY;
    /** 移动方向：1 = 向右，-1 = 向左 */
    private int dir;
    /** 距下次随机变向剩余秒数 */
    private double turnIn;
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
        scheduleTurn();
    }

    @Override
    public void onGrab(Hook hook) {
        // 被抓时停止移动（grabbed 字段由 setGrabbed 维护）
    }

    /**
     * FR-13 每帧推进：未被抓取时在矿洞内随机游走——
     * 保持匀速水平移动，每隔 0.8~2.5 秒随机决定是否换向，撞边必然反弹；被抓取后不动。
     */
    @Override
    public void updatePosition() {
        if (isGrabbed()) {
            return;
        }
        x += dir * MOVE_SPEED * FRAME_STEP;

        // 撞边反向
        if (x < minX) {
            x = minX;
            dir = 1;
            scheduleTurn();
        } else if (x > maxX) {
            x = maxX;
            dir = -1;
            scheduleTurn();
        } else {
            // 随机游走：随机间隔有 50% 概率换向
            turnIn -= FRAME_STEP;
            if (turnIn <= 0) {
                if (Math.random() < 0.5) {
                    dir = -dir;
                }
                scheduleTurn();
            }
        }

        // Y 保持不变（鼹鼠不跳高）
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

    /** 随机安排下一次变向判定（0.8~2.5 秒后） */
    private void scheduleTurn() {
        turnIn = TURN_INTERVAL_MIN
                + Math.random() * (TURN_INTERVAL_MAX - TURN_INTERVAL_MIN);
    }
}
