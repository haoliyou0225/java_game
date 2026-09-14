// FR-UI ResultViewImpl：胜负结算实现（遮罩 + 双方人物表情序列帧 + 获胜文案 + 双方分数 + 重新开始/返回主菜单）
package Main.view;

import Main.config.Config;
import Main.model.GameModel;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 胜负结算界面实现（FR-31）
 * <p>
 * 布局：全屏半透明深色遮罩 +
 * 左侧 P1 人物立绘、右侧 P2 人物立绘（按胜负播放 laugh/cry 序列帧循环）+
 * 居中结算卡片：标题（获胜方/平局文案）→ 玩家1分数（蓝）→ 玩家2分数（红）
 * → “重新开始”、“返回主菜单”两个按钮（水平并排）。
 * 分数颜色与 HUD（FR-29）保持一致：P1 蓝色 / P2 红色。
 * <p>
 * 表情规则：胜方播放 laugh，败方播放 cry，平局双方都播放 laugh；
 * 帧率由 {@link Config#RESULT_CHAR_FRAME_FPS} 配置（8~12 FPS），
 * 每个角色按自己的帧数独立循环；结算关闭时停止动画并重置到首帧。
 */
public class ResultViewImpl implements ResultView {

    /** 平局标题金色 */
    private static final String GOLD_TITLE_STYLE = "-fx-font-size: 64px; -fx-font-weight: bold; -fx-text-fill: #ffe259;";
    /** 玩家1获胜标题蓝色（与 HUD P1 配色一致） */
    private static final String BLUE_TITLE_STYLE = "-fx-font-size: 64px; -fx-font-weight: bold; -fx-text-fill: #5db0ff;";
    /** 玩家2获胜标题红色（与 HUD P2 配色一致） */
    private static final String RED_TITLE_STYLE = "-fx-font-size: 64px; -fx-font-weight: bold; -fx-text-fill: #ff6b6b;";

    // ===== 人物序列帧资源（classpath 加载一次，缺失则回退静态立绘） =====
    /** P1 大笑序列帧（/images/P1/laugh/p1_laugh1.png ...） */
    private static final List<Image> P1_LAUGH_FRAMES =
            loadFrames("/images/P1/laugh", "p1_laugh");
    /** P1 大哭序列帧（/images/P1/cry/p1_cry1.png ...） */
    private static final List<Image> P1_CRY_FRAMES =
            loadFrames("/images/P1/cry", "p1_cry");
    /** P2 大笑序列帧 */
    private static final List<Image> P2_LAUGH_FRAMES =
            loadFrames("/images/P2/laugh", "p2_laugh");
    /** P2 大哭序列帧 */
    private static final List<Image> P2_CRY_FRAMES =
            loadFrames("/images/P2/cry", "p2_cry");
    /** P1 静态立绘（序列帧缺失时兜底） */
    private static final Image P1_IDLE_IMAGE = loadImageOrNull("/images/P1/p1人物.png");
    /** P2 静态立绘（序列帧缺失时兜底） */
    private static final Image P2_IDLE_IMAGE = loadImageOrNull("/images/P2/p2人物.png");

    /** 结算界面根节点（全屏遮罩 Pane：人物绝对定位在左右，中间放结算卡片） */
    private final Pane overlay;

    /** 居中结算卡片（标题/分数/按钮） */
    private final VBox resultBox;

    /** 标题：获胜方/平局文案 */
    private final Label titleLabel;

    /** 玩家1最终分数标签 */
    private final Label score1Label;

    /** 玩家2最终分数标签 */
    private final Label score2Label;

    /** 左侧 P1 人物立绘（laugh/cry 序列帧） */
    private final ImageView p1CharView = createCharView();
    /** 右侧 P2 人物立绘（laugh/cry 序列帧） */
    private final ImageView p2CharView = createCharView();

    /** 序列帧循环时间线（show 时按配置帧率启动，hide 时停止） */
    private Timeline frameTimeline;
    /** 当前结算 P1 使用的帧序列（laugh/cry，每角色帧数可不同） */
    private List<Image> p1Frames = Collections.emptyList();
    /** 当前结算 P2 使用的帧序列 */
    private List<Image> p2Frames = Collections.emptyList();
    /** P1 当前帧索引（关闭结算时重置为 0） */
    private int p1FrameIndex;
    /** P2 当前帧索引 */
    private int p2FrameIndex;

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
        // 结算卡片本身透明，遮罩底色由 overlay 承担；
        // pickOnBounds=false 让空白区域不参与拾取（按钮是子节点，仍可正常点击）
        resultBox.setStyle("-fx-background-color: transparent;");
        resultBox.setPickOnBounds(false);
        actionBox.setPickOnBounds(false);

        // 全屏半透明深色遮罩：覆盖对局画面，突出结算内容
        overlay = new Pane();
        overlay.setPrefSize(Config.WIDTH, Config.HEIGHT);
        overlay.setStyle("-fx-background-color: rgba(20, 12, 4, 0.88);");
        // 层级：人物在遮罩之上、中间卡片之下（人物居左右，与卡片不重叠）
        overlay.getChildren().addAll(p1CharView, p2CharView, resultBox);
    }

    /** 创建人物立绘 ImageView：高度按屏幕比例，宽度按帧图比例，鼠标穿透 */
    private static ImageView createCharView() {
        ImageView view = new ImageView();
        view.setFitHeight(Config.HEIGHT * Config.RESULT_CHAR_HEIGHT_RATIO);
        view.setPreserveRatio(true);
        view.setSmooth(true);
        view.setMouseTransparent(true); // 纯装饰，不拦截按钮点击
        return view;
    }

    @Override
    public Parent build() {
        return overlay;
    }

    @Override
    public void show(Pane container, GameModel model) {
        // 读取双方最终分数（View 仅依赖 Model 接口，不依赖实现类）
        int score1 = model.getPlayer1().getScore();
        int score2 = model.getPlayer2().getScore();

        // 胜负判定下沉到 Model 层（FR-31）：本层仅负责文案、颜色与表情帧选择
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

        // 表情：胜方 laugh / 败方 cry / 平局双方 laugh；序列帧缺失时回退静态立绘
        p1Frames = pickFrames(winner >= 0 ? P1_LAUGH_FRAMES : P1_CRY_FRAMES, P1_IDLE_IMAGE);
        p2Frames = pickFrames(winner <= 0 ? P2_LAUGH_FRAMES : P2_CRY_FRAMES, P2_IDLE_IMAGE);

        // 重置到各自首帧并定位（P1 左、P2 右，对称且不遮挡中间卡片）
        p1FrameIndex = 0;
        p2FrameIndex = 0;
        applyFrame(p1CharView, p1Frames, 0);
        applyFrame(p2CharView, p2Frames, 0);
        layoutChar(p1CharView, true);
        layoutChar(p2CharView, false);

        startFrameAnimation();

        // 幂等挂载：防止重复添加
        UiStyle.mountOnce(container, overlay);
    }

    @Override
    public void hide() {
        // 关闭结算：停止序列帧动画并重置到首帧
        stopFrameAnimation();
        p1FrameIndex = 0;
        p2FrameIndex = 0;
        applyFrame(p1CharView, p1Frames, 0);
        applyFrame(p2CharView, p2Frames, 0);
        UiStyle.unmount(overlay);
    }

    /**
     * 启动序列帧循环：统一帧率 {@link Config#RESULT_CHAR_FRAME_FPS}，
     * 每次 tick 两个角色各自按自己的帧数取模推进。
     * 任一方帧数 ≤ 1（纯静态兜底图）时无需启动动画。
     */
    private void startFrameAnimation() {
        stopFrameAnimation();
        if (p1Frames.size() <= 1 && p2Frames.size() <= 1) {
            return;
        }
        Duration frameInterval = Duration.seconds(1.0 / Config.RESULT_CHAR_FRAME_FPS);
        frameTimeline = new Timeline(new KeyFrame(frameInterval, e -> {
            if (!p1Frames.isEmpty()) {
                p1FrameIndex = (p1FrameIndex + 1) % p1Frames.size();
                p1CharView.setImage(p1Frames.get(p1FrameIndex));
            }
            if (!p2Frames.isEmpty()) {
                p2FrameIndex = (p2FrameIndex + 1) % p2Frames.size();
                p2CharView.setImage(p2Frames.get(p2FrameIndex));
            }
        }));
        frameTimeline.setCycleCount(Animation.INDEFINITE);
        frameTimeline.play();
    }

    /** 停止序列帧循环（幂等） */
    private void stopFrameAnimation() {
        if (frameTimeline != null) {
            frameTimeline.stop();
            frameTimeline = null;
        }
    }

    /** 选择表情帧序列：序列帧可用则使用，否则用静态立绘包成单帧列表，再否则空列表 */
    private static List<Image> pickFrames(List<Image> frames, Image idleImage) {
        if (frames != null && !frames.isEmpty()) {
            return frames;
        }
        if (idleImage != null && !idleImage.isError()) {
            return Collections.singletonList(idleImage);
        }
        return Collections.emptyList();
    }

    /** 将指定索引的帧应用到 ImageView（空序列时清空图像） */
    private void applyFrame(ImageView view, List<Image> frames, int index) {
        if (frames == null || frames.isEmpty()) {
            view.setImage(null);
            view.setVisible(false);
            return;
        }
        view.setImage(frames.get(index));
        view.setVisible(true);
    }

    /**
     * 定位人物立绘：垂直居中；P1 贴左、P2 贴右（按屏幕宽高比例，多分辨率适配）。
     * 宽度由首帧原图比例算出，保证左右对称且不与中间结算卡片重叠。
     */
    private void layoutChar(ImageView view, boolean leftSide) {
        double charH = Config.HEIGHT * Config.RESULT_CHAR_HEIGHT_RATIO;
        Image img = view.getImage();
        double charW = (img != null && img.getWidth() > 0 && img.getHeight() > 0)
                ? charH * img.getWidth() / img.getHeight()
                : charH;
        double sideMargin = Config.WIDTH * Config.RESULT_CHAR_SIDE_MARGIN_RATIO;
        view.setLayoutX(leftSide ? sideMargin : Config.WIDTH - sideMargin - charW);
        view.setLayoutY((Config.HEIGHT - charH) / 2.0);
    }

    /**
     * 按目录 + 前缀探测加载序列帧：{dir}/{prefix}1.png、{prefix}2.png ...
     * 直到资源缺失或加载失败为止；因此每个角色可拥有自己的帧数。
     */
    private static List<Image> loadFrames(String dir, String filePrefix) {
        List<Image> frames = new ArrayList<>();
        for (int i = 1; i <= 64; i++) {
            String path = dir + "/" + filePrefix + i + ".png";
            try (InputStream is = ResultViewImpl.class.getResourceAsStream(path)) {
                if (is == null) {
                    break;
                }
                Image img = new Image(is); // InputStream 默认同步加载，立即可取尺寸
                if (img.isError()) {
                    break;
                }
                frames.add(img);
            } catch (Exception e) {
                break;
            }
        }
        return Collections.unmodifiableList(frames);
    }

    /** 加载单张图片（失败返回 null，调用方做兜底） */
    private static Image loadImageOrNull(String path) {
        try (InputStream is = ResultViewImpl.class.getResourceAsStream(path)) {
            if (is == null) {
                return null;
            }
            Image img = new Image(is);
            return img.isError() ? null : img;
        } catch (Exception e) {
            return null;
        }
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
