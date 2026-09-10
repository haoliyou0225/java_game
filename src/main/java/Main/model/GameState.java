// FR-UI GameState：UI 对局状态枚举（READY/PLAYING/PAUSED/FINISHED），来自 feature_ui 分支
package Main.model;

/**
 * UI 层对局状态枚举（feature_ui 分支）。
 * 与 hook 分支的 GameStage（READY/PLAYING/GAME_OVER）共存：
 * GameStage 服务钩子玩法循环，GameState 服务界面流程（含暂停 PAUSED）。
 */
public enum GameState {
    READY,
    PLAYING,
    PAUSED,
    FINISHED
}
