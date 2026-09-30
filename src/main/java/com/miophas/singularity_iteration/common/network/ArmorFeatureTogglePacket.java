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
 * 装备特性（开关型）切换网络包。
 *
 * <p>客户端切换后同步到服务端。槽位使用通用槽位标识（head/chest/legs/feet/back），
 * 由 {@link ArmorFeatureSlots} 解析，因此原版护甲与 Curios 背槽装备共用同一路径。
 */
@SuppressWarnings("null")
public record ArmorFeatureTogglePacket(String slotId, String featureKey, boolean enabled) implements CustomPacketPayload {

    public static final Type<ArmorFeatureTogglePacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "armor_feature_toggle")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ArmorFeatureTogglePacket> CODEC = StreamCodec.of(
        (buf, packet) -> {
            buf.writeUtf(packet.slotId == null ? "" : packet.slotId, 32);
            buf.writeUtf(packet.featureKey == null ? "" : packet.featureKey, 64);
            buf.writeBoolean(packet.enabled);
        },
        buf -> new ArmorFeatureTogglePacket(
            buf.readUtf(32),
            buf.readUtf(64),
            buf.readBoolean()
        )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** 处理包。 */
    public static void handle(ArmorFeatureTogglePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (packet.featureKey.isEmpty()) return;

            String slotId = packet.slotId;
            ItemStack stack = ArmorFeatureSlots.stackById(player, slotId);
            if (!ArmorFeatures.isToggleFeature(stack, null, packet.featureKey)) {
                // 回退：物品声明的槽位与实际所在槽位不一致时（例如背槽物品声明 CHEST），全槽位查找
                slotId = ArmorFeatureSlots.findDeclaringSlotId(player, packet.featureKey);
                if (slotId == null) return;
                stack = ArmorFeatureSlots.stackById(player, slotId);
                if (!ArmorFeatures.isToggleFeature(stack, null, packet.featureKey)) return;
            }

            ArmorFeatures.setEnabled(stack, packet.featureKey, packet.enabled);
            ArmorFeatureSlots.write(player, slotId, stack);
        });
    }
}
