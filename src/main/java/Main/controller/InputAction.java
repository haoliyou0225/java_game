// FR-14 输入动作接口：THROW_P1=空格抛玩家1钩子、THROW_P2=回车抛玩家2钩子
package Main.controller;

public interface InputAction {
    String THROW_P1 = "THROW_P1";
    String THROW_P2 = "THROW_P2";
    String getType();
    int getPlayerId();
}
