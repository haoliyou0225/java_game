// FR-11 钩子接口：钟摆 updateSwing / 抛出 throwHook / 收回 retractHook / 与物品碰撞 checkCollisionItem / 与对钩碰撞 checkCollisionOtherHook
package Main.model;

import java.util.List;

public interface Hook {
    void updateSwing(double deltaTime);
    void throwHook();
    void retractHook();
    CollisionResult checkCollisionItem(Item item);
    boolean checkCollisionOtherHook(Hook other);
    void update(double deltaTime, List<Item> items, Hook otherHook);
    boolean ownsItem(Item item);
    HookState getState();
    double getAngle();
    double getRopeLength();
    int getPlayerId();
    Rope getRope();
    void setState(HookState state);
}
