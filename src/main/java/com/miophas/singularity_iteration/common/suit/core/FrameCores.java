// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit.core;

import com.miophas.singularity_iteration.common.registry.mio_icif_data_components;
import com.miophas.singularity_iteration.common.suit.SuitModules;
import com.miophas.singularity_iteration.core.api.item.ArmorFeatureInfo;
import com.miophas.singularity_iteration.core.api.item.IBatteryItem;
import com.miophas.singularity_iteration.core.api.suit.SuitClass;
import com.miophas.singularity_iteration.core.prefab.item.AbstractElectricArmor;
import com.miophas.singularity_iteration.core.prefab.item.ArmorFeatureState;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Rules and passive effects of the frame cores (0.1.9.0).
 *
 * <p>A core sits in the core socket of a suit helmet or chestplate (data component
 * {@code mio_icif:suit_core}). The socket is separate from the unit slots and the nano lattice,
 * so a core never competes with the units. The Universal Suit Modification Station installs,
 * swaps (the old core comes back) and removes cores.
 */
public final class FrameCores {
    private FrameCores() {}

    /** Propulsion core: capacity added to its chestplate (100 000 000 EU in total with the 10 000 000 of the piece). */
    public static final long PROPULSION_CAPACITY = 90_000_000L;
    /** Graviton emergency repair: below this share of the max health, 1 health per tick for this EU. */
    public static final float GRAVITON_HEAL_THRESHOLD = 0.2F;
    public static final long GRAVITON_HEAL_EU = 5_000;
    /** Propulsion repair: 1.0 health every 10 ticks (2.0 per second) for 15 000 EU (30 000 EU/s). */
    public static final long PROPULSION_HEAL_EU = 15_000;
    /** Propulsion thrust multiplier and maneuver vectoring multiplier. */
    public static final double PROPULSION_THRUST = 1.15, PROPULSION_VECTORING = 1.3;

    static final ResourceLocation ARMOR_ID = ResourceLocation.fromNamespaceAndPath("mio_icif", "frame_core_armor");
    static final ResourceLocation KNOCKBACK_ID = ResourceLocation.fromNamespaceAndPath("mio_icif", "frame_core_knockback");
    static final ResourceLocation BLAST_KNOCKBACK_ID = ResourceLocation.fromNamespaceAndPath("mio_icif", "frame_core_blast_knockback");

    public enum Check { OK, SWAP, NOT_A_SUIT_PIECE, WRONG_CLASS, WRONG_SLOT, ALREADY_INSTALLED }

    // ------------------------------------------------------------------ data

    @Nullable
    public static FrameCore core(ItemStack piece) {
        if (piece.isEmpty()) return null;
        String id = piece.get(mio_icif_data_components.SUIT_CORE.get());
        return FrameCore.byId(id);
    }

    public static Check check(ItemStack piece, FrameCore core) {
        SuitClass suitClass = SuitModules.suitClass(piece);
        if (suitClass == null) return Check.NOT_A_SUIT_PIECE;
        if (!core.allows(suitClass)) return Check.WRONG_CLASS;
        if (SuitModules.slotOf(piece) != core.slot()) return Check.WRONG_SLOT;
        FrameCore current = core(piece);
        if (current == core) return Check.ALREADY_INSTALLED;
        return current == null ? Check.OK : Check.SWAP;
    }

    /** Installs {@code core}; returns the core it replaced (or null). Nothing changes when the check fails. */
    @Nullable
    public static FrameCore install(ItemStack piece, FrameCore core) {
        Check check = check(piece, core);
        if (check != Check.OK && check != Check.SWAP) return null;
        FrameCore old = core(piece);
        long energy = piece.getItem() instanceof AbstractElectricArmor armor ? armor.getEnergy(piece) : 0;
        piece.set(mio_icif_data_components.SUIT_CORE.get(), core.id());
        // the capacity may change (propulsion core): keep the stored energy, clamped to the new capacity
        if (piece.getItem() instanceof AbstractElectricArmor armor) armor.setEnergy(piece, energy);
        return old;
    }

    /** Removes the core; returns it (or null). Energy above the lower capacity is cut. */
    @Nullable
    public static FrameCore remove(ItemStack piece) {
        FrameCore old = core(piece);
        if (old == null) return null;
        long energy = piece.getItem() instanceof AbstractElectricArmor armor ? armor.getEnergy(piece) : 0;
        piece.remove(mio_icif_data_components.SUIT_CORE.get());
        if (piece.getItem() instanceof AbstractElectricArmor armor) armor.setEnergy(piece, energy);
        return old;
    }

    /** The worn piece in the core's socket slot, when it carries {@code core}; else EMPTY. */
    public static ItemStack wornWith(LivingEntity entity, FrameCore core) {
        ItemStack piece = entity.getItemBySlot(core.slot());
        return core(piece) == core ? piece : ItemStack.EMPTY;
    }

    /** The core is worn, the config allows it, and the piece holds energy (the solar arrays need none). */
    public static boolean active(LivingEntity entity, FrameCore core) {
        ItemStack piece = wornWith(entity, core);
        if (piece.isEmpty() || !CoreConfig.enabled(core)) return false;
        return core.isSolar() || SuitModules.energy(piece) > 0;
    }

    public static boolean feature(ItemStack piece, String key) {
        return ArmorFeatureState.isEnabled(piece, key);
    }

    /** Gravity anchor: graviton core active and its anchor switch on. */
    public static boolean anchored(LivingEntity entity) {
        return active(entity, FrameCore.GRAVITON) && feature(wornWith(entity, FrameCore.GRAVITON), FrameCore.KEY_ANCHOR);
    }

    // ------------------------------------------------------------------ hooks

    /** Panel switches of the core of a piece (registered as an {@code ArmorFeatures} extension). */
    static List<ArmorFeatureInfo> features(ItemStack stack) {
        FrameCore core = core(stack);
        if (core == null || !SuitModules.isSuitPiece(stack)) return List.of();
        EquipmentSlot slot = core.slot();
        List<ArmorFeatureInfo> out = new ArrayList<>(4);
        switch (core) {
            case SOLAR_ARRAY_1, SOLAR_ARRAY_2, SOLAR_ARRAY_3 -> {
                out.add(new ArmorFeatureInfo(slot, FrameCore.KEY_SOLAR, "core.mio_icif.feature.solar"));
                out.add(new ArmorFeatureInfo(slot, FrameCore.KEY_SOLAR_ALL, "core.mio_icif.feature.solar_all"));
            }
            case GRAVITON -> {
                out.add(new ArmorFeatureInfo(slot, FrameCore.KEY_HEALING, "core.mio_icif.feature.emergency_repair"));
                out.add(new ArmorFeatureInfo(slot, FrameCore.KEY_ANCHOR, "core.mio_icif.feature.anchor"));
            }
            case PROPULSION -> {
                out.add(new ArmorFeatureInfo(slot, FrameCore.KEY_HEALING, "core.mio_icif.feature.repair"));
                out.add(new ArmorFeatureInfo(slot, FrameCore.KEY_KNOCKBACK, "core.mio_icif.feature.knockback"));
            }
        }
        out.add(new ArmorFeatureInfo(slot, FrameCore.KEY_VISUAL, "core.mio_icif.feature.visual"));
        return out;
    }

    /** Extra capacity: the propulsion core adds {@link #PROPULSION_CAPACITY} to its chestplate. */
    static long capacityBonus(ItemStack stack) {
        if (!stack.has(mio_icif_data_components.SUIT_CORE.get())) return 0;
        return core(stack) == FrameCore.PROPULSION && SuitModules.isQuantumPiece(stack) ? PROPULSION_CAPACITY : 0;
    }

    private static boolean bootstrapped;

    public static synchronized void bootstrap() {
        if (bootstrapped) return;
        bootstrapped = true;
        com.miophas.singularity_iteration.core.prefab.item.ArmorFeatures.registerExtension(FrameCores::features);
        com.miophas.singularity_iteration.core.api.item.ArmorCapacityBonus.register(FrameCores::capacityBonus);
        com.miophas.singularity_iteration.core.prefab.flight.JetpackFlightController.THRUST_SCALE =
            stack -> core(stack) == FrameCore.PROPULSION ? PROPULSION_THRUST : 1.0;
        com.miophas.singularity_iteration.core.prefab.flight.JetpackFlightController.NO_CEILING =
            stack -> core(stack) == FrameCore.PROPULSION;
    }

    // ------------------------------------------------------------------ server tick

    /** Called each tick for every server player (from {@code SuitServer}). */
    public static void tick(ServerPlayer player) {
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD), chest = player.getItemBySlot(EquipmentSlot.CHEST);
        FrameCore headCore = core(head), chestCore = core(chest);
        boolean graviton = chestCore == FrameCore.GRAVITON && active(player, FrameCore.GRAVITON);
        boolean propulsion = chestCore == FrameCore.PROPULSION && active(player, FrameCore.PROPULSION);
        // attributes (always settled, so taking the piece off removes them)
        double armor = graviton || propulsion ? 1.0 : 0;
        boolean anchor = graviton && feature(chest, FrameCore.KEY_ANCHOR);
        double knockback = anchor ? 1.0 : propulsion && feature(chest, FrameCore.KEY_KNOCKBACK) ? 0.5 : 0;
        modifier(player.getAttribute(Attributes.ARMOR), ARMOR_ID, armor);
        modifier(player.getAttribute(Attributes.KNOCKBACK_RESISTANCE), KNOCKBACK_ID, knockback);
        modifier(player.getAttribute(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE), BLAST_KNOCKBACK_ID, anchor ? 1.0 : 0);
        if (headCore != null && headCore.isSolar() && active(player, headCore) && feature(head, FrameCore.KEY_SOLAR)) solar(player, head, headCore);
        // graviton emergency repair: below 20 % health, 1 health per tick for 5 000 EU (as the heavy quantum chestplate)
        if (graviton && feature(chest, FrameCore.KEY_HEALING) && player.getHealth() < player.getMaxHealth() * GRAVITON_HEAL_THRESHOLD
                && SuitModules.energy(player, chest) >= GRAVITON_HEAL_EU) {
            SuitModules.extract(player, chest, GRAVITON_HEAL_EU);
            player.heal(1.0F);
        }
        // propulsion repair: 2.0 health per second (normalised from the old 10 per second)
        if (propulsion && feature(chest, FrameCore.KEY_HEALING) && player.getHealth() < player.getMaxHealth()
                && player.tickCount % 10 == 0 && SuitModules.energy(player, chest) >= PROPULSION_HEAL_EU) {
            SuitModules.extract(player, chest, PROPULSION_HEAL_EU);
            player.heal(1.0F);
        }
    }

    /** Solar array output: day rate in clear daylight, night rate under open sky otherwise, nothing under a roof. */
    public static int solarRate(Level level, Player player, FrameCore core) {
        BlockPos headPos = BlockPos.containing(player.getX(), player.getY() + 1, player.getZ());
        if (!level.canSeeSky(headPos)) return 0;
        long time = level.getDayTime() % 24000;
        boolean day = time < 12000;
        boolean rain = level.isRainingAt(headPos) || level.isThundering();
        return day && !rain ? core.solarDay() : core.solarNight();
    }

    private static void solar(ServerPlayer player, ItemStack head, FrameCore core) {
        if (!(head.getItem() instanceof IBatteryItem battery)) return;
        int rate = solarRate(player.level(), player, core);
        if (feature(head, FrameCore.KEY_SOLAR_ALL)) {
            com.miophas.singularity_iteration.core.runtime.energy.SolarHelmetCharging.tick(head, battery, player.level(), player, rate, core.transferLimit());
        } else {
            com.miophas.singularity_iteration.core.runtime.energy.SolarHelmetCharging.tickArmorOnly(head, battery, player.level(), player, rate, core.transferLimit());
        }
    }

    private static void modifier(@Nullable AttributeInstance attribute, ResourceLocation id, double amount) {
        if (attribute == null) return;
        if (amount == 0) {
            if (attribute.hasModifier(id)) attribute.removeModifier(id);
            return;
        }
        AttributeModifier current = attribute.getModifier(id);
        if (current == null || current.amount() != amount) {
            attribute.addOrUpdateTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
        }
    }
}
