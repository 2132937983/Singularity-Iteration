// SCEX 2026-09-12: repaired malformed UTF-8 bytes in comments only.
package com.miophas.singularity_iteration.common.recipe.canner.canning;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/**
 * 装罐模式配方输入
 * 包装输入槽物品和材料槽物??? */
public record CanningRecipeInput(ItemStack inputCan, ItemStack material) implements RecipeInput {

    @Override
    public ItemStack getItem(int slot) {
        return switch (slot) {
            case 0 -> this.inputCan;
            case 1 -> this.material;
            default -> throw new IllegalArgumentException("No item for slot " + slot);
        };
    }

    @Override
    public int size() {
        return 2;
    }
}


