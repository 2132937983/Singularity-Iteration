package com.miophas.singularity_iteration.common.menu.generator;

import com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_experience_generator;
import com.miophas.singularity_iteration.common.registry.mio_icif_menus;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.Nullable;

/** 0.1.11.19 (the experience recorder): the values of {@link IncineratorValues#of(com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_experience_generator)}. */
@SuppressWarnings("null")
public class ExperienceGeneratorMenu extends MimicGeneratorMenu {
    public ExperienceGeneratorMenu(int containerId, Inventory playerInventory) { this(containerId, playerInventory, null); }

    public ExperienceGeneratorMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_experience_generator be) {
        super(mio_icif_menus.EXPERIENCE_GENERATOR_MENU_TYPE.get(), containerId, 0, playerInventory, null, IncineratorValues.XP_DATA_COUNT, be);
    }

    public int getEnergy() { return value(0); }
    public int getMaxEnergy() { return value(1); }
    public int getFuel() { return value(2); }
    public boolean isWorking() { return value(3) == 1; }

    @Override protected void addMachineSlots() {}

    @Override
    protected int[] values() { return IncineratorValues.of((com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_experience_generator) blockEntity); }
}
