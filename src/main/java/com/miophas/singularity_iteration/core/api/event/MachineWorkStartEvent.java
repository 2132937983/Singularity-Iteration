package com.miophas.singularity_iteration.core.api.event;

import com.miophas.singularity_iteration.core.api.machine.IProducerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * Event for the start of machine work.
 *
 * <p><b>When</b>: The event fires when a machine changes from idle to working (for example, the machine starts to process after an input is added).
 *
 * <p><b>How often</b>: One time each time the machine starts. Usually when an input is added, energy is sufficient and the redstone condition is satisfied.
 *
 * <p><b>Scope</b>: All machine types that implement the IProducerBlock interface, including:
 * <ul>
 *   <li>basic machines such as the compressor, extractor, macerator and centrifuge</li>
 *   <li>multi-input machines such as the welder and the precision assembler</li>
 *   <li>the nuclear reactor (at the start of each cycle)</li>
 * </ul>
 *
 * <p>Cancel the event to prevent the start of the machine work.
 *
 * <p>Example:
 * <pre>{@code
 * @SubscribeEvent
 * public void onMachineWorkStart(MachineWorkStartEvent event) {
 *     // Log the machine start.
 *     logger.info("Machine started working at " + event.getPos());
 *
 *     // Prevent work under a specific condition.
 *     if (event.getLevel().isRainingAt(event.getPos().above())) {
 *         event.setCanceled(true);
 *     }
 * }
 * }</pre>
 */
public class MachineWorkStartEvent extends Event implements ICancellableEvent {

    private final Level level;
    private final BlockPos pos;
    private final IProducerBlock machine;

    public MachineWorkStartEvent(Level level, BlockPos pos, IProducerBlock machine) {
        this.level = level;
        this.pos = pos;
        this.machine = machine;
    }

    /**
     * Gets the world.
     */
    public Level getLevel() {
        return level;
    }

    /**
     * Gets the machine position.
     */
    public BlockPos getPos() {
        return pos;
    }

    /**
     * Gets the machine instance.
     */
    public IProducerBlock getMachine() {
        return machine;
    }
}