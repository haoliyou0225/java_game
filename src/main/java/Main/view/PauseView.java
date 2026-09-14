// FR-UI PauseView：暂停遮罩接口（show/hide + 回到主菜单/重新开始回调）
package Main.view;

import javafx.scene.Parent;
import javafx.scene.layout.Pane;

/**
 * 暂停遮罩界面接口（暂停功能扩展）
 * <p>
 * 职责：对局暂停时显示「已暂停」提示遮罩 + 「继续 / 重新开始 / 回到主菜单」按钮，
 * 恢复时隐藏；遮罩仅负责展示与按钮回调，不拦截键盘事件（键盘监听挂在 Scene 上）。
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

    /**
     * 注册「重新开始」按钮回调：完整释放当前对局资源后开启全新对局（FR-25）。
     *
     * @param action 回调；传 null 可清除
     */
    void setOnRestart(Runnable action);

    /**
     * 注册「回到主菜单」按钮回调：完整释放当前对局资源后显示主菜单。
     *
     * @param action 回调；传 null 可清除
     */
    void setOnBackToMenu(Runnable action);
}
