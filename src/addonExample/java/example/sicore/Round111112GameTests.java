package example.sicore;

import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractProcessingMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

/**
 * 0.1.11.112 (processing overhaul M5): the compressor runs each recipe as written (snowballs 100 t × 5 EU/t, iron plates
 * to a dense plate 400 t × 20 EU/t; a recipe without values, the coal ball, the stock 300 t × 2 EU/t) with a buffer that
 * does not change with the input; the advanced compressor does the same work in a quarter of the time from its
 * accumulator, and a stroke waits until the accumulator holds all of it. (The mod's common classes by reflection: addon
 * examples do not compile against them.)
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111112GameTests {
    private Round111112GameTests() {}

    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }
    private static Item item(String path) { return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", path)); }

    private static AbstractProcessingMachineBlockEntity place(GameTestHelper h, BlockPos at, String id) {
        h.setBlock(at, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", id)));
        return (AbstractProcessingMachineBlockEntity) h.getBlockEntity(at);
    }

    /** input, count, energy, power, ticks (compressor); the advanced compressor: same energy in ⌈ticks / 4⌉. */
    private static final Object[][] INPUTS = {
        {Items.SNOWBALL, 4, 500L, 5L, 100}, {"resource/item_iron_plate", 9, 8000L, 20L, 400}, {"resource/item_coal_ball", 1, 600L, 2L, 300},
        {"resource/item_coal_chunk", 1, 18000L, 30L, 600}};

    private static ItemStack stack(Object[] row) {
        Item it = row[0] instanceof Item i ? i : item((String) row[0]);
        return new ItemStack(it, (int) row[1]);
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void compressorsRunTheRecipes(GameTestHelper h) {
        var basic = place(h, new BlockPos(1, 2, 1), "producer/block_compressor_elc");
        var advanced = place(h, new BlockPos(3, 2, 1), "producer/block_compressor_advanced_elc");
        long[] caps = new long[INPUTS.length];
        step(h, basic, advanced, caps, 0);
    }

    private static void step(GameTestHelper h, AbstractProcessingMachineBlockEntity basic, AbstractProcessingMachineBlockEntity advanced, long[] caps, int i) {
        if (i == INPUTS.length) {
            for (long c : caps) h.assertTrue(c == caps[0], "the compressor's buffer changed with the input: " + java.util.Arrays.toString(caps));
            h.assertTrue(caps[0] == 600 + 18000, "compressor buffer " + caps[0]);
            h.assertTrue(advanced.getEffectiveCapacity() == 20000, "accumulator " + advanced.getEffectiveCapacity());
            h.succeed();
            return;
        }
        Object[] row = INPUTS[i];
        ((IItemHandlerModifiable) basic.getItemHandler()).setStackInSlot(0, stack(row));
        ((IItemHandlerModifiable) advanced.getItemHandler()).setStackInSlot(0, stack(row));
        h.runAfterDelay(3, () -> {
            Object a = call(basic, "processCost"), b = call(advanced, "processCost");
            String what = row[0] + ": compressor " + a + ", " + basic.getMaxProgress() + " t; advanced " + b + ", " + advanced.getMaxProgress() + " t";
            h.assertTrue((long) call(a, "energy") == (long) row[2] && (long) call(a, "power") == (long) row[3] && basic.getMaxProgress() == (int) row[4], what);
            int strokeTicks = ((int) row[4] + 3) / 4;
            h.assertTrue(advanced.getMaxProgress() == strokeTicks && (long) call(b, "energy") >= (long) row[2]
                && (long) call(b, "energy") < (long) row[2] + strokeTicks, what);
            caps[i] = basic.getEffectiveCapacity();
            step(h, basic, advanced, caps, i + 1);
        });
    }

    /** With 4,000 EU stored (a dense plate needs 8,000) the press charges and waits; with enough it strokes in 100 t. */
    @GameTest(template = "empty", timeoutTicks = 300)
    public static void accumulatorGatesTheStroke(GameTestHelper h) {
        var m = place(h, new BlockPos(1, 2, 1), "producer/block_compressor_advanced_elc");
        IItemHandlerModifiable items = (IItemHandlerModifiable) m.getItemHandler();
        items.setStackInSlot(0, new ItemStack(item("resource/item_iron_plate"), 18));
        m.getEnergyStorageInternal().setStored(4000);
        h.runAfterDelay(20, () -> {
            h.assertTrue(m.getProgress() == 0 && items.getStackInSlot(1).isEmpty(), "pressed without the pressure: progress " + m.getProgress());
            h.assertTrue((int) call(m, "processStatus") == 6, "status " + call(m, "processStatus") + " (charging is 6)");
            double p = (double) call(m, "pressureMpa"), need = (double) call(m, "requiredPressureMpa");
            h.assertTrue(Math.abs(p - 6.4) < 0.01 && Math.abs(need - 12.8) < 0.05, "pressure " + p + " MPa, needs " + need + " MPa");
            m.getEnergyStorageInternal().setStored(9000);
            h.runAfterDelay(105, () -> {
                h.assertTrue(items.getStackInSlot(1).getCount() == 1, "one stroke in 100 ticks: " + items.getStackInSlot(1));
                h.succeed();
            });
        });
    }
}
