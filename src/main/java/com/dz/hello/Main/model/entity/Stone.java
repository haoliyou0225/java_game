package com.dz.hello.Main.model.entity;

import com.dz.hello.controller.hook.IHook;

/**
 * 石头：低分重型物品。
 * 虽然 scoreVal 标记为 10，但游戏结算规则规定石头回收仅计 1 金币。
 */
public class Stone extends Item {

    public Stone(double x, double y) {
        this.x = x;
        this.y = y;
        this.scoreVal = 10;
        this.weight = 3.0;
        this.grabbed = false;
    }

    @Override
    public void onGrab(IHook hook) {
        this.grabbed = true;
    }

    /**
     * 结算规则重写：石头又重又不值钱，回收仅计 1 金币
     */
    @Override
    public int getSettlementGold() {
        return 1;
    }
}
