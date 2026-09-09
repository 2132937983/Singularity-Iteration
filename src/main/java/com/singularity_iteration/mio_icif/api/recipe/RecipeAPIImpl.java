package com.singularity_iteration.mio_icif.api.recipe;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.fluids.FluidStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 配方 API 实现
 *
 * <p><strong>内部实现类，不应直接访问。</strong>
 * 请通过 {@link com.singularity_iteration.mio_icif.api.MioIcifAPI#getRecipeAPI()} 获取接口实例。
 */
@SuppressWarnings("null")
public class RecipeAPIImpl implements IRecipeAPI {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeAPIImpl.class);

    // 静态实例引用，用于事件处理器访问
    private static RecipeAPIImpl instance;

    /**
     * 缂撳瓨鎸夌被鍨嬪垎缁勭殑閰嶆柟锛岄伩鍏嶆瘡娆℃煡鎵鹃兘閬嶅巻鍏ㄩ儴閰嶆柟銆�
     *
     * <p><b>线程安全说明：</b>配方数据来自全局 {@link net.minecraft.world.item.crafting.RecipeManager}，
 * 在 vanilla Minecraft 中所有 Level 共享同一实例，因此存以 {@link RecipeType} 为 key 是安全的。
     * 如果未来出现每维度独立 RecipeManager 的场景，需在 {@link #invalidateCache()} 中按维度清除。
     */
    private final ConcurrentHashMap<RecipeType<?>, List<RecipeHolder<?>>> recipeCache = new ConcurrentHashMap<>();

    public RecipeAPIImpl() {
        instance = this;
    }

    /**
     * 获取 RecipeAPIImpl 实例（包级私有，供事件处理器使用）
     */
    static RecipeAPIImpl getInstance() {
        return instance;
    }

    /**
     * 娓呴櫎閰嶆柟缂撳瓨
     * 
     * <p>当配方系统发生变化时（如数据包重载、动态注册配方等），
 * 应调用此方法清除存，确保查询结果正确。
     */
    public void invalidateCache() {
        recipeCache.clear();
    }

    /**
 * 静态方法：清除配方存（供事件处理器调用）
     */
    public static void invalidateStaticCache() {
        if (instance != null) {
            instance.invalidateCache();
        }
    }

    private List<RecipeHolder<?>> getRecipesByType(RecipeType<?> type, Level level) {
        return recipeCache.computeIfAbsent(type, k ->
            level.getRecipeManager().getRecipes().stream()
                .filter(holder -> holder.value().getType() == type)
                .collect(Collectors.toList())
        );
    }

    @Override
    public RecipeType<?> getPowderRecipeType() {
        return RecipeTypeBridge.powder();
    }

    @Override
    public RecipeType<?> getCompressorRecipeType() {
        return RecipeTypeBridge.compressor();
    }

    @Override
    public RecipeType<?> getExtractorRecipeType() {
        return RecipeTypeBridge.extractor();
    }

    @Override
    public RecipeType<?> getWasherRecipeType() { return RecipeTypeBridge.washer(); }
    @Override
    public RecipeType<?> getCentrifugeRecipeType() { return RecipeTypeBridge.centrifuge(); }
    @Override @Deprecated
    public RecipeType<?> getMetalFormerRecipeType() { return RecipeTypeBridge.rolling(); }
    @Override
    public RecipeType<?> getBlastFurnaceRecipeType() { return RecipeTypeBridge.blastFurnace(); }
    @Override
    public RecipeType<?> getCannerRecipeType() { return RecipeTypeBridge.canning(); }
    @Override
    public RecipeType<?> getBlockCutterRecipeType() { return RecipeTypeBridge.blockCutter(); }
    @Override
    public RecipeType<?> getCuttingRecipeType() { return RecipeTypeBridge.cutting(); }
    @Override
    public RecipeType<?> getExtrudingRecipeType() { return RecipeTypeBridge.extruding(); }
    @Override
    public RecipeType<?> getWelderRecipeType() { return RecipeTypeBridge.welder(); }
    @Override
    public RecipeType<?> getPrecisionAssemblerRecipeType() { return RecipeTypeBridge.precisionAssembler(); }
    @Override
    public RecipeType<?> getFusionReactorRecipeType() { return RecipeTypeBridge.fusionReactor(); }
    @Override
    public RecipeType<?> getLatheRecipeType() { return RecipeTypeBridge.lathe(); }
    @Override
    public RecipeType<?> getLaserEngraverRecipeType() { return RecipeTypeBridge.laserEngraver(); }
    @Override
    public RecipeType<?> getMassFabricatorRecipeType() { return RecipeTypeBridge.massFabricator(); }
    @Override
    public RecipeType<?> getPlasmaFurnaceRecipeType() { return RecipeTypeBridge.plasmaFurnace(); }
    @Override
    public RecipeType<?> getReplicatorRecipeType() { return RecipeTypeBridge.replicator(); }
    @Override
    public RecipeType<?> getScannerRecipeType() { return RecipeTypeBridge.scanner(); }
    @Override
    public RecipeType<?> getVacuumFreezerRecipeType() { return RecipeTypeBridge.vacuumFreezer(); }
    @Override
    public RecipeType<?> getBlenderRecipeType() { return RecipeTypeBridge.blender(); }
    @Override
    public RecipeType<?> getElectrolyzerRecipeType() { return RecipeTypeBridge.electrolyzer(); }
    @Override
    public RecipeType<?> getFermenterRecipeType() { return RecipeTypeBridge.fermenter(); }
    @Override
    public RecipeType<?> getRecyclerRecipeType() { return RecipeTypeBridge.recycler(); }
    @Override
    public RecipeType<?> getNeutronPolymerizerRecipeType() { return RecipeTypeBridge.neutronPolymerizer(); }

    @Override
    public RecipeType<?> getFluidRefiningRecipeType() { return RecipeTypeBridge.fluidRefining(); }

    @Override
    @Deprecated
    @SuppressWarnings("unchecked")
    public <T extends Recipe<?>> Optional<RecipeHolder<T>> findRecipe(RecipeType<T> type, ItemStack input, Level level) {
        if (level == null || input.isEmpty()) return Optional.empty();
 // 跨敤缂撳瓨鐨勬寜绫诲瀷鍒嗙粍閰嶆柟锛岄伩鍏� O(n) 鍏ㄩ噺閬嶅巻
        List<RecipeHolder<?>> cached = getRecipesByType(type, level);
        for (RecipeHolder<?> holder : cached) {
            Recipe<?> recipe = holder.value();
            NonNullList<Ingredient> ingredients = recipe.getIngredients();
            if (!ingredients.isEmpty()) {
                // 注意：此方法仅适用于单原料配方查找
                // 对于多原料配方（如焊接机），只检查第一个原料，可能导致误匹配
                // 建议多原料配方使用专门的 findMultiIngredientRecipe 方法
                if (ingredients.getFirst().test(input)) {
                    try {
                        return Optional.of((RecipeHolder<T>) holder);
                    } catch (ClassCastException e) {
                        continue;
                    }
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<? extends RecipeHolder<?>> findPowderRecipe(ItemStack input, Level level) {
        return findRecipe(getPowderRecipeType(), input, level);
    }

    @Override
    public Optional<? extends RecipeHolder<?>> findCompressorRecipe(ItemStack input, Level level) {
        return findRecipe(getCompressorRecipeType(), input, level);
    }

    @Override
    public Optional<? extends RecipeHolder<?>> findExtractorRecipe(ItemStack input, Level level) {
        return findRecipe(getExtractorRecipeType(), input, level);
    }

    @Override
    public Optional<? extends RecipeHolder<?>> findWasherRecipe(ItemStack input, Level level) {
        return findRecipe(getWasherRecipeType(), input, level);
    }

    @Override
    public Optional<? extends RecipeHolder<?>> findCentrifugeRecipe(ItemStack input, Level level) {
        return findRecipe(getCentrifugeRecipeType(), input, level);
    }

    @Override
    public List<ResourceLocation> getAllRecipeTypeIds() {
        return List.of(
            ResourceLocation.fromNamespaceAndPath("mio_icif", "powder"),
            ResourceLocation.fromNamespaceAndPath("mio_icif", "compressor"),
            ResourceLocation.fromNamespaceAndPath("mio_icif", "extractor"),
            ResourceLocation.fromNamespaceAndPath("mio_icif", "washer"),
            ResourceLocation.fromNamespaceAndPath("mio_icif", "centrifuge"),
            ResourceLocation.fromNamespaceAndPath("mio_icif", "rolling"),
            ResourceLocation.fromNamespaceAndPath("mio_icif", "cutting"),
            ResourceLocation.fromNamespaceAndPath("mio_icif", "extruding"),
            ResourceLocation.fromNamespaceAndPath("mio_icif", "blast_furnace"),
            ResourceLocation.fromNamespaceAndPath("mio_icif", "canning"),
            ResourceLocation.fromNamespaceAndPath("mio_icif", "empty_to_tank"),
            ResourceLocation.fromNamespaceAndPath("mio_icif", "fill_from_tank"),
            ResourceLocation.fromNamespaceAndPath("mio_icif", "mix"),
            ResourceLocation.fromNamespaceAndPath("mio_icif", "block_cutter"),
            ResourceLocation.fromNamespaceAndPath("mio_icif", "condensator_repair"),
            ResourceLocation.fromNamespaceAndPath("mio_icif", "welder"),
            ResourceLocation.fromNamespaceAndPath("mio_icif", "precision_assembler"),
            ResourceLocation.fromNamespaceAndPath("mio_icif", "fusion_reactor"),
            ResourceLocation.fromNamespaceAndPath("mio_icif", "lathe"),
            ResourceLocation.fromNamespaceAndPath("mio_icif", "laser_engraver"),
            ResourceLocation.fromNamespaceAndPath("mio_icif", "mass_fabricator"),
            ResourceLocation.fromNamespaceAndPath("mio_icif", "plasma_furnace"),
            ResourceLocation.fromNamespaceAndPath("mio_icif", "replicator"),
            ResourceLocation.fromNamespaceAndPath("mio_icif", "scanner"),
            ResourceLocation.fromNamespaceAndPath("mio_icif", "vacuum_freezer"),
            ResourceLocation.fromNamespaceAndPath("mio_icif", "blender"),
            ResourceLocation.fromNamespaceAndPath("mio_icif", "electrolyzer"),
            ResourceLocation.fromNamespaceAndPath("mio_icif", "fermenter"),
            ResourceLocation.fromNamespaceAndPath("mio_icif", "recycler"),
            ResourceLocation.fromNamespaceAndPath("mio_icif", "fluid_refining")
        );
    }

    // ========== 流体精炼配方 API 实现 ==========

    @Override
    public Optional<? extends RecipeHolder<?>> findFluidRefiningRecipe(ItemStack inputItem, FluidStack inputFluid, Level level) {
        if (level == null || inputFluid.isEmpty()) return Optional.empty();
        List<RecipeHolder<?>> cached = getRecipesByType(getFluidRefiningRecipeType(), level);
        for (RecipeHolder<?> holder : cached) {
            if (holder.value() instanceof com.singularity_iteration.mio_icif.recipe.fluid_refining.FluidRefiningRecipe recipe) {
                var recipeInput = new com.singularity_iteration.mio_icif.recipe.fluid_refining.FluidRefiningRecipeInput(inputItem, inputFluid);
                if (recipe.matches(recipeInput, level)) {
                    return Optional.of(holder);
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<? extends RecipeHolder<?>> findFluidRefiningRecipeForFluid(FluidStack fluid, Level level) {
        if (level == null || fluid.isEmpty()) return Optional.empty();
        List<RecipeHolder<?>> cached = getRecipesByType(getFluidRefiningRecipeType(), level);
        for (RecipeHolder<?> holder : cached) {
            if (holder.value() instanceof com.singularity_iteration.mio_icif.recipe.fluid_refining.FluidRefiningRecipe recipe) {
                if (recipe.matchesFluid(fluid.getFluid())) {
                    return Optional.of(holder);
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public FluidStack getFluidRefiningOutputFluid(RecipeHolder<?> recipe) {
        if (recipe == null) return FluidStack.EMPTY;
        if (recipe.value() instanceof com.singularity_iteration.mio_icif.recipe.fluid_refining.FluidRefiningRecipe fluidRecipe) {
            return fluidRecipe.getOutputFluid();
        }
        return FluidStack.EMPTY;
    }

    // ========== 配方信息访问 API 实现 ==========

    @Override
    public ItemStack getRecipeOutput(RecipeHolder<?> recipe) {
        if (recipe == null) return ItemStack.EMPTY;
        try {
            return recipe.value().getResultItem(null);
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

    @Override
    public int getRecipeProcessTime(RecipeHolder<?> recipe) {
        if (recipe == null) return 200;
        Recipe<?> r = recipe.value();
        if (r instanceof IRecipeProcessingInfo processingInfo) {
            return processingInfo.getProcessingTime();
        }
        LOGGER.debug("Recipe {} does not implement IRecipeProcessingInfo, using default value 200", recipe.id());
        return 200;
    }

    @Override
    public long getRecipeEnergyCost(RecipeHolder<?> recipe) {
        if (recipe == null) return 0;
        Recipe<?> r = recipe.value();
        if (r instanceof IRecipeProcessingInfo processingInfo) {
            return processingInfo.getTotalEnergyCost();
        }
        LOGGER.debug("Recipe {} does not implement IRecipeProcessingInfo, returning 0", recipe.id());
        return 0;
    }

    @Override
    public long getRecipeEnergyPerTick(RecipeHolder<?> recipe) {
        if (recipe == null) return 0;
        Recipe<?> r = recipe.value();
        if (r instanceof IRecipeProcessingInfo processingInfo) {
            return processingInfo.getEnergyPerTick();
        }
        LOGGER.debug("Recipe {} does not implement IRecipeProcessingInfo, returning 0", recipe.id());
        return 0;
    }

    @Override
    public int getRecipeIngredientCount(RecipeHolder<?> recipe) {
        if (recipe == null) return 1;
        Recipe<?> r = recipe.value();
        if (r instanceof IRecipeProcessingInfo processingInfo) {
            return processingInfo.getIngredientCount();
        }
        return 1;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Recipe<?>> Optional<RecipeHolder<T>> findRecipeMultiInput(RecipeType<T> type, ItemStack[] inputs, Level level) {
        if (level == null || inputs == null || inputs.length == 0) return Optional.empty();
        
        List<RecipeHolder<?>> cached = getRecipesByType(type, level);
        for (RecipeHolder<?> holder : cached) {
            Recipe<?> recipe = holder.value();
            NonNullList<Ingredient> ingredients = recipe.getIngredients();
            
            // 如果配方需要的原料数量大于提供的输入数量，跳过
            if (ingredients.size() > inputs.length) continue;
            
            boolean allMatch = true;
            for (int i = 0; i < ingredients.size(); i++) {
                Ingredient ingredient = ingredients.get(i);
                // 检查原料是否为空（可选原料）
                if (ingredient.isEmpty()) continue;
                // 检查输入是否匹配原料
                if (i >= inputs.length || !ingredient.test(inputs[i])) {
                    allMatch = false;
                    break;
                }
            }
            
            if (allMatch) {
                try {
                    return Optional.of((RecipeHolder<T>) holder);
                } catch (ClassCastException e) {
                    continue;
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<? extends RecipeHolder<?>> findWelderRecipe(ItemStack[] inputs, Level level) {
        return findRecipeMultiInput(getWelderRecipeType(), inputs, level);
    }

    @Override
    public Optional<? extends RecipeHolder<?>> findPrecisionAssemblerRecipe(ItemStack[] inputs, Level level) {
        return findRecipeMultiInput(getPrecisionAssemblerRecipeType(), inputs, level);
    }

    @Override
    public Optional<? extends RecipeHolder<?>> findFusionReactorRecipe(ItemStack[] inputs, Level level) {
        return findRecipeMultiInput(getFusionReactorRecipeType(), inputs, level);
    }
}