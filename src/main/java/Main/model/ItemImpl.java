// FR-06 物品抽象基类实现：x/y 坐标、score 分值、weight 重量、grabbed 抓取状态，子类重写 onGrab/updatePosition
package Main.model;

public abstract class ItemImpl implements Item {
    protected double x;
    protected double y;
    protected int score;
    protected double weight;
    protected boolean grabbed;

    public ItemImpl(double x, double y, int score, double weight) {
        this.x = x;
        this.y = y;
        this.score = score;
        this.weight = weight;
        this.grabbed = false;
    }

    @Override
    public abstract void onGrab(Hook hook);

    @Override
    public abstract void updatePosition();

    @Override public double getX() { return x; }
    @Override public double getY() { return y; }
    @Override public int getScore() { return score; }
    @Override public double getWeight() { return weight; }
    @Override public boolean isGrabbed() { return grabbed; }
    @Override public void setX(double x) { this.x = x; }
    @Override public void setY(double y) { this.y = y; }
    @Override public void setGrabbed(boolean grabbed) { this.grabbed = grabbed; }
}
