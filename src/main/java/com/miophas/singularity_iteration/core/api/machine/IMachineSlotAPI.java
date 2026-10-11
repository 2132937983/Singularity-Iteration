package com.miophas.singularity_iteration.core.api.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * Machine slot API.
 *
 * <p>Use it to query the slot layout of a machine and to operate on its items.
 * It is split from IMachineAPI to follow the single responsibility principle.
 */
public interface IMachineSlotAPI {

    /**
     * Gets the slot layout of the machine.
     *
     * @param world the world
     * @param pos the block position
     * @return the slot layout, or an empty Optional if no machine is at the position or the layout is not available
     */
    Optional<ISlotLayout> getMachineSlotLayout(Level world, BlockPos pos);

    /**
     * Gets the item in a slot of the machine.
     *
     * @param world the world
     * @param pos the block position
     * @param slot the slot index
     * @return the item in the slot, or an empty ItemStack if the slot is not valid
     */
    ItemStack getMachineSlotItem(Level world, BlockPos pos, int slot);

    /**
     * Sets the item in a slot of the machine.
     *
     * @param world the world
     * @param pos the block position
     * @param slot the slot index
     * @param stack the item to set
     * @return true if the item was set
     */
    boolean setMachineSlotItem(Level world, BlockPos pos, int slot, ItemStack stack);

    /**
     * Gets the total number of slots of the machine.
     *
     * @param world the world
     * @param pos the block position
     * @return the total number of slots
     */
    int getMachineSlotCount(Level world, BlockPos pos);

    /**
     * Gets the work progress of the machine.
     *
     * @param world the world
     * @param pos the block position
     * @return the current progress in ticks, or 0 if the machine does not work
     * @deprecated use {@link IMachineControlAPI#getProgress(Level, BlockPos)}
     */
    @Deprecated
    int getMachineProgress(Level world, BlockPos pos);

    /**
     * Gets the total progress that the machine needs to complete its work.
     *
     * @param world the world
     * @param pos the block position
     * @return the total progress in ticks
     * @deprecated use {@link IMachineControlAPI#getMaxProgress(Level, BlockPos)}
     */
    @Deprecated
    int getMachineMaxProgress(Level world, BlockPos pos);

    /**
     * Gets the energy consumption of the machine per tick.
     *
     * @param world the world
     * @param pos the block position
     * @return the energy used per tick (EU/t)
     * @deprecated this method belongs to the control/query interfaces; use {@link IProducerBlock#getEnergyPerTick()}
     */
    @Deprecated
    long getMachineEnergyPerTick(Level world, BlockPos pos);
}
