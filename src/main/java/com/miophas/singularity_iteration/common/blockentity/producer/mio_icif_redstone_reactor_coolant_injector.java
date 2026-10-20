package com.miophas.singularity_iteration.common.blockentity.producer;

import com.miophas.singularity_iteration.common.item.reactor.mio_icif_reactors;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 红石冷却液注入器 (RSH): IC2 1.12 {@code TileEntityRCI_RSH}. Redstone blocks refill the RSH
 * condensators (20,000 heat) of the reactor behind it; see {@link AbstractReactorCoolantInjector}.
 */
public class mio_icif_redstone_reactor_coolant_injector extends AbstractReactorCoolantInjector {

    public mio_icif_redstone_reactor_coolant_injector(BlockPos pos, BlockState state) {
        this(pos, state, null);
    }

    public mio_icif_redstone_reactor_coolant_injector(BlockPos pos, BlockState state, @Nullable BlockEntityType<?> type) {
        super(type != null ? type : mio_icif_block_entities.REDSTONE_REACTOR_COOLANT_INJECTOR_ENTITY_TYPE.get(), pos, state,
            Items.REDSTONE_BLOCK, mio_icif_reactors.CONDENSATOR.get());
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.redstone_reactor_coolant_injector");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.miophas.singularity_iteration.common.menu.producer.RedstoneReactorCoolantInjectorMenu(containerId, playerInventory, this);
    }
}
