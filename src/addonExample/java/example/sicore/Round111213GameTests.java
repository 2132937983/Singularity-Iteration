package example.sicore;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * GameTests of 0.1.11.213 (MT1 X1): what the molecular transformer tells the client for its chamber renderer (input, result,
 * process kind, working, the EU paid / needed / drawn and the time), the client's progress run-on from it, the
 * continuous-stream threshold, and the item atoms (texture pixels on a 16x16 raster).
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111213GameTests {
    private Round111213GameTests() {}

    private static final String ATOMS = "com.miophas.singularity_iteration" + ".common.client.molecular.MolecularAtoms";

    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    private static Class<?> atoms() {
        try {
            return Class.forName(ATOMS);
        } catch (ClassNotFoundException e) {
            throw new AssertionError(ATOMS, e);
        }
    }

    private static ItemStack item(String path) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", path)));
    }

    private static BlockEntity place(GameTestHelper h, BlockPos pos) {
        h.setBlock(pos, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "producer/block_molecular_transformer")));
        return h.getBlockEntity(pos);
    }

    /** Texture pixels become atoms: transparent ones are left out, big textures are sampled down, small ones centred. */
    @GameTest(template = "empty", batch = "round111213_mt_view", timeoutTicks = 20)
    public static void texturePixelsBecomeAtoms(GameTestHelper h) {
        int[] px = new int[16 * 16];
        for (int j = 4; j < 12; j++) for (int i = 2; i < 14; i++) px[j * 16 + i] = 0xFF336699;
        px[0] = 0x40FFFFFF;   // faint: left out
        List<?> a = (List<?>) call(atoms(), "sample", px, 16, 16);
        h.assertTrue(a.size() == 8 * 12, "8x12 opaque pixels give 96 atoms, got " + a.size());
        Object first = a.get(0);
        h.assertTrue((int) call(first, "i") == 2 && (int) call(first, "j") == 4 && (int) call(first, "rgb") == 0x336699, "first atom at (2,4)");

        int[] big = new int[32 * 32];
        java.util.Arrays.fill(big, 0xFF000001);
        h.assertTrue(((List<?>) call(atoms(), "sample", big, 32, 32)).size() == 256, "a 32x32 texture is sampled to 16x16");

        int[] anim = new int[16 * 48];
        for (int i = 0; i < 16 * 16; i++) anim[i] = 0xFF010101;
        for (int i = 16 * 16; i < anim.length; i++) anim[i] = 0xFF020202;
        List<?> frame = (List<?>) call(atoms(), "sample", anim, 16, 48);
        h.assertTrue(frame.size() == 256 && (int) call(frame.get(255), "rgb") == 0x010101, "an animated strip uses its first frame");

        int[] small = new int[8 * 8];
        java.util.Arrays.fill(small, 0xFF0A0B0C);
        List<?> centred = (List<?>) call(atoms(), "sample", small, 8, 8);
        h.assertTrue(centred.size() == 64 && (int) call(centred.get(0), "i") == 4 && (int) call(centred.get(0), "j") == 4, "an 8x8 texture is centred");

        List<?> thin = (List<?>) call(atoms(), "thin", a, 24);
        h.assertTrue(thin.size() == 48, "Fast graphics keeps half the atoms, got " + thin.size());
        h.assertTrue(((List<?>) call(atoms(), "thin", thin.subList(0, 20), 24)).size() == 20, "a small item is kept whole");
        h.assertTrue(!((List<?>) call(atoms(), "fallback")).isEmpty(), "a stand-in exists for unreadable textures");
        h.succeed();
    }

    /** Iron to iridium at 100,000 EU a tick: one item takes 90 ticks, so the chamber shows it item by item. */
    @GameTest(template = "empty", batch = "round111213_mt_view", timeoutTicks = 60)
    public static void workingMachineSendsItsChamberView(GameTestHelper h) {
        BlockPos pos = new BlockPos(1, 2, 1);
        BlockEntity mt = place(h, pos);
        IItemHandler items = (IItemHandler) call(mt, "getItemHandler");
        h.assertTrue(items.insertItem(0, new ItemStack(Items.IRON_INGOT, 4), false).isEmpty(), "iron goes in");
        h.onEachTick(() -> { var be = h.getLevel().getBlockEntity(h.absolutePos(pos)); if (be != null) call(be, "apiSetEnergy", 100_000L); });
        h.runAfterDelay(30, () -> {
            BlockEntity be = h.getBlockEntity(pos);
            h.assertTrue((boolean) call(be, "viewWorking"), "the view says working");
            h.assertTrue(((ItemStack) call(be, "viewInput")).is(Items.IRON_INGOT), "the view input is the iron");
            h.assertTrue(((ItemStack) call(be, "viewResult")).is(item("resource/item_iridium").getItem()), "the view result is iridium");
            h.assertTrue("nuclear".equals(call(be, "viewKind")), "the view kind is nuclear");
            double tpi = (double) call(be, "viewTicksPerItem");
            h.assertTrue(tpi > 60 && tpi < 130, "about 90 ticks an item, got " + tpi);
            h.assertTrue(!(boolean) call(be, "viewStream"), "90 ticks an item is item by item");
            double t = h.getLevel().getGameTime();
            double p0 = (double) call(be, "viewProgress", t), p1 = (double) call(be, "viewProgress", t + 9.0);
            h.assertTrue(p0 >= 0 && p0 < 1 && p1 >= 0 && p1 < 1, "progress stays in 0..1");
            double step = (p1 - p0 + 1) % 1;
            h.assertTrue(Math.abs(step - 9.0 / tpi) < 0.02, "progress runs on at the synced draw, step " + step);

            CompoundTag view = ((CompoundTag) call(be, "getUpdateTag", h.getLevel().registryAccess())).getCompound("mt_view");
            h.assertTrue(view.getBoolean("work") && "nuclear".equals(view.getString("kind")) && view.getLong("cost") == 9_000_000L,
                "the update tag carries the view");
            h.assertTrue(view.contains("in") && view.contains("out"), "the update tag carries input and result");
            // a fresh client copy gets the same view from the tag
            BlockEntity copy = (BlockEntity) call(be.getType(), "create", be.getBlockPos(), be.getBlockState());
            copy.setLevel(h.getLevel());
            call(copy, "handleUpdateTag", call(be, "getUpdateTag", h.getLevel().registryAccess()), h.getLevel().registryAccess());
            h.assertTrue((boolean) call(copy, "viewWorking") && ((ItemStack) call(copy, "viewInput")).is(Items.IRON_INGOT)
                && "nuclear".equals(call(copy, "viewKind")), "the client copy reads the view back");
            h.assertTrue(call(be, "getUpdatePacket") != null, "the machine sends block entity data packets");

            items.extractItem(0, 64, false);
            h.runAfterDelay(3, () -> {
                BlockEntity after = h.getBlockEntity(pos);
                h.assertTrue(((ItemStack) call(after, "viewInput")).isEmpty() && !(boolean) call(after, "viewWorking"),
                    "an emptied chamber shows nothing");
                h.setBlock(pos, Blocks.AIR);
                h.succeed();
            });
        });
    }

    /** Glowstone dust to sunnarium at 2,000,000 EU a tick: two items a tick, so the chamber shows a continuous stream. */
    @GameTest(template = "empty", batch = "round111213_mt_view", timeoutTicks = 40)
    public static void fastMachineShowsAStream(GameTestHelper h) {
        BlockPos pos = new BlockPos(1, 2, 1);
        BlockEntity mt = place(h, pos);
        IItemHandler items = (IItemHandler) call(mt, "getItemHandler");
        items.insertItem(0, new ItemStack(Items.GLOWSTONE_DUST, 64), false);
        h.onEachTick(() -> { var be = h.getLevel().getBlockEntity(h.absolutePos(pos)); if (be != null) call(be, "apiSetEnergy", 2_000_000L); });
        h.runAfterDelay(12, () -> {
            BlockEntity be = h.getBlockEntity(pos);
            h.assertTrue("exotic".equals(call(be, "viewKind")), "glowstone to sunnarium is exotic");
            h.assertTrue((boolean) call(be, "viewStream"), "under 40 ticks an item is a stream, got "
                + call(be, "viewTicksPerItem") + " ticks");
            h.setBlock(pos, Blocks.AIR);
            h.succeed();
        });
    }
}
