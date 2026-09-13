// FR-UI AudioManager：背景音乐循环播放与音效播放的统一入口，音量随 GameSettings 实时调整
package Main.view;

import Main.config.GameSettings;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * 游戏音频统一管理器（单例）。
 * <p>
 * 职责划分：
 * <ul>
 *   <li>背景音乐：{@code /music/backMusic.mp3} 经 {@link MediaPlayer} 循环播放，
 *       音量由 {@link GameSettings#getMusicVolume()} 控制；</li>
 *   <li>音效：短音频经 {@link AudioClip} 播放（如 Select/largegold/bomb/finish），
 *       首次播放时惰性加载并缓存，音量由 {@link GameSettings#getSfxVolume()} 控制。</li>
 * </ul>
 * <p>
 * 本类依赖 JavaFX media 模块，仅允许 View 层调用（Model/Controller 不依赖 JavaFX）；
 * 所有音频资源缺失或加载失败时静默降级，不影响游戏主流程。
 */
public final class AudioManager {

    /** 全局唯一实例 */
    private static final AudioManager INSTANCE = new AudioManager();

    /** 背景音乐播放器（null 表示未加载成功） */
    private MediaPlayer musicPlayer;

    /** 音效缓存：文件名（无扩展名） → 已加载的 AudioClip */
    private final Map<String, AudioClip> sfxClips = new HashMap<>();

    private AudioManager() {
        // 单例禁止外部实例化
    }

    /** 获取全局唯一实例 */
    public static AudioManager get() {
        return INSTANCE;
    }

    /**
     * 启动背景音乐（幂等：已启动则直接返回）。
     * 加载失败（资源缺失/格式不支持）时静默跳过，游戏无音乐也能正常运行。
     */
    public void startMusic() {
        if (musicPlayer != null) {
            return;
        }
        try {
            URL url = getClass().getResource("/music/backMusic.mp3");
            if (url == null) {
                return;
            }
            musicPlayer = new MediaPlayer(new Media(url.toExternalForm()));
            musicPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            musicPlayer.setVolume(GameSettings.getMusicVolume());
            musicPlayer.play();
        } catch (Exception ignored) {
            musicPlayer = null; // 加载失败后允许下次重试
        }
    }

    /**
     * 实时应用音乐音量（设置面板拖动滑块时调用）。
     *
     * @param volume 音量 0~1
     */
    public void applyMusicVolume(double volume) {
        if (musicPlayer != null) {
            musicPlayer.setVolume(volume);
        }
    }

    /**
     * 实时应用音效音量：同时更新已加载的全部音效（未加载的惰性加载时取当前值）。
     *
     * @param volume 音量 0~1
     */
    public void applySfxVolume(double volume) {
        for (AudioClip clip : sfxClips.values()) {
            clip.setVolume(volume);
        }
    }

    /**
     * 播放音效（按文件名，不带 .mp3 扩展名，如 "Select"、"largegold"）。
     * 音量为 0 时直接跳过；资源缺失或加载失败静默降级。
     *
     * @param name 音效文件名（无扩展名）
     */
    public void playSfx(String name) {
        if (GameSettings.getSfxVolume() <= 0) {
            return;
        }
        AudioClip clip = sfxClips.computeIfAbsent(name, this::loadClip);
        if (clip != null) {
            clip.play();
        }
    }

    /** 惰性加载音效：资源缺失/格式不支持时返回 null（不缓存，下次重试加载） */
    private AudioClip loadClip(String name) {
        try {
            URL url = getClass().getResource("/music/" + name + ".mp3");
            if (url == null) {
                return null;
            }
            AudioClip clip = new AudioClip(url.toExternalForm());
            clip.setVolume(GameSettings.getSfxVolume());
            return clip;
        } catch (Exception ignored) {
            return null;
        }
    }
}
