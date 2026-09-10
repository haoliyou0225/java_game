// FR-16 游戏管理器实现：每帧调度 gameLoopTick(deltaTime)、输入分发 dispatchAction、胜负判定 judgeGameOver，组装 Hook/Rope/GameData/Level
package Main.controller;

import Main.config.GameConfig;
import Main.model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 游戏管理器实现（严格对齐 UML）
 * 每帧调度 Hook/Item 更新，处理碰撞、计分、胜负判定
 */
public class GameManagerImpl implements GameManager {

    private Hook hookP1;
    private Rope ropeP1;
    private Hook hookP2;
    private Rope ropeP2;
    private GameData gameData;
    private Level level;
    private List<Item> sceneItemList;

    public GameManagerImpl() {
        this.ropeP1 = new RopeImpl(GameConfig.ROPE_MAX_EXTEND_LENGTH);
        this.ropeP2 = new RopeImpl(GameConfig.ROPE_MAX_EXTEND_LENGTH);
        this.hookP1 = new HookImpl(1, ropeP1);
        this.hookP2 = new HookImpl(2, ropeP2);
        this.gameData = new GameDataImpl(GameConfig.GAME_TOTAL_SEC);
        this.level = new LevelImpl();
        this.sceneItemList = new ArrayList<>();
    }

    /** 启动游戏 */
    @Override
    public void startGame() {
        sceneItemList = level.generateSceneItems();
        gameData.setStage(GameStage.PLAYING);
    }

    /** 每帧推进 */
    @Override
    public void gameLoopTick(double deltaTime) {
        if (gameData.getStage() != GameStage.PLAYING) return;

        // 更新两个钩子
        hookP1.update(deltaTime, sceneItemList, hookP2);
        hookP2.update(deltaTime, sceneItemList, hookP1);

        // 更新已抓取物品位置 + 收回完成结算
        List<Item> settled = new ArrayList<>();
        for (Item item : sceneItemList) {
            item.updatePosition();
            if (item.isGrabbed() && item.getWeight() > 0) {
                Hook owner = itemOnHook(item) == 1 ? hookP1 : hookP2;
                if (owner.getState() == HookState.SWINGING) {
                    item.setGrabbed(false);
                    gameData.addScore(owner.getPlayerId(), item.getScore());
                    if (item instanceof Bomb) {
                        ((Bomb) item).triggerExplode();
                    }
                    settled.add(item);
                }
            }
        }
        sceneItemList.removeAll(settled);

        // 移除已爆炸的炸弹
        sceneItemList.removeIf(i -> (i instanceof Bomb && ((Bomb) i).isExploded()));

        // 每秒倒计时
        if (Math.random() < deltaTime) {
            gameData.countDownTick();
        }
    }

    /** 分发输入动作 */
    @Override
    public void dispatchAction(InputAction action) {
        if (action == null || gameData.getStage() != GameStage.PLAYING) return;
        if (action.getType().equals(InputAction.THROW_P1)) {
            hookP1.throwHook();
        } else if (action.getType().equals(InputAction.THROW_P2)) {
            hookP2.throwHook();
        }
    }

    /** 判定是否结束 */
    @Override
    public boolean judgeGameOver() {
        return gameData.getStage() == GameStage.GAME_OVER;
    }

    /** 判断物品挂在哪个钩子上 */
    private int itemOnHook(Item item) {
        if (hookP1.ownsItem(item)) return 1;
        if (hookP2.ownsItem(item)) return 2;
        return 1;
    }

    // ===== Getter =====

    @Override public Hook getHookP1() { return hookP1; }
    @Override public Hook getHookP2() { return hookP2; }
    @Override public GameData getGameData() { return gameData; }
    @Override public Level getLevel() { return level; }
    @Override public List<Item> getSceneItemList() { return sceneItemList; }
}
