// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.energy.engine;

import com.miophas.singularity_iteration.core.runtime.CoreConfig;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * One admission policy for numerical ownership and placement/load factories.
 * Properties are launch-time overrides; hot switching is unsupported.
 * The independently maintained engine is the default product implementation.
 *
 * <h2>Distribution and line loss (0.1.8.0)</h2>
 * <ul>
 *   <li>{@link #fairDistribution()}: {@code true} selects
 *       {@link FairShareDistributor} (the default). {@code false} selects the
 *       legacy packet priority walk ({@link FractionalDistributor}).</li>
 *   <li>{@link #lineLoss()}: the loss model of the fair-share distributor.
 *       The default is {@link FairShareDistributor.LineLoss#LOSSLESS}.</li>
 * </ul>
 * <p>Order of precedence for each value: the launch system property
 * ({@code -Dscex.independent.distribution}, {@code -Dscex.independent.lineLoss}),
 * then the value that the common config set with a setter of this class, then
 * {@code core.ini}, then the default. The system property is read at each
 * call, so a test server can select a mode for one batch.
 *
 * <h2>Cached switches (0.1.10.7)</h2>
 * The energy settlement asks the overload switches once per conductor of every delivery path,
 * every tick. A server profile showed these config reads at 7 % of the server thread. The
 * switches are now computed once and kept until {@link CoreConfig#revision()} changes (a config
 * override) or a setter of this class runs. {@link #enabled()} and {@link #feature(String)} read
 * their launch properties once. The distribution mode, the line loss model and the loss rounding
 * still read their system property at each call (tests select them per batch); only their config
 * part is cached.
 */
public final class IndependentEnergyMode {
    private IndependentEnergyMode() { }

    private static final boolean ENABLED = Boolean.parseBoolean(System.getProperty("scex.independent.energy", "true"));
    private static final ConcurrentHashMap<String, Boolean> FEATURES = new ConcurrentHashMap<>();

    public static boolean enabled() { return ENABLED; }
    public static boolean feature(String feature) {
        return ENABLED && FEATURES.computeIfAbsent(feature,
            name -> Boolean.parseBoolean(System.getProperty("scex.independent." + name, "true")));
    }

    private static volatile Boolean voltageOverloadOverride = null;
    private static volatile String distributionOverride = null;
    private static volatile String lineLossOverride = null;
    private static volatile String dispatchOverride = null;

    /** The switches derived from the config and the overrides, for one config revision. */
    private record Switches(int revision, Boolean overloadOverride, String distributionOverride, String lineLossOverride,
                            boolean overload, boolean explosions, boolean meltdown, boolean shock, boolean roundLoss,
                            String distribution, String lineLoss) { }

    private static volatile Switches switches;

    private static Switches switches() {
        Switches current = switches;
        int revision = CoreConfig.revision();
        Boolean overload = voltageOverloadOverride;
        String distribution = distributionOverride, loss = lineLossOverride;
        if (current != null && current.revision == revision && current.overloadOverride == overload
                && current.distributionOverride == distribution && current.lineLossOverride == loss) return current;
        boolean on = overload != null ? overload : CoreConfig.getBool("energynet", "enableVoltageOverload", true);
        current = new Switches(revision, overload, distribution, loss, on,
            on && CoreConfig.getBool("energynet", "enableEnetExplosions", true),
            on && CoreConfig.getBool("energynet", "enableEnetCableMeltdown", true),
            on && CoreConfig.getBool("energynet", "enableEnetShockDamage", true),
            CoreConfig.getBool("energynet", "roundEnetLoss", true),
            distribution != null ? distribution : CoreConfig.getString("energynet", "distributionMode", "fair"),
            loss != null ? loss : CoreConfig.getString("energynet", "lineLossModel", "lossless"));
        switches = current;
        return current;
    }

    public static boolean voltageOverload() { return switches().overload(); }

    public static boolean enetExplosions() { return switches().explosions(); }

    public static boolean enetCableMeltdown() { return switches().meltdown(); }

    public static boolean enetShockDamage() { return switches().shock(); }

    public static boolean roundEnetLoss() {
        String property = System.getProperty("scex.independent.roundEnetLoss");
        return property != null ? Boolean.parseBoolean(property) : switches().roundLoss();
    }

    public static void setVoltageOverload(boolean enabled) {
        voltageOverloadOverride = enabled;
        CoreConfig.overrideBool("energynet", "enableVoltageOverload", enabled);
    }

    /**
     * Tells which distributor the native pass uses.
     *
     * @return {@code true} for the fair-share distributor ({@code fair}, the
     *         default), {@code false} for the legacy packet walk ({@code legacy})
     */
    public static boolean fairDistribution() {
        return !"legacy".equals(distributionMode());
    }

    /** Returns {@code fair} or {@code legacy}. An unknown value gives {@code fair}. */
    public static String distributionMode() {
        String value = System.getProperty("scex.independent.distribution");
        if (value == null) value = switches().distribution();
        return "legacy".equalsIgnoreCase(value.trim()) ? "legacy" : "fair";
    }

    /** Returns the line loss model of the fair-share distributor. An unknown value gives LOSSLESS. */
    public static FairShareDistributor.LineLoss lineLoss() {
        String value = System.getProperty("scex.independent.lineLoss");
        if (value == null) value = switches().lineLoss();
        return "proportional".equals(value.trim().toLowerCase(Locale.ROOT))
            ? FairShareDistributor.LineLoss.PROPORTIONAL : FairShareDistributor.LineLoss.LOSSLESS;
    }

    /**
     * Sets the distribution mode from the common config.
     *
     * @param mode {@code fair}, {@code legacy}, or {@code null} to remove the override
     */
    public static void setDistributionMode(String mode) { distributionOverride = mode; }

    /**
     * Sets the line loss model from the common config.
     *
     * @param model {@code lossless}, {@code proportional}, or {@code null} to remove the override
     */
    public static void setLineLossModel(String model) { lineLossOverride = model; }

    /**
     * <p><b>EN:</b> 0.1.10.22 (G3): the dispatch order of the fair distribution. {@code MERIT} (default): must-run
     * sources and relays, then dispatchable (fuel) generators, then storage; fuel charges storage. {@code ECONOMIC}:
     * must-run sources and relays, then storage, then fuel; fuel does not charge storage. {@code ROTATION}: the order
     * before 0.1.10.22 (all generators and relays take turns, then storage).
     * <p><b>ZH:</b> 0.1.10.22（G3）：公平分配的调度次序。{@code MERIT}（默认）：必须运行的电源与中继 → 可调度（燃料）发电机 → 储能，燃料为储能充电。
     * {@code ECONOMIC}：必须运行的电源与中继 → 储能 → 燃料，燃料不为储能充电。{@code ROTATION}：0.1.10.22 之前的次序（发电机与中继轮流，然后储能）。
     * <p><b>JA:</b> 0.1.10.22（G3）：公平分配の給電順。{@code MERIT}（既定）：必ず使う電源と中継 → 調整できる（燃料）発電機 → 蓄電。燃料は蓄電を充電する。
     * {@code ECONOMIC}：必ず使う電源と中継 → 蓄電 → 燃料。燃料は蓄電を充電しない。{@code ROTATION}：0.1.10.22 より前の順（発電機と中継が交代、次に蓄電）。
     */
    public enum DispatchMode { MERIT, ECONOMIC, ROTATION }

    public static DispatchMode dispatchMode() {
        String value = System.getProperty("scex.independent.dispatch");
        if (value == null) value = dispatchOverride;
        if (value == null) value = CoreConfig.getString("energynet", "dispatchMode", "merit");
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "economic" -> DispatchMode.ECONOMIC;
            case "rotation" -> DispatchMode.ROTATION;
            default -> DispatchMode.MERIT;
        };
    }

    public static void setDispatchMode(String mode) { dispatchOverride = mode; }
}
