// FR-UI MineMap：矿洞地图接口（物品列表/生成/左右半区价值/边界），来自 feature_ui 分支
package Main.model;

import java.util.List;

/**
 * 矿洞地图接口（FR-01、FR-28）
 * 负责物品生成、管理、左右半区价值均衡
 */
public interface MineMap {

    /** 获取所有物品列表（FR-28） */
    List<Item> getItems();

    /** 生成新地图（FR-01） */
    void generate();

    /** 左半区（x < 中线）物品总价值 */
    int getLeftTotalValue();

    /** 右半区（x > 中线）物品总价值 */
    int getRightTotalValue();

    /** 矿洞左边界（用于碰撞检测） */
    double getMinX();

    /** 矿洞上边界 */
    double getMinY();

    /** 矿洞右边界 */
    double getMaxX();

    /** 矿洞下边界 */
    double getMaxY();
}
