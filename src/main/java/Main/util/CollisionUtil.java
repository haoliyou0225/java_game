// FR-17 碰撞检测工具：circleCollision(x1,y1,r1,x2,y2,r2) 圆形碰撞，静态方法，被 Hook.checkCollision* 调用
package Main.util;

/**
 * 碰撞检测工具（严格对齐 UML: <<static>>）
 */
public final class CollisionUtil {

    private CollisionUtil() {}

    /**
     * 圆形碰撞检测
     * @return 两圆是否相交
     */
    public static boolean circleCollision(double x1, double y1, double r1,
                                          double x2, double y2, double r2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        double dist = Math.hypot(dx, dy);
        return dist <= r1 + r2;
    }
}
