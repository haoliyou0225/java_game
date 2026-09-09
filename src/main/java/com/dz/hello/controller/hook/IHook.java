package com.dz.hello.controller.hook;

/**
 * 钩子接口：由控制器层（controller）的钩子实现类实现。
 * 模型层（model.entity）的物品在被抓取时通过 {@code onGrab(IHook hook)}
 * 回调拿到抓取它的钩子实例，以此与控制器层解耦。
 *
 * <p>当前为最小契约，仅作为类型标识；钩子的具体行为
 * （摆动、伸出、收回、抓取/释放物品等）由控制器层补充。</p>
 */
public interface IHook {
}
