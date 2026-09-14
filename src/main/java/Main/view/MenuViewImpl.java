// FR-UI MenuViewImpl：主菜单实现（背景图+标题图+两侧矿工+金块START按钮+三小按钮）
package Main.view;

import Main.config.Config;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * 主菜单视图实现（FR-32）
 * <p>
 * 渲染结构（1280×720）：
 * <ul>
 *   <li>背景层：menu-background.png 拉伸填充整个窗口；</li>
 *   <li>顶部：gold-miner-text.png 标题图 + “双人PK版”副标题，水平居中；</li>
 *   <li>正中：P1 矿工（金块左侧）+ 金块开始按钮（叠加 START 字样、外围慢速金色呼吸光晕，
 *       点击进入游戏）+ P2 矿工（金块右侧）；</li>
 *   <li>底部：新手指南 / 设置 / 退出游戏 三个缩小版按钮（quitbtn.png 底图），
 *       纵向居中排列，与中央区域不重叠。</li>
 * </ul>
 */
public class MenuViewImpl implements MenuView {

    /** 金块开始按钮图片显示宽度（高度按原图比例，按要求保持不变） */
    private static final double START_BTN_WIDTH = 300;
    /** 叠加在金块上的 START 字样图片宽度 */
    private static final double START_TEXT_WIDTH = 200;
    /** 两侧矿工立绘高度（宽度按原图比例） */
    private static final double CHAR_HEIGHT = 290;
    /** 矿工与金块之间的水平间距 */
    private static final double CHAR_GAP = 36;
    /** 底部小按钮尺寸 */
    private static final double SMALL_BTN_WIDTH = 132;
    private static final double SMALL_BTN_HEIGHT = 42;
    /** 底部小按钮之间的垂直间距 */
    private static final double SMALL_BTN_GAP = 12;
    /** 金色光晕呼吸周期的一半（秒），autoReverse 后完整周期约 3.6 秒，变化较慢 */
    private static final double GLOW_HALF_CYCLE_SEC = 1.8;

    /** 主菜单根容器（StackPane：背景 + 顶部标题 + 中央金块群 + 底部三按钮） */
    private final StackPane root;

    /** 开始对战图片按钮（点击进入游戏；金色光晕直接挂在该按钮上呼吸，画面上只有一个金块） */
    private final Button startButton;
    /** 金色光晕慢速呼吸动画（show 时播放，hide 时停止） */
    private final Timeline glowTimeline;

    /** 新手指南按钮（点击后由上层弹出独立指南窗口） */
    private final Button guideButton;

    /** 设置按钮（点击后由上层弹出独立设置窗口） */
    private final Button settingsButton;

    /** 退出游戏按钮 */
    private final Button exitButton;

    /** 由上层注入的开局流程回调 */
    private Runnable onStartGame;

    /** 由上层注入的打开新手指南窗口回调 */
    private Runnable onOpenGuide;

    /** 由上层注入的打开设置窗口窗口回调 */
    private Runnable onOpenSettings;

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

        // ---- 顶部：标题图 gold-miner-text.png + “双人PK版”副标题 ----
        Image titleImage = new Image(
                getClass().getResourceAsStream("/images/menu/gold-miner-text.png"));
        ImageView titleView = new ImageView(titleImage);
        // 标题图按比例放大到约 600px 宽（原图 425×87，放大后约 600×123）
        titleView.setFitWidth(600);
        titleView.setPreserveRatio(true);

        Label subtitleLabel = new Label("双人PK版");
        subtitleLabel.setStyle("-fx-font-size: 24px; -fx-text-fill: #e8c87a; "
                + "-fx-effect: dropshadow(one-pass-box, #000000, 3, 1.0, 1, 1);");

        VBox topBox = new VBox(12, titleView, subtitleLabel);
        topBox.setAlignment(Pos.CENTER);
        topBox.setPadding(new javafx.geometry.Insets(30, 0, 0, 0));
        // 关键：禁止 VBox 被 StackPane 拉伸填满，否则内容会跑到屏幕正中
        topBox.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane.setAlignment(topBox, Pos.TOP_CENTER);

        // ---- 中央：P1 + 金块（START 字样 + 金色呼吸光晕）+ P2 ----
        Image startImage = new Image(
                getClass().getResourceAsStream("/images/menu/开始按键.png"));

        // 金色光晕直接作为按钮图片的 DropShadow：只在金块外围发光，不会出现第二个金块
        DropShadow glow = new DropShadow(BlurType.GAUSSIAN,
                Color.rgb(255, 194, 77, 0.65), 18, 0.25, 0, 0);

        // 真正可点击的金块按钮：透明背景、无边框，仅显示金块图片
        startButton = new Button();
        ImageView startIcon = new ImageView(startImage);
        startIcon.setFitWidth(START_BTN_WIDTH);
        startIcon.setPreserveRatio(true);
        startIcon.setEffect(glow); // 光晕挂在图片上，金块始终只有一个
        startButton.setGraphic(startIcon);
        startButton.setStyle("-fx-background-color: transparent; -fx-background-insets: 0; "
                + "-fx-padding: 0; -fx-border-width: 0; -fx-cursor: hand;");
        // 禁止按钮被 StackPane 横向拉满，保持图片原始尺寸
        startButton.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        startButton.setPickOnBounds(true);
        startButton.setOnAction(e -> {
            if (onStartGame != null) {
                onStartGame.run();
            }
        });
        // 点击音效（addEventHandler 不覆盖上面的 onAction，两者共存）
        startButton.addEventHandler(ActionEvent.ACTION,
                e -> AudioManager.get().playSfx("Select"));

        // 叠加在金块中央的 START 字样（透明底，鼠标穿透，不影响点击）
        Image startTextImage = new Image(
                getClass().getResourceAsStream("/images/menu/start字样.png"));
        ImageView startTextView = new ImageView(startTextImage);
        startTextView.setFitWidth(START_TEXT_WIDTH);
        startTextView.setPreserveRatio(true);
        startTextView.setMouseTransparent(true);
        // 字样略偏金块中心（金块视觉中心偏上一点更自然，这里保持正中）
        StackPane goldHolder = new StackPane(startButton, startTextView);
        goldHolder.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        // 两侧矿工立绘（P1 朝右、P2 朝左，正好面向金块）
        ImageView p1View = makeCharView("/images/menu/menu_p1.png");
        ImageView p2View = makeCharView("/images/menu/menu_p2.png");

        // 三者横向排列：P1 在金块左侧、P2 在金块右侧，间距对称保证金块居中
        HBox centerBox = new HBox(CHAR_GAP, p1View, goldHolder, p2View);
        centerBox.setAlignment(Pos.CENTER);
        centerBox.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane.setAlignment(centerBox, Pos.CENTER);

        // 光晕慢速呼吸：只动画 DropShadow 的半径/扩散/颜色透明度，金块本体不变形、无第二张图
        glowTimeline = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(glow.radiusProperty(), 18),
                        new KeyValue(glow.spreadProperty(), 0.25),
                        new KeyValue(glow.colorProperty(),
                                Color.rgb(255, 194, 77, 0.55))),
                new KeyFrame(Duration.seconds(GLOW_HALF_CYCLE_SEC),
                        new KeyValue(glow.radiusProperty(), 58, Interpolator.EASE_BOTH),
                        new KeyValue(glow.spreadProperty(), 0.72, Interpolator.EASE_BOTH),
                        new KeyValue(glow.colorProperty(),
                                Color.rgb(255, 240, 168, 0.95), Interpolator.EASE_BOTH)));
        glowTimeline.setAutoReverse(true);
        glowTimeline.setCycleCount(Animation.INDEFINITE);

        // ---- 底部：另外三个按钮缩小、使用 quitbtn.png 底图 ----
        guideButton = makeSmallButton("新手指南");
        guideButton.setOnAction(e -> {
            if (onOpenGuide != null) {
                onOpenGuide.run();
            }
        });

        settingsButton = makeSmallButton("设置");
        settingsButton.setOnAction(e -> {
            if (onOpenSettings != null) {
                onOpenSettings.run();
            }
        });

        exitButton = makeSmallButton("退出游戏");
        exitButton.setOnAction(e -> {
            if (onExit != null) {
                onExit.run();
            }
        });

        VBox bottomBox = new VBox(SMALL_BTN_GAP, guideButton, settingsButton, exitButton);
        bottomBox.setAlignment(Pos.CENTER);
        bottomBox.setPadding(new javafx.geometry.Insets(0, 0, 30, 0));
        // 关键：禁止 VBox 被 StackPane 拉伸填满，保证三按钮固定在底部居中
        bottomBox.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane.setAlignment(bottomBox, Pos.BOTTOM_CENTER);

        // ---- 组装根容器：背景 + 顶部标题 + 中央金块群 + 底部三按钮 ----
        root = new StackPane();
        root.setBackground(new Background(bg));
        root.setPrefSize(Config.WIDTH, Config.HEIGHT);
        root.getChildren().addAll(topBox, centerBox, bottomBox);
    }

    /** 创建一个矿工立绘视图：固定高度、保持比例、鼠标穿透（仅装饰，不挡点击） */
    private ImageView makeCharView(String resourcePath) {
        ImageView view = new ImageView(new Image(getClass().getResourceAsStream(resourcePath)));
        view.setFitHeight(CHAR_HEIGHT);
        view.setPreserveRatio(true);
        view.setMouseTransparent(true);
        return view;
    }

    /** 创建底部小按钮：quitbtn.png 拉伸作底图 + 深棕色加粗文字，尺寸统一缩小 */
    private Button makeSmallButton(String text) {
        Image btnImage = new Image(
                getClass().getResourceAsStream("/images/menu/quitbtn.png"));
        BackgroundImage bgImg = new BackgroundImage(btnImage,
                BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                new BackgroundSize(SMALL_BTN_WIDTH, SMALL_BTN_HEIGHT,
                        false, false, false, false));
        Button button = new Button(text);
        button.setBackground(new Background(bgImg));
        button.setPrefSize(SMALL_BTN_WIDTH, SMALL_BTN_HEIGHT);
        button.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        button.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        button.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #6a3b05; "
                + "-fx-padding: 0; -fx-cursor: hand;");
        return button;
    }

    @Override
    public Parent build() {
        return root;
    }

    @Override
    public void show(Pane container) {
        glowTimeline.play();
        UiStyle.mountOnce(container, root);
    }

    @Override
    public void hide() {
        glowTimeline.stop();
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
    public void setOnOpenSettings(Runnable action) {
        this.onOpenSettings = action;
    }

    @Override
    public void setOnExit(Runnable action) {
        this.onExit = action;
    }
}
