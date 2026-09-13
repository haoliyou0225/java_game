// FR-UI Main：View 装配入口——主菜单/对局/暂停/结算四阶段，键位映射/全屏切换，经 InputController 派发到逻辑层
package Main;

import Main.config.Config;
import Main.controller.ActionType;
import Main.controller.GameActionHandler;
import Main.controller.GameManagerImpl;
import Main.controller.GameTimer;
import Main.controller.GameTimerImpl;
import Main.controller.InputController;
import Main.controller.InputControllerImpl;
import Main.model.GameModel;
import Main.model.GameState;
import Main.view.HUDView;
import Main.view.HUDViewImpl;
import Main.view.ControlGuidePane;
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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.layout.VBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.transform.Scale;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * 程序入口（View 装配层，FR-18/FR-26/FR-32）
 * 启动后显示主菜单：标题 GoldMainer、"开始对战"、"新手指南"、"退出游戏"。
 * 点击"新手指南"弹出独立模态窗口（{@link #openGuideWindow(Stage)}），
 * 窗口内容为可复用组件 {@link ControlGuidePane}（双人键位表 + 游戏规则，FR-26）。
 * <p>
 * 分层结构：本类只负责 View 装配（键位映射、场景切换、全屏、窗口），
 * 键盘事件经 {@link Main.controller.InputController} 防抖后派发到逻辑层
 * （{@link GameManagerImpl#handleAction}），物理/库存/道具效果均不在本层修改。
 * Model/Controller 不依赖 JavaFX，可通过 {@link HeadlessGameMain} 脱离界面跑完一整局。
 */
public class Main extends Application {

    /** 当前对局的输入控制器；非对局状态为 null（供窗口失焦监听统一清空防抖标志，避免重复注册） */
    private InputController currentInputController;

    /** F11 按下边沿防抖标志（全局：菜单与对局中均可切换全屏，避免系统自动重复连发） */
    private boolean f11Down = false;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("黄金矿工 - 双人PK版");
        primaryStage.setResizable(false); // 窗口模式锁定 1280×720；全屏通过 F11 等比切换

        Pane root = new Pane(); // 全局根容器：主菜单 / 对局界面 / 结算界面均挂载于此
        Scene scene = new Scene(root, Config.WIDTH, Config.HEIGHT);
        scene.setFill(Color.BLACK); // 全屏等比缩放时外围黑边
        primaryStage.setScene(scene);

        // F11 全屏切换：禁用默认 ESC 退出全屏（ESC 在对局中专用于暂停）
        primaryStage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        primaryStage.setFullScreenExitHint("");
        primaryStage.fullScreenProperty().addListener((obs, wasFull, isFull) ->
                applyFullScreenScale(root, isFull));

        // F11 全局键（菜单/对局均生效）：事件过滤器在捕获阶段处理并消费，
        // 不随每局 setOnKeyPressed 重设而丢失，也不会穿透到对局动作映射
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.F11 && !f11Down) {
                f11Down = true;
                primaryStage.setFullScreen(!primaryStage.isFullScreen());
                event.consume();
            }
        });
        scene.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
            if (event.getCode() == KeyCode.F11) {
                f11Down = false;
                event.consume();
            }
        });

        // 窗口失焦：清空当前对局按键防抖标志，防止切窗回来后“卡键”（只注册一次，避免反复开局累积监听）
        primaryStage.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused && currentInputController != null) {
                currentInputController.resetPressedState();
            }
        });

        primaryStage.show(); // 显示窗口

        showMainMenu(root, primaryStage);
    }

    /**
     * 全屏/窗口切换时对 1280×720 逻辑画面做等比缩放并居中，保证任何屏幕比例下都保持 16:9（外围黑边）。
     */
    private void applyFullScreenScale(Pane root, boolean isFull) {
        root.getTransforms().clear();
        root.setTranslateX(0);
        root.setTranslateY(0);
        if (!isFull) {
            return;
        }
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        double scale = Math.min(bounds.getWidth() / Config.WIDTH,
                bounds.getHeight() / Config.HEIGHT);
        root.getTransforms().add(new Scale(scale, scale, 0, 0));
        root.setTranslateX((bounds.getWidth() - Config.WIDTH * scale) / 2);
        root.setTranslateY((bounds.getHeight() - Config.HEIGHT * scale) / 2);
    }

    /** 显示主菜单（FR-32） */
    private void showMainMenu(Pane root, Stage stage) {
        MenuView menuView = new MenuViewImpl();

        // 点击"开始对战"：关闭主菜单，执行开局流程
        menuView.setOnStartGame(() -> {
            menuView.hide();
            startGame(root);
        });

        // 点击"新手指南"：弹出独立的模态指南窗口（不离开主菜单，关闭即返回）
        menuView.setOnOpenGuide(() -> openGuideWindow(stage));

        // 点击"退出游戏"：关闭游戏窗口
        menuView.setOnExit(stage::close);

        menuView.show(root);
    }

    /**
     * 打开“新手指南”独立窗口（FR-26）。
     * <p>
     * 实现要点：
     * 1. 独立 {@link Stage} + {@link Modality#APPLICATION_MODAL}：弹出期间主菜单不可点击，
     *    玩家必须先关闭指南窗口才能继续操作，防止点击穿透导致 UI 错位；
     * 2. 内容使用可复用组件 {@link ControlGuidePane}（双人键位表 + 游戏规则），
     *    外层包 {@link ScrollPane} 兜底，低分辨率或系统字体放大时可滚动，保证不裁切；
     * 3. 场景逻辑分辨率与主菜单一致（{@link Config#WIDTH}×{@link Config#HEIGHT}），
     *    屏幕可用区不足时对内容做等比 {@link Scale} 缩放（原点左上角），保证清晰度；
     * 4. 关闭（×/ESC 关闭窗口）后自动返回主菜单；onHidden 中清空场景按键回调与内容引用，
     *    避免窗口关闭后按键监听器仍被持有或拦截。
     *
     * @param owner 主窗口（作为模态属主，指南窗口相对它居中）
     */
    private void openGuideWindow(Stage owner) {
        Stage guideStage = new Stage();
        guideStage.initOwner(owner);
        // 应用级模态：指南窗口弹出期间屏蔽主菜单输入
        guideStage.initModality(Modality.APPLICATION_MODAL);
        guideStage.setTitle("新手指南 · 操作提示与游戏规则");
        guideStage.setResizable(false);

        // 可复用内容组件：P1/P2 键位映射 + 游戏规则
        ControlGuidePane guidePane = new ControlGuidePane();

        // 滚动内容容器：与主菜单一致的矿洞背景，指南卡片在其中水平居中并上下留白
        VBox contentBox = new VBox(guidePane);
        contentBox.setAlignment(Pos.CENTER);
        contentBox.setPadding(new Insets(28, 0, 28, 0));
        contentBox.setStyle("-fx-background-color: linear-gradient(to bottom, #2b1a0e, #4a2f17);");

        // ScrollPane 兜底：内容超高时出现纵向滚动条，横向滚动条禁用（卡片宽度固定且适配）
        ScrollPane scroll = new ScrollPane(contentBox);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        // 滚动面板底色取矿洞渐变的中间色（必须为纯色：Modena 滚动条箭头样式会对
        // -fx-background 做 derive()，填渐变会抛 LinearGradient→Color 的告警）；
        // 渐变背景由内容容器 contentBox 承载，视觉效果一致
        scroll.setStyle("-fx-background: #3a2212; -fx-background-color: #3a2212;");

        // 场景逻辑分辨率与主菜单一致：1280×720
        Scene scene = new Scene(scroll, Config.WIDTH, Config.HEIGHT);
        guideStage.setScene(scene);

        // 多分辨率适配：屏幕可用区小于逻辑分辨率时，等比缩放内容（缩放原点为左上角）
        Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
        double scale = Math.min(1.0,
                Math.min(visualBounds.getWidth() / Config.WIDTH,
                        visualBounds.getHeight() / Config.HEIGHT));
        if (scale < 1.0) {
            contentBox.getTransforms().add(new Scale(scale, scale, 0, 0));
            guideStage.setWidth(Config.WIDTH * scale);
            guideStage.setHeight(Config.HEIGHT * scale);
        }

        // 关闭清理：本窗口自身不注册游戏按键逻辑，这里再做防御性解绑与引用释放
        guideStage.setOnHidden(e -> {
            scene.setOnKeyPressed(null);
            scene.setOnKeyReleased(null);
            scroll.setContent(null);
        });

        guideStage.show();
        // 相对主窗口居中（show 后才能拿到实际宽高）
        guideStage.setX(owner.getX() + (owner.getWidth() - guideStage.getWidth()) / 2);
        guideStage.setY(owner.getY() + (owner.getHeight() - guideStage.getHeight()) / 2);
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

        // FR-18 输入分层装配：
        // GameManagerImpl 同时是 GameModel（状态/数据）与 GameActionHandler（动作权威处理），
        // InputController 只做 justPressed 防抖与派发，不直接改物理/库存/效果。
        GameActionHandler actionHandler = (GameActionHandler) model;
        InputController inputController = new InputControllerImpl(model, actionHandler);
        currentInputController = inputController;

        // 暂停遮罩（挂在 gamePane 顶层覆盖游戏画面；初始隐藏）
        PauseView pauseView = new PauseViewImpl();

        Scene scene = root.getScene();
        if (scene != null) {
            // 按下：KeyCode → (玩家, 语义动作) 的映射只存在于 View 层（mapKeyBinding）；
            // F11 已在场景事件过滤器中全局处理并消费，不会到达这里
            scene.setOnKeyPressed(event -> {
                KeyCode code = event.getCode();
                if (code == KeyCode.ESCAPE) {
                    // ESC 经 controller 防抖后由逻辑层切换；View 只按返回状态显隐遮罩
                    GameState state = inputController.pressPause();
                    if (state == GameState.PAUSED) {
                        pauseView.show(gamePane);
                    } else {
                        pauseView.hide();
                    }
                    return;
                }
                KeyBinding binding = mapKeyBinding(code);
                if (binding != null) {
                    inputController.pressAction(binding.playerId(), binding.action());
                }
            });
            // 松开：复位对应防抖标志（ESC 复位暂停通道）
            scene.setOnKeyReleased(event -> {
                KeyCode code = event.getCode();
                if (code == KeyCode.ESCAPE) {
                    inputController.releaseAction(0, ActionType.TOGGLE_PAUSE);
                    return;
                }
                KeyBinding binding = mapKeyBinding(code);
                if (binding != null) {
                    inputController.releaseAction(binding.playerId(), binding.action());
                }
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

        // 倒计时控制器：每秒递减 1，归零结束对局（FR-10）
        // 注意：GameTimerImpl 的 tick 在后台线程执行，所有 UI 操作必须用
        // Platform.runLater 切回 JavaFX Application Thread（分层约束：controller 不依赖 javafx）
        GameTimer timer = new GameTimerImpl(model);
        // 结束流程幂等保护：倒计时归零(FR-10)与物品清空(FR-08)可能竞争，只允许进入一次结算
        final boolean[] ended = {false};
        Runnable enterResult = () -> {
            if (ended[0]) return;
            ended[0] = true;
            physicsTimer.stop(); // 停止钩子物理驱动（渲染帧循环）
            timer.stop();        // 停止后台倒计时线程（防御性：归零路径也显式停止）
            showResultFlow(root, gamePane, model, timer);
        };
        timer.setOnTick(() -> Platform.runLater(() -> hudView.render(model)));
        timer.setOnTimeUp(() -> Platform.runLater(enterResult));
        timer.start();

        // FR-08 自动结束：场上物品清空且双钩均回 SWINGING 时模型立即回调（在 FX 线程触发，可直接操作 UI）
        model.setOnGameEnd(() -> {
            timer.stop();
            enterResult.run();
        });

        root.getChildren().add(gamePane);
    }

    /**
     * View 层键位映射结果：(玩家编号, 语义动作)。
     */
    private record KeyBinding(int playerId, ActionType action) {
    }

    /**
     * 物理键位 → 语义动作映射（严格对齐《按键与功能映射明细表》）。
     * P1：S 抛钩 / W 炸药 / A 强力药水 / D 冰冻箱 / F 幸运草 / G 钻石升级 / H 石头书；
     * P2：↓ 抛钩 / ↑ 炸药 / Num1~5 对应五种道具（无小键盘时主键盘 1~5 作别名）。
     * ESC、F11 属于全局键，不在此映射（由 View 层直接处理）。
     *
     * @return 映射结果；非游戏按键返回 null
     */
    private static KeyBinding mapKeyBinding(KeyCode code) {
        // 注意：这里刻意使用传统 switch 语句（每分支直接 return），
        // 不用 switch 表达式——Temurin 17.0.20 的 javac 对
        // "多分支 new 对象 + default null" 的 switch 表达式会生成
        // 错误的 StackMapTable（合并帧被写成 java/lang/Object），触发 VerifyError。
        switch (code) {
            // ===== 玩家1 =====
            case S: return new KeyBinding(1, ActionType.THROW_HOOK);
            case W: return new KeyBinding(1, ActionType.USE_DYNAMITE);
            case A: return new KeyBinding(1, ActionType.USE_POWER_POTION);
            case D: return new KeyBinding(1, ActionType.USE_FREEZE_BOX);
            case F: return new KeyBinding(1, ActionType.USE_LUCKY_CLOVER);
            case G: return new KeyBinding(1, ActionType.USE_DIAMOND_BOOST);
            case H: return new KeyBinding(1, ActionType.USE_STONE_BOOK);
            // ===== 玩家2（NUMPADn 为主，DIGITn 为无小键盘键盘的别名） =====
            case DOWN: return new KeyBinding(2, ActionType.THROW_HOOK);
            case UP: return new KeyBinding(2, ActionType.USE_DYNAMITE);
            case NUMPAD1:
            case DIGIT1: return new KeyBinding(2, ActionType.USE_POWER_POTION);
            case NUMPAD2:
            case DIGIT2: return new KeyBinding(2, ActionType.USE_FREEZE_BOX);
            case NUMPAD3:
            case DIGIT3: return new KeyBinding(2, ActionType.USE_LUCKY_CLOVER);
            case NUMPAD4:
            case DIGIT4: return new KeyBinding(2, ActionType.USE_DIAMOND_BOOST);
            case NUMPAD5:
            case DIGIT5: return new KeyBinding(2, ActionType.USE_STONE_BOOK);
            default: return null;
        }
    }

    /**
     * 倒计时归零：进入胜负判定并弹出结算界面（FR-31）。
     * 结算界面挂在全局根容器上（覆盖对局画面），提供两个出口：
     * <ul>
     *   <li>“重新开始”：关闭结算界面 → 移除旧对局画面 → 触发新一轮开局流程；</li>
     *   <li>“返回主菜单”：停止计时器 → 清空场景按键监听 → 移除结算/对局节点
     *       → 释放模型后台资源 → 重新显示主菜单（FR-25）。</li>
     * </ul>
     * 注意：新一轮对局在 {@link #startGame(Pane)} 中通过 {@code new GameManagerImpl()}
     * 完整重建模型（新物品列表、新玩家：分数 0、初始 1 个炸药、无道具效果），
     * 因此旧对局数据不会以任何静态/共享状态残留。
     *
     * @param timer 本局倒计时器（返回主菜单时再次防御性停止）
     */
    private void showResultFlow(Pane root, Pane gamePane, GameModel model, GameTimer timer) {
        // 对局结束：先释放模型后台资源（钩爪收回线程池），防止反复开局累积线程
        model.shutdown();

        ResultView resultView = new ResultViewImpl();

        // 点击"重新开始"：关闭结算界面并开始新一轮对局（模型由 startGame 整体重建）
        resultView.setOnRestart(() -> {
            resultView.hide();
            root.getChildren().remove(gamePane);
            startGame(root);
        });

        // 点击"返回主菜单"：完整对局资源释放 + 界面切换（全部在 FX 线程，仅节点增删，耗时远小于 50ms）
        resultView.setOnBackToMenu(() -> {
            // 1. 停止本局全部后台计时器（倒计时线程；物理帧循环已在进入结算时停止）
            timer.stop();
            // 2. 解绑对局模型的结束回调，避免旧模型被回调链引用
            model.setOnGameEnd(null);
            // 3. 清空场景级按键监听，防止返回菜单后旧 InputController/旧模型仍拦截按键
            Scene scene = root.getScene();
            if (scene != null) {
                scene.setOnKeyPressed(null);
                scene.setOnKeyReleased(null);
            }
            // 4. 移除结算遮罩与整棵对局节点树（Canvas/HUD/暂停遮罩随之一并释放，无旧界面残影）
            resultView.hide();
            root.getChildren().remove(gamePane);
            currentInputController = null; // 本局输入控制器随对局一起释放
            // 5. 重新显示主菜单（全新 MenuView，三个按钮均正常可点）
            Stage stage = (Stage) root.getScene().getWindow();
            showMainMenu(root, stage);
            System.out.println("[FR-31] 已返回主菜单，对局资源释放完成");
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
