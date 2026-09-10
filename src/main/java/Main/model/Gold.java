package Main.model;

import controller.hook.IHook;

/**
 * 金块：高分轻量物品
 */
public class Gold extends Item {

    public Gold(double x, double y) {
        this.x = x;
        this.y = y;
        this.scoreVal = 100;
        this.weight = 1.0;
        this.grabbed = false;
    }

    @Override
    public void onGrab(IHook hook) {
        this.grabbed = true;
    }
}
