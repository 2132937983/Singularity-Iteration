package com.singularity_iteration.mio_icif.Menu.Producer;

import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_metal_former_advanced;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.Nullable;

@Deprecated
@SuppressWarnings("null")
public class ExtrudingMachineMenu extends MetalFormerAdvancedMenu {

    public ExtrudingMachineMenu(int containerId, Inventory playerInventory) {
        super(containerId, playerInventory);
    }

    public ExtrudingMachineMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_metal_former_advanced blockEntity) {
        super(containerId, playerInventory, blockEntity);
    }
}