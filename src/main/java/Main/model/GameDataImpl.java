// FR-12 游戏数据实现：双玩家独立计分 scoreP1/P2、倒计时 remainSec、阶段 stage、胜负判定 getWinnerId
package Main.model;

public class GameDataImpl implements GameData {
    private int scoreP1;
    private int scoreP2;
    private int remainSec;
    private GameStage stage;

    public GameDataImpl(int totalSec) {
        this.scoreP1 = 0;
        this.scoreP2 = 0;
        this.remainSec = totalSec;
        this.stage = GameStage.READY;
    }

    @Override
    public void addScore(int playerId, int val) {
        if (playerId == 1) {
            scoreP1 += val;
        } else if (playerId == 2) {
            scoreP2 += val;
        }
    }

    @Override
    public void subScore(int playerId, int val) {
        if (playerId == 1) {
            scoreP1 -= val;
        } else if (playerId == 2) {
            scoreP2 -= val;
        }
    }

    @Override
    public void countDownTick() {
        if (remainSec > 0) {
            remainSec--;
            if (remainSec <= 0) {
                stage = GameStage.GAME_OVER;
            }
        }
    }

    @Override
    public int getWinnerId() {
        if (scoreP1 > scoreP2) return 1;
        if (scoreP2 > scoreP1) return 2;
        return 0;
    }

    @Override public GameStage getStage() { return stage; }
    @Override public void setStage(GameStage stage) { this.stage = stage; }
    @Override public int getScoreP1() { return scoreP1; }
    @Override public int getScoreP2() { return scoreP2; }
    @Override public int getRemainSec() { return remainSec; }
}
