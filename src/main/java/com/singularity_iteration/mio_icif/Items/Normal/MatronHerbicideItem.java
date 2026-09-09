package com.singularity_iteration.mio_icif.Items.Normal;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * 作物监管机专用除草剂物品
 * 用于清除作物架上的杂草
 */
@SuppressWarnings("null")
public class MatronHerbicideItem extends Item {

    // 每次使用提供的除草剂用量
    public static final int HERBICIDE_VALUE = 100;

    // 耐久度设置（比铁低）
    public static final int DURABILITY = 64;

    public MatronHerbicideItem(Properties properties) {
        super(properties.durability(DURABILITY));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.mio_icif.herbicide.tooltip"));
        tooltipComponents.add(Component.translatable("item.mio_icif.herbicide.tooltip.matron"));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }

    /**
     * 获取除草剂效果值
     * @return 除草剂效果值（用于除草计算）
     */
    public int getHerbicideValue() {
        return HERBICIDE_VALUE;
    }

    /**
     * 检查物品是否可以用作除草剂
     * @param stack 物品堆
     * @return 是否可以用作除草剂
     */
    public static boolean isHerbicide(ItemStack stack) {
        return stack.getItem() instanceof MatronHerbicideItem;
    }
}