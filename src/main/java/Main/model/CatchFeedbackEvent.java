// 钓获反馈事件：结算完成后由 GameManager 构造，传递给视图层显示（JavaFX 无关）
package Main.model;

import Main.config.GameConfig;

/**
 * 本次钓获反馈事件（GameManager → View 单向数据载体）。
 * <p>
 * 仅在物品被成功拉回并完成结算后构造；空钩、未拉回、中途失败、未结算时不构造。
 * 视图层据此渲染：图标 + 名称 + 数量/效果 + 加分。
 *
 * @param playerId     抓取玩家编号（1=P1，2=P2）
 * @param item         本次结算的物品（福袋时为福袋本身，开出的道具见 reward）
 * @param scoreGained  本次结算最终得分（已套算石头×3 / 钻石×2 / 幸运草×1.5 等）
 * @param reward       福袋额外奖励类型；非福袋为 null
 * @param extraGold    福袋额外奖励附带金币：0=道具入库成功，50=库存满折算，100~800=金币档
 */
public record CatchFeedbackEvent(
        int playerId,
        Item item,
        int scoreGained,
        GameConfig.MysteryReward reward,
        int extraGold
) {}
