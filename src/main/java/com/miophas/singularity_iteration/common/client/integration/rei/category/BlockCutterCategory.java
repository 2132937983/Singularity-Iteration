package com.miophas.singularity_iteration.common.client.integration.rei.category;

import com.miophas.singularity_iteration.common.registry.mio_icif_blocks;
import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.client.integration.rei.display.BlockCutterDisplay;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.network.chat.Component;

@SuppressWarnings("null")
public class BlockCutterCategory implements DisplayCategory<BlockCutterDisplay> {

    public static final CategoryIdentifier<BlockCutterDisplay> TYPE = CategoryIdentifier.of(Singularity_Iteration.MOD_ID, "block_cutter");

    @Override
    public CategoryIdentifier<? extends BlockCutterDisplay> getCategoryIdentifier() {
        return TYPE;
    }

    @Override
    public Renderer getIcon() {
        return EntryStacks.of(mio_icif_blocks.BLOCK_CUTTER.get());
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.mio_icif.block_cutter");
    }
}