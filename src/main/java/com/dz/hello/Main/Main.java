package com.dz.hello.Main;

import com.dz.hello.Main.model.Player;
import com.dz.hello.Main.model.Diamond;
import com.dz.hello.Main.model.Gold;
import com.dz.hello.Main.model.Item;
import com.dz.hello.Main.model.ItemFactory;
import com.dz.hello.Main.model.Stone;
import com.dz.hello.Main.util.CollisionUtil;
import com.dz.hello.controller.hook.IHook;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.QuadCurve;
import javafx.scene.shape.Shape;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 游戏入口（可视化测试版），完整串联模型层功能：
 *  1. 物品生成：ItemFactory 按权重表随机生成金块/钻石/石头/炸弹
 *  2. 抓取：点击物品 = 当前玩家钩爪抓住（onGrab）
 *  3. 多态运动：被携带物品每帧 updatePosition 跟随钩爪，越重越慢
 *  4. 物品结算：钩爪带着物品回到顶部起点，物品销毁，金币计入“抓取者”账户
 *     （石头特殊：仅计 1 金币；炸弹：扣 150）
 *  5. 独立计分：玩家 A / B 账户完全隔离，按空格切换当前操作玩家，
 *     A 抓的物品即使中途切到 B 再回起点，金币仍然进 A 账户
 */
public class Main extends Application {

    private static final double HOOK_RADIUS = 18;
    /** 钩爪起点/回收结算点（画面顶部中央） */
    private static final double START_X = 320;
    private static final double START_Y = 60;
    /** 钩爪进入该半径范围即视为回到起点，触发结算 */
    private static final double SETTLE_RADIUS = 40;
    /** 随机生成的物品数量 */
    private static final int ITEM_COUNT = 9;

    private final Random rnd = new Random();
    private final ItemFactory itemFactory = new ItemFactory();

    /** 两名玩家，各自独立账户 */
    private final Player[] players = {new Player("A"), new Player("B")};
    /** 两名玩家各自的钩爪（IHook 实例） */
    private final IHook[] hooks = {new IHook() {
    }, new IHook() {
    }};
    private int activeIndex = 0;

    private final List<Item> items = new ArrayList<>();
    private final Map<Item, Group> itemViews = new HashMap<>();
    private final Map<Item, Shape> itemShapes = new HashMap<>();
    private final Map<Item, Double> itemRadii = new HashMap<>();
    private final Map<Item, Color> itemBaseStrokes = new HashMap<>();

    /** 当前被钩爪携带的物品及其抓取者（结算时计入抓取者账户） */
    private Item carried;
    private Player carriedBy;

    /** 钩爪当前位置（跟随鼠标） */
    private double hookX = START_X;
    private double hookY = START_Y;

    private Pane root;
    private Circle hookHead;
    private Text scoreA;
    private Text scoreB;
    private Text activeLabel;
    private Text message;
    private long messageUntil;

    @Override
    public void start(Stage stage) {
        root = new Pane();
        root.setStyle("-fx-background-color: #f0f4f8;");

        drawStartPoint();
        spawnItems();

        // 钩爪视图：颜色随当前操作玩家变化（A红 B蓝）
        hookHead = new Circle(START_X, START_Y, HOOK_RADIUS, Color.INDIANRED);
        hookHead.setStroke(Color.DARKRED);
        hookHead.setStrokeWidth(2);
        hookHead.setMouseTransparent(true);
        root.getChildren().add(hookHead);

        scoreA = new Text(15, 25, "");
        scoreA.setFont(Font.font(15));
        scoreA.setFill(Color.INDIANRED);
        scoreB = new Text(170, 25, "");
        scoreB.setFont(Font.font(15));
        scoreB.setFill(Color.ROYALBLUE);
        activeLabel = new Text(330, 25, "");
        activeLabel.setFont(Font.font(14));
        message = new Text(220, 55, "");
        message.setFont(Font.font(16));
        message.setFill(Color.DARKGREEN);
        root.getChildren().addAll(scoreA, scoreB, activeLabel, message);

        Scene scene = new Scene(root, 640, 420);

        // 空格切换当前操作玩家
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.SPACE) {
                activeIndex = 1 - activeIndex;
                boolean isA = activeIndex == 0;
                hookHead.setFill(isA ? Color.INDIANRED : Color.ROYALBLUE);
                hookHead.setStroke(isA ? Color.DARKRED : Color.NAVY);
                flash("已切换到玩家" + players[activeIndex].getName());
            }
        });

        scene.setOnMouseMoved(e -> {
            hookX = e.getX();
            hookY = e.getY();
            hookHead.setCenterX(hookX);
            hookHead.setCenterY(hookY);
            highlightCollisions();
        });

        // 游戏主循环：多态运动 + 结算检测
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (carried != null) {
                    carried.followHook(hookX, hookY);
                    carried.updatePosition(); // 多态：各物品按自身速度跟随
                    Group view = itemViews.get(carried);
                    view.setTranslateX(carried.getX());
                    view.setTranslateY(carried.getY());

                    // 钩爪回到起点 -> 物品结算销毁
                    if (Math.hypot(hookX - START_X, hookY - START_Y) <= SETTLE_RADIUS) {
                        settle();
                    }
                }
                refreshHud();
            }
        }.start();

        stage.setTitle("黄金矿工 - 完整玩法测试");
        stage.setScene(scene);
        stage.show();
    }

    /** 绘制顶部回收起点 */
    private void drawStartPoint() {
        Circle winch = new Circle(START_X, START_Y, 12);
        winch.setFill(Color.STEELBLUE);
        winch.setStroke(Color.NAVY);
        winch.setStrokeWidth(2);
        Text startText = new Text(START_X - 30, START_Y + 34, "回收起点");
        startText.setFont(Font.font(13));
        startText.setFill(Color.NAVY);
        // 结算范围提示圈
        Circle range = new Circle(START_X, START_Y, SETTLE_RADIUS);
        range.setFill(Color.LIGHTBLUE);
        range.setOpacity(0.25);
        range.setMouseTransparent(true);
        root.getChildren().addAll(range, winch, startText);
    }

    /** 物品生成：工厂按权重表随机生成全品类物品，位置随机 */
    private void spawnItems() {
        for (int i = 0; i < ITEM_COUNT; i++) {
            double x = 50 + rnd.nextDouble() * 540;
            double y = 140 + rnd.nextDouble() * 240;
            Item item = itemFactory.createRandomItem(x, y); // 返回 Item，实际是子类（多态）
            addItemView(item);
            items.add(item);
        }
    }

    private void addItemView(Item item) {
        double baseRadius = 22 + item.getWeight() * 7;

        double r;
        if (item instanceof Gold || item instanceof Stone) {
            double size = item instanceof Gold
                    ? 0.75 + rnd.nextDouble() * 0.55
                    : 0.80 + rnd.nextDouble() * 0.45;
            r = baseRadius * size;
        } else {
            r = baseRadius;
        }

        Node shapeNode = createView(item, r, itemShapes, itemBaseStrokes);

        Shape shape = itemShapes.get(item);
        double collisionR;
        if (shape instanceof Polygon) {
            collisionR = maxRadius((Polygon) shape, 0, 0);
        } else {
            collisionR = ((Circle) shape).getRadius();
        }
        itemRadii.put(item, collisionR);

        Group wrap = new Group(shapeNode);
        wrap.setTranslateX(item.getX());
        wrap.setTranslateY(item.getY());

        // 点击物品：当前玩家钩爪抓取（仅当钩爪空闲时）
        wrap.setOnMouseClicked(e -> {
            if (carried == null && item.isActive() && !item.isGrabbed()) {
                item.onGrab(hooks[activeIndex]);
                item.followHook(hookX, hookY);
                carried = item;
                carriedBy = players[activeIndex]; // 记录抓取者，结算只进他的账户
                flash("玩家" + carriedBy.getName() + " 抓住了物品，拖回起点结算");
            }
        });

        itemViews.put(item, wrap);
        root.getChildren().add(wrap);
    }

    /** 结算：物品销毁，金币计入抓取者本人账户 */
    private void settle() {
        int gold = carried.getSettlementGold(); // 多态：石头重写为1金币
        carriedBy.addGold(gold);

        Group view = itemViews.remove(carried);
        root.getChildren().remove(view);
        items.remove(carried);
        carried.destroy();

        flash("玩家" + carriedBy.getName() + " 结算 " + (gold >= 0 ? "+" : "") + gold + " 金币");
        carried = null;
        carriedBy = null;
    }

    private void highlightCollisions() {
        for (Item item : items) {
            if (item.isGrabbed()) {
                continue;
            }
            Shape shape = itemShapes.get(item);
            boolean hit = CollisionUtil.circleCollision(
                    hookX, hookY, HOOK_RADIUS,
                    item.getX(), item.getY(), itemRadii.get(item));
            shape.setStroke(hit ? Color.RED : itemBaseStrokes.get(item));
            shape.setStrokeWidth(hit ? 4 : 2.5);
        }
    }

    private void refreshHud() {
        scoreA.setText("玩家A（红钩）: " + players[0].getGold() + " 金币");
        scoreB.setText("玩家B（蓝钩）: " + players[1].getGold() + " 金币");
        activeLabel.setText("当前: 玩家" + players[activeIndex].getName() + "，空格切换");
        if (System.currentTimeMillis() > messageUntil) {
            message.setText("");
        }
    }

    private void flash(String text) {
        message.setText(text);
        messageUntil = System.currentTimeMillis() + 1800;
    }

    /**
     * 按物品类型创建外形（相对坐标 0,0 绘制，由外部 Group 平移定位）：
     * Gold=随机圆滑亮黄金块，Diamond=浅蓝切面宝石，Stone=随机圆滑蓝灰岩石，Bomb=黑球+引线+火花
     */
    private Node createView(Item item, double r,
                            Map<Item, Shape> shapes, Map<Item, Color> baseStrokes) {
        if (item instanceof Diamond) {
            Polygon diamond = new Polygon(
                    -r * 0.30, -r * 0.55,
                    r * 0.30, -r * 0.55,
                    r * 0.55, -r * 0.05,
                    0.0, r * 0.60,
                    -r * 0.55, -r * 0.05);
            diamond.setFill(new LinearGradient(0, 0, 0, 1, true,
                    CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#E6FBFF")),
                    new Stop(0.5, Color.web("#8BE3F0")),
                    new Stop(1, Color.web("#33C6DC"))));
            Color diamondEdge = Color.web("#16A8C4");
            diamond.setStroke(diamondEdge);
            diamond.setStrokeWidth(2);

            Polygon crownFacet = new Polygon(
                    -r * 0.30, -r * 0.55,
                    0, -r * 0.55,
                    0, -r * 0.05,
                    -r * 0.55, -r * 0.05);
            crownFacet.setFill(Color.WHITE);
            crownFacet.setOpacity(0.35);
            crownFacet.setMouseTransparent(true);

            Line girdle = new Line(-r * 0.55, -r * 0.05, r * 0.55, -r * 0.05);
            girdle.setStroke(Color.WHITE);
            girdle.setOpacity(0.6);
            girdle.setMouseTransparent(true);

            shapes.put(item, diamond);
            baseStrokes.put(item, diamondEdge);
            return new Group(diamond, crownFacet, girdle);
        }

        if (item instanceof Gold) {
            Polygon gold = nugget(0, 0, r);
            gold.setFill(new RadialGradient(0, 0.15, 0.35, 0.30, 0.9, true,
                    CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#FFF9C4")),
                    new Stop(0.55, Color.web("#FFD600")),
                    new Stop(1, Color.web("#F0A500"))));
            Color goldEdge = Color.web("#B8860B");
            gold.setStroke(goldEdge);
            gold.setStrokeWidth(2.5);

            Circle shine = new Circle(
                    -r * (0.20 + rnd.nextDouble() * 0.20),
                    -r * (0.28 + rnd.nextDouble() * 0.18),
                    r * (0.12 + rnd.nextDouble() * 0.07), Color.WHITE);
            shine.setOpacity(0.55);
            shine.setMouseTransparent(true);

            shapes.put(item, gold);
            baseStrokes.put(item, goldEdge);
            return new Group(gold, shine);
        }

        if (item instanceof Stone) {
            Polygon stone = rock(0, 0, r);
            stone.setFill(new RadialGradient(0, 0.2, 0.35, 0.30, 0.9, true,
                    CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#D2DBE4")),
                    new Stop(1, Color.web("#7E8FA0"))));
            Color stoneEdge = Color.web("#5D6B7A");
            stone.setStroke(stoneEdge);
            stone.setStrokeWidth(2.5);

            double m = 0.9 + rnd.nextDouble() * 0.2;
            QuadCurve mark1 = new QuadCurve(
                    r * 0.12 * m, -r * 0.28,
                    r * 0.52, -r * 0.08 * m,
                    r * 0.18, r * 0.18);
            mark1.setStroke(stoneEdge);
            mark1.setStrokeWidth(2);
            mark1.setFill(null);
            mark1.setOpacity(0.65);
            mark1.setMouseTransparent(true);

            QuadCurve mark2 = new QuadCurve(
                    r * 0.28, r * 0.28 * m,
                    r * 0.58 * m, r * 0.42,
                    r * 0.32, r * 0.58);
            mark2.setStroke(stoneEdge);
            mark2.setStrokeWidth(2);
            mark2.setFill(null);
            mark2.setOpacity(0.55);
            mark2.setMouseTransparent(true);

            shapes.put(item, stone);
            baseStrokes.put(item, stoneEdge);
            return new Group(stone, mark1, mark2);
        }

        // Bomb：黑色球体 + 棕色引线 + 橙色火花
        Circle body = new Circle(0, 0, r * 0.85);
        body.setFill(new RadialGradient(0, 0, 0.3, 0.3, 1, true,
                CycleMethod.NO_CYCLE,
                new Stop(0, Color.DIMGRAY),
                new Stop(1, Color.BLACK)));
        body.setStroke(Color.DARKRED);
        body.setStrokeWidth(2);

        Line fuse = new Line(r * 0.35, -r * 0.75, r * 0.7, -r * 1.15);
        fuse.setStroke(Color.SADDLEBROWN);
        fuse.setStrokeWidth(3);
        fuse.setMouseTransparent(true);

        Circle spark = new Circle(r * 0.7, -r * 1.15, 4, Color.ORANGE);
        spark.setMouseTransparent(true);

        shapes.put(item, body);
        baseStrokes.put(item, Color.DARKRED);
        return new Group(body, fuse, spark);
    }

    /**
     * 随机但圆滑的金块轮廓：3 个低频正弦波叠加 + 56 个采样点
     */
    private Polygon nugget(double cx, double cy, double r) {
        int waves = 3;
        int[] freq = new int[waves];
        double[] amp = new double[waves];
        double[] phase = new double[waves];
        for (int i = 0; i < waves; i++) {
            freq[i] = 2 + rnd.nextInt(4);
            amp[i] = 0.15 / (i + 1) + rnd.nextDouble() * 0.05;
            phase[i] = rnd.nextDouble() * Math.PI * 2;
        }
        Polygon poly = new Polygon();
        int steps = 56;
        for (int i = 0; i < steps; i++) {
            double a = Math.PI * 2 * i / steps;
            double k = 0.92;
            for (int w = 0; w < waves; w++) {
                k += amp[w] * Math.cos(freq[w] * a + phase[w]);
            }
            poly.getPoints().addAll(cx + r * k * Math.cos(a), cy + r * k * Math.sin(a));
        }
        return poly;
    }

    /**
     * 随机但圆滑的岩石轮廓：低频正弦波叠加 + 顶部平滑收窄
     */
    private Polygon rock(double cx, double cy, double r) {
        int waves = 3;
        int[] freq = new int[waves];
        double[] amp = new double[waves];
        double[] phase = new double[waves];
        for (int i = 0; i < waves; i++) {
            freq[i] = 2 + rnd.nextInt(3);
            amp[i] = 0.10 / (i + 1) + rnd.nextDouble() * 0.04;
            phase[i] = rnd.nextDouble() * Math.PI * 2;
        }
        Polygon poly = new Polygon();
        int steps = 56;
        for (int i = 0; i < steps; i++) {
            double a = Math.PI * 2 * i / steps;
            double k = 0.90;
            for (int w = 0; w < waves; w++) {
                k += amp[w] * Math.cos(freq[w] * a + phase[w]);
            }
            // 顶部（a=-PI/2 附近）平滑收窄形成圆润尖顶
            double topWeight = Math.max(0, Math.cos(a + Math.PI / 2));
            topWeight = topWeight * topWeight;
            k *= 1 - 0.22 * topWeight;
            poly.getPoints().addAll(cx + r * k * Math.cos(a), cy + r * k * Math.sin(a));
        }
        return poly;
    }

    /** 计算多边形顶点中距中心最远的距离，用作碰撞半径 */
    private double maxRadius(Polygon poly, double cx, double cy) {
        double max = 0;
        var points = poly.getPoints();
        for (int i = 0; i < points.size(); i += 2) {
            double dx = points.get(i) - cx;
            double dy = points.get(i + 1) - cy;
            max = Math.max(max, Math.sqrt(dx * dx + dy * dy));
        }
        return max;
    }

    public static void main(String[] args) {
        launch(args);
    }
}