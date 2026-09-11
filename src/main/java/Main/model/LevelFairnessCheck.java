// FR-01 公平地图验收自检：物品数量 25~30、左右半区价值差 ≤5%、福袋数量差 ≤1、
// 全部物品落在矿洞边界内且互不重叠、9 类物品均可生成。
// 无 JUnit 依赖，直接 java Main.model.LevelFairnessCheck 运行。
package Main.model;

import Main.config.Config;
import Main.config.GameConfig;
import Main.util.SelfCheck;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LevelFairnessCheck extends SelfCheck {

    public static void main(String[] args) {
        LevelFairnessCheck check = new LevelFairnessCheck();
        check.satisfiesAllFairnessRules();
        check.itemsDoNotOverlap();
        check.allNineItemTypesCanSpawn();
        check.finish();
    }

    /** 重复生成 20 局地图，每局都必须满足全部公平性约束 */
    private void satisfiesAllFairnessRules() {
        for (int round = 0; round < 20; round++) {
            Level level = new LevelImpl();
            List<Item> items = level.generateSceneItems();

            // FR-01：每局物品数量 25~30
            checkTrue(items.size() >= GameConfig.SCENE_ITEM_MIN_COUNT,
                    "第" + round + "局 物品数量不应少于 " + GameConfig.SCENE_ITEM_MIN_COUNT
                            + "，实际 " + items.size());
            checkTrue(items.size() <= GameConfig.SCENE_ITEM_MAX_COUNT,
                    "第" + round + "局 物品数量不应多于 " + GameConfig.SCENE_ITEM_MAX_COUNT
                            + "，实际 " + items.size());

            double midX = Config.WIDTH / 2.0;
            int leftValue = 0, rightValue = 0;
            int leftBags = 0, rightBags = 0;

            for (Item item : items) {
                // 所有物品必须落在矿洞活动边界内
                checkTrue(item.getX() >= GameConfig.MINE_MIN_X && item.getX() <= GameConfig.MINE_MAX_X,
                        "第" + round + "局 物品越界 x=" + item.getX());
                checkTrue(item.getY() <= GameConfig.MINE_MAX_Y,
                        "第" + round + "局 物品越界 y=" + item.getY());

                boolean onLeft = item.getX() < midX;
                if (item instanceof MysteryBag) {
                    if (onLeft) leftBags++; else rightBags++;
                }
                // 炸弹不计分，排除在总价值外
                if (item instanceof Bomb) continue;
                if (onLeft) leftValue += item.getScore();
                else rightValue += item.getScore();
            }

            // FR-01：左右福袋数量差 ≤1
            checkTrue(Math.abs(leftBags - rightBags) <= GameConfig.MAP_BAG_MAX_DIFF,
                    "第" + round + "局 福袋数量差 " + Math.abs(leftBags - rightBags)
                            + " 超过上限 " + GameConfig.MAP_BAG_MAX_DIFF);

            // FR-01：左右半区普通物品总价值差 ≤5%
            int maxV = Math.max(leftValue, rightValue);
            int minV = Math.min(leftValue, rightValue);
            double diffPercent = maxV == 0 ? 0.0 : (maxV - minV) * 100.0 / maxV;
            checkTrue(diffPercent <= GameConfig.MAP_MAX_VALUE_DIFF_PERCENT,
                    String.format("第%d局 左右价值差 %.2f%% 超过上限 %d%%（左=%d 右=%d）",
                            round, diffPercent, GameConfig.MAP_MAX_VALUE_DIFF_PERCENT,
                            leftValue, rightValue));
        }
    }

    /** 生成时物品互不重叠（圆心距 ≥ MIN_ITEM_GAP），重复 10 局 */
    private void itemsDoNotOverlap() {
        for (int round = 0; round < 10; round++) {
            List<Item> items = new LevelImpl().generateSceneItems();
            for (int i = 0; i < items.size(); i++) {
                for (int j = i + 1; j < items.size(); j++) {
                    double dx = items.get(i).getX() - items.get(j).getX();
                    double dy = items.get(i).getY() - items.get(j).getY();
                    double dist = Math.sqrt(dx * dx + dy * dy);
                    checkTrue(dist >= 47.0,
                            "第" + round + "局 物品重叠："
                                    + items.get(i).getClass().getSimpleName() + " 与 "
                                    + items.get(j).getClass().getSimpleName()
                                    + " 圆心距仅 " + dist);
                }
            }
        }
    }

    /** FR-10 全 9 类物品在足够多样本中均应出现（加权生成无死类） */
    private void allNineItemTypesCanSpawn() {
        Set<Class<?>> seen = new HashSet<>();
        for (int map = 0; map < 30; map++) {
            for (Item item : new LevelImpl().generateSceneItems()) {
                seen.add(item.getClass());
            }
        }
        Set<Class<?>> expected = Set.of(
                Gold.class, MediumGold.class, BigGold.class, Diamond.class,
                Stone.class, Bomb.class, MysteryBag.class, Mole.class, DiamondPig.class);
        checkEq(expected, seen, "加权生成应覆盖全部 9 类物品");
    }
}
