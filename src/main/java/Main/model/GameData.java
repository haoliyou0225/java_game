// FR-12 游戏数据：双玩家独立计分 scoreP1/P2、倒计时 remainSec、阶段 stage、胜负判定 getWinnerId
package Main.model;

public class GameData {
    private int scoreP1;
    private int scoreP2;
    private int remainSec;
    private GameStage stage;

    public GameData(int totalSec) {
        this.scoreP1 = 0;
        this.scoreP2 = 0;
        this.remainSec = totalSec;
        this.stage = GameStage.READY;
    }

    public void addScore(int score, int playerId) {
        if (playerId == 1) {
            scoreP1 += score;
        } else if (playerId == 2) {
            scoreP2 += score;
        }
    }

    public void countDownTick() {
        if (remainSec > 0) {
            remainSec--;
            if (remainSec <= 0) {
                stage = GameStage.GAME_OVER;
            }
        }
    }

    public int getWinnerId() {
        if (scoreP1 > scoreP2) return 1;
        if (scoreP2 > scoreP1) return 2;
        return 0;
    }

    public GameStage getStage() { return stage; }
    public void setStage(GameStage stage) { this.stage = stage; }
    public int getScoreP1() { return scoreP1; }
    public int getScoreP2() { return scoreP2; }
    public int getRemainSec() { return remainSec; }
}
