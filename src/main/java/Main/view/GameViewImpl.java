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
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

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
     * 金块/中金块/大金块：棱角分明的不规则金矿石块面 + 金黄径向渐变（底部沾矿土的棕灰） + 深棕描边。
     * 每个金块形状基于对象 hashCode 固定，重绘不变。
     */
    private void drawGoldNugget(GraphicsContext gc, Item item, double r) {
        double cx = item.getX();
        double cy = item.getY();
        long seed = item.hashCode() & 0x7fffffffL;
        Random rnd = new Random(seed);

        // 12 个棱角分明的顶点（模拟金矿石的块面感）
        int corners = 12;
        double[] px = new double[corners];
        double[] py = new double[corners];
        for (int i = 0; i < corners; i++) {
            double angle = (double) i / corners * Math.PI * 2 + rnd.nextDouble() * 0.15;
            // 每个顶点半径独立随机：0.78~1.22 倍 r，形成明显凹凸块面
            double rr = 0.78 + rnd.nextDouble() * 0.44;
            px[i] = cx + Math.cos(angle) * r * rr;
            py[i] = cy + Math.sin(angle) * r * rr;
        }

        // 金黄径向渐变：高光→纯金→金棕→矿土棕灰（底部自然沾上矿洞土色）
        gc.setFill(new RadialGradient(
                0, 0,
                cx - r * 0.35, cy - r * 0.45,
                r * 1.4, false, CycleMethod.NO_CYCLE,
                new Stop(0.00, Color.rgb(255, 245, 100)),   // 高光：亮黄带白
                new Stop(0.35, Color.rgb(255, 215, 50)),   // 亮金
                new Stop(0.70, Color.rgb(230, 170, 30)),   // 暗金
                new Stop(1.00, Color.rgb(140, 90, 35))));  // 底部矿土棕灰

        gc.fillPolygon(px, py, corners);

        // 深棕描边（2px 让轮廓在矿洞背景上清晰）
        gc.setStroke(Color.rgb(70, 40, 5));
        gc.setLineWidth(2.0);
        gc.strokePolygon(px, py, corners);

        // 弧形高光带（模拟光线斜扫金块，不是圆形光斑）
        gc.setFill(Color.rgb(255, 250, 180, 0.55));
        gc.beginPath();
        // 一条弧形亮带：从左上到中间
        gc.moveTo(cx - r * 0.55, cy - r * 0.55);
        gc.quadraticCurveTo(cx - r * 0.15, cy - r * 0.75, cx + r * 0.35, cy - r * 0.35);
        gc.lineTo(cx + r * 0.25, cy - r * 0.22);
        gc.quadraticCurveTo(cx - r * 0.15, cy - r * 0.6, cx - r * 0.48, cy - r * 0.42);
        gc.closePath();
        gc.fill();

        // 高光带的薄描边让它更立体
        gc.setStroke(Color.rgb(255, 255, 220, 0.8));
        gc.setLineWidth(0.8);
        gc.beginPath();
        gc.moveTo(cx - r * 0.55, cy - r * 0.55);
        gc.quadraticCurveTo(cx - r * 0.15, cy - r * 0.75, cx + r * 0.35, cy - r * 0.35);
        gc.stroke();
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
     * 石头：圆滚岩石，40 点平滑不规则轮廓 + 灰蓝渐变 + 底部暗影 + 2~3 条裂纹 + 左上高光。
     * 每块石头基于 hashCode 形状固定但各不相同。
     */
    private void drawStone(GraphicsContext gc, Stone item, double r) {
        double cx = item.getX();
        double cy = item.getY();
        long seed = item.hashCode() & 0x7fffffffL;
        Random rnd = new Random(seed);

        // 40 点平滑轮廓：2~3 个低频正弦波叠加，振幅比金块小（岩石更圆润）
        int points = 40;
        double[] xs = new double[points];
        double[] ys = new double[points];
        int waves = 2 + rnd.nextInt(2);
        double[] waveAmp = new double[waves];
        double[] wavePhase = new double[waves];
        for (int i = 0; i < waves; i++) {
            waveAmp[i] = 0.06 + rnd.nextDouble() * 0.07;
            wavePhase[i] = rnd.nextDouble() * Math.PI * 2;
        }
        // 整体形状微扁（石头比金块扁一点）
        double squashY = 0.88;

        for (int i = 0; i < points; i++) {
            double angle = (double) i / points * Math.PI * 2;
            double rr = 1.0;
            for (int w = 0; w < waves; w++) {
                rr += waveAmp[w] * Math.sin((w + 2) * angle + wavePhase[w]);
            }
            rr = Math.max(0.82, Math.min(1.15, rr));
            xs[i] = cx + Math.cos(angle) * r * rr;
            ys[i] = cy + Math.sin(angle) * r * rr * squashY;
        }

        // 灰蓝渐变：左上高光 → 中灰 → 底部暗灰蓝
        gc.setFill(new RadialGradient(
                0, 0,
                cx - r * 0.3, cy - r * 0.35,
                r * 1.4, false, CycleMethod.NO_CYCLE,
                new Stop(0.00, Color.rgb(215, 218, 222)),
                new Stop(0.50, Color.rgb(140, 144, 152)),
                new Stop(1.00, Color.rgb(60, 62, 68))));
        gc.fillPolygon(xs, ys, points);

        // 底部暗影椭圆（石头放在地面上的接触阴影）
        gc.setFill(Color.rgb(20, 20, 24, 0.35));
        gc.fillOval(cx - r * 0.8, cy + r * 0.6, r * 1.6, r * 0.35);

        // 深灰描边
        gc.setStroke(Color.rgb(35, 35, 40));
        gc.setLineWidth(2.0);
        gc.strokePolygon(xs, ys, points);

        // 裂纹（2~3 条，从边缘向内延伸的短折线）
        gc.setStroke(Color.rgb(80, 82, 88, 0.6));
        gc.setLineWidth(1.0);
        int cracks = 2 + rnd.nextInt(2);
        for (int i = 0; i < cracks; i++) {
            // 裂纹起点在轮廓上
            double startAngle = rnd.nextDouble() * Math.PI * 2;
            double sx = cx + Math.cos(startAngle) * r * 0.9;
            double sy = cy + Math.sin(startAngle) * r * 0.9 * squashY;
            // 裂纹向内延伸，分 2~3 段折线
            gc.beginPath();
            gc.moveTo(sx, sy);
            double px = sx;
            double py = sy;
            int segs = 2 + rnd.nextInt(2);
            for (int s = 0; s < segs; s++) {
                double dx = (rnd.nextDouble() - 0.5) * r * 0.5;
                double dy = (rnd.nextDouble() - 0.5) * r * 0.5;
                px += dx;
                py += dy;
                gc.lineTo(px, py);
            }
            gc.stroke();
        }

        // 左上弧形高光带
        gc.setFill(Color.rgb(240, 242, 248, 0.3));
        gc.beginPath();
        gc.moveTo(cx - r * 0.6, cy - r * 0.5);
        gc.quadraticCurveTo(cx, cy - r * 0.85, cx + r * 0.4, cy - r * 0.4);
        gc.lineTo(cx + r * 0.3, cy - r * 0.28);
        gc.quadraticCurveTo(cx - r * 0.1, cy - r * 0.65, cx - r * 0.52, cy - r * 0.38);
        gc.closePath();
        gc.fill();
    }

    /**
     * 鼹鼠：棕色圆身 + 小耳朵 + 眼睛 + 鼻子 + 小尾巴，朝向移动方向。
     */
    private void drawMole(GraphicsContext gc, Mole item, double r) {
        double cx = item.getX();
        double cy = item.getY();
        int facing = item.getFacing();

        // 尾巴
        double tailX = cx - facing * r * 0.9;
        double tailY = cy + r * 0.1;
        gc.setFill(Color.rgb(120, 75, 40));
        gc.fillOval(tailX - r * 0.25, tailY - r * 0.25, r * 0.5, r * 0.5);

        // 身体（棕色椭圆）
        RadialGradient bodyGrad = makeRadial2(cx, cy, r,
                Color.rgb(175, 115, 55),
                Color.rgb(100, 60, 25));
        gc.setFill(bodyGrad);
        gc.fillOval(cx - r, cy - r * 0.85, r * 2, r * 1.7);

        // 耳朵（头顶两个小圆）
        double earOff = r * 0.35;
        gc.setFill(Color.rgb(100, 60, 30));
        gc.fillOval(cx - earOff - r * 0.22, cy - r * 0.75 - r * 0.22, r * 0.44, r * 0.44);
        gc.fillOval(cx + earOff - r * 0.22, cy - r * 0.75 - r * 0.22, r * 0.44, r * 0.44);

        // 眼睛（两只小黑点 + 白色反光）
        double eyeY = cy - r * 0.05;
        double eyeOff = r * 0.28;
        gc.setFill(Color.BLACK);
        gc.fillOval(cx - eyeOff - r * 0.10, eyeY - r * 0.10, r * 0.20, r * 0.20);
        gc.fillOval(cx + eyeOff - r * 0.10, eyeY - r * 0.10, r * 0.20, r * 0.20);
        gc.setFill(Color.WHITE);
        gc.fillOval(cx - eyeOff + r * 0.03 - r * 0.04, eyeY - r * 0.03 - r * 0.04, r * 0.08, r * 0.08);
        gc.fillOval(cx + eyeOff + r * 0.03 - r * 0.04, eyeY - r * 0.03 - r * 0.04, r * 0.08, r * 0.08);

        // 鼻子
        double snoutX = cx + facing * r * 0.55;
        double snoutY = cy + r * 0.1;
        gc.setFill(Color.rgb(80, 40, 20));
        gc.fillOval(snoutX - r * 0.18, snoutY - r * 0.13, r * 0.36, r * 0.26);

        // 胡须
        gc.setStroke(Color.rgb(60, 30, 15));
        gc.setLineWidth(0.8);
        double wbX = snoutX + facing * r * 0.1;
        double wbY = snoutY + r * 0.05;
        for (int w = -1; w <= 1; w++) {
            gc.beginPath();
            gc.moveTo(wbX, wbY + w * r * 0.08);
            gc.lineTo(wbX + facing * r * 0.3, wbY + w * r * 0.15);
            gc.stroke();
        }
    }

    /**
     * 福袋：浅米色袋子 + 顶部绳子扎口 + 中间大橙色问号。
     */
    private void drawMysteryBag(GraphicsContext gc, MysteryBag item, double r) {
        double cx = item.getX();
        double cy = item.getY();

        // 袋身（上窄下宽梯形）
        double topW = r * 1.1;
        double botW = r * 1.5;
        double topY = cy - r * 1.1;
        double botY = cy + r * 1.0;
        double[] px = {
                cx - topW / 2, cx + topW / 2,
                cx + botW / 2, cx - botW / 2,
        };
        double[] py = { topY, topY, botY, botY };

        gc.setFill(makeRadial2(cx, cy, r,
                Color.rgb(255, 235, 170),
                Color.rgb(200, 170, 100)));
        gc.fillPolygon(px, py, 4);

        gc.setStroke(Color.rgb(120, 85, 40));
        gc.setLineWidth(1.8);
        gc.strokePolygon(px, py, 4);

        // 顶部绳子扎口（曲线 + 绳结）
        gc.setStroke(Color.rgb(140, 100, 50));
        gc.setLineWidth(2.0);
        gc.beginPath();
        gc.moveTo(cx - topW / 2 - r * 0.15, topY - r * 0.1);
        gc.quadraticCurveTo(cx, topY - r * 0.5, cx + topW / 2 + r * 0.15, topY - r * 0.1);
        gc.stroke();

        gc.setFill(Color.rgb(170, 120, 60));
        gc.fillOval(cx - r * 0.18, topY - r * 0.15 - r * 0.18, r * 0.36, r * 0.36);

        // 中间大橙色问号
        gc.setFill(Color.rgb(230, 120, 20));
        gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, r * 1.6));
        gc.fillText("?", cx - r * 0.35, cy + r * 0.35);
    }

    /**
     * 炸弹：黑色球体 + 红色 TNT + 棕色引线 + 火花。
     */
    private void drawBomb(GraphicsContext gc, Bomb item, double r) {
        double cx = item.getX();
        double cy = item.getY();

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
     * FR-11 钻石猪：粉色猪身 + 朝向猪鼻 + 头顶浅蓝钻石图标；加速时白色描边。
     */
    private void drawDiamondPig(GraphicsContext gc, DiamondPig pig, double r) {
        double cx = pig.getX();
        double cy = pig.getY();

        // 猪身（粉色椭圆）
        gc.setFill(makeRadial2(cx, cy, r,
                Color.rgb(255, 200, 220),
                Color.rgb(220, 130, 170)));
        gc.fillOval(cx - r, cy - r * 0.9, r * 2, r * 1.8);

        gc.setStroke(Color.rgb(180, 90, 130));
        gc.setLineWidth(1.2);
        gc.strokeOval(cx - r, cy - r * 0.9, r * 2, r * 1.8);

        if (pig.isDashing()) {
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(2.5);
            gc.strokeOval(cx - r * 1.08, cy - r * 0.98, r * 2.16, r * 1.96);
        }

        int facing = pig.getFacing();

        // 猪鼻
        double snoutX = cx + facing * r * 0.65;
        double snoutY = cy;
        gc.setFill(Color.rgb(230, 110, 150));
        gc.fillOval(snoutX - r * 0.28, snoutY - r * 0.22, r * 0.56, r * 0.44);
        gc.setFill(Color.rgb(150, 60, 90));
        gc.fillOval(snoutX + facing * r * 0.08 - r * 0.05, snoutY - r * 0.06 - r * 0.05, r * 0.10, r * 0.10);
        gc.fillOval(snoutX + facing * r * 0.08 - r * 0.05, snoutY + r * 0.06 - r * 0.05, r * 0.10, r * 0.10);

        // 眼睛
        double eyeX = cx + facing * r * 0.25;
        double eyeY = cy - r * 0.25;
        gc.setFill(Color.BLACK);
        gc.fillOval(eyeX - r * 0.09, eyeY - r * 0.09, r * 0.18, r * 0.18);
        gc.setFill(Color.WHITE);
        gc.fillOval(eyeX + r * 0.03 - r * 0.04, eyeY - r * 0.03 - r * 0.04, r * 0.08, r * 0.08);

        // 耳朵
        double earY = cy - r * 0.8;
        gc.setFill(Color.rgb(210, 120, 160));
        gc.beginPath();
        gc.moveTo(cx - r * 0.5, earY);
        gc.lineTo(cx - r * 0.25, earY - r * 0.35);
        gc.lineTo(cx - r * 0.15, earY);
        gc.closePath();
        gc.fill();
        gc.beginPath();
        gc.moveTo(cx + r * 0.5, earY);
        gc.lineTo(cx + r * 0.25, earY - r * 0.35);
        gc.lineTo(cx + r * 0.15, earY);
        gc.closePath();
        gc.fill();

        // 头顶钻石图标
        double dcx = cx;
        double dcy = cy - r - 6;
        gc.setFill(Color.rgb(120, 200, 255));
        gc.beginPath();
        gc.moveTo(dcx, dcy - 8);
        gc.lineTo(dcx + 7, dcy);
        gc.lineTo(dcx, dcy + 8);
        gc.lineTo(dcx - 7, dcy);
        gc.closePath();
        gc.fill();
        gc.setStroke(Color.rgb(60, 120, 180));
        gc.setLineWidth(1.0);
        gc.stroke();
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
