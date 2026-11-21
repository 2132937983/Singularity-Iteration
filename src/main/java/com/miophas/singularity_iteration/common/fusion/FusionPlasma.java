package com.miophas.singularity_iteration.common.fusion;

import net.minecraft.nbt.CompoundTag;

/**
 * 0.1.11.92: the plasma and the magnets of a fusion reactor (tokamak), as a model without world access: the controller gives it
 * what the structure offers each tick ({@link Inputs}) and gets back what happened ({@link Result}).
 *
 * <p>0.1.11.133 (fusion overhaul F1): the plasma is a zero-dimensional power balance of a reference machine the size of ITER
 * ({@link #VOLUME} m³), in SI units, scaled to EU by one factor ({@link #EU_PER_WATT}) so that the reference burn gives
 * {@link #FULL_FUSION} EU/t:
 * <ul>
 *   <li>The feed step sets the electron density: step k aims at k/16 of {@link #REFERENCE_DENSITY}, reached with a fuelling
 *       time of {@link #FUELLING_SECONDS}; the plasma's thermal energy W = 3nTV stays as the density moves.</li>
 *   <li>Fusion power P = (n/2)² ⟨σv⟩(T) × 17.6 MeV × V, with the Bosch-Hale fit of the D-T reactivity ⟨σv⟩ (so it grows
 *       with the square of the density, and about with the square of the temperature near 10-20 keV). 20% stays in the
 *       plasma as alpha heating, 80% goes to the blanket as neutrons.</li>
 *   <li>Losses: conduction W/τE, with an H-mode confinement time that scales like ITER's IPB98(y,2) law in the density
 *       (n^0.41) and the heating power (P^-0.69, at most {@link #TAU_CAP} times the reference); and bremsstrahlung
 *       5.35e-37 n² Z_eff √T W/m³.</li>
 *   <li>Calibration: with no outside heating the reference density burns in balance at {@link #REFERENCE_KEV} keV and gives
 *       exactly {@link #FULL_FUSION} EU/t; the confinement time there is {@link #referenceTauE()} (about 2 s).</li>
 *   <li>Ignition: once the plasma is at least {@link #IGNITION_MIN_KEV} keV and the alpha heating alone covers its losses,
 *       the heating ports switch off (Q infinite). Lower densities cannot ignite (the Lawson criterion: the triple product
 *       n T τE stays short of about 3e21 keV s/m³); they burn driven, with the heating on and a finite Q. An ignited plasma
 *       that cools below {@link #REIGNITE_KEV} keV (a lower feed step) is heated again; one that falls below
 *       {@link #COLLAPSE_KEV} keV with no heating ends.</li>
 *   <li>Fuel: one unit of deuterium and one of tritium per tick for each 1/16 of the full fusion rate.</li>
 *   <li>0.1.11.134 (F2), the operating limits:
 *     <ul>
 *       <li>L-mode and H-mode: the confinement above is H-mode's. The plasma enters H-mode when the power through its edge
 *           (alpha + outside heating - bremsstrahlung) reaches the Martin 2008 threshold P_LH = 0.0488 n^0.717 B^0.803
 *           S^0.941 MW (about 87 MW at 10²⁰ m⁻³) and leaves it below {@link #LH_HYSTERESIS} of it; in L-mode the
 *           confinement time is halved ({@link #L_MODE_FACTOR}). One heating port (40 MW) cannot bring a dense plasma into
 *           H-mode.</li>
 *       <li>ELMs: in H-mode the edge relaxes every {@link #ELM_BASE_SECONDS} s at the reference power (more often with more
 *           power, 1-6 s), throwing {@link #ELM_SHARE} of the thermal energy onto the divertor; the conduction between them
 *           is lower by as much, so the average balance is unchanged.</li>
 *       <li>The feed goes up to {@link #MAX_FEED} steps, past the reference {@link #REFERENCE_FEED}. The Greenwald density
 *           n_G = I_p / (π a²) (1.19 × 10²⁰ m⁻³) and the Troyon limit β_N = β a B / I_p ≤ {@link #BETA_N_LIMIT}: from
 *           {@link #RISK_FROM} of either, the plasma may disrupt ({@link #hazard} per second, a few percent at the limit,
 *           certain within seconds past it). The reference burn is at 0.84 of the Greenwald density and 0.87 of the beta
 *           limit: safe.</li>
 *     </ul></li>
 *   <li>The superconducting magnets: the upkeep {@link #MAGNET_UPKEEP} EU/t is the cryogenic plant's load (1% of
 *       {@link #FULL_POWER}); they cool in {@link #COOLDOWN_TICKS} ticks of paid upkeep and quench when it is not paid.</li>
 *   <li>Start-up: the central solenoid takes {@link #STARTUP_ENERGY} EU at up to {@link #STARTUP_RATE} EU/t. Its flux
 *       carries the plasma current for {@link #PULSE_TICKS} ticks of plasma; two electron cyclotron heaters drive the current
 *       without it ({@link #CURRENT_DRIVE_SHARE} of the fusion power each), and the burn is steady.</li>
 *   <li>A disruption follows a quench or a broken structure while there is plasma, helium ash left in a fusing plasma without
 *       an exhaust port ({@link #ASH_LIMIT} ticks), or an overheated blanket. A redstone stop, the fuel running out, the end of
 *       a pulse or a radiative collapse end the plasma gently.</li>
 * </ul>
 */
public final class FusionPlasma {
    /**
     * 2 x the Mekanism fusion reactor's air-cooled maximum (19,600,000 FE/t at injection rate 98): the full-power advancement.
     * It was the output cap until 0.1.11.98; uncapped, the demonstration blanket (proton quad rods boxed in by iridium
     * overclocked vents) gives 41,440,000 EU/t and stronger blankets more.
     */
    public static final long FULL_POWER = 39_200_000L;
    /** 0.1.11.98: the highest output, the EU grid's top voltage (MAX, 2,147,483,647 EU/t): an overflow guard, not a cap. */
    public static final long OUTPUT_LIMIT = Integer.MAX_VALUE;
    /** 0.1.11.134: the feed goes past the reference (16 steps) up to 20, into the density and beta limits. */
    public static final int MIN_FEED = 1, REFERENCE_FEED = 16, MAX_FEED = 20;
    /** The fusion power of the reference burn (16 feed steps, {@link #REFERENCE_KEV} keV): 9,800,000 EU/t. */
    public static final long FULL_FUSION = 9_800_000L;
    /** The full fusion power over the 16 steps (0.1.11.133: the power now grows with the square of the step). */
    public static final long FUSION_PER_FEED = FULL_FUSION / REFERENCE_FEED;
    public static final double ALPHA_SHARE = 0.2, NEUTRON_SHARE = 0.8;
    public static final double START_KEV = 1.0;
    public static final long MAGNET_UPKEEP = FULL_POWER / 100;        // 392,000 EU/t
    public static final int COOLDOWN_TICKS = 600;
    public static final long STARTUP_ENERGY = 300_000_000L, STARTUP_RATE = 10_000_000L;
    public static final long HEATING_PER_PORT = 245_000L;
    public static final int PULSE_TICKS = 8_000;
    public static final double CURRENT_DRIVE_SHARE = 0.0125;          // per heater; two needed
    public static final int ASH_LIMIT = 1_200;
    public static final int DISRUPTION_TICKS = 200, QUENCH_TICKS = 1_200;

    // ---------------------------------------------------------------- 0.1.11.133: the reference machine (SI)
    /** Plasma volume (m³, ITER 830), the electron density at 16 steps (m⁻³), Z_eff. */
    public static final double VOLUME = 830.0, REFERENCE_DENSITY = 1.0e20, Z_EFF = 1.2;
    /** The reference burn temperature (keV), the ignition window, re-heating and collapse temperatures. */
    public static final double REFERENCE_KEV = 15.0, IGNITION_MIN_KEV = 10.0, REIGNITE_KEV = 8.0, COLLAPSE_KEV = 0.5;
    /** The confinement time's caps and scaling exponents (IPB98(y,2): density 0.41, power -0.69). */
    public static final double TAU_CAP = 2.0, TAU_DENSITY_EXP = 0.41, TAU_POWER_EXP = -0.69;
    /** How long the density takes to follow the feed step (s). */
    public static final double FUELLING_SECONDS = 1.0;
    /** The Lawson triple product for ignition (keV s/m³). */
    public static final double LAWSON = 3.0e21;
    public static final double SECONDS_PER_TICK = 0.05;
    // ---------------------------------------------------------------- 0.1.11.134: the operating limits
    /** Field on axis (T), minor radius (m), plasma current (MA), plasma surface (m²): ITER's. */
    public static final double FIELD = 5.3, MINOR_RADIUS = 2.0, PLASMA_CURRENT = 15.0, SURFACE = 683.0;
    /** The Greenwald density (m⁻³): I_p / (π a²) x 10²⁰. */
    public static final double GREENWALD = PLASMA_CURRENT / (Math.PI * MINOR_RADIUS * MINOR_RADIUS) * 1.0e20;
    public static final double BETA_N_LIMIT = 3.5, RISK_FROM = 0.9;
    public static final double LH_HYSTERESIS = 0.8, L_MODE_FACTOR = 0.5;
    public static final double ELM_BASE_SECONDS = 2.0, ELM_SHARE = 0.015, ELM_MIN_SECONDS = 1.0, ELM_MAX_SECONDS = 6.0;
    private static final double MU0 = 4.0e-7 * Math.PI;
    private static final double KEV_J = 1.602176634e-16, FUSION_J = 17.6e3 * KEV_J, BREMS = 5.35e-37;
    private static final double SV_REF = sigmaV(REFERENCE_KEV);
    private static final double P_REF = fusionWatts(REFERENCE_DENSITY, REFERENCE_KEV);
    /** EU/t per watt: the reference fusion power is {@link #FULL_FUSION}. */
    public static final double EU_PER_WATT = FULL_FUSION / P_REF;
    private static final double LOSS_REF = ALPHA_SHARE * P_REF - bremsWatts(REFERENCE_DENSITY, REFERENCE_KEV);
    private static final double TAU_REF = thermalJoules(REFERENCE_DENSITY, REFERENCE_KEV) / LOSS_REF;

    /** The D-T reactivity ⟨σv⟩ (m³/s) at a temperature (keV): the Bosch-Hale fit (1992), 0.2-100 keV. */
    public static double sigmaV(double kev) {
        if (kev < 0.2) return 0;
        double t = Math.min(kev, 100.0);
        double bg = 34.3827, mrc2 = 1_124_656;
        double c1 = 1.17302e-9, c2 = 1.51361e-2, c3 = 7.51886e-2, c4 = 4.60643e-3, c5 = 1.35e-2, c6 = -1.0675e-4, c7 = 1.366e-5;
        double theta = t / (1 - t * (c2 + t * (c4 + t * c6)) / (1 + t * (c3 + t * (c5 + t * c7))));
        double xi = Math.cbrt(bg * bg / (4 * theta));
        return c1 * theta * Math.sqrt(xi / (mrc2 * t * t * t)) * Math.exp(-3 * xi) * 1e-6;
    }

    /** Fusion power (W) of the reference volume at a density (m⁻³, half deuterium, half tritium) and temperature. */
    public static double fusionWatts(double density, double kev) {
        double half = density / 2;
        return half * half * sigmaV(kev) * FUSION_J * VOLUME;
    }

    /** Bremsstrahlung (W) of the reference volume. */
    public static double bremsWatts(double density, double kev) {
        return BREMS * density * density * Z_EFF * Math.sqrt(Math.max(0, kev)) * VOLUME;
    }

    /** The plasma's thermal energy (J): 3 n T V (electrons and ions at one temperature). */
    public static double thermalJoules(double density, double kev) {
        return 3 * density * kev * KEV_J * VOLUME;
    }

    /** The energy confinement time (s) at a density and a heating power through the plasma (W). */
    public static double tauE(double density, double watts) {
        double scaled = TAU_REF * Math.pow(Math.max(density, 1) / REFERENCE_DENSITY, TAU_DENSITY_EXP)
            * Math.pow(Math.max(watts, 1.0e3) / LOSS_REF, TAU_POWER_EXP);
        return Math.min(TAU_CAP * TAU_REF, scaled);
    }

    /** The confinement time of the reference burn (s). */
    public static double referenceTauE() { return TAU_REF; }

    /** The density (m⁻³) a feed step aims at. */
    public static double targetDensity(int feed) {
        return REFERENCE_DENSITY * clampFeed(feed) / REFERENCE_FEED;
    }

    /** 0.1.11.134: the L-H power threshold (W) at a density: Martin 2008. */
    public static double lhThreshold(double density) {
        return 0.0488 * Math.pow(Math.max(0, density) / 1.0e20, 0.717) * Math.pow(FIELD, 0.803) * Math.pow(SURFACE, 0.941) * 1.0e6;
    }

    /** 0.1.11.134: the normalised beta β_N (β in %, x a B / I_p) at a density and temperature. */
    public static double betaN(double density, double kev) {
        double pressure = 2 * density * kev * KEV_J;   // electrons and ions
        double beta = pressure / (FIELD * FIELD / (2 * MU0)) * 100;
        return beta * MINOR_RADIUS * FIELD / PLASMA_CURRENT;
    }

    /**
     * 0.1.11.134: the chance per second that a plasma at {@code f} of a limit disrupts: none below {@link #RISK_FROM}, rising to
     * 2% at the limit, then 2% more per 1% past it.
     */
    public static double hazard(double f) {
        if (f < RISK_FROM) return 0;
        if (f <= 1) {
            double k = (f - RISK_FROM) / (1 - RISK_FROM);
            return 0.02 * k * k * k;
        }
        return 0.02 + 2.0 * (f - 1);
    }

    /** 0.1.11.134: the time between two ELMs (s) at a power through the edge (W). */
    public static double elmInterval(double edgeWatts) {
        return Math.max(ELM_MIN_SECONDS, Math.min(ELM_MAX_SECONDS, ELM_BASE_SECONDS * LOSS_REF / Math.max(edgeWatts, 1.0e3)));
    }

    /** Fusion power (EU/t) at a density and temperature. */
    public static long fusionPower(double density, double kev) {
        double d = density / REFERENCE_DENSITY;
        return Math.round(FULL_FUSION * d * d * sigmaV(kev) / SV_REF);
    }

    /** Fusion power (EU/t) at a feed step's target density and a temperature. */
    public static long fusionPower(int feed, double kev) {   // at a feed step's target density
        return fusionPower(targetDensity(feed), kev);
    }

    /** The net heating (W) of a plasma with only its alpha particles: positive when it is ignited. */
    public static double alphaMargin(double density, double kev) {
        double alpha = ALPHA_SHARE * fusionWatts(density, kev), brems = bremsWatts(density, kev);
        return alpha - thermalJoules(density, kev) / tauE(density, alpha - brems) - brems;
    }

    public enum State { OFF, COOLING, CHARGING, HEATING, BURNING, DISRUPTED, QUENCHED }

    public enum Event { NONE, PLASMA_START, IGNITION, PULSE_END, DISRUPTION, QUENCH }

    /** 0.1.11.134: why the last disruption came. */
    public enum Cause { NONE, QUENCH, STRUCTURE, ASH, BLANKET, DENSITY_LIMIT, BETA_LIMIT }

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
    private double kev, density, fuelCarry, lastTau;
    private int cooled, burnTicks, ash, timer;
    private long charged, lastHeating, lastFusion, lastAlpha, lastLoss;
    private boolean hMode;
    private int elmTicks, elms;
    private double lastGreenwald, lastBeta;
    private Cause cause = Cause.NONE;
    private java.util.random.RandomGenerator random = new java.util.SplittableRandom();

    /** 0.1.11.134: a fixed random source (tests). */
    public void random(java.util.random.RandomGenerator random) { this.random = random; }
    /** 0.1.11.134: H-mode, the ELMs so far in this plasma, the share of the Greenwald density and of the beta limit. */
    public boolean hMode() { return hMode; }
    public int elms() { return elms; }
    public double greenwaldFraction() { return lastGreenwald; }
    public double betaFraction() { return lastBeta; }
    /** 0.1.11.134: why the last disruption came. */
    public Cause cause() { return cause; }
    /** 0.1.11.134: the ticks to the next ELM (H-mode). */
    public int elmTicks() { return elmTicks; }

    public State state() { return state; }
    public double kev() { return kev; }
    /** 0.1.11.133: the electron density (m⁻³). */
    public double density() { return density; }
    /** 0.1.11.133: the energy confinement time of the last tick (s). */
    public double tauE() { return lastTau; }
    /** 0.1.11.133: the Lawson triple product n T τE of the last tick (keV s/m³). */
    public double tripleProduct() { return density * kev * lastTau; }
    /** 0.1.11.133: the alpha heating and the losses (conduction and bremsstrahlung) of the last tick, in EU/t. */
    public long lastAlpha() { return lastAlpha; }
    public long lastLoss() { return lastLoss; }
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
        lastAlpha = 0;
        lastLoss = 0;
        Event event = Event.NONE;

        // the timers of a lost plasma and of warm magnets run whatever the reactor does
        if (state == State.DISRUPTED || state == State.QUENCHED) {
            if (--timer > 0) return new Result(0, 0, 0, 0, Event.NONE);
            state = state == State.QUENCHED ? State.COOLING : State.CHARGING;
            if (state == State.COOLING) cooled = 0;
        }
        if (!in.formed() && hasPlasma()) return disrupt(Event.DISRUPTION, false, 0, Cause.STRUCTURE);
        if (!in.formed() || !in.run()) {
            if (hasPlasma()) endPlasma();
            state = State.OFF;
            cooled = Math.max(0, cooled - 1);   // the magnets warm up slowly without upkeep
            return new Result(0, 0, 0, 0, Event.NONE);
        }
        if (state == State.OFF) state = cooled >= COOLDOWN_TICKS ? State.CHARGING : State.COOLING;

        // the magnets first: without the cryogenic plant they quench
        if (budget < MAGNET_UPKEEP) {
            if (state == State.COOLING) {
                cooled = Math.max(0, cooled - 2);
                return new Result(0, 0, 0, 0, Event.NONE);
            }
            return disrupt(hasPlasma() ? Event.DISRUPTION : Event.QUENCH, true, 0, Cause.QUENCH);
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
                    density = targetDensity(feed);
                    fuelCarry = 0;
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
                if (in.blanketOverheated()) return disrupt(Event.DISRUPTION, false, used, Cause.BLANKET);
                // the density follows the feed step; the thermal energy stays
                if (density <= 0) density = targetDensity(feed);
                double w = thermalJoules(density, kev);
                density += (targetDensity(feed) - density) * Math.min(1, SECONDS_PER_TICK / FUELLING_SECONDS);
                kev = w / (3 * density * KEV_J * VOLUME);
                // an ignited plasma that has cooled is heated again
                if (state == State.BURNING && kev < REIGNITE_KEV) state = State.HEATING;
                long heating = 0;
                if (state == State.HEATING) {
                    heating = Math.min((long) in.heaters() * HEATING_PER_PORT, budget);
                    budget -= heating;
                    used += heating;
                }
                long fusion = fusionPower(density, kev);
                // current drive: two electron cyclotron heaters keep the plasma current without the solenoid
                long drive = in.cyclotronHeaters() >= 2 ? Math.round(2 * CURRENT_DRIVE_SHARE * fusion) : 0;
                boolean steady = in.cyclotronHeaters() >= 2 && drive <= budget;
                if (steady) {
                    budget -= drive;
                    used += drive;
                }
                // the power balance
                double fusionW = fusionWatts(density, kev), alphaW = ALPHA_SHARE * fusionW, auxW = heating / EU_PER_WATT;
                double bremsW = bremsWatts(density, kev), edgeW = alphaW + auxW - bremsW, threshold = lhThreshold(density);
                // L-H transition, with hysteresis
                if (!hMode && edgeW >= threshold) {
                    hMode = true;
                    elmTicks = (int) Math.round(elmInterval(edgeW) / SECONDS_PER_TICK);
                } else if (hMode && edgeW < LH_HYSTERESIS * threshold) {
                    hMode = false;
                }
                lastTau = tauE(density, edgeW) * (hMode ? 1 : L_MODE_FACTOR);
                double conductionW = w / lastTau, lossW = conductionW + bremsW;
                double elmW = 0;
                if (hMode) {
                    // ELMs carry ELM_SHARE of W at each crash; the conduction between them is lower by their average
                    double interval = elmInterval(edgeW);
                    elmW = Math.min(conductionW, ELM_SHARE * w / interval);
                }
                w = Math.max(0, w + (alphaW + auxW - (conductionW - elmW) - bremsW) * SECONDS_PER_TICK);
                if (hMode && --elmTicks <= 0) {
                    w *= 1 - ELM_SHARE;
                    elms++;
                    elmTicks = Math.max(1, (int) Math.round(elmInterval(edgeW) / SECONDS_PER_TICK));
                }
                kev = w / (3 * density * KEV_J * VOLUME);
                lastAlpha = Math.round(alphaW * EU_PER_WATT);
                lastLoss = Math.round(lossW * EU_PER_WATT);
                // the density and beta limits
                lastGreenwald = density / GREENWALD;
                lastBeta = betaN(density, kev) / BETA_N_LIMIT;
                double chance = 1 - Math.exp(-(hazard(lastGreenwald) + hazard(lastBeta)) * SECONDS_PER_TICK);
                if (chance > 0 && random.nextDouble() < chance)
                    return disrupt(Event.DISRUPTION, false, used, hazard(lastGreenwald) >= hazard(lastBeta) ? Cause.DENSITY_LIMIT : Cause.BETA_LIMIT);
                if (state == State.HEATING && kev >= IGNITION_MIN_KEV && alphaMargin(density, kev) >= 0
                    && ALPHA_SHARE * fusionW - bremsW >= LH_HYSTERESIS * threshold) {   // 0.1.11.134: and alpha alone keeps H-mode
                    state = State.BURNING;
                    event = Event.IGNITION;
                }
                // fuel: one unit of each per tick for each 1/16 of the full fusion rate
                fuelCarry += (double) REFERENCE_FEED * fusion / FULL_FUSION;
                long fuel = (long) Math.floor(fuelCarry);
                fuelCarry -= fuel;
                fuel = Math.min(fuel, Math.min(in.deuterium(), in.tritium()));
                lastHeating = heating;
                lastFusion = fusion;
                if (fusion > 0) {
                    ash = in.exhaust() ? 0 : ash + 1;
                    if (ash >= ASH_LIMIT) return disrupt(Event.DISRUPTION, false, used, Cause.ASH);
                }
                if (!steady && ++burnTicks >= PULSE_TICKS) {
                    endPlasma();
                    return new Result(used, heating, fusion, fuel, Event.PULSE_END);
                }
                // too cold to keep: the plasma radiates its last heat away
                if (heating == 0 && kev < COLLAPSE_KEV) {
                    endPlasma();
                    return new Result(used, 0, fusion, fuel, Event.PULSE_END);
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
        clearPlasma();
    }

    private void clearPlasma() {
        hMode = false;
        elmTicks = 0;
        elms = 0;
        lastGreenwald = 0;
        lastBeta = 0;
        kev = 0;
        density = 0;
        fuelCarry = 0;
        lastTau = 0;
        charged = 0;
        burnTicks = 0;
        ash = 0;
    }

    private Result disrupt(Event event, boolean quench, long used, Cause why) {
        boolean plasma = hasPlasma();
        if (plasma || quench) cause = why;
        clearPlasma();
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

    /** 0.1.11.133: the plasma's thermal energy (J): what a disruption throws onto the first wall. */
    public double thermalEnergy() {
        return thermalJoules(density, kev);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("State", state.name());
        tag.putDouble("Kev", kev);
        tag.putDouble("Density", density);
        tag.putDouble("FuelCarry", fuelCarry);
        tag.putBoolean("HMode", hMode);
        tag.putInt("ElmTicks", elmTicks);
        tag.putInt("Elms", elms);
        tag.putString("Cause", cause.name());
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
        density = Math.max(0, tag.getDouble("Density"));   // 0.1.11.133; an older save finds it again from the feed step
        fuelCarry = Math.max(0, Math.min(1, tag.getDouble("FuelCarry")));
        hMode = tag.getBoolean("HMode");
        elmTicks = Math.max(0, tag.getInt("ElmTicks"));
        elms = Math.max(0, tag.getInt("Elms"));
        try {
            cause = tag.contains("Cause") ? Cause.valueOf(tag.getString("Cause")) : Cause.NONE;
        } catch (IllegalArgumentException e) {
            cause = Cause.NONE;
        }
        cooled = Math.max(0, Math.min(COOLDOWN_TICKS, tag.getInt("Cooled")));
        burnTicks = Math.max(0, tag.getInt("Burn"));
        ash = Math.max(0, tag.getInt("Ash"));
        timer = Math.max(0, tag.getInt("Timer"));
        charged = Math.max(0, Math.min(STARTUP_ENERGY, tag.getLong("Charged")));
    }
}
