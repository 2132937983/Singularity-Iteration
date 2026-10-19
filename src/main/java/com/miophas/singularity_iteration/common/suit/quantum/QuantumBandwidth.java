// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit.quantum;

import com.miophas.singularity_iteration.common.suit.SuitModuleType;
import com.miophas.singularity_iteration.common.suit.SuitModules;
import com.miophas.singularity_iteration.common.suit.UnitRef;
import com.miophas.singularity_iteration.core.api.suit.SuitBandwidth;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Compute bandwidth (0.1.10.1). Every active unit on a worn quantum piece loads the suit computer
 * with its QB cost (a unit installed on two pieces counts once); a Mk.II unit costs 1 QB more.
 * The budget is {@link #BASE_BUDGET} QB, +{@link #CORE_BONUS} with an active compute core.
 *
 * <p>The base budget is the heaviest loadout that 0.1.9.2 allowed (helmet: ESP, predictor, holomap,
 * guidance = 11; chestplate: threat sensor, blast warning, deflector = 4; leggings and boots: ore
 * scanner, telemetry, resonance field = 5), proved by enumeration in {@code check_1101.py}. Thus a
 * suit built before 0.1.10.1 never saturates. Above the budget the suit saturates: the sensors
 * refresh at half rate and the coherence regenerates at half speed; nothing switches off.
 */
public final class QuantumBandwidth {
    private QuantumBandwidth() {}

    public static final int BASE_BUDGET = 20, CORE_BONUS = 8;
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static boolean bootstrapped;

    public static synchronized void bootstrap() {
        if (bootstrapped) return;
        bootstrapped = true;
        SuitBandwidth.setView(new SuitBandwidth.View() {
            @Override public int load(Player player) { return QuantumBandwidth.load(player); }
            @Override public int budget(Player player) { return QuantumBandwidth.budget(player); }
        });
    }

    /** QB cost of a built-in unit at Mk.I. */
    public static int cost(SuitModuleType type) {
        return switch (type) {
            case ENTITY_ESP, BEHAVIOR_PREDICTOR, HOLOMAP, ORE_SCANNER, PHASE_MODULATOR, ENTANGLEMENT_RELAY -> 3;
            case SMART_GUIDANCE, THREAT_SENSOR, PHASE_JUMP_DRIVE -> 2;
            case BALLISTIC, BLAST_WARNING, GRID_TELEMETRY, DEFLECTOR, RESONANCE_FIELD -> 1;
            default -> 0;
        };
    }

    /** QB cost of a unit at {@code tier} (built-in: +1 for a quantum Mk.II; addon: its registered cost). */
    public static int cost(UnitRef unit, int tier) {
        if (unit.builtin() != null) return cost(unit.builtin()) + (tier >= 2 && unit.builtin().quantumMk2() ? 1 : 0);
        return SuitBandwidth.cost(unit.key());
    }

    /** Current load: the active units on worn quantum pieces, each unit once. */
    public static int load(Player player) {
        int sum = 0;
        Set<String> seen = new HashSet<>();
        for (EquipmentSlot slot : ARMOR) {
            ItemStack piece = player.getItemBySlot(slot);
            if (!SuitModules.isQuantumPiece(piece)) continue;
            for (UnitRef unit : SuitModules.installed(piece).units()) {
                if (!seen.add(unit.key()) || !SuitModules.isActive(player, unit)) continue;
                sum += cost(unit, SuitModules.tier(piece, unit.key()));
            }
        }
        return sum;
    }

    public static int budget(Player player) {
        return BASE_BUDGET + CORE_BONUS * QuantumLinks.activeComputeCores(player);   // 0.1.10.6: the cores stack
    }

    public static boolean saturated(Player player) {
        return load(player) > budget(player);
    }
}
