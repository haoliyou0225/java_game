// FR-16 游戏管理器实现：每帧调度 gameLoopTick(deltaTime)、输入分发 dispatchAction、胜负判定 judgeGameOver，组装 Hook/Rope/GameData/Level
// 融合版：同时实现 GameModel 接口，替代 GameModelImpl 成为 Main.java 的唯一模型装配点
package Main.controller;

import Main.config.Config;
import Main.config.GameConfig;
import Main.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 游戏管理器实现（严格对齐 UML）+ GameModel 接口（融合版）
 * 每帧调度 Hook/Item 更新，处理碰撞、计分、胜负判定
 * 同时作为 UI 层 GameModel 接口的唯一实现，Main.java 直接 new 本类
 */
public class GameManagerImpl implements GameManager, GameModel {

    // ===== GameManager 原版字段 =====
    private Hook hookP1;
    private Rope ropeP1;
    private Hook hookP2;
    private Rope ropeP2;
    private GameData gameData;
    private Level level;
    private List<Item> sceneItemList;

    // ===== GameModel 融合字段 =====
    private Player player1;
    private Player player2;
    private MineMap mineMap;
    private volatile double remainingTime;
    private volatile GameState state;

    /** 钩爪模拟收回定时器（UI 兼容保留） */
    private final ScheduledExecutorService hookRetrieveExecutor =
            Executors.newScheduledThreadPool(2, r -> {
                Thread t = new Thread(r, "hook-retrieve");
                t.setDaemon(true);
                return t;
            });

    /** 对局提前结束回调（场上物品清空且双方钩爪均回到 SWINGING 时触发） */
    private Runnable onGameEnd;

    public GameManagerImpl() {
        // hook 原版初始化
        this.ropeP1 = new RopeImpl(GameConfig.ROPE_MAX_EXTEND_LENGTH);
        this.ropeP2 = new RopeImpl(GameConfig.ROPE_MAX_EXTEND_LENGTH);
        this.hookP1 = new HookImpl(1, ropeP1);
        this.hookP2 = new HookImpl(2, ropeP2);
        this.gameData = new GameDataImpl(GameConfig.GAME_TOTAL_SEC);
        this.level = new LevelImpl();
        this.sceneItemList = new ArrayList<>();

        // GameModel 融合初始化
        this.player1 = new PlayerImpl();
        this.player2 = new PlayerImpl();
        this.remainingTime = Config.GAME_DURATION;
        this.state = GameState.PLAYING;

        // 原版：构造时就生成物品（与 GameModelImpl 保持一致）
        this.sceneItemList = level.generateSceneItems();
        gameData.setStage(GameStage.PLAYING);

        // MineMap 匿名实现（与 GameModelImpl 一致）
        this.mineMap = new MineMap() {
            private final List<Item> items = sceneItemList;
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

    // ===== GameManager 原版方法 =====

    /** 启动游戏 */
    @Override
    public void startGame() {
        sceneItemList = level.generateSceneItems();
        gameData.setStage(GameStage.PLAYING);
    }

    /**
     * 每帧推进（融合版：同时服务 GameManager 和 GameModel）
     * PAUSED 状态下冻结所有推进（钩子停摆、物品不更新、不计分）
     */
    @Override
    public void gameLoopTick(double deltaTime) {
        // 融合：同时检查 GameManager 的 GameStage 和 GameModel 的 GameState
        if (gameData.getStage() != GameStage.PLAYING) return;
        if (state != GameState.PLAYING) return;

        // 1. 推进双钩物理（钟摆/抛出/碰撞抓取/收回）
        hookP1.update(deltaTime, sceneItemList, hookP2);
        hookP2.update(deltaTime, sceneItemList, hookP1);

        // 2. 物品位置更新 + 携带物品收回完成时结算分数
        List<Item> settled = new ArrayList<>();
        for (Item item : sceneItemList) {
            item.updatePosition();
            if (item.isGrabbed() && item.getWeight() > 0) {
                Hook owner = itemOnHook(item) == 1 ? hookP1 : hookP2;
                if (owner.getState() == HookState.SWINGING) {
                    item.setGrabbed(false);
                    // 原版：用 getSettlementGold 结算（石头=1金币）
                    int score = item.getSettlementGold();
                    // 同时写入 GameManager 和 GameModel 双体系
                    gameData.addScore(owner.getPlayerId(), score);
                    if (owner.getPlayerId() == 1) {
                        player1.addScore(score);
                    } else {
                        player2.addScore(score);
                    }
                    settled.add(item);
                }
            }
        }
        sceneItemList.removeAll(settled);

        // 3. 移除已爆炸的炸弹
        sceneItemList.removeIf(i -> (i instanceof Bomb && ((Bomb) i).isExploded()));

        // 改进对局结束机制：场上可抓取物品清空且双方钩爪均处于 SWINGING 状态时，立即结束
        if (sceneItemList.isEmpty()
                && hookP1.getState() == HookState.SWINGING
                && hookP2.getState() == HookState.SWINGING) {
            setState(GameState.FINISHED);
            if (onGameEnd != null) {
                onGameEnd.run();
            }
            return;
        }

        // 4. GameManager 原版每秒倒计时（GameTimerImpl 也在倒计时，双重保障）
        if (Math.random() < deltaTime) {
            gameData.countDownTick();
        }
    }

    /** 分发输入动作 */
    @Override
    public void dispatchAction(InputAction action) {
        if (action == null) return;
        // 融合：同时检查 GameManager 和 GameState
        if (gameData.getStage() != GameStage.PLAYING) return;
        if (state != GameState.PLAYING) return;
        if (action.getType().equals(InputAction.THROW_P1)) {
            hookP1.throwHook();
        } else if (action.getType().equals(InputAction.THROW_P2)) {
            hookP2.throwHook();
        }
    }

    /** 判定是否结束 */
    @Override
    public boolean judgeGameOver() {
        return gameData.getStage() == GameStage.GAME_OVER;
    }

    /** 判断物品挂在哪个钩子上 */
    private int itemOnHook(Item item) {
        if (hookP1.ownsItem(item)) return 1;
        if (hookP2.ownsItem(item)) return 2;
        return 1;
    }

    // ===== GameManager Getter =====

    @Override public Hook getHookP1() { return hookP1; }
    @Override public Hook getHookP2() { return hookP2; }
    @Override public GameData getGameData() { return gameData; }
    @Override public Level getLevel() { return level; }
    @Override public List<Item> getSceneItemList() { return sceneItemList; }

    // ===== GameModel 融合方法 =====

    @Override public Player getPlayer1() { return player1; }
    @Override public Player getPlayer2() { return player2; }
    @Override public double getRemainingTime() { return remainingTime; }
    @Override public GameState getState() { return state; }

    @Override
    public void setState(GameState state) {
        synchronized (this) {
            this.state = state;
            this.notifyAll(); // 唤醒暂停挂起中的钩爪收回任务
            // 同步 GameManager 体系的 GameStage
            if (state == GameState.PLAYING) {
                gameData.setStage(GameStage.PLAYING);
            } else if (state == GameState.FINISHED) {
                gameData.setStage(GameStage.GAME_OVER);
            }
        }
    }

    @Override public void setRemainingTime(double time) { this.remainingTime = time; }
    @Override public Hook getHook1() { return hookP1; }
    @Override public Hook getHook2() { return hookP2; }
    @Override public MineMap getMineMap() { return mineMap; }

    /**
     * FR-18 P0（模拟收回）：UI 兼容保留。
     */
    @Override
    public void scheduleHookRetrieve(Hook hook, String playerLabel) {
        hookRetrieveExecutor.schedule(() -> {
            synchronized (this) {
                while (getState() == GameState.PAUSED) {
                    try {
                        wait(200);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
            if (getState() == GameState.FINISHED) return;
            if (hook.getState() == HookState.THROWING) {
                hook.setState(HookState.SWINGING);
            }
        }, 2, TimeUnit.SECONDS);
    }

    /** 释放后台资源 */
    @Override
    public void shutdown() {
        hookRetrieveExecutor.shutdownNow();
    }

    /** 设置对局提前结束回调 */
    @Override
    public void setOnGameEnd(Runnable action) {
        this.onGameEnd = action;
    }
}
