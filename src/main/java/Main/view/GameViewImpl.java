// FR-UI GameViewImpl：游戏画面渲染（地面/矿洞/物品/双钩/绳索），来自 feature_ui 分支，已适配 hook 分支 Item/Hook 接口
package Main.view;

import Main.config.Config;
import Main.model.Bomb;
import Main.model.GameModel;
import Main.model.Hook;
import Main.model.Item;
import Main.model.MineMap;
import Main.model.Mole;
import Main.model.MysteryBag;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
<<<<<<< Updated upstream
=======
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

import java.io.InputStream;
import java.util.Random;
import java.util.WeakHashMap;
>>>>>>> Stashed changes

/**
 * 游戏画面渲染（FR-28）
 * 布局（贴合效果图）：
 *   HUD（顶部，由 HUDView 叠加）
 *   顶部地面条（钩爪起点所在）
 *   矿洞区域（物品、钩爪、绳索向下抓取）
 *   矿洞底部
 */
public class GameViewImpl implements GameView {

<<<<<<< Updated upstream
    /** 顶部地面条上沿 Y（与 Config.HUD_HEIGHT 对齐，HUD 下方） */
=======
    /** 钩爪贴图：用户提供的 hook(1).png，直接 file URL 加载 */
    private static final Image HOOK_IMAGE = loadFileImage("hook(1).png");

    // 石头主体渐变使用比例坐标（0,0,1,1），与位置无关，全局复用
    private static final LinearGradient STONE_BODY_FILL = new LinearGradient(
            0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0.00, Color.rgb(160, 170, 168)),
            new Stop(0.50, Color.rgb(126, 128, 134)),
            new Stop(1.00, Color.rgb(88, 84, 96)));
    private static final Color STONE_EDGE_W4 = Color.rgb(48, 42, 50, 0.05);
    private static final Color STONE_EDGE_W6 = Color.rgb(48, 42, 50, 0.09);
    private static final Color STONE_EDGE_W32 = Color.rgb(48, 42, 50, 0.14);
    private static final Color STONE_EDGE_W16 = Color.rgb(48, 42, 50, 0.20);
    private static final Color STONE_SHADOW_OVAL = Color.rgb(42, 34, 38, 0.16);

    /** 钻石猪冲刺光环渐变（局部坐标，与位置无关，按 r 固定缓存一次） */
    private static RadialGradient pigDashGlowSprite;
    private static RadialGradient pigDashGlowVector;
    private static double pigDashGlowSpriteR = -1;
    private static double pigDashGlowVectorR = -1;

    private static Image loadFileImage(String name) {
        try {
            String dir = System.getProperty("user.dir").replace('\\', '/');
            Image img = new Image("file:" + dir + "/resources/Resources/" + name);
            if (img.isError()) return null;
            return img;
        } catch (Exception e) {
            return null;
        }
    }

>>>>>>> Stashed changes
    private static final double GROUND_TOP = Config.HUD_HEIGHT;
    /** 顶部地面条高度 */
    private static final double GROUND_HEIGHT = 60;

    private final Canvas canvas;

<<<<<<< Updated upstream
    public GameViewImpl(Canvas canvas) {
        this.canvas = canvas;
=======
    /** 矿洞背景图（静态加载一次，所有 GameViewImpl 实例复用；加载失败为 null 时回退纯色） */
    private static volatile Image caveBgImage;
    private static volatile boolean caveBgTried;

    // ===== 物品形状/渐变缓存（避免每帧重新计算与分配） =====

    /** 金块形状缓存：相对多边形偏移 + 绝对坐标 + 渐变（按物品引用缓存，位置不变时零分配） */
    private static final class GoldCache {
        final double[] rxs;        // 相对中心的 x 偏移（按 hashCode 固定，仅算一次）
        final double[] rys;        // 相对中心的 y 偏移
        final boolean hasNotch;     // r >= 18
        final boolean hasSpot;      // r >= 20 && (seed & 1) == 0
        final Color strokeColor;
        final Color notchColor;
        final Color spotColor;
        final double[] xs;          // 绝对坐标（复用，位置变化时重算）
        final double[] ys;
        double lastCx = Double.NaN, lastCy = Double.NaN;
        RadialGradient fill;

        GoldCache(double[] rxs, double[] rys, boolean hasNotch, boolean hasSpot,
                  Color strokeColor, Color notchColor, Color spotColor) {
            this.rxs = rxs;
            this.rys = rys;
            this.hasNotch = hasNotch;
            this.hasSpot = hasSpot;
            this.strokeColor = strokeColor;
            this.notchColor = notchColor;
            this.spotColor = spotColor;
            this.xs = new double[rxs.length];
            this.ys = new double[rxs.length];
        }
    }

    /** 石头形状缓存：相对多边形偏移 + 绝对坐标 + 渐变（位置不变时零分配） */
    private static final class StoneCache {
        final double[] rxs;
        final double[] rys;
        final int n;
        final double hw, hh, edgeScale;
        final double[] xs;
        final double[] ys;
        double lastCx = Double.NaN, lastCy = Double.NaN;
        RadialGradient highlight;   // 左上受光
        RadialGradient shadow;      // 右下暗部
        RadialGradient ground;      // 底部接地

        StoneCache(double[] rxs, double[] rys, int n, double hw, double hh, double edgeScale) {
            this.rxs = rxs;
            this.rys = rys;
            this.n = n;
            this.hw = hw;
            this.hh = hh;
            this.edgeScale = edgeScale;
            this.xs = new double[n];
            this.ys = new double[n];
        }
    }

    /** 钻石形状缓存：八面体多边形 + 渐变（位置不变时零分配） */
    private static final class DiamondCache {
        final double[] rxs;
        final double[] rys;
        final double[] xs;
        final double[] ys;
        double lastCx = Double.NaN, lastCy = Double.NaN;
        RadialGradient fill;

        DiamondCache(double[] rxs, double[] rys) {
            this.rxs = rxs;
            this.rys = rys;
            this.xs = new double[rxs.length];
            this.ys = new double[rxs.length];
        }
    }

    private final WeakHashMap<Item, GoldCache> goldShapeCache = new WeakHashMap<>();
    private final WeakHashMap<Item, StoneCache> stoneShapeCache = new WeakHashMap<>();
    private final WeakHashMap<Item, DiamondCache> diamondShapeCache = new WeakHashMap<>();

    /** 上一帧 P1 钩子状态（检测进入 GRABBING 时播放抓取音效，初始 null） */
    private HookState prevHook1State;

    /** 上一帧 P2 钩子状态（同上） */
    private HookState prevHook2State;

    /** 上一帧场上炸弹数量（-1 表示尚未渲染过；数量减少即 TNT 爆炸，播放炸弹音效） */
    private int prevBombCount = -1;

    public GameViewImpl(Canvas canvas) {
        this.canvas = canvas;
        loadCaveBg();
    }

    /** 静态加载矿洞背景图（只加载一次，避免每次 startGame 重新触发 PNG 解码与大对象分配） */
    private static void loadCaveBg() {
        if (caveBgTried) return;
        synchronized (GameViewImpl.class) {
            if (caveBgTried) return;
            caveBgTried = true;
            try {
                Image bg = new Image(GameViewImpl.class.getResourceAsStream("/images/background/mineBG1.png"));
                if (bg.isError()) bg = null;
                caveBgImage = bg;
            } catch (Exception ignored) {
                caveBgImage = null;
            }
        }
>>>>>>> Stashed changes
    }

    @Override
    public void render(GameModel model) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();

        // 1. 顶部地面条（钩爪起点所在，HUD 下方）
        gc.setFill(Color.rgb(139, 90, 43));
        gc.fillRect(0, GROUND_TOP, w, GROUND_HEIGHT);

        // 2. 矿洞背景（深棕色）
        gc.setFill(Color.rgb(60, 42, 30));
        gc.fillRect(0, GROUND_TOP + GROUND_HEIGHT, w, h - GROUND_TOP - GROUND_HEIGHT);

        // 3. 矿洞底部（更深的底色）
        gc.setFill(Color.rgb(38, 25, 17));
        gc.fillRect(0, h - 60, w, 60);

        // 4. 矿洞边界（从 MineMap 接口读取）
        MineMap mineMap = model.getMineMap();
        if (mineMap != null) {
            gc.setStroke(Color.rgb(220, 180, 120));
            gc.setLineWidth(2);
            gc.strokeRect(mineMap.getMinX(), mineMap.getMinY(),
                    mineMap.getMaxX() - mineMap.getMinX(),
                    mineMap.getMaxY() - mineMap.getMinY());
        }

        // 5. 所有物品（包括被钩住的——它们会跟着钩尖移动显示出来）
        if (mineMap != null && mineMap.getItems() != null) {
            for (Item item : mineMap.getItems()) {
                double r = item.getRadius();
                if (item instanceof Bomb) {
                    // 炸弹：黑圆 + 红 TNT
                    gc.setFill(Color.rgb(30, 30, 30));
                    gc.fillOval(item.getX() - r, item.getY() - r, r * 2, r * 2);
                    gc.setFill(Color.rgb(220, 40, 40));
                    gc.fillText("TNT", item.getX() - 12, item.getY() + 4);
                } else if (item instanceof Mole) {
                    // 鼹鼠：棕圆
                    gc.setFill(Color.rgb(139, 90, 43));
                    gc.fillOval(item.getX() - r, item.getY() - r, r * 2, r * 2);
                    gc.setFill(Color.WHITE);
                    gc.fillText("10", item.getX() - 8, item.getY() + 5);
                } else if (item instanceof MysteryBag) {
                    // 福袋：紫圆 + 黄字
                    gc.setFill(Color.rgb(128, 64, 200));
                    gc.fillOval(item.getX() - r, item.getY() - r, r * 2, r * 2);
                    gc.setFill(Color.rgb(255, 215, 0));
                    gc.fillText("袋", item.getX() - 8, item.getY() + 5);
                } else {
                    // 普通物品：按分值分档着色
                    gc.setFill(colorOf(item));
                    gc.fillOval(item.getX() - r, item.getY() - r, r * 2, r * 2);
                    gc.setFill(Color.WHITE);
                    gc.fillText(String.valueOf(item.getScore()),
                            item.getX() - 10, item.getY() + 5);
                }
            }
        }

        // 6. 玩家1钩爪（蓝色）：起点标记 + 绳索 + 钩爪头
        drawHook(gc, model.getHook1(), Color.DODGERBLUE);

        // 7. 玩家2钩爪（红色）
        drawHook(gc, model.getHook2(), Color.CRIMSON);
    }

    /**
     * 绘制单个钩爪：地面上的起点标记、绳索（起点→钩爪头）、钩爪头圆点。
     * 绳索端点取自 Hook 接口实时数据，长度随钩爪位置动态变化。
     */
    private void drawHook(GraphicsContext gc, Hook hook, Color color) {
        if (hook == null) {
            return;
        }
<<<<<<< Updated upstream
        // 起点标记（顶部地面上的小底座）
        gc.setFill(color);
        gc.fillRect(hook.getStartX() - 14, hook.getStartY() - 10, 28, 20);
        // 绳索
=======
        HookState prev = isHook1 ? prevHook1State : prevHook2State;
        HookState current = hook.getState();
        if (prev != HookState.GRABBING && current == HookState.GRABBING) {
            AudioManager.get().playSfx("largegold");
        }
        if (isHook1) {
            prevHook1State = current;
        } else {
            prevHook2State = current;
        }
    }

    /** 创建径向渐变：左上高光 */
    private static RadialGradient makeRadial(double cx, double cy, double r,
                                             Color c0, Color c1, Color c2) {
        return new RadialGradient(
                0, 0,                         // focusX, focusY
                cx - r * 0.3, cy - r * 0.3,   // centerX, centerY（高光偏左上）
                r * 1.3,                      // radius
                false,                        // proportional
                CycleMethod.NO_CYCLE,
                new Stop(0.0, c0),
                new Stop(0.5, c1),
                new Stop(1.0, c2));
    }

    /** 创建单 Stop 径向渐变（双色） */
    private static RadialGradient makeRadial2(double cx, double cy, double r,
                                              Color c0, Color c1) {
        return new RadialGradient(
                0, 0,
                cx - r * 0.3, cy - r * 0.3,
                r * 1.3,
                false,
                CycleMethod.NO_CYCLE,
                new Stop(0.0, c0),
                new Stop(1.0, c1));
    }

    /**
     * 金块：光润圆滑的瘤状金块（3 个不可通约的低频余弦波 2/3/5 叠加），
     * 亮黄高光 + 金黄渐变 + 琥珀描边。中大金块额外带顶部小凹痕与左下小金斑；
     * 形状基于 hashCode 固定，重绘不变。形状与渐变按物品缓存，位置不变时零分配。
     */
    private void drawGoldNugget(GraphicsContext gc, Item item, double r) {
        double cx = item.getX();
        double cy = item.getY();
        long seed = item.hashCode() & 0x7fffffffL;

        GoldCache cache = goldShapeCache.get(item);
        if (cache == null) {
            // 首次：基于 hashCode 计算相对多边形偏移（仅执行一次）
            Random rnd = new Random(seed);
            int points = 64;
            double[] rxs = new double[points];
            double[] rys = new double[points];
            int[] freq = {2, 3, 5};
            double[] mag = {
                    0.12 + rnd.nextDouble() * 0.05,
                    0.07 + rnd.nextDouble() * 0.04,
                    0.04 + rnd.nextDouble() * 0.03
            };
            double[] ph = {
                    rnd.nextDouble() * Math.PI * 2,
                    rnd.nextDouble() * Math.PI * 2,
                    rnd.nextDouble() * Math.PI * 2
            };
            for (int i = 0; i < points; i++) {
                double a = (double) i / points * Math.PI * 2;
                double rr = 1.0;
                for (int k = 0; k < 3; k++) {
                    rr += mag[k] * Math.cos(freq[k] * a + ph[k]);
                }
                rr = Math.max(0.70, Math.min(1.32, rr));
                rxs[i] = Math.cos(a) * r * rr;
                rys[i] = Math.sin(a) * r * rr * 0.96;
            }
            double sx = 0.88 + rnd.nextDouble() * 0.3;
            double sy = 0.88 + rnd.nextDouble() * 0.22;
            double rot = rnd.nextDouble() * Math.PI;
            double ca = Math.cos(rot), sa = Math.sin(rot);
            for (int i = 0; i < points; i++) {
                double dx = rxs[i], dy = rys[i];
                double rx = dx * ca + dy * sa;
                double ry = -dx * sa + dy * ca;
                rx *= sx;
                ry *= sy;
                rxs[i] = rx * ca - ry * sa;
                rys[i] = rx * sa + ry * ca;
            }
            cache = new GoldCache(rxs, rys, r >= 18, r >= 20 && (seed & 1) == 0,
                    Color.rgb(115, 72, 0),
                    Color.rgb(120, 75, 0, 0.85),
                    Color.rgb(255, 245, 150, 0.55));
            goldShapeCache.put(item, cache);
        }

        // 位置变化时重算绝对坐标与渐变（静止时复用，零分配）
        if (cache.lastCx != cx || cache.lastCy != cy) {
            for (int i = 0; i < cache.rxs.length; i++) {
                cache.xs[i] = cx + cache.rxs[i];
                cache.ys[i] = cy + cache.rys[i];
            }
            cache.fill = new RadialGradient(
                    0, 0,
                    cx - r * 0.35, cy - r * 0.4,
                    r * 1.35, false, CycleMethod.NO_CYCLE,
                    new Stop(0.00, Color.rgb(255, 255, 205)),
                    new Stop(0.35, Color.rgb(255, 228, 45)),
                    new Stop(0.75, Color.rgb(240, 180, 10)),
                    new Stop(1.00, Color.rgb(175, 115, 0)));
            cache.lastCx = cx;
            cache.lastCy = cy;
        }

        gc.setFill(cache.fill);
        gc.fillPolygon(cache.xs, cache.ys, cache.xs.length);

        gc.setStroke(cache.strokeColor);
        gc.setLineWidth(1.8);
        gc.strokePolygon(cache.xs, cache.ys, cache.xs.length);

        if (cache.hasNotch) {
            gc.setStroke(cache.notchColor);
            gc.setLineWidth(1.3);
            gc.beginPath();
            gc.moveTo(cx - r * 0.02, cy - r * 0.82);
            gc.quadraticCurveTo(cx + r * 0.14, cy - r * 1.02, cx + r * 0.24, cy - r * 0.78);
            gc.stroke();
        }

        if (cache.hasSpot) {
            gc.setFill(cache.spotColor);
            gc.fillOval(cx - r * 0.48, cy + r * 0.22, r * 0.26, r * 0.18);
        }
    }

    /**
     * 钻石：八面体菱形，浅青蓝渐变 + 白色高光切面。
     * 形状与渐变按物品缓存，位置不变时零分配。
     */
    private void drawDiamond(GraphicsContext gc, Diamond item, double r) {
        double cx = item.getX();
        double cy = item.getY();
        double outerR = r * 1.1;

        DiamondCache cache = diamondShapeCache.get(item);
        if (cache == null) {
            // 首次：计算八面体相对偏移（仅执行一次）
            double[] rxs = new double[8];
            double[] rys = new double[8];
            double[] ox = {
                    0,             -outerR,
                    outerR * 0.6,  -outerR * 0.4,
                    outerR,         0,
                    outerR * 0.6,   outerR * 0.4,
                    0,              outerR,
                    -outerR * 0.6,  outerR * 0.4,
                    -outerR,        0,
                    -outerR * 0.6, -outerR * 0.4,
            };
            for (int i = 0; i < 8; i++) { rxs[i] = ox[i * 2]; rys[i] = ox[i * 2 + 1]; }
            cache = new DiamondCache(rxs, rys);
            diamondShapeCache.put(item, cache);
        }

        if (cache.lastCx != cx || cache.lastCy != cy) {
            for (int i = 0; i < 8; i++) {
                cache.xs[i] = cx + cache.rxs[i];
                cache.ys[i] = cy + cache.rys[i];
            }
            cache.fill = makeRadial(cx, cy, r,
                    Color.rgb(220, 245, 255),
                    Color.rgb(120, 200, 255),
                    Color.rgb(60, 120, 180));
            cache.lastCx = cx;
            cache.lastCy = cy;
        }

        gc.setFill(cache.fill);
        gc.fillPolygon(cache.xs, cache.ys, 8);

        gc.setStroke(Color.rgb(30, 80, 140));
        gc.setLineWidth(1.5);
        gc.strokePolygon(cache.xs, cache.ys, 8);

        // 中间分割线
        gc.setStroke(Color.rgb(180, 230, 255, 0.7));
        gc.setLineWidth(1.0);
        gc.beginPath();
        gc.moveTo(cx - outerR * 0.9, cy);
        gc.lineTo(cx + outerR * 0.9, cy);
        gc.stroke();

        // 左上三角形高光切面
        gc.setFill(Color.rgb(255, 255, 255, 0.5));
        gc.beginPath();
        gc.moveTo(cx, cy - outerR);
        gc.lineTo(cx - outerR * 0.6, cy - outerR * 0.4);
        gc.lineTo(cx - outerR * 0.15, cy - outerR * 0.15);
        gc.closePath();
        gc.fill();
    }

    /**
     * 石头：每块都不一样的不规则毛石——极坐标多点扰动（鼓包、凹陷、歪斜、上宽下窄各不同），
     * Catmull-Rom 闭合样条保证轮廓处处圆滑；底部同样起伏不平，靠接触阴影接地。
     * 左青灰受光、右紫灰暗面，多层渐隐描边模拟柔和失焦暗边。
     * 形状与渐变按物品缓存，位置不变时零分配。
     */
    private void drawStone(GraphicsContext gc, Stone item, double r) {
        double cx = item.getX();
        double cy = item.getY();

        StoneCache cache = stoneShapeCache.get(item);
        if (cache == null) {
            // 首次：基于 hashCode 计算相对多边形偏移与体态参数（仅执行一次）
            long seed = item.hashCode() & 0x7fffffffL;
            Random rnd = new Random(seed);
            double hw = r * (0.80 + rnd.nextDouble() * 0.32);
            double hh = r * (0.68 + rnd.nextDouble() * 0.24);
            double lean = (rnd.nextDouble() - 0.5) * 0.14;
            double taper = 0.05 + rnd.nextDouble() * 0.12;
            double ph1 = rnd.nextDouble() * Math.PI * 2;
            double ph2 = rnd.nextDouble() * Math.PI * 2;
            double ph3 = rnd.nextDouble() * Math.PI * 2;
            double a1 = 0.05 + rnd.nextDouble() * 0.07;
            double a2 = 0.03 + rnd.nextDouble() * 0.05;
            double a3 = 0.02 + rnd.nextDouble() * 0.04;
            int n = 10 + rnd.nextInt(3);
            double[] rxs = new double[n], rys = new double[n];
            for (int i = 0; i < n; i++) {
                double th = Math.PI * 2 * i / n + (rnd.nextDouble() - 0.5) * 0.22;
                double sn = Math.sin(th), cs = Math.cos(th);
                double rr = 1
                        + a1 * Math.sin(2 * th + ph1)
                        + a2 * Math.sin(3 * th + ph2)
                        + a3 * Math.sin(5 * th + ph3)
                        + (rnd.nextDouble() - 0.5) * 0.06;
                double wprof = 1 + taper * sn;
                double px = hw * rr * wprof * cs;
                double py = hh * rr * sn;
                px += lean * sn * hw;
                rxs[i] = px;
                rys[i] = py;
            }
            double edgeScale = hw > r ? 1.0 : 0.8;
            cache = new StoneCache(rxs, rys, n, hw, hh, edgeScale);
            stoneShapeCache.put(item, cache);
        }

        // 位置变化时重算绝对坐标与位置相关渐变（静止时复用，零分配）
        if (cache.lastCx != cx || cache.lastCy != cy) {
            for (int i = 0; i < cache.n; i++) {
                cache.xs[i] = cx + cache.rxs[i];
                cache.ys[i] = cy + cache.rys[i];
            }
            cache.highlight = new RadialGradient(
                    0, 0,
                    cx - cache.hw * 0.35, cy - cache.hh * 0.38,
                    cache.hw * 1.4, false, CycleMethod.NO_CYCLE,
                    new Stop(0.0, Color.rgb(182, 194, 188, 0.35)),
                    new Stop(1.0, Color.rgb(182, 194, 188, 0.0)));
            cache.shadow = new RadialGradient(
                    0, 0,
                    cx + cache.hw * 0.42, cy + cache.hh * 0.30,
                    cache.hw * 1.35, false, CycleMethod.NO_CYCLE,
                    new Stop(0.0, Color.rgb(66, 62, 74, 0.38)),
                    new Stop(1.0, Color.rgb(66, 62, 74, 0.0)));
            cache.ground = new RadialGradient(
                    0, 0,
                    cx, cy + cache.hh * 0.62,
                    cache.hw * 1.1, false, CycleMethod.NO_CYCLE,
                    new Stop(0.0, Color.rgb(44, 38, 46, 0.28)),
                    new Stop(1.0, Color.rgb(44, 38, 46, 0.0)));
            cache.lastCx = cx;
            cache.lastCy = cy;
        }

        double hw = cache.hw;
        double hh = cache.hh;
        double edgeScale = cache.edgeScale;

        // ---- 底部柔和接触影 ----
        gc.setFill(STONE_SHADOW_OVAL);
        gc.fillOval(cx - hw * 0.95, cy + hh * 0.82, hw * 1.9, hh * 0.30);

        // ---- 四层渐隐粗暗边（由宽到窄、由淡到深，外侧半圈羽化）----
        traceStonePath(gc, cache.xs, cache.ys);
        gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        gc.setLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
        gc.setLineWidth(10 * edgeScale); gc.setStroke(STONE_EDGE_W4); gc.stroke();
        traceStonePath(gc, cache.xs, cache.ys);
        gc.setLineWidth(6 * edgeScale); gc.setStroke(STONE_EDGE_W6); gc.stroke();
        traceStonePath(gc, cache.xs, cache.ys);
        gc.setLineWidth(3.2 * edgeScale); gc.setStroke(STONE_EDGE_W32); gc.stroke();
        traceStonePath(gc, cache.xs, cache.ys);
        gc.setLineWidth(1.6 * edgeScale); gc.setStroke(STONE_EDGE_W16); gc.stroke();

        // ---- 主体：柔和对角渐变（青灰 → 中性灰 → 紫灰，比例坐标全局复用）----
        traceStonePath(gc, cache.xs, cache.ys);
        gc.setFill(STONE_BODY_FILL);
        gc.fill();

        // ---- 大尺度柔光（全部裁剪在轮廓内，无可辨斑块边界）----
        gc.save();
        traceStonePath(gc, cache.xs, cache.ys);
        gc.clip();
        gc.setFill(cache.highlight);
        gc.fillRect(cx - hw * 1.6, cy - hh * 1.6, hw * 3.2, hh * 3.2);
        gc.setFill(cache.shadow);
        gc.fillRect(cx - hw * 1.6, cy - hh * 1.6, hw * 3.2, hh * 3.2);
        gc.setFill(cache.ground);
        gc.fillRect(cx - hw * 1.4, cy - hh * 1.4, hw * 2.8, hh * 2.8);
        gc.restore();
    }

    /**
     * 按不规则轮廓点构建闭合路径：Catmull-Rom 样条转三次贝塞尔，
     * 段间切线天然连续（C1），鼓包和凹陷都保持圆滑、无尖角。
     */
    private void traceStonePath(GraphicsContext gc, double[] xs, double[] ys) {
        int n = xs.length;
        gc.beginPath();
        gc.moveTo(xs[0], ys[0]);
        for (int i = 0; i < n; i++) {
            double x0 = xs[(i - 1 + n) % n], y0 = ys[(i - 1 + n) % n];
            double x1 = xs[i],               y1 = ys[i];
            double x2 = xs[(i + 1) % n],     y2 = ys[(i + 1) % n];
            double x3 = xs[(i + 2) % n],     y3 = ys[(i + 2) % n];
            gc.bezierCurveTo(x1 + (x2 - x0) / 6.0, y1 + (y2 - y0) / 6.0,
                             x2 - (x3 - x1) / 6.0, y2 - (y3 - y1) / 6.0,
                             x2, y2);
        }
        gc.closePath();
    }

    /**
     * 鼹鼠（侧视氃匍爬行）：驼背深棕身体 + 尖头橙鼻 + 细尾 + 前后两腿交替摆动，
     * 爬行时身体轻微起伏、停顿时伏在地上。按 getFacing() 左右镜像。
     */
    private void drawMole(GraphicsContext gc, Mole item, double r) {
        double cx = item.getX();
        double cy = item.getY();
        int facing = item.getFacing();
        boolean moving = item.isCrawling() && !item.isGrabbed();
        double phase = item.getLegPhase();

        // ===== 贴图优先分支 =====
        Image sprite = loadMoleSprite();
        if (sprite != null) {
            // 爬动中身体随步伐起伏+前后倾（幅度比钻石猪轻——鼹鼠身体更小）
            double bobY = moving ? -0.05 * r * Math.abs(Math.sin(phase)) : 0;
            double tilt = moving ? 0.05 * Math.sin(phase) : 0; // 约 3°
            gc.save();
            gc.translate(cx, cy + bobY);
            gc.rotate(Math.toDegrees(tilt * facing)); // 头朝哪边就向哪边倾
            gc.scale(facing, 1);                      // 原图面朝右，朝左移动时镜像
            double tw = r * 2.8;
            double th = tw * (sprite.getHeight() / sprite.getWidth());
            gc.drawImage(sprite, -tw / 2, -th / 2, tw, th);
            gc.restore();
            return;
        }

        // ===== 矢量兜底 =====

        // 以朝右为基准绘制，朝左整体镜像
        gc.save();
        gc.translate(cx, cy);
        gc.scale(facing, 1);
        gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        gc.setLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);

        // 身体随步伐平滑起伏（sin²：过零点导数为 0，无"咯嚓"尖峰）；停顿/被抓时伏地
        double bob = moving ? (0.5 - 0.5 * Math.cos(2 * phase)) * r * 0.05 : 0;
        gc.translate(0, -bob);

        // 1. 细尾（爬行时尾尖随步伐轻摆）
        double tailWag = moving ? Math.sin(phase + 0.6) * r * 0.07 : 0;
        gc.setStroke(Color.rgb(90, 52, 20));
        gc.setLineWidth(3.0);
        gc.beginPath();
        gc.moveTo(-r * 0.92, r * 0.02);
        gc.quadraticCurveTo(-r * 1.24, -r * 0.05, -r * 1.36 + tailWag, -r * 0.36);
        gc.stroke();

        // 2. 步态参数：脚前后位移 0.35r（与爬行速度匹配，撑地不打滑）；
        //    前摆半周（cos>0）抬腿，后蹬半周脚贴地面
        double stepX = moving ? r * 0.35 : 0;
        double lift = moving ? r * 0.16 : 0;
        double groundY = r * 0.70;
        gc.setLineWidth(r * 0.19);

        // 后腿（相位 phase，颜色略深显远）
        double bp = phase;
        double bfx = -r * 0.42 + Math.sin(bp) * stepX;
        double bfy = groundY - Math.max(0, Math.cos(bp)) * lift;
        gc.setStroke(Color.rgb(64, 36, 14));
        gc.beginPath();
        gc.moveTo(-r * 0.42, r * 0.26);
        gc.lineTo(bfx, bfy);
        gc.stroke();
        gc.setFill(Color.rgb(64, 36, 14));
        gc.fillOval(bfx - r * 0.15, bfy - r * 0.05, r * 0.3, r * 0.13);

        // 前腿（反相 phase+π）
        double fp = phase + Math.PI;
        double ffx = r * 0.42 + Math.sin(fp) * stepX;
        double ffy = groundY - Math.max(0, Math.cos(fp)) * lift;
        gc.setStroke(Color.rgb(84, 48, 19));
        gc.beginPath();
        gc.moveTo(r * 0.42, r * 0.26);
        gc.lineTo(ffx, ffy);
        gc.stroke();
        gc.setFill(Color.rgb(84, 48, 19));
        gc.fillOval(ffx - r * 0.15, ffy - r * 0.05, r * 0.3, r * 0.13);

        // 3. 身体+头一体侧影：流畅驼背水滴形，背峰居中、头部自然收窄、腹线压平（氃匍姿态）
        gc.setFill(new RadialGradient(
                0, 0,
                -r * 0.25, -r * 0.5, r * 1.45, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.rgb(150, 95, 46)),
                new Stop(0.65, Color.rgb(112, 66, 28)),
                new Stop(1.0, Color.rgb(78, 44, 17))));
        gc.beginPath();
        gc.moveTo(-r * 0.92, r * 0.05);                                    // 尾根
        gc.quadraticCurveTo(-r * 0.95, -r * 0.55, -r * 0.55, -r * 0.78);   // 后腰上弧
        gc.quadraticCurveTo(-r * 0.05, -r * 0.92, r * 0.35, -r * 0.70);    // 背峰（居中）
        gc.quadraticCurveTo(r * 0.62, -r * 0.62, r * 0.82, -r * 0.40);     // 颈背→头顶
        gc.quadraticCurveTo(r * 0.98, -r * 0.24, r * 1.02, -r * 0.05);     // 鼻梁→鼻尖
        gc.quadraticCurveTo(r * 1.00, r * 0.12, r * 0.80, r * 0.20);       // 下颚
        gc.quadraticCurveTo(r * 0.60, r * 0.30, r * 0.40, r * 0.35);       // 喉→胸
        gc.quadraticCurveTo(-r * 0.15, r * 0.62, -r * 0.60, r * 0.48);     // 压平的腹线
        gc.quadraticCurveTo(-r * 0.88, r * 0.38, -r * 0.92, r * 0.05);     // 腰→尾根
        gc.closePath();
        gc.fill();
        gc.setStroke(Color.rgb(58, 31, 11));
        gc.setLineWidth(1.5);
        gc.stroke();

        // 4. 圆耳朵（头顶靠后）
        gc.setFill(Color.rgb(74, 42, 16));
        gc.fillOval(r * 0.22, -r * 0.72, r * 0.22, r * 0.24);

        // 5. 鼻部橙黄脸颊斑 + 深色鼻尖（贴合吻部轮廓）
        gc.setFill(Color.rgb(232, 150, 85));
        gc.fillOval(r * 0.74, -r * 0.15, r * 0.3, r * 0.26);
        gc.setFill(Color.rgb(45, 24, 9));
        gc.fillOval(r * 0.94, -r * 0.08, r * 0.13, r * 0.12);

        // 6. 眼睛（黑 + 白点高光）
        gc.setFill(Color.BLACK);
        gc.fillOval(r * 0.55, -r * 0.45, r * 0.14, r * 0.14);
        gc.setFill(Color.WHITE);
        gc.fillOval(r * 0.58, -r * 0.42, r * 0.05, r * 0.05);

        gc.restore();
    }

    /**
     * 福袋：优先使用美术原图（mystery_bag.png，已软键控抠透明）；
     * 静止物体，加 ±0.02r 呼吸 bob 让画面有微生命感。矢量兜底保持历史造型。
     */
    private void drawMysteryBag(GraphicsContext gc, MysteryBag item, double r) {
        double cx = item.getX();
        double cy = item.getY();
        Image sprite = loadBagSprite();

        // ===== 贴图优先分支 =====
        if (sprite != null) {
            // 呼吸 bob：静止物体 ±0.02r 微起伏，周期 2.2s
            double t = System.nanoTime() * 1e-9;
            double bobY = -Math.sin(t * Math.PI * 2 / 2.2) * 0.02 * r;
            gc.save();
            gc.translate(cx, cy + bobY);
            double tw = r * 3.2;
            double th = tw * (sprite.getHeight() / sprite.getWidth());
            gc.drawImage(sprite, -tw / 2, -th / 2, tw, th);
            gc.restore();
            return;
        }

        // ===== 矢量兜底 =====
        double neckY = cy - r * 0.85;

        // 1. 顶部 5 片张开的布（尖齿片形，中间最高）
        for (int k = 0; k < 5; k++) {
            double off = (k - 2) * r * 0.16;
            double tipY = neckY - r * 0.55 - (2 - Math.abs(k - 2)) * r * 0.22;
            gc.setFill(Color.rgb(248, 222, 150));
            gc.beginPath();
            gc.moveTo(cx + off - r * 0.09, neckY);
            gc.lineTo(cx + off, tipY);
            gc.lineTo(cx + off + r * 0.09, neckY);
            gc.closePath();
            gc.fill();
            gc.setStroke(Color.rgb(130, 90, 30));
            gc.setLineWidth(1.4);
            gc.stroke();
        }

        // 2. 圆球袋身（米黄渐变，底部微金）
        gc.setFill(new RadialGradient(
                0, 0,
                cx - r * 0.3, cy - r * 0.35,
                r * 1.35, false, CycleMethod.NO_CYCLE,
                new Stop(0.00, Color.rgb(255, 242, 198)),
                new Stop(0.60, Color.rgb(246, 216, 142)),
                new Stop(1.00, Color.rgb(198, 158, 84))));
        gc.fillOval(cx - r * 1.02, cy - r * 0.95, r * 2.04, r * 2.0);

        // 3. 袋身描边
        gc.setStroke(Color.rgb(130, 90, 30));
        gc.setLineWidth(1.8);
        gc.strokeOval(cx - r * 1.02, cy - r * 0.95, r * 2.04, r * 2.0);

        // 4. 颈部收口（小梯形盖住袋身与布片的接缝）
        double[] nx = {cx - r * 0.3, cx + r * 0.3, cx + r * 0.22, cx - r * 0.22};
        double[] ny = {neckY, neckY, neckY + r * 0.28, neckY + r * 0.28};
        gc.setFill(Color.rgb(240, 210, 135));
        gc.fillPolygon(nx, ny, 4);
        gc.setStroke(Color.rgb(130, 90, 30));
        gc.setLineWidth(1.4);
        gc.strokePolygon(nx, ny, 4);

        // 5. 白色系绳（横带 + 左侧绳结）
        gc.setStroke(Color.rgb(245, 245, 235));
        gc.setLineWidth(3.0);
        gc.beginPath();
        gc.moveTo(cx - r * 0.32, neckY + r * 0.08);
        gc.lineTo(cx + r * 0.32, neckY + r * 0.08);
        gc.stroke();
        gc.setFill(Color.rgb(240, 240, 230));
        gc.fillOval(cx - r * 0.42, neckY, r * 0.16, r * 0.16);

        // 6. 橙色大问号
        gc.setFill(Color.rgb(232, 108, 12));
        gc.setFont(javafx.scene.text.Font.font("Arial",
                javafx.scene.text.FontWeight.BOLD, r * 1.7));
        gc.fillText("?", cx - r * 0.34, cy + r * 0.5);
    }

    /**
     * TNT 桶：优先使用美术原图（tnt.png，已软键控抠透明）；静止物体，
     * 加 ±0.02r 呼吸 bob；已爆炸的 Bomb 直接跳过渲染。矢量兜底保留历史造型。
     */
    private void drawBomb(GraphicsContext gc, Bomb item, double r) {
        // 爆炸后的 Bomb 不再渲染
        if (item.isExploded()) return;

        double cx = item.getX();
        double cy = item.getY();
        Image sprite = loadTntSprite();

        // ===== 贴图优先分支 =====
        if (sprite != null) {
            double t = System.nanoTime() * 1e-9;
            double bobY = -Math.sin(t * Math.PI * 2 / 2.5 + 0.5) * 0.02 * r;
            gc.save();
            gc.translate(cx, cy + bobY);
            double tw = r * 3.8;
            double th = tw * (sprite.getHeight() / sprite.getWidth());
            gc.drawImage(sprite, -tw / 2, -th / 2, tw, th);
            gc.restore();
            return;
        }

        // ===== 矢量兜底 =====
        gc.setFill(makeRadial(cx, cy, r,
                Color.rgb(80, 80, 80),
                Color.rgb(30, 30, 30),
                Color.rgb(10, 10, 10)));
        gc.fillOval(cx - r, cy - r, r * 2, r * 2);

        gc.setStroke(Color.rgb(5, 5, 5));
        gc.setLineWidth(1.5);
        gc.strokeOval(cx - r, cy - r, r * 2, r * 2);

        // TNT 红色文字
        gc.setFill(Color.rgb(220, 40, 40));
        gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, r * 0.9));
        gc.fillText("TNT", cx - r * 0.7, cy + r * 0.3);

        // 引线（右上棕色曲线）
        gc.setStroke(Color.rgb(140, 85, 30));
        gc.setLineWidth(2.0);
        gc.beginPath();
        gc.moveTo(cx + r * 0.5, cy - r * 0.7);
        gc.quadraticCurveTo(cx + r * 1.1, cy - r * 1.3, cx + r * 0.9, cy - r * 1.6);
        gc.stroke();

        // 引线顶端火花
        gc.setFill(Color.rgb(255, 200, 30));
        gc.fillOval(cx + r * 0.9 - r * 0.18, cy - r * 1.6 - r * 0.18, r * 0.36, r * 0.36);
    }

    /**
     * FR-11 钻石猪：棕色卡通造型（大圆头、两只粉内耳圆耳、黑点眼、橙色吻鼻、弧形微笑、
     * 圆胖身体、四条短腿、后身上翘细尾），胸前双爪抱着一颗强辉光青蓝钻石，
     * 冲刺时周身白色光环。标准造型面朝左，按 getFacing() 左右镜像。
     */
    private void drawDiamondPig(GraphicsContext gc, DiamondPig pig, double r) {
        // ===== 爬行姿态参数（纯渲染：相位由模型按真实位移累积，这里只读不改坐标） =====
        double blend = pig.getCrawlBlend();                 // 0=伏身停顿, 1=爬动中
        double phase = pig.getLegPhase();
        double dashAmp = pig.isDashing() ? 1.35 : 1.0;     // 冲刺时前倾幅度加大
        // 爬动中身体随步伐上下拱动（|sin| 每步一次），停顿中平滑归零
        double bobY = -blend * 0.085 * r * Math.abs(Math.sin(phase));
        // 前后倾（约 5°，冲刺约 7°）；在世界系旋转，下面按朝向修正方向
        double tilt = blend * 0.085 * dashAmp * Math.sin(phase);
        // 停顿时纵向蹲伏 10%
        double squashY = 1.0 - (1.0 - blend) * 0.10;

        // 优先使用美术原图（/images/diamond_pig.png）：100% 还原参考图
        Image sprite = loadPigSprite();
        if (sprite != null) {
            gc.save();
            gc.translate(pig.getX(), pig.getY() + bobY);
            // 冲刺白色光环
            if (pig.isDashing()) {
                if (pigDashGlowSprite == null || pigDashGlowSpriteR != r) {
                    pigDashGlowSprite = new RadialGradient(0, 0, 0, 0, r * 1.5,
                            false, CycleMethod.NO_CYCLE,
                            new Stop(0, Color.rgb(255, 255, 255, 0.4)),
                            new Stop(1, Color.rgb(255, 255, 255, 0)));
                    pigDashGlowSpriteR = r;
                }
                gc.setFill(pigDashGlowSprite);
                gc.fillOval(-r * 1.5, -r * 1.4, r * 3, r * 2.8);
            }
            // 世界系前后倾（乘朝向：猪头朝哪边就向哪边倾）
            gc.rotate(Math.toDegrees(tilt * pig.getFacing()));
            // 原图面朝左；朝右移动时水平翻转
            gc.scale(-pig.getFacing(), 1);
            double tw = r * 2.9;
            double th = tw * (sprite.getHeight() / sprite.getWidth());
            // 蹲伏：以脚底为锚点纵向压缩，下移补偿使四脚不离开原站位
            double drop = (th / 2) * (1 - squashY);
            gc.translate(0, drop);
            gc.scale(1, squashY);
            gc.drawImage(sprite, -tw / 2, -th / 2, tw, th);
            gc.restore();
            return;
        }

        // 无素材时的矢量兜底绘制
        final Color DARK   = Color.rgb(99, 58, 27);
        final Color BODY   = Color.rgb(178, 113, 58);
        final Color HILITE = Color.rgb(201, 142, 85);
        final Color SHADE  = Color.rgb(146, 88, 40);
        final Color PINK   = Color.rgb(240, 171, 181);
        final Color MUZZLE = Color.rgb(229, 135, 66);
        final Color NOSE   = Color.rgb(168, 86, 39);

        gc.save();
        gc.translate(pig.getX(), pig.getY() + bobY);
        gc.rotate(Math.toDegrees(tilt * pig.getFacing()));
        gc.scale(-pig.getFacing(), 1);   // 标准造型面朝左
        gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        gc.setLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
        double lw = 0.1 * r;

        // 0. 冲刺白色光环（最底层）
        if (pig.isDashing()) {
            if (pigDashGlowVector == null || pigDashGlowVectorR != r) {
                pigDashGlowVector = new RadialGradient(0, 0, -r * 0.1, -r * 0.1, r * 1.55,
                        false, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.rgb(255, 255, 255, 0.35)),
                        new Stop(1, Color.rgb(255, 255, 255, 0)));
                pigDashGlowVectorR = r;
            }
            gc.setFill(pigDashGlowVector);
            gc.fillOval(-r * 1.4, -r * 1.2, r * 2.8, r * 2.5);
        }

        // 1. 后身上翘细尾
        gc.setStroke(DARK);
        gc.setLineWidth(0.13 * r);
        gc.beginPath();
        gc.moveTo(0.82 * r, -0.1 * r);
        gc.quadraticCurveTo(1.24 * r, -0.2 * r, 1.34 * r, -0.52 * r);
        gc.stroke();

        // 2. 两条后腿（短粗圆角柱，远侧颜色深）
        drawPigRoundLeg(gc, 0.66 * r, 0.5 * r, 0.2 * r, 0.34 * r, DARK, SHADE);
        drawPigRoundLeg(gc, 0.32 * r, 0.56 * r, 0.2 * r, 0.36 * r, DARK, BODY);

        // 3. 圆胖身体（前端被头遮住）
        gc.setFill(new RadialGradient(0, 0, 0.25 * r, -0.15 * r, r * 1.1,
                false, CycleMethod.NO_CYCLE,
                new Stop(0, HILITE),
                new Stop(0.65, BODY),
                new Stop(1, SHADE)));
        gc.beginPath();
        gc.moveTo(-0.08 * r, -0.12 * r);
        gc.quadraticCurveTo(0.25 * r, -0.3 * r, 0.6 * r, -0.26 * r);
        gc.quadraticCurveTo(0.9 * r, -0.22 * r, 1.0 * r, -0.02 * r);
        gc.quadraticCurveTo(1.08 * r, 0.28 * r, 0.88 * r, 0.52 * r);
        gc.quadraticCurveTo(0.62 * r, 0.72 * r, 0.3 * r, 0.68 * r);
        gc.quadraticCurveTo(0.0 * r, 0.64 * r, -0.12 * r, 0.42 * r);
        gc.quadraticCurveTo(-0.2 * r, 0.18 * r, -0.08 * r, -0.12 * r);
        gc.closePath();
        gc.fill();
        gc.setStroke(DARK);
        gc.setLineWidth(lw);
        gc.stroke();

        // 4. 两只圆耳朵（深棕外圈 + 棕色 + 粉色内耳），头部随后遮住耳根
        drawPigEar(gc, -0.62 * r, -0.74 * r, 0.21 * r, DARK, BODY, PINK);
        drawPigEar(gc, 0.0 * r, -0.76 * r, 0.19 * r, DARK, BODY, PINK);

        // 5. 大圆头
        double hx = -0.28 * r, hy = -0.16 * r, hr = 0.72 * r;
        gc.setFill(new RadialGradient(0, 0, hx - 0.27 * r, hy - 0.3 * r, hr * 1.35,
                false, CycleMethod.NO_CYCLE,
                new Stop(0, HILITE),
                new Stop(0.6, BODY),
                new Stop(1, SHADE)));
        gc.fillOval(hx - hr, hy - hr, hr * 2, hr * 2);
        gc.setStroke(DARK);
        gc.setLineWidth(lw);
        gc.strokeOval(hx - hr, hy - hr, hr * 2, hr * 2);

        // 6. 面部：黑点双眼
        gc.setFill(Color.BLACK);
        gc.fillOval(-0.595 * r, -0.355 * r, 0.15 * r, 0.15 * r);
        gc.fillOval(-0.135 * r, -0.355 * r, 0.15 * r, 0.15 * r);
        gc.setFill(Color.rgb(255, 255, 255, 0.7));
        gc.fillOval(-0.56 * r, -0.33 * r, 0.045 * r, 0.045 * r);
        gc.fillOval(-0.1 * r, -0.33 * r, 0.045 * r, 0.045 * r);

        // 7. 橙色吻部 + 深色鼻 + 人中 + 弧形微笑
        gc.setFill(MUZZLE);
        gc.fillOval(-0.53 * r, -0.13 * r, 0.46 * r, 0.34 * r);
        gc.setFill(NOSE);
        gc.fillOval(-0.385 * r, -0.075 * r, 0.17 * r, 0.12 * r);
        gc.setStroke(DARK);
        gc.setLineWidth(0.045 * r);
        gc.beginPath();
        gc.moveTo(-0.3 * r, 0.045 * r);
        gc.lineTo(-0.3 * r, 0.09 * r);
        gc.quadraticCurveTo(-0.15 * r, 0.17 * r, -0.03 * r, 0.09 * r);
        gc.stroke();

        // 8. 远侧前腿（垫在钻石后面）
        drawPigRoundLeg(gc, -0.52 * r, 0.48 * r, 0.18 * r, 0.36 * r, DARK, SHADE);

        // 9. 胸前青蓝钻石：辉光光晕 + 阴影模糊 + 宝石本体
        double gx = -0.34 * r, gy = 0.54 * r;
        gc.setFill(new RadialGradient(0, 0, gx, gy, 0.8 * r, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(120, 246, 255, 0.55)),
                new Stop(0.6, Color.rgb(90, 235, 250, 0.22)),
                new Stop(1, Color.rgb(90, 235, 250, 0))));
        gc.fillOval(gx - 0.8 * r, gy - 0.8 * r, 1.6 * r, 1.6 * r);

        gc.beginPath();
        gc.moveTo(gx, gy - 0.4 * r);
        gc.quadraticCurveTo(gx + 0.34 * r, gy - 0.38 * r, gx + 0.38 * r, gy - 0.14 * r);
        gc.quadraticCurveTo(gx + 0.4 * r, gy + 0.02 * r, gx + 0.16 * r, gy + 0.16 * r);
        gc.quadraticCurveTo(gx + 0.05 * r, gy + 0.3 * r, gx, gy + 0.48 * r);
        gc.quadraticCurveTo(gx - 0.05 * r, gy + 0.3 * r, gx - 0.16 * r, gy + 0.16 * r);
        gc.quadraticCurveTo(gx - 0.4 * r, gy + 0.02 * r, gx - 0.38 * r, gy - 0.14 * r);
        gc.quadraticCurveTo(gx - 0.34 * r, gy - 0.38 * r, gx, gy - 0.4 * r);
        gc.closePath();
        gc.setFill(new RadialGradient(0, 0, gx - 0.12 * r, gy - 0.18 * r, 0.62 * r,
                false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(235, 255, 255)),
                new Stop(0.35, Color.rgb(105, 240, 248)),
                new Stop(1, Color.rgb(15, 170, 200))));
        gc.fill();
        // 边缘辉光：两层加粗半透明青蓝描边模拟发光溢出
        gc.setStroke(Color.rgb(120, 245, 255, 0.35));
        gc.setLineWidth(0.2 * r);
        gc.stroke();
        gc.setStroke(Color.rgb(150, 248, 255, 0.18));
        gc.setLineWidth(0.34 * r);
        gc.stroke();
        gc.setStroke(Color.rgb(8, 120, 150));
        gc.setLineWidth(0.045 * r);
        gc.stroke();
        // 切面刻面
        gc.setStroke(Color.rgb(220, 252, 255, 0.55));
        gc.setLineWidth(0.03 * r);
        gc.beginPath();
        gc.moveTo(gx - 0.3 * r, gy + 0.02 * r);
        gc.lineTo(gx + 0.3 * r, gy + 0.02 * r);
        gc.moveTo(gx, gy - 0.38 * r);
        gc.lineTo(gx - 0.3 * r, gy + 0.02 * r);
        gc.moveTo(gx, gy - 0.38 * r);
        gc.lineTo(gx + 0.3 * r, gy + 0.02 * r);
        gc.moveTo(gx, gy + 0.46 * r);
        gc.lineTo(gx - 0.16 * r, gy + 0.16 * r);
        gc.moveTo(gx, gy + 0.46 * r);
        gc.lineTo(gx + 0.16 * r, gy + 0.16 * r);
        gc.stroke();
        // 白色高光
        gc.setFill(Color.rgb(255, 255, 255, 0.85));
        gc.fillOval(gx - 0.2 * r, gy - 0.28 * r, 0.13 * r, 0.2 * r);
        gc.setFill(Color.rgb(255, 255, 255, 0.55));
        gc.fillOval(gx + 0.08 * r, gy - 0.05 * r, 0.08 * r, 0.1 * r);

        // 10. 近侧前腿（压在钻石右下方，呈双爪抱钻）
        drawPigRoundLeg(gc, -0.06 * r, 0.46 * r, 0.18 * r, 0.38 * r, DARK, BODY);

        gc.restore();
    }

    /** 钻石猪短粗圆角腿：深色描边底 + 棕色填充柱（x 为水平中心，y 为顶端） */
    private void drawPigRoundLeg(GraphicsContext gc, double x, double y, double w, double h,
                                 Color outline, Color fill) {
        gc.setFill(outline);
        gc.fillRoundRect(x - w / 2 - 1.6, y - 1.6, w + 3.2, h + 3.2, w, w);
        gc.setFill(fill);
        gc.fillRoundRect(x - w / 2, y, w, h, w * 0.9, w * 0.9);
    }

    /** 钻石猪圆耳朵：深棕外圈 + 棕色中圈 + 粉色内耳 */
    private void drawPigEar(GraphicsContext gc, double ex, double ey, double er,
                            Color dark, Color body, Color pink) {
        gc.setFill(dark);
        gc.fillOval(ex - er, ey - er, er * 2, er * 2);
        gc.setFill(body);
        gc.fillOval(ex - er * 0.74, ey - er * 0.74, er * 1.48, er * 1.48);
        gc.setFill(pink);
        gc.fillOval(ex - er * 0.44, ey - er * 0.44, er * 0.88, er * 0.88);
    }

    /** 钻石猪原图缓存（背景已软键控抠透明）；加载只尝试一次 */
    private static volatile Image pigSprite;
    private static volatile boolean pigSpriteTried;
    /** 福袋贴图缓存（背景已软键控抠透明） */
    private static volatile Image bagSprite;
    private static volatile boolean bagSpriteTried;
    /** TNT 桶贴图缓存（背景已软键控抠透明） */
    private static volatile Image tntSprite;
    private static volatile boolean tntSpriteTried;
    /** 鼹鼠贴图缓存（原图已是透明 PNG，仅做兜底） */
    private static volatile Image moleSprite;
    private static volatile boolean moleSpriteTried;

    /**
     * 加载 /images/diamond_pig.png（美术原图）。原图带浅米色背景且边缘是模糊发光，
     * 采用软键控颜色抠图：与背景键色的距离在 LO~HI 之间线性过渡 alpha；
     * 对半透明边缘做"去底色"，避免贴到矿洞后出现米色彩边；
     * 粉色内耳（R 明显大于 G）单独保护，防止被误抠。
     */
    private Image loadPigSprite() {
        if (pigSpriteTried) {
            return pigSprite;
        }
        synchronized (GameViewImpl.class) {
            if (pigSpriteTried) {
                return pigSprite;
            }
            pigSpriteTried = true;
            try (InputStream is = getClass().getResourceAsStream("/images/diamond_pig.png")) {
                if (is == null) {
                    return null;
                }
                Image raw = new Image(is);
                PixelReader pr = raw.getPixelReader();
                int w = (int) raw.getWidth();
                int h = (int) raw.getHeight();

                // 背景键色：四角平均（实测约 248,220,184 米色）
                long sr = 0, sg = 0, sb = 0, cnt = 0;
                int[][] corners = {{0, 0}, {w - 1, 0}, {0, h - 1}, {w - 1, h - 1}};
                for (int[] c : corners) {
                    Color kc = pr.getColor(c[0], c[1]);
                    sr += (long) (kc.getRed() * 255);
                    sg += (long) (kc.getGreen() * 255);
                    sb += (long) (kc.getBlue() * 255);
                    cnt++;
                }
                int kr = (int) (sr / cnt), kg = (int) (sg / cnt), kb = (int) (sb / cnt);

                int[] buf = new int[w * h];
                pr.getPixels(0, 0, w, h,
                        javafx.scene.image.PixelFormat.getIntArgbInstance(), buf, 0, w);

                // 主体颜色（棕/橙/青钻）距背景键色 >160；仅对锯齿过渡像素落在 80~135
                final double LO = 80, HI = 135;
                for (int i = 0; i < buf.length; i++) {
                    int p = buf[i];
                    int a0 = (p >>> 24);
                    if (a0 == 0) {
                        buf[i] = 0;
                        continue;
                    }
                    int rr = (p >> 16) & 0xFF, gg = (p >> 8) & 0xFF, bb = p & 0xFF;
                    int dr = rr - kr, dg = gg - kg, db = bb - kb;
                    double d = Math.sqrt(dr * dr + dg * dg + db * db);

                    int a;
                    // 粉色内耳保护：米色 R-G≤8，粉色 R-G>45
                    boolean pinkInnerEar = (rr - gg) > 45 && rr > 180;
                    if (pinkInnerEar || d >= HI) {
                        a = 255;
                    } else if (d <= LO) {
                        a = 0;
                    } else {
                        a = (int) Math.round((d - LO) / (HI - LO) * 255);
                    }

                    if (a == 0) {
                        buf[i] = 0;
                        continue;
                    }
                    // 去底色：把边缘半透明像素中混入的米色减掉，贴任何背景都不泛黄
                    int orr = rr, ogg = gg, obb = bb;
                    if (a < 255) {
                        double f = 255.0 / a;
                        orr = Math.max(0, Math.min(255, (int) Math.round(kr + dr * f)));
                        ogg = Math.max(0, Math.min(255, (int) Math.round(kg + dg * f)));
                        obb = Math.max(0, Math.min(255, (int) Math.round(kb + db * f)));
                    }
                    buf[i] = (a << 24) | (orr << 16) | (ogg << 8) | obb;
                }

                WritableImage out = new WritableImage(w, h);
                PixelWriter pw = out.getPixelWriter();
                pw.setPixels(0, 0, w, h,
                        javafx.scene.image.PixelFormat.getIntArgbInstance(), buf, 0, w);
                pigSprite = out;
            } catch (Exception e) {
                pigSprite = null;
            }
            return pigSprite;
        }
    }

    /** 加载 /images/mystery_bag.png（福袋美术原图，已软键控抠透明） */
    private Image loadBagSprite() {
        if (bagSpriteTried) return bagSprite;
        synchronized (GameViewImpl.class) {
            if (bagSpriteTried) return bagSprite;
            bagSpriteTried = true;
            try (InputStream is = getClass().getResourceAsStream("/images/mystery_bag.png")) {
                bagSprite = is == null ? null : new Image(is);
            } catch (Exception e) {
                bagSprite = null;
            }
            return bagSprite;
        }
    }

    /** 加载 /images/tnt.png（已预处理为透明 PNG） */
    private Image loadTntSprite() {
        if (tntSpriteTried) return tntSprite;
        synchronized (GameViewImpl.class) {
            if (tntSpriteTried) return tntSprite;
            tntSpriteTried = true;
            try (InputStream is = getClass().getResourceAsStream("/images/tnt.png")) {
                tntSprite = is == null ? null : new Image(is);
            } catch (Exception e) {
                tntSprite = null;
            }
            return tntSprite;
        }
    }

    /** 加载 /images/mole.png（已预处理为透明 PNG） */
    private Image loadMoleSprite() {
        if (moleSpriteTried) return moleSprite;
        synchronized (GameViewImpl.class) {
            if (moleSpriteTried) return moleSprite;
            moleSpriteTried = true;
            try (InputStream is = getClass().getResourceAsStream("/images/mole.png")) {
                moleSprite = is == null ? null : new Image(is);
            } catch (Exception e) {
                moleSprite = null;
            }
            return moleSprite;
        }
    }

    /**
     * 绘制单个钩爪：绳索 → 钩爪贴图。
     * 矿工由 HUDViewImpl 以 ImageView 渲染在 topbg 之上（层级高于 Canvas）；
     * 钩爪贴图根据 getAngle() 旋转朝向。
     */
    private void drawHook(GraphicsContext gc, Hook hook, Color color) {
        if (hook == null) return;

        // 1. 绳索（HUD 层矿工会盖住绳头，视觉上绳从脚下卷绳器出来）
>>>>>>> Stashed changes
        gc.setStroke(color);
        gc.setLineWidth(3);
        gc.strokeLine(hook.getStartX(), hook.getStartY(), hook.getX(), hook.getY());
        // 钩爪头
        gc.setFill(color);
        gc.fillOval(hook.getX() - 8, hook.getY() - 8, 16, 16);
    }

    /** 按物品分值分档选择颜色（用于 Gold/BigGold/Diamond/Stone） */
    private Color colorOf(Item item) {
        int value = item.getScore();
        if (value >= 400) {
            return Color.rgb(230, 90, 90);    // BigGold 大金块：红色
        }
        if (value >= 100) {
            return Color.rgb(120, 200, 255); // Diamond 钻石：浅蓝
        }
        if (value >= 50) {
            return Color.rgb(255, 215, 0);   // Gold 金块：金色
        }
        return Color.rgb(160, 160, 160);      // Stone 石头：灰色
    }
}
