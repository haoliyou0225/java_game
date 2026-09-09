// FR-10 炸弹：被钩子勾住后触发爆炸，扣分 30（负分），立即清除自身
package com.dz.hello.main.model;

public class Bomb extends Item {
    private boolean exploded;

    public Bomb(double x, double y) {
        super(x, y, -30, 3.0);
    }

    @Override
    public void onGrab(Hook hook) {
        // FR-14 碰到TNT后触发爆炸逻辑
        if (!exploded) {
            triggerExplode();
        }
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
