// FR-UI ControlGuidePane：可复用的“操作提示 + 游戏规则”组件（FR-26），供新手指南窗口与暂停界面复用
package Main.view;

import Main.config.Config;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * 操作提示与游戏规则的可复用面板（FR-26）。
 * <p>
 * 设计目的：主菜单“新手指南”弹窗与暂停界面（FR-26）都需要展示同一套
 * 双人按键映射与游戏规则，统一抽取到本组件，避免两处重复开发、内容不一致。
 * <p>
 * 内容分区（自上而下）：
 * 1. “操作说明”标题；
 * 2. P1 / P2 两列按键映射表（键帽 + 功能，数据由 {@link #P1_KEY_HINTS}/{@link #P2_KEY_HINTS} 驱动）；
 * 3. 双方共用的 ESC 暂停提示；
 * 4. “游戏规则”标题与条目（数值描述对齐 GameConfig）。
 * <p>
 * 本组件为纯展示组件，不持有任何回调与键盘监听器，宿主窗口关闭时无需额外解绑。
 */
public class ControlGuidePane extends VBox {

    /** P1 键位表：每项 = {键帽文本, 功能说明}，顺序即界面展示顺序 */
    public static final String[][] P1_KEY_HINTS = {
            {"S", "释放钩爪"},
            {"W", "使用炸药"},
            {"A", "强力药水"},
            {"D", "冰冻箱"},
            {"F", "幸运草"},
            {"G", "钻石升级"},
            {"H", "石头书"},
    };

    /** P2 键位表：每项 = {键帽文本, 功能说明}，顺序即界面展示顺序 */
    public static final String[][] P2_KEY_HINTS = {
            {"↓", "释放钩爪"},
            {"↑", "使用炸药"},
            {"Num1", "强力药水"},
            {"Num2", "冰冻箱"},
            {"Num3", "幸运草"},
            {"Num4", "钻石升级"},
            {"Num5", "石头书"},
    };

    /** 规则正文最大行宽，超出自动换行，保证小尺寸窗口下不裁切、不错位 */
    private static final double RULE_TEXT_MAX_WIDTH = 1080;

    /** 组件卡片固定宽度（逻辑分辨率 1280 下左右各留 40 边距） */
    private static final double CARD_WIDTH = Config.WIDTH - 80;

    public ControlGuidePane() {
        // ===== 操作说明分区 =====
        Label controlTitle = new Label("操作说明");
        controlTitle.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #e8c87a;");

        // P1 / P2 两列键位表水平并排
        HBox keyColumns = new HBox(96,
                buildKeyColumn("玩家1（P1）", P1_KEY_HINTS),
                buildKeyColumn("玩家2（P2）", P2_KEY_HINTS));
        keyColumns.setAlignment(Pos.CENTER);

        // 双方共用的暂停键提示
        Label escLabel = new Label("ESC：暂停 / 继续对局");
        escLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #bfa37a;");

        // ===== 游戏规则分区 =====
        Label ruleTitle = new Label("游戏规则");
        ruleTitle.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #e8c87a;");

        VBox ruleBox = new VBox(8);
        ruleBox.setAlignment(Pos.TOP_LEFT);
        // 规则条目：数值与 GameConfig 对齐（90 秒 / 炸药 1~3 个 / 药水 ×2 持续 10 秒 / 冰冻 3 秒等）
        String[] rules = {
                "1. 双人同屏对战：P1 居左、P2 居右，各操控一台矿工；对局时长 90 秒，倒计时归零后总分高者获胜。",
                "2. 钩爪自动左右摆动，在摆动时按“释放钩爪”键抛出；钩中物品后自动收回并按物品价值计分（金块、钻石价值高，石头价值低），空钩快速收回。",
                "3. 炸药：钩到不想要的物品时按炸药键炸毁携带物，钩爪立即空钩收回（开局各 1 个，每局最多持有 3 个）。",
                "4. 强力药水：自身钩爪收回速度 ×2，持续 10 秒；冰冻箱：冻结对方钩爪 3 秒（两种短时道具库存上限各 5 个）。",
                "5. 幸运草：本局物品收益 ×1.5；钻石升级：钻石价值 ×2；石头书：石头价值 ×3（三类持续道具单局激活，按对应键触发）。",
                "6. 福袋随机开出金币、炸药或道具；对应库存已满时自动折算为 50 金币。",
                "7. ESC 暂停 / 继续对局；窗口失焦时系统会自动清空按键状态，避免“卡键”。",
        };
        for (String rule : rules) {
            ruleBox.getChildren().add(buildRuleLabel(rule));
        }

        // ===== 卡片整体装配 =====
        getChildren().addAll(controlTitle, keyColumns, escLabel, ruleTitle, ruleBox);
        setAlignment(Pos.CENTER);
        setSpacing(12);
        setPadding(new Insets(20, 40, 22, 40));
        setPrefWidth(CARD_WIDTH);
        setMaxWidth(CARD_WIDTH);
        // 半透明深色卡片 + 金色细描边，与矿洞深色背景区分层次
        setStyle("-fx-background-color: rgba(20, 10, 2, 0.35); "
                + "-fx-background-radius: 16; "
                + "-fx-border-color: rgba(201, 138, 45, 0.45); "
                + "-fx-border-radius: 16; -fx-border-width: 1;");
    }

    /**
     * 构建单个玩家的键位列：列标题 + 若干“键帽 + 功能说明”行。
     *
     * @param columnTitle 列标题（如 “玩家1（P1）”）
     * @param keyHints    键位表，每项为 {键帽文本, 功能说明}
     * @return 左对齐的键位列容器
     */
    private VBox buildKeyColumn(String columnTitle, String[][] keyHints) {
        VBox column = new VBox(5);
        column.setAlignment(Pos.TOP_LEFT);
        column.getChildren().add(UiStyle.createColumnTitle(columnTitle));
        for (String[] hint : keyHints) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getChildren().addAll(UiStyle.createKeyCap(hint[0]), UiStyle.createHintLabel(hint[1]));
            column.getChildren().add(row);
        }
        return column;
    }

    /**
     * 构建单条规则文本：浅色文字 + 自动换行，宽度受 {@link #RULE_TEXT_MAX_WIDTH} 约束。
     *
     * @param text 规则内容
     * @return 可自动换行的标签
     */
    private Label buildRuleLabel(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setMaxWidth(RULE_TEXT_MAX_WIDTH);
        label.setStyle("-fx-font-size: 15px; -fx-text-fill: #e8d5b0; -fx-line-spacing: 2;");
        return label;
    }
}
