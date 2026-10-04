package com.miophas.singularity_iteration.common.network;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 核爆炸蘑菇云动画数据包
 * 从服务端发送到客户端，触发客户端渲染蘑菇云动画
 */
public record NuclearExplosionAnimationPacket(double centerX, double centerY, double centerZ, int explosionRadius) 
        implements CustomPacketPayload {

    public NuclearExplosionAnimationPacket {
        if (explosionRadius < 1 || explosionRadius > 2000)
            throw new IllegalArgumentException("Invalid nuclear animation radius");
        com.miophas.singularity_iteration.core.runtime.reactor.BlastInput.nuke(
            centerX, centerY, centerZ, 1, explosionRadius);
    }

    public static final CustomPacketPayload.Type<NuclearExplosionAnimationPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "nuclear_explosion_animation"));

    public static final StreamCodec<FriendlyByteBuf, NuclearExplosionAnimationPacket> CODEC = 
        StreamCodec.composite(
            net.minecraft.network.codec.ByteBufCodecs.DOUBLE, NuclearExplosionAnimationPacket::centerX,
            net.minecraft.network.codec.ByteBufCodecs.DOUBLE, NuclearExplosionAnimationPacket::centerY,
            net.minecraft.network.codec.ByteBufCodecs.DOUBLE, NuclearExplosionAnimationPacket::centerZ,
            net.minecraft.network.codec.ByteBufCodecs.INT, NuclearExplosionAnimationPacket::explosionRadius,
            NuclearExplosionAnimationPacket::new
        );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * 处理数据包（在客户端执行）
     */
    public static void handle(NuclearExplosionAnimationPacket packet, IPayloadContext context) {
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return;
        }

        context.enqueueWork(() -> {
            com.miophas.singularity_iteration.common.client.ClientPacketHandlers.handleNuclearExplosionAnimation(
                packet.centerX(), packet.centerY(), packet.centerZ(), packet.explosionRadius());
        });
    }
}