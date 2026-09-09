// FR-16 游戏管理器：每帧调度 gameLoopTick(deltaTime)、输入分发 dispatchAction、胜负判定 judgeOver，组装 Hook/Rope/GameData/Level
package com.dz.hello.main.controller;

import com.dz.hello.main.model.*;
import com.dz.hello.main.config.GameConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * 游戏管理器（严格对齐 UML）
 * 每帧调度 Hook/Item 更新，处理碰撞、计分、胜负判定
 */
public class GameManager {

    private Hook hookP1;
    private Rope ropeP1;
    private Hook hookP2;
    private Rope ropeP2;
    private GameData gameData;
    private Level level;
    private List<Item> sceneItemList;

    public GameManager() {
        this.ropeP1 = new Rope(300);
        this.ropeP2 = new Rope(300);
        this.hookP1 = new Hook(1, ropeP1);
        this.hookP2 = new Hook(2, ropeP2);
        this.gameData = new GameData(GameConfig.GAME_TOTAL_SEC);
        this.level = new Level();
        this.sceneItemList = new ArrayList<>();
    }

    /** 启动游戏 */
    public void startGame() {
        sceneItemList = level.generateSceneItems();
        gameData.setStage(GameStage.PLAYING);
    }

    /** 每帧推进 */
    public void gameLoopTick(double deltaTime) {
        if (gameData.getStage() != GameStage.PLAYING) return;

        // 更新两个钩子
        hookP1.update(deltaTime, sceneItemList, hookP2);
        hookP2.update(deltaTime, sceneItemList, hookP1);

        // 更新已抓取物品位置 + 炸弹触发
        for (Item item : sceneItemList) {
            item.updatePosition();
            if (item.isGrabbed() && item.getWeight() > 0) {
                Item grabb = item;
                Hook owner = itemOnHook(item) == 1 ? hookP1 : hookP2;
                if (owner.getState() == HookState.SWINGING) {
                    grabb.setGrabbed(false);
                    gameData.addScore(grabb.getScore(), itemOnHook(item));
                    if (grabb instanceof Bomb) {
                        ((Bomb) grabb).triggerExplode();
                    }
                }
            }
        }

        // 移除已爆炸的炸弹和已抓取完成的物品
        sceneItemList.removeIf(i -> (i instanceof Bomb && ((Bomb) i).isExploded()));

        // 每秒倒计时
        if (Math.random() < deltaTime) {
            gameData.countDownTick();
        }
    }

    /** 分发输入动作 */
    public void dispatchAction(InputAction action) {
        if (action == null || gameData.getStage() != GameStage.PLAYING) return;
        if (action.getType().equals(InputAction.THROW_P1)) {
            hookP1.throwHook();
        } else if (action.getType().equals(InputAction.THROW_P2)) {
            hookP2.throwHook();
        }
    }

    /** 判定是否结束 */
    public boolean judgeOver() {
        return gameData.getStage() == GameStage.GAME_OVER;
    }

    /** 粗略判断物品在哪个钩子上 */
    private int itemOnHook(Item item) {
        // 简化：看 item 被哪个钩子的绳长范围圈住
        double ix = item.getX(), iy = item.getY();
        double h1x = 0, h1y = 0;
        double h2x = 0, h2y = 0;
        double d1 = Math.hypot(ix - h1x, iy - h1y);
        double d2 = Math.hypot(ix - h2x, iy - h2y);
        if (d1 < hookP1.getRopeLength()) return 1;
        if (d2 < hookP2.getRopeLength()) return 2;
        return 1;
    }

    // ===== Getter =====

    public Hook getHookP1() { return hookP1; }
    public Hook getHookP2() { return hookP2; }
    public GameData getGameData() { return gameData; }
    public Level getLevel() { return level; }
    public List<Item> getSceneItemList() { return sceneItemList; }
}
