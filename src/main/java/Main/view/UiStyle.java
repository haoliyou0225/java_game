// FR-UI UiStyle：视图公共工具（金色按钮样式/幂等挂载卸载），来自 feature_ui 分支
package Main.view;

import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;

/**
 * 视图层公共 UI 工具：统一金色按钮样式与幂等挂载/卸载逻辑。
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
