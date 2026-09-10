// FR-07 鼢鼠：分数 10、重量 1.0、会左右移动的活物（贴近原版）
package Main.model;

import Main.config.GameConfig;

public class Mole extends ItemImpl {
    /** 移动相位 */
    private double phase;
    /** 原始 X 坐标（移动中心） */
    private final double originX;
    /** 振幅 */
    private final double amplitude;
    /** 角速度（弧度/秒） */
    private final double speed;

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
    public void updatePosition() {
        if (isGrabbed()) {
            return;
        }
        // 假设 ~60fps，每帧推进 speed * 0.016 弧度
        phase += speed * 0.016;
        x = originX + Math.sin(phase) * amplitude;
    }

    @Override
    public double getRadius() {
        return 16;
    }
}