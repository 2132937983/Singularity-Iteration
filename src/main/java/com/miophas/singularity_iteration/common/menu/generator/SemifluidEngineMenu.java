package com.miophas.singularity_iteration.common.menu.generator;

import com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_Semifluid_generator;
import com.miophas.singularity_iteration.common.registry.mio_icif_menus;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.Nullable;

/** 0.1.11.15: the engine menu under this generator's menu type. */
public class SemifluidEngineMenu extends EngineMenu {
    public SemifluidEngineMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null);
    }

    public SemifluidEngineMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_Semifluid_generator blockEntity) {
        super(mio_icif_menus.SEMIFLUID_GENERATOR_MENU_TYPE.get(), containerId, playerInventory, blockEntity, blockEntity != null ? blockEntity.getEngineData() : null);
    }
}
