// 钓获反馈 HUD：屏幕中线左右两侧显示本次钓到的物品（图标+名称+加分），淡入上浮缩放+停留+淡出
package Main.view;

import Main.config.Config;
import Main.config.GameConfig;
import Main.model.BigGold;
import Main.model.CatchFeedbackEvent;
import Main.model.Diamond;
import Main.model.DiamondPig;
import Main.model.Gold;
import Main.model.Item;
import Main.model.MediumGold;
import Main.model.Mole;
import Main.model.MysteryBag;
import Main.model.Stone;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 本次钓获反馈 HUD。
 * <p>
 * 触发条件：物品被成功拉回并完成结算后由 GameManager 回调 showFeedback。
 * 空钩、未拉回、中途失败、未结算时不显示。
 * <p>
 * 显示区域（放置在 HUD 面板内，紧贴各侧矿工旁，不与人物重叠）：
 * <ul>
 *   <li>P1 提示左边 365（矿工贴图右边 371 + 9px 间距），反馈 365~535；</li>
 *   <li>P2 提示右边 940（矿工贴图左边 931 + 9px 间距），反馈 770~940；</li>
 *   <li>提示顶部 Y = 13，整体落在 HUD 高度（0~90）内（高度 64，底边 77 &lt; 90）；</li>
 *   <li>无背景阴影：仅图标(44px) + 加分文字(24px) 直接显示。</li>
 * </ul>
 * 动画：淡入+上浮+轻微缩放(0.2s) → 停留(1.5s) → 淡出+上浮(0.3s) → 回收。
 * 不阻塞玩家操作；连续钓获按槽位堆叠；旧提示按自己的生命周期结束，不卡住新提示。
 */
public class CatchFeedbackPane {

    // ===== 布局常量 =====
    private static final double POPUP_WIDTH = 170;
    private static final double POPUP_HEIGHT = 64;
    /** P1 提示中心 X：紧贴 P1 矿工右边（贴图右边 371 + 9px 间距 + 半宽 85），反馈 365~535 */
    private static final double P1_CENTER_X = 450;
    /** P2 提示中心 X：右移靠近 P2 矿工（贴图左边 931 - 9px 间距 - 半宽 85），反馈 745~915 */
    private static final double P2_CENTER_X = 855;
    /** 提示顶部 Y：落在 HUD 高度（0~90）内，不遮挡分数与矿工 */
    private static final double BASE_Y = 13;
    /** 同一玩家多次钓获的垂直堆叠间距 */
    private static final double STACK_GAP = 3;

    // ===== 动画时长（毫秒），停留时长可配置 =====
    private static final double FADE_IN_MS = 200;
    private static final double STAY_MS = 1500;
    private static final double FADE_OUT_MS = 300;
    /** 淡入/淡出上浮像素 */
    private static final double FLOAT_PX = 10;
    /** 入场缩放起始倍率（轻微缩放） */
    private static final double SCALE_FROM = 0.85;

    private final Pane root;
    /** 每位玩家当前占用的槽位集合（从 0 起的最小未占用槽位），实现堆叠显示 */
    private final Map<Integer, Set<Integer>> usedSlots = new HashMap<>();
    /** 图标缓存（资源路径 → Image），避免反复加载 */
    private final Map<String, Image> iconCache = new HashMap<>();

    public CatchFeedbackPane() {
        root = new Pane();
        root.setPickOnBounds(false);
        root.setMouseTransparent(true);
        root.setPrefSize(Config.WIDTH, Config.HEIGHT);
    }

    /** 构建根节点，挂载到对局场景（叠加在 Canvas 与 HUD 之上） */
    public Parent build() {
        return root;
    }

    /**
     * 显示一次钓获反馈。仅在结算完成后调用。
     * 自动按玩家分配槽位，同一玩家连续钓获垂直堆叠，互不遮挡。
     */
    public void showFeedback(CatchFeedbackEvent event) {
        int pid = event.playerId();
        int slot = acquireSlot(pid);
        double centerX = (pid == 1) ? P1_CENTER_X : P2_CENTER_X;
        double y = BASE_Y + slot * (POPUP_HEIGHT + STACK_GAP);

        Node popup = createPopupNode(event);
        popup.setLayoutX(centerX - POPUP_WIDTH / 2);
        popup.setLayoutY(y);

        root.getChildren().add(popup);
        playLifecycle(popup, pid, slot);
    }

    /**
     * 分配一个可用槽位（从 0 起的最小未占用槽位），实现堆叠显示。
     * 动画结束时由 releaseSlot 释放，后续新提示可复用该槽位。
     */
    private int acquireSlot(int pid) {
        Set<Integer> used = usedSlots.computeIfAbsent(pid, k -> new HashSet<>());
        int slot = 0;
        while (used.contains(slot)) slot++;
        used.add(slot);
        return slot;
    }

    /** 释放槽位（动画结束时调用） */
    private void releaseSlot(int pid, int slot) {
        Set<Integer> used = usedSlots.get(pid);
        if (used != null) used.remove(slot);
    }

    // ===== 提示节点构建 =====

    /**
     * 构建单个提示节点：图标 + 加分（紧凑布局，无背景阴影）。
     */
    private Node createPopupNode(CatchFeedbackEvent event) {
        String iconPath = resolveIconPath(event);
        int score = event.scoreGained();

        // 图标（classpath 加载，缓存复用）
        ImageView icon = new ImageView();
        icon.setFitWidth(44);
        icon.setFitHeight(44);
        icon.setPreserveRatio(true);
        if (iconPath != null) {
            Image img = iconCache.computeIfAbsent(iconPath, this::loadIcon);
            if (img != null && !img.isError()) {
                icon.setImage(img);
            }
        }

        // 加分/道具名：福袋显示开出的道具名，普通物品显示 +分数
        Label scoreLabel;
        if (event.reward() != null) {
            // 福袋：显示开出的道具名（如"炸药"），不显示 +0
            scoreLabel = new Label(event.reward().cnName());
            scoreLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #ffd27f;");
        } else {
            // 普通物品：显示 +分数
            scoreLabel = new Label((score >= 0 ? "+" : "") + score);
            scoreLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #7fff7f;");
        }

        // 内容行：图标 + 文字紧挨，间距仅一点点空隙，无背景
        HBox content = new HBox(4);
        content.setAlignment(Pos.CENTER);
        content.setPrefSize(POPUP_WIDTH, POPUP_HEIGHT);
        content.setPadding(new Insets(0, 8, 0, 8));
        if (icon.getImage() != null) {
            content.getChildren().add(icon);
        }
        content.getChildren().add(scoreLabel);

        return content;
    }

    private Image loadIcon(String path) {
        try {
            return new Image(getClass().getResourceAsStream(path));
        } catch (Exception e) {
            return null;
        }
    }

    // ===== 显示信息解析（物品类 → 图标） =====

    private static String resolveIconPath(CatchFeedbackEvent event) {
        Item item = event.item();
        if (item instanceof MysteryBag && event.reward() != null) {
            return iconForReward(event.reward());
        }
        return iconForItem(item);
    }

    private static String iconForItem(Item item) {
        if (item instanceof BigGold || item instanceof MediumGold || item instanceof Gold) {
            return "/images/HUD/金块.png";
        }
        if (item instanceof Diamond || item instanceof DiamondPig) {
            return "/images/HUD/钻石.png";
        }
        if (item instanceof Stone) {
            return "/images/HUD/石头.png";
        }
        return null; // 鼹鼠等无专属 HUD 图标，仅显示文字
    }

    private static String iconForReward(GameConfig.MysteryReward reward) {
        return switch (reward) {
            case MYSTERY_GOLD -> "/images/HUD/金块.png";
            case DYNAMITE -> "/images/HUD/炸药.png";
            case POWER_POTION -> "/images/HUD/强力药水.png";
            case FREEZE_BOX -> "/images/HUD/冰冻箱.png";
            case LUCKY_CLOVER -> "/images/HUD/幸运草.png";
            case DIAMOND_BOOST -> "/images/HUD/钻石升级.png";
            case STONE_BOOK -> "/images/HUD/石头收藏书.png";
        };
    }

    // ===== 生命周期动画 =====

    /**
     * 播放完整生命周期：淡入+上浮+缩放 → 停留 → 淡出+上浮 → 回收。
     * 缩放时用 translateX 补偿偏移，使提示中心位置在缩放过程中保持不变。
     */
    private void playLifecycle(Node popup, int pid, int slot) {
        double compX = (1.0 - SCALE_FROM) * POPUP_WIDTH / 2;

        // 初始状态
        popup.setOpacity(0);
        popup.setTranslateX(compX);
        popup.setTranslateY(FLOAT_PX);
        popup.setScaleX(SCALE_FROM);
        popup.setScaleY(SCALE_FROM);

        // 进入：淡入 + 上浮 + 轻微缩放（X 平移补偿使中心不变）
        FadeTransition fadeIn = new FadeTransition(Duration.millis(FADE_IN_MS), popup);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        TranslateTransition moveIn = new TranslateTransition(Duration.millis(FADE_IN_MS), popup);
        moveIn.setFromX(compX);
        moveIn.setFromY(FLOAT_PX);
        moveIn.setToX(0);
        moveIn.setToY(0);

        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(FADE_IN_MS), popup);
        scaleIn.setFromX(SCALE_FROM);
        scaleIn.setFromY(SCALE_FROM);
        scaleIn.setToX(1);
        scaleIn.setToY(1);

        ParallelTransition enter = new ParallelTransition(fadeIn, moveIn, scaleIn);

        // 退出：淡出 + 上浮
        FadeTransition fadeOut = new FadeTransition(Duration.millis(FADE_OUT_MS), popup);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);

        TranslateTransition moveOut = new TranslateTransition(Duration.millis(FADE_OUT_MS), popup);
        moveOut.setFromY(0);
        moveOut.setToY(-FLOAT_PX);

        ParallelTransition exit = new ParallelTransition(fadeOut, moveOut);

        // 串行：进入 → 停留 → 退出 → 回收
        SequentialTransition seq = new SequentialTransition(
                enter,
                new PauseTransition(Duration.millis(STAY_MS)),
                exit
        );
        seq.setOnFinished(e -> {
            root.getChildren().remove(popup);
            releaseSlot(pid, slot);
        });
        seq.play();
    }
}
