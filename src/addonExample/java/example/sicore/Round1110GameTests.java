package example.sicore;

import com.miophas.singularity_iteration.core.api.capability.IMioIcifCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests of 0.1.10.10 (performance, phase D): save marks instead of per-tick setChanged (D1), the
 * neighbour capability cache (D3), saturated windows (E11), the radiation check of creatures that
 * wear a nuclear item (D6), and the load line of /mio_icif perf (D7).
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round1110GameTests {
    private Round1110GameTests() {}

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

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static long total(String kind) {
        Class<?> kinds = core("runtime.PerfCounters$Kind");
        Object value = Enum.valueOf((Class) kinds, kind);
        return (Long) Round33GameTests.call(core("runtime.PerfCounters"), "total", value);
    }

    private static BlockState facing(BlockState state, Direction direction) {
        return state.setValue((DirectionProperty) state.getBlock().getStateDefinition().getProperty("facing"), direction);
    }

    // ------------------------------------------------------------------ D1

    /** A solar panel keeps its chunk marked for saving every tick, but no longer updates its neighbours. */
    @GameTest(batch = "round1110_marks", template = "empty", timeoutTicks = 60)
    public static void solarPanelMarksSaveWithoutNeighbourUpdates(GameTestHelper h) {
        h.setBlock(new BlockPos(2, 1, 2), block("generator/block_solar_generator"));
        long[] before = new long[2];
        h.runAfterDelay(5, () -> { before[0] = total("SET_CHANGED"); before[1] = total("SAVE_MARKS"); });
        h.runAfterDelay(30, () -> {
            long changed = total("SET_CHANGED") - before[0], marks = total("SAVE_MARKS") - before[1];
            h.assertTrue(marks >= 20, "the panel did not mark its chunk each tick: " + marks);
            h.assertTrue(changed <= 3, "the panel still calls setChanged every tick: " + changed);
            h.succeed();
        });
    }

    // ------------------------------------------------------------------ D3

    /** The kept heat capability of a neighbour follows the neighbour: same object, gone, other facing. */
    @GameTest(batch = "round1110_caps", template = "empty", timeoutTicks = 40)
    public static void neighbourCapsFollowTheNeighbour(GameTestHelper h) {
        BlockPos owner = new BlockPos(1, 1, 2), neighbour = owner.east();
        h.setBlock(owner, block("generator/block_solar_generator"));
        h.setBlock(neighbour, facing(block("generator/block_stirling_generator"), Direction.WEST));   // front toward the owner
        Class<?> caps = core("runtime.NeighbourCaps");
        h.runAfterDelay(2, () -> {
            var tile = h.getBlockEntity(owner);
            Object first = Round33GameTests.call(caps, "get", tile, IMioIcifCapabilities.HEAT_STORAGE_BLOCK, Direction.EAST);
            long cached = total("CAP_CACHED");
            Object second = Round33GameTests.call(caps, "get", tile, IMioIcifCapabilities.HEAT_STORAGE_BLOCK, Direction.EAST);
            h.assertTrue(first != null, "the front face of the Stirling Generator gives no heat capability");
            h.assertTrue(first == second && total("CAP_CACHED") == cached + 1, "the second lookup was not answered from the cache");
            h.setBlock(neighbour, Blocks.AIR);
            h.assertTrue(Round33GameTests.call(caps, "get", tile, IMioIcifCapabilities.HEAT_STORAGE_BLOCK, Direction.EAST) == null,
                "a removed neighbour still answers");
            h.setBlock(neighbour, facing(block("generator/block_stirling_generator"), Direction.NORTH));   // side toward the owner
            h.assertTrue(Round33GameTests.call(caps, "get", tile, IMioIcifCapabilities.HEAT_STORAGE_BLOCK, Direction.EAST) == null,
                "a side face of the Stirling Generator gives a heat capability");
            h.succeed();
        });
    }

    // ------------------------------------------------------------------ E11

    // load priority, the early end of a full window and the replay of a steady plan exist only in the fair distribution;
    // the addon test server runs the legacy walk (build.gradle), so this batch switches to fair as FairGridGameTests does
    @BeforeBatch(batch = "round1110_saturated")
    public static void fairOnSaturated(ServerLevel level) { System.setProperty("scex.independent.distribution", "fair"); }

    @AfterBatch(batch = "round1110_saturated")
    public static void fairOffSaturated(ServerLevel level) { System.setProperty("scex.independent.distribution", "legacy"); }

    /** A sink without room: the windows end early; with room again, energy arrives. */
    @GameTest(batch = "round1110_saturated", template = "reactor", timeoutTicks = 120)
    public static void saturatedWindowsEndEarly(GameTestHelper h) {
        var sourcePos = new BlockPos(1, 2, 2); var cable = sourcePos.east(); var sinkPos = cable.east();
        h.setBlock(sourcePos, CoreExampleMod.SOURCE.get()); h.setBlock(sinkPos, CoreExampleMod.SINK.get());
        h.setBlock(cable, BuiltInRegistries.BLOCK.get(rl("wiring/cable/block_tin_cable_1")).defaultBlockState());
        InheritedSource source = h.getBlockEntity(sourcePos); InheritedSink sink = h.getBlockEntity(sinkPos);
        Object engine = Round33GameTests.call(core("runtime.energy.IndependentSiEnergy"), "current", h.getLevel().getServer());
        h.assertTrue(engine != null, "no energy engine");
        long[] seen = new long[2];
        int[] tick = {0};
        h.onEachTick(() -> {
            int now = ++tick[0];
            if (now <= 100) source.generate(32);
            if (now <= 60) sink.getEnergyStorageInternal().setStored(sink.getEnergyStorageInternal().getCapacity());
            if (now == 20) seen[0] = ((long[]) Round33GameTests.call(engine, "optimizationCounters"))[5];
            if (now == 60) {
                long saturated = ((long[]) Round33GameTests.call(engine, "optimizationCounters"))[5];
                h.assertTrue(saturated > seen[0], "no saturated window ended early");
                sink.getEnergyStorageInternal().setStored(0);
            }
            if (now > 75 && now <= 100 && sink.stored() > 0) seen[1] = 1;
            if (now == 101) {
                h.assertTrue(seen[1] == 1, "the sink with room again received nothing");
                for (var p : java.util.List.of(sourcePos, cable, sinkPos)) h.setBlock(p, Blocks.AIR);
            }
            if (now == 110) h.succeed();
        });
    }

    // ------------------------------------------------------------------ D6

    /** A creature that wears a nuclear item gets radiation without a per-entity tick hook. */
    @GameTest(batch = "round1110_radiation", template = "empty", timeoutTicks = 80)
    public static void nuclearWearerGetsRadiation(GameTestHelper h) {
        var zombie = h.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(2, 1, 2));
        zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(BuiltInRegistries.ITEM.get(rl("resource/item_uran"))));
        var radiation = BuiltInRegistries.MOB_EFFECT.getHolder(rl("radiation")).orElseThrow();
        h.succeedWhen(() -> h.assertTrue(zombie.hasEffect(radiation), "the zombie with uranium on its head has no radiation"));
    }

    // ------------------------------------------------------------------ D7

    @GameTest(batch = "round1110_marks", template = "empty", timeoutTicks = 20)
    public static void perfCommandKnowsTheLoadLine(GameTestHelper h) {
        var perf = h.getLevel().getServer().getCommands().getDispatcher().getRoot().getChild("mio_icif");
        h.assertTrue(perf != null && perf.getChild("perf") != null, "/mio_icif perf is not registered");
        h.assertTrue(core("runtime.PerfCounters$Kind").getEnumConstants().length >= 9, "load counters missing");
        h.succeed();
    }
}
