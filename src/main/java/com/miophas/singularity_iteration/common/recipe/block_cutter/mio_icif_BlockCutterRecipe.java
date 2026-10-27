package com.miophas.singularity_iteration.common.recipe.block_cutter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import com.miophas.singularity_iteration.core.api.recipe.IRecipeProcessingInfo;

@SuppressWarnings("null")
public class mio_icif_BlockCutterRecipe implements Recipe<mio_icif_BlockCutterRecipeInput>, IRecipeProcessingInfo {

    private final String group;
    private final Ingredient ingredient;
    private final ItemStack result;
    private final int processingTime;
    private final int energyPerTick;
    private final int ingredientCount;
    private final int hardness;
    /** 0.1.10.29: the result is the planks of the input log ({@link WoodPlanks}); the result count is planks per 4 crafted. */
    private final boolean wood;

    public mio_icif_BlockCutterRecipe(String group, Ingredient ingredient, ItemStack result,
                                      int processingTime, int energyPerTick, int ingredientCount, int hardness) {
        this(group, ingredient, result, processingTime, energyPerTick, ingredientCount, hardness, false);
    }

    public mio_icif_BlockCutterRecipe(String group, Ingredient ingredient, ItemStack result,
                                      int processingTime, int energyPerTick, int ingredientCount, int hardness, boolean wood) {
        this.group = group;
        this.ingredient = ingredient;
        this.result = result;
        this.processingTime = processingTime;
        this.energyPerTick = energyPerTick;
        this.ingredientCount = ingredientCount;
        this.hardness = hardness;
        this.wood = wood;
    }

    @Override
    public boolean matches(mio_icif_BlockCutterRecipeInput input, Level level) {
        if (!this.ingredient.test(input.item()) || input.item().getCount() < this.ingredientCount) return false;
        return !this.wood || !resultFor(level, input.item()).isEmpty();
    }

    /** The output for {@code input}: the result, or for a wood recipe the planks of that log (empty when it has none). */
    public ItemStack resultFor(@Nullable Level level, ItemStack input) {
        if (!this.wood) return this.result;
        return level == null ? ItemStack.EMPTY
            : WoodPlanks.planks(level.getRecipeManager(), level.registryAccess(), input, this.result.getCount());
    }

    @Override
    public ItemStack assemble(mio_icif_BlockCutterRecipeInput input, HolderLookup.Provider registries) {
        if (this.wood) {
            var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                ItemStack planks = WoodPlanks.planks(server.getRecipeManager(), registries, input.item(), this.result.getCount());
                if (!planks.isEmpty()) return planks;
            }
        }
        return this.result.copy();
    }

    /**
     * Recipes for JEI / EMI: a wood recipe becomes one recipe per planks crafting recipe (the logs it takes and the cutter's
     * planks), id {@code <recipe>/<crafting namespace>/<crafting path>}; other recipes are kept.
     */
    public static List<RecipeHolder<mio_icif_BlockCutterRecipe>> displays(RecipeManager manager, @Nullable HolderLookup.Provider access,
                                                                          List<RecipeHolder<mio_icif_BlockCutterRecipe>> recipes) {
        List<RecipeHolder<mio_icif_BlockCutterRecipe>> out = new ArrayList<>();
        for (RecipeHolder<mio_icif_BlockCutterRecipe> h : recipes) {
            mio_icif_BlockCutterRecipe r = h.value();
            if (!r.wood) { out.add(h); continue; }
            for (WoodPlanks.Source s : WoodPlanks.sources(manager, access)) {
                ItemStack[] logs = Arrays.stream(r.ingredient.getItems()).filter(s.ingredient())
                    .map(ItemStack::copy).toArray(ItemStack[]::new);
                if (logs.length == 0) continue;
                ItemStack planks = s.planks().copyWithCount(WoodPlanks.count(s.planks().getCount(), r.result.getCount()));
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath(h.id().getNamespace(), h.id().getPath() + "/"
                    + s.recipe().id().getNamespace() + "/" + s.recipe().id().getPath());
                out.add(new RecipeHolder<>(id, new mio_icif_BlockCutterRecipe(r.group, Ingredient.of(logs), planks,
                    r.processingTime, r.energyPerTick, r.ingredientCount, r.hardness)));
            }
        }
        return out;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.add(this.ingredient);
        return list;
    }

    @Override
    public String getGroup() {
        return this.group;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return mio_icif_BlockCutterRecipes.BLOCK_CUTTER_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return mio_icif_BlockCutterRecipes.BLOCK_CUTTER_TYPE.get();
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(net.minecraft.world.level.block.Blocks.CRAFTING_TABLE);
    }

    @Override
    public int getProcessingTime() {
        return processingTime;
    }

    @Override
    public int getEnergyPerTick() {
        return energyPerTick;
    }

    public Ingredient getIngredient() {
        return ingredient;
    }

    public ItemStack getResult() {
        return result;
    }

    @Override
    public int getIngredientCount() {
        return ingredientCount;
    }

    public int getHardness() {
        return hardness;
    }

    public boolean isWood() {
        return wood;
    }
}
