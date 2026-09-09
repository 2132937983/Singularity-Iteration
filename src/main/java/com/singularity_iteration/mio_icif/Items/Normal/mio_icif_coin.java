package com.singularity_iteration.mio_icif.Items.Normal;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 货币物品
 * 支持大堆叠（9999个）
 */
@SuppressWarnings("null")
public class mio_icif_coin extends Item {
    
    public static final int MAX_STACK_SIZE = 9999;
    
    public mio_icif_coin(Properties properties) {
        super(properties.stacksTo(MAX_STACK_SIZE));
    }
    
    @Override
    public int getMaxStackSize(ItemStack stack) {
        return MAX_STACK_SIZE;
    }
}


