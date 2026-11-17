package com.miophas.singularity_iteration.common.blockentity.generator;

import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import com.miophas.singularity_iteration.core.api.energy.grid.*;
import com.miophas.singularity_iteration.core.runtime.energy.grid.*;
import com.miophas.singularity_iteration.core.api.energy.ICableTier;
import com.miophas.singularity_iteration.core.api.machine.IGeneratorBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.List;

@SuppressWarnings("null")
public class mio_icif_advanced_drop_generator extends mio_icif_drop_generator {

    /** 0.1.11.13: the same machine with bigger numbers (it was a full copy of the basic class). */
    private static final Profile ADVANCED = new Profile(100000L, 512L, CableTier.HV, 40L, 5, 2);

    public mio_icif_advanced_drop_generator(BlockPos pos, BlockState state) {
        super(pos, state, mio_icif_block_entities.ADVANCED_DROP_GENERATOR_ENTITY_TYPE.get(), ADVANCED);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_advanced_drop_generator blockEntity) {
        mio_icif_drop_generator.tick(level, pos, state, blockEntity);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.advanced_drop_generator");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.miophas.singularity_iteration.common.menu.generator.AdvancedDropGeneratorMenu(containerId, playerInventory, this);
    }
}
