package com.singularity_iteration.mio_icif.api.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

@SuppressWarnings("rawtypes")
public interface IRecipeAPI {

    RecipeType<?> getPowderRecipeType();

    RecipeType<?> getCompressorRecipeType();

    RecipeType<?> getExtractorRecipeType();

    RecipeType<?> getWasherRecipeType();

    RecipeType<?> getCentrifugeRecipeType();

    /**
     * @deprecated 金属成型机已拆分为卷板机、切割机和压膜器。
     * 请使用 {@link #getRollingMachineRecipeType()}、{@link #getCuttingRecipeType()} 或 {@link #getExtrudingRecipeType()} 代替。
     */
    @Deprecated
    RecipeType<?> getMetalFormerRecipeType();

    /**
     * @deprecated 卷板机配方类型。请使用 {@link #getMetalFormerRecipeType()} 代替。
     */
    @Deprecated
    default RecipeType<?> getRollingMachineRecipeType() { return getMetalFormerRecipeType(); }

    RecipeType<?> getBlastFurnaceRecipeType();

    RecipeType<?> getCannerRecipeType();

    RecipeType<?> getBlockCutterRecipeType();

    RecipeType<?> getCuttingRecipeType();

    RecipeType<?> getExtrudingRecipeType();

    RecipeType<?> getWelderRecipeType();

    RecipeType<?> getPrecisionAssemblerRecipeType();

    RecipeType<?> getFusionReactorRecipeType();

    RecipeType<?> getLatheRecipeType();

    RecipeType<?> getLaserEngraverRecipeType();

    RecipeType<?> getMassFabricatorRecipeType();

    RecipeType<?> getPlasmaFurnaceRecipeType();

    RecipeType<?> getReplicatorRecipeType();

    RecipeType<?> getScannerRecipeType();

    RecipeType<?> getVacuumFreezerRecipeType();

    RecipeType<?> getBlenderRecipeType();

    RecipeType<?> getElectrolyzerRecipeType();

    RecipeType<?> getFermenterRecipeType();

    RecipeType<?> getRecyclerRecipeType();

    RecipeType<?> getNeutronPolymerizerRecipeType();

    RecipeType<?> getFluidRefiningRecipeType();

    /**
     * 查找与输入物品匹配的配方
     *
     * <p><b>⚠️ 重要警告：</b>此方法仅检查配方的第一个 Ingredient 是否匹配输入物品。
     * 对于多成分配方（如焊接机、精密组装机等需要多个输入的配方），此方法可能返回
     * <b>错误的匹配结果</b>，因为它不会验证其他输入槽位是否满足。
     *
     * <p><b>正确用法：</b>
     * <ul>
     *   <li>✅ 单输入配方：压缩机、粉碎机、提取器等 - 使用此方法</li>
     *   <li>❌ 多输入配方：焊接机、精密组装机、聚变反应堆等 - 使用 {@link #findRecipeMultiInput(RecipeType, ItemStack[], Level)}</li>
     * </ul>
     *
     * @deprecated 此方法仅适用于单输入配方。对于多输入配方，请使用 {@link #findRecipeMultiInput(RecipeType, ItemStack[], Level)}。
     * @param type 配方类型
     * @param input 输入物品
     * @param level 世界
     * @return 匹配的配方，如果没有则返回 Optional.empty()
     */
    @Deprecated
    <T extends Recipe<?>> Optional<RecipeHolder<T>> findRecipe(RecipeType<T> type, ItemStack input, Level level);

    Optional<? extends RecipeHolder<?>> findPowderRecipe(ItemStack input, Level level);

    Optional<? extends RecipeHolder<?>> findCompressorRecipe(ItemStack input, Level level);

    Optional<? extends RecipeHolder<?>> findExtractorRecipe(ItemStack input, Level level);

    Optional<? extends RecipeHolder<?>> findWasherRecipe(ItemStack input, Level level);

    Optional<? extends RecipeHolder<?>> findCentrifugeRecipe(ItemStack input, Level level);

    // ========== 便捷配方查找（default 方法委托到 findRecipe） ==========

    /**
     * 鏌ユ壘楂樼倝閰嶆柟
     */
    @SuppressWarnings("unchecked")
    default Optional<? extends RecipeHolder<?>> findBlastFurnaceRecipe(ItemStack input, Level level) {
        return findRecipe((RecipeType) getBlastFurnaceRecipeType(), input, level);
    }

    /**
     * 查找罐装机配方
     */
    @SuppressWarnings("unchecked")
    default Optional<? extends RecipeHolder<?>> findCannerRecipe(ItemStack input, Level level) {
        return findRecipe((RecipeType) getCannerRecipeType(), input, level);
    }

    /**
     * 查找方块切割机配方
     */
    @SuppressWarnings("unchecked")
    default Optional<? extends RecipeHolder<?>> findBlockCutterRecipe(ItemStack input, Level level) {
        return findRecipe((RecipeType) getBlockCutterRecipeType(), input, level);
    }

    /**
     * 查找切割机配方
     */
    @SuppressWarnings("unchecked")
    default Optional<? extends RecipeHolder<?>> findCuttingRecipe(ItemStack input, Level level) {
        return findRecipe((RecipeType) getCuttingRecipeType(), input, level);
    }

    /**
     * 查找卷板机配方
     */
    @SuppressWarnings("unchecked")
    default Optional<? extends RecipeHolder<?>> findRollingRecipe(ItemStack input, Level level) {
        return findRecipe((RecipeType) getMetalFormerRecipeType(), input, level);
    }

    /**
     * 查找压膜器配方
     */
    @SuppressWarnings("unchecked")
    default Optional<? extends RecipeHolder<?>> findExtrudingRecipe(ItemStack input, Level level) {
        return findRecipe((RecipeType) getExtrudingRecipeType(), input, level);
    }

    /**
     * 查找车床配方
     */
    @SuppressWarnings("unchecked")
    default Optional<? extends RecipeHolder<?>> findLatheRecipe(ItemStack input, Level level) {
        return findRecipe((RecipeType) getLatheRecipeType(), input, level);
    }

    /**
     * 查找激光雕刻机配方
     */
    @SuppressWarnings("unchecked")
    default Optional<? extends RecipeHolder<?>> findLaserEngraverRecipe(ItemStack input, Level level) {
        return findRecipe((RecipeType) getLaserEngraverRecipeType(), input, level);
    }

    /**
     * 查找真空冷冻机配方
     */
    @SuppressWarnings("unchecked")
    default Optional<? extends RecipeHolder<?>> findVacuumFreezerRecipe(ItemStack input, Level level) {
        return findRecipe((RecipeType) getVacuumFreezerRecipeType(), input, level);
    }

    /**
     * 查找搅拌机配方
     */
    @SuppressWarnings("unchecked")
    default Optional<? extends RecipeHolder<?>> findBlenderRecipe(ItemStack input, Level level) {
        return findRecipe((RecipeType) getBlenderRecipeType(), input, level);
    }

    /**
     * 查找电解槽配方
     */
    @SuppressWarnings("unchecked")
    default Optional<? extends RecipeHolder<?>> findElectrolyzerRecipe(ItemStack input, Level level) {
        return findRecipe((RecipeType) getElectrolyzerRecipeType(), input, level);
    }

    /**
     * 查找发酵罐配方
     */
    @SuppressWarnings("unchecked")
    default Optional<? extends RecipeHolder<?>> findFermenterRecipe(ItemStack input, Level level) {
        return findRecipe((RecipeType) getFermenterRecipeType(), input, level);
    }

    /**
     * 查找回收机配方
     */
    @SuppressWarnings("unchecked")
    default Optional<? extends RecipeHolder<?>> findRecyclerRecipe(ItemStack input, Level level) {
        return findRecipe((RecipeType) getRecyclerRecipeType(), input, level);
    }

    /**
     * 鏌ユ壘绛夌�诲瓙鐔旂倝閰嶆柟
     */
    @SuppressWarnings("unchecked")
    default Optional<? extends RecipeHolder<?>> findPlasmaFurnaceRecipe(ItemStack input, Level level) {
        return findRecipe((RecipeType) getPlasmaFurnaceRecipeType(), input, level);
    }

    /**
     * 查找物质制造机配方
     */
    @SuppressWarnings("unchecked")
    default Optional<? extends RecipeHolder<?>> findMassFabricatorRecipe(ItemStack input, Level level) {
        return findRecipe((RecipeType) getMassFabricatorRecipeType(), input, level);
    }

    /**
     * 查找复制机配方
     */
    @SuppressWarnings("unchecked")
    default Optional<? extends RecipeHolder<?>> findReplicatorRecipe(ItemStack input, Level level) {
        return findRecipe((RecipeType) getReplicatorRecipeType(), input, level);
    }

    /**
     * 查找扫描机配方
     */
    @SuppressWarnings("unchecked")
    default Optional<? extends RecipeHolder<?>> findScannerRecipe(ItemStack input, Level level) {
        return findRecipe((RecipeType) getScannerRecipeType(), input, level);
    }

    List<ResourceLocation> getAllRecipeTypeIds();

    @SuppressWarnings("unchecked")
    default Optional<? extends RecipeHolder<?>> findNeutronPolymerizerRecipe(ItemStack input, Level level) {
        return findRecipe((RecipeType) getNeutronPolymerizerRecipeType(), input, level);
    }

    // ========== 流体精炼配方 API ==========

    Optional<? extends RecipeHolder<?>> findFluidRefiningRecipe(ItemStack inputItem, net.neoforged.neoforge.fluids.FluidStack inputFluid, Level level);

    Optional<? extends RecipeHolder<?>> findFluidRefiningRecipeForFluid(net.neoforged.neoforge.fluids.FluidStack fluid, Level level);

    net.neoforged.neoforge.fluids.FluidStack getFluidRefiningOutputFluid(RecipeHolder<?> recipe);

    // ========== 配方信息访问 API ==========

    /**
     * 获取配方的输出物品
     *
     * @param recipe 配方
     * @return 输出物品，如果配方为null则返回空ItemStack
     */
    ItemStack getRecipeOutput(RecipeHolder<?> recipe);

    /**
     * 获取配方的处理时间（tick）
     *
     * @param recipe 配方
     * @return 处理时间（tick），如果配方为null或无法获取则返回200
     */
    int getRecipeProcessTime(RecipeHolder<?> recipe);

    /**
     * 获取配方的能耗（EU）
     *
     * @param recipe 配方
     * @return 总能耗（EU），如果配方为null或无法获取则返回0
     */
    long getRecipeEnergyCost(RecipeHolder<?> recipe);

    /**
     * 获取配方的每tick能耗（EU/tick）
     *
     * @param recipe 配方
     * @return 每tick能耗（EU），如果配方为null或无法获取则返回0
     */
    long getRecipeEnergyPerTick(RecipeHolder<?> recipe);

    /**
     * 获取配方的输入物品消耗数量
     *
     * @param recipe 配方
     * @return 消耗数量，如果配方为null或无法获取则返回1
     */
    int getRecipeIngredientCount(RecipeHolder<?> recipe);

    /**
     * 多输入配方查找
     *
     * <p>用于焊接机、精密组装机、聚变反应堆等需要多个输入物品的配方。
     * 该方法会检查所有输入槽是否与配方的原料匹配。
     *
     * @param type 配方类型
     * @param inputs 输入物品数组
     * @param level 世界
     * @return 匹配的配方，如果没有则返回 Optional.empty()
     */
    <T extends Recipe<?>> Optional<RecipeHolder<T>> findRecipeMultiInput(RecipeType<T> type, ItemStack[] inputs, Level level);

    Optional<? extends RecipeHolder<?>> findWelderRecipe(ItemStack[] inputs, Level level);

    Optional<? extends RecipeHolder<?>> findPrecisionAssemblerRecipe(ItemStack[] inputs, Level level);

    Optional<? extends RecipeHolder<?>> findFusionReactorRecipe(ItemStack[] inputs, Level level);

    /**
     * 娓呴櫎閰嶆柟缂撳瓨
     * 
     * <p>当配方系统发生变化时（如数据包重载、动态注册配方等），
 * 应调用此方法清除存，确保查询结果正确。
     * 
     * <p>附属模组在运行时注册新配方后应调用此方法。
     */
    void invalidateCache();
}