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

    /** Sneak + right click switches the inventory auto-charge mode (right click alone still equips). */
    @Override
    public net.minecraft.world.InteractionResultHolder<net.minecraft.world.item.ItemStack> use(net.minecraft.world.level.Level level,
            net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand) {
        net.minecraft.world.item.ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown() || stack.getCount() != 1) return super.use(level, player, hand);
        if (!level.isClientSide) {
            boolean on = com.miophas.singularity_iteration.core.api.item.BatteryAutoCharge.toggle(stack);
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(on ? "message.mio_icif.bat.auto_on" : "message.mio_icif.bat.auto_off",
                getEnergy(stack), getMaxEnergy(stack)), true);
        }
        return net.minecraft.world.InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void inventoryTick(net.minecraft.world.item.ItemStack stack, net.minecraft.world.level.Level level,
            net.minecraft.world.entity.Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (!level.isClientSide && entity instanceof net.minecraft.world.entity.player.Player player) {
            com.miophas.singularity_iteration.core.api.item.BatteryAutoCharge.tick(player, stack, this, getChargeRate(stack), slotId);
        }
    }

    @Override
    public void appendHoverText(net.minecraft.world.item.ItemStack stack, TooltipContext context,
            java.util.List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        boolean on = com.miophas.singularity_iteration.core.api.item.BatteryAutoCharge.isOn(stack);
        tooltip.add(net.minecraft.network.chat.Component.translatable(on ? "tooltip.mio_icif.bat.auto_on" : "tooltip.mio_icif.pack.auto_hint")
            .withStyle(on ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(net.minecraft.world.item.ItemStack stack) {
        return super.isFoil(stack) || com.miophas.singularity_iteration.core.api.item.BatteryAutoCharge.isOn(stack);
    }
}
