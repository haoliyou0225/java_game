// FR-18 画布渲染接口：renderAll(GameData,Hook,Hook,List<Item>) 绘制背景/双钩/绳索/物品，drawHUD 分数与倒计时
package Main.view;

import Main.model.GameData;
import Main.model.Hook;
import Main.model.Item;
import javafx.scene.canvas.GraphicsContext;

import java.util.List;

public interface GameUI {
    void renderAll(GameData gd, Hook h1, Hook h2, List<Item> items);
    void drawHUD(GraphicsContext g, GameData gd);
}
