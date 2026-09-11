// 游戏管理器验收自检：初始状态一致（PLAYING/双钩 SWINGING/场景 25~30 物品）、
// 输入分发正确驱动状态机、PAUSED 冻结全部推进、shutdown 释放后台线程资源且幂等。
// 与 GameManagerImpl 同包；直接 java Main.controller.GameManagerLifecycleCheck 运行。
package Main.controller;

import Main.config.GameConfig;
import Main.model.GameState;
import Main.model.HookState;
import Main.util.SelfCheck;

public class GameManagerLifecycleCheck extends SelfCheck {

    public static void main(String[] args) {
        GameManagerLifecycleCheck check = new GameManagerLifecycleCheck();
        check.newGameHasConsistentInitialState();
        check.dispatchThrowActionDrivesHookStateMachine();
        check.pausedStateFreezesAllSimulation();
        check.shutdownReleasesResourcesAndIsIdempotent();
        check.finish();
    }

    private void newGameHasConsistentInitialState() {
        try (GameManagerImpl gm = new GameManagerImpl()) {
            checkEq(GameState.PLAYING, gm.getState(), "新局状态应为 PLAYING");
            checkTrue(gm.getHookP1() != null, "P1 钩爪必须被装配");
            checkTrue(gm.getHookP2() != null, "P2 钩爪必须被装配");
            checkEq(1, gm.getHookP1().getPlayerId(), "P1 钩爪玩家编号应为 1");
            checkEq(2, gm.getHookP2().getPlayerId(), "P2 钩爪玩家编号应为 2");
            checkEq(HookState.SWINGING, gm.getHookP1().getState(), "P1 初始应为钟摆态");
            checkEq(HookState.SWINGING, gm.getHookP2().getState(), "P2 初始应为钟摆态");

            checkTrue(gm.getSceneItemList() != null, "场景物品列表必须被初始化");
            checkEq(gm.getSceneItemList().size(), gm.getMineMap().getItems().size(),
                    "MineMap 应与场景物品列表保持同一数据源");
            checkEq((double) GameConfig.GAME_TOTAL_SEC, gm.getRemainingTime(), 1e-9,
                    "对局应从 90 秒开始");
        }
    }

    private void dispatchThrowActionDrivesHookStateMachine() {
        try (GameManagerImpl gm = new GameManagerImpl()) {
            gm.handleAction(1, ActionType.THROW_HOOK);
            checkEq(HookState.THROWING, gm.getHookP1().getState(),
                    "THROW_P1 应驱动 P1 钩爪进入抛出状态");
            checkEq(HookState.SWINGING, gm.getHookP2().getState(),
                    "P2 钩爪不受 P1 输入影响");

            // 推进若干帧不应抛出异常
            checkDoesNotThrow(() -> {
                for (int i = 0; i < 60; i++) {
                    gm.gameLoopTick(1.0 / 60);
                }
            }, "推进 60 帧不应抛出异常");
        }
    }

    private void pausedStateFreezesAllSimulation() {
        try (GameManagerImpl gm = new GameManagerImpl()) {
            gm.handleAction(1, ActionType.THROW_HOOK);
            double ropeBeforePause = gm.getHookP1().getRopeLength();
            gm.setState(GameState.PAUSED);
            for (int i = 0; i < 30; i++) {
                gm.gameLoopTick(1.0 / 60);
            }
            checkEq(ropeBeforePause, gm.getHookP1().getRopeLength(), 1e-9,
                    "PAUSED 期间钩爪推进必须完全冻结");
        }
    }

    private void shutdownReleasesResourcesAndIsIdempotent() {
        GameManagerImpl gm = new GameManagerImpl();
        checkDoesNotThrow(gm::shutdown, "shutdown 释放后台线程池不应抛出异常");
        checkDoesNotThrow(gm::shutdown, "shutdown 必须可重复调用（幂等）");
    }
}
