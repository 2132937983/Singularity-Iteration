// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit.core;

import com.miophas.singularity_iteration.common.suit.SuitRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * One legacy shell alone in the crafting grid turns into its conversion (0.1.9.0), with all its
 * components. For shells kept in chests or machines, which do not tick in a player inventory.
 */
public class LegacyConversionRecipe extends CustomRecipe {
    public LegacyConversionRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return single(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack legacy = single(input);
        if (legacy == null) return ItemStack.EMPTY;
        LegacyArmor.Result result = LegacyArmor.convert(legacy);
        return result == null ? ItemStack.EMPTY : result.stack();
    }

    private static ItemStack single(CraftingInput input) {
        ItemStack found = null;
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.isEmpty()) continue;
            if (found != null || !LegacyArmor.isLegacy(s)) return null;
            found = s;
        }
        return found;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SuitRegistry.LEGACY_CONVERSION.get();
    }
}
