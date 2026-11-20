package com.miophas.singularity_iteration.common.fusion;

import com.miophas.singularity_iteration.common.reactor.ReactorInventory;
import com.miophas.singularity_iteration.core.runtime.reactor.ReactorCycle;
import com.miophas.singularity_iteration.core.runtime.reactor.ReactorCycle.Kind;
import com.miophas.singularity_iteration.core.runtime.reactor.ReactorCycle.Part;
import net.minecraft.world.item.ItemStack;

/**
 * 0.1.11.92: the fusion reactor's blanket: a grid of 9 columns x 6 rows that takes the fission reactor's components and runs
 * the fission reactor's own cycle ({@link ReactorCycle}) on them, with the plasma's neutrons as an outside source.
 *
 * <ul>
 *   <li>Column 1 faces the plasma. Fusion neutrons reach each slot of column c as {@code round(PEAK x f x 0.6^(c-1))} per
 *       cycle, f being the plasma's neutron power over its full power: 2, 1, 1 in the first three columns at full power.</li>
 *   <li>A fuel rod counts them as pulses (more heat and more fission); a lithium cell breeds {@link #BREED_PER_NEUTRON} per
 *       neutron (tritium for the plasma), a near-depleted isotope rod is re-enriched. Every other component works as in the
 *       fission reactor: vents, heat exchangers, coolant cells and platings keep the blanket's heat in check.</li>
 *   <li>The blanket is subcritical: its rods fission only while the plasma burns.</li>
 *   <li>Output (EU/t): the neutron power, 20% more for the share of neutrons lithium cells take (lithium-6 + n releases 4.8 MeV),
 *       plus {@link #EU_PER_FISSION_EU} times the fission reactor output the rods give, times f. Until 0.1.11.98 it was capped
 *       at {@link FusionPlasma#FULL_POWER} (twice the Mekanism fusion reactor); now proton quad rods boxed in by iridium
 *       overclocked vents give 41,440,000 EU/t and stronger layouts more (up to {@link FusionPlasma#OUTPUT_LIMIT}, the grid's
 *       top voltage, as an overflow guard).</li>
 *   <li>Too hot (the hull at its maximum, 10,000 + platings) is an overheated blanket: the plasma disrupts.</li>
 * </ul>
 */
public final class FusionBlanket {
    private FusionBlanket() {}

    public static final int COLUMNS = 9, ROWS = 6, SLOTS = COLUMNS * ROWS;
    public static final int PEAK_NEUTRONS = 2;
    public static final double ATTENUATION = 0.6;
    public static final int BREED_PER_NEUTRON = 40;
    public static final double LITHIUM_GAIN = 0.2;
    /** EU/t of blanket output per EU/t the rods give in the fission reactor's terms, at full neutron power. */
    public static final long EU_PER_FISSION_EU = 14_000L;
    /** The neutron power at full fusion power (16 feed steps). */
    public static final long FULL_NEUTRONS = Math.round(FusionPlasma.MAX_FEED * FusionPlasma.FUSION_PER_FEED * FusionPlasma.NEUTRON_SHARE);

    /** One cycle: the new stacks, the hull heat, the output (EU/t) and its parts. */
    public record Cycle(ItemStack[] stacks, long hull, long maxHull, long output, double fissionEu, double lithiumShare,
                        boolean overheated, boolean invalid) {}

    /** Fusion neutrons per slot for a share {@code f} (0-1) of the full neutron power. */
    public static int[] neutrons(double f) {
        int[] out = new int[SLOTS];
        double share = Math.max(0, Math.min(1, f));
        for (int i = 0; i < SLOTS; i++) out[i] = (int) Math.round(PEAK_NEUTRONS * share * Math.pow(ATTENUATION, i % COLUMNS));
        return out;
    }

    /** The output for a neutron power, a lithium share and the rods' fission output (fission-reactor EU/t). */
    public static long output(long neutronPower, double lithiumShare, double fissionEu) {
        if (neutronPower <= 0) return 0;
        double f = Math.min(1.0, (double) neutronPower / FULL_NEUTRONS);
        double eu = neutronPower * (1 + LITHIUM_GAIN * lithiumShare) + EU_PER_FISSION_EU * fissionEu * f;
        return (long) Math.min(FusionPlasma.OUTPUT_LIMIT, Math.max(0, Math.floor(eu)));   // 0.1.11.98: no cap
    }

    /**
     * Runs one cycle (20 ticks) on the grid. {@code neutronPower}: the plasma's neutron power (EU/t); 0 = no plasma (the rods
     * rest, the vents still cool). A grid the fission reactor cannot read (an item that is not a component, a stack of more than
     * one) leaves everything as it was ({@code invalid}).
     */
    public static Cycle cycle(ItemStack[] grid, long hull, long neutronPower) {
        if (grid.length != SLOTS) throw new IllegalArgumentException("a blanket has " + SLOTS + " slots");
        Part[] parts = new Part[SLOTS];
        try {
            for (int i = 0; i < SLOTS; i++) parts[i] = ReactorInventory.read(grid[i]);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return new Cycle(copy(grid), hull, 10_000, 0, 0, 0, false, true);
        }
        double f = Math.min(1.0, Math.max(0, (double) neutronPower / FULL_NEUTRONS));
        int[] ext = neutrons(f);
        // the share of the neutrons the lithium cells take (before they turn into tritium this cycle)
        long lithium = 0, total = 0;
        for (int i = 0; i < SLOTS; i++) {
            total += ext[i];
            if (parts[i] != null && parts[i].profile().kind() == Kind.LITHIUM && parts[i].stored() < parts[i].profile().capacity()) lithium += ext[i];
        }
        ReactorCycle.Result r = ReactorCycle.step(parts, COLUMNS, Math.max(0, hull), neutronPower > 0, ext, BREED_PER_NEUTRON);
        Part[] next = r.parts();
        boolean[] depleted = r.depletedFuel();
        ItemStack[] out = new ItemStack[SLOTS];
        for (int i = 0; i < SLOTS; i++) out[i] = ReactorInventory.write(grid[i], parts[i], next[i], depleted[i]);
        double lithiumShare = total > 0 ? (double) lithium / total : 0;
        long hullAfter = Math.min(r.hullHeat(), r.maxHullHeat());
        return new Cycle(out, hullAfter, r.maxHullHeat(), output(neutronPower, lithiumShare, r.euPerTick()), r.euPerTick(), lithiumShare,
            r.hullHeat() >= r.maxHullHeat(), false);
    }

    private static ItemStack[] copy(ItemStack[] grid) {
        ItemStack[] out = new ItemStack[grid.length];
        for (int i = 0; i < grid.length; i++) out[i] = grid[i].copy();
        return out;
    }
}
