// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.item.armor;

import com.miophas.singularity_iteration.core.api.item.IEnergyPackItem;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorMaterial;

/**
 * SI 四个充电背包的共同基类。
 *
 * <p>与 IC2 原版一致，背包自身没有 onArmorTick：它们是被动的能量源，
 * 只在电动工具使用时被 {@code EnergyPackCharging.chargeFromArmor()} 取电。
 */
@SuppressWarnings({"null", "deprecation"})
public abstract class mio_icif_chestplate_energy_pack extends mio_icif_armor_elc implements IEnergyPackItem {

    public mio_icif_chestplate_energy_pack(Holder<ArmorMaterial> material, Properties properties,
            int maxEnergy, String texture, int transferLimit, int tier) {
        super(material, Type.CHESTPLATE, properties, maxEnergy, 0, texture, transferLimit, 0, tier);
    }

    @Override
    public long getEnergyPerDamage() {
        return 0;
    }
}
