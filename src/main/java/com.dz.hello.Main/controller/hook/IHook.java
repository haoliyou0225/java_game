# 钩子核心行为 顶级接口
public interface IHook {
    // FR-03 启动独立钟摆摇摆，1:1复刻原版黄金矿工手感
    void startIndependentSwing();
    // FR-04 沿当前摇摆角度直线抛出钩爪
    void throwHook();
    // FR-05 携带命中物品收回，自动按物品重量档位动态调整收回速度
    void retractWithTargetItem(Item targetItem);
    // FR-06 空钩快速收回，速度显著高于带物品状态
    void retractEmptyFast();
    // FR-07 触达地图边界强制收回，返回起点后自动恢复摇摆
    void forceRetractByBoundary();
    // FR-08 紧急收回，空钩状态下立即回弹无需等待触达边界
    void emergencyRetractNow();
    // FR-16/FR-22 冻结钩爪全部运动指定秒数，到期自动恢复
    void freezeMovement(int freezeSeconds);
    // FR-21 开启收回速度翻倍效果，持续指定秒数
    void enableRetractSpeedDouble(int durationSeconds);
    // 逐帧更新钩子物理状态，由GameManager主循环调用，对齐全局类图updateSwing方法
    void updateSwing(double deltaTime);
    // 处理玩家输入动作，对齐全局类图inputAction方法
    void inputAction(InputAction action);
    // 获取钩子当前状态，对齐全局类图getState方法
    HookState getState();
    // 获取绳索当前长度，对齐全局类图getRopeLength方法
    double getRopeLength();
    // 碰撞检测方法，对齐全局类图checkCollisionWithItem方法
    CollisionResult checkCollisionWithItem(Item item);
}