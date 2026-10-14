// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.registry.mio_icif_data_components;
import com.miophas.singularity_iteration.core.api.item.ArmorFeatureInfo;
import com.miophas.singularity_iteration.core.prefab.item.AbstractElectricArmor;
import com.miophas.singularity_iteration.core.prefab.item.ArmorFeatures;
import com.miophas.singularity_iteration.core.api.suit.SuitClass;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Rules of the quantum suit upgrade units: which piece takes which unit, how many units fit,
 * when a unit is active and what it costs.
 */
public final class SuitModules {
    private SuitModules() {}

    /** Items that accept upgrade units: the quantum pieces and the helmets made from the quantum helmet. */
    public static final TagKey<Item> QUANTUM_SUIT = TagKey.create(Registries.ITEM,
        ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "quantum_suit"));

    /** EU the deflector spends for one point of damage it cancels. */
    public static final long DEFLECTOR_EU_PER_DAMAGE = 4_000;
    /** 0.1.10.0: the same for a Mk.II deflector. */
    public static final long DEFLECTOR_EU_PER_DAMAGE_MK2 = 3_000;
    /** Damage the deflector can cancel in one hit (two hearts more than a diamond sword crit). */
    public static final float DEFLECTOR_MAX_PER_HIT = 12.0F;
    /** Station work per unit: ticks and EU per tick. */
    public static final int INSTALL_TICKS = 100;
    public static final long INSTALL_EU_PER_TICK = 64;
    /** Drain is collected once a second so the piece is not re-synced every tick. */
    public static final int DRAIN_INTERVAL = 20;

    /** 0.1.8.5: the four nano suit pieces; they take units by lattice points (LP). */
    public static final TagKey<Item> NANO_SUIT = TagKey.create(Registries.ITEM,
        ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "nano_suit"));

    /** LP that one lattice densifier adds to a nano piece, and the number of densifiers a piece takes. */
    public static final int LATTICE_STEP = 2, LATTICE_MAX_LEVEL = 2;

    /** Quantum pieces: number of units per piece. */
    public static int capacity(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> 4;
            case CHEST -> 3;
            case LEGS, FEET -> 2;
            default -> 0;
        };
    }

    /** 0.1.10.0: unit slots that one quantum bay expander adds, and the number of expanders a piece takes. */
    public static final int BAY_STEP = 1, BAY_MAX_LEVEL = 1;

    /**
     * 0.1.10.0: unit slots of a quantum piece: the base of its slot plus its expansion bays.
     * A piece without the bay component has the base count, as before 0.1.10.0.
     */
    public static int capacity(ItemStack piece) {
        EquipmentSlot slot = slotOf(piece);
        return slot == null ? 0 : capacity(slot) + BAY_STEP * bayLevel(piece);
    }

    /** Expansion bay level of a quantum piece, 0..{@link #BAY_MAX_LEVEL}. */
    public static int bayLevel(ItemStack piece) {
        Integer level = piece.isEmpty() ? null : piece.get(mio_icif_data_components.SUIT_BAYS.get());
        return level == null ? 0 : Math.max(0, Math.min(BAY_MAX_LEVEL, level));
    }

    /** True when a quantum bay expander can still be applied to {@code piece}. */
    public static boolean canExpand(ItemStack piece) {
        return isQuantumPiece(piece) && slotOf(piece) != null && bayLevel(piece) < BAY_MAX_LEVEL;
    }

    /** Applies one quantum bay expander (+{@link #BAY_STEP} unit slot). */
    public static boolean expand(ItemStack piece) {
        if (!canExpand(piece)) return false;
        piece.set(mio_icif_data_components.SUIT_BAYS.get(), bayLevel(piece) + 1);
        return true;
    }

    /** Nano pieces (0.1.8.5): base lattice points per piece (head 4, chest 6, legs 4, feet 4). */
    public static int latticeBase(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD, LEGS, FEET -> 4;
            case CHEST -> 6;
            default -> 0;
        };
    }

    /** Densifier level of a nano piece, 0..{@link #LATTICE_MAX_LEVEL}. */
    public static int latticeLevel(ItemStack piece) {
        Integer level = piece.isEmpty() ? null : piece.get(mio_icif_data_components.SUIT_LATTICE.get());
        return level == null ? 0 : Math.max(0, Math.min(LATTICE_MAX_LEVEL, level));
    }

    /** Lattice points of a nano piece (base + densifiers); 0 for any other item. */
    public static int latticeCapacity(ItemStack piece) {
        EquipmentSlot slot = slotOf(piece);
        if (!isNanoPiece(piece) || slot == null) return 0;
        return latticeBase(slot) + LATTICE_STEP * latticeLevel(piece);
    }

    /** Lattice points the installed units take. */
    public static int latticeUsed(ItemStack piece) {
        int used = 0;
        for (UnitRef unit : installed(piece).units()) used += unit.latticePoints();
        return used;
    }

    /** True when a lattice densifier can still be applied to {@code piece}. */
    public static boolean canDensify(ItemStack piece) {
        return isNanoPiece(piece) && latticeLevel(piece) < LATTICE_MAX_LEVEL;
    }

    /** Applies one lattice densifier (+{@link #LATTICE_STEP} LP). */
    public static boolean densify(ItemStack piece) {
        if (!canDensify(piece)) return false;
        piece.set(mio_icif_data_components.SUIT_LATTICE.get(), latticeLevel(piece) + 1);
        return true;
    }

    /** 0.1.8.5: the suit class of a piece, or null when it takes no units. */
    @Nullable
    public static SuitClass suitClass(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ArmorItem)) return null;
        if (stack.is(QUANTUM_SUIT)) return SuitClass.QUANTUM;
        if (stack.is(NANO_SUIT)) return SuitClass.NANO;
        return null;
    }

    /** A piece that takes units: a quantum piece or (0.1.8.5) a nano piece. */
    public static boolean isSuitPiece(ItemStack stack) {
        return suitClass(stack) != null;
    }

    public static boolean isQuantumPiece(ItemStack stack) {
        return suitClass(stack) == SuitClass.QUANTUM;
    }

    public static boolean isNanoPiece(ItemStack stack) {
        return suitClass(stack) == SuitClass.NANO;
    }

    @Nullable
    public static EquipmentSlot slotOf(ItemStack stack) {
        return stack.getItem() instanceof ArmorItem armor ? armor.getEquipmentSlot() : null;
    }

    public static InstalledModules installed(ItemStack stack) {
        if (stack.isEmpty()) return InstalledModules.EMPTY;
        InstalledModules value = stack.get(mio_icif_data_components.SUIT_MODULES.get());
        return value == null ? InstalledModules.EMPTY : value;
    }

    public static boolean has(ItemStack stack, SuitModuleType type) {
        return installed(stack).has(type);
    }

    /**
     * Result of an install check. 0.1.8.5: WRONG_CLASS (a nano-only unit on a quantum piece, or a
     * quantum unit on a nano piece) and NO_LATTICE (not enough free lattice points on a nano piece).
     */
    public enum InstallCheck { OK, NOT_A_SUIT_PIECE, WRONG_SLOT, ALREADY_INSTALLED, FULL, WRONG_CLASS, NO_LATTICE }

    public static InstallCheck check(ItemStack piece, SuitModuleType type) { return check(piece, UnitRef.of(type)); }
    public static boolean install(ItemStack piece, SuitModuleType type) { return install(piece, UnitRef.of(type)); }
    public static boolean remove(ItemStack piece, SuitModuleType type) { return remove(piece, UnitRef.of(type)); }

    /** 0.1.8.0: install check for both kinds of unit (built-in and addon). Addon units count against the capacity too. */
    public static InstallCheck check(ItemStack piece, UnitRef unit) {
        return check(piece, unit, 1);
    }

    // ------------------------------------------------------------------ Mk.II (0.1.8.6)

    /** Tier of an installed unit: 2 when it is installed as Mk.II, else 1 (0 when it is not installed). */
    public static int tier(ItemStack piece, String key) {
        if (!installed(piece).has(key)) return 0;
        InstalledModules mk2 = piece.get(mio_icif_data_components.SUIT_MK2.get());
        return mk2 != null && mk2.has(key) ? 2 : 1;
    }

    /** Tier of a built-in unit on the worn piece that carries it (0 when none). */
    public static int tier(Player player, SuitModuleType type) {
        ItemStack piece = wornPieceWith(player, type);
        return piece.isEmpty() ? 0 : tier(piece, type.id());
    }

    /** True when the check of {@code unit} at {@code tier} on {@code piece} is an in-place Mk.I to Mk.II upgrade. */
    public static boolean isUpgrade(ItemStack piece, UnitRef unit, int tier) {
        return tier > 1 && tier(piece, unit.key()) == 1;
    }

    /**
     * 0.1.8.6: install check for a unit item of {@code tier}. A Mk.II item over the installed Mk.I
     * of the same unit is an upgrade (no extra lattice points or unit slot).
     */
    public static InstallCheck check(ItemStack piece, UnitRef unit, int tier) {
        SuitClass suitClass = suitClass(piece);
        if (suitClass == null) return InstallCheck.NOT_A_SUIT_PIECE;
        if (!unit.allows(suitClass)) return InstallCheck.WRONG_CLASS;
        EquipmentSlot slot = slotOf(piece);
        if (slot == null || !unit.fits(slot)) return InstallCheck.WRONG_SLOT;
        InstalledModules modules = installed(piece);
        if (modules.has(unit.key())) return isUpgrade(piece, unit, tier) ? InstallCheck.OK : InstallCheck.ALREADY_INSTALLED;
        if (suitClass == SuitClass.NANO) {
            // 0.1.8.5: a nano piece counts lattice points, not units
            if (latticeUsed(piece) + unit.latticePoints() > latticeCapacity(piece)) return InstallCheck.NO_LATTICE;
        } else if (modules.units().size() >= capacity(piece)) {             // 0.1.10.0: + expansion bays
            return InstallCheck.FULL;
        }
        return InstallCheck.OK;
    }

    public static boolean install(ItemStack piece, UnitRef unit) {
        return install(piece, unit, 1);
    }

    /** 0.1.8.6: installs (or upgrades to) {@code tier}. */
    public static boolean install(ItemStack piece, UnitRef unit, int tier) {
        if (check(piece, unit, tier) != InstallCheck.OK) return false;
        piece.set(mio_icif_data_components.SUIT_MODULES.get(), installed(piece).with(unit.key()));
        if (tier > 1) {
            InstalledModules mk2 = piece.getOrDefault(mio_icif_data_components.SUIT_MK2.get(), InstalledModules.EMPTY);
            piece.set(mio_icif_data_components.SUIT_MK2.get(), mk2.with(unit.key()));
        }
        return true;
    }

    public static boolean remove(ItemStack piece, UnitRef unit) {
        InstalledModules modules = installed(piece);
        if (!modules.has(unit.key())) return false;
        InstalledModules next = modules.without(unit.key());
        if (next.ids().isEmpty()) piece.remove(mio_icif_data_components.SUIT_MODULES.get());
        else piece.set(mio_icif_data_components.SUIT_MODULES.get(), next);
        InstalledModules mk2 = piece.get(mio_icif_data_components.SUIT_MK2.get());
        if (mk2 != null && mk2.has(unit.key())) {
            InstalledModules rest = mk2.without(unit.key());
            if (rest.ids().isEmpty()) piece.remove(mio_icif_data_components.SUIT_MK2.get());
            else piece.set(mio_icif_data_components.SUIT_MK2.get(), rest);
        }
        return true;
    }

    // ------------------------------------------------------------------ worn state

    /** The worn piece that carries the unit, or EMPTY. */
    public static ItemStack wornPieceWith(Player player, SuitModuleType type) {
        for (EquipmentSlot slot : type.slotArray()) {
            ItemStack stack = player.getItemBySlot(slot);
            if (isSuitPiece(stack) && has(stack, type)) return stack;
        }
        return ItemStack.EMPTY;
    }

    /** 0.1.8.0: the worn piece that carries {@code unit} (either kind), or EMPTY. */
    public static ItemStack wornPieceWith(Player player, UnitRef unit) {
        if (unit.builtin() != null) return wornPieceWith(player, unit.builtin());
        for (EquipmentSlot slot : unit.slots()) {
            ItemStack stack = player.getItemBySlot(slot);
            if (isSuitPiece(stack) && installed(stack).has(unit.key())) return stack;
        }
        return ItemStack.EMPTY;
    }

    /** A quantum helmet visor (the HUD of the quantum-only sensor units). */
    public static boolean hasVisor(Player player) {
        return isQuantumPiece(player.getItemBySlot(EquipmentSlot.HEAD));
    }

    /**
     * 0.1.8.5: the visor rule of one unit. A quantum visor shows every HUD unit; the nano helmet
     * visor shows the units that the nano suit can carry (blast warning, threat sensor).
     */
    public static boolean hasVisor(Player player, UnitRef unit) {
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        SuitClass suitClass = suitClass(head);
        return suitClass == SuitClass.QUANTUM || suitClass == SuitClass.NANO && unit.allows(SuitClass.NANO);
    }

    /** Installed, switched on, powered, and (for HUD units) seen through a suit visor. */
    public static boolean isActive(Player player, SuitModuleType type) {
        ItemStack piece = wornPieceWith(player, type);
        if (piece.isEmpty()) return false;
        if (!type.switchable()) return true;               // passive hardware (micro-capacitor)
        if (type.needsVisor() && !hasVisor(player, UnitRef.of(type))) return false;
        // 0.1.8.0: a full set shares one energy pool (SuitEnergyPool). 0.1.8.6: the generators run on an empty suit.
        if (type.needsPower() && SuitEnergyPool.available(player, piece) < Math.max(1, type.drainPerTick())) return false;
        // unit switches are toggle features: read the switch state directly, without building the feature list
        return com.miophas.singularity_iteration.core.prefab.item.ArmorFeatureState.isEnabled(piece, type.featureKey());
    }

    /** 0.1.8.0: the same rule for either kind of unit. */
    public static boolean isActive(Player player, UnitRef unit) {
        if (unit.builtin() != null) return isActive(player, unit.builtin());
        ItemStack piece = wornPieceWith(player, unit);
        if (piece.isEmpty()) return false;
        if (unit.needsVisor() && !hasVisor(player, unit)) return false;
        if (SuitEnergyPool.available(player, piece) < Math.max(1, unit.drainPerTick())) return false;
        return com.miophas.singularity_iteration.core.prefab.item.ArmorFeatureState.isEnabled(piece, unit.featureKey());
    }

    /**
     * 0.1.8.0 public API: runs the behavior of each active addon unit once per tick (server).
     * Free when no addon unit is registered.
     */
    public static void tickAddons(Player player) {
        if (com.miophas.singularity_iteration.core.api.suit.SuitModuleRegistry.isEmpty()) return;
        for (var entry : com.miophas.singularity_iteration.core.api.suit.SuitModuleRegistry.all()) {
            UnitRef unit = UnitRef.of(entry.spec());
            if (!isActive(player, unit)) continue;
            try {
                entry.behavior().activeTick(player, wornPieceWith(player, unit));
            } catch (RuntimeException e) {
                Singularity_Iteration.LOGGER.error("Addon suit unit {} failed; it stays off for this tick", entry.spec().id(), e);
            }
        }
    }

    public static long energy(ItemStack piece) {
        return piece.getItem() instanceof AbstractElectricArmor armor ? armor.getEnergy(piece) : 0;
    }

    public static long extract(ItemStack piece, long amount) {
        // suit units and Special Maneuver Mode pay through here: Energy Saving applies
        return piece.getItem() instanceof AbstractElectricArmor armor
            ? armor.extractEnergy(piece, com.miophas.singularity_iteration.core.api.item.EnergySaving.apply(piece, amount)) : 0;
    }

    /**
     * Pays a cost of {@code piece} for the wearer. With a full set the four
     * pieces share the cost (0.1.8.0). Energy Saving of {@code piece} applies.
     */
    public static long extract(Player player, ItemStack piece, long amount) {
        return SuitEnergyPool.pay(player, piece, amount);
    }

    /** Energy available to a cost of {@code piece}: the pool of a full set, else the piece. */
    public static long energy(Player player, ItemStack piece) {
        return SuitEnergyPool.available(player, piece);
    }

    /** Units active on the player right now (cheap: at most 4 worn pieces). */
    public static List<SuitModuleType> activeUnits(Player player) {
        List<SuitModuleType> out = new ArrayList<>();
        for (SuitModuleType type : SuitModuleType.VALUES) if (isActive(player, type)) out.add(type);
        return out;
    }

    /** EU/t of every active unit together. */
    public static long totalDrain(Player player) {
        long sum = 0;
        for (SuitModuleType type : SuitModuleType.VALUES) if (isActive(player, type)) sum += type.drainPerTick();
        return sum;
    }

    /** 0.1.8.6: drain of one unit per second; a Mk.II unit draws 20 % less (config {@code mk2DrainFactor}). */
    public static long drainPerSecond(SuitModuleType type, int tier) {
        long base = type.drainPerTick() * DRAIN_INTERVAL;
        return tier >= 2 ? Math.round(base * com.miophas.singularity_iteration.common.suit.nano.NanoConfig.mk2DrainFactor()) : base;
    }

    /** Server, once a second: collect the drain of every active unit (shared by a full set). */
    public static void drain(Player player) {
        for (SuitModuleType type : SuitModuleType.VALUES) {
            if (type.drainPerTick() <= 0 || !isActive(player, type)) continue;
            ItemStack piece = wornPieceWith(player, type);
            extract(player, piece, drainPerSecond(type, tier(piece, type.id())));
        }
        // 0.1.8.0: addon units of the public API pay the same way
        if (com.miophas.singularity_iteration.core.api.suit.SuitModuleRegistry.isEmpty()) return;
        for (var entry : com.miophas.singularity_iteration.core.api.suit.SuitModuleRegistry.all()) {
            UnitRef unit = UnitRef.of(entry.spec());
            if (unit.drainPerTick() <= 0 || !isActive(player, unit)) continue;
            extract(player, wornPieceWith(player, unit), unit.drainPerTick() * DRAIN_INTERVAL);
        }
    }

    // ------------------------------------------------------------------ console features

    static List<ArmorFeatureInfo> features(ItemStack stack) {
        if (!isSuitPiece(stack)) return List.of();
        InstalledModules modules = installed(stack);
        if (modules.ids().isEmpty()) return List.of();
        EquipmentSlot slot = slotOf(stack);
        List<ArmorFeatureInfo> out = new ArrayList<>();
        // 0.1.8.0: addon units get a panel switch as the built-in ones do (0.1.8.5: passive hardware has none)
        for (UnitRef unit : modules.units()) if (unit.switchable()) out.add(new ArmorFeatureInfo(slot, unit.featureKey(), unit.nameKey()));
        return out;
    }

    /** 0.1.8.5: extra EU capacity of a nano piece from its micro-capacitor lattice. */
    static long capacityBonus(ItemStack stack) {
        if (!stack.has(mio_icif_data_components.SUIT_MODULES.get())) return 0;
        if (!installed(stack).has(SuitModuleType.MICRO_CAPACITOR) || !isNanoPiece(stack)) return 0;
        long bonus = com.miophas.singularity_iteration.common.suit.nano.NanoConfig.capacitorBonus();
        // 0.1.8.6 Mk.II: +400 000 EU at the default (bonus x 1.6)
        return tier(stack, SuitModuleType.MICRO_CAPACITOR.id()) >= 2 ? bonus * 8 / 5 : bonus;
    }

    private static boolean bootstrapped;

    /** Called from the mod constructor. */
    public static synchronized void bootstrap() {
        if (bootstrapped) return;
        bootstrapped = true;
        ArmorFeatures.registerExtension(SuitModules::features);
        com.miophas.singularity_iteration.core.api.suit.SuitModuleRegistry.setQuery((player, spec) -> isActive(player, UnitRef.of(spec)));
        com.miophas.singularity_iteration.core.api.item.ArmorCapacityBonus.register(SuitModules::capacityBonus);
        ManeuverMode.bootstrap();
        com.miophas.singularity_iteration.common.suit.nano.NanoSuit.bootstrap();
        com.miophas.singularity_iteration.common.suit.quantum.QuantumCore.bootstrap();   // 0.1.10.0 quantum core view
    }
}
