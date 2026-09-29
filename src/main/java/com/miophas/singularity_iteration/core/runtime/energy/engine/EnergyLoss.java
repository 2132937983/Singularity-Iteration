package com.miophas.singularity_iteration.core.runtime.energy.engine;

import java.math.BigInteger;

/** One loss policy for native accounting, effects validation and FE quantization. */
public final class EnergyLoss {
    private EnergyLoss() { }
    public static EnergyAmount fromMilli(long milli) {
        if (milli < 0) throw new IllegalArgumentException("Negative wire loss");
        if (IndependentEnergyMode.roundEnetLoss()) return EnergyAmount.of(milli / 1000);
        return EnergyAmount.fromUnits(BigInteger.valueOf(milli).shiftLeft(EnergyAmount.FRACTION_BITS)
            .divide(BigInteger.valueOf(1000)));
    }
    /** FE is integral: charge the smallest whole FE covering the configured route loss. */
    public static int feLoss(long milli) {
        var unitsPerFe = BigInteger.valueOf(EnergyAmount.UNITS / 4);
        return fromMilli(milli).units().add(unitsPerFe.subtract(BigInteger.ONE)).divide(unitsPerFe)
            .min(BigInteger.valueOf(Integer.MAX_VALUE)).intValueExact();
    }
}
