// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.reactor;

import com.miophas.singularity_iteration.common.item.component.FuelRodDurability;
import com.miophas.singularity_iteration.common.item.component.ReactorComponentData;
import com.miophas.singularity_iteration.common.registry.mio_icif_data_components;
import com.miophas.singularity_iteration.core.runtime.reactor.ReactorCycle.Kind;
import com.miophas.singularity_iteration.core.runtime.reactor.ReactorCycle.Part;
import com.miophas.singularity_iteration.core.runtime.reactor.ReactorCycle.Profile;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** SI identities are frozen public registry/save observations; numerical profiles are R116/R117 references. */
public final class ReactorInventory {
    private ReactorInventory(){}
    private static final Profile INERT=new Profile(Kind.INERT,0,false,0,0,0,0,0,0,0);
    /** IC2 ItemReactorHeatpack(1000, 1)。 */
    public static final int HEATPACK_MAX_PER=1000,HEATPACK_HEAT_PER=1;
    /** IC2 ItemReactorLithiumCell 的增殖阈值。 */
    public static final int LITHIUM_THRESHOLD=10000;
    /** 锂单元增殖完成后的产物（IC2 tritium_fuel_rod）。 */
    public static final ResourceLocation LITHIUM_PRODUCT=ResourceLocation.parse("mio_icif:normal/item_tritium_cell");
    /** IC2 ItemReactorDepletedUranium（近衰变同位素棒）的增殖阈值。 */
    public static final int ISOTOPE_THRESHOLD=10000;
    /** IC2 ItemReactorDepletedUranium 每个脉冲的固定增量（IC2 的 +1；锂单元没有这一项）。 */
    public static final int ISOTOPE_PULSE_BONUS=1;
    /** 近衰变同位素棒增殖完成后的产物（IC2 nuclear#re_enriched_uranium）。 */
    public static final ResourceLocation ISOTOPE_PRODUCT=ResourceLocation.parse("mio_icif:normal/item_re_enriched_uranium");
    private static final Map<String,Profile> PROFILES=Map.ofEntries(
        Map.entry("uranium_simple",Profile.fuel(1,false)),Map.entry("uranium_dual",Profile.fuel(2,false)),Map.entry("uranium_quad",Profile.fuel(4,false)),
        Map.entry("mox_simple",Profile.fuel(1,true)),Map.entry("mox_dual",Profile.fuel(2,true)),Map.entry("mox_quad",Profile.fuel(4,true)),
        Map.entry("collant_simple",Profile.cell(10000)),Map.entry("collant_triple",Profile.cell(30000)),Map.entry("coolant_six",Profile.cell(60000)),
        Map.entry("condensator",Profile.condensator(20000)),Map.entry("condensator_lap",Profile.condensator(100000)),
        Map.entry("vent",Profile.vent(1000,6,0)),Map.entry("vent_core",Profile.vent(1000,5,5)),
        Map.entry("golden_vent",Profile.vent(1000,20,36)),Map.entry("diamond_vent",Profile.vent(1000,12,0)),
        Map.entry("vent_spread",Profile.componentVent()),
        // Explicit SI extensions, observed through public item metadata; no original equivalence claimed.
        Map.entry("iridium_vent",Profile.vent(4000,36,0)),Map.entry("iridium_oc_vent",Profile.vent(4000,56,72)),
        Map.entry("heat_switch",Profile.exchanger(2500,12,4)),Map.entry("heat_switch_core",Profile.exchanger(5000,0,72)),
        Map.entry("heat_switch_spread",Profile.exchanger(5000,36,0)),Map.entry("diamond_heat_switch",Profile.exchanger(10000,24,8)),
        // IC2 隔板：plating 1000/0.95、heat_plating 2000/0.99、containment_plating 500/0.9
        Map.entry("plate",Profile.plating(1000,5)),Map.entry("heat_plate",Profile.plating(2000,1)),Map.entry("explosive_plate",Profile.plating(500,10)),
        Map.entry("reflector",Profile.reflector(30000)),Map.entry("thick_reflector",Profile.reflector(120000)),
        // IC2 铱中子反射板为无限耐久
        Map.entry("iridium_reflector",Profile.reflector(0)),
        Map.entry("heatpack",Profile.heatpack(HEATPACK_MAX_PER,HEATPACK_HEAT_PER)),
        Map.entry("lithium_cell",Profile.lithium(LITHIUM_THRESHOLD)),
        Map.entry("isotope_rod",Profile.isotope(ISOTOPE_THRESHOLD,ISOTOPE_PULSE_BONUS)),
        Map.entry("uranium_simple_depleted",INERT),Map.entry("uranium_dual_depleted",INERT),Map.entry("uranium_quad_deplete",INERT),
        Map.entry("mox_simple_depleted",INERT),Map.entry("mox_dual_deplete",INERT),Map.entry("mox_quad_deplete",INERT));
    private static String key(ItemStack stack){var id=BuiltInRegistries.ITEM.getKey(stack.getItem());String prefix="reactor/item_reactor_";
        return id.getNamespace().equals("mio_icif")&&id.getPath().startsWith(prefix)?id.getPath().substring(prefix.length()):"";}
    public static boolean supported(ItemStack stack){return stack.isEmpty()||PROFILES.containsKey(key(stack));}
    public static Part read(ItemStack stack){
        if(stack.isEmpty())return null;
        if(stack.getCount()!=1)throw new IllegalArgumentException("Reactor slots require exactly one item");
        Profile p=PROFILES.get(key(stack));if(p==null)throw new IllegalArgumentException("Unsupported reactor component: "+BuiltInRegistries.ITEM.getKey(stack.getItem()));
        if(p.kind()==Kind.FUEL){
            var d=stack.get(mio_icif_data_components.FUEL_ROD_DURABILITY.get());
            if(d==null||d.maxUses()<=0||d.remainingUses()<0||d.remainingUses()>d.maxUses()||d.tickCounter()<0||d.tickCounter()>=20)
                throw new IllegalArgumentException("Invalid fuel durability retained");
            return new Part(p,0,d.remainingUses());
        }
        if(p.storesHeat()||p.kind()==Kind.REFLECTOR||p.breeding()){
            var d=stack.get(mio_icif_data_components.REACTOR_COMPONENT_DATA.get());
            if(d==null||d.maxValue()!=p.capacity()||d.storedValue()<0||d.storedValue()>d.maxValue())
                throw new IllegalArgumentException("Unknown thermal component state retained");
            // 只有有限耐久的反射器会用尽；capacity==0（铱反射板）永远不视为枯竭。
            if(p.kind()==Kind.REFLECTOR&&p.capacity()>0&&d.storedValue()==d.maxValue())return new Part(INERT,0,0);
            return new Part(p,d.storedValue(),0);
        }
        return new Part(p,0,0);
    }
    public static ItemStack write(ItemStack previous,Part before,Part next,boolean depleted){
        if(before==null)return previous.copy();
        if(next==null)return ItemStack.EMPTY;
        if(before.profile().kind()==Kind.INERT)return previous.copy();
        ItemStack out=previous.copy();
        if(depleted){
            String target=switch(key(previous)){
                case "uranium_simple"->"uranium_simple_depleted";case "uranium_dual"->"uranium_dual_depleted";case "uranium_quad"->"uranium_quad_deplete";
                case "mox_simple"->"mox_simple_depleted";case "mox_dual"->"mox_dual_deplete";case "mox_quad"->"mox_quad_deplete";
                default->throw new IllegalArgumentException("No depleted identity");};
            var item=BuiltInRegistries.ITEM.get(ResourceLocation.parse("mio_icif:reactor/item_reactor_"+target));
            out=new ItemStack(item);out.applyComponents(previous.getComponentsPatch());out.remove(mio_icif_data_components.FUEL_ROD_DURABILITY.get());
        }else if(next.profile().kind()==Kind.FUEL){
            FuelRodDurability old=previous.get(mio_icif_data_components.FUEL_ROD_DURABILITY.get());
            // Preserve legacy remaining cycle counts and their historical maximum; never refill old fuel.
            out.set(mio_icif_data_components.FUEL_ROD_DURABILITY.get(),new FuelRodDurability(next.remaining(),old.maxUses(),old.tickCounter()));
        }else if(next.profile().breeding()){
            // IC2：锂单元累积到阈值后变成氚燃料棒；近衰变同位素棒累积到阈值后变成重新浓缩的铀
            if(next.profile().capacity()>0&&next.stored()>=next.profile().capacity()){
                ItemStack product=new ItemStack(BuiltInRegistries.ITEM.get(
                    next.profile().kind()==Kind.LITHIUM?LITHIUM_PRODUCT:ISOTOPE_PRODUCT));
                product.applyComponents(previous.getComponentsPatch());
                product.remove(mio_icif_data_components.REACTOR_COMPONENT_DATA.get());
                return product;
            }
            out.set(mio_icif_data_components.REACTOR_COMPONENT_DATA.get(),new ReactorComponentData(next.stored(),next.profile().capacity()));
        }else if(next.profile().storesHeat()||next.profile().kind()==Kind.REFLECTOR){
            out.set(mio_icif_data_components.REACTOR_COMPONENT_DATA.get(),new ReactorComponentData(next.stored(),next.profile().capacity()));
        }
        return out;
    }
}
