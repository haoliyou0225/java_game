// FR-05 碰撞检测结果封装实现：isHit 是否命中 + hitItem 命中的物品实体
package Main.model;

public class CollisionResultImpl implements CollisionResult {
    private boolean isHit;
    private Item hitItem;

    public CollisionResultImpl(boolean isHit, Item hitItem) {
        this.isHit = isHit;
        this.hitItem = hitItem;
    }

    @Override public boolean isHit() { return isHit; }
    @Override public Item getHitItem() { return hitItem; }
}
