package com.miophas.singularity_iteration.common.client.integration.rei.category;

import com.miophas.singularity_iteration.common.registry.mio_icif_blocks;
import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.client.integration.rei.display.CuttingDisplay;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.network.chat.Component;

@SuppressWarnings("null")
public class CuttingCategory implements DisplayCategory<CuttingDisplay> {

    public static final CategoryIdentifier<CuttingDisplay> TYPE = CategoryIdentifier.of(Singularity_Iteration.MOD_ID, "cutting");

    @Override
    public CategoryIdentifier<? extends CuttingDisplay> getCategoryIdentifier() {
        return TYPE;
    }

    @Override
    public Renderer getIcon() {
        return EntryStacks.of(mio_icif_blocks.METAL_FORMER.get());
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.mio_icif.cutting");
    }
}