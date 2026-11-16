// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.energy;

import java.util.function.DoubleUnaryOperator;
import java.util.function.IntUnaryOperator;
import net.minecraft.nbt.CompoundTag;

/**
 * 0.1.11.15: the engine of the semifluid, advanced semifluid and diesel generators (a generator set), shared by the
 * machines, their screen and the tests.
 *
 * <ul>
 *   <li><b>Load following.</b> The governor measures what is taken from the storage and sets the load to it, plus a
 *       top-up while the storage is below 90 %; the load follows with a lag (about 1 s). Running, it never drops below
 *       10 % (idle).</li>
 *   <li><b>Stop and start.</b> It stops when the storage is full (98 %) and starts again below 60 %, so a full storage no
 *       longer burns fuel.</li>
 *   <li><b>Part-load efficiency.</b> The fuel used per EU is lowest near 80 % load (5 % better than full load) and rises
 *       at part load (friction and pumping are a larger share): at full load the fuel value is the machine's stock one.</li>
 *   <li><b>Warm-up.</b> The coolant warms from 20 °C to about 85 °C with the load; a cold engine has 80 % of its
 *       efficiency, a warm one (80 °C) all of it.</li>
 *   <li><b>Fuel.</b> Fuel is taken from the tank by the millibucket into an energy credit, so no part of a millibucket is
 *       lost.</li>
 * </ul>
 */
public final class EngineGovernor {
    public static final double START_BELOW = 0.6, STOP_AT = 0.98, IDLE = 0.1, LAG = 0.05;
    public static final int SAMPLE = 10, SAMPLES = 96, DATA_COUNT = 16 + SAMPLES / 2;
    /** States for the screen. */
    public static final int STOPPED_FULL = 0, STOPPED_FUEL = 1, WARMING = 2, LOW_LOAD = 3, RUNNING = 4;

    public final int cylinders;
    private double load, rpm, coolant = 20, credit, demand, fuelFlow, eff = 1, lastOutput;
    private boolean running = true;
    private long lastStored = -1;
    private int state = STOPPED_FUEL;
    private final byte[] histLoad = new byte[SAMPLES], histEgt = new byte[SAMPLES];
    private int histHead, histTick;

    public EngineGovernor(int cylinders) {
        this.cylinders = cylinders;
    }

    /** Relative efficiency by load (1 at full load, 1.05 at 80 %, lower below). */
    public static double loadEfficiency(double load) {
        if (load <= 0) return 0;
        if (load < 0.8) return 1.05 - 0.6 * Math.pow((0.8 - load) / 0.8, 1.6);
        return 1.05 - 0.05 * Math.pow((load - 0.8) / 0.2, 2);
    }

    /** Efficiency factor of the coolant temperature (0.8 cold .. 1 at 80 °C). */
    public static double warmth(double coolant) {
        return 0.8 + 0.2 * Math.max(0, Math.min(1, (coolant - 20) / 60.0));
    }

    /** Exhaust gas temperature (°C) of a load and coolant temperature, for the screen. */
    public static double exhaust(double load, double coolant, boolean running) {
        return running ? 120 + 380 * load * Math.min(1, coolant / 85.0) : 20 + (coolant - 20) * 0.6;
    }

    /**
     * One tick.
     *
     * @param stored   EU in the storage now
     * @param capacity the storage's capacity
     * @param rated    full output in EU/t (the output multiplier included); 0 without fuel
     * @param euPerMb  EU per mB of the fuel in the tank at full load (multiplier included); 0 without fuel
     * @param drain    takes up to n mB from the tank, returns what it took
     * @param produce  writes EU into the storage, returns what it stored
     * @return the EU made this tick
     */
    public double tick(long stored, long capacity, double rated, double euPerMb, IntUnaryOperator drain, DoubleUnaryOperator produce) {
        double fill = capacity > 0 ? stored / (double) capacity : 1;
        if (lastStored >= 0) demand += (Math.max(0, lastStored - stored) - demand) * 0.1;
        boolean fuel = rated > 0 && euPerMb > 0;
        if (fill >= STOP_AT) running = false;
        else if (fill < START_BELOW) running = true;
        boolean firing = running && fuel;
        double target = firing ? Math.max(IDLE, Math.min(1, (demand + Math.max(0, 0.9 - fill) * rated * 0.5) / rated)) : 0;
        load += (target - load) * LAG;
        if (!firing && load < 0.005) load = 0;
        rpm += ((firing ? 900 + 900 * load : 0) - rpm) * 0.04;
        coolant += firing ? (85 - coolant) * (0.002 + 0.004 * load) : (20 - coolant) * 0.001;
        eff = loadEfficiency(load) * warmth(coolant);
        double made = 0, taken = 0;
        if (firing && load > 0) {
            double want = rated * load;
            double perMb = euPerMb * eff;
            if (credit < want && perMb > 0) {
                int n = (int) Math.ceil((want - credit) / perMb);
                taken = drain.applyAsInt(n);
                credit += taken * perMb;
            }
            double room = Math.max(0, capacity - stored);
            double give = Math.min(want, Math.min(credit, room));
            if (give > 0) {
                made = produce.applyAsDouble(give);
                credit -= made;
            }
        }
        fuelFlow += (taken - fuelFlow) * 0.05;
        lastOutput = made;
        lastStored = stored + Math.round(made);
        state = !running ? (fill >= START_BELOW ? STOPPED_FULL : STOPPED_FUEL) : !fuel ? STOPPED_FUEL
            : coolant < 70 ? WARMING : load < 0.4 ? LOW_LOAD : RUNNING;
        if (++histTick >= SAMPLE) {
            histTick = 0;
            histLoad[histHead] = ink(load);
            histEgt[histHead] = ink(exhaust(load, coolant, firing) / 600.0);
            histHead = (histHead + 1) % SAMPLES;
        }
        return made;
    }

    private static byte ink(double v) {
        return (byte) (1 + (int) Math.round(Math.max(0, Math.min(1, v)) * 254));
    }

    public double load() { return load; }
    public double rpm() { return rpm; }
    public double coolant() { return coolant; }
    public double efficiency() { return eff; }
    public double fuelFlow() { return fuelFlow; }
    public double lastOutput() { return lastOutput; }
    public boolean running() { return running; }
    public int state() { return state; }

    /**
     * Menu data 0..15 and the record from 16: 0 state, 1 load x1000, 2 rpm, 3 coolant °C x10, 4 output EU/t x100,
     * 5 rated EU/t x100, 6 fuel mB/t x1000, 7 efficiency x1000, 8 EU per mB now x10, 9..11 filled by the machine (tank
     * amount, capacity, fluid id), 12 energy, 13 capacity, 14 cylinders, 15 record head; 16.. two samples an int (load,
     * exhaust, load, exhaust bytes), oldest first from the head.
     */
    public int data(int i, double rated, double euPerMb, long energy, long capacity) {
        return switch (i) {
            case 0 -> state;
            case 1 -> (int) Math.round(load * 1000);
            case 2 -> (int) Math.round(rpm);
            case 3 -> (int) Math.round(coolant * 10);
            case 4 -> (int) Math.round(lastOutput * 100);
            case 5 -> (int) Math.round(rated * 100);
            case 6 -> (int) Math.round(fuelFlow * 1000);
            case 7 -> (int) Math.round(eff * 1000);
            case 8 -> (int) Math.round(euPerMb * eff * 10);
            case 12 -> (int) Math.min(Integer.MAX_VALUE, energy);
            case 13 -> (int) Math.min(Integer.MAX_VALUE, capacity);
            case 14 -> cylinders;
            case 15 -> histHead;
            default -> {
                int k = (i - 16) * 2;
                if (k < 0 || k + 1 >= SAMPLES) yield 0;
                yield (histLoad[k] & 0xFF) | (histEgt[k] & 0xFF) << 8 | (histLoad[k + 1] & 0xFF) << 16 | (histEgt[k + 1] & 0xFF) << 24;
            }
        };
    }

    public void save(CompoundTag tag) {
        CompoundTag t = new CompoundTag();
        t.putDouble("Load", load);
        t.putDouble("Rpm", rpm);
        t.putDouble("Coolant", coolant);
        t.putDouble("Credit", credit);
        t.putDouble("Demand", demand);
        t.putBoolean("Running", running);
        t.putByteArray("HistLoad", histLoad);
        t.putByteArray("HistEgt", histEgt);
        t.putInt("HistHead", histHead);
        tag.put("Engine", t);
    }

    /** Saves before 0.1.11.15 have no engine: it starts cold with the old fuel credit. */
    public void load(CompoundTag tag, double legacyCredit) {
        if (!tag.contains("Engine")) {
            credit = Math.max(0, legacyCredit);
            return;
        }
        CompoundTag t = tag.getCompound("Engine");
        load = t.getDouble("Load");
        rpm = t.getDouble("Rpm");
        coolant = t.contains("Coolant") ? t.getDouble("Coolant") : 20;
        credit = Math.max(0, t.getDouble("Credit"));
        demand = t.getDouble("Demand");
        running = t.getBoolean("Running");
        byte[] l = t.getByteArray("HistLoad"), e = t.getByteArray("HistEgt");
        if (l.length == SAMPLES && e.length == SAMPLES) {
            System.arraycopy(l, 0, histLoad, 0, SAMPLES);
            System.arraycopy(e, 0, histEgt, 0, SAMPLES);
        }
        histHead = Math.floorMod(t.getInt("HistHead"), SAMPLES);
        lastStored = -1;
    }
}
