// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.armory.blade;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import java.util.function.Consumer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 0.1.11.236 (SlashBlade collab B4): the singularity blade's event horizon, server to client: where the micro-singularity
 * opens, its horizon radius in blocks and the tick of its Hawking burst. The client bends the picture around it
 * ({@code SingularityLens}). The timing envelopes are here so the GameTests check them without a client.
 */
public record SingularityFxPacket(double x, double y, double z, float radius, int burstTick) implements CustomPacketPayload {
    public static final Type<SingularityFxPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "singularity_fx"));

    /** Set on the client by {@code SingularityLens}. */
    public static volatile Consumer<SingularityFxPacket> HOOK;

    /** Ticks the horizon takes to open, to close after the burst, and the burst's flash. */
    public static final int OPEN = 6, CLOSE = 4, FLASH = 10;

    public static final StreamCodec<RegistryFriendlyByteBuf, SingularityFxPacket> CODEC = StreamCodec.of(
        (buf, p) -> {
            buf.writeDouble(p.x);
            buf.writeDouble(p.y);
            buf.writeDouble(p.z);
            buf.writeFloat(p.radius);
            buf.writeVarInt(p.burstTick);
        },
        buf -> new SingularityFxPacket(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readFloat(), buf.readVarInt()));

    public Vec3 center() { return new Vec3(x, y, z); }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    /** How open the horizon is {@code age} ticks after it appeared (0..1): opens, holds until the burst, closes. */
    public static float strength(float age, int burstTick) {
        if (age < 0) return 0;
        if (age < burstTick) return Mth.clamp(age / OPEN, 0, 1);
        return Mth.clamp(1 - (age - burstTick) / CLOSE, 0, 1);
    }

    /** The Hawking burst's flash {@code age} ticks after the horizon appeared (0..1). */
    public static float burst(float age, int burstTick) {
        float t = age - burstTick;
        return t < 0 || t > FLASH ? 0 : 1 - t / FLASH;
    }

    /** True while the effect still shows. */
    public static boolean alive(float age, int burstTick) {
        return age <= burstTick + Math.max(CLOSE, FLASH);
    }

    public static void register(PayloadRegistrar registrar) {
        registrar.playToClient(TYPE, CODEC, (p, c) -> {
            if (FMLEnvironment.dist != Dist.CLIENT) return;
            c.enqueueWork(() -> {
                Consumer<SingularityFxPacket> hook = HOOK;
                if (hook != null) hook.accept(p);
            });
        });
    }
}
