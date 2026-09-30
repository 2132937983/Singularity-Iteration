package com.miophas.singularity_iteration.common.recipe.charge_carrying;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;

import java.util.List;

/**
 * 电量传递的无序合成配方。
 *
 * <p>继承原版 {@link ShapelessRecipe}（与有序版继承 {@code ShapedRecipe} 同理）：
 * 匹配使用原版的完整二分匹配，旧实现的"逐个取第一个匹配物品"贪心算法在材料重叠时
 * （例如 tag 与具体物品同时出现）会把合法摆放判为不匹配；同时配方书、JEI/EMI
 * 也能按无序配方正确显示。只替换 {@link #assemble} 与 {@link #getRemainingItems}。
 */
public class ChargeCarryingShapelessRecipe extends ShapelessRecipe {

    private final List<Ingredient> ingredientList;
    private final ItemStack result;
    private final boolean transferCharge;

    public ChargeCarryingShapelessRecipe(String group, List<Ingredient> ingredients, ItemStack result, boolean transferCharge) {
        super(group, CraftingBookCategory.MISC, result, toNonNullList(ingredients));
        this.ingredientList = List.copyOf(ingredients);
        this.result = result;
        this.transferCharge = transferCharge;
    }

    private static NonNullList<Ingredient> toNonNullList(List<Ingredient> ingredients) {
        NonNullList<Ingredient> list = NonNullList.create();
        list.addAll(ingredients);
        return list;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack ret = this.result.copy();
        if (this.transferCharge) {
            ChargeCarryingRecipe.chargeResult(ret, ChargeCarryingRecipe.collectInputCharge(input));
        }
        return ret;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ChargeCarryingRecipes.CHARGE_CARRYING_SHAPELESS_SERIALIZER.get();
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

    public List<Ingredient> getIngredientList() {
        return this.ingredientList;
    }

    public ItemStack getResult() {
        return this.result;
    }

    public boolean shouldTransferCharge() {
        return this.transferCharge;
    }
}
