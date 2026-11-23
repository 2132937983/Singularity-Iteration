// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.builder;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 0.1.11.182: the packets of a blueprint upload ({@link BlueprintUploads}). Client to server: {@link Begin} (the open builder
 * menu's id, the file name, its size and SHA-256) and the {@link Piece}s in order; server to client: {@link Status} (accepted,
 * done, or why not). Every value from the client is checked again on the server: the menu must be the sender's open
 * Blueprint Builder menu, the name a plain file name, the size within the limit, the pieces in order and at most
 * {@link BlueprintUploads#CHUNK} bytes, the whole matching its hash and readable as a blueprint.
 */
public final class BuilderUploadPackets {
    private BuilderUploadPackets() {}

    /** Client side: what to do with a status (set by the client setup; the server never calls it). */
    public static volatile java.util.function.BiConsumer<Integer, Integer> CLIENT_HOOK = (code, extra) -> { };

    public record Begin(int containerId, String name, int size, byte[] sha) implements CustomPacketPayload {
        public static final Type<Begin> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "builder_upload_begin"));
        public static final StreamCodec<FriendlyByteBuf, Begin> CODEC = StreamCodec.of(
            (buf, p) -> { buf.writeVarInt(p.containerId); buf.writeUtf(p.name, 256); buf.writeVarInt(p.size); buf.writeByteArray(p.sha); },
            buf -> new Begin(buf.readVarInt(), buf.readUtf(256), buf.readVarInt(), buf.readByteArray(32)));

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

        static void handle(Begin p, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (!(context.player() instanceof ServerPlayer player)) return;
                var open = player.containerMenu;
                int code;
                if (open == null || open.containerId != p.containerId || !open.stillValid(player)
                    || !(open instanceof BlueprintBuilderMenu menu) || !(menu.getBlockEntity() instanceof BlueprintBuilderBlockEntity builder)) code = BlueprintUploads.ERR_MENU;
                else code = BlueprintUploads.begin(player, builder, p.name, p.size, p.sha);
                reply(player, code, p.name);
            });
        }
    }

    public record Piece(int offset, byte[] data) implements CustomPacketPayload {
        public static final Type<Piece> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "builder_upload_piece"));
        public static final StreamCodec<FriendlyByteBuf, Piece> CODEC = StreamCodec.of(
            (buf, p) -> { buf.writeVarInt(p.offset); buf.writeByteArray(p.data); },
            buf -> new Piece(buf.readVarInt(), buf.readByteArray(BlueprintUploads.CHUNK)));

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

        static void handle(Piece p, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (!(context.player() instanceof ServerPlayer player)) return;
                int code = BlueprintUploads.chunk(player, p.offset, p.data);
                if (code != BlueprintUploads.MORE) reply(player, code, "");
            });
        }
    }

    /** The result of a begin or of the last piece; {@code extra} is the size limit in KB with {@code ERR_SIZE}. */
    public record Status(int code, int extra) implements CustomPacketPayload {
        public static final Type<Status> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "builder_upload_status"));
        public static final StreamCodec<FriendlyByteBuf, Status> CODEC = StreamCodec.of(
            (buf, p) -> { buf.writeVarInt(p.code); buf.writeVarInt(p.extra); },
            buf -> new Status(buf.readVarInt(), buf.readVarInt()));

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

        static void handle(Status p, IPayloadContext context) {
            context.enqueueWork(() -> CLIENT_HOOK.accept(p.code, p.extra));
        }
    }

    private static void reply(ServerPlayer player, int code, String name) {
        int extra = code == BlueprintUploads.ERR_SIZE ? BlueprintUploads.maxBytes() / 1024 : 0;
        PacketDistributor.sendToPlayer(player, new Status(code, extra));
        if (code >= BlueprintUploads.ERR_DISABLED)
            player.displayClientMessage(Component.translatable("message.mio_icif.builder.upload.error." + code, extra), false);
    }

    public static void register(PayloadRegistrar registrar) {
        registrar.playToServer(Begin.TYPE, Begin.CODEC, Begin::handle);
        registrar.playToServer(Piece.TYPE, Piece.CODEC, Piece::handle);
        registrar.playToClient(Status.TYPE, Status.CODEC, Status::handle);
    }
}
