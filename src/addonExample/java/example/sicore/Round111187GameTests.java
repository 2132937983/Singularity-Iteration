package example.sicore;

import static example.sicore.Round33GameTests.call;

import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.187: the Infinite Armory Material Interface gives every material without end (nothing used, no screen, no item
 * handler) and a Blueprint Builder builds from it alone; a blueprint has no block limit (a litematic of 1,050,600 blocks,
 * over the old 1,048,576, is read whole) and the hologram's parts keep positions thousands of blocks apart (the old packing
 * stopped at 1,024 a side). Classes by name.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111187GameTests {
    private Round111187GameTests() {}

    private static final BlockPos AT = new BlockPos(1, 2, 1);

    private static Block block(String id) { return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", id)); }

    private static void delete(Path dir) {
        try {
            if (Files.isDirectory(dir)) try (Stream<Path> files = Files.walk(dir)) {
                files.sorted(Comparator.reverseOrder()).forEach(f -> f.toFile().delete());
            }
        } catch (java.io.IOException ignored) { }
    }

    @GameTest(template = "empty")
    public static void infiniteInterfaceGivesWithoutEnd(GameTestHelper h) {
        BlockPos at = new BlockPos(2, 2, 2);
        h.setBlock(at, block("producer/block_infinite_material_interface"));
        BlockEntity be = h.getBlockEntity(at);
        h.assertTrue(be != null && Round33GameTests.common("builder.MaterialSource").isInstance(be), "the infinite interface is no material source: " + be);
        h.assertTrue((long) call(be, "count", Items.STONE) == Long.MAX_VALUE, "it does not have every material: " + call(be, "count", Items.STONE));
        h.assertTrue((long) call(be, "count", Items.AIR) == 0, "it has air");
        for (int k = 0; k < 3; k++) {
            ItemStack got = (ItemStack) call(be, "extract", Items.OAK_PLANKS, 12);
            h.assertTrue(got.is(Items.OAK_PLANKS) && got.getCount() == 12 && got.isComponentsPatchEmpty(), "take " + k + " gave " + got);
        }
        h.assertTrue(((ItemStack) call(be, "insert", new ItemStack(Items.STONE, 5))).isEmpty(), "it hands material back");
        BlockPos abs = h.absolutePos(at);
        for (Direction d : Direction.values())
            h.assertTrue(h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, abs, d) == null, "it has an item handler on " + d);
        ServerPlayer p = ArmoryServiceGameTests.player(h);
        try {
            h.getLevel().getBlockState(abs).useWithoutItem(h.getLevel(), p, new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false));
            h.assertTrue(p.containerMenu == p.inventoryMenu, "a click opened " + p.containerMenu);
        } finally {
            p.discard();
        }
        h.succeed();
    }

    /** A builder with only an infinite interface near builds its blueprint and nothing is left over to bring back. */
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void builderBuildsFromTheInfiniteInterface(GameTestHelper h) throws Exception {
        h.setBlock(AT, block("producer/block_blueprint_builder"));
        h.setBlock(new BlockPos(1, 2, 3), block("producer/block_infinite_material_interface"));
        BlockEntity builder = h.getBlockEntity(AT);
        ServerPlayer p = ArmoryServiceGameTests.player(h);
        Class<?> up = Round33GameTests.common("builder.BlueprintUploads");
        Path dir = (Path) call(up, "folder", p.getUUID());
        List<?> plan;
        try {
            byte[] file = Round111182GameTests.structure();
            h.assertTrue((int) call(up, "begin", p, builder, "infinite.nbt", file.length, call(up, "sha256", file)) == 1, "the upload is not accepted");
            h.assertTrue((int) call(up, "chunk", p, 0, file) == 2, "the upload is not done");
            plan = (List<?>) call(builder, "plan");
            h.assertTrue(plan != null && plan.size() == 2, "the plan is " + plan);
            call(builder, "apiSetEnergy", 200_000L);
            call(builder, "action", p, 6, 0);   // A_START
            h.assertTrue((int) call(builder, "status") == 2, "the builder does not run: status " + call(builder, "status"));
        } finally {
            delete(dir);
            p.discard();
        }
        h.succeedWhen(() -> {
            for (Object e : plan) {
                BlockPos pos = (BlockPos) call(e, "pos");
                h.assertTrue(h.getLevel().getBlockState(pos).is(Blocks.STONE), "nothing built at " + pos + " yet");
            }
            h.assertTrue((int) call(builder, "status") == 4, "the builder is not done: status " + call(builder, "status"));
        });
    }

    /** 1,030 x 1 x 1,020 stone (1,050,600 blocks, the old limit was 1,048,576) is read whole, 1,030 blocks across. */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void aBlueprintHasNoBlockLimit(GameTestHelper h) {
        int sx = 1030, sz = 1020;
        CompoundTag region = new CompoundTag();
        CompoundTag position = new CompoundTag();
        position.putInt("x", 0); position.putInt("y", 0); position.putInt("z", 0);
        CompoundTag size = new CompoundTag();
        size.putInt("x", sx); size.putInt("y", 1); size.putInt("z", sz);
        region.put("Position", position);
        region.put("Size", size);
        ListTag palette = new ListTag();
        for (String name : new String[]{"minecraft:air", "minecraft:stone"}) {
            CompoundTag state = new CompoundTag();
            state.putString("Name", name);
            palette.add(state);
        }
        region.put("BlockStatePalette", palette);
        long volume = (long) sx * sz;
        long[] data = new long[(int) ((volume * 2 + 63) / 64)];
        Arrays.fill(data, 0x5555555555555555L);   // 2 bits each, every one 1 (stone)
        region.putLongArray("BlockStates", data);
        CompoundTag regions = new CompoundTag();
        regions.put("Main", region);
        CompoundTag root = new CompoundTag();
        root.put("Regions", regions);
        Object bp = call(Round33GameTests.common("builder.BlueprintReader"), "litematic", root, "wide.litematic", h.getLevel().holderLookup(Registries.BLOCK));
        List<?> entries = (List<?>) call(bp, "entries");
        h.assertTrue(entries.size() == sx * sz, "the blueprint holds " + entries.size() + " blocks of " + sx * sz);
        h.assertTrue(call(bp, "size").equals(new Vec3i(sx, 1, sz)), "the blueprint is " + call(bp, "size"));
        h.assertTrue((boolean) call(Round33GameTests.common("builder.Blueprint"), "affordable", 1L << 20), "a megabyte does not fit in the memory");
        h.succeed();
    }

    /** A plan 13,000 blocks across in two parts: every block comes out where it is, with its state. */
    @GameTest(template = "empty")
    public static void hologramPartsKeepFarPositions(GameTestHelper h) throws Exception {
        Constructor<?> entry = Round33GameTests.common("builder.Blueprint$Entry").getDeclaredConstructor(BlockPos.class, BlockState.class);
        List<Object> plan = new ArrayList<>();
        plan.add(entry.newInstance(new BlockPos(-1500, 70, 4000), Blocks.STONE.defaultBlockState()));
        plan.add(entry.newInstance(new BlockPos(3000, -40, -2000), Blocks.GLASS.defaultBlockState()));
        for (int x = 0; x < 10_000; x++) plan.add(entry.newInstance(new BlockPos(x, 64, x % 7), (x % 2 == 0 ? Blocks.OAK_PLANKS : Blocks.BRICKS).defaultBlockState()));
        Class<?> pp = Round33GameTests.common("builder.BuilderPlanPackets");
        BlockPos origin = (BlockPos) call(pp, "origin", null, plan);
        h.assertTrue(origin.equals(new BlockPos(-1500, -40, -2000)), "the plan's corner is " + origin);
        List<?> parts = (List<?>) call(pp, "parts", BlockPos.ZERO, 7, new int[3], origin, plan);
        h.assertTrue(parts.size() == 2, "the plan comes in " + parts.size() + " parts");
        int i = 0;
        for (Object part : parts) {
            h.assertTrue((int) call(part, "count") == 2, "a part says " + call(part, "count") + " parts");
            int n = (int) call(part, "blocks");
            for (int k = 0; k < n; k++, i++) {
                BlockPos pos = (BlockPos) call(part, "pos", k);
                BlockState state = Block.stateById((int) call(part, "stateId", k));
                h.assertTrue(pos.equals(call(plan.get(i), "pos")) && state == call(plan.get(i), "state"),
                    "block " + i + " is " + pos + " " + state + ", the plan has " + call(plan.get(i), "pos") + " " + call(plan.get(i), "state"));
            }
        }
        h.assertTrue(i == plan.size(), "the parts hold " + i + " blocks of " + plan.size());
        h.succeed();
    }
}
