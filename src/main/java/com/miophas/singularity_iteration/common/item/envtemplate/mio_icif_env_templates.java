// SCEX 2026-09-12: repaired malformed UTF-8 bytes in comments only.
package com.miophas.singularity_iteration.common.item.envtemplate;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registration of the terrain transformation template items. */
@SuppressWarnings("null")
public class mio_icif_env_templates {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Singularity_Iteration.MOD_ID);

    // Terrain transformation template: blank
    public static final DeferredItem<mio_icif_EvT_empty> EVT_EMPTY =
        ITEMS.register("env_template/evt_empty", () -> new mio_icif_EvT_empty(new Item.Properties()));

    // Terrain transformation template: farmland
    public static final DeferredItem<mio_icif_EvT_cultivation> EVT_CULTIVATION =
        ITEMS.register("env_template/evt_cultivation", () -> new mio_icif_EvT_cultivation(new Item.Properties()));

    // Terrain transformation template: desert
    public static final DeferredItem<mio_icif_EvT_desert> EVT_DESERT =
        ITEMS.register("env_template/evt_desert", () -> new mio_icif_EvT_desert(new Item.Properties()));

    // Terrain transformation template: irrigation
    public static final DeferredItem<mio_icif_EvT_irrigation> EVT_IRRIGATION =
        ITEMS.register("env_template/evt_irrigation", () -> new mio_icif_EvT_irrigation(new Item.Properties()));

    // Terrain transformation template: ice plains
    public static final DeferredItem<mio_icif_EvT_chilling> EVT_CHILLING =
        ITEMS.register("env_template/evt_chilling", () -> new mio_icif_EvT_chilling(new Item.Properties()));

    // Terrain transformation template: flatland
    public static final DeferredItem<mio_icif_EvT_flatification> EVT_FLATIFICATION =
        ITEMS.register("env_template/evt_flatification", () -> new mio_icif_EvT_flatification(new Item.Properties()));

    // Terrain transformation template: mushroom
    public static final DeferredItem<mio_icif_EvT_mushroom> EVT_MUSHROOM =
        ITEMS.register("env_template/evt_mushroom", () -> new mio_icif_EvT_mushroom(new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}


