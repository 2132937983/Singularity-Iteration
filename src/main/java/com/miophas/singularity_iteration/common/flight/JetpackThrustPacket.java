// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.flight;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * <p><b>EN:</b> Server to client: the jetpack thrust state of another player changed (0.1.8.1).
 * The client uses it for the thruster sound of remote players. The local player predicts its
 * own sound and does not get this packet.
 * <ul>
 *   <li>{@code entityId}: the flying player.</li>
 *   <li>{@code state}: {@link JetpackThrustSync#pack} value; 0 = off, else throttle level 1..3
 *       (bits 0-3) and sound profile index (bits 4-7).</li>
 * </ul>
 * <p>Rate: one packet per change, only to the players that track the flier
 * ({@link JetpackThrustSync}). A steady flight sends nothing.
 *
 * <p><b>ZH:</b> 服务器到客户端：其他玩家的喷气背包推力状态改变（0.1.8.1）。客户端用它播放远程玩家的推进器音效。
 * 本地玩家自行预测自己的音效，不接收此数据包。{@code state}：0 = 关闭，否则为油门档位 1..3（位 0-3）与音效档索引（位 4-7）。
 * 频率：每次变化发送一个数据包，只发给追踪该玩家的玩家。稳定飞行时不发送。
 *
 * <p><b>JA:</b> サーバーからクライアントへ：他プレイヤーのジェットパック推力状態が変わった（0.1.8.1）。クライアントは
 * リモートプレイヤーのスラスター音に使う。ローカルプレイヤーは自分の音を予測するので、このパケットを受け取らない。
 * {@code state}：0 = 停止、それ以外はスロットル段階 1..3（ビット 0-3）と音プロファイル番号（ビット 4-7）。
 * 頻度：変化ごとに 1 パケット、飛行者を追跡しているプレイヤーにだけ送る。定常飛行中は何も送らない。
 */
public record JetpackThrustPacket(int entityId, int state) implements CustomPacketPayload {
    public static final Type<JetpackThrustPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "jetpack_thrust"));

    public static final StreamCodec<RegistryFriendlyByteBuf, JetpackThrustPacket> CODEC = StreamCodec.of(
        (buf, p) -> {
            buf.writeVarInt(p.entityId);
            buf.writeByte(p.state);
        },
        buf -> new JetpackThrustPacket(buf.readVarInt(), buf.readUnsignedByte()));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(JetpackThrustPacket packet, IPayloadContext context) {
        if (FMLEnvironment.dist != Dist.CLIENT) return;
        context.enqueueWork(() -> com.miophas.singularity_iteration.common.client.flight.JetpackSoundClient.remoteState(packet.entityId, packet.state));
    }
}
