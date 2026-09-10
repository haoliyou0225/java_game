// FR-09 石头：分数 10（最低）、重量 5.0（最重），收回速度最慢
package Main.model;

public class Stone extends ItemImpl {
    public Stone(double x, double y) { super(x, y, 10, 3.0); }
    @Override public void onGrab(Hook hook) {
        this.grabbed = true; // 原版：onGrab 设 grabbed=true
    }
    @Override public void updatePosition() {}
    /** 原版：石头又重又不值钱，回收仅计 1 金币 */
    @Override public int getSettlementGold() { return 1; }
}
