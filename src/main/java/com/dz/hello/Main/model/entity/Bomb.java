package com.dz.hello.Main.model.entity;

import com.dz.hello.controller.hook.IHook;

/**
 * 炸弹：负分物品，可被触发爆炸
 */
public class Bomb extends Item {

    private boolean exploded = false;

    public Bomb(double x, double y) {
        this.x = x;
        this.y = y;
        this.scoreVal = -150;
        this.weight = 1.5;
        this.grabbed = false;
    }

    @Override
    public void onGrab(IHook hook) {
        this.grabbed = true;
    }

    /**
     * 触发爆炸
     */
    public void triggerExplode() {
        this.exploded = true;
    }

    /**
     * 是否已爆炸
     * @return true 已爆炸；false 未爆炸
     */
    public boolean isExploded() {
        return exploded;
    }
}
