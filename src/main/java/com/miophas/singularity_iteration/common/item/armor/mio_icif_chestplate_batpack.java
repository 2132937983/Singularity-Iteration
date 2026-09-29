package com.miophas.singularity_iteration.common.item.armor;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorMaterial;

/**
 * 电池背包 (BatPack)
 * 佩戴在胸甲槽，为手持电力工具充电
 */
public class mio_icif_chestplate_batpack extends mio_icif_chestplate_energy_pack {

    public static final int MAX_ENERGY = 60000;

    // 每 tick 给手持物品充多少 EU（IC2 传输限制 100 EU/t）
    public static final int CHARGE_RATE = 100;

    public mio_icif_chestplate_batpack(Holder<ArmorMaterial> material, Properties properties) {
        super(material, properties, MAX_ENERGY, "batpack", CHARGE_RATE, 1);
    }
}