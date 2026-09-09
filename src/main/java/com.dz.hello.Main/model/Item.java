// FR-06 物品抽象基类：x/y 坐标、score 分值、weight 重量、grabbed 抓取状态，子类重写 onGrab/updatePosition
package com.dz.hello.main.model;

public abstract class Item {
    protected double x;
    protected double y;
    protected int score;
    protected double weight;
    protected boolean grabbed;
    public Item(double x, double y, int score, double weight) {}
    public abstract void onGrab(Hook hook);
    public abstract void updatePosition();
    public double getX() { return 0; }
    public double getY() { return 0; }
    public int getScore() { return 0; }
    public double getWeight() { return 0; }
    public boolean isGrabbed() { return false; }
    public void setX(double x) {}
    public void setY(double y) {}
    public void setGrabbed(boolean grabbed) {}
}
