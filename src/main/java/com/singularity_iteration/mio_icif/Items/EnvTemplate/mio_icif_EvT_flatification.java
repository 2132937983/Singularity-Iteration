package com.singularity_iteration.mio_icif.Items.EnvTemplate;

import net.minecraft.world.item.Item;

/**
 * 地形转换模板-平地
 * 能够将附近填平至环境改造机同一高度
 * 如果方块上方有遮挡物（比如树叶、蜡烛、落叶箱子之类的），这个方块及其下方方块不会消失
 * 消耗电量：800 EU/�? */
@SuppressWarnings("null")
public class mio_icif_EvT_flatification extends mio_icif_EvT_default {

    public mio_icif_EvT_flatification(Item.Properties properties) {
        super(properties, TemplateType.FLATIFICATION);
    }
}


