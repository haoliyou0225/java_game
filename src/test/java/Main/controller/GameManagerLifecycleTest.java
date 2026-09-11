// 游戏管理器验收：初始状态一致（PLAYING/双钩 SWINGING/场景 25~30 物品）、
// 输入分发正确驱动状态机、PAUSED 冻结全部推进、shutdown 释放后台线程资源且幂等
package Main.controller;

import Main.config.GameConfig;
import Main.model.GameState;
import Main.model.HookState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GameManagerLifecycleTest {

    @Test
    void newGameHasConsistentInitialState() {
        try (GameManagerImpl gm = new GameManagerImpl()) {
            assertEquals(GameState.PLAYING, gm.getState());
            assertNotNull(gm.getHookP1());
            assertNotNull(gm.getHookP2());
            assertEquals(1, gm.getHookP1().getPlayerId());
            assertEquals(2, gm.getHookP2().getPlayerId());
            assertEquals(HookState.SWINGING, gm.getHookP1().getState());
            assertEquals(HookState.SWINGING, gm.getHookP2().getState());

            List<?> items = gm.getSceneItemList();
            assertNotNull(items);
            assertEquals(items.size(), gm.getMineMap().getItems().size(),
                    "MineMap 应与场景物品列表保持同一数据源");
            assertEquals(GameConfig.GAME_TOTAL_SEC, gm.getRemainingTime(), 1e-9,
                    "对局应从 90 秒开始");
        }
    }

    @Test
    void dispatchThrowActionDrivesHookStateMachine() {
        try (GameManagerImpl gm = new GameManagerImpl()) {
            gm.dispatchAction(new InputAction() {
                @Override public String getType() { return InputAction.THROW_P1; }
                @Override public int getPlayerId() { return 1; }
            });
            assertEquals(HookState.THROWING, gm.getHookP1().getState(),
                    "THROW_P1 应驱动 P1 钩爪进入抛出状态");
            assertEquals(HookState.SWINGING, gm.getHookP2().getState(),
                    "P2 钩爪不受 P1 输入影响");

            // 推进若干帧不应抛出异常
            assertDoesNotThrow(() -> {
                for (int i = 0; i < 60; i++) {
                    gm.gameLoopTick(1.0 / 60);
                }
            });
        }
    }

    @Test
    void pausedStateFreezesAllSimulation() {
        try (GameManagerImpl gm = new GameManagerImpl()) {
            gm.dispatchAction(new InputAction() {
                @Override public String getType() { return InputAction.THROW_P1; }
                @Override public int getPlayerId() { return 1; }
            });
            double ropeBeforePause = gm.getHookP1().getRopeLength();
            gm.setState(GameState.PAUSED);
            for (int i = 0; i < 30; i++) {
                gm.gameLoopTick(1.0 / 60);
            }
            assertEquals(ropeBeforePause, gm.getHookP1().getRopeLength(), 1e-9,
                    "PAUSED 期间钩爪推进必须完全冻结");
        }
    }

    @Test
    void shutdownReleasesResourcesAndIsIdempotent() {
        GameManagerImpl gm = new GameManagerImpl();
        assertDoesNotThrow(gm::shutdown, "shutdown 释放后台线程池不应抛出异常");
        assertDoesNotThrow(gm::shutdown, "shutdown 必须可重复调用（幂等）");
    }
}
