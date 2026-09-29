// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.item.reactor;
import com.miophas.singularity_iteration.common.item.component.ReactorComponentData;
import com.miophas.singularity_iteration.common.registry.mio_icif_data_components;
import com.miophas.singularity_iteration.core.api.reactor.ReactorComponentType;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Independent item operations consistent with the R116/R117 observed cooling and melt boundaries. */
public class mio_icif_heat_vent extends mio_icif_reactor {
    public static final int OPERATION_INTERVAL=20;
    private final int capacity,selfCooling,hullCooling;
    public mio_icif_heat_vent(Properties properties,int capacity,int selfCooling,int hullCooling){
        super(properties,capacity,ReactorComponentType.HEAT_SINK,false,true);
        if(capacity<0||selfCooling<0||hullCooling<0||capacity==0&&(selfCooling>0||hullCooling>0))throw new IllegalArgumentException("Invalid vent profile");
        this.capacity=capacity;this.selfCooling=selfCooling;this.hullCooling=hullCooling;
    }
    @Override public int getStoredHeat(ItemStack stack){var d=stack.get(mio_icif_data_components.REACTOR_COMPONENT_DATA.get());return d==null?0:d.storedValue();}
    @Override public void setStoredHeat(ItemStack stack,int heat){
        if(stack.isEmpty()||capacity==0)return;
        if(heat>capacity){stack.shrink(1);return;}
        stack.set(mio_icif_data_components.REACTOR_COMPONENT_DATA.get(),new ReactorComponentData(Math.max(0,heat),capacity));
    }
    /** Melt takes the component's heat with it. Exact capacity survives, as measured in R117. */
    public int addHeat(ItemStack stack,int amount){if(amount<=0)return 0;if(stack.isEmpty()||capacity==0)return amount;long next=(long)getStoredHeat(stack)+amount;if(next>capacity)stack.shrink(1);else setStoredHeat(stack,(int)next);return 0;}
    public int removeHeat(ItemStack stack,int amount){int removed=Math.min(Math.max(0,amount),getStoredHeat(stack));if(!stack.isEmpty()&&removed>0)setStoredHeat(stack,getStoredHeat(stack)-removed);return removed;}
    public int getSelfCoolingRate(){return selfCooling;}
    public int getReactorHeatAbsorptionRate(){return hullCooling;}
    /** One reactor operation, returning the hull debit and accumulating actual emitted heat. */
    public int cool(ItemStack stack,long reactorHeat,AtomicInteger emitted){
        if(stack.isEmpty())return 0;int drawn=(int)Math.min(Math.max(0,reactorHeat),hullCooling);addHeat(stack,drawn);
        if(!stack.isEmpty())emitted.addAndGet(removeHeat(stack,selfCooling));return drawn;
    }
    @Override public int absorbHeat(ItemStack stack,int amount){return Math.max(0,amount)-addHeat(stack,amount);}
    @Override public int getMaxHeatStorage(){return capacity;}
    @Override public boolean isMelted(ItemStack stack){return stack.isEmpty()||getStoredHeat(stack)>capacity;}
    @Override public boolean isDepleted(ItemStack stack){return isMelted(stack);}
    public int getRemainingHeatCapacity(ItemStack stack){return stack.isEmpty()?0:Math.max(0,capacity-getStoredHeat(stack));}
    @Override public int getBarColor(ItemStack stack){return getStoredHeat(stack)*2L>=capacity?0xFF5555:0x55AAFF;}
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag){super.appendHoverText(stack,context,lines,flag);lines.add(Component.literal(getStoredHeat(stack)+" / "+capacity+" HU; "+selfCooling+" HU / 20 ticks"));}
}
