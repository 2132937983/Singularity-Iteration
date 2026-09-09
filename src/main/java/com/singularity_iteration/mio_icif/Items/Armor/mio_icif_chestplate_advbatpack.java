package com.singularity_iteration.mio_icif.Items.Armor;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorMaterial;

/**
 * 高级电池背包 (Advanced BatPack)
 * 佩戴在胸甲槽，为手持电力工具充电
 */
public class mio_icif_chestplate_advbatpack extends mio_icif_chestplate_energy_pack {

    public static final int MAX_ENERGY = 600000;

    public static final int CHARGE_RATE = 128;

    public mio_icif_chestplate_advbatpack(Holder<ArmorMaterial> material, Properties properties) {
        super(material, properties, MAX_ENERGY, "advbatpack", CHARGE_RATE, 2);
    }
}