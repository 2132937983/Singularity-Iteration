package com.singularity_iteration.mio_icif.integration.emi;

import java.util.List;

import com.google.common.collect.Lists;
import com.singularity_iteration.mio_icif.Blocks.mio_icif_blocks;
import com.singularity_iteration.mio_icif.Menu.Producer.IndustrialWorkbenchMenu;
import com.singularity_iteration.mio_icif.Menu.mio_icif_menus;
import com.singularity_iteration.mio_icif.Singularity_Iteration;
import com.singularity_iteration.mio_icif.recipe.mio_icif_ModRecipes;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.recipe.handler.StandardRecipeHandler;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.RecipeManager;
import org.jetbrains.annotations.Nullable;

/**
 * EMI 插件主类
 * 为 EMI 提供原生配方支持，无需依赖 JEI
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

    public static final EmiRecipeCategory EMPTY_TO_TANK = new EmiRecipeCategory(
            ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "empty_to_tank"),
            EmiStack.of(mio_icif_blocks.CANNER_ELC.get()));

    public static final EmiRecipeCategory FILL_FROM_TANK = new EmiRecipeCategory(
            ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "fill_from_tank"),
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
        registry.addCategory(EMPTY_TO_TANK);
        registry.addCategory(FILL_FROM_TANK);
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
        registry.addWorkstation(EMPTY_TO_TANK, EmiStack.of(mio_icif_blocks.CANNER_ELC.get()));
        registry.addWorkstation(FILL_FROM_TANK, EmiStack.of(mio_icif_blocks.CANNER_ELC.get()));
        registry.addWorkstation(MIX, EmiStack.of(mio_icif_blocks.CANNER_ELC.get()));
        registry.addWorkstation(BLAST_FURNACE, EmiStack.of(mio_icif_blocks.BLAST_FURNACE.get()));
        registry.addWorkstation(MOLECULAR_TRANSFORMER, EmiStack.of(mio_icif_blocks.MOLECULAR_TRANSFORMER.get()));

        // 注册工业工作台为合成配方工作站
        registry.addWorkstation(VanillaEmiRecipeCategories.CRAFTING,
                EmiStack.of(mio_icif_blocks.INDUSTRIAL_WORKBENCH.get()));

        // 注册工业工作台配方转移处理器
        registry.addRecipeHandler(mio_icif_menus.INDUSTRIAL_WORKBENCH_MENU_TYPE.get(),
                new IndustrialWorkbenchRecipeHandler());

        // 获取配方管理器并注册配方
        RecipeManager recipeManager = registry.getRecipeManager();
        if (recipeManager != null) {
            // 注册打粉配方
            recipeManager.getAllRecipesFor(mio_icif_ModRecipes.POWDER_TYPE.get())
                    .forEach(recipe -> registry.addRecipe(new MioIcifEmiRecipe(POWDER, recipe)));

            // 注册洗矿配方
            recipeManager.getAllRecipesFor(mio_icif_ModRecipes.WASHER_TYPE.get())
                    .forEach(recipe -> registry.addRecipe(new MioIcifEmiRecipe(WASHER, recipe)));

            // 注册离心机配方
            recipeManager.getAllRecipesFor(mio_icif_ModRecipes.CENTRIFUGE_TYPE.get())
                    .forEach(recipe -> registry.addRecipe(new MioIcifEmiRecipe(CENTRIFUGE, recipe)));

            // 注册提取机配方
            recipeManager.getAllRecipesFor(mio_icif_ModRecipes.EXTRACTOR_TYPE.get())
                    .forEach(recipe -> registry.addRecipe(new MioIcifEmiRecipe(EXTRACTOR, recipe)));

            // 注册压缩机配方
            recipeManager.getAllRecipesFor(mio_icif_ModRecipes.COMPRESSOR_TYPE.get())
                    .forEach(recipe -> registry.addRecipe(new MioIcifEmiRecipe(COMPRESSOR, recipe)));

            // 注册方块切割机配方
            recipeManager.getAllRecipesFor(mio_icif_ModRecipes.BLOCK_CUTTER_TYPE.get())
                    .forEach(recipe -> registry.addRecipe(new MioIcifEmiRecipe(BLOCK_CUTTER, recipe)));

            // 注册金属成型机 - 轧制配方
            recipeManager.getAllRecipesFor(mio_icif_ModRecipes.ROLLING_TYPE.get())
                    .forEach(recipe -> registry.addRecipe(new MioIcifEmiRecipe(ROLLING, recipe)));

            // 注册金属成型机 - 切割配方
            recipeManager.getAllRecipesFor(mio_icif_ModRecipes.CUTTING_TYPE.get())
                    .forEach(recipe -> registry.addRecipe(new MioIcifEmiRecipe(CUTTING, recipe)));

            // 注册金属成型机 - 挤出配方
            recipeManager.getAllRecipesFor(mio_icif_ModRecipes.EXTRUDING_TYPE.get())
                    .forEach(recipe -> registry.addRecipe(new MioIcifEmiRecipe(EXTRUDING, recipe)));

            // 注册装罐配方
            recipeManager.getAllRecipesFor(mio_icif_ModRecipes.CANNING_TYPE.get())
                    .forEach(recipe -> registry.addRecipe(new MioIcifEmiRecipe(CANNING, recipe)));

            // 注册空罐注液配方
            recipeManager.getAllRecipesFor(mio_icif_ModRecipes.EMPTY_TO_TANK_TYPE.get())
                    .forEach(recipe -> registry.addRecipe(new MioIcifEmiRecipe(EMPTY_TO_TANK, recipe)));

            // 注册从罐抽液配方
            recipeManager.getAllRecipesFor(mio_icif_ModRecipes.FILL_FROM_TANK_TYPE.get())
                    .forEach(recipe -> registry.addRecipe(new MioIcifEmiRecipe(FILL_FROM_TANK, recipe)));

            // 注册混合配方
            recipeManager.getAllRecipesFor(mio_icif_ModRecipes.MIX_TYPE.get())
                    .forEach(recipe -> registry.addRecipe(new MioIcifEmiRecipe(MIX, recipe)));

            // 娉ㄥ唽楂樼倝閰嶆柟
            recipeManager.getAllRecipesFor(mio_icif_ModRecipes.BLAST_FURNACE_TYPE.get())
                    .forEach(recipe -> registry.addRecipe(new MioIcifEmiRecipe(BLAST_FURNACE, recipe)));

            // 注册分子重组仪配方
            recipeManager.getAllRecipesFor(mio_icif_ModRecipes.MOLECULAR_TRANSFORMER_TYPE.get())
                    .forEach(recipe -> registry.addRecipe(new MioIcifEmiRecipe(MOLECULAR_TRANSFORMER, recipe)));
        }
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
            // 缂撳啿瀛樺偍鍖� (10-27)
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