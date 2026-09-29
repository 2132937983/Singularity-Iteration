package com.miophas.singularity_iteration.common.recipe.charge_carrying;

import com.miophas.singularity_iteration.core.api.item.IBatteryItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.Level;

public class ChargeCarryingRecipe implements CraftingRecipe {

    private final String group;
    private final ShapedRecipePattern pattern;
    private final ItemStack result;
    private final boolean transferCharge;

    public ChargeCarryingRecipe(String group, ShapedRecipePattern pattern, ItemStack result, boolean transferCharge) {
        this.group = group;
        this.pattern = pattern;
        this.result = result;
        this.transferCharge = transferCharge;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return this.pattern.matches(input);
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack ret = this.result.copy();
        if (this.transferCharge) {
            long totalCharge = collectInputCharge(input);
            chargeResult(ret, totalCharge);
        }
        return ret;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= this.pattern.width() && height >= this.pattern.height();
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.result;
    }

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.MISC;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ChargeCarryingRecipes.CHARGE_CARRYING_SERIALIZER.get();
    }

    @Override
    public String getGroup() {
        return this.group;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        // 电池作为普通合成材料被消耗（电量已通过 chargeResult 转移到产物），不保留在合成格中
        NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.hasCraftingRemainingItem()) {
                remaining.set(i, stack.getCraftingRemainingItem());
            }
        }
        return remaining;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return this.pattern.ingredients();
    }

    @Override
    public boolean isSpecial() {
        return false;
    }

    public ShapedRecipePattern getPattern() {
        return this.pattern;
    }

    public ItemStack getResult() {
        return this.result;
    }

    public boolean shouldTransferCharge() {
        return this.transferCharge;
    }

    public static long collectInputCharge(CraftingInput input) {
        long totalCharge = 0L;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof IBatteryItem battery) {
                totalCharge += battery.getEnergy(stack);
            }
        }
        return totalCharge;
    }

    public static void chargeResult(ItemStack result, long charge) {
        if (result.isEmpty()) return;
        if (result.getItem() instanceof IBatteryItem battery) {
            battery.setEnergy(result, 0);
            if (charge > 0) {
                battery.addEnergy(result, charge);
            }
        }
    }
}