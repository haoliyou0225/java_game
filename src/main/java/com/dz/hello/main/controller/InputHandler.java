// FR-15 键盘输入处理：onKeyPressed(KeyCode) → InputAction，空格=玩家1、回车=玩家2
package com.dz.hello.main.controller;

import javafx.scene.input.KeyCode;

public class InputHandler {

    /** 按键 → 输入动作分发 */
    public InputAction onKeyPressed(KeyCode key) {
        if (key == KeyCode.SPACE) {
            return new InputAction(InputAction.THROW_P1, 1);
        }
        if (key == KeyCode.ENTER) {
            return new InputAction(InputAction.THROW_P2, 2);
        }
        return null;
    }
}
