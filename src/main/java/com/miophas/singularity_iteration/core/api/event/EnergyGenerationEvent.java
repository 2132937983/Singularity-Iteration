package com.miophas.singularity_iteration.core.api.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * Energy generation event.
 *
 * <p><b>When</b>: the event fires when a generator makes energy. Generators include nuclear reactors, solar panels, wind generators and others.
 *
 * <p><b>Frequency</b>: the event fires one time per game tick while the generator works. The frequency is high.
 *
 * <p><b>Scope</b>: all generators that extend AbstractEnergyBlockEntity, including:
 * <ul>
 *   <li>Nuclear reactors and fluid reactors</li>
 *   <li>Solar panels and wind generators</li>
 *   <li>Geothermal generators and RTG generators</li>
 *   <li>All other generators</li>
 * </ul>
 *
 * <p>Cancel the event to stop the energy generation. You can also change the generated amount.
 *
 * <p>Example:
 * <pre>{@code
 * @SubscribeEvent
 * public void onEnergyGenerated(EnergyGenerationEvent event) {
 *     // Record the generated energy
 *     stats.recordGeneration(event.getPos(), event.getAmount());
 *
 *     // Change the generated energy (for example, for an upgrade effect)
 *     event.setAmount(event.getAmount() * 2);
 * }
 * }</pre>
 */
public class EnergyGenerationEvent extends Event implements ICancellableEvent {

    private final Level world;
    private final BlockPos pos;
    private long amount;
    private final long maxCapacity;
    private final long currentStored;

    public EnergyGenerationEvent(Level world, BlockPos pos, long amount, long maxCapacity, long currentStored) {
        this.world = world;
        this.pos = pos;
        this.amount = amount;
        this.maxCapacity = maxCapacity;
        this.currentStored = currentStored;
    }

    /**
     * Gets the level.
     *
     * @return the level
     */
    public Level getWorld() {
        return world;
    }

    /**
     * Gets the position.
     *
     * @return the block position
     */
    public BlockPos getPos() {
        return pos;
    }

    /**
     * Gets the generated energy.
     *
     * @return the generated energy (EU)
     */
    public long getAmount() {
        return amount;
    }

    /**
     * Sets the generated energy.
     * The value is limited to the free space of the storage (maxCapacity - currentStored).
     *
     * @param amount the new generated energy (EU)
     */
    public void setAmount(long amount) {
        // Make sure that the generated energy does not exceed the storage limit
        long space = maxCapacity - currentStored;
        this.amount = Math.min(amount, Math.max(0, space));
    }

    /**
     * Gets the maximum capacity.
     *
     * @return the maximum energy capacity (EU)
     */
    public long getMaxCapacity() {
        return maxCapacity;
    }

    /**
     * Gets the stored energy.
     *
     * @return the energy that is stored now (EU)
     */
    public long getCurrentStored() {
        return currentStored;
    }
}