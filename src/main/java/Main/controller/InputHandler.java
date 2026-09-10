// FR-15 键盘输入处理接口：onKeyPressed(KeyCode) → InputAction，空格=玩家1、回车=玩家2
package Main.controller;

import javafx.scene.input.KeyCode;

public interface InputHandler {
    InputAction onKeyPressed(KeyCode key);
}
