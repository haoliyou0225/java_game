// FR-UI PlayerImpl：玩家分数实现，来自 feature_ui 分支
package Main.model;

public class PlayerImpl implements Player {

    /** 当前分数（FR-29） */
    private int score;

    @Override
    public int getScore() {
        return score;
    }

    @Override
    public void addScore(int points) {
        this.score += points;
    }
}
