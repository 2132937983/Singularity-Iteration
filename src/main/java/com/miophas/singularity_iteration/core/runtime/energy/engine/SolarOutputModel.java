// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.energy.engine;

/**
 * Independent steady-output hypothesis frozen and tested on new public-game
 * inputs in R29. Its 5e-6 EU/t tolerance is not exact cumulative parity or a
 * prediction of the reference panel's refresh phase. The inputs are public
 * world time, local sky light and normalized rain/thunder levels.
 */
public final class SolarOutputModel {
    private SolarOutputModel() { }
    public static float rate(long dayTime, int skyLight, float rain, float thunder) {
        if (skyLight < 0 || skyLight > 15 || !Float.isFinite(rain) || !Float.isFinite(thunder)
                || rain < 0 || rain > 1 || thunder < 0 || thunder > 1)
            throw new IllegalArgumentException("Invalid solar environment");
        float angle = (Math.floorMod(dayTime, 24000L) + 1) / 24000F - .25F;
        if (angle < 0) angle += 1;
        if (angle > 1) angle -= 1;
        float smooth = 1 - (float) ((Math.cos(angle * Math.PI) + 1) / 2);
        angle += (smooth - angle) / 3;
        float light = skyLight / 15F;
        float clear = (float) (Math.max(0, Math.min(1, Math.cos(angle * Math.PI * 2) * 2 + .2)) * light);
        return clear * (1 - rain * .3125F) * (1 - thunder * .3125F);
    }
}
