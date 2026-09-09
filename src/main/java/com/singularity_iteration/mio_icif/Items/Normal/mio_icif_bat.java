package com.singularity_iteration.mio_icif.Items.Normal;

import com.singularity_iteration.mio_icif.api.item.AbstractBattery;
import net.minecraft.world.item.ItemStack;

/**
 * mio_icif 内部电池基类。
 *
 * @deprecated 使用 {@link AbstractBattery} 代替。
 * 此类仅为内部向后兼容保留，addon 开发者应直接继承 {@link AbstractBattery}。
 */
@Deprecated
public class mio_icif_bat extends AbstractBattery {

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

    @Override
    protected long getStackableEnergy(ItemStack stack) {
        BatteryEnergy data = stack.get(mio_icif_data_components.BATTERY_ENERGY.get());
        return data != null ? data.energy() : 0L;
    }

    @Override
    protected void setStackableEnergy(ItemStack stack, long energy) {
        stack.set(mio_icif_data_components.BATTERY_ENERGY.get(), new BatteryEnergy(energy, getMaxEnergy()));
    }

    public String getTexturePrefix() {
        return texturePrefix;
    }
}