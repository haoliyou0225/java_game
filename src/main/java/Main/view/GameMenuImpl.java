// FR-19 菜单界面实现：drawStartMenu() 开始菜单、drawResultPanel(winnerId) 结算弹窗
package Main.view;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * 菜单界面实现（严格对齐 UML）
 */
public class GameMenuImpl implements GameMenu {

    private Canvas canvas;

    public GameMenuImpl(Canvas canvas) {
        this.canvas = canvas;
    }

    /** 绘制开始菜单 */
    @Override
    public void drawStartMenu() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setFill(Color.DARKSLATEBLUE);
        g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        g.setFill(Color.WHITE);
        g.setFont(Font.font(32));
        g.fillText("双人钩子矿工", 260, 250);
        g.setFont(Font.font(18));
        g.fillText("P1: 空格  |  P2: 回车", 290, 300);
        g.fillText("按空格或回车开始游戏", 300, 350);
    }

    /** 绘制结果面板 */
    @Override
    public void drawResultPanel(int winnerId) {
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setFill(Color.rgb(0, 0, 0, 0.75));
        g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        g.setFill(Color.YELLOW);
        g.setFont(Font.font(36));
        String text = winnerId == 0 ? "平局!" : "玩家 " + winnerId + " 获胜!";
        g.fillText(text, 320, 300);
    }
}
