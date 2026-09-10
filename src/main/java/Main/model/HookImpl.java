// FR-11 钩子核心实现：钟摆 / 直线抛出 / 边界收回 / 重量档位收回 / TNT立即爆炸 / 鼹鼠偏角 / 双钩眩晕 / 炸药丢弃
package Main.model;

import Main.config.GameConfig;
import Main.util.CollisionUtil;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 钩子实体实现（严格按钩子玩法规格）
 * 状态机：
 *   SWINGING 钟摆（±1.25rad，1.5rad/s）
 *     → THROWING 沿当前角度直线抛出（500px/s，触边界/最大绳长转 RETRACTING）
 *       → GRABBING 携带物品收回（轻1.2s/中2.5s/重5.0s）→ SWINGING
 *       → RETRACTING 空钩收回（800px/s）→ SWINGING
 *     → STUNNED 碰撞点冻结2秒（无法操作）→ RETRACTING → SWINGING
 */
public class HookImpl implements Hook {

    /** 钩尖碰撞半径（像素） */
    private static final double TIP_RADIUS = 15;
    /** 钟摆摆动时的基础绳长（像素，锚点80 + 70 = 矿洞顶150） */
    private static final double SWING_LENGTH = 70;
    /** 鼹鼠偏转冷却（纳秒，避免同一只鼹鼠连续偏转） */
    private static final long DEFLECT_COOLDOWN_NS = 250_000_000L;
    /** 钻石猪逃脱冷却（纳秒，同一只猪逃脱后 0.5s 内不再掷捕获骰） */
    private static final long PIG_ESCAPE_COOLDOWN_NS = 500_000_000L;

    private HookState state;
    private double angle;      // 弧度，PI/2 表示垂直向下
    private double ropeLength;
    private Rope rope;
    private int playerId;
    private int swingDir = 1;  // 摆动方向
    private Item grabbedItem;  // 当前携带的物品

    // ===== 眩晕 / 抢夺 / 档位相关状态 =====
    /** 眩晕剩余秒数 */
    private double stunTimer;
    /** FR-16 冰冻剩余秒数（FROZEN 态倒计时） */
    private double frozenTimer;
    /** 进入 FROZEN 前的状态，解冻后恢复 */
    private HookState preFrozenState;
    /** FR-16 强力药水剩余秒数（>0 时空钩/携带收回速度 ×2） */
    private double speedBoostTimer;
    /** 抓取瞬间的绳长（收回速度 = 该绳长 / 物品收回耗时） */
    private double grabRopeLength;
    /** 最近一次抓取时间戳（毫秒，50ms 抢夺窗口） */
    private long grabTimestampMs;
    /** 抓取时物品原始坐标（抢夺后弹回） */
    private double grabOriginX, grabOriginY;
    /** 活物交互冷却：最近造成偏转/逃脱的鼹鼠或钻石猪与时间 */
    private Item lastInteractMob;
    private long lastInteractNano;

    // ===== 结算瞬时飘字（融合 feature/item） =====
    /** 飘字文本（null 无飘字） */
    private String settleLabel;
    /** 飘字过期时间戳（毫秒） */
    private long settleLabelUntil;
    /** 飘字对应福袋道具图标（null 为普通分数） */
    private Main.config.GameConfig.MysteryReward settleIcon;

    public HookImpl(int playerId, Rope rope) {
        this.playerId = playerId;
        this.rope = rope;
        this.state = HookState.SWINGING;
        this.angle = Math.PI / 2;
        this.ropeLength = SWING_LENGTH;
        // 两钩初始摆动方向相反，视觉上交替摆动
        this.swingDir = (playerId == 2) ? -1 : 1;
        rope.setCurrentLen(ropeLength);
    }

    /** 钟摆摆动：固定角速度 1.5rad/s，幅度 ±1.25rad */
    @Override
    public void updateSwing(double deltaTime) {
        angle += swingDir * GameConfig.HOOK_SWING_SPEED * deltaTime;
        double max = Math.PI / 2 + GameConfig.HOOK_SWING_MAX_OFFSET;
        double min = Math.PI / 2 - GameConfig.HOOK_SWING_MAX_OFFSET;
        if (angle > max) {
            angle = max;
            swingDir = -1;
        } else if (angle < min) {
            angle = min;
            swingDir = 1;
        }
    }

    /** 抛出钩子：仅 SWINGING 可抛出，沿当前摆动角度直线飞出 */
    @Override
    public void throwHook() {
        if (state != HookState.SWINGING) return;
        grabbedItem = null;
        state = HookState.THROWING;
    }

    /** 紧急收回（空钩） */
    @Override
    public void retractHook() {
        if (state == HookState.THROWING || state == HookState.RETRACTING) {
            state = HookState.RETRACTING;
        }
    }

    /** 与物品碰撞检测（跳过已被抓取的物品） */
    @Override
    public CollisionResult checkCollisionItem(Item item) {
        if (item == null || item.isGrabbed()) {
            return new CollisionResultImpl(false, null);
        }
        boolean hit = tipHits(item);
        return new CollisionResultImpl(hit, hit ? item : null);
    }

    /** 钩尖原始圆形碰撞（忽略 grabbed，供抢夺判定） */
    @Override
    public boolean tipHits(Item item) {
        if (item == null) return false;
        double[] tip = hookTip();
        return CollisionUtil.circleCollision(
                tip[0], tip[1], TIP_RADIUS,
                item.getX(), item.getY(), item.getRadius());
    }

    /** 与对方钩子碰撞检测（钩尖对钩尖） */
    @Override
    public boolean checkCollisionOtherHook(Hook other) {
        double[] t1 = hookTip();
        double[] t2 = ((HookImpl) other).hookTip();
        return CollisionUtil.circleCollision(t1[0], t1[1], TIP_RADIUS, t2[0], t2[1], TIP_RADIUS);
    }

    /** 每帧更新（由 GameManager 驱动） */
    @Override
    public void update(double deltaTime, List<Item> items, Hook otherHook) {
        // FR-16 强力药水倒计时：对局内任意状态都持续消耗（PAUSED 时本方法不被调用，天然冻结）
        if (speedBoostTimer > 0) {
            speedBoostTimer = Math.max(0, speedBoostTimer - deltaTime);
        }
        switch (state) {
            case SWINGING:
                updateSwing(deltaTime);
                break;
            case THROWING:
                ropeLength += GameConfig.HOOK_THROW_SPEED * deltaTime;
                if (hitBoundary() || ropeLength >= rope.getMaxLen()) {
                    // 触达矿洞边界/最大绳长 → 空钩收回
                    ropeLength = Math.min(ropeLength, rope.getMaxLen());
                    state = HookState.RETRACTING;
                } else {
                    grabIfCollide(items);
                }
                break;
            case GRABBING:
                // 收回速度 = 抓取瞬间绳长 / 物品收回耗时（全程匀速，强力药水生效 ×2）
                ropeLength -= carriedRetractSpeed() * deltaTime;
                dragItem();
                if (ropeLength <= SWING_LENGTH) {
                    ropeLength = SWING_LENGTH;
                    state = HookState.SWINGING;
                }
                break;
            case RETRACTING:
                // 空钩固定 800px/s（强力药水生效 ×2）
                double emptySpeed = GameConfig.HOOK_EMPTY_RETRACT_SPEED
                        * (speedBoostTimer > 0 ? GameConfig.HOOK_SPEED_BOOST_MULTIPLIER : 1.0);
                ropeLength -= emptySpeed * deltaTime;
                if (ropeLength <= SWING_LENGTH) {
                    ropeLength = SWING_LENGTH;
                    state = HookState.SWINGING;
                }
                break;
            case STUNNED:
                // 碰撞点冻结，绳长不变、无法操作；倒计时结束自动空钩收回
                stunTimer -= deltaTime;
                if (stunTimer <= 0) {
                    state = HookState.RETRACTING;
                }
                break;
            case FROZEN:
                // FR-16 冰冻箱：运动完全暂停、绳长/物品不动、无法操作；倒计时结束恢复冻结前状态
                frozenTimer -= deltaTime;
                if (frozenTimer <= 0) {
                    state = preFrozenState;
                }
                break;
        }
        rope.setCurrentLen(ropeLength);
    }

    /** 抛出过程中尝试碰撞：鼹鼠偏转 / 钻石猪概率捕获 / TNT立即爆炸 / 普通物品附着 */
    private void grabIfCollide(List<Item> items) {
        long nowNano = System.nanoTime();
        for (Item item : items) {
            if (item.isGrabbed()) continue;
            if (!tipHits(item)) continue;

            // 鼹鼠：THROWING 钩爪碰撞后运动角度随机偏移 ±15°~±30°，不被抓取，继续飞行
            // （RETRACT/GRABBING 状态不受影响，也走不到本方法）
            if (item instanceof Mole) {
                if (item == lastInteractMob && nowNano - lastInteractNano < DEFLECT_COOLDOWN_NS) {
                    continue;
                }
                double deg = GameConfig.MOLE_DEFLECT_MIN_DEG
                        + Math.random() * (GameConfig.MOLE_DEFLECT_MAX_DEG - GameConfig.MOLE_DEFLECT_MIN_DEG);
                double sign = ThreadLocalRandom.current().nextBoolean() ? 1 : -1;
                angle += sign * Math.toRadians(deg);
                lastInteractMob = item;
                lastInteractNano = nowNano;
                continue;
            }

            // 钻石猪（FR-11）：碰撞时仅约 40% 概率被捕获；逃脱则立即冲刺并进入短暂冷却
            if (item instanceof DiamondPig) {
                if (item == lastInteractMob && nowNano - lastInteractNano < PIG_ESCAPE_COOLDOWN_NS) {
                    continue;
                }
                DiamondPig pig = (DiamondPig) item;
                if (!pig.tryCapture()) {
                    lastInteractMob = item;
                    lastInteractNano = nowNano;
                    continue;
                }
                // 捕获成功：走下方通用附着流程
            }

            item.onGrab(this);

            // TNT：立即爆炸，清除 150px 半径内环境物品；不计分、不扣金币、不眩晕，当前钩爪立即空钩收回
            if (item instanceof Bomb) {
                ((Bomb) item).triggerExplode();
                double bx = item.getX(), by = item.getY();
                Iterator<Item> it = items.iterator();
                while (it.hasNext()) {
                    Item target = it.next();
                    if (target == item || target instanceof Bomb) continue;
                    double dx = target.getX() - bx;
                    double dy = target.getY() - by;
                    if (Math.hypot(dx, dy) <= GameConfig.TNT_EXPLOSION_RADIUS) {
                        it.remove();
                    }
                }
                items.remove(item);
                grabbedItem = null;
                state = HookState.RETRACTING;
                return;
            }

            // 普通物品：立即附着钩尖，记录抓取瞬间绳长/时间/物品原位，进入携带收回
            grabbedItem = item;
            item.setGrabbed(true);
            grabRopeLength = ropeLength;
            grabTimestampMs = System.currentTimeMillis();
            grabOriginX = item.getX();
            grabOriginY = item.getY();
            state = HookState.GRABBING;
            return;
        }
    }

    /** 携带物品收回时，物品跟随钩尖移动 */
    private void dragItem() {
        if (grabbedItem == null) return;
        double[] tip = hookTip();
        grabbedItem.setX(tip[0]);
        grabbedItem.setY(tip[1]);
    }

    /**
     * 携带物品收回速度（FR-04/FR-10）：
     * 速度 = 抓取瞬间绳长 / 该物品 getRetractDuration() 指定耗时；
     * 强力药水生效期间（FR-16）整体 ×2。
     */
    private double carriedRetractSpeed() {
        double duration = grabbedItem == null
                ? GameConfig.RETRACT_TIME_LIGHT
                : grabbedItem.getRetractDuration();
        double speed = grabRopeLength / duration;
        if (speedBoostTimer > 0) {
            speed *= GameConfig.HOOK_SPEED_BOOST_MULTIPLIER;
        }
        return speed;
    }

    /** 钩尖是否触达矿洞边界（左/右/下） */
    private boolean hitBoundary() {
        double[] tip = hookTip();
        return tip[0] <= GameConfig.MINE_MIN_X
                || tip[0] >= GameConfig.MINE_MAX_X
                || tip[1] >= GameConfig.MINE_MAX_Y;
    }

    /** 钩尖坐标 */
    private double[] hookTip() {
        double anchorX = playerId == 1 ? GameConfig.HOOK_ANCHOR_X_P1 : GameConfig.HOOK_ANCHOR_X_P2;
        double anchorY = GameConfig.HOOK_ANCHOR_Y;
        return new double[]{
                anchorX + Math.cos(angle) * ropeLength,
                anchorY + Math.sin(angle) * ropeLength
        };
    }

    /** 进入眩晕：碰撞点冻结2秒，松开携带物品（归属/原位由 GameManager 处理），结束自动空钩收回 */
    @Override
    public void stun() {
        this.grabbedItem = null;
        this.stunTimer = GameConfig.HOOK_STUN_DURATION_SEC;
        this.state = HookState.STUNNED;
    }

    /** 炸药：炸毁携带物品并立即空钩收回；无携带返回 null */
    @Override
    public Item detachCarriedItem() {
        if (state != HookState.GRABBING || grabbedItem == null) {
            return null;
        }
        Item carried = grabbedItem;
        grabbedItem = null;
        carried.setGrabbed(false);
        state = HookState.RETRACTING;
        return carried;
    }

    // ===== FR-16 冰冻箱 / 强力药水 =====

    /**
     * 冰冻箱：进入 FROZEN，运动完全暂停 seconds 秒，解冻后恢复冻结前状态。
     * 再次冻结仅刷新倒计时（FR-18 刷新精神，不叠加）。
     */
    @Override
    public void freeze(double seconds) {
        if (state == HookState.FROZEN) {
            frozenTimer = seconds;
            return;
        }
        preFrozenState = state;
        frozenTimer = seconds;
        state = HookState.FROZEN;
    }

    @Override public boolean isFrozen() { return state == HookState.FROZEN; }

    @Override public double getFreezeRemaining() {
        return state == HookState.FROZEN ? Math.max(0, frozenTimer) : 0;
    }

    /**
     * 强力药水：收回速度 ×2 持续 seconds 秒；
     * 已生效时再次使用仅把剩余时间重置为 seconds（FR-18：刷新持续时间，不叠加倍率）。
     */
    @Override
    public void applySpeedBoost(double seconds) {
        this.speedBoostTimer = seconds;
    }

    @Override public boolean isSpeedBoostActive() { return speedBoostTimer > 0; }

    @Override public double getSpeedBoostRemaining() {
        return Math.max(0, speedBoostTimer);
    }

    /** 该钩子当前是否携带此物品（供 GameManager 结算归属） */
    @Override
    public boolean ownsItem(Item item) {
        return grabbedItem == item;
    }

    @Override public HookState getState() { return state; }
    @Override public double getAngle() { return angle; }
    @Override public double getRopeLength() { return ropeLength; }
    @Override public int getPlayerId() { return playerId; }
    @Override public Rope getRope() { return rope; }
    @Override public void setState(HookState state) { this.state = state; }

    // ===== 渲染读取（UI 层 GameView 使用） =====
    @Override public double getX() { return hookTip()[0]; }
    @Override public double getY() { return hookTip()[1]; }
    @Override public double getStartX() {
        return playerId == 1 ? GameConfig.HOOK_ANCHOR_X_P1 : GameConfig.HOOK_ANCHOR_X_P2;
    }
    @Override public double getStartY() { return GameConfig.HOOK_ANCHOR_Y; }

    // ===== 抢夺/眩晕/炸药读取 =====
    @Override public Item getGrabbedItem() { return grabbedItem; }
    @Override public long getGrabTimestampMs() { return grabTimestampMs; }
    @Override public double getGrabOriginX() { return grabOriginX; }
    @Override public double getGrabOriginY() { return grabOriginY; }

    // ===== 结算瞬时飘字（融合 feature/item） =====
    /** 设置普通分数飘字（durationMs 毫秒后失效） */
    public void setSettleLabel(String text, long durationMs) {
        this.settleLabel = text;
        this.settleIcon = null;
        this.settleLabelUntil = System.currentTimeMillis() + durationMs;
    }

    /** 设置福袋道具飘字（带道具类型，渲染时显示道具名） */
    public void setSettleLabel(String text, Main.config.GameConfig.MysteryReward icon, long durationMs) {
        this.settleLabel = text;
        this.settleIcon = icon;
        this.settleLabelUntil = System.currentTimeMillis() + durationMs;
    }

    @Override public String getSettleLabel() { return settleLabel; }
    @Override public long getSettleLabelUntil() { return settleLabelUntil; }
    @Override public Main.config.GameConfig.MysteryReward getSettleIcon() { return settleIcon; }
}
