// FR-10 大金块：400 金，收回耗时 5.0s（重档最慢），体积最大
package Main.model;

public class BigGold extends ItemImpl {
    public BigGold(double x, double y) {
        super(x, y, 400, 5.0);
    }

    @Override
    public void onGrab(Hook hook) {
        // 普通物品无特殊抓取行为
    }

    @Override
    public void updatePosition() {
        // 大金块静止在矿洞地图上
    }

    /** 大金块半径最大，视觉与碰撞范围最显著 */
    @Override
    public double getRadius() {
        return 28;
    }
}
