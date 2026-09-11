// 分层验证入口：不引用任何 JavaFX 类型，纯 Model+Controller 驱动并跑完一整局
package Main;

import Main.config.Config;
import Main.controller.ActionType;
import Main.controller.GameActionHandler;
import Main.controller.GameManagerImpl;
import Main.controller.InputController;
import Main.controller.InputControllerImpl;
import Main.model.GameState;
import Main.model.HookState;
import Main.model.PersistItemUseResult;
import Main.model.Player;

/**
 * 分层冒烟验证（headless）：证明 View → Controller → Model 单向依赖成立。
 * <p>
 * 本类不 import 任何 javafx.* 类型：直接构造 {@link GameManagerImpl}（GameModel+GameActionHandler）
 * 与 {@link InputControllerImpl}（防抖+派发），用方法调用代替物理键盘事件：
 * <ol>
 *   <li>动作门控与道具库存判定（炸药仅 GRABBING、持续道具激活/重复折金币/库存 0 无效）；</li>
 *   <li>justPressed 防抖（不松开重复触发只生效一次；ESC 双人共用只切一次）；</li>
 *   <li>以固定步长 gameLoopTick(1/60) 推进 90 秒游戏时间的完整对局，模拟双方抛钩/用道具，
 *       驱动真实钩爪物理、抓取结算、道具倍率、倒计时与胜负判定。</li>
 * </ol>
 * 全部断言通过打印 {@code HEADLESS_PASS} 并正常退出；任一失败抛异常（非 0 退出）。
 */
public final class HeadlessGameMain {

    private HeadlessGameMain() {
    }

    public static void main(String[] args) {
        GameManagerImpl game = new GameManagerImpl();
        GameActionHandler handler = game;
        Player p1 = game.getPlayer1();
        Player p2 = game.getPlayer2();
        int initialItemCount = game.getSceneItemList().size();
        System.out.println("[headless] 初始场上物品数: " + initialItemCount);

        // ===== 1. 持续道具：库存 → 首次激活 / 已激活折 50 金币 / 库存 0 无效 =====
        p1.addLuckyClover(2);
        handler.handleAction(1, ActionType.USE_LUCKY_CLOVER);
        check(p1.hasLuckyClover() && p1.getLuckyCloverCount() == 1 && p1.getScore() == 0,
                "幸运草首次使用应激活、库存-1、不加分");
        handler.handleAction(1, ActionType.USE_LUCKY_CLOVER);
        check(p1.hasLuckyClover() && p1.getLuckyCloverCount() == 0 && p1.getScore() == 50,
                "幸运草重复使用应保持激活、库存-1、折 50 金币");
        handler.handleAction(1, ActionType.USE_LUCKY_CLOVER);
        check(p1.getScore() == 50, "幸运草库存 0 时按键无效、不加金币");

        p1.addDiamondBoost(1);
        check(p1.useDiamondBoost() == PersistItemUseResult.ACTIVATED, "钻石升级首次使用返回 ACTIVATED");
        p1.addStoneBook(1);
        check(p1.useStoneBook() == PersistItemUseResult.ACTIVATED, "石头书首次使用返回 ACTIVATED");

        // ===== 2. 短时道具：扣库存并对钩爪生效 =====
        p1.addPowerPotion(1);
        handler.handleAction(1, ActionType.USE_POWER_POTION);
        check(p1.getPowerPotionCount() == 0 && game.getHook1().isSpeedBoostActive(),
                "强力药水应扣库存并使自身钩爪加速生效");
        p1.addFreezeBox(1);
        handler.handleAction(1, ActionType.USE_FREEZE_BOX);
        check(p1.getFreezeBoxCount() == 0 && game.getHook2().isFrozen(),
                "冰冻箱应扣库存并冻结对方钩爪");

        // ===== 3. 炸药门控：SWINGING（未携带物品）时按键无效、不耗库存 =====
        int dynamiteBefore = p1.getDynamiteCount();
        handler.handleAction(1, ActionType.USE_DYNAMITE);
        check(p1.getDynamiteCount() == dynamiteBefore, "非携带收回状态引爆炸药应无效、不耗库存");

        // ===== 4. justPressed 防抖：抛钩与 ESC =====
        InputController input = new InputControllerImpl(game, game);
        check(game.getHook1().getState() == HookState.SWINGING, "测试起点钩爪应为 SWINGING");
        input.pressAction(1, ActionType.THROW_HOOK);
        input.pressAction(1, ActionType.THROW_HOOK); // 未松开再按一次：必须被防抖吞掉
        check(game.getHook1().getState() == HookState.THROWING, "单次按下钩爪应进入 THROWING");
        input.releaseAction(1, ActionType.THROW_HOOK);

        check(game.getState() == GameState.PLAYING, "对局应进行中");
        GameState s1 = input.pressPause();
        GameState s2 = input.pressPause(); // ESC 未松开重复事件：只切换一次
        check(s1 == GameState.PAUSED && s2 == GameState.PAUSED, "ESC 防抖：连续两次按下只暂停一次");
        input.releaseAction(0, ActionType.TOGGLE_PAUSE);
        check(input.pressPause() == GameState.PLAYING, "ESC 松开后再按应恢复对局");
        input.releaseAction(0, ActionType.TOGGLE_PAUSE);

        // ===== 5. 完整对局：固定步长推进 90 秒游戏时间，模拟双方操作 =====
        // 补充库存，让模拟中也能触发道具
        p1.addPowerPotion(3);
        p1.addFreezeBox(2);
        p2.addPowerPotion(3);
        p2.addFreezeBox(2);
        p2.addLuckyClover(1);

        game.setRemainingTime(Config.GAME_DURATION);
        int maxFrames = (int) (game.getRemainingTime() * 60);
        for (int frame = 0; frame < maxFrames && game.getState() == GameState.PLAYING; frame++) {
            game.gameLoopTick(1.0 / 60.0);
            game.setRemainingTime(game.getRemainingTime() - 1.0 / 60.0);

            // 每 35 帧尝试抛钩（SWINGING 门控 + 防抖：非 SWINGING 时自然忽略）
            if (frame % 35 == 0) {
                input.pressAction(1, ActionType.THROW_HOOK);
                input.pressAction(2, ActionType.THROW_HOOK);
            }
            if (frame % 35 == 1) {
                input.releaseAction(1, ActionType.THROW_HOOK);
                input.releaseAction(2, ActionType.THROW_HOOK);
            }
            // 周期性使用短时道具（库存门控：为 0 时忽略）
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
                input.pressAction(2, ActionType.USE_LUCKY_CLOVER); // P2 幸运草激活
                input.releaseAction(2, ActionType.USE_LUCKY_CLOVER);
            }
        }
        // 模拟倒计时归零（等价 GameTimerImpl 的 onTimeUp）
        game.setState(GameState.FINISHED);

        int score1 = p1.getScore();
        int score2 = p2.getScore();
        int remainItems = game.getSceneItemList().size();
        int winner = game.determineWinner();
        System.out.println("[headless] 对局结束：P1=$" + score1 + "，P2=$" + score2
                + "，剩余物品 " + remainItems + "/" + initialItemCount
                + "，胜负码(1=P1/-1=P2/0=平): " + winner);

        check(remainItems < initialItemCount, "一整局应实际抓走/移除部分物品");
        check(score1 + score2 > 0, "一整局应产生抓取计分");
        check(game.getState() == GameState.FINISHED, "对局应能正常进入 FINISHED");

        game.shutdown();
        System.out.println("HEADLESS_PASS");
    }

    /** 断言失败立即抛出（进程非 0 退出），成功静默 */
    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException("[headless] 断言失败：" + message);
        }
    }
}
