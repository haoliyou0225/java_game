// FR-UI HUDView：HUD 接口（build 节点 / render 刷新分数与倒计时），来自 feature_ui 分支
package Main.view;

import Main.model.GameModel;
import javafx.scene.Parent;

public interface HUDView {

    /** 构建 HUD 根节点（挂载到对局场景） */
    Parent build();

    /** 根据模型刷新显示（倒计时数字、双方分数） */
    void render(GameModel model);
}
