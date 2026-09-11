// FR-UI PauseViewImpl：暂停遮罩实现（「已暂停」+ ESC 继续提示 + 复用 ControlGuidePane 操作指南，FR-26）
package Main.view;

import Main.config.Config;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

/**
 * 暂停遮罩界面实现（暂停功能扩展 + FR-26 操作提示复用）。
 * <p>
 * 布局：全屏半透明深色遮罩 + 居中内容：
 * 「已暂停」大字标题 + “按 ESC 继续对局”提示 + {@link ControlGuidePane} 操作指南
 * （与主菜单“新手指南”窗口共用同一组件，键位/规则只需维护一处）。
 * <p>
 * 指南区域包一层固定视口高度的 {@link ScrollPane}，保证任何字体缩放下
 * 暂停遮罩总高度都不超过 {@link Config#HEIGHT}，内容超高时在卡片内部滚动，不裁切。
 */
public class PauseViewImpl implements PauseView {

    /** 暂停遮罩根节点（全屏遮罩） */
    private final VBox pauseBox;

    public PauseViewImpl() {
        // 「已暂停」标题
        Label titleLabel = new Label("已暂停");
        titleLabel.setStyle("-fx-font-size: 56px; -fx-font-weight: bold; "
                + "-fx-text-fill: #ffe259;");

        // 操作提示：告知玩家如何恢复
        Label hintLabel = new Label("按 ESC 继续对局");
        hintLabel.setStyle("-fx-font-size: 22px; -fx-text-fill: #e8c87a;");

        // FR-26：复用与“新手指南”窗口相同的操作指南组件（键位表 + 游戏规则）
        ControlGuidePane guidePane = new ControlGuidePane();
        ScrollPane guideScroll = new ScrollPane(guidePane);
        guideScroll.setFitToWidth(true);
        guideScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        guideScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        // 视口尺寸收敛：左右各留 80 遮罩边距，高度固定 430，保证暂停卡片在 720 高度内完整呈现
        guideScroll.setPrefSize(Config.WIDTH - 160, 430);
        guideScroll.setMaxSize(Config.WIDTH - 160, 430);
        // 透明背景：露出遮罩底色，指南卡片自身带半透明深色底与金色描边
        guideScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        pauseBox = new VBox(14, titleLabel, hintLabel, guideScroll);
        pauseBox.setAlignment(Pos.CENTER);
        pauseBox.setPrefSize(Config.WIDTH, Config.HEIGHT);
        // 半透明遮罩：比原方案略加深（0.78），保证指南文字在游戏画面之上清晰可读
        pauseBox.setStyle("-fx-background-color: rgba(20, 12, 4, 0.78);");
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
