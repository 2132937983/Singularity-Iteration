// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.radiation;

/** Local ground-based fallout fog. A contaminated column cannot fog clean terrain 64 blocks away. */
public final class FalloutVisualRange {
    public static final int HORIZONTAL=2,VERTICAL=16;
    private FalloutVisualRange(){}
    public static float intensity(double dx,double dy,double dz){
        if(!Double.isFinite(dx)||!Double.isFinite(dy)||!Double.isFinite(dz))return 0;
        double horizontal=Math.sqrt(dx*dx+dz*dz);
        return (float)(Math.max(0,1-horizontal/HORIZONTAL)*Math.max(0,1-Math.abs(dy)/VERTICAL));
    }
}
