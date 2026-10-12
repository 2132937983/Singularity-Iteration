// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.energy.engine;

import com.miophas.singularity_iteration.core.runtime.CoreConfig;
import java.util.Locale;

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
 */
public final class IndependentEnergyMode {
    private IndependentEnergyMode() { }
    public static boolean enabled() { return Boolean.parseBoolean(System.getProperty("scex.independent.energy", "true")); }
    public static boolean feature(String feature) {
        return enabled() && Boolean.parseBoolean(System.getProperty("scex.independent." + feature, "true"));
    }

    private static volatile Boolean voltageOverloadOverride = null;
    private static volatile String distributionOverride = null;
    private static volatile String lineLossOverride = null;

    public static boolean voltageOverload() {
        if (voltageOverloadOverride != null) return voltageOverloadOverride;
        return CoreConfig.getBool("energynet", "enableVoltageOverload", true);
    }

    public static boolean enetExplosions() {
        if (!voltageOverload()) return false;
        return CoreConfig.getBool("energynet", "enableEnetExplosions", true);
    }

    public static boolean enetCableMeltdown() {
        if (!voltageOverload()) return false;
        return CoreConfig.getBool("energynet", "enableEnetCableMeltdown", true);
    }

    public static boolean enetShockDamage() {
        if (!voltageOverload()) return false;
        return CoreConfig.getBool("energynet", "enableEnetShockDamage", true);
    }

    public static boolean roundEnetLoss() {
        return Boolean.parseBoolean(System.getProperty("scex.independent.roundEnetLoss",
            Boolean.toString(CoreConfig.getBool("energynet", "roundEnetLoss", true))));
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
        if (value == null) value = distributionOverride;
        if (value == null) value = CoreConfig.getString("energynet", "distributionMode", "fair");
        return "legacy".equalsIgnoreCase(value.trim()) ? "legacy" : "fair";
    }

    /** Returns the line loss model of the fair-share distributor. An unknown value gives LOSSLESS. */
    public static FairShareDistributor.LineLoss lineLoss() {
        String value = System.getProperty("scex.independent.lineLoss");
        if (value == null) value = lineLossOverride;
        if (value == null) value = CoreConfig.getString("energynet", "lineLossModel", "lossless");
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
}
