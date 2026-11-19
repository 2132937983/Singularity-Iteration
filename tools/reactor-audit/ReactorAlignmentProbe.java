import com.miophas.singularity_iteration.core.runtime.reactor.ReactorCycle;
import com.miophas.singularity_iteration.core.runtime.reactor.ReactorCycle.Part;
import com.miophas.singularity_iteration.core.runtime.reactor.ReactorCycle.Profile;
import com.miophas.singularity_iteration.core.runtime.reactor.FluidReactorCycle;

/** Regression checks, not a replacement reactor implementation.
 * Reference expectations are hand-derived from the local IC2 2.8.221 decompilation;
 * see docs/reactor_ic2_alignment_audit.md for derivations and decompiler limitations.
 * A DIFFERENCE fails the run; expectations do not follow the implementation.
 */
public final class ReactorAlignmentProbe {
    private static int matches, differences;
    private static Part part(Profile profile, int heat) { return new Part(profile, heat, 0); }
    private static Part fuel(int cells, boolean mox) { return new Part(Profile.fuel(cells, mox), 0, 100); }
    private static ReactorCycle.Result run(Part[] layout, long hull, boolean fluid) {
        return ReactorCycle.step(layout, 9, hull, true, fluid);
    }
    private static void compare(String label, double actual, double reference) {
        boolean same = Math.abs(actual - reference) < 0.0001;
        if (same) matches++; else differences++;
        System.out.printf(java.util.Locale.ROOT, "%s | %s | SI=%.6f | IC2-reference=%.6f%n",
            same ? "MATCH" : "DIFFERENCE", label, actual, reference);
    }
    public static void main(String[] args) {
        for (int cells : new int[] {1, 2, 4}) {
            Part[] layout = new Part[54]; layout[0] = fuel(cells, false);
            var result = run(layout, 0, false);
            int pulses = 1 + cells / 2;
            compare("isolated uranium " + cells + " cells EU/t", result.euPerTick(), 5 * cells * pulses);
            compare("isolated uranium " + cells + " cells heat/cycle", result.generatedHeat(), 2 * cells * pulses * (pulses + 1));
            compare("uranium durability decremented once", result.parts()[0].remaining(), 99);
        }

        Part[] layout = new Part[54];
        layout[10] = fuel(1, false);
        layout[9] = part(Profile.cell(10000), 0); // left
        layout[1] = part(Profile.cell(10000), 0); // up
        layout[19] = part(Profile.cell(10000), 0); // down
        var result = run(layout, 0, false);
        // IC2 deque: left <- 4/3=1; up <- 3/2=1; down <- 2/1=2.
        compare("three acceptors: upper cell heat", result.parts()[1].stored(), 1);
        compare("three acceptors: lower cell heat", result.parts()[19].stored(), 2);

        layout = new Part[54]; layout[10] = fuel(1, false);
        layout[9] = part(Profile.cell(10000), 10000);
        layout[11] = part(Profile.cell(10000), 0);
        result = run(layout, 0, false);
        // Left gets 2, melts, returns -1. Right receives the remaining 1, not 2.
        compare("overflow remainder delivered to right cell", result.parts()[11].stored(), 1);

        layout = new Part[54]; layout[0] = fuel(1, true);
        result = run(layout, 5000, false);
        compare("MOX uses heat after thermal pass", result.euPerTick(), (1.0f + 4.0f * (5004.0f / 10000.0f)) * 5.0f);
        layout[53] = part(Profile.plating(2000, 1), 0);
        result = run(layout, 5000, false);
        compare("MOX uses final plated heat capacity", result.euPerTick(), (1.0f + 4.0f * (5004.0f / 12000.0f)) * 5.0f);
        result = run(layout, 5500, true);
        // MOX precedes plating: maxHeat is still 10000 when its thermal pass runs.
        compare("fluid MOX before plating doubles heat at 5500", result.generatedHeat(), 8);

        layout = new Part[54]; layout[0] = part(Profile.vent(1000, 20, 36), 990);
        result = run(layout, 100, true);
        // IC2 continues processChamber on the old stack after removing it from its slot.
        compare("overheated vent emits on destruction cycle", result.emittedHeat(), 20);

        layout = new Part[54]; layout[0] = fuel(2, false);
        layout[1] = part(Profile.lithium(10000), 9999);
        result = run(layout, 3000, false);
        // The first cell breeds lithium into a non-pulseable product: 24 + 12 = 36.
        compare("breeder replacement occurs between cell iterations", result.generatedHeat(), 36);

        layout = new Part[54]; layout[0] = part(Profile.exchanger(2500, 12, 4), 250);
        layout[1] = part(Profile.condensator(20000), 10000);
        result = run(layout, 0, false);
        // IC2 condensator inherits the public getCurrentHeat(...)=0 method.
        compare("exchanger sees IC2 condensator heat accessor", result.parts()[1].stored(), 10012);

        layout = new Part[54]; layout[0] = part(Profile.vent(1000, 20, 36), 100);
        result = ReactorCycle.step(layout, 9, 0, false, true);
        compare("cooling still runs with redstone off", result.emittedHeat(), 20);
        var conversion = FluidReactorCycle.convert(100, 400, 0, 10000);
        compare("fluid conversion for 100 emitted heat", conversion.millibuckets(), 200);
        compare("fluid conversion does not return absorbed heat", conversion.returnedHeat(), 0);
        conversion = FluidReactorCycle.convert(100, 0, 0, 10000);
        compare("missing coolant returns heat to hull", conversion.returnedHeat(), 100);
        layout = new Part[54]; layout[0] = fuel(1, false);
        layout[1] = part(Profile.vent(1000, 20, 36), 100);
        var blocked = FluidReactorCycle.step(layout, 9, 0, true, 400, 10, 10000, 20, false);
        compare("incompatible hot fluid: conversion blocked", blocked.converted(), 0);
        compare("incompatible hot fluid: input preserved", blocked.coolant(), 400);
        compare("incompatible hot fluid: output preserved", blocked.hotCoolant(), 10);
        compare("incompatible hot fluid: heat returned to hull", blocked.cycle().hullHeat(), 20);
        compare("incompatible hot fluid: fuel still consumed", blocked.cycle().parts()[0].remaining(), 99);
        var resumed = FluidReactorCycle.step(blocked.cycle().parts(), 9, blocked.cycle().hullHeat(), true, 400, 0, 10000, 20, true);
        compare("draining incompatible output resumes conversion", resumed.converted(), 40);
        compare("resumed cycle cools hull", resumed.cycle().hullHeat(), 0);
        layout = new Part[54]; layout[0] = part(Profile.plating(2000, 1), 0); layout[53] = fuel(1, true);
        compare("fluid MOX after plating uses increased capacity", run(layout, 5500, true).generatedHeat(), 4);
        layout = new Part[54]; layout[0] = fuel(1, true);
        compare("fluid MOX at exactly half heat is not doubled", run(layout, 5000, true).generatedHeat(), 4);
        layout = new Part[54]; layout[0] = part(Profile.exchanger(2500, 12, 4), 250);
        layout[1] = part(Profile.condensator(20000), 20000);
        compare("full condensator is excluded from exchange", run(layout, 0, false).parts()[0].stored(), 246);
        System.out.printf("TOTAL: %d matching observations; %d differing observations.%n", matches, differences);
        if (differences != 0) throw new AssertionError(differences + " reactor alignment regressions");
    }
}
