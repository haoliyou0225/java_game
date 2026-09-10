// FR-10 钻石：600 金，收回耗时 2.0s（轻档与中档之间，按数值表逐物品指定）
package Main.model;

public class Diamond extends ItemImpl {
    public Diamond(double x, double y) { super(x, y, 600, 2.0); }
    @Override public void onGrab(Hook hook) {}
    @Override public void updatePosition() {}
}
