// FR-14 输入动作实现：THROW_P1=空格抛玩家1钩子、THROW_P2=回车抛玩家2钩子
package Main.controller;

public class InputActionImpl implements InputAction {
    private final String type;
    private final int playerId;

    public InputActionImpl(String type, int playerId) {
        this.type = type;
        this.playerId = playerId;
    }

    @Override public String getType() { return type; }
    @Override public int getPlayerId() { return playerId; }
}
