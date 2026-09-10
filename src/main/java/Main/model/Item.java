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
     * 默认返回 getScore()，特殊物品（如 Stone）重写返回 1。
     */
    default int getSettlementGold() { return getScore(); }
}
