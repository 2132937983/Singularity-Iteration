package example.sicore;

import com.miophas.singularity_iteration.core.runtime.energy.IndependentSiEnergy;
import com.miophas.singularity_iteration.core.runtime.energy.engine.ConductorRegistry;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests of 0.1.11.0 (grid phase 1, islands): the catch-up after a pause gives back the paused tick (C-1), an edit of one
 * island keeps the routing generation of the grid (P-3), the endpoints that an edit pauses are the ones of the full scan
 * of 0.1.10.44 (P-9), and the engine's topology reads the engine's conductor index (P-8).
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round1150GameTests {
    private Round1150GameTests() {}

    private static BlockState cable() {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "wiring/cable/block_tin_cable_1")).defaultBlockState();
    }

    private static ConductorRegistry.Position point(BlockPos at) { return new ConductorRegistry.Position(at.getX(), at.getY(), at.getZ()); }

    // ------------------------------------------------------------------ C-1: the catch-up gives back the paused tick

    /**
     * Four edits next to a source, a cable and a sink. Each edit pauses the circuit for one END and the next START settles
     * it. Until 0.1.10.44 that START used the budgets of its own tick (the source's 32 EU/t, the sink's 32 EU/t), which the
     * END of that tick then lacked: every pause lost one tick of transfer (the source kept the EU). The engine reads the
     * source at START and settles at END, so one tick of generation is in the pipeline (the source holds 32 EU when the
     * test reads). Now the reads of ticks 31 to 89 carry exactly the generation of ticks 29 to 87: a paused tick reads 0
     * and the next one 64.
     */
    @GameTest(template = "reactor", batch = "round1150_catchup", timeoutTicks = 140)
    public static void catchUpGivesBackThePausedTick(GameTestHelper h) { catchUp(h); }

    /** The same in the fair distribution (its own per-tick input budget, I-3). */
    @GameTest(template = "reactor", batch = "round1150_catchup_fair", timeoutTicks = 140)
    public static void catchUpGivesBackThePausedTickInFairMode(GameTestHelper h) { catchUp(h); }

    // the addon test server runs the legacy walk (build.gradle); this batch switches to fair as Round1120GameTests does
    @BeforeBatch(batch = "round1150_catchup_fair")
    public static void fairOn(ServerLevel level) { System.setProperty("scex.independent.distribution", "fair"); }

    @AfterBatch(batch = "round1150_catchup_fair")
    public static void fairOff(ServerLevel level) { System.setProperty("scex.independent.distribution", "legacy"); }

    private static void catchUp(GameTestHelper h) {
        var cable = cable();
        BlockPos at = new BlockPos(1, 2, 1), wire = at.east(), sinkAt = at.east(2), edit = wire.above();
        h.setBlock(at, CoreExampleMod.SOURCE.get()); h.setBlock(wire, cable); h.setBlock(sinkAt, CoreExampleMod.SINK.get());
        InheritedSource source = h.getBlockEntity(at); InheritedSink sink = h.getBlockEntity(sinkAt);
        int[] tick = {0}, pauses = {0}; long[] total = {0}; var trace = new StringBuilder(); var early = new StringBuilder();
        h.onEachTick(() -> {
            int now = ++tick[0];
            long got = sink.stored();
            if (now > 30 && now <= 89) { total[0] += got; if (got == 0) pauses[0]++; }
            if (now > 36 && now <= 76) trace.append(got).append(' ');
            if (now <= 30) early.append(now).append(':').append(source.stored()).append('/').append(got).append(' ');
            if (now == 30) h.assertTrue(source.stored() == 32 && got == 32, "not steady before the edits: the source holds "
                + source.stored() + " EU, the sink got " + got + " (expected 32 and 32; tick:source/sink " + early + ")");
            if (now <= 95) sink.getEnergyStorageInternal().setStored(0);
            if (now <= 88) source.generate(32);
            if (now == 40 || now == 60) h.setBlock(edit, cable);
            if (now == 50 || now == 70) h.setBlock(edit, Blocks.AIR);
            if (now == 89) {
                h.assertTrue(pauses[0] >= 1, "no edit paused the circuit, so nothing tested the catch-up: " + trace);
                h.assertTrue(total[0] == 59 * 32, "the pauses lost " + (59 * 32 - total[0]) + " EU (reads of ticks 37-76: " + trace + ")");
                for (var p : List.of(at, wire, sinkAt, edit)) h.setBlock(p, Blocks.AIR);
            }
            if (now == 95) h.succeed();
        });
    }

    // ------------------------------------------------------------------ P-3: an edit of one island keeps the generation

    /**
     * Two circuits; island B is edited four times. The routing generation (the identity of the routed endpoints) stays:
     * until 0.1.10.44 the pause, the catch-up and the next full settlement each started a new one (3 per edit) and every
     * plan of the grid was built again. Circuit A gets its 32 EU every tick and circuit B loses nothing.
     */
    @GameTest(template = "reactor_loop", batch = "round1150_generation", timeoutTicks = 140)
    public static void editOfAnotherIslandKeepsTheRoutingGeneration(GameTestHelper h) {
        var cable = cable();
        BlockPos a = new BlockPos(1, 2, 1), b = new BlockPos(1, 2, 9), edit = b.east().above();
        for (var p : List.of(a, b)) {
            h.setBlock(p, CoreExampleMod.SOURCE.get()); h.setBlock(p.east(), cable); h.setBlock(p.east(2), CoreExampleMod.SINK.get());
        }
        InheritedSource sourceA = h.getBlockEntity(a), sourceB = h.getBlockEntity(b);
        InheritedSink sinkA = h.getBlockEntity(a.east(2)), sinkB = h.getBlockEntity(b.east(2));
        var engine = IndependentSiEnergy.current(h.getLevel().getServer());
        int[] tick = {0}; long[] generation = {0}, totalB = {0};
        h.onEachTick(() -> {
            int now = ++tick[0];
            if (now > 30 && now <= 80) {
                h.assertTrue(sinkA.stored() == 32, "an edit of island B paused or doubled circuit A at tick " + now + ": " + sinkA.stored() + " EU");
                totalB[0] += sinkB.stored();
            }
            if (now <= 85) { sinkA.getEnergyStorageInternal().setStored(0); sinkB.getEnergyStorageInternal().setStored(0); }
            if (now <= 79) { sourceA.generate(32); sourceB.generate(32); }
            if (now == 30) generation[0] = engine.routingMetrics(h.getLevel()).topologyGenerations();
            if (now == 40 || now == 60) h.setBlock(edit, cable);
            if (now == 50 || now == 70) h.setBlock(edit, Blocks.AIR);
            if (now == 80) {
                long started = engine.routingMetrics(h.getLevel()).topologyGenerations() - generation[0];
                h.assertTrue(started == 0, "four edits of island B started " + started + " routing generations (0.1.10.44: 3 per edit)");
                h.assertTrue(totalB[0] == 50 * 32, "circuit B lost " + (50 * 32 - totalB[0]) + " EU to its pauses");
                for (var p : List.of(a, b)) for (int i = 0; i < 3; i++) h.setBlock(p.east(i), Blocks.AIR);
                h.setBlock(edit, Blocks.AIR);
            }
            if (now == 90) h.succeed();
        });
    }

    // ------------------------------------------------------------------ P-9: the paused endpoints

    /**
     * The endpoints that an edit pauses ({@code affectedEndpoints}) are the ones of the full scan of 0.1.10.44 (every
     * endpoint's emitters against the islands of the change), for a change at each of the 676 positions of the lower template
     * and 200 pairs: an island of 11 cables (more conductors than endpoints: the full scan), islands of 1 cable (the
     * neighbour scan of 0.1.11.0), an endpoint between two islands, endpoints without a cable.
     */
    @GameTest(template = "reactor_loop", batch = "round1150_affected", timeoutTicks = 60)
    public static void pausedEndpointsMatchTheFullScan(GameTestHelper h) {
        var cable = cable();
        var placed = new ArrayList<BlockPos>();
        for (int x = 1; x <= 11; x++) placed.add(new BlockPos(x, 2, 1));
        placed.add(new BlockPos(3, 2, 6)); placed.add(new BlockPos(3, 2, 8));
        for (var p : placed) h.setBlock(p, cable);
        var sources = List.of(new BlockPos(1, 2, 2), new BlockPos(2, 2, 6), new BlockPos(10, 2, 10));
        var sinks = List.of(new BlockPos(11, 2, 2), new BlockPos(6, 3, 1), new BlockPos(4, 2, 6), new BlockPos(8, 2, 8), new BlockPos(3, 2, 7));
        for (var p : sources) h.setBlock(p, CoreExampleMod.SOURCE.get());
        for (var p : sinks) h.setBlock(p, CoreExampleMod.SINK.get());
        int[] tick = {0};
        h.onEachTick(() -> {
            if (++tick[0] != 20) return;
            var engine = IndependentSiEnergy.current(h.getLevel().getServer());
            Object grid = ((Map<?, ?>) ArmoryServiceGameTests.field(engine, "worlds")).get(h.getLevel());
            h.assertTrue(grid != null, "no grid for the test level");
            var changes = new ArrayList<Set<BlockPos>>();
            for (int x = 0; x < 13; x++) for (int y = 1; y <= 4; y++) for (int z = 0; z < 13; z++) changes.add(Set.of(h.absolutePos(new BlockPos(x, y, z))));
            var random = new java.util.Random(1150);
            for (int i = 0; i < 200; i++)
                changes.add(Set.of(h.absolutePos(new BlockPos(random.nextInt(13), 1 + random.nextInt(4), random.nextInt(13))),
                    h.absolutePos(new BlockPos(random.nextInt(13), 1 + random.nextInt(4), random.nextInt(13)))));
            int differences = 0, withEndpoints = 0; String first = null;
            for (var change : changes) {
                @SuppressWarnings("unchecked")
                var now = (Set<BlockPos>) ArmoryServiceGameTests.call(IndependentSiEnergy.class, "affectedEndpoints", grid, new HashSet<>(change));
                var before = fullScan(grid, change);
                if (!now.equals(before)) { differences++; if (first == null) first = change + ": " + now + " / " + before; }
                if (before.size() > 7 * change.size()) withEndpoints++;
            }
            h.assertTrue(differences == 0, differences + " of " + changes.size() + " changes pause other endpoints than the full scan, first " + first);
            h.assertTrue(withEndpoints >= 100, "only " + withEndpoints + " changes reached an endpoint through an island");
            for (var p : placed) h.setBlock(p, Blocks.AIR);
            for (var p : sources) h.setBlock(p, Blocks.AIR);
            for (var p : sinks) h.setBlock(p, Blocks.AIR);
            h.succeed();
        });
    }

    // ------------------------------------------------------------------ P-8: one conductor index

    /**
     * The engine's topology keeps no registry of its own (until 0.1.10.44 a second one that nothing read, whose set of
     * edited positions was never cleared): its snapshot is the engine's index, and it counts the conductors for its
     * capacity check.
     */
    @GameTest(template = "reactor", batch = "round1150_index", timeoutTicks = 40)
    public static void topologyReadsTheEngineIndex(GameTestHelper h) {
        var cable = cable();
        var placed = List.of(new BlockPos(1, 2, 1), new BlockPos(2, 2, 1), new BlockPos(3, 2, 1));
        for (var p : placed) h.setBlock(p, cable);
        h.runAfterDelay(10, () -> {
            var engine = IndependentSiEnergy.current(h.getLevel().getServer());
            var topology = (com.miophas.singularity_iteration.core.platform.neoforge.energy.PlatformTopology) ArmoryServiceGameTests.field(engine, "topology");
            Object grid = ((Map<?, ?>) ArmoryServiceGameTests.field(engine, "worlds")).get(h.getLevel());
            var index = (ConductorRegistry) ArmoryServiceGameTests.field(grid, "conductors");
            h.assertTrue(topology.ready(h.getLevel()), "the topology is not ready");
            var snapshot = topology.snapshot(h.getLevel());
            h.assertTrue(snapshot == index.snapshot(), "the topology's snapshot is not the engine's index");
            h.assertTrue(topology.isCurrent(h.getLevel(), snapshot), "the engine's lease is not current for the topology");
            for (var p : placed) h.assertTrue(snapshot.contains(point(h.absolutePos(p))), "the index lacks the cable at " + p);
            h.assertTrue(!((Map<?, ?>) ArmoryServiceGameTests.field(topology, "worlds")).containsKey(h.getLevel()), "the topology made a registry of its own");
            Object members = ((Map<?, ?>) ArmoryServiceGameTests.field(topology, "members")).get(h.getLevel());
            h.assertTrue(members != null && (int) ArmoryServiceGameTests.field(members, "size") >= placed.size(), "the topology does not count the conductors");
            for (var p : placed) h.setBlock(p, Blocks.AIR);
        });
        h.runAfterDelay(20, h::succeed);
    }

    /** The affected endpoints of 0.1.10.44 (IndependentSiEnergy.affectedEndpoints before the island index). */
    @SuppressWarnings("unchecked")
    private static Set<BlockPos> fullScan(Object grid, Set<BlockPos> changes) {
        var result = new HashSet<BlockPos>(changes);
        var graph = (ConductorRegistry.Snapshot) ArmoryServiceGameTests.call(ArmoryServiceGameTests.field(grid, "conductors"), "snapshot");
        var components = new HashSet<Integer>();
        for (var at : changes) {
            if (graph.contains(point(at))) components.add(graph.componentOf(point(at)));
            for (var side : Direction.values()) {
                var next = at.relative(side); result.add(next);
                if (graph.contains(point(next))) components.add(graph.componentOf(point(next)));
            }
        }
        for (var entry : ((Map<BlockPos, BlockEntity>) ArmoryServiceGameTests.field(grid, "machines")).entrySet()) {
            for (var emitter : (List<BlockPos>) ArmoryServiceGameTests.call(IndependentSiEnergy.class, "emissionPositions", entry.getValue()))
                for (var side : Direction.values()) {
                    var contact = point(emitter.relative(side));
                    if (graph.contains(contact) && components.contains(graph.componentOf(contact))) result.add(entry.getKey());
                }
        }
        return result;
    }
}
