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
                if (item instanceof DiamondPig) {
                    drawDiamondPig(gc, (DiamondPig) item, r);
                } else if (item instanceof Bomb) {
                    // 炸弹：黑圆 + 红 TNT
                    gc.setFill(Color.rgb(30, 30, 30));
                    gc.fillOval(item.getX() - r, item.getY() - r, r * 2, r * 2);
                    gc.setFill(Color.rgb(220, 40, 40));
                    gc.fillText("TNT", item.getX() - 12, item.getY() + 4);
                } else if (item instanceof Mole) {
                    // 鼹鼠：棕圆 + 分值
                    gc.setFill(Color.rgb(139, 90, 43));
                    gc.fillOval(item.getX() - r, item.getY() - r, r * 2, r * 2);
                    gc.setFill(Color.WHITE);
                    gc.fillText(String.valueOf(item.getScore()), item.getX() - 8, item.getY() + 5);
                } else if (item instanceof MysteryBag) {
                    // 福袋：紫圆 + 黄字
                    gc.setFill(Color.rgb(128, 64, 200));
                    gc.fillOval(item.getX() - r, item.getY() - r, r * 2, r * 2);
                    gc.setFill(Color.rgb(255, 215, 0));
                    gc.fillText("袋", item.getX() - 8, item.getY() + 5);
                } else {
                    // 普通矿石：按品类着色 + 分值
                    gc.setFill(colorOf(item));
                    gc.fillOval(item.getX() - r, item.getY() - r, r * 2, r * 2);
                    gc.setFill(Color.WHITE);
                    gc.fillText(String.valueOf(item.getScore()),
                            item.getX() - 12, item.getY() + 5);
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

        // 结算瞬时飘字（融合 feature/item）：钩爪回起点时在锚点旁显示 2 秒
        long now = System.currentTimeMillis();
        if (hook.getSettleLabel() != null && now < hook.getSettleLabelUntil()) {
            String text = hook.getSettleLabel();
            double labelX = hook.getStartX() + 20;
            double labelY = hook.getStartY() + 4;
            // 半透明黑底（ASCII 约 9px/字，中文约 16px/字）
            double boxW = text.chars().mapToDouble(c -> c > 0x2E80 ? 16 : 9).sum() + 8;
            gc.setFill(Color.rgb(0, 0, 0, 0.7));
            gc.fillRect(labelX - 4, labelY - 15, boxW, 20);
            // 福袋道具名用金色，普通分数用白色
            gc.setFill(hook.getSettleIcon() != null ? Color.rgb(255, 215, 0) : Color.WHITE);
            gc.fillText(text, labelX, labelY);
        }
    }

    /**
     * FR-11 钻石猪：粉色猪身圆 + 朝向猪鼻 + 头顶浅蓝钻石图标 + 钻石颗数；加速时白色描边高亮。
     */
    private void drawDiamondPig(GraphicsContext gc, DiamondPig pig, double r) {
        // 猪身（粉色圆）
        gc.setFill(Color.rgb(255, 158, 190));
        gc.fillOval(pig.getX() - r, pig.getY() - r, r * 2, r * 2);
        // 加速冲刺时白色描边提示
        if (pig.isDashing()) {
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(2.5);
            gc.strokeOval(pig.getX() - r, pig.getY() - r, r * 2, r * 2);
        }
        // 猪鼻（朝向方向的深色小圆）
        double snoutOff = pig.getFacing() * r * 0.55;
        gc.setFill(Color.rgb(230, 110, 150));
        gc.fillOval(pig.getX() + snoutOff - 5, pig.getY() - 3, 10, 8);
        // 头顶钻石图标（浅蓝小菱形）
        double cx = pig.getX();
        double cy = pig.getY() - r - 6;
        gc.setFill(Color.rgb(120, 200, 255));
        gc.fillPolygon(new double[]{cx, cx + 6, cx, cx - 6},
                new double[]{cy - 7, cy, cy + 7, cy}, 4);
        // 颗数文字
        gc.setFill(Color.WHITE);
        gc.fillText("x" + pig.getDiamonds(), pig.getX() - 10, pig.getY() + 5);
    }

    /** 按物品种类选择颜色（用于 Gold/MediumGold/BigGold/Diamond/Stone） */
    private Color colorOf(Item item) {
        if (item instanceof BigGold) {
            return Color.rgb(230, 90, 90);      // 大金块：红色
        }
        if (item instanceof MediumGold) {
            return Color.rgb(255, 150, 40);     // 中金块：橙色
        }
        if (item instanceof Diamond) {
            return Color.rgb(120, 200, 255);    // 钻石：浅蓝
        }
        if (item instanceof Gold) {
            return Color.rgb(255, 215, 0);      // 小金块：金色
        }
        return Color.rgb(160, 160, 160);        // 石头：灰色
    }
}
