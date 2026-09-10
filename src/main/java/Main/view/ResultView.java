// FR-UI ResultView：胜负结算接口（show 展示结果/重新开始回调），来自 feature_ui 分支
package Main.view;

import Main.model.GameModel;
import javafx.scene.Parent;
import javafx.scene.layout.Pane;

/**
 * 胜负结算界面接口（FR-31）
 * <p>
 * 职责：对局结束后读取双方最终分数，完成胜负判定并展示结果；
 * 提供"重新开始"回调，由上层（Main）注入新一轮开局流程。
 */
public interface ResultView {

    /** 构建结算界面根节点 */
    Parent build();

    /**
     * 显示结算界面：从模型中读取双方最终分数，判定胜负/平局并渲染，
     * 同时将界面节点挂载到指定容器。
     *
     * @param container 挂载容器（全局根容器，覆盖在对局画面上方）
     * @param model     对局模型（读取 getPlayer1()/getPlayer2() 的最终分数）
     */
    void show(Pane container, GameModel model);

    /** 关闭结算界面（从容器中移除自身节点） */
    void hide();

    /**
     * 注入"重新开始"回调：点击"重新开始"按钮时触发，
     * 由上层负责关闭结算界面并执行新一轮开局流程。
     *
     * @param action 重新开始流程（不允许为 null）
     */
    void setOnRestart(Runnable action);
}
