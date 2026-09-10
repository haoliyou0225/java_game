// FR-UI GameTimer：对局倒计时控制器接口（start/stop/每秒回调/归零回调），来自 feature_ui 分支
package Main.controller;

/**
 * 对局倒计时控制器（FR-30）
 * 负责每秒递减模型中的剩余时间，归零时结束对局并通知上层进入胜负判定。
 */
public interface GameTimer {

    /** 开始倒计时（每秒递减 1 秒） */
    void start();

    /**
     * 停止倒计时。
     * 注意：底层 Timer 取消后不可复用，如需重启请创建新实例。
     */
    void stop();

    /** 每秒递减后触发（用于刷新 HUD 倒计时显示） */
    void setOnTick(Runnable action);

    /** 倒计时归零后触发（游戏结束，进入胜负判定） */
    void setOnTimeUp(Runnable action);
}
