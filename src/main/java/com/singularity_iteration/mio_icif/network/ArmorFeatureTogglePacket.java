package com.singularity_iteration.mio_icif.network;

import com.singularity_iteration.mio_icif.Items.Armor.ArmorFeatureToggle;
import com.singularity_iteration.mio_icif.Singularity_Iteration;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
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
            buf.writeUtf(packet.featureKey);
            buf.writeBoolean(packet.enabled);
        },
        buf -> new ArmorFeatureTogglePacket(
            buf.readEnum(EquipmentSlot.class),
            buf.readUtf(),
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
            Player player = context.player();
            if (player == null) return;

            // 验证槽位是否匹配
            ItemStack stack = player.getItemBySlot(packet.slot);
            if (!stack.isEmpty()) {
                // 设置特性状态
                ArmorFeatureToggle.setEnabled(stack, packet.featureKey, packet.enabled);
            }
        });
    }
}