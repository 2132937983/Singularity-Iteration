// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.registry.mio_icif_data_components;
import com.miophas.singularity_iteration.core.api.item.ArmorFeatureInfo;
import com.miophas.singularity_iteration.core.prefab.item.AbstractElectricArmor;
import com.miophas.singularity_iteration.core.prefab.item.ArmorFeatures;
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
    /** Damage the deflector can cancel in one hit (two hearts more than a diamond sword crit). */
    public static final float DEFLECTOR_MAX_PER_HIT = 12.0F;
    /** Station work per unit: ticks and EU per tick. */
    public static final int INSTALL_TICKS = 100;
    public static final long INSTALL_EU_PER_TICK = 64;
    /** Drain is collected once a second so the piece is not re-synced every tick. */
    public static final int DRAIN_INTERVAL = 20;

    public static int capacity(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> 4;
            case CHEST -> 3;
            case LEGS, FEET -> 2;
            default -> 0;
        };
    }

    public static boolean isSuitPiece(ItemStack stack) {
        return !stack.isEmpty() && stack.is(QUANTUM_SUIT) && stack.getItem() instanceof ArmorItem;
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

    public enum InstallCheck { OK, NOT_A_SUIT_PIECE, WRONG_SLOT, ALREADY_INSTALLED, FULL }

    public static InstallCheck check(ItemStack piece, SuitModuleType type) { return check(piece, UnitRef.of(type)); }
    public static boolean install(ItemStack piece, SuitModuleType type) { return install(piece, UnitRef.of(type)); }
    public static boolean remove(ItemStack piece, SuitModuleType type) { return remove(piece, UnitRef.of(type)); }

    /** 0.1.8.0: install check for both kinds of unit (built-in and addon). Addon units count against the capacity too. */
    public static InstallCheck check(ItemStack piece, UnitRef unit) {
        if (!isSuitPiece(piece)) return InstallCheck.NOT_A_SUIT_PIECE;
        EquipmentSlot slot = slotOf(piece);
        if (slot == null || !unit.fits(slot)) return InstallCheck.WRONG_SLOT;
        InstalledModules modules = installed(piece);
        if (modules.has(unit.key())) return InstallCheck.ALREADY_INSTALLED;
        if (modules.units().size() >= capacity(slot)) return InstallCheck.FULL;
        return InstallCheck.OK;
    }

    public static boolean install(ItemStack piece, UnitRef unit) {
        if (check(piece, unit) != InstallCheck.OK) return false;
        piece.set(mio_icif_data_components.SUIT_MODULES.get(), installed(piece).with(unit.key()));
        return true;
    }

    public static boolean remove(ItemStack piece, UnitRef unit) {
        InstalledModules modules = installed(piece);
        if (!modules.has(unit.key())) return false;
        InstalledModules next = modules.without(unit.key());
        if (next.ids().isEmpty()) piece.remove(mio_icif_data_components.SUIT_MODULES.get());
        else piece.set(mio_icif_data_components.SUIT_MODULES.get(), next);
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

    public static boolean hasVisor(Player player) {
        return isSuitPiece(player.getItemBySlot(EquipmentSlot.HEAD));
    }

    /** Installed, switched on, powered, and (for HUD units) seen through a quantum visor. */
    public static boolean isActive(Player player, SuitModuleType type) {
        ItemStack piece = wornPieceWith(player, type);
        if (piece.isEmpty()) return false;
        if (type.needsVisor() && !hasVisor(player)) return false;
        // 0.1.8.0: a full set shares one energy pool (SuitEnergyPool).
        if (SuitEnergyPool.available(player, piece) < Math.max(1, type.drainPerTick())) return false;
        // unit switches are toggle features: read the switch state directly, without building the feature list
        return com.miophas.singularity_iteration.core.prefab.item.ArmorFeatureState.isEnabled(piece, type.featureKey());
    }

    /** 0.1.8.0: the same rule for either kind of unit. */
    public static boolean isActive(Player player, UnitRef unit) {
        if (unit.builtin() != null) return isActive(player, unit.builtin());
        ItemStack piece = wornPieceWith(player, unit);
        if (piece.isEmpty()) return false;
        if (unit.needsVisor() && !hasVisor(player)) return false;
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

    /** Server, once a second: collect the drain of every active unit (shared by a full set). */
    public static void drain(Player player) {
        for (SuitModuleType type : SuitModuleType.VALUES) {
            if (type.drainPerTick() <= 0 || !isActive(player, type)) continue;
            extract(player, wornPieceWith(player, type), type.drainPerTick() * DRAIN_INTERVAL);
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
        // 0.1.8.0: addon units get a panel switch as the built-in ones do
        for (UnitRef unit : modules.units()) out.add(new ArmorFeatureInfo(slot, unit.featureKey(), unit.nameKey()));
        return out;
    }

    private static boolean bootstrapped;

    /** Called from the mod constructor. */
    public static synchronized void bootstrap() {
        if (bootstrapped) return;
        bootstrapped = true;
        ArmorFeatures.registerExtension(SuitModules::features);
        com.miophas.singularity_iteration.core.api.suit.SuitModuleRegistry.setQuery((player, spec) -> isActive(player, UnitRef.of(spec)));
        ManeuverMode.bootstrap();
    }
}
