// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.item.resource;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * <p><b>EN:</b> Milk Wart (0.1.10.6). Eaten like food, it works like a bucket of milk: it removes the
 * status effects that milk cures and gives a little food. It is in the milk item tags
 * ({@code c:buckets/milk}, {@code c:drinks/milk}, {@code c:milks}, {@code forge:milk}), so recipes that ask
 * for milk by tag take it, and there is a cake recipe that uses it.
 * <p><b>ZH:</b> 牛奶疣（0.1.10.6）。可以像食物一样吃下，效果同一桶牛奶：清除牛奶能解除的状态效果，并恢复少量饱食度。
 * 它属于牛奶物品标签，按标签要求牛奶的配方都接受它，另有一个使用它的蛋糕配方。
 * <p><b>JA:</b> 牛乳イボ（0.1.10.6）。食べ物のように食べられ、効果は牛乳バケツと同じ：牛乳で消える状態効果を消し、満腹度を少し回復する。
 * 牛乳のアイテムタグに入るので、タグで牛乳を求めるレシピに使え、これを使うケーキのレシピもある。
 */
public class MilkWartItem extends Item {
    /** Nutrition 2, saturation 0.3; edible when full, so it can always cure. */
    public static final FoodProperties FOOD = new FoodProperties.Builder()
        .nutrition(2).saturationModifier(0.3F).alwaysEdible().build();

    public MilkWartItem(Properties properties) {
        super(properties.food(FOOD));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!level.isClientSide) entity.removeEffectsCuredBy(net.neoforged.neoforge.common.EffectCures.MILK);
        return result;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mio_icif.milk_wart").withStyle(net.minecraft.ChatFormatting.GRAY));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
