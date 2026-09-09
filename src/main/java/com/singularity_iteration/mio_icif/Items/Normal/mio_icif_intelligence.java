package com.singularity_iteration.mio_icif.Items.Normal;

import com.singularity_iteration.mio_icif.future.FutureCommodity;
import com.singularity_iteration.mio_icif.future.FutureCommodityManager;
import com.singularity_iteration.mio_icif.future.FutureMarketData;
import com.singularity_iteration.mio_icif.future.event.FuturePriceEvent;
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
 * 情报物品
 * 
 * 右击使用：显示当天期货机中某个商品的某个事件信息到聊天栏
 */
@SuppressWarnings("null")
public class mio_icif_intelligence extends Item {
    
    private static final Random RANDOM = new Random();
    
    public mio_icif_intelligence(Properties properties) {
        super(properties);
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            // 播放使用音效
            level.playSound(null, serverPlayer.blockPosition(), 
                SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.5f, 1.0f);
            
            // 获取所有货品
            List<FutureCommodity> commodities = FutureCommodityManager.getCommodities();
            
            if (commodities.isEmpty()) {
                // 如果没有货品，发送提示
                serverPlayer.sendSystemMessage(
                    Component.translatable("message.mio_icif.intelligence.no_commodities").withStyle(ChatFormatting.RED)
                );
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                return InteractionResultHolder.success(stack);
            }
            
            // 获取市场数据
            FutureMarketData marketData = FutureMarketData.get(level);
            if (marketData == null) {
                serverPlayer.sendSystemMessage(
                    Component.translatable("message.mio_icif.intelligence.no_market_data").withStyle(ChatFormatting.RED)
                );
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                return InteractionResultHolder.success(stack);
            }
            
            // 遍历所有货品，找到有事件的货品
            FutureCommodity selectedCommodity = null;
            List<FuturePriceEvent> selectedEvents = null;
            
            // 尝试最多查找 commodities.size() 次，找到一个有事件的货品
            int maxAttempts = commodities.size();
            for (int i = 0; i < maxAttempts; i++) {
                FutureCommodity commodity = commodities.get(RANDOM.nextInt(commodities.size()));
                List<FuturePriceEvent> events = marketData.getEvents(commodity.getItemId());
                
                if (!events.isEmpty()) {
                    selectedCommodity = commodity;
                    selectedEvents = events;
                    break;
                }
            }
            
            if (selectedCommodity == null || selectedEvents == null) {
                serverPlayer.sendSystemMessage(
                    Component.translatable("message.mio_icif.intelligence.no_events")
                );
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                return InteractionResultHolder.success(stack);
            }
            
            // 随机选择一个事件
            FuturePriceEvent event = selectedEvents.get(RANDOM.nextInt(selectedEvents.size()));
            
            // 显示事件信息
            Component eventInfo = getEventInfo(event, selectedCommodity, marketData);
            serverPlayer.sendSystemMessage(eventInfo);
            
            // 消耗物品（创造模式除外）
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        
        return InteractionResultHolder.success(stack);
    }
    
    /**
     * 获取事件信息显示
     */
    private Component getEventInfo(FuturePriceEvent event, FutureCommodity commodity, FutureMarketData marketData) {
        long currentDay = marketData.getCurrentDay();
        int currentPrice = marketData.getPrice(commodity.getItemId(), commodity.getBasePrice());
        int remainingDays = event.getRemainingDays(currentDay);
        
        // 事件类型
        String eventTypeName = getEventTypeName(event.getType());
        
        // 事件强度
        String intensityStr = String.format("%.1f", event.getIntensity() * 100) + "%";
        
        // 构建完整消息
        return Component.translatable("message.mio_icif.intelligence.event_info_header")
            .append(Component.literal(commodity.getDisplayName()).withStyle(ChatFormatting.GOLD))
            .append(Component.literal(" - ").withStyle(ChatFormatting.WHITE))
            .append(Component.translatable(eventTypeName).withStyle(ChatFormatting.AQUA))
            .append(Component.translatable("message.mio_icif.intelligence.current_price", currentPrice).withStyle(ChatFormatting.WHITE))
            .append(Component.translatable("message.mio_icif.intelligence.remaining_days", remainingDays).withStyle(ChatFormatting.WHITE))
            .append(Component.translatable("message.mio_icif.intelligence.intensity", intensityStr).withStyle(ChatFormatting.WHITE));
    }
    
    /**
     * 获取事件类型的显示名称
     */
    private String getEventTypeName(FuturePriceEvent.EventType type) {
        return switch (type) {
            case RISING_TREND -> "gui.mio_icif.future.event_rising";
            case FALLING_TREND -> "gui.mio_icif.future.event_falling";
            case HIGH_VOLATILITY -> "gui.mio_icif.future.event_high_volatility";
            case LOW_VOLATILITY -> "gui.mio_icif.future.event_low_volatility";
            case PRICE_STABILIZATION -> "gui.mio_icif.future.event_stabilization";
            case SHORT_BUBBLE -> "gui.mio_icif.future.event_short_bubble";
            case LONG_BUBBLE -> "gui.mio_icif.future.event_long_bubble";
            case SHORT_CRASH_REBOUND -> "gui.mio_icif.future.event_short_crash";
            case LONG_CRASH_REBOUND -> "gui.mio_icif.future.event_long_crash";
            default -> "gui.mio_icif.future.event_unknown";
        };
    }
    
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mio_icif.intelligence.usage").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.mio_icif.intelligence.usage_desc").withStyle(ChatFormatting.DARK_GRAY));
    }
}