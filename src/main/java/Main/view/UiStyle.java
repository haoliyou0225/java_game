// FR-UI UiStyle：视图公共工具（金色按钮样式/幂等挂载卸载），来自 feature_ui 分支
package Main.view;

import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;

/**
 * 视图层公共 UI 工具：统一金色按钮样式、操作提示样式与幂等挂载/卸载逻辑。
 * 供 MenuViewImpl / ResultViewImpl / PauseViewImpl 复用，避免样式代码多处复制粘贴。
 */
public final class UiStyle {

    /** 按钮默认样式 */
    private static final String BUTTON_STYLE = "-fx-font-size: 24px; -fx-font-weight: bold; "
            + "-fx-background-color: #c98a2d; -fx-text-fill: #ffffff; "
            + "-fx-background-radius: 12; -fx-cursor: hand;";

    /** 按钮悬停样式（颜色加深） */
    private static final String BUTTON_HOVER_STYLE = "-fx-font-size: 24px; -fx-font-weight: bold; "
            + "-fx-background-color: #e09f3a; -fx-text-fill: #ffffff; "
            + "-fx-background-radius: 12; -fx-cursor: hand;";

    /** 键帽徽章样式：深色底 + 金色描边，模拟物理键盘键帽 */
    private static final String KEY_CAP_STYLE = "-fx-font-size: 14px; -fx-font-weight: bold; "
            + "-fx-font-family: \"Consolas\", monospace; -fx-text-fill: #ffd970; "
            + "-fx-background-color: #1f1208; -fx-background-radius: 8; "
            + "-fx-border-color: #c98a2d; -fx-border-radius: 8; -fx-border-width: 2; "
            + "-fx-padding: 2 0 2 0; -fx-min-width: 62; -fx-pref-width: 62; "
            + "-fx-alignment: center; -fx-cursor: default";

    /** 按键功能说明文字样式 */
    private static final String HINT_LABEL_STYLE = "-fx-font-size: 15px; -fx-text-fill: #e8d5b0; "
            + "-fx-cursor: default";

    /** 操作提示分区标题样式（玩家1 / 玩家2 列标题） */
    private static final String COLUMN_TITLE_STYLE = "-fx-font-size: 18px; -fx-font-weight: bold; "
            + "-fx-text-fill: #ffe259; -fx-cursor: default;";

    private UiStyle() {
        // 工具类禁止实例化
    }

    /**
     * 创建统一风格的金色菜单按钮（含悬停加深效果）。
     *
     * @param text 按钮文本（如 "开始对战"）
     * @return 配置好尺寸与样式的按钮
     */
    public static Button createGoldButton(String text) {
        Button button = new Button(text);
        button.setPrefSize(260, 64);
        button.setStyle(BUTTON_STYLE);
        // 悬停效果：颜色加深
        button.setOnMouseEntered(e -> button.setStyle(BUTTON_HOVER_STYLE));
        button.setOnMouseExited(e -> button.setStyle(BUTTON_STYLE));
        return button;
    }

    /**
     * 创建统一风格的键帽徽章（深色底 + 金色描边，等宽字体居中）。
     * 用于主菜单操作提示中展示物理按键，如 "S"、"↓"、"Num1"。
     *
     * @param key 按键显示文本
     * @return 配置好固定宽度与键帽样式的标签
     */
    public static Label createKeyCap(String key) {
        Label cap = new Label(key);
        cap.setStyle(KEY_CAP_STYLE);
        return cap;
    }

    /**
     * 创建按键功能说明标签（浅色文字），与键帽徽章配合使用，如 "释放钩爪"。
     *
     * @param text 功能说明文本
     * @return 配置好样式的标签
     */
    public static Label createHintLabel(String text) {
        Label label = new Label(text);
        label.setStyle(HINT_LABEL_STYLE);
        return label;
    }

    /**
     * 创建操作提示分区标题标签（金色加粗），如 "玩家1（P1）"。
     *
     * @param text 分区标题文本
     * @return 配置好样式的标签
     */
    public static Label createColumnTitle(String text) {
        Label label = new Label(text);
        label.setStyle(COLUMN_TITLE_STYLE);
        return label;
    }

    /**
     * 幂等挂载：节点已存在于容器则跳过，防止重复添加。
     *
     * @param container 目标容器
     * @param node      待挂载节点
     */
    public static void mountOnce(Pane container, Parent node) {
        if (!container.getChildren().contains(node)) {
            container.getChildren().add(node);
        }
    }

    /**
     * 从父容器中移除节点（父节点必须为 Pane，否则不处理）。
     *
     * @param node 待移除节点
     */
    public static void unmount(Parent node) {
        Parent parent = node.getParent();
        if (parent instanceof Pane pane) {
            pane.getChildren().remove(node);
        }
    }
}
