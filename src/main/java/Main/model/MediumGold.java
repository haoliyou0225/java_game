// FR-10 中金块：120 金，收回耗时 2.5s（中档），体积介于小金块与大金块之间
package Main.model;

public class MediumGold extends ItemImpl {
    public MediumGold(double x, double y) {
        super(x, y, 120, 2.5);
    }

    @Override
    public void onGrab(Hook hook) {
        // 普通物品无特殊抓取行为
    }

    @Override
    public void updatePosition() {
        // 中金块静止在矿洞地图上
    }

    @Override
    public double getRadius() {
        return 21;
    }
}
