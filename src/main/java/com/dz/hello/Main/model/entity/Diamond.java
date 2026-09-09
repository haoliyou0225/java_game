package com.dz.hello.Main.model.entity;

import com.dz.hello.controller.hook.IHook;

/**
 * 钻石：最高分轻量物品
 */
public class Diamond extends Item {

    public Diamond(double x, double y) {
        this.x = x;
        this.y = y;
        this.scoreVal = 200;
        this.weight = 1.2;
        this.grabbed = false;
    }

    @Override
    public void onGrab(IHook hook) {
        this.grabbed = true;
    }
}
