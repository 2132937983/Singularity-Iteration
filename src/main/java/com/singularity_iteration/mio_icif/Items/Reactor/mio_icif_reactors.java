package com.singularity_iteration.mio_icif.Items.Reactor;

import com.singularity_iteration.mio_icif.Items.DataComponent.FuelRodDurability;
import com.singularity_iteration.mio_icif.Items.DataComponent.ReactorComponentData;
import com.singularity_iteration.mio_icif.Items.Normal.mio_icif_data_components;
import com.singularity_iteration.mio_icif.Singularity_Iteration;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@SuppressWarnings("null")
public class mio_icif_reactors {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Singularity_Iteration.MOD_ID);

    // ????????????
    public static final DeferredItem<mio_icif_heat_exchanger> COLLANT_SIMPLE = ITEMS.register("reactor/item_reactor_collant_simple",
        () -> new mio_icif_heat_exchanger(
            new Item.Properties().component(mio_icif_data_components.REACTOR_COMPONENT_DATA.get(), ReactorComponentData.full(10000)), 10000));
    public static final DeferredItem<mio_icif_heat_exchanger> COLLANT_TRIPLE = ITEMS.register("reactor/item_reactor_collant_triple",
        () -> new mio_icif_heat_exchanger(
            new Item.Properties().component(mio_icif_data_components.REACTOR_COMPONENT_DATA.get(), ReactorComponentData.full(30000)), 30000));
    public static final DeferredItem<mio_icif_heat_exchanger> COOLANT_SIX = ITEMS.register("reactor/item_reactor_coolant_six",
        () -> new mio_icif_heat_exchanger(
            new Item.Properties().component(mio_icif_data_components.REACTOR_COMPONENT_DATA.get(), ReactorComponentData.full(60000)), 60000));

    // ????
    // ???????20000?????????20000
    public static final DeferredItem<mio_icif_redstone_condensator> CONDENSATOR = ITEMS.register("reactor/item_reactor_condensator",
        () -> new mio_icif_redstone_condensator(
            new Item.Properties().component(mio_icif_data_components.REACTOR_COMPONENT_DATA.get(), ReactorComponentData.full(20000))));
    // ????????100000?????????20000??????40000
    public static final DeferredItem<mio_icif_lapis_condensator> CONDENSATOR_LAP = ITEMS.register("reactor/item_reactor_condensator_lap",
        () -> new mio_icif_lapis_condensator(
            new Item.Properties().component(mio_icif_data_components.REACTOR_COMPONENT_DATA.get(), ReactorComponentData.full(100000))));

    // ????
    // ???????2500????????2???????1
    public static final DeferredItem<mio_icif_basic_heat_switch> HEAT_SWITCH = ITEMS.register("reactor/item_reactor_heat_switch",
        () -> new mio_icif_basic_heat_switch(
            new Item.Properties().component(mio_icif_data_components.REACTOR_COMPONENT_DATA.get(), ReactorComponentData.full(mio_icif_basic_heat_switch.MAX_HEAT))));
    public static final DeferredItem<mio_icif_reactor_heat_switch> HEAT_SWITCH_CORE = ITEMS.register("reactor/item_reactor_heat_switch_core",
        () -> new mio_icif_reactor_heat_switch(
            new Item.Properties().component(mio_icif_data_components.REACTOR_COMPONENT_DATA.get(), ReactorComponentData.full(mio_icif_reactor_heat_switch.MAX_HEAT))));
    public static final DeferredItem<mio_icif_component_heat_switch> HEAT_SWITCH_SPREAD = ITEMS.register("reactor/item_reactor_heat_switch_spread",
        () -> new mio_icif_component_heat_switch(
            new Item.Properties().component(mio_icif_data_components.REACTOR_COMPONENT_DATA.get(), ReactorComponentData.full(mio_icif_component_heat_switch.MAX_HEAT))));
    public static final DeferredItem<mio_icif_advanced_heat_switch> DIAMOND_HEAT_SWITCH = ITEMS.register("reactor/item_reactor_diamond_heat_switch",
        () -> new mio_icif_advanced_heat_switch(
            new Item.Properties().component(mio_icif_data_components.REACTOR_COMPONENT_DATA.get(), ReactorComponentData.full(mio_icif_advanced_heat_switch.MAX_HEAT))));

    // ??????10000?????????12/??????????
    public static final DeferredItem<mio_icif_advanced_heat_vent> DIAMOND_VENT = ITEMS.register("reactor/item_reactor_diamond_vent",
        () -> new mio_icif_advanced_heat_vent(
            new Item.Properties().component(mio_icif_data_components.REACTOR_COMPONENT_DATA.get(), ReactorComponentData.full(mio_icif_advanced_heat_vent.MAX_HEAT))));

    public static final DeferredItem<Item> HEATPACK = ITEMS.register("reactor/item_reactor_heatpack", () -> new Item(new Item.Properties().stacksTo(64)));

    // ??????10000?????????1/???????????
    public static final DeferredItem<mio_icif_basic_heat_vent> VENT = ITEMS.register("reactor/item_reactor_vent",
        () -> new mio_icif_basic_heat_vent(
            new Item.Properties().component(mio_icif_data_components.REACTOR_COMPONENT_DATA.get(), ReactorComponentData.full(mio_icif_basic_heat_vent.MAX_HEAT))));

    // ????????????????????????????/??
    public static final DeferredItem<mio_icif_component_heat_vent> VENT_SPREAD = ITEMS.register("reactor/item_reactor_vent_spread",
        () -> new mio_icif_component_heat_vent(
            new Item.Properties().component(mio_icif_data_components.REACTOR_COMPONENT_DATA.get(), ReactorComponentData.full(0))));

    // ???????10000?????????5/????????5/??
    public static final DeferredItem<mio_icif_reactor_heat_vent> VENT_CORE = ITEMS.register("reactor/item_reactor_vent_core",
        () -> new mio_icif_reactor_heat_vent(
            new Item.Properties().component(mio_icif_data_components.REACTOR_COMPONENT_DATA.get(), ReactorComponentData.full(mio_icif_reactor_heat_vent.MAX_HEAT))));

    // ??????10000?????????10/????????36/??
    public static final DeferredItem<mio_icif_overclocked_heat_vent> OVERCLOCKED_HEAT_VENT = ITEMS.register("reactor/item_reactor_golden_vent",
        () -> new mio_icif_overclocked_heat_vent(
            new Item.Properties().component(mio_icif_data_components.REACTOR_COMPONENT_DATA.get(), ReactorComponentData.full(mio_icif_overclocked_heat_vent.MAX_HEAT))));

    // ??????4000?????????36/??????????
    public static final DeferredItem<mio_icif_heat_vent> IRIDIUM_HEAT_VENT = ITEMS.register("reactor/item_reactor_iridium_vent",
        () -> new mio_icif_heat_vent(
            new Item.Properties().component(mio_icif_data_components.REACTOR_COMPONENT_DATA.get(), ReactorComponentData.full(4000)),
            4000, 36, 0));
    // ????????4000?????????56/????????72/??
    public static final DeferredItem<mio_icif_heat_vent> IRIDIUM_OVERCLOCKED_HEAT_VENT = ITEMS.register("reactor/item_reactor_iridium_oc_vent",
        () -> new mio_icif_heat_vent(
            new Item.Properties().component(mio_icif_data_components.REACTOR_COMPONENT_DATA.get(), ReactorComponentData.full(4000)),
            4000, 56, 72));

    // ????????
    // ?????????+1000?????5%????
    public static final DeferredItem<mio_icif_reactor_plating> PLATE = ITEMS.register("reactor/item_reactor_plate",
        () -> new mio_icif_reactor_plating(new Item.Properties(), 1000, 5));
    // ?????????+500?????10%????
    public static final DeferredItem<mio_icif_reactor_plating> EXPLOSIVE_PLATE = ITEMS.register("reactor/item_reactor_explosive_plate",
        () -> new mio_icif_reactor_plating(new Item.Properties(), 500, 10));
    // IC2 heat plating provides +2000 hull heat. Its custom explosion reduction remains unchanged.
    public static final DeferredItem<mio_icif_reactor_plating> HEAT_PLATE = ITEMS.register("reactor/item_reactor_heat_plate",
        () -> new mio_icif_reactor_plating(new Item.Properties(), 2000, 1));

    // ???????????????????????????
    public static final DeferredItem<Item> URANIUM_SIMPLE_DEPLETED = ITEMS.register("reactor/item_reactor_uranium_simple_depleted", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> URANIUM_DUAL_DEPLETED = ITEMS.register("reactor/item_reactor_uranium_dual_depleted", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> URANIUM_QUAD_DEPLETED = ITEMS.register("reactor/item_reactor_uranium_quad_deplete", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> MOX_SIMPLE_DEPLETED = ITEMS.register("reactor/item_reactor_mox_simple_depleted", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> MOX_DUAL_DEPLETED = ITEMS.register("reactor/item_reactor_mox_dual_deplete", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> MOX_QUAD_DEPLETED = ITEMS.register("reactor/item_reactor_mox_quad_deplete", () -> new Item(new Item.Properties().stacksTo(64)));

    // ???- ?????5 EU/t, 4 HU/t, 10000 tick???
    public static final DeferredItem<mio_icif_nuclear_reactor> URANIUM_SIMPLE = ITEMS.register("reactor/item_reactor_uranium_simple",
        () -> new mio_icif_nuclear_reactor(
            new Item.Properties().component(mio_icif_data_components.FUEL_ROD_DURABILITY.get(), FuelRodDurability.full(10000)),
            10000, 5, 4, mio_icif_nuclear_reactor.FuelRodType.SINGLE, () -> URANIUM_SIMPLE_DEPLETED.get()));

    // IC2 fuel rods all consume one durability per 20-tick reactor cycle.
    public static final DeferredItem<mio_icif_nuclear_reactor> URANIUM_DUAL = ITEMS.register("reactor/item_reactor_uranium_dual",
        () -> new mio_icif_nuclear_reactor(
            new Item.Properties().component(mio_icif_data_components.FUEL_ROD_DURABILITY.get(), FuelRodDurability.full(10000)),
            10000, 5, 4, mio_icif_nuclear_reactor.FuelRodType.DUAL, () -> URANIUM_DUAL_DEPLETED.get()));

    // Quad rods have the same cycle lifetime; their advantage is output density, not total lifetime.
    public static final DeferredItem<mio_icif_nuclear_reactor> URANIUM_QUAD = ITEMS.register("reactor/item_reactor_uranium_quad",
        () -> new mio_icif_nuclear_reactor(
            new Item.Properties().component(mio_icif_data_components.FUEL_ROD_DURABILITY.get(), FuelRodDurability.full(10000)),
            10000, 5, 4, mio_icif_nuclear_reactor.FuelRodType.QUAD, () -> URANIUM_QUAD_DEPLETED.get()));

    // ???- MOX????10 EU/t, 8 HU/t, 10000 tick??????????????
    public static final DeferredItem<mio_icif_mox_reactor> MOX_SIMPLE = ITEMS.register("reactor/item_reactor_mox_simple",
        () -> new mio_icif_mox_reactor(
            new Item.Properties().component(mio_icif_data_components.FUEL_ROD_DURABILITY.get(), FuelRodDurability.full(10000)),
            10000, 10, 8, mio_icif_nuclear_reactor.FuelRodType.SINGLE, () -> MOX_SIMPLE_DEPLETED.get()));

    // MOX???????
    public static final DeferredItem<mio_icif_mox_reactor> MOX_DUAL = ITEMS.register("reactor/item_reactor_mox_dual",
        () -> new mio_icif_mox_reactor(
            new Item.Properties().component(mio_icif_data_components.FUEL_ROD_DURABILITY.get(), FuelRodDurability.full(10000)),
            10000, 10, 8, mio_icif_nuclear_reactor.FuelRodType.DUAL, () -> MOX_DUAL_DEPLETED.get()));

    // MOX???????
    public static final DeferredItem<mio_icif_mox_reactor> MOX_QUAD = ITEMS.register("reactor/item_reactor_mox_quad",
        () -> new mio_icif_mox_reactor(
            new Item.Properties().component(mio_icif_data_components.FUEL_ROD_DURABILITY.get(), FuelRodDurability.full(10000)),
            10000, 10, 8, mio_icif_nuclear_reactor.FuelRodType.QUAD, () -> MOX_QUAD_DEPLETED.get()));

    // ?????
    // ????????30000???????????????????????
    public static final DeferredItem<mio_icif_neutron_reflector> REFLECTOR = ITEMS.register("reactor/item_reactor_reflector",
        () -> new mio_icif_neutron_reflector(
            new Item.Properties().component(mio_icif_data_components.REACTOR_COMPONENT_DATA.get(), ReactorComponentData.full(30000)), 30000));
    // ????????120000???????????????????????
    public static final DeferredItem<mio_icif_neutron_reflector> THICK_REFLECTOR = ITEMS.register("reactor/item_reactor_thick_reflector",
        () -> new mio_icif_neutron_reflector(
            new Item.Properties().component(mio_icif_data_components.REACTOR_COMPONENT_DATA.get(), ReactorComponentData.full(120000)), 120000));


    // ??
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}