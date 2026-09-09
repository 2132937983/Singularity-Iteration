package com.singularity_iteration.mio_icif.recipe.canner.mix;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * 混合模式配方输入
 * 包装输入物品、材料物品和流体
 */
public record MixRecipeInput(ItemStack input, ItemStack material, FluidStack inputFluid) implements RecipeInput {

    @Override
    public ItemStack getItem(int slot) {
        return switch (slot) {
            case 0 -> this.input;
            case 1 -> this.material;
            default -> throw new IllegalArgumentException("No item for slot " + slot);
        };
    }

    @Override
    public int size() {
        return 2;
    }

    /**
     * 获取输入流体
     */
    public FluidStack inputFluid() {
        return inputFluid;
    }

    /**
     * 检查是否有流体
     */
    public boolean hasFluid() {
        return !inputFluid.isEmpty();
    }
}


