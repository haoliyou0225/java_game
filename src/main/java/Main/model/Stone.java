// FR-10 石头：基础 11 金，收回耗时 4.0s（重档）；FR-17 石头收藏书激活时结算 ×3
package Main.model;

public class Stone extends ItemImpl {
    public Stone(double x, double y) { super(x, y, 11, 4.0); }

    @Override
    public void onGrab(Hook hook) {
        this.grabbed = true; // 原版：onGrab 设 grabbed=true
    }

    @Override
    public void updatePosition() {}
}
