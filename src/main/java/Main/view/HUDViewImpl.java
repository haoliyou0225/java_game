// FR-UI HUDViewImpl：HUD 实现（顶部三栏 P1分数+全部道具状态 | 倒计时 | P2分数+全部道具状态）
package Main.view;

import Main.config.Config;
import Main.model.GameModel;
import Main.model.Hook;
import Main.model.Player;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

/**
 * HUD 视图实现（FR-29 + FR-30 + FR-22）。
 * 顶部三栏布局：左侧玩家1分数与道具状态 | 中间剩余时间 | 右侧玩家2分数与道具状态。
 * 道具状态分两行（6 种道具全部可见，名称与新手指南/按键表完全一致）：
 * <ul>
 *   <li>第一行：冰冻倒计时（若有）/ 炸药 / 强力药水（生效中带剩余秒）/ 冰冻箱；</li>
 *   <li>第二行：幸运草 / 钻石升级 / 石头书（显示库存，已激活带 ✓ 标记）。</li>
 * </ul>
 */
public class HUDViewImpl implements HUDView {

    /** 玩家1分数标签 */
    private final Label p1ScoreLabel;
    /** 玩家2分数标签 */
    private final Label p2ScoreLabel;

    /** 玩家1道具第一行（炸药/强力药水/冰冻箱/冰冻中） */
    private final Label p1ItemLine1;
    /** 玩家1道具第二行（幸运草/钻石升级/石头书） */
    private final Label p1ItemLine2;
    /** 玩家2道具第一行 */
    private final Label p2ItemLine1;
    /** 玩家2道具第二行 */
    private final Label p2ItemLine2;

    /** 倒计时数字标签 */
    private final Label timeLabel;

    /** HUD 根节点 */
    private final Pane root;

    public HUDViewImpl() {
        root = new Pane();
        root.setPickOnBounds(false); // 不拦截鼠标事件

        // 文字描边效果（深色阴影，让亮色文字在任何背景上都清晰可读）
        final String OUTLINE = "-fx-effect: dropshadow(one-pass-box, #000000, 2, 1.0, 1, 1);";

        // ---------- 左栏：玩家1分数 + 道具两行（删除多余 P1 标题，整体上移）----------
        p1ScoreLabel = new Label("P1: $0");
        p1ScoreLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; "
                + "-fx-text-fill: #5db0ff; " + OUTLINE);

        p1ItemLine1 = new Label("");
        p1ItemLine1.setStyle("-fx-font-size: 12px; -fx-text-fill: #9fd0ff; " + OUTLINE);
        p1ItemLine2 = new Label("");
        p1ItemLine2.setStyle("-fx-font-size: 12px; -fx-text-fill: #9fd0ff; " + OUTLINE);

        VBox p1Box = new VBox(0, p1ScoreLabel, p1ItemLine1, p1ItemLine2);
        p1Box.setAlignment(Pos.CENTER_LEFT);
        p1Box.setPrefWidth(Config.WIDTH / 3.0);
        // 负 top padding 上移，补偿删除标题后多出的空间
        p1Box.setPadding(new Insets(-8, 0, 0, 36));

        // ---------- 中栏：剩余时间 ----------
        Label timeTitle = new Label("剩余时间");
        timeTitle.setStyle("-fx-font-size: 16px; -fx-text-fill: #e8c87a; " + OUTLINE);

        timeLabel = new Label("90");
        timeLabel.setStyle("-fx-font-size: 40px; -fx-font-weight: bold; "
                + "-fx-text-fill: #ffe259; " + OUTLINE);

        VBox timeBox = new VBox(0, timeTitle, timeLabel);
        timeBox.setAlignment(Pos.CENTER);
        timeBox.setPrefWidth(Config.WIDTH / 3.0);

        // ---------- 右栏：玩家2分数 + 道具两行（删除多余 P2 标题，整体上移）----------
        p2ScoreLabel = new Label("P2: $0");
        p2ScoreLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; "
                + "-fx-text-fill: #ff6b6b; " + OUTLINE);

        p2ItemLine1 = new Label("");
        p2ItemLine1.setStyle("-fx-font-size: 12px; -fx-text-fill: #ffb3b3; " + OUTLINE);
        p2ItemLine2 = new Label("");
        p2ItemLine2.setStyle("-fx-font-size: 12px; -fx-text-fill: #ffb3b3; " + OUTLINE);

        VBox p2Box = new VBox(0, p2ScoreLabel, p2ItemLine1, p2ItemLine2);
        p2Box.setAlignment(Pos.CENTER_RIGHT);
        p2Box.setPrefWidth(Config.WIDTH / 3.0);
        p2Box.setPadding(new Insets(-8, 36, 0, 0));

        // ---------- 组装三栏 HUD ----------
        HBox hudBar = new HBox(p1Box, timeBox, p2Box);
        hudBar.setPrefSize(Config.WIDTH, Config.HUD_HEIGHT);
        hudBar.setAlignment(Pos.CENTER);
        // 背景透明：底层 ImageView 承载背景图，hudBar 只负责文字信息
        hudBar.setStyle("-fx-background-color: transparent;");
        // hudBar 不拦截背景图下方的鼠标事件，但保持自身文字可点可选
        hudBar.setMouseTransparent(false);

        // ---------- HUD 背景图（最底层，不影响上方文字信息显示）----------
        Image hudBgImage = new Image(
                getClass().getResourceAsStream("/images/HUD/topbg.png"));
        ImageView hudBg = new ImageView(hudBgImage);
        // 拉伸到 HUD 区域宽高（Config.WIDTH × Config.HUD_HEIGHT）
        hudBg.setFitWidth(Config.WIDTH);
        hudBg.setFitHeight(Config.HUD_HEIGHT);
        hudBg.setPreserveRatio(false); // 拉伸填充，与 HUD 栏完全重合
        hudBg.setMouseTransparent(true); // 背景图不拦截鼠标，事件穿透到下方游戏画面

        // 先放背景图（底层），再放 hudBar（顶层文字信息），层级正确
        root.getChildren().addAll(hudBg, hudBar);
    }

    @Override
    public Parent build() {
        return root;
    }

    @Override
    public void render(GameModel model) {
        // 玩家1分数（FR-29：独立显示，不混淆）
        p1ScoreLabel.setText("P1: $" + model.getPlayer1().getScore());

        // 倒计时（FR-30：向上取整显示整数秒）
        int seconds = (int) Math.ceil(model.getRemainingTime());
        timeLabel.setText(String.valueOf(seconds));

        // 玩家2分数（FR-29：独立显示，不混淆）
        p2ScoreLabel.setText("P2: $" + model.getPlayer2().getScore());

        // FR-22：双方独立显示全部 6 种道具的库存/激活/剩余状态
        p1ItemLine1.setText(buildShortItemLine(model.getPlayer1(), model.getHook1()));
        p1ItemLine2.setText(buildPersistItemLine(model.getPlayer1()));
        p2ItemLine1.setText(buildShortItemLine(model.getPlayer2(), model.getHook2()));
        p2ItemLine2.setText(buildPersistItemLine(model.getPlayer2()));
    }

    /**
     * 第一行：冰冻状态（被冻时优先提示）→ 炸药 → 强力药水（生效中附剩余秒）→ 冰冻箱。
     */
    private String buildShortItemLine(Player player, Hook hook) {
        StringBuilder sb = new StringBuilder();
        if (hook != null && hook.isFrozen()) {
            sb.append("冰冻中").append((int) Math.ceil(hook.getFreezeRemaining())).append("s  ");
        }
        sb.append("炸药×").append(player.getDynamiteCount());
        sb.append("  强力药水×").append(player.getPowerPotionCount());
        if (hook != null && hook.isSpeedBoostActive()) {
            sb.append("(").append((int) Math.ceil(hook.getSpeedBoostRemaining())).append("s)");
        }
        sb.append("  冰冻箱×").append(player.getFreezeBoxCount());
        return sb.toString();
    }

    /**
     * 第二行：幸运草 / 钻石升级 / 石头书的库存数量；已激活的持续道具追加 ✓ 标记（FR-22）。
     */
    private String buildPersistItemLine(Player player) {
        return "幸运草×" + player.getLuckyCloverCount() + (player.hasLuckyClover() ? "✓" : "")
                + "  钻石升级×" + player.getDiamondBoostCount() + (player.hasDiamondBoost() ? "✓" : "")
                + "  石头书×" + player.getStoneBookCount() + (player.hasStoneBook() ? "✓" : "");
    }
}
