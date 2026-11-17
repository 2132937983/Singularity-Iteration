package example.sicore;

import com.miophas.singularity_iteration.core.platform.neoforge.energy.PlatformTopology;
import com.miophas.singularity_iteration.core.runtime.energy.IndependentSiEnergy;
import com.miophas.singularity_iteration.core.runtime.energy.engine.ConductorRegistry;
import com.miophas.singularity_iteration.core.runtime.energy.engine.DeferredEntries;
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

import java.util.Map;

/**
 * 0.1.11.32 (cable audit W3): the grid's bounds leave extra blocks out instead of failing the level, an addon callback
 * that throws after a commit isolates its chunk only, and error rebuilds of one level back off.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11532GameTests {
    private Round11532GameTests() {}

    private static BlockState facing(String id, Direction side) {
        var state = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", id)).defaultBlockState();
        return state.setValue((DirectionProperty) state.getBlock().getStateDefinition().getProperty("facing"), side);
    }

    /** The entry queue over its bounds: new entries are left out (counted), removals and updates still publish. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void entryQueueDropsOverItsBounds(GameTestHelper h) {
        try (var entries = new DeferredEntries<String, Integer>(2, 3)) {
            entries.observe(0, "a", 1); entries.observe(0, "b", 1); entries.observe(0, "c", 1);
            entries.observe(0, "d", 1);   // pending is full: left out
            var first = entries.advance(1);
            h.assertTrue(first.size() == 2 && entries.metrics().published() == 2, "published " + first);
            h.assertTrue(entries.metrics().dropped() == 2, "one over the pending bound, one over the published bound: " + entries.metrics());
            entries.observe(2, "a", null); entries.observe(2, "b", 2); entries.observe(2, "e", 1);
            var second = entries.advance(3);
            h.assertTrue(second.size() == 3 && entries.metrics().published() == 2, "a removal makes room for e: " + second);
        }
        try (var registry = new ConductorRegistry(2, 4)) {
            var a = new ConductorRegistry.Position(0, 0, 0); var b = new ConductorRegistry.Position(1, 0, 0); var c = new ConductorRegistry.Position(2, 0, 0);
            registry.put(a, 0, ConductorRegistry.ALL_FACES); registry.put(b, 0, ConductorRegistry.ALL_FACES);
            h.assertTrue(!registry.hasRoomFor(c) && registry.hasRoomFor(a), "the registry at its bound");
        }
        h.succeed();
    }

    /**
     * An MFE feeds an LV machine whose overload handler throws: the engine isolates that chunk (no rebuild of the
     * dimension) and the machine survives; with the handler fixed, the next overload destroys it as usual.
     */
    @GameTest(template = "empty", batch = "round11532_callback", timeoutTicks = 140)
    public static void throwingOverloadHandlerIsolatesItsChunk(GameTestHelper h) {
        BlockPos sourcePos = new BlockPos(1, 2, 1), sinkPos = sourcePos.east();
        h.setBlock(sourcePos, facing("wiring/block_mfe", Direction.EAST));
        var source = (com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity) h.getBlockEntity(sourcePos);
        h.setBlock(sinkPos, CoreExampleMod.SINK.get());
        InheritedSink sink = (InheritedSink) h.getBlockEntity(sinkPos);
        sink.failOverload = true;
        var engine = IndependentSiEnergy.current(h.getLevel().getServer());
        long rebuilds = engine.topologyRecoveryMetrics().rebuilds(), isolated = engine.topologyRecoveryMetrics().isolatedFailures();
        source.getEnergyStorageInternal().setStored(100_000);
        int[] tick = {0};
        h.onEachTick(() -> {
            int now = ++tick[0];
            if (now == 60) {
                var metrics = engine.topologyRecoveryMetrics();
                h.assertTrue(sink.overloads > 0, "the overload handler was never called");
                h.assertTrue(metrics.isolatedFailures() > isolated, "the failure was not isolated: " + metrics);
                h.assertTrue(metrics.rebuilds() == rebuilds, "the dimension was rebuilt: " + metrics);
                h.assertTrue(h.getLevel().getBlockEntity(h.absolutePos(sinkPos)) == sink, "the machine exploded although its handler threw");
                sink.failOverload = false;
                sink.getEnergyStorageInternal().setStored(0);   // it filled up while the handler threw (the energy is committed first)
            }
            if (now > 60 && h.getLevel().getBlockEntity(h.absolutePos(sinkPos)) != sink) {
                h.setBlock(sourcePos, Blocks.AIR);
                h.succeed();
            }
        });
    }

    /** A second error rebuild right after the first waits (back-off), and the level does not settle meanwhile. */
    @GameTest(template = "empty", batch = "round11532_backoff", timeoutTicks = 80)
    public static void errorRebuildsBackOff(GameTestHelper h) {
        var level = h.getLevel();
        var topology = new PlatformTopology(level.getServer(), Map.of(ResourceLocation.parse("mio_icif:wiring/cable/block_tin_cable"), 200L), 10_000, 16, 32, 128);
        int[] tick = {0};
        h.onEachTick(() -> {
            int now = ++tick[0];
            try {
                if (now == 2) {
                    topology.rebuild(level, "first");
                    topology.rebuild(level, "second");
                    h.assertTrue(topology.recoveryMetrics().rebuilds() == 1, "the second rebuild did not wait: " + topology.recoveryMetrics());
                    h.assertTrue(!topology.operational(level), "a level with a held rebuild still settles");
                }
                if (now == 40) {
                    h.assertTrue(topology.recoveryMetrics().rebuilds() == 2, "the held rebuild never ran: " + topology.recoveryMetrics());
                    topology.close();
                    h.succeed();
                }
            } catch (RuntimeException | AssertionError e) { topology.close(); throw e; }
        });
    }
}
