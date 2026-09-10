package com.dz.hello.Main.model.entity;

import com.dz.hello.controller.hook.IHook;

/**
 * 物品抽象父类：所有可被钩子抓取的物品统一继承本类。
 * 仅做数据模型，不引入任何 JavaFX 包，不做绘图。
 */
public abstract class Item {

    protected double x;
    protected double y;
    protected int scoreVal;
    protected double weight;
    protected boolean grabbed;
    protected boolean active = true;

    /** 被携带时跟随的目标点（钩爪当前位置），由控制层每帧通过 followHook 更新 */
    private double targetX;
    private double targetY;

    /**
     * 被钩子抓取时回调触发
     * @param hook 抓取本物品的钩子实例（由外部队友提供的 IHook 接口）
     */
    public abstract void onGrab(IHook hook);

    /**
     * 控制层每帧调用：告知物品钩爪当前位置，物品在 updatePosition 中向其移动
     */
    public void followHook(double hookX, double hookY) {
        this.targetX = hookX;
        this.targetY = hookY;
    }

    /**
     * 每一帧更新物品位置（多态运动）：
     * 被抓取后沿直线向钩爪位置移动，移动速度由 getFollowSpeed 决定，
     * 子类可重写 getFollowSpeed 实现各自的运动特性（越重越慢）。
     */
    public void updatePosition() {
        if (!grabbed) {
            return;
        }
        double dx = targetX - x;
        double dy = targetY - y;
        double dist = Math.sqrt(dx * dx + dy * dy);
        double step = getFollowSpeed();
        if (dist <= step) {
            // 本帧即可到达目标点
            x = targetX;
            y = targetY;
        } else {
            x += dx / dist * step;
            y += dy / dist * step;
        }
    }

    /**
     * 被钩爪携带时的跟随速度（像素/帧），默认按重量计算：越重越慢。
     * 子类可重写以实现特殊运动特性。
     */
    protected double getFollowSpeed() {
        return 8.0 / weight;
    }

    /**
     * 结算金币：物品被钩回起点后计入玩家账户的金币数。
     * 默认返回物品分值；特殊物品（如石头仅值1金币）由子类重写。
     */
    public int getSettlementGold() {
        return scoreVal;
    }

    /**
     * 物品结算回收后销毁：标记为失效，由控制层从画面和列表中移除
     */
    public void destroy() {
        this.active = false;
    }

    public boolean isActive() {
        return active;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public int getScoreVal() {
        return scoreVal;
    }

    public double getWeight() {
        return weight;
    }

    public boolean isGrabbed() {
        return grabbed;
    }
}
