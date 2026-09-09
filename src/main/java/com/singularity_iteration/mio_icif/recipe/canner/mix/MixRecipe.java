package com.singularity_iteration.mio_icif.recipe.canner.mix;

import com.singularity_iteration.mio_icif.Items.Cell.mio_icif_cells;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import com.singularity_iteration.mio_icif.api.recipe.IRecipeProcessingInfo;

@SuppressWarnings("null")
public class MixRecipe implements Recipe<MixRecipeInput>, IRecipeProcessingInfo {

    private final String group;
    private final FluidStack inputFluid;           // 输入流体
    private final Ingredient materialIngredient;   // 材料物品
    private final int materialCount;               // 材料数量
    private final FluidStack resultFluid;          // 结果流体
    private final ItemStack resultCell;            // 结果单元（如果有空单元时输出�
private final int processingTime;
    private final int energyPerTick;

    public MixRecipe(String group, FluidStack inputFluid, Ingredient materialIngredient, int materialCount,
                     FluidStack resultFluid, ItemStack resultCell, int processingTime, int energyPerTick) {
        this.group = group;
        this.inputFluid = inputFluid;
        this.materialIngredient = materialIngredient;
        this.materialCount = materialCount;
        this.resultFluid = resultFluid;
        this.resultCell = resultCell;
        this.processingTime = processingTime;
        this.energyPerTick = energyPerTick;
    }

    @Override
    public boolean matches(MixRecipeInput input, Level level) {
        // 检查材料是否匹�
    if (!this.materialIngredient.test(input.material())) {
            return false;
        }

        // 检查材料数据
    if (input.material().getCount() < this.materialCount) {
            return false;
        }

        // 检查输入流体是否匹�
    FluidStack tankFluid = input.inputFluid();
        if (tankFluid.isEmpty()) {
            return false;
        }

        return tankFluid.getFluid() == this.inputFluid.getFluid() &&
               tankFluid.getAmount() >= this.inputFluid.getAmount();
    }

    @Override
    public ItemStack assemble(MixRecipeInput input, HolderLookup.Provider registries) {
        // 混合模式主要输出流体，这里返回空物品栈
    return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.resultCell.copy();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.add(this.materialIngredient);
        return list;
    }

    @Override
    public String getGroup() {
        return this.group;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return MixRecipes.MIX_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return MixRecipes.MIX_TYPE.get();
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(net.minecraft.world.level.block.Blocks.CAULDRON);
    }

    // Getters
    public FluidStack getInputFluid() {
        return inputFluid;
    }

    public Ingredient getMaterialIngredient() {
        return materialIngredient;
    }

    public int getMaterialCount() {
        return materialCount;
    }

    public FluidStack getResultFluid() {
        return resultFluid;
    }

    public ItemStack getResultCell() {
        return resultCell;
    }

    public ItemStack getDynamicResultCell() {
        if (!resultFluid.isEmpty()) {
            return mio_icif_cells.createDynamicFilledCell(resultFluid.getFluid(), resultFluid.getAmount());
        }
        return resultCell;
    }

    @Override
    public int getProcessingTime() {
        return processingTime;
    }

    @Override
    public int getEnergyPerTick() {
        return energyPerTick;
    }

    @Override
    public int getIngredientCount() {
        return materialCount;
    }
}