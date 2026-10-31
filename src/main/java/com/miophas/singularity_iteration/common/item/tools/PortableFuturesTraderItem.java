package com.miophas.singularity_iteration.common.item.tools;

import com.miophas.singularity_iteration.common.Singularity_Iteration_Config;
import com.miophas.singularity_iteration.common.exchange.DataPlan;
import com.miophas.singularity_iteration.common.menu.tool.PortableFuturesMenu;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 0.1.10.38: Portable Futures Trader. Right-click opens the full futures panel (all tabs, the sell box and the settlement slot)
 * anywhere. Each exchange action costs the Futures Machine's energy fee from the trader's charge (tier 2, 100,000 EU), and the
 * player needs a paid data plan: 200 Industrial Coins per 30 market days by default ({@link DataPlan}, server config).
 */
public class PortableFuturesTraderItem extends mio_icif_tool_elc {
    public static final long MAX_ENERGY = 100_000L;
    public static final long CHARGE_RATE = 128L;
    public static final int TIER = 2;

    public PortableFuturesTraderItem(Properties properties) {
        super(properties.stacksTo(1), MAX_ENERGY, 0L, CHARGE_RATE, Singularity_Iteration_Config.FUTURE_ENERGY_PER_TRADE.getDefault(), TIER);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) open(serverPlayer, stack, hand);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private static void open(ServerPlayer player, ItemStack stack, InteractionHand hand) {
        if (!DataPlan.ensure(player)) {
            player.displayClientMessage(Component.translatable("message.mio_icif.portable_trader.plan_unpaid", DataPlan.price(), DataPlan.days()), true);
            return;
        }
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new PortableFuturesMenu(id, inventory, hand), stack.getHoverName()),
            buffer -> buffer.writeEnum(hand));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.mio_icif.portable_trader.use", Singularity_Iteration_Config.FUTURE_ENERGY_PER_TRADE.get())
            .withStyle(ChatFormatting.GRAY));
        long price = Singularity_Iteration_Config.PORTABLE_TRADER_PLAN_COINS.get();
        tooltip.add((price > 0 ? Component.translatable("tooltip.mio_icif.portable_trader.plan", price, Singularity_Iteration_Config.PORTABLE_TRADER_PLAN_DAYS.get())
            : Component.translatable("tooltip.mio_icif.portable_trader.plan_free")).withStyle(ChatFormatting.DARK_AQUA));
    }
}
