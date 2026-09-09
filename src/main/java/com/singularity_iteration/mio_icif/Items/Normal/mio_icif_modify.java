package com.singularity_iteration.mio_icif.Items.Normal;

import com.singularity_iteration.mio_icif.future.FutureCommodity;
import com.singularity_iteration.mio_icif.future.FutureCommodityManager;
import com.singularity_iteration.mio_icif.future.FutureMarketData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Random;

/**
 * 价格修改器物品
 * 
 * 使用方法：
 * - 主手持有 MODIFY
 * - 副手持有期货商品物品
 * - 右键点击刷新该商品今日的价格
 */
@SuppressWarnings("null")
public class mio_icif_modify extends Item {
    
    private static final Random RANDOM = new Random();
    
    public mio_icif_modify(Properties properties) {
        super(properties);
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            // 获取副手物品
            ItemStack offhandStack = player.getOffhandItem();
            
            if (offhandStack.isEmpty()) {
                serverPlayer.sendSystemMessage(
                    Component.translatable("message.mio_icif.modify.offhand_required").withStyle(ChatFormatting.RED)
                );
                return InteractionResultHolder.success(stack);
            }
            
            // 播放使用音效
            level.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(), 
                SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 1.0f, 1.0f);
            
            // 获取市场数据
            FutureMarketData marketData = FutureMarketData.get(level);
            if (marketData == null) {
                serverPlayer.sendSystemMessage(
                    Component.translatable("message.mio_icif.modify.no_market_data").withStyle(ChatFormatting.RED)
                );
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                return InteractionResultHolder.success(stack);
            }
            
            // 获取副手物品的物品ID
            String offhandItemId = offhandStack.getItem().toString();
            
            // 查找对应的期货商品
            FutureCommodity targetCommodity = null;
            List<FutureCommodity> commodities = FutureCommodityManager.getCommodities();
            
            for (FutureCommodity commodity : commodities) {
                if (commodity.getItemId().equals(offhandItemId)) {
                    targetCommodity = commodity;
                    break;
                }
            }
            
            if (targetCommodity == null) {
                serverPlayer.sendSystemMessage(
                    Component.translatable("message.mio_icif.modify.not_commodity").withStyle(ChatFormatting.RED)
                        .append(Component.literal(" (").withStyle(ChatFormatting.DARK_GRAY))
                        .append(Component.literal(offhandItemId).withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(")").withStyle(ChatFormatting.DARK_GRAY))
                );
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                return InteractionResultHolder.success(stack);
            }
            
            // 刷新价格（重新计算今日价格）
            int oldPrice = marketData.getPrice(targetCommodity.getItemId(), targetCommodity.getBasePrice());
            
            // 生成新价格（基于基础价格和波动率）
            float volatility = targetCommodity.getVolatility();
            float priceChange = (RANDOM.nextFloat() - 0.5f) * 2 * volatility;
            int newPrice = (int) Math.round(targetCommodity.getBasePrice() * (1 + priceChange));
            newPrice = Math.max(1, newPrice);
            
            // 更新价格
            marketData.setPrice(targetCommodity.getItemId(), newPrice);
            marketData.recordTodayPrice(targetCommodity.getItemId(), newPrice);
            
            // 发送成功消息
            int priceChangeAmount = newPrice - oldPrice;
            @SuppressWarnings("unused")
            ChatFormatting changeFormat = priceChangeAmount >= 0 ? ChatFormatting.GREEN : ChatFormatting.RED;
            String changeSign = priceChangeAmount >= 0 ? "+" : "";
            
            serverPlayer.sendSystemMessage(
                Component.translatable("message.mio_icif.modify.price_updated",
                    targetCommodity.getDisplayName(),
                    oldPrice,
                    newPrice,
                    changeSign + priceChangeAmount
                ).withStyle(ChatFormatting.GRAY)
            );
            
            // 消耗物品（创造模式除外）
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        
        return InteractionResultHolder.success(stack);
    }
    
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mio_icif.modify.usage").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.mio_icif.modify.usage_desc").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.mio_icif.modify.usage_desc2").withStyle(ChatFormatting.DARK_GRAY));
    }
}