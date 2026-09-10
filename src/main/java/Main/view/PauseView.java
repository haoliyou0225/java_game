// FR-UI PauseView：暂停遮罩接口（show/hide），来自 feature_ui 分支
package Main.view;

import javafx.scene.Parent;
import javafx.scene.layout.Pane;

/**
 * 暂停遮罩界面接口（暂停功能扩展）
 * <p>
 * 职责：对局暂停时显示「已暂停」提示遮罩，恢复时隐藏；
 * 遮罩仅负责展示，不拦截键盘事件（键盘监听挂在 Scene 上）。
 */
public interface PauseView {

    /** 构建暂停遮罩根节点 */
    Parent build();

    /**
     * 显示暂停遮罩（幂等：已挂载则不重复添加）。
     *
     * @param container 挂载容器（对局画面 gamePane，覆盖在游戏内容之上）
     */
    void show(Pane container);

    /** 隐藏暂停遮罩（从容器中移除自身节点） */
    void hide();
}
