package com.singularity_iteration.mio_icif.integration.jei;

import com.singularity_iteration.mio_icif.Blocks.mio_icif_blocks;
import com.singularity_iteration.mio_icif.Items.Armor.mio_icif_items_armors;
import com.singularity_iteration.mio_icif.Items.Cell.mio_icif_cells;
import com.singularity_iteration.mio_icif.Items.Normal.mio_icif_normal;
import com.singularity_iteration.mio_icif.Items.Tools.mio_icif_items_tools;
import com.singularity_iteration.mio_icif.Menu.Producer.IndustrialWorkbenchMenu;
import com.singularity_iteration.mio_icif.Menu.mio_icif_menus;
import com.singularity_iteration.mio_icif.Singularity_Iteration;
import com.singularity_iteration.mio_icif.api.item.IBatteryItem;
import com.singularity_iteration.mio_icif.integration.CuriosIntegration;
import com.singularity_iteration.mio_icif.integration.jei.category.*;
import com.singularity_iteration.mio_icif.recipe.mio_icif_ModRecipes;
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
        mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter<ItemStack> damageInterpreter =
            (itemStack, context) -> String.valueOf(itemStack.getDamageValue());
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
        registration.registerSubtypeInterpreter(mio_icif_normal.ADVCHARGEBAT_0.get(), damageInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_normal.CHARGEBAT_LEV0.get(), damageInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_normal.CRYSTAL_CHARGEBAT_LEV0.get(), damageInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_normal.CRYSTAL_LEV0.get(), batteryInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_normal.LAPOTRON_CRYSTAL_LEV0.get(), batteryInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_normal.LAMACRYSTAL_CHARGEBAT_LEV0.get(), damageInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_normal.SUPER_LAPOTRON_CRYSTAL.get(), damageInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_normal.CHARGING_SUPER_LAPOTRON_CRYSTAL.get(), damageInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_normal.LITHIUM_BATTERY.get(), batteryInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_normal.ADV_LITHIUM_BATTERY.get(), batteryInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_normal.THORIUM_BATTERY.get(), damageInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_normal.SOLAR_HELMET.get(), damageInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_normal.CF_SPRAYER.get(), damageInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_normal.CROP_SEED.get(), fluidCellInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_items_tools.WRENCH_ELC.get(), damageInterpreter);

        registration.registerSubtypeInterpreter(mio_icif_items_armors.ARMOR_BATPACK.get(), damageInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_items_armors.ARMOR_ADV_BATPACK.get(), damageInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_items_armors.ARMOR_ENERGYPACK.get(), damageInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_items_armors.ARMOR_LAPPACK.get(), damageInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_items_armors.ARMOR_NIGHTVISION_GOGGLES.get(), damageInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_items_armors.ARMOR_NANO_HELMET.get(), damageInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_items_armors.ARMOR_NANO_CHESTPLATE.get(), damageInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_items_armors.ARMOR_NANO_LEGGINGS.get(), damageInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_items_armors.ARMOR_NANO_BOOTS.get(), damageInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_items_armors.ARMOR_QUANTUM_HELMET.get(), damageInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_items_armors.ARMOR_QUANTUM_CHESTPLATE.get(), damageInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_items_armors.ARMOR_QUANTUM_LEGGINGS.get(), damageInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_items_armors.ARMOR_QUANTUM_BOOTS.get(), damageInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_items_armors.ARMOR_JETPACK_ELECTRIC.get(), damageInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_items_armors.ARMOR_DIVING_MASK.get(), damageInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_items_armors.ARMOR_ADVANCED_JETPACK.get(), damageInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_items_armors.ARMOR_HEAVY_QUANTUM_CHESTPLATE.get(), damageInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_items_armors.ARMOR_ADVANCED_QUANTUM_CHESTPLATE.get(), damageInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_items_armors.ARMOR_ADVANCED_SOLAR_HELMET.get(), damageInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_items_armors.ARMOR_HYBRID_SOLAR_HELMET.get(), damageInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_items_armors.ARMOR_ULTIMATE_SOLAR_HELMET.get(), damageInterpreter);

        registration.registerSubtypeInterpreter(mio_icif_blocks.BAT_BOX.get().asItem(), nbtInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_blocks.CESU.get().asItem(), nbtInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_blocks.MFE.get().asItem(), nbtInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_blocks.MFSU.get().asItem(), nbtInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_blocks.LESU.get().asItem(), nbtInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_blocks.EESU.get().asItem(), nbtInterpreter);
        registration.registerSubtypeInterpreter(mio_icif_blocks.GESU_CORE.get().asItem(), nbtInterpreter);

        registration.registerSubtypeInterpreter(mio_icif_cells.CELL_EMPTY.get(), fluidCellInterpreter);

        if (net.neoforged.fml.ModList.get().isLoaded("curios")) {
            try {
                registration.registerSubtypeInterpreter(CuriosIntegration.TRINKET_FIREPROOF_NECKLACE.get(), damageInterpreter);
                registration.registerSubtypeInterpreter(CuriosIntegration.TRINKET_ENERGY_CRYSTAL_BELT.get(), damageInterpreter);
                registration.registerSubtypeInterpreter(CuriosIntegration.TRINKET_LAPORTON_CRYSTAL_BELT.get(), damageInterpreter);
                registration.registerSubtypeInterpreter(CuriosIntegration.TRINKET_LIFE_SUPPORT_RING.get(), damageInterpreter);
                registration.registerSubtypeInterpreter(CuriosIntegration.TRINKET_FLIGHT_RING.get(), damageInterpreter);
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
            new mio_icif_EmptyToTankCategory(registration.getJeiHelpers().getGuiHelper()),
            new mio_icif_FillFromTankCategory(registration.getJeiHelpers().getGuiHelper()),
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
        registration.addRecipes(mio_icif_PowderCategory.POWDER_TYPE, powderRecipes.stream().map(r -> r.value()).toList());

        // 注册洗矿配方
        var washerRecipes = recipeManager.getAllRecipesFor(mio_icif_ModRecipes.WASHER_TYPE.get());
        registration.addRecipes(mio_icif_WasherCategory.WASHER_TYPE, washerRecipes.stream().map(r -> r.value()).toList());

        // 注册离心机配方?
        var centrifugeRecipes = recipeManager.getAllRecipesFor(mio_icif_ModRecipes.CENTRIFUGE_TYPE.get());
        registration.addRecipes(mio_icif_CentrifugeCategory.CENTRIFUGE_TYPE, centrifugeRecipes.stream().map(r -> r.value()).toList());

        // 注册提取机配方?
        var extractorRecipes = recipeManager.getAllRecipesFor(mio_icif_ModRecipes.EXTRACTOR_TYPE.get());
        registration.addRecipes(mio_icif_ExtractorCategory.EXTRACTOR_TYPE, extractorRecipes.stream().map(r -> r.value()).toList());

        // 注册压缩机配方?
        var compressorRecipes = recipeManager.getAllRecipesFor(mio_icif_ModRecipes.COMPRESSOR_TYPE.get());
        registration.addRecipes(mio_icif_CompressorCategory.COMPRESSOR_TYPE, compressorRecipes.stream().map(r -> r.value()).toList());

        // 注册方块切割机配方?
        var blockCutterRecipes = recipeManager.getAllRecipesFor(mio_icif_ModRecipes.BLOCK_CUTTER_TYPE.get());
        registration.addRecipes(mio_icif_BlockCutterCategory.BLOCK_CUTTER_TYPE, blockCutterRecipes.stream().map(r -> r.value()).toList());

        // 注册金属成型�?轧制配方
        var rollingRecipes = recipeManager.getAllRecipesFor(mio_icif_ModRecipes.ROLLING_TYPE.get());
        registration.addRecipes(mio_icif_RollingCategory.ROLLING_TYPE, rollingRecipes.stream().map(r -> r.value()).toList());

        // 注册金属成型�?切割配方
        var cuttingRecipes = recipeManager.getAllRecipesFor(mio_icif_ModRecipes.CUTTING_TYPE.get());
        registration.addRecipes(mio_icif_CuttingCategory.CUTTING_TYPE, cuttingRecipes.stream().map(r -> r.value()).toList());

        // 注册金属成型�?挤出配方
        var extrudingRecipes = recipeManager.getAllRecipesFor(mio_icif_ModRecipes.EXTRUDING_TYPE.get());
        registration.addRecipes(mio_icif_ExtrudingCategory.EXTRUDING_TYPE, extrudingRecipes.stream().map(r -> r.value()).toList());

        // 注册装罐机配方?
        var canningRecipes = recipeManager.getAllRecipesFor(mio_icif_ModRecipes.CANNING_TYPE.get());
        registration.addRecipes(mio_icif_CanningCategory.CANNING_TYPE, canningRecipes.stream().map(r -> r.value()).toList());

        // 注册空罐注液配方
        var emptyToTankRecipes = recipeManager.getAllRecipesFor(mio_icif_ModRecipes.EMPTY_TO_TANK_TYPE.get());
        registration.addRecipes(mio_icif_EmptyToTankCategory.EMPTY_TO_TANK_TYPE, emptyToTankRecipes.stream().map(r -> r.value()).toList());

        // 注册从罐抽液配方
        var fillFromTankRecipes = recipeManager.getAllRecipesFor(mio_icif_ModRecipes.FILL_FROM_TANK_TYPE.get());
        registration.addRecipes(mio_icif_FillFromTankCategory.FILL_FROM_TANK_TYPE, fillFromTankRecipes.stream().map(r -> r.value()).toList());

        // 注册混合配方
        var mixRecipes = recipeManager.getAllRecipesFor(mio_icif_ModRecipes.MIX_TYPE.get());
        registration.addRecipes(mio_icif_MixCategory.MIX_TYPE, mixRecipes.stream().map(r -> r.value()).toList());

        // 娉ㄥ唽楂樼倝閰嶆柟
        var blastFurnaceRecipes = recipeManager.getAllRecipesFor(mio_icif_ModRecipes.BLAST_FURNACE_TYPE.get());
        registration.addRecipes(mio_icif_BlastFurnaceCategory.BLAST_FURNACE_TYPE, blastFurnaceRecipes.stream().map(r -> r.value()).toList());

        // 注册分子重组仪配方
        var molecularTransformerRecipes = recipeManager.getAllRecipesFor(mio_icif_ModRecipes.MOLECULAR_TRANSFORMER_TYPE.get());
        registration.addRecipes(mio_icif_MolecularTransformerCategory.MOLECULAR_TRANSFORMER_TYPE, molecularTransformerRecipes.stream().map(r -> r.value()).toList());

        var neutronPolymerizerRecipes = recipeManager.getAllRecipesFor(com.singularity_iteration.mio_icif.recipe.generic.NeutronPolymerizerRecipes.NEUTRON_POLYMERIZER_TYPE.get());
        registration.addRecipes(mio_icif_NeutronPolymerizerCategory.NEUTRON_POLYMERIZER_TYPE, neutronPolymerizerRecipes.stream().map(r -> r.value()).toList());

        var fluidRefiningRecipes = recipeManager.getAllRecipesFor(com.singularity_iteration.mio_icif.recipe.fluid_refining.FluidRefiningRecipes.FLUID_REFINING_TYPE.get());
        registration.addRecipes(mio_icif_FluidRefiningCategory.FLUID_REFINING_TYPE, fluidRefiningRecipes.stream().map(r -> r.value()).toList());
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        // 注册GUI交互�?- 如有需要可以添加自定义GUI处理
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
        registration.addRecipeCatalyst(new ItemStack(mio_icif_blocks.CANNER_ELC.get()), mio_icif_EmptyToTankCategory.EMPTY_TO_TANK_TYPE);
        registration.addRecipeCatalyst(new ItemStack(mio_icif_blocks.CANNER_ELC.get()), mio_icif_FillFromTankCategory.FILL_FROM_TANK_TYPE);
        registration.addRecipeCatalyst(new ItemStack(mio_icif_blocks.CANNER_ELC.get()), mio_icif_MixCategory.MIX_TYPE);
        registration.addRecipeCatalyst(new ItemStack(mio_icif_blocks.BLAST_FURNACE.get()), mio_icif_BlastFurnaceCategory.BLAST_FURNACE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(mio_icif_blocks.MOLECULAR_TRANSFORMER.get()), mio_icif_MolecularTransformerCategory.MOLECULAR_TRANSFORMER_TYPE);
        registration.addRecipeCatalyst(new ItemStack(mio_icif_blocks.NEUTRON_POLYMERIZER.get()), mio_icif_NeutronPolymerizerCategory.NEUTRON_POLYMERIZER_TYPE);
        registration.addRecipeCatalyst(new ItemStack(mio_icif_blocks.OIL_REFINERY_ELC.get()), mio_icif_FluidRefiningCategory.FLUID_REFINING_TYPE);
    }
}