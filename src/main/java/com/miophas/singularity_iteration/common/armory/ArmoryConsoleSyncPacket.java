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
 * Server to client: the contents of the Armory section of the Equipment Console. The contents
 * are the suits of the paired Armory and the showcases of the network.
 *
 * <p>When: {@code ArmoryRemoteService.push} sends one packet as the answer to each
 * {@link ArmoryConsolePacket}. The client asks once per 20 client ticks while the Armory tab is
 * open, and after each button click.
 *
 * <p>Fields:
 * <ul>
 *   <li>{@code hasRemote}: true when the player wears or carries an Armory Remote, paired or
 *       not.</li>
 *   <li>{@code snapshot}: state of the paired Armory, or null when no remote is paired. The
 *       snapshot status tells if the Armory is missing, unbound, owned by a different player,
 *       or available.</li>
 *   <li>{@code showcases}: showcases with their own outfit within
 *       {@code ArmoryRemoteService.PLAYER_RANGE} (24) blocks of the player or
 *       {@code ArmoryRemoteService.ARMORY_RANGE} (16) blocks of the Armory. Nearest first.
 *       Each entry has the block position, a label of at most 64 characters, and exactly
 *       {@code ArmoryPiece.COLUMNS.length} item stacks (empty stacks allowed).</li>
 *   <li>{@code wingmen}: the loaded Wingmen of the player (0.1.8.0), at most {@link #MAX_WINGMEN},
 *       each with the suit set, mode, state, health and energy fractions and the worn items.</li>
 *   <li>{@code message}: result text of the last action. Empty for a refresh.</li>
 * </ul>
 *
 * <p>Receiver validation: the decoder reads at most 64 showcases. The snapshot decoder limits
 * the number of suit sets and log entries. The message uses
 * {@code ComponentSerialization.TRUSTED_STREAM_CODEC}. This is correct only in this direction,
 * because the client trusts the server.
 *
 * <p>Client state: {@link #latest()} holds the last received contents. {@link #version()}
 * increments on each receive, so the screen can see a new copy. These fields are
 * {@code volatile}. The handler writes them on the client main thread.
 *
 * <p>Missing channel: the sender checks {@code hasChannel} and ignores send errors. Thus a fake
 * player that uses the console gets no packet.
 */
public record ArmoryConsoleSyncPacket(boolean hasRemote, @Nullable ArmorySnapshot snapshot,
                                      List<ArmoryRemoteService.ShowcaseView> showcases, List<WingmanService.View> wingmen,
                                      Component message) implements CustomPacketPayload {
    /** Maximum number of showcase entries in one packet. */
    public static final int MAX_SHOWCASES = 64;
    /** Maximum number of Wingman entries in one packet. */
    public static final int MAX_WINGMEN = 8;

    public static final Type<ArmoryConsoleSyncPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "armory_console_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ArmoryConsoleSyncPacket> CODEC = StreamCodec.of(
        (buf, p) -> {
            buf.writeBoolean(p.hasRemote);
            buf.writeBoolean(p.snapshot != null);
            if (p.snapshot != null) p.snapshot.write(buf);
            // Contract: at most MAX_SHOWCASES entries, each with exactly COLUMNS.length stacks.
            // The decoder rejects a larger count, so the encoder must cap it (0.1.7.36).
            int count = com.miophas.singularity_iteration.common.util.PacketLimits.writeCount(buf, p.showcases.size(), MAX_SHOWCASES);
            for (int i = 0; i < count; i++) {
                var v = p.showcases.get(i);
                buf.writeBlockPos(v.pos());
                String label = v.label();
                buf.writeUtf(label.length() > 64 ? label.substring(0, 64) : label, 64);
                for (int j = 0; j < ArmoryPiece.COLUMNS.length; j++) {
                    ItemStack s = j < v.items().size() ? v.items().get(j) : ItemStack.EMPTY;
                    ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, s);
                }
            }
            int wingmen = com.miophas.singularity_iteration.common.util.PacketLimits.writeCount(buf, p.wingmen.size(), MAX_WINGMEN);
            for (int i = 0; i < wingmen; i++) p.wingmen.get(i).write(buf);
            ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buf, p.message);
        },
        buf -> {
            boolean has = buf.readBoolean();
            ArmorySnapshot snapshot = buf.readBoolean() ? ArmorySnapshot.read(buf) : null;
            int n = com.miophas.singularity_iteration.common.util.PacketLimits.readCount(buf, MAX_SHOWCASES, "showcase");
            List<ArmoryRemoteService.ShowcaseView> views = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                BlockPos pos = buf.readBlockPos();
                String label = buf.readUtf(64);
                List<ItemStack> items = new ArrayList<>();
                for (int j = 0; j < ArmoryPiece.COLUMNS.length; j++) items.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
                views.add(new ArmoryRemoteService.ShowcaseView(pos, label, items));
            }
            int w = com.miophas.singularity_iteration.common.util.PacketLimits.readCount(buf, MAX_WINGMEN, "wingman");
            List<WingmanService.View> wingmen = new ArrayList<>();
            for (int i = 0; i < w; i++) wingmen.add(WingmanService.View.read(buf));
            return new ArmoryConsoleSyncPacket(has, snapshot, views, wingmen, ComponentSerialization.TRUSTED_STREAM_CODEC.decode(buf));
        });

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    private static volatile ArmoryConsoleSyncPacket latest;
    private static volatile long receivedAt;
    private static volatile int version;

    /** Client: the last received console contents. Null before the first answer. */
    @Nullable public static ArmoryConsoleSyncPacket latest() { return latest; }
    /** Client: wall-clock time (ms) of the last receive. 0 before the first answer. */
    public static long receivedAt() { return receivedAt; }
    /** Client: receive counter. It increments by 1 on each receive. */
    public static int version() { return version; }

    /**
     * Client handler. It runs on the client main thread (default handler thread).
     * {@code enqueueWork} runs the task at once on that thread. A dedicated server ignores the
     * payload.
     */
    public static void handle(ArmoryConsoleSyncPacket packet, IPayloadContext context) {
        if (FMLEnvironment.dist != Dist.CLIENT) return;
        context.enqueueWork(() -> {
            ArmoryConsoleSyncPacket previous = latest;
            // A refresh has an empty message. Keep the last action message until a new one arrives.
            if (packet.message.getString().isEmpty() && previous != null) {
                latest = new ArmoryConsoleSyncPacket(packet.hasRemote, packet.snapshot, packet.showcases, packet.wingmen, previous.message);
            } else {
                latest = packet;
            }
            receivedAt = System.currentTimeMillis();
            version++;
        });
    }
}
