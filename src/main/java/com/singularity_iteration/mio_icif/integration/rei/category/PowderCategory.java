package com.singularity_iteration.mio_icif.integration.rei.category;

import com.singularity_iteration.mio_icif.Blocks.mio_icif_blocks;
import com.singularity_iteration.mio_icif.Singularity_Iteration;
import com.singularity_iteration.mio_icif.integration.rei.display.PowderDisplay;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.network.chat.Component;

@SuppressWarnings("null")
public class PowderCategory implements DisplayCategory<PowderDisplay> {

    public static final CategoryIdentifier<PowderDisplay> TYPE = CategoryIdentifier.of(Singularity_Iteration.MOD_ID, "powder");

    @Override
    public CategoryIdentifier<? extends PowderDisplay> getCategoryIdentifier() {
        return TYPE;
    }

    @Override
    public Renderer getIcon() {
        return EntryStacks.of(mio_icif_blocks.POWDER_ELC.get());
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.mio_icif.powder");
    }
}