// FR-UI CollisionUtils：圆碰撞/点圆碰撞/边界检测静态工具，来自 feature_ui 分支（与 hook 的 CollisionUtil 共存）
package Main.util;

/**
 * 碰撞与边界检测工具类（供钩爪物理、抓取结算使用）。
 * 全部为纯静态方法，无状态、无 JavaFX 依赖。
 */
public final class CollisionUtils {

    private CollisionUtils() {
        // 工具类禁止实例化
    }

    /**
     * 圆与圆碰撞检测（钩爪头 vs 物品）。
     * 采用平方距离比较，避免开方运算开销。
     *
     * @param x1 圆1圆心 X
     * @param y1 圆1圆心 Y
     * @param r1 圆1半径（>0）
     * @param x2 圆2圆心 X
     * @param y2 圆2圆心 Y
     * @param r2 圆2半径（>0）
     * @return true=两圆相交或相切（圆心距 ≤ r1+r2）
     */
    public static boolean circlesCollide(double x1, double y1, double r1,
                                         double x2, double y2, double r2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double minDist = r1 + r2;
        return dx * dx + dy * dy <= minDist * minDist;
    }

    /**
     * 点与圆碰撞检测（绳索/钩爪端点是否进入物品范围）。
     *
     * @param px 点 X
     * @param py 点 Y
     * @param cx 圆心 X
     * @param cy 圆心 Y
     * @param r  圆半径（>0）
     * @return true=点在圆内或圆上（距离 ≤ r）
     */
    public static boolean pointInCircle(double px, double py,
                                        double cx, double cy, double r) {
        double dx = px - cx;
        double dy = py - cy;
        return dx * dx + dy * dy <= r * r;
    }

    /**
     * 边界检测：目标坐标是否超出给定矩形区域（矿洞边界）。
     *
     * @param x      待检测点 X
     * @param y      待检测点 Y
     * @param left   区域左边界
     * @param top    区域上边界
     * @param right  区域右边界
     * @param bottom 区域下边界
     * @return true=点位于区域之外（含边界线之外），false=在区域内
     */
    public static boolean isOutOfBounds(double x, double y,
                                        double left, double top,
                                        double right, double bottom) {
        return x < left || x > right || y < top || y > bottom;
    }
}
