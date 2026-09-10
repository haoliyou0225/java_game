// FR-05 碰撞检测结果封装：isHit 是否命中 + hitItem 命中的物品实体
package com.dz.hello.Main.model;

public class CollisionResult {
    private boolean isHit;
    private Item hitItem;

    public CollisionResult(boolean isHit, Item hitItem) {
        this.isHit = isHit;
        this.hitItem = hitItem;
    }

    public boolean isHit() { return isHit; }
    public Item getHitItem() { return hitItem; }
}
