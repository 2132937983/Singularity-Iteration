package example.sicore;

import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractProcessingMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

/**
 * 0.1.11.113 (processing overhaul M6): the furnaces have a temperature. The electric furnace heats its chamber 10 °C a
 * tick (food from 250 °C, ores from 1,000 °C) and cools by Newton's law (time constant 600 ticks); the induction
 * furnace heats its melt with up to 64 EU/t towards 1,600 °C, holds it by replacing the loss (8 EU/t at full heat),
 * smelts from 600 °C at full speed at 1,600 °C, and stops for a remote switch. (The mod's common classes by reflection:
 * addon examples do not compile against them.)
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111113GameTests {
    private Round111113GameTests() {}

    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }
    private static double temp(Object be) { return (double) call(be, "temperature"); }

    private static AbstractProcessingMachineBlockEntity place(GameTestHelper h, BlockPos at, String id) {
        h.setBlock(at, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", id)));
        return (AbstractProcessingMachineBlockEntity) h.getBlockEntity(at);
    }

    private static void keepCharged(GameTestHelper h, AbstractProcessingMachineBlockEntity m) {
        h.onEachTick(() -> m.getEnergyStorageInternal().setStored(m.getEnergyStorageInternal().getCapacity()));
    }

    /** Raw iron waits for 1,000 °C (about 100 ticks of heating), raw beef only for 250 °C. */
    @GameTest(template = "empty", timeoutTicks = 300)
    public static void electricFurnaceHeatsUp(GameTestHelper h) {
        var iron = place(h, new BlockPos(1, 2, 1), "producer/block_furnace_elc");
        var beef = place(h, new BlockPos(3, 2, 1), "producer/block_furnace_elc");
        ((IItemHandlerModifiable) iron.getItemHandler()).setStackInSlot(0, new ItemStack(Items.RAW_IRON, 4));
        ((IItemHandlerModifiable) beef.getItemHandler()).setStackInSlot(0, new ItemStack(Items.BEEF, 4));
        keepCharged(h, iron);
        keepCharged(h, beef);
        h.runAfterDelay(50, () -> {
            h.assertTrue(iron.getProgress() == 0 && (int) call(iron, "processStatus") == 7, "raw iron at " + temp(iron) + " °C: progress "
                + iron.getProgress() + ", status " + call(iron, "processStatus"));
            h.assertTrue(temp(iron) > 400 && temp(iron) < 600, "50 ticks of heating: " + temp(iron) + " °C");
            h.assertTrue(beef.getProgress() > 0, "raw beef did not start at " + temp(beef) + " °C");
            h.runAfterDelay(160, () -> {
                int ingots = ((IItemHandlerModifiable) iron.getItemHandler()).getStackInSlot(2).getCount();
                h.assertTrue(ingots == 1, "about 100 ticks to heat and 100 to smelt: " + ingots + " ingots after 210 ticks");
                h.succeed();
            });
        });
    }

    /** A chamber at 1,000 °C left alone is at about 380 °C after 600 ticks (Newton's law, time constant 600 ticks). */
    @GameTest(template = "empty", timeoutTicks = 700)
    public static void electricFurnaceCools(GameTestHelper h) {
        var m = place(h, new BlockPos(1, 2, 1), "producer/block_furnace_elc");
        CompoundTag tag = new CompoundTag();
        tag.putDouble("furnace_temperature", 1000);
        tag.putLong("furnace_temperature_time", h.getLevel().getGameTime());
        m.loadAdditional(tag, h.getLevel().registryAccess());
        h.runAfterDelay(600, () -> {
            double t = temp(m);
            h.assertTrue(Math.abs(t - 380.5) < 12, "after 600 ticks: " + t + " °C");
            h.succeed();
        });
    }

    /** Heat hold from cold: about 260 °C in 100 ticks; at full heat holding costs about 8 EU/t; a remote stop stops it. */
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void inductionHeatsHoldsAndStops(GameTestHelper h) {
        var m = place(h, new BlockPos(1, 2, 1), "producer/block_induction_elc");
        call(m, "toggleHeatHold");
        keepCharged(h, m);
        h.runAfterDelay(100, () -> {
            double t = temp(m);
            h.assertTrue(t > 230 && t < 300, "100 ticks of heating from 20 °C: " + t + " °C");
            CompoundTag tag = new CompoundTag();
            tag.putDouble("induction_temperature", 1600);
            tag.putBoolean("heat_hold", true);
            m.loadAdditional(tag, h.getLevel().registryAccess());
            h.runAfterDelay(5, () -> {
                long draw = (long) call(m, "heaterDraw");
                h.assertTrue(draw >= 7 && draw <= 9 && temp(m) > 1595, "holding 1,600 °C: " + draw + " EU/t at " + temp(m) + " °C");
                m.setRemotelyDisabled(true);
                h.runAfterDelay(20, () -> {
                    h.assertTrue((long) call(m, "heaterDraw") == 0 && temp(m) < 1600, "remote stop: heater " + call(m, "heaterDraw") + ", " + temp(m) + " °C");
                    h.succeed();
                });
            });
        });
    }

    /** At 1,600 °C each lane smelts an item every 12 ticks. */
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void inductionSmeltsAtFullSpeed(GameTestHelper h) {
        var m = place(h, new BlockPos(1, 2, 1), "producer/block_induction_elc");
        CompoundTag tag = new CompoundTag();
        tag.putDouble("induction_temperature", 1600);
        m.loadAdditional(tag, h.getLevel().registryAccess());
        IItemHandlerModifiable items = (IItemHandlerModifiable) m.getItemHandler();
        items.setStackInSlot(0, new ItemStack(Items.RAW_IRON, 16));
        items.setStackInSlot(1, new ItemStack(Items.RAW_COPPER, 16));
        keepCharged(h, m);
        h.runAfterDelay(62, () -> {
            int a = items.getStackInSlot(3).getCount(), b = items.getStackInSlot(4).getCount();
            h.assertTrue(a >= 4 && a <= 6 && a == b, "60 ticks at 1,600 °C: " + a + " iron and " + b + " copper ingots (5 each expected)");
            h.succeed();
        });
    }
}
