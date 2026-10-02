package com.miophas.singularity_iteration.common.armory;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Connector Kit: fits equipment from other mods with an Armory link.
 *
 * <p>Hold the kit in the main hand and the equipment in the off hand, then use the kit.
 * Armour docks to the slot the item declares; anything else becomes a main-hand piece
 * (sneak while using to bind it as an off-hand piece instead). One kit per item.
 */
public class ArmoryConnectorItem extends Item {
    public ArmoryConnectorItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    /** Binds {@code target}; returns the message key describing the outcome. */
    public static String bind(ItemStack target, boolean offhandPiece) {
        if (target.isEmpty()) return "message.mio_icif.armory_connector.no_target";
        if (ArmoryRules.isLinked(target)) return "message.mio_icif.armory_connector.already";
        if (ArmoryRules.isNative(target)) return "message.mio_icif.armory_connector.native";
        EquipmentSlot natural = ArmoryRules.naturalSlot(target);
        ArmoryPiece piece = natural != null && natural.getType() == EquipmentSlot.Type.HUMANOID_ARMOR
            ? ArmoryPiece.of(natural) : offhandPiece ? ArmoryPiece.OFFHAND : ArmoryPiece.MAINHAND;
        target.set(ArmoryComponents.LINK.get(), new ArmoryLink(piece.name().toLowerCase(java.util.Locale.ROOT)));
        return "message.mio_icif.armory_connector.bound";
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack kit = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.pass(kit);
        ItemStack target = player.getOffhandItem();
        if (!level.isClientSide) {
            String result = bind(target, player.isShiftKeyDown());
            player.displayClientMessage(Component.translatable(result, target.getHoverName()), true);
            if (result.endsWith(".bound")) {
                if (!player.getAbilities().instabuild) kit.shrink(1);
                level.playSound(null, player.blockPosition(), SoundEvents.SMITHING_TABLE_USE, SoundSource.PLAYERS, 0.8F, 1.4F);
            }
        }
        return InteractionResultHolder.sidedSuccess(kit, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.mio_icif.armory_connector.tip.use").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.mio_icif.armory_connector.tip.sneak").withStyle(ChatFormatting.DARK_GRAY));
    }
}
