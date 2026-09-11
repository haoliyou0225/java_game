// FR-UI GameModelImpl：UI 对局模型实现，已接入 hook 分支真实物理（HookImpl + LevelImpl），保留 UI 暂停状态与倒计时调度
package Main.model;

import Main.config.Config;
import Main.config.GameConfig;
import Main.util.LogUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * UI 层对局模型实现。
 * 已恢复 hook 分支真实物理玩法：双钩使用 HookImpl+RopeImpl，物品使用 LevelImpl 随机生成。
 * 保留 GameState 暂停状态以服务 UI 流程（PAUSED 时冻结 gameLoopTick）。
 */
public class GameModelImpl implements GameModel {
    private Player player1;
    private Player player2;
    private Hook hook1;
    private Hook hook2;
    private MineMap mineMap;
    /** 剩余时间（秒）：后台倒计时线程写入、JavaFX 线程读取，需 volatile */
    private volatile double remainingTime;

    /** 对局状态：输入线程/倒计时线程写入、多线程读取，需 volatile */
    private volatile GameState state;

    /**
     * FR-18 钩爪模拟收回定时器（双线程池）。
     * 每个玩家独立线程：暂停挂起时互不阻塞；守护线程随游戏退出自动终止。
     */
    private final ScheduledExecutorService hookRetrieveExecutor =
            Executors.newScheduledThreadPool(2, r -> {
                Thread t = new Thread(r, "hook-retrieve");
                t.setDaemon(true);
                return t;
            });

    /** 场景物品列表（供 hook.update 碰撞检测与收回结算使用） */
    private final List<Item> sceneItems;

    public GameModelImpl() {
        this.player1 = new PlayerImpl();
        this.player2 = new PlayerImpl();
        this.remainingTime = Config.GAME_DURATION;
        this.state = GameState.PLAYING;

        // hook 分支真实物理接入：双钩左右分置，绳索最大延伸取自 GameConfig
        this.hook1 = new HookImpl(1, new RopeImpl(GameConfig.ROPE_MAX_EXTEND_LENGTH));
        this.hook2 = new HookImpl(2, new RopeImpl(GameConfig.ROPE_MAX_EXTEND_LENGTH));

        // 真实物品生成：LevelImpl 随机生成 Gold/Diamond/Stone/Bomb 混合场景
        this.sceneItems = new ArrayList<>(new LevelImpl().generateSceneItems());

        // 包装为 MineMap 供 UI 渲染边界与物品列表
        this.mineMap = new MineMap() {
            private final List<Item> items = sceneItems;
            @Override public List<Item> getItems() { return items; }
            @Override public void generate() { }
            @Override public int getLeftTotalValue() {
                int sum = 0;
                for (Item it : items) if (it.getX() < Config.WIDTH / 2.0) sum += it.getScore();
                return sum;
            }
            @Override public int getRightTotalValue() {
                int sum = 0;
                for (Item it : items) if (it.getX() >= Config.WIDTH / 2.0) sum += it.getScore();
                return sum;
            }
            @Override public double getMinX() { return 50; }
            @Override public double getMinY() { return Config.HUD_HEIGHT + 60; }
            @Override public double getMaxX() { return Config.WIDTH - 50; }
            @Override public double getMaxY() { return Config.HEIGHT - 60; }
        };
    }

    // 其他 getter/setter 方法
    @Override public Player getPlayer1() { return player1; }
    @Override public Player getPlayer2() { return player2; }
    @Override public double getRemainingTime() { return remainingTime; }
    @Override public GameState getState() { return state; }
    @Override
    public void setState(GameState state) {
        synchronized (this) {
            this.state = state;
            // 唤醒暂停挂起中的钩爪收回任务（FR-19：恢复后立即继续计时收回）
            this.notifyAll();
        }
    }
    @Override public void setRemainingTime(double time) { this.remainingTime = time; }
    @Override public Hook getHook1() { return hook1; }
    @Override public Hook getHook2() { return hook2; }
    @Override public MineMap getMineMap() { return mineMap; }

    /**
     * FR-11/FR-16 真实钩子物理驱动：每帧由装配层（Main）调用。
     * 推进双钩钟摆/抛出/收回/抓取状态机，并在携带物品收回完成时结算分数。
     * PAUSED 状态下冻结推进，与 GameTimer 倒计时联动。
     */
    @Override
    public void gameLoopTick(double deltaTime) {
        if (state != GameState.PLAYING) return;

        // 1. 推进双钩物理（钟摆/抛出/碰撞抓取/收回）
        hook1.update(deltaTime, sceneItems, hook2);
        hook2.update(deltaTime, sceneItems, hook1);

        // 2. 物品位置更新 + 携带物品收回完成时结算分数
        List<Item> settled = new ArrayList<>();
        for (Item item : sceneItems) {
            item.updatePosition();
            if (item.isGrabbed() && item.getWeight() > 0) {
                Hook owner = hook1.ownsItem(item) ? hook1 : (hook2.ownsItem(item) ? hook2 : null);
                if (owner != null && owner.getState() == HookState.SWINGING) {
                    item.setGrabbed(false);
                    // 原版：用 getSettlementGold 结算（石头=1金币）
                    int score = item.getSettlementGold();
                    if (owner.getPlayerId() == 1) {
                        player1.addScore(score);
                    } else {
                        player2.addScore(score);
                    }
                    settled.add(item);
                    System.out.println(LogUtils.format("玩家" + owner.getPlayerId()
                            + " 收回物品，" + (score >= 0 ? "+" : "") + "$" + score));
                }
            }
        }
        sceneItems.removeAll(settled);

        // 3. 移除已爆炸的炸弹
        sceneItems.removeIf(it -> (it instanceof Bomb && ((Bomb) it).isExploded()));
    }

    /**
     * FR-18 P0（模拟收回）：保留用于 UI 流程兼容，真实物理收回由 gameLoopTick 驱动。
     */
    @Override
    public void scheduleHookRetrieve(Hook hook, String playerLabel) {
        hookRetrieveExecutor.schedule(() -> {
            // 暂停冻结：等待游戏恢复后再执行收回（不含暂停时长的纯游戏时间 2 秒）；
            // 用 wait/notifyAll 替代忙轮询：恢复时由 setState 的 notifyAll 立即唤醒
            synchronized (this) {
                while (getState() == GameState.PAUSED) {
                    try {
                        wait(200); // 超时兜底（200ms），正常情况下由 notifyAll 唤醒
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return; // 线程被中断（如 shutdown）则放弃本次收回
                    }
                }
            }
            // 对局已结束（时间到）则不再收回，避免结算后打印无意义日志
            if (getState() == GameState.FINISHED) {
                return;
            }
            // 仅当钩爪仍处于 THROWING 时才收回：防止定时任务与后续操作产生竞态
            if (hook.getState() == HookState.THROWING) {
                hook.setState(HookState.SWINGING);
                System.out.println(LogUtils.format(playerLabel + " 钩爪收回"));
            }
        }, 2, TimeUnit.SECONDS); // 2 秒（游戏时间）后自动收回
    }

    /**
     * 释放后台资源：终止钩爪收回定时线程池（FR-18）。
     * 对局结束/进入结算前由装配层（Main）调用，防止反复开局累积线程；
     * 调用后本实例不再调度新的收回任务，挂起中的任务被中断放弃。
     */
    @Override
    public void shutdown() {
        hookRetrieveExecutor.shutdownNow();
    }

    /**
     * 从场景移除物品（玩家按炸药键炸毁钩上携带物时调用）。
     * 注意：本类已被融合版 GameManagerImpl 取代，仅保留接口实现以维持编译。
     */
    @Override
    public void removeItem(Item item) {
        if (item != null) {
            sceneItems.remove(item);
        }
    }

    /**
     * FR-08 自动结束回调：本类已被 GameManagerImpl 取代，仅空实现以维持接口编译。
     */
    @Override
    public void setOnGameEnd(Runnable action) {
        // no-op：GameManagerImpl 才是 Main.java 实际装配的模型
    }
}
