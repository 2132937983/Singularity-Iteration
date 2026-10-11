package com.miophas.singularity_iteration.core.api.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;

/**
 * Generator stop event.
 *
 * <p><b>When it fires</b>: when a generator changes from the generating state to the stopped state
 * (for example: the fuel is empty, or the redstone signal stops).
 *
 * <p><b>How often</b>: one time for each stop of a generator. Usually this occurs when the fuel is empty,
 * the energy storage is full, or the redstone condition is not met.
 *
 * <p><b>Scope</b>: all generators that extend AbstractEnergyBlockEntity (the same as GeneratorStartEvent).
 *
 * <p><b>Note</b>: you cannot cancel this event. Use it to record statistics or to do cleanup.
 *
 * <p>Example:
 * <pre>{@code
 * @SubscribeEvent
 * public void onGeneratorStop(GeneratorStopEvent event) {
 *     // Record the stop time
 *     stats.recordStop(event.getPos());
 * }
 * }</pre>
 */

public class GeneratorStopEvent extends Event {

    private final Level world;
    private final BlockPos pos;
    private final long totalGenerated;

    public GeneratorStopEvent(Level world, BlockPos pos, long totalGenerated) {
        this.world = world;
        this.pos = pos;
        this.totalGenerated = totalGenerated;
    }

    /**
     * Gets the world.
     *
     * @return the world
     */
    public Level getWorld() {
        return world;
    }

    /**
     * Gets the position.
     *
     * @return the generator position
     */
    public BlockPos getPos() {
        return pos;
    }

    /**
     * Gets the total energy generated.
     *
     * @return the total energy generated during this run, in EU
     */
    public long getTotalGenerated() {
        return totalGenerated;
    }
}