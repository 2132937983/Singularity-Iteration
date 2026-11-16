package com.miophas.singularity_iteration.common.menu.generator;

import com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_diesel_generator;
import com.miophas.singularity_iteration.common.registry.mio_icif_menus;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.Nullable;

/** 0.1.11.15: the engine menu under this generator's menu type. */
public class DieselGeneratorMenu extends EngineMenu {
    public DieselGeneratorMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null);
    }

    public DieselGeneratorMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_diesel_generator blockEntity) {
        super(mio_icif_menus.DIESEL_GENERATOR_MENU_TYPE.get(), containerId, playerInventory, blockEntity, blockEntity != null ? blockEntity.getEngineData() : null);
    }

    /** Diesel only. */
    @Override
    protected boolean isValidFuel(net.minecraft.world.item.ItemStack stack) {
        return holdsFluid(stack, f -> f.isSame(com.miophas.singularity_iteration.common.block.environment.fluid.mio_icif_fluids.DIESELOIL.get()))
            || stack.is(com.miophas.singularity_iteration.common.block.environment.fluid.mio_icif_fluids.DIESELOIL_BUCKET.get())
            || com.miophas.singularity_iteration.common.item.cell.mio_icif_cells.isCellContainingFluid(stack, com.miophas.singularity_iteration.common.block.environment.fluid.mio_icif_fluids.DIESELOIL.get());
    }
}
