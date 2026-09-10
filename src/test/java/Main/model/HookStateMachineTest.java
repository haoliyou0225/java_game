// FR-07/FR-16 钩爪状态机验收：FROZEN 冰冻期间运动完全暂停且解冻恢复原状态、
// STUNNED 2 秒后自动空钩收回、强力药水收回速度×2 且重复使用仅刷新不叠加
package Main.model;

import Main.config.GameConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HookStateMachineTest {

    private HookImpl hook;

    @BeforeEach
    void setUp() {
        hook = new HookImpl(1, new RopeImpl(GameConfig.ROPE_MAX_EXTEND_LENGTH));
    }

    @Test
    void initialStateIsSwinging() {
        assertEquals(HookState.SWINGING, hook.getState());
    }

    @Test
    void frozenHookPausesCompletelyAndResumesSwinging() {
        hook.freeze(GameConfig.HOOK_FREEZE_DURATION_SEC);

        assertTrue(hook.isFrozen());
        assertEquals(HookState.FROZEN, hook.getState());
        double angleBefore = hook.getAngle();
        double ropeBefore = hook.getRopeLength();

        // 冰冻中推进 0.5 秒：角度/绳长均不得变化
        hook.update(0.5, List.of(), null);
        assertEquals(HookState.FROZEN, hook.getState());
        assertEquals(angleBefore, hook.getAngle(), 1e-9, "冰冻期间钟摆角度必须冻结");
        assertEquals(ropeBefore, hook.getRopeLength(), 1e-9, "冰冻期间绳长必须冻结");

        // 3 秒倒计时结束：恢复冻结前的 SWINGING
        hook.update(3.0, List.of(), null);
        assertEquals(HookState.SWINGING, hook.getState());
        assertFalse(hook.isFrozen());
    }

    @Test
    void freezeDuringThrowingRestoresThrowing() {
        hook.throwHook();
        assertEquals(HookState.THROWING, hook.getState());

        hook.freeze(0.1);
        assertEquals(HookState.FROZEN, hook.getState());

        // 冰冻期间抛出运动也必须暂停
        double ropeBefore = hook.getRopeLength();
        hook.update(0.05, List.of(), null);
        assertEquals(ropeBefore, hook.getRopeLength(), 1e-9, "冰冻期间绳长必须冻结");

        hook.update(0.2, List.of(), null);
        assertEquals(HookState.THROWING, hook.getState(), "解冻后应恢复冻结前的 THROWING 状态");
    }

    @Test
    void repeatedFreezeRefreshesTimerInsteadOfStacking() {
        hook.freeze(3.0);
        hook.update(1.0, List.of(), null);
        hook.freeze(3.0); // 再次冰冻仅刷新倒计时
        assertTrue(hook.getFreezeRemaining() > 2.8,
                "重复冰冻应刷新为 3 秒而非叠加，实际剩余 " + hook.getFreezeRemaining());
    }

    @Test
    void stunLastsTwoSecondsThenEntersRetracting() {
        hook.stun();
        assertEquals(HookState.STUNNED, hook.getState());

        hook.update(1.9, List.of(), null);
        assertEquals(HookState.STUNNED, hook.getState(), "眩晕 2 秒内不得恢复");

        hook.update(0.2, List.of(), null);
        assertEquals(HookState.RETRACTING, hook.getState(), "眩晕结束应自动转入空钩收回");
    }

    @Test
    void speedBoostDoublesRetractSpeedAndRefreshesDuration() {
        // 抛出 0.5 秒使绳长伸长到约 320px，再转空钩收回
        hook.throwHook();
        hook.update(0.5, List.of(), null);
        hook.retractHook();
        assertEquals(HookState.RETRACTING, hook.getState());

        // 未用药水：空钩收回 800px/s，0.1 秒应收短 80px
        double before = hook.getRopeLength();
        hook.update(0.1, List.of(), null);
        double normalDelta = before - hook.getRopeLength();
        assertEquals(80.0, normalDelta, 1.0, "空钩收回速度应为 800px/s");

        // 使用强力药水：收回速度×2，0.1 秒应收短 160px
        hook.applySpeedBoost(GameConfig.HOOK_SPEED_BOOST_DURATION_SEC);
        assertTrue(hook.isSpeedBoostActive());
        double boostedBefore = hook.getRopeLength();
        hook.update(0.1, List.of(), null);
        double boostedDelta = boostedBefore - hook.getRopeLength();
        assertEquals(160.0, boostedDelta, 1.0, "强力药水中收回速度应为 1600px/s");

        // FR-18：重复使用仅刷新为 10 秒，不叠加成 20 秒
        hook.applySpeedBoost(GameConfig.HOOK_SPEED_BOOST_DURATION_SEC);
        assertTrue(hook.getSpeedBoostRemaining() <= GameConfig.HOOK_SPEED_BOOST_DURATION_SEC + 0.01,
                "重复使用药水应刷新时长而非叠加，实际剩余 " + hook.getSpeedBoostRemaining());
    }
}
