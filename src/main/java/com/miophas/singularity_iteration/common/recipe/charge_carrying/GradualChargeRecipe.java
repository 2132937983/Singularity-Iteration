package com.miophas.singularity_iteration.common.recipe.charge_carrying;

import com.miophas.singularity_iteration.core.api.item.IBatteryItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class GradualChargeRecipe implements CraftingRecipe {

    private final String group;
    private final Item targetItem;
    private final Ingredient chargeMaterial;
    private final long chargePerMaterial;

    public GradualChargeRecipe(String group, Item targetItem, Ingredient chargeMaterial, long chargePerMaterial) {
        this.group = group;
        this.targetItem = targetItem;
        this.chargeMaterial = chargeMaterial;
        this.chargePerMaterial = chargePerMaterial;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        boolean foundTarget = false;
        int chargeMats = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() == this.targetItem && !foundTarget) {
                foundTarget = true;
            } else if (this.chargeMaterial.test(stack)) {
                chargeMats++;
            } else {
                return false;
            }
        }
        return foundTarget && chargeMats > 0;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack target = null;
        int chargeMats = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() == this.targetItem && target == null) {
                target = stack;
            } else if (this.chargeMaterial.test(stack)) {
                chargeMats++;
            }
        }
        if (target == null || chargeMats <= 0) return ItemStack.EMPTY;

        ItemStack result = target.copy();
        long chargeToAdd = this.chargePerMaterial * chargeMats;

        if (result.getItem() instanceof IBatteryItem battery) {
            battery.addEnergy(result, chargeToAdd);
        }

        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return new ItemStack(this.targetItem);
    }

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.MISC;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ChargeCarryingRecipes.GRADUAL_CHARGE_SERIALIZER.get();
    }

    @Override
    public String getGroup() {
        return this.group;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.getItem() == this.targetItem) {
                remaining.set(i, ItemStack.EMPTY);
            } else if (this.chargeMaterial.test(stack)) {
                if (stack.hasCraftingRemainingItem()) {
                    remaining.set(i, stack.getCraftingRemainingItem());
                }
            } else if (stack.hasCraftingRemainingItem()) {
                remaining.set(i, stack.getCraftingRemainingItem());
            }
        }
        return remaining;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.add(Ingredient.of(this.targetItem));
        list.add(this.chargeMaterial);
        return list;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public Item getTargetItem() {
        return this.targetItem;
    }

    public Ingredient getChargeMaterial() {
        return this.chargeMaterial;
    }

    public long getChargePerMaterial() {
        return this.chargePerMaterial;
    }
}