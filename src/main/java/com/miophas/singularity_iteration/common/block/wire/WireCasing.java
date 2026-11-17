// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.block.wire;

import net.minecraft.util.StringRepresentable;

/**
 * 0.1.11.33 (cable audit W4): what surrounds a cable, one block-state property {@code casing} in place of the four
 * booleans foamlogged, foam_reinforced, foam_hardened and disguised (2048 states per cable, many of them impossible;
 * now 1280). Old saves are converted by {@link WireCasingMigration}.
 * <ul>
 *   <li>{@code none}: a bare cable; {@code scaffold}: laid through an iron scaffold (foams into reinforced foam).</li>
 *   <li>{@code foam} / {@code reinforced_foam}: wet construction foam around it; it sets into
 *       {@code cf_wall} / {@code construction_wall} with the cable still inside and conducting.</li>
 *   <li>{@code disguised_*}: the same four foam casings with an Obscurator disguise (the block entity holds the block).</li>
 * </ul>
 */
public enum WireCasing implements StringRepresentable {
    NONE("none", false, false, false, false),
    SCAFFOLD("scaffold", false, true, false, false),
    FOAM("foam", true, false, false, false),
    REINFORCED_FOAM("reinforced_foam", true, true, false, false),
    CF_WALL("cf_wall", true, false, true, false),
    CONSTRUCTION_WALL("construction_wall", true, true, true, false),
    DISGUISED_FOAM("disguised_foam", true, false, false, true),
    DISGUISED_REINFORCED_FOAM("disguised_reinforced_foam", true, true, false, true),
    DISGUISED_CF_WALL("disguised_cf_wall", true, false, true, true),
    DISGUISED_CONSTRUCTION_WALL("disguised_construction_wall", true, true, true, true);

    private final String name;
    /** Foam (wet or set) fills the block. */
    public final boolean foamlogged;
    /** Reinforced: the scaffold, reinforced foam or the construction wall (blast proof). */
    public final boolean reinforced;
    /** The foam has set into a wall. */
    public final boolean hardened;
    public final boolean disguised;

    WireCasing(String name, boolean foamlogged, boolean reinforced, boolean hardened, boolean disguised) {
        this.name = name; this.foamlogged = foamlogged; this.reinforced = reinforced; this.hardened = hardened; this.disguised = disguised;
    }

    @Override public String getSerializedName() { return name; }

    /** The casing of the four old flags; an impossible combination maps to the nearest real one (no foam: no wall, no disguise). */
    public static WireCasing of(boolean foamlogged, boolean reinforced, boolean hardened, boolean disguised) {
        if (!foamlogged) return reinforced ? SCAFFOLD : NONE;
        for (var casing : values())
            if (casing.foamlogged && casing.reinforced == reinforced && casing.hardened == hardened && casing.disguised == disguised) return casing;
        throw new IllegalStateException();
    }

    /** A set wall (CF wall or construction wall), disguised or not. */
    public boolean embedded() { return foamlogged && hardened; }
    /** Wet foam, disguised or not. */
    public boolean wetFoam() { return foamlogged && !hardened; }
    public WireCasing hardenedForm() { return of(foamlogged, reinforced, true, disguised); }
    public WireCasing withDisguise(boolean disguise) { return of(foamlogged, reinforced, hardened, disguise); }
    /** Foam sprayed on: reinforced when the foam is reinforced or the cable lay in a scaffold. */
    public static WireCasing foamed(boolean reinforced) { return reinforced ? REINFORCED_FOAM : FOAM; }
}
