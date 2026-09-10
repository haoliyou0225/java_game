// FR-13 关卡：generateSceneItems() 随机生成 Gold/Diamond/Stone/Bomb 混合场景
package com.dz.hello.Main.model;

import com.dz.hello.Main.config.GameConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Level {

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
    /** 物品最低点距锚点的竖直偏移（像素） */
    private static final double ITEM_Y_OFFSET = 40;

    private List<Item> itemPool;

    public Level() {
        this.itemPool = new ArrayList<>();
    }

    /** 随机生成混合物品场景（保证位于钩子可及范围内） */
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

    /** 随机生成一种物品 */
    private Item createRandomItem(Random rnd, double x, double y) {
        switch (rnd.nextInt(4)) {
            case 0: return new Gold(x, y);
            case 1: return new Diamond(x, y);
            case 2: return new Stone(x, y);
            default: return new Bomb(x, y);
        }
    }

    public List<Item> getItemPool() { return itemPool; }
}
