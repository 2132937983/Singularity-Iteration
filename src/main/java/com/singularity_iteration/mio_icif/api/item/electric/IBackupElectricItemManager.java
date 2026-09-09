package com.singularity_iteration.mio_icif.api.item.electric;

import net.minecraft.world.item.ItemStack;

/**
 * 备用电力物品管理器接口。
 * <p>
 * 对应 IC2 1.12.2 的 {@code IBackupElectricItemManager}。
 * 允许不实现 {@link IElectricItem} 接口的物品参与电力系统。
 * <p>
 * 实现此接口的管理器可以处理任意物品，只要 {@link #handles(ItemStack)} 返回 true。
 * 这对于集成其他模组的电池物品非常有用。
 *
 * @deprecated 此 IC2 兼容 API 已不再需要。计划在下一个主要版本中移除。
 */
@Deprecated(since = "1.21.1", forRemoval = true)
public interface IBackupElectricItemManager extends IElectricItemManager {

    /**
     * 检查此管理器是否处理指定的物品。
     *
     * @param stack 物品堆
     * @return 如果此管理器可以处理该物品则返回 true
     */
    @Deprecated(since = "1.21.1", forRemoval = true)
    boolean handles(ItemStack stack);
}