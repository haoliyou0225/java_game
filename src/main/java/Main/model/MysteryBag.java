// FR-07 福袋：无金币价值、重量 1.5，抓取后仅开出道具奖励（炸药/药水/冰冻箱/持续道具）
package Main.model;

public class MysteryBag extends ItemImpl {
    public MysteryBag(double x, double y) {
        // 福袋不计金币：基础价值 0，实际收益由 GameManager 加权抽取道具入库存
        super(x, y, 0, 1.5);
    }

    @Override
    public void onGrab(Hook hook) {
        // 福袋抓取后由 GameManager 结算：仅发放道具奖励，不发放金币
    }

    @Override
    public void updatePosition(double deltaTime) {
        // 福袋静止
    }

    @Override
    public double getRadius() {
        return 18;
    }
}
