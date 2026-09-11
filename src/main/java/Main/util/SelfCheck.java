// 无框架自检基类：替代 JUnit 的最小断言工具，供 main 源码树内的验收自检类使用。
// 用法：自检类继承本类，在 main 方法中执行各 checkXxx 后调用 finish()；
// 任一断言失败抛出 AssertionError 并打印失败项，全部通过则输出通过条数。
package Main.util;

import java.util.Objects;

public abstract class SelfCheck {

    /** 已通过断言计数 */
    private int passed;

    protected void checkTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
        passed++;
    }

    protected void checkFalse(boolean condition, String message) {
        checkTrue(!condition, message);
    }

    protected void checkEq(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(message + " ==> expected: <" + expected + "> but was: <" + actual + ">");
        }
        passed++;
    }

    protected void checkEq(double expected, double actual, double delta, String message) {
        if (Double.isNaN(actual) || Math.abs(expected - actual) > delta) {
            throw new AssertionError(message + " ==> expected: <" + expected + "> but was: <" + actual + ">");
        }
        passed++;
    }

    protected void checkDoesNotThrow(Runnable action, String message) {
        try {
            action.run();
        } catch (Throwable t) {
            throw new AssertionError(message, t);
        }
        passed++;
    }

    /** 全部检查完成时输出结果 */
    protected void finish() {
        System.out.println(getClass().getName() + ": all " + passed + " checks passed");
    }
}
