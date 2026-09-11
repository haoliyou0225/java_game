// FR-UI DummyDataFactory：临时假数据工厂（假矿洞/假钩爪），来自 feature_ui 分支，已适配 hook 分支 Hook/Item 接口
package Main.model;

import Main.config.Config;

import java.util.ArrayList;
import java.util.List;

/**
 * 临时假数据工厂（P0 占位，feature_ui 分支）。
 * <p>
 * 所有假物品/假钩爪/假矿洞集中在本类，GameModelImpl 仅负责组装调用；
 * 真实物品生成与真实钩爪物理接入时只需替换本类的实现
 * 或改为依赖注入，不再改动 GameModelImpl，降低三方合并冲突风险。
 * <p>
 * 适配说明：DummyItem/DummyHook 实现的是 hook 分支的 Item/Hook 接口，
 * 物理方法为空实现，仅提供渲染所需的位置/状态读取。
 */
public final class DummyDataFactory {

    private DummyDataFactory() {
        // 工厂类禁止实例化
    }

    /**
     * 创建假矿洞地图（P0 占位，FR-28）。
     * 左右半区分布 5 个假物品；边界与 1280x720 场景尺寸对应。
     *
     * @return 假 MineMap 实例
     */
    public static MineMap createDummyMineMap() {
        return new MineMap() {
            private final List<Item> dummyItems = new ArrayList<>();

            {
                // 左右半区分布（贴合效果图：矿洞中部）
                dummyItems.add(new DummyItem(350, 320, 20, 100));
                dummyItems.add(new DummyItem(560, 420, 25, 200));
                dummyItems.add(new DummyItem(760, 360, 30, 400));
                dummyItems.add(new DummyItem(250, 520, 22, 150));
                dummyItems.add(new DummyItem(1020, 500, 26, 300));
            }

            @Override
            public List<Item> getItems() {
                return dummyItems;
            }

            @Override
            public void generate() {
                // 假实现：真实物品生成逻辑（FR-01）接入后替换
            }

            /**
             * 注意：假数据恒定返回 0，未实现真实均衡统计；
             * 真实物品生成接入后必须同步实现左右半区价值统计。
             */
            @Override
            public int getLeftTotalValue() {
                return 0;
            }

            @Override
            public int getRightTotalValue() {
                return 0;
            }

            @Override
            public double getMinX() {
                return 50;
            }

            @Override
            public double getMinY() {
                return 160;
            }

            @Override
            public double getMaxX() {
                return 1230;
            }

            @Override
            public double getMaxY() {
                return 700;
            }
        };
    }

    /**
     * 创建玩家1假钩爪（P0 占位）：起点取自 Config，斜向下 45°，长度 113。
     *
     * @return 玩家1假 Hook 实例
     */
    public static Hook createDummyHook1() {
        return new DummyHook(1, Config.HOOK1_START_X, Config.HOOK_START_Y,
                400, 220, Math.toRadians(45), 113);
    }

    /**
     * 创建玩家2假钩爪（P0 占位）：起点取自 Config，斜向下 135°，长度 113。
     *
     * @return 玩家2假 Hook 实例
     */
    public static Hook createDummyHook2() {
        return new DummyHook(2, Config.HOOK2_START_X, Config.HOOK_START_Y,
                880, 220, Math.toRadians(135), 113);
    }

    /** 内部假物品：位置/半径/分值固定，重量统一 1.0（P0 无抓取物理） */
    private static class DummyItem implements Item {
        private final double x;
        private final double y;
        private final double radius;
        private final int score;

        DummyItem(double x, double y, double radius, int score) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.score = score;
        }

        @Override public void onGrab(Hook hook) {
            // 假实现：真实抓取行为接入后替换
        }

        @Override public void updatePosition() {
            // 假实现：静止物品无需移动
        }

        @Override public double getX() { return x; }
        @Override public double getY() { return y; }
        @Override public int getScore() { return score; }
        @Override public double getWeight() { return 1.0; }
        @Override public double getRadius() { return radius; }
        @Override public boolean isGrabbed() { return false; }
        @Override public void setX(double x) {
            // 假实现：位置固定
        }
        @Override public void setY(double y) {
            // 假实现：位置固定
        }
        @Override public void setGrabbed(boolean grabbed) {
            // 假实现：真实抓取状态接入后维护
        }
    }

    /** 内部假钩爪：位置固定（P0 无物理），仅状态可变；物理方法均为空实现 */
    private static class DummyHook implements Hook {
        private final int playerId;
        private final double startX;
        private final double startY;
        private final double x;
        private final double y;
        private final double angle;
        private final double ropeLength;

        /**
         * FR-18：可变的钩爪状态字段。
         * 后台定时线程写入、JavaFX 渲染/输入线程读取，故用 volatile 保证内存可见性。
         */
        private volatile HookState state;

        DummyHook(int playerId, double startX, double startY,
                  double x, double y, double angle, double ropeLength) {
            this.playerId = playerId;
            this.startX = startX;
            this.startY = startY;
            this.x = x;
            this.y = y;
            this.angle = angle;
            this.ropeLength = ropeLength;
            this.state = HookState.SWINGING; // 初始为摇摆中
        }

        // ===== 钩子物理方法（假实现，空操作） =====
        @Override public void updateSwing(double deltaTime) { }
        @Override public void throwHook() { }
        @Override public void retractHook() { }
        @Override
        public CollisionResult checkCollisionItem(Item item) {
            return new CollisionResultImpl(false, null);
        }
        @Override public boolean checkCollisionOtherHook(Hook other) { return false; }
        @Override public void update(double deltaTime, List<Item> items, Hook otherHook) { }
        @Override public boolean ownsItem(Item item) { return false; }

        // ===== 状态/角度读取 =====
        @Override public HookState getState() { return state; }
        @Override public double getAngle() { return angle; }
        @Override public double getRopeLength() { return ropeLength; }
        @Override public int getPlayerId() { return playerId; }
        @Override public Rope getRope() { return null; }
        @Override public void setState(HookState state) { this.state = state; }

        // ===== 渲染坐标读取（UI 层 GameView 使用） =====
        @Override public double getX() { return x; }
        @Override public double getY() { return y; }
        @Override public double getStartX() { return startX; }
        @Override public double getStartY() { return startY; }
        /** 假钩爪始终为直绳，无折点 */
        @Override public List<double[]> getBendPoints() { return List.of(); }

        // ===== 抢夺/眩晕/炸药（假实现，空操作/默认值） =====
        @Override public Item getGrabbedItem() { return null; }
        @Override public boolean tipHits(Item item) { return false; }
        @Override public long getGrabTimestampMs() { return 0L; }
        @Override public double getGrabOriginX() { return 0; }
        @Override public double getGrabOriginY() { return 0; }
        @Override public void stun() { }
        @Override public Item detachCarriedItem() { return null; }
        // FR-16 冰冻箱 / 强力药水（假实现，空操作/默认值）
        @Override public void freeze(double seconds) { }
        @Override public boolean isFrozen() { return false; }
        @Override public double getFreezeRemaining() { return 0; }
        @Override public void applySpeedBoost(double seconds) { }
        @Override public boolean isSpeedBoostActive() { return false; }
        @Override public double getSpeedBoostRemaining() { return 0; }
        // 结算飘字（DummyHook 不产生飘字）
        @Override public String getSettleLabel() { return null; }
        @Override public long getSettleLabelUntil() { return 0; }
        @Override public Main.config.GameConfig.MysteryReward getSettleIcon() { return null; }
    }
}
