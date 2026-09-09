package com.singularity_iteration.mio_icif.network;

import com.singularity_iteration.mio_icif.api.tool.ToggleableElectricTool;
import com.singularity_iteration.mio_icif.Singularity_Iteration;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 工具开关切杢数杮包
 * 客户端按下G键时坑逝此包到朝务�?
 * 朝务端收到坎切杢手挝工具的开关状态?
 */
@SuppressWarnings("null")
public record ToolTogglePacket() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ToolTogglePacket> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "tool_toggle"));

    public static final StreamCodec<FriendlyByteBuf, ToolTogglePacket> CODEC = StreamCodec.of(
        (buf, packet) -> {},
        buf -> new ToolTogglePacket()
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ToolTogglePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                ItemStack mainHand = serverPlayer.getMainHandItem();
                ItemStack offHand = serverPlayer.getOffhandItem();

                if (mainHand.getItem() instanceof ToggleableElectricTool toggleable) {
                    toggleable.toggleActive(mainHand, serverPlayer);
                } else if (offHand.getItem() instanceof ToggleableElectricTool toggleable) {
                    toggleable.toggleActive(offHand, serverPlayer);
                }
            }
        });
    }
}

