// FR-10 炸弹：被钩子勾住后触发爆炸，扣分 30（负分），立即清除自身
package com.dz.hello.Main.model;

public class Bomb extends Item {
    private boolean exploded;

    public Bomb(double x, double y) {
        super(x, y, -30, 3.0);
    }

    @Override
    public void onGrab(Hook hook) {
        // 被抓取时不立即爆炸，待 GameManager 在收回结算时统一触发（见 triggerExplode）
    }

    @Override
    public void updatePosition() {
        // 炸弹静止在矿洞地图上，不需要自动移动
    }

    public void triggerExplode() {
        exploded = true;
        // 后续调用全局爆炸逻辑，清除150px半径内所有普通物品
    }

    public boolean isExploded() {
        return exploded;
    }
}
