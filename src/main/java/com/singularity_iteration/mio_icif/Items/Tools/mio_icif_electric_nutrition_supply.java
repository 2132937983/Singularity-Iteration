package com.singularity_iteration.mio_icif.Items.Tools;

import com.singularity_iteration.mio_icif.api.tool.ToggleableElectricTool;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 电力营养供应器
 * 手持时自动补充饥饿值（需要开启自动模式）
 * G键切换自动补充模式
 */
@SuppressWarnings("null")
public class mio_icif_electric_nutrition_supply extends mio_icif_tool_elc implements ToggleableElectricTool {

    private static final int MAX_ENERGY = 10000;
    private static final int TRANSFER_SPEED = 32;
    private static final int ENERGY_COST = 100;
    private static final String TAG_AUTO_SUPPLEMENT = "AutoSupplement";

    public mio_icif_electric_nutrition_supply() {
        super(new Item.Properties(), MAX_ENERGY, MAX_ENERGY, "item_electric_nutrition_supply", TRANSFER_SPEED, ENERGY_COST, 1);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        if (level.isClientSide || !(entity instanceof Player player)) {
            return;
        }

        if (!getAutoSupplement(stack)) {
            return;
        }

        if (player.getFoodData().needsFood()) {
            if (getEnergy(stack) >= ENERGY_COST) {
                extractEnergy(stack, ENERGY_COST);
                player.getFoodData().eat(1, 0.2F);
                player.containerMenu.broadcastChanges();
            }
        }
    }

    @Override
    public void toggleActive(ItemStack stack, Player player) {
        boolean autoSupplement = !getAutoSupplement(stack);
        setAutoSupplement(stack, autoSupplement);

        if (autoSupplement) {
            player.sendSystemMessage(Component.translatable("message.mio_icif.electric_nutrition_supply.auto_on"));
        } else {
            player.sendSystemMessage(Component.translatable("message.mio_icif.electric_nutrition_supply.auto_off"));
        }
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("tooltip.mio_icif.electric_nutrition_supply.mode",
            getAutoSupplement(stack)
                ? Component.translatable("tooltip.mio_icif.electric_nutrition_supply.auto_on")
                : Component.translatable("tooltip.mio_icif.electric_nutrition_supply.auto_off")));
    }

    public static boolean getAutoSupplement(ItemStack stack) {
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null && !customData.isEmpty()) {
            return customData.copyTag().getBoolean(TAG_AUTO_SUPPLEMENT);
        }
        return false;
    }

    public static void setAutoSupplement(ItemStack stack, boolean value) {
        var customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        tag.putBoolean(TAG_AUTO_SUPPLEMENT, value);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
}
