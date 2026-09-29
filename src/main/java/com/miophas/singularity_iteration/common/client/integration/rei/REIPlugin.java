package com.miophas.singularity_iteration.common.client.integration.rei;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.registry.mio_icif_blocks;
import com.miophas.singularity_iteration.common.client.integration.rei.category.*;
import com.miophas.singularity_iteration.common.client.integration.rei.display.*;
import com.miophas.singularity_iteration.common.recipe.blast_furnace.mio_icif_BlastFurnaceRecipe;
import com.miophas.singularity_iteration.common.recipe.block_cutter.mio_icif_BlockCutterRecipe;
import com.miophas.singularity_iteration.common.recipe.canner.canning.CanningRecipe;
import com.miophas.singularity_iteration.common.recipe.canner.mix.MixRecipe;
import com.miophas.singularity_iteration.common.recipe.centrifuge.mio_icif_CentrifugeRecipe;
import com.miophas.singularity_iteration.common.recipe.compressor.mio_icif_CompressorRecipe;
import com.miophas.singularity_iteration.common.recipe.extractor.mio_icif_ExtractorRecipe;
import com.miophas.singularity_iteration.common.recipe.metal_former.cutting.mio_icif_CuttingRecipe;
import com.miophas.singularity_iteration.common.recipe.metal_former.extruding.mio_icif_ExtrudingRecipe;
import com.miophas.singularity_iteration.common.recipe.metal_former.rolling.mio_icif_RollingRecipe;
import com.miophas.singularity_iteration.common.recipe.mio_icif_ModRecipes;
import com.miophas.singularity_iteration.common.recipe.mio_icif_PowderRecipe;
import com.miophas.singularity_iteration.common.recipe.molecular_transformer.mio_icif_MolecularTransformerRecipe;
import com.miophas.singularity_iteration.common.recipe.washer.mio_icif_WasherRecipe;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.client.Minecraft;

@SuppressWarnings("null")
public class REIPlugin implements REIClientPlugin {

    @Override
    public String getPluginProviderName() {
        return Singularity_Iteration.MOD_ID + ":rei_plugin";
    }

    @Override
    public void registerCategories(CategoryRegistry registry) {
        registry.add(new PowderCategory());
        registry.add(new WasherCategory());
        registry.add(new CentrifugeCategory());
        registry.add(new ExtractorCategory());
        registry.add(new CompressorCategory());
        registry.add(new BlockCutterCategory());
        registry.add(new RollingCategory());
        registry.add(new CuttingCategory());
        registry.add(new ExtrudingCategory());
        registry.add(new CanningCategory());
        registry.add(new MixCategory());
        registry.add(new BlastFurnaceCategory());
        registry.add(new MolecularTransformerCategory());

        registry.addWorkstations(PowderCategory.TYPE, EntryStacks.of(mio_icif_blocks.POWDER_ELC.get()));
        registry.addWorkstations(WasherCategory.TYPE, EntryStacks.of(mio_icif_blocks.WASHER_ELC.get()));
        registry.addWorkstations(CentrifugeCategory.TYPE, EntryStacks.of(mio_icif_blocks.CENTRIFUGE_ELC.get()));
        registry.addWorkstations(ExtractorCategory.TYPE, EntryStacks.of(mio_icif_blocks.EXTRACTOR_ELC.get()));
        registry.addWorkstations(CompressorCategory.TYPE, EntryStacks.of(mio_icif_blocks.COMPRESSOR_ELC.get()));
        registry.addWorkstations(BlockCutterCategory.TYPE, EntryStacks.of(mio_icif_blocks.BLOCK_CUTTER.get()));
        registry.addWorkstations(RollingCategory.TYPE, EntryStacks.of(mio_icif_blocks.METAL_FORMER.get()));
        registry.addWorkstations(CuttingCategory.TYPE, EntryStacks.of(mio_icif_blocks.METAL_FORMER.get()));
        registry.addWorkstations(ExtrudingCategory.TYPE, EntryStacks.of(mio_icif_blocks.METAL_FORMER.get()));
        registry.addWorkstations(CanningCategory.TYPE, EntryStacks.of(mio_icif_blocks.CANNER_ELC.get()));
        registry.addWorkstations(MixCategory.TYPE, EntryStacks.of(mio_icif_blocks.CANNER_ELC.get()));
        registry.addWorkstations(BlastFurnaceCategory.TYPE, EntryStacks.of(mio_icif_blocks.BLAST_FURNACE.get()));
        registry.addWorkstations(MolecularTransformerCategory.TYPE, EntryStacks.of(mio_icif_blocks.MOLECULAR_TRANSFORMER.get()));
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;

        registry.registerRecipeFiller(mio_icif_PowderRecipe.class, mio_icif_ModRecipes.POWDER_TYPE.get(), PowderDisplay::new);
        registry.registerRecipeFiller(mio_icif_WasherRecipe.class, mio_icif_ModRecipes.WASHER_TYPE.get(), WasherDisplay::new);
        registry.registerRecipeFiller(mio_icif_CentrifugeRecipe.class, mio_icif_ModRecipes.CENTRIFUGE_TYPE.get(), CentrifugeDisplay::new);
        registry.registerRecipeFiller(mio_icif_ExtractorRecipe.class, mio_icif_ModRecipes.EXTRACTOR_TYPE.get(), ExtractorDisplay::new);
        registry.registerRecipeFiller(mio_icif_CompressorRecipe.class, mio_icif_ModRecipes.COMPRESSOR_TYPE.get(), CompressorDisplay::new);
        registry.registerRecipeFiller(mio_icif_BlockCutterRecipe.class, mio_icif_ModRecipes.BLOCK_CUTTER_TYPE.get(), BlockCutterDisplay::new);
        registry.registerRecipeFiller(mio_icif_RollingRecipe.class, mio_icif_ModRecipes.ROLLING_TYPE.get(), RollingDisplay::new);
        registry.registerRecipeFiller(mio_icif_CuttingRecipe.class, mio_icif_ModRecipes.CUTTING_TYPE.get(), CuttingDisplay::new);
        registry.registerRecipeFiller(mio_icif_ExtrudingRecipe.class, mio_icif_ModRecipes.EXTRUDING_TYPE.get(), ExtrudingDisplay::new);
        registry.registerRecipeFiller(CanningRecipe.class, mio_icif_ModRecipes.CANNING_TYPE.get(), CanningDisplay::new);
        registry.registerRecipeFiller(MixRecipe.class, mio_icif_ModRecipes.MIX_TYPE.get(), MixDisplay::new);
        registry.registerRecipeFiller(mio_icif_BlastFurnaceRecipe.class, mio_icif_ModRecipes.BLAST_FURNACE_TYPE.get(), BlastFurnaceDisplay::new);
        registry.registerRecipeFiller(mio_icif_MolecularTransformerRecipe.class, mio_icif_ModRecipes.MOLECULAR_TRANSFORMER_TYPE.get(), MolecularTransformerDisplay::new);
    }
}