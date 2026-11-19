package example.sicore;

import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.api.kinetic.IKineticAPI;
import com.miophas.singularity_iteration.core.api.kinetic.IKineticStorageAccess;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.86: the kinetic API (MioIcifAPI.getKineticAPI) sees SI's own kinetic blocks. It knew only block entities that
 * implement the core API kinetic interfaces; SI's kinetic sources and kinetic generators implement none of them and
 * expose the KINETIC_STORAGE_BLOCK capability instead, so hasKineticTile said false, getKineticStorage was empty and
 * insertKinetic moved nothing for every one of them.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11586GameTests {
    private Round11586GameTests() {}

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }

    private static void place(GameTestHelper h, BlockPos at, String block, boolean facingEast) {
        var state = BuiltInRegistries.BLOCK.get(id(block)).defaultBlockState();
        h.setBlock(at, facingEast ? state.setValue(BlockStateProperties.FACING, Direction.EAST) : state);
    }

    @GameTest(template = "empty", batch = "round11586_kinetic_api", timeoutTicks = 40)
    public static void kineticApiSeesSiKineticBlocks(GameTestHelper h) {
        BlockPos chest = new BlockPos(0, 1, 1), twin = new BlockPos(1, 1, 1), stirling = new BlockPos(2, 1, 1), turbo = new BlockPos(3, 1, 1),
            empty = new BlockPos(4, 1, 1), basic = new BlockPos(0, 2, 1);
        Map<BlockPos, String> sources = Map.of(new BlockPos(0, 1, 3), "kugenerator/block_wind_kinetic_generator",
            new BlockPos(1, 1, 3), "kugenerator/block_water_kinetic_generator", new BlockPos(2, 1, 3), "kugenerator/block_manual_kinetic_generator",
            new BlockPos(3, 1, 3), "producer/block_steam_kinetic_generator", new BlockPos(4, 1, 3), "kugenerator/block_kinetic_generator_elc",
            stirling, "kugenerator/block_stirling_kinetic_generator");
        Map<BlockPos, String> generators = Map.of(twin, "generator/block_twin_turbo_kinetic_generator",
            turbo, "generator/block_turbo_kinetic_generator", basic, "generator/block_kinetic_generator");
        h.setBlock(chest, Blocks.CHEST);
        sources.forEach((at, block) -> place(h, at, block, false));
        generators.forEach((at, block) -> place(h, at, block, true));

        h.runAfterDelay(3, () -> {
            IKineticAPI api = MioIcifAPI.instance().getKineticAPI();
            var level = h.getLevel();
            List<String> wrong = new ArrayList<>();
            Map<BlockPos, String> all = new java.util.HashMap<>(sources);
            all.putAll(generators);
            all.forEach((at, block) -> {
                BlockPos abs = h.absolutePos(at);
                if (!api.hasKineticTile(level, abs)) wrong.add(block + ": hasKineticTile false");
                if (api.getKineticStorage(level, abs).isEmpty()) wrong.add(block + ": no kinetic storage");
            });
            for (BlockPos none : List.of(chest, empty)) {
                if (api.hasKineticTile(level, h.absolutePos(none))) wrong.add(none + " (not kinetic): hasKineticTile true");
                if (api.getKineticStorage(level, h.absolutePos(none)).isPresent()) wrong.add(none + " (not kinetic): has a kinetic storage");
            }

            // KU goes into a generator through its front, as a kinetic source would put it there; a generator gives none
            BlockPos twinAbs = h.absolutePos(twin);
            IKineticStorageAccess generator = api.getKineticStorage(level, twinAbs).orElseThrow();
            long before = generator.getKineticStored();
            long simulated = api.insertKinetic(level, twinAbs, 1_000L, true);
            long inserted = api.insertKinetic(level, twinAbs, 1_000L, false);
            if (simulated != 1_000L || inserted != 1_000L) wrong.add("twin turbo took " + simulated + " (simulated) and " + inserted + " of 1000 KU");
            if (generator.getKineticStored() - before != inserted) wrong.add("twin turbo stores " + (generator.getKineticStored() - before) + " KU more after taking " + inserted);
            if (api.extractKinetic(level, twinAbs, 1_000L, true) != 0) wrong.add("twin turbo gives KU");
            if (generator.isKineticSource()) wrong.add("twin turbo counts as a kinetic source");
            if (!api.getKineticStorage(level, h.absolutePos(stirling)).orElseThrow().isKineticSource()) wrong.add("Stirling kinetic generator is not a kinetic source");

            // the twin's neighbours: the Stirling kinetic generator (east) is kinetic, the chest (west) is not
            Set<Direction> links = new HashSet<>(api.getKineticConnections(level, twinAbs));
            if (!links.equals(Set.of(Direction.EAST))) wrong.add("twin turbo connections " + links + ", expected [east]");

            // the kinetic generators' screens: a wind or water kinetic generator in front is "wind or water" (1); the water
            // one had no capability and showed as nothing (0)
            Class<?> menu = Round33GameTests.common("menu.generator.MimicGeneratorMenu");
            for (BlockPos source : List.of(new BlockPos(0, 1, 3), new BlockPos(1, 1, 3))) {
                int[] shown = (int[]) Round33GameTests.call(menu, "source", level, h.absolutePos(source.above()), Direction.DOWN);
                if (shown[0] != 1) wrong.add(sources.get(source) + " in front of a generator shows as source kind " + shown[0] + ", expected 1");
            }

            h.assertTrue(wrong.isEmpty(), "kinetic API on SI blocks: " + wrong);
            h.succeed();
        });
    }
}
