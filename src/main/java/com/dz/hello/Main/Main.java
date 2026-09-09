// FR-20 程序启动器：不含 main()，避免 JavaFX 模块检查问题，由 Application.launch(MainApp) 拉起
package com.dz.hello.Main;

import javafx.application.Application;

/**
 * 程序启动器（严格对齐 UML）
 * 不继承 Application，避免 JavaFX 模块检查问题
 */
public class Main {
    public static void main(String[] args) {
        Application.launch(MainApp.class, args);
    }
}
