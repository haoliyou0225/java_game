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
    private Item grabbedItem;  // 当前携带的物品（内部状态）

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
                grabbedItem = item;
                item.setGrabbed(true);
                item.onGrab(this);
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
        double anchorX = playerId == 1 ? GameConfig.HOOK_ANCHOR_X_P1 : GameConfig.HOOK_ANCHOR_X_P2;
        double anchorY = GameConfig.HOOK_ANCHOR_Y;
        return new double[]{
                anchorX + Math.cos(angle) * ropeLength,
                anchorY + Math.sin(angle) * ropeLength
        };
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
}
