// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.reactor;

import com.miophas.singularity_iteration.common.Singularity_Iteration_Config;
import com.miophas.singularity_iteration.common.registry.mio_icif_data_components;
import com.miophas.singularity_iteration.core.api.reactor.ReactorLayout;
import com.miophas.singularity_iteration.core.runtime.reactor.ReactorBlastProfile;
import com.miophas.singularity_iteration.core.runtime.reactor.ReactorCycle;
import com.miophas.singularity_iteration.core.runtime.reactor.ReactorCycle.Profile;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.10.19: a {@link ReactorLayout} resolved for the JEI category "Reactor Layouts": the component in each
 * slot, and the numbers of the design (computed on the first {@link #result()}, then kept).
 *
 * <p>The numbers come from {@link ReactorSimulation#fullRun}: the model of the reactor block from a cold
 * start with new components, for a full uranium fuel load or until the steady state. The explosion uses
 * the reactor explosion settings of the local config ({@link ReactorBlastProfile}).
 */
public final class ReactorLayoutAnalysis {
    /** The risk of a design, from the safest. */
    public enum Verdict { STABLE, HULL_RISING, COMPONENTS_MELT, MELTDOWN }

    /**
     * The numbers of a design.
     *
     * @param report      the full run
     * @param blast       the explosion of the new design with the config settings
     * @param fuelCycles  cycles (seconds) of the shortest-lived fuel rod, 0 without fuel
     * @param verdict     the risk
     * @param riskSeconds seconds until the risk of the verdict happens (meltdown, first melt, extrapolated meltdown), or -1
     */
    public record Result(ReactorSimulation.Report report, ReactorBlastProfile blast, int fuelCycles, Verdict verdict, int riskSeconds) {
        /** EU of one fuel load (0 without fuel or for a design that is not stable). */
        public double euPerLoad() {
            return verdict == Verdict.STABLE ? report.euPerTick() * 20.0 * fuelCycles : 0;
        }
    }

    private static final String PREFIX = "reactor/item_reactor_";

    private final ReactorLayout layout;
    private final Profile[] profiles = new Profile[54];
    private final ResourceLocation[] items = new ResourceLocation[54];
    private final List<String> unknown = new ArrayList<>();
    private Result result;

    private ReactorLayoutAnalysis(ReactorLayout layout) {
        this.layout = layout;
        int columns = layout.columns();
        for (int row = 0; row < ReactorLayout.MAX_ROWS; row++) {
            for (int column = 0; column < columns; column++) {
                String cell = layout.cell(row, column);
                if (cell.equals(ReactorLayout.EMPTY)) continue;
                int slot = row * 9 + column;
                String key = keyOf(cell);
                Profile profile = key == null ? null : ReactorInventory.profile(key);
                if (profile == null) {
                    if (!unknown.contains(cell)) unknown.add(cell);
                    ResourceLocation id = cell.indexOf(':') >= 0 ? ResourceLocation.tryParse(cell) : key == null ? null : ReactorInventory.itemId(key);
                    if (id != null && BuiltInRegistries.ITEM.containsKey(id)) items[slot] = id;
                    continue;
                }
                profiles[slot] = profile;
                items[slot] = ReactorInventory.itemId(key);
            }
        }
    }

    /** Resolves a layout; the numbers are computed later. */
    public static ReactorLayoutAnalysis of(ReactorLayout layout) {
        return new ReactorLayoutAnalysis(layout);
    }

    /** The component key of a cell: a key, or the item id of an SI reactor component; null for another item. */
    @Nullable
    static String keyOf(String cell) {
        if (cell.indexOf(':') < 0) return cell;
        ResourceLocation id = ResourceLocation.tryParse(cell);
        if (id == null || !id.getNamespace().equals("mio_icif") || !id.getPath().startsWith(PREFIX)) return null;
        return id.getPath().substring(PREFIX.length());
    }

    public ReactorLayout layout() { return layout; }

    public int columns() { return layout.columns(); }

    /** The item of slot {@code slot} (row * 9 + column), or null for an empty slot or an unknown item. */
    @Nullable
    public ResourceLocation item(int slot) { return items[slot]; }

    /** The cells that are not SI reactor components (the design cannot be simulated then). */
    public List<String> unknown() { return List.copyOf(unknown); }

    public boolean simulable() { return unknown.isEmpty(); }

    /** The numbers (computed once; null when the design cannot be simulated). */
    @Nullable
    public synchronized Result result() {
        if (!simulable()) return null;
        if (result == null) result = compute();
        return result;
    }

    private Result compute() {
        int columns = columns();
        ReactorSimulation.Report report = ReactorSimulation.fullRun(profiles, columns);
        ReactorCycle.Part[] parts = new ReactorCycle.Part[54];
        int fuel = 0;
        for (int i = 0; i < 54; i++) {
            Profile p = profiles[i];
            if (p == null) continue;
            boolean rod = p.kind() == ReactorCycle.Kind.FUEL;
            parts[i] = new ReactorCycle.Part(p, 0, rod ? ReactorSimulation.FUEL : 0);
            if (rod) {
                int life = rodLife(items[i]);
                fuel = fuel == 0 ? life : Math.min(fuel, life);
            }
        }
        ReactorBlastProfile blast = ReactorBlastProfile.calculate(parts, explosionMultiplier(), explosionPowerLimit());
        Verdict verdict;
        int seconds;
        if (report.meltdownCycle() >= 0) { verdict = Verdict.MELTDOWN; seconds = report.meltdownCycle(); }
        else if (report.melted() > 0) { verdict = Verdict.COMPONENTS_MELT; seconds = report.firstMeltCycle(); }
        else if (report.hullTrend() > 0) { verdict = Verdict.HULL_RISING; seconds = report.secondsToMeltdown(); }
        else { verdict = Verdict.STABLE; seconds = -1; }
        return new Result(report, blast, fuel, verdict, seconds);
    }

    /** Cycles of a new rod of this item (20,000 uranium, 10,000 MOX); the simulation value when unknown. */
    private static int rodLife(@Nullable ResourceLocation item) {
        if (item == null) return ReactorSimulation.FUEL;
        var durability = BuiltInRegistries.ITEM.get(item).components().get(mio_icif_data_components.FUEL_ROD_DURABILITY.get());
        return durability == null || durability.maxUses() <= 0 ? ReactorSimulation.FUEL : durability.maxUses();
    }

    static double explosionMultiplier() {
        try {
            return Singularity_Iteration_Config.REACTOR_EXPLOSION_MULTIPLIER.get();
        } catch (IllegalStateException e) {
            return Singularity_Iteration_Config.REACTOR_EXPLOSION_MULTIPLIER.getDefault();
        }
    }

    static int explosionPowerLimit() {
        try {
            return Singularity_Iteration_Config.REACTOR_EXPLOSION_POWER_LIMIT.get();
        } catch (IllegalStateException e) {
            return Singularity_Iteration_Config.REACTOR_EXPLOSION_POWER_LIMIT.getDefault();
        }
    }
}
