// FR-UI HUDViewImpl：HUD 实现（顶部三栏 P1分数+全部道具状态 | 倒计时 | P2分数+全部道具状态）
package Main.view;

import Main.config.Config;
import Main.config.GameConfig;
import Main.model.GameModel;
import Main.model.Hook;
import Main.model.HookState;
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

    /** 矿工摇绳动画两帧（classpath 加载一次） */
    private final Image minerFrame1 = loadMinerImage("/images/miner/minerAction1.png");
    private final Image minerFrame2 = loadMinerImage("/images/miner/minerAction2.png");
    /** 两个矿工节点（层级在 topbg 背景图之上） */
    private final ImageView miner1 = new ImageView();
    private final ImageView miner2 = new ImageView();
    /** 矿工动画帧计时（120ms 切帧） */
    private int animFrame = 0;
    private long animLastTick = 0;
    private static final long FRAME_INTERVAL_MS = 120;
    /** 矿工显示尺寸与左脚对齐偏移（左脚在贴图中比例 45.5/126） */
    private static final double MINER_SIZE = 80;
    private static final double MINER_FOOT_RATIO = 45.5 / 126.0;

    private static Image loadMinerImage(String path) {
        try {
            return new Image(HUDViewImpl.class.getResourceAsStream(path));
        } catch (Exception e) {
            return null;
        }
    }

    public HUDViewImpl() {
        root = new Pane();
        root.setPickOnBounds(false); // 不拦截鼠标事件

        // ---------- 左栏：玩家1分数 + 道具两行 ----------
        Label p1Title = new Label("P1");
        p1Title.setStyle("-fx-font-size: 16px; -fx-text-fill: #8ec9ff;");

        p1ScoreLabel = new Label("P1: $0");
        p1ScoreLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; "
                + "-fx-text-fill: #5db0ff;");

        p1ItemLine1 = new Label("");
        p1ItemLine1.setStyle("-fx-font-size: 12px; -fx-text-fill: #9fd0ff;");
        p1ItemLine2 = new Label("");
        p1ItemLine2.setStyle("-fx-font-size: 12px; -fx-text-fill: #9fd0ff;");

        VBox p1Box = new VBox(0, p1Title, p1ScoreLabel, p1ItemLine1, p1ItemLine2);
        p1Box.setAlignment(Pos.CENTER_LEFT);
        p1Box.setPrefWidth(Config.WIDTH / 3.0);
        p1Box.setPadding(new Insets(0, 0, 0, 36));

        // ---------- 中栏：剩余时间 ----------
        Label timeTitle = new Label("剩余时间");
        timeTitle.setStyle("-fx-font-size: 16px; -fx-text-fill: #e8c87a;");

        timeLabel = new Label("90");
        timeLabel.setStyle("-fx-font-size: 40px; -fx-font-weight: bold; "
                + "-fx-text-fill: #ffe259;");

        VBox timeBox = new VBox(0, timeTitle, timeLabel);
        timeBox.setAlignment(Pos.CENTER);
        timeBox.setPrefWidth(Config.WIDTH / 3.0);

        // ---------- 右栏：玩家2分数 + 道具两行 ----------
        Label p2Title = new Label("P2");
        p2Title.setStyle("-fx-font-size: 16px; -fx-text-fill: #ff9d9d;");

        p2ScoreLabel = new Label("P2: $0");
        p2ScoreLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; "
                + "-fx-text-fill: #ff6b6b;");

        p2ItemLine1 = new Label("");
        p2ItemLine1.setStyle("-fx-font-size: 12px; -fx-text-fill: #ffb3b3;");
        p2ItemLine2 = new Label("");
        p2ItemLine2.setStyle("-fx-font-size: 12px; -fx-text-fill: #ffb3b3;");

        VBox p2Box = new VBox(0, p2Title, p2ScoreLabel, p2ItemLine1, p2ItemLine2);
        p2Box.setAlignment(Pos.CENTER_RIGHT);
        p2Box.setPrefWidth(Config.WIDTH / 3.0);
        p2Box.setPadding(new Insets(0, 36, 0, 0));

        // ---------- 组装三栏 HUD ----------
        HBox hudBar = new HBox(p1Box, timeBox, p2Box);
        hudBar.setPrefSize(Config.WIDTH, Config.HUD_HEIGHT);
        hudBar.setAlignment(Pos.CENTER);
        // 背景透明：底层 ImageView 承载背景图，hudBar 只负责文字信息（文字描边仍保留）
        hudBar.setStyle("-fx-background-color: transparent;");
        hudBar.setMouseTransparent(false);

        // ---------- HUD 背景图 topbg.png（最底层，拉伸铺满 HUD 栏，鼠标穿透）----------
        Image hudBgImage = new Image(
                getClass().getResourceAsStream("/images/HUD/topbg.png"));
        ImageView hudBg = new ImageView(hudBgImage);
        hudBg.setFitWidth(Config.WIDTH);
        hudBg.setFitHeight(Config.HUD_HEIGHT);
        hudBg.setPreserveRatio(false);
        hudBg.setMouseTransparent(true);

        // ---------- 两个矿工（最顶层，站在 topbg 背景图上；鼠标穿透）----------
        configureMiner(miner1, GameConfig.HOOK_ANCHOR_X_P1);
        configureMiner(miner2, GameConfig.HOOK_ANCHOR_X_P2);

        // 层级：背景图（底层）→ 文字栏 → 矿工（最上层，盖住背景图与绳头）
        root.getChildren().addAll(hudBg, hudBar, miner1, miner2);
    }

    /** 配置矿工节点：80×80，左脚水平对齐锚点 X，脚底落在 topbg 底边附近（绳从脚下出来） */
    private void configureMiner(ImageView miner, double anchorX) {
        miner.setFitWidth(MINER_SIZE);
        miner.setFitHeight(MINER_SIZE);
        miner.setPreserveRatio(false);
        miner.setMouseTransparent(true);
        miner.setVisible(false);
        // 水平：左脚（贴图比例 0.361 处）对齐绳索竖直线
        miner.setLayoutX(anchorX - MINER_SIZE * MINER_FOOT_RATIO);
        // 垂直：贴图内脚底距顶约 79.4px，令脚底落在 y≈88（topbg 底边/绳锚点附近）
        miner.setLayoutY(Config.HUD_HEIGHT - 2 - 127.0 / 128.0 * MINER_SIZE);
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

        // 矿工摇绳动画：收回（空钩/携带）时两帧交替，其他状态停在第一帧
        updateMiner(miner1, model.getHook1());
        updateMiner(miner2, model.getHook2());
    }

    /** 按钩爪状态更新矿工贴图：GRABBING/RETRACTING 时播放两帧动画，否则静止帧 */
    private void updateMiner(ImageView miner, Hook hook) {
        if (minerFrame1 == null || hook == null) {
            miner.setVisible(false);
            return;
        }
        miner.setVisible(true);
        HookState state = hook.getState();
        if (minerFrame2 != null
                && (state == HookState.GRABBING || state == HookState.RETRACTING)) {
            long now = System.currentTimeMillis();
            if (now - animLastTick >= FRAME_INTERVAL_MS) {
                animFrame = 1 - animFrame;
                animLastTick = now;
            }
            miner.setImage(animFrame == 0 ? minerFrame1 : minerFrame2);
        } else {
            miner.setImage(minerFrame1);
        }
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
