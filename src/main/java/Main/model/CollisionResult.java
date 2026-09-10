// FR-05 碰撞检测结果接口：isHit 是否命中 + hitItem 命中的物品实体
package Main.model;

public interface CollisionResult {
    boolean isHit();
    Item getHitItem();
}
