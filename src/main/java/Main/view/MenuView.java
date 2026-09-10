// FR-UI MenuView：主菜单接口（开始对战/退出按钮回调），来自 feature_ui 分支
package Main.view;

import javafx.scene.Parent;
import javafx.scene.layout.Pane;

/**
 * 主菜单视图接口（FR-32）
 * 职责：构建并展示主菜单 UI（标题 GoldMainer + 开始对战 + 退出游戏）。
 * 按钮点击后的业务行为（开局流程、关闭窗口）不在本层实现，
 * 而是通过回调交由上层（程序入口）绑定，保持视图与逻辑解耦。
 */
public interface MenuView {

    /** 构建主菜单 UI 节点（标题、开始对战按钮、退出游戏按钮） */
    Parent build();

    /** 将主菜单挂载到指定容器并显示 */
    void show(Pane container);

    /** 关闭主菜单（从容器中移除自身节点） */
    void hide();

    /** 注册"开始对战"按钮回调（由上层执行开局流程） */
    void setOnStartGame(Runnable action);

    /** 注册"退出游戏"按钮回调（由上层关闭游戏窗口） */
    void setOnExit(Runnable action);
}
