// FR-UI PlayerImpl：玩家分数 + 炸药库存实现，来自 feature_ui 分支
package Main.model;

import Main.config.GameConfig;

public class PlayerImpl implements Player {

    /** 当前分数（FR-29） */
    private int score;
    /** 炸药库存（初始 PLAYER_INIT_DYNAMITE_COUNT，上限 PLAYER_MAX_DYNAMITE_COUNT） */
    private int dynamiteCount = GameConfig.PLAYER_INIT_DYNAMITE_COUNT;

    @Override
    public int getScore() {
        return score;
    }

    @Override
    public void addScore(int points) {
        this.score += points;
    }

    @Override
    public int getDynamiteCount() {
        return dynamiteCount;
    }

    @Override
    public boolean useDynamite() {
        if (dynamiteCount <= 0) {
            return false;
        }
        dynamiteCount--;
        return true;
    }
}
