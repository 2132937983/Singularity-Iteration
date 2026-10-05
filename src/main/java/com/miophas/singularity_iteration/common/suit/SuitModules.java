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

    public static InstallCheck check(ItemStack piece, SuitModuleType type) {
        if (!isSuitPiece(piece)) return InstallCheck.NOT_A_SUIT_PIECE;
        EquipmentSlot slot = slotOf(piece);
        if (slot == null || !type.fits(slot)) return InstallCheck.WRONG_SLOT;
        InstalledModules modules = installed(piece);
        if (modules.has(type)) return InstallCheck.ALREADY_INSTALLED;
        if (modules.types().size() >= capacity(slot)) return InstallCheck.FULL;
        return InstallCheck.OK;
    }

    public static boolean install(ItemStack piece, SuitModuleType type) {
        if (check(piece, type) != InstallCheck.OK) return false;
        piece.set(mio_icif_data_components.SUIT_MODULES.get(), installed(piece).with(type));
        return true;
    }

    public static boolean remove(ItemStack piece, SuitModuleType type) {
        InstalledModules modules = installed(piece);
        if (!modules.has(type)) return false;
        InstalledModules next = modules.without(type);
        if (next.ids().isEmpty()) piece.remove(mio_icif_data_components.SUIT_MODULES.get());
        else piece.set(mio_icif_data_components.SUIT_MODULES.get(), next);
        return true;
    }

    // ------------------------------------------------------------------ worn state

    /** The worn piece that carries the unit, or EMPTY. */
    public static ItemStack wornPieceWith(Player player, SuitModuleType type) {
        for (EquipmentSlot slot : type.slots()) {
            ItemStack stack = player.getItemBySlot(slot);
            if (isSuitPiece(stack) && has(stack, type)) return stack;
        }
        return ItemStack.EMPTY;
    }

    public static boolean hasVisor(Player player) {
        return isSuitPiece(player.getItemBySlot(EquipmentSlot.HEAD));
    }

    /** Installed, switched on, powered, and (for HUD units) seen through a quantum visor. */
    public static boolean isActive(Player player, SuitModuleType type) {
        ItemStack piece = wornPieceWith(player, type);
        if (piece.isEmpty() || !ArmorFeatures.isEnabled(piece, type.featureKey())) return false;
        if (type.needsVisor() && !hasVisor(player)) return false;
        return energy(piece) >= Math.max(1, type.drainPerTick());
    }

    public static long energy(ItemStack piece) {
        return piece.getItem() instanceof AbstractElectricArmor armor ? armor.getEnergy(piece) : 0;
    }

    public static long extract(ItemStack piece, long amount) {
        return piece.getItem() instanceof AbstractElectricArmor armor ? armor.extractEnergy(piece, amount) : 0;
    }

    /** Units active on the player right now (cheap: at most 4 worn pieces). */
    public static List<SuitModuleType> activeUnits(Player player) {
        List<SuitModuleType> out = new ArrayList<>();
        for (SuitModuleType type : SuitModuleType.values()) if (isActive(player, type)) out.add(type);
        return out;
    }

    /** EU/t of every active unit together. */
    public static long totalDrain(Player player) {
        long sum = 0;
        for (SuitModuleType type : activeUnits(player)) sum += type.drainPerTick();
        return sum;
    }

    /** Server, once a second: collect the drain of every active unit from its own piece. */
    public static void drain(Player player) {
        for (SuitModuleType type : SuitModuleType.values()) {
            if (type.drainPerTick() <= 0 || !isActive(player, type)) continue;
            extract(wornPieceWith(player, type), type.drainPerTick() * DRAIN_INTERVAL);
        }
    }

    // ------------------------------------------------------------------ console features

    static List<ArmorFeatureInfo> features(ItemStack stack) {
        if (!isSuitPiece(stack)) return List.of();
        InstalledModules modules = installed(stack);
        if (modules.ids().isEmpty()) return List.of();
        EquipmentSlot slot = slotOf(stack);
        List<ArmorFeatureInfo> out = new ArrayList<>();
        for (SuitModuleType type : modules.types()) out.add(new ArmorFeatureInfo(slot, type.featureKey(), type.nameKey()));
        return out;
    }

    private static boolean bootstrapped;

    /** Called from the mod constructor. */
    public static synchronized void bootstrap() {
        if (bootstrapped) return;
        bootstrapped = true;
        ArmorFeatures.registerExtension(SuitModules::features);
        ViltrumFlight.bootstrap();
    }
}
