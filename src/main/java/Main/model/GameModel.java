// FR-UI GameModel：UI 对局模型接口（双方玩家/倒计时/状态/双钩/矿洞/模拟收回），来自 feature_ui 分支
package Main.model;

public interface GameModel {

    Player getPlayer1();

    Player getPlayer2();

    double getRemainingTime();

    GameState getState();

    void setState(GameState state);

    /** 设置剩余时间（由 GameTimer 调用） */
    void setRemainingTime(double time);

    /** 玩家1的钩爪（FR-28） */
    Hook getHook1();

    /** 玩家2的钩爪（FR-28） */
    Hook getHook2();

    /** 矿洞地图（含所有物品）（FR-28） */
    MineMap getMineMap();

    /**
     * FR-11/FR-16 真实钩子物理驱动：每帧由装配层（Main）调用，
     * 推进双钩钟摆/抛出/收回/抓取状态机，并在携带物品收回完成时结算分数。
     * PAUSED 状态下冻结推进，与 GameTimer 倒计时联动。
     *
     * @param deltaTime 距上一帧的秒数
     */
    void gameLoopTick(double deltaTime);

    /**
     * FR-18 P0（模拟收回）：安排指定钩爪在 THROWING 状态 2 秒后自动切回 SWINGING。
     * 真实物理收回由 gameLoopTick 驱动；本方法保留用于 UI 流程兼容。
     *
     * @param hook        目标钩爪（getHook1() 或 getHook2()）
     * @param playerLabel 玩家标签（如 "玩家1"），仅用于日志输出
     */
    void scheduleHookRetrieve(Hook hook, String playerLabel);

    /**
     * 释放后台资源（钩爪收回定时线程池）。
     * 对局结束/重开前由装配层调用，防止反复开局累积线程；
     * 调用后本实例不再调度新的钩爪收回任务。
     */
    void shutdown();

    /**
     * 胜负判定（FR-31）：比较双方最终分数。
     * 业务规则下沉到 Model 层，View 层仅负责文案与颜色渲染。
     *
     * @return 正数=玩家1获胜，负数=玩家2获胜，0=平局
     */
    default int determineWinner() {
        return Integer.compare(getPlayer1().getScore(), getPlayer2().getScore());
    }
}
