// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit.quantum;

import com.miophas.singularity_iteration.common.registry.mio_icif_data_components;
import com.miophas.singularity_iteration.common.registry.mio_icif_sounds;
import com.miophas.singularity_iteration.common.suit.SuitModuleType;
import com.miophas.singularity_iteration.common.suit.SuitModules;
import com.miophas.singularity_iteration.common.suit.UnitRef;
import com.miophas.singularity_iteration.core.api.suit.SuitSynergies;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Entanglement links (0.1.10.1). The worn quantum chestplate is the link hub: its component
 * {@code mio_icif:suit_links} lists the linked synergy ids (by unit, not by piece, so a link holds
 * when the units move between pieces). It takes {@link #BASE_LINKS} links, one more while a compute
 * core is active. When the capacity falls, the first links in the list keep working.
 *
 * <p>A link works when its synergy switch is on (server config), it is within the capacity, and
 * both units are active on worn pieces. A link to a missing unit stays stored (broken) until the
 * wearer removes it.
 */
public final class QuantumLinks {
    private QuantumLinks() {}

    public static final int BASE_LINKS = 2, MAX_STORED = 8;
    /** Results of {@link #toggle}. */
    public static final int LINKED = 0, UNLINKED = 1, NO_HUB = 2, FULL = 3, MISSING_UNITS = 4, UNKNOWN = 5;

    private static boolean bootstrapped;

    public static synchronized void bootstrap() {
        if (bootstrapped) return;
        bootstrapped = true;
        SuitSynergies.setQuery(QuantumLinks::active);
    }

    /** The worn quantum chestplate (the link hub), or EMPTY. */
    public static ItemStack hub(Player player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        return SuitModules.isQuantumPiece(chest) ? chest : ItemStack.EMPTY;
    }

    /** The link ids stored on a piece (empty when none). */
    public static List<String> links(ItemStack piece) {
        if (piece.isEmpty()) return List.of();
        List<String> v = piece.get(mio_icif_data_components.SUIT_LINKS.get());
        return v == null ? List.of() : v;
    }

    /** Links the hub takes now: 2, 3 with an active compute core, 0 without a quantum chestplate. */
    public static int capacity(Player player) {
        if (hub(player).isEmpty()) return 0;
        return BASE_LINKS + (computeCore(player) ? 1 : 0);
    }

    /** True while a compute core is active on a worn quantum piece. */
    public static boolean computeCore(Player player) {
        ItemStack piece = SuitModules.wornPieceWith(player, SuitModuleType.COMPUTE_CORE);
        return !piece.isEmpty() && SuitModules.isQuantumPiece(piece) && SuitModules.isActive(player, SuitModuleType.COMPUTE_CORE);
    }

    /** True when {@code key} is linked and within the capacity. */
    public static boolean linked(Player player, String key) {
        List<String> list = links(hub(player));
        int i = list.indexOf(key);
        return i >= 0 && i < capacity(player);
    }

    /** The two unit keys of a synergy key (built-in or addon), or null for an unknown key. */
    @Nullable
    public static String[] units(String key) {
        QuantumSynergy s = QuantumSynergy.byId(key);
        if (s != null) return new String[]{s.a.id(), s.b.id()};
        var e = SuitSynergies.get(key);
        return e == null ? null : new String[]{e.unitA(), e.unitB()};
    }

    /** True when the synergy works for the wearer now. */
    public static boolean active(Player player, String key) {
        QuantumSynergy s = QuantumSynergy.byId(key);
        if (s != null && !QuantumConfig.enabled("synergy." + s.id)) return false;
        String[] u = units(key);
        if (u == null || !linked(player, key)) return false;
        UnitRef a = UnitRef.byKey(u[0]), b = UnitRef.byKey(u[1]);
        return a != null && b != null && onQuantum(player, u[0]) && onQuantum(player, u[1])
            && SuitModules.isActive(player, a) && SuitModules.isActive(player, b);
    }

    public static boolean active(Player player, QuantumSynergy s) {
        return active(player, s.id);
    }

    /** True when both units of the synergy are installed on worn quantum pieces (it can be linked). */
    public static boolean available(Player player, String key) {
        String[] u = units(key);
        if (u == null) return false;
        return UnitRef.byKey(u[0]) != null && UnitRef.byKey(u[1]) != null && onQuantum(player, u[0]) && onQuantum(player, u[1]);
    }

    /** True when a worn quantum piece carries the unit (the synergies are quantum: a unit on a nano piece does not count). */
    static boolean onQuantum(Player player, String unitKey) {
        for (EquipmentSlot slot : ARMOR) {
            ItemStack piece = player.getItemBySlot(slot);
            if (SuitModules.isQuantumPiece(piece) && SuitModules.installed(piece).has(unitKey)) return true;
        }
        return false;
    }

    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    /** Every synergy key: the built-in ones, then the addon ones. */
    public static List<String> allKeys() {
        List<String> out = new ArrayList<>();
        for (QuantumSynergy s : QuantumSynergy.VALUES) out.add(s.id);
        for (var e : SuitSynergies.all()) out.add(e.id().toString());
        return out;
    }

    /** Synergies that work now (HUD). */
    public static List<String> activeKeys(Player player) {
        List<String> out = new ArrayList<>();
        for (String key : links(hub(player))) if (active(player, key)) out.add(key);
        return out;
    }

    // ------------------------------------------------------------------ server

    /** Server: links or unlinks {@code key}. Returns one of the result codes. */
    public static int toggle(ServerPlayer player, String key) {
        if (key.length() > 128 || units(key) == null) return UNKNOWN;
        ItemStack hub = hub(player);
        if (hub.isEmpty()) return NO_HUB;
        List<String> list = new ArrayList<>(links(hub));
        if (list.remove(key)) {
            store(hub, list);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), mio_icif_sounds.LINK_OFF.get(), SoundSource.PLAYERS, 0.6F, 1.0F);
            return UNLINKED;
        }
        // broken links (an unknown id or a unit gone) do not take a place
        list.removeIf(k -> units(k) == null);
        if (!available(player, key)) return MISSING_UNITS;
        if (list.size() >= capacity(player)) return FULL;
        list.add(key);
        store(hub, list);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), mio_icif_sounds.LINK_ON.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
        return LINKED;
    }

    private static void store(ItemStack hub, List<String> list) {
        if (list.isEmpty()) hub.remove(mio_icif_data_components.SUIT_LINKS.get());
        else hub.set(mio_icif_data_components.SUIT_LINKS.get(), List.copyOf(list.size() > MAX_STORED ? list.subList(0, MAX_STORED) : list));
    }
}
