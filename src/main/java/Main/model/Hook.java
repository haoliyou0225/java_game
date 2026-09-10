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

    // ===== FR-16 短时道具效果（冰冻箱 / 强力药水） =====
    /**
     * 冰冻箱：对该钩爪施加 seconds 秒 FROZEN 冻结，运动完全暂停、无法操作，
     * 倒计时结束恢复冻结前状态；已处于 FROZEN 时再次施加仅刷新时长（不叠加）。
     */
    void freeze(double seconds);
    /** 当前是否处于 FROZEN 冰冻状态 */
    boolean isFrozen();
    /** 冰冻剩余秒数（未冻结返回 0） */
    double getFreezeRemaining();
    /**
     * 强力药水：自身收回速度（空钩 + 携带）×2 持续 seconds 秒；
     * FR-18 已生效时再次使用仅刷新持续时间，不叠加倍率。
     */
    void applySpeedBoost(double seconds);
    /** 强力药水加速是否生效中 */
    boolean isSpeedBoostActive();
    /** 强力药水剩余秒数（未生效返回 0，供 HUD 显示） */
    double getSpeedBoostRemaining();

    // ===== 结算瞬时飘字（融合 feature/item：钩爪回到起点时在锚点旁显示几秒） =====
    /** 飘字文本（null 表示无飘字） */
    String getSettleLabel();
    /** 飘字过期系统时间戳（毫秒），超过则不再渲染 */
    long getSettleLabelUntil();
    /** 飘字对应的福袋道具图标（null 表示普通分数飘字） */
    Main.config.GameConfig.MysteryReward getSettleIcon();
}
