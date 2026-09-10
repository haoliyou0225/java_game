// FR-06 物品接口：x/y 坐标、score 分值、weight 重量、grabbed 抓取状态，子类重写 onGrab/updatePosition
package Main.model;

public interface Item {
    void onGrab(Hook hook);
    void updatePosition();
    double getX();
    double getY();
    int getScore();
    double getWeight();
    boolean isGrabbed();
    void setX(double x);
    void setY(double y);
    void setGrabbed(boolean grabbed);
}
