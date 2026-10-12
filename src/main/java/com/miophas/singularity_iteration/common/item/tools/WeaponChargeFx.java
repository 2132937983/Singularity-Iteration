// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.item.tools;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Charge and compression effect of the energy weapons (0.1.8.0). When a gun fires, the energy
 * of the shot gathers at the muzzle: a ring of sparks converges into a bright core, then a flash
 * releases the bolt. The size and the number of particles grow with the energy of the shot
 * ({@link #power}: about 0.6 at 1 000 EU, 0.8 at 10 000 EU, 1.0 at 100 000 EU).
 *
 * <p>The server sends one {@link Shot} packet per shot to the shooter and the players that track
 * the shooter. The client draws the particles; no server particles, no entity.
 */
public final class WeaponChargeFx {
    private WeaponChargeFx() {}

    /** 0..1 from the energy of one shot (logarithmic). */
    public static float power(long eu) {
        return (float) Math.max(0.15, Math.min(1.0, Math.log10(Math.max(1, eu)) / 5.0));
    }

    /** Server: sends the effect of a shot of {@code eu} EU from {@code hand}, colour 0xRRGGBB. */
    public static void emit(Player player, InteractionHand hand, long eu, int color) {
        if (!(player instanceof ServerPlayer sp)) return;
        try {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(sp, new Shot(sp.getId(), hand == InteractionHand.MAIN_HAND, power(eu), color));
        } catch (RuntimeException ignored) {
            // fake players have no connection
        }
    }

    /** Server to client: one shot effect. Fields: shooter id, hand, power 0..1, colour. */
    public record Shot(int entityId, boolean mainHand, float power, int color) implements CustomPacketPayload {
        public static final Type<Shot> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "weapon_charge_fx"));
        public static final StreamCodec<ByteBuf, Shot> CODEC = StreamCodec.of((b, p) -> {
            FriendlyByteBuf f = new FriendlyByteBuf(b);
            f.writeVarInt(p.entityId); f.writeBoolean(p.mainHand); f.writeFloat(p.power); f.writeInt(p.color);
        }, b -> {
            FriendlyByteBuf f = new FriendlyByteBuf(b);
            int id = f.readVarInt();
            boolean main = f.readBoolean();
            float power = f.readFloat();
            int color = f.readInt();
            return new Shot(id, main, Float.isFinite(power) ? Math.max(0, Math.min(1, power)) : 0.5F, color);
        });
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

        public static void handle(Shot shot, IPayloadContext context) {
            if (net.neoforged.fml.loading.FMLEnvironment.dist != net.neoforged.api.distmarker.Dist.CLIENT) return;
            context.enqueueWork(() -> com.miophas.singularity_iteration.common.client.render.WeaponChargeFxClient.play(shot));
        }
    }
}
