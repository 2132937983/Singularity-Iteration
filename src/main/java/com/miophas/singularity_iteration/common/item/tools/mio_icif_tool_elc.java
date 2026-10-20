package com.miophas.singularity_iteration.common.item.tools;

import com.miophas.singularity_iteration.core.prefab.item.AbstractElectricTool;
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

    /**
     * A main-hand attack damage modifier, as swords and axes have it: shown in the tooltip and used by
     * every wielder. Players add it to their 1 base damage; mobs (and maids of maid mods, which only
     * take an item with an attack damage modifier as a weapon) to theirs.
     */
    protected static net.minecraft.world.item.component.ItemAttributeModifiers attackDamage(double amount) {
        return net.minecraft.world.item.component.ItemAttributeModifiers.builder()
            .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
                new net.minecraft.world.entity.ai.attributes.AttributeModifier(BASE_ATTACK_DAMAGE_ID, amount,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
            .build();
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