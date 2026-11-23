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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.184: the Blueprint Builder's plan for the clients' block hologram: a builder without a plan sends one empty part;
 * a loaded plan raises the plan version and comes out in parts whose blocks (position relative to the plan's lowest corner,
 * block state id) are exactly the plan's. (Classes by name; the plan comes from an upload as in Round111182GameTests.)
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111184GameTests {
    private Round111184GameTests() {}

    private static final BlockPos AT = new BlockPos(1, 2, 1);

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void builderPlanPartsMatchThePlan(GameTestHelper h) throws Exception {
        h.setBlock(AT, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "producer/block_blueprint_builder")));
        BlockEntity builder = h.getBlockEntity(AT);
        ServerPlayer p = ArmoryServiceGameTests.player(h);
        Class<?> up = Round33GameTests.common("builder.BlueprintUploads"), pp = Round33GameTests.common("builder.BuilderPlanPackets");
        Path dir = (Path) call(up, "folder", p.getUUID());
        try {
            List<?> none = (List<?>) call(pp, "parts", builder);
            h.assertTrue(none.size() == 1 && (int) call(none.get(0), "count") == 0, "a builder without a plan sends " + none.size() + " parts");
            int before = (int) call(builder, "planVersion");
            byte[] file = Round111182GameTests.structure();
            byte[] sha = (byte[]) call(up, "sha256", file);
            h.assertTrue((int) call(up, "begin", p, builder, "plan parts.nbt", file.length, sha) == 1, "the upload is not accepted");
            h.assertTrue((int) call(up, "chunk", p, 0, file) == 2, "the upload is not done");
            h.assertTrue((int) call(builder, "planVersion") != before, "the plan version stayed " + before);
            List<?> plan = (List<?>) call(builder, "plan");
            List<?> parts = (List<?>) call(pp, "parts", builder);
            h.assertTrue(parts.size() == 1 && (int) call(parts.get(0), "count") == 1, "the plan comes in " + parts.size() + " parts");
            Object part = parts.get(0);
            h.assertTrue((int) call(part, "version") == (int) call(builder, "planVersion"), "the part carries another version");
            int blocks = (int) call(part, "blocks");   // 0.1.11.187: x, y, z and the state id a block (was a 10-bit packing)
            h.assertTrue(blocks == plan.size(), "the part holds " + blocks + " blocks, the plan " + plan.size());
            for (int i = 0; i < plan.size(); i++) {
                BlockPos pos = (BlockPos) call(part, "pos", i);
                BlockState state = Block.stateById((int) call(part, "stateId", i));
                h.assertTrue(pos.equals(call(plan.get(i), "pos")) && state == call(plan.get(i), "state"),
                    "block " + i + " is " + pos + " " + state + ", the plan has " + call(plan.get(i), "pos") + " " + call(plan.get(i), "state"));
            }
        } finally {
            if (Files.isDirectory(dir)) try (Stream<Path> files = Files.walk(dir)) {
                files.sorted(Comparator.reverseOrder()).forEach(f -> f.toFile().delete());
            }
            p.discard();
        }
        h.succeed();
    }

    /**
     * 0.1.11.186: the range limit went: a blueprint moved 100 blocks away (the old limit was 64) is ready to build, not "too
     * far", and its blocks are where the offset puts them.
     */
    @GameTest(template = "empty", timeoutTicks = 60)
    public static void farPlanIsReady(GameTestHelper h) throws Exception {
        h.setBlock(AT, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "producer/block_blueprint_builder")));
        BlockEntity builder = h.getBlockEntity(AT);
        ServerPlayer p = ArmoryServiceGameTests.player(h);
        Class<?> up = Round33GameTests.common("builder.BlueprintUploads");
        Path dir = (Path) call(up, "folder", p.getUUID());
        byte[] file = Round111182GameTests.structure();
        h.assertTrue((int) call(up, "begin", p, builder, "far plan.nbt", file.length, call(up, "sha256", file)) == 1, "the upload is not accepted");
        h.assertTrue((int) call(up, "chunk", p, 0, file) == 2, "the upload is not done");
        for (int k = 0; k < 100; k++) call(builder, "action", p, 5, 1);   // A_OFFSET, x + 1
        h.succeedWhen(() -> {
            h.assertTrue((int) call(builder, "status") == 1, "the far plan is not ready: status " + call(builder, "status"));
            int[] offset = (int[]) call(builder, "planOffset");
            h.assertTrue(offset[0] == 101, "the plan is placed at offset " + offset[0]);
            List<?> plan = (List<?>) call(builder, "plan");
            BlockPos first = (BlockPos) call(plan.get(0), "pos");
            h.assertTrue(first.getX() - builder.getBlockPos().getX() >= 101, "the first block is at " + first + ", the builder at " + builder.getBlockPos());
            try {
                if (Files.isDirectory(dir)) try (Stream<Path> files = Files.walk(dir)) {
                    files.sorted(Comparator.reverseOrder()).forEach(f -> f.toFile().delete());
                }
            } catch (java.io.IOException ignored) { }
            p.discard();
        });
    }
}
