// FR-13 关卡实现：generateSceneItems() 随机生成 Gold/Diamond/Stone/Bomb 混合场景
package Main.model;

import Main.config.Config;
import Main.config.GameConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LevelImpl implements Level {

    /** 物品距锚点最近距离（像素） */
    private static final double ITEM_MIN_DIST = 100;
    /** 物品距锚点最远距离（像素，需小于绳索最大长度） */
    private static final double ITEM_MAX_DIST = 420;
    /** 物品角度相对垂直向下的最大偏移（弧度，朝内侧，需小于钩子摆动范围 1.25） */
    private static final double ITEM_ANGLE_MAX_OFFSET = 1.25;
    /** 物品角度朝画布外侧的最大偏移（弧度，按画布边界收窄防止出界） */
    private static final double ITEM_ANGLE_EDGE_OFFSET = 0.65;
    /** 物品 Y 坐标起始基准：与钩子锚点 Y 对齐，物品从锚点正下方开始分布 */
    private static final double ITEM_Y_OFFSET = GameConfig.HOOK_ANCHOR_Y;

    private List<Item> itemPool;

    public LevelImpl() {
        this.itemPool = new ArrayList<>();
    }

    /** 随机生成混合物品场景（保证位于钩子可及范围内，并通过公平性校验） */
    @Override
    public List<Item> generateSceneItems() {
        Random rnd = new Random();
        // FR-13 公平性校验：最多重试 MAP_FAIRNESS_MAX_RETRY 次，通过则返回；否则保留最优结果
        List<Item> best = null;
        double bestDiff = Double.MAX_VALUE;
        for (int attempt = 0; attempt < GameConfig.MAP_FAIRNESS_MAX_RETRY; attempt++) {
            itemPool = generateOnce(rnd);
            if (isMapFair(itemPool)) {
                return itemPool;
            }
            // 记录价值差最小的一次作为兜底（避免极端情况下重试耗尽无结果）
            double diff = valueDiffPercent(itemPool);
            if (diff < bestDiff) {
                bestDiff = diff;
                best = new ArrayList<>(itemPool);
            }
        }
        // 重试耗尽：返回最优结果（仍可能不严格满足，但已是最接近公平的方案）
        itemPool = best;
        return itemPool;
    }

    /** 单次生成物品场景（不含公平性校验） */
    private List<Item> generateOnce(Random rnd) {
        List<Item> pool = new ArrayList<>();
        int itemCount = GameConfig.SCENE_ITEM_MIN_COUNT
                + rnd.nextInt(GameConfig.SCENE_ITEM_MAX_COUNT - GameConfig.SCENE_ITEM_MIN_COUNT + 1);
        for (int i = 0; i < itemCount; i++) {
            // 随机选择一个钩子锚点作为分布圆心
            boolean leftSide = rnd.nextBoolean();
            double centerX = leftSide
                    ? GameConfig.HOOK_ANCHOR_X_P1
                    : GameConfig.HOOK_ANCHOR_X_P2;
            // 按极坐标生成，角度限制在钩子摆动范围内；朝内侧摆幅大、朝外侧收窄防止出界
            double low = leftSide ? -ITEM_ANGLE_MAX_OFFSET : -ITEM_ANGLE_EDGE_OFFSET;
            double high = leftSide ? ITEM_ANGLE_EDGE_OFFSET : ITEM_ANGLE_MAX_OFFSET;
            double dist = ITEM_MIN_DIST + rnd.nextDouble() * (ITEM_MAX_DIST - ITEM_MIN_DIST);
            double a = Math.PI / 2 + low + rnd.nextDouble() * (high - low);
            double x = centerX + Math.cos(a) * dist;
            double y = ITEM_Y_OFFSET + Math.sin(a) * dist;
            pool.add(createRandomItem(rnd, x, y));
        }
        return pool;
    }

    /**
     * FR-13 地图公平性校验：
     * 1) 左右两侧普通物品价值差 ≤ MAP_MAX_VALUE_DIFF_PERCENT（福袋/炸弹不计入价值）
     * 2) 福袋与炸弹两侧数量差 ≤ MAP_SPECIAL_ITEM_MAX_DIFF
     */
    private boolean isMapFair(List<Item> items) {
        // 中线取画布宽度的一半
        double midX = Config.WIDTH / 2.0;
        int leftValue = 0, rightValue = 0;
        int leftSpecial = 0, rightSpecial = 0;
        for (Item item : items) {
            boolean onLeft = item.getX() < midX;
            boolean isSpecial = item instanceof MysteryBag || item instanceof Bomb;
            if (isSpecial) {
                if (onLeft) leftSpecial++; else rightSpecial++;
            } else {
                int score = item.getScore();
                if (onLeft) leftValue += score; else rightValue += score;
            }
        }
        // 特殊物品两侧数量差校验
        if (Math.abs(leftSpecial - rightSpecial) > GameConfig.MAP_SPECIAL_ITEM_MAX_DIFF) {
            return false;
        }
        // 价值差百分比校验（一侧为 0 时若另一侧非 0 直接判不公平）
        int maxV = Math.max(leftValue, rightValue);
        int minV = Math.min(leftValue, rightValue);
        if (maxV == 0) {
            return minV == 0; // 两侧都为 0 视为公平
        }
        double diffPercent = (maxV - minV) * 100.0 / maxV;
        return diffPercent <= GameConfig.MAP_MAX_VALUE_DIFF_PERCENT;
    }

    /** 计算左右两侧普通物品价值差百分比（用于兜底择优，不含福袋/炸弹） */
    private double valueDiffPercent(List<Item> items) {
        double midX = Config.WIDTH / 2.0;
        int leftValue = 0, rightValue = 0;
        for (Item item : items) {
            boolean isSpecial = item instanceof MysteryBag || item instanceof Bomb;
            if (isSpecial) continue;
            if (item.getX() < midX) leftValue += item.getScore();
            else rightValue += item.getScore();
        }
        int maxV = Math.max(leftValue, rightValue);
        int minV = Math.min(leftValue, rightValue);
        if (maxV == 0) return 0.0;
        return (maxV - minV) * 100.0 / maxV;
    }

    /** 随机生成一种物品（贴近原版 7 种：金块/大金块/钻石/石头/炸弹/福袋/鼢鼠） */
    private Item createRandomItem(Random rnd, double x, double y) {
        switch (rnd.nextInt(7)) {
            case 0: return new Gold(x, y);
            case 1: return new BigGold(x, y);
            case 2: return new Diamond(x, y);
            case 3: return new Stone(x, y);
            case 4: return new Bomb(x, y);
            case 5: return new MysteryBag(x, y);
            default: return new Mole(x, y);
        }
    }

    @Override public List<Item> getItemPool() { return itemPool; }
}
