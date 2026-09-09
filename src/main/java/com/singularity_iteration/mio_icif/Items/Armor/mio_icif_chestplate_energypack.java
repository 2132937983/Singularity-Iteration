package com.singularity_iteration.mio_icif.Items.Armor;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorMaterial;

/**
 * 能量背包 (Energypack)
 * 佩戴在胸甲槽，为手持电力工具充电
 */
public class mio_icif_chestplate_energypack extends mio_icif_chestplate_energy_pack {

    public static final int MAX_ENERGY = 2000000;

    public static final int CHARGE_RATE = 512;

    public mio_icif_chestplate_energypack(Holder<ArmorMaterial> material, Properties properties) {
        super(material, properties, MAX_ENERGY, "energypack", CHARGE_RATE, 3);
    }
}