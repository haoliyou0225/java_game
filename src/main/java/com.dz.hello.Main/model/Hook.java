// FR-11 钩子核心：钟摆 updateSwing / 抛出 throwHook / 收回 retractHook / 与物品碰撞 checkCollisionItem / 与对钩碰撞 checkCollisionOtherHook
package com.dz.hello.main.model;

import java.util.List;

public class Hook {
    private HookState state;
    private double angle;
    private double ropeLength;
    private Rope rope;
    private int playerId;
    public Hook(int playerId, Rope rope) {}
    public void updateSwing(double deltaTime) {}
    public void throwHook() {}
    public void retractHook() {}
    public CollisionResult checkCollisionItem(Item item) { return null; }
    public boolean checkCollisionOtherHook(Hook other) { return false; }
    public void update(double deltaTime, List<Item> items, Hook otherHook) {}
    public HookState getState() { return null; }
    public double getAngle() { return 0; }
    public double getRopeLength() { return 0; }
    public int getPlayerId() { return 0; }
    public Rope getRope() { return null; }
    public void setState(HookState state) {}
}
