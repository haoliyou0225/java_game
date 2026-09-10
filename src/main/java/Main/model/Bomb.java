// FR-10 炸弹：原版 feature/item 分支实现，负分物品(-150)，可被触发爆炸清除周围物品
package Main.model;

public class Bomb extends ItemImpl {
    private boolean exploded = false;

    public Bomb(double x, double y) {
        super(x, y, -150, 1.5);
    }

    @Override
    public void onGrab(Hook hook) {
        this.grabbed = true; // 原版：onGrab 设 grabbed=true
    }

    @Override
    public void updatePosition() {
        // 炸弹静止
    }

    /**
     * 原版：无参 triggerExplode，只标记已爆炸。
     * 爆炸半径内物品的清除由 GameModelImpl/gameLoopTick 负责。
     */
    public void triggerExplode() {
        exploded = true;
    }

    public boolean isExploded() {
        return exploded;
    }
}
