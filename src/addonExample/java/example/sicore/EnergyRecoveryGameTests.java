package example.sicore;

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
public final class EnergyRecoveryGameTests {
    @GameTest(template = "reactor_loop", batch = "energy_local_edits", timeoutTicks = 150)
    public static void independentCircuitKeepsRunningDuringRepeatedEdits(GameTestHelper h) {
        var p = new BlockPos(1,2,1); var edit = new BlockPos(9,2,9);
        h.setBlock(p, CoreExampleMod.SOURCE.get()); h.setBlock(p.east(), CoreExampleMod.SINK.get());
        InheritedSource source = h.getBlockEntity(p); InheritedSink sink = h.getBlockEntity(p.east());
        int[] tick = {0};
        h.onEachTick(() -> {
            int now = ++tick[0];
            if (now > 25 && now <= 110) h.assertTrue(sink.stored() == 32, "Unrelated edit paused or duplicated healthy output at tick " + now);
            if (now <= 110) { sink.getEnergyStorageInternal().setStored(0); source.generate(32); }
            if (now >= 30 && now < 100 && now % 3 == 0) h.setBlock(edit, now % 2 == 0 ? glass() : Blocks.AIR.defaultBlockState());
            if (now == 110) for (var at : List.of(p,p.east(),edit)) h.setBlock(at, Blocks.AIR);
            if (now == 130) h.succeed();
        });
    }

    @GameTest(template = "reactor_loop", batch = "energy_fault_isolation", timeoutTicks = 170)
    public static void badAddonIsQuarantinedThenRecoversWithoutStoppingOtherChunks(GameTestHelper h) {
        var p = new BlockPos(1,2,1); var world = h.getLevel(); var badPos = h.absolutePos(p).offset(48,0,0);
        world.setChunkForced(badPos.getX() >> 4, badPos.getZ() >> 4, true);
        world.setBlockAndUpdate(badPos, CoreExampleMod.PACKET_SOURCE.get().defaultBlockState());
        world.setBlockAndUpdate(badPos.east(), CoreExampleMod.SINK.get().defaultBlockState());
        ComposedPacketSource bad = (ComposedPacketSource) world.getBlockEntity(badPos);
        InheritedSink badSink = (InheritedSink) world.getBlockEntity(badPos.east());
        h.setBlock(p, CoreExampleMod.SOURCE.get()); h.setBlock(p.east(), CoreExampleMod.SINK.get());
        InheritedSource source = h.getBlockEntity(p); InheritedSink sink = h.getBlockEntity(p.east());
        var engine = IndependentSiEnergy.current(world.getServer()); long initialFailures = engine.topologyRecoveryMetrics().isolatedFailures();
        int[] tick = {0};
        h.onEachTick(() -> {
            int now = ++tick[0];
            if (now > 25 && now <= 130) h.assertTrue(sink.stored() == 32, "Fault in another chunk paused healthy output at tick " + now);
            if (now > 105 && now <= 130) h.assertTrue(badSink.stored() == 256, "Repaired addon never rejoined the grid");
            if (now <= 130) {
                sink.getEnergyStorageInternal().setStored(0); source.generate(32);
                badSink.getEnergyStorageInternal().setStored(0); bad.energyComponent().generate(256, false);
            }
            if (now == 30) bad.failPolicy = true;
            if (now == 65) {
                h.assertTrue(engine.topologyRecoveryMetrics().isolatedFailures() > initialFailures, "Failure injection did not enter isolation");
                bad.failPolicy = false;
            }
            if (now == 130) {
                h.assertTrue(engine.metrics().failure().isEmpty(), "Recoverable addon error stopped the engine");
                for (var at : List.of(p,p.east())) h.setBlock(at, Blocks.AIR);
                world.setBlockAndUpdate(badPos, Blocks.AIR.defaultBlockState()); world.setBlockAndUpdate(badPos.east(), Blocks.AIR.defaultBlockState());
                world.setChunkForced(badPos.getX() >> 4, badPos.getZ() >> 4, false);
            }
            if (now == 155) h.succeed();
        });
    }

    @GameTest(template = "empty", batch = "energy_queue_recovery", timeoutTicks = 170)
    public static void distinctChunkOverflowRebuildsWithABoundedQueue(GameTestHelper h) {
        queueRecovery(h, false);
    }
    @GameTest(template = "empty", batch = "energy_queue_unload", timeoutTicks = 170)
    public static void rebuildDoesNotResurrectAChunkUnloadedDuringRecovery(GameTestHelper h) {
        queueRecovery(h, true);
    }
    private static void queueRecovery(GameTestHelper h, boolean unloadDuringRecovery) {
        var world = h.getLevel(); var positions = new ArrayList<BlockPos>();
        var base = h.absolutePos(new BlockPos(512,2,0));
        for (int i = 0; i < 9; i++) {
            var at = base.offset(i * 16,0,0); positions.add(at);
            world.setChunkForced(at.getX() >> 4, at.getZ() >> 4, true);
            world.setBlockAndUpdate(at, glass());
        }
        PlatformTopology[] watcher = {null};
        h.runAfterDelay(15, () -> {
            watcher[0] = new PlatformTopology(world.getServer(), Map.of(ResourceLocation.parse("mio_icif:wiring/cable/block_glass_cable"), 25L), 10000, 16, 3, 16);
            for (var at : positions) watcher[0].changed(world, at);
            h.assertTrue(watcher[0].recoveryMetrics().rebuilds() > 0, "Fixture did not exceed the distinct-chunk queue limit");
            // Feed the lifecycle notification to the isolated observer while
            // its immutable rebuild snapshot still contains this chunk.
            if (unloadDuringRecovery) watcher[0].onChunkUnload(new net.neoforged.neoforge.event.level.ChunkEvent.Unload(world.getChunkAt(positions.getFirst())));
        });
        h.onEachTick(() -> { if (watcher[0] != null) h.assertTrue(watcher[0].metrics().queued() <= 3 && watcher[0].failure().isEmpty(), "Recovery escaped the queue bound"); });
        h.runAfterDelay(110, () -> {
            try {
                h.assertTrue(watcher[0].ready(world), "Overflow recovery did not converge: " + watcher[0].recoveryMetrics());
                var graph = watcher[0].snapshot(world);
                for (var at : positions) h.assertTrue(graph.contains(new ConductorRegistry.Position(at.getX(),at.getY(),at.getZ()))
                    == (!unloadDuringRecovery || !at.equals(positions.getFirst())), "Recovery lost a chunk or resurrected an unloaded one");
            } finally {
                watcher[0].close(); watcher[0] = null;
                for (var at : positions) {
                    world.setBlockAndUpdate(at, Blocks.AIR.defaultBlockState());
                    world.setChunkForced(at.getX() >> 4, at.getZ() >> 4, false);
                }
            }
        });
        h.runAfterDelay(145, h::succeed);
    }

    @GameTest(template = "reactor_loop", batch = "energy_large_grid", timeoutTicks = 300)
    public static void fiveHundredSourcesResumeBoundedSearchAndKeepMakingProgress(GameTestHelper h) {
        stress(h, false);
    }

    @GameTest(template = "reactor_loop", batch = "energy_large_grid_multiple_loads", timeoutTicks = 300)
    public static void fiveHundredSourcesKeepFairnessWithMultipleLoads(GameTestHelper h) {
        stress(h, true);
    }

    private static void stress(GameTestHelper h, boolean multipleLoads) {
        var placed = new ArrayList<BlockPos>(); var sources = new ArrayList<InheritedSource>();
        for (int y = 2; y <= 10; y += 2) {
            for (int x = 1; x <= 12; x++) for (int z = 1; z <= 12; z++) {
                var at = new BlockPos(x,y,z); h.setBlock(at,glass()); placed.add(at);
            }
            if (y < 10) { var at = new BlockPos(1,y+1,1); h.setBlock(at,glass()); placed.add(at); }
            for (int x = 2; x <= 11; x++) for (int z = 2; z <= 11; z++) {
                var at = new BlockPos(x,y+1,z); h.setBlock(at,CoreExampleMod.SOURCE.get()); placed.add(at);
                InheritedSource source = h.getBlockEntity(at); source.generate(1024); sources.add(source);
            }
        }
        var load = new BlockPos(12,3,12); var state = block("wiring/block_bat_box");
        h.setBlock(load,state.setValue((DirectionProperty)state.getBlock().getStateDefinition().getProperty("facing"),Direction.EAST)); placed.add(load);
        AbstractEnergyBlockEntity sink = h.getBlockEntity(load);
        var sinks = new ArrayList<AbstractEnergyBlockEntity>(); sinks.add(sink);
        if (multipleLoads) {
            var second = new BlockPos(12,5,12); h.setBlock(second,h.getBlockState(load)); placed.add(second);
            sinks.add(h.getBlockEntity(second));
        }
        var engine = IndependentSiEnergy.current(h.getLevel().getServer()); var initial = engine.routingMetrics(h.getLevel());
        int[] tick = {0}; int[] served = new int[sources.size()]; long[] totalCredits = {0}, beforeLoss = {0};
        h.onEachTick(() -> {
            int now = ++tick[0];
            if (now >= 30 && now <= 220) {
                long debit = 0;
                for (int i = 0; i < sources.size(); i++) {
                    long spent = 1024 - sources.get(i).stored(); debit += spent;
                    if (spent > 0) served[i]++;
                }
                long credit = sinks.stream().mapToLong(s -> s.getEnergyStorageInternal().getAmount()).sum();
                if (now > 30) h.assertTrue(debit == credit + engine.metrics().dissipated() - beforeLoss[0], "Large network lost energy");
                totalCredits[0] += credit;
                beforeLoss[0] = engine.metrics().dissipated();
            }
            if (now <= 220) {
                for (var target : sinks) target.getEnergyStorageInternal().setStored(0);
                for (var source : sources) source.generate(1024);
            }
            if (multipleLoads && now >= 80 && now <= 120 && now % 8 == 0)
                h.setBlock(new BlockPos(0,2,0), now % 16 == 0 ? glass() : Blocks.AIR.defaultBlockState());
            if (now == 220) {
                for (int i = 0; i < served.length; i++) h.assertTrue(served[i] > 100, "Source " + i + " starved: " + served[i]);
                var routing = engine.routingMetrics(h.getLevel());
                h.assertTrue(routing.budgetYields() > initial.budgetYields() && routing.plansResumed() > initial.plansResumed(), "Fixture did not exercise cross-tick planning");
                System.out.println("ENERGY_STRESS sources=500 wires=724 loads=" + sinks.size() + " credits=" + totalCredits[0] + " routing=" + routing + " timings=" + engine.performanceMetrics(h.getLevel()));
                for (var at : placed) h.setBlock(at,Blocks.AIR);
                h.setBlock(new BlockPos(0,2,0),Blocks.AIR);
            }
            if (now == 255) h.succeed();
        });
    }
    private static BlockState glass() { return block("wiring/cable/block_glass_cable"); }
    private static BlockState block(String path) { return BuiltInRegistries.BLOCK.get(ResourceLocation.parse("mio_icif:" + path)).defaultBlockState(); }
}
