// FR-12 游戏数据接口：双玩家独立计分 scoreP1/P2、倒计时 remainSec、阶段 stage、胜负判定 getWinnerId
package Main.model;

public interface GameData {
    void addScore(int playerId, int val);
    void subScore(int playerId, int val);
    void countDownTick();
    void setStage(GameStage stage);
    GameStage getStage();
    int getWinnerId();
    int getScoreP1();
    int getScoreP2();
    int getRemainSec();
}
