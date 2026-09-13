// FR-UI GameSettings：运行时可变设置（音乐/音效音量、钩爪速度倍率），主界面“设置”按钮调节
package Main.config;

/**
 * 游戏运行时可变设置（区别于 {@link GameConfig} 的编译期固定常量）。
 * <p>
 * 由主界面“设置”面板（SettingsPane）读写：
 * <ul>
 *   <li>音乐音量：背景音乐音量 0~1（AudioManager 实时应用）；</li>
 *   <li>音效音量：抓取/按钮/炸弹等音效音量 0~1（AudioManager 实时应用）；</li>
 *   <li>钩爪速度倍率：摆动/抛出/收回速度整体倍率 0.5~1.5（HookImpl 每帧读取）。</li>
 * </ul>
 * <p>
 * 本类为纯静态配置（不依赖 JavaFX），model 层的 {@code HookImpl} 可直接读取钩爪速度倍率，
 * 不破坏“Model/Controller 不依赖 JavaFX”的分层约束。
 */
public final class GameSettings {

    private GameSettings() {}

    /** 默认音乐音量（0~1） */
    private static double musicVolume = 0.7;

    /** 默认音效音量（0~1） */
    private static double sfxVolume = 0.8;

    /** 默认钩爪速度倍率（×1.0，即 GameConfig 原速） */
    private static double hookSpeedMultiplier = 1.0;

    /** 钩爪速度倍率下限 */
    public static final double MIN_HOOK_SPEED = 0.5;

    /** 钩爪速度倍率上限 */
    public static final double MAX_HOOK_SPEED = 1.5;

    /** 音乐音量（0~1） */
    public static double getMusicVolume() {
        return musicVolume;
    }

    /** 设置音乐音量（自动夹紧到 0~1） */
    public static void setMusicVolume(double volume) {
        musicVolume = clamp(volume, 0.0, 1.0);
    }

    /** 音效音量（0~1） */
    public static double getSfxVolume() {
        return sfxVolume;
    }

    /** 设置音效音量（自动夹紧到 0~1） */
    public static void setSfxVolume(double volume) {
        sfxVolume = clamp(volume, 0.0, 1.0);
    }

    /** 钩爪速度倍率（0.5~1.5） */
    public static double getHookSpeedMultiplier() {
        return hookSpeedMultiplier;
    }

    /** 设置钩爪速度倍率（自动夹紧到 0.5~1.5） */
    public static void setHookSpeedMultiplier(double multiplier) {
        hookSpeedMultiplier = clamp(multiplier, MIN_HOOK_SPEED, MAX_HOOK_SPEED);
    }

    /** 数值夹紧到 [min, max] */
    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
