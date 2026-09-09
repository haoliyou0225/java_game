// FR-12 游戏数据：双玩家独立计分 scoreP1/P2、倒计时 remainSec、阶段 stage、胜负判定 getWinnerId
package com.dz.hello.main.model;

public class GameData {
    private int scoreP1;
    private int scoreP2;
    private int remainSec;
    private GameStage stage;
    public GameData(int totalSec) {}
    public void addScore(int score, int playerId) {}
    public void countDownTick() {}
    public int getWinnerId() { return 0; }
    public GameStage getStage() { return null; }
    public void setStage(GameStage stage) {}
    public int getScoreP1() { return 0; }
    public int getScoreP2() { return 0; }
    public int getRemainSec() { return 0; }
}
