// FR-UI ResultViewImpl：胜负结算实现（遮罩 + 获胜文案 + 双方分数 + 重新开始/返回主菜单），来自 feature_ui 分支
package Main.view;

import Main.config.Config;
import Main.model.GameModel;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

/**
 * 胜负结算界面实现（FR-31）
 * <p>
 * 布局：全屏半透明深色遮罩 + 居中结算卡片：
 * 标题（获胜方/平局文案）→ 玩家1分数（蓝）→ 玩家2分数（红）
 * → “重新开始”、“返回主菜单”两个按钮（水平并排）。
 * 分数颜色与 HUD（FR-29）保持一致：P1 蓝色 / P2 红色。
 */
public class ResultViewImpl implements ResultView {

    /** 平局标题金色 */
    private static final String GOLD_TITLE_STYLE = "-fx-font-size: 64px; -fx-font-weight: bold; -fx-text-fill: #ffe259;";
    /** 玩家1获胜标题蓝色（与 HUD P1 配色一致） */
    private static final String BLUE_TITLE_STYLE = "-fx-font-size: 64px; -fx-font-weight: bold; -fx-text-fill: #5db0ff;";
    /** 玩家2获胜标题红色（与 HUD P2 配色一致） */
    private static final String RED_TITLE_STYLE = "-fx-font-size: 64px; -fx-font-weight: bold; -fx-text-fill: #ff6b6b;";

    /** 结算界面根节点（全屏遮罩） */
    private final VBox resultBox;

    /** 标题：获胜方/平局文案 */
    private final Label titleLabel;

    /** 玩家1最终分数标签 */
    private final Label score1Label;

    /** 玩家2最终分数标签 */
    private final Label score2Label;

    /** 由上层注入的"重新开始"回调 */
    private Runnable onRestart;

    /** 由上层注入的"返回主菜单"回调 */
    private Runnable onBackToMenu;

    public ResultViewImpl() {
        // 标题（初始为平局金色，show() 时按判定结果更新文案与颜色）
        titleLabel = new Label();
        titleLabel.setStyle(GOLD_TITLE_STYLE);

        // 双方最终分数（颜色与 HUD 一致，避免混淆）
        score1Label = new Label();
        score1Label.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #5db0ff;");
        score2Label = new Label();
        score2Label.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #ff6b6b;");

        // 重新开始按钮（样式统一由 UiStyle 提供）
        Button restartButton = UiStyle.createGoldButton("重新开始");
        restartButton.setOnAction(e -> {
            if (onRestart != null) {
                onRestart.run();
            }
        });

        // 返回主菜单按钮：由上层注入资源释放与回到主菜单的完整流程
        Button backToMenuButton = UiStyle.createGoldButton("返回主菜单");
        backToMenuButton.setOnAction(e -> {
            if (onBackToMenu != null) {
                onBackToMenu.run();
            }
        });

        // 两个结算操作按钮水平并排，间距 32
        HBox actionBox = new HBox(32, restartButton, backToMenuButton);
        actionBox.setAlignment(Pos.CENTER);

        resultBox = new VBox(24, titleLabel, score1Label, score2Label, actionBox);
        resultBox.setAlignment(Pos.CENTER);
        resultBox.setPrefSize(Config.WIDTH, Config.HEIGHT);
        // 半透明深色遮罩：覆盖对局画面，突出结算内容
        resultBox.setStyle("-fx-background-color: rgba(20, 12, 4, 0.88);");
    }

    @Override
    public Parent build() {
        return resultBox;
    }

    @Override
    public void show(Pane container, GameModel model) {
        // 读取双方最终分数（View 仅依赖 Model 接口，不依赖实现类）
        int score1 = model.getPlayer1().getScore();
        int score2 = model.getPlayer2().getScore();

        // 胜负判定下沉到 Model 层（FR-31）：本层仅负责文案与颜色渲染
        int winner = model.determineWinner();
        if (winner > 0) {
            titleLabel.setText("玩家1 获胜！");
            titleLabel.setStyle(BLUE_TITLE_STYLE);
        } else if (winner < 0) {
            titleLabel.setText("玩家2 获胜！");
            titleLabel.setStyle(RED_TITLE_STYLE);
        } else {
            titleLabel.setText("平局！");
            titleLabel.setStyle(GOLD_TITLE_STYLE);
        }

        score1Label.setText("玩家1 得分：$" + score1);
        score2Label.setText("玩家2 得分：$" + score2);

        // 幂等挂载：防止重复添加
        UiStyle.mountOnce(container, resultBox);
    }

    @Override
    public void hide() {
        UiStyle.unmount(resultBox);
    }

    @Override
    public void setOnRestart(Runnable action) {
        this.onRestart = action;
    }

    @Override
    public void setOnBackToMenu(Runnable action) {
        this.onBackToMenu = action;
    }
}
