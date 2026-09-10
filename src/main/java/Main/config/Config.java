// FR-UI Config：UI 层常量（窗口 1280x720、钩爪起点、HUD 高度、对局时长），来自 feature_ui 分支
package Main.config;

/**
 * UI 层配置常量（feature_ui 分支）。
 * 与 hook 分支的 GameConfig 共存：GameConfig 服务钩子物理，本类服务界面布局。
 */
public final class Config {
    private Config() {}

    public static final int WIDTH = 1280;
    public static final int HEIGHT = 720;
    public static final double ASPECT_RATIO = 16.0 / 9.0;
    public static final int TARGET_FPS = 60;
    /** 对局总时长（秒），倒计时从该值开始（FR-30） */
    public static final double GAME_DURATION = 90;
    public static final double HOOK1_START_X = 320;
    public static final double HOOK2_START_X = 960;
    /** 钩爪起点 Y（地面条内，与 GameConfig.HOOK_ANCHOR_Y 对齐），钩爪向下抓取 */
    public static final double HOOK_START_Y = 80;
    /** HUD 顶部信息栏高度（FR-29/FR-30），GameView 地面条上沿与之对齐 */
    public static final double HUD_HEIGHT = 90;
}
