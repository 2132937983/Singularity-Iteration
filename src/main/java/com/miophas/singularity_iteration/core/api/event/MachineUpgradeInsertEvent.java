package com.miophas.singularity_iteration.core.api.event;

import com.miophas.singularity_iteration.core.api.machine.IProducerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * Upgrade insert event.
 *
 * <p><b>When it fires</b>: when a player or an automation system inserts an upgrade into an upgrade slot of a machine.
 *
 * <p><b>Frequency</b>: one time for each insert operation. Usually when a player clicks an upgrade slot, or when a pipe or hopper inserts an item.
 *
 * <p><b>Scope</b>: all machine types that implement IProducerBlock, including:
 * <ul>
 *   <li>basic machines such as the compressor, extractor, macerator and centrifuge</li>
 *   <li>advanced machines such as the welder and the precision assembler</li>
 *   <li>power generation equipment such as the nuclear reactor and the fluid reactor</li>
 * </ul>
 *
 * <p>Cancel the event to stop the insertion.
 *
 * <p>Example:
 * <pre>{@code
 * @SubscribeEvent
 * public void onUpgradeInsert(MachineUpgradeInsertEvent event) {
 *     // Stop the insertion of a specific upgrade
 *     if (event.getUpgrade().getItem() == MyModItems.FORBIDDEN_UPGRADE.get()) {
 *         event.setCanceled(true);
 *     }
 * }
 * }</pre>
 */
public class MachineUpgradeInsertEvent extends Event implements ICancellableEvent {

    private final Level level;
    private final BlockPos pos;
    private final IProducerBlock machine;
    private final int slot;
    private ItemStack upgrade;

    public MachineUpgradeInsertEvent(Level level, BlockPos pos, IProducerBlock machine, int slot, ItemStack upgrade) {
        this.level = level;
        this.pos = pos;
        this.machine = machine;
        this.slot = slot;
        this.upgrade = upgrade;
    }

    /**
     * Returns the world.
     */
    public Level getLevel() {
        return level;
    }

    /**
     * Returns the machine position.
     */
    public BlockPos getPos() {
        return pos;
    }

    /**
     * Returns the machine instance.
     */
    public IProducerBlock getMachine() {
        return machine;
    }

    /**
     * Returns the index of the upgrade slot.
     */
    public int getSlot() {
        return slot;
    }

    /**
     * Returns the upgrade item.
     */
    public ItemStack getUpgrade() {
        return upgrade;
    }

    /**
     * Replaces the upgrade item.
     */
    public void setUpgrade(ItemStack upgrade) {
        this.upgrade = upgrade;
    }
}