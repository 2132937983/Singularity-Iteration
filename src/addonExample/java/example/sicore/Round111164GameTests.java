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
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.entity.ComparatorBlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * 0.1.11.164: a comparator reads the item buffer: 0 when empty, 1 with anything inside, 8 with half its 48 content slots full,
 * 15 when all are full (the upgrade slots do not count), as a vanilla container.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111164GameTests {
    private Round111164GameTests() {}

    private static final BlockPos BUFFER = new BlockPos(1, 1, 2), COMPARATOR = new BlockPos(2, 1, 2);

    private static int output(GameTestHelper h) {
        return ((ComparatorBlockEntity) h.getBlockEntity(COMPARATOR)).getOutputSignal();
    }

    @GameTest(template = "empty", batch = "round111164_buffer", timeoutTicks = 200)
    public static void aComparatorReadsTheItemBuffer(GameTestHelper h) {
        h.setBlock(BUFFER, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "producer/block_item_buffer_elc")));
        // the comparator reads the block on its facing side (west: the buffer) and outputs east; it needs solid ground
        h.setBlock(COMPARATOR.below(), Blocks.STONE);
        h.setBlock(COMPARATOR,Blocks.COMPARATOR.defaultBlockState().setValue(ComparatorBlock.FACING, Direction.WEST));
        IItemHandler top = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(BUFFER), Direction.UP);
        h.assertTrue(top != null, "no item handler on the buffer");
        h.startSequence()
            .thenIdle(4)
            .thenExecute(() -> h.assertTrue(output(h) == 0, "an empty buffer gives " + output(h)))
            .thenExecute(() -> top.insertItem(0, new ItemStack(Items.STONE, 1), false))
            .thenWaitUntil(() -> h.assertTrue(output(h) == 1, "one stone gives " + output(h)))
            .thenExecute(() -> {
                top.extractItem(0, 1, false);
                for (int i = 0; i < 24; i++) top.insertItem(i, new ItemStack(Items.STONE, 64), false);
            })
            .thenWaitUntil(() -> h.assertTrue(output(h) == 8, "24 full stacks of 48 give " + output(h)))
            .thenExecute(() -> { for (int i = 24; i < 48; i++) top.insertItem(i, new ItemStack(Items.STONE, 64), false); })
            .thenWaitUntil(() -> h.assertTrue(output(h) == 15, "a full buffer gives " + output(h)))
            .thenSucceed();
    }
}
