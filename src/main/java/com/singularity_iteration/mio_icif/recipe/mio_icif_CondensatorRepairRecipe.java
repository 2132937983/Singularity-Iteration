package com.singularity_iteration.mio_icif.recipe;

import com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_condensator;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * 冷凝模块修复配方
 *
 * 允许使用红石或青金石在工作台中修复冷凝模式? * - 红石冷凝模块：红石修复?0000点耐久
 * - 青金石冷凝模块：红石修复10000点，青金石修复?0000�? */
@SuppressWarnings("null")
public class mio_icif_CondensatorRepairRecipe extends CustomRecipe {

    public mio_icif_CondensatorRepairRecipe() {
        super(null); // 不需要特殊分配
}

    @Override
    public boolean matches(CraftingInput input, Level level) {
        ItemStack condensatorStack = ItemStack.EMPTY;
        int redstoneCount = 0;
        int lapisCount = 0;
        int otherItems = 0;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;

            if (stack.getItem() instanceof mio_icif_condensator) {
                if (!condensatorStack.isEmpty()) {
                    return false; // 只能有一个冷凝模式
            }
                condensatorStack = stack;
            } else if (stack.getItem() == Items.REDSTONE) {
                redstoneCount += stack.getCount();
            } else if (stack.getItem() == Items.LAPIS_LAZULI) {
                lapisCount += stack.getCount();
            } else {
                otherItems++;
            }
        }

        // 必须有一个冷凝模块，且至少有红石或青金石，不能有其他物品
        if (condensatorStack.isEmpty() || (redstoneCount == 0 && lapisCount == 0) || otherItems > 0) {
            return false;
        }

        // 检查冷凝模块是否需要修复
    mio_icif_condensator condensator = (mio_icif_condensator) condensatorStack.getItem();
        return condensator.getStoredHeat(condensatorStack) > 0;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack condensatorStack = ItemStack.EMPTY;
        int redstoneCount = 0;
        int lapisCount = 0;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;

            if (stack.getItem() instanceof mio_icif_condensator) {
                condensatorStack = stack.copy();
            } else if (stack.getItem() == Items.REDSTONE) {
                redstoneCount += stack.getCount();
            } else if (stack.getItem() == Items.LAPIS_LAZULI) {
                lapisCount += stack.getCount();
            }
        }

        if (condensatorStack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        mio_icif_condensator condensator = (mio_icif_condensator) condensatorStack.getItem();

        // 计算总修复量
        int totalRepair = 0;

        // 红石修复
        if (redstoneCount > 0) {
            totalRepair += redstoneCount * condensator.getRedstoneRepairAmount();
        }

        // 青金石修复（仅青金石冷凝模块可用�
    if (lapisCount > 0 && condensator.getLapisRepairAmount() > 0) {
            totalRepair += lapisCount * condensator.getLapisRepairAmount();
        }

        // 执行修复
        if (totalRepair > 0) {
            condensator.removeHeat(condensatorStack, totalRepair);
        }

        return condensatorStack;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2; // 至少需�?个格子（冷凝模块 + 修复材料�
}

    @Override
    public RecipeSerializer<?> getSerializer() {
        return mio_icif_ModRecipes.CONDENSATOR_REPAIR_SERIALIZER.get();
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        // 修复配方消耗所有材料，没有剩余物品
        return NonNullList.withSize(input.size(), ItemStack.EMPTY);
    }
}


