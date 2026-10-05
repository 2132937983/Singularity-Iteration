// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

/** An upgrade unit item. The quantum modification station installs it into a suit piece. */
public class SuitModuleItem extends Item {
    private final SuitModuleType type;

    public SuitModuleItem(SuitModuleType type, Properties properties) {
        super(properties.stacksTo(16).rarity(Rarity.EPIC));
        this.type = type;
    }

    public SuitModuleType type() { return type; }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(type.descriptionKey()).withStyle(ChatFormatting.GRAY));
        Component slots = null;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (!type.fits(slot)) continue;
            Component name = Component.translatable("module.mio_icif.slot." + slot.getName());
            slots = slots == null ? name.copy() : slots.copy().append(" / ").append(name);
        }
        tooltip.add(Component.translatable("module.mio_icif.tooltip.slots", slots).withStyle(ChatFormatting.DARK_AQUA));
        if (type == SuitModuleType.DEFLECTOR) {
            tooltip.add(Component.translatable("module.mio_icif.tooltip.deflector_cost",
                SuitModules.DEFLECTOR_EU_PER_DAMAGE, (int) SuitModules.DEFLECTOR_MAX_PER_HIT).withStyle(ChatFormatting.DARK_AQUA));
        } else {
            tooltip.add(Component.translatable("module.mio_icif.tooltip.drain", type.drainPerTick()).withStyle(ChatFormatting.DARK_AQUA));
        }
        if (type.needsVisor()) tooltip.add(Component.translatable("module.mio_icif.tooltip.visor").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("module.mio_icif.tooltip.station").withStyle(ChatFormatting.DARK_GRAY));
    }
}
