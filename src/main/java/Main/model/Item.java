// FR-06 物品接口：x/y 坐标、score 分值、weight 重量、grabbed 抓取状态，子类重写 onGrab/updatePosition
package Main.model;

public interface Item {
    void onGrab(Hook hook);
    void updatePosition();
    double getX();
    double getY();
    int getScore();
    double getWeight();
    /** 物品碰撞/渲染半径 */
    double getRadius();
    boolean isGrabbed();
    void setX(double x);
    void setY(double y);
    void setGrabbed(boolean grabbed);
    /**
     * 结算金币（原版 feature/item 分支方法）。
     * 默认返回 getScore() 基础价值；FR-17 的石头×3/钻石×2/幸运草×1.5
     * 统一在 GameManager 结算链中套算，物品类自身不再改写本方法。
     */
    default int getSettlementGold() { return getScore(); }

    /**
     * FR-10 携带收回耗时（秒）：钩爪抓住该物品后完成全程收回的固定耗时，
     * 收回速度 = 抓取瞬间绳长 / 该耗时。数值表逐品类指定
     * （1.0/1.2/1.5/2.0/2.5/3.0/4.0/5.0），各物品构造时 weight 即按该表赋值，
     * 故默认返回 weight；特殊物品可重写。
     */
    default double getRetractDuration() { return getWeight(); }
}
