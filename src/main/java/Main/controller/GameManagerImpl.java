// FR-16 游戏管理器实现：每帧调度 gameLoopTick(deltaTime)、语义动作处理 handleAction、胜负判定，组装 Hook/Rope/GameData/Level
// 融合版：同时实现 GameModel 与 GameActionHandler 接口，替代 GameModelImpl 成为 Main.java 的唯一模型装配点
package Main.controller;

import Main.config.Config;
import Main.config.GameConfig;
import Main.model.*;
import Main.util.LogUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 游戏管理器实现（严格对齐 UML）+ GameModel / GameActionHandler 接口（融合版）
 * 每帧调度 Hook/Item 更新，处理碰撞、计分、胜负判定；
 * 同时是所有语义动作（抛钩/炸药/道具/暂停）的权威处理层：状态校验、物理推进、
 * 库存扣减、道具效果全部集中在本类，输入控制器只负责防抖与派发。
 * 本类不依赖任何 JavaFX 类型，可脱离界面在 headless main() 中跑完一整局。
 */
public class GameManagerImpl implements GameManager, GameModel, GameActionHandler, AutoCloseable {

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

    /** FR-08 对局提前结束回调（物品清空且双钩均 SWINGING 时触发） */
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

        // 1.5 双钩交互：50ms 抢夺窗口判定 + 钩尖直接相撞 → 双方 STUNNED 2 秒
        resolveHookConflict();

        // 2. 物品位置更新 + 携带物品收回完成时结算分数
        //    普通物品走 FR-17 结算链；福袋走 FR-14（先必给金币，再加权抽额外奖励入库存）
        List<Item> settled = new ArrayList<>();
        for (Item item : sceneItemList) {
            item.updatePosition();
            if (item.isGrabbed() && item.getWeight() > 0) {
                Hook owner = itemOnHook(item) == 1 ? hookP1 : hookP2;
                if (owner.getState() == HookState.SWINGING) {
                    item.setGrabbed(false);
                    Player grabber = owner.getPlayerId() == 1 ? player1 : player2;

                    int finalScore;
                    GameConfig.MysteryReward reward = null;
                    if (item instanceof MysteryBag) {
                        // FR-14：福袋带回起点立即结算
                        // ① 先发放生成时预计算的 100~800 金币（基础收益，幸运草可加成）
                        int baseGold = item.getScore();
                        int baseAfterClover = grabber.hasLuckyClover()
                                ? (int) Math.round(baseGold * GameConfig.LUCKY_CLOVER_BONUS_RATE)
                                : baseGold;
                        // ② 再按 35/35/20/10 加权抽取额外奖励，道具自动入库存，奖励附带金币
                        reward = rollBagExtraReward(ThreadLocalRandom.current());
                        int extraGold = grantBagExtra(reward, grabber, ThreadLocalRandom.current());
                        finalScore = baseAfterClover + extraGold;
                    } else {
                        // FR-17：基础价值 → 石头×3 → 钻石×2 → 幸运草×1.5 → 四舍五入
                        finalScore = settleNormalItem(item, grabber);
                    }

                    // 写入 GameManager 和 GameModel 双体系
                    gameData.addScore(owner.getPlayerId(), finalScore);
                    grabber.addScore(finalScore);
                    settled.add(item);

                    // 结算瞬时飘字（锚点旁显示 2 秒）
                    if (owner instanceof HookImpl) {
                        HookImpl hookOwner = (HookImpl) owner;
                        if (item instanceof MysteryBag && reward != null) {
                            hookOwner.setSettleLabel("+" + finalScore + " " + reward.cnName(),
                                    reward, 2000);
                        } else {
                            hookOwner.setSettleLabel((finalScore >= 0 ? "+" : "") + finalScore, 2000);
                        }
                    }
                }
            }
        }
        sceneItemList.removeAll(settled);

        // 3. 移除已爆炸的炸弹
        sceneItemList.removeIf(i -> (i instanceof Bomb && ((Bomb) i).isExploded()));

        // 3.5 FR-08 自动结束：场上可抓取物品全部清空，且双方钩爪均回到 SWINGING → 立即结束
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

    /**
     * 语义动作权威处理（FR-18，View→Controller→本方法）。
     * 唯一入口：先做对局状态门控（暂停除外），再按动作类型分发到具体私有流程；
     * 钩爪/库存的二级门控（SWINGING/GRABBING/库存>0）在各流程内完成，门控不通过即静默忽略。
     *
     * @param playerId 1=P1，2=P2；TOGGLE_PAUSE 传 0
     * @param action   语义动作
     */
    @Override
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
            System.out.println(LogUtils.format(playerLabel + " 释放钩爪，角度: "
                    + String.format("%.1f°", Math.toDegrees(hook.getAngle()))));
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
        System.out.println(LogUtils.format(playerLabel + " 引爆炸药，炸毁物品，剩余炸药: "
                + player.getDynamiteCount()));
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
        System.out.println(LogUtils.format(playerLabel + " 使用强力药水，收回速度×2 持续 10 秒，剩余库存: "
                + player.getPowerPotionCount()));
    }

    /**
     * 冰冻箱（FR-15/FR-16）：库存 >0 时扣 1，对方钩爪 FROZEN 冻结 3 秒（运动完全暂停）。
     */
    private void useFreezeBox(Hook opponentHook, Player player, String playerLabel) {
        if (opponentHook == null || !player.consumeFreezeBox()) {
            return;
        }
        opponentHook.freeze(GameConfig.HOOK_FREEZE_DURATION_SEC);
        System.out.println(LogUtils.format(playerLabel + " 使用冰冻箱，对方钩爪冻结 3 秒，剩余库存: "
                + player.getFreezeBoxCount()));
    }

    /**
     * 持续道具按键使用（FR-18，幸运草/钻石升级/石头书）：
     * Player 已完成库存扣减与结果判定，本方法只负责日志反馈。
     * ACTIVATED=首次激活；DUPLICATE_GOLD=已激活折 50 金币；NO_STOCK=库存 0（按键无效）。
     */
    private void usePersistItem(PersistItemUseResult result, String itemName, Player player, String playerLabel) {
        switch (result) {
            case ACTIVATED -> System.out.println(LogUtils.format(playerLabel + " 使用" + itemName + "，本局效果已激活"));
            case DUPLICATE_GOLD -> System.out.println(LogUtils.format(playerLabel + " " + itemName
                    + "已激活，自动折算 +" + GameConfig.ITEM_DUP_AUTO_GOLD + " 金币，当前分数: " + player.getScore()));
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
            System.out.println(LogUtils.format("游戏暂停"));
        } else if (state == GameState.PAUSED) {
            setState(GameState.PLAYING);
            System.out.println(LogUtils.format("游戏继续"));
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

    /**
     * 双钩冲突裁决（规格 FR-12）：
     * 1) 抢夺：双钩在 ≤50ms 窗口命中同一物品 → 物品弹回原位，双方在碰撞点 STUNNED 2 秒
     * 2) 钩尖直接相撞：双方 STUNNED 2 秒，携带中的物品在当前点松脱落回矿洞
     * STUNNED 期间不再重复触发（避免眩晕计时被刷新导致永远无法恢复）
     */
    private void resolveHookConflict() {
        // 已眩晕或被冰冻的钩子运动暂停，不参与新的冲突判定（FR-07/FR-16）
        if (hookP1.getState() == HookState.STUNNED || hookP2.getState() == HookState.STUNNED
                || hookP1.getState() == HookState.FROZEN || hookP2.getState() == HookState.FROZEN) {
            return;
        }

        long now = System.currentTimeMillis();
        Item i1 = hookP1.getGrabbedItem();
        Item i2 = hookP2.getGrabbedItem();

        // === 抢夺判定：同一物品在 50ms 窗口内被双钩命中 ===
        Item stolen = null;
        if (i1 != null && i1 == i2) {
            stolen = i1;
        } else if (i1 != null
                && hookP2.getState() == HookState.THROWING
                && hookP2.tipHits(i1)
                && now - hookP1.getGrabTimestampMs() <= GameConfig.HOOK_STEAL_WINDOW_MS) {
            stolen = i1;
        } else if (i2 != null
                && hookP1.getState() == HookState.THROWING
                && hookP1.tipHits(i2)
                && now - hookP2.getGrabTimestampMs() <= GameConfig.HOOK_STEAL_WINDOW_MS) {
            stolen = i2;
        }

        if (stolen != null) {
            // 物品弹回被抓取前的原位
            stolen.setGrabbed(false);
            stolen.setX(hookP1.ownsItem(stolen) ? hookP1.getGrabOriginX() : hookP2.getGrabOriginX());
            stolen.setY(hookP1.ownsItem(stolen) ? hookP1.getGrabOriginY() : hookP2.getGrabOriginY());
            // 双方在碰撞点停滞 2 秒，之后自动空钩收回起点恢复摇摆
            hookP1.stun();
            hookP2.stun();
            return;
        }

        // === 钩尖直接相撞（双钩均已伸出矿洞时才判定，钟摆状态相距 640px 不可能相碰） ===
        boolean extended1 = hookP1.getRopeLength() > 80;
        boolean extended2 = hookP2.getRopeLength() > 80;
        if (extended1 && extended2 && hookP1.checkCollisionOtherHook(hookP2)) {
            // 携带中的物品在当前碰撞点松脱，落回矿洞（保持可再抓取）
            releaseCarriedAtCurrentPoint(hookP1);
            releaseCarriedAtCurrentPoint(hookP2);
            hookP1.stun();
            hookP2.stun();
        }
    }

    /** 眩晕时松开携带物品：物品留在钩尖当前坐标，恢复未抓取状态可被再次抓取 */
    private void releaseCarriedAtCurrentPoint(Hook hook) {
        Item carried = hook.getGrabbedItem();
        if (carried != null) {
            carried.setGrabbed(false);
        }
    }

    /**
     * FR-14 福袋额外奖励加权抽奖：35% 金币 / 35% 炸药 / 20% 短时道具 / 10% 持续道具。
     * 包级静态以便单元测试直接验证权重分布。
     */
    static GameConfig.MysteryReward rollBagExtraReward(Random rnd) {
        int roll = rnd.nextInt(100);
        if (roll < GameConfig.BAG_WEIGHT_GOLD) {
            return GameConfig.MysteryReward.MYSTERY_GOLD;
        } else if (roll < GameConfig.BAG_WEIGHT_GOLD + GameConfig.BAG_WEIGHT_DYNAMITE) {
            return GameConfig.MysteryReward.DYNAMITE;
        } else if (roll < GameConfig.BAG_WEIGHT_GOLD + GameConfig.BAG_WEIGHT_DYNAMITE
                + GameConfig.BAG_WEIGHT_SHORT_ITEM) {
            // 20% 短时道具：强力药水 / 冰冻箱等概率
            return rnd.nextBoolean()
                    ? GameConfig.MysteryReward.POWER_POTION
                    : GameConfig.MysteryReward.FREEZE_BOX;
        } else {
            // 10% 持续道具：幸运草 / 钻石药水 / 石头书等概率
            switch (rnd.nextInt(3)) {
                case 0: return GameConfig.MysteryReward.LUCKY_CLOVER;
                case 1: return GameConfig.MysteryReward.DIAMOND_BOOST;
                default: return GameConfig.MysteryReward.STONE_BOOK;
            }
        }
    }

    /**
     * FR-17 普通物品结算链：基础价值 → 石头×3（石头书）→ 钻石×2（钻石药水）→ 幸运草×1.5 → 四舍五入。
     * 幸运草不作用于道具库存溢出转化的 50 金币（该部分在 grantBagExtra 中直接发放）。
     * 包级静态以便单元测试验证结算顺序与倍率。
     */
    static int settleNormalItem(Item item, Player player) {
        int value = item.getSettlementGold();
        if (item instanceof Stone && player.hasStoneBook()) {
            value *= GameConfig.STONE_BOOK_MULTIPLIER;
        }
        if (item instanceof Diamond && player.hasDiamondBoost()) {
            value *= GameConfig.DIAMOND_BOOST_MULTIPLIER;
        }
        if (player.hasLuckyClover()) {
            value = (int) Math.round(value * GameConfig.LUCKY_CLOVER_BONUS_RATE);
        }
        return value;
    }

    /**
     * FR-14/FR-15/FR-18 福袋额外奖励发放：道具自动入库存；
     * 炸药满 3、短时道具满 5、持续道具已激活时，再次获得自动转为 50 金币。
     * 包级静态以便单元测试验证库存溢出转化。
     *
     * @return 本次奖励附带的金币（金币档为 100~800 随机；溢出转化为 50；道具正常入库为 0）
     */
    static int grantBagExtra(GameConfig.MysteryReward reward, Player player, Random rnd) {
        switch (reward) {
            case MYSTERY_GOLD:
                // FR-14：金币档发放 100~800 随机金币
                return GameConfig.MYSTERY_BAG_MIN_GOLD
                        + rnd.nextInt(GameConfig.MYSTERY_BAG_MAX_GOLD - GameConfig.MYSTERY_BAG_MIN_GOLD + 1);
            case DYNAMITE:
                return player.addDynamite(1) == 0 ? GameConfig.ITEM_DUP_AUTO_GOLD : 0;
            case POWER_POTION:
                return player.addPowerPotion(1) == 0 ? GameConfig.ITEM_DUP_AUTO_GOLD : 0;
            case FREEZE_BOX:
                return player.addFreezeBox(1) == 0 ? GameConfig.ITEM_DUP_AUTO_GOLD : 0;
            case LUCKY_CLOVER:
                // FR-18：持续道具先入库存，玩家按 F/Num3 消耗激活（库存满则折 50 金币）
                return player.addLuckyClover(1) == 0 ? GameConfig.ITEM_DUP_AUTO_GOLD : 0;
            case DIAMOND_BOOST:
                return player.addDiamondBoost(1) == 0 ? GameConfig.ITEM_DUP_AUTO_GOLD : 0;
            case STONE_BOOK:
                return player.addStoneBook(1) == 0 ? GameConfig.ITEM_DUP_AUTO_GOLD : 0;
            default:
                return 0;
        }
    }

    /** 从场景移除物品（炸药炸毁钩上携带物时调用） */
    @Override
    public void removeItem(Item item) {
        if (item != null) {
            sceneItemList.remove(item);
        }
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

    /** 兼容 try-with-resources：行为等价于 shutdown() */
    @Override
    public void close() {
        shutdown();
    }

    /** FR-08 注册对局提前结束回调 */
    @Override
    public void setOnGameEnd(Runnable action) {
        this.onGameEnd = action;
    }
}
