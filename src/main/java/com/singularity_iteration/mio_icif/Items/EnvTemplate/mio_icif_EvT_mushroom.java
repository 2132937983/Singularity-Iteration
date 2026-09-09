package com.singularity_iteration.mio_icif.Items.EnvTemplate;

import net.minecraft.world.item.Item;

/**
 * 地形转换模板-蘑菇
 * 能够将附近变为菌丝，并会在菌丝上生长巨型蘑菇
 * 能够将附近的生物群系改造成蘑菇岛类型的（会生成哞菇�? * 消耗电量：160 EU/�? */
@SuppressWarnings("null")
public class mio_icif_EvT_mushroom extends mio_icif_EvT_default {

    public mio_icif_EvT_mushroom(Item.Properties properties) {
        super(properties, TemplateType.MUSHROOM);
    }
}


