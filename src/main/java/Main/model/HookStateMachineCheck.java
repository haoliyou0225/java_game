// FR-07/FR-16 钩爪状态机验收自检：FROZEN 冰冻期间运动完全暂停且解冻恢复原状态、
// STUNNED 2 秒后自动空钩收回、强力药水收回速度×2 且重复使用仅刷新不叠加。
// 无 JUnit 依赖，直接 java Main.model.HookStateMachineCheck 运行。
package Main.model;

import Main.config.GameConfig;
import Main.util.SelfCheck;

import java.util.List;

public class HookStateMachineCheck extends SelfCheck {

    public static void main(String[] args) {
        HookStateMachineCheck check = new HookStateMachineCheck();
        check.initialStateIsSwinging();
        check.frozenHookPausesCompletelyAndResumesSwinging();
        check.freezeDuringThrowingRestoresThrowing();
        check.repeatedFreezeRefreshesTimerInsteadOfStacking();
        check.stunLastsTwoSecondsThenEntersRetracting();
        check.speedBoostDoublesRetractSpeedAndRefreshesDuration();
        check.finish();
    }

    private HookImpl freshHook() {
        return new HookImpl(1, new RopeImpl(GameConfig.ROPE_MAX_EXTEND_LENGTH));
    }

    private void initialStateIsSwinging() {
        checkEq(HookState.SWINGING, freshHook().getState(), "初始状态应为 SWINGING");
    }

    private void frozenHookPausesCompletelyAndResumesSwinging() {
        HookImpl hook = freshHook();
        hook.freeze(GameConfig.HOOK_FREEZE_DURATION_SEC);

        checkTrue(hook.isFrozen(), "施加冰冻后应处于冰冻状态");
        checkEq(HookState.FROZEN, hook.getState(), "状态应为 FROZEN");
        double angleBefore = hook.getAngle();
        double ropeBefore = hook.getRopeLength();

        // 冰冻中推进 0.5 秒：角度/绳长均不得变化
        hook.update(0.5, List.of(), null);
        checkEq(HookState.FROZEN, hook.getState(), "冰冻未到期应仍为 FROZEN");
        checkEq(angleBefore, hook.getAngle(), 1e-9, "冰冻期间钟摆角度必须冻结");
        checkEq(ropeBefore, hook.getRopeLength(), 1e-9, "冰冻期间绳长必须冻结");

        // 3 秒倒计时结束：恢复冻结前的 SWINGING
        hook.update(3.0, List.of(), null);
        checkEq(HookState.SWINGING, hook.getState(), "冰冻结束应恢复 SWINGING");
        checkFalse(hook.isFrozen(), "冰冻结束后 isFrozen 应为 false");
    }

    private void freezeDuringThrowingRestoresThrowing() {
        HookImpl hook = freshHook();
        hook.throwHook();
        checkEq(HookState.THROWING, hook.getState(), "抛钩后应为 THROWING");

        hook.freeze(0.1);
        checkEq(HookState.FROZEN, hook.getState(), "抛出中被冰冻应为 FROZEN");

        // 冰冻期间抛出运动也必须暂停
        double ropeBefore = hook.getRopeLength();
        hook.update(0.05, List.of(), null);
        checkEq(ropeBefore, hook.getRopeLength(), 1e-9, "冰冻期间绳长必须冻结");

        hook.update(0.2, List.of(), null);
        checkEq(HookState.THROWING, hook.getState(), "解冻后应恢复冻结前的 THROWING 状态");
    }

    private void repeatedFreezeRefreshesTimerInsteadOfStacking() {
        HookImpl hook = freshHook();
        hook.freeze(3.0);
        hook.update(1.0, List.of(), null);
        hook.freeze(3.0); // 再次冰冻仅刷新倒计时
        checkTrue(hook.getFreezeRemaining() > 2.8,
                "重复冰冻应刷新为 3 秒而非叠加，实际剩余 " + hook.getFreezeRemaining());
    }

    private void stunLastsTwoSecondsThenEntersRetracting() {
        HookImpl hook = freshHook();
        hook.stun();
        checkEq(HookState.STUNNED, hook.getState(), "眩晕后应为 STUNNED");

        hook.update(1.9, List.of(), null);
        checkEq(HookState.STUNNED, hook.getState(), "眩晕 2 秒内不得恢复");

        hook.update(0.2, List.of(), null);
        checkEq(HookState.RETRACTING, hook.getState(), "眩晕结束应自动转入空钩收回");
    }

    private void speedBoostDoublesRetractSpeedAndRefreshesDuration() {
        HookImpl hook = freshHook();
        // 抛出 0.5 秒使绳长伸长到约 320px，再转空钩收回
        hook.throwHook();
        hook.update(0.5, List.of(), null);
        hook.retractHook();
        checkEq(HookState.RETRACTING, hook.getState(), "应收回到 RETRACTING");

        // 未用药水：空钩收回 800px/s，0.1 秒应收短 80px
        double before = hook.getRopeLength();
        hook.update(0.1, List.of(), null);
        double normalDelta = before - hook.getRopeLength();
        checkEq(80.0, normalDelta, 1.0, "空钩收回速度应为 800px/s");

        // 使用强力药水：收回速度×2，0.1 秒应收短 160px
        hook.applySpeedBoost(GameConfig.HOOK_SPEED_BOOST_DURATION_SEC);
        checkTrue(hook.isSpeedBoostActive(), "使用药水后应处于加速状态");
        double boostedBefore = hook.getRopeLength();
        hook.update(0.1, List.of(), null);
        double boostedDelta = boostedBefore - hook.getRopeLength();
        checkEq(160.0, boostedDelta, 1.0, "强力药水中收回速度应为 1600px/s");

        // FR-18：重复使用仅刷新为 10 秒，不叠加成 20 秒
        hook.applySpeedBoost(GameConfig.HOOK_SPEED_BOOST_DURATION_SEC);
        checkTrue(hook.getSpeedBoostRemaining() <= GameConfig.HOOK_SPEED_BOOST_DURATION_SEC + 0.01,
                "重复使用药水应刷新时长而非叠加，实际剩余 " + hook.getSpeedBoostRemaining());
    }
}
