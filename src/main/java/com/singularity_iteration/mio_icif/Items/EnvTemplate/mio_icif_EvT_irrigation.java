package com.singularity_iteration.mio_icif.Items.EnvTemplate;

import net.minecraft.world.item.Item;

/**
 * 地形转换模板-灌溉
 * 能将沙子替换为草方块，并随机在比环境改造机的高度低一层的地方生成�? * 消耗电量：160 EU/�? */
@SuppressWarnings("null")
public class mio_icif_EvT_irrigation extends mio_icif_EvT_default {

    public mio_icif_EvT_irrigation(Item.Properties properties) {
        super(properties, TemplateType.IRRIGATION);
    }
}


