// FR-21 JavaFX 主界面：extends Application，start() 中搭场景/键盘监听/AnimationTimer 主循环，三阶段状态机切换
package com.dz.hello.Main;

import com.dz.hello.Main.config.GameConfig;
import com.dz.hello.Main.controller.GameManager;
import com.dz.hello.Main.controller.InputAction;
import com.dz.hello.Main.controller.InputHandler;
import com.dz.hello.Main.model.GameStage;
import com.dz.hello.Main.view.GameMenu;
import com.dz.hello.Main.view.GameUI;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * JavaFX 应用主类（严格对齐 UML）
 */
public class MainApp extends Application {

    private GameManager gameManager;
    private InputHandler inputHandler;
    private GameUI gameUI;
    private GameMenu gameMenu;
    private Canvas canvas;
    private AnimationTimer timer;

    @Override
    public void start(Stage stage) {
        canvas = new Canvas(GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT);
        gameUI = new GameUI(canvas);
        gameMenu = new GameMenu(canvas);
        gameManager = new GameManager();
        inputHandler = new InputHandler();

        StackPane root = new StackPane(canvas);
        Scene scene = new Scene(root, GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT);

        // 键盘输入
        scene.setOnKeyPressed(e -> {
            InputAction action = inputHandler.onKeyPressed(e.getCode());
            if (action != null) {
                if (gameManager.getGameData().getStage() == GameStage.READY) {
                    gameManager.startGame();
                }
                gameManager.dispatchAction(action);
            }
        });

        stage.setTitle("双人钩子矿工");
        stage.setScene(scene);
        stage.show();

        // 游戏主循环
        timer = new AnimationTimer() {
            private long lastNano = 0;
            @Override
            public void handle(long now) {
                double delta = (now - lastNano) / 1_000_000_000.0;
                lastNano = now;
                if (gameManager.getGameData().getStage() == GameStage.READY) {
                    gameMenu.drawStartMenu();
                } else if (gameManager.getGameData().getStage() == GameStage.PLAYING) {
                    gameManager.gameLoopTick(delta);
                    gameUI.renderAll(
                            gameManager.getGameData(),
                            gameManager.getHookP1(),
                            gameManager.getHookP2(),
                            gameManager.getSceneItemList());
                } else if (gameManager.getGameData().getStage() == GameStage.GAME_OVER) {
                    gameMenu.drawResultPanel(gameManager.getGameData().getWinnerId());
                }
            }
        };
        timer.start();
    }
}
