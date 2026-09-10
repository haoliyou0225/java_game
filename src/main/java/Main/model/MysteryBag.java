// FR-07 福袋：分数随机 100-800、重量 1.5，神秘奖励物品（贴近原版）
package Main.model;

import Main.config.GameConfig;

import java.util.concurrent.ThreadLocalRandom;

public class MysteryBag extends ItemImpl {
    public MysteryBag(double x, double y) {
        super(x, y,
                ThreadLocalRandom.current().nextInt(
                        GameConfig.MYSTERY_BAG_MIN_GOLD,
                        GameConfig.MYSTERY_BAG_MAX_GOLD + 1),
                1.5);
    }

    @Override
    public void onGrab(Hook hook) {
        // 福袋抓取后由 GameManager 结算随机金币
    }

    @Override
    public void updatePosition() {
        // 福袋静止
    }

    @Override
    public double getRadius() {
        return 18;
    }
}