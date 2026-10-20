package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.menu.producer.RedstoneReactorCoolantInjectorMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/** GUI of the Redstone Coolant Injector; see {@link mio_icif_gui_reactor_coolant_injector}. */
@OnlyIn(Dist.CLIENT)
public class mio_icif_gui_redstone_reactor_coolant_injector extends mio_icif_gui_reactor_coolant_injector<RedstoneReactorCoolantInjectorMenu> {

    public mio_icif_gui_redstone_reactor_coolant_injector(RedstoneReactorCoolantInjectorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }
}
