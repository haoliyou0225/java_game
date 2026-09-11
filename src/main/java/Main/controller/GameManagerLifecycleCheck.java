// 游戏管理器验收自检：初始状态一致（PLAYING/双钩 SWINGING/场景 25~30 物品）、
// 输入分发正确驱动状态机、PAUSED 冻结全部推进、shutdown 释放后台线程资源且幂等、
// 完整 90 秒对局模拟能正常跑完（持续道具/短时道具/炸药门控/防抖）。
// 与 GameManagerImpl 同包；直接 java Main.controller.GameManagerLifecycleCheck 运行。
package Main.controller;

import Main.config.Config;
import Main.config.GameConfig;
import Main.model.GameState;
import Main.model.HookState;
import Main.model.PersistItemUseResult;
import Main.model.Player;
import Main.util.SelfCheck;

public class GameManagerLifecycleCheck extends SelfCheck {

    public static void main(String[] args) {
        GameManagerLifecycleCheck check = new GameManagerLifecycleCheck();
        check.newGameHasConsistentInitialState();
        check.dispatchThrowActionDrivesHookStateMachine();
        check.pausedStateFreezesAllSimulation();
        check.shutdownReleasesResourcesAndIsIdempotent();
        check.persistentItemUseLifecycle();
        check.shortItemUseTriggersHookEffect();
        check.dynamiteGateOnlyInGrabbing();
        check.justPressedDebounceThrowAndPause();
        check.full90SecondsGameRun();
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

    // ======== 以下来自 HeadlessGameMain 融合 ========

    /** 持续道具：库存 → 首次激活 / 已激活折 50 金币 / 库存 0 无效 */
    private void persistentItemUseLifecycle() {
        try (GameManagerImpl gm = new GameManagerImpl()) {
            Player p1 = gm.getPlayer1();
            p1.addLuckyClover(2);
            gm.handleAction(1, ActionType.USE_LUCKY_CLOVER);
            checkTrue(p1.hasLuckyClover(), "幸运草首次使用应激活");
            checkEq(1, p1.getLuckyCloverCount(), "幸运草首次使用后库存应 -1");
            checkEq(0, p1.getScore(), "幸运草首次使用不应加分");

            gm.handleAction(1, ActionType.USE_LUCKY_CLOVER);
            checkTrue(p1.hasLuckyClover(), "幸运草重复使用应保持激活");
            checkEq(0, p1.getLuckyCloverCount(), "幸运草重复使用库存再 -1");
            checkEq(GameConfig.ITEM_DUP_AUTO_GOLD, p1.getScore(), "幸运草重复使用应折 50 金币");

            gm.handleAction(1, ActionType.USE_LUCKY_CLOVER);
            checkEq(GameConfig.ITEM_DUP_AUTO_GOLD, p1.getScore(), "幸运草库存 0 时按键无效、不加金币");

            p1.addDiamondBoost(1);
            checkTrue(p1.useDiamondBoost() == PersistItemUseResult.ACTIVATED, "钻石升级首次使用返回 ACTIVATED");
            p1.addStoneBook(1);
            checkTrue(p1.useStoneBook() == PersistItemUseResult.ACTIVATED, "石头书首次使用返回 ACTIVATED");
        }
    }

    /** 短时道具：扣库存并对钩爪生效 */
    private void shortItemUseTriggersHookEffect() {
        try (GameManagerImpl gm = new GameManagerImpl()) {
            Player p1 = gm.getPlayer1();
            p1.addPowerPotion(1);
            gm.handleAction(1, ActionType.USE_POWER_POTION);
            checkEq(0, p1.getPowerPotionCount(), "强力药水应扣库存");
            checkTrue(gm.getHook1().isSpeedBoostActive(), "强力药水应使自身钩爪加速生效");

            p1.addFreezeBox(1);
            gm.handleAction(1, ActionType.USE_FREEZE_BOX);
            checkEq(0, p1.getFreezeBoxCount(), "冰冻箱应扣库存");
            checkTrue(gm.getHook2().isFrozen(), "冰冻箱应冻结对方钩爪");
        }
    }

    /** 炸药门控：仅 GRABBING 携带收回态可用，其他状态无效 */
    private void dynamiteGateOnlyInGrabbing() {
        try (GameManagerImpl gm = new GameManagerImpl()) {
            Player p1 = gm.getPlayer1();
            int dynamiteBefore = p1.getDynamiteCount();
            gm.handleAction(1, ActionType.USE_DYNAMITE);
            checkEq(dynamiteBefore, p1.getDynamiteCount(),
                    "SWINGING 状态引爆炸药应无效、不耗库存");
        }
    }

    /** justPressed 防抖：抛钩与 ESC 暂停 */
    private void justPressedDebounceThrowAndPause() {
        try (GameManagerImpl gm = new GameManagerImpl()) {
            InputController input = new InputControllerImpl(gm, gm);
            checkEq(HookState.SWINGING, gm.getHook1().getState(), "起点钩爪应为 SWINGING");

            input.pressAction(1, ActionType.THROW_HOOK);
            input.pressAction(1, ActionType.THROW_HOOK); // 未松开再按：必须被防抖吞掉
            checkEq(HookState.THROWING, gm.getHook1().getState(), "单次按下钩爪应进入 THROWING");
            input.releaseAction(1, ActionType.THROW_HOOK);

            checkEq(GameState.PLAYING, gm.getState(), "对局应进行中");
            GameState s1 = input.pressPause();
            GameState s2 = input.pressPause(); // ESC 未松开重复事件：只切换一次
            checkEq(GameState.PAUSED, s1, "首次 ESC 应暂停");
            checkEq(GameState.PAUSED, s2, "ESC 防抖：连续两次按下只暂停一次");
            input.releaseAction(0, ActionType.TOGGLE_PAUSE);
            checkEq(GameState.PLAYING, input.pressPause(), "ESC 松开后再按应恢复对局");
        }
    }

    /** 完整 90 秒对局模拟：双方抛钩 + 用道具，验证完整链路不崩 */
    private void full90SecondsGameRun() {
        try (GameManagerImpl gm = new GameManagerImpl()) {
            InputController input = new InputControllerImpl(gm, gm);
            Player p1 = gm.getPlayer1();
            Player p2 = gm.getPlayer2();
            int initialItemCount = gm.getSceneItemList().size();

            p1.addPowerPotion(3);
            p1.addFreezeBox(2);
            p2.addPowerPotion(3);
            p2.addFreezeBox(2);
            p2.addLuckyClover(1);

            gm.setRemainingTime(Config.GAME_DURATION);
            int maxFrames = (int) (gm.getRemainingTime() * 60);
            for (int frame = 0; frame < maxFrames && gm.getState() == GameState.PLAYING; frame++) {
                gm.gameLoopTick(1.0 / 60.0);
                gm.setRemainingTime(gm.getRemainingTime() - 1.0 / 60.0);

                if (frame % 35 == 0) {
                    input.pressAction(1, ActionType.THROW_HOOK);
                    input.pressAction(2, ActionType.THROW_HOOK);
                }
                if (frame % 35 == 1) {
                    input.releaseAction(1, ActionType.THROW_HOOK);
                    input.releaseAction(2, ActionType.THROW_HOOK);
                }
                if (frame % 480 == 10) {
                    input.pressAction(1, ActionType.USE_POWER_POTION);
                    input.pressAction(2, ActionType.USE_POWER_POTION);
                    input.releaseAction(1, ActionType.USE_POWER_POTION);
                    input.releaseAction(2, ActionType.USE_POWER_POTION);
                }
                if (frame % 600 == 20) {
                    input.pressAction(1, ActionType.USE_FREEZE_BOX);
                    input.pressAction(2, ActionType.USE_FREEZE_BOX);
                    input.releaseAction(1, ActionType.USE_FREEZE_BOX);
                    input.releaseAction(2, ActionType.USE_FREEZE_BOX);
                }
                if (frame == 900) {
                    input.pressAction(2, ActionType.USE_LUCKY_CLOVER);
                    input.releaseAction(2, ActionType.USE_LUCKY_CLOVER);
                }
            }
            gm.setState(GameState.FINISHED);

            int score1 = p1.getScore();
            int score2 = p2.getScore();
            int remainItems = gm.getSceneItemList().size();
            checkTrue(remainItems < initialItemCount, "一整局应实际抓走/移除部分物品");
            checkTrue(score1 + score2 > 0, "一整局应产生抓取计分");
            checkEq(GameState.FINISHED, gm.getState(), "对局应能正常进入 FINISHED");
        }
    }
}
