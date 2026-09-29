// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.energy.engine;

import java.util.Objects;
import java.util.function.IntUnaryOperator;

/**
 * Independent model of the reference command/save environment's measured sky
 * attenuation. New absolute-height experiments distinguish a 16-block light
 * section boundary from a fixed panel-distance cutoff (16/16 versus 14/16).
 * Water costs 3, leaves 1; after attenuation begins, intervening transparent
 * cells cost at least 1. These are observed input/output rules, not an IC2
 * implementation. Player lighting updates and arbitrary old worlds remain
 * separate validation scopes.
 */
public final class SolarColumnLight {
    private SolarColumnLight() { }
    public static int sample(int sampleY, int maximumBuildHeight, IntUnaryOperator opacityAtY) {
        Objects.requireNonNull(opacityAtY, "opacityAtY");
        if (sampleY >= maximumBuildHeight) return 15;
        int top = Math.min(maximumBuildHeight - 1, Math.floorDiv(sampleY, 16) * 16 + 15);
        int light = 15;
        for (int offset = 0; offset <= top - sampleY && light > 0; offset++) {
            int opacity = opacityAtY.applyAsInt(top - offset);
            if (opacity < 0 || opacity > 15) throw new IllegalArgumentException("Invalid sky opacity");
            light = Math.max(0, light - (light == 15 ? opacity : Math.max(1, opacity)));
        }
        return light;
    }
}
