package example.sicore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

/**
 * Player reports: the Nano Saber cuts cobwebs and its damage is an attribute (mobs, and maids of maid
 * mods, use it); the chainsaw and the drills show and give theirs the same way; the Fluid Regulator
 * turns a compressed air cell into an empty cell and makes no air out of nothing.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class PlayerReportGameTests {
    private PlayerReportGameTests() {}

    private static ResourceLocation si(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }
    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }
    private static Item item(String path) { return BuiltInRegistries.ITEM.get(si(path)); }

    /** The main-hand attack damage modifiers of a stack, summed. */
    private static double attackDamage(ItemStack stack) {
        double[] sum = {0};
        stack.getAttributeModifiers().forEach(EquipmentSlotGroup.MAINHAND, (attribute, modifier) -> {
            if (attribute.is(Attributes.ATTACK_DAMAGE)) sum[0] += modifier.amount();
        });
        return sum[0];
    }

    private static ItemStack saber(boolean active) {
        ItemStack saber = new ItemStack(item("item_tool_nanosaber"));
        call(Round33GameTests.common("item.tools.mio_icif_nanosaber"), "setActive", saber, active);
        return saber;
    }

    @GameTest(batch = "player_reports", template = "empty", timeoutTicks = 20)
    public static void nanoSaberCutsCobwebs(GameTestHelper h) {
        var cobweb = Blocks.COBWEB.defaultBlockState();
        ItemStack on = saber(true), off = saber(false);
        h.assertTrue(on.isCorrectToolForDrops(cobweb) && off.isCorrectToolForDrops(cobweb), "cobwebs drop no string");
        h.assertTrue(on.getDestroySpeed(cobweb) == 50.0F && on.getDestroySpeed(Blocks.DIRT.defaultBlockState()) == 4.0F
            && off.getDestroySpeed(cobweb) == 1.0F, "speeds " + on.getDestroySpeed(cobweb) + " / " + off.getDestroySpeed(cobweb));
        h.succeed();
    }

    /** A mob with the saber in hand gets its damage: what a maid (any mob) attacks with. */
    @GameTest(batch = "player_reports", template = "empty", timeoutTicks = 40)
    public static void nanoSaberDamageIsAnAttribute(GameTestHelper h) {
        h.assertTrue(attackDamage(saber(true)) == 20.0 && attackDamage(saber(false)) == 4.0,
            "saber damage " + attackDamage(saber(true)) + " / " + attackDamage(saber(false)));
        Zombie zombie = h.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
        double base = zombie.getAttributeValue(Attributes.ATTACK_DAMAGE);
        zombie.setItemSlot(EquipmentSlot.MAINHAND, saber(true));
        h.succeedWhen(() -> {
            double now = zombie.getAttributeValue(Attributes.ATTACK_DAMAGE);
            h.assertTrue(now == base + 20.0, "a mob holding the lit saber has " + now + " attack damage (base " + base + ")");
            zombie.discard();
        });
    }

    @GameTest(batch = "player_reports", template = "empty", timeoutTicks = 20)
    public static void chainsawAndDrillsShowTheirDamage(GameTestHelper h) {
        h.assertTrue(attackDamage(new ItemStack(item("item_tool_iron_chainsaw"))) == 9.0, "chainsaw");
        h.assertTrue(attackDamage(new ItemStack(item("item_tool_iron_driller"))) == 8.0, "mining drill");
        h.assertTrue(attackDamage(new ItemStack(item("item_tool_diamond_driller"))) == 8.0, "diamond drill");
        h.assertTrue(attackDamage(new ItemStack(item("item_tool_iridium_driller"))) == 20.0, "iridium drill");
        h.succeed();
    }

    private static Fluid air() { return BuiltInRegistries.FLUID.get(si("air")); }

    /** Two compressed air cells: 2,000 mB of air and two empty cells, only as the output slot makes room. */
    @GameTest(batch = "player_reports", template = "empty", timeoutTicks = 40)
    public static void fluidRegulatorEmptiesAirCellsOnce(GameTestHelper h) {
        BlockPos pos = new BlockPos(1, 1, 1);
        h.setBlock(pos, BuiltInRegistries.BLOCK.get(si("producer/block_fluid_regulator_elc")));
        var regulator = h.getBlockEntity(pos);
        var slots = (IItemHandlerModifiable) call(regulator, "getItemHandler");
        FluidTank tank = (FluidTank) call(regulator, "getFluidTank");
        ItemStack emptyCell = new ItemStack(item("cell/item_cell_empty"));
        slots.setStackInSlot(1, new ItemStack(item("cell/item_cell_air"), 2));
        slots.setStackInSlot(2, emptyCell.copyWithCount(emptyCell.getMaxStackSize()));   // no room for the empty cell
        int[] made = {0};
        h.startSequence()
            .thenIdle(5)
            .thenExecute(() -> {
                h.assertTrue(tank.isEmpty() && slots.getStackInSlot(1).getCount() == 2, "air without room for the empty cell: " + tank.getFluidAmount());
                slots.setStackInSlot(2, ItemStack.EMPTY);
            })
            .thenIdle(5)
            .thenExecute(() -> {
                h.assertTrue(tank.getFluid().getFluid() == air() && tank.getFluidAmount() == 2000, "tank " + tank.getFluid());
                ItemStack out = slots.getStackInSlot(2);
                h.assertTrue(out.is(emptyCell.getItem()) && out.getCount() == 2
                    && out.getCapability(Capabilities.FluidHandler.ITEM).getFluidInTank(0).isEmpty(), "output " + out);
                h.assertTrue(slots.getStackInSlot(1).isEmpty(), "input " + slots.getStackInSlot(1));
                made[0] += tank.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE).getAmount();
            })
            // the tank keeps room now (as with a blast furnace drawing from it): no more air appears
            .thenIdle(10)
            .thenExecute(() -> {
                made[0] += tank.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE).getAmount();
                h.assertTrue(made[0] == 2000, made[0] + " mB of air from two cells");
            })
            .thenSucceed();
    }

    /** The universal cell filled with air (how the compressor made it before): the same, an empty cell out. */
    @GameTest(batch = "player_reports", template = "empty", timeoutTicks = 20)
    public static void fluidRegulatorEmptiesUniversalAirCells(GameTestHelper h) {
        BlockPos pos = new BlockPos(1, 1, 1);
        h.setBlock(pos, BuiltInRegistries.BLOCK.get(si("producer/block_fluid_regulator_elc")));
        var regulator = h.getBlockEntity(pos);
        var slots = (IItemHandlerModifiable) call(regulator, "getItemHandler");
        FluidTank tank = (FluidTank) call(regulator, "getFluidTank");
        ItemStack universal = (ItemStack) call(Round33GameTests.common("item.cell.mio_icif_cells"), "createDynamicFilledCell", air(), 1000);
        slots.setStackInSlot(1, universal);
        h.runAfterDelay(3, () -> {
            ItemStack out = slots.getStackInSlot(2);
            h.assertTrue(tank.getFluidAmount() == 1000 && slots.getStackInSlot(1).isEmpty(), "tank " + tank.getFluidAmount());
            h.assertTrue(out.getCount() == 1 && out.getCapability(Capabilities.FluidHandler.ITEM).getFluidInTank(0).isEmpty(),
                "output still holds air: " + out);
            h.succeed();
        });
    }

    /** Every static cell can be drained through its fluid capability and leaves an empty cell (the air cell had none). */
    @GameTest(batch = "player_reports", template = "empty", timeoutTicks = 20)
    public static void drainedStaticCellsBecomeEmptyCells(GameTestHelper h) {
        Item emptyCell = item("cell/item_cell_empty");
        for (String path : new String[] {"cell/item_cell_air", "cell/item_cell_water", "cell/item_cell_lava"}) {
            var handler = new ItemStack(item(path)).getCapability(Capabilities.FluidHandler.ITEM);
            h.assertTrue(handler != null, path + " has no fluid capability");
            FluidStack drained = handler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
            h.assertTrue(drained.getAmount() == 1000, path + " gave " + drained.getAmount());
            h.assertTrue(handler.getContainer().is(emptyCell), path + " drained into " + handler.getContainer());
        }
        h.succeed();
    }
}
