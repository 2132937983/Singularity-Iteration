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

/**
 * An upgrade unit item. The Universal Suit Modification Station installs it into a suit piece.
 * 0.1.8.6: a unit item has a tier; a Mk.II item installs the same unit as Mk.II.
 */
public class SuitModuleItem extends Item {
    private final SuitModuleType type;
    private final int tier;

    public SuitModuleItem(SuitModuleType type, Properties properties) {
        this(type, 1, properties);
    }

    public SuitModuleItem(SuitModuleType type, int tier, Properties properties) {
        super(properties.stacksTo(16).rarity(Rarity.EPIC));
        this.type = type;
        this.tier = tier;
    }

    public SuitModuleType type() { return type; }
    /** 1 or 2 (Mk.II). */
    public int tier() { return tier; }

    /** Tier of a unit item stack (1 for addon unit items). */
    public static int tierOf(ItemStack stack) {
        return stack.getItem() instanceof SuitModuleItem item ? item.tier : 1;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return tier >= 2;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(type.descriptionKey()).withStyle(ChatFormatting.GRAY));
        if (tier >= 2) {
            tooltip.add(Component.translatable("module.mio_icif.tooltip.mk2").withStyle(ChatFormatting.LIGHT_PURPLE));
            tooltip.add(Component.translatable(type.descriptionKey() + ".mk2").withStyle(ChatFormatting.LIGHT_PURPLE));
        } else if (type.hasMk2()) {
            tooltip.add(Component.translatable("module.mio_icif.tooltip.mk2_available").withStyle(ChatFormatting.DARK_PURPLE));
        }
        Component slots = null;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (!type.fits(slot)) continue;
            Component name = Component.translatable("module.mio_icif.slot." + slot.getName());
            slots = slots == null ? name.copy() : slots.copy().append(" / ").append(name);
        }
        tooltip.add(Component.translatable("module.mio_icif.tooltip.slots", slots).withStyle(ChatFormatting.DARK_AQUA));
        // 0.1.8.5: which suit classes take the unit, and its lattice points on a nano piece
        boolean quantum = type.allows(com.miophas.singularity_iteration.core.api.suit.SuitClass.QUANTUM);
        boolean nano = type.allows(com.miophas.singularity_iteration.core.api.suit.SuitClass.NANO);
        String classKey = quantum && nano ? "shared" : nano ? "nano" : "quantum";
        tooltip.add(Component.translatable("module.mio_icif.tooltip.class." + classKey, type.latticePoints())
            .withStyle(nano && !quantum ? ChatFormatting.AQUA : quantum && !nano ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GREEN));
        if (type == SuitModuleType.DEFLECTOR) {
            tooltip.add(Component.translatable("module.mio_icif.tooltip.deflector_cost",
                SuitModules.DEFLECTOR_EU_PER_DAMAGE, (int) SuitModules.DEFLECTOR_MAX_PER_HIT).withStyle(ChatFormatting.DARK_AQUA));
        } else if (type.drainPerTick() > 0 || type.burst() == null) {
            double drain = tier >= 2 ? SuitModules.drainPerSecond(type, 2) / (double) SuitModules.DRAIN_INTERVAL : type.drainPerTick();
            tooltip.add(Component.translatable("module.mio_icif.tooltip.drain", drain == Math.rint(drain) ? String.valueOf((long) drain)
                : String.format(java.util.Locale.ROOT, "%.1f", drain)).withStyle(ChatFormatting.DARK_AQUA));
        }
        var burst = com.miophas.singularity_iteration.common.suit.nano.NanoConfig.burst(type);
        if (burst != null) {
            tooltip.add(Component.translatable("module.mio_icif.tooltip.burst." + (burst.sustained() ? "sustained" : "single"),
                burst.reserveCost(), burst.heat(), String.format(java.util.Locale.ROOT, "%.1f", burst.cooldownTicks() / 20F))
                .withStyle(ChatFormatting.GOLD));
        }
        if (type == SuitModuleType.MYOFIBER) tooltip.add(Component.translatable("module.mio_icif.tooltip.hf_sync").withStyle(ChatFormatting.BLUE));
        if (type.needsVisor()) tooltip.add(Component.translatable("module.mio_icif.tooltip.visor").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("module.mio_icif.tooltip.station").withStyle(ChatFormatting.DARK_GRAY));
    }
}
