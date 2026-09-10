package Main.model;

/**
 * 玩家账户：每个玩家各自持有独立的金币余额。
 * 物品结算时只给抓取者本人的账户加金币，玩家之间完全隔离，
 * A 抓取的物品分数绝对不会加到 B 的账户。
 */
public class Player {

    private final String name;
    private int gold;

    public Player(String name) {
        this.name = name;
        this.gold = 0;
    }

    /**
     * 物品结算入账：正数加分（金块/钻石），负数扣分（炸弹）
     */
    public void addGold(int amount) {
        this.gold += amount;
    }

    public String getName() {
        return name;
    }

    public int getGold() {
        return gold;
    }
}
