package com.miophas.singularity_iteration.common.client.integration.rei.category;

import com.miophas.singularity_iteration.common.registry.mio_icif_blocks;
import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.client.integration.rei.display.BlastFurnaceDisplay;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.network.chat.Component;

@SuppressWarnings("null")
public class BlastFurnaceCategory implements DisplayCategory<BlastFurnaceDisplay> {

    public static final CategoryIdentifier<BlastFurnaceDisplay> TYPE = CategoryIdentifier.of(Singularity_Iteration.MOD_ID, "blast_furnace");

    @Override
    public CategoryIdentifier<? extends BlastFurnaceDisplay> getCategoryIdentifier() {
        return TYPE;
    }

    @Override
    public Renderer getIcon() {
        return EntryStacks.of(mio_icif_blocks.BLAST_FURNACE.get());
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.mio_icif.blast_furnace");
    }
}