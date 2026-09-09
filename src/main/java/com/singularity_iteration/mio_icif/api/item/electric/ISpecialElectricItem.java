package com.singularity_iteration.mio_icif.api.item.electric;

import net.minecraft.world.item.ItemStack;

/**
 * 特殊电力物品接口。
 * <p>
 * 对应 IC2 1.12.2 的 {@code ISpecialElectricItem}。
 * 实现此接口的物品可以提供自定义的 {@link IElectricItemManager}，
 * 用于实现特殊的充放电逻辑。
 *
 * @deprecated 此 IC2 兼容 API 已不再需要。计划在下一个主要版本中移除。
 */
@Deprecated(since = "1.21.1", forRemoval = true)
public interface ISpecialElectricItem {

    /**
     * 获取此物品的自定义管理器。
     *
     * @param stack 物品堆
     * @return 自定义管理器，不应返回 null
     */
    @Deprecated(since = "1.21.1", forRemoval = true)
    IElectricItemManager getManager(ItemStack stack);
}