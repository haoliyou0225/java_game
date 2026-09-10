// FR-UI PauseViewImpl：暂停遮罩实现（「已暂停」+ ESC 继续提示），来自 feature_ui 分支
package Main.view;

import Main.config.Config;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

/**
 * 暂停遮罩界面实现（暂停功能扩展）
 * <p>
 * 布局：全屏半透明深色遮罩 + 居中提示卡片：
 * 「已暂停」大字标题 + "按 ESC 继续对局"操作提示。
 * 遮罩挂在 gamePane 顶层，覆盖游戏画面与 HUD，明确告知玩家当前处于暂停状态。
 */
public class PauseViewImpl implements PauseView {

    /** 暂停遮罩根节点（全屏遮罩） */
    private final VBox pauseBox;

    public PauseViewImpl() {
        // 「已暂停」标题
        Label titleLabel = new Label("已暂停");
        titleLabel.setStyle("-fx-font-size: 72px; -fx-font-weight: bold; "
                + "-fx-text-fill: #ffe259;");

        // 操作提示：告知玩家如何恢复
        Label hintLabel = new Label("按 ESC 继续对局");
        hintLabel.setStyle("-fx-font-size: 28px; -fx-text-fill: #e8c87a;");

        pauseBox = new VBox(20, titleLabel, hintLabel);
        pauseBox.setAlignment(Pos.CENTER);
        pauseBox.setPrefSize(Config.WIDTH, Config.HEIGHT);
        // 半透明遮罩：隐约可见被冻结的游戏画面，突出暂停状态
        pauseBox.setStyle("-fx-background-color: rgba(20, 12, 4, 0.65);");
    }

    @Override
    public Parent build() {
        return pauseBox;
    }

    @Override
    public void show(Pane container) {
        // 幂等挂载：防止重复添加
        UiStyle.mountOnce(container, pauseBox);
    }

    @Override
    public void hide() {
        UiStyle.unmount(pauseBox);
    }
}
