// SCEX 2026-09-12: repaired malformed UTF-8 bytes in comments only.
package com.miophas.singularity_iteration.common.item.envtemplate;

import net.minecraft.world.item.Item;

/**
 * 地形转换模板-耕地
 * 能将沙子替换为草方块，并随机种植各种树木、南瓜、西瓜、小??? * 消耗电量：400 EU/??? */
@SuppressWarnings("null")
public class mio_icif_EvT_cultivation extends mio_icif_EvT_default {

    public mio_icif_EvT_cultivation(Item.Properties properties) {
        super(properties, TemplateType.CULTIVATION);
    }
}


