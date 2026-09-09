com.goldminer
└── feature
    └── hook                           # 人员2 唯一归属模块，无其他无关代码
        ├── IHook.java                 # 钩子核心行为 顶级接口
        ├── HookImpl.java              # 钩子核心物理逻辑 唯一实现类，对齐全局类图Hook类属性
        ├── IRope.java                 # 绳索行为 顶级接口
        ├── RopeImpl.java              # 绳索物理运动 唯一实现类，对齐全局类图Rope类属性
        ├── IHookEffect.java           # 钩子临时效果 顶级接口
        ├── effect                     # 临时效果实现子包
        │   ├── StunFreezeEffect.java  # FR-16/FR-22 冻结眩晕效果实现
        │   ├── SpeedBoostEffect.java  # FR-21 强力药水加速效果实现
        │   └── SelfExplodeEffect.java # FR-17 手动炸钩物品效果实现
        └── collision                  # 碰撞逻辑子包
            ├── HookItemCollider.java  # FR-04/FR-05/FR-14/FR-15 钩子与物品碰撞逻辑
            └── HookHookCollider.java  # FR-13 双钩抢夺互斥碰撞逻辑
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
public interface IRope {
    // FR-03 随钩子摇摆同步更新绳索角度与最大长度
    void syncSwingState(double currentAngle, double maxRopeLength);
    // FR-04 抛出过程中绳索匀速伸长
    void extendOnThrow();
    // FR-05/FR-06 收回过程中绳索匀速缩短
    void shrinkOnRetract();
    // 获取绳索当前长度，对齐全局类图getCurrentLen方法
    double getCurrentLen(double len);
    // 获取绳索当前角度，对齐全局类图getCurrentLen方法
    double getCurrentLen();
}
public interface IHookEffect {
    // 效果激活时触发回调
    void onEffectActivate(IHook targetHook);
    // 逐帧更新效果计时状态
    void onEffectUpdate(long deltaMs);
    // 效果到期自动触发回调，恢复钩子正常状态
    void onEffectExpire(IHook targetHook);
    // 判断当前效果是否已经结束
    boolean isEffectFinished();
}
