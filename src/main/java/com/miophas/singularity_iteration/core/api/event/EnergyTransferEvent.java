package com.miophas.singularity_iteration.core.api.event;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * Energy transfer event.
 *
 * <p><b>When it fires</b>: when energy moves through the cable network from one block to another block.
 *
 * <p><b>Frequency</b>: one time each game tick (when energy moves). The frequency is very high. Do not do complex calculations in the handler.
 *
 * <p><b>Scope</b>: all energy that moves through the mio_icif energy network, including:
 * <ul>
 *   <li>generator → cable → energy storage</li>
 *   <li>energy storage → cable → machine</li>
 *   <li>cable → cable (relay in the network)</li>
 * </ul>
 *
 * <p>Cancel the event to stop the energy transfer, or change the transfer amount.
 *
 * <p>Example:
 * <pre>{@code
 * @SubscribeEvent
 * public void onEnergyTransfer(EnergyTransferEvent event) {
 *     // Record the energy transfer
 *     stats.recordTransfer(event.getFrom(), event.getTo(), event.getAmount());
 *
 *     // Decrease the transfer loss (for example, use a better cable)
 *     long loss = event.getAmount() / 10; // 10% loss
 *     event.setAmount(event.getAmount() - loss);
 * }
 * }</pre>
 */
public class EnergyTransferEvent extends Event implements ICancellableEvent {

    private final Level world;
    private final BlockPos from;
    private final BlockPos to;
    private final Direction direction;
    private long amount;
    private final long maxExtract;

    public EnergyTransferEvent(Level world, BlockPos from, BlockPos to, Direction direction, long amount, long maxExtract) {
        this.world = world;
        this.from = from;
        this.to = to;
        this.direction = direction;
        this.amount = amount;
        this.maxExtract = maxExtract;
    }

    /**
     * Returns the world.
     *
     * @return the world
     */
    public Level getWorld() {
        return world;
    }

    /**
     * Returns the source position.
     *
     * @return the position of the energy source
     */
    public BlockPos getFrom() {
        return from;
    }

    /**
     * Returns the target position.
     *
     * @return the position of the energy target
     */
    public BlockPos getTo() {
        return to;
    }

    /**
     * Returns the transfer direction.
     *
     * @return the transfer direction
     */
    public Direction getDirection() {
        return direction;
    }

    /**
     * Returns the transfer amount.
     *
     * @return the energy that moves (EU)
     */
    public long getAmount() {
        return amount;
    }

    /**
     * Sets the transfer amount. The value is clamped to the range 0 to {@link #getMaxExtract()}.
     *
     * @param amount the new transfer amount (EU)
     */
    public void setAmount(long amount) {
        this.amount = Math.max(0, Math.min(amount, maxExtract));
    }

    /**
     * Returns the maximum amount that can be extracted.
     *
     * @return the maximum extract limit (EU)
     */
    public long getMaxExtract() {
        return maxExtract;
    }
}