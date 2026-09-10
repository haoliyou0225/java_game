// FR-UI GameViewImpl：游戏画面渲染（地面/矿洞/物品/双钩/绳索），来自 feature_ui 分支，已适配 hook 分支 Item/Hook 接口
package Main.view;

import Main.config.Config;
import Main.model.Bomb;
import Main.model.Diamond;
import Main.model.GameModel;
import Main.model.Hook;
import Main.model.Item;
import Main.model.MineMap;
import Main.model.Mole;
import Main.model.MysteryBag;
import Main.model.Player;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

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
                if (item instanceof Bomb) {
                    drawTNT(gc, item.getX(), item.getY(), r);
                } else if (item instanceof Mole) {
                    drawMole(gc, item.getX(), item.getY(), r, ((Mole) item).getFacing());
                } else if (item instanceof MysteryBag) {
                    drawMysteryBag(gc, item.getX(), item.getY(), r);
                } else if (item instanceof Diamond) {
                    drawDiamond(gc, item.getX(), item.getY(), r);
                } else if (item.getScore() >= 50) {
                    // Gold / BigGold：不规则金块
                    drawGold(gc, item.getX(), item.getY(), r, item.getScore() >= 400);
                } else {
                    // Stone：不规则灰石
                    drawStone(gc, item.getX(), item.getY(), r);
                }
            }
        }

        // 6. 玩家1钩爪（蓝色）：起点标记 + 绳索 + 钩爪头 + 炸药图标
        drawHook(gc, model.getHook1(), Color.DODGERBLUE);
        drawBombIcon(gc, model.getHook1(), model.getPlayer1());

        // 7. 玩家2钩爪（红色）：起点标记 + 绳索 + 钩爪头 + 炸药图标
        drawHook(gc, model.getHook2(), Color.CRIMSON);
        drawBombIcon(gc, model.getHook2(), model.getPlayer2());
    }

    /**
     * 绘制单个钩爪：地面上的起点标记、绳索（起点→钩爪头）、钩爪头圆点。
     * 结算瞬时标注（钩爪刚回到起点时在起点旁显示几秒）由 Hook.getSettleLabel() 提供，
     * 当前时间超过 getSettleLabelUntil() 则不再渲染。
     * 若 getSettleIcon() 非 null 且非 MYSTERY_GOLD，在文本标注前绘制对应道具图标。
     */
    private void drawHook(GraphicsContext gc, Hook hook, Color color) {
        if (hook == null) {
            return;
        }
        // 起点标记（顶部地面上的小底座）
        gc.setFill(color);
        gc.fillRect(hook.getStartX() - 14, hook.getStartY() - 10, 28, 20);
        // 绳索
        gc.setStroke(color);
        gc.setLineWidth(3);
        gc.strokeLine(hook.getStartX(), hook.getStartY(), hook.getX(), hook.getY());
        // 钩爪头
        gc.setFill(color);
        gc.fillOval(hook.getX() - 8, hook.getY() - 8, 16, 16);

        // 结算瞬时标注：起点旁，只显示几秒
        long now = System.currentTimeMillis();
        if (hook.getSettleLabel() != null && now < hook.getSettleLabelUntil()) {
            String text = hook.getSettleLabel();
            Main.config.GameConfig.MysteryReward icon = hook.getSettleIcon();

            gc.setFont(new javafx.scene.text.Font(14));
            double iconW = (icon != null && icon != Main.config.GameConfig.MysteryReward.MYSTERY_GOLD) ? 18 : 0;
            double textW = gc.getFont().getSize() * text.length();
            double boxW = iconW + textW + 8;

            double labelX = hook.getStartX() + 20;
            double labelY = hook.getStartY() + 4;

            // 半透明黑底
            gc.setFill(Color.rgb(0, 0, 0, 0.7));
            gc.fillRect(labelX - 3, labelY - 14, boxW + 6, 18);

            // 道具图标（非金币时绘制）
            if (iconW > 0) {
                drawMysteryIcon(gc, icon, labelX, labelY - 10);
            }

            // 文本（图标后偏移）
            gc.setFill(Color.YELLOW);
            gc.fillText(text, labelX + iconW, labelY);
        }
    }

    /**
     * 福袋道具图标绘制：幸运草=绿色四叶草，钻石升级药水=浅蓝菱形，石头收藏书=棕方块，强力药水=紫色闪电。
     * 在给定 Canvas 坐标系下，图标左上角位于 (startX, startY)，尺寸 16x16。
     */
    private void drawMysteryIcon(GraphicsContext gc, Main.config.GameConfig.MysteryReward icon, double startX, double startY) {
        switch (icon) {
            case LUCKY_CLOVER:
                // 绿色四叶草：4 个小圆 + 中心黄点
                gc.setFill(Color.FORESTGREEN);
                gc.fillOval(startX + 1, startY + 1, 6, 6);
                gc.fillOval(startX + 9, startY + 1, 6, 6);
                gc.fillOval(startX + 1, startY + 9, 6, 6);
                gc.fillOval(startX + 9, startY + 9, 6, 6);
                gc.setFill(Color.YELLOW);
                gc.fillOval(startX + 6, startY + 6, 4, 4);
                break;
            case DIAMOND_BOOST:
                // 浅蓝菱形
                gc.setFill(Color.SKYBLUE);
                gc.beginPath();
                gc.moveTo(startX + 8, startY);
                gc.lineTo(startX + 16, startY + 8);
                gc.lineTo(startX + 8, startY + 16);
                gc.lineTo(startX, startY + 8);
                gc.closePath();
                gc.fill();
                gc.setFill(Color.CYAN);
                gc.fillOval(startX + 6, startY + 6, 4, 4);
                break;
            case STONE_BOOK:
                // 棕方块（书的样子）
                gc.setFill(Color.SADDLEBROWN);
                gc.fillRect(startX, startY + 1, 16, 14);
                gc.setFill(Color.WHEAT);
                gc.fillRect(startX + 2, startY + 3, 12, 2);
                gc.fillRect(startX + 2, startY + 7, 12, 2);
                gc.fillRect(startX + 2, startY + 11, 8, 2);
                break;
            case DYNAMITE:
                // 黑球 + 引线
                gc.setFill(Color.BLACK);
                gc.fillOval(startX + 2, startY + 3, 12, 12);
                gc.setStroke(Color.SADDLEBROWN);
                gc.setLineWidth(2);
                gc.beginPath();
                gc.moveTo(startX + 14, startY + 4);
                gc.quadraticCurveTo(startX + 18, startY - 2, startX + 15, startY - 4);
                gc.stroke();
                gc.setFill(Color.ORANGE);
                gc.fillOval(startX + 14, startY - 4, 4, 4);
                break;
            case POWER_POTION:
                // 紫色闪电
                gc.setFill(Color.DARKVIOLET);
                gc.beginPath();
                gc.moveTo(startX + 8, startY);
                gc.lineTo(startX + 2, startY + 8);
                gc.lineTo(startX + 7, startY + 8);
                gc.lineTo(startX + 4, startY + 16);
                gc.lineTo(startX + 14, startY + 6);
                gc.lineTo(startX + 9, startY + 6);
                gc.closePath();
                gc.fill();
                break;
            default:
                break;
        }
    }

    /** 按物品分值分档选择颜色（已废弃，保留兼容引用） */
    @Deprecated
    private Color colorOf(Item item) {
        int value = item.getScore();
        if (value >= 400) return Color.rgb(255, 200, 0);
        if (value >= 100) return Color.rgb(120, 200, 255);
        if (value >= 50) return Color.rgb(255, 215, 0);
        return Color.rgb(160, 160, 160);
    }

    /** TNT 木桶：棕木桶 + 红色 TNT 标签 */
    private void drawTNT(GraphicsContext gc, double cx, double cy, double r) {
        double w = r * 1.8, h = r * 2.0;
        double x = cx - w / 2, y = cy - h / 2;
        // 木桶主体
        gc.setFill(Color.rgb(140, 90, 45));
        gc.fillRect(x, y, w, h);
        // 桶箍（上中下三条深色带）
        gc.setFill(Color.rgb(80, 50, 25));
        gc.fillRect(x - 2, y, w + 4, 4);
        gc.fillRect(x - 2, cy - 2, w + 4, 4);
        gc.fillRect(x - 2, y + h - 4, w + 4, 4);
        // 红色 TNT 标签
        gc.setFill(Color.rgb(220, 40, 40));
        gc.fillRect(x + 2, y + h * 0.35, w - 4, h * 0.3);
        gc.setFill(Color.WHITE);
        gc.setFont(new javafx.scene.text.Font(Math.max(10, r * 0.9)));
        gc.fillText("TNT", cx - 10, cy + 4);
    }

    /**
     * 金块：不规则多边形团块，深色描边 + 亮黄主体 + 左上高光白点。
     */
    private void drawGold(GraphicsContext gc, double cx, double cy, double r, boolean isBig) {
        double size = isBig ? r * 1.7 : r * 1.2;
        // 7 个顶点的不规则多边形（无贝塞尔，纯直线）
        double[][] pts = {
                { -0.9, -0.3 }, { -0.4, -1.0 }, { 0.5, -0.8 },
                { 0.9, -0.1 }, { 0.6, 0.9 }, { -0.3, 1.0 }, { -0.8, 0.4 }
        };

        // 1. 深色描边底色
        gc.setFill(Color.rgb(60, 35, 10));
        gc.beginPath();
        gc.moveTo(cx + pts[0][0] * (size * 1.08), cy + pts[0][1] * (size * 1.08));
        for (int i = 1; i < pts.length; i++) {
            gc.lineTo(cx + pts[i][0] * (size * 1.08), cy + pts[i][1] * (size * 1.08));
        }
        gc.closePath();
        gc.fill();

        // 2. 亮黄主体
        gc.setFill(Color.rgb(255, 210, 45));
        gc.beginPath();
        gc.moveTo(cx + pts[0][0] * size, cy + pts[0][1] * size);
        for (int i = 1; i < pts.length; i++) {
            gc.lineTo(cx + pts[i][0] * size, cy + pts[i][1] * size);
        }
        gc.closePath();
        gc.fill();

        // 3. 高光白点
        gc.setFill(Color.rgb(255, 250, 220));
        gc.fillOval(cx - size * 0.55, cy - size * 0.55, size * 0.6, size * 0.35);
    }

    /**
     * 钻石：圆滑菱形（切面水晶感）。
     * 用 quadraticCurveTo 让菱形边缘有微弧，不是硬棱角。
     */
    private void drawDiamond(GraphicsContext gc, double cx, double cy, double r) {
        double w = r * 1.0, h = r * 1.1;

        // 底层阴影（右下偏移）
        gc.setFill(Color.rgb(60, 130, 200));
        gc.beginPath();
        gc.moveTo(cx, cy - h);
        gc.lineTo(cx + w * 1.05, cy);
        gc.lineTo(cx, cy + h);
        gc.lineTo(cx - w * 1.05, cy);
        gc.closePath();
        gc.fill();

        // 主体：亮蓝菱形，边缘微弧
        gc.setFill(Color.rgb(110, 195, 255));
        gc.beginPath();
        gc.moveTo(cx, cy - h);
        // 右上边：微弧
        gc.quadraticCurveTo(cx + w * 0.5, cy - h * 0.3, cx + w, cy);
        // 右下边：微弧
        gc.quadraticCurveTo(cx + w * 0.5, cy + h * 0.3, cx, cy + h);
        // 左下边：微弧
        gc.quadraticCurveTo(cx - w * 0.5, cy + h * 0.3, cx - w, cy);
        // 左上边：微弧
        gc.quadraticCurveTo(cx - w * 0.5, cy - h * 0.3, cx, cy - h);
        gc.closePath();
        gc.fill();

        // 内部切面：左上三角高光
        gc.setFill(Color.rgb(210, 240, 255));
        gc.beginPath();
        gc.moveTo(cx, cy - h * 0.7);
        gc.quadraticCurveTo(cx + w * 0.15, cy - h * 0.2, cx + w * 0.25, cy);
        gc.lineTo(cx - w * 0.25, cy);
        gc.quadraticCurveTo(cx - w * 0.15, cy - h * 0.2, cx, cy - h * 0.7);
        gc.closePath();
        gc.fill();

        // 右下暗面三角
        gc.setFill(Color.rgb(70, 150, 220));
        gc.beginPath();
        gc.moveTo(cx, cy + h * 0.3);
        gc.lineTo(cx + w * 0.4, cy);
        gc.lineTo(cx + w * 0.15, cy + h * 0.6);
        gc.closePath();
        gc.fill();
    }

    /** 石头：不规则灰色多边形 */
    private void drawStone(GraphicsContext gc, double cx, double cy, double r) {
        gc.setFill(Color.rgb(150, 150, 155));
        gc.beginPath();
        gc.moveTo(cx - r * 0.8, cy - r * 0.5);
        gc.lineTo(cx - r * 0.3, cy - r);
        gc.lineTo(cx + r * 0.6, cy - r * 0.7);
        gc.lineTo(cx + r * 0.9, cy + r * 0.2);
        gc.lineTo(cx + r * 0.4, cy + r);
        gc.lineTo(cx - r * 0.5, cy + r * 0.8);
        gc.lineTo(cx - r * 0.9, cy + r * 0.1);
        gc.closePath();
        gc.fill();
        // 阴影
        gc.setFill(Color.rgb(120, 120, 125));
        gc.beginPath();
        gc.moveTo(cx - r * 0.1, cy + r * 0.3);
        gc.lineTo(cx + r * 0.4, cy + r);
        gc.lineTo(cx - r * 0.5, cy + r * 0.8);
        gc.closePath();
        gc.fill();
    }

    /**
     * 福袋：圆润黄袋 + 顶部褶皱 + 红色丝带 + 大问号。
     * 用椭圆主体 + 贝塞尔曲线袋口褶皱，边缘圆滑。
     */
    private void drawMysteryBag(GraphicsContext gc, double cx, double cy, double r) {
        double bodyW = r * 1.9, bodyH = r * 1.7;
        // 袋身阴影（右下偏移）
        gc.setFill(Color.rgb(200, 150, 20));
        gc.fillOval(cx - bodyW / 2 + 3, cy - bodyH / 2 + 5, bodyW, bodyH);
        // 袋身主体（亮黄椭圆）
        gc.setFill(Color.rgb(255, 205, 45));
        gc.fillOval(cx - bodyW / 2, cy - bodyH / 2, bodyW, bodyH);

        // 袋口褶皱（顶部波浪边，用贝塞尔曲线）
        gc.setFill(Color.rgb(220, 170, 30));
        gc.beginPath();
        double foldY = cy - bodyH / 2;
        gc.moveTo(cx - bodyW / 2, foldY);
        gc.quadraticCurveTo(cx - bodyW * 0.35, foldY - r * 0.3, cx - bodyW * 0.15, foldY - r * 0.05);
        gc.quadraticCurveTo(cx, foldY - r * 0.4, cx + bodyW * 0.15, foldY - r * 0.05);
        gc.quadraticCurveTo(cx + bodyW * 0.35, foldY - r * 0.3, cx + bodyW / 2, foldY);
        gc.lineTo(cx + bodyW / 2, foldY + r * 0.15);
        gc.lineTo(cx - bodyW / 2, foldY + r * 0.15);
        gc.closePath();
        gc.fill();

        // 顶部小揪揪（三角）
        gc.setFill(Color.rgb(180, 130, 20));
        gc.beginPath();
        gc.moveTo(cx - r * 0.25, foldY - r * 0.2);
        gc.lineTo(cx + r * 0.25, foldY - r * 0.2);
        gc.lineTo(cx, foldY - r * 0.7);
        gc.closePath();
        gc.fill();

        // 红色丝带（椭圆弧带，用 fillOval + clip 模拟）
        gc.setFill(Color.rgb(220, 45, 45));
        gc.fillOval(cx - bodyW * 0.55, cy - bodyH * 0.15 - r * 0.18, bodyW * 1.1, r * 0.36);
        // 丝带高光
        gc.setFill(Color.rgb(255, 100, 100));
        gc.fillOval(cx - bodyW * 0.4, cy - bodyH * 0.18 - r * 0.06, bodyW * 0.8, r * 0.12);

        // 问号（深棕，居中偏大）
        gc.setFill(Color.rgb(90, 50, 0));
        gc.setFont(new javafx.scene.text.Font("Bold", r * 1.5));
        gc.fillText("?", cx - r * 0.3, cy + bodyH * 0.2);
    }

    /** 鼹鼠：横向椭圆身体 + 小耳朵 + 粉鼻子 + 白牙 + 尾巴。facing=1 头朝右，facing=-1 头朝左（水平镜像）。 */
    private void drawMole(GraphicsContext gc, double cx, double cy, double r, int facing) {
        // 保存当前变换，准备水平镜像
        gc.save();
        if (facing < 0) {
            // 以 cx 为轴做水平镜像：先平移到原点 → X 翻转 → 平移回来
            gc.translate(cx, 0);
            gc.scale(-1, 1);
            gc.translate(-cx, 0);
        }

        // 横向椭圆身体（宽 2.4r，高 1.2r）
        double bodyW = r * 2.4, bodyH = r * 1.2;
        gc.setFill(Color.rgb(139, 90, 43));
        gc.fillOval(cx - bodyW / 2, cy - bodyH / 2, bodyW, bodyH);
        // 头部（右侧小圆）
        double headR = r * 0.9;
        gc.fillOval(cx + bodyW * 0.3, cy - headR / 2, headR, headR);
        // 小耳朵
        gc.fillOval(cx + bodyW * 0.35, cy - headR * 0.6, r * 0.25, r * 0.35);
        gc.fillOval(cx + bodyW * 0.55, cy - headR * 0.6, r * 0.25, r * 0.35);
        // 眼睛
        gc.setFill(Color.BLACK);
        gc.fillOval(cx + bodyW * 0.45, cy - headR * 0.15, r * 0.18, r * 0.18);
        gc.fillOval(cx + bodyW * 0.62, cy - headR * 0.15, r * 0.18, r * 0.18);
        // 粉色鼻子
        gc.setFill(Color.rgb(240, 130, 130));
        gc.fillOval(cx + bodyW * 0.72, cy - r * 0.05, r * 0.22, r * 0.15);
        // 白牙
        gc.setFill(Color.WHITE);
        gc.fillRect(cx + bodyW * 0.68, cy + r * 0.2, r * 0.1, r * 0.15);
        gc.fillRect(cx + bodyW * 0.8, cy + r * 0.2, r * 0.1, r * 0.15);
        // 尾巴（左侧小三角）
        gc.setFill(Color.rgb(100, 65, 30));
        gc.beginPath();
        gc.moveTo(cx - bodyW / 2, cy);
        gc.lineTo(cx - bodyW / 2 - r * 0.5, cy - r * 0.3);
        gc.lineTo(cx - bodyW / 2 - r * 0.5, cy + r * 0.3);
        gc.closePath();
        gc.fill();

        // 恢复变换
        gc.restore();
    }

    /**
     * 在钩爪锚点旁边绘制炸药图标（黑球 + 引线 + 火花）和库存数量。
     * 锚点左侧：玩家1炸药；锚点右侧：玩家2炸药。
     * 库存为 0 时炸药图标变灰半透明，提示不可用。
     */
    private void drawBombIcon(GraphicsContext gc, Hook hook, Player player) {
        if (hook == null || player == null) return;

        double anchorX = hook.getStartX();
        double anchorY = hook.getStartY();
        boolean isPlayer1 = hook.getPlayerId() == 1;

        // 图标位置：玩家1在锚点左侧，玩家2在锚点右侧
        double iconX = isPlayer1 ? anchorX - 36 : anchorX + 36;
        double iconY = anchorY + 28; // 地面下方一点

        int count = player.getBombCount();
        boolean available = count > 0;

        // 炸药主体：黑球
        double bodyR = 9;
        gc.setFill(available ? Color.rgb(30, 30, 30) : Color.rgb(120, 120, 120));
        if (!available) gc.setGlobalAlpha(0.5);
        gc.fillOval(iconX - bodyR, iconY - bodyR, bodyR * 2, bodyR * 2);

        // 引线：棕色斜线
        gc.setStroke(available ? Color.SADDLEBROWN : Color.rgb(100, 100, 100));
        gc.setLineWidth(2);
        gc.strokeLine(iconX + bodyR * 0.5, iconY - bodyR * 0.7,
                iconX + bodyR * 1.4, iconY - bodyR * 1.4);

        // 火花：橙色小圆
        gc.setFill(available ? Color.ORANGE : Color.rgb(150, 150, 150));
        gc.fillOval(iconX + bodyR * 1.4 - 2.5, iconY - bodyR * 1.4 - 2.5, 5, 5);

        // 库存数量：锚点正下方显示
        gc.setGlobalAlpha(1);
        gc.setFill(available ? Color.WHITE : Color.rgb(180, 180, 180));
        gc.setFont(new javafx.scene.text.Font(12));
        gc.fillText("x" + count, iconX - 8, iconY + 22);
    }
}
