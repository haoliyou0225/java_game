// 逻辑层动作处理接口：Controller 只派发语义动作，物理/库存/效果全部由本接口实现处理
package Main.controller;

/**
 * 对局语义动作处理器（逻辑层入口，FR-18）。
 * <p>
 * 由模型/逻辑层（GameManagerImpl）实现：每个动作在此做权威的状态前置校验，
 * 并修改物理状态、库存或道具效果。InputController 不允许直接触碰这些状态，
 * 只能在 justPressed 防抖通过后调用本方法，从而保证 View → Controller → Model 单向依赖，
 * 且 Model 可以脱离界面直接接收动作跑完一整局（headless 可验证）。
 */
public interface GameActionHandler {

    /**
     * 处理一名玩家的语义动作（暂停动作 playerId 传 0）。
     * 实现方负责：对局状态门控 → 钩爪/库存状态门控 → 扣库存/改物理/激活效果。
     *
     * @param playerId 玩家编号（1=P1，2=P2；TOGGLE_PAUSE 传 0）
     * @param action   语义动作（不允许为 null）
     */
    void handleAction(int playerId, ActionType action);
}
