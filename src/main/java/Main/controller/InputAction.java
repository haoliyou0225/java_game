// FR-14 输入动作常量：THROW_P1=空格抛玩家1钩子、THROW_P2=回车抛玩家2钩子
package Main.controller;

public class InputAction {
    public static final String THROW_P1 = "THROW_P1";
    public static final String THROW_P2 = "THROW_P2";
    private final String type;
    private final int playerId;
    public InputAction(String type, int playerId) {
        this.type = type;
        this.playerId = playerId;
    }
    public String getType() { return type; }
    public int getPlayerId() { return playerId; }
}