// FR-08 钻石：分数 100（最高）、重量 1.0（最轻），收回速度最快
package Main.model;

public class Diamond extends ItemImpl {
    public Diamond(double x, double y) { super(x, y, 100, 1.0); }
    @Override public void onGrab(Hook hook) {}
    @Override public void updatePosition() {}
}
