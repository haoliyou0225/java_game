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

    // ===== 渲染读取（供 UI 层 GameView 绘制绳索与钩爪头） =====
    /** 钩尖当前 X 坐标 */
    double getX();
    /** 钩尖当前 Y 坐标 */
    double getY();
    /** 钩爪起点（锚点）X 坐标 */
    double getStartX();
    /** 钩爪起点（锚点）Y 坐标 */
    double getStartY();
}
