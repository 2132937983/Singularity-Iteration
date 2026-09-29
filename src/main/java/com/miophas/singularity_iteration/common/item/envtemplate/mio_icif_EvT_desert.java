// SCEX 2026-09-12: repaired malformed UTF-8 bytes in comments only.
package com.miophas.singularity_iteration.common.item.envtemplate;

import net.minecraft.world.item.Item;

/**
 * 地形转换模板-沙漠
 * 能将泥土与草方块替换为沙子，引发森林大火、杀死附近动物，并随机种植仙人掌
 * 消耗电量：80 EU/??? */
@SuppressWarnings("null")
public class mio_icif_EvT_desert extends mio_icif_EvT_default {

    public mio_icif_EvT_desert(Item.Properties properties) {
        super(properties, TemplateType.DESERTIFICATION);
    }
}


