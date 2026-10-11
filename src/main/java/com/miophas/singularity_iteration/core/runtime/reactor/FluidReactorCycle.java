// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.reactor;

/** Ordinary R119 tank and heat observations, including odd remaining millibuckets. */
public final class FluidReactorCycle {
    private FluidReactorCycle(){}
    /**
     * IC2 {@code balance/energyfix/FluidReactor/outputModifier} has the default 1.0 → huOutputModifier = 40.
     * The emitted heat is first multiplied by this factor. Then the coolant huPerMB converts it to the output quantity.
     */
    public static final int HU_OUTPUT_MODIFIER=40;
    /** IC2 coolant heat exchange default {@code heatExchangerHotCoolant = 1.0} → round(20 × 1.0) = 20 HU/mB. */
    public static final int COOLANT_HU_PER_MB=20;
    /** IC2 water heat exchange default {@code heatExchangerWater = 1.0} → round(1.0 × 1.0) = 1 HU/mB. */
    public static final int WATER_HU_PER_MB=1;
    public record Conversion(int millibuckets,long returnedHeat){}
    public record Result(ReactorCycle.Result cycle,int coolant,int hotCoolant,int converted,long returnedHeat){}
    /** Converts with the coolant rate by default (20 HU/mB). */
    public static Conversion convert(long emittedHeat,int coolant,int hotCoolant,int capacity){
        return convert(emittedHeat,coolant,hotCoolant,capacity,COOLANT_HU_PER_MB);
    }
    /**
     * Equivalent to the IC2 fluid reactor conversion:
     * <pre>
     *   huOutput     = huOutputModifier × emitted heat
     *   output (mB)  = huOutput / huPerMB          (also limited by the free space of the output tank and the input coolant quantity)
     *   returned heat = (huOutput - output × huPerMB) / huOutputModifier
     * </pre>
     * When there is no input coolant, the output is 0 and all emitted heat goes back to the hull heat. This agrees with the IC2
     * behavior in the {@code inputFluid == null} branch.
     */
    public static Conversion convert(long emittedHeat,int coolant,int hotCoolant,int capacity,int huPerMB){
        return convert(emittedHeat,coolant,hotCoolant,capacity,huPerMB,true);
    }
    /** Incompatible hot fluid blocks conversion, but does not stop the reactor cycle. */
    public static Conversion convert(long emittedHeat,int coolant,int hotCoolant,int capacity,int huPerMB,boolean outputCompatible){
        if(emittedHeat<0||coolant<0||hotCoolant<0||capacity<0||coolant>capacity||hotCoolant>capacity)
            throw new IllegalArgumentException("Invalid fluid reactor state");
        if(huPerMB<=0)throw new IllegalArgumentException("Invalid heat exchange rate");
        if(!outputCompatible)return new Conversion(0,emittedHeat);
        long available=Math.min(coolant,capacity-hotCoolant);
        long huOutput=Math.multiplyExact(emittedHeat,HU_OUTPUT_MODIFIER);
        int converted=(int)Math.min(huOutput/huPerMB,available);
        long leftover=huOutput-Math.multiplyExact((long)converted,huPerMB);
        return new Conversion(converted,leftover/HU_OUTPUT_MODIFIER);
    }
    public static Result step(ReactorCycle.Part[] parts,int columns,long heat,boolean enabled,int coolant,int hotCoolant,int capacity,int huPerMB){
        return step(parts,columns,heat,enabled,coolant,hotCoolant,capacity,huPerMB,true);
    }
    public static Result step(ReactorCycle.Part[] parts,int columns,long heat,boolean enabled,int coolant,int hotCoolant,int capacity,int huPerMB,boolean outputCompatible){
        var raw=ReactorCycle.step(parts,columns,heat,enabled,true);
        var converted=convert(raw.emittedHeat(),coolant,hotCoolant,capacity,huPerMB,outputCompatible);
        var next=new ReactorCycle.Result(raw.parts(),Math.addExact(raw.hullHeat(),converted.returnedHeat()),raw.maxHullHeat(),
                raw.emittedHeat(),raw.generatedHeat(),0,raw.depletedFuel());
        return new Result(next,coolant-converted.millibuckets(),hotCoolant+converted.millibuckets(),converted.millibuckets(),converted.returnedHeat());
    }
}
