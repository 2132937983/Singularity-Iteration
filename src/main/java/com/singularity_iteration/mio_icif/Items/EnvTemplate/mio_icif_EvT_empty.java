package com.singularity_iteration.mio_icif.Items.EnvTemplate;

import net.minecraft.world.item.Item;

/**
 * 地形转换模板-空白
 * 不能用于地形转换机，主要用于合成其他模板
 */
@SuppressWarnings("null")
public class mio_icif_EvT_empty extends mio_icif_EvT_default {

    public mio_icif_EvT_empty(Item.Properties properties) {
        super(properties, TemplateType.EMPTY);
    }
}


