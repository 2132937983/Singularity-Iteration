package com.miophas.singularity_iteration.common.menu.producer;

import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_redstone_reactor_coolant_injector;
import com.miophas.singularity_iteration.common.registry.mio_icif_menus;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.Nullable;

/** Menu of the Redstone Coolant Injector; see {@link ReactorCoolantInjectorMenu}. */
public class RedstoneReactorCoolantInjectorMenu extends ReactorCoolantInjectorMenu {

    public RedstoneReactorCoolantInjectorMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null);
    }

    public RedstoneReactorCoolantInjectorMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_redstone_reactor_coolant_injector blockEntity) {
        super(mio_icif_menus.REDSTONE_REACTOR_COOLANT_INJECTOR_MENU_TYPE.get(), containerId, playerInventory,
            blockEntity != null ? blockEntity.getItemHandler() : null, blockEntity != null ? blockEntity.getContainerData() : null, blockEntity);
    }
}
