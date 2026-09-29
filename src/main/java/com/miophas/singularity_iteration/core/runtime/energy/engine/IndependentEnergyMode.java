// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.energy.engine;

import com.miophas.singularity_iteration.core.runtime.CoreConfig;

/**
 * One admission policy for numerical ownership and placement/load factories.
 * Properties are launch-time overrides; hot switching is unsupported.
 * The independently maintained engine is the default product implementation.
 */
public final class IndependentEnergyMode {
    private IndependentEnergyMode() { }
    public static boolean enabled() { return Boolean.parseBoolean(System.getProperty("scex.independent.energy", "true")); }
    public static boolean feature(String feature) {
        return enabled() && Boolean.parseBoolean(System.getProperty("scex.independent." + feature, "true"));
    }

    private static volatile Boolean voltageOverloadOverride = null;

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
}
