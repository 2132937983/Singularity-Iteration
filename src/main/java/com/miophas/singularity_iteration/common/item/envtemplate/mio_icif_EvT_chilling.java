// SCEX 2026-09-12: repaired malformed UTF-8 bytes in comments only.
package com.miophas.singularity_iteration.common.item.envtemplate;

import net.minecraft.world.item.Item;

/**
 * Terraformer template: ice plains.
 * It makes snow on blocks and freezes water into ice.
 * Energy use: 80 EU/t.
 */
@SuppressWarnings("null")
public class mio_icif_EvT_chilling extends mio_icif_EvT_default {

    public mio_icif_EvT_chilling(Item.Properties properties) {
        super(properties, TemplateType.CHILLING);
    }
}


