package com.singularity_iteration.mio_icif.Items.Armor;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorMaterial;

/**
 * 电池背包 (BatPack)
 * 佩戴在胸甲槽，为手持电力工具充电
 */
public class mio_icif_chestplate_batpack extends mio_icif_chestplate_energy_pack {

    public static final int MAX_ENERGY = 60000;

    // 每 tick 给手持物品充多少 EU
    public static final int CHARGE_RATE = 32;

    public mio_icif_chestplate_batpack(Holder<ArmorMaterial> material, Properties properties) {
        super(material, properties, MAX_ENERGY, "batpack", CHARGE_RATE, 1);
    }
}