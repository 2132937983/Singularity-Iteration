package com.miophas.singularity_iteration.common.network;

import com.miophas.singularity_iteration.common.item.armor.ArmorFeatureToggle;
import com.miophas.singularity_iteration.common.Singularity_Iteration;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 装备特性切换网络包
 * 客户端切换特性后同步到服务端
 */
@SuppressWarnings("null")
public record ArmorFeatureTogglePacket(EquipmentSlot slot, String featureKey, boolean enabled) implements CustomPacketPayload {

    public static final Type<ArmorFeatureTogglePacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "armor_feature_toggle")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ArmorFeatureTogglePacket> CODEC = StreamCodec.of(
        (buf, packet) -> {
            buf.writeEnum(packet.slot);
             buf.writeUtf(packet.featureKey == null ? "" : packet.featureKey, 64);
            buf.writeBoolean(packet.enabled);
        },
        buf -> new ArmorFeatureTogglePacket(
            buf.readEnum(EquipmentSlot.class),
             buf.readUtf(64),
            buf.readBoolean()
        )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * 处理包
     */
    public static void handle(ArmorFeatureTogglePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (packet.slot != EquipmentSlot.HEAD && packet.slot != EquipmentSlot.CHEST
                    && packet.slot != EquipmentSlot.LEGS && packet.slot != EquipmentSlot.FEET) return;

            // 验证槽位是否匹配
            ItemStack stack = player.getItemBySlot(packet.slot);
            if (ArmorFeatureToggle.isToggleFeature(stack, packet.slot, packet.featureKey)) {
                // 设置特性状态
                ArmorFeatureToggle.setEnabled(stack, packet.featureKey, packet.enabled);
            }
        });
    }
}
