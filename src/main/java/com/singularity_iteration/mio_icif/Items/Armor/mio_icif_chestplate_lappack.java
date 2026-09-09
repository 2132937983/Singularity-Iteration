package com.singularity_iteration.mio_icif.Items.Armor;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorMaterial;

/**
 * 兰波顿储电背包 (Lappack)
 * 佩戴在胸甲槽，为手持电力工具充电
 */
public class mio_icif_chestplate_lappack extends mio_icif_chestplate_energy_pack {

    public static final int MAX_ENERGY = 10000000;

    public static final int CHARGE_RATE = 2048;

    public mio_icif_chestplate_lappack(Holder<ArmorMaterial> material, Properties properties) {
        super(material, properties, MAX_ENERGY, "lappack", CHARGE_RATE, 4);
    }
}