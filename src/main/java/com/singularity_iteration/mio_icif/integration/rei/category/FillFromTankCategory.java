package com.singularity_iteration.mio_icif.integration.rei.category;

import com.singularity_iteration.mio_icif.Blocks.mio_icif_blocks;
import com.singularity_iteration.mio_icif.Singularity_Iteration;
import com.singularity_iteration.mio_icif.integration.rei.display.FillFromTankDisplay;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.network.chat.Component;

@SuppressWarnings("null")
public class FillFromTankCategory implements DisplayCategory<FillFromTankDisplay> {

    public static final CategoryIdentifier<FillFromTankDisplay> TYPE = CategoryIdentifier.of(Singularity_Iteration.MOD_ID, "fill_from_tank");

    @Override
    public CategoryIdentifier<? extends FillFromTankDisplay> getCategoryIdentifier() {
        return TYPE;
    }

    @Override
    public Renderer getIcon() {
        return EntryStacks.of(mio_icif_blocks.CANNER_ELC.get());
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.mio_icif.fill_from_tank");
    }
}