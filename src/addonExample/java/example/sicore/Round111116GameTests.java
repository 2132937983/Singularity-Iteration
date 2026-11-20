package example.sicore;

import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractProcessingMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

/**
 * 0.1.11.116 (processing overhaul M9): the extractor runs the recipe's own time and power (gravel 150 t × 12 EU/t; resin,
 * which writes none, the stock 300 t × 2 EU/t); the recycler counts its yield instead of rolling it (twelve items give
 * exactly one scrap and leave half of the next); the canner reports why it stands. (The mod's common classes by
 * reflection: addon examples do not compile against them.)
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111116GameTests {
    private Round111116GameTests() {}

    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    private static AbstractProcessingMachineBlockEntity place(GameTestHelper h, BlockPos at, String id) {
        h.setBlock(at, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", id)));
        return (AbstractProcessingMachineBlockEntity) h.getBlockEntity(at);
    }

    private static void keepCharged(GameTestHelper h, AbstractProcessingMachineBlockEntity m) {
        h.onEachTick(() -> m.getEnergyStorageInternal().setStored(m.getEnergyStorageInternal().getCapacity()));
    }

    private static ItemStack item(String id, int n) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", id)), n);
    }

    /** Gravel to flint: 150 t at 12 EU/t, one flint after about 152 ticks; resin (no values written): 300 t at 2 EU/t. */
    @GameTest(template = "empty", timeoutTicks = 260)
    public static void extractorRunsTheRecipe(GameTestHelper h) {
        var gravel = place(h, new BlockPos(1, 2, 1), "producer/block_extractor_elc");
        var resin = place(h, new BlockPos(3, 2, 1), "producer/block_extractor_elc");
        IItemHandlerModifiable items = (IItemHandlerModifiable) gravel.getItemHandler();
        items.setStackInSlot(0, new ItemStack(Items.GRAVEL, 4));
        ((IItemHandlerModifiable) resin.getItemHandler()).setStackInSlot(0, item("resource/item_harz", 4));
        keepCharged(h, gravel);
        keepCharged(h, resin);
        h.runAfterDelay(3, () -> {
            h.assertTrue(gravel.getTicksPerOperation() == 150 && gravel.getEffectiveEnergyPerTick() == 12,
                "gravel: " + gravel.getTicksPerOperation() + " t at " + gravel.getEffectiveEnergyPerTick() + " EU/t");
            h.assertTrue(resin.getTicksPerOperation() == 300 && resin.getEffectiveEnergyPerTick() == 2,
                "resin: " + resin.getTicksPerOperation() + " t at " + resin.getEffectiveEnergyPerTick() + " EU/t");
            h.runAfterDelay(155, () -> {
                int flint = items.getStackInSlot(1).getCount();
                h.assertTrue(items.getStackInSlot(1).is(Items.FLINT) && flint == 1, "flint after 158 ticks: " + items.getStackInSlot(1));
                h.succeed();
            });
        });
    }

    /** Twelve cobblestone through the recycler (4 overclockers): exactly one scrap, and half of the next one carried. */
    @GameTest(template = "empty", timeoutTicks = 300)
    public static void recyclerCountsTheYield(GameTestHelper h) {
        var m = place(h, new BlockPos(1, 2, 1), "producer/block_recycler_elc");
        IItemHandlerModifiable items = (IItemHandlerModifiable) m.getItemHandler();
        items.setStackInSlot(m.getUpgradeSlotStart(), item("upgrade/overclocker_upgrade", 4));
        items.setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 12));
        keepCharged(h, m);
        h.runAfterDelay(250, () -> {
            h.assertTrue(items.getStackInSlot(0).isEmpty(), "cobblestone left: " + items.getStackInSlot(0));
            ItemStack out = items.getStackInSlot(2);
            h.assertTrue(out.getCount() == 1, "scrap from 12 items: " + out + " (counted: exactly 1)");
            h.assertTrue(Math.abs((double) call(m, "yieldCarry") - 0.5) < 1e-9, "carry " + call(m, "yieldCarry"));
            h.succeed();
        });
    }

    /** An empty canner says it has no input; one with a can but no power says so. */
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void cannerReportsItsStatus(GameTestHelper h) {
        var empty = place(h, new BlockPos(1, 2, 1), "producer/block_canner_elc");
        var unpowered = place(h, new BlockPos(3, 2, 1), "producer/block_canner_elc");
        ((IItemHandlerModifiable) unpowered.getItemHandler()).setStackInSlot(0, item("normal/item_tin_empty_can", 4));
        h.runAfterDelay(3, () -> {
            h.assertTrue((int) call(empty, "processStatus") == 1, "empty canner: " + call(empty, "processStatus"));
            h.assertTrue((int) call(unpowered, "processStatus") == 4, "canner without power: " + call(unpowered, "processStatus"));
            h.succeed();
        });
    }
}
