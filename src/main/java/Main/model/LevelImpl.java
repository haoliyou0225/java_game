// FR-13 关卡实现：generateSceneItems() 随机生成 Gold/Diamond/Stone/Bomb 混合场景
package Main.model;

import Main.config.GameConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LevelImpl implements Level {

    /** 场景物品数量 */
    private static final int SCENE_ITEM_COUNT = 10;
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

    /** 随机生成混合物品场景（保证位于钩子可及范围内） */
    @Override
    public List<Item> generateSceneItems() {
        Random rnd = new Random();
        itemPool = new ArrayList<>();
        for (int i = 0; i < SCENE_ITEM_COUNT; i++) {
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
            itemPool.add(createRandomItem(rnd, x, y));
        }
        return itemPool;
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
