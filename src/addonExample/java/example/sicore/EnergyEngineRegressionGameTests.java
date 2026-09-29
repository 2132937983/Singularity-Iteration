package example.sicore;

import com.miophas.singularity_iteration.core.platform.neoforge.energy.IndependentTransformerBlockEntity;
import com.miophas.singularity_iteration.core.platform.neoforge.energy.PlatformTopology;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import com.miophas.singularity_iteration.core.runtime.energy.IndependentSiEnergy;
import com.miophas.singularity_iteration.core.runtime.energy.engine.ConductorRegistry;
import com.miophas.singularity_iteration.core.runtime.energy.engine.DomainDistributor;
import com.miophas.singularity_iteration.core.runtime.energy.engine.EnergyAmount;
import com.miophas.singularity_iteration.core.runtime.energy.engine.FractionalDistributor;
import com.miophas.singularity_iteration.core.runtime.energy.engine.RouteCosts;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class EnergyEngineRegressionGameTests {
    private static RouteCosts route(long... loss) {
        return new RouteCosts() {
            public boolean reaches(int id) { return loss[id] >= 0; }
            public long lossMilliTo(int id) { return loss[id]; }
        };
    }

    @GameTest(template = "empty", batch = "packet_arithmetic")
    public static void partialPacketsPayLossAndStopAtActualCapacity(GameTestHelper h) {
        var source = List.of(new DomainDistributor.Source(4096, 512, false, 4));
        var domain = List.of(new DomainDistributor.Domain(new int[]{0}, List.of(route(1000)), new int[][]{{0}}));
        var round = FractionalDistributor.allocateTraced(source, List.of(EnergyAmount.of(4096)), domain,
            new int[]{0}, List.of(EnergyAmount.fromDouble(512.5)), List.of(), new Random(1));
        h.assertTrue(round.credit(0).equals(EnergyAmount.fromDouble(512.5))
                && round.debit(0).equals(EnergyAmount.fromDouble(514.5)) && round.dissipated().equals(EnergyAmount.of(2))
                && round.deliveries().size() == 2, "Fractional demand exceeded capacity or paid the wrong per-packet loss");
        var split = DomainDistributor.allocateTraced(List.of(new DomainDistributor.Source(64, 32, false, 2)),
            List.of(new DomainDistributor.Domain(new int[]{0}, List.of(route(0, 1000)), new int[][]{{0, 1}})),
            new int[]{0, 1}, new long[]{1, 1000}, new Random(1));
        h.assertTrue(split.debit(0) == 64 && split.credit(0) == 1 && split.credit(1) == 61
                && split.dissipated() == 2 && split.deliveries().size() == 3, "Packet remainder was lost or enlarged between sinks");
        var insufficient = DomainDistributor.allocateTraced(List.of(new DomainDistributor.Source(511, 512, false, 4)),
            domain, new int[]{0}, new long[]{10_000}, new Random(1));
        h.assertTrue(insufficient.debit(0) == 0, "Transformer emitted without a complete initial packet");
        h.succeed();
    }

    @GameTest(template = "empty", batch = "packet_arithmetic")
    public static void integralAndFractionalPlannersAgreeAcrossRandomDomains(GameTestHelper h) {
        Random fixtures = new Random(20260929);
        for (int trial = 0; trial < 400; trial++) {
            int ns = 1 + fixtures.nextInt(5), nr = 1 + fixtures.nextInt(6), nd = 1 + fixtures.nextInt(3);
            var sources = new ArrayList<DomainDistributor.Source>();
            var amounts = new ArrayList<EnergyAmount>();
            int[] ids = new int[ns], contacts = new int[nr];
            long[] room = new long[nr];
            var exactRoom = new ArrayList<EnergyAmount>();
            for (int i = 0; i < ns; i++) {
                long packet = 32L << fixtures.nextInt(4);
                long reserve = fixtures.nextInt(10) * packet + fixtures.nextInt(32);
                sources.add(new DomainDistributor.Source(reserve, packet, false, 1 + fixtures.nextInt(4)));
                // Full-packet sources cannot spend this fraction, but it forces the exact planner.
                amounts.add(new EnergyAmount(reserve, EnergyAmount.UNITS / 4));
                ids[i] = i;
            }
            for (int i = 0; i < nr; i++) {
                contacts[i] = i; room[i] = fixtures.nextInt(1500); exactRoom.add(EnergyAmount.of(room[i]));
            }
            var domains = new ArrayList<DomainDistributor.Domain>();
            for (int d = 0; d < nd; d++) {
                var routes = new ArrayList<RouteCosts>(); int[][] priorities = new int[ns][nr];
                for (int s = 0; s < ns; s++) {
                    long[] costs = new long[nr];
                    for (int r = 0; r < nr; r++) { costs[r] = fixtures.nextInt(5) == 0 ? -1 : fixtures.nextInt(8) * 1000; priorities[s][r] = r; }
                    for (int r = nr - 1; r > 0; r--) {
                        int at = fixtures.nextInt(r + 1), old = priorities[s][r]; priorities[s][r] = priorities[s][at]; priorities[s][at] = old;
                    }
                    routes.add(route(costs));
                }
                domains.add(new DomainDistributor.Domain(ids, routes, priorities));
            }
            long seed = fixtures.nextLong();
            var integer = DomainDistributor.allocateTraced(sources, domains, contacts, room, new Random(seed));
            var exact = FractionalDistributor.allocateTraced(sources, amounts, domains, contacts, exactRoom, List.of(), new Random(seed));
            long debits = 0, credits = 0;
            for (int s = 0; s < ns; s++) {
                h.assertTrue(exact.debit(s).equals(EnergyAmount.of(integer.debit(s))), "Planner debit mismatch trial " + trial);
                h.assertTrue(integer.debit(s) <= Math.min(sources.get(s).reserve(), sources.get(s).packet() * sources.get(s).packetCount()), "Source budget overspent");
                debits += integer.debit(s);
            }
            for (int r = 0; r < nr; r++) {
                h.assertTrue(exact.credit(r).equals(EnergyAmount.of(integer.credit(r))) && integer.credit(r) <= room[r], "Planner capacity/credit mismatch trial " + trial);
                credits += integer.credit(r);
            }
            h.assertTrue(debits == credits + integer.dissipated() && exact.dissipated().equals(EnergyAmount.of(integer.dissipated())), "Planner lost energy");
            for (var delivery : integer.deliveries())
                h.assertTrue(delivery.sourceDebit() <= sources.get(delivery.source()).packet(), "Multi-packet output became one overvoltage packet");
        }
        h.succeed();
    }

    @GameTest(template = "reactor_loop", batch = "step_down_throughput", timeoutTicks = 100)
    public static void bothStepDownTiersFeedFourPacketsIntoOneStorage(GameTestHelper h) {
        var centers = List.of(new BlockPos(3, 3, 3), new BlockPos(3, 3, 9));
        var sources = new ArrayList<AbstractEnergyBlockEntity>();
        var sinks = new ArrayList<AbstractEnergyBlockEntity>();
        for (int i = 0; i < centers.size(); i++) {
            var at = centers.get(i);
            h.setBlock(at.west(), facing(i == 0 ? "wiring/block_mfsu" : "wiring/block_eesu", Direction.EAST));
            h.setBlock(at, facing(i == 0 ? "wiring/transformer_hv_ev" : "wiring/transformer_ev_sc", Direction.WEST));
            h.setBlock(at.east(), facing(i == 0 ? "wiring/block_mfe" : "wiring/block_mfsu", Direction.EAST));
            AbstractEnergyBlockEntity source = h.getBlockEntity(at.west());
            source.getEnergyStorageInternal().setStored(1_000_000); sources.add(source); sinks.add(h.getBlockEntity(at.east()));
            IndependentTransformerBlockEntity transformer = h.getBlockEntity(at);
            transformer.setSavedMode(1);
        }
        long[] previous = new long[2]; int[] tick = {0};
        h.onEachTick(() -> {
            int now = ++tick[0];
            if (now >= 20 && now <= 60) for (int i = 0; i < 2; i++) {
                long current = sinks.get(i).getEnergyStorageInternal().getAmount();
                if (now > 20) h.assertTrue(current - previous[i] == (i == 0 ? 2048 : 8192), "Step-down receiver did not get four low packets per tick");
                previous[i] = current;
            }
            if (now == 60) for (var at : centers) { h.setBlock(at.west(), Blocks.AIR); h.setBlock(at, Blocks.AIR); h.setBlock(at.east(), Blocks.AIR); }
            if (now == 80) h.succeed();
        });
    }

    @GameTest(template = "reactor_loop", batch = "wired_boost_throughput", timeoutTicks = 110)
    public static void wiredBoostReusesTopologyWhileBalancesChange(GameTestHelper h) {
        var centers = List.of(new BlockPos(3, 3, 3), new BlockPos(3, 3, 9));
        var sinks = new ArrayList<AbstractEnergyBlockEntity>();
        Direction[] sides = {Direction.WEST, Direction.NORTH, Direction.SOUTH, Direction.DOWN};
        for (int i = 0; i < 2; i++) {
            var bus = centers.get(i);
            h.setBlock(bus, block("wiring/cable/block_glass_cable"));
            for (var side : sides) {
                h.setBlock(bus.relative(side), facing(i == 0 ? "wiring/block_mfe" : "wiring/block_mfsu", side.getOpposite()));
                AbstractEnergyBlockEntity source = h.getBlockEntity(bus.relative(side));
                source.getEnergyStorageInternal().setStored(1_000_000);
            }
            h.setBlock(bus.east(), facing(i == 0 ? "wiring/transformer_hv_ev" : "wiring/transformer_ev_sc", Direction.EAST));
            IndependentTransformerBlockEntity transformer = h.getBlockEntity(bus.east()); transformer.setSavedMode(0);
            h.setBlock(bus.east(2), facing("wiring/block_eesu", Direction.EAST)); sinks.add(h.getBlockEntity(bus.east(2)));
        }
        var engine = IndependentSiEnergy.current(h.getLevel().getServer());
        long[] previous = new long[2], generation = new long[1]; int[] tick = {0};
        h.onEachTick(() -> {
            int now = ++tick[0];
            if (now == 25) generation[0] = engine.routingMetrics(h.getLevel()).topologyGenerations();
            if (now >= 25 && now <= 65) for (int i = 0; i < 2; i++) {
                long current = sinks.get(i).getEnergyStorageInternal().getAmount();
                if (now > 25) h.assertTrue(current - previous[i] == (i == 0 ? 2048 : 8192), "Wired four-source boost skipped a tick");
                previous[i] = current;
            }
            if (now == 65) {
                h.assertTrue(engine.routingMetrics(h.getLevel()).topologyGenerations() == generation[0], "Energy quotes invalidated the static routing generation");
                for (var bus : centers) {
                    for (var side : sides) h.setBlock(bus.relative(side), Blocks.AIR);
                    h.setBlock(bus, Blocks.AIR); h.setBlock(bus.east(), Blocks.AIR); h.setBlock(bus.east(2), Blocks.AIR);
                }
            }
            if (now == 85) h.succeed();
        });
    }

    @GameTest(template = "empty", batch = "real_overvoltage", timeoutTicks = 50)
    public static void fullHighVoltagePacketsStillDestroyLowTierReceivers(GameTestHelper h) {
        BlockPos sourcePos = new BlockPos(1, 1, 1), sinkPos = sourcePos.east();
        h.setBlock(sourcePos, facing("wiring/block_mfsu", Direction.EAST));
        h.setBlock(sinkPos, facing("wiring/block_mfe", Direction.EAST));
        AbstractEnergyBlockEntity source = h.getBlockEntity(sourcePos);
        source.getEnergyStorageInternal().setStored(100_000);
        h.runAfterDelay(20, () -> {
            h.assertTrue(h.getLevel().getBlockEntity(h.absolutePos(sinkPos)) == null, "Real 2048 EU overload no longer destroys an MFE");
            h.assertTrue(source.getEnergyStorageInternal().getAmount() == 100_000 - 2048, "Destructive packet was not debited exactly once");
            h.setBlock(sourcePos, Blocks.AIR); h.setBlock(sinkPos, Blocks.AIR);
        });
        h.runAfterDelay(32, h::succeed);
    }

    @GameTest(template = "empty", batch = "topology_queue_recovery", timeoutTicks = 100)
    public static void positionFloodCompactsIntoChunksAndForgetsRemovedWires(GameTestHelper h) {
        BlockPos removed = new BlockPos(1, 1, 1), kept = removed.east();
        var cable = ResourceLocation.parse("mio_icif:wiring/cable/block_glass_cable");
        var topology = new PlatformTopology(h.getLevel().getServer(), Map.of(cable, 25L), 10_000, 32, 32, 128);
        h.setBlock(removed, BuiltInRegistries.BLOCK.get(cable)); h.setBlock(kept, BuiltInRegistries.BLOCK.get(cable));
        topology.changed(h.getLevel(), h.absolutePos(removed)); topology.changed(h.getLevel(), h.absolutePos(kept));
        h.runAfterDelay(8, () -> {
            h.assertTrue(topology.ready(h.getLevel()) && topology.snapshot(h.getLevel()).contains(point(h.absolutePos(removed))), "Initial topology did not publish");
            h.setBlock(removed, Blocks.AIR);
            BlockPos base = h.absolutePos(kept); int x = base.getX() & ~15, z = base.getZ() & ~15;
            for (int i = 0; i < 400; i++) topology.changed(h.getLevel(), new BlockPos(x + i % 16, base.getY() + i / 256, z + i / 16 % 16));
            h.assertTrue(topology.metrics().queued() <= 32 && topology.failure().isEmpty() && topology.queueCompactions() > 0,
                "Position flood exceeded the queue bound or stopped the topology");
        });
        h.runAfterDelay(35, () -> {
            try {
                h.assertTrue(topology.ready(h.getLevel()), "Compacted topology never became ready again: " + topology.metrics());
                var snapshot = topology.snapshot(h.getLevel());
                h.assertTrue(snapshot.contains(point(h.absolutePos(kept))) && !snapshot.contains(point(h.absolutePos(removed))),
                    "Chunk rebuild missed the surviving wire or retained a removed wire");
            } finally {
                topology.close(); h.setBlock(removed, Blocks.AIR); h.setBlock(kept, Blocks.AIR);
            }
        });
        h.runAfterDelay(48, h::succeed);
    }

    @GameTest(template = "empty", batch = "cable_packet_effects", timeoutTicks = 60)
    public static void cableEffectsUseActualPacketAndPreserveRealFuseProtection(GameTestHelper h) {
        BlockPos sourcePos = new BlockPos(0, 1, 1), wirePos = sourcePos.east(), sinkPos = sourcePos.east(2);
        var wire = block("wiring/cable/block_tin_cable_1");
        h.setBlock(sourcePos, facing("wiring/block_mfe", Direction.EAST));
        h.setBlock(wirePos, wire);
        h.setBlock(sinkPos, facing("wiring/block_bat_box", Direction.EAST));
        AbstractEnergyBlockEntity source = h.getBlockEntity(sourcePos), sink = h.getBlockEntity(sinkPos);
        source.getEnergyStorageInternal().setStored(100_000);
        sink.getEnergyStorageInternal().setStored(sink.getEnergyStorageInternal().getCapacity() - 1);
        h.runAfterDelay(16, () -> {
            h.assertTrue(h.getBlockState(wirePos).is(wire.getBlock()) && h.getBlockEntity(sinkPos) == sink,
                "One-EU actual packet melted a tin cable or exploded a BatBox");
            h.assertTrue(source.getEnergyStorageInternal().getAmount() == 99_999
                && sink.getEnergyStorageInternal().getAmount() == sink.getEnergyStorageInternal().getCapacity(), "One-EU packet/loss mismatch");
            sink.getEnergyStorageInternal().setStored(0);
        });
        h.runAfterDelay(28, () -> {
            h.assertTrue(h.getBlockState(wirePos).isAir(), "Full 512-EU packet no longer melts a tin cable");
            h.assertTrue(h.getLevel().getBlockEntity(h.absolutePos(sinkPos)) == sink, "Cable fuse failed to suppress receiver explosion");
            h.assertTrue(source.getEnergyStorageInternal().getAmount() == 99_999 - 512, "Fuse packet was duplicated or lost");
            h.setBlock(sourcePos, Blocks.AIR); h.setBlock(wirePos, Blocks.AIR); h.setBlock(sinkPos, Blocks.AIR);
        });
        h.runAfterDelay(42, h::succeed);
    }

    private static ConductorRegistry.Position point(BlockPos p) { return new ConductorRegistry.Position(p.getX(), p.getY(), p.getZ()); }
    private static BlockState block(String id) { return BuiltInRegistries.BLOCK.get(ResourceLocation.parse("mio_icif:" + id)).defaultBlockState(); }
    private static BlockState facing(String id, Direction side) {
        var state = block(id);
        return state.setValue((DirectionProperty) state.getBlock().getStateDefinition().getProperty("facing"), side);
    }
}
