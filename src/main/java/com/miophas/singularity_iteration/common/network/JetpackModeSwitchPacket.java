// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.network;

import com.miophas.singularity_iteration.common.item.armor.ArmorFeatureSlots;
import com.miophas.singularity_iteration.core.api.item.IJetpackItem;
import com.miophas.singularity_iteration.core.prefab.item.ArmorFeatures;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Explicit client request to toggle the worn jetpack's hover mode. */
public record JetpackModeSwitchPacket() implements CustomPacketPayload {
    public static final Type<JetpackModeSwitchPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath("mio_icif", "jetpack_mode_switch"));
    public static final StreamCodec<FriendlyByteBuf, JetpackModeSwitchPacket> CODEC =
        StreamCodec.unit(new JetpackModeSwitchPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(JetpackModeSwitchPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            ItemStack stack = findModeTarget(player);
            if (!(stack.getItem() instanceof IJetpackItem jetpack)) return;
            if (!ArmorFeatures.cycleMode(stack, IJetpackItem.FEATURE_MODE_KEY)) return;

            // 背槽物品需要写回以触发 Curios 同步；胸甲槽由玩家装备槽自身同步
            if (stack != player.getItemBySlot(EquipmentSlot.CHEST)) {
                ArmorFeatureSlots.write(player, ArmorFeatureSlots.BACK, stack);
            }

            if (jetpack.getMode(stack) == IJetpackItem.JetpackMode.HOVER) {
                player.setDeltaMovement(player.getDeltaMovement().x, 0.0, player.getDeltaMovement().z);
                player.hurtMarked = true;
                player.sendSystemMessage(Component.translatable("hud.mio_icif.jetpack.hover_enabled"));
            } else {
                player.sendSystemMessage(Component.translatable("hud.mio_icif.jetpack.hover_disabled"));
            }
        });
    }

    /** The normal chest slot wins; otherwise use the first functional back slot. */
    public static ItemStack findModeTarget(Player player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.getCount() == 1 && chest.getItem() instanceof IJetpackItem) return chest;
        return ArmorFeatureSlots.backSlotStack(player);
    }
}
