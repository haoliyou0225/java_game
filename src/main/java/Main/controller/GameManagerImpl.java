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

<<<<<<< Updated upstream
=======
    /** 钓获反馈回调（物品被成功拉回并完成结算后触发，传递给视图层显示） */
    private Consumer<CatchFeedbackEvent> onCatchSettled;

    /** 上一帧双钩是否处于钩尖相撞状态（边沿检测：仅"未相撞→相撞"瞬间触发一次冻结，解冻收回途中钩尖未分离前不重复冻结，防卡死） */
    private boolean hooksColliding;

    /** 每帧结算完成的物品缓冲区（复用避免每帧 new ArrayList 分配） */
    private final List<Item> settledBuffer = new ArrayList<>();

>>>>>>> Stashed changes
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
<<<<<<< Updated upstream
        List<Item> settled = new ArrayList<>();
=======
        //    普通物品走 FR-17 结算链；福袋走 FR-14（先必给金币，再加权抽额外奖励入库存）
        settledBuffer.clear();
>>>>>>> Stashed changes
        for (Item item : sceneItemList) {
            item.updatePosition(deltaTime);
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
<<<<<<< Updated upstream
                    settled.add(item);
=======

                    // 写入 GameManager 和 GameModel 双体系
                    gameData.addScore(owner.getPlayerId(), finalScore);
                    grabber.addScore(finalScore);
                    settledBuffer.add(item);

                    // 结算瞬时飘字（锚点旁显示 2 秒）
                    if (owner instanceof HookImpl) {
                        HookImpl hookOwner = (HookImpl) owner;
                        if (item instanceof MysteryBag && reward != null) {
                            // 福袋只显示开出的道具名，不显示金币
                            hookOwner.setSettleLabel(reward.cnName(), reward, 2000);
                        } else {
                            hookOwner.setSettleLabel((finalScore >= 0 ? "+" : "") + finalScore, 2000);
                        }
                    }

                    // 钓获反馈 HUD：仅在成功拉回并完成结算后触发
                    // 空钩/未拉回/中途失败/未结算时不进入此分支，不会触发回调
                    if (onCatchSettled != null) {
                        onCatchSettled.accept(new CatchFeedbackEvent(
                                owner.getPlayerId(), item, finalScore, reward, extraGold));
                    }
>>>>>>> Stashed changes
                }
            }
        }
        sceneItemList.removeAll(settledBuffer);

        // 3. 移除已爆炸的炸弹
        sceneItemList.removeIf(i -> (i instanceof Bomb && ((Bomb) i).isExploded()));

<<<<<<< Updated upstream
        // 改进对局结束机制：场上可抓取物品清空且双方钩爪均处于 SWINGING 状态时，立即结束
        if (sceneItemList.isEmpty()
=======
        // 3.5 FR-08 自动结束：场上可抓取物品全部清空（或仅剩鼹鼠，鼹鼠不可抓取），
        //     且双方钩爪均回到 SWINGING → 立即结束
        boolean onlyMoleLeft = true;
        for (Item item : sceneItemList) {
            if (!(item instanceof Mole)) {
                onlyMoleLeft = false;
                break;
            }
        }
        if (onlyMoleLeft
>>>>>>> Stashed changes
                && hookP1.getState() == HookState.SWINGING
                && hookP2.getState() == HookState.SWINGING) {
            setState(GameState.FINISHED);
            if (onGameEnd != null) {
                onGameEnd.run();
            }
            return;
        }

        // 倒计时已由 GameTimerImpl 在后台线程每秒执行，此处不再冗余触发
    }

    /** 分发输入动作 */
    @Override
<<<<<<< Updated upstream
    public void dispatchAction(InputAction action) {
        if (action == null) return;
        // 融合：同时检查 GameManager 和 GameState
        if (gameData.getStage() != GameStage.PLAYING) return;
        if (state != GameState.PLAYING) return;
        if (action.getType().equals(InputAction.THROW_P1)) {
            hookP1.throwHook();
        } else if (action.getType().equals(InputAction.THROW_P2)) {
            hookP2.throwHook();
=======
    public void handleAction(int playerId, ActionType action) {
        if (action == null) {
            return;
        }
        // 暂停切换是唯一允许在 PAUSED 下处理的动作
        if (action == ActionType.TOGGLE_PAUSE) {
            togglePause();
            return;
        }
        if (state != GameState.PLAYING || gameData.getStage() != GameStage.PLAYING) {
            return;
        }
        Hook selfHook = playerId == 1 ? hookP1 : playerId == 2 ? hookP2 : null;
        Hook opponentHook = playerId == 1 ? hookP2 : playerId == 2 ? hookP1 : null;
        Player self = playerId == 1 ? player1 : playerId == 2 ? player2 : null;
        if (selfHook == null || self == null) {
            return;
        }
        String label = "玩家" + playerId;

        switch (action) {
            case THROW_HOOK -> releaseHook(selfHook, label);
            case USE_DYNAMITE -> useDynamite(selfHook, self, label);
            case USE_POWER_POTION -> usePowerPotion(selfHook, self, label);
            case USE_FREEZE_BOX -> useFreezeBox(opponentHook, self, label);
            case USE_LUCKY_CLOVER -> usePersistItem(self.useLuckyClover(), "幸运草", self, label);
            case USE_DIAMOND_BOOST -> usePersistItem(self.useDiamondBoost(), "钻石升级", self, label);
            case USE_STONE_BOOK -> usePersistItem(self.useStoneBook(), "石头书", self, label);
            default -> { /* 未知动作忽略 */ }
        }
    }

    /**
     * 释放钩爪（FR-11）：仅 SWINGING 可触发（hook.throwHook 内部门控），
     * 触发后以 HOOK_THROW_SPEED（500px/s）沿当前摆角直线抛出。
     */
    private void releaseHook(Hook hook, String playerLabel) {
        HookState before = hook.getState();
        hook.throwHook();
        if (hook.getState() == HookState.THROWING && before == HookState.SWINGING) {
            LogUtils.log(playerLabel + " 释放钩爪，角度: "
                    + String.format("%.1f°", Math.toDegrees(hook.getAngle())));
        }
    }

    /**
     * 引爆炸药（FR-15）：门控链 = PLAYING → 钩爪 GRABBING（携带物品收回中）→ 炸药库存>0。
     * 炸毁携带物（不计分、移出场景），钩爪立即空钩收回，库存 -1。
     */
    private void useDynamite(Hook hook, Player player, String playerLabel) {
        if (hook.getState() != HookState.GRABBING) {
            return; // SWINGING/THROWING/空钩收回等状态按键无效，不耗库存
        }
        if (player.getDynamiteCount() <= 0) {
            return;
        }
        Item carried = hook.detachCarriedItem();
        if (carried == null) {
            return;
        }
        player.useDynamite();
        removeItem(carried);
        LogUtils.log(playerLabel + " 引爆炸药，炸毁物品，剩余炸药: "
                + player.getDynamiteCount());
    }

    /**
     * 强力药水（FR-16）：库存 >0 时扣 1，自身钩爪收回速度 ×2 持续 10 秒；
     * 生效中再次使用仅刷新剩余时长（倍率不叠加，由 HookImpl 保证）。
     */
    private void usePowerPotion(Hook hook, Player player, String playerLabel) {
        if (!player.consumePowerPotion()) {
            return;
        }
        hook.applySpeedBoost(GameConfig.POWER_POTION_DURATION_SEC);
        LogUtils.log(playerLabel + " 使用强力药水，收回速度×2 持续 10 秒，剩余库存: "
                + player.getPowerPotionCount());
    }

    /**
     * 冰冻箱（FR-15/FR-16）：库存 >0 时扣 1，对方钩爪 FROZEN 冻结 3 秒（运动完全暂停）。
     */
    private void useFreezeBox(Hook opponentHook, Player player, String playerLabel) {
        if (opponentHook == null || !player.consumeFreezeBox()) {
            return;
        }
        opponentHook.freeze(GameConfig.HOOK_FREEZE_DURATION_SEC);
        LogUtils.log(playerLabel + " 使用冰冻箱，对方钩爪冻结 3 秒，剩余库存: "
                + player.getFreezeBoxCount());
    }

    /**
     * 持续道具按键使用（FR-18，幸运草/钻石升级/石头书）：
     * Player 已完成库存扣减与结果判定，本方法只负责日志反馈。
     * ACTIVATED=首次激活；DUPLICATE_GOLD=已激活折 50 金币；NO_STOCK=库存 0（按键无效）。
     */
    private void usePersistItem(PersistItemUseResult result, String itemName, Player player, String playerLabel) {
        switch (result) {
            case ACTIVATED -> LogUtils.log(playerLabel + " 使用" + itemName + "，本局效果已激活");
            case DUPLICATE_GOLD -> LogUtils.log(playerLabel + " " + itemName
                    + "已激活，自动折算 +" + GameConfig.ITEM_DUP_AUTO_GOLD + " 金币，当前分数: " + player.getScore());
            case NO_STOCK -> { /* 库存为 0，按键无效 */ }
        }
    }

    /**
     * 暂停/恢复（ESC，双方共用）：PLAYING↔PAUSED 互切，READY/FINISHED 忽略。
     * 双人同帧按 ESC 由 InputController 的 justPressed 防抖保证只派发一次。
     */
    private void togglePause() {
        if (state == GameState.PLAYING) {
            setState(GameState.PAUSED);
            LogUtils.log("游戏暂停");
        } else if (state == GameState.PAUSED) {
            setState(GameState.PLAYING);
            LogUtils.log("游戏继续");
>>>>>>> Stashed changes
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
