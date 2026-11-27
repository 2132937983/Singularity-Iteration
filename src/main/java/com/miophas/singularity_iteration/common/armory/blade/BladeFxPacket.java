// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.armory.blade;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import java.util.function.Consumer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Unified server-to-client visual effect trigger for the three SlashBlade collab blades.
 * Lives in {@code common.armory.blade} with zero SlashBlade imports so soft-dependency isolation and GameTests stay intact.
 */
public record BladeFxPacket(
    byte kind,
    double x, double y, double z,
    float dx, float dy, float dz,
    float p1, float p2,
    int flags,
    int[] entityIds
) implements CustomPacketPayload {

    public static final byte NANO_WAVE = 1;
    public static final byte NANO_SLASH = 2;
    public static final byte NANO_HIT = 3;
    public static final byte NANO_OVERHEAT = 4;
    public static final byte NANO_HEAT_ARC = 5;

    public static final byte QUANTUM_CAST = 10;
    public static final byte QUANTUM_COLLAPSE = 11;
    public static final byte QUANTUM_ENTANGLE_HIT = 12;
    public static final byte QUANTUM_ENTANGLE_MARK = 13;

    public static final byte SINGULARITY_TIDAL = 21;
    public static final byte SINGULARITY_UNSTABLE = 22;

    public static final int FLAG_NORMAL = 0;
    public static final int FLAG_JUST = 1;
    public static final int FLAG_SUPER = 2;
    public static final int FLAG_RED_HOT = 1 << 2;
    public static final int FLAG_CRITICAL = 1 << 3;

    public static final int[] EMPTY_IDS = new int[0];

    public static final Type<BladeFxPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "blade_fx"));

    /** Set on the client by {@code BladeFxRenderer}. */
    public static volatile Consumer<BladeFxPacket> HOOK;

    public static final StreamCodec<RegistryFriendlyByteBuf, BladeFxPacket> CODEC = StreamCodec.of(
        (buf, p) -> {
            buf.writeByte(p.kind);
            buf.writeDouble(p.x);
            buf.writeDouble(p.y);
            buf.writeDouble(p.z);
            buf.writeFloat(p.dx);
            buf.writeFloat(p.dy);
            buf.writeFloat(p.dz);
            buf.writeFloat(p.p1);
            buf.writeFloat(p.p2);
            buf.writeVarInt(p.flags);
            int[] ids = p.entityIds == null ? EMPTY_IDS : p.entityIds;
            int n = Math.min(ids.length, 32);
            buf.writeVarInt(n);
            for (int i = 0; i < n; i++) buf.writeVarInt(ids[i]);
        },
        buf -> {
            byte kind = buf.readByte();
            double x = buf.readDouble(), y = buf.readDouble(), z = buf.readDouble();
            float dx = buf.readFloat(), dy = buf.readFloat(), dz = buf.readFloat();
            float p1 = buf.readFloat(), p2 = buf.readFloat();
            int flags = buf.readVarInt();
            int n = Math.max(0, Math.min(buf.readVarInt(), 32));
            int[] ids = n == 0 ? EMPTY_IDS : new int[n];
            for (int i = 0; i < n; i++) ids[i] = buf.readVarInt();
            return new BladeFxPacket(kind, x, y, z, dx, dy, dz, p1, p2, flags, ids);
        }
    );

    public static BladeFxPacket of(byte kind, Vec3 pos, Vec3 dir, float p1, float p2, int flags) {
        return new BladeFxPacket(kind, pos.x, pos.y, pos.z, (float) dir.x, (float) dir.y, (float) dir.z, p1, p2, flags, EMPTY_IDS);
    }

    public static BladeFxPacket of(byte kind, Vec3 pos, Vec3 dir, float p1, float p2, int flags, int[] entityIds) {
        return new BladeFxPacket(kind, pos.x, pos.y, pos.z, (float) dir.x, (float) dir.y, (float) dir.z, p1, p2, flags, entityIds);
    }

    public Vec3 pos() { return new Vec3(x, y, z); }

    public Vec3 dir() { return new Vec3(dx, dy, dz); }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void register(PayloadRegistrar registrar) {
        registrar.playToClient(TYPE, CODEC, (p, c) -> {
            if (FMLEnvironment.dist != Dist.CLIENT) return;
            c.enqueueWork(() -> {
                Consumer<BladeFxPacket> hook = HOOK;
                if (hook != null) hook.accept(p);
            });
        });
    }
}
