// FR-07 鼢鼠：分数 10、重量 1.0、会全图范围缓慢移动的活物
package Main.model;

import Main.config.Config;
import Main.config.GameConfig;

public class Mole extends ItemImpl {
    /** 水平移动速度（像素/秒），慢速全图跑 */
    private static final double MOVE_SPEED = 40;

    /** 原始 Y 坐标（鼹鼠不上下移动，只水平跑） */
    private final double originY;
    /** 移动方向：1 = 向右，-1 = 向左 */
    private int dir;
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
        this.minX = 50;
        this.maxX = Config.WIDTH - 50;
    }

    @Override
    public void onGrab(Hook hook) {
        // 被抓时停止移动（grabbed 字段由 setGrabbed 维护）
    }

    /** 每帧推进：未被抓取时水平匀速穿过全图，撞边反向 */
    @Override
    public void updatePosition() {
        if (isGrabbed()) {
            return;
        }
        x += dir * MOVE_SPEED * 0.016; // 假设 ~60fps

        // 撞边反向
        if (x < minX) {
            x = minX;
            dir = 1;
        } else if (x > maxX) {
            x = maxX;
            dir = -1;
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
}
