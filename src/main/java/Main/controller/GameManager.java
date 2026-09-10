// FR-16 游戏管理器接口：每帧调度 gameLoopTick(deltaTime)、输入分发 dispatchAction、胜负判定 judgeGameOver，组装 Hook/Rope/GameData/Level
package Main.controller;

import Main.model.GameData;
import Main.model.Hook;
import Main.model.Item;
import Main.model.Level;

import java.util.List;

public interface GameManager {
    void startGame();
    void gameLoopTick(double deltaTime);
    void dispatchAction(InputAction action);
    boolean judgeGameOver();
    Hook getHookP1();
    Hook getHookP2();
    GameData getGameData();
    Level getLevel();
    List<Item> getSceneItemList();
}
