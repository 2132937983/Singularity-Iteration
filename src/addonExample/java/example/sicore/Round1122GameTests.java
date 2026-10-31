package example.sicore;

import com.miophas.singularity_iteration.core.api.energy.grid.LoadPriority;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests of 0.1.10.22 (grid G3 / G4 / G5 / G7): a breaker cable trips on an overload, recloses three times and then
 * locks out until a reset; a fuse blows once and is restored; the voltage element of a breaker spares the storage block
 * behind it (an unprotected line explodes it); load priority gives a critical load the energy first; the energy
 * management terminal lists the breaker, the priorities and the alarm; the default dispatch mode is the merit order.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round1122GameTests {
    private Round1122GameTests() {}

    private static final String CORE = "com.miophas.singularity_iteration" + ".core.";

    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    private static Class<?> core(String name) {
        try {
            return Class.forName(CORE + name);
        } catch (ClassNotFoundException e) {
            throw new AssertionError(name, e);
        }
    }

    private static BlockState block(String id) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.parse("mio_icif:" + id)).defaultBlockState();
    }

    private static BlockState facing(String id, Direction side) {
        var state = block(id);
        return state.setValue((DirectionProperty) state.getBlock().getStateDefinition().getProperty("facing"), side);
    }

    /** The relay of the protective cable at {@code pos} (reflection: the class is engine internal). */
    private static Object relay(GameTestHelper h, BlockPos pos) {
        BlockEntity cable = h.getBlockEntity(pos);
        h.assertTrue(cable != null && (boolean) call(cable, "protective"), "no protective cable at " + pos);
        return call(cable, "relay");
    }

    private static String state(Object relay) { return call(relay, "state").toString(); }

    private static void configure(GameTestHelper h, BlockPos pos, long flow, long voltage, int delay, boolean reclose) {
        call(h.getBlockEntity(pos), "configure", flow, voltage, delay, reclose);
    }

    // ------------------------------------------------------------------ G5 breaker

    /** 32 EU/t through a breaker rated 8 EU/t: it trips, recloses after 3 s three times, then locks out until a reset. */
    @GameTest(template = "reactor", batch = "round1122_protection", timeoutTicks = 420)
    public static void breakerTripsReclosesAndLocksOut(GameTestHelper h) {
        BlockPos p = new BlockPos(1, 2, 2), wire = p.east(), load = wire.east();
        h.setBlock(p, CoreExampleMod.SOURCE.get());
        h.setBlock(wire, block("wiring/block_eu_breaker_cable"));
        h.setBlock(load, CoreExampleMod.SINK.get());
        InheritedSource source = h.getBlockEntity(p);
        InheritedSink sink = h.getBlockEntity(load);
        configure(h, wire, 8, 0, 2, true);
        int[] tick = {0}, trippedAt = {-1}, received = {0}, quiet = {0};
        boolean[] reset = {false};
        h.onEachTick(() -> {
            int now = ++tick[0];
            if (now > 380) { if (now == 400) h.succeed(); return; }   // the parts are gone (removed at 380): nothing to read
            Object relay = relay(h, wire);
            String s = state(relay);
            if (now <= 380) source.generate(32);
            long got = sink.stored();
            sink.getEnergyStorageInternal().setStored(0);
            if (got > 0) received[0]++;
            if (trippedAt[0] < 0 && "TRIPPED".equals(s)) trippedAt[0] = now;
            // open: nothing passes (one tick of grace for the publication at the next tick start)
            if (("TRIPPED".equals(s) || "LOCKED_OUT".equals(s)) && got == 0) quiet[0]++;
            if (now == 40) h.assertTrue(trippedAt[0] > 0 && trippedAt[0] < 40, "the overloaded breaker did not trip");
            if (now == 320) {
                h.assertTrue("LOCKED_OUT".equals(s), "the breaker is not locked out after four trips: " + s);
                h.assertTrue((int) call(relay, "recentTrips") == 4, "trip count " + call(relay, "recentTrips"));
                h.assertTrue(quiet[0] > 150, "energy passed while the breaker was open (" + quiet[0] + " quiet ticks)");
                var alarms = (List<?>) call(core("runtime.energy.GridAlarms"), "recent", h.getLevel());
                long at = h.absolutePos(wire).asLong();
                h.assertTrue(alarms.stream().anyMatch(a -> "BREAKER_LOCKOUT".equals(call(a, "type").toString()) && (long) call(a, "pos") == at),
                    "no lockout alarm");
                configure(h, wire, 1024, 0, 20, true);
                h.assertTrue((boolean) call(h.getBlockEntity(wire), "resetProtection"), "reset refused");
                reset[0] = true;
            }
            if (now == 360) h.assertTrue("CLOSED".equals(s) && got > 0, "the reset breaker does not conduct: " + s + " / " + got);
            if (now == 380) {
                for (var at : List.of(p, wire, load)) h.setBlock(at, Blocks.AIR);
            }
        });
    }

    /** A fuse rated 8 EU/t blows once and stays open (redstone does not matter); a reset restores it. */
    @GameTest(template = "reactor", batch = "round1122_protection", timeoutTicks = 140)
    public static void fuseBlowsOnceAndIsRestored(GameTestHelper h) {
        BlockPos p = new BlockPos(1, 2, 2), wire = p.east(), load = wire.east();
        h.setBlock(p, CoreExampleMod.SOURCE.get());
        h.setBlock(wire, block("wiring/block_eu_fuse_cable"));
        h.setBlock(load, CoreExampleMod.SINK.get());
        InheritedSource source = h.getBlockEntity(p);
        InheritedSink sink = h.getBlockEntity(load);
        configure(h, wire, 8, 0, 1, false);
        int[] tick = {0};
        h.onEachTick(() -> {
            int now = ++tick[0];
            if (now > 110) { if (now == 125) h.succeed(); return; }   // the parts are gone (removed at 110): nothing to read
            String s = state(relay(h, wire));
            if (now <= 110) source.generate(32);
            long got = sink.stored();
            sink.getEnergyStorageInternal().setStored(0);
            if (now > 20 && now <= 70) {
                h.assertTrue("BLOWN".equals(s), "the fuse did not blow: " + s);
                if (now > 25) h.assertTrue(got == 0, "energy passed a blown fuse");
            }
            if (now == 50) {
                BlockState fuse = h.getBlockState(wire);
                var active = fuse.getBlock().getStateDefinition().getProperty("active");
                h.assertTrue(active != null && Boolean.FALSE.equals(fuse.getValue(active)), "the blown fuse still shows active");
            }
            if (now == 70) {
                configure(h, wire, 1024, 0, 20, false);
                h.assertTrue((boolean) call(h.getBlockEntity(wire), "resetProtection"), "fuse reset refused");
            }
            if (now == 100) h.assertTrue("CLOSED".equals(s) && got > 0, "the restored fuse does not conduct");
            if (now == 110) for (var at : List.of(p, wire, load)) h.setBlock(at, Blocks.AIR);
        });
    }

    /**
     * An MFE (512 EU packets) feeds two BatBoxes (LV): through a breaker with a rated voltage of 128 EU the BatBox
     * survives and the breaker trips for overvoltage; through a breaker without a voltage element it explodes.
     */
    @GameTest(template = "empty", batch = "round1122_voltage", timeoutTicks = 80)
    public static void voltageElementSparesTheStorageBehindIt(GameTestHelper h) {
        BlockPos protectedSource = new BlockPos(0, 1, 1), protectedBreaker = protectedSource.east(), protectedSink = protectedSource.east(2);
        BlockPos bareSource = new BlockPos(0, 1, 4), bareBreaker = bareSource.east(), bareSink = bareSource.east(2);
        for (BlockPos[] line : new BlockPos[][] {{protectedSource, protectedBreaker, protectedSink}, {bareSource, bareBreaker, bareSink}}) {
            h.setBlock(line[0], facing("wiring/block_mfe", Direction.EAST));
            h.setBlock(line[1], block("wiring/block_eu_breaker_cable"));
            h.setBlock(line[2], facing("wiring/block_bat_box", Direction.EAST));
        }
        configure(h, protectedBreaker, 1_000_000, 128, 20, false);
        configure(h, bareBreaker, 1_000_000, 0, 20, false);
        for (BlockPos s : new BlockPos[] {protectedSource, bareSource}) {
            AbstractEnergyBlockEntity mfe = h.getBlockEntity(s);
            mfe.getEnergyStorageInternal().setStored(100_000);
        }
        BlockEntity spared = h.getBlockEntity(protectedSink);
        h.runAfterDelay(30, () -> {
            h.assertTrue(h.getLevel().getBlockEntity(h.absolutePos(protectedSink)) == spared, "the protected BatBox exploded");
            Object relay = relay(h, protectedBreaker);
            h.assertTrue("TRIPPED".equals(state(relay)) && "OVERVOLTAGE".equals(call(relay, "lastCause").toString()),
                "the voltage element did not trip: " + state(relay) + " / " + call(relay, "lastCause"));
            h.assertTrue(!h.getBlockState(bareSink).is(block("wiring/block_bat_box").getBlock()), "the unprotected BatBox did not explode");
        });
        h.runAfterDelay(45, () -> {
            for (BlockPos at : List.of(protectedSource, protectedBreaker, protectedSink, bareSource, bareBreaker, bareSink)) h.setBlock(at, Blocks.AIR);
        });
        h.runAfterDelay(60, h::succeed);
    }

    // ------------------------------------------------------------------ G4 load priority

    // load priority, the early end of a full window and the replay of a steady plan exist only in the fair distribution;
    // the addon test server runs the legacy walk (build.gradle), so this batch switches to fair as FairGridGameTests does
    @BeforeBatch(batch = "round1122_priority")
    public static void fairOnPriority(ServerLevel level) { System.setProperty("scex.independent.distribution", "fair"); }

    @AfterBatch(batch = "round1122_priority")
    public static void fairOffPriority(ServerLevel level) { System.setProperty("scex.independent.distribution", "legacy"); }

    /**
     * 32 EU/t for two loads of 32 EU/t: both normal share it; a critical load then takes it all and the deferrable one
     * gets nothing; swapped priorities swap the result.
     */
    @GameTest(template = "reactor", batch = "round1122_priority", timeoutTicks = 200)
    public static void criticalLoadIsServedFirst(GameTestHelper h) {
        BlockPos p = new BlockPos(1, 2, 2), wire = p.east(), a = wire.east(), b = wire.south();
        h.setBlock(p, CoreExampleMod.SOURCE.get());
        h.setBlock(wire, block("wiring/cable/block_tin_cable_1"));
        h.setBlock(a, CoreExampleMod.SINK.get());
        h.setBlock(b, CoreExampleMod.SINK.get());
        InheritedSource source = h.getBlockEntity(p);
        InheritedSink sinkA = h.getBlockEntity(a), sinkB = h.getBlockEntity(b);
        long[] sumA = new long[4], sumB = new long[4];
        int[] tick = {0};
        h.onEachTick(() -> {
            int now = ++tick[0];
            if (now <= 170) source.generate(32);
            long gotA = sinkA.stored(), gotB = sinkB.stored();
            sinkA.getEnergyStorageInternal().setStored(0);
            sinkB.getEnergyStorageInternal().setStored(0);
            int phase = now <= 60 ? 0 : now <= 110 ? 1 : now <= 160 ? 2 : 3;
            boolean measuring = now > 20 && now <= 60 || now > 70 && now <= 110 || now > 120 && now <= 160;
            if (measuring) { sumA[phase] += gotA; sumB[phase] += gotB; }
            if (now == 60) {
                h.assertTrue(Math.abs(sumA[0] - sumB[0]) <= 64 && sumA[0] > 0, "normal loads do not share: " + sumA[0] + " / " + sumB[0]);
                sinkA.setLoadPriority(LoadPriority.DEFERRABLE);
                sinkB.setLoadPriority(LoadPriority.CRITICAL);
            }
            if (now == 110) {
                h.assertTrue(sumA[1] == 0 && sumB[1] > 0, "the critical load did not come first: " + sumA[1] + " / " + sumB[1]);
                sinkA.setLoadPriority(LoadPriority.CRITICAL);
                sinkB.setLoadPriority(LoadPriority.DEFERRABLE);
            }
            if (now == 160) h.assertTrue(sumB[2] == 0 && sumA[2] > 0, "swapped priorities did not swap: " + sumA[2] + " / " + sumB[2]);
            if (now == 170) for (var at : List.of(p, wire, a, b)) h.setBlock(at, Blocks.AIR);
            if (now == 185) h.succeed();
        });
    }

    // ------------------------------------------------------------------ G7 terminal, G3 default

    /** The energy management terminal lists the breaker (closed), the load priority and the dispatch mode. */
    @GameTest(template = "reactor", batch = "round1122_terminal", timeoutTicks = 120)
    public static void terminalListsBreakerAndPriority(GameTestHelper h) {
        BlockPos p = new BlockPos(1, 2, 2), breaker = p.east(), wire = breaker.east(), load = wire.east(), terminal = wire.north();
        h.setBlock(p, CoreExampleMod.SOURCE.get());
        h.setBlock(breaker, block("wiring/block_eu_breaker_cable"));
        h.setBlock(wire, block("wiring/cable/block_tin_cable_1"));
        h.setBlock(load, CoreExampleMod.SINK.get());
        h.setBlock(terminal, block("wiring/block_energy_terminal"));
        InheritedSink sink = h.getBlockEntity(load);
        sink.setLoadPriority(LoadPriority.CRITICAL);
        h.runAfterDelay(70, () -> {
            Object snapshot = call(h.getBlockEntity(terminal), "snapshot");
            List<?> breakers = (List<?>) field(snapshot, "breakers");
            List<?> devices = (List<?>) field(snapshot, "devices");
            h.assertTrue(breakers.size() == 1 && (int) call(breakers.get(0), "state") == 0, "the terminal does not list the closed breaker: " + breakers);
            h.assertTrue(devices.stream().anyMatch(d -> "PROTECTION".equals(call(d, "category").toString())), "no protection device");
            h.assertTrue(devices.stream().anyMatch(d -> "CONSUMER".equals(call(d, "category").toString()) && (int) call(d, "priority") == 0),
                "the critical priority is not shown");
            h.assertTrue((int) field(snapshot, "dispatchMode") == 0, "the default dispatch mode is not the merit order");
            h.assertTrue("MERIT".equals(call(core("runtime.energy.engine.IndependentEnergyMode"), "dispatchMode").toString()), "engine dispatch mode");
        });
        h.runAfterDelay(85, () -> { for (var at : List.of(p, breaker, wire, load, terminal)) h.setBlock(at, Blocks.AIR); });
        h.runAfterDelay(100, h::succeed);
    }

    private static Object field(Object target, String field) {
        try {
            return target.getClass().getField(field).get(target);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(field, e);
        }
    }
}
