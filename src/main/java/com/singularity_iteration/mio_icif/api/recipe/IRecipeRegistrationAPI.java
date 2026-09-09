package com.singularity_iteration.mio_icif.api.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.resources.ResourceLocation;

/**
 * 配方注册 API
 *
 * <p>提供注册自定义配方的接口，允许附属模组添加新的加工配方。
 *
 * <p>使用示例：
 * <pre>{@code
 * IRecipeRegistrationAPI recipeAPI = MioIcifAPI.instance().getRecipeRegistrationAPI();
 *
 * // 注册压缩机配方
 * recipeAPI.registerCompressorRecipe(
 *     new ResourceLocation("mymod", "compress_iron"),
 *     Ingredient.of(Items.IRON_INGOT),
 *     new ItemStack(ModItems.COMPRESSED_IRON.get()),
 *     200 // 处理时间
 * );
 *
 * // 注册提取机配方
 * recipeAPI.registerExtractorRecipe(
 *     new ResourceLocation("mymod", "extract_rubber"),
 *     Ingredient.of(ModItems.RUBBER_WOOD.get()),
 *     new ItemStack(ModItems.RUBBER.get(), 3),
 *     150
 * );
 * }</pre>
 */
public interface IRecipeRegistrationAPI {

    /**
     * 注册压缩机配方
     *
     * @param id 配方 ID
     * @param input 输入材料
     * @param output 输出物品
     * @param processTime 处理时间（刻）
     * @return true 如果注册成功
     */
    boolean registerCompressorRecipe(ResourceLocation id, Ingredient input, ItemStack output, int processTime);

    /**
     * 注册提取机配方
     *
     * @param id 配方 ID
     * @param input 输入材料
     * @param output 输出物品
     * @param processTime 处理时间（刻）
     * @return true 如果注册成功
     */
    boolean registerExtractorRecipe(ResourceLocation id, Ingredient input, ItemStack output, int processTime);

    /**
 * 注册电配方
     *
     * @param id 配方 ID
     * @param input 输入材料
     * @param output 输出物品
     * @param processTime 处理时间（刻）
     * @param experience 经验值
     * @return true 如果注册成功
     */
    boolean registerElectricFurnaceRecipe(ResourceLocation id, Ingredient input, ItemStack output, int processTime, float experience);

    /**
     * 注册打粉机配方
     *
     * @param id 配方 ID
     * @param input 输入材料
     * @param output 输出物品
     * @param processTime 处理时间（刻）
     * @return true 如果注册成功
     */
    boolean registerMaceratorRecipe(ResourceLocation id, Ingredient input, ItemStack output, int processTime);

    /**
     * 注册离心机配方
     *
     * @param id 配方 ID
     * @param input 输入材料
     * @param outputs 输出物品数组（支持多个输出）
     * @param processTime 处理时间（刻）
     * @return true 如果注册成功
     */
    boolean registerCentrifugeRecipe(ResourceLocation id, Ingredient input, ItemStack[] outputs, int processTime);

    /**
     * 注册回收机配方
     *
     * @param id 配方 ID
     * @param input 输入材料
     * @param outputs 输出物品数组（支持随机输出）
     * @param chances 每个输出的概率（0-1）
     * @param processTime 处理时间（刻）
     * @return true 如果注册成功
     */
    boolean registerRecyclerRecipe(ResourceLocation id, Ingredient input, ItemStack[] outputs, float[] chances, int processTime);

    /**
     * 注册洗矿机配方
     *
     * @param id 配方 ID
     * @param input 输入材料
     * @param outputs 输出物品数组
     * @param processTime 处理时间（刻）
     * @return true 如果注册成功
     */
    boolean registerOreWasherRecipe(ResourceLocation id, Ingredient input, ItemStack[] outputs, int processTime);

    /**
     * 注册热能离心机配方
     *
     * @param id 配方 ID
     * @param input 输入材料
     * @param outputs 输出物品数组
     * @param processTime 处理时间（刻）
     * @param heatRequired 所需热量
     * @return true 如果注册成功
     */
    boolean registerThermalCentrifugeRecipe(ResourceLocation id, Ingredient input, ItemStack[] outputs, int processTime, int heatRequired);

    /**
     * 注册搅拌机配方
     *
     * @param id 配方 ID
     * @param input 输入材料
     * @param output 输出物品
     * @param processTime 处理时间（刻）
     * @return true 如果注册成功
     */
    boolean registerBlenderRecipe(ResourceLocation id, Ingredient input, ItemStack output, int processTime);

    /**
     * 注册发酵机配方
     *
     * @param id 配方 ID
     * @param input 输入材料
     * @param output 输出物品
     * @param processTime 处理时间（刻）
     * @return true 如果注册成功
     */
    boolean registerFermenterRecipe(ResourceLocation id, Ingredient input, ItemStack output, int processTime);

    /**
     * 注册流体固体灌装机配方
     *
     * @param id 配方 ID
     * @param itemInput 物品输入
     * @param fluidInput 流体输入（格式："fluid_id:amount"）
     * @param output 输出物品
     * @param processTime 处理时间（刻）
     * @return true 如果注册成功
     */
    boolean registerFluidSolidRecipe(ResourceLocation id, Ingredient itemInput, String fluidInput, ItemStack output, int processTime);

    /**
     * 注册 canner 配方
     *
     * @param id 配方 ID
     * @param input 输入材料
     * @param output 输出物品
     * @param processTime 处理时间（刻）
     * @return true 如果注册成功
     */
    boolean registerCannerRecipe(ResourceLocation id, Ingredient input, ItemStack output, int processTime);

    /**
     * 注册电解机配方
     *
     * @param id 配方 ID
     * @param input 输入材料
     * @param outputs 输出物品数组
     * @param processTime 处理时间（刻）
     * @return true 如果注册成功
     */
    boolean registerElectrolyzerRecipe(ResourceLocation id, Ingredient input, ItemStack[] outputs, int processTime);

    /**
     * 注册压膜器配方
     *
     * @param id 配方 ID
     * @param input 输入材料
     * @param output 输出物品
     * @param processTime 处理时间（刻）
     * @return true 如果注册成功
     */
    boolean registerExtruderRecipe(ResourceLocation id, Ingredient input, ItemStack output, int processTime);

    /**
     * 注册车床配方
     *
     * @param id 配方 ID
     * @param input 输入材料
     * @param output 输出物品
     * @param processTime 处理时间（刻）
     * @return true 如果注册成功
     */
    boolean registerLatheRecipe(ResourceLocation id, Ingredient input, ItemStack output, int processTime);

    /**
     * 注册卷板机配方
     *
     * @param id 配方 ID
     * @param input 输入材料
     * @param output 输出物品
     * @param processTime 处理时间（刻）
     * @return true 如果注册成功
     */
    boolean registerRollingMachineRecipe(ResourceLocation id, Ingredient input, ItemStack output, int processTime);

    /**
     * 注册切割机配方
     *
     * @param id 配方 ID
     * @param input 输入材料
     * @param output 输出物品
     * @param processTime 处理时间（刻）
     * @return true 如果注册成功
     */
    boolean registerCutterRecipe(ResourceLocation id, Ingredient input, ItemStack output, int processTime);

    /**
     * 注册焊接机配方
     *
     * @param id 配方 ID
     * @param input 输入材料
     * @param output 输出物品
     * @param processTime 处理时间（刻）
     * @return true 如果注册成功
     */
    boolean registerWelderRecipe(ResourceLocation id, Ingredient input, ItemStack output, int processTime);

    /**
     * 注册焊接机配方（多输入版本）
     *
     * @param id 配方 ID
     * @param inputs 输入材料数组
     * @param output 输出物品
     * @param processTime 处理时间（刻）
     * @return true 如果注册成功
     */
    boolean registerWelderRecipe(ResourceLocation id, Ingredient[] inputs, ItemStack output, int processTime);

    /**
     * 注册激光雕刻机配方
     *
     * @param id 配方 ID
     * @param input 输入材料
     * @param output 输出物品
     * @param processTime 处理时间（刻）
     * @return true 如果注册成功
     */
    boolean registerLaserEngraverRecipe(ResourceLocation id, Ingredient input, ItemStack output, int processTime);

    /**
     * 注册精密组装机配方
     *
     * @param id 配方 ID
     * @param input 输入材料
     * @param output 输出物品
     * @param processTime 处理时间（刻）
     * @return true 如果注册成功
     */
    boolean registerPrecisionAssemblerRecipe(ResourceLocation id, Ingredient input, ItemStack output, int processTime);

    /**
     * 注册精密组装机配方（多输入版本）
     *
     * @param id 配方 ID
     * @param inputs 输入材料数组
     * @param output 输出物品
     * @param processTime 处理时间（刻）
     * @return true 如果注册成功
     */
    boolean registerPrecisionAssemblerRecipe(ResourceLocation id, Ingredient[] inputs, ItemStack output, int processTime);

    /**
     * 注册真空冷冻机配方
     *
     * @param id 配方 ID
     * @param input 输入材料
     * @param output 输出物品
     * @param processTime 处理时间（刻）
     * @return true 如果注册成功
     */
    boolean registerVacuumFreezerRecipe(ResourceLocation id, Ingredient input, ItemStack output, int processTime);

    /**
     * 娉ㄥ唽绛夌�诲瓙鐐夐厤鏂�
     *
     * @param id 配方 ID
     * @param input 输入材料
     * @param output 输出物品
     * @param processTime 处理时间（刻）
     * @return true 如果注册成功
     */
    boolean registerPlasmaFurnaceRecipe(ResourceLocation id, Ingredient input, ItemStack output, int processTime);

    /**
     * 注册聚变反应堆配方
     *
     * @param id 配方 ID
     * @param input1 输入材料1
     * @param input2 输入材料2
     * @param output 输出物品
     * @param energyRequired 所需能量
     * @param processTime 处理时间（刻）
     * @return true 如果注册成功
     */
    boolean registerFusionReactorRecipe(ResourceLocation id, Ingredient input1, Ingredient input2, ItemStack output, long energyRequired, int processTime);

    /**
     * 注册质量 Fabricator 配方
     *
     * @param id 配方 ID
     * @param input 输入材料
     * @param output 输出物品
     * @param processTime 处理时间（刻）
     * @return true 如果注册成功
     */
    boolean registerMassFabricatorRecipe(ResourceLocation id, Ingredient input, ItemStack output, int processTime);

    /**
     * 注册复制机配方
     *
     * @param id 配方 ID
     * @param input 输入材料
     * @param output 输出物品
     * @param processTime 处理时间（刻）
     * @return true 如果注册成功
     */
    boolean registerReplicatorRecipe(ResourceLocation id, Ingredient input, ItemStack output, int processTime);

    /**
     * 注册扫描机配方
     *
     * @param id 配方 ID
     * @param input 输入材料
     * @param output 输出物品
     * @param processTime 处理时间（刻）
     * @return true 如果注册成功
     */
    boolean registerScannerRecipe(ResourceLocation id, Ingredient input, ItemStack output, int processTime);

    boolean registerFluidRefiningRecipe(ResourceLocation id, Ingredient ingredient, net.neoforged.neoforge.fluids.FluidStack inputFluid, net.neoforged.neoforge.fluids.FluidStack outputFluid, int processTime, int energyPerTick);

    /**
     * 将运行时注册的所有配方同步到 RecipeManager
     *
     * <p>由于 Minecraft 的配方系统在服务器启动时从数据包加载，
     * 运行时注册的配方需要通过此方法手动同步到 RecipeManager。
     *
     * <p>建议在 {@link net.neoforged.neoforge.event.server.ServerStartedEvent} 中调用此方法。
     *
     * <p>使用示例：
     * <pre>{@code
     * @SubscribeEvent
     * public void onServerStarted(ServerStartedEvent event) {
     *     MioIcifAPI.instance().getRecipeRegistrationAPI().syncToRecipeManager();
     * }
     * }</pre>
     */
    void syncToRecipeManager();
}