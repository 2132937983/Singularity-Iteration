package com.singularity_iteration.mio_icif.api.item;

/**
 * 扳手物品标记接口。
 * <p>
 * 实现此接口的物品可以被识别为扳手工具，
 * 用于旋转方块朝向或拆卸机器。
 * <p>
 * 注意：此接口为标记接口，具体行为由物品实现类定义。
 * 方块通过 {@link com.singularity_iteration.mio_icif.api.tool.IWrenchable} 接口
 * 来声明自己可以被扳手交互。
 */
public interface IWrenchItem {
}
