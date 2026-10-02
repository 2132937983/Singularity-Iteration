package com.miophas.singularity_iteration.common.armory;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Server -> client: contents of the Equipment Console's Armory section (the paired Armory's
 * suits and the network's showcases). The latest copy is kept in {@link #latest()}.
 */
public record ArmoryConsoleSyncPacket(boolean hasRemote, @Nullable ArmorySnapshot snapshot,
                                      List<ArmoryRemoteService.ShowcaseView> showcases, Component message) implements CustomPacketPayload {
    public static final Type<ArmoryConsoleSyncPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "armory_console_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ArmoryConsoleSyncPacket> CODEC = StreamCodec.of(
        (buf, p) -> {
            buf.writeBoolean(p.hasRemote);
            buf.writeBoolean(p.snapshot != null);
            if (p.snapshot != null) p.snapshot.write(buf);
            buf.writeVarInt(p.showcases.size());
            for (var v : p.showcases) {
                buf.writeBlockPos(v.pos());
                buf.writeUtf(v.label(), 64);
                for (ItemStack s : v.items()) ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, s);
            }
            ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buf, p.message);
        },
        buf -> {
            boolean has = buf.readBoolean();
            ArmorySnapshot snapshot = buf.readBoolean() ? ArmorySnapshot.read(buf) : null;
            int n = Math.min(64, buf.readVarInt());
            List<ArmoryRemoteService.ShowcaseView> views = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                BlockPos pos = buf.readBlockPos();
                String label = buf.readUtf(64);
                List<ItemStack> items = new ArrayList<>();
                for (int j = 0; j < ArmoryPiece.COLUMNS.length; j++) items.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
                views.add(new ArmoryRemoteService.ShowcaseView(pos, label, items));
            }
            return new ArmoryConsoleSyncPacket(has, snapshot, views, ComponentSerialization.TRUSTED_STREAM_CODEC.decode(buf));
        });

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    private static volatile ArmoryConsoleSyncPacket latest;
    private static volatile long receivedAt;
    private static volatile int version;

    /** Client: the last console contents received (null before the first request). */
    @Nullable public static ArmoryConsoleSyncPacket latest() { return latest; }
    public static long receivedAt() { return receivedAt; }
    public static int version() { return version; }

    public static void handle(ArmoryConsoleSyncPacket packet, IPayloadContext context) {
        if (FMLEnvironment.dist != Dist.CLIENT) return;
        context.enqueueWork(() -> {
            ArmoryConsoleSyncPacket previous = latest;
            // keep the last action message visible until a new one arrives
            if (packet.message.getString().isEmpty() && previous != null) {
                latest = new ArmoryConsoleSyncPacket(packet.hasRemote, packet.snapshot, packet.showcases, previous.message);
            } else {
                latest = packet;
            }
            receivedAt = System.currentTimeMillis();
            version++;
        });
    }
}
