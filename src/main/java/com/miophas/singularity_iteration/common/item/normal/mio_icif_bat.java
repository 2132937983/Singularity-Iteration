package com.miophas.singularity_iteration.common.item.normal;

import com.miophas.singularity_iteration.core.api.item.IEnergyDistributable;

import com.miophas.singularity_iteration.core.prefab.item.AbstractBattery;

/**
 * mio_icif 内部电池基类。
 *
 * @deprecated 使用 {@link AbstractBattery} 代替。
 * 此类仅为内部向后兼容保留，addon 开发者应直接继承 {@link AbstractBattery}。
 */
@Deprecated
public class mio_icif_bat extends AbstractBattery implements IEnergyDistributable {

    public static final long MAX_ENERGY = 10000;

    private final String texturePrefix;

    public mio_icif_bat(Properties properties) {
        this(properties, MAX_ENERGY, MAX_ENERGY, "item_bat", MAX_ENERGY);
    }

    public mio_icif_bat(Properties properties, long initialEnergy) {
        this(properties, MAX_ENERGY, initialEnergy, "item_bat", MAX_ENERGY);
    }

    public mio_icif_bat(Properties properties, long maxEnergy, long initialEnergy) {
        this(properties, maxEnergy, initialEnergy, "item_bat", maxEnergy);
    }

    public mio_icif_bat(Properties properties, long maxEnergy, long initialEnergy, String texturePrefix) {
        this(properties, maxEnergy, initialEnergy, texturePrefix, maxEnergy);
    }

    public mio_icif_bat(Properties properties, long maxEnergy, long initialEnergy, String texturePrefix, long chargeRate) {
        this(properties, maxEnergy, initialEnergy, texturePrefix, chargeRate, 1);
    }

    public mio_icif_bat(Properties properties, long maxEnergy, long initialEnergy, String texturePrefix, long chargeRate, int maxStackSize) {
        super(properties, maxEnergy, initialEnergy, chargeRate, maxStackSize);
        this.texturePrefix = texturePrefix;
    }

    public String getTexturePrefix() {
        return texturePrefix;
    }
}