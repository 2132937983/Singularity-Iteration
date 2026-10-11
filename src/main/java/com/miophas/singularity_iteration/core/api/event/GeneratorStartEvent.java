package com.miophas.singularity_iteration.core.api.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * Generator start event.
 *
 * <p><b>When</b>: the event fires when a generator changes from the stopped state to the generating state
 * (for example, when a nuclear reactor starts after you put in a fuel rod).
 *
 * <p><b>Frequency</b>: the event fires one time for each generator start.
 * Usually this occurs when fuel is added, the energy conditions are met, or the redstone signal allows it.
 *
 * <p><b>Scope</b>: all generators that extend AbstractEnergyBlockEntity, including:
 * <ul>
 *   <li>Nuclear reactors and fluid reactors</li>
 *   <li>Solar panels (when the light conditions are met)</li>
 *   <li>Wind generators (when wind is present)</li>
 *   <li>Geothermal generators, RTG generators and others</li>
 * </ul>
 *
 * <p>Cancel the event to stop the generator start.
 *
 * <p>Example:
 * <pre>{@code
 * @SubscribeEvent
 * public void onGeneratorStart(GeneratorStartEvent event) {
 *     // Check if it is day
 *     if (event.getWorld().isDay()) {
 *         // Allow generation
 *     } else {
 *         // Cancel generation
 *         event.setCanceled(true);
 *     }
 * }
 * }</pre>
 */

public class GeneratorStartEvent extends Event implements ICancellableEvent {

    private final Level world;
    private final BlockPos pos;
    private final long generationRate;

    public GeneratorStartEvent(Level world, BlockPos pos, long generationRate) {
        this.world = world;
        this.pos = pos;
        this.generationRate = generationRate;
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
     * @return the generator position
     */
    public BlockPos getPos() {
        return pos;
    }

    /**
     * Gets the generation rate.
     *
     * @return the generation rate (EU/tick)
     */
    public long getGenerationRate() {
        return generationRate;
    }
}