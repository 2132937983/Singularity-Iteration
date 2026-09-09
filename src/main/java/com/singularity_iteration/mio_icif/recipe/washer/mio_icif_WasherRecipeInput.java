package com.singularity_iteration.mio_icif.recipe.washer;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/**
 * 洗矿配方输入包装置? * 包装单个物品作为洗矿配方的输�? */
public record mio_icif_WasherRecipeInput(ItemStack item) implements RecipeInput {

    /**
     * 获取输入物品
     */
    public ItemStack item() {
        return item;
    }

    /**
     * 获取输入物品数量
     */
    public int count() {
        return item.getCount();
    }

    @Override
    public ItemStack getItem(int i) {
        return item;
    }

    @Override
    public int size() {
        return 1;
    }

    /**
     * 检查输入是否为�?     */
    public boolean isEmpty() {
        return item.isEmpty();
    }
}


