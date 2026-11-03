package example.sicore;

import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import com.miophas.singularity_iteration.core.runtime.energy.CustomEUEnergyStorage;
import com.miophas.singularity_iteration.core.runtime.energy.FeLedger;
import com.miophas.singularity_iteration.core.runtime.energy.IndependentSiEnergy;
import com.miophas.singularity_iteration.core.runtime.energy.engine.ConductorGraph;
import com.miophas.singularity_iteration.core.runtime.energy.engine.ConductorRegistry;
import com.miophas.singularity_iteration.core.runtime.energy.engine.EnergyAmount;
import com.miophas.singularity_iteration.core.runtime.energy.engine.IndependentEnergyMode;
import com.miophas.singularity_iteration.core.runtime.energy.grid.FECompatTile;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests of 0.1.10.44 (grid phase 0): foreign discovery without capability caches on probes and with block changes probed
 * at once (P-5), bare-cable deliveries settled from the path profile when nobody is near (P-1), conductor facts dropped with
 * their block (P-2), one input budget for native EU and FE (I-3), and the conductor indexes after the hash changes (P-7).
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round1144GameTests {
    private Round1144GameTests() {}

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }

    private static BlockState cable(String name) { return BuiltInRegistries.BLOCK.get(id("wiring/cable/" + name)).defaultBlockState(); }

    private static BlockState facing(Block block, Direction direction) {
        BlockState state = block.defaultBlockState();
        return state.getBlock().getStateDefinition().getProperty("facing") instanceof DirectionProperty facing
            && facing.getPossibleValues().contains(direction) ? state.setValue(facing, direction) : state;
    }

    private static int cacheCount(FECompatTile adapter) {
        try {
            var field = FECompatTile.class.getDeclaredField("caches");
            field.setAccessible(true);
            return ((Map<?, ?>) field.get(adapter)).size();
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("FECompatTile.caches", e);
        }
    }

    // ------------------------------------------------------------------ P-5: foreign discovery

    /** A probe looks its capabilities up live (no cache, so no capability listener); a kept adapter caches them. */
    @GameTest(batch = "round1144_probe", template = "empty", timeoutTicks = 20)
    public static void probeAdaptersKeepNoCapabilityCaches(GameTestHelper h) {
        var battery = new BlockPos(2, 1, 2);
        h.setBlock(battery, CoreExampleMod.FOREIGN_BATTERY.get());
        var probe = new FECompatTile(h.getLevel(), h.absolutePos(battery), Direction.EAST);
        h.assertTrue(probe.isValid() && probe.canReceive(), "the probe does not see the foreign battery");
        h.assertTrue(cacheCount(probe) == 0, "a probe made " + cacheCount(probe) + " capability caches");
        var kept = new FECompatTile(h.getLevel(), h.absolutePos(battery), Direction.EAST).retain();
        h.assertTrue(kept.isValid() && cacheCount(kept) > 0, "a kept adapter made no capability cache");
        h.assertTrue(kept.receiveEnergy(400, false) == 400, "the kept adapter did not deliver through its caches");
        h.assertTrue(((ForeignBattery) h.getBlockEntity(battery)).storage.getEnergyStored() == 400, "the battery did not get the FE");
        h.setBlock(battery, Blocks.AIR);
        h.succeed();
    }

    /**
     * A foreign battery placed next to a grid of 101 anchors after the full discovery pass gets FE within 3 ticks: the
     * block change is probed at once (the slow sweep checks 8 anchors per tick and would need up to 13 ticks).
     */
    @GameTest(batch = "round1144_discovery", template = "reactor_loop", timeoutTicks = 80)
    public static void blockChangesNextToTheGridAreProbedAtOnce(GameTestHelper h) {
        var source = new BlockPos(1, 4, 5);
        var cables = new ArrayList<BlockPos>();
        for (int x = 2; x <= 11; x++) for (int z = 1; z <= 10; z++) cables.add(new BlockPos(x, 4, z));
        h.setBlock(source, facing(BuiltInRegistries.BLOCK.get(id("wiring/block_mfsu")), Direction.EAST));
        for (var at : cables) h.setBlock(at, cable("block_superconducting_cable"));
        Round33GameTests.call(Round33GameTests.call(h.getBlockEntity(source), "getEnergyStorageInternal"), "setStored", 1_000_000L);
        var battery = new BlockPos(9, 5, 8);
        h.runAfterDelay(40, () -> h.setBlock(battery, CoreExampleMod.FOREIGN_BATTERY.get()));
        h.runAfterDelay(43, () -> {
            int stored = ((ForeignBattery) h.getBlockEntity(battery)).storage.getEnergyStored();
            h.assertTrue(stored > 0, "the battery placed next to the grid got no FE within 3 ticks");
            h.setBlock(battery, Blocks.AIR); h.setBlock(source, Blocks.AIR);
            for (var at : cables) h.setBlock(at, Blocks.AIR);
        });
        h.runAfterDelay(50, h::succeed);
    }

    // ------------------------------------------------------------------ P-1: bare cable deliveries

    /**
     * Bare tin cable above its insulation: with no living entity near the path the deliveries are settled from the path
     * profile (no path visit); a cow in the same area but away from the cables is not hurt.
     */
    @GameTest(batch = "round1144_bare", template = "reactor_loop", timeoutTicks = 100)
    public static void bareCableDeliveriesUseThePathProfile(GameTestHelper h) {
        h.assertTrue(IndependentEnergyMode.enetShockDamage(), "shock damage is off in the test configuration");
        var sourcePos = new BlockPos(1, 3, 2); var sinkPos = new BlockPos(8, 3, 2);
        var wire = new ArrayList<BlockPos>();
        for (int x = 2; x <= 7; x++) wire.add(new BlockPos(x, 3, 2));
        h.setBlock(sourcePos, CoreExampleMod.SOURCE.get()); h.setBlock(sinkPos, CoreExampleMod.SINK.get());
        for (var at : wire) h.setBlock(at, cable("block_tin_cable"));
        InheritedSource source = h.getBlockEntity(sourcePos); InheritedSink sink = h.getBlockEntity(sinkPos);
        var cow = h.spawn(EntityType.COW, new BlockPos(10, 3, 10)); cow.setNoAi(true); cow.setNoGravity(true);
        var engine = IndependentSiEnergy.current(h.getLevel().getServer());
        h.assertTrue(engine != null, "no energy engine");
        long[] before = new long[2];
        int[] tick = {0};
        h.onEachTick(() -> {
            int now = ++tick[0];
            if (now <= 80) source.generate(32);
            if (now == 30) { var counters = engine.optimizationCounters(); before[0] = counters[0]; before[1] = counters[1]; }
            // six bare tin cables lose 1.2 EU of a 32 EU packet when line loss is on (the test configuration)
            if (now > 30 && now <= 70) h.assertTrue(sink.stored() >= 30 && sink.stored() <= 32, "the sink did not get one packet a tick: " + sink.stored());
            if (now == 70) {
                var counters = engine.optimizationCounters();
                h.assertTrue(counters[0] - before[0] >= 30, "bare-cable deliveries did not use the path profile: " + (counters[0] - before[0]));
                h.assertTrue(counters[1] == before[1], "bare-cable deliveries visited their path " + (counters[1] - before[1]) + " times");
                h.assertTrue(cow.getHealth() == cow.getMaxHealth(), "a cow away from the cable was hurt");
            }
            if (now <= 80) sink.getEnergyStorageInternal().setStored(0);
            if (now == 80) {
                cow.discard();
                for (var p : List.of(sourcePos, sinkPos)) h.setBlock(p, Blocks.AIR);
                for (var p : wire) h.setBlock(p, Blocks.AIR);
            }
            if (now == 90) h.succeed();
        });
    }

    /**
     * P-2: the conductor facts of a profiled path are dropped with the block. An insulated cable next to a cow is replaced by
     * a bare one while the path delivers every tick: the next deliveries read the bare cable and shock the cow.
     */
    @GameTest(batch = "round1144_facts", template = "reactor_loop", timeoutTicks = 100)
    public static void aReplacedCableIsReadAgain(GameTestHelper h) {
        h.assertTrue(IndependentEnergyMode.enetShockDamage(), "shock damage is off in the test configuration");
        var sourcePos = new BlockPos(1, 3, 2); var first = sourcePos.east(); var second = first.east(); var sinkPos = second.east();
        h.setBlock(sourcePos, CoreExampleMod.SOURCE.get()); h.setBlock(sinkPos, CoreExampleMod.SINK.get());
        h.setBlock(first, cable("block_tin_cable_1")); h.setBlock(second, cable("block_tin_cable_1"));
        InheritedSource source = h.getBlockEntity(sourcePos); InheritedSink sink = h.getBlockEntity(sinkPos);
        var cow = h.spawn(EntityType.COW, first); cow.setNoAi(true); cow.setNoGravity(true);
        int[] tick = {0};
        h.onEachTick(() -> {
            int now = ++tick[0];
            if (now <= 70) source.generate(32);
            if (now > 10 && now <= 30) {
                h.assertTrue(sink.stored() == 32, "the sink did not get the packet: " + sink.stored());
                h.assertTrue(cow.getHealth() == cow.getMaxHealth(), "an insulated cable shocked the cow");
            }
            if (now == 30) h.setBlock(first, cable("block_tin_cable"));
            if (now <= 70) sink.getEnergyStorageInternal().setStored(0);
            if (now == 50) h.assertTrue(cow.getHealth() < cow.getMaxHealth(), "the bare cable that replaced an insulated one did not shock the cow");
            if (now == 72) {
                cow.discard();
                for (var p : List.of(sourcePos, first, second, sinkPos)) h.setBlock(p, Blocks.AIR);
            }
            if (now == 80) h.succeed();
        });
    }

    // ------------------------------------------------------------------ I-3: one input budget

    /** The ledger: native credits of the tick shrink the FE room (rounded up to whole FE), FE input counts as EU. */
    @GameTest(batch = "round1144_budget", template = "empty", timeoutTicks = 20)
    public static void ledgerSharesOneInputBudget(GameTestHelper h) {
        long[] clock = {100};
        var storage = new CustomEUEnergyStorage(10_000, 100, 100, CableTier.LV);
        var ledger = new FeLedger(storage, () -> clock[0], () -> true);
        h.assertTrue(ledger.receive(Integer.MAX_VALUE, true) == 400, "the FE room of 100 EU/t is not 400 FE");
        ledger.countNativeInput(EnergyAmount.of(60));
        h.assertTrue(ledger.receive(Integer.MAX_VALUE, true) == 160, "60 native EU did not leave 160 FE");
        ledger.countNativeInput(new EnergyAmount(0, EnergyAmount.UNITS / 8));
        h.assertTrue(ledger.receive(Integer.MAX_VALUE, true) == 159, "a native fraction was not rounded up to a whole FE");
        h.assertTrue(ledger.receive(100, false) == 100, "the ledger refused FE within the room");
        h.assertTrue(ledger.inputThisTick().equals(EnergyAmount.of(25)), "100 FE did not count as 25 EU: " + ledger.inputThisTick());
        h.assertTrue(ledger.receiveWholeEu(1_000, true) == 14, "whole-EU input does not share the room: " + ledger.receiveWholeEu(1_000, true));
        clock[0]++;
        h.assertTrue(ledger.receive(Integer.MAX_VALUE, true) == 400 && ledger.inputThisTick().isZero(), "the budget did not renew on the next tick");
        h.succeed();
    }

    /** In the grid: a sink that took its rated 32 EU natively in this tick accepts no FE in the same tick. */
    @GameTest(batch = "round1144_budget_grid", template = "reactor", timeoutTicks = 80)
    public static void nativeInputUsesTheFeBudgetOfTheTick(GameTestHelper h) {
        var sourcePos = new BlockPos(1, 2, 2); var first = sourcePos.east(); var sinkPos = first.east();
        h.setBlock(sourcePos, CoreExampleMod.SOURCE.get()); h.setBlock(sinkPos, CoreExampleMod.SINK.get());
        h.setBlock(first, cable("block_tin_cable_1"));
        InheritedSource source = h.getBlockEntity(sourcePos); InheritedSink sink = h.getBlockEntity(sinkPos);
        int[] tick = {0};
        h.onEachTick(() -> {
            int now = ++tick[0];
            if (now > 64) { if (now == 70) h.succeed(); return; }
            if (now <= 60) source.generate(32);
            var fe = h.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, h.absolutePos(sinkPos), Direction.UP);
            h.assertTrue(fe != null, "the sink has no FE port");
            if (now > 30 && now <= 50) {
                h.assertTrue(sink.stored() == 32, "the sink did not get its rated input natively: " + sink.stored());
                h.assertTrue(fe.receiveEnergy(1_000, true) == 0, "the sink takes FE on top of its rated native input");
            }
            if (now <= 60) sink.getEnergyStorageInternal().setStored(0);
            // the source still holds the packets of the first ticks (before the grid was built); empty it, then the sink idles
            if (now == 60) source.getEnergyStorageInternal().setStored(0);
            if (now == 63) h.assertTrue(fe.receiveEnergy(1_000, true) == 128, "an idle sink does not take its rated FE input");
            if (now == 64) { for (var p : List.of(sourcePos, first, sinkPos)) h.setBlock(p, Blocks.AIR); }
        });
    }

    // ------------------------------------------------------------------ P-7: conductor indexes

    /** Duplicate links are still refused after the sorted check; chunk activity still hides and restores conductors. */
    @GameTest(batch = "round1144_index", template = "empty", timeoutTicks = 20)
    public static void conductorIndexesKeepTheirRules(GameTestHelper h) {
        try {
            new ConductorGraph(new long[]{0, 0, 0}, new int[][]{{0, 1}, {1, 2}, {1, 0}});
            throw new AssertionError("a duplicate link was accepted");
        } catch (IllegalArgumentException expected) {
            h.assertTrue("Self or duplicate link".equals(expected.getMessage()), "wrong rejection: " + expected.getMessage());
        }
        new ConductorGraph(new long[]{0, 0, 0}, new int[][]{{0, 1}, {1, 2}});
        try (var registry = new ConductorRegistry(1_000, 16)) {
            for (int x = -2; x <= 18; x++) registry.put(new ConductorRegistry.Position(x, 64, -1), 100);
            var inside = new ConductorRegistry.Position(17, 64, -1);
            h.assertTrue(registry.snapshot().contains(inside), "the conductor is missing");
            h.assertTrue(registry.setChunkActive(1, -1, false), "the chunk did not become inactive");
            h.assertTrue(!registry.snapshot().contains(inside) && registry.snapshot().contains(new ConductorRegistry.Position(15, 64, -1)),
                "an inactive chunk did not hide only its own conductors");
            h.assertTrue(registry.setChunkActive(1, -1, true) && registry.snapshot().contains(inside), "the chunk did not come back");
            h.assertTrue(registry.unloadChunk(-1, -1) == 2 && registry.size() == 19, "the unload did not remove the chunk's two conductors");
        }
        h.succeed();
    }
}
