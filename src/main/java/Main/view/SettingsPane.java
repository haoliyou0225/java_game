// FR-UI SettingsPane：可复用的游戏设置面板（音乐音量/音效音量/钩爪速度），供主界面“设置”弹窗使用
package Main.view;

import Main.config.GameSettings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * 游戏设置面板（纯内容组件，不持有窗口）。
 * <p>
 * 三个调节项（拖动滑块实时生效并写回 {@link GameSettings}）：
 * <ol>
 *   <li>音乐音量：0~100%，实时应用到 {@link AudioManager} 背景音乐；</li>
 *   <li>音效音量：0~100%，实时应用到全部已加载音效，松开滑块播放一次提示音反馈；</li>
 *   <li>钩爪速度：×0.5~×1.5，对局中钩爪摆动/抛出/收回速度整体倍率（对局开始后生效）。</li>
 * </ol>
 * <p>
 * “返回”按钮仅抛出回调，由宿主窗口决定关闭行为；本组件不注册键盘监听器，
 * 宿主窗口关闭时无需额外解绑（与 {@link ControlGuidePane} 设计一致）。
 */
public class SettingsPane extends VBox {

    /** 卡片固定宽度（与 ControlGuidePane 一致的逻辑宽度） */
    private static final double CARD_WIDTH = 720;

    /** 音乐音量滑块 */
    private final Slider musicSlider;

    /** 音效音量滑块 */
    private final Slider sfxSlider;

    /** 钩爪速度滑块 */
    private final Slider hookSpeedSlider;

    /** 音乐音量数值标签（如 "70%"） */
    private final Label musicValueLabel;

    /** 音效音量数值标签（如 "80%"） */
    private final Label sfxValueLabel;

    /** 钩爪速度数值标签（如 "x1.0"） */
    private final Label hookSpeedValueLabel;

    /** 由上层注入的“返回”回调（宿主窗口关闭逻辑） */
    private Runnable onBack;

    public SettingsPane() {
        // ===== 标题 =====
        Label title = new Label("游戏设置");
        title.setStyle("-fx-font-size: 30px; -fx-font-weight: bold; -fx-text-fill: #e8c87a; "
                + "-fx-effect: dropshadow(one-pass-box, #000000, 4, 1.0, 1, 1);");

        // ===== 音乐音量行 =====
        musicSlider = createSlider(0, 100, GameSettings.getMusicVolume() * 100);
        musicValueLabel = createValueLabel();
        musicSlider.valueProperty().addListener((obs, oldV, newV) -> {
            double volume = newV.doubleValue() / 100.0;
            GameSettings.setMusicVolume(volume);
            AudioManager.get().applyMusicVolume(volume);
            musicValueLabel.setText(Math.round(newV.doubleValue()) + "%");
        });
        HBox musicRow = buildRow("音乐音量", musicSlider, musicValueLabel);

        // ===== 音效音量行 =====
        sfxSlider = createSlider(0, 100, GameSettings.getSfxVolume() * 100);
        sfxValueLabel = createValueLabel();
        sfxSlider.valueProperty().addListener((obs, oldV, newV) -> {
            double volume = newV.doubleValue() / 100.0;
            GameSettings.setSfxVolume(volume);
            AudioManager.get().applySfxVolume(volume);
            sfxValueLabel.setText(Math.round(newV.doubleValue()) + "%");
        });
        // 松开滑块时播放一次提示音，即时反馈音效音量大小
        sfxSlider.setOnMouseReleased(e -> AudioManager.get().playSfx("Select"));
        HBox sfxRow = buildRow("音效音量", sfxSlider, sfxValueLabel);

        // ===== 钩爪速度行 =====
        hookSpeedSlider = createSlider(
                GameSettings.MIN_HOOK_SPEED * 100, GameSettings.MAX_HOOK_SPEED * 100,
                GameSettings.getHookSpeedMultiplier() * 100);
        // 细分档位：createSlider 默认 majorTickUnit=50 + snapToTicks，0.5x~1.5x 只能吸附
        // 在 0.5/1.0/1.5 三档，中间值一松手就被吸回（手感像“调了没效果”）；改为每 0.1 一档
        hookSpeedSlider.setMajorTickUnit(10);
        hookSpeedSlider.setMinorTickCount(4);
        hookSpeedValueLabel = createValueLabel();
        hookSpeedSlider.valueProperty().addListener((obs, oldV, newV) -> {
            double multiplier = newV.doubleValue() / 100.0;
            GameSettings.setHookSpeedMultiplier(multiplier);
            hookSpeedValueLabel.setText(String.format("x%.1f", multiplier));
        });
        HBox hookRow = buildRow("钩爪速度", hookSpeedSlider, hookSpeedValueLabel);

        // ===== 返回按钮 =====
        Button backButton = UiStyle.createGoldButton("返回");
        backButton.setOnAction(e -> {
            if (onBack != null) {
                onBack.run();
            }
        });

        // ===== 卡片整体装配 =====
        getChildren().addAll(title, musicRow, sfxRow, hookRow, backButton);
        setAlignment(Pos.CENTER);
        setSpacing(22);
        setPadding(new Insets(28, 48, 30, 48));
        setPrefWidth(CARD_WIDTH);
        setMaxWidth(CARD_WIDTH);
        // 半透明深色卡片 + 金色细描边（与 ControlGuidePane 一致）
        setStyle("-fx-background-color: rgba(20, 10, 2, 0.72); "
                + "-fx-background-radius: 16; "
                + "-fx-border-color: rgba(201, 138, 45, 0.45); "
                + "-fx-border-radius: 16; -fx-border-width: 1;");

        // 初始显示数值
        musicValueLabel.setText(Math.round(GameSettings.getMusicVolume() * 100) + "%");
        sfxValueLabel.setText(Math.round(GameSettings.getSfxVolume() * 100) + "%");
        hookSpeedValueLabel.setText(String.format("x%.1f", GameSettings.getHookSpeedMultiplier()));
    }

    /**
     * 注册“返回”按钮回调（由上层执行窗口关闭逻辑）。
     *
     * @param action 返回动作
     */
    public void setOnBack(Runnable action) {
        this.onBack = action;
    }

    /** 创建统一规格的调节滑块（数值刻度 10%，步进对齐刻度） */
    private static Slider createSlider(double min, double max, double value) {
        Slider slider = new Slider(min, max, value);
        slider.setPrefWidth(320);
        slider.setBlockIncrement(10);
        slider.setMajorTickUnit(50);
        slider.setMinorTickCount(4);
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setSnapToTicks(true);
        return slider;
    }

    /** 创建数值展示标签（金色，定宽右对齐） */
    private static Label createValueLabel() {
        Label label = new Label("");
        label.setPrefWidth(76);
        label.setAlignment(Pos.CENTER_RIGHT);
        label.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #ffd970;");
        return label;
    }

    /** 组装一行：名称标签 + 滑块 + 数值标签 */
    private static HBox buildRow(String name, Slider slider, Label valueLabel) {
        Label nameLabel = new Label(name);
        nameLabel.setPrefWidth(100);
        nameLabel.setStyle("-fx-font-size: 20px; -fx-text-fill: #e8d5b0;");
        HBox row = new HBox(18, nameLabel, slider, valueLabel);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }
}
