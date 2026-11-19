package example.sicore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * 0.1.11.77: the diesel generator has a fluid capability (diesel only, every side), so an oil refinery with a fluid
 * ejector upgrade fills it. The oil refinery's cell slots have the types of what they do: Shift-click sends empty cells
 * to the top-right slot that fills them and never to the bottom-left one; automation puts empty cells into the
 * top-right slot and takes the emptied container out of the bottom-left one; an item ejector pushes that container out
 * and leaves the waiting empty cells.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11577GameTests {
    private Round11577GameTests() {}

    /** Refinery slots: input cell, emptied container, empty cells to fill, filled cells, battery, upgrades 5..8. */
    private static final int INPUT = 0, EMPTIED = 1, TO_FILL = 2, FILLED = 3, UPGRADE = 5;

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }
    private static ItemStack item(String path, int count) { return new ItemStack(BuiltInRegistries.ITEM.get(id(path)), count); }
    private static FluidStack diesel(int amount) { return new FluidStack(BuiltInRegistries.FLUID.get(id("dieseloil")), amount); }
    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    private static BlockEntity place(GameTestHelper h, BlockPos at, String block) {
        h.setBlock(at, BuiltInRegistries.BLOCK.get(id(block)));
        return h.getBlockEntity(at);
    }

    private static IItemHandler slots(BlockEntity refinery) { return (IItemHandler) call(refinery, "getItemHandler"); }

    @GameTest(template = "empty", batch = "round11577_diesel", timeoutTicks = 20)
    public static void dieselGeneratorHasAFluidCapability(GameTestHelper h) {
        BlockPos at = new BlockPos(1, 2, 1);
        BlockEntity generator = place(h, at, "generator/block_diesel_generator");
        for (Direction side : Direction.values()) {
            IFluidHandler fluids = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(at), side);
            h.assertTrue(fluids != null, "no fluid capability on the " + side + " side");
        }
        IFluidHandler fluids = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(at), Direction.NORTH);
        h.assertTrue(fluids.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE) == 0, "the generator took water");
        h.assertTrue(fluids.fill(diesel(1000), IFluidHandler.FluidAction.EXECUTE) == 1000, "the generator did not take diesel");
        h.assertTrue((int) call(generator, "getFuelAmount") == 1000, "the diesel did not reach the fuel tank");
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round11577_ejector", timeoutTicks = 120)
    public static void refineryFluidEjectorFillsTheDieselGenerator(GameTestHelper h) {
        BlockPos at = new BlockPos(1, 2, 1);
        BlockEntity refinery = place(h, at, "producer/block_oil_refinery_elc");
        BlockEntity generator = place(h, at.east(), "generator/block_diesel_generator");
        slots(refinery).insertItem(UPGRADE, item("upgrade/fluid_ejector_upgrade", 1), false);
        IFluidHandler output = (IFluidHandler) call(refinery, "getOutputTank");
        h.assertTrue(output.fill(diesel(2000), IFluidHandler.FluidAction.EXECUTE) == 2000, "the refinery's output tank took no diesel");
        h.succeedWhen(() -> h.assertTrue((int) call(generator, "getFuelAmount") > 0,
            "the fluid ejector moved no diesel into the generator (refinery has " + output.getFluidInTank(0).getAmount() + " mB)"));
    }

    @GameTest(template = "empty", batch = "round11577_shift", timeoutTicks = 20)
    public static void refineryShiftClickSendsCellsWhereTheyWork(GameTestHelper h) {
        BlockEntity refinery = place(h, new BlockPos(1, 2, 1), "producer/block_oil_refinery_elc");
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.getInventory().add(item("cell/item_cell_empty", 4));
        player.getInventory().add(item("crudeoil_bucket", 1));
        AbstractContainerMenu menu = ((MenuProvider) refinery).createMenu(1, player.getInventory(), player);
        h.assertTrue(!menu.getSlot(EMPTIED).mayPlace(item("cell/item_cell_empty", 1)), "the bottom-left slot takes empty cells");
        for (int i = menu.slots.size() - 1; i > FILLED; i--) {
            if (menu.getSlot(i).container == player.getInventory() && !menu.getSlot(i).getItem().isEmpty()) menu.quickMoveStack(player, i);
        }
        var inside = slots(refinery);
        h.assertTrue(inside.getStackInSlot(TO_FILL).getCount() == 4 && inside.getStackInSlot(EMPTIED).isEmpty(),
            "empty cells: top-right " + inside.getStackInSlot(TO_FILL) + ", bottom-left " + inside.getStackInSlot(EMPTIED));
        h.assertTrue(inside.getStackInSlot(INPUT).is(BuiltInRegistries.ITEM.get(id("crudeoil_bucket"))), "the crude oil bucket is not in the input slot");
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round11577_automation", timeoutTicks = 20)
    public static void refineryAutomationUsesTheRightCellSlots(GameTestHelper h) {
        BlockPos at = new BlockPos(1, 2, 1);
        BlockEntity refinery = place(h, at, "producer/block_oil_refinery_elc");
        IItemHandler side = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(at), Direction.UP);
        h.assertTrue(side != null, "no item capability");
        // a hopper offers its stack to each slot in turn
        ItemStack rest = item("cell/item_cell_empty", 8);
        for (int i = 0; i < side.getSlots() && !rest.isEmpty(); i++) rest = side.insertItem(i, rest, false);
        var inside = slots(refinery);
        h.assertTrue(rest.isEmpty() && inside.getStackInSlot(TO_FILL).getCount() == 8 && inside.getStackInSlot(EMPTIED).isEmpty(),
            "automation put empty cells into " + (inside.getStackInSlot(EMPTIED).isEmpty() ? "nowhere useful" : "the bottom-left slot"));
        // the emptied container comes out; the cells waiting to be filled stay
        ((net.neoforged.neoforge.items.IItemHandlerModifiable) inside).setStackInSlot(EMPTIED, new ItemStack(net.minecraft.world.item.Items.BUCKET));
        h.assertTrue(!side.extractItem(EMPTIED, 1, false).isEmpty(), "automation cannot take the emptied container out");
        h.assertTrue(side.extractItem(TO_FILL, 8, false).isEmpty(), "automation took the cells waiting to be filled");
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round11577_item_ejector", timeoutTicks = 120)
    public static void refineryItemEjectorPushesTheEmptiedContainer(GameTestHelper h) {
        BlockPos at = new BlockPos(1, 2, 1);
        BlockEntity refinery = place(h, at, "producer/block_oil_refinery_elc");
        h.setBlock(at.east(), Blocks.CHEST);
        var inside = slots(refinery);
        inside.insertItem(UPGRADE, item("upgrade/ejector_upgrade", 1), false);
        inside.insertItem(TO_FILL, item("cell/item_cell_empty", 3), false);
        ((net.neoforged.neoforge.items.IItemHandlerModifiable) inside).setStackInSlot(EMPTIED, new ItemStack(net.minecraft.world.item.Items.BUCKET));
        h.succeedWhen(() -> {
            h.assertTrue(inside.getStackInSlot(EMPTIED).isEmpty(), "the ejector left the emptied bucket in the refinery");
            h.assertTrue(inside.getStackInSlot(TO_FILL).getCount() == 3, "the ejector pushed out the cells waiting to be filled");
            h.assertContainerContains(at.east(), net.minecraft.world.item.Items.BUCKET);
        });
    }
}
