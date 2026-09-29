package com.miophas.singularity_iteration.common.recipe;

import com.miophas.singularity_iteration.common.item.reactor.mio_icif_condensator;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/** IC2 GradualRecipe: one condensator and one kind of filler, counted per occupied slot. */
public class mio_icif_CondensatorRepairRecipe extends CustomRecipe {
    public mio_icif_CondensatorRepairRecipe() { super(CraftingBookCategory.MISC); }

    private record Repair(ItemStack stack, mio_icif_condensator item, int amount) {}

    private static Repair findRepair(CraftingInput input) {
        ItemStack condensator = ItemStack.EMPTY;
        int redstone = 0, lapis = 0;
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof mio_icif_condensator) {
                if (!condensator.isEmpty()) return null;
                condensator = stack;
            } else if (stack.is(Items.REDSTONE)) redstone++;
            else if (stack.is(Items.LAPIS_LAZULI)) lapis++;
            else return null;
        }
        // IC2 has separate gradual recipes for each filler, so mixing fillers is invalid.
        if (condensator.isEmpty() || redstone + lapis == 0 || redstone > 0 && lapis > 0) return null;
        var item = (mio_icif_condensator) condensator.getItem();
        int amount = redstone * item.getRedstoneRepairAmount() + lapis * item.getLapisRepairAmount();
        return amount > 0 ? new Repair(condensator, item, amount) : null;
    }

    @Override public boolean matches(CraftingInput input, Level level) { return findRepair(input) != null; }

    @Override public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        Repair repair = findRepair(input);
        if (repair == null) return ItemStack.EMPTY;
        ItemStack result = repair.stack().copyWithCount(1);
        repair.item().removeHeat(result, repair.amount());
        return result;
    }

    @Override public boolean canCraftInDimensions(int width, int height) { return width * height >= 2; }
    @Override public RecipeSerializer<?> getSerializer() { return mio_icif_ModRecipes.CONDENSATOR_REPAIR_SERIALIZER.get(); }
}
