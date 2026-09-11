// FR-UI MenuViewImpl：主菜单实现（背景图+标题图+三按钮），基于 feature_ui 分支
package Main.view;

import Main.config.Config;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * 主菜单视图实现（FR-32）
 * <p>
 * 渲染结构：
 * <ul>
 *   <li>背景层：menu-background.png 拉伸填充整个 1280×720 窗口；</li>
 *   <li>标题层：gold-miner-text.png 作为标题图片，水平居中，位于画面上半部；</li>
 *   <li>按钮层：开始对战 / 新手指南 / 退出游戏 三个按钮纵向排列，水平居中，
 *       位于标题下方，样式由 {@link UiStyle#createGoldButton} 统一提供（样式不变）。</li>
 * </ul>
 * <p>
 * 说明：完整的双人键位表与游戏规则已抽取到可复用组件 {@link ControlGuidePane}，
 * 由“新手指南”按钮回调上层弹出独立窗口展示（FR-26）。
 */
public class MenuViewImpl implements MenuView {

    /** 主菜单根容器（StackPane：背景图层 + 内容层） */
    private final StackPane root;

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
        // ---- 加载背景图（仅加载一次，拉伸填充窗口）----
        Image bgImage = new Image(
                getClass().getResourceAsStream("/images/menu/menu-background.png"));
        BackgroundImage bg = new BackgroundImage(bgImage,
                BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.DEFAULT,
                new BackgroundSize(Config.WIDTH, Config.HEIGHT, false, false, false, false));

        // ---- 加载标题图片 gold-miner-text.png ----
        Image titleImage = new Image(
                getClass().getResourceAsStream("/images/menu/gold-miner-text.png"));
        ImageView titleView = new ImageView(titleImage);
        // 标题图按比例放大到约 600px 宽（原图 425×87，放大后约 600×123）
        titleView.setFitWidth(600);
        titleView.setPreserveRatio(true);

        // 副标题（保留，位于标题图下方）
        Label subtitleLabel = new Label("双人PK版");
        subtitleLabel.setStyle("-fx-font-size: 24px; -fx-text-fill: #e8c87a; "
                + "-fx-effect: dropshadow(one-pass-box, #000000, 3, 1.0, 1, 1);");

        // ---- 三个按钮（样式由 UiStyle 统一提供，样式不变）----
        startButton = UiStyle.createGoldButton("开始对战");
        startButton.setOnAction(e -> {
            if (onStartGame != null) {
                onStartGame.run();
            }
        });

        guideButton = UiStyle.createGoldButton("新手指南");
        guideButton.setOnAction(e -> {
            if (onOpenGuide != null) {
                onOpenGuide.run();
            }
        });

        exitButton = UiStyle.createGoldButton("退出游戏");
        exitButton.setOnAction(e -> {
            if (onExit != null) {
                onExit.run();
            }
        });

        // ---- 内容层：标题图 + 副标题 + 三按钮，纵向居中排列 ----
        VBox contentBox = new VBox(24, titleView, subtitleLabel, startButton, guideButton, exitButton);
        contentBox.setAlignment(Pos.CENTER);
        contentBox.setPadding(new Insets(60, 0, 0, 0)); // 整体略微上移，让标题位于画面上半部
        contentBox.setPrefSize(Config.WIDTH, Config.HEIGHT);

        // ---- 组装根容器：背景图层 + 内容层 ----
        root = new StackPane();
        root.setBackground(new Background(bg));
        root.setPrefSize(Config.WIDTH, Config.HEIGHT);
        root.getChildren().add(contentBox);
    }

    @Override
    public Parent build() {
        return root;
    }

    @Override
    public void show(Pane container) {
        UiStyle.mountOnce(container, root);
    }

    @Override
    public void hide() {
        UiStyle.unmount(root);
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
