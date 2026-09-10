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

    // ===== 抢夺/眩晕/炸药（FR-12 双钩交互） =====
    /** 当前携带的物品（无携带返回 null） */
    Item getGrabbedItem();
    /** 钩尖与物品的原始圆形碰撞（忽略 grabbed 标记，供抢夺判定） */
    boolean tipHits(Item item);
    /** 最近一次抓取物品的系统时间戳（毫秒，抢夺 50ms 窗口判定） */
    long getGrabTimestampMs();
    /** 最近一次抓取时物品的原始 X（抢夺后物品弹回原位） */
    double getGrabOriginX();
    /** 最近一次抓取时物品的原始 Y */
    double getGrabOriginY();
    /** 进入眩晕：在当前碰撞点冻结 HOOK_STUN_DURATION_SEC 秒，松开携带物品，结束后自动空钩收回 */
    void stun();
    /** 炸药：炸毁当前携带的物品并立即空钩收回；返回被炸毁的物品（无携带返回 null） */
    Item detachCarriedItem();
}
