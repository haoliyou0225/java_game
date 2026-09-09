# 钩子临时效果 顶级接口
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