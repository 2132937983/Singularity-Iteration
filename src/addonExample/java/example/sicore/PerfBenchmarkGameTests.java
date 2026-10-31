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
}
