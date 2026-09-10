// FR-06 物品抽象基类：x/y 坐标、score 分值、weight 重量、grabbed 抓取状态，子类重写 onGrab/updatePosition
package com.dz.hello.Main.model;

public abstract class Item {
    protected double x;
    protected double y;
    protected int score;
    protected double weight;
    protected boolean grabbed;

    public Item(double x, double y, int score, double weight) {
        this.x = x;
        this.y = y;
        this.score = score;
        this.weight = weight;
        this.grabbed = false;
    }

    public abstract void onGrab(Hook hook);
    public abstract void updatePosition();

    public double getX() { return x; }
    public double getY() { return y; }
    public int getScore() { return score; }
    public double getWeight() { return weight; }
    public boolean isGrabbed() { return grabbed; }
    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }
    public void setGrabbed(boolean grabbed) { this.grabbed = grabbed; }
}
