package com.dz.hello.main.controller;

// FR-15 键盘输入处理：onKeyPressed(KeyCode) → InputAction，空格=玩家1、回车=玩家2
import javafx.scene.input.KeyCode;

// FR-15 键盘输入处理：空格=玩家1 回车=玩家2
public class InputHandler {
    public InputAction onKeyPressed(KeyCode key) {
        // 后续在这里写空格、回车等按键的分发逻辑
        return null;
    }
}
