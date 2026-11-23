// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.builder;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 0.1.11.184: the Blueprint Builder's plan for the clients' block hologram (as a schematic mod shows the blocks still to
 * place). The plan lives on the server (the blueprint file may be the server's own): a client near a builder whose hologram
 * is on asks for it with {@link Request} (the builder's plan version from its update tag), and the server answers with the
 * plan in {@link Part}s of at most {@link #PER_PART} blocks. A part also carries the offset the plan was placed with (the
 * client moves the ghosts ahead of the server while the player places the blueprint). The server answers a player at most
 * every {@link #GAP} ticks and only within {@link #REACH} blocks of the builder.
 *
 * <p>0.1.11.187 (a blueprint has no block limit any more):
 * <ul>
 *   <li>A block is {@link #STRIDE} numbers: its position relative to the plan's lowest corner, x, y and z, and its block
 *       state id. It was one number for the position, 10 bits an axis, so a plan over 1,024 blocks across showed nothing.</li>
 *   <li>The parts go out a few a tick ({@link #LOCAL_PARTS} to the player of a single player world, {@link #REMOTE_PARTS} to
 *       the others) instead of all at once: a plan of millions of blocks would have stopped the player's connection for
 *       as long as it took. The first part goes at once; the client shows each part as it arrives.</li>
 * </ul>
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID)
public final class BuilderPlanPackets {
    private BuilderPlanPackets() {}

    public static final int PER_PART = 8192, STRIDE = 4, GAP = 20;
    /** Parts a tick to the player of a single player world (no network between) and to every other player. */
    public static final int LOCAL_PARTS = 16, REMOTE_PARTS = 1;
    public static final double REACH = 512;   // 0.1.11.186: was 192 (a plan may be very large now)

    /** Client side: a received part (set by the client setup; the server never calls it). */
    public static volatile java.util.function.Consumer<Part> CLIENT_HOOK = p -> { };

    private static final Map<UUID, Long> LAST = new HashMap<>();
    /** 0.1.11.187: the plans still being sent, by player and builder. */
    private static final Map<UUID, Map<BlockPos, Sending>> SENDING = new HashMap<>();

    public record Request(BlockPos builder, int version) implements CustomPacketPayload {
        public static final Type<Request> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "builder_plan_request"));
        public static final StreamCodec<FriendlyByteBuf, Request> CODEC = StreamCodec.of(
            (buf, p) -> { buf.writeBlockPos(p.builder); buf.writeVarInt(p.version); },
            buf -> new Request(buf.readBlockPos(), buf.readVarInt()));

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

        static void handle(Request p, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (!(context.player() instanceof ServerPlayer player)) return;
                var level = player.serverLevel();
                long now = level.getGameTime();
                Long last = LAST.get(player.getUUID());
                if (last != null && now - last < GAP && now >= last) return;
                if (!level.isLoaded(p.builder) || player.distanceToSqr(p.builder.getCenter()) > REACH * REACH) return;
                if (!(level.getBlockEntity(p.builder) instanceof BlueprintBuilderBlockEntity builder)) return;
                LAST.put(player.getUUID(), now);
                send(player, builder);
            });
        }
    }

    /**
     * Part {@code index} of {@code count} of a builder's plan {@code version}: {@code origin} is the plan's lowest corner,
     * {@code data} the blocks, {@link #STRIDE} numbers each (x, y, z from the corner, block state id). {@code count} 0: no
     * plan to show.
     */
    public record Part(BlockPos builder, int version, int index, int count, BlockPos origin, int[] offset, int[] data) implements CustomPacketPayload {
        public static final Type<Part> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "builder_plan_part"));
        public static final StreamCodec<FriendlyByteBuf, Part> CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBlockPos(p.builder); buf.writeVarInt(p.version); buf.writeVarInt(p.index); buf.writeVarInt(p.count);
                buf.writeBlockPos(p.origin); buf.writeVarIntArray(p.offset); buf.writeVarIntArray(p.data);
            },
            buf -> new Part(buf.readBlockPos(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readBlockPos(), buf.readVarIntArray(3),
                buf.readVarIntArray(STRIDE * PER_PART)));

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

        /** The number of blocks in this part. */
        public int blocks() { return data.length / STRIDE; }

        /** The world position of block {@code k} of this part. */
        public BlockPos pos(int k) { return origin.offset(data[STRIDE * k], data[STRIDE * k + 1], data[STRIDE * k + 2]); }

        /** The block state id of block {@code k} of this part. */
        public int stateId(int k) { return data[STRIDE * k + 3]; }

        static void handle(Part p, IPayloadContext context) {
            context.enqueueWork(() -> CLIENT_HOOK.accept(p));
        }
    }

    /** A plan being sent to a player, a few parts a tick (0.1.11.187). */
    private static final class Sending {
        final ResourceKey<Level> dimension;
        final BlockPos builder, origin;
        final int version, count;
        final int[] offset;
        final List<Blueprint.Entry> plan;
        int next = 1;   // part 0 went with the answer

        Sending(ResourceKey<Level> dimension, BlockPos builder, BlockPos origin, int version, int count, int[] offset, List<Blueprint.Entry> plan) {
            this.dimension = dimension; this.builder = builder; this.origin = origin; this.version = version; this.count = count;
            this.offset = offset; this.plan = plan;
        }

        /** Whether the builder still has this plan and the player is still near it. */
        boolean valid(ServerPlayer player) {
            var level = player.serverLevel();
            return level.dimension() == dimension && level.isLoaded(builder) && player.distanceToSqr(builder.getCenter()) <= REACH * REACH
                && level.getBlockEntity(builder) instanceof BlueprintBuilderBlockEntity b && b.planVersion() == version && b.plan() == plan;
        }
    }

    /** Answers {@code player}'s request: the first part now, the rest by {@link #onServerTick}. */
    static void send(ServerPlayer player, BlueprintBuilderBlockEntity builder) {
        BlockPos at = builder.getBlockPos().immutable();
        List<Blueprint.Entry> plan = builder.plan();
        int[] offset = builder.planOffset().clone();
        Map<BlockPos, Sending> mine = SENDING.get(player.getUUID());
        if (mine != null) mine.remove(at);   // a new request starts the plan again (the client has not got it)
        if (plan == null || plan.isEmpty()) {
            PacketDistributor.sendToPlayer(player, new Part(at, builder.planVersion(), 0, 0, at, offset, new int[0]));
            return;
        }
        BlockPos origin = origin(builder.bounds(), plan);
        int count = count(plan);
        PacketDistributor.sendToPlayer(player, part(at, builder.planVersion(), 0, count, origin, offset, plan));
        if (count > 1) SENDING.computeIfAbsent(player.getUUID(), k -> new HashMap<>())
            .put(at, new Sending(player.serverLevel().dimension(), at, origin, builder.planVersion(), count, offset, plan));
    }

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        if (SENDING.isEmpty()) return;
        var server = event.getServer();
        for (Iterator<Map.Entry<UUID, Map<BlockPos, Sending>>> it = SENDING.entrySet().iterator(); it.hasNext(); ) {
            var en = it.next();
            ServerPlayer player = server.getPlayerList().getPlayer(en.getKey());
            if (player == null) { it.remove(); continue; }
            int budget = server.isSingleplayerOwner(player.getGameProfile()) ? LOCAL_PARTS : REMOTE_PARTS;
            for (Iterator<Sending> jt = en.getValue().values().iterator(); jt.hasNext() && budget > 0; ) {
                Sending s = jt.next();
                if (!s.valid(player)) { jt.remove(); continue; }
                for (; budget > 0 && s.next < s.count; budget--)
                    PacketDistributor.sendToPlayer(player, part(s.builder, s.version, s.next++, s.count, s.origin, s.offset, s.plan));
                if (s.next >= s.count) jt.remove();
            }
            if (en.getValue().isEmpty()) it.remove();
        }
    }

    @SubscribeEvent
    static void onServerStopped(ServerStoppedEvent event) {
        SENDING.clear();
        LAST.clear();
    }

    /** The number of parts of {@code plan}. */
    static int count(List<Blueprint.Entry> plan) { return (plan.size() + PER_PART - 1) / PER_PART; }

    /** The plan's lowest corner: from the builder's box, or (no box) from the blocks. */
    static BlockPos origin(@org.jetbrains.annotations.Nullable AABB bounds, List<Blueprint.Entry> plan) {
        if (bounds != null) return BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ);
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        for (Blueprint.Entry e : plan) {
            minX = Math.min(minX, e.pos().getX()); minY = Math.min(minY, e.pos().getY()); minZ = Math.min(minZ, e.pos().getZ());
        }
        return new BlockPos(minX, minY, minZ);
    }

    /** Part {@code index} of {@code plan}. */
    static Part part(BlockPos at, int version, int index, int count, BlockPos origin, int[] offset, List<Blueprint.Entry> plan) {
        int from = index * PER_PART, to = Math.min(plan.size(), from + PER_PART);
        int[] data = new int[STRIDE * Math.max(0, to - from)];
        for (int i = from, k = 0; i < to; i++, k += STRIDE) {
            Blueprint.Entry e = plan.get(i);
            data[k] = e.pos().getX() - origin.getX();
            data[k + 1] = e.pos().getY() - origin.getY();
            data[k + 2] = e.pos().getZ() - origin.getZ();
            data[k + 3] = Block.getId(e.state());
        }
        return new Part(at, version, index, count, origin, offset, data);
    }

    /** All parts of a builder's plan at once (one empty part when there is nothing to show); the game sends them by ticks. */
    public static List<Part> parts(BlueprintBuilderBlockEntity builder) {
        BlockPos at = builder.getBlockPos();
        List<Blueprint.Entry> plan = builder.plan();
        int[] offset = builder.planOffset().clone();
        if (plan == null || plan.isEmpty()) return List.of(new Part(at, builder.planVersion(), 0, 0, at, offset, new int[0]));
        return parts(at, builder.planVersion(), offset, origin(builder.bounds(), plan), plan);
    }

    /** All parts of {@code plan} (any size; tests build very wide plans without a builder). */
    public static List<Part> parts(BlockPos at, int version, int[] offset, BlockPos origin, List<Blueprint.Entry> plan) {
        int count = count(plan);
        List<Part> out = new ArrayList<>(count);
        for (int k = 0; k < count; k++) out.add(part(at, version, k, count, origin, offset, plan));
        return out;
    }

    public static void register(PayloadRegistrar registrar) {
        registrar.playToServer(Request.TYPE, Request.CODEC, Request::handle);
        registrar.playToClient(Part.TYPE, Part.CODEC, Part::handle);
    }
}
