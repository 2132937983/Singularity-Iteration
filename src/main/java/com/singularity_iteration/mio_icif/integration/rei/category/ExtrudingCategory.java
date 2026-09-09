package com.singularity_iteration.mio_icif.integration.rei.category;

import com.singularity_iteration.mio_icif.Blocks.mio_icif_blocks;
import com.singularity_iteration.mio_icif.Singularity_Iteration;
import com.singularity_iteration.mio_icif.integration.rei.display.ExtrudingDisplay;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.network.chat.Component;

@SuppressWarnings("null")
public class ExtrudingCategory implements DisplayCategory<ExtrudingDisplay> {

    public static final CategoryIdentifier<ExtrudingDisplay> TYPE = CategoryIdentifier.of(Singularity_Iteration.MOD_ID, "extruding");

    @Override
    public CategoryIdentifier<? extends ExtrudingDisplay> getCategoryIdentifier() {
        return TYPE;
    }

    @Override
    public Renderer getIcon() {
        return EntryStacks.of(mio_icif_blocks.METAL_FORMER.get());
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.mio_icif.extruding");
    }
}