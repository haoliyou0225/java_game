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
    /** 当前携带的物品（GRABBING 状态下非 null，其余状态 null） */
    Item getGrabbedItem();
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

    // ===== 结算瞬时标注（钩爪回到起点时显示几秒，由 GameManager 设置，GameView 渲染） =====
    /** 结算标注文本（null 表示无标注） */
    String getSettleLabel();
    /** 结算标注过期时间戳（毫秒），当前时间超过则标注失效 */
    long getSettleLabelUntil();
    /** 结算图标类型（NONE=无图标，其余对应福袋开出的道具图标） */
    Main.config.GameConfig.MysteryReward getSettleIcon();
}
