// FR-07 大金块：分数 500（高）、重量 4.0（最重之一），收回速度极慢
package Main.model;

public class BigGold extends ItemImpl {
    public BigGold(double x, double y) {
        super(x, y, 500, 4.0);
    }

    @Override
    public void onGrab(Hook hook) {
        // 普通物品无特殊抓取行为
    }

    @Override
    public void updatePosition() {
        // 大金块静止在矿洞地图上
    }

    /** 大金块半径更大，视觉与碰撞范围更显著 */
    @Override
    public double getRadius() {
        return 28;
    }
}