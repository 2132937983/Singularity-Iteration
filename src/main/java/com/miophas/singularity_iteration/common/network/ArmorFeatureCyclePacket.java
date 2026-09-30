package com.miophas.singularity_iteration.common.network;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.item.armor.ArmorFeatureSlots;
import com.miophas.singularity_iteration.core.prefab.item.ArmorFeatures;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 装备特性（模式型）循环网络包。
 *
 * <p>用于通用模式切换：物品声明 {@code isMode=true} 的特性并实现
 * {@code IArmorModeFeatureItem}（喷气背包经 {@code IJetpackItem} 已内置默认实现）。
 */
@SuppressWarnings("null")
public record ArmorFeatureCyclePacket(String slotId, String featureKey) implements CustomPacketPayload {

    public static final Type<ArmorFeatureCyclePacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "armor_feature_cycle")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ArmorFeatureCyclePacket> CODEC = StreamCodec.of(
        (buf, packet) -> {
            buf.writeUtf(packet.slotId == null ? "" : packet.slotId, 32);
            buf.writeUtf(packet.featureKey == null ? "" : packet.featureKey, 64);
        },
        buf -> new ArmorFeatureCyclePacket(buf.readUtf(32), buf.readUtf(64))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** 处理包。 */
    public static void handle(ArmorFeatureCyclePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (packet.featureKey.isEmpty()) return;

            String slotId = packet.slotId;
            ItemStack stack = ArmorFeatureSlots.stackById(player, slotId);
            if (!ArmorFeatures.isModeFeature(stack, null, packet.featureKey)) {
                slotId = ArmorFeatureSlots.findDeclaringSlotId(player, packet.featureKey);
                if (slotId == null) return;
                stack = ArmorFeatureSlots.stackById(player, slotId);
                if (!ArmorFeatures.isModeFeature(stack, null, packet.featureKey)) return;
            }

            if (ArmorFeatures.cycleMode(stack, packet.featureKey)) {
                ArmorFeatureSlots.write(player, slotId, stack);
            }
        });
    }
}
