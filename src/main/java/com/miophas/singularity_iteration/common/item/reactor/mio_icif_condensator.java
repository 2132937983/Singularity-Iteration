// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.item.reactor;
import com.miophas.singularity_iteration.common.item.component.ReactorComponentData;
import com.miophas.singularity_iteration.common.registry.mio_icif_data_components;
import com.miophas.singularity_iteration.core.api.reactor.ReactorComponentType;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Saturating storage from normal R116 observations; repair amounts are SI public constructor contracts. */
public class mio_icif_condensator extends mio_icif_reactor {
    private final int capacity,absorption,redstoneRepair,lapisRepair;
    public mio_icif_condensator(Properties properties,int capacity,int absorption,int redstoneRepair,int lapisRepair){
        super(properties,capacity,ReactorComponentType.CONDENSATOR,false,true);
        if(capacity<=0||absorption<0||redstoneRepair<0||lapisRepair<0)throw new IllegalArgumentException("Invalid condensator profile");
        this.capacity=capacity;this.absorption=absorption;this.redstoneRepair=redstoneRepair;this.lapisRepair=lapisRepair;
    }
    @Override public int getStoredHeat(ItemStack stack){var d=stack.get(mio_icif_data_components.REACTOR_COMPONENT_DATA.get());return d==null?0:d.storedValue();}
    @Override public void setStoredHeat(ItemStack stack,int heat){if(!stack.isEmpty())stack.set(mio_icif_data_components.REACTOR_COMPONENT_DATA.get(),new ReactorComponentData(Math.clamp(heat,0,capacity),capacity));}
    /** Legacy public return value is the unaccepted remainder, confirmed by R121 calls. */
    public int addHeat(ItemStack stack,int amount){if(amount<=0)return 0;if(stack.isEmpty())return amount;int accepted=Math.min(amount,Math.max(0,capacity-getStoredHeat(stack)));setStoredHeat(stack,getStoredHeat(stack)+accepted);return amount-accepted;}
    public int removeHeat(ItemStack stack,int amount){int removed=Math.min(Math.max(0,amount),getStoredHeat(stack));if(removed>0)setStoredHeat(stack,getStoredHeat(stack)-removed);return removed;}
    @Override public int absorbHeat(ItemStack stack,int amount){return Math.max(0,amount)-addHeat(stack,amount);}
    public boolean repairWithRedstone(ItemStack stack){return removeHeat(stack,redstoneRepair)>0;}
    public boolean repairWithLapis(ItemStack stack){return removeHeat(stack,lapisRepair)>0;}
    public int getRedstoneRepairAmount(){return redstoneRepair;}
    public int getLapisRepairAmount(){return lapisRepair;}
    public int getHeatAbsorptionRate(){return absorption;}
    @Override public int getMaxHeatStorage(){return capacity;}
    public boolean isFull(ItemStack stack){return getStoredHeat(stack)>=capacity;}
    @Override public boolean isMelted(ItemStack stack){return stack.isEmpty();}
    @Override public boolean isBarVisible(ItemStack stack){return getStoredHeat(stack)>0;}
    @Override public int getBarColor(ItemStack stack){return isFull(stack)?0xFF5555:0xFFAA00;}
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag){super.appendHoverText(stack,context,lines,flag);lines.add(Component.literal(getStoredHeat(stack)+" / "+capacity+" HU"));}
}
