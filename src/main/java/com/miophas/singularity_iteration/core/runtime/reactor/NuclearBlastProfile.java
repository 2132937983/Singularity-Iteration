// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.reactor;

/** Independent bomb yield scaling; reactor accident profiles use their own multiplier. */
public record NuclearBlastProfile(float power,int radius){
    public static NuclearBlastProfile calculate(float basePower,double multiplier){
        if(!Float.isFinite(basePower)||basePower<0||!Double.isFinite(multiplier)||multiplier<0||multiplier>16)
            throw new IllegalArgumentException("Invalid nuclear bomb strength");
        float power=(float)Math.min(1_000_000,basePower*multiplier);
        int radius=power<=0?0:Math.max(20,Math.min((int)Math.ceil(20+power/25.0),2000));
        return new NuclearBlastProfile(power,radius);
    }
    public boolean disabled(){return power<=0;}
}
