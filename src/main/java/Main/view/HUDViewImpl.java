// FR-UI HUDViewImpl：HUD 实现（顶部三栏 P1分数+全部道具状态 | 倒计时 | P2分数+全部道具状态）
package Main.view;

import Main.config.Config;
import Main.config.GameConfig;
import Main.model.GameModel;
import Main.model.Hook;
import Main.model.HookState;
import Main.model.Player;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * HUD 视图实现（FR-29 + FR-30 + FR-22）。
 * 顶部三栏布局：左侧玩家1分数与已获得道具图标+数量 | 中间剩余时间 | 右侧玩家2分数与已获得道具图标+数量。
 * 道具图标行只显示当前库存 > 0 的道具（图标 ×数量），未获得或用完的不显示。
 *
 * 道具用尽渐隐动画：当上一帧显示、本帧不显示某道具时，在 fadingLayer 上叠加一个胶囊副本，
 * 播放 alpha 1→0 + scale 1→0.8 共 0.3 秒动画，期间该副本拦截鼠标事件防止重复使用，
 * 动画结束后自动移除。逻辑层扣减立即生效，视觉动画不阻塞操作。
 */
public class HUDViewImpl implements HUDView {

    /** 玩家1分数标签 */
    private final Label p1ScoreLabel;
    /** 玩家2分数标签 */
    private final Label p2ScoreLabel;

    /** 玩家1道具图标行（动态装填图标×数量胶囊） */
    private final HBox p1ItemBox;
    /** 玩家2道具图标行 */
    private final HBox p2ItemBox;

    /** 倒计时数字标签 */
    private final Label timeLabel;

    /** HUD 根节点 */
    private final Pane root;

    /** 渐隐动画层：叠加在 HUD 顶层，承载用尽道具的胶囊副本，0.3 秒后销毁 */
    private final Pane fadingLayer = new Pane();

    /** 渐隐动画时长（秒）：alpha 1→0 + scale 1→0.8 */
    private static final double FADE_DURATION_SEC = 0.3;

    /** 道具图标种类：与 HUD 显示一一对应，用于追踪上一帧/本帧差异并触发渐隐动画 */
    private enum ItemKind {
        /** 炸药库存 */
        DYNAMITE,
        /** 强力药水（生效中倒计时 或 库存） */
        POWER_POTION,
        /** 冰冻箱——被冻状态（带剩余秒） */
        FREEZE_BOX_FROZEN,
        /** 冰冻箱——库存（×N） */
        FREEZE_BOX_STOCK,
        /** 幸运草库存 */
        LUCKY_CLOVER,
        /** 钻石升级库存 */
        DIAMOND_BOOST,
        /** 石头书库存 */
        STONE_BOOK
    }

    /** 胶囊快照：记录某 capsule 在 fadingLayer 坐标系下的位置 + 外观，用于重建渐隐副本 */
    private static final class CapsuleSnapshot {
        final double x, y;     // 在 fadingLayer 坐标系下的左上角
        final Image image;
        final String text;
        final String color;
        CapsuleSnapshot(double x, double y, Image image, String text, String color) {
            this.x = x; this.y = y; this.image = image; this.text = text; this.color = color;
        }
    }

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

    /** 道具图标缓存（路径→Image，classpath 加载一次） */
    private static final Image ICON_DYNAMITE   = loadIcon("/images/HUD/炸药.png");
    private static final Image ICON_POWER       = loadIcon("/images/HUD/强力药水.png");
    private static final Image ICON_FREEZE      = loadIcon("/images/HUD/冰冻箱.png");
    private static final Image ICON_CLOVER      = loadIcon("/images/HUD/幸运草.png");
    private static final Image ICON_DIAMOND_B   = loadIcon("/images/HUD/钻石升级.png");
    private static final Image ICON_STONE_BOOK  = loadIcon("/images/HUD/石头收藏书.png");

    /** 玩家静态头像（classpath 加载一次；HUD 顶部仅显示静态图，不播序列帧动画） */
    private static final Image AVATAR_P1 = loadIcon("/images/P1/p1人物.png");
    private static final Image AVATAR_P2 = loadIcon("/images/P2/p2人物.png");

    private static Image loadIcon(String path) {
        try {
            return new Image(HUDViewImpl.class.getResourceAsStream(path));
        } catch (Exception e) {
            return null;
        }
    }

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

        // ---------- 左栏：玩家1头像 + 分数 + 道具图标行 ----------
        p1ScoreLabel = new Label("$0");
        p1ScoreLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; "
                + "-fx-text-fill: #5db0ff;");

        // 头像在左、分数在右（静态头像，鼠标穿透，不遮挡/拦截其他 HUD）
        HBox p1ScoreRow = new HBox(8);
        p1ScoreRow.setAlignment(Pos.CENTER_LEFT);
        ImageView p1Avatar = makeAvatar(AVATAR_P1);
        if (p1Avatar != null) {
            p1ScoreRow.getChildren().add(p1Avatar);
        }
        p1ScoreRow.getChildren().add(p1ScoreLabel);

        p1ItemBox = new HBox(8);
        p1ItemBox.setAlignment(Pos.CENTER_LEFT);
        p1ItemBox.setPrefHeight(32);

        VBox p1Box = new VBox(2, p1ScoreRow, p1ItemBox);
        p1Box.setAlignment(Pos.TOP_LEFT);
        p1Box.setPrefWidth(Config.WIDTH / 3.0);
        p1Box.setPadding(new Insets(6, 0, 0, 36));

        // ---------- 中栏：剩余时间 ----------
        Label timeTitle = new Label("剩余时间");
        timeTitle.setStyle("-fx-font-size: 16px; -fx-text-fill: #e8c87a;");

        timeLabel = new Label("90");
        timeLabel.setStyle("-fx-font-size: 40px; -fx-font-weight: bold; "
                + "-fx-text-fill: #ffe259;");

        VBox timeBox = new VBox(0, timeTitle, timeLabel);
        timeBox.setAlignment(Pos.CENTER);
        timeBox.setPrefWidth(Config.WIDTH / 3.0);

        // ---------- 右栏：玩家2头像 + 分数 + 道具图标行 ----------
        p2ScoreLabel = new Label("$0");
        p2ScoreLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; "
                + "-fx-text-fill: #ff6b6b;");

        // 分数在左、头像在右（右对齐，与 P1 左右对称；静态头像，鼠标穿透）
        HBox p2ScoreRow = new HBox(8);
        p2ScoreRow.setAlignment(Pos.CENTER_RIGHT);
        p2ScoreRow.getChildren().add(p2ScoreLabel);
        ImageView p2Avatar = makeAvatar(AVATAR_P2);
        if (p2Avatar != null) {
            p2ScoreRow.getChildren().add(p2Avatar);
        }

        p2ItemBox = new HBox(8);
        p2ItemBox.setAlignment(Pos.CENTER_RIGHT);
        p2ItemBox.setPrefHeight(32);

        VBox p2Box = new VBox(2, p2ScoreRow, p2ItemBox);
        p2Box.setAlignment(Pos.TOP_RIGHT);
        p2Box.setPrefWidth(Config.WIDTH / 3.0);
        p2Box.setPadding(new Insets(6, 36, 0, 0));

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

        // ---------- 渐隐动画层（最顶层，承接用尽道具胶囊副本）----------
        fadingLayer.setPickOnBounds(false);    // 整层只让 fading 节点本身拦截鼠标
        fadingLayer.setMouseTransparent(false);
        fadingLayer.setStyle("-fx-background-color: transparent;");

        // 层级：背景图（底层）→ 文字栏 → 矿工 → fadingLayer（最顶层）
        root.getChildren().addAll(hudBg, hudBar, miner1, miner2, fadingLayer);
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

    /**
     * 构造 HUD 顶部玩家静态头像：高度 {@link Config#HUD_AVATAR_HEIGHT}，
     * 宽度按原图比例自动缩放；纯装饰节点，鼠标穿透不拦截点击。
     * 图片缺失或加载失败时返回 null（调用方跳过头像，只保留分数文字）。
     */
    private ImageView makeAvatar(Image avatar) {
        if (avatar == null || avatar.isError()) {
            return null;
        }
        ImageView iv = new ImageView(avatar);
        iv.setFitHeight(Config.HUD_AVATAR_HEIGHT);
        iv.setPreserveRatio(true);
        iv.setSmooth(true);
        iv.setMouseTransparent(true);
        return iv;
    }

    @Override
    public Parent build() {
        return root;
    }

    @Override
    public void render(GameModel model) {
        // 玩家1分数（FR-29：独立显示，不混淆；玩家身份由左侧静态头像表达）
        p1ScoreLabel.setText("$" + model.getPlayer1().getScore());

        // 倒计时（FR-30：向上取整显示整数秒）
        int seconds = (int) Math.ceil(model.getRemainingTime());
        timeLabel.setText(String.valueOf(seconds));

        // 玩家2分数（FR-29：独立显示，不混淆；玩家身份由右侧静态头像表达）
        p2ScoreLabel.setText("$" + model.getPlayer2().getScore());

        // 在 fillItemBox 调用 clear 之前，先快照当前每个 capsule 的位置与外观（上一帧状态）
        Map<ItemKind, CapsuleSnapshot> prev1 = snapshotBox(p1ItemBox);
        Map<ItemKind, CapsuleSnapshot> prev2 = snapshotBox(p2ItemBox);

        // FR-22：只显示当前库存 > 0 的道具（图标×数量），未获得或用完的不显示
        fillItemBox(p1ItemBox, model.getPlayer1(), model.getHook1());
        fillItemBox(p2ItemBox, model.getPlayer2(), model.getHook2());

        // 对比上一帧与本帧：找出"上一帧有、本帧无"的道具，启动渐隐动画
        triggerFadeOutForMissing(prev1, p1ItemBox);
        triggerFadeOutForMissing(prev2, p2ItemBox);

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
     * 填充道具图标行：只显示库存 > 0 的道具（图标 ×数量）。
     * 炸药/冰冻箱/幸运草/钻石升级/石头书按库存数量显示；
     * 强力药水按生效中显示（剩余秒），未生效不显示。
     * 冰冻中状态优先显示冰冻箱图标 + 剩余秒。
     * 每个 capsule 设置 userData(ItemKind) 供渐隐动画系统对比上一帧/本帧差异。
     */
    private void fillItemBox(HBox box, Player player, Hook hook) {
        box.getChildren().clear();

        // 冰冻中提示（若被冻，优先显示带剩余秒的冰冻箱图标）
        if (hook != null && hook.isFrozen()) {
            box.getChildren().add(makeCapsule(ICON_FREEZE,
                    (int) Math.ceil(hook.getFreezeRemaining()) + "s", "#9fd0ff",
                    ItemKind.FREEZE_BOX_FROZEN));
        }

        if (player.getDynamiteCount() > 0) {
            box.getChildren().add(makeCapsule(ICON_DYNAMITE,
                    "×" + player.getDynamiteCount(), "#e8821e",
                    ItemKind.DYNAMITE));
        }
        if (hook != null && hook.isSpeedBoostActive()) {
            box.getChildren().add(makeCapsule(ICON_POWER,
                    (int) Math.ceil(hook.getSpeedBoostRemaining()) + "s", "#ff9d9d",
                    ItemKind.POWER_POTION));
        } else if (player.getPowerPotionCount() > 0) {
            box.getChildren().add(makeCapsule(ICON_POWER,
                    "×" + player.getPowerPotionCount(), "#ffb3b3",
                    ItemKind.POWER_POTION));
        }
        if (player.getFreezeBoxCount() > 0 && !(hook != null && hook.isFrozen())) {
            box.getChildren().add(makeCapsule(ICON_FREEZE,
                    "×" + player.getFreezeBoxCount(), "#9fd0ff",
                    ItemKind.FREEZE_BOX_STOCK));
        }
        if (player.getLuckyCloverCount() > 0) {
            box.getChildren().add(makeCapsule(ICON_CLOVER,
                    "×" + player.getLuckyCloverCount(), "#9be3a0",
                    ItemKind.LUCKY_CLOVER));
        }
        if (player.getDiamondBoostCount() > 0) {
            box.getChildren().add(makeCapsule(ICON_DIAMOND_B,
                    "×" + player.getDiamondBoostCount(), "#9fd0ff",
                    ItemKind.DIAMOND_BOOST));
        }
        if (player.getStoneBookCount() > 0) {
            box.getChildren().add(makeCapsule(ICON_STONE_BOOK,
                    "×" + player.getStoneBookCount(), "#d0c8a0",
                    ItemKind.STONE_BOOK));
        }
    }

    /**
     * 快照当前 box 内每个 capsule 的 ItemKind、位置与外观。
     * 必须在 fillItemBox 调用 clear 之前调用，否则数据已被清空。
     */
    private Map<ItemKind, CapsuleSnapshot> snapshotBox(HBox box) {
        Map<ItemKind, CapsuleSnapshot> map = new LinkedHashMap<>();
        for (Node n : box.getChildren()) {
            if (!(n instanceof HBox capsule)) continue;
            Object ud = capsule.getUserData();
            if (!(ud instanceof ItemKind kind)) continue;
            // 转换 capsule 在 fadingLayer 坐标系下的位置（fadingLayer 与 root 同坐标系）
            Bounds localBounds = capsule.getBoundsInLocal();
            Bounds sceneBounds = capsule.localToScene(localBounds);
            Bounds layerBounds = fadingLayer.sceneToLocal(sceneBounds);
            ImageView iv = findImageView(capsule);
            Label lbl = findLabel(capsule);
            Image img = iv != null ? iv.getImage() : null;
            String text = lbl != null ? lbl.getText() : "";
            String color = lbl != null ? parseTextColor(lbl) : "#ffffff";
            map.put(kind, new CapsuleSnapshot(
                    layerBounds.getMinX(), layerBounds.getMinY(), img, text, color));
        }
        return map;
    }

    /**
     * 检测本帧缺失的道具种类，对每个"上一帧显示、本帧不显示"的道具触发渐隐动画。
     */
    private void triggerFadeOutForMissing(Map<ItemKind, CapsuleSnapshot> prev, HBox box) {
        Set<ItemKind> cur = EnumSet.noneOf(ItemKind.class);
        for (Node n : box.getChildren()) {
            if (n instanceof HBox capsule && capsule.getUserData() instanceof ItemKind k) {
                cur.add(k);
            }
        }
        for (Map.Entry<ItemKind, CapsuleSnapshot> e : prev.entrySet()) {
            if (!cur.contains(e.getKey())) {
                spawnFadingCapsule(e.getValue());
            }
        }
    }

    /**
     * 在 fadingLayer 创建一个渐隐胶囊副本：
     *  - 复制上一帧的位置与外观
     *  - alpha 1→0 + scale 1→0.8，共 0.3 秒
     *  - 期间拦截鼠标事件与射线检测，防止重复使用
     *  - 动画结束后自动从 fadingLayer 移除
     */
    private void spawnFadingCapsule(CapsuleSnapshot snap) {
        HBox capsule = makeCapsule(snap.image, snap.text, snap.color, null);
        capsule.setLayoutX(snap.x);
        capsule.setLayoutY(snap.y);
        // 动画期间该副本拦截鼠标事件与射线检测，防止重复使用
        capsule.setMouseTransparent(false);
        capsule.setPickOnBounds(true);

        FadeTransition fade = new FadeTransition(Duration.seconds(FADE_DURATION_SEC), capsule);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);

        ScaleTransition scale = new ScaleTransition(Duration.seconds(FADE_DURATION_SEC), capsule);
        scale.setFromX(1.0); scale.setFromY(1.0);
        scale.setToX(0.8);    scale.setToY(0.8);

        ParallelTransition pt = new ParallelTransition(fade, scale);
        pt.setOnFinished(e -> fadingLayer.getChildren().remove(capsule));
        fadingLayer.getChildren().add(capsule);
        pt.play();
    }

    /** 在 capsule 内查找 ImageView（道具图标） */
    private ImageView findImageView(HBox capsule) {
        for (Node n : capsule.getChildren()) {
            if (n instanceof ImageView iv) return iv;
        }
        return null;
    }

    /** 在 capsule 内查找 Label（数量文字） */
    private Label findLabel(HBox capsule) {
        for (Node n : capsule.getChildren()) {
            if (n instanceof Label l) return l;
        }
        return null;
    }

    /** 从 Label 的 -fx-text-fill 样式中解析颜色十六进制（找不到返回白色） */
    private String parseTextColor(Label lbl) {
        String s = lbl.getStyle();
        int i = s.indexOf("-fx-text-fill:");
        if (i < 0) return "#ffffff";
        int end = s.indexOf(';', i);
        if (end < 0) end = s.length();
        return s.substring(i + "-fx-text-fill:".length(), end).trim();
    }

    /**
     * 构造一个 图标 + ×数量 的小胶囊节点（HBox 包 ImageView + Label），仅当数量≥1 时由调用方加入。
     * @param kind 道具种类（用于渐隐动画系统对比上一帧/本帧差异），可传 null 表示渐隐副本
     */
    private HBox makeCapsule(Image icon, String countText, String color, ItemKind kind) {
        Label count = new Label(countText);
        count.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        count.setMinWidth(20);
        HBox capsule = new HBox(2);
        capsule.setAlignment(Pos.CENTER_LEFT);
        capsule.setUserData(kind);
        if (icon != null && !icon.isError()) {
            ImageView iv = new ImageView(icon);
            iv.setFitWidth(28);
            iv.setFitHeight(28);
            iv.setPreserveRatio(true);
            capsule.getChildren().add(iv);
        }
        capsule.getChildren().add(count);
        return capsule;
    }
}
