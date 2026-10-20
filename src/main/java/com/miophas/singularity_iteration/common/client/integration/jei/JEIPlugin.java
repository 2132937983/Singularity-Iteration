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

    /** 0.1.8.0: the tactical map opens loot item recipes through the runtime (see JeiBridge). */
    @Override
    public void onRuntimeAvailable(mezz.jei.api.runtime.IJeiRuntime jeiRuntime) {
        JeiBridge.runtime = jeiRuntime;
    }

    @Override
    public void onRuntimeUnavailable() {
        JeiBridge.runtime = null;
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter<ItemStack> batteryInterpreter =
            (itemStack, context) -> {
                // Recipe lookups match on the item alone: a half-charged tool must still find its recipes/uses.
                if (context == mezz.jei.api.ingredients.subtypes.UidContext.Recipe) return mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter.NONE;
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
                if (context == mezz.jei.api.ingredients.subtypes.UidContext.Recipe) return mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter.NONE;
                if (itemStack.getItem() instanceof IBatteryItem bat) {
                    return bat.getEnergy(itemStack) + "/" + bat.getMaxEnergy(itemStack);
                }
                return String.valueOf(itemStack.getDamageValue());
            };
        mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter<ItemStack> nbtInterpreter =
            (itemStack, context) -> {
                if (context == mezz.jei.api.ingredients.subtypes.UidContext.Recipe) return mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter.NONE;
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
        // 0.1.9.2: each category is built on its own; a failing one is logged and skipped, so the
        // others still register and JEI never gets an empty array (it rejects one).
        var gui = registration.getJeiHelpers().getGuiHelper();
        java.util.List<mezz.jei.api.recipe.category.IRecipeCategory<?>> categories = new java.util.ArrayList<>();
        category(categories, "mio_icif_PowderCategory", () -> new mio_icif_PowderCategory(gui));
        category(categories, "mio_icif_WasherCategory", () -> new mio_icif_WasherCategory(gui));
        category(categories, "mio_icif_CentrifugeCategory", () -> new mio_icif_CentrifugeCategory(gui));
        category(categories, "mio_icif_ExtractorCategory", () -> new mio_icif_ExtractorCategory(gui));
        category(categories, "mio_icif_CompressorCategory", () -> new mio_icif_CompressorCategory(gui));
        category(categories, "mio_icif_BlockCutterCategory", () -> new mio_icif_BlockCutterCategory(gui));
        category(categories, "mio_icif_RollingCategory", () -> new mio_icif_RollingCategory(gui));
        category(categories, "mio_icif_CuttingCategory", () -> new mio_icif_CuttingCategory(gui));
        category(categories, "mio_icif_ExtrudingCategory", () -> new mio_icif_ExtrudingCategory(gui));
        category(categories, "mio_icif_CanningCategory", () -> new mio_icif_CanningCategory(gui));
        category(categories, "mio_icif_MixCategory", () -> new mio_icif_MixCategory(gui));
        category(categories, "mio_icif_BlastFurnaceCategory", () -> new mio_icif_BlastFurnaceCategory(gui));
        category(categories, "mio_icif_MolecularTransformerCategory", () -> new mio_icif_MolecularTransformerCategory(gui));
        category(categories, "mio_icif_NeutronPolymerizerCategory", () -> new mio_icif_NeutronPolymerizerCategory(gui));
        category(categories, "mio_icif_FluidRefiningCategory", () -> new mio_icif_FluidRefiningCategory(gui));
        category(categories, "mio_icif_FermenterCategory", () -> new mio_icif_FermenterCategory(gui));
        category(categories, "mio_icif_LatheCategory", () -> new mio_icif_LatheCategory(gui));
        category(categories, "mio_icif_CannerTankCategory(fill)", () -> new mio_icif_CannerTankCategory(gui, false));
        category(categories, "mio_icif_CannerTankCategory(empty)", () -> new mio_icif_CannerTankCategory(gui, true));
        // One category per machine with hardcoded outputs, tall enough for its longest note list.
        java.util.Map<net.minecraft.world.level.ItemLike, Integer> lines = new java.util.LinkedHashMap<>();
        for (var conversion : conversions()) lines.merge(conversion.machine(), conversion.notes().size(), Math::max);
        lines.forEach((machine, count) -> category(categories, "conversion " + machine,
            () -> new mio_icif_MachineConversionCategory(gui, machine, count)));
        if (!categories.isEmpty()) registration.addRecipeCategories(categories.toArray(new mezz.jei.api.recipe.category.IRecipeCategory<?>[0]));
    }

    private static void category(java.util.List<mezz.jei.api.recipe.category.IRecipeCategory<?>> out, String name,
                                 java.util.function.Supplier<mezz.jei.api.recipe.category.IRecipeCategory<?>> factory) {
        try {
            out.add(factory.get());
        } catch (RuntimeException | LinkageError e) {
            com.miophas.singularity_iteration.core.runtime.diagnostics.QuietFailures.report("jei-category-" + name, "Building the JEI category " + name, e);
        }
    }

    /** The machines' own conversions ({@link com.miophas.singularity_iteration.common.blockentity.MachineConversions}); empty if building them fails. */
    private static java.util.List<com.miophas.singularity_iteration.common.blockentity.MachineConversion> conversions() {
        try {
            var level = Minecraft.getInstance().level;
            return com.miophas.singularity_iteration.common.blockentity.MachineConversions.all(level == null ? null : level.getRecipeManager());
        } catch (RuntimeException | LinkageError e) {
            com.miophas.singularity_iteration.core.runtime.diagnostics.QuietFailures.report("jei-conversions", "Building the machine conversions for JEI", e);
            return java.util.List.of();
        }
    }

    /** 0.1.9.2: runs one JEI registration step; a failure is logged and the next step still runs. */
    private static void guarded(String name, Runnable step) {
        try {
            step.run();
        } catch (RuntimeException | LinkageError e) {
            com.miophas.singularity_iteration.core.runtime.diagnostics.QuietFailures.report("jei-" + name, "JEI registration step " + name, e);
        }
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;

        var recipeManager = level.getRecipeManager();

        // Register the macerator recipes.
        guarded("powder", () -> {
            var powderRecipes = recipeManager.getAllRecipesFor(mio_icif_ModRecipes.POWDER_TYPE.get());
            registration.addRecipes(mio_icif_PowderCategory.POWDER_TYPE, powderRecipes.stream().toList());
        });

        // Register the ore washing recipes.
        guarded("washer", () -> {
            var washerRecipes = recipeManager.getAllRecipesFor(mio_icif_ModRecipes.WASHER_TYPE.get());
            registration.addRecipes(mio_icif_WasherCategory.WASHER_TYPE, washerRecipes.stream().toList());
        });

        // Register the centrifuge recipes.
        guarded("centrifuge", () -> {
            var centrifugeRecipes = recipeManager.getAllRecipesFor(mio_icif_ModRecipes.CENTRIFUGE_TYPE.get());
            registration.addRecipes(mio_icif_CentrifugeCategory.CENTRIFUGE_TYPE, centrifugeRecipes.stream().toList());
        });

        // Register the extractor recipes.
        guarded("extractor", () -> {
            var extractorRecipes = recipeManager.getAllRecipesFor(mio_icif_ModRecipes.EXTRACTOR_TYPE.get());
            registration.addRecipes(mio_icif_ExtractorCategory.EXTRACTOR_TYPE, extractorRecipes.stream().toList());
        });

        // Register the compressor recipes.
        guarded("compressor", () -> {
            var compressorRecipes = recipeManager.getAllRecipesFor(mio_icif_ModRecipes.COMPRESSOR_TYPE.get());
            registration.addRecipes(mio_icif_CompressorCategory.COMPRESSOR_TYPE, compressorRecipes.stream().toList());
        });

        // Register the block cutter recipes.
        guarded("blockCutter", () -> {
            var blockCutterRecipes = recipeManager.getAllRecipesFor(mio_icif_ModRecipes.BLOCK_CUTTER_TYPE.get());
            registration.addRecipes(mio_icif_BlockCutterCategory.BLOCK_CUTTER_TYPE, blockCutterRecipes.stream().toList());
        });

        // Register the metal former rolling recipes.
        guarded("rolling", () -> {
            var rollingRecipes = recipeManager.getAllRecipesFor(mio_icif_ModRecipes.ROLLING_TYPE.get());
            registration.addRecipes(mio_icif_RollingCategory.ROLLING_TYPE, rollingRecipes.stream().toList());
        });

        // Register the metal former cutting recipes.
        guarded("cutting", () -> {
            var cuttingRecipes = recipeManager.getAllRecipesFor(mio_icif_ModRecipes.CUTTING_TYPE.get());
            registration.addRecipes(mio_icif_CuttingCategory.CUTTING_TYPE, cuttingRecipes.stream().toList());
        });

        // Register the metal former extruding recipes.
        guarded("extruding", () -> {
            var extrudingRecipes = recipeManager.getAllRecipesFor(mio_icif_ModRecipes.EXTRUDING_TYPE.get());
            registration.addRecipes(mio_icif_ExtrudingCategory.EXTRUDING_TYPE, extrudingRecipes.stream().toList());
        });

        // Register the canner recipes.
        guarded("canning", () -> {
            var canningRecipes = recipeManager.getAllRecipesFor(mio_icif_ModRecipes.CANNING_TYPE.get());
            registration.addRecipes(mio_icif_CanningCategory.CANNING_TYPE, canningRecipes.stream().toList());
        });

        // Register the mixing recipes.
        guarded("mix", () -> {
            var mixRecipes = recipeManager.getAllRecipesFor(mio_icif_ModRecipes.MIX_TYPE.get());
            registration.addRecipes(mio_icif_MixCategory.MIX_TYPE, mixRecipes.stream().toList());
        });

        // Register the blast furnace recipes.
        guarded("blastFurnace", () -> {
            var blastFurnaceRecipes = recipeManager.getAllRecipesFor(mio_icif_ModRecipes.BLAST_FURNACE_TYPE.get());
            registration.addRecipes(mio_icif_BlastFurnaceCategory.BLAST_FURNACE_TYPE, blastFurnaceRecipes.stream().toList());
        });

        // Register the molecular transformer recipes.
        guarded("molecularTransformer", () -> {
            var molecularTransformerRecipes = recipeManager.getAllRecipesFor(mio_icif_ModRecipes.MOLECULAR_TRANSFORMER_TYPE.get());
            registration.addRecipes(mio_icif_MolecularTransformerCategory.MOLECULAR_TRANSFORMER_TYPE, molecularTransformerRecipes.stream().toList());
        });

        guarded("neutronPolymerizer", () -> {
            var neutronPolymerizerRecipes = recipeManager.getAllRecipesFor(com.miophas.singularity_iteration.common.recipe.generic.NeutronPolymerizerRecipes.NEUTRON_POLYMERIZER_TYPE.get());
            registration.addRecipes(mio_icif_NeutronPolymerizerCategory.NEUTRON_POLYMERIZER_TYPE, neutronPolymerizerRecipes.stream().toList());
        });

        guarded("fluidRefining", () -> {
            var fluidRefiningRecipes = recipeManager.getAllRecipesFor(com.miophas.singularity_iteration.common.recipe.fluid_refining.FluidRefiningRecipes.FLUID_REFINING_TYPE.get());
            registration.addRecipes(mio_icif_FluidRefiningCategory.FLUID_REFINING_TYPE, fluidRefiningRecipes.stream().toList());
        });

        guarded("fermenter", () -> registration.addRecipes(mio_icif_FermenterCategory.FERMENTER_TYPE,
            recipeManager.getAllRecipesFor(com.miophas.singularity_iteration.common.recipe.fermenter.FermenterRecipes.FERMENTER_TYPE.get())));
        guarded("lathe", () -> registration.addRecipes(mio_icif_LatheCategory.LATHE_TYPE,
            com.miophas.singularity_iteration.common.item.lathe.LatheShapes.all()));
        guarded("cannerTank", () -> {
            var cellFills = mio_icif_CannerTankCategory.recipes();
            registration.addRecipes(mio_icif_CannerTankCategory.FILL_FROM_TANK_TYPE, cellFills);
            registration.addRecipes(mio_icif_CannerTankCategory.EMPTY_TO_TANK_TYPE, cellFills);
        });
        guarded("conversions", () -> {
            var byMachine = new java.util.LinkedHashMap<net.minecraft.world.level.ItemLike,
                java.util.List<com.miophas.singularity_iteration.common.blockentity.MachineConversion>>();
            for (var conversion : conversions()) byMachine.computeIfAbsent(conversion.machine(), m -> new java.util.ArrayList<>()).add(conversion);
            byMachine.forEach((machine, list) -> registration.addRecipes(mio_icif_MachineConversionCategory.typeFor(machine), list));
        });
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        // 注册GUI交互处理
        // 高级采矿机：支持把 JEI 物品面板里的物品拖入黑白名单过滤槽（幽灵拖拽）
        registration.addGhostIngredientHandler(
            com.miophas.singularity_iteration.common.client.screen.mio_icif_gui_advanced_miner_elc.class,
            new AdvancedMinerGhostHandler());
        // the left utility dock (upgrade slots, area / XP buttons) is outside the GUI rectangle:
        // tell JEI so its bookmark / ingredient overlays never cover it
        registration.addGenericGuiContainerHandler(
            com.miophas.singularity_iteration.common.client.screen.mio_icif_screen.class,
            new mezz.jei.api.gui.handlers.IGuiContainerHandler<com.miophas.singularity_iteration.common.client.screen.mio_icif_screen<?>>() {
                @Override
                public java.util.List<net.minecraft.client.renderer.Rect2i> getGuiExtraAreas(
                        com.miophas.singularity_iteration.common.client.screen.mio_icif_screen<?> screen) {
                    int[] b = screen.dockBounds();
                    return b[2] <= 0 ? java.util.List.of() : java.util.List.of(new net.minecraft.client.renderer.Rect2i(b[0], b[1], b[2] + 2, b[3]));
                }
            });
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
        registration.addRecipeCatalyst(new ItemStack(mio_icif_blocks.FERMENTER_ELC.get()), mio_icif_FermenterCategory.FERMENTER_TYPE);
        registration.addRecipeCatalyst(new ItemStack(mio_icif_blocks.LATHE.get()), mio_icif_LatheCategory.LATHE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(mio_icif_blocks.CANNER_ELC.get()), mio_icif_CannerTankCategory.FILL_FROM_TANK_TYPE);
        registration.addRecipeCatalyst(new ItemStack(mio_icif_blocks.CANNER_ELC.get()), mio_icif_CannerTankCategory.EMPTY_TO_TANK_TYPE);
        // Each conversion category: its machine, then the other blocks the conversions run in.
        var catalysts = new java.util.LinkedHashMap<net.minecraft.world.level.ItemLike, java.util.Set<net.minecraft.world.level.ItemLike>>();
        for (var conversion : conversions()) catalysts.computeIfAbsent(conversion.machine(), m -> new java.util.LinkedHashSet<>()).addAll(conversion.machines());
        catalysts.forEach((machine, blocks) -> {
            var type = mio_icif_MachineConversionCategory.typeFor(machine);
            for (var block : blocks) registration.addRecipeCatalyst(new ItemStack(block), type);
        });
        // Advanced / upgraded machines run the same recipe sets as their basic versions.
        registration.addRecipeCatalyst(new ItemStack(mio_icif_blocks.POWDER_ADVANCED_ELC.get()), mio_icif_PowderCategory.POWDER_TYPE);
        registration.addRecipeCatalyst(new ItemStack(mio_icif_blocks.COMPRESSOR_ADVANCED_ELC.get()), mio_icif_CompressorCategory.COMPRESSOR_TYPE);
        registration.addRecipeCatalyst(new ItemStack(mio_icif_blocks.METAL_FORMER_ADVANCED.get()), mio_icif_RollingCategory.ROLLING_TYPE);
        registration.addRecipeCatalyst(new ItemStack(mio_icif_blocks.METAL_FORMER_ADVANCED.get()), mio_icif_CuttingCategory.CUTTING_TYPE);
        registration.addRecipeCatalyst(new ItemStack(mio_icif_blocks.METAL_FORMER_ADVANCED.get()), mio_icif_ExtrudingCategory.EXTRUDING_TYPE);
        registration.addRecipeCatalyst(new ItemStack(mio_icif_blocks.BLAST_FURNACE_ELC.get()), mio_icif_BlastFurnaceCategory.BLAST_FURNACE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(mio_icif_blocks.BLAST_FURNACE_ADVANCED.get()), mio_icif_BlastFurnaceCategory.BLAST_FURNACE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(mio_icif_blocks.FURNACE_ELC.get()), RecipeTypes.SMELTING);
        registration.addRecipeCatalyst(new ItemStack(mio_icif_blocks.INDUCTION_ELC.get()), RecipeTypes.SMELTING);
    }
}