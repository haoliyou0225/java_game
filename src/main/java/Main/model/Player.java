// FR-UI Player：玩家接口（分数读取/加分），来自 feature_ui 分支
package Main.model;

public interface Player {

    /** 当前分数（FR-29：HUD 实时显示） */
    int getScore();

    /** 增加分数（抓到物品结算后调用） */
    void addScore(int points);
}
