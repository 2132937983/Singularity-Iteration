package example.sicore;

import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractProcessingMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

/**
 * 0.1.11.114 (processing overhaul M7): the ore washer passes the recipe's water (1,000 mB for crushed ore) and keeps 80 %
 * of it, so a washing nets 200 mB drawn tick by tick; with half that it runs half way and waits. The thermal centrifuge
 * heats only with work, starts at the recipe's minimum heat (cobblestone 100 HU, not the old fixed 5,000), and its rotor
 * spins up 10 % a second. (The mod's common classes by reflection: addon examples do not compile against them.)
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111114GameTests {
    private Round111114GameTests() {}

    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    private static AbstractProcessingMachineBlockEntity place(GameTestHelper h, BlockPos at, String id) {
        h.setBlock(at, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", id)));
        return (AbstractProcessingMachineBlockEntity) h.getBlockEntity(at);
    }

    private static void keepCharged(GameTestHelper h, AbstractProcessingMachineBlockEntity m) {
        h.onEachTick(() -> m.getEnergyStorageInternal().setStored(m.getEnergyStorageInternal().getCapacity()));
    }

    private static ItemStack crushedIron(int n) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "resource/item_iron_ore_crushed")), n);
    }

    /** One washing of crushed iron: 1,000 mB flow, 200 mB net, drawn over the 400 ticks. */
    @GameTest(template = "empty", timeoutTicks = 500)
    public static void washerRecoversItsWater(GameTestHelper h) {
        var m = place(h, new BlockPos(1, 2, 1), "producer/block_washer_elc");
        IFluidHandler tank = (IFluidHandler) call(m, "getFluidHandler");
        tank.fill(new FluidStack(Fluids.WATER, 8000), IFluidHandler.FluidAction.EXECUTE);
        ((IItemHandlerModifiable) m.getItemHandler()).setStackInSlot(0, crushedIron(4));
        keepCharged(h, m);
        h.runAfterDelay(3, () -> {
            h.assertTrue((int) call(m, "grossWaterPerOperation") == 1000 && (int) call(m, "netWaterPerOperation") == 200,
                "flow " + call(m, "grossWaterPerOperation") + ", net " + call(m, "netWaterPerOperation"));
            h.runAfterDelay(402, () -> {
                int left = (int) call(m, "getFluidAmount"), done = 4 - m.getItemHandler().getStackInSlot(0).getCount();
                h.assertTrue(done == 1, "washings in 405 ticks: " + done);
                h.assertTrue(left >= 7795 && left <= 7802, "water left after one washing: " + left + " mB (8,000 - 200)");
                h.succeed();
            });
        });
    }

    /** With 100 mB (half a washing) the washer gets half way and waits for water. */
    @GameTest(template = "empty", timeoutTicks = 500)
    public static void washerWaitsForWater(GameTestHelper h) {
        var m = place(h, new BlockPos(1, 2, 1), "producer/block_washer_elc");
        ((IFluidHandler) call(m, "getFluidHandler")).fill(new FluidStack(Fluids.WATER, 100), IFluidHandler.FluidAction.EXECUTE);
        ((IItemHandlerModifiable) m.getItemHandler()).setStackInSlot(0, crushedIron(4));
        keepCharged(h, m);
        h.runAfterDelay(400, () -> {
            int progress = m.getProgress();
            h.assertTrue(progress >= 190 && progress <= 210, "progress on 100 mB: " + progress + " of 400");
            h.assertTrue((int) call(m, "processStatus") == 8, "status " + call(m, "processStatus") + " (no water is 8)");
            h.succeed();
        });
    }

    /** Cobblestone needs 100 HU: the centrifuge starts in about 25 ticks; an empty one stays cold; the rotor spins up. */
    @GameTest(template = "empty", timeoutTicks = 300)
    public static void centrifugeHeatsToTheRecipe(GameTestHelper h) {
        var m = place(h, new BlockPos(1, 2, 1), "producer/block_centrifuge_elc");
        var empty = place(h, new BlockPos(3, 2, 1), "producer/block_centrifuge_elc");
        ((IItemHandlerModifiable) m.getItemHandler()).setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 8));
        keepCharged(h, m);
        keepCharged(h, empty);
        h.runAfterDelay(40, () -> {
            h.assertTrue((int) call(m, "requiredHeat") == 100, "cobblestone needs " + call(m, "requiredHeat") + " HU");
            int heat = (int) call(m, "getHeatStorage");
            h.assertTrue(heat >= 100 && heat < 5000 && m.isWorking(), "40 ticks: " + heat + " HU, working " + m.isWorking());
            h.assertTrue((int) call(empty, "getHeatStorage") == 0, "an empty centrifuge heated: " + call(empty, "getHeatStorage"));
            h.runAfterDelay(220, () -> {
                double speed = (double) call(m, "getRotorSpeed");
                h.assertTrue(speed == 1.0, "rotor after about 230 working ticks: " + speed);
                h.assertTrue(Math.abs((double) call(m, "relativeCentrifugalForce") - 16_097) < 50, "force " + call(m, "relativeCentrifugalForce"));
                h.succeed();
            });
        });
    }
}
