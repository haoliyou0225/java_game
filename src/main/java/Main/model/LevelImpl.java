// FR-01 公平地图生成：每局 25~30 个物品，全图随机分布禁止镜像对称
// 左右半区普通物品总价值差 ≤5%，左右福袋数量差 ≤1，生成阶段完成公平性校验
package Main.model;

import Main.config.Config;
import Main.config.GameConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LevelImpl implements Level {

    /** 物品分布区域（矿洞内，避免被地面条遮挡或贴边） */
    private static final double BOUND_X_MIN = 70;
    private static final double BOUND_X_MAX = 1210;
    private static final double BOUND_Y_MIN = 165;
    private static final double BOUND_Y_MAX = 640;
    /** 物品之间最小圆心距（防止重叠，最大半径 28） */
    private static final double MIN_ITEM_GAP = 48;
    /** 单个物品寻找合法落点的最大尝试次数 */
    private static final int PLACE_MAX_TRY = 80;

    private List<Item> itemPool;

    public LevelImpl() {
        this.itemPool = new ArrayList<>();
    }

    /**
     * FR-01 生成公平矿洞地图：
     * 全图随机生成 25~30 个物品（天然非镜像对称），随后做公平性校验，
     * 最多重试 MAP_FAIRNESS_MAX_RETRY 次，通过即返回；耗尽则返回价值差最小的兜底方案。
     */
    @Override
    public List<Item> generateSceneItems() {
        Random rnd = new Random();
        List<Item> best = null;
        double bestDiff = Double.MAX_VALUE;
        for (int attempt = 0; attempt < GameConfig.MAP_FAIRNESS_MAX_RETRY; attempt++) {
            List<Item> pool = generateOnce(rnd);
            if (pool.size() < GameConfig.SCENE_ITEM_MIN_COUNT) {
                continue; // 物品数量不足（极端落点冲突），本次作废重试
            }
            if (isMapFair(pool)) {
                this.itemPool = pool;
                return pool;
            }
            double diff = valueDiffPercent(pool);
            if (diff < bestDiff) {
                bestDiff = diff;
                best = new ArrayList<>(pool);
            }
        }
        // 重试耗尽：返回最接近公平的一版
        this.itemPool = best != null ? best : new ArrayList<>();
        return itemPool;
    }

    /** 单次全图随机生成（无镜像、无对称），每个落点校验钩爪可达性与间距 */
    private List<Item> generateOnce(Random rnd) {
        List<Item> pool = new ArrayList<>();
        int itemCount = GameConfig.SCENE_ITEM_MIN_COUNT
                + rnd.nextInt(GameConfig.SCENE_ITEM_MAX_COUNT - GameConfig.SCENE_ITEM_MIN_COUNT + 1);
        for (int i = 0; i < itemCount; i++) {
            Item placed = null;
            for (int t = 0; t < PLACE_MAX_TRY; t++) {
                // 全图均匀随机（独立 x/y，不做镜像配对），禁止镜像对称
                double x = BOUND_X_MIN + rnd.nextDouble() * (BOUND_X_MAX - BOUND_X_MIN);
                double y = BOUND_Y_MIN + rnd.nextDouble() * (BOUND_Y_MAX - BOUND_Y_MIN);
                if (!isReachable(x, y)) continue;          // 至少一条钩爪能在摆角/绳长内够到
                if (tooClose(pool, x, y)) continue;        // 不与已有物品重叠
                placed = createRandomItem(rnd, x, y);
                break;
            }
            if (placed != null) {
                pool.add(placed);
            }
        }
        return pool;
    }

    /**
     * 可达性校验：物品必须位于至少一条钩爪的钟摆角度范围（±1.25rad）内，
     * 且距锚点不超过绳索最大长度，保证全图物品理论上都可被抓取。
     */
    private boolean isReachable(double x, double y) {
        double[] anchors = {GameConfig.HOOK_ANCHOR_X_P1, GameConfig.HOOK_ANCHOR_X_P2};
        double minAngle = Math.PI / 2 - GameConfig.HOOK_SWING_MAX_OFFSET;
        double maxAngle = Math.PI / 2 + GameConfig.HOOK_SWING_MAX_OFFSET;
        for (double ax : anchors) {
            double dx = x - ax;
            double dy = y - GameConfig.HOOK_ANCHOR_Y;
            double dist = Math.sqrt(dx * dx + dy * dy);
            if (dist > GameConfig.ROPE_MAX_EXTEND_LENGTH - 30) continue;
            double angle = Math.atan2(dy, dx);
            if (angle >= minAngle && angle <= maxAngle) {
                return true;
            }
        }
        return false;
    }

    /** 与已放置物品的最小间距校验 */
    private boolean tooClose(List<Item> pool, double x, double y) {
        for (Item it : pool) {
            double dx = it.getX() - x;
            double dy = it.getY() - y;
            if (Math.sqrt(dx * dx + dy * dy) < MIN_ITEM_GAP) {
                return true;
            }
        }
        return false;
    }

    /**
     * FR-01 公平性校验：
     * 1) 左右半区物品总价值差百分比 ≤5%（福袋 100~800 金币、钻石猪颗数×600+10
     *    均在生成时预计算，计入总价值；炸弹为负且不计分，排除）
     * 2) 左右半区福袋数量差 ≤1
     */
    private boolean isMapFair(List<Item> items) {
        double midX = Config.WIDTH / 2.0;
        int leftValue = 0, rightValue = 0;
        int leftBags = 0, rightBags = 0;
        for (Item item : items) {
            boolean onLeft = item.getX() < midX;
            if (item instanceof MysteryBag) {
                if (onLeft) leftBags++; else rightBags++;
            }
            if (item instanceof Bomb) {
                // 炸弹不计分、不计入价值
                continue;
            }
            if (onLeft) leftValue += item.getScore(); else rightValue += item.getScore();
        }
        if (Math.abs(leftBags - rightBags) > GameConfig.MAP_BAG_MAX_DIFF) {
            return false;
        }
        int maxV = Math.max(leftValue, rightValue);
        int minV = Math.min(leftValue, rightValue);
        if (maxV == 0) {
            return minV == 0;
        }
        return (maxV - minV) * 100.0 / maxV <= GameConfig.MAP_MAX_VALUE_DIFF_PERCENT;
    }

    /** 左右半区物品总价值差百分比（兜底择优用，口径与 isMapFair 一致） */
    private double valueDiffPercent(List<Item> items) {
        double midX = Config.WIDTH / 2.0;
        int leftValue = 0, rightValue = 0;
        for (Item item : items) {
            if (item instanceof Bomb) continue;
            if (item.getX() < midX) leftValue += item.getScore();
            else rightValue += item.getScore();
        }
        int maxV = Math.max(leftValue, rightValue);
        int minV = Math.min(leftValue, rightValue);
        if (maxV == 0) return 0.0;
        return (maxV - minV) * 100.0 / maxV;
    }

    /**
     * 加权随机生成一种物品（FR-10 全 9 品类）：
     * 小金块20 / 中金块15 / 大金块10 / 钻石8 / 石头22 / 炸弹8 / 福袋7 / 鼹鼠5 / 钻石猪5（百分比）。
     * 低值矿石铺量、高值目标稀有，贴近原版手感。
     */
    private Item createRandomItem(Random rnd, double x, double y) {
        int r = rnd.nextInt(100);
        if (r < 20) {
            return new Gold(x, y);           // <20
        } else if (r < 35) {
            return new MediumGold(x, y);     // 20~34
        } else if (r < 45) {
            return new BigGold(x, y);        // 35~44
        } else if (r < 53) {
            return new Diamond(x, y);        // 45~52
        } else if (r < 75) {
            return new Stone(x, y);          // 53~74
        } else if (r < 83) {
            return new Bomb(x, y);           // 75~82
        } else if (r < 90) {
            return new MysteryBag(x, y);     // 83~89
        } else if (r < 95) {
            return new Mole(x, y);           // 90~94
        } else {
            return new DiamondPig(x, y);     // 95~99
        }
    }

    @Override public List<Item> getItemPool() { return itemPool; }
}
