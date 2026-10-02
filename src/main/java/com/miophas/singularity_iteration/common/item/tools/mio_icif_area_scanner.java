package com.miophas.singularity_iteration.common.item.tools;

import com.miophas.singularity_iteration.common.area.AreaPreviewService;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Area Scanner: shows the working range of SI machines in the world.
 * <ul>
 *   <li>Right-click a machine: toggle that machine's range.</li>
 *   <li>Right-click the air: show every ranged machine within 32 blocks.</li>
 *   <li>Sneak + right-click the air: hide all ranges.</li>
 * </ul>
 * Works without energy; the server computes the areas and only the user sees them.
 */
public class mio_icif_area_scanner extends Item {

    public mio_icif_area_scanner(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        if (context.getLevel().isClientSide) {
            return context.getLevel().getBlockEntity(context.getClickedPos())
                instanceof com.miophas.singularity_iteration.common.area.WorkAreaProvider ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer server && AreaPreviewService.sendFor(server, context.getClickedPos(), true)) {
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer server) {
            if (player.isShiftKeyDown()) {
                AreaPreviewService.clear(server);
                player.displayClientMessage(Component.translatable("message.mio_icif.area_scanner.cleared"), true);
            } else {
                int found = AreaPreviewService.sendNearby(server, AreaPreviewService.NEARBY_RADIUS);
                player.displayClientMessage(Component.translatable("message.mio_icif.area_scanner.found", found,
                    AreaPreviewService.NEARBY_RADIUS), true);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.mio_icif.item_tool_area_scanner.tooltip.machine").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.mio_icif.item_tool_area_scanner.tooltip.air").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.mio_icif.item_tool_area_scanner.tooltip.sneak").withStyle(ChatFormatting.DARK_GRAY));
    }
}
