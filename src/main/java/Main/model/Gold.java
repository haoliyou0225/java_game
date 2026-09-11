// FR-10 小金块：25 金，收回耗时 1.2s（轻档）
package Main.model;

public class Gold extends ItemImpl {
    public Gold(double x, double y) { super(x, y, 25, 1.2); }
    @Override public void onGrab(Hook hook) {}
    @Override public void updatePosition() {}
}
