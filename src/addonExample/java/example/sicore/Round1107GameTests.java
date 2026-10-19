package example.sicore;

import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.api.energy.grid.NodeStats;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests of 0.1.10.7 (performance): cached energy switches, path profiles of deliveries and
 * deferred meter samples, cables without a ticker, the geomagnetic structure notice, the pacing of
 * item automation, the /mio_icif perf command.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round1107GameTests {
    private Round1107GameTests() {}

    private static final String CORE = "com.miophas.singularity_iteration" + ".core.";

    private static ResourceLocation rl(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }

    private static Class<?> core(String name) {
        try {
            return Class.forName(CORE + name);
        } catch (ClassNotFoundException e) {
            throw new AssertionError(name, e);
        }
    }

    private static BlockState cable(String name) {
        return BuiltInRegistries.BLOCK.get(rl("wiring/cable/" + name)).defaultBlockState();
    }

    // ------------------------------------------------------------------ E1: cached switches

    @GameTest(batch = "round1107_config", template = "empty", timeoutTicks = 20)
    public static void energySwitchesFollowTheConfig(GameTestHelper h) {
        Class<?> config = core("runtime.CoreConfig");
        Class<?> mode = core("runtime.energy.engine.IndependentEnergyMode");
        boolean before = (Boolean) Round33GameTests.call(config, "getBool", "energynet", "enableEnetShockDamage", true);
        try {
            Round33GameTests.call(config, "overrideBool", "energynet", "enableEnetShockDamage", false);
            h.assertTrue(!(Boolean) Round33GameTests.call(mode, "enetShockDamage"), "the cached switch ignored a config change");
            Round33GameTests.call(config, "overrideBool", "energynet", "enableEnetShockDamage", true);
            h.assertTrue((Boolean) Round33GameTests.call(mode, "enetShockDamage") == (Boolean) Round33GameTests.call(mode, "voltageOverload"),
                "the cached switch did not come back");
        } finally {
            Round33GameTests.call(config, "overrideBool", "energynet", "enableEnetShockDamage", before);
        }
        h.succeed();
    }

    @GameTest(batch = "round1107_config", template = "empty", timeoutTicks = 20)
    public static void automationPacingDelays(GameTestHelper h) {
        Class<?> pacing = core("runtime.processing.AutomationPacing");
        int[] expected = {4, 8, 16, 20, 20};
        for (int i = 0; i < expected.length; i++)
            h.assertTrue((Integer) Round33GameTests.call(pacing, "idleDelay", i + 1) == expected[i], "idle delay " + (i + 1));
        var perf = h.getLevel().getServer().getCommands().getDispatcher().getRoot().getChild("mio_icif");
        h.assertTrue(perf != null && perf.getChild("perf") != null, "/mio_icif perf is not registered");
        h.succeed();
    }

    // ------------------------------------------------------------------ E9: cables without a ticker

    @GameTest(batch = "round1107_wires", template = "empty", timeoutTicks = 60)
    public static void controlledWiresDoNotTick(GameTestHelper h) {
        BlockPos at = new BlockPos(2, 2, 2);
        h.setBlock(at, cable("block_tin_cable"));
        BlockEntity wire = h.getBlockEntity(at);
        BlockState state = h.getBlockState(at);
        h.assertTrue(((EntityBlock) state.getBlock()).getTicker(h.getLevel(), state, wire.getType()) == null,
            "a cable of the independent network still ticks");
        Round33GameTests.call(wire, "onEnergyPass");
        h.assertTrue((Boolean) Round33GameTests.call(wire, "isPowered"), "a cable that passed energy is not powered");
        h.runAfterDelay(25, () -> {
            h.assertTrue(!(Boolean) Round33GameTests.call(wire, "isPowered"), "the powered state did not end without a tick");
            h.succeed();
        });
    }

    // ------------------------------------------------------------------ E2/E3: path profiles and meter samples

    @GameTest(batch = "round1107_paths", template = "reactor", timeoutTicks = 120)
    public static void deliveriesUseCachedPathProfiles(GameTestHelper h) {
        var sourcePos = new BlockPos(1, 2, 2); var first = sourcePos.east(); var second = first.east(); var sinkPos = second.east();
        h.setBlock(sourcePos, CoreExampleMod.SOURCE.get()); h.setBlock(sinkPos, CoreExampleMod.SINK.get());
        h.setBlock(first, cable("block_tin_cable_1")); h.setBlock(second, cable("block_tin_cable_1"));
        InheritedSource source = h.getBlockEntity(sourcePos); InheritedSink sink = h.getBlockEntity(sinkPos);
        Object engine = Round33GameTests.call(core("runtime.energy.IndependentSiEnergy"), "current", h.getLevel().getServer());
        h.assertTrue(engine != null, "no energy engine");
        long hitsBefore = ((long[]) Round33GameTests.call(engine, "optimizationCounters"))[0];
        int[] tick = {0};
        h.onEachTick(() -> {
            int now = ++tick[0];
            if (now <= 100) source.generate(32);
            if (now > 60 && now <= 80) h.assertTrue(sink.stored() == 32, "the sink did not get the full packet: " + sink.stored());
            if (now == 80) {
                long hits = ((long[]) Round33GameTests.call(engine, "optimizationCounters"))[0];
                h.assertTrue(hits > hitsBefore, "no delivery used a path profile");
                NodeStats stats = MioIcifAPI.instance().getEnergyNetAPI().getNodeStats(h.getLevel(), h.absolutePos(first));
                h.assertTrue(stats != null && stats.energyIn() > 0, "the cable meter sample is missing: " + stats);
                h.assertTrue((Boolean) Round33GameTests.call(h.getBlockEntity(first), "isPowered"), "the cable on the path is not powered");
            }
            if (now <= 100) sink.getEnergyStorageInternal().setStored(0);
            if (now == 100) for (var p : java.util.List.of(sourcePos, first, second, sinkPos)) h.setBlock(p, Blocks.AIR);
            if (now == 115) h.succeed();
        });
    }

    // ------------------------------------------------------------------ G1: geomagnetic structure notice

    @GameTest(batch = "round1107_geomag", template = "empty", timeoutTicks = 40)
    public static void geomagneticStructureReactsAtOnce(GameTestHelper h) {
        BlockPos gen = new BlockPos(2, 2, 2);
        var antenna = BuiltInRegistries.BLOCK.get(rl("generator/block_geomagnetic_antenna"));
        var pedestal = BuiltInRegistries.BLOCK.get(rl("generator/block_geomagnetic_pedestal"));
        for (BlockPos p : new BlockPos[]{gen.offset(1, -1, 0), gen.offset(-1, -1, 0), gen.offset(0, -1, 1), gen.offset(0, -1, -1)})
            h.setBlock(p, pedestal);
        h.setBlock(gen.above(), antenna);
        h.setBlock(gen.above(2), antenna);
        h.setBlock(gen, BuiltInRegistries.BLOCK.get(rl("generator/block_geomagnetic_generator")));
        h.runAfterDelay(3, () -> {
            Object be = h.getBlockEntity(gen);
            h.assertTrue((Boolean) Round33GameTests.call(be, "isStructureComplete"), "the complete structure was not seen");
            h.setBlock(gen.above(2), Blocks.AIR);
            h.runAfterDelay(3, () -> {
                h.assertTrue(!(Boolean) Round33GameTests.call(h.getBlockEntity(gen), "isStructureComplete"),
                    "removing an antenna was not noticed before the periodic check");
                h.succeed();
            });
        });
    }
}
