package com.singularity_iteration.mio_icif.Items.Tools;

import com.singularity_iteration.mio_icif.api.item.AbstractElectricTool;
import net.minecraft.world.item.ItemStack;

/**
 * mio_icif 内部电动工具基类。
 *
 * @deprecated 使用 {@link AbstractElectricTool} 代替。
 * 此类仅为内部向后兼容保留，addon 开发者应直接继承 {@link AbstractElectricTool}。
 */
@Deprecated
public class mio_icif_tool_elc extends AbstractElectricTool {

    public static final long DEFAULT_TOOL_MAX_ENERGY = 10000;

    public mio_icif_tool_elc(Properties properties) {
        this(properties, DEFAULT_TOOL_MAX_ENERGY, 0, DEFAULT_TOOL_MAX_ENERGY, 100, 1);
    }

    public mio_icif_tool_elc(Properties properties, long initialEnergy) {
        this(properties, DEFAULT_TOOL_MAX_ENERGY, initialEnergy, DEFAULT_TOOL_MAX_ENERGY, 100, 1);
    }

    public mio_icif_tool_elc(Properties properties, long maxEnergy, long initialEnergy) {
        this(properties, maxEnergy, initialEnergy, maxEnergy, 100, 1);
    }

    public mio_icif_tool_elc(Properties properties, long maxEnergy, long initialEnergy, long energyPerUse) {
        this(properties, maxEnergy, initialEnergy, maxEnergy, energyPerUse, 1);
    }

    public mio_icif_tool_elc(Properties properties, long maxEnergy, long initialEnergy, long chargeRate, long energyPerUse, int toolTier) {
        this(properties, maxEnergy, initialEnergy, "tool_elc", chargeRate, energyPerUse, toolTier);
    }

    public mio_icif_tool_elc(Properties properties, long maxEnergy, long initialEnergy, String texturePrefix, long chargeRate, long energyPerUse, int toolTier) {
        super(properties, maxEnergy, initialEnergy, chargeRate, energyPerUse, toolTier);
    }

    protected long apiGetEnergy(ItemStack stack) {
        return super.getEnergy(stack);
    }

    protected void apiSetEnergy(ItemStack stack, long energy) {
        super.setEnergy(stack, energy);
    }

    protected long apiAddEnergy(ItemStack stack, long amount) {
        return super.addEnergy(stack, amount);
    }

    protected long apiExtractEnergy(ItemStack stack, long amount) {
        return super.extractEnergy(stack, amount);
    }
}