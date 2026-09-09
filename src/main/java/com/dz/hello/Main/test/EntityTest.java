package com.dz.hello.Main.test;

import com.dz.hello.Main.model.Player;
import com.dz.hello.Main.model.entity.Bomb;
import com.dz.hello.Main.model.entity.Diamond;
import com.dz.hello.Main.model.entity.Gold;
import com.dz.hello.Main.model.entity.Item;
import com.dz.hello.Main.model.entity.ItemFactory;
import com.dz.hello.Main.model.entity.Stone;
import com.dz.hello.Main.util.CollisionUtil;
import com.dz.hello.controller.hook.IHook;

import java.util.HashMap;
import java.util.Map;

/**
 * 模型层逻辑自测类（不依赖 JavaFX，可直接右键运行 main 方法）。
 * 验证内容：物品属性、onGrab、碰撞检测、权重随机生成、
 * 多态运动、结算金币（石头仅1金币）、玩家账户隔离、物品销毁。
 */
public class EntityTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        IHook fakeHook = new IHook() {
        };

        // ---------- 1. Gold 金块 ----------
        Gold gold = new Gold(100, 200);
        check("Gold 初始 x=100", gold.getX() == 100.0);
        check("Gold 初始 y=200", gold.getY() == 200.0);
        check("Gold 分值=100", gold.getScoreVal() == 100);
        check("Gold 重量=1.0", gold.getWeight() == 1.0);
        check("Gold 初始未被抓取", !gold.isGrabbed());
        gold.onGrab(fakeHook);
        check("Gold 调用 onGrab 后 grabbed=true", gold.isGrabbed());

        // ---------- 2. Diamond 钻石 ----------
        Diamond diamond = new Diamond(30, 40);
        check("Diamond 初始 x=30", diamond.getX() == 30.0);
        check("Diamond 初始 y=40", diamond.getY() == 40.0);
        check("Diamond 分值=200", diamond.getScoreVal() == 200);
        check("Diamond 重量=1.2", diamond.getWeight() == 1.2);
        check("Diamond 初始未被抓取", !diamond.isGrabbed());
        diamond.onGrab(fakeHook);
        check("Diamond 调用 onGrab 后 grabbed=true", diamond.isGrabbed());

        // ---------- 3. Stone 石头 ----------
        Stone stone = new Stone(0, 0);
        check("Stone 分值=10", stone.getScoreVal() == 10);
        check("Stone 重量=3.0（最重）", stone.getWeight() == 3.0);
        check("Stone 初始未被抓取", !stone.isGrabbed());
        stone.onGrab(fakeHook);
        check("Stone 调用 onGrab 后 grabbed=true", stone.isGrabbed());

        // ---------- 4. Bomb 炸弹 ----------
        Bomb bomb = new Bomb(0, 0);
        check("Bomb 分值=-150（负分）", bomb.getScoreVal() == -150);
        check("Bomb 重量=1.5", bomb.getWeight() == 1.5);
        check("Bomb 初始未爆炸", !bomb.isExploded());
        bomb.triggerExplode();
        check("Bomb 调用 triggerExplode 后 exploded=true", bomb.isExploded());
        bomb.onGrab(fakeHook);
        check("Bomb 调用 onGrab 后 grabbed=true", bomb.isGrabbed());

        // ---------- 5. CollisionUtil 碰撞检测 ----------
        check("两圆心重合 -> 碰撞", CollisionUtil.circleCollision(0, 0, 5, 0, 0, 5));
        check("两圆外切（距离=半径和 10）-> 碰撞",
                CollisionUtil.circleCollision(0, 0, 5, 10, 0, 5));
        check("两圆相距 20、半径和 10 -> 不碰撞",
                !CollisionUtil.circleCollision(0, 0, 5, 20, 0, 5));
        check("斜向 3-4-5：距离 5 = 半径和 2+3 -> 碰撞",
                CollisionUtil.circleCollision(0, 0, 2, 3, 4, 3));
        check("斜向 3-4-5：距离 5 > 半径和 2+2 -> 不碰撞",
                !CollisionUtil.circleCollision(0, 0, 2, 3, 4, 2));

        // ---------- 6. 多态引用 ----------
        Item item = new Gold(1, 1);
        check("Item 引用可指向 Gold 子类实例", item.getScoreVal() == 100);
        item.onGrab(fakeHook);
        check("通过 Item 引用调用 onGrab 生效", item.isGrabbed());

        // ---------- 7. ItemFactory 权重随机生成 ----------
        ItemFactory factory = new ItemFactory();
        Map<String, Integer> counts = new HashMap<>();
        for (int i = 0; i < 3000; i++) {
            Item generated = factory.createRandomItem(0, 0);
            counts.merge(typeName(generated), 1, Integer::sum);
        }
        check("3000次生成包含金块", counts.getOrDefault("Gold", 0) > 0);
        check("3000次生成包含石头", counts.getOrDefault("Stone", 0) > 0);
        check("3000次生成包含炸弹", counts.getOrDefault("Bomb", 0) > 0);
        check("3000次生成包含钻石", counts.getOrDefault("Diamond", 0) > 0);
        check("金块(权重50)生成数远多于钻石(权重8)",
                counts.get("Gold") > counts.get("Diamond") * 4);
        check("石头(权重30)生成数多于炸弹(权重12)",
                counts.get("Stone") > counts.get("Bomb"));

        // ---------- 8. 结算金币（多态） ----------
        check("金块结算 +100 金币", new Gold(0, 0).getSettlementGold() == 100);
        check("钻石结算 +200 金币", new Diamond(0, 0).getSettlementGold() == 200);
        check("炸弹结算 -150 金币", new Bomb(0, 0).getSettlementGold() == -150);
        check("石头仅结算 1 金币（特殊规则重写）", new Stone(0, 0).getSettlementGold() == 1);

        // ---------- 9. 多态运动：被携带后跟随钩爪，越重越慢 ----------
        Gold moveGold = new Gold(0, 0);
        moveGold.onGrab(fakeHook);
        moveGold.followHook(100, 0);
        Stone moveStone = new Stone(0, 0);
        moveStone.onGrab(fakeHook);
        moveStone.followHook(100, 0);
        for (int i = 0; i < 10; i++) {
            moveGold.updatePosition();
            moveStone.updatePosition();
        }
        check("金块被携带 10 帧后向钩爪移动（8*10=80）", moveGold.getX() == 80.0);
        check("石头（重）10 帧后比金块移动得慢", moveStone.getX() < moveGold.getX());
        check("石头 10 帧移动约 8/3.0*10≈26.7", Math.abs(moveStone.getX() - 8.0 / 3.0 * 10) < 0.01);

        Gold idleGold = new Gold(5, 7);
        idleGold.updatePosition(); // 未抓取
        check("未被抓取的物品 updatePosition 不移动", idleGold.getX() == 5.0 && idleGold.getY() == 7.0);

        // ---------- 10. 物品销毁 ----------
        Gold destroyed = new Gold(0, 0);
        check("物品初始为 active", destroyed.isActive());
        destroyed.destroy();
        check("调用 destroy 后 active=false", !destroyed.isActive());

        // ---------- 11. 玩家账户独立计分 ----------
        Player playerA = new Player("A");
        Player playerB = new Player("B");
        check("新玩家账户初始为 0 金币", playerA.getGold() == 0 && playerB.getGold() == 0);

        // A 抓金块结算 +100
        playerA.addGold(new Gold(0, 0).getSettlementGold());
        check("A 结算金块后账户=100", playerA.getGold() == 100);
        check("A 加分不影响 B（B 仍为 0）", playerB.getGold() == 0);

        // B 抓石头结算仅 +1
        playerB.addGold(new Stone(0, 0).getSettlementGold());
        check("B 结算石头后账户=1（石头仅1金币）", playerB.getGold() == 1);
        check("B 加分不影响 A（A 仍为 100）", playerA.getGold() == 100);

        // B 再抓炸弹 -150
        playerB.addGold(new Bomb(0, 0).getSettlementGold());
        check("B 结算炸弹后账户=1-150=-149", playerB.getGold() == -149);
        check("A 账户始终不受 B 影响（仍为 100）", playerA.getGold() == 100);

        // 模拟“A 抓取中途切换玩家 B，回起点结算”：分数仍归抓取者 A
        Item grabbedByA = new Diamond(0, 0);
        grabbedByA.onGrab(fakeHook);
        Player grabber = playerA; // 抓取瞬间记录的抓取者
        grabber.addGold(grabbedByA.getSettlementGold());
        check("A 抓的钻石即使中途切到 B，结算仍进 A 账户（A=300，B=-149）",
                playerA.getGold() == 300 && playerB.getGold() == -149);

        // ---------- 汇总 ----------
        System.out.println("========================================");
        System.out.println("测试结果：通过 " + passed + " 项，失败 " + failed + " 项");
        if (failed == 0) {
            System.out.println("全部通过，模型层逻辑正确！");
        } else {
            System.out.println("存在失败项，请检查上面的 [FAIL] 行");
            System.exit(1);
        }
    }

    private static String typeName(Item item) {
        if (item instanceof Gold) {
            return "Gold";
        }
        if (item instanceof Diamond) {
            return "Diamond";
        }
        if (item instanceof Stone) {
            return "Stone";
        }
        if (item instanceof Bomb) {
            return "Bomb";
        }
        return "Other";
    }

    private static void check(String name, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("[PASS] " + name);
        } else {
            failed++;
            System.out.println("[FAIL] " + name);
        }
    }
}
