// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.reactor;

import com.miophas.singularity_iteration.common.item.component.FuelRodDurability;
import com.miophas.singularity_iteration.common.item.component.ReactorComponentData;
import com.miophas.singularity_iteration.common.registry.mio_icif_data_components;
import com.miophas.singularity_iteration.core.api.reactor.ReactorTransformation;
import com.miophas.singularity_iteration.core.runtime.reactor.ReactorCycle.Kind;
import com.miophas.singularity_iteration.core.runtime.reactor.ReactorCycle.Part;
import com.miophas.singularity_iteration.core.runtime.reactor.ReactorCycle.Profile;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
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
    /** 0.1.10.36: lithium breeding: +1 progress per pulse besides hull/3000 (IC2 experimental), 1 HU per absorbed pulse. */
    public static final int LITHIUM_PULSE_BONUS=1,LITHIUM_HEAT_PER_PULSE=1;
    /** 0.1.10.36: the tritium rod is a neutron source: it decays in TRITIUM_LIFE cycles and gives 1 HU of decay heat per cycle. */
    public static final int TRITIUM_LIFE=10000,TRITIUM_HEAT_PER_CYCLE=1;
    /** 0.1.10.36: a decayed tritium rod leaves the empty fuel rod it was canned in (the helium-3 escapes). */
    public static final ResourceLocation TRITIUM_PRODUCT=ResourceLocation.parse("mio_icif:normal/item_fuel_rod");
    /**
     * 0.1.10.36: the proton rod (Industrial Upgrade style): 1.5x the heat of uranium, 40000 cycles. 0.1.10.38: like MOX, its EU
     * rises with the hull heat (x1 cold to x5 at full heat; it was a flat 3x), and in a fluid-mode reactor (the fluid and the
     * steam reactor) above half heat its heat doubles. 0.1.10.42: 30 EU per pulse cold (6x uranium's 5), so 30 to 150 EU per
     * pulse with the hull heat; the octuple twice that.
     */
    public static final float PROTON_EU=6.0f,PROTON_HEAT=1.5f;
    public static final int PROTON_LIFE=40000;
    /** 0.1.10.36: heat capacity of the IC2C steam vents. */
    public static final int STEAM_VENT_HEAT=1100;
    /** 0.1.10.36: the tritium rod keeps its item id (normal/item_tritium_cell); its component key is "tritium_cell". */
    private static final String TRITIUM_KEY="tritium_cell";
    /**
     * 0.1.10.37: what each breeding or decaying component turns into (lithium cell -> tritium rod, isotope rod -> re-enriched
     * uranium, tritium rod -> empty fuel rod). One table for the reactor ({@link #write}) and for recipe viewers
     * ({@link #transformations}), in display order.
     */
    private static final Map<String,ResourceLocation> PRODUCTS=new java.util.LinkedHashMap<>();
    static{PRODUCTS.put("lithium_cell",LITHIUM_PRODUCT);PRODUCTS.put("isotope_rod",ISOTOPE_PRODUCT);PRODUCTS.put(TRITIUM_KEY,TRITIUM_PRODUCT);}
    /** 0.1.10.36: the spent identity of each fuel rod (was a switch). */
    private static final Map<String,String> DEPLETED=Map.of(
        "uranium_simple","uranium_simple_depleted","uranium_dual","uranium_dual_depleted","uranium_quad","uranium_quad_deplete",
        "mox_simple","mox_simple_depleted","mox_dual","mox_dual_deplete","mox_quad","mox_quad_deplete",
        "proton_simple","proton_simple_depleted","proton_dual","proton_dual_depleted","proton_quad","proton_quad_depleted",
        "proton_octuple","proton_octuple_depleted");
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
        Map.entry("lithium_cell",Profile.lithium(LITHIUM_THRESHOLD,LITHIUM_PULSE_BONUS,LITHIUM_HEAT_PER_PULSE)),
        Map.entry("isotope_rod",Profile.isotope(ISOTOPE_THRESHOLD,ISOTOPE_PULSE_BONUS)),
        Map.entry(TRITIUM_KEY,Profile.neutronSource(TRITIUM_LIFE,TRITIUM_HEAT_PER_CYCLE)),
        Map.entry("proton_simple",Profile.fuel(1,true,PROTON_EU,PROTON_HEAT)),Map.entry("proton_dual",Profile.fuel(2,true,PROTON_EU,PROTON_HEAT)),
        Map.entry("proton_quad",Profile.fuel(4,true,PROTON_EU,PROTON_HEAT)),
        // 0.1.10.38: two quads in one slot: a quad's pulses with twice its EU and heat
        Map.entry("proton_octuple",Profile.fuel(4,true,2*PROTON_EU,2*PROTON_HEAT)),
        Map.entry("steam_vent",Profile.steamVent(STEAM_VENT_HEAT,12,0)),Map.entry("steam_vent_core",Profile.steamVent(STEAM_VENT_HEAT,10,10)),
        Map.entry("steam_vent_overclocked",Profile.steamVent(STEAM_VENT_HEAT,40,72)),Map.entry("steam_vent_advanced",Profile.steamVent(STEAM_VENT_HEAT,24,0)),
        Map.entry("uranium_simple_depleted",INERT),Map.entry("uranium_dual_depleted",INERT),Map.entry("uranium_quad_deplete",INERT),
        Map.entry("mox_simple_depleted",INERT),Map.entry("mox_dual_deplete",INERT),Map.entry("mox_quad_deplete",INERT),
        Map.entry("proton_simple_depleted",INERT),Map.entry("proton_dual_depleted",INERT),Map.entry("proton_quad_depleted",INERT),
        Map.entry("proton_octuple_depleted",INERT));
    /**
     * Components of the reactor simulator palette (0.1.8.0), in display order: fuel, heat
     * storage, vents, exchangers, plating, reflectors, special. Depleted rods are not in it.
     */
    public static final java.util.List<String> PALETTE=java.util.List.of(
        "uranium_simple","uranium_dual","uranium_quad","mox_simple","mox_dual","mox_quad",
        "collant_simple","collant_triple","coolant_six","condensator","condensator_lap",
        "vent","vent_core","golden_vent","diamond_vent","vent_spread","iridium_vent","iridium_oc_vent",
        "heat_switch","heat_switch_core","heat_switch_spread","diamond_heat_switch",
        "plate","heat_plate","explosive_plate","reflector","thick_reflector","iridium_reflector",
        // 0.1.10.36: proton fuel, the lithium breeder and the tritium neutron source
        "proton_simple","proton_dual","proton_quad","lithium_cell",TRITIUM_KEY,
        // 0.1.10.38
        "proton_octuple");
    /** The numeric profile of component {@code key}, or null. */
    public static Profile profile(String key){Profile p=PROFILES.get(key);return p==null||p.kind()==Kind.INERT?null:p;}
    /** Item id of component {@code key}. */
    public static ResourceLocation itemId(String key){
        return key.equals(TRITIUM_KEY)?LITHIUM_PRODUCT:ResourceLocation.fromNamespaceAndPath("mio_icif","reactor/item_reactor_"+key);}
    /** 0.1.10.42: the life of the fuel rod whose profile is {@code profile} (for the simulator); 20000 for a profile of no rod. */
    static int fuelLife(Profile profile){
        for(var e:PROFILES.entrySet())if(e.getValue()==profile&&e.getValue().kind()==Kind.FUEL)return fuelLife(e.getKey());
        return 20000;}
    /** Cycles of a fresh fuel rod of component {@code key} (0: not a fuel rod). */
    public static int fuelLife(String key){
        Profile p=PROFILES.get(key);if(p==null||p.kind()!=Kind.FUEL)return 0;
        return key.startsWith("mox_")?10000:key.startsWith("proton_")?PROTON_LIFE:20000;}
    private static String key(ItemStack stack){return key(BuiltInRegistries.ITEM.getKey(stack.getItem()));}
    private static String key(ResourceLocation id){String prefix="reactor/item_reactor_";
        if(id.equals(LITHIUM_PRODUCT))return TRITIUM_KEY;
        return id.getNamespace().equals("mio_icif")&&id.getPath().startsWith(prefix)?id.getPath().substring(prefix.length()):"";}
    public static boolean supported(ItemStack stack){return stack.isEmpty()||PROFILES.containsKey(key(stack));}
    /**
     * A product of breeding or decay without a profile of its own (re-enriched uranium, the empty fuel rod of a decayed tritium
     * rod): an inert part until it leaves the reactor. 0.1.10.37: every product of {@link #PRODUCTS}; the empty fuel rod was
     * missing, so the cycle that decayed a tritium rod threw, the rod stayed at 9999 and the reactor stopped for good.
     */
    private static boolean product(ItemStack stack){return PRODUCTS.containsValue(BuiltInRegistries.ITEM.getKey(stack.getItem()));}
    /**
     * Items that are not reactor components but stay in their slot: the bred tritium rod, and (0.1.10.38) every depleted rod and
     * every product of breeding or decay (re-enriched uranium, the empty fuel rod). Before, these were thrown out at the reactor's
     * own position, where a reactor with six chambers or the 5x5x5 fluid reactor lost them inside solid blocks (and nothing
     * dropped with doTileDrops off). They stay until a player or automation takes them out.
     */
    public static boolean staysInReactor(ItemStack stack){return !stack.isEmpty()&&(PROFILES.containsKey(key(stack))||product(stack));}
    /**
     * 0.1.10.37: the component changes of this reactor for the JEI category "Nuclear Fuel Breeding & Decay"
     * ({@code core.api.reactor.ReactorTransformationRegistry}), from the tables the reactor runs on: the fuel rods burning out
     * ({@link #DEPLETED}, in palette order), then the breeders and the tritium rod ({@link #PRODUCTS}). The notes come from the
     * profiles, so they follow any change of the numbers.
     */
    public static List<ReactorTransformation> transformations(){
        var out=new ArrayList<ReactorTransformation>();
        var fuels=new ArrayList<>(DEPLETED.keySet());
        fuels.sort(java.util.Comparator.comparingInt((String k)->PALETTE.contains(k)?PALETTE.indexOf(k):PALETTE.size()).thenComparing(k->k));
        for(String key:fuels){
            Profile p=PROFILES.get(key);var notes=new ArrayList<Component>();
            if(p.euFactor()!=1f||p.heatFactor()!=1f)notes.add(note("factors",factor(p.euFactor()),factor(p.heatFactor())));
            if(p.mox())notes.add(note("mox"));
            out.add(new ReactorTransformation(id("depletion/"+key),ReactorTransformation.Process.DEPLETION,itemId(key),
                itemId(DEPLETED.get(key)),fuelLife(key),notes));
        }
        PRODUCTS.forEach((key,product)->{
            Profile p=PROFILES.get(key);var notes=new ArrayList<Component>();
            if(p.breeding()){
                notes.add(note("breeding_gain",p.selfCooling()));
                if(p.kind()==Kind.LITHIUM&&p.hullCooling()>0)notes.add(note("breeding_heat",p.hullCooling()));
                notes.add(note("breeder_pulse"));
            }else{
                notes.add(note("neutron_source"));
                if(p.selfCooling()>0)notes.add(note("decay_heat",p.selfCooling()));
            }
            out.add(new ReactorTransformation(id((p.breeding()?"breeding/":"decay/")+key),
                p.breeding()?ReactorTransformation.Process.BREEDING:ReactorTransformation.Process.DECAY,itemId(key),product,p.capacity(),notes));
        });
        return out;
    }
    private static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath("mio_icif",path);}
    private static Component note(String key,Object... args){return Component.translatable("jei.mio_icif.reactor_transformation."+key,args);}
    private static String factor(float f){return f==Math.rint(f)?Integer.toString((int)f):Float.toString(f);}
    public static Part read(ItemStack stack){
        if(stack.isEmpty())return null;
        if(stack.getCount()!=1)throw new IllegalArgumentException("Reactor slots require exactly one item");
        Profile p=PROFILES.get(key(stack));
        // 0.1.10.36: a finished breeder is read as inert; before, the cycle that bred it threw and the reactor stalled for good
        if(p==null&&product(stack))return new Part(INERT,0,0);
        if(p==null)throw new IllegalArgumentException("Unsupported reactor component: "+BuiltInRegistries.ITEM.getKey(stack.getItem()));
        if(p.kind()==Kind.FUEL){
            var d=stack.get(mio_icif_data_components.FUEL_ROD_DURABILITY.get());
            if(d==null||d.maxUses()<=0||d.remainingUses()<0||d.remainingUses()>d.maxUses()||d.tickCounter()<0||d.tickCounter()>=20)
                throw new IllegalArgumentException("Invalid fuel durability retained");
            return new Part(p,0,d.remainingUses());
        }
        if(p.storesHeat()||p.kind()==Kind.REFLECTOR||p.breeding()||p.kind()==Kind.NEUTRON_SOURCE){
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
            String target=DEPLETED.get(key(previous));
            if(target==null)throw new IllegalArgumentException("No depleted identity");
            var item=BuiltInRegistries.ITEM.get(ResourceLocation.parse("mio_icif:reactor/item_reactor_"+target));
            out=new ItemStack(item);out.applyComponents(previous.getComponentsPatch());out.remove(mio_icif_data_components.FUEL_ROD_DURABILITY.get());
        }else if(next.profile().kind()==Kind.FUEL){
            FuelRodDurability old=previous.get(mio_icif_data_components.FUEL_ROD_DURABILITY.get());
            // Preserve legacy remaining cycle counts and their historical maximum; never refill old fuel.
            out.set(mio_icif_data_components.FUEL_ROD_DURABILITY.get(),new FuelRodDurability(next.remaining(),old.maxUses(),old.tickCounter()));
        }else if(next.profile().breeding()){
            // IC2：锂单元累积到阈值后变成氚燃料棒；近衰变同位素棒累积到阈值后变成重新浓缩的铀
            if(next.profile().capacity()>0&&next.stored()>=next.profile().capacity()){
                ItemStack product=new ItemStack(BuiltInRegistries.ITEM.get(PRODUCTS.get(key(previous))));
                product.applyComponents(previous.getComponentsPatch());
                // 0.1.10.36: the tritium rod is a component: it starts undecayed (the cell's breeding progress is not its decay)
                if(next.profile().kind()==Kind.LITHIUM)
                    product.set(mio_icif_data_components.REACTOR_COMPONENT_DATA.get(),ReactorComponentData.full(TRITIUM_LIFE));
                else product.remove(mio_icif_data_components.REACTOR_COMPONENT_DATA.get());
                return product;
            }
            out.set(mio_icif_data_components.REACTOR_COMPONENT_DATA.get(),new ReactorComponentData(next.stored(),next.profile().capacity()));
        }else if(next.profile().kind()==Kind.NEUTRON_SOURCE){
            // 0.1.10.36: a decayed tritium rod leaves its empty fuel rod
            if(next.stored()>=next.profile().capacity())return new ItemStack(BuiltInRegistries.ITEM.get(PRODUCTS.get(TRITIUM_KEY)));
            out.set(mio_icif_data_components.REACTOR_COMPONENT_DATA.get(),new ReactorComponentData(next.stored(),next.profile().capacity()));
        }else if(next.profile().storesHeat()||next.profile().kind()==Kind.REFLECTOR){
            out.set(mio_icif_data_components.REACTOR_COMPONENT_DATA.get(),new ReactorComponentData(next.stored(),next.profile().capacity()));
        }
        return out;
    }
}
