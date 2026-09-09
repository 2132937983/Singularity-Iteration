package com.singularity_iteration.mio_icif.Items.Lathe;

import com.singularity_iteration.mio_icif.api.item.ILatheItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 车床车刀物品
 * 对应 IC2 �?ItemLathingTool
 * 
 * 用于车床加工，有耐久度，需要硬度大于加工件才能加工
 */
@SuppressWarnings("null")
public class LathingToolItem extends Item implements ILatheItem.ILatheTool {

    private final ToolMaterial material;

    public LathingToolItem(ToolMaterial material) {
        super(new Item.Properties().stacksTo(1));
        this.material = material;
    }

    @Override
    public int getHardness(ItemStack stack) {
        return material.hardness;
    }

    @Override
    public int getCustomDamage(ItemStack stack) {
        CompoundTag tag = getCustomDataTag(stack);
        if (tag != null && tag.contains("customDamage")) {
            return tag.getInt("customDamage");
        }
        return 0;
    }

    @Override
    public void setCustomDamage(ItemStack stack, int damage) {
        CompoundTag tag = getCustomDataTag(stack);
        if (tag == null) {
            tag = new CompoundTag();
        }
        tag.putInt("customDamage", Math.max(0, Math.min(damage, getMaxCustomDamage(stack))));
        setCustomDataTag(stack, tag);
    }

    @Override
    public int getMaxCustomDamage(ItemStack stack) {
        return material.maxDamage;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getCustomDamage(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int customDamage = getCustomDamage(stack);
        int maxDamage = getMaxCustomDamage(stack);
        if (maxDamage <= 0) return 0;
        return Math.round((1.0f - (float) customDamage / maxDamage) * 13.0f);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        float f = Math.max(0.0F, ((float) getMaxCustomDamage(stack) - (float) getCustomDamage(stack)) / (float) getMaxCustomDamage(stack));
        return java.awt.Color.HSBtoRGB(f / 3.0F, 1.0F, 1.0F);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        int damage = getCustomDamage(stack);
        int maxDamage = getMaxCustomDamage(stack);
        tooltip.add(Component.translatable("item.mio_icif.lathing_tool.tooltip.hardness", material.hardness));
        tooltip.add(Component.translatable("item.mio_icif.lathing_tool.tooltip.damage", maxDamage - damage, maxDamage));
    }

    @Nullable
    private CompoundTag getCustomDataTag(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            return customData.copyTag();
        }
        return null;
    }

    private void setCustomDataTag(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /**
     * 车刀材质枚举
     */
    @SuppressWarnings("null")
public enum ToolMaterial {
        IRON(2, 32),    // 铁车刀：硬�?，耐久32
        DIAMOND(4, 64); // 钻石车刀：硬�?，耐久64

        public final int hardness;
        public final int maxDamage;

        ToolMaterial(int hardness, int maxDamage) {
            this.hardness = hardness;
            this.maxDamage = maxDamage;
        }
    }
}

