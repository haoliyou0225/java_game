// FR-11 钩子核心实现：钟摆 updateSwing / 抛出 throwHook / 收回 retractHook / 与物品碰撞 checkCollisionItem / 与对钩碰撞 checkCollisionOtherHook
package Main.model;

import Main.config.GameConfig;
import Main.util.CollisionUtil;

import java.util.List;

/**
 * 钩子实体实现（对齐 UML）
 * 状态机：SWINGING 钟摆 → THROWING 抛出 → GRABBING 携带收回 / RETRACTING 空钩收回 → SWINGING
 */
public class HookImpl implements Hook {

    /** 钩尖碰撞半径（像素） */
    private static final double TIP_RADIUS = 15;
    /** 物品碰撞半径（像素） */
    private static final double ITEM_RADIUS = 15;
    /** 钟摆摆动时的基础绳长（像素，保证摆动弧线可见） */
    private static final double SWING_LENGTH = 70;
    /** 抛出速度（像素/秒） */
    private static final double THROW_SPEED = 500;
    /** 钟摆最大偏移角（相对垂直向下的弧度） */
    private static final double SWING_MAX_OFFSET = 1.25;

    private HookState state;
    private double angle;      // 弧度，PI/2 表示垂直向下
    private double ropeLength;
    private Rope rope;
    private int playerId;
    private int swingDir = 1;  // 摆动方向
<<<<<<< Updated upstream
    private Item grabbedItem;  // 当前携带的物品（内部状态）
=======
    private Item grabbedItem;  // 当前携带的物品

    /** 钩尖坐标复用缓冲（避免每次 hookTip 调用 new double[2]） */
    private final double[] tipBuf = new double[2];
    /** 锚点坐标复用缓冲（避免每次 anchorPoint 调用 new double[2]） */
    private final double[] anchorBuf = new double[2];

    /**
     * 绳路折点（按锚点向外顺序）。鼹鼠碰撞偏转时在碰撞瞬间的钩尖处记录一个折点，
     * 绳索呈 锚点→折点₁→…→折点ₙ→钩尖 的折线；ropeLength 始终表示沿该折线的
     * 累计路径长度。收回时沿折线原路折返，钩尖退过某折点即将其逆序移除。
     */
    private final List<double[]> bendPoints = new ArrayList<>();

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
>>>>>>> Stashed changes

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

    /** 钟摆摆动 */
    @Override
    public void updateSwing(double deltaTime) {
        angle += swingDir * GameConfig.HOOK_SWING_SPEED * deltaTime;
        if (angle > Math.PI / 2 + SWING_MAX_OFFSET) {
            angle = Math.PI / 2 + SWING_MAX_OFFSET;
            swingDir = -1;
        } else if (angle < Math.PI / 2 - SWING_MAX_OFFSET) {
            angle = Math.PI / 2 - SWING_MAX_OFFSET;
            swingDir = 1;
        }
    }

    /** 抛出钩子 */
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

    /** 与物品碰撞检测 */
    @Override
    public CollisionResult checkCollisionItem(Item item) {
        if (item == null || item.isGrabbed()) {
            return new CollisionResultImpl(false, null);
        }
        double[] tip = hookTip();
        boolean hit = CollisionUtil.circleCollision(
                tip[0], tip[1], TIP_RADIUS,
                item.getX(), item.getY(), ITEM_RADIUS);
        return new CollisionResultImpl(hit, hit ? item : null);
    }

    /** 与对方钩子碰撞检测 */
    @Override
    public boolean checkCollisionOtherHook(Hook other) {
        double[] t1 = hookTip();
        double[] t2 = ((HookImpl) other).hookTip();
        return CollisionUtil.circleCollision(t1[0], t1[1], TIP_RADIUS, t2[0], t2[1], TIP_RADIUS);
    }

    /** 每帧更新（由 GameManager 驱动） */
    @Override
    public void update(double deltaTime, List<Item> items, Hook otherHook) {
        switch (state) {
            case SWINGING:
                updateSwing(deltaTime);
                break;
            case THROWING:
                ropeLength += THROW_SPEED * deltaTime;
                if (ropeLength >= rope.getMaxLen()) {
                    ropeLength = rope.getMaxLen();
                    state = HookState.RETRACTING;
                } else {
                    grabIfCollide(items);
                }
                break;
            case GRABBING:
                ropeLength -= retractSpeed() * deltaTime;
                dragItem();
                if (ropeLength <= SWING_LENGTH) {
                    ropeLength = SWING_LENGTH;
                    state = HookState.SWINGING;
                }
                break;
            case RETRACTING:
                ropeLength -= GameConfig.HOOK_EMPTY_RETRACT_SPEED * deltaTime;
                if (ropeLength <= SWING_LENGTH) {
                    ropeLength = SWING_LENGTH;
                    state = HookState.SWINGING;
                }
                break;
        }
        rope.setCurrentLen(ropeLength);
    }

    /** 抛出过程中尝试抓取物品 */
    private void grabIfCollide(List<Item> items) {
        for (Item item : items) {
            CollisionResult result = checkCollisionItem(item);
            if (result.isHit()) {
                item.onGrab(this);
                if (item instanceof Bomb) {
                    // 炸弹：碰到立即爆炸（无参 triggerExplode，对齐原版 feature/item）
                    // 同时清除爆炸半径内所有普通物品
                    ((Bomb) item).triggerExplode();
                    double bx = item.getX(), by = item.getY();
                    java.util.Iterator<Item> it = items.iterator();
                    while (it.hasNext()) {
                        Item target = it.next();
                        if (target == item || target instanceof Bomb) continue;
                        double dx = target.getX() - bx;
                        double dy = target.getY() - by;
                        if (Math.sqrt(dx * dx + dy * dy) <= GameConfig.TNT_EXPLOSION_RADIUS) {
                            it.remove();
                        }
                    }
                    grabbedItem = null;
                    state = HookState.RETRACTING;
                    return;
                }
                // 普通物品：携带收回
                grabbedItem = item;
                item.setGrabbed(true);
                state = HookState.GRABBING;
                return;
            }
        }
    }

    /** 携带物品收回时，物品跟随钩尖移动 */
    private void dragItem() {
        if (grabbedItem == null) return;
        double[] tip = hookTip();
        grabbedItem.setX(tip[0]);
        grabbedItem.setY(tip[1]);
    }

    /** 带物品收回速度：物品越重收回越慢 */
    private double retractSpeed() {
        double weight = grabbedItem == null ? 1.0 : grabbedItem.getWeight();
        return Math.max(60, GameConfig.HOOK_WITH_ITEM_BASE_RETRACT_SPEED * 3.0 / (weight + 2.0));
    }

    /** 钩尖坐标 */
    private double[] hookTip() {
<<<<<<< Updated upstream
        double anchorX = playerId == 1 ? GameConfig.HOOK_ANCHOR_X_P1 : GameConfig.HOOK_ANCHOR_X_P2;
        double anchorY = GameConfig.HOOK_ANCHOR_Y;
        return new double[]{
                anchorX + Math.cos(angle) * ropeLength,
                anchorY + Math.sin(angle) * ropeLength
        };
    }

=======
        double[] anchor = anchorPoint();
        double px = anchor[0];
        double py = anchor[1];
        double remaining = ropeLength;
        for (double[] bend : bendPoints) {
            double segLen = Math.hypot(bend[0] - px, bend[1] - py);
            if (remaining <= segLen) {
                // 钩尖落在本段（上一顶点 → 该折点）上：收回折返经过这里
                double k = segLen == 0 ? 0 : remaining / segLen;
                tipBuf[0] = px + (bend[0] - px) * k;
                tipBuf[1] = py + (bend[1] - py) * k;
                return tipBuf;
            }
            remaining -= segLen;
            px = bend[0];
            py = bend[1];
        }
        // 最后一段沿当前飞行方向（鼹鼠偏转后即偏转方向）
        tipBuf[0] = px + Math.cos(angle) * remaining;
        tipBuf[1] = py + Math.sin(angle) * remaining;
        return tipBuf;
    }

    /** 锚点坐标 */
    private double[] anchorPoint() {
        anchorBuf[0] = playerId == 1 ? GameConfig.HOOK_ANCHOR_X_P1 : GameConfig.HOOK_ANCHOR_X_P2;
        anchorBuf[1] = GameConfig.HOOK_ANCHOR_Y;
        return anchorBuf;
    }

    /**
     * 收回时按原路折返：钩尖沿折线逐段退回，一旦退过某折点，
     * 移除该折点及其后所有折点，并把当前段方向恢复为该折点与上一顶点
     * （锚点或前一折点）的连线方向，保证绳索严格沿抛出路径退回锚点。
     */
    private void consumePassedBends() {
        double[] anchor = anchorPoint();
        double px = anchor[0];
        double py = anchor[1];
        double accumulated = 0;
        int keep = bendPoints.size();
        for (int i = 0; i < bendPoints.size(); i++) {
            double[] bend = bendPoints.get(i);
            double segLen = Math.hypot(bend[0] - px, bend[1] - py);
            if (ropeLength <= accumulated + segLen) {
                // 钩尖已退回折点 i 之前：消折 i 及其后折点，退回方向取本段原方向
                angle = Math.atan2(bend[1] - py, bend[0] - px);
                keep = i;
                break;
            }
            accumulated += segLen;
            px = bend[0];
            py = bend[1];
        }
        while (bendPoints.size() > keep) {
            bendPoints.remove(bendPoints.size() - 1);
        }
    }

    /**
     * 进入眩晕：碰撞点冻结2秒，携带物品随钩一起冻结；
     * 解冻时物品留在当前碰撞点（由 STUNNED 到期逻辑释放），钩子空钩收回。
     */
    @Override
    public void stun() {
        this.stunTimer = GameConfig.HOOK_STUN_DURATION_SEC;
        this.state = HookState.STUNNED;
    }

    /** 眩晕剩余秒数 */
    @Override
    public double getStunRemaining() {
        return stunTimer;
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

>>>>>>> Stashed changes
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
}
