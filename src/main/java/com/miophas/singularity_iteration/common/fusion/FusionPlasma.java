package com.miophas.singularity_iteration.common.fusion;

import net.minecraft.nbt.CompoundTag;

/**
 * 0.1.11.92: the plasma and the magnets of a fusion reactor (tokamak), as a model without world access: the controller gives it
 * what the structure offers each tick ({@link Inputs}) and gets back what happened ({@link Result}).
 *
 * <p>The numbers follow a real tokamak, scaled so that the best layout gives {@link #OUTPUT_CAP}:
 * <ul>
 *   <li>Fusion power: {@link #FUSION_PER_FEED} EU/t per feed step (1-16), times the reactivity at the plasma temperature
 *       (none below 4 keV, full from 14 keV). D-T fusion gives 20% of its energy to alpha particles, which stay in the plasma
 *       and heat it, and 80% to neutrons, which go into the blanket ({@code FusionBlanket} multiplies them).</li>
 *   <li>The plasma loses heat in proportion to its temperature; with the alpha heating alone it settles at 15 keV (about
 *       170 million K). Below that it needs the heating ports: up to fusion power / 10 (Q = 10, as ITER), 245,000 EU/t per port.</li>
 *   <li>The superconducting magnets cost {@link #MAGNET_UPKEEP} EU/t (1% of the cap) while the reactor runs, cool down in
 *       {@link #COOLDOWN_TICKS} ticks of paid upkeep, and quench when the upkeep is not paid.</li>
 *   <li>Start-up: the central solenoid takes {@link #STARTUP_ENERGY} EU, at up to {@link #STARTUP_RATE} EU/t (30 s at full rate).
 *       Its flux carries a burn of {@link #PULSE_TICKS} ticks (ITER: 300-500 s); two electron cyclotron heaters drive the plasma
 *       current without it ({@link #CURRENT_DRIVE_SHARE} of the fusion power each), and the burn is steady.</li>
 *   <li>A disruption (the plasma lost in an instant) follows a quench or a broken structure while there is plasma, helium ash
 *       left in the plasma without an exhaust port ({@link #ASH_LIMIT} ticks), or an overheated blanket. A redstone stop, the
 *       fuel running out or the end of a pulse end the plasma gently.</li>
 * </ul>
 */
public final class FusionPlasma {
    /** 2 x the Mekanism fusion reactor's air-cooled maximum (19,600,000 FE/t at injection rate 98). */
    public static final long OUTPUT_CAP = 39_200_000L;
    public static final int MIN_FEED = 1, MAX_FEED = 16;
    public static final long FUSION_PER_FEED = 612_500L;              // 16 steps: 9,800,000 EU/t of fusion power
    public static final double ALPHA_SHARE = 0.2, NEUTRON_SHARE = 0.8;
    public static final double ONSET_KEV = 4.0, IGNITION_KEV = 14.0, BURN_KEV = 15.0, START_KEV = 1.0;
    /** Heat lost per keV and per feed step: the alpha heating at full reactivity balances it at {@link #BURN_KEV}. */
    public static final double LOSS_PER_KEV = ALPHA_SHARE * FUSION_PER_FEED / BURN_KEV;
    /** EU to raise the plasma one keV, per feed step. */
    public static final double HEAT_CAPACITY = 400_000.0;
    public static final long MAGNET_UPKEEP = OUTPUT_CAP / 100;        // 392,000 EU/t
    public static final int COOLDOWN_TICKS = 600;
    public static final long STARTUP_ENERGY = 300_000_000L, STARTUP_RATE = 10_000_000L;
    public static final long HEATING_PER_PORT = 245_000L;             // 4 ports: 980,000 EU/t = fusion power at 16 steps / 10
    public static final int PULSE_TICKS = 8_000;
    public static final double CURRENT_DRIVE_SHARE = 0.0125;          // per heater; two needed
    public static final int ASH_LIMIT = 1_200;
    public static final int DISRUPTION_TICKS = 200, QUENCH_TICKS = 1_200;

    public enum State { OFF, COOLING, CHARGING, HEATING, BURNING, DISRUPTED, QUENCHED }

    public enum Event { NONE, PLASMA_START, IGNITION, PULSE_END, DISRUPTION, QUENCH }

    /**
     * What the structure offers the plasma this tick. {@code energy}: EU the controller can spend; {@code deuterium},
     * {@code tritium}: fuel units in its tanks; {@code heaters}: heating ports (neutral beam and electron cyclotron),
     * {@code cyclotronHeaters}: the electron cyclotron ones (current drive).
     */
    public record Inputs(boolean formed, boolean run, int feed, int heaters, int cyclotronHeaters, long energy, long deuterium,
                         long tritium, boolean exhaust, boolean blanketOverheated) {}

    /** What happened: EU spent (upkeep, start-up, heating, current drive), fusion power (EU/t), fuel units used of each. */
    public record Result(long energyUsed, long heating, long fusionPower, long fuelUsed, Event event) {
        public long neutronPower() { return Math.round(fusionPower * NEUTRON_SHARE); }
    }

    private State state = State.OFF;
    private double kev;
    private int cooled, burnTicks, ash, timer;
    private long charged, lastHeating, lastFusion;

    public State state() { return state; }
    public double kev() { return kev; }
    public int cooled() { return cooled; }
    public long charged() { return charged; }
    public int burnTicks() { return burnTicks; }
    public int ash() { return ash; }
    public int timer() { return timer; }
    public long lastHeating() { return lastHeating; }
    public long lastFusion() { return lastFusion; }
    public boolean hasPlasma() { return state == State.HEATING || state == State.BURNING; }

    /** Fusion power over heating power (infinite once ignited and the heating is off). */
    public double q() {
        return lastHeating > 0 ? (double) lastFusion / lastHeating : lastFusion > 0 ? Double.POSITIVE_INFINITY : 0;
    }

    /** The share of the full fusion rate at a temperature: none below {@link #ONSET_KEV}, all from {@link #IGNITION_KEV}. */
    public static double reactivity(double kev) {
        return Math.max(0, Math.min(1, (kev - ONSET_KEV) / (IGNITION_KEV - ONSET_KEV)));
    }

    public static long fusionPower(int feed, double kev) {
        return Math.round(clampFeed(feed) * FUSION_PER_FEED * reactivity(kev));
    }

    public static int clampFeed(int feed) {
        return Math.max(MIN_FEED, Math.min(MAX_FEED, feed));
    }

    /** One tick. */
    public Result tick(Inputs in) {
        int feed = clampFeed(in.feed());
        long budget = Math.max(0, in.energy());
        long used = 0;
        lastHeating = 0;
        lastFusion = 0;
        Event event = Event.NONE;

        // the timers of a lost plasma and of warm magnets run whatever the reactor does
        if (state == State.DISRUPTED || state == State.QUENCHED) {
            if (--timer > 0) return new Result(0, 0, 0, 0, Event.NONE);
            state = state == State.QUENCHED ? State.COOLING : State.CHARGING;
            if (state == State.COOLING) cooled = 0;
        }
        if (!in.formed() && hasPlasma()) return disrupt(Event.DISRUPTION, false);
        if (!in.formed() || !in.run()) {
            if (hasPlasma()) endPlasma();
            state = State.OFF;
            cooled = Math.max(0, cooled - 1);   // the magnets warm up slowly without upkeep
            return new Result(0, 0, 0, 0, Event.NONE);
        }
        if (state == State.OFF) state = cooled >= COOLDOWN_TICKS ? State.CHARGING : State.COOLING;

        // the magnets first: without their upkeep they quench
        if (budget < MAGNET_UPKEEP) {
            if (state == State.COOLING) {
                cooled = Math.max(0, cooled - 2);
                return new Result(0, 0, 0, 0, Event.NONE);
            }
            return disrupt(hasPlasma() ? Event.DISRUPTION : Event.QUENCH, true);
        }
        budget -= MAGNET_UPKEEP;
        used += MAGNET_UPKEEP;

        switch (state) {
            case COOLING -> {
                if (++cooled >= COOLDOWN_TICKS) state = State.CHARGING;
            }
            case CHARGING -> {
                long take = Math.min(Math.min(STARTUP_RATE, budget), STARTUP_ENERGY - charged);
                charged += take;
                used += take;
                if (charged >= STARTUP_ENERGY && in.deuterium() > 0 && in.tritium() > 0) {
                    state = State.HEATING;
                    kev = START_KEV;
                    burnTicks = 0;
                    ash = 0;
                    event = Event.PLASMA_START;
                }
            }
            case HEATING, BURNING -> {
                if (in.deuterium() <= 0 || in.tritium() <= 0) {
                    endPlasma();
                    return new Result(used, 0, 0, 0, Event.PULSE_END);
                }
                if (in.blanketOverheated()) return disrupt(Event.DISRUPTION, false, used);
                long heating = 0;
                if (state == State.HEATING) {
                    heating = Math.min(Math.min((long) in.heaters() * HEATING_PER_PORT, feed * FUSION_PER_FEED / 10), budget);
                    budget -= heating;
                    used += heating;
                }
                long fusion = fusionPower(feed, kev);
                // current drive: two electron cyclotron heaters keep the plasma current without the solenoid
                long drive = in.cyclotronHeaters() >= 2 ? Math.round(2 * CURRENT_DRIVE_SHARE * fusion) : 0;
                boolean steady = drive > 0 && drive <= budget;
                if (steady) {
                    budget -= drive;
                    used += drive;
                }
                double alpha = ALPHA_SHARE * fusion;
                double loss = LOSS_PER_KEV * feed * kev;
                kev = Math.max(0, kev + (heating + alpha - loss) / (HEAT_CAPACITY * feed));
                if (state == State.HEATING && kev >= IGNITION_KEV) {
                    state = State.BURNING;
                    event = Event.IGNITION;
                }
                long fuel = fusion > 0 ? Math.max(1, Math.round(feed * reactivity(kev))) : 0;
                fuel = Math.min(fuel, Math.min(in.deuterium(), in.tritium()));
                lastHeating = heating;
                lastFusion = fusion;
                if (state == State.BURNING) {
                    ash = in.exhaust() ? 0 : ash + 1;
                    if (ash >= ASH_LIMIT) return disrupt(Event.DISRUPTION, false, used);
                    if (!steady && ++burnTicks >= PULSE_TICKS) {
                        endPlasma();
                        return new Result(used, heating, fusion, fuel, Event.PULSE_END);
                    }
                }
                return new Result(used, heating, fusion, fuel, event);
            }
            default -> { }
        }
        return new Result(used, 0, 0, 0, event);
    }

    /** The plasma ends gently: the solenoid has to be charged again for the next one. */
    private void endPlasma() {
        state = State.CHARGING;
        kev = 0;
        charged = 0;
        burnTicks = 0;
        ash = 0;
    }

    private Result disrupt(Event event, boolean quench) {
        return disrupt(event, quench, 0);
    }

    private Result disrupt(Event event, boolean quench, long used) {
        boolean plasma = hasPlasma();
        kev = 0;
        charged = 0;
        burnTicks = 0;
        ash = 0;
        if (quench) {
            state = State.QUENCHED;
            timer = QUENCH_TICKS;
            cooled = 0;
        } else {
            state = State.DISRUPTED;
            timer = DISRUPTION_TICKS;
        }
        return new Result(used, 0, 0, 0, plasma ? Event.DISRUPTION : event);
    }

    /** The plasma's thermal energy (EU) at a feed step: what a disruption throws onto the first wall. */
    public double thermalEnergy(int feed) {
        return HEAT_CAPACITY * clampFeed(feed) * kev;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("State", state.name());
        tag.putDouble("Kev", kev);
        tag.putInt("Cooled", cooled);
        tag.putInt("Burn", burnTicks);
        tag.putInt("Ash", ash);
        tag.putInt("Timer", timer);
        tag.putLong("Charged", charged);
        return tag;
    }

    public void load(CompoundTag tag) {
        try {
            state = State.valueOf(tag.getString("State"));
        } catch (IllegalArgumentException e) {
            state = State.OFF;
        }
        kev = Math.max(0, tag.getDouble("Kev"));
        cooled = Math.max(0, Math.min(COOLDOWN_TICKS, tag.getInt("Cooled")));
        burnTicks = Math.max(0, tag.getInt("Burn"));
        ash = Math.max(0, tag.getInt("Ash"));
        timer = Math.max(0, tag.getInt("Timer"));
        charged = Math.max(0, Math.min(STARTUP_ENERGY, tag.getLong("Charged")));
    }
}
