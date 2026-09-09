package com.singularity_iteration.mio_icif.recipe;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import com.singularity_iteration.mio_icif.recipe.canner.canning.CanningRecipe;
import com.singularity_iteration.mio_icif.recipe.canner.canning.CanningRecipes;
import com.singularity_iteration.mio_icif.recipe.canner.canning.DynamicCanningRecipe;
import com.singularity_iteration.mio_icif.recipe.canner.empty_to_tank.EmptyToTankRecipe;
import com.singularity_iteration.mio_icif.recipe.canner.empty_to_tank.EmptyToTankRecipes;
import com.singularity_iteration.mio_icif.recipe.canner.fill_from_tank.FillFromTankRecipe;
import com.singularity_iteration.mio_icif.recipe.canner.fill_from_tank.FillFromTankRecipes;
import com.singularity_iteration.mio_icif.recipe.canner.mix.MixRecipe;
import com.singularity_iteration.mio_icif.recipe.canner.mix.MixRecipes;
import com.singularity_iteration.mio_icif.recipe.compressor.mio_icif_CompressorRecipe;
import com.singularity_iteration.mio_icif.recipe.compressor.mio_icif_CompressorRecipes;
import com.singularity_iteration.mio_icif.recipe.extractor.mio_icif_ExtractorRecipe;
import com.singularity_iteration.mio_icif.recipe.extractor.mio_icif_ExtractorRecipeSerializer;
import com.singularity_iteration.mio_icif.recipe.washer.mio_icif_WasherRecipe;
import com.singularity_iteration.mio_icif.recipe.washer.mio_icif_WasherRecipes;
import com.singularity_iteration.mio_icif.recipe.centrifuge.mio_icif_CentrifugeRecipe;
import com.singularity_iteration.mio_icif.recipe.centrifuge.mio_icif_CentrifugeRecipes;
import com.singularity_iteration.mio_icif.recipe.block_cutter.mio_icif_BlockCutterRecipe;
import com.singularity_iteration.mio_icif.recipe.block_cutter.mio_icif_BlockCutterRecipes;
import com.singularity_iteration.mio_icif.recipe.metal_former.rolling.mio_icif_RollingRecipe;
import com.singularity_iteration.mio_icif.recipe.metal_former.rolling.mio_icif_RollingRecipes;
import com.singularity_iteration.mio_icif.recipe.metal_former.cutting.mio_icif_CuttingRecipe;
import com.singularity_iteration.mio_icif.recipe.metal_former.cutting.mio_icif_CuttingRecipes;
import com.singularity_iteration.mio_icif.recipe.metal_former.extruding.mio_icif_ExtrudingRecipe;
import com.singularity_iteration.mio_icif.recipe.metal_former.extruding.mio_icif_ExtrudingRecipes;
import com.singularity_iteration.mio_icif.recipe.blast_furnace.mio_icif_BlastFurnaceRecipe;
import com.singularity_iteration.mio_icif.recipe.blast_furnace.mio_icif_BlastFurnaceRecipes;
import com.singularity_iteration.mio_icif.recipe.molecular_transformer.mio_icif_MolecularTransformerRecipe;
import com.singularity_iteration.mio_icif.recipe.molecular_transformer.mio_icif_MolecularTransformerRecipes;
import com.singularity_iteration.mio_icif.recipe.generic.AbstractSingleInputRecipe;
import com.singularity_iteration.mio_icif.recipe.generic.NeutronPolymerizerRecipes;
import com.singularity_iteration.mio_icif.recipe.fluid_refining.FluidRefiningRecipes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 配方注册器
 * 统一管理所有自定义配方的注册
 */
@SuppressWarnings("null")
public class mio_icif_ModRecipes {

    // 配方类型注册器
public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
        DeferredRegister.create(BuiltInRegistries.RECIPE_TYPE, Singularity_Iteration.MOD_ID);

    // 配方序列化器注册器
public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
        DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, Singularity_Iteration.MOD_ID);

    // 注册打粉配方类型
    public static final DeferredHolder<RecipeType<?>, RecipeType<mio_icif_PowderRecipe>> POWDER_TYPE =
        RECIPE_TYPES.register("powder", () -> new RecipeType<mio_icif_PowderRecipe>() {
            @Override
            public String toString() {
                return "powder";
            }
        });

    // 注册打粉配方序列化器
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<mio_icif_PowderRecipe>> POWDER_SERIALIZER =
        RECIPE_SERIALIZERS.register("powder", mio_icif_PowderRecipeSerializer::new);

    // 注册洗矿配方类型（委托给washer包）
    public static final DeferredHolder<RecipeType<?>, RecipeType<mio_icif_WasherRecipe>> WASHER_TYPE =
        mio_icif_WasherRecipes.WASHER_TYPE;

    // 注册洗矿配方序列化器（委托给washer包）
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<mio_icif_WasherRecipe>> WASHER_SERIALIZER =
        mio_icif_WasherRecipes.WASHER_SERIALIZER;

    // 注册热能离心机配方类型（委托给centrifuge包）
    public static final DeferredHolder<RecipeType<?>, RecipeType<mio_icif_CentrifugeRecipe>> CENTRIFUGE_TYPE =
        mio_icif_CentrifugeRecipes.CENTRIFUGE_TYPE;

    // 注册热能离心机配方序列化器（委托给centrifuge包）
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<mio_icif_CentrifugeRecipe>> CENTRIFUGE_SERIALIZER =
        mio_icif_CentrifugeRecipes.CENTRIFUGE_SERIALIZER;

    // 注册提取机配方类型
public static final DeferredHolder<RecipeType<?>, RecipeType<mio_icif_ExtractorRecipe>> EXTRACTOR_TYPE =
        RECIPE_TYPES.register("extractor", () -> new RecipeType<mio_icif_ExtractorRecipe>() {
            @Override
            public String toString() {
                return "extractor";
            }
        });

    // 注册提取机配方序列化器
public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<mio_icif_ExtractorRecipe>> EXTRACTOR_SERIALIZER =
        RECIPE_SERIALIZERS.register("extractor", mio_icif_ExtractorRecipeSerializer::new);

    // 注册压缩机配方类型（委托给compressor包）
    public static final DeferredHolder<RecipeType<?>, RecipeType<mio_icif_CompressorRecipe>> COMPRESSOR_TYPE =
        mio_icif_CompressorRecipes.COMPRESSOR_TYPE;

    // 注册压缩机配方序列化器（委托给compressor包）
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<mio_icif_CompressorRecipe>> COMPRESSOR_SERIALIZER =
        mio_icif_CompressorRecipes.COMPRESSOR_SERIALIZER;

    // 注册装罐机配方类型（委托给canner包）
    public static final DeferredHolder<RecipeType<?>, RecipeType<CanningRecipe>> CANNING_TYPE =
        CanningRecipes.CANNING_TYPE;
    public static final DeferredHolder<RecipeType<?>, RecipeType<EmptyToTankRecipe>> EMPTY_TO_TANK_TYPE =
        EmptyToTankRecipes.EMPTY_TO_TANK_TYPE;
    public static final DeferredHolder<RecipeType<?>, RecipeType<FillFromTankRecipe>> FILL_FROM_TANK_TYPE =
        FillFromTankRecipes.FILL_FROM_TANK_TYPE;
    public static final DeferredHolder<RecipeType<?>, RecipeType<MixRecipe>> MIX_TYPE =
        MixRecipes.MIX_TYPE;

    // 注册装罐机配方序列化器（委托给canner包）
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CanningRecipe>> CANNING_SERIALIZER =
        CanningRecipes.CANNING_SERIALIZER;
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<EmptyToTankRecipe>> EMPTY_TO_TANK_SERIALIZER =
        EmptyToTankRecipes.EMPTY_TO_TANK_SERIALIZER;
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<FillFromTankRecipe>> FILL_FROM_TANK_SERIALIZER =
        FillFromTankRecipes.FILL_FROM_TANK_SERIALIZER;
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MixRecipe>> MIX_SERIALIZER =
        MixRecipes.MIX_SERIALIZER;

    // 注册动态装罐配方类型和序列化器
    public static final DeferredHolder<RecipeType<?>, RecipeType<DynamicCanningRecipe>> DYNAMIC_CANNING_TYPE =
        CanningRecipes.DYNAMIC_CANNING_TYPE;
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<DynamicCanningRecipe>> DYNAMIC_CANNING_SERIALIZER =
        CanningRecipes.DYNAMIC_CANNING_SERIALIZER;

    // 注册冷凝模块修复配方类型
    public static final DeferredHolder<RecipeType<?>, RecipeType<mio_icif_CondensatorRepairRecipe>> CONDENSATOR_REPAIR_TYPE =
        RECIPE_TYPES.register("condensator_repair", () -> new RecipeType<mio_icif_CondensatorRepairRecipe>() {
            @Override
            public String toString() {
                return "condensator_repair";
            }
        });

    // 注册冷凝模块修复配方序列化器
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<mio_icif_CondensatorRepairRecipe>> CONDENSATOR_REPAIR_SERIALIZER =
        RECIPE_SERIALIZERS.register("condensator_repair", () -> mio_icif_CondensatorRepairRecipeSerializer.INSTANCE);

    // 注册方块切割机配方
public static final DeferredHolder<RecipeType<?>, RecipeType<mio_icif_BlockCutterRecipe>> BLOCK_CUTTER_TYPE =
        mio_icif_BlockCutterRecipes.BLOCK_CUTTER_TYPE;

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<mio_icif_BlockCutterRecipe>> BLOCK_CUTTER_SERIALIZER =
        mio_icif_BlockCutterRecipes.BLOCK_CUTTER_SERIALIZER;

    // 注册金属成型机配方
public static final DeferredHolder<RecipeType<?>, RecipeType<mio_icif_RollingRecipe>> ROLLING_TYPE =
        mio_icif_RollingRecipes.ROLLING_TYPE;
    public static final DeferredHolder<RecipeType<?>, RecipeType<mio_icif_CuttingRecipe>> CUTTING_TYPE =
        mio_icif_CuttingRecipes.CUTTING_TYPE;
    public static final DeferredHolder<RecipeType<?>, RecipeType<mio_icif_ExtrudingRecipe>> EXTRUDING_TYPE =
        mio_icif_ExtrudingRecipes.EXTRUDING_TYPE;

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<mio_icif_RollingRecipe>> ROLLING_SERIALIZER =
        mio_icif_RollingRecipes.ROLLING_SERIALIZER;
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<mio_icif_CuttingRecipe>> CUTTING_SERIALIZER =
        mio_icif_CuttingRecipes.CUTTING_SERIALIZER;
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<mio_icif_ExtrudingRecipe>> EXTRUDING_SERIALIZER =
        mio_icif_ExtrudingRecipes.EXTRUDING_SERIALIZER;

    // 注册高炉配方
    public static final DeferredHolder<RecipeType<?>, RecipeType<mio_icif_BlastFurnaceRecipe>> BLAST_FURNACE_TYPE =
        mio_icif_BlastFurnaceRecipes.BLAST_FURNACE_TYPE;
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<mio_icif_BlastFurnaceRecipe>> BLAST_FURNACE_SERIALIZER =
        mio_icif_BlastFurnaceRecipes.BLAST_FURNACE_SERIALIZER;

    // 注册分子重组仪配方
    public static final DeferredHolder<RecipeType<?>, RecipeType<mio_icif_MolecularTransformerRecipe>> MOLECULAR_TRANSFORMER_TYPE =
        mio_icif_MolecularTransformerRecipes.MOLECULAR_TRANSFORMER_TYPE;
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<mio_icif_MolecularTransformerRecipe>> MOLECULAR_TRANSFORMER_SERIALIZER =
        mio_icif_MolecularTransformerRecipes.MOLECULAR_TRANSFORMER_SERIALIZER;

 // 注册电炉配方
    public static final DeferredHolder<RecipeType<?>, RecipeType<com.singularity_iteration.mio_icif.recipe.generic.ElectricFurnaceRecipe>> ELECTRIC_FURNACE_TYPE =
        com.singularity_iteration.mio_icif.recipe.generic.ElectricFurnaceRecipes.ELECTRIC_FURNACE_TYPE;
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AbstractSingleInputRecipe<mio_icif_SingleItemRecipeInput>>> ELECTRIC_FURNACE_SERIALIZER =
        com.singularity_iteration.mio_icif.recipe.generic.ElectricFurnaceRecipes.ELECTRIC_FURNACE_SERIALIZER;

    // 注册回收机配方
    public static final DeferredHolder<RecipeType<?>, RecipeType<com.singularity_iteration.mio_icif.recipe.generic.RecyclerRecipe>> RECYCLER_TYPE =
        com.singularity_iteration.mio_icif.recipe.generic.RecyclerRecipes.RECYCLER_TYPE;
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AbstractSingleInputRecipe<mio_icif_SingleItemRecipeInput>>> RECYCLER_SERIALIZER =
        com.singularity_iteration.mio_icif.recipe.generic.RecyclerRecipes.RECYCLER_SERIALIZER;

    // 注册搅拌机配方
    public static final DeferredHolder<RecipeType<?>, RecipeType<com.singularity_iteration.mio_icif.recipe.generic.BlenderRecipe>> BLENDER_TYPE =
        com.singularity_iteration.mio_icif.recipe.generic.BlenderRecipes.BLENDER_TYPE;
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AbstractSingleInputRecipe<mio_icif_SingleItemRecipeInput>>> BLENDER_SERIALIZER =
        com.singularity_iteration.mio_icif.recipe.generic.BlenderRecipes.BLENDER_SERIALIZER;

    // 注册发酵机配方
    public static final DeferredHolder<RecipeType<?>, RecipeType<com.singularity_iteration.mio_icif.recipe.generic.FermenterRecipe>> FERMENTER_TYPE =
        com.singularity_iteration.mio_icif.recipe.generic.FermenterRecipes.FERMENTER_TYPE;
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AbstractSingleInputRecipe<mio_icif_SingleItemRecipeInput>>> FERMENTER_SERIALIZER =
        com.singularity_iteration.mio_icif.recipe.generic.FermenterRecipes.FERMENTER_SERIALIZER;

    // 注册电解机配方
    public static final DeferredHolder<RecipeType<?>, RecipeType<com.singularity_iteration.mio_icif.recipe.generic.ElectrolyzerRecipe>> ELECTROLYZER_TYPE =
        com.singularity_iteration.mio_icif.recipe.generic.ElectrolyzerRecipes.ELECTROLYZER_TYPE;
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AbstractSingleInputRecipe<mio_icif_SingleItemRecipeInput>>> ELECTROLYZER_SERIALIZER =
        com.singularity_iteration.mio_icif.recipe.generic.ElectrolyzerRecipes.ELECTROLYZER_SERIALIZER;

    // 注册车床配方
    public static final DeferredHolder<RecipeType<?>, RecipeType<com.singularity_iteration.mio_icif.recipe.generic.LatheRecipe>> LATHE_TYPE =
        com.singularity_iteration.mio_icif.recipe.generic.LatheRecipes.LATHE_TYPE;
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AbstractSingleInputRecipe<mio_icif_SingleItemRecipeInput>>> LATHE_SERIALIZER =
        com.singularity_iteration.mio_icif.recipe.generic.LatheRecipes.LATHE_SERIALIZER;

    // 注册焊接机配方
    public static final DeferredHolder<RecipeType<?>, RecipeType<com.singularity_iteration.mio_icif.recipe.generic.WelderRecipe>> WELDER_TYPE =
        com.singularity_iteration.mio_icif.recipe.generic.WelderRecipes.WELDER_TYPE;
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AbstractSingleInputRecipe<mio_icif_SingleItemRecipeInput>>> WELDER_SERIALIZER =
        com.singularity_iteration.mio_icif.recipe.generic.WelderRecipes.WELDER_SERIALIZER;

    // 注册激光雕刻机配方
    public static final DeferredHolder<RecipeType<?>, RecipeType<com.singularity_iteration.mio_icif.recipe.generic.LaserEngraverRecipe>> LASER_ENGRAVER_TYPE =
        com.singularity_iteration.mio_icif.recipe.generic.LaserEngraverRecipes.LASER_ENGRAVER_TYPE;
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AbstractSingleInputRecipe<mio_icif_SingleItemRecipeInput>>> LASER_ENGRAVER_SERIALIZER =
        com.singularity_iteration.mio_icif.recipe.generic.LaserEngraverRecipes.LASER_ENGRAVER_SERIALIZER;

    // 注册精密组装机配方
    public static final DeferredHolder<RecipeType<?>, RecipeType<com.singularity_iteration.mio_icif.recipe.generic.PrecisionAssemblerRecipe>> PRECISION_ASSEMBLER_TYPE =
        com.singularity_iteration.mio_icif.recipe.generic.PrecisionAssemblerRecipes.PRECISION_ASSEMBLER_TYPE;
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AbstractSingleInputRecipe<mio_icif_SingleItemRecipeInput>>> PRECISION_ASSEMBLER_SERIALIZER =
        com.singularity_iteration.mio_icif.recipe.generic.PrecisionAssemblerRecipes.PRECISION_ASSEMBLER_SERIALIZER;

    // 注册真空冷冻机配方
    public static final DeferredHolder<RecipeType<?>, RecipeType<com.singularity_iteration.mio_icif.recipe.generic.VacuumFreezerRecipe>> VACUUM_FREEZER_TYPE =
        com.singularity_iteration.mio_icif.recipe.generic.VacuumFreezerRecipes.VACUUM_FREEZER_TYPE;
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AbstractSingleInputRecipe<mio_icif_SingleItemRecipeInput>>> VACUUM_FREEZER_SERIALIZER =
        com.singularity_iteration.mio_icif.recipe.generic.VacuumFreezerRecipes.VACUUM_FREEZER_SERIALIZER;

    // 注册等离子炉配方
    public static final DeferredHolder<RecipeType<?>, RecipeType<com.singularity_iteration.mio_icif.recipe.generic.PlasmaFurnaceRecipe>> PLASMA_FURNACE_TYPE =
        com.singularity_iteration.mio_icif.recipe.generic.PlasmaFurnaceRecipes.PLASMA_FURNACE_TYPE;
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AbstractSingleInputRecipe<mio_icif_SingleItemRecipeInput>>> PLASMA_FURNACE_SERIALIZER =
        com.singularity_iteration.mio_icif.recipe.generic.PlasmaFurnaceRecipes.PLASMA_FURNACE_SERIALIZER;

    // 注册物质生成机配方
    public static final DeferredHolder<RecipeType<?>, RecipeType<com.singularity_iteration.mio_icif.recipe.generic.MassFabricatorRecipe>> MASS_FABRICATOR_TYPE =
        com.singularity_iteration.mio_icif.recipe.generic.MassFabricatorRecipes.MASS_FABRICATOR_TYPE;
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AbstractSingleInputRecipe<mio_icif_SingleItemRecipeInput>>> MASS_FABRICATOR_SERIALIZER =
        com.singularity_iteration.mio_icif.recipe.generic.MassFabricatorRecipes.MASS_FABRICATOR_SERIALIZER;

    // 注册复制机配方
    public static final DeferredHolder<RecipeType<?>, RecipeType<com.singularity_iteration.mio_icif.recipe.generic.ReplicatorRecipe>> REPLICATOR_TYPE =
        com.singularity_iteration.mio_icif.recipe.generic.ReplicatorRecipes.REPLICATOR_TYPE;
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AbstractSingleInputRecipe<mio_icif_SingleItemRecipeInput>>> REPLICATOR_SERIALIZER =
        com.singularity_iteration.mio_icif.recipe.generic.ReplicatorRecipes.REPLICATOR_SERIALIZER;

    // 注册扫描机配方
    public static final DeferredHolder<RecipeType<?>, RecipeType<com.singularity_iteration.mio_icif.recipe.generic.ScannerRecipe>> SCANNER_TYPE =
        com.singularity_iteration.mio_icif.recipe.generic.ScannerRecipes.SCANNER_TYPE;
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AbstractSingleInputRecipe<mio_icif_SingleItemRecipeInput>>> SCANNER_SERIALIZER =
        com.singularity_iteration.mio_icif.recipe.generic.ScannerRecipes.SCANNER_SERIALIZER;

    // 注册聚变反应堆配方
    public static final DeferredHolder<RecipeType<?>, RecipeType<com.singularity_iteration.mio_icif.recipe.generic.FusionReactorRecipe>> FUSION_REACTOR_TYPE =
        com.singularity_iteration.mio_icif.recipe.generic.FusionReactorRecipes.FUSION_REACTOR_TYPE;
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<com.singularity_iteration.mio_icif.recipe.generic.FusionReactorRecipe>> FUSION_REACTOR_SERIALIZER =
        com.singularity_iteration.mio_icif.recipe.generic.FusionReactorRecipes.FUSION_REACTOR_SERIALIZER;

    /**
     * 注册所有配方类型和序列化器
     * @param eventBus 事件总线
     */
    public static void register(IEventBus eventBus) {
        RECIPE_TYPES.register(eventBus);
        RECIPE_SERIALIZERS.register(eventBus);
        // 注册洗矿配方
        mio_icif_WasherRecipes.register(eventBus);
        // 注册热能离心机配方
    mio_icif_CentrifugeRecipes.register(eventBus);
        // 注册压缩机配方
    mio_icif_CompressorRecipes.register(eventBus);
        // 注册装罐机配方
    CanningRecipes.register(eventBus);
        EmptyToTankRecipes.register(eventBus);
        FillFromTankRecipes.register(eventBus);
        MixRecipes.register(eventBus);
        // 注册方块切割机配方
    mio_icif_BlockCutterRecipes.register(eventBus);
        // 注册金属成型机配方
    mio_icif_RollingRecipes.register(eventBus);
        mio_icif_CuttingRecipes.register(eventBus);
        mio_icif_ExtrudingRecipes.register(eventBus);
        // 注册高炉配方
        mio_icif_BlastFurnaceRecipes.register(eventBus);
        // 注册分子重组仪配方
        mio_icif_MolecularTransformerRecipes.register(eventBus);
        // 注册粒子聚合发生器配方
        NeutronPolymerizerRecipes.register(eventBus);
        // 注册流体精炼配方
        FluidRefiningRecipes.RECIPE_TYPES.register(eventBus);
        FluidRefiningRecipes.RECIPE_SERIALIZERS.register(eventBus);
    }
}