// FR-UI LogUtils：统一控制台日志 [FR-18] HH:mm:ss.SSS 动作描述，来自 feature_ui 分支
package Main.util;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * 统一控制台日志工具。
 * 所有调试日志统一为 [FR-18] HH:mm:ss.SSS 动作描述 格式，
 * 毫秒时间戳用于验证两名玩家的操作互不阻塞、收回各自独立计时。
 */
public final class LogUtils {

    /** 毫秒级时间戳格式 */
    private static final DateTimeFormatter TS_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private LogUtils() {
        // 工具类禁止实例化
    }

    /**
     * 生成统一格式的日志行。
     *
     * @param action 动作描述文本（如 "玩家1 释放钩爪"）
     * @return 带毫秒时间戳的完整日志行，形如 [FR-18] 12:00:00.123 玩家1 释放钩爪
     */
    public static String format(String action) {
        String ts = LocalTime.now().format(TS_FORMAT);
        return "[FR-18] " + ts + " " + action;
    }
}
