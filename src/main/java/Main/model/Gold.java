// FR-07 金块：分数 50、重量 2.0，默认普通物品
package Main.model;

public class Gold extends ItemImpl {
    public Gold(double x, double y) { super(x, y, 50, 2.0); }
    @Override public void onGrab(Hook hook) {}
    @Override public void updatePosition() {}
}
