// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.armory;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * S2C notification sent when an Armory deploys a Wingman, driving the client-side spectacle:
 * Armory silo hatch & railgun launch pulse, or high-altitude quantum transfer gate + orbital air-brake ring + body scan.
 */
public record WingmanDeployFxPacket(
    int entityId,
    double x, double y, double z,
    BlockPos armoryPos,
    boolean launch,
    byte mode,
    boolean livingLining,
    int staggerTicks,
    float dropHeight
) implements CustomPacketPayload {
    public static final Type<WingmanDeployFxPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "wingman_deploy_fx"));

    /** Set on the client by {@code WingmanDeployFx}. */
    public static volatile Consumer<WingmanDeployFxPacket> HOOK;

    public static final StreamCodec<RegistryFriendlyByteBuf, WingmanDeployFxPacket> CODEC = StreamCodec.of(
        (buf, p) -> {
            buf.writeVarInt(p.entityId);
            buf.writeDouble(p.x);
            buf.writeDouble(p.y);
            buf.writeDouble(p.z);
            buf.writeBlockPos(p.armoryPos);
            buf.writeBoolean(p.launch);
            buf.writeByte(p.mode);
            buf.writeBoolean(p.livingLining);
            buf.writeVarInt(p.staggerTicks);
            buf.writeFloat(p.dropHeight);
        },
        buf -> new WingmanDeployFxPacket(
            buf.readVarInt(),
            buf.readDouble(), buf.readDouble(), buf.readDouble(),
            buf.readBlockPos(),
            buf.readBoolean(),
            buf.readByte(),
            buf.readBoolean(),
            buf.readVarInt(),
            buf.readFloat()
        )
    );

    public Vec3 origin() { return new Vec3(x, y, z); }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void register(PayloadRegistrar registrar) {
        registrar.playToClient(TYPE, CODEC, (p, c) -> {
            if (FMLEnvironment.dist != Dist.CLIENT) return;
            c.enqueueWork(() -> {
                Consumer<WingmanDeployFxPacket> hook = HOOK;
                if (hook != null) hook.accept(p);
            });
        });
    }
}
