// FR-18 持续道具按键使用结果：成功激活 / 已激活转金币 / 库存不足
package Main.model;

/**
 * 持续型道具（幸运草 / 钻石升级 / 石头书）按键使用结果（FR-18）。
 * 由 Player 的 useXxx() 返回，逻辑层（GameManager）据此输出日志/HUD 反馈：
 * <ul>
 *   <li>{@link #ACTIVATED}：库存扣 1 且效果首次激活；</li>
 *   <li>{@link #DUPLICATE_GOLD}：效果已激活，库存扣 1 并折算 50 金币；</li>
 *   <li>{@link #NO_STOCK}：库存为 0，按键无效、不扣库存。</li>
 * </ul>
 */
public enum PersistItemUseResult {
    /** 库存扣 1，效果首次激活 */
    ACTIVATED,
    /** 效果已激活，库存扣 1，折算 {@link Main.config.GameConfig#ITEM_DUP_AUTO_GOLD} 金币 */
    DUPLICATE_GOLD,
    /** 库存为 0，按键无效 */
    NO_STOCK
}
