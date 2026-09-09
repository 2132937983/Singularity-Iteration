package com.singularity_iteration.mio_icif.Items.EnvTemplate;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 地形转换模板物品注册�? */
@SuppressWarnings("null")
public class mio_icif_env_templates {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Singularity_Iteration.MOD_ID);

    // 地形转换模板-空白
    public static final DeferredItem<mio_icif_EvT_empty> EVT_EMPTY =
        ITEMS.register("env_template/evt_empty", () -> new mio_icif_EvT_empty(new Item.Properties()));

    // 地形转换模板-耕地
    public static final DeferredItem<mio_icif_EvT_cultivation> EVT_CULTIVATION =
        ITEMS.register("env_template/evt_cultivation", () -> new mio_icif_EvT_cultivation(new Item.Properties()));

    // 地形转换模板-沙漠
    public static final DeferredItem<mio_icif_EvT_desert> EVT_DESERT =
        ITEMS.register("env_template/evt_desert", () -> new mio_icif_EvT_desert(new Item.Properties()));

    // 地形转换模板-灌溉
    public static final DeferredItem<mio_icif_EvT_irrigation> EVT_IRRIGATION =
        ITEMS.register("env_template/evt_irrigation", () -> new mio_icif_EvT_irrigation(new Item.Properties()));

    // 地形转换模板-冰原
    public static final DeferredItem<mio_icif_EvT_chilling> EVT_CHILLING =
        ITEMS.register("env_template/evt_chilling", () -> new mio_icif_EvT_chilling(new Item.Properties()));

    // 地形转换模板-平地
    public static final DeferredItem<mio_icif_EvT_flatification> EVT_FLATIFICATION =
        ITEMS.register("env_template/evt_flatification", () -> new mio_icif_EvT_flatification(new Item.Properties()));

    // 地形转换模板-蘑菇
    public static final DeferredItem<mio_icif_EvT_mushroom> EVT_MUSHROOM =
        ITEMS.register("env_template/evt_mushroom", () -> new mio_icif_EvT_mushroom(new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}


