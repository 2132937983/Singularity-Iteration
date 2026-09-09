package com.singularity_iteration.mio_icif.Blocks.entity.producer;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Menu.Producer.MetalFormerAdvancedMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

@Deprecated
@SuppressWarnings("null")
public class mio_icif_extruding_machine extends mio_icif_metal_former_advanced {

    public mio_icif_extruding_machine(BlockPos pos, BlockState state) {
        super(pos, state, mio_icif_block_entities.METAL_FORMER_ADVANCED_ENTITY_TYPE.get());
    }

    public mio_icif_extruding_machine(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type);
    }

    public mio_icif_extruding_machine(BlockPos pos, BlockState state, BlockEntityType<?> type,
                                       long capacity, long maxReceive, long maxExtract,
                                       int workTime, long energyPerTick) {
        super(pos, state, type);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_extruding_machine blockEntity) {
        mio_icif_metal_former_advanced.tick(level, pos, state, blockEntity);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.metal_former_advanced");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MetalFormerAdvancedMenu(containerId, playerInventory, this);
    }
}