package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.menu.producer.MetalFormerAdvancedMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

@Deprecated
@SuppressWarnings("null")
public class mio_icif_gui_extruding_machine extends mio_icif_gui_metal_former_advanced {

    public mio_icif_gui_extruding_machine(MetalFormerAdvancedMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }
}