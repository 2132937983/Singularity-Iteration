package example.sicore;

import com.miophas.singularity_iteration.core.platform.neoforge.energy.IndependentTransformerBlockEntity;
import com.miophas.singularity_iteration.core.platform.neoforge.energy.PlatformTopology;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import com.miophas.singularity_iteration.core.runtime.energy.IndependentSiEnergy;
import com.miophas.singularity_iteration.core.runtime.energy.engine.ConductorRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
        // 0.1.11.31 (D3): the packet is what the source sends (capped by what it has), not the receiver's room
        source.getEnergyStorageInternal().setStored(1);
        h.runAfterDelay(16, () -> {
            h.assertTrue(h.getBlockState(wirePos).is(wire.getBlock()) && h.getBlockEntity(sinkPos) == sink,
                "One-EU actual packet melted a tin cable or exploded a BatBox");
            h.assertTrue(source.getEnergyStorageInternal().getAmount() == 0
                && sink.getEnergyStorageInternal().getAmount() == 1, "One-EU packet/loss mismatch");
            source.getEnergyStorageInternal().setStored(100_000);
            sink.getEnergyStorageInternal().setStored(sink.getEnergyStorageInternal().getCapacity() - 1);
        });
        h.runAfterDelay(28, () -> {
            h.assertTrue(h.getBlockState(wirePos).isAir(), "A 512-EU packet into a nearly full BatBox no longer melts a tin cable");
            h.assertTrue(h.getLevel().getBlockEntity(h.absolutePos(sinkPos)) == sink, "Cable fuse failed to suppress receiver explosion");
            h.assertTrue(source.getEnergyStorageInternal().getAmount() == 100_000 - 1
                && sink.getEnergyStorageInternal().getAmount() == sink.getEnergyStorageInternal().getCapacity(), "The melting delivery was duplicated or lost: source " + source.getEnergyStorageInternal().getAmount() + " sink " + sink.getEnergyStorageInternal().getAmount() + "/" + sink.getEnergyStorageInternal().getCapacity());
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
