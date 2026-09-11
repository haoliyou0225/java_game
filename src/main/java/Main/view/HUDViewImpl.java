// FR-UI HUDViewImpl：HUD 实现（顶部三栏 P1分数 | 倒计时 | P2分数），来自 feature_ui 分支
package Main.view;

import Main.config.Config;
import Main.model.GameModel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

/**
 * HUD 视图实现（FR-29 + FR-30）
 * 顶部三栏布局：左侧玩家1分数 | 中间剩余时间 | 右侧玩家2分数，双方数据独立显示。
 */
public class HUDViewImpl implements HUDView {

    /** 玩家1分数标签 */
    private final Label p1ScoreLabel;

    /** 玩家2分数标签 */
    private final Label p2ScoreLabel;

    /** 玩家1道具/效果标签（FR-22：炸药、短时道具库存、持续道具激活状态） */
    private final Label p1ItemLabel;

    /** 玩家2道具/效果标签 */
    private final Label p2ItemLabel;

    /** 倒计时数字标签 */
    private final Label timeLabel;

    /** HUD 根节点 */
    private final Pane root;

    public HUDViewImpl() {
        root = new Pane();
        root.setPickOnBounds(false); // 不拦截鼠标事件

        // ---------- 左栏：玩家1分数 ----------
        Label p1Title = new Label("P1");
        p1Title.setStyle("-fx-font-size: 16px; -fx-text-fill: #8ec9ff;");

        p1ScoreLabel = new Label("P1: $0");
        p1ScoreLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; "
                + "-fx-text-fill: #5db0ff;");

        p1ItemLabel = new Label("");
        p1ItemLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #9fd0ff;");

        VBox p1Box = new VBox(0, p1Title, p1ScoreLabel, p1ItemLabel);
        p1Box.setAlignment(Pos.CENTER_LEFT);
        p1Box.setPrefWidth(Config.WIDTH / 3.0);
        p1Box.setPadding(new Insets(0, 0, 0, 48));

        // ---------- 中栏：剩余时间 ----------
        Label timeTitle = new Label("剩余时间");
        timeTitle.setStyle("-fx-font-size: 16px; -fx-text-fill: #e8c87a;");

        timeLabel = new Label("90");
        timeLabel.setStyle("-fx-font-size: 40px; -fx-font-weight: bold; "
                + "-fx-text-fill: #ffe259;");

        VBox timeBox = new VBox(0, timeTitle, timeLabel);
        timeBox.setAlignment(Pos.CENTER);
        timeBox.setPrefWidth(Config.WIDTH / 3.0);

        // ---------- 右栏：玩家2分数 ----------
        Label p2Title = new Label("P2");
        p2Title.setStyle("-fx-font-size: 16px; -fx-text-fill: #ff9d9d;");

        p2ScoreLabel = new Label("P2: $0");
        p2ScoreLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; "
                + "-fx-text-fill: #ff6b6b;");

        p2ItemLabel = new Label("");
        p2ItemLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #ffb3b3;");

        VBox p2Box = new VBox(0, p2Title, p2ScoreLabel, p2ItemLabel);
        p2Box.setAlignment(Pos.CENTER_RIGHT);
        p2Box.setPrefWidth(Config.WIDTH / 3.0);
        p2Box.setPadding(new Insets(0, 48, 0, 0));

        // ---------- 组装三栏 HUD ----------
        HBox hudBar = new HBox(p1Box, timeBox, p2Box);
        hudBar.setPrefSize(Config.WIDTH, Config.HUD_HEIGHT);
        hudBar.setAlignment(Pos.CENTER);
        hudBar.setStyle("-fx-background-color: rgba(30, 19, 12, 0.92);");

        root.getChildren().add(hudBar);
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

        // FR-22：双方独立显示 炸药数 / 短时道具库存 / 钩爪增益剩余 / 持续道具激活状态
        p1ItemLabel.setText(buildItemText(model.getPlayer1(), model.getHook1()));
        p2ItemLabel.setText(buildItemText(model.getPlayer2(), model.getHook2()));
    }

    /**
     * 拼装单玩家道具状态行（FR-22）：
     * 冰冻中优先提示；随后 炸药/药水（生效中带剩余秒）/冰箱库存；最后已激活持续道具。
     */
    private String buildItemText(Main.model.Player player, Main.model.Hook hook) {
        StringBuilder sb = new StringBuilder();
        if (hook != null && hook.isFrozen()) {
            sb.append("冰冻中").append((int) Math.ceil(hook.getFreezeRemaining())).append("s  ");
        }
        sb.append("炸药x").append(player.getDynamiteCount());
        sb.append("  药水x").append(player.getPowerPotionCount());
        if (hook != null && hook.isSpeedBoostActive()) {
            sb.append("(").append((int) Math.ceil(hook.getSpeedBoostRemaining())).append("s)");
        }
        sb.append("  冰箱x").append(player.getFreezeBoxCount());
        StringBuilder persist = new StringBuilder();
        if (player.hasLuckyClover()) persist.append(" 四叶");
        if (player.hasDiamondBoost()) persist.append(" 钻药");
        if (player.hasStoneBook()) persist.append(" 石书");
        if (persist.length() > 0) {
            sb.append(" |").append(persist);
        }
        return sb.toString();
    }
}
