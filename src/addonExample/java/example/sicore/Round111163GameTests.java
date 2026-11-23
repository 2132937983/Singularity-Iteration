package example.sicore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * 0.1.11.163: the item buffer's faces follow IC2. The top and bottom reach all 48 content slots; a side puts items into the
 * right half only and takes from the right half, or from the left half once the right half is empty. Before, the top and
 * bottom reached only the left half and the sides only the right half, so the two halves never met: what a pipe put in through
 * a side never came out of a hopper under the buffer.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111163GameTests {
    private Round111163GameTests() {}

    private static final BlockPos BUFFER = new BlockPos(1, 2, 1);

    private static void placeBuffer(GameTestHelper h) {
        h.setBlock(BUFFER, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "producer/block_item_buffer_elc")));
    }

    private static IItemHandler face(GameTestHelper h, Direction face) {
        IItemHandler handler = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(BUFFER), face);
        if (handler == null) throw new net.minecraft.gametest.framework.GameTestAssertException("no item handler on the " + face + " face");
        return handler;
    }

    private static ItemStack insert(IItemHandler handler, ItemStack stack) {
        ItemStack rest = stack;
        for (int i = 0; i < handler.getSlots() && !rest.isEmpty(); i++) rest = handler.insertItem(i, rest, false);
        return rest;
    }

    private static int extract(IItemHandler handler, int wanted) {
        int got = 0;
        for (int i = 0; i < handler.getSlots() && got < wanted; i++) got += handler.extractItem(i, wanted - got, false).getCount();
        return got;
    }

    /** Items piped in through a side come out of a hopper under the buffer. */
    @GameTest(template = "empty", batch = "round111163_buffer", timeoutTicks = 200)
    public static void sideInputLeavesThroughAHopperBelow(GameTestHelper h) {
        placeBuffer(h);
        h.setBlock(BUFFER.below(), Blocks.HOPPER);
        ItemStack rest = insert(face(h, Direction.EAST), new ItemStack(Items.COBBLESTONE, 5));
        h.assertTrue(rest.isEmpty(), "the side refused " + rest.getCount() + " cobblestone");
        h.succeedWhen(() -> {
            HopperBlockEntity hopper = (HopperBlockEntity) h.getBlockEntity(BUFFER.below());
            int inHopper = 0;
            for (int i = 0; i < hopper.getContainerSize(); i++) inHopper += hopper.getItem(i).getCount();
            h.assertTrue(inHopper == 5, "the hopper under the buffer holds " + inHopper + " of 5 cobblestone");
        });
    }

    /** Items put in from the top can be taken out through a side once the right half is empty. */
    @GameTest(template = "empty", batch = "round111163_buffer")
    public static void topInputLeavesThroughASide(GameTestHelper h) {
        placeBuffer(h);
        h.assertTrue(insert(face(h, Direction.UP), new ItemStack(Items.IRON_INGOT, 7)).isEmpty(), "the top refused iron");
        int got = extract(face(h, Direction.WEST), 64);
        h.assertTrue(got == 7, "the west side gave " + got + " of 7 iron ingots put in from the top");
        h.succeed();
    }

    /** A side takes from its own (right) half first: the left half only once the right half is empty. */
    @GameTest(template = "empty", batch = "round111163_buffer")
    public static void aSideEmptiesItsOwnHalfFirst(GameTestHelper h) {
        placeBuffer(h);
        IItemHandler side = face(h, Direction.NORTH), top = face(h, Direction.UP);
        h.assertTrue(insert(top, new ItemStack(Items.GOLD_INGOT, 3)).isEmpty(), "the top refused gold");   // left half (slot 0)
        h.assertTrue(insert(side, new ItemStack(Items.COPPER_INGOT, 4)).isEmpty(), "the side refused copper");   // right half
        ItemStack first = ItemStack.EMPTY;
        for (int i = 0; i < side.getSlots() && first.isEmpty(); i++) first = side.extractItem(i, 64, false);
        h.assertTrue(first.is(Items.COPPER_INGOT) && first.getCount() == 4, "the side gave " + first + " first, not the 4 copper of its own half");
        ItemStack second = ItemStack.EMPTY;
        for (int i = 0; i < side.getSlots() && second.isEmpty(); i++) second = side.extractItem(i, 64, false);
        h.assertTrue(second.is(Items.GOLD_INGOT) && second.getCount() == 3, "with its half empty the side gave " + second + ", not the 3 gold");
        h.succeed();
    }

    /** A side never fills the left half: with the right half full it refuses, and the top still takes 24 more stacks. */
    @GameTest(template = "empty", batch = "round111163_buffer")
    public static void aSideFillsOnlyTheRightHalf(GameTestHelper h) {
        placeBuffer(h);
        IItemHandler side = face(h, Direction.SOUTH), top = face(h, Direction.UP);
        int refused = 0;
        for (int i = 0; i < 25; i++) refused += insert(side, new ItemStack(Items.STONE, 64)).getCount();
        h.assertTrue(refused == 64, "the side took " + (25 * 64 - refused) + " stone (the right half holds 24 stacks)");
        int topRefused = 0;
        for (int i = 0; i < 25; i++) topRefused += insert(top, new ItemStack(Items.DIRT, 64)).getCount();
        h.assertTrue(topRefused == 64, "the top took " + (25 * 64 - topRefused) + " dirt into the left half (24 stacks free)");
        h.succeed();
    }
}
