// FR-UI GameTimerImpl：倒计时实现（java.util.Timer 守护线程，暂停冻结），来自 feature_ui 分支
package Main.controller;

import Main.model.GameModel;
import Main.model.GameState;

import java.util.Timer;
import java.util.TimerTask;

/**
 * 对局倒计时控制器实现（FR-30）
 * 基于纯 Java 的 java.util.Timer（守护线程），每秒触发一次：
 * 剩余时间递减 1 秒，归零时将模型状态置为 FINISHED 并回调进入胜负判定。
 * <p>
 * 分层约束：本类不导入任何 javafx.* 类型，controller 层只依赖 model 层。
 * 线程模型：tick 在 Timer 后台线程执行，仅做纯数据操作（修改 Model，字段已 volatile）；
 * 涉及 UI 的回调（onTick / onTimeUp）需由装配层（Main）用 Platform.runLater 包装后
 * 在 JavaFX Application Thread 执行，本类不感知任何 UI。
 */
public class GameTimerImpl implements GameTimer {

    /** 每秒一次的 Tick 间隔（毫秒） */
    private static final long TICK_INTERVAL_MS = 1000;

    private final GameModel model;

    /** 守护定时器：stop() 后线程随任务结束退出，不泄漏资源 */
    private final Timer timer;

    private Runnable onTick;
    private Runnable onTimeUp;

    public GameTimerImpl(GameModel model) {
        this.model = model;
        this.timer = new Timer("game-timer", true);
    }

    /** 每秒执行一次：递减剩余时间；归零则结束对局（暂停时冻结，不递减） */
    private void tick() {
        try {
            // 暂停状态：冻结倒计时与所有 tick 驱动逻辑（模拟加分/物品更新均不触发），
            // 恢复后从同一剩余时间继续，实现"无缝恢复"
            if (model.getState() == GameState.PAUSED) {
                return;
            }

            double remaining = model.getRemainingTime() - 1;
            model.setRemainingTime(Math.max(0, remaining));

            if (onTick != null) {
                onTick.run();
            }

            if (remaining <= 0) {
                stop();
                model.setState(GameState.FINISHED);
                if (onTimeUp != null) {
                    onTimeUp.run();
                }
            }
        } catch (Exception e) {
            // Timer 线程一旦抛出未捕获异常即整体终止，此处兜底避免倒计时静默失效
            System.err.println("[FR-30] 倒计时 tick 异常: " + e.getMessage());
        }
    }

    @Override
    public void start() {
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                tick();
            }
        }, TICK_INTERVAL_MS, TICK_INTERVAL_MS);
    }

    @Override
    public void stop() {
        timer.cancel();
    }

    @Override
    public void setOnTick(Runnable action) {
        this.onTick = action;
    }

    @Override
    public void setOnTimeUp(Runnable action) {
        this.onTimeUp = action;
    }
}
