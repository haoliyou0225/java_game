// FR-UI Main：程序入口（feature_ui 版主界面）——主菜单/对局/暂停/结算四阶段流程，组装 GameModel/GameView/HUDView/GameTimer/InputController
package Main;

import Main.config.Config;
import Main.controller.GameManagerImpl;
import Main.controller.GameTimer;
import Main.controller.GameTimerImpl;
import Main.controller.InputController;
import Main.controller.InputControllerImpl;
import Main.model.GameModel;
import Main.model.GameState;
import Main.view.HUDView;
import Main.view.HUDViewImpl;
import Main.view.GameView;
import Main.view.GameViewImpl;
import Main.view.MenuView;
import Main.view.MenuViewImpl;
import Main.view.PauseView;
import Main.view.PauseViewImpl;
import Main.view.ResultView;
import Main.view.ResultViewImpl;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

/**
 * 程序入口（FR-32 主菜单，feature_ui 分支版本）
 * 启动后显示主菜单：标题 GoldMainer、"开始对战"、"退出游戏"。
 * <p>
 * 说明：hook 分支的钩子玩法（GameManager/HookImpl 真实物理）保留在原类中，
 * 当前入口使用 UI 分支的 GameModel + 假数据（DummyDataFactory）跑通完整界面流程；
 * 后续将 DummyDataFactory 替换为真实钩爪/物品即可接入钩子玩法。
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("黄金矿工 - 双人PK版");
        primaryStage.setResizable(false); // 锁定窗口大小

        Pane root = new Pane(); // 全局根容器：主菜单 / 对局界面 / 结算界面均挂载于此
        Scene scene = new Scene(root, Config.WIDTH, Config.HEIGHT);
        primaryStage.setScene(scene);
        primaryStage.show(); // 显示窗口

        showMainMenu(root, primaryStage);
    }

    /** 显示主菜单（FR-32） */
    private void showMainMenu(Pane root, Stage stage) {
        MenuView menuView = new MenuViewImpl();

        // 点击"开始对战"：关闭主菜单，执行开局流程
        menuView.setOnStartGame(() -> {
            menuView.hide();
            startGame(root);
        });

        // 点击"退出游戏"：关闭游戏窗口
        menuView.setOnExit(stage::close);

        menuView.show(root);
    }

    /**
     * 开局流程（FR-32）
     * 组装模型、游戏画面（FR-28）、HUD（FR-29/FR-30）与倒计时控制器，启动对局。
     */
    private void startGame(Pane root) {
        Pane gamePane = new Pane();
        gamePane.setPrefSize(Config.WIDTH, Config.HEIGHT);

        // 模型初始化（融合版：GameManagerImpl 同时实现 GameModel 接口）
        GameModel model = new GameManagerImpl();
        model.setRemainingTime(Config.GAME_DURATION); // 从 90 秒开始
        model.setState(GameState.PLAYING);

        // 游戏画面：Canvas + GameViewImpl（FR-28：地面/矿洞/物品/钩爪/绳索）
        Canvas canvas = new Canvas(Config.WIDTH, Config.HEIGHT);
        gamePane.getChildren().add(canvas);
        GameView gameView = new GameViewImpl(canvas);
        gameView.render(model); // 初始渲染

        // HUD：顶部三栏（P1分数 | 剩余时间 | P2分数）（FR-29/FR-30）
        HUDView hudView = new HUDViewImpl();
        gamePane.getChildren().add(hudView.build());
        hudView.render(model); // 初始显示

        // FR-18：双人按键独立监听（View/Main 层监听 JavaFX 键盘事件，转发给 controller）
        // S → 玩家1释放钩爪；↓ → 玩家2释放钩爪；ESC → 暂停/继续（双方共用）
        InputController inputController = new InputControllerImpl(model);

        // 暂停遮罩（挂在 gamePane 顶层覆盖游戏画面；初始隐藏）
        PauseView pauseView = new PauseViewImpl();

        Scene scene = root.getScene();
        if (scene != null) {
            scene.setOnKeyPressed(event -> {
                KeyCode code = event.getCode();
                if (code == KeyCode.S) {
                    inputController.player1ReleaseHook();   // 玩家1：释放钩爪
                } else if (code == KeyCode.W) {
                    inputController.player1UseDynamite();   // 玩家1：引爆炸药（炸毁钩上物品）
                } else if (code == KeyCode.DOWN) {
                    inputController.player2ReleaseHook();   // 玩家2：释放钩爪
                } else if (code == KeyCode.UP) {
                    inputController.player2UseDynamite();   // 玩家2：引爆炸药（炸毁钩上物品）
                } else if (code == KeyCode.ESCAPE) {
                    inputController.togglePause();
                    // 按切换后的最新状态显示/隐藏「已暂停」遮罩
                    if (model.getState() == GameState.PAUSED) {
                        pauseView.show(gamePane);
                    } else {
                        pauseView.hide();
                    }
                }
                // 其余按键本阶段忽略（道具快捷键后续接入）
            });
        }

        // 钩子真实物理驱动：每帧由 AnimationTimer 推进 HookImpl 钟摆/抛出/收回/抓取，并重绘画面
        // PAUSED 时 gameLoopTick 内部冻结，与 GameTimer 倒计时联动
        final long[] lastFrameNanos = {0};
        final AnimationTimer physicsTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (lastFrameNanos[0] == 0) {
                    lastFrameNanos[0] = now;
                    return;
                }
                double deltaTime = (now - lastFrameNanos[0]) / 1_000_000_000.0;
                lastFrameNanos[0] = now;
                // 防止窗口失焦后 deltaTime 留间过长导致物理跳变
                if (deltaTime > 0.1) deltaTime = 0.1;
                model.gameLoopTick(deltaTime);
                gameView.render(model);
                hudView.render(model);
            }
        };
        physicsTimer.start();

        // 倒计时控制器：每秒递减 1，归零结束对局（FR-30）
        // 注意：GameTimerImpl 的 tick 在后台线程执行，所有 UI 操作必须用
        // Platform.runLater 切回 JavaFX Application Thread（分层约束：controller 不依赖 javafx）
        GameTimer timer = new GameTimerImpl(model);
        timer.setOnTick(() -> Platform.runLater(() -> hudView.render(model)));
        timer.setOnTimeUp(() -> Platform.runLater(() -> {
            physicsTimer.stop(); // 停止钩子物理驱动
            showResultFlow(root, gamePane, model);
        }));
        timer.start();

        root.getChildren().add(gamePane);
    }

    /**
     * 倒计时归零：进入胜负判定并弹出结算界面（FR-31）。
     * 结算界面挂在全局根容器上（覆盖对局画面）；
     * 点击"重新开始"：关闭结算界面 → 移除旧对局画面 → 触发新一轮开局流程。
     */
    private void showResultFlow(Pane root, Pane gamePane, GameModel model) {
        // 对局结束：先释放模型后台资源（钩爪收回线程池），防止反复开局累积线程
        model.shutdown();

        ResultView resultView = new ResultViewImpl();

        // 点击"重新开始"：关闭结算界面并开始新一轮对局
        resultView.setOnRestart(() -> {
            resultView.hide();
            root.getChildren().remove(gamePane);
            startGame(root);
        });

        // 胜负判定与渲染由 ResultView 内部完成（读取双方最终分数）
        resultView.show(root, model);
        System.out.println("[FR-31] 时间到，进入结算：玩家1=$" + model.getPlayer1().getScore()
                + "，玩家2=$" + model.getPlayer2().getScore());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
