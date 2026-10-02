package com.miophas.singularity_iteration.common.armory;

import net.minecraft.ChatFormatting;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Armory Remote Controller.
 * <ul>
 *   <li>Sneak + right-click your Armory: pair the remote with it.</li>
 *   <li>Right-click: open the remote console (suit list, one-tap summon, history, EU, distance).</li>
 * </ul>
 */
public class ArmoryRemoteItem extends Item {
    public ArmoryRemoteItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) return InteractionResult.PASS;
        Level level = context.getLevel();
        if (!(level.getBlockEntity(context.getClickedPos()) instanceof mio_icif_armory armory)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (armory.owner() == null) {
            Component bind = armory.tryBind(player);
            if (bind != null) player.displayClientMessage(bind, true);
            if (armory.owner() == null) return InteractionResult.CONSUME;
        }
        if (!armory.isOwner(player)) {
            player.displayClientMessage(Component.translatable("message.mio_icif.armory.owned", armory.ownerName()), true);
            return InteractionResult.CONSUME;
        }
        context.getItemInHand().set(ArmoryComponents.TARGET.get(), GlobalPos.of(level.dimension(), context.getClickedPos().immutable()));
        player.displayClientMessage(Component.translatable("message.mio_icif.armory_remote.paired",
            context.getClickedPos().toShortString()), true);
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        GlobalPos target = stack.get(ArmoryComponents.TARGET.get());
        if (target == null) {
            if (!level.isClientSide) player.displayClientMessage(Component.translatable("message.mio_icif.armory_remote.unpaired"), true);
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            sp.openMenu(new SimpleMenuProvider((id, inv, p) -> new ArmoryRemoteMenu(id, inv, target, hand),
                    Component.translatable("item.mio_icif.normal.item_armory_remote")),
                buf -> { GlobalPos.STREAM_CODEC.encode(buf, target); buf.writeBoolean(hand == InteractionHand.OFF_HAND); });
            if (sp.containerMenu instanceof ArmoryRemoteMenu menu) menu.push(Component.empty());
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        GlobalPos target = stack.get(ArmoryComponents.TARGET.get());
        if (target != null) {
            tooltip.add(Component.translatable("item.mio_icif.armory_remote.target", target.pos().toShortString(),
                target.dimension().location().toString()).withStyle(ChatFormatting.AQUA));
        } else {
            tooltip.add(Component.translatable("item.mio_icif.armory_remote.tip.pair").withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("item.mio_icif.armory_remote.tip.open").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return false;
    }

    /** Sneak-use on an Armor Showcase must reach the showcase (to unlink it). */
    @Override
    public boolean doesSneakBypassUse(ItemStack stack, net.minecraft.world.level.LevelReader level, net.minecraft.core.BlockPos pos,
                                      net.minecraft.world.entity.player.Player player) {
        return level.getBlockState(pos).getBlock() instanceof ArmorShowcaseBlock;
    }
}
