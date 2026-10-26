// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit;

import com.miophas.singularity_iteration.common.fcs.TargetPriority;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.pathfinder.Path;

/**
 * 0.1.10.23 (FCS P2, data link): the server tells a Behavior Predictor Mk.II wearer where the nearby creatures are
 * walking: the next nodes of their navigation path, which the client cannot know. The predictor then follows those
 * nodes round corners ({@code fcs.PathFollower}) instead of a straight line (in the offline test the mean error of the
 * 1 s path of a creature that walks a path with corners fell from 4.04 to 1.36 blocks).
 *
 * <p>Every {@link #INTERVAL} ticks: the mobs within {@link #RANGE} blocks that walk a path, the {@link #MAX_MOBS} with
 * the highest {@code TargetPriority} score, at most {@link #MAX_NODES} nodes each, sent only when the list changed
 * (and again every {@link #REFRESH} ticks, the client forgets it after 1.5 s). One empty list goes out when the unit
 * stops. Packet: {@link SuitPackets.PathNodes}, at most about 240 bytes.
 */
public final class PathLink {
    private PathLink() {}

    /** Ticks between two looks (offset from the threat list). */
    public static final int INTERVAL = 5;
    /** The predictor range of the client (blocks). */
    public static final double RANGE = 24;
    public static final int MAX_MOBS = 8, MAX_NODES = 6, REFRESH = 20;

    /** Per wearer: the hash of the last list and the game time it was sent. */
    private static final Map<UUID, long[]> SENT = new HashMap<>();

    static void tick(ServerPlayer player) {
        boolean on = SuitModules.isActive(player, SuitModuleType.BEHAVIOR_PREDICTOR) && SuitModules.tier(player, SuitModuleType.BEHAVIOR_PREDICTOR) >= 2;
        if (!on) {
            if (SENT.remove(player.getUUID()) != null) SuitPackets.send(player, new SuitPackets.PathNodes(new int[0], new int[0][]));
            return;
        }
        record Entry(Mob mob, double score, int[] nodes) { }
        List<Entry> entries = new ArrayList<>();
        for (Mob mob : player.serverLevel().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(RANGE),
                m -> m.isAlive() && !m.isSpectator() && m.distanceToSqr(player) <= RANGE * RANGE)) {
            Path path = mob.getNavigation().getPath();
            if (path == null || path.isDone()) continue;
            int from = path.getNextNodeIndex(), to = Math.min(path.getNodeCount(), from + MAX_NODES);
            if (to <= from) continue;
            int[] nodes = new int[3 * (to - from)];
            for (int i = from; i < to; i++) {
                BlockPos p = path.getNodePos(i);
                nodes[3 * (i - from)] = p.getX(); nodes[3 * (i - from) + 1] = p.getY(); nodes[3 * (i - from) + 2] = p.getZ();
            }
            int hostility = mob.getTarget() == player ? 4 : TargetRating.hostility(mob, 0);
            double score = TargetPriority.score(TargetRating.rating(mob, player.position(), mob.getDeltaMovement(), hostility >= 4 ? 2 : 0, -1));
            entries.add(new Entry(mob, score, nodes));
        }
        entries.sort((a, b) -> Double.compare(b.score(), a.score()));
        int n = Math.min(MAX_MOBS, entries.size());
        int[] ids = new int[n];
        int[][] nodes = new int[n][];
        long hash = 17;
        for (int i = 0; i < n; i++) {
            ids[i] = entries.get(i).mob().getId();
            nodes[i] = entries.get(i).nodes();
            hash = 31 * hash + ids[i];
            hash = 31 * hash + java.util.Arrays.hashCode(nodes[i]);
        }
        long now = player.serverLevel().getGameTime();
        long[] last = SENT.get(player.getUUID());
        boolean changed = last == null || last[0] != hash;
        boolean refresh = n > 0 && last != null && now - last[1] >= REFRESH;
        if (!changed && !refresh) return;
        SENT.put(player.getUUID(), new long[] {hash, now});
        SuitPackets.send(player, new SuitPackets.PathNodes(ids, nodes));
    }

    static void forget(UUID player) {
        SENT.remove(player);
    }
}
