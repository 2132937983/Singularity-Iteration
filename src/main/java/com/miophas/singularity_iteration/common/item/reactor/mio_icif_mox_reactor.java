// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.item.reactor;
import java.util.function.Supplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** MOX metadata and finite public-call compatibility; actual world behavior uses R117/R119 ReactorCycle. */
public class mio_icif_mox_reactor extends mio_icif_nuclear_reactor {
    public mio_icif_mox_reactor(Properties p,int uses,int energy,int heat,FuelRodType type){super(p,uses,energy,heat,type);}
    public mio_icif_mox_reactor(Properties p,int uses,int energy,int heat,FuelRodType type,Supplier<Item> depleted){super(p,uses,energy,heat,type,depleted);}
    @Override public boolean isMoxFuel(){return true;}
    private float ratio(float value){if(!Float.isFinite(value)||value<0)throw new IllegalArgumentException("Invalid hull heat ratio");return value;}
    public int getMoxEnergyOutput(ItemStack stack,int pulses,float ratio){if(isDepleted(stack))return 0;int base=Math.multiplyExact(getEnergyOutput(),Math.addExact(getBaseSelfPulses(),pulses));double energy=base*(1+4*ratio(ratio));if(energy>Integer.MAX_VALUE)throw new ArithmeticException("MOX output overflow");return (int)energy;}
    public int getMoxHeatOutput(ItemStack stack,int pulses,float ratio,boolean fluid){ratio(ratio);return isDepleted(stack)?0:Math.multiplyExact(calculateHeatOutput(pulses),fluid&&ratio>.5f?2:1);}
    public OperationResult operateMox(ItemStack stack,int pulses,int reflected,float ratio){int energy=getMoxEnergyOutput(stack,pulses,ratio);int heat=calculateHeatOutput(pulses(pulses,reflected));if(!advanceLegacyTick(stack))return new OperationResult(0,0,true);return new OperationResult(energy,heat,isDepleted(stack));}
    public OperationResult getMoxHeatOutputForFluid(ItemStack stack,int pulses,int reflected,float ratio){int heat=Math.multiplyExact(2,getMoxHeatOutput(stack,pulses(pulses,reflected),ratio,true));if(!advanceLegacyTick(stack))return new OperationResult(0,0,true);return new OperationResult(0,heat,isDepleted(stack));}
}
