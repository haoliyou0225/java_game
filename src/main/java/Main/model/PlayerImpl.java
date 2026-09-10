// FR-UI PlayerImpl：玩家分数 + 炸药库存 + 道具效果实现，来自 feature_ui 分支
package Main.model;

import Main.config.GameConfig;

public class PlayerImpl implements Player {

    /** 当前分数（FR-29） */
    private int score;

    /** 炸药库存数量（初始 1，上限 3，由 GameConfig 控制） */
    private int bombCount = GameConfig.PLAYER_INIT_DYNAMITE_COUNT;

    // ===== 道具效果标志 =====
    /** 幸运草：本局所有物品收益 +50% */
    private boolean luckyClover;
    /** 钻石升级药水：本局钻石价值翻倍 */
    private boolean diamondBoost;
    /** 石头收藏书：本局石头消除惩罚（仅 +1 金币） */
    private boolean stoneBook;

    @Override
    public int getScore() {
        return score;
    }

    @Override
    public void addScore(int points) {
        this.score += points;
    }

    @Override
    public int getBombCount() {
        return bombCount;
    }

    @Override
    public boolean useBomb() {
        if (bombCount > 0) {
            bombCount--;
            return true;
        }
        return false;
    }

    @Override
    public int addBomb(int count) {
        if (count <= 0) return 0;
        int before = bombCount;
        bombCount = Math.min(bombCount + count, GameConfig.PLAYER_MAX_DYNAMITE_COUNT);
        int added = bombCount - before;
        int overflow = count - added;
        // 溢出部分自动转为金币
        if (overflow > 0) {
            this.score += overflow * GameConfig.BOMB_FULL_AUTO_GOLD;
        }
        return added;
    }

    // ===== 道具效果 =====
    @Override public boolean hasLuckyClover() { return luckyClover; }
    @Override public void grantLuckyClover() { this.luckyClover = true; }
    @Override public boolean hasDiamondBoost() { return diamondBoost; }
    @Override public void grantDiamondBoost() { this.diamondBoost = true; }
    @Override public boolean hasStoneBook() { return stoneBook; }
    @Override public void grantStoneBook() { this.stoneBook = true; }
}
