package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.menu.producer.LapisReactorCoolantInjectorMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/** GUI of the Lapis Coolant Injector; see {@link mio_icif_gui_reactor_coolant_injector}. */
@OnlyIn(Dist.CLIENT)
public class mio_icif_gui_lapis_reactor_coolant_injector extends mio_icif_gui_reactor_coolant_injector<LapisReactorCoolantInjectorMenu> {

    public mio_icif_gui_lapis_reactor_coolant_injector(LapisReactorCoolantInjectorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }
}
