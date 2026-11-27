// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.armory.blade;

import com.miophas.singularity_iteration.common.registry.mio_icif_data_components;
import java.util.function.Function;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.11.233 (SlashBlade collab B1): the three collab blades and the numbers of their resources. The blades are SlashBlade
 * named blades ({@code data/mio_icif/slashblade/named_blades/}), so in the game each one is a {@code slashblade:slashblade}
 * stack; which blade a stack is comes from its {@link BladeCore} once the blade has been seen, or else from
 * {@link #resolver} (SlashBlade's translation key, set by {@code common.compat.slashblade} when SlashBlade is installed).
 * Nothing here names a SlashBlade class.
 *
 * <ul>
 *   <li>Nano: its slash art costs EU and heats the piezo edge; every hit adds a little heat; the edge vents on its own and
 *       an overheated edge refuses the slash art until it has cooled to {@link #NANO_RESUME}.</li>
 *   <li>Quantum: its slash art is paid with the quantum suit's coherence when the wearer has a quantum core; without one
 *       the blade forces the collapse with EU (several times as much).</li>
 *   <li>Singularity: kills feed mass into the micro-singularity; holding any mass costs EU every tick (the containment);
 *       when the EU runs out the containment fails, the blade is unstable and the mass leaks. The slash art costs EU and
 *       mass.</li>
 * </ul>
 */
public enum CollabBlade {
    NANO("nano_blade", 200_000, 4_000),
    QUANTUM("quantum_blade", 400_000, 12_000),
    SINGULARITY("singularity_blade", 2_000_000, 40_000);

    /** Name of the named blade (mio_icif:<name>) and the last part of its translation key. */
    public final String id;
    /** EU the blade holds. */
    public final long euMax;
    /** EU of one slash art (for the quantum blade: only without a quantum core). */
    public final long saEu;

    CollabBlade(String id, long euMax, long saEu) {
        this.id = id;
        this.euMax = euMax;
        this.saEu = saEu;
    }

    // nano: heat of the piezo edge
    public static final float NANO_HEAT_CAP = 100, NANO_SA_HEAT = 35, NANO_HIT_HEAT = 3, NANO_RESUME = 40;
    /** Heat the edge vents per tick (6 per second). */
    public static final float NANO_VENT = 0.3F;
    public static final long NANO_HIT_EU = 50;
    /** 0.1.11.234 (B2): the piezo edge's extra (armour-ignoring) damage: base, plus heat / this, at most {@link #PIEZO_MAX}. */
    public static final float PIEZO_BASE = 2, PIEZO_HEAT_DIV = 25, PIEZO_MAX = 6;
    /** Heat from which the edge is red-hot: hits set the target on fire. */
    public static final float NANO_RED_HOT = 70;
    /** Nanobot self-repair: 1 durability every this many ticks while carried, for this much EU. */
    public static final int NANO_MEND_INTERVAL = 40;
    public static final long NANO_MEND_EU = 400;
    /** 0.1.11.235 (B3): the collapse cut strikes at most this many targets, locked or entangled ones within this range. */
    public static final int COLLAPSE_MAX = 6;
    public static final double COLLAPSE_RANGE = 32, COLLAPSE_FALLBACK_RANGE = 12;
    // quantum: coherence of one slash art when the wearer has a quantum core
    public static final float QUANTUM_SA_COHERENCE = 30;
    // singularity: mass and containment
    public static final float MASS_MAX = 100, SINGULARITY_SA_MASS = 10, MASS_PER_KILL = 1, MASS_PER_BIG_KILL = 5;
    /** EU per tick the containment takes while the blade holds any mass. */
    public static final long CONTAINMENT_EU = 8;
    /** Mass lost per tick while the containment has failed (1 every 2 seconds). */
    public static final float MASS_LEAK = 0.025F;

    public String translationKey() { return "item.mio_icif." + id; }

    /** Folder of the blade's model, texture and charge glow under {@code assets/mio_icif/model/slashblade/}. */
    public String folder() { return id.substring(0, id.length() - "_blade".length()); }

    @Nullable
    public static CollabBlade byId(String id) {
        for (CollabBlade b : values()) if (b.id.equals(id)) return b;
        return null;
    }

    @Nullable
    public static CollabBlade byTranslationKey(@Nullable String key) {
        if (key == null) return null;
        for (CollabBlade b : values()) if (b.translationKey().equals(key)) return b;
        return null;
    }

    /** Which collab blade a stack is when it has no {@link BladeCore} yet; set when SlashBlade is installed. */
    public static volatile Function<ItemStack, CollabBlade> resolver = stack -> null;

    /** The collab blade of a stack, or null. */
    @Nullable
    public static CollabBlade of(ItemStack stack) {
        if (stack.isEmpty()) return null;
        BladeCore core = stack.get(mio_icif_data_components.BLADE_CORE.get());
        if (core != null) {
            CollabBlade kind = byId(core.kind());
            if (kind != null) return kind;
        }
        return resolver.apply(stack);
    }

    /** The resources of a collab blade stack (a fresh, empty core when it has none yet), or null for any other stack. */
    @Nullable
    public static BladeCore core(ItemStack stack) {
        CollabBlade kind = of(stack);
        if (kind == null) return null;
        BladeCore core = stack.get(mio_icif_data_components.BLADE_CORE.get());
        return core != null && kind.id.equals(core.kind()) ? core : BladeCore.fresh(kind);
    }

    public static void set(ItemStack stack, BladeCore core) {
        stack.set(mio_icif_data_components.BLADE_CORE.get(), core);
    }
}
