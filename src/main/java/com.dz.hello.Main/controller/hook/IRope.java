# 绳索行为 顶级接口
        /public interface IRope {
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