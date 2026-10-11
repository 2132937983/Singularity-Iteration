package com.miophas.singularity_iteration.core.api.event;

import com.miophas.singularity_iteration.core.api.machine.IProducerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * Upgrade removal event.
 *
 * <p><b>When</b>: the event fires when a player or an automation system removes an upgrade from the upgrade slot of a machine.
 *
 * <p><b>Frequency</b>: the event fires one time for each removal.
 * Usually this occurs when a player clicks the upgrade slot, or when a pipe or hopper removes the item.
 *
 * <p><b>Scope</b>: all machine types that implement the IProducerBlock interface (the same as MachineUpgradeInsertEvent).
 *
 * <p>Cancel the event to stop the removal.
 *
 * <p>Example:
 * <pre>{@code
 * @SubscribeEvent
 * public void onUpgradeRemove(MachineUpgradeRemoveEvent event) {
 *     // Stop the removal of the upgrade (for example, lock upgrades while the machine works)
 *     if (event.getMachine().isWorking()) {
 *         event.setCanceled(true);
 *     }
 * }
 * }</pre>
 */
public class MachineUpgradeRemoveEvent extends Event implements ICancellableEvent {

    private final Level level;
    private final BlockPos pos;
    private final IProducerBlock machine;
    private final int slot;
    private final ItemStack upgrade;

    public MachineUpgradeRemoveEvent(Level level, BlockPos pos, IProducerBlock machine, int slot, ItemStack upgrade) {
        this.level = level;
        this.pos = pos;
        this.machine = machine;
        this.slot = slot;
        this.upgrade = upgrade;
    }

    /**
     * Gets the level.
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

    /**
     * Gets the index of the upgrade slot.
     */
    public int getSlot() {
        return slot;
    }

    /**
     * Gets the upgrade item.
     */
    public ItemStack getUpgrade() {
        return upgrade;
    }
}