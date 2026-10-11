// SCEX 2026-09-12: repaired malformed UTF-8 bytes in comments only.
package com.miophas.singularity_iteration.common.item.envtemplate;

import net.minecraft.world.item.Item;

/**
 * Terrain conversion template: mushroom.
 * It changes nearby blocks to mycelium. Huge mushrooms grow on the mycelium.
 * It changes nearby biomes to the mushroom fields type. Mooshrooms spawn there.
 * Energy use: 160 EU/t.
 */
@SuppressWarnings("null")
public class mio_icif_EvT_mushroom extends mio_icif_EvT_default {

    public mio_icif_EvT_mushroom(Item.Properties properties) {
        super(properties, TemplateType.MUSHROOM);
    }
}


