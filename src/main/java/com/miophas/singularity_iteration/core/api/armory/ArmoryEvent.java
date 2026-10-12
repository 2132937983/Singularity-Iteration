// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.armory;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * Armory events (0.1.8.0, public API). Singularity Iteration posts them on
 * {@code NeoForge.EVENT_BUS}, on the server thread.
 *
 * <ul>
 *   <li>{@link Summon}: before a suit flies to a wearer (the player or a companion). Cancel it
 *       to stop the summon; no energy is spent and no item moves.</li>
 *   <li>{@link PieceDocked}: after a flying piece is equipped on its wearer.</li>
 *   <li>{@link WingmanDeploy}: before a stored suit deploys as a Wingman drone. Cancel it to
 *       stop the deploy.</li>
 * </ul>
 */
public abstract class ArmoryEvent extends Event {
    private final Player player;

    protected ArmoryEvent(Player player) { this.player = player; }

    /** The player who gave the order (owner of the Armory). */
    public Player getPlayer() { return player; }

    /** Before a summon. The pieces are still in the Armory. */
    public static final class Summon extends ArmoryEvent implements ICancellableEvent {
        private final LivingEntity wearer;
        private final BlockPos armory;
        private final int set;
        private final List<EquipmentSlot> slots;

        public Summon(Player player, LivingEntity wearer, BlockPos armory, int set, List<EquipmentSlot> slots) {
            super(player);
            this.wearer = wearer;
            this.armory = armory;
            this.set = set;
            this.slots = List.copyOf(slots);
        }

        /** The entity that gets the suit (the player, or a companion such as a maid). */
        public LivingEntity getWearer() { return wearer; }
        public BlockPos getArmory() { return armory; }
        /** Suit set index of the Armory (0..5). */
        public int getSet() { return set; }
        /** Slots that receive a piece. */
        public List<EquipmentSlot> getSlots() { return slots; }
    }

    /** After a piece is equipped. Do not replace {@code stack}; it is the worn item now. */
    public static final class PieceDocked extends ArmoryEvent {
        private final LivingEntity wearer;
        private final EquipmentSlot slot;
        private final ItemStack stack;

        public PieceDocked(Player player, LivingEntity wearer, EquipmentSlot slot, ItemStack stack) {
            super(player);
            this.wearer = wearer;
            this.slot = slot;
            this.stack = stack;
        }

        public LivingEntity getWearer() { return wearer; }
        public EquipmentSlot getSlot() { return slot; }
        public ItemStack getStack() { return stack; }
    }

    /** Before a Wingman deploy. */
    public static final class WingmanDeploy extends ArmoryEvent implements ICancellableEvent {
        private final BlockPos armory;
        private final int set;
        private final String mode;

        public WingmanDeploy(Player player, BlockPos armory, int set, String mode) {
            super(player);
            this.armory = armory;
            this.set = set;
            this.mode = mode;
        }

        public BlockPos getArmory() { return armory; }
        public int getSet() { return set; }
        /** Tactical mode name: OFFENSIVE, DISPERSAL, ESCORT or FULTON. */
        public String getMode() { return mode; }
    }
}
