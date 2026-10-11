// SCEX 2026-09-12: repaired malformed UTF-8 bytes in comments only.
package com.miophas.singularity_iteration.common.item.envtemplate;

import net.minecraft.world.item.Item;

/**
 * Terrain conversion template: irrigation.
 * It replaces sand with grass blocks. It also randomly places water one layer below the height of the terraformer.
 * Energy use: 160 EU/t
 */
@SuppressWarnings("null")
public class mio_icif_EvT_irrigation extends mio_icif_EvT_default {

    public mio_icif_EvT_irrigation(Item.Properties properties) {
        super(properties, TemplateType.IRRIGATION);
    }
}


