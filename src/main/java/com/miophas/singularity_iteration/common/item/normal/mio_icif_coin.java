package com.miophas.singularity_iteration.common.item.normal;

import com.miophas.singularity_iteration.common.item.LargeStackItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 货币物品
 * 支持大堆叠（9999个）
 *
 * <p>0.1.10.16: the 9,999 stack comes from {@link #getMaxStackSize(ItemStack)} and
 * {@link LargeStackItem}; the {@code max_stack_size} component stays inside the vanilla range (99), so no
 * global mixin is needed for it any more. Containers and slots with the vanilla default limit accept
 * 9,999 coins; every other item keeps the vanilla rules ({@link com.miophas.singularity_iteration.common.item.LargeStacks}).
 */
@SuppressWarnings("null")
public class mio_icif_coin extends Item implements LargeStackItem {

    public static final int MAX_STACK_SIZE = 9999;

    public mio_icif_coin(Properties properties) {
        super(properties.stacksTo(com.miophas.singularity_iteration.common.item.LargeStacks.VANILLA_LIMIT));
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return MAX_STACK_SIZE;
    }

    @Override
    public int largeStackLimit() {
        return MAX_STACK_SIZE;
    }
}
