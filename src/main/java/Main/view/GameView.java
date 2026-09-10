// FR-UI GameView：游戏画面渲染接口（render 一帧），来自 feature_ui 分支
package Main.view;

import Main.model.GameModel;

public interface GameView {
    void render(GameModel model);
}
