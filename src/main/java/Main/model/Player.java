// FR-UI Player：玩家接口（分数读取/加分/炸药库存），来自 feature_ui 分支
package Main.model;

public interface Player {

    /** 当前分数（FR-29：HUD 实时显示） */
    int getScore();

    /** 增加分数（抓到物品结算后调用） */
    void addScore(int points);

    /** 当前炸药库存（炸药键炸毁钩上携带物，库存减 1） */
    int getDynamiteCount();

    /**
     * 使用 1 个炸药（库存 > 0 时扣减并返回 true；库存为 0 返回 false）
     */
    boolean useDynamite();
}
