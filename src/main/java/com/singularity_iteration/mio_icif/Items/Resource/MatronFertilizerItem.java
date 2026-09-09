package com.singularity_iteration.mio_icif.Items.Resource;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * 作物监管机专用肥料物�? * 用于促进作物生长
 */
@SuppressWarnings("null")
public class MatronFertilizerItem extends Item {

    // 每次使用提供的肥料值（类似骨粉，但效果更好�
public static final int FERTILIZER_VALUE = 200;

    public MatronFertilizerItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.mio_icif.fertilizer.tooltip"));
        tooltipComponents.add(Component.translatable("item.mio_icif.fertilizer.tooltip.matron"));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }

    /**
     * 获取肥料效果�
 * @return 肥料效果值（用于作物生长计算�
 */
    public int getFertilizerValue() {
        return FERTILIZER_VALUE;
    }

    /**
     * 检查物品是否可以用作肥�
 * @param stack 物品栈
 * @return 是否可以用作肥料
     */
    public static boolean isFertilizer(ItemStack stack) {
        return stack.getItem() instanceof MatronFertilizerItem;
    }
}


