// SCEX 2026-09-12: invalidate the immutable sorted snapshot after registration.
/*
 * MIT License
 *
 * Copyright (c) 2020 Azercoco & Technici4n
 * Copyright (c) 2024 Industrial_Craft_In_Future
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.miophas.singularity_iteration.core.api.energy.storage;

import com.miophas.singularity_iteration.core.api.energy.ICableTier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Voltage tier definitions. All cable parameters are set in this class.
 *
 * <p>The tiers agree with the original IC2 voltage tier system:</p>
 * <pre>
 *   LV = 32 EU/t   (tier 0) - Low Voltage
 *   MV = 128 EU/t  (tier 1) - Medium Voltage
 *   HV = 512 EU/t  (tier 2) - High Voltage
 *   EV = 2048 EU/t (tier 3) - Extreme Voltage
 *   IV = 8192 EU/t (tier 4) - Insane Voltage
 *   LuV = 32768 EU/t (tier 5) - Ludicrous Voltage
 *   ZPMV = 131072 EU/t (tier 6) - Zero Point Module Voltage
 *   UV = 524288 EU/t (tier 7) - Ultimate Voltage
 *   UHV = 2097152 EU/t (tier 8) - Ultra High Voltage
 *   UEV = 8388608 EU/t (tier 9) - Ultra Excessive Voltage
 *   UIV = 33554432 EU/t (tier 10) - Ultra Immense Voltage
 *   UXV = 134217728 EU/t (tier 11) - Ultra Extreme Voltage
 *   OpV = 536870912 EU/t (tier 12) - Overpowered Voltage
 *   MAX = 2147483647 EU/t (tier 13) - Maximum Voltage
 * </pre>
 *
 * <h3>Parameters</h3>
 * <ul>
 *   <li>{@link #powerRating} - the voltage rating (EU/t). It sets the energy transfer rate that a machine accepts.</li>
 *   <li>{@link #electricDamage} - the shock damage of a bare cable (0 for an insulated cable).</li>
 *   <li>{@link #conductorBreakdownEnergy} - the conductor breakdown threshold. Above this value, the conductor is destroyed.</li>
 *   <li>{@link #insulationBreakdownEnergy} - the insulation breakdown threshold. Above this value, the insulation is removed.</li>
 *   <li>{@link #insulationEnergyAbsorption} - the insulation absorption threshold of a bare cable. Below this value, the insulation absorbs all energy.</li>
 *   <li>{@link #conductionLoss} - the conduction loss of a bare cable, per block (EU per packet).</li>
 *   <li>{@link #insulatedInsulationEnergyAbsorption} - the insulation absorption threshold of an insulated cable.</li>
 *   <li>{@link #insulatedConductionLoss} - the conduction loss of an insulated cable, per block (EU per packet).</li>
 * </ul>
 *
 * <p><b>To change cable parameters, change only the instance definitions in this class.</b></p>
 */
@SuppressWarnings("null")
public final class CableTier implements Comparable<CableTier>, ICableTier {

    // ============ Voltage tier instances ============
    // SCEX clean-room: base cable losses calibrated against 2.8.222-ex112
    // normal placement and saved-energy traces; see R3 grid fixtures/evidence.
    // Insulation does not change the measured loss of the corresponding metal.

    /** LV (Low Voltage): 32 EU/t. Shock damage is 4 points. */
    public static CableTier LV = new CableTier(
        "lv", "LV", "Low Voltage",
        32, 0, 4.0f,
        33L,       // conductorBreakdownEnergy: 32 + 1
        9001L,     // insulationBreakdownEnergy
        8.0,       // bare tin: IC2 tier 0
        0.2,       // conductionLoss (bare), EU per packet per block
        32.0,      // insulatedInsulationEnergyAbsorption: full voltage
        0.2        // insulatedConductionLoss
    );

    /** MV (Medium Voltage): 128 EU/t. Shock damage is 8 points. */
    public static CableTier MV = new CableTier(
        "mv", "MV", "Medium Voltage",
        128, 1, 8.0f,
        129L,
        9001L,
        32.0,      // bare copper
        0.2,
        128.0,
        0.2
    );

    /** HV (High Voltage): 512 EU/t. Shock damage is 18 points. */
    public static CableTier HV = new CableTier(
        "hv", "HV", "High Voltage",
        512, 2, 18.0f,
        513L,
        9001L,
        32.0,      // bare gold
        0.4,
        128.0,     // one layer; two layers absorb 512 EU
        0.4
    );

    /** EV (Extreme Voltage): 2048 EU/t. Shock damage is 40 points. */
    public static CableTier EV = new CableTier(
        "ev", "EV", "Extreme Voltage",
        2048, 3, 40.0f,
        2049L,
        9001L,
        32.0,      // bare iron
        0.8,
        128.0,     // one layer; two/three layers absorb 512/2048 EU
        0.8
    );

    /** IV (Insane Voltage): 8192 EU/t. No shock damage. Glass fibre cable; the loss is 0.025 EU per packet per block. */
    public static CableTier IV = new CableTier(
        "iv", "IV", "Insane Voltage",
        8192, 4, 0.0f,
        8193L,
        9001L,
        2048.0,
        0.025,
        8192.0,
        0.025
    );

    /** LuV (Ludicrous Voltage): 32768 EU/t. No shock damage. Superconductor alloy; fully insulated, zero loss. */
    public static CableTier LuV = new CableTier(
        "luv", "LuV", "Ludicrous Voltage",
        32768, 5, 0.0f,
        32769L,
        9001L,
        8192.0,
        0.0,
        32768.0,
        0.0
    );

    /** ZPMV (Zero Point Module Voltage): 131072 EU/t. No shock damage. Zero point module; fully insulated, zero loss. */
    public static CableTier ZPMV = new CableTier(
        "zpmv", "ZPMV", "Zero Point Module Voltage",
        131072, 6, 0.0f,
        131073L,
        9001L,
        32768.0,
        0.0,
        131072.0,
        0.0
    );

    /** UV (Ultimate Voltage): 524288 EU/t. No shock damage. Superconductor; fully insulated, zero loss. */
    public static CableTier UV = new CableTier(
        "uv", "UV", "Ultimate Voltage",
        524288, 7, 0.0f,
        524289L,
        9001L,
        131072.0,
        0.0,
        524288.0,
        0.0
    );

    /** UHV (Ultra High Voltage): 2097152 EU/t. No shock damage. Superconductor; fully insulated, zero loss. */
    public static CableTier UHV = new CableTier(
        "uhv", "UHV", "Ultra High Voltage",
        2097152, 8, 0.0f,
        2097153L,
        9001L,
        524288.0,
        0.0,
        2097152.0,
        0.0
    );

    /** UEV (Ultra Excessive Voltage): 8388608 EU/t. No shock damage. Superconductor; fully insulated, zero loss. */
    public static CableTier UEV = new CableTier(
        "uev", "UEV", "Ultra Excessive Voltage",
        8388608, 9, 0.0f,
        8388609L,
        9001L,
        2097152.0,
        0.0,
        8388608.0,
        0.0
    );

    /** UIV (Ultra Immense Voltage): 33554432 EU/t. No shock damage. Superconductor; fully insulated, zero loss. */
    public static CableTier UIV = new CableTier(
        "uiv", "UIV", "Ultra Immense Voltage",
        33554432, 10, 0.0f,
        33554433L,
        9001L,
        8388608.0,
        0.0,
        33554432.0,
        0.0
    );

    /** UXV (Ultra Extreme Voltage): 134217728 EU/t. No shock damage. Superconductor; fully insulated, zero loss. */
    public static CableTier UXV = new CableTier(
        "uxv", "UXV", "Ultra Extreme Voltage",
        134217728, 11, 0.0f,
        134217729L,
        9001L,
        33554432.0,
        0.0,
        134217728.0,
        0.0
    );

    /** OpV (Overpowered Voltage): 536870912 EU/t. No shock damage. Superconductor; fully insulated, zero loss. */
    public static CableTier OpV = new CableTier(
        "opv", "OpV", "Overpowered Voltage",
        536870912, 12, 0.0f,
        536870913L,
        9001L,
        134217728.0,
        0.0,
        536870912.0,
        0.0
    );

    /** MAX (Maximum Voltage): 2147483647 EU/t. No shock damage. Superconductor; fully insulated, zero loss. */
    public static CableTier MAX = new CableTier(
        "max", "MAX", "Maximum Voltage",
        2147483647L, 13, 0.0f,
        2147483647L,
        9001L,
        536870912.0,
        0.0,
        2147483647.0,
        0.0
    );

    /**
     * Superconducting alloy cable (0.1.7.32): no voltage or packet ceiling at all. Shows as MAX but
     * is not registered as a tier of its own, so tier lookups and indices are unchanged; its
     * conductor and insulation never break down and it carries packets of any size losslessly.
     */
    public static final CableTier UNLIMITED = new CableTier(
        "max", "MAX", "Maximum Voltage",
        Long.MAX_VALUE, 13, 0.0f,
        Long.MAX_VALUE,
        Long.MAX_VALUE,
        Double.MAX_VALUE,
        0.0,
        Double.MAX_VALUE,
        0.0,
        true
    );

    // ============ Fields ============

    /** The internal name (for example "lv", "mv"). */
    public final String name;
    /** The short display name (for example "LV", "MV"). */
    public final String shortEnglishName;
    /** The long display name (for example "Low Voltage"). */
    public final String longEnglishName;
    /** The voltage rating (EU/t). */
    public final long powerRating;
    /** The tier index: 0=LV, 1=MV, 2=HV, 3=EV, 4=IV, 5=LuV, and so on up to 13=MAX. */
    public final int tierIndex;
    /** True if this is a built-in tier. */
    public final boolean builtin;

    // --- Cable electrical parameters ---

    /** The shock damage. 0 means no damage. It is always 0 for an insulated cable. */
    public final float electricDamage;
    /** The conductor breakdown threshold (EU). Above this value, the conductor itself is destroyed. */
    public final long conductorBreakdownEnergy;
    /** The insulation breakdown threshold (EU). Above this value, the insulation is removed. Never less than {@link #conductorBreakdownEnergy}. */
    public final long insulationBreakdownEnergy;
    /** The insulation absorption threshold of a bare cable (EU). Below this value, the insulation absorbs all energy. Above this value, the cable causes shock damage. It is Integer.MAX_VALUE when {@link #electricDamage} is 0 or less. */
    public final double insulationEnergyAbsorption;
    /** The conduction loss of a bare cable (EU per packet per block). */
    public final double conductionLoss;
    /** The insulation absorption threshold of an insulated cable (EU). It is Integer.MAX_VALUE when {@link #electricDamage} is 0 or less. */
    public final double insulatedInsulationEnergyAbsorption;
    /** The conduction loss of an insulated cable (EU per packet per block). */
    public final double insulatedConductionLoss;

    // ============ Constructors ============

    /**
     * Makes a built-in voltage tier with all cable electrical parameters.
     *
     * @param name                            the internal name
     * @param shortEnglishName                the short display name
     * @param longEnglishName                 the long display name
     * @param powerRating                     the voltage rating (EU/t); must not be negative
     * @param tierIndex                       the tier index
     * @param electricDamage                  the shock damage
     * @param conductorBreakdownEnergy        the conductor breakdown energy (EU)
     * @param insulationBreakdownEnergy       the insulation breakdown energy (EU)
     * @param insulationEnergyAbsorption      the insulation absorption threshold of a bare cable
     * @param conductionLoss                  the conduction loss of a bare cable
     * @param insulatedInsulationEnergyAbsorption the insulation absorption threshold of an insulated cable
     * @param insulatedConductionLoss         the conduction loss of an insulated cable
     * @throws IllegalArgumentException if {@code powerRating} is negative
     */
    public CableTier(String name, String shortEnglishName, String longEnglishName,
                     long powerRating, int tierIndex, float electricDamage,
                     long conductorBreakdownEnergy,
                     long insulationBreakdownEnergy,
                     double insulationEnergyAbsorption,
                     double conductionLoss,
                     double insulatedInsulationEnergyAbsorption,
                     double insulatedConductionLoss) {
        if (powerRating < 0) {
            throw new IllegalArgumentException("Voltage rating must be non-negative");
        }

        this.name = name;
        this.shortEnglishName = shortEnglishName;
        this.longEnglishName = longEnglishName;
        this.powerRating = powerRating;
        this.tierIndex = tierIndex;
        this.builtin = true;
        this.electricDamage = electricDamage;
        this.conductorBreakdownEnergy = conductorBreakdownEnergy;
        this.insulationBreakdownEnergy = Math.max(insulationBreakdownEnergy, conductorBreakdownEnergy);
        this.insulationEnergyAbsorption = electricDamage <= 0 ? Integer.MAX_VALUE : insulationEnergyAbsorption;
        this.conductionLoss = conductionLoss;
        this.insulatedInsulationEnergyAbsorption = electricDamage <= 0 ? Integer.MAX_VALUE : insulatedInsulationEnergyAbsorption;
        this.insulatedConductionLoss = insulatedConductionLoss;
    }

    /**
     * Makes a custom voltage tier. You can set whether it is built-in.
     * The other parameters are the same as in the other constructor.
     *
     * @throws IllegalArgumentException if {@code powerRating} is negative
     */
    public CableTier(String name, String shortEnglishName, String longEnglishName,
                     long powerRating, int tierIndex, float electricDamage,
                     long conductorBreakdownEnergy,
                     long insulationBreakdownEnergy,
                     double insulationEnergyAbsorption,
                     double conductionLoss,
                     double insulatedInsulationEnergyAbsorption,
                     double insulatedConductionLoss,
                     boolean builtin) {
        if (powerRating < 0) {
            throw new IllegalArgumentException("Voltage rating must be non-negative");
        }

        this.name = name;
        this.shortEnglishName = shortEnglishName;
        this.longEnglishName = longEnglishName;
        this.powerRating = powerRating;
        this.tierIndex = tierIndex;
        this.builtin = builtin;
        this.electricDamage = electricDamage;
        this.conductorBreakdownEnergy = conductorBreakdownEnergy;
        this.insulationBreakdownEnergy = Math.max(insulationBreakdownEnergy, conductorBreakdownEnergy);
        this.insulationEnergyAbsorption = electricDamage <= 0 ? Integer.MAX_VALUE : insulationEnergyAbsorption;
        this.conductionLoss = conductionLoss;
        this.insulatedInsulationEnergyAbsorption = electricDamage <= 0 ? Integer.MAX_VALUE : insulatedInsulationEnergyAbsorption;
        this.insulatedConductionLoss = insulatedConductionLoss;
    }

    // ============ Getters ============

    public long getPowerRating() {
        return powerRating;
    }

    public int getTierIndex() {
        return tierIndex;
    }

    /**
     * Gets the energy level number: LV = 1, MV = 2, HV = 3, and so on (the tier index + 1).
     */
    public int getEnergyLevel() {
        return tierIndex + 1;
    }

    @Override
    public int getTier() {
        return tierIndex;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDisplayName() {
        return shortEnglishName;
    }

    @Override
    public String getFullName() {
        return longEnglishName;
    }

    @Override
    public float getElectricDamage() {
        return electricDamage;
    }

    @Override
    public long getConductorBreakdownEnergy() {
        return conductorBreakdownEnergy;
    }

    @Override
    public long getInsulationBreakdownEnergy() {
        return insulationBreakdownEnergy;
    }

    /** @deprecated Use {@link #getPowerRating()} instead. */
    @Deprecated
    public long getMaxTransfer() {
        return powerRating;
    }

    /** @deprecated Use {@link #getPowerRating()} instead. */
    @Deprecated
    public long getEu() {
        return powerRating;
    }

    // ============ Display names ============

    public String shortEnglishKey() {
        return "cable_tier_short.mio_icif." + name;
    }

    public MutableComponent shortEnglishName() {
        return Component.translatable(shortEnglishKey());
    }

    public String longEnglishKey() {
        return "cable_tier_long.mio_icif." + name;
    }

    public MutableComponent longEnglishName() {
        return Component.translatable(longEnglishKey());
    }

    // ============ Comparison ============

    @Override
    public int compareTo(CableTier other) {
        return Long.compare(powerRating, other.powerRating);
    }

    @Override
    public String toString() {
        return name;
    }

    // ============ Registry ============

    private final static Map<String, CableTier> tiers = new HashMap<>();
    private static List<CableTier> cachedAllTiers;

    /**
     * Registers a new voltage tier. Not thread-safe; call during mod initialization.
     *
     * @throws IllegalArgumentException if a tier with the same name or the same voltage rating already exists
     */
    public static void addTier(CableTier tier) {
        for (var existingTier : tiers.values()) {
            if (existingTier.name.equals(tier.name)) {
                throw new IllegalArgumentException("Tier " + tier + " already exists!");
            }
            if (existingTier.powerRating == tier.powerRating) {
                throw new IllegalArgumentException("A tier with voltage rating " + tier.powerRating + " already exists!");
            }
        }
        tiers.put(tier.name, tier);
        cachedAllTiers = null;
    }

    /**
     * Finds a registered voltage tier by its name.
     *
     * @throws NoSuchElementException if no tier has this name
     */
    public static CableTier getTier(String name) {
        CableTier existing = tiers.get(name);
        if (existing == null) {
            throw new NoSuchElementException("No such cable tier: " + name);
        }
        return existing;
    }

    public static CableTier fromICableTier(ICableTier tier) {
        if (tier instanceof CableTier ct) return ct;
        return getTier(tier.getName());
    }

    /**
     * @return all registered voltage tiers, sorted by voltage rating (an immutable list)
     */
    public static List<CableTier> allTiers() {
        if (cachedAllTiers == null) {
            cachedAllTiers = tiers.values().stream().sorted().toList();
        }
        return cachedAllTiers;
    }

    static {
        addTier(LV);
        addTier(MV);
        addTier(HV);
        addTier(EV);
        addTier(IV);
        addTier(LuV);
        addTier(ZPMV);
        addTier(UV);
        addTier(UHV);
        addTier(UEV);
        addTier(UIV);
        addTier(UXV);
        addTier(OpV);
        addTier(MAX);
        cachedAllTiers = tiers.values().stream().sorted().toList();
    }
}
