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
 * 0.1.11.111 (processing overhaul M4): every processing machine overclocks exactly: n overclockers make an operation
 * take {@code base × 0.7^n} ticks (it was rounded up per tick in the machines outside the standard base: 2 overclockers
 * gave ×3 instead of ×2.04). Checked on the recycler (45 t), the advanced compressor (70 t) and the block cutter (900 t):
 * the ticks per operation the GUI shows, and the recycler's real running time.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111111GameTests {
    private Round111111GameTests() {}

    private static AbstractProcessingMachineBlockEntity place(GameTestHelper h, BlockPos at, String id) {
        h.setBlock(at, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", id)));
        return (AbstractProcessingMachineBlockEntity) h.getBlockEntity(at);
    }

    private static void overclock(AbstractProcessingMachineBlockEntity m, int n) {
        IItemHandlerModifiable items = (IItemHandlerModifiable) m.getItemHandler();
        items.setStackInSlot(m.getUpgradeSlotStart(), new ItemStack(BuiltInRegistries.ITEM.get(
            ResourceLocation.fromNamespaceAndPath("mio_icif", "upgrade/overclocker_upgrade")), n));
    }

    /** With 2 overclockers: ⌈45 × 0.49⌉ = 23, ⌈75 × 0.49⌉ = 37 (0.1.11.112: the advanced compressor's stock stroke is 75 t), ⌈900 × 0.49⌉ = 441 ticks per operation. */
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void exactOverclockTimes(GameTestHelper h) {
        Object[][] machines = {{"producer/block_recycler_elc", 23}, {"producer/block_compressor_advanced_elc", 37}, {"producer/block_block_cutter", 441}};
        AbstractProcessingMachineBlockEntity[] placed = new AbstractProcessingMachineBlockEntity[machines.length];
        for (int i = 0; i < machines.length; i++) {
            placed[i] = place(h, new BlockPos(1 + 2 * i, 2, 1), (String) machines[i][0]);
            overclock(placed[i], 2);
        }
        h.runAfterDelay(3, () -> {
            for (int i = 0; i < machines.length; i++) {
                int ticks = placed[i].getTicksPerOperation();
                h.assertTrue(ticks == (int) machines[i][1], machines[i][0] + " with 2 overclockers: " + ticks + " ticks per operation");
            }
            h.succeed();
        });
    }

    /** The recycler with 2 overclockers runs 10 operations in about 230 ticks (it took 150 with the old rounding). */
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void recyclerRunsAtTheExactSpeed(GameTestHelper h) {
        var m = place(h, new BlockPos(1, 2, 1), "producer/block_recycler_elc");
        overclock(m, 2);
        IItemHandlerModifiable items = (IItemHandlerModifiable) m.getItemHandler();
        items.setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 64));
        h.onEachTick(() -> m.getEnergyStorageInternal().setStored(m.getEnergyStorageInternal().getCapacity()));
        int[] start = {-1};
        h.runAfterDelay(5, () -> {
            start[0] = items.getStackInSlot(0).getCount();
            h.runAfterDelay(230, () -> {
                int done = start[0] - items.getStackInSlot(0).getCount();
                h.assertTrue(done >= 9 && done <= 11, "operations in 230 ticks: " + done + " (exact 0.7^2: 10; old rounding: 15)");
                h.succeed();
            });
        });
    }
}
