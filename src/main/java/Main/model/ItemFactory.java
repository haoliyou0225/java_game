package Main.model;

import java.util.Random;

/**
 * 物品工厂：按权重表随机生成全品类物品。
 * 返回类型统一声明为抽象父类 Item，实际对象是各子类实例，
 * 调用方拿到后通过多态使用（onGrab / updatePosition / getSettlementGold）。
 *
 * 权重表：金块最常见、石头次之、炸弹较少、钻石最稀有。
 */
public class ItemFactory {

    /** 金块权重：常见 */
    private static final int WEIGHT_GOLD = 50;
    /** 石头权重：较常见 */
    private static final int WEIGHT_STONE = 30;
    /** 炸弹权重：较少 */
    private static final int WEIGHT_BOMB = 12;
    /** 钻石权重：稀有 */
    private static final int WEIGHT_DIAMOND = 8;

    private static final int TOTAL_WEIGHT =
            WEIGHT_GOLD + WEIGHT_STONE + WEIGHT_BOMB + WEIGHT_DIAMOND;

    private final Random random = new Random();

    /**
     * 在指定坐标随机生成一个物品
     * @param x 生成位置横坐标
     * @param y 生成位置纵坐标
     * @return 具体子类实例（以 Item 类型返回，多态）
     */
    public Item createRandomItem(double x, double y) {
        int roll = random.nextInt(TOTAL_WEIGHT);
        int acc = 0;

        acc += WEIGHT_GOLD;
        if (roll < acc) {
            return new Gold(x, y);
        }
        acc += WEIGHT_STONE;
        if (roll < acc) {
            return new Stone(x, y);
        }
        acc += WEIGHT_BOMB;
        if (roll < acc) {
            return new Bomb(x, y);
        }
        return new Diamond(x, y);
    }
}
