// FR-18 画布渲染：renderAll(GameData,Hook,Hook,List<Item>) 绘制背景/双钩/绳索/物品，drawHUD 分数与倒计时
package com.dz.hello.main.view;

import com.dz.hello.main.model.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.List;

/**
 * 画布渲染（严格对齐 UML）
 */
public class GameUI {

    private Canvas canvas;

    public GameUI(Canvas canvas) {
        this.canvas = canvas;
    }

    /** 渲染全部游戏内容 */
    public void renderAll(GameData gd, Hook h1, Hook h2, List<Item> items) {
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setFill(Color.LIGHTBLUE);
        g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // 画钩子1
        drawHook(g, h1, Color.RED);
        // 画钩子2
        drawHook(g, h2, Color.BLUE);
        // 画物品
        for (Item item : items) {
            if (item.isGrabbed()) continue;
            double r = 15;
            if (item instanceof Diamond) { g.setFill(Color.CYAN); r = 12; }
            else if (item instanceof Bomb) { g.setFill(Color.BLACK); r = 15; }
            else if (item instanceof Stone) { g.setFill(Color.GRAY); r = 18; }
            else { g.setFill(Color.ORANGE); r = 15; }
            g.fillOval(item.getX() - r, item.getY() - r, r * 2, r * 2);
        }
        drawHUD(g, gd);
    }

    /** 绘制单个钩子与绳索 */
    private void drawHook(GraphicsContext g, Hook hook, Color color) {
        int anchorX = 400, anchorY = 0;
        double tipX = anchorX + Math.cos(hook.getAngle()) * hook.getRopeLength();
        double tipY = anchorY + Math.sin(hook.getAngle()) * hook.getRopeLength();
        g.setStroke(color);
        g.setLineWidth(3);
        g.beginPath();
        g.moveTo(anchorX, anchorY);
        g.lineTo(tipX, tipY);
        g.stroke();
        g.setFill(color);
        g.fillOval(tipX - 6, tipY - 6, 12, 12);
    }

    /** 绘制 HUD（分数/倒计时） */
    public void drawHUD(GraphicsContext g, GameData gd) {
        g.setFill(Color.WHITE);
        g.setFont(Font.font(16));
        g.fillText("P1: " + gd.getScoreP1(), 20, 25);
        g.fillText("P2: " + gd.getScoreP2(), 120, 25);
        g.fillText("倒计时: " + gd.getRemainSec(), 240, 25);
    }
}
