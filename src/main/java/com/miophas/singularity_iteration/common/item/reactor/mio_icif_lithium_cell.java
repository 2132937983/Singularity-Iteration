package com.miophas.singularity_iteration.common.item.reactor;

import com.miophas.singularity_iteration.common.item.component.ReactorComponentData;
import com.miophas.singularity_iteration.common.reactor.ReactorInventory;
import com.miophas.singularity_iteration.common.registry.mio_icif_data_components;
import com.miophas.singularity_iteration.core.api.reactor.ReactorComponentType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * 锂单元（对齐 IC2 1.12.2 {@code ItemReactorLithiumCell}）。
 *
 * <p>行为：
 * <ul>
 *   <li>与中子反射板一样会放大相邻燃料棒的脉冲数。</li>
 *   <li>每收到一个中子脉冲，按 {@code 外壳堆温 / 3000} 累积增殖进度，上限 10000。</li>
 *   <li>进度满值后<b>直接变成氚燃料棒</b>（{@code normal/item_tritium_cell}），
 *       由 {@code ReactorInventory.write} 在同一循环内完成替换。</li>
 * </ul>
 * {@code ReactorComponentData} 的 {@code storedValue} 即 IC2 的 custom damage。
 */
@SuppressWarnings("null")
public class mio_icif_lithium_cell extends mio_icif_reactor {

    public static final int THRESHOLD = ReactorInventory.LITHIUM_THRESHOLD;

    public mio_icif_lithium_cell(Properties properties) {
        super(properties, THRESHOLD, ReactorComponentType.OTHER, false, true);
    }

    /** 已累积的增殖进度。 */
    public int getBredAmount(ItemStack stack) {
        ReactorComponentData data = stack.get(mio_icif_data_components.REACTOR_COMPONENT_DATA.get());
        return data == null ? 0 : data.storedValue();
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.mio_icif.reactor.component.breeding_progress",
            getBredAmount(stack), THRESHOLD).withStyle(ChatFormatting.GRAY));
    }
}
