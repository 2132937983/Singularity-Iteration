package com.singularity_iteration.mio_icif.Menu.Tool;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null")
public class mio_icif_meter_menu_provider implements MenuProvider {

    private final BlockPos targetPos;
    private final int modeOrdinal;

    public mio_icif_meter_menu_provider(BlockPos targetPos, int modeOrdinal) {
        this.targetPos = targetPos;
        this.modeOrdinal = modeOrdinal;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("item.mio_icif.item_tool_meter");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new mio_icif_meter_menu(containerId, playerInventory, targetPos, modeOrdinal);
    }

    public BlockPos getTargetPos() {
        return targetPos;
    }

    public int getModeOrdinal() {
        return modeOrdinal;
    }
}

