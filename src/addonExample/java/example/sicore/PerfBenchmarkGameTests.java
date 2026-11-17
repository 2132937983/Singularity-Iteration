package example.sicore;

import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.api.crop.IPlanter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * 0.1.10.39: tick-cost benchmark of processing machines and crops, meant to run unchanged on older versions too (registry ids,
 * reflection and APIs that already existed in 0.1.10.37). Run alone: {@code ./gradlew runBenchGameTestServer} (namespace
 * si_bench). Each scenario places its blocks, lets the level tick them for a while (warm-up, idle sleep settles), then on each of
 * the next server ticks times one extra pass of every block entity's own ticker with System.nanoTime() (the level keeps ticking
 * them as usual, untimed). Reported: the mean cost of one tick of the whole group, as the median of five windows, and per block.
 */
@GameTestHolder(PerfBenchmarkGameTests.NAMESPACE)
public final class PerfBenchmarkGameTests {
    private PerfBenchmarkGameTests() {}

    static final String NAMESPACE = "si_bench";
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(PerfBenchmarkGameTests.class);
    private static final int MACHINES = 50, WARMUP = 300, WINDOWS = 5, WINDOW = 200, CROP_WINDOW = 256;
    private static final String FURNACE = "mio_icif:producer/block_furnace_elc", CENTRIFUGE = "mio_icif:producer/block_centrifuge_elc";

    @GameTestGenerator
    public static Collection<TestFunction> benchmarks() {
        List<TestFunction> tests = new ArrayList<>();
        tests.add(test("idle_furnaces", () -> machines(FURNACE, false, false, false)));
        tests.add(test("idle_centrifuges", () -> machines(CENTRIFUGE, false, false, false)));
        tests.add(test("active_furnaces", () -> machines(FURNACE, true, true, false)));
        tests.add(test("active_centrifuges", () -> machines(CENTRIFUGE, true, true, false)));
        tests.add(test("starved_furnaces", () -> machines(FURNACE, true, false, true)));
        tests.add(test("crop_field", PerfBenchmarkGameTests::crops));
        // 0.1.11.22: the generators of the 0.1.11.9..21 overhaul (the performance audit)
        tests.add(test("wind_rotors", PerfBenchmarkGameTests::windRotors));
        tests.add(test("water_generators", PerfBenchmarkGameTests::waterGenerators));
        tests.add(test("steam_turbines", PerfBenchmarkGameTests::steamTurbines));
        tests.add(test("water_rotors", PerfBenchmarkGameTests::waterRotors));
        // 0.1.11.23: the energy network (settlement and the terminal), the P2 batch of the performance audit
        tests.add(test("cable_network", () -> network(false)));
        tests.add(test("cable_network_terminal", () -> network(true)));
        return tests;
    }

    private static TestFunction test(String name, java.util.function.Supplier<Consumer<GameTestHelper>> body) {
        return new TestFunction("bench_" + name, "perfbenchmarkgametests." + name, NAMESPACE + ":empty", 2_400, 0, true, body.get());
    }

    private static ResourceLocation id(String path) { return ResourceLocation.parse(path); }

    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    @SuppressWarnings("deprecation")
    private static void setEu(BlockEntity be, long eu) { call(call(be, "getEnergyStorageInternal"), "setStored", eu); }

    @SuppressWarnings("deprecation")
    private static long capacity(BlockEntity be) { return (Long) call(call(be, "getEnergyStorageInternal"), "getCapacity"); }

    private static void setField(Object target, String name, Object value) {
        try {
            for (Class<?> k = target.getClass(); k != null; k = k.getSuperclass()) {
                try { var f = k.getDeclaredField(name); f.setAccessible(true); f.set(target, value); return; }
                catch (NoSuchFieldException next) { /* superclass */ }
            }
        } catch (IllegalAccessException e) { throw new AssertionError(e); }
        throw new AssertionError("no field " + name);
    }

    /** One timed pass: every block entity's own ticker once; returns the nanoseconds. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static long pass(GameTestHelper h, List<BlockPos> positions) {
        var level = h.getLevel();
        long start = System.nanoTime();
        for (BlockPos at : positions) {
            BlockEntity be = level.getBlockEntity(at);
            if (be == null) continue;
            BlockState state = be.getBlockState();
            BlockEntityTicker ticker = state.getTicker(level, (BlockEntityType) be.getType());
            if (ticker != null) ticker.tick(level, at, state, be);
        }
        return System.nanoTime() - start;
    }

    /** Warm-up, then WINDOWS windows of {@code window} timed passes; logs the result and succeeds. */
    private static void measure(GameTestHelper h, String scenario, List<BlockPos> positions, int window, Runnable beforeEachTick) {
        int total = WARMUP + WINDOWS * window;
        long[] sums = new long[WINDOWS];
        long[] all = new long[WINDOWS * window];
        int[] tick = {0};
        h.onEachTick(() -> {
            int now = tick[0]++;
            if (beforeEachTick != null) beforeEachTick.run();
            if (now < WARMUP) return;
            if (now >= total) return;
            long ns = pass(h, positions);
            sums[(now - WARMUP) / window] += ns;
            all[now - WARMUP] = ns;
            if (now == total - 1) {
                double[] means = new double[WINDOWS];
                for (int w = 0; w < WINDOWS; w++) means[w] = sums[w] / (double) window;
                double[] sorted = means.clone();
                Arrays.sort(sorted);
                long[] samples = all.clone();
                Arrays.sort(samples);
                double median = sorted[WINDOWS / 2];
                StringBuilder windows = new StringBuilder();
                for (double m : means) windows.append(windows.length() == 0 ? "" : " / ").append(String.format("%.1f", m / 1000));
                LOG.info("[bench] {} | blocks {} | mean per tick {} us (windows {} us) | p50 {} us p95 {} us | per block {} ns",
                    scenario, positions.size(), String.format("%.1f", median / 1000), windows,
                    String.format("%.1f", samples[samples.length / 2] / 1000.0), String.format("%.1f", samples[samples.length * 95 / 100] / 1000.0),
                    String.format("%.0f", median / positions.size()));
                h.succeed();
            }
        });
    }

    /**
     * 50 machines, 2 blocks apart, no cables. {@code input}: 64 cobblestone each. {@code fed}: the energy buffer is filled on every
     * tick (outside the timing). {@code starved}: only enough energy for one tick of work every second tick, so the machine starts
     * and stops all the time (lamp flicker). Without input the buffer is filled once.
     */
    private static Consumer<GameTestHelper> machines(String machine, boolean input, boolean fed, boolean starved) {
        return h -> {
            List<BlockPos> positions = new ArrayList<>();
            for (int i = 0; i < MACHINES; i++) {
                BlockPos at = new BlockPos((i % 10) * 2, 1, (i / 10) * 2);
                h.setBlock(at, BuiltInRegistries.BLOCK.get(id(machine)));
                positions.add(h.absolutePos(at));
            }
            var level = h.getLevel();
            for (BlockPos at : positions) {
                BlockEntity be = level.getBlockEntity(at);
                if (machine.equals(CENTRIFUGE)) setField(be, "heatStorage", 5000);   // a centrifuge that ran before: hot
                setEu(be, capacity(be));
                if (input) ((IItemHandler) call(be, "getItemHandler")).insertItem(0, new ItemStack(Items.COBBLESTONE, 64), false);
            }
            String scenario = (input ? fed ? "active " : "starved " : "idle ") + (machine.equals(FURNACE) ? "electric furnaces" : "thermal centrifuges");
            int[] tick = {0};
            measure(h, scenario, positions, WINDOW, !input ? null : () -> {
                int now = tick[0]++;
                for (BlockPos at : positions) {
                    BlockEntity be = level.getBlockEntity(at);
                    if (be == null) continue;
                    if (fed) setEu(be, capacity(be));
                    else if (starved) setEu(be, now % 2 == 0 ? 8 : 0);   // a little energy every second tick
                }
            });
        };
    }

    /** A 20 x 20 field of wheat on crop sticks (farmland under them, water and nutrients full): 400 crop block entities. */
    private static Consumer<GameTestHelper> crops() {
        return h -> {
            var wheat = MioIcifAPI.instance().getCropAPI().getPlant("mio_icif", "wheat");
            List<BlockPos> positions = new ArrayList<>();
            for (int x = 0; x < 20; x++) for (int z = 0; z < 20; z++) {
                BlockPos at = new BlockPos(x, 1, z);
                h.setBlock(at.below(), Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7));
                h.setBlock(at, BuiltInRegistries.BLOCK.get(id("mio_icif:crop/stick")));
                IPlanter crop = (IPlanter) h.getBlockEntity(at);
                crop.setPlant(wheat);
                crop.setGrowthStage(1 + (x + z) % 3);
                crop.setGrowthSpeed(10 + (x * 7 + z) % 20);
                crop.setYield(5 + (x + z * 3) % 20);
                crop.setResilience((x * z) % 25);
                crop.setWater(200);
                crop.setNutrients(100);
                positions.add(h.absolutePos(at));
            }
            measure(h, "crop field (wheat)", positions, CROP_WINDOW, null);
        };
    }

    private static Object field(Object target, String name) {
        for (Class<?> k = target.getClass(); k != null; k = k.getSuperclass()) {
            try { var f = k.getDeclaredField(name); f.setAccessible(true); return f.get(target); }
            catch (NoSuchFieldException next) { /* superclass */ }
            catch (IllegalAccessException e) { throw new AssertionError(e); }
        }
        throw new AssertionError("no field " + name);
    }

    private static ItemStack item(String path) { return new ItemStack(BuiltInRegistries.ITEM.get(id(path))); }

    /** 20 kinetic wind generators with carbon rotors (diameter 11), 24 blocks apart in the air, a kinetic generator behind each. */
    private static Consumer<GameTestHelper> windRotors() {
        return h -> {
            List<BlockPos> positions = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                BlockPos at = new BlockPos((i % 5) * 24, 30, (i / 5) * 40);
                h.setBlock(at, BuiltInRegistries.BLOCK.get(id("mio_icif:kugenerator/block_wind_kinetic_generator")));
                call(h.getBlockEntity(at), "setRotorStack", item("mio_icif:rotors/item_carbon_rotor"));
                positions.add(h.absolutePos(at));
            }
            measure(h, "kinetic wind generators (carbon rotors)", positions, WINDOW, null);
        };
    }

    /** 50 electric water generators, each with a water source in front. */
    private static Consumer<GameTestHelper> waterGenerators() {
        return h -> {
            List<BlockPos> positions = new ArrayList<>();
            for (int i = 0; i < MACHINES; i++) {
                BlockPos at = new BlockPos((i % 10) * 3, 1, (i / 10) * 3);
                h.setBlock(at, BuiltInRegistries.BLOCK.get(id("mio_icif:generator/block_water_generator")));
                var facing = h.getBlockState(at).getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING);
                h.setBlock(at.relative(facing), Blocks.WATER);
                positions.add(h.absolutePos(at));
            }
            var level = h.getLevel();
            measure(h, "water generators", positions, WINDOW, () -> {
                for (BlockPos at : positions) if (level.getBlockEntity(at) != null) setEu(level.getBlockEntity(at), 0);
            });
        };
    }

    /** 20 steam turbines (EU) full of steam, a condenser beside every other one, the storage emptied each tick. */
    private static Consumer<GameTestHelper> steamTurbines() {
        return h -> {
            List<BlockPos> positions = new ArrayList<>();
            var steam = BuiltInRegistries.FLUID.get(id("mio_icif:steam"));
            for (int i = 0; i < 20; i++) {
                BlockPos at = new BlockPos((i % 5) * 3, 1, (i / 5) * 3);
                h.setBlock(at, BuiltInRegistries.BLOCK.get(id("mio_icif:generator/block_steam_turbine")));
                if (i % 2 == 0) h.setBlock(at.east(), BuiltInRegistries.BLOCK.get(id("mio_icif:producer/block_condenser")));
                positions.add(h.absolutePos(at));
            }
            var level = h.getLevel();
            measure(h, "steam turbines (half with a condenser)", positions, WINDOW, () -> {
                for (BlockPos at : positions) {
                    var be = level.getBlockEntity(at);
                    if (be == null) continue;
                    setEu(be, 0);
                    var tank = (net.neoforged.neoforge.fluids.capability.IFluidHandler) field(be, "fuelTank");
                    tank.fill(new net.neoforged.neoforge.fluids.FluidStack(steam, 2000), net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                }
            });
        };
    }

    /** 20 kinetic water generators with carbon rotors in a plane of water (diameter 11 discs). */
    private static Consumer<GameTestHelper> waterRotors() {
        return h -> {
            List<BlockPos> positions = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                BlockPos at = new BlockPos((i % 5) * 14, 8, (i / 5) * 4 + 2);
                h.setBlock(at, BuiltInRegistries.BLOCK.get(id("mio_icif:kugenerator/block_water_kinetic_generator")));
                for (int dx = -5; dx <= 5; dx++) for (int dy = -5; dy <= 5; dy++) h.setBlock(at.offset(dx, dy, -1), Blocks.WATER);
                call(h.getBlockEntity(at), "setRotorStack", item("mio_icif:rotors/item_carbon_rotor"));
                positions.add(h.absolutePos(at));
            }
            measure(h, "kinetic water generators (carbon rotors, water discs)", positions, WINDOW, null);
        };
    }

    // ---- 0.1.11.23: the energy network ------------------------------------------------------------------------------

    /** The level being timed, and the start of the timed part of its END (between the HIGH and LOWEST listeners). */
    private static volatile net.minecraft.server.level.ServerLevel timedLevel;
    private static long settleStart, settleNanos;
    private static boolean listening;

    private static final int ROWS = Integer.getInteger("si.bench.rows", 20), ROW = 16;

    private static synchronized void listen() {
        if (listening) return;
        listening = true;
        var bus = net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
        bus.addListener(net.neoforged.bus.api.EventPriority.HIGH, net.neoforged.neoforge.event.tick.LevelTickEvent.Post.class, e -> {
            if (e.getLevel() == timedLevel) settleStart = System.nanoTime();
        });
        bus.addListener(net.neoforged.bus.api.EventPriority.LOWEST, net.neoforged.neoforge.event.tick.LevelTickEvent.Post.class, e -> {
            if (e.getLevel() == timedLevel && settleStart != 0) { settleNanos = System.nanoTime() - settleStart; settleStart = 0; }
        });
    }

    /**
     * A 339-block glass fibre cable run (a standing serpentine) with 16 sources (32 EU/t each, topped up every tick) along its
     * first row and 16 sinks (drained every tick) along its last, so every delivery crosses about 300 to 339 cables; {@code terminal}: an energy terminal on the run. Timed: one pass
     * of the block entities (the terminal's own tick) plus the energy settlement at the level's END, as measured by
     * listeners around it (it runs at LOW priority).
     */
    private static Consumer<GameTestHelper> network(boolean terminal) {
        return h -> {
            listen();
            var cable = BuiltInRegistries.BLOCK.get(id(System.getProperty("si.bench.cable", "mio_icif:wiring/cable/block_glass_cable")));
            var level = h.getLevel();
            List<BlockPos> sources = new ArrayList<>(), sinks = new ArrayList<>(), timed = new ArrayList<>();
            // a serpentine standing in a 16 x 40 wall: the engine settles only when every conductor chunk ticks, and the test
            // server ticks only the chunks around the test. 20 rows of 16 cables at y 2, 4 .. 40, joined at alternate ends;
            // the sources beside row 0 (z 1), the sinks beside the last (z 3)
            List<BlockPos> run = new ArrayList<>();
            for (int row = 0; row < ROWS; row++) {
                for (int x = 0; x < ROW; x++) run.add(new BlockPos(x, 2 + row * 2, 2));
                if (row < ROWS - 1) run.add(new BlockPos(row % 2 == 0 ? ROW - 1 : 0, 3 + row * 2, 2));
            }
            for (BlockPos at : run) h.setBlock(at, cable);
            for (int i = 0; i < ROW; i++) {
                BlockPos at = new BlockPos(i, 2, 1);
                h.setBlock(at, CoreExampleMod.SOURCE.get());
                sources.add(h.absolutePos(at));
            }
            for (int i = 0; i < ROW; i++) {
                BlockPos at = new BlockPos(i, ROWS * 2, 3);
                h.setBlock(at, CoreExampleMod.SINK.get());
                sinks.add(h.absolutePos(at));
            }
            if (terminal) {
                BlockPos at = new BlockPos(8, 7, 2);
                h.setBlock(at, BuiltInRegistries.BLOCK.get(id("mio_icif:wiring/block_energy_terminal")));
                timed.add(h.absolutePos(at));
            }
            timed.addAll(sources);
            timed.addAll(sinks);
            int total = WARMUP + WINDOWS * WINDOW;
            long[] sums = new long[WINDOWS], settleSums = new long[WINDOWS], passSum = {0};
            long[] all = new long[WINDOWS * WINDOW];
            long[] pending = {-1}, delivered = {0};
            int[] tick = {0};
            timedLevel = level;
            Thread serverThread = Thread.currentThread();
            java.util.Map<String, int[]> profile = new java.util.HashMap<>();
            boolean profiling = Boolean.getBoolean("si.bench.profile");
            h.onEachTick(() -> {
                int now = tick[0]++;
                // last tick's settlement belongs to last tick's sample
                if (pending[0] >= 0) {
                    int k = (int) pending[0];
                    sums[k / WINDOW] += settleNanos;
                    settleSums[k / WINDOW] += settleNanos;
                    all[k] += settleNanos;
                    pending[0] = -1;
                }
                settleNanos = 0;
                for (BlockPos at : sources) if (level.getBlockEntity(at) instanceof InheritedSource src) src.generate(32);
                for (BlockPos at : sinks) if (level.getBlockEntity(at) instanceof InheritedSink sink) { delivered[0] += sink.stored(); setEu(sink, 0); }
                if (profiling && now == WARMUP) {
                    Thread sampler = new Thread(() -> {
                        while (timedLevel == level) {
                            StackTraceElement[] st = serverThread.getStackTrace();
                            // the innermost frame of the energy engine and the frame under it (self time by method)
                            String key = null;
                            for (int i = 0; i < st.length && i < 60; i++) {
                                String c = st[i].getClassName();
                                if (c.contains("runtime.energy") || c.contains("platform.network") || c.contains("wiring")) {
                                    key = c.substring(c.lastIndexOf('.') + 1) + "." + st[i].getMethodName() + ":" + st[i].getLineNumber()
                                        + " <- " + (i > 0 ? st[i - 1].getClassName().substring(st[i - 1].getClassName().lastIndexOf('.') + 1) + "." + st[i - 1].getMethodName() : "");
                                    break;
                                }
                            }
                            if (key != null) synchronized (profile) { profile.computeIfAbsent(key, k -> new int[1])[0]++; }
                            try { Thread.sleep(0, 200_000); } catch (InterruptedException e) { return; }
                        }
                    }, "si-bench-sampler");
                    sampler.setDaemon(true);
                    sampler.start();
                }
                if (now < WARMUP || now >= total) {
                    if (now == total) {
                        timedLevel = null;
                        if (profiling) synchronized (profile) {
                            profile.entrySet().stream().sorted((a, b) -> b.getValue()[0] - a.getValue()[0]).limit(40)
                                .forEach(e -> LOG.info("[profile] {} {}", e.getValue()[0], e.getKey()));
                        }
                        double[] means = new double[WINDOWS];
                        for (int w = 0; w < WINDOWS; w++) means[w] = sums[w] / (double) WINDOW;
                        double[] sorted = means.clone();
                        Arrays.sort(sorted);
                        long[] samples = all.clone();
                        Arrays.sort(samples);
                        double settle = 0;
                        for (long v : settleSums) settle += v;
                        StringBuilder windows = new StringBuilder();
                        for (double m : means) windows.append(windows.length() == 0 ? "" : " / ").append(String.format("%.1f", m / 1000));
                        LOG.info("[bench] {} | blocks {} | mean per tick {} us (windows {} us) | p50 {} us p95 {} us | settlement {} us | terminal tick {} us | per block {} ns",
                            terminal ? "cable network with a terminal" : "cable network", timed.size() + run.size(),
                            String.format("%.1f", sorted[WINDOWS / 2] / 1000), windows,
                            String.format("%.1f", samples[samples.length / 2] / 1000.0), String.format("%.1f", samples[samples.length * 95 / 100] / 1000.0),
                            String.format("%.1f", settle / (WINDOWS * WINDOW) / 1000), String.format("%.1f", passSum[0] / (double) (WINDOWS * WINDOW) / 1000),
                            String.format("%.0f", sorted[WINDOWS / 2] / (timed.size() + run.size())));
                        int cables = 0;
                        for (BlockPos at : run) if (level.getBlockState(h.absolutePos(at)).is(cable)) cables++;
                        long held = 0;
                        for (BlockPos at : sources) if (level.getBlockEntity(at) instanceof InheritedSource src) held += src.stored();
                        var engine = com.miophas.singularity_iteration.core.runtime.energy.IndependentSiEnergy.current(level.getServer());
                        LOG.info("[bench] {} | delivered {} EU/t ({} EU in all) | cables left {} | sources hold {} EU | first sink stats {} | last cable stats {}",
                            terminal ? "cable network with a terminal" : "cable network", delivered[0] / total, delivered[0], cables, held,
                            engine == null ? null : engine.nodeStats(level, sinks.get(0)),
                            engine == null ? null : engine.nodeStats(level, h.absolutePos(run.get(run.size() - 1))));
                        h.assertTrue(delivered[0] > 0, "the sinks get energy");
                        h.succeed();
                    }
                    return;
                }
                long ns = pass(h, terminal ? timed.subList(0, 1) : List.of());
                passSum[0] += ns;
                int k = now - WARMUP;
                sums[k / WINDOW] += ns;
                all[k] = ns;
                pending[0] = k;
            });
        };
    }
}
