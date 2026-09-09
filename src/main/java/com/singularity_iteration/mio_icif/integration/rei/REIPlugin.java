package com.singularity_iteration.mio_icif.integration.rei;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import com.singularity_iteration.mio_icif.Blocks.mio_icif_blocks;
import com.singularity_iteration.mio_icif.integration.rei.category.*;
import com.singularity_iteration.mio_icif.integration.rei.display.*;
import com.singularity_iteration.mio_icif.recipe.blast_furnace.mio_icif_BlastFurnaceRecipe;
import com.singularity_iteration.mio_icif.recipe.block_cutter.mio_icif_BlockCutterRecipe;
import com.singularity_iteration.mio_icif.recipe.canner.canning.CanningRecipe;
import com.singularity_iteration.mio_icif.recipe.canner.empty_to_tank.EmptyToTankRecipe;
import com.singularity_iteration.mio_icif.recipe.canner.fill_from_tank.FillFromTankRecipe;
import com.singularity_iteration.mio_icif.recipe.canner.mix.MixRecipe;
import com.singularity_iteration.mio_icif.recipe.centrifuge.mio_icif_CentrifugeRecipe;
import com.singularity_iteration.mio_icif.recipe.compressor.mio_icif_CompressorRecipe;
import com.singularity_iteration.mio_icif.recipe.extractor.mio_icif_ExtractorRecipe;
import com.singularity_iteration.mio_icif.recipe.metal_former.cutting.mio_icif_CuttingRecipe;
import com.singularity_iteration.mio_icif.recipe.metal_former.extruding.mio_icif_ExtrudingRecipe;
import com.singularity_iteration.mio_icif.recipe.metal_former.rolling.mio_icif_RollingRecipe;
import com.singularity_iteration.mio_icif.recipe.mio_icif_ModRecipes;
import com.singularity_iteration.mio_icif.recipe.mio_icif_PowderRecipe;
import com.singularity_iteration.mio_icif.recipe.molecular_transformer.mio_icif_MolecularTransformerRecipe;
import com.singularity_iteration.mio_icif.recipe.washer.mio_icif_WasherRecipe;
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
        registry.add(new EmptyToTankCategory());
        registry.add(new FillFromTankCategory());
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
        registry.addWorkstations(EmptyToTankCategory.TYPE, EntryStacks.of(mio_icif_blocks.CANNER_ELC.get()));
        registry.addWorkstations(FillFromTankCategory.TYPE, EntryStacks.of(mio_icif_blocks.CANNER_ELC.get()));
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
        registry.registerRecipeFiller(EmptyToTankRecipe.class, mio_icif_ModRecipes.EMPTY_TO_TANK_TYPE.get(), EmptyToTankDisplay::new);
        registry.registerRecipeFiller(FillFromTankRecipe.class, mio_icif_ModRecipes.FILL_FROM_TANK_TYPE.get(), FillFromTankDisplay::new);
        registry.registerRecipeFiller(MixRecipe.class, mio_icif_ModRecipes.MIX_TYPE.get(), MixDisplay::new);
        registry.registerRecipeFiller(mio_icif_BlastFurnaceRecipe.class, mio_icif_ModRecipes.BLAST_FURNACE_TYPE.get(), BlastFurnaceDisplay::new);
        registry.registerRecipeFiller(mio_icif_MolecularTransformerRecipe.class, mio_icif_ModRecipes.MOLECULAR_TRANSFORMER_TYPE.get(), MolecularTransformerDisplay::new);
    }
}