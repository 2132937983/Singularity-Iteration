package example.sicore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;

/**
 * GameTests of 0.1.10.8 (performance, phase C): the route plan cache of multi-window grids, the
 * idle sleep of basic machines and its wake-ups, the config switch of the sleep, the reuse line of
 * /mio_icif perf.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round1108GameTests {
    private Round1108GameTests() {}

    private static final String CORE = "com.miophas.singularity_iteration" + ".core.";

    private static ResourceLocation rl(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }

    private static Class<?> core(String name) {
        try {
            return Class.forName(CORE + name);
        } catch (ClassNotFoundException e) {
            throw new AssertionError(name, e);
        }
    }

    private static BlockState block(String path) {
        return BuiltInRegistries.BLOCK.get(rl(path)).defaultBlockState();
    }

    private static long skippedTicks() {
        return (Long) Round33GameTests.call(core("runtime.processing.MachineSleep"), "skippedTicks");
    }

    // ------------------------------------------------------------------ E7: route plan cache

    /**
     * 50 sources on a 5 x 5 cable layer (above and below it) form two native windows (at most 32
     * sources each). Once warm, both windows come from the plan cache: no new plan while the grid
     * stays the same, and the sink keeps receiving. Removing a source builds new plans.
     */
    @GameTest(batch = "round1108_plans", template = "reactor", timeoutTicks = 160)
    public static void multiWindowPlansComeFromTheCache(GameTestHelper h) {
        var sources = new ArrayList<InheritedSource>();
        var sourcePositions = new ArrayList<BlockPos>();
        for (int x = 1; x <= 5; x++) for (int z = 1; z <= 5; z++) {
            h.setBlock(new BlockPos(x, 3, z), block("wiring/cable/block_tin_cable_1"));
            for (int y : new int[]{2, 4}) {
                BlockPos at = new BlockPos(x, y, z);
                h.setBlock(at, CoreExampleMod.SOURCE.get());
                sourcePositions.add(at);
            }
        }
        BlockPos sinkPos = new BlockPos(0, 3, 3);
        h.setBlock(sinkPos, CoreExampleMod.SINK.get());
        for (BlockPos at : sourcePositions) sources.add(h.getBlockEntity(at));
        InheritedSink sink = h.getBlockEntity(sinkPos);
        Object engine = Round33GameTests.call(core("runtime.energy.IndependentSiEnergy"), "current", h.getLevel().getServer());
        h.assertTrue(engine != null, "no energy engine");
        long[] seen = new long[4];   // cache hits at 50, plans built at 50, plans built at 90, received in 50..90
        int[] tick = {0};
        h.onEachTick(() -> {
            int now = ++tick[0];
            if (now <= 130) for (var source : sources) if (!source.isRemoved()) source.generate(32);
            long stored = sink.stored();
            if (now > 50 && now <= 90) seen[3] += stored;
            sink.getEnergyStorageInternal().setStored(0);
            long hits = ((long[]) Round33GameTests.call(engine, "optimizationCounters"))[4];
            long built = (Long) Round33GameTests.call(Round33GameTests.call(engine, "routingMetrics", h.getLevel()), "plansBuilt");
            if (now == 50) { seen[0] = hits; seen[1] = built; }
            if (now == 90) {
                seen[2] = built;
                h.assertTrue(hits > seen[0], "the windows did not come from the plan cache");
                h.assertTrue(built == seen[1], "plans were built again on an unchanged grid: " + seen[1] + " -> " + built);
                h.assertTrue(seen[3] > 0, "the sink received nothing");
                h.setBlock(sourcePositions.get(7), Blocks.AIR);
            }
            if (now == 110) {
                h.assertTrue(built > seen[2], "removing a source did not build new plans");
            }
            if (now == 130) {
                for (int x = 0; x <= 5; x++) for (int y = 2; y <= 4; y++) for (int z = 0; z <= 5; z++) h.setBlock(new BlockPos(x, y, z), Blocks.AIR);
            }
            if (now == 145) h.succeed();
        });
    }

    // ------------------------------------------------------------------ M3: machine sleep

    @GameTest(batch = "round1108_sleep", template = "empty", timeoutTicks = 80)
    public static void idleMaceratorSleepsAndWakesOnInput(GameTestHelper h) {
        BlockPos at = new BlockPos(2, 1, 2);
        h.setBlock(at, block("producer/block_powder_elc"));
        long skippedBefore = skippedTicks();
        h.runAfterDelay(30, () -> {
            BlockEntity machine = h.getBlockEntity(at);
            h.assertTrue((Boolean) Round33GameTests.call(machine, "isIdleAsleep"), "an idle macerator does not sleep");
            h.assertTrue(skippedTicks() > skippedBefore, "no machine tick was skipped");
            IItemHandler items = (IItemHandler) Round33GameTests.call(machine, "getItemHandler");
            items.insertItem(0, new ItemStack(Items.COBBLESTONE), false);
            h.runAfterDelay(1, () -> {
                h.assertTrue(!(Boolean) Round33GameTests.call(machine, "isIdleAsleep"), "an inserted item did not wake the macerator");
                h.succeed();
            });
        });
    }

    @GameTest(batch = "round1108_sleep", template = "empty", timeoutTicks = 80)
    public static void idleRecyclerSleeps(GameTestHelper h) {
        // the recycler marked itself changed every tick before 0.1.10.8 and could never sleep
        BlockPos at = new BlockPos(2, 1, 2);
        h.setBlock(at, block("producer/block_recycler_elc"));
        h.runAfterDelay(30, () -> {
            h.assertTrue((Boolean) Round33GameTests.call(h.getBlockEntity(at), "isIdleAsleep"), "an idle recycler does not sleep");
            h.succeed();
        });
    }

    @GameTest(batch = "round1108_sleep_off", template = "empty", timeoutTicks = 80)
    public static void sleepFollowsTheConfigSwitch(GameTestHelper h) {
        Class<?> sleep = core("runtime.processing.MachineSleep");
        boolean before = (Boolean) Round33GameTests.call(sleep, "enabled");
        Round33GameTests.call(sleep, "setEnabled", false);
        BlockPos at = new BlockPos(2, 1, 2);
        h.setBlock(at, block("producer/block_powder_elc"));
        h.runAfterDelay(30, () -> {
            try {
                h.assertTrue(!(Boolean) Round33GameTests.call(h.getBlockEntity(at), "isIdleAsleep"), "a machine slept with the sleep switched off");
            } finally {
                Round33GameTests.call(sleep, "setEnabled", before);
            }
            h.succeed();
        });
    }

    @GameTest(batch = "round1108_sleep", template = "empty", timeoutTicks = 20)
    public static void sleepConstantsAndPerfLine(GameTestHelper h) {
        Class<?> sleep = core("runtime.processing.MachineSleep");
        int recheck;
        try {
            recheck = sleep.getField("RECHECK_TICKS").getInt(null);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("RECHECK_TICKS", e);
        }
        h.assertTrue(recheck <= 20, "a missed wake-up could wait longer than 1 s");
        var perf = h.getLevel().getServer().getCommands().getDispatcher().getRoot().getChild("mio_icif");
        h.assertTrue(perf != null && perf.getChild("perf") != null, "/mio_icif perf is not registered");
        h.succeed();
    }
}
