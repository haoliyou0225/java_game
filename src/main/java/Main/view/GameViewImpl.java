// FR-UI GameViewImpl：游戏画面渲染（地面/矿洞/物品/双钩/绳索），来自 feature_ui 分支，已适配 hook 分支 Item/Hook 接口
package Main.view;

import Main.config.Config;
import Main.model.BigGold;
import Main.model.Bomb;
import Main.model.Diamond;
import Main.model.DiamondPig;
import Main.model.GameModel;
import Main.model.Gold;
import Main.model.Hook;
import Main.model.Item;
import Main.model.MediumGold;
import Main.model.MineMap;
import Main.model.Mole;
import Main.model.MysteryBag;
import Main.model.Stone;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

import java.io.InputStream;
import java.util.Random;

/**
 * 游戏画面渲染（FR-28）
 * 布局（贴合效果图）：
 *   HUD（顶部，由 HUDView 叠加）
 *   顶部地面条（钩爪起点所在）
 *   矿洞区域（物品、钩爪、绳索向下抓取）
 *   矿洞底部
 */
public class GameViewImpl implements GameView {

    /** 顶部地面条上沿 Y（与 Config.HUD_HEIGHT 对齐，HUD 下方） */
    private static final double GROUND_TOP = Config.HUD_HEIGHT;
    /** 顶部地面条高度 */
    private static final double GROUND_HEIGHT = 60;

    private final Canvas canvas;

    public GameViewImpl(Canvas canvas) {
        this.canvas = canvas;
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
                if (item instanceof DiamondPig) {
                    drawDiamondPig(gc, (DiamondPig) item, r);
                } else if (item instanceof Bomb) {
                    drawBomb(gc, (Bomb) item, r);
                } else if (item instanceof Mole) {
                    drawMole(gc, (Mole) item, r);
                } else if (item instanceof MysteryBag) {
                    drawMysteryBag(gc, (MysteryBag) item, r);
                } else if (item instanceof Diamond) {
                    drawDiamond(gc, (Diamond) item, r);
                } else if (item instanceof Stone) {
                    drawStone(gc, (Stone) item, r);
                } else {
                    drawGoldNugget(gc, item, r);
                }
            }
        }

        // 6. 玩家1钩爪（蓝色）：起点标记 + 绳索 + 钩爪头
        drawHook(gc, model.getHook1(), Color.DODGERBLUE);

        // 7. 玩家2钩爪（红色）
        drawHook(gc, model.getHook2(), Color.CRIMSON);
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
     * 生成平滑瘤状轮廓：多频余弦波叠加 + 高密度采样，边缘圆滑无棱角。
     * 返回 {xs, ys}。
     */
    private static double[][] blobPoints(double cx, double cy, double r,
                                         int lobes, double amp, double amp2,
                                         long seed, double squashY, int points) {
        Random rnd = new Random(seed);
        double phase1 = rnd.nextDouble() * Math.PI * 2;
        double phase2 = rnd.nextDouble() * Math.PI * 2;
        int lobes2 = lobes + 2 + rnd.nextInt(3);
        double[] xs = new double[points];
        double[] ys = new double[points];
        for (int i = 0; i < points; i++) {
            double a = (double) i / points * Math.PI * 2;
            double rr = 1.0
                    + amp * Math.cos(lobes * a + phase1)
                    + amp2 * Math.cos(lobes2 * a + phase2);
            xs[i] = cx + Math.cos(a) * r * rr;
            ys[i] = cy + Math.sin(a) * r * rr * squashY;
        }
        return new double[][]{xs, ys};
    }

    /**
     * 金块：光滑圆润的瘤状金块（5~6 个圆鼓包），亮黄高光 + 金黄渐变 + 琥珀边缘。
     * 中/大金块额外带顶部小凹痕与左下小金斑；形状基于 hashCode 固定。
     */
    private void drawGoldNugget(GraphicsContext gc, Item item, double r) {
        double cx = item.getX();
        double cy = item.getY();
        long seed = item.hashCode() & 0x7fffffffL;
        Random rnd = new Random(seed);

        // 3 个不可通约的低频波（频率 2/3/5）错位叠加：形状明显不规则（土豆/异形矿石），
        // 但全部是余弦波，边缘处处连续光滑，绝不会出现"角"或细密小疙瘩。
        int points = 64;
        double[] xs = new double[points];
        double[] ys = new double[points];
        int[] freq = {2, 3, 5};
        double[] mag = {
                0.12 + rnd.nextDouble() * 0.05,   // 主频 0.12~0.17
                0.07 + rnd.nextDouble() * 0.04,   // 次频 0.07~0.11
                0.04 + rnd.nextDouble() * 0.03    // 低频细节 0.04~0.07（仍是大尺度起伏）
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
            xs[i] = cx + Math.cos(a) * r * rr;
            ys[i] = cy + Math.sin(a) * r * rr * 0.96;
        }

        // 随机旋转的椭圆拉伸：让每块金块形状不对称、不雷同（边缘依然圆滑）
        double sx = 0.88 + rnd.nextDouble() * 0.3;   // X 轴 0.88~1.18
        double sy = 0.88 + rnd.nextDouble() * 0.22;  // Y 轴 0.88~1.10
        double rot = rnd.nextDouble() * Math.PI;
        double ca = Math.cos(rot), sa = Math.sin(rot);
        for (int i = 0; i < xs.length; i++) {
            double dx = xs[i] - cx, dy = ys[i] - cy;
            double rx = dx * ca + dy * sa;
            double ry = -dx * sa + dy * ca;
            rx *= sx;
            ry *= sy;
            xs[i] = cx + rx * ca - ry * sa;
            ys[i] = cy + rx * sa + ry * ca;
        }

        // 亮黄→金→琥珀 径向渐变（高光在左上）
        gc.setFill(new RadialGradient(
                0, 0,
                cx - r * 0.35, cy - r * 0.4,
                r * 1.35, false, CycleMethod.NO_CYCLE,
                new Stop(0.00, Color.rgb(255, 255, 205)),
                new Stop(0.35, Color.rgb(255, 228, 45)),
                new Stop(0.75, Color.rgb(240, 180, 10)),
                new Stop(1.00, Color.rgb(175, 115, 0))));
        gc.fillPolygon(xs, ys, xs.length);

        // 深琥珀描边
        gc.setStroke(Color.rgb(115, 72, 0));
        gc.setLineWidth(1.8);
        gc.strokePolygon(xs, ys, xs.length);

        if (r >= 18) {
            // 顶部小凹痕（大金块顶部那条自然缝隙）
            gc.setStroke(Color.rgb(120, 75, 0, 0.85));
            gc.setLineWidth(1.3);
            gc.beginPath();
            gc.moveTo(cx - r * 0.02, cy - r * 0.82);
            gc.quadraticCurveTo(cx + r * 0.14, cy - r * 1.02, cx + r * 0.24, cy - r * 0.78);
            gc.stroke();
        }

        if (r >= 20 && (seed & 1) == 0) {
            // 左下侧小金斑（次高光）
            gc.setFill(Color.rgb(255, 245, 150, 0.55));
            gc.fillOval(cx - r * 0.48, cy + r * 0.22, r * 0.26, r * 0.18);
        }
    }

    /**
     * 钻石：八面体菱形，浅青蓝渐变 + 白色高光切面。
     */
    private void drawDiamond(GraphicsContext gc, Diamond item, double r) {
        double cx = item.getX();
        double cy = item.getY();

        double outerR = r * 1.1;
        double[] ox = {
                cx,                    cy - outerR,
                cx + outerR * 0.6,     cy - outerR * 0.4,
                cx + outerR,           cy,
                cx + outerR * 0.6,     cy + outerR * 0.4,
                cx,                    cy + outerR,
                cx - outerR * 0.6,     cy + outerR * 0.4,
                cx - outerR,           cy,
                cx - outerR * 0.6,     cy - outerR * 0.4,
        };
        double[] px = new double[8];
        double[] py = new double[8];
        for (int i = 0; i < 8; i++) { px[i] = ox[i * 2]; py[i] = ox[i * 2 + 1]; }

        gc.setFill(makeRadial(cx, cy, r,
                Color.rgb(220, 245, 255),
                Color.rgb(120, 200, 255),
                Color.rgb(60, 120, 180)));
        gc.fillPolygon(px, py, 8);

        gc.setStroke(Color.rgb(30, 80, 140));
        gc.setLineWidth(1.5);
        gc.strokePolygon(px, py, 8);

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
     * 石头：每块都不一样的不规则毛石——极坐标多点扰动（鼓包、凹陷、歪斜、
     * 上窄下宽各不同），Catmull-Rom 闭合样条保证轮廓处处圆滑，
     * 底部同样起伏不平，靠接触影接地。左青灰受光、右紫灰暗面，
     * 多层渐隐描边模拟柔和失焦暗边。
     */
    private void drawStone(GraphicsContext gc, Stone item, double r) {
        double cx = item.getX();
        double cy = item.getY();
        long seed = item.hashCode() & 0x7fffffffL;
        Random rnd = new Random(seed);

        // ---- 每块石头独立的体态参数（宽扁/圆墩/歪斜程度都随机）----
        double hw = r * (0.80 + rnd.nextDouble() * 0.32);
        double hh = r * (0.68 + rnd.nextDouble() * 0.24);
        double lean  = (rnd.nextDouble() - 0.5) * 0.14;  // 整体歪斜
        double taper = 0.05 + rnd.nextDouble() * 0.12;   // 上窄下宽
        double ph1 = rnd.nextDouble() * Math.PI * 2;
        double ph2 = rnd.nextDouble() * Math.PI * 2;
        double ph3 = rnd.nextDouble() * Math.PI * 2;
        double a1 = 0.05 + rnd.nextDouble() * 0.07;      // 大尺度鼓包
        double a2 = 0.03 + rnd.nextDouble() * 0.05;      // 中尺度起伏
        double a3 = 0.02 + rnd.nextDouble() * 0.04;      // 小尺度凹凸
        int n = 10 + rnd.nextInt(3);                     // 轮廓点数 10~12

        // ---- 极坐标采样不规则轮廓点（含底部，全程参与起伏）----
        double[] xs = new double[n], ys = new double[n];
        for (int i = 0; i < n; i++) {
            double th = Math.PI * 2 * i / n + (rnd.nextDouble() - 0.5) * 0.22;
            double sn = Math.sin(th), cs = Math.cos(th);
            double rr = 1
                    + a1 * Math.sin(2 * th + ph1)
                    + a2 * Math.sin(3 * th + ph2)
                    + a3 * Math.sin(5 * th + ph3)
                    + (rnd.nextDouble() - 0.5) * 0.06;
            double wprof = 1 + taper * sn;              // 下半部更宽
            double px = hw * rr * wprof * cs;
            double py = hh * rr * sn;
            px += lean * sn * hw;                       // 顶底错位的歪斜感
            xs[i] = cx + px;
            ys[i] = cy + py;
        }

        // ---- 底部柔和接触影 ----
        gc.setFill(Color.rgb(42, 34, 38, 0.16));
        gc.fillOval(cx - hw * 0.95, cy + hh * 0.82, hw * 1.9, hh * 0.30);

        // ---- 四层渐隐软暗边（由宽到窄、由淡到深，外侧半圈羽化）----
        double edgeScale = hw > r ? 1.0 : 0.8;
        traceStonePath(gc, xs, ys);
        gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        gc.setLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
        gc.setLineWidth(10  * edgeScale); gc.setStroke(Color.rgb(48, 42, 50, 0.05)); gc.stroke();
        traceStonePath(gc, xs, ys);
        gc.setLineWidth(6   * edgeScale); gc.setStroke(Color.rgb(48, 42, 50, 0.09)); gc.stroke();
        traceStonePath(gc, xs, ys);
        gc.setLineWidth(3.2 * edgeScale); gc.setStroke(Color.rgb(48, 42, 50, 0.14)); gc.stroke();
        traceStonePath(gc, xs, ys);
        gc.setLineWidth(1.6 * edgeScale); gc.setStroke(Color.rgb(48, 42, 50, 0.20)); gc.stroke();

        // ---- 主体：柔和对角渐变（青灰 → 中性灰 → 紫灰）----
        traceStonePath(gc, xs, ys);
        gc.setFill(new LinearGradient(
                0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0.00, Color.rgb(160, 170, 168)),
                new Stop(0.50, Color.rgb(126, 128, 134)),
                new Stop(1.00, Color.rgb(88, 84, 96))));
        gc.fill();

        // ---- 大尺度柔光（全部裁剪在轮廓内，无可辨斑块边界）----
        gc.save();
        traceStonePath(gc, xs, ys);
        gc.clip();
        // 左上受光
        gc.setFill(new RadialGradient(
                0, 0,
                cx - hw * 0.35, cy - hh * 0.38,
                hw * 1.4, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.rgb(182, 194, 188, 0.35)),
                new Stop(1.0, Color.rgb(182, 194, 188, 0.0))));
        gc.fillRect(cx - hw * 1.6, cy - hh * 1.6, hw * 3.2, hh * 3.2);
        // 右下暗部
        gc.setFill(new RadialGradient(
                0, 0,
                cx + hw * 0.42, cy + hh * 0.30,
                hw * 1.35, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.rgb(66, 62, 74, 0.38)),
                new Stop(1.0, Color.rgb(66, 62, 74, 0.0))));
        gc.fillRect(cx - hw * 1.6, cy - hh * 1.6, hw * 3.2, hh * 3.2);
        // 底部接地柔暗
        gc.setFill(new RadialGradient(
                0, 0,
                cx, cy + hh * 0.62,
                hw * 1.1, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.rgb(44, 38, 46, 0.28)),
                new Stop(1.0, Color.rgb(44, 38, 46, 0.0))));
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
     * 鼹鼠（侧视匍匐爬行）：驼背深棕身体 + 尖头橙鼻 + 细尾 + 前后两腿交替摆动，
     * 爬行时身体轻微起伏、停顿伏在地上。按 getFacing() 左右镜像。
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
            double tilt = moving ? 0.05 * Math.sin(phase) : 0; // ≈3°
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

        // 身体随步伐平滑起伏（sin²：过零点导数为 0，无"咯噔"尖峰）；停顿/被抓时伏地
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

        // 3. 身体+头一体侧影：流畅驼背水滴形，背峰居中、头部自然收窄、腹线压平（匍匐姿态）
        gc.setFill(new RadialGradient(
                0, 0,
                -r * 0.25, -r * 0.5, r * 1.45, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.rgb(150, 95, 46)),
                new Stop(0.65, Color.rgb(112, 66, 28)),
                new Stop(1.0, Color.rgb(78, 44, 17))));
        gc.beginPath();
        gc.moveTo(-r * 0.92, r * 0.05);                                    // 尾根
        gc.quadraticCurveTo(-r * 0.95, -r * 0.55, -r * 0.55, -r * 0.78);   // 后腰上弓
        gc.quadraticCurveTo(-r * 0.05, -r * 0.92, r * 0.35, -r * 0.70);    // 背峰（居中）
        gc.quadraticCurveTo(r * 0.62, -r * 0.62, r * 0.82, -r * 0.40);     // 颈背→头顶
        gc.quadraticCurveTo(r * 0.98, -r * 0.24, r * 1.02, -r * 0.05);     // 鼻梁→鼻尖
        gc.quadraticCurveTo(r * 1.00, r * 0.12, r * 0.80, r * 0.20);       // 下颌
        gc.quadraticCurveTo(r * 0.60, r * 0.30, r * 0.40, r * 0.35);       // 喉→胸
        gc.quadraticCurveTo(-r * 0.15, r * 0.62, -r * 0.60, r * 0.48);     // 压平的腹线
        gc.quadraticCurveTo(-r * 0.88, r * 0.38, -r * 0.92, r * 0.05);     // 腹→尾根
        gc.closePath();
        gc.fill();
        gc.setStroke(Color.rgb(58, 31, 11));
        gc.setLineWidth(1.5);
        gc.stroke();

        // 4. 圆耳朵（头顶靠后）
        gc.setFill(Color.rgb(74, 42, 16));
        gc.fillOval(r * 0.22, -r * 0.72, r * 0.22, r * 0.24);

        // 5. 吻部橙黄色脸颊斑 + 深色鼻尖（贴合吻部轮廓）
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
     * 静止物体，加 ±0.02r 呼吸 bob 让画面有微生命感。
     * 矢量兜底保持历史造型。
     */
    private void drawMysteryBag(GraphicsContext gc, MysteryBag item, double r) {
        double cx = item.getX();
        double cy = item.getY();
        Image sprite = loadBagSprite();
        try (java.io.PrintWriter pw = new java.io.PrintWriter(
                new java.io.FileWriter("d:/java/java_game/target/run_diag.log", true))) {
            pw.printf("[drawMysteryBag] x=%.0f y=%.0f r=%.1f sprite=%b size=%s%n",
                    cx, cy, r, sprite != null,
                    sprite != null ? ((int) sprite.getWidth() + "x" + (int) sprite.getHeight()) : "null");
        } catch (Exception ignored) {}

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

        // 1. 顶部 5 片张开的布（尖齿扇形，中间最高）
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
        try (java.io.PrintWriter pw = new java.io.PrintWriter(
                new java.io.FileWriter("d:/java/java_game/target/run_diag.log", true))) {
            pw.printf("[drawBomb] x=%.0f y=%.0f r=%.1f sprite=%b%n", cx, cy, r, sprite != null);
        } catch (Exception ignored) {}

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
     * FR-11 钻石猪：棕色卡通造型（大圆头、两只粉内耳圆耳、黑点眼、橙色吻鼻+微笑、
     * 圆胖身体、四条短腿、后身上翘细尾），胸前双爪抱着一颗强辉光青蓝钻石；
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
                gc.setFill(new RadialGradient(0, 0, 0, 0, r * 1.5,
                        false, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.rgb(255, 255, 255, 0.4)),
                        new Stop(1, Color.rgb(255, 255, 255, 0))));
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
            gc.setFill(new RadialGradient(0, 0, -r * 0.1, -r * 0.1, r * 1.55,
                    false, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.rgb(255, 255, 255, 0.35)),
                    new Stop(1, Color.rgb(255, 255, 255, 0))));
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
     * 采用软颜色键控：与背景键色的距离在 LO~HI 之间线性过渡 alpha，
     * 对半透明边缘做"去底色"避免贴到矿洞后出现米色彩边；
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

                // 主体颜色（棕/橙/青钻）距背景均 >160；仅抗锯齿过渡像素落在 80~135
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
                    // 粉色内耳保护：米色 R-G≈28，粉色 R-G>45
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

    /**
     * Border-inward Flood Fill 抠图（方向反过来！从四条边向内标记背景）。
     * 四条边的像素保证是背景，从所有 border 像素同时入队 BFS 向内扩散，
     * 每个像素与任一种子阈值内即标记为背景(alpha=0)。天然处理渐变背景。
     * 主体被背景包围则永远不会被标记——即使主体内部有与背景相似的颜色也安全。
     * RGB 永不重采样，只改 alpha 通道。
     *
     * @param raw       原始 Image（带不透明背景）
     * @param threshold 色差阈值（与任一边界像素的欧几里得距离）
     * @param soft      软边缘：主体像素周围有多少背景邻居就给多少半透明 alpha
     */
    private static Image cutoutByFloodFill(Image raw, int threshold, int soft) {
        PixelReader pr = raw.getPixelReader();
        int w = (int) raw.getWidth();
        int h = (int) raw.getHeight();
        try {
        int[] buf = new int[w * h];
        pr.getPixels(0, 0, w, h,
                javafx.scene.image.PixelFormat.getIntArgbInstance(), buf, 0, w);

        // 背景 mask：true = 背景
        boolean[] bg = new boolean[w * h];
        int[] qx = new int[w * h];
        int[] qy = new int[w * h];
        int qHead = 0, qTail = 0;

        // 四条边全部入队 + 标记
        for (int x = 0; x < w; x++) {
            qx[qTail] = x; qy[qTail] = 0; bg[x] = true; qTail++;
            qx[qTail] = x; qy[qTail] = h - 1; bg[(h - 1) * w + x] = true; qTail++;
        }
        for (int y = 1; y < h - 1; y++) {
            qx[qTail] = 0; qy[qTail] = y; bg[y * w] = true; qTail++;
            qx[qTail] = w - 1; qy[qTail] = y; bg[y * w + w - 1] = true; qTail++;
        }

        int thr2 = threshold * threshold;
        int bgCount = qTail;
        while (qHead < qTail) {
            int x = qx[qHead], y = qy[qHead]; qHead++;
            int idx = y * w + x;
            // 当前像素（已标记背景）的颜色
            int cp = buf[idx];
            int cr = (cp >> 16) & 0xFF, cg = (cp >> 8) & 0xFF, cb = cp & 0xFF;

            // 检查四邻居：色差内则标记为背景并入队
            // 右
            if (x + 1 < w) {
                int nIdx = idx + 1;
                if (!bg[nIdx]) {
                    int np = buf[nIdx];
                    int dr = ((np >> 16) & 0xFF) - cr, dg = ((np >> 8) & 0xFF) - cg, db = (np & 0xFF) - cb;
                    if (dr * dr + dg * dg + db * db <= thr2 || (np >>> 24) == 0) {
                        bg[nIdx] = true; qx[qTail] = x + 1; qy[qTail] = y; qTail++; bgCount++;
                    }
                }
            }
            if (x - 1 >= 0) {
                int nIdx = idx - 1;
                if (!bg[nIdx]) {
                    int np = buf[nIdx];
                    int dr = ((np >> 16) & 0xFF) - cr, dg = ((np >> 8) & 0xFF) - cg, db = (np & 0xFF) - cb;
                    if (dr * dr + dg * dg + db * db <= thr2 || (np >>> 24) == 0) {
                        bg[nIdx] = true; qx[qTail] = x - 1; qy[qTail] = y; qTail++; bgCount++;
                    }
                }
            }
            if (y + 1 < h) {
                int nIdx = idx + w;
                if (!bg[nIdx]) {
                    int np = buf[nIdx];
                    int dr = ((np >> 16) & 0xFF) - cr, dg = ((np >> 8) & 0xFF) - cg, db = (np & 0xFF) - cb;
                    if (dr * dr + dg * dg + db * db <= thr2 || (np >>> 24) == 0) {
                        bg[nIdx] = true; qx[qTail] = x; qy[qTail] = y + 1; qTail++; bgCount++;
                    }
                }
            }
            if (y - 1 >= 0) {
                int nIdx = idx - w;
                if (!bg[nIdx]) {
                    int np = buf[nIdx];
                    int dr = ((np >> 16) & 0xFF) - cr, dg = ((np >> 8) & 0xFF) - cg, db = (np & 0xFF) - cb;
                    if (dr * dr + dg * dg + db * db <= thr2 || (np >>> 24) == 0) {
                        bg[nIdx] = true; qx[qTail] = x; qy[qTail] = y - 1; qTail++; bgCount++;
                    }
                }
            }
        }

        try (java.io.PrintWriter pw = new java.io.PrintWriter(
                new java.io.FileWriter("d:/java/java_game/target/run_diag.log", true))) {
            pw.printf("[cutout] BORDER-INWARD thresh=%d bg=%d/%d size=%dx%d%n",
                    threshold, bgCount, w * h, w, h);
        } catch (Exception ignored) {}

        // 写 alpha：背景=0，主体=255，主体边缘有背景邻居则给软 alpha
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int idx = y * w + x;
                int p = buf[idx];
                int rr = (p >> 16) & 0xFF, gg = (p >> 8) & 0xFF, bb = p & 0xFF;
                byte a;
                if (bg[idx]) {
                    a = (byte) 0;
                } else {
                    // 主体：看 8 邻域有多少背景邻居，越多越透明（软边缘）
                    int bgN = 0;
                    if (x > 0 && bg[idx - 1]) bgN++;
                    if (x < w - 1 && bg[idx + 1]) bgN++;
                    if (y > 0 && bg[idx - w]) bgN++;
                    if (y < h - 1 && bg[idx + w]) bgN++;
                    if (x > 0 && y > 0 && bg[idx - w - 1]) bgN++;
                    if (x < w - 1 && y > 0 && bg[idx - w + 1]) bgN++;
                    if (x > 0 && y < h - 1 && bg[idx + w - 1]) bgN++;
                    if (x < w - 1 && y < h - 1 && bg[idx + w + 1]) bgN++;
                    if (bgN == 0) {
                        a = (byte) 255;
                    } else {
                        a = (byte) Math.max(32, 255 - bgN * soft);
                    }
                }
                buf[idx] = ((a & 0xFF) << 24) | (rr << 16) | (gg << 8) | bb;
            }
        }

        WritableImage out = new WritableImage(w, h);
        PixelWriter pw2 = out.getPixelWriter();
        pw2.setPixels(0, 0, w, h,
                javafx.scene.image.PixelFormat.getIntArgbInstance(), buf, 0, w);

        // DUMP 质量门禁
        try {
            String name = (w == 59 ? "bag" : w == 93 ? "tnt" : w == 82 ? "mole" : "unknown");
            java.awt.image.BufferedImage bi = new java.awt.image.BufferedImage(
                    w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            bi.setRGB(0, 0, w, h, buf, 0, w);
            javax.imageio.ImageIO.write(bi, "png",
                    new java.io.File("d:/java/java_game/target/dump_" + name + ".png"));
        } catch (Exception ignored) {}

        return out;
        } catch (Throwable t) {
            try (java.io.PrintWriter pw = new java.io.PrintWriter(
                    new java.io.FileWriter("d:/java/java_game/target/run_diag.log", true))) {
                pw.println("[cutout] EXCEPTION " + t);
                t.printStackTrace(pw);
            } catch (Exception ignored2) {}
            return null;
        }
    }

    /**
     * 加载 /images/mystery_bag.png（福袋美术原图）。原图背景渐变色（顶部黄→底部米白），
     * 四角平均键色法失效。改用 Flood Fill 从主体色点 (30,24) 泛洪，阈值 140。
     * RGB 永不重采样，只改 alpha。
     */
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

    /**
     * 加载 /images/tnt.png（已预处理为透明 PNG）。
     */
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

    /**
     * 加载 /images/mole.png（已预处理为透明 PNG）。
     */
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
     * 绘制单个钩爪：地面上的起点标记、绳索、钩爪头圆点。
     */
    private void drawHook(GraphicsContext gc, Hook hook, Color color) {
        if (hook == null) return;

        gc.setFill(color);
        gc.fillRect(hook.getStartX() - 14, hook.getStartY() - 10, 28, 20);

        gc.setStroke(color);
        gc.setLineWidth(3);
        gc.strokeLine(hook.getStartX(), hook.getStartY(), hook.getX(), hook.getY());

        gc.setFill(color);
        gc.fillOval(hook.getX() - 8, hook.getY() - 8, 16, 16);

        // 结算瞬时飘字
        long now = System.currentTimeMillis();
        if (hook.getSettleLabel() != null && now < hook.getSettleLabelUntil()) {
            String text = hook.getSettleLabel();
            double labelX = hook.getStartX() + 20;
            double labelY = hook.getStartY() + 4;
            double boxW = text.chars().mapToDouble(c -> c > 0x2E80 ? 16 : 9).sum() + 8;
            gc.setFill(Color.rgb(0, 0, 0, 0.7));
            gc.fillRect(labelX - 4, labelY - 15, boxW, 20);
            gc.setFill(hook.getSettleIcon() != null ? Color.rgb(255, 215, 0) : Color.WHITE);
            gc.fillText(text, labelX, labelY);
        }
    }
}
