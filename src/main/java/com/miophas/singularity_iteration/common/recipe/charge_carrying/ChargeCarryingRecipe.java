package com.miophas.singularity_iteration.common.recipe.charge_carrying;

import com.miophas.singularity_iteration.core.api.item.IBatteryItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;

/**
 * 电量传递的有序合成配方（合成时把材料上的电量汇总进产物）。
 *
 * <p>这里必须继承原版 {@link ShapedRecipe}，而不是只实现 {@code CraftingRecipe}：
 * 合成台、配方书、JEI/EMI 都是靠 {@code instanceof ShapedRecipe} 拿 {@link #getWidth()} /
 * {@link #getHeight()} 才能按 3x3 真实位置摆放材料。以前只实现 {@code CraftingRecipe} 时，
 * 查看器读不到形状信息，只能把 {@code getIngredients()} 里的材料并排显示成一行，
 * 于是出现「JEI 显示第一行并排、实际要竖着摆才合成」的错位（“+”一键填充也会填错位置）。
 *
 * <p>配方数据格式与匹配逻辑保持不变：shape 仍由 {@link ShapedRecipePattern} 描述，
 * 只有 {@link #assemble} 与 {@link #getRemainingItems} 被替换成带电量传递的版本。
 */
public class ChargeCarryingRecipe extends ShapedRecipe {

    private final ShapedRecipePattern pattern;
    private final ItemStack result;
    private final boolean transferCharge;

    public ChargeCarryingRecipe(String group, ShapedRecipePattern pattern, ItemStack result, boolean transferCharge) {
        super(group, CraftingBookCategory.MISC, pattern, result);
        this.pattern = pattern;
        this.result = result;
        this.transferCharge = transferCharge;
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
    public RecipeSerializer<?> getSerializer() {
        return ChargeCarryingRecipes.CHARGE_CARRYING_SERIALIZER.get();
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

    /** 供序列化器读写 pattern（{@code ShapedRecipePattern.MAP_CODEC} 需要）。 */
    public ShapedRecipePattern getPattern() {
        return this.pattern;
    }

    public ItemStack getResult() {
        return this.result;
    }

    public boolean shouldTransferCharge() {
        return this.transferCharge;
    }

    /** Sum of the charge on every electric input; saturates instead of overflowing. */
    public static long collectInputCharge(CraftingInput input) {
        long totalCharge = 0L;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof IBatteryItem battery) {
                long charge = Math.max(0L, battery.getEnergy(stack));
                totalCharge = charge > Long.MAX_VALUE - totalCharge ? Long.MAX_VALUE : totalCharge + charge;
            }
        }
        return totalCharge;
    }

    /**
     * Gives the result exactly the carried charge, capped at its capacity.
     * {@link IBatteryItem#setEnergy} is optional (a no-op by default), so any
     * charge left on the result after it is drained explicitly. Excess charge
     * beyond the result's capacity is not preserved, as in IC2.
     */
    public static void chargeResult(ItemStack result, long charge) {
        if (result.isEmpty() || !(result.getItem() instanceof IBatteryItem battery)) return;
        battery.setEnergy(result, 0);
        long left = battery.getEnergy(result);
        if (left > 0) battery.extractEnergy(result, left);
        if (charge > 0) battery.addEnergy(result, charge);
    }
}
