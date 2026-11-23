package example.sicore;

import static example.sicore.Round33GameTests.call;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.189: the Blueprint Builder takes a block in the way out and builds the blueprint's block (a block with a block entity,
 * a chest, stays and counts as obstructed); after the last block it checks the build again and builds what went missing
 * meanwhile, and a start after the build checks it once more. The blueprint is Round111182GameTests' two stone blocks from an
 * upload, the material an Infinite Armory Material Interface.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111189GameTests {
    private Round111189GameTests() {}

    private static final BlockPos AT = new BlockPos(1, 2, 1);

    private static Block block(String id) { return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", id)); }

    private static void delete(Path dir) {
        try {
            if (Files.isDirectory(dir)) try (Stream<Path> files = Files.walk(dir)) {
                files.sorted(Comparator.reverseOrder()).forEach(f -> f.toFile().delete());
            }
        } catch (java.io.IOException ignored) { }
    }

    /** A builder with the two stone blocks loaded and power; returns their world positions, west first. */
    private static BlockPos[] loaded(GameTestHelper h, BlockEntity builder) throws Exception {
        h.setBlock(new BlockPos(1, 2, 3), block("producer/block_infinite_material_interface"));
        ServerPlayer p = ArmoryServiceGameTests.player(h);
        Class<?> up = Round33GameTests.common("builder.BlueprintUploads");
        Path dir = (Path) call(up, "folder", p.getUUID());
        try {
            byte[] file = Round111182GameTests.structure();
            h.assertTrue((int) call(up, "begin", p, builder, "stone.nbt", file.length, call(up, "sha256", file)) == 1, "the upload is not accepted");
            h.assertTrue((int) call(up, "chunk", p, 0, file) == 2, "the upload is not done");
        } finally {
            delete(dir);
            p.discard();
        }
        List<?> plan = (List<?>) call(builder, "plan");
        h.assertTrue(plan != null && plan.size() == 2, "the plan is " + plan);
        call(builder, "apiSetEnergy", 200_000L);
        BlockPos a = (BlockPos) call(plan.get(0), "pos"), b = (BlockPos) call(plan.get(1), "pos");
        return a.getX() <= b.getX() ? new BlockPos[]{a, b} : new BlockPos[]{b, a};
    }

    private static void start(GameTestHelper h, BlockEntity builder) {
        ServerPlayer p = ArmoryServiceGameTests.player(h);
        try {
            call(builder, "action", p, 6, 0);   // A_START
        } finally {
            p.discard();
        }
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void blocksInTheWayAreTakenOut(GameTestHelper h) throws Exception {
        h.setBlock(AT, block("producer/block_blueprint_builder"));
        BlockEntity builder = h.getBlockEntity(AT);
        BlockPos[] at = loaded(h, builder);
        h.getLevel().setBlock(at[0], Blocks.DIRT.defaultBlockState(), 3);    // in the way: taken out
        h.getLevel().setBlock(at[1], Blocks.CHEST.defaultBlockState(), 3);   // a block entity: stays
        start(h, builder);
        h.succeedWhen(() -> {
            h.assertTrue((int) call(builder, "status") == 4, "the builder is not done: status " + call(builder, "status"));
            h.assertTrue(h.getLevel().getBlockState(at[0]).is(Blocks.STONE), "the dirt was not replaced: " + h.getLevel().getBlockState(at[0]));
            h.assertTrue(h.getLevel().getBlockState(at[1]).is(Blocks.CHEST), "the chest went: " + h.getLevel().getBlockState(at[1]));
            h.assertTrue((int) ArmoryServiceGameTests.field(builder, "cleared") == 1, "cleared " + ArmoryServiceGameTests.field(builder, "cleared"));
            h.assertTrue((int) ArmoryServiceGameTests.field(builder, "obstructed") >= 1, "the chest is not counted as obstructed");
        });
    }

    /** A block broken after it was built (before the check) is built again; a start after the build checks it again. */
    @GameTest(template = "empty", timeoutTicks = 600)
    public static void theCheckBuildsWhatWentMissing(GameTestHelper h) throws Exception {
        h.setBlock(AT, block("producer/block_blueprint_builder"));
        BlockEntity builder = h.getBlockEntity(AT);
        BlockPos[] at = loaded(h, builder);
        start(h, builder);
        h.startSequence()
            .thenWaitUntil(() -> h.assertTrue(h.getLevel().getBlockState(at[0]).is(Blocks.STONE) && h.getLevel().getBlockState(at[1]).is(Blocks.STONE),
                "the stone is not built yet"))
            .thenExecute(() -> {
                h.assertTrue((int) call(builder, "status") == 2, "the builder stopped before its check: status " + call(builder, "status"));
                h.getLevel().setBlock(at[0], Blocks.AIR.defaultBlockState(), 3);   // broken before the check
            })
            .thenWaitUntil(() -> {
                h.assertTrue((int) call(builder, "status") == 4, "the builder is not done: status " + call(builder, "status"));
                h.assertTrue(h.getLevel().getBlockState(at[0]).is(Blocks.STONE), "the broken block was not built again");
                h.assertTrue((int) ArmoryServiceGameTests.field(builder, "checkPasses") >= 2, "checks " + ArmoryServiceGameTests.field(builder, "checkPasses"));
            })
            .thenExecute(() -> {
                h.getLevel().setBlock(at[1], Blocks.AIR.defaultBlockState(), 3);   // broken after the build
                start(h, builder);
            })
            .thenWaitUntil(() -> {
                h.assertTrue((int) call(builder, "status") == 4, "the builder is not done again: status " + call(builder, "status"));
                h.assertTrue(h.getLevel().getBlockState(at[1]).is(Blocks.STONE), "a start after the build did not fill the gap");
            })
            .thenSucceed();
    }
}
