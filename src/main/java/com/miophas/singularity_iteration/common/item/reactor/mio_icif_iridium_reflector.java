package com.miophas.singularity_iteration.common.item.reactor;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * 铱中子反射板（对齐 IC2 1.12.2 {@code ItemReactorIridiumReflector}）。
 *
 * <p>与中子反射板作用相同，但<b>永不磨损</b>：{@code ReactorCycle} 里 {@code Profile.reflector(0)}
 * 表示容量为 0 即无限耐久，因此不会像中子反射板那样消耗脉冲。
 * 爆炸威力加成与普通反射板一致（-1）。
 */
@SuppressWarnings("null")
public class mio_icif_iridium_reflector extends mio_icif_neutron_reflector {

    public mio_icif_iridium_reflector(Properties properties) {
        super(properties, 0);
    }

    /** 无限耐久，不显示耐久条。 */
    @Override
    public boolean isBarVisible(ItemStack stack) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.mio_icif.reactor.component.infinite_durability").withStyle(ChatFormatting.GRAY));
    }
}
