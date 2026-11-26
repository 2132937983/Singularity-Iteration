package com.miophas.singularity_iteration.common.client.integration.emi;

import java.util.List;

import com.google.common.collect.Lists;
import com.miophas.singularity_iteration.common.registry.mio_icif_blocks;
import com.miophas.singularity_iteration.common.menu.producer.IndustrialWorkbenchMenu;
import com.miophas.singularity_iteration.common.registry.mio_icif_menus;
import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.client.integration.AdvancedMinerFilters;
import com.miophas.singularity_iteration.common.client.screen.GridScadaScreen;
import com.miophas.singularity_iteration.common.client.screen.mio_icif_gui_advanced_miner_elc;
import com.miophas.singularity_iteration.common.client.screen.mio_icif_gui_future_elc;
import com.miophas.singularity_iteration.common.client.screen.mio_icif_screen;
import dev.emi.emi.api.EmiDragDropHandler;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.recipe.handler.StandardRecipeHandler;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Bounds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.RecipeManager;
import org.jetbrains.annotations.Nullable;

/**
 * EMI 插件主类
 * 为 EMI 提供原生配方支持，无需依赖 JEI
 * <p>0.1.11.209: 只要模组带有 EMI 插件，EMI 就完全不调用该模组的 JEI 插件（即使同时装了 JEI），
 * 所以 JEI 插件里的每个配方页都必须在这里有原生版本（见 {@link SiEmiRecipes}）。
 */
@SuppressWarnings("null")
@EmiEntrypoint
public class MioIcifEmiPlugin implements EmiPlugin {

    // 配方分类定义
    public static final EmiRecipeCategory POWDER = new EmiRecipeCategory(
            ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "powder"),
            EmiStack.of(mio_icif_blocks.POWDER_ELC.get()));

    public static final EmiRecipeCategory WASHER = new EmiRecipeCategory(
            ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "washer"),
            EmiStack.of(mio_icif_blocks.WASHER_ELC.get()));

    public static final EmiRecipeCategory CENTRIFUGE = new EmiRecipeCategory(
            ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "centrifuge"),
            EmiStack.of(mio_icif_blocks.CENTRIFUGE_ELC.get()));

    public static final EmiRecipeCategory EXTRACTOR = new EmiRecipeCategory(
            ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "extractor"),
            EmiStack.of(mio_icif_blocks.EXTRACTOR_ELC.get()));

    public static final EmiRecipeCategory COMPRESSOR = new EmiRecipeCategory(
            ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "compressor"),
            EmiStack.of(mio_icif_blocks.COMPRESSOR_ELC.get()));

    public static final EmiRecipeCategory BLOCK_CUTTER = new EmiRecipeCategory(
            ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "block_cutter"),
            EmiStack.of(mio_icif_blocks.BLOCK_CUTTER.get()));

    public static final EmiRecipeCategory ROLLING = new EmiRecipeCategory(
            ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "rolling"),
            EmiStack.of(mio_icif_blocks.METAL_FORMER.get()));

    public static final EmiRecipeCategory CUTTING = new EmiRecipeCategory(
            ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "cutting"),
            EmiStack.of(mio_icif_blocks.METAL_FORMER.get()));

    public static final EmiRecipeCategory EXTRUDING = new EmiRecipeCategory(
            ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "extruding"),
            EmiStack.of(mio_icif_blocks.METAL_FORMER.get()));

    public static final EmiRecipeCategory CANNING = new EmiRecipeCategory(
            ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "canning"),
            EmiStack.of(mio_icif_blocks.CANNER_ELC.get()));

    public static final EmiRecipeCategory MIX = new EmiRecipeCategory(
            ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "mix"),
            EmiStack.of(mio_icif_blocks.CANNER_ELC.get()));


    public static final EmiRecipeCategory BLAST_FURNACE = new EmiRecipeCategory(
            ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "blast_furnace"),
            EmiStack.of(mio_icif_blocks.BLAST_FURNACE.get()));

    public static final EmiRecipeCategory MOLECULAR_TRANSFORMER = new EmiRecipeCategory(
            ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "molecular_transformer"),
            EmiStack.of(mio_icif_blocks.MOLECULAR_TRANSFORMER.get()));

    @Override
    public void register(EmiRegistry registry) {
        // 注册配方分类
        registry.addCategory(POWDER);
        registry.addCategory(WASHER);
        registry.addCategory(CENTRIFUGE);
        registry.addCategory(EXTRACTOR);
        registry.addCategory(COMPRESSOR);
        registry.addCategory(BLOCK_CUTTER);
        registry.addCategory(ROLLING);
        registry.addCategory(CUTTING);
        registry.addCategory(EXTRUDING);
        registry.addCategory(CANNING);
        registry.addCategory(MIX);
        registry.addCategory(BLAST_FURNACE);
        registry.addCategory(MOLECULAR_TRANSFORMER);

        // 注册工作站（催化剂）
        registry.addWorkstation(POWDER, EmiStack.of(mio_icif_blocks.POWDER_ELC.get()));
        registry.addWorkstation(WASHER, EmiStack.of(mio_icif_blocks.WASHER_ELC.get()));
        registry.addWorkstation(CENTRIFUGE, EmiStack.of(mio_icif_blocks.CENTRIFUGE_ELC.get()));
        registry.addWorkstation(EXTRACTOR, EmiStack.of(mio_icif_blocks.EXTRACTOR_ELC.get()));
        registry.addWorkstation(COMPRESSOR, EmiStack.of(mio_icif_blocks.COMPRESSOR_ELC.get()));
        registry.addWorkstation(BLOCK_CUTTER, EmiStack.of(mio_icif_blocks.BLOCK_CUTTER.get()));
        registry.addWorkstation(ROLLING, EmiStack.of(mio_icif_blocks.METAL_FORMER.get()));
        registry.addWorkstation(CUTTING, EmiStack.of(mio_icif_blocks.METAL_FORMER.get()));
        registry.addWorkstation(EXTRUDING, EmiStack.of(mio_icif_blocks.METAL_FORMER.get()));
        registry.addWorkstation(CANNING, EmiStack.of(mio_icif_blocks.CANNER_ELC.get()));
        registry.addWorkstation(MIX, EmiStack.of(mio_icif_blocks.CANNER_ELC.get()));
        registry.addWorkstation(BLAST_FURNACE, EmiStack.of(mio_icif_blocks.BLAST_FURNACE.get()));
        registry.addWorkstation(MOLECULAR_TRANSFORMER, EmiStack.of(mio_icif_blocks.MOLECULAR_TRANSFORMER.get()));
        // 0.1.11.209: the advanced machines run the same recipe sets; the electric furnaces smelt (as the JEI catalysts)
        registry.addWorkstation(POWDER, EmiStack.of(mio_icif_blocks.POWDER_ADVANCED_ELC.get()));
        registry.addWorkstation(COMPRESSOR, EmiStack.of(mio_icif_blocks.COMPRESSOR_ADVANCED_ELC.get()));
        registry.addWorkstation(ROLLING, EmiStack.of(mio_icif_blocks.METAL_FORMER_ADVANCED.get()));
        registry.addWorkstation(CUTTING, EmiStack.of(mio_icif_blocks.METAL_FORMER_ADVANCED.get()));
        registry.addWorkstation(EXTRUDING, EmiStack.of(mio_icif_blocks.METAL_FORMER_ADVANCED.get()));
        registry.addWorkstation(BLAST_FURNACE, EmiStack.of(mio_icif_blocks.BLAST_FURNACE_ELC.get()));
        registry.addWorkstation(BLAST_FURNACE, EmiStack.of(mio_icif_blocks.BLAST_FURNACE_ADVANCED.get()));
        registry.addWorkstation(VanillaEmiRecipeCategories.SMELTING, EmiStack.of(mio_icif_blocks.FURNACE_ELC.get()));
        registry.addWorkstation(VanillaEmiRecipeCategories.SMELTING, EmiStack.of(mio_icif_blocks.INDUCTION_ELC.get()));


        // 注册工业工作台为合成配方工作站
        registry.addWorkstation(VanillaEmiRecipeCategories.CRAFTING,
                EmiStack.of(mio_icif_blocks.INDUSTRIAL_WORKBENCH.get()));

        // 注册工业工作台配方转移处理器
        registry.addRecipeHandler(mio_icif_menus.INDUSTRIAL_WORKBENCH_MENU_TYPE.get(),
                new IndustrialWorkbenchRecipeHandler());

        // 0.1.10.24: keep EMI's side panels off the full-screen data panels (futures exchange, grid monitoring) and off the
        // left utility dock of machine GUIs (same areas as the JEI GUI handlers)
        registry.addGenericExclusionArea((screen, out) -> {
            if (screen instanceof mio_icif_gui_future_elc || screen instanceof GridScadaScreen) {
                out.accept(new Bounds(0, 0, screen.width, screen.height));
            } else if (screen instanceof mio_icif_screen<?> machine) {
                int[] b = machine.dockBounds();
                if (b[2] > 0) out.accept(new Bounds(b[0], b[1], b[2] + 2, b[3]));
            }
        });

        // 0.1.11.217: the processing machine pages have the layouts of their JEI twins and the same data
        // (MachineRecipeViews): input counts, arrow, time, energy, air cells, water. Before, one generic page showed
        // most inputs as a single item (one Tin Plate for the Dense Tin Plate, which takes 9).
        RecipeManager recipeManager = registry.getRecipeManager();
        if (recipeManager != null) SiEmiMachines.register(registry, recipeManager);

        // 0.1.11.209: every other page of the JEI plugin, natively. EMI never calls the JEI plugin of a mod that has an EMI
        // plugin, so without these the Neutron Polymerizer (the Neutron Plate), the refinery, the structures... were missing.
        SiEmiRecipes.register(registry);

        // 0.1.11.209: an item dragged from EMI onto an Advanced Miner filter slot sets the filter (as the JEI ghost handler)
        registry.addDragDropHandler(mio_icif_gui_advanced_miner_elc.class,
            new EmiDragDropHandler.BoundsBased<mio_icif_gui_advanced_miner_elc>((gui, out) -> {
                for (AdvancedMinerFilters.Target target : AdvancedMinerFilters.targets(gui)) {
                    out.accept(new Bounds(target.x(), target.y(), 16, 16), ingredient -> {
                        List<EmiStack> stacks = ingredient.getEmiStacks();
                        if (!stacks.isEmpty()) AdvancedMinerFilters.set(gui, target, stacks.get(0).getItemStack());
                    });
                }
            }));

    }

    /**
     * 工业工作台配方转移处理器
     * 实现 StandardRecipeHandler 以支持 EMI 的配方自动填充功能
     */
    public static class IndustrialWorkbenchRecipeHandler implements StandardRecipeHandler<IndustrialWorkbenchMenu> {
        @Override
        public List<Slot> getInputSources(IndustrialWorkbenchMenu handler) {
            List<Slot> list = Lists.newArrayList();
            // 合成网格槽位 (1-9)
            for (int i = 1; i <= 9; i++) {
                list.add(handler.getSlot(i));
            }
            // Buffer storage slots (10-27)
            for (int i = 10; i <= 27; i++) {
                list.add(handler.getSlot(i));
            }
            // 玩家主背包 (34-60)
            for (int i = 34; i <= 60; i++) {
                list.add(handler.getSlot(i));
            }
            // 玩家快捷栏 (61-69)
            for (int i = 61; i <= 69; i++) {
                list.add(handler.getSlot(i));
            }
            return list;
        }

        @Override
        public List<Slot> getCraftingSlots(IndustrialWorkbenchMenu handler) {
            List<Slot> list = Lists.newArrayList();
            // 合成网格槽位 (1-9)
            for (int i = 1; i <= 9; i++) {
                list.add(handler.getSlot(i));
            }
            return list;
        }

        @Override
        public @Nullable Slot getOutputSlot(IndustrialWorkbenchMenu handler) {
            return handler.slots.get(0);
        }

        @Override
        public boolean supportsRecipe(EmiRecipe recipe) {
            return recipe.getCategory() == VanillaEmiRecipeCategories.CRAFTING && recipe.supportsRecipeTree();
        }
    }
}