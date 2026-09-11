// FR-UI MenuViewImpl：主菜单实现（GoldMainer 标题 + 开始对战/新手指南/退出按钮），来自 feature_ui 分支
package Main.view;

import Main.config.Config;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

/**
 * 主菜单视图实现（FR-32）
 * 布局：居中显示标题 GoldMainer、副标题，以及
 * “开始对战”、“新手指南”、“退出游戏”三个按钮。
 * <p>
 * 说明：完整的双人键位表与游戏规则已抽取到可复用组件 {@link ControlGuidePane}，
 * 由“新手指南”按钮回调上层弹出独立窗口展示（FR-26），主菜单不再内联整份提示，
 * 以保证 1280×720 逻辑分辨率下排版整齐、不被裁切。
 */
public class MenuViewImpl implements MenuView {

    /** 主菜单根节点 */
    private final VBox menuBox;

    /** 开始对战按钮 */
    private final Button startButton;

    /** 新手指南按钮（点击后由上层弹出独立指南窗口） */
    private final Button guideButton;

    /** 退出游戏按钮 */
    private final Button exitButton;

    /** 由上层注入的开局流程回调 */
    private Runnable onStartGame;

    /** 由上层注入的打开新手指南窗口回调 */
    private Runnable onOpenGuide;

    /** 由上层注入的退出游戏回调 */
    private Runnable onExit;

    public MenuViewImpl() {
        // 游戏标题
        Label titleLabel = new Label("GoldMainer");
        titleLabel.setStyle("-fx-font-size: 96px; -fx-font-weight: bold; "
                + "-fx-text-fill: linear-gradient(to bottom, #ffe259, #ffa751);");

        // 副标题
        Label subtitleLabel = new Label("双人PK版");
        subtitleLabel.setStyle("-fx-font-size: 28px; -fx-text-fill: #e8c87a;");

        // 开始对战按钮（样式统一由 UiStyle 提供）
        startButton = UiStyle.createGoldButton("开始对战");
        startButton.setOnAction(e -> {
            if (onStartGame != null) {
                onStartGame.run();
            }
        });

        // 新手指南按钮：点击后由上层弹出独立的操作指南窗口
        guideButton = UiStyle.createGoldButton("新手指南");
        guideButton.setOnAction(e -> {
            if (onOpenGuide != null) {
                onOpenGuide.run();
            }
        });

        // 退出游戏按钮
        exitButton = UiStyle.createGoldButton("退出游戏");
        exitButton.setOnAction(e -> {
            if (onExit != null) {
                onExit.run();
            }
        });

        // 垂直布局：标题 / 副标题 / 开始对战 / 新手指南 / 退出游戏
        menuBox = new VBox(20, titleLabel, subtitleLabel, startButton, guideButton, exitButton);
        menuBox.setAlignment(Pos.CENTER);
        menuBox.setPadding(new Insets(40));
        menuBox.setPrefSize(Config.WIDTH, Config.HEIGHT);
        // 矿洞风格的深色背景
        menuBox.setStyle("-fx-background-color: linear-gradient(to bottom, #2b1a0e, #4a2f17);");
    }

    @Override
    public Parent build() {
        return menuBox;
    }

    @Override
    public void show(Pane container) {
        UiStyle.mountOnce(container, menuBox);
    }

    @Override
    public void hide() {
        UiStyle.unmount(menuBox);
    }

    @Override
    public void setOnStartGame(Runnable action) {
        this.onStartGame = action;
    }

    @Override
    public void setOnOpenGuide(Runnable action) {
        this.onOpenGuide = action;
    }

    @Override
    public void setOnExit(Runnable action) {
        this.onExit = action;
    }
}
