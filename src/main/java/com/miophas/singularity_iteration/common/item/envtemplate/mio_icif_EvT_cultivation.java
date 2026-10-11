// SCEX 2026-09-12: repaired malformed UTF-8 bytes in comments only.
package com.miophas.singularity_iteration.common.item.envtemplate;

import net.minecraft.world.item.Item;

/**
 * Terrain conversion template: cultivation.
 * It replaces sand with grass blocks. It also plants random trees, pumpkins, melons and other plants.
 * Energy consumption: 400 EU/t.
 */
@SuppressWarnings("null")
public class mio_icif_EvT_cultivation extends mio_icif_EvT_default {

    public mio_icif_EvT_cultivation(Item.Properties properties) {
        super(properties, TemplateType.CULTIVATION);
    }
}


