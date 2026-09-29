package com.miophas.singularity_iteration.common.recipe.charge_carrying;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.List;

public class ChargeCarryingShapelessRecipe implements CraftingRecipe {

    private final String group;
    private final List<Ingredient> ingredients;
    private final ItemStack result;
    private final boolean transferCharge;

    public ChargeCarryingShapelessRecipe(String group, List<Ingredient> ingredients, ItemStack result, boolean transferCharge) {
        this.group = group;
        this.ingredients = ingredients;
        this.result = result;
        this.transferCharge = transferCharge;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        java.util.List<ItemStack> available = new java.util.ArrayList<>();
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty()) {
                available.add(stack.copy());
            }
        }
        for (Ingredient ingredient : this.ingredients) {
            boolean matched = false;
            for (int j = 0; j < available.size(); j++) {
                if (ingredient.test(available.get(j))) {
                    available.remove(j);
                    matched = true;
                    break;
                }
            }
            if (!matched) return false;
        }
        return available.isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack ret = this.result.copy();
        if (this.transferCharge) {
            long totalCharge = ChargeCarryingRecipe.collectInputCharge(input);
            ChargeCarryingRecipe.chargeResult(ret, totalCharge);
        }
        return ret;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= this.ingredients.size();
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
        return ChargeCarryingRecipes.CHARGE_CARRYING_SHAPELESS_SERIALIZER.get();
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
        NonNullList<Ingredient> list = NonNullList.create();
        list.addAll(this.ingredients);
        return list;
    }

    @Override
    public boolean isSpecial() {
        return false;
    }

    public List<Ingredient> getIngredientList() {
        return this.ingredients;
    }

    public ItemStack getResult() {
        return this.result;
    }

    public boolean shouldTransferCharge() {
        return this.transferCharge;
    }
}