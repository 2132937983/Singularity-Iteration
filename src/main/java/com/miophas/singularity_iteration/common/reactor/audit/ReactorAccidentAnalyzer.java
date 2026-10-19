// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.reactor.audit;

import com.miophas.singularity_iteration.common.reactor.audit.ReactorBlackbox.Melt;
import com.miophas.singularity_iteration.common.reactor.audit.ReactorBlackbox.Sample;
import com.miophas.singularity_iteration.core.runtime.reactor.ReactorCycle.Kind;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;

/**
 * 0.1.10.12: reads a reactor blackbox and names the causes of an accident. Pure logic (no world access),
 * deterministic. The first finding is the main cause; the others are contributing factors.
 *
 * <p>Codes (lang key {@code reactor_audit.cause.<code>} with a title and a {@code .desc} text, args in order):
 * <ul>
 *   <li>{@code removed} (hull %): the reactor was removed while running or hot (no explosion).</li>
 *   <li>{@code chunk_boundary} (unloaded faces, columns before, after): a chamber's chunk was not loaded,
 *       the reactor lost columns and dropped the components in them.</li>
 *   <li>{@code chamber_lost} (columns before, after, dropped): a chamber was removed or destroyed.</li>
 *   <li>{@code grid_blast} (seconds before, x, y, z): an overvoltage explosion of a machine next to the reactor.</li>
 *   <li>{@code coolant_hot_full} (hot mB, capacity): fluid reactor, the hot coolant was not taken out.</li>
 *   <li>{@code coolant_empty} (cold mB): fluid reactor without coolant.</li>
 *   <li>{@code liquid_cooling_lost} (HU per cycle before, "empty" or "hot_full"): the liquid cooling stopped.</li>
 *   <li>{@code sudden} (hull % before, after): the hull went from below half to the limit in one cycle.</li>
 *   <li>{@code component_melted} (count, kinds "cell:1,vent:2", seconds before the first melt): cooling parts melted.</li>
 *   <li>{@code heat_runaway} (generated, removed HU per cycle, seconds of rise): not enough cooling.</li>
 *   <li>{@code fuel_added} (rods before, after).</li>
 *   <li>{@code feed_rate} (feed rate, extra HU per cycle).</li>
 *   <li>{@code automation} (items in, out, seconds).</li>
 *   <li>{@code component_saturation} (percent): the cooling parts were nearly full of heat.</li>
 *   <li>{@code unknown}: none of the above.</li>
 * </ul>
 */
public final class ReactorAccidentAnalyzer {
    private ReactorAccidentAnalyzer() {}

    /** One finding: the cause code and its arguments (already formatted numbers or codes). */
    public record Finding(String code, List<String> args) {
        public Finding(String code, Object... args) {
            this(code, java.util.Arrays.stream(args).map(String::valueOf).toList());
        }
    }

    /** An overvoltage explosion near the reactor: position and game time. */
    public record Blast(int x, int y, int z, long gameTime) { }

    /**
     * @param samples the cycles, oldest first (may be empty)
     * @param melts the components that melted in the window
     * @param eventTime game time of the accident or removal
     * @param removed true for a removal (not an explosion)
     * @param blasts overvoltage explosions within 16 blocks in the window
     * @return the findings, main cause first; never empty
     */
    public static List<Finding> analyze(List<Sample> samples, List<Melt> melts, long eventTime, boolean removed, List<Blast> blasts) {
        var found = new ArrayList<Finding>();
        int n = samples.size();
        Sample last = n == 0 ? null : samples.get(n - 1);
        if (removed) found.add(new Finding("removed", last == null ? 0 : percent(last.hullAfter(), last.maxHull())));
        if (n == 0) {
            if (!removed) found.add(new Finding("unknown"));
            return found;
        }
        Sample first = samples.get(0);
        long windowStart = first.gameTime() - 20;

        // chambers: columns going down between two cycles
        int lostFrom = -1, lostTo = -1, unloaded = 0, dropped = 0;
        for (int i = 1; i < n; i++) {
            var a = samples.get(i - 1);
            var b = samples.get(i);
            if (b.columns() < a.columns()) {
                if (lostFrom < 0) lostFrom = a.columns();
                lostTo = b.columns();
                unloaded = Math.max(unloaded, b.unloadedNeighbours());
            }
        }
        for (var s : samples) dropped += s.dropped();
        if (lostFrom >= 0) {
            if (unloaded > 0) found.add(new Finding("chunk_boundary", unloaded, lostFrom, lostTo));
            found.add(new Finding("chamber_lost", lostFrom, lostTo, dropped));
        }
        for (var blast : blasts) {
            if (blast.gameTime() < windowStart) continue;
            found.add(new Finding("grid_blast", seconds(eventTime - blast.gameTime()), blast.x(), blast.y(), blast.z()));
            break;
        }

        // coolant of a fluid reactor, and the liquid cooling of a standard reactor
        if (last.fluidMode()) {
            boolean hotFull = last.fluidCapacity() > 0 && last.hotOut() >= last.fluidCapacity() && last.converted() == 0 && last.emitted() > 0;
            boolean empty = last.coldIn() == 0 && last.converted() == 0 && last.emitted() > 0;
            if (hotFull) found.add(new Finding("coolant_hot_full", last.hotOut(), last.fluidCapacity()));
            else if (empty) found.add(new Finding("coolant_empty", last.coldIn()));
        } else {
            long before = 0;
            for (int i = 0; i < n - 1; i++) before = Math.max(before, samples.get(i).coolUsed());
            if (before > 0 && last.coolUsed() == 0 && last.coolBudget() == 0)
                found.add(new Finding("liquid_cooling_lost", before, last.coolCold() == 0 ? "empty" : "hot_full"));
        }

        // a jump within one cycle
        if (n >= 2) {
            var prev = samples.get(n - 2);
            int before = percent(prev.hullAfter(), prev.maxHull()), after = percent(last.hullAfter(), last.maxHull());
            if (before < 50 && after >= 100) found.add(new Finding("sudden", before, after));
        }

        // melted parts against a slow build-up
        int rising = 0;
        for (int i = n - 1; i >= 0 && samples.get(i).hullAfter() > samples.get(i).hullBefore(); i--) rising++;
        Finding runaway = null;
        if (rising >= 3) {
            long generated = 0, removedHeat = 0;
            int from = n - rising;
            for (int i = from; i < n; i++) {
                var s = samples.get(i);
                long heatIn = s.generated() + s.excess();
                long componentDelta = i > 0 ? s.componentHeat() - samples.get(i - 1).componentHeat() : 0;
                generated += heatIn;
                removedHeat += Math.max(0, heatIn - (s.hullAfter() - s.hullBefore()) - componentDelta);
            }
            runaway = new Finding("heat_runaway", generated / rising, removedHeat / rising, rising);
        }
        Finding melted = null;
        var cooling = melts.stream().filter(m -> m.component().kind() != Kind.REFLECTOR.ordinal()).toList();
        if (!cooling.isEmpty()) {
            var kinds = new TreeMap<String, Integer>();
            long firstMelt = Long.MAX_VALUE;
            for (var m : cooling) {
                kinds.merge(kindName(m.component().kind()), 1, Integer::sum);
                firstMelt = Math.min(firstMelt, m.gameTime());
            }
            var summary = new StringBuilder();
            kinds.forEach((k, v) -> summary.append(summary.length() == 0 ? "" : ",").append(k).append(':').append(v));
            melted = new Finding("component_melted", cooling.size(), summary.toString(), seconds(eventTime - firstMelt));
        }
        // a long rise before the first melt means too little cooling; melting with a calm hull means the parts failed
        if (runaway != null && (melted == null || rising >= 5)) { found.add(runaway); if (melted != null) found.add(melted); }
        else { if (melted != null) found.add(melted); if (runaway != null) found.add(runaway); }

        if (last.fuelRods() > first.fuelRods()) found.add(new Finding("fuel_added", first.fuelRods(), last.fuelRods()));
        if (last.feeds() > 1 && last.excess() > 0) found.add(new Finding("feed_rate", last.feeds(), last.excess()));
        int in = 0, out = 0;
        for (var s : samples) { in += s.automationIn(); out += s.automationOut(); }
        if (in + out > 0) found.add(new Finding("automation", in, out, seconds(eventTime - windowStart)));
        if (last.componentCapacity() > 0) {
            int saturation = percent(last.componentHeat(), last.componentCapacity());
            if (saturation >= 90) found.add(new Finding("component_saturation", saturation));
        }
        if (found.isEmpty()) found.add(new Finding("unknown"));
        return found;
    }

    static int percent(long value, long capacity) {
        return capacity <= 0 ? 0 : (int) Math.min(999, Math.round(value * 100.0 / capacity));
    }

    static String seconds(long ticks) {
        return String.format(Locale.ROOT, "%.0f", Math.max(0, ticks) / 20.0);
    }

    /** The lower-case name of a component kind (lang key {@code reactor_audit.kind.<name>}). */
    public static String kindName(int ordinal) {
        var kinds = Kind.values();
        return ordinal >= 0 && ordinal < kinds.length ? kinds[ordinal].name().toLowerCase(Locale.ROOT) : "inert";
    }
}
