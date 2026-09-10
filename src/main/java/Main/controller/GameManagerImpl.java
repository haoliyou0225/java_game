// FR-16 游戏管理器实现：每帧调度 gameLoopTick(deltaTime)、输入分发 dispatchAction、胜负判定 judgeGameOver，组装 Hook/Rope/GameData/Level
// 融合版：同时实现 GameModel 接口，替代 GameModelImpl 成为 Main.java 的唯一模型装配点
package Main.controller;

import Main.config.Config;
import Main.config.GameConfig;
import Main.model.*;
import Main.util.LogUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
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

        // 1.1 读取钩子待结算分数（目前仅福袋/特殊标注使用，TNT 炸药桶已改为只爆炸不扣分，settleScore 恒为 0 自动跳过）
        if (hookP1 instanceof HookImpl) {
            HookImpl h1 = (HookImpl) hookP1;
            int sc1 = h1.getSettleScore();
            if (sc1 != 0) {
                gameData.addScore(1, sc1);
                player1.addScore(sc1);
                h1.clearSettleScore();
            }
        }
        if (hookP2 instanceof HookImpl) {
            HookImpl h2 = (HookImpl) hookP2;
            int sc2 = h2.getSettleScore();
            if (sc2 != 0) {
                gameData.addScore(2, sc2);
                player2.addScore(sc2);
                h2.clearSettleScore();
            }
        }

        // 2. 物品位置更新 + 携带物品收回完成时结算分数
        List<Item> settled = new ArrayList<>();
        for (Item item : sceneItemList) {
            item.updatePosition();
            if (item.isGrabbed() && item.getWeight() > 0) {
                Hook owner = itemOnHook(item) == 1 ? hookP1 : hookP2;
                if (owner.getState() == HookState.SWINGING) {
                    item.setGrabbed(false);
                    Player grabber = owner.getPlayerId() == 1 ? player1 : player2;
                    int rawScore = item.getSettlementGold();

                    // === 道具效果套算 ===
                    int finalScore = rawScore;
                    GameConfig.MysteryReward mysteryReward = null;

                    // 福袋特殊结算：6 种奖励等概率抽取，抽到金币才给金币
                    if (item instanceof MysteryBag) {
                        // 只抽一次奖励类型，结算与标注共用
                        mysteryReward = GameConfig.MysteryReward.values()[
                                ThreadLocalRandom.current().nextInt(GameConfig.MysteryReward.values().length)];
                        if (mysteryReward == GameConfig.MysteryReward.MYSTERY_GOLD) {
                            finalScore = ThreadLocalRandom.current().nextInt(
                                    GameConfig.MYSTERY_GOLD_MIN, GameConfig.MYSTERY_GOLD_MAX + 1);
                        } else {
                            finalScore = 0;
                        }
                        applyMysteryReward(mysteryReward, grabber, owner.getPlayerId());
                    } else {
                        // 普通物品：钻石升级药水 / 幸运草套算
                        if (item instanceof Diamond && grabber.hasDiamondBoost()) {
                            finalScore *= GameConfig.DIAMOND_BOOST_MULTIPLIER;
                        }
                        if (grabber.hasLuckyClover()) {
                            finalScore = (int) Math.round(finalScore * GameConfig.LUCKY_CLOVER_BONUS_RATE);
                        }
                    }

                    // 同时写入 GameManager 和 GameModel 双体系
                    gameData.addScore(owner.getPlayerId(), finalScore);
                    grabber.addScore(finalScore);
                    settled.add(item);

                    // 结算瞬时标注：显示在吊机起点旁，2 秒后消失
                    if (owner instanceof HookImpl) {
                        HookImpl hookOwner = (HookImpl) owner;
                        if (item instanceof MysteryBag && mysteryReward != null) {
                            String labelText;
                            if (mysteryReward == GameConfig.MysteryReward.MYSTERY_GOLD) {
                                labelText = (finalScore >= 0 ? "+" : "") + finalScore;
                            } else {
                                labelText = "+" + mysteryReward.cnName();
                            }
                            hookOwner.setSettleLabel(labelText, mysteryReward, 2000);
                        } else {
                            String sign = finalScore >= 0 ? "+" : "";
                            hookOwner.setSettleLabel(sign + finalScore, 2000);
                        }
                    }
                }
            }
        }
        sceneItemList.removeAll(settled);

        // 3. 移除已爆炸的炸弹
        sceneItemList.removeIf(i -> (i instanceof Bomb && ((Bomb) i).isExploded()));

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

    /**
     * 玩家使用炸药触发爆炸效果。
     * 若该玩家钩爪正携带物品 → 炸掉物品（从场景移除 + 清除 grabbedItem 引用）→ 钩爪变 RETRACTING 空钩收回。
     * 若钩爪未携带物品 → 无效果（炸药已消耗）。
     */
    @Override
    public void triggerExplosion(int playerId) {
        Hook hook = playerId == 1 ? hookP1 : hookP2;
        Item grabbed = hook.getGrabbedItem();
        if (grabbed != null) {
            // 炸掉物品：从场景移除
            sceneItemList.remove(grabbed);
            // 钩爪放弃物品（HookImpl.grabbedItem 需置 null）
            // 这里通过 retractHook() + 状态切换让 hook 自行清理
            hook.retractHook();
            System.out.println(LogUtils.format("玩家" + playerId + " 炸药炸掉了 " + grabbed.getClass().getSimpleName()));
        } else {
            System.out.println(LogUtils.format("玩家" + playerId + " 炸药未命中（钩爪未携带物品）"));
        }
    }

    /**
     * 应用福袋随机道具奖励。
     *
     * @param reward  抽取到的奖励类型
     * @param player  获得奖励的玩家
     * @param playerId 玩家编号（日志用）
     */
    private void applyMysteryReward(GameConfig.MysteryReward reward, Player player, int playerId) {
        String label = "玩家" + playerId + " 福袋开出 ";
        switch (reward) {
            case LUCKY_CLOVER:
                player.grantLuckyClover();
                System.out.println(LogUtils.format(label + "幸运草！本局收益 +50%"));
                break;
            case DIAMOND_BOOST:
                player.grantDiamondBoost();
                System.out.println(LogUtils.format(label + "钻石升级药水！钻石价值翻倍"));
                break;
            case STONE_BOOK:
                player.grantStoneBook();
                System.out.println(LogUtils.format(label + "石头收藏书！石头消除惩罚"));
                break;
            case MYSTERY_GOLD:
                // 金币已在上方结算时发放（200~800 随机），这里不再重复加分
                System.out.println(LogUtils.format(label + "金币！"));
                break;
            case DYNAMITE:
                int added = player.addBomb(1);
                if (added > 0) {
                    System.out.println(LogUtils.format(label + "炸药！库存 +1"));
                } else {
                    System.out.println(LogUtils.format(label + "炸药！但库存已满，自动转为 +" + GameConfig.BOMB_FULL_AUTO_GOLD + " 金币"));
                    player.addScore(GameConfig.BOMB_FULL_AUTO_GOLD);
                }
                break;
            case POWER_POTION:
                System.out.println(LogUtils.format(label + "强力药水（钩爪收回速度翻倍，预留实现）"));
                break;
        }
    }
}
