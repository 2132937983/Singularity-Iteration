// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.item.reactor;
import com.miophas.singularity_iteration.common.registry.mio_icif_data_components;
import com.miophas.singularity_iteration.core.api.reactor.ReactorComponentType;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** SI item identity and legacy public calls. The authoritative world cycle is ReactorCycle. */
public class mio_icif_nuclear_reactor extends mio_icif_reactor {
    public static final int OPERATION_INTERVAL=20;
    private final int baseEnergy,baseHeat;private final FuelRodType rodType;private final boolean reflector;private final Supplier<Item> depleted;
    public mio_icif_nuclear_reactor(Properties p,int uses,int energy,int heat,FuelRodType type){this(p,uses,energy,heat,type,false,null);}
    public mio_icif_nuclear_reactor(Properties p,int uses,int energy,int heat,FuelRodType type,Supplier<Item> depleted){this(p,uses,energy,heat,type,false,depleted);}
    public mio_icif_nuclear_reactor(Properties p,int uses,int energy,int heat,FuelRodType type,boolean reflector){this(p,uses,energy,heat,type,reflector,null);}
    public mio_icif_nuclear_reactor(Properties p,int uses,int energy,int heat,FuelRodType type,boolean reflector,Supplier<Item> depleted){
        super(p,uses,reflector?ReactorComponentType.NEUTRON_REFLECTOR:ReactorComponentType.FUEL_ROD,true,false);
        if(uses<=0||energy<0||heat<0)throw new IllegalArgumentException("Invalid fuel profile");
        baseEnergy=energy;baseHeat=heat;rodType=Objects.requireNonNull(type);this.reflector=reflector;this.depleted=depleted;
    }
    public int getEnergyOutput(){return baseEnergy;}
    public int getActualEnergyOutput(){return Math.multiplyExact(baseEnergy,getNumberOfCells());}
    @Override public int getHeatOutput(){return baseHeat;}
    public int getActualHeatOutput(){return Math.multiplyExact(baseHeat,getNumberOfCells());}
    public FuelRodType getRodType(){return rodType;}
    @Override public boolean isNeutronReflector(){return reflector;}
    @Override public Item getDepletedItem(){return depleted==null?null:depleted.get();}
    public int getNeutronPulseCount(){return rodType.getNeutronPulseCount();}
    @Override public int getNeutronPulseOutput(){return getNeutronPulseCount();}
    public int getBaseEnergyMultiplier(){return rodType.getBaseEnergyMultiplier();}
    public int getBaseSelfPulses(){return rodType.getRange();}
    @Override public int getNumberOfCells(){return rodType.getEfficiencyMultiplier();}
    @Override public boolean canReceiveNeutronPulse(){return true;}
    /** R121 public-call convention: each legacy call advances a 20-call durability clock. */
    protected boolean advanceLegacyTick(ItemStack stack){
        if(stack.isEmpty())return false;var d=stack.get(mio_icif_data_components.FUEL_ROD_DURABILITY.get());
        if(d==null||d.maxUses()<=0||d.remainingUses()<=0||d.remainingUses()>d.maxUses()||d.tickCounter()<0||d.tickCounter()>=20)return false;
        int tick=d.tickCounter()+1;stack.set(mio_icif_data_components.FUEL_ROD_DURABILITY.get(),new com.miophas.singularity_iteration.common.item.component.FuelRodDurability(d.remainingUses()-(tick==20?1:0),d.maxUses(),tick%20));return true;
    }
    protected int pulses(int a,int b){if(a<0||b<0)throw new IllegalArgumentException("Negative pulse count");return Math.addExact(a,b);}
    public int calculateHeatOutput(int count){if(count<0)throw new IllegalArgumentException("Negative pulse count");return Math.toIntExact(Math.multiplyExact((long)baseHeat,Math.multiplyExact((long)count,count+1L))/2);}
    public OperationResult getHeatOutput(ItemStack stack,int pulses,int reflected){int heat=Math.multiplyExact(2,calculateHeatOutput(pulses(pulses,reflected)));if(!advanceLegacyTick(stack))return new OperationResult(0,0,true);return new OperationResult(0,heat,isDepleted(stack));}
    public OperationResult operate(ItemStack stack,int pulses,int reflected){int heat=calculateHeatOutput(pulses(pulses,reflected));int eu=Math.multiplyExact(baseEnergy,Math.addExact(getBaseEnergyMultiplier(),pulses));if(!advanceLegacyTick(stack))return new OperationResult(0,0,true);return new OperationResult(eu,heat,isDepleted(stack));}
    public boolean acceptNeutronPulse(ItemStack stack,AtomicInteger energy,boolean heatPhase){if(isDepleted(stack))return false;if(!heatPhase)energy.updateAndGet(n->Math.addExact(n,baseEnergy));return true;}
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag){super.appendHoverText(stack,context,lines,flag);lines.add(Component.literal(getCurrentDurability(stack)+" reactor cycles; "+getNumberOfCells()+" cells"));}
    public enum FuelRodType {
        SINGLE(1,1),DUAL(2,2),QUAD(4,3);
        private final int cells,self;
        FuelRodType(int cells,int self){this.cells=cells;this.self=self;}
        public int getEfficiencyMultiplier(){return cells;}
        public int getRange(){return self;}
        public int getNeutronPulseCount(){return cells;}
        public int getBaseEnergyMultiplier(){return cells*self;}
    }
    public static class OperationResult {
        public final int energyProduced,heatProduced;public final boolean depleted;
        public OperationResult(int energy,int heat,boolean depleted){energyProduced=energy;heatProduced=heat;this.depleted=depleted;}
    }
}
