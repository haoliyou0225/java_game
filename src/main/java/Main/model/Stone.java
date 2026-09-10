// FR-09 石头：分数 10（最低）、重量 5.0（最重），收回速度最慢
package Main.model;

public class Stone extends Item {
    public Stone(double x, double y) { super(x, y, 10, 5.0); }
    @Override public void onGrab(Hook hook) {}
    @Override public void updatePosition() {}
}
