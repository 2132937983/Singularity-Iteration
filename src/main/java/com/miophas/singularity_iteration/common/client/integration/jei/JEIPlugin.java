package com.miophas.singularity_iteration.common.client.integration.jei;

import com.miophas.singularity_iteration.common.registry.mio_icif_blocks;
import com.miophas.singularity_iteration.common.item.armor.mio_icif_items_armors;
import com.miophas.singularity_iteration.common.item.cell.mio_icif_cells;
import com.miophas.singularity_iteration.common.item.normal.mio_icif_normal;
import com.miophas.singularity_iteration.common.item.tools.mio_icif_items_tools;
import com.miophas.singularity_iteration.common.menu.producer.IndustrialWorkbenchMenu;
import com.miophas.singularity_iteration.common.registry.mio_icif_menus;
import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.core.api.item.IBatteryItem;
import com.miophas.singularity_iteration.common.integration.CuriosIntegration;
import com.miophas.singularity_iteration.common.client.integration.jei.category.*;
import com.miophas.singularity_iteration.common.recipe.mio_icif_ModRecipes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.*;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

@JeiPlugin
@SuppressWarnings({"null", "deprecation"})
public class JEIPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "jei_plugin");
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter<ItemStack> batteryInterpreter =
            (itemStack, context) -> {
                if (itemStack.getItem() instanceof IBatteryItem bat) {
                    return String.valueOf(bat.getEnergy(itemStack));
                }
                return mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter.NONE;
            };
        // 能量感知子类型：能量物品（工具/装甲/电池/饰品）用“当前电量/最大电量”区分，
        // 其余物品退回耐久值区分。这样创造模式标签页里同时收录“空电”与“满电”两个版本时，
        // JEI 不会把它们判定为重复物品。
        mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter<ItemStack> energyInterpreter =
            (itemStack, context) -> {
                if (itemStack.getItem() instanceof IBatteryItem bat) {
                    return bat.getEnergy(itemStack) + "/" + bat.getMaxEnergy(itemStack);
                }
                return String.valueOf(itemStack.getDamageValue());
            };
        mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter<ItemStack> nbtInterpreter =
            (itemStack, context) -> {
                var beData = itemStack.get(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA);
                if (beData != null) {
                    return beData.copyTag().toString();
                }
                return mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter.NONE;
            };
        mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter<ItemStack> fluidCellInterpreter =
            (itemStack, context) -> {
                var customData = itemStack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
                if (customData != null) {
                    return customData.copyTag().toString();
                }
                return mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter.NONE;
            };

        registration.registerSubtypeInterpreter(mio_icif_normal.BAT_LEV0.get(), batteryInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_normal.ADVBAT_LEV0.get(), batteryInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_normal.ADVCHARGEBAT_0.get(), energyInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_normal.CHARGEBAT_LEV0.get(), energyInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_normal.CRYSTAL_CHARGEBAT_LEV0.get(), energyInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_normal.CRYSTAL_LEV0.get(), batteryInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_normal.LAPOTRON_CRYSTAL_LEV0.get(), batteryInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_normal.LAMACRYSTAL_CHARGEBAT_LEV0.get(), energyInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_normal.SUPER_LAPOTRON_CRYSTAL.get(), energyInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_normal.CHARGING_SUPER_LAPOTRON_CRYSTAL.get(), energyInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_normal.LITHIUM_BATTERY.get(), batteryInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_normal.ADV_LITHIUM_BATTERY.get(), batteryInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_normal.THORIUM_BATTERY.get(), energyInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_normal.SOLAR_HELMET.get(), energyInterpreter);
        // CF喷枪存储的是建筑泡沫流体（CUSTOM_DATA），按流体数据区分子类型
        registration.registerSubtypeInterpreter(mio_icif_normal.CF_SPRAYER.get(), fluidCellInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_normal.CROP_ANALYZER.get(), energyInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_normal.CROP_SEED.get(), fluidCellInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_items_tools.WRENCH_ELC.get(), energyInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_items_tools.DIAMOND_DRILLER.get(), energyInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_items_tools.TOOL_LASER_MINER.get(), energyInterpreter);

        registration.registerSubtypeInterpreter(mio_icif_items_armors.ARMOR_BATPACK.get(), energyInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_items_armors.ARMOR_ADV_BATPACK.get(), energyInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_items_armors.ARMOR_ENERGYPACK.get(), energyInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_items_armors.ARMOR_LAPPACK.get(), energyInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_items_armors.ARMOR_NIGHTVISION_GOGGLES.get(), energyInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_items_armors.ARMOR_NANO_HELMET.get(), energyInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_items_armors.ARMOR_NANO_CHESTPLATE.get(), energyInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_items_armors.ARMOR_NANO_LEGGINGS.get(), energyInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_items_armors.ARMOR_NANO_BOOTS.get(), energyInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_items_armors.ARMOR_QUANTUM_HELMET.get(), energyInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_items_armors.ARMOR_QUANTUM_CHESTPLATE.get(), energyInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_items_armors.ARMOR_QUANTUM_LEGGINGS.get(), energyInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_items_armors.ARMOR_QUANTUM_BOOTS.get(), energyInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_items_armors.ARMOR_JETPACK_ELECTRIC.get(), energyInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_items_armors.ARMOR_DIVING_MASK.get(), energyInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_items_armors.ARMOR_ADVANCED_JETPACK.get(), energyInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_items_armors.ARMOR_HEAVY_QUANTUM_CHESTPLATE.get(), energyInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_items_armors.ARMOR_ADVANCED_QUANTUM_CHESTPLATE.get(), energyInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_items_armors.ARMOR_ADVANCED_SOLAR_HELMET.get(), energyInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_items_armors.ARMOR_HYBRID_SOLAR_HELMET.get(), energyInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_items_armors.ARMOR_ULTIMATE_SOLAR_HELMET.get(), energyInterpreter);

        registration.registerSubtypeInterpreter(mio_icif_blocks.BAT_BOX.get().asItem(), nbtInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_blocks.CESU.get().asItem(), nbtInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_blocks.MFE.get().asItem(), nbtInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_blocks.MFSU.get().asItem(), nbtInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_blocks.LESU.get().asItem(), nbtInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_blocks.EESU.get().asItem(), nbtInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_blocks.GESU_CORE.get().asItem(), nbtInterpreter);
        // 充电座方块：同样通过 BLOCK_ENTITY_DATA 里的电量区分“空电/满电”
        registration.registerSubtypeInterpreter(mio_icif_blocks.BATBOX_CHARGER.get().asItem(), nbtInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_blocks.CESU_CHARGER.get().asItem(), nbtInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_blocks.MFE_CHARGER.get().asItem(), nbtInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_blocks.MFSU_CHARGER.get().asItem(), nbtInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_blocks.LESU_CHARGER.get().asItem(), nbtInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_blocks.EESU_CHARGER.get().asItem(), nbtInterpreter);

        registration.registerSubtypeInterpreter(mio_icif_cells.CELL_EMPTY.get(), fluidCellInterpreter);

        if (net.neoforged.fml.ModList.get().isLoaded("curios")) {
            try {
                registration.registerSubtypeInterpreter(CuriosIntegration.TRINKET_FIREPROOF_NECKLACE.get(), energyInterpreter);
                registration.registerSubtypeInterpreter(CuriosIntegration.TRINKET_ENERGY_CRYSTAL_BELT.get(), energyInterpreter);
                registration.registerSubtypeInterpreter(CuriosIntegration.TRINKET_LAPORTON_CRYSTAL_BELT.get(), energyInterpreter);
                registration.registerSubtypeInterpreter(CuriosIntegration.TRINKET_LIFE_SUPPORT_RING.get(), energyInterpreter);
                registration.registerSubtypeInterpreter(CuriosIntegration.TRINKET_FLIGHT_RING.get(), energyInterpreter);
            } catch (Exception e) {
            }
        }
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        // 注册所有配方类�?
        registration.addRecipeCategories(
            // 生产机器
            new mio_icif_PowderCategory(registration.getJeiHelpers().getGuiHelper()),
            new mio_icif_WasherCategory(registration.getJeiHelpers().getGuiHelper()),
            new mio_icif_CentrifugeCategory(registration.getJeiHelpers().getGuiHelper()),
            new mio_icif_ExtractorCategory(registration.getJeiHelpers().getGuiHelper()),
            new mio_icif_CompressorCategory(registration.getJeiHelpers().getGuiHelper()),
            new mio_icif_BlockCutterCategory(registration.getJeiHelpers().getGuiHelper()),
            // 金属成型�?
            new mio_icif_RollingCategory(registration.getJeiHelpers().getGuiHelper()),
            new mio_icif_CuttingCategory(registration.getJeiHelpers().getGuiHelper()),
            new mio_icif_ExtrudingCategory(registration.getJeiHelpers().getGuiHelper()),
            // 装罐�?
            new mio_icif_CanningCategory(registration.getJeiHelpers().getGuiHelper()),
            new mio_icif_MixCategory(registration.getJeiHelpers().getGuiHelper()),
            new mio_icif_BlastFurnaceCategory(registration.getJeiHelpers().getGuiHelper()),
            new mio_icif_MolecularTransformerCategory(registration.getJeiHelpers().getGuiHelper()),
            new mio_icif_NeutronPolymerizerCategory(registration.getJeiHelpers().getGuiHelper()),
            new mio_icif_FluidRefiningCategory(registration.getJeiHelpers().getGuiHelper())
        );
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;

        var recipeManager = level.getRecipeManager();

        // 注册打粉配方
        var powderRecipes = recipeManager.getAllRecipesFor(mio_icif_ModRecipes.POWDER_TYPE.get());
        registration.addRecipes(mio_icif_PowderCategory.POWDER_TYPE, powderRecipes.stream().toList());

        // 注册洗矿配方
        var washerRecipes = recipeManager.getAllRecipesFor(mio_icif_ModRecipes.WASHER_TYPE.get());
        registration.addRecipes(mio_icif_WasherCategory.WASHER_TYPE, washerRecipes.stream().toList());

        // 注册离心机配方?
        var centrifugeRecipes = recipeManager.getAllRecipesFor(mio_icif_ModRecipes.CENTRIFUGE_TYPE.get());
        registration.addRecipes(mio_icif_CentrifugeCategory.CENTRIFUGE_TYPE, centrifugeRecipes.stream().toList());

        // 注册提取机配方?
        var extractorRecipes = recipeManager.getAllRecipesFor(mio_icif_ModRecipes.EXTRACTOR_TYPE.get());
        registration.addRecipes(mio_icif_ExtractorCategory.EXTRACTOR_TYPE, extractorRecipes.stream().toList());

        // 注册压缩机配方?
        var compressorRecipes = recipeManager.getAllRecipesFor(mio_icif_ModRecipes.COMPRESSOR_TYPE.get());
        registration.addRecipes(mio_icif_CompressorCategory.COMPRESSOR_TYPE, compressorRecipes.stream().toList());

        // 注册方块切割机配方?
        var blockCutterRecipes = recipeManager.getAllRecipesFor(mio_icif_ModRecipes.BLOCK_CUTTER_TYPE.get());
        registration.addRecipes(mio_icif_BlockCutterCategory.BLOCK_CUTTER_TYPE, blockCutterRecipes.stream().toList());

        // 注册金属成型�?轧制配方
        var rollingRecipes = recipeManager.getAllRecipesFor(mio_icif_ModRecipes.ROLLING_TYPE.get());
        registration.addRecipes(mio_icif_RollingCategory.ROLLING_TYPE, rollingRecipes.stream().toList());

        // 注册金属成型�?切割配方
        var cuttingRecipes = recipeManager.getAllRecipesFor(mio_icif_ModRecipes.CUTTING_TYPE.get());
        registration.addRecipes(mio_icif_CuttingCategory.CUTTING_TYPE, cuttingRecipes.stream().toList());

        // 注册金属成型�?挤出配方
        var extrudingRecipes = recipeManager.getAllRecipesFor(mio_icif_ModRecipes.EXTRUDING_TYPE.get());
        registration.addRecipes(mio_icif_ExtrudingCategory.EXTRUDING_TYPE, extrudingRecipes.stream().toList());

        // 注册装罐机配方?
        var canningRecipes = recipeManager.getAllRecipesFor(mio_icif_ModRecipes.CANNING_TYPE.get());
        registration.addRecipes(mio_icif_CanningCategory.CANNING_TYPE, canningRecipes.stream().toList());

        // 注册混合配方
        var mixRecipes = recipeManager.getAllRecipesFor(mio_icif_ModRecipes.MIX_TYPE.get());
        registration.addRecipes(mio_icif_MixCategory.MIX_TYPE, mixRecipes.stream().toList());

        // 注册高炉配方
        var blastFurnaceRecipes = recipeManager.getAllRecipesFor(mio_icif_ModRecipes.BLAST_FURNACE_TYPE.get());
        registration.addRecipes(mio_icif_BlastFurnaceCategory.BLAST_FURNACE_TYPE, blastFurnaceRecipes.stream().toList());

        // 注册分子重组仪配方
        var molecularTransformerRecipes = recipeManager.getAllRecipesFor(mio_icif_ModRecipes.MOLECULAR_TRANSFORMER_TYPE.get());
        registration.addRecipes(mio_icif_MolecularTransformerCategory.MOLECULAR_TRANSFORMER_TYPE, molecularTransformerRecipes.stream().toList());

        var neutronPolymerizerRecipes = recipeManager.getAllRecipesFor(com.miophas.singularity_iteration.common.recipe.generic.NeutronPolymerizerRecipes.NEUTRON_POLYMERIZER_TYPE.get());
        registration.addRecipes(mio_icif_NeutronPolymerizerCategory.NEUTRON_POLYMERIZER_TYPE, neutronPolymerizerRecipes.stream().toList());

        var fluidRefiningRecipes = recipeManager.getAllRecipesFor(com.miophas.singularity_iteration.common.recipe.fluid_refining.FluidRefiningRecipes.FLUID_REFINING_TYPE.get());
        registration.addRecipes(mio_icif_FluidRefiningCategory.FLUID_REFINING_TYPE, fluidRefiningRecipes.stream().toList());
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        // 注册GUI交互处理
        // 高级采矿机：支持把 JEI 物品面板里的物品拖入黑白名单过滤槽（幽灵拖拽）
        registration.addGhostIngredientHandler(
            com.miophas.singularity_iteration.common.client.screen.mio_icif_gui_advanced_miner_elc.class,
            new AdvancedMinerGhostHandler());
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        // 注册工业工作台的配方转移支持（JEI "+" 按钮）
        registration.addRecipeTransferHandler(
            IndustrialWorkbenchMenu.class,
            mio_icif_menus.INDUSTRIAL_WORKBENCH_MENU_TYPE.get(),
            RecipeTypes.CRAFTING,
            1,  // 合成网格起始槽位（Menu 索引 1）
            9,  // 合成网格槽位数量（3x3 = 9）
            10, // 玩家背包起始槽位（Menu 索引 10）
            36  // 玩家背包槽位数量
        );

        // 批量工作台：JEI "+" 只写入上方的 3x3 影子模板格，不消耗玩家物品（对齐 IC2 TransferHandlerBatchCrafter）
        registration.addRecipeTransferHandler(new BatchCrafterTransferHandler(), RecipeTypes.CRAFTING);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        // 注册配方催化剂（机器方块作为催化剂）
        registration.addRecipeCatalyst(new ItemStack(mio_icif_blocks.POWDER_ELC.get()), mio_icif_PowderCategory.POWDER_TYPE);
        registration.addRecipeCatalyst(new ItemStack(mio_icif_blocks.WASHER_ELC.get()), mio_icif_WasherCategory.WASHER_TYPE);
        registration.addRecipeCatalyst(new ItemStack(mio_icif_blocks.CENTRIFUGE_ELC.get()), mio_icif_CentrifugeCategory.CENTRIFUGE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(mio_icif_blocks.EXTRACTOR_ELC.get()), mio_icif_ExtractorCategory.EXTRACTOR_TYPE);
        registration.addRecipeCatalyst(new ItemStack(mio_icif_blocks.COMPRESSOR_ELC.get()), mio_icif_CompressorCategory.COMPRESSOR_TYPE);
        registration.addRecipeCatalyst(new ItemStack(mio_icif_blocks.BLOCK_CUTTER.get()), mio_icif_BlockCutterCategory.BLOCK_CUTTER_TYPE);
        registration.addRecipeCatalyst(new ItemStack(mio_icif_blocks.METAL_FORMER.get()), mio_icif_RollingCategory.ROLLING_TYPE);
        registration.addRecipeCatalyst(new ItemStack(mio_icif_blocks.METAL_FORMER.get()), mio_icif_CuttingCategory.CUTTING_TYPE);
        registration.addRecipeCatalyst(new ItemStack(mio_icif_blocks.METAL_FORMER.get()), mio_icif_ExtrudingCategory.EXTRUDING_TYPE);
        registration.addRecipeCatalyst(new ItemStack(mio_icif_blocks.CANNER_ELC.get()), mio_icif_CanningCategory.CANNING_TYPE);
        registration.addRecipeCatalyst(new ItemStack(mio_icif_blocks.CANNER_ELC.get()), mio_icif_MixCategory.MIX_TYPE);
        registration.addRecipeCatalyst(new ItemStack(mio_icif_blocks.BLAST_FURNACE.get()), mio_icif_BlastFurnaceCategory.BLAST_FURNACE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(mio_icif_blocks.MOLECULAR_TRANSFORMER.get()), mio_icif_MolecularTransformerCategory.MOLECULAR_TRANSFORMER_TYPE);
        registration.addRecipeCatalyst(new ItemStack(mio_icif_blocks.NEUTRON_POLYMERIZER.get()), mio_icif_NeutronPolymerizerCategory.NEUTRON_POLYMERIZER_TYPE);
        registration.addRecipeCatalyst(new ItemStack(mio_icif_blocks.OIL_REFINERY_ELC.get()), mio_icif_FluidRefiningCategory.FLUID_REFINING_TYPE);
    }
}