package example.sicore;

import java.lang.reflect.Proxy;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests of 0.1.10.20: futures market M1 (one market per server, daily counters per player, quotes with spread
 * and budget), grid G1 (an edit rebuilds only its own conductor component; the other grid keeps delivering) and G2
 * (a steady window is replayed), and the FCS parts (turn tracker, intercept) on the server side.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round1120GameTests {
    private Round1120GameTests() {}

    private static final String CORE = "com.miophas.singularity_iteration" + ".core.";

    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    private static Class<?> core(String name) {
        try {
            return Class.forName(CORE + name);
        } catch (ClassNotFoundException e) {
            throw new AssertionError(name, e);
        }
    }

    private static BlockState block(String path) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", path)).defaultBlockState();
    }

    private static Object engine(GameTestHelper h) {
        Object engine = call(core("runtime.energy.IndependentSiEnergy"), "current", h.getLevel().getServer());
        h.assertTrue(engine != null, "no energy engine");
        return engine;
    }

    // ------------------------------------------------------------------ M1 futures market

    /** One market for the server (the Nether reads the overworld market); the daily counters roll with the market day. */
    @GameTest(batch = "round1120_market", template = "empty", timeoutTicks = 20)
    public static void marketIsServerWideWithPlayerCounters(GameTestHelper h) {
        Class<?> data = Round33GameTests.common("future.FutureMarketData");
        var server = h.getLevel().getServer();
        ServerLevel nether = server.getLevel(Level.NETHER);
        Object market = call(data, "get", server.overworld());
        if (nether != null) h.assertTrue(call(data, "get", nether) == market, "the Nether has its own market");
        UUID player = UUID.randomUUID();
        long day = (long) call(market, "getCurrentDay");
        call(market, "addPlayerVolume", player, 5);
        call(market, "addSystemBought", "test:round1120", 7);
        h.assertTrue((int) call(market, "getPlayerVolume", player) == 5, "player volume");
        h.assertTrue((int) call(market, "getSystemBought", "test:round1120") == 7, "system bought");
        call(market, "setCurrentDay", day + 1);
        h.assertTrue((int) call(market, "getPlayerVolume", player) == 0 && (int) call(market, "getSystemBought", "test:round1120") == 0,
            "the counters did not start again with the new day");
        call(market, "setCurrentDay", day);
        h.succeed();
    }

    /** The quotes: half the spread on each side, the budget lowers later sales and refuses one above it. */
    @GameTest(batch = "round1120_market", template = "empty", timeoutTicks = 20)
    public static void marketQuotes(GameTestHelper h) {
        Class<?> quote = Round33GameTests.common("future.MarketQuote");
        h.assertTrue((long) call(quote, "buyCost", 100, 64, 0.06) == 6592, "buy cost");
        h.assertTrue((long) call(quote, "sellPayout", 100, 64, 0.06, 0, 0, 0.3) == 6208, "sale without a budget");
        long first = (long) call(quote, "sellPayout", 100, 64, 0.06, 0, 256, 0.3);
        long last = (long) call(quote, "sellPayout", 100, 64, 0.06, 192, 256, 0.3);
        h.assertTrue(first > last && last > 0, "the budget does not lower later sales: " + first + " / " + last);
        h.assertTrue((long) call(quote, "sellPayout", 100, 64, 0.06, 200, 256, 0.3) == -1, "a sale above the budget");
        h.succeed();
    }

    // ------------------------------------------------------------------ G1 / G2 grid

    /**
     * Two separate source - cable - sink lines. A cable placed next to line A rebuilds one component (line B keeps
     * its graph), and line B receives energy in every tick around the edit.
     */
    @GameTest(batch = "round1120_zones", template = "reactor", timeoutTicks = 120)
    public static void editRebuildsOnlyItsComponent(GameTestHelper h) {
        BlockState cable = block("wiring/cable/block_tin_cable_1");
        BlockPos sourceA = new BlockPos(1, 2, 1), sinkA = sourceA.east(2);
        BlockPos sourceB = new BlockPos(1, 2, 3), sinkB = sourceB.east(2);
        h.setBlock(sourceA, CoreExampleMod.SOURCE.get()); h.setBlock(sinkA, CoreExampleMod.SINK.get()); h.setBlock(sourceA.east(), cable);
        h.setBlock(sourceB, CoreExampleMod.SOURCE.get()); h.setBlock(sinkB, CoreExampleMod.SINK.get()); h.setBlock(sourceB.east(), cable);
        InheritedSource a = h.getBlockEntity(sourceA), b = h.getBlockEntity(sourceB);
        InheritedSink sinkOfB = h.getBlockEntity(sinkB);
        Object engine = engine(h);
        long[] before = new long[1];
        int[] tick = {0}, gaps = {0};
        h.onEachTick(() -> {
            int now = ++tick[0];
            if (now <= 90) { a.generate(32); b.generate(32); }
            if (now == 40) {
                before[0] = ((long[]) call(engine, "gridReuseCounters"))[0];
                h.setBlock(sourceA.east().above(), cable);                  // an edit at line A only
            }
            if (now == 45) {
                long[] counters = (long[]) call(engine, "gridReuseCounters");
                h.assertTrue(counters[0] - before[0] <= 2, "an edit at line A rebuilt " + (counters[0] - before[0]) + " components");
                h.assertTrue(counters[1] >= 1, "no component was kept");
            }
            if (now > 30 && now <= 80) {
                // line B: drained every tick, so each tick must bring new energy (no pause around the edit)
                if (now > 31 && sinkOfB.stored() <= 0) gaps[0]++;
                sinkOfB.getEnergyStorageInternal().setStored(0);
            }
            if (now == 81) {
                h.assertTrue(gaps[0] <= 1, "line B received nothing in " + gaps[0] + " ticks around the edit at line A");
                for (var p : java.util.List.of(sourceA, sourceA.east(), sourceA.east().above(), sinkA, sourceB, sourceB.east(), sinkB))
                    h.setBlock(p, Blocks.AIR);
            }
            if (now == 90) h.succeed();
        });
    }

    // load priority, the early end of a full window and the replay of a steady plan exist only in the fair distribution;
    // the addon test server runs the legacy walk (build.gradle), so this batch switches to fair as FairGridGameTests does
    @BeforeBatch(batch = "round1120_steady")
    public static void fairOnSteady(ServerLevel level) { System.setProperty("scex.independent.distribution", "fair"); }

    @AfterBatch(batch = "round1120_steady")
    public static void fairOffSteady(ServerLevel level) { System.setProperty("scex.independent.distribution", "legacy"); }

    /**
     * A source feeding a drained sink is a steady window: its allocation is replayed instead of computed. 0.1.10.42: the circuit
     * stands in a force-loaded Nether chunk. The engine plans a dimension as one grid, and energy blocks other tests leave in
     * the overworld (generators running by day, machines switching) broke the single-window plan now and then (a flaky test).
     */
    @GameTest(batch = "round1120_steady", template = "reactor", timeoutTicks = 120)
    public static void steadyWindowIsReplayed(GameTestHelper h) {
        ServerLevel nether = h.getLevel().getServer().getLevel(Level.NETHER);
        h.assertTrue(nether != null, "no Nether");
        BlockPos sourcePos = new BlockPos(4_200, 70, 4_200), cable = sourcePos.east(), sinkPos = cable.east();
        nether.getChunk(sourcePos);   // loads (and generates) the chunk now
        nether.setChunkForced(sourcePos.getX() >> 4, sourcePos.getZ() >> 4, true);
        nether.setBlock(sourcePos, CoreExampleMod.SOURCE.get().defaultBlockState(), 3);
        nether.setBlock(sinkPos, CoreExampleMod.SINK.get().defaultBlockState(), 3);
        nether.setBlock(cable, block("wiring/cable/block_tin_cable_1"), 3);
        InheritedSource source = (InheritedSource) nether.getBlockEntity(sourcePos);
        InheritedSink sink = (InheritedSink) nether.getBlockEntity(sinkPos);
        h.assertTrue(source != null && sink != null, "the Nether circuit has no block entities");
        Object engine = engine(h);
        long[] seen = new long[2];
        int[] tick = {0};
        h.onEachTick(() -> {
            int now = ++tick[0];
            if (now <= 90) source.generate(32);
            if (now > 10 && now <= 80) {
                if (now > 30 && sink.stored() > 0) seen[1]++;
                sink.getEnergyStorageInternal().setStored(0);
            }
            if (now == 30) seen[0] = ((long[]) call(engine, "gridReuseCounters"))[2];
            if (now == 81) {
                long replays = ((long[]) call(engine, "gridReuseCounters"))[2] - seen[0];
                for (var p : java.util.List.of(sourcePos, cable, sinkPos)) nether.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                nether.setChunkForced(sourcePos.getX() >> 4, sourcePos.getZ() >> 4, false);
                h.assertTrue(replays >= 20, "steady windows replayed: " + replays);
                h.assertTrue(seen[1] >= 45, "the replayed window did not deliver every tick: " + seen[1]);
            }
            if (now == 90) h.succeed();
        });
    }

    // ------------------------------------------------------------------ FCS (pure parts on the server)

    /** The tracker finds the turn of a circling target and none on a straight line; the intercept hits a crossing target. */
    @GameTest(batch = "round1120_fcs", template = "empty", timeoutTicks = 20)
    public static void fcsTrackerAndIntercept(GameTestHelper h) throws ReflectiveOperationException {
        Class<?> trackerType = Round33GameTests.common("fcs.TargetTracker");
        Object circle = trackerType.getDeclaredConstructor().newInstance(), line = trackerType.getDeclaredConstructor().newInstance();
        // positions as the client shows them: a new target every 3 ticks, 3 lerp steps
        double cx = 0, cz = 0, tx = 0, tz = 0; int steps = 0;
        for (int t = 0; t < 40; t++) {
            if (t % 3 == 0) { double angle = t * 0.25 / 5.0; tx = Math.cos(angle) * 5; tz = Math.sin(angle) * 5; steps = 3; }
            if (steps > 0) { cx += (tx - cx) / steps; cz += (tz - cz) / steps; steps--; }
            call(circle, "update", cx, 64.0, cz);
            call(line, "update", 0.2 * t, 64.0, 0.0);
        }
        h.assertTrue(Math.abs((double) call(circle, "turnRate")) > 0.03, "no turn found for a circling target");
        h.assertTrue((double) call(line, "turnRate") == 0.0, "a turn found on a straight line");

        Class<?> intercept = Round33GameTests.common("fcs.Intercept");
        Class<?> profileType = Round33GameTests.common("fcs.Intercept$Profile");
        Class<?> pathType = Round33GameTests.common("fcs.Intercept$TargetPath");
        Object bow = profileType.getDeclaredConstructor(double.class, double.class, double.class).newInstance(3.0, 0.05, 0.99);
        Object path = Proxy.newProxyInstance(pathType.getClassLoader(), new Class<?>[] {pathType}, (proxy, method, args) -> {
            if (!method.getName().equals("at")) return null;
            double ticks = (double) args[0]; double[] out = (double[]) args[1];
            out[0] = 40; out[1] = 64.9; out[2] = 0.2 * ticks;
            return null;
        });
        Object solution = null;
        for (var m : intercept.getDeclaredMethods()) {
            if (m.getName().equals("solve") && m.getParameterCount() == 5) {
                solution = m.invoke(null, bow, new double[] {0, 65.52, 0}, new double[] {0, 0, 0}, path, null);
            }
        }
        h.assertTrue(solution != null, "no fire-control solution at 40 m");
        h.assertTrue((double) call(solution, "miss") < 0.1, "the solution misses by " + call(solution, "miss"));
        h.succeed();
    }
}
