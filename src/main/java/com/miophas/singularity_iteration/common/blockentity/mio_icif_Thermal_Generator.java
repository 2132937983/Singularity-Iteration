package com.miophas.singularity_iteration.common.blockentity;

import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;

import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractGeneratorBlockEntity;

import com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Thermal generator block entity (LV tier).
 * It burns coal and other fuels to make energy.
 * It uses the EU (Energy Unit) energy system.
 * LV tier settings: 128 EU/tick output, 4000 EU capacity, generation rate 10 EU/tick.
 * 
 * This generator sends energy through the power grid (PowerGrid). It does not send energy directly to adjacent blocks.
 * The grid uses extractPowerForConsumer to take energy from the energyStorage of the generator.
 */
@SuppressWarnings("null")
public class mio_icif_Thermal_Generator extends AbstractGeneratorBlockEntity {
    
    // LV tier: matches original IC2. Capacity is 4000 EU. Generation rate is 10 EU/tick. Maximum output is 32 EU/tick.
    public mio_icif_Thermal_Generator(BlockPos pos, BlockState state) {
        super(pos, state, mio_icif_block_entities.THERMAL_GENERATOR_ENTITY_TYPE.get(),
            SlotLayout.builder().input(1).battery().build(), 10L, 4000L, 0L, 32L, CableTier.LV);
    }

    public mio_icif_Thermal_Generator(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type,
            SlotLayout.builder().input(1).battery().build(), 10L, 4000L, 0L, 32L, CableTier.LV);
    }
    
    /**
     * The block entity registers to the grid when it loads.
     * The base class AbstractEnergyBlockEntity does the registration automatically through EnergyTileLoadEvent.
     */

    /**
     * The thermal generator sends energy through the grid. It does not send energy directly to adjacent blocks.
     * The grid uses extractPowerForConsumer to take energy from the storage of the generator.
     */
    @Override
    protected boolean shouldDirectlyDistributeEnergy() {
        return false;
    }
    
    /**
     * Gets the burn time of the fuel.
 * Like the vanilla furnace, it burns the item to make energy if the item is a fuel.
     * @param fuel the fuel item
     * @return the burn time in ticks. 0 means that the item is not a fuel.
     */
    @Override
    public int getFuelBurnTime(ItemStack fuel) {
        return fuelBurnTime(fuel, energyStorage.scexNetworkControlled());
    }

    /** Shared fuel rule for the machine and its client menu prediction. */
    public static int fuelBurnTime(ItemStack fuel, boolean independentNetwork) {
        if (fuel.isEmpty()) {
            return 0;
        }

        if (independentNetwork && fuel.is(net.minecraft.world.item.Items.LAVA_BUCKET)) {
            return 0;
        }
        int burnTime = fuel.getBurnTime(null);
        // R12 public game observations: coal/charcoal 400, stick 25, planks 75,
        // carpet 16 and slab 37 ticks. New-platform/addon fuels remain extensions.
        if (independentNetwork) burnTime /= 4;
        return burnTime > 0 ? burnTime : 0;
    }
    
    /**
     * 获取发电机的显示名称
     * @return 显示名称
     */
    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.thermal_generator");
    }
    
    /**
     * Creates the menu.
     * @param id the menu ID
     * @param playerInventory the player inventory
     * @param player the player
     * @return the menu instance
     */
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new com.miophas.singularity_iteration.common.menu.generator.ThermalGeneratorMenu(id, playerInventory, this, this.getItemHandler(), null);
    }
}
