package Main.util;

/**
 * 碰撞检测工具类（final，不可继承；私有构造，不可实例化）
 */
public final class CollisionUtil {

    private CollisionUtil() {
    }

    /**
     * 圆形碰撞检测：两圆心距离 <= 半径之和即视为碰撞
     * @param x1 圆1圆心横坐标
     * @param y1 圆1圆心纵坐标
     * @param r1 圆1半径
     * @param x2 圆2圆心横坐标
     * @param y2 圆2圆心纵坐标
     * @param r2 圆2半径
     * @return true 碰撞；false 未碰撞
     */
    public static boolean circleCollision(double x1, double y1, double r1,
                                          double x2, double y2, double r2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        double distance = Math.sqrt(dx * dx + dy * dy);
        return distance <= r1 + r2;
    }
}
