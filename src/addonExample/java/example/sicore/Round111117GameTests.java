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
 * 0.1.11.117 (processing overhaul M10, the balance pass): the ore washer runs a recipe's own time and power (crushed
 * niobium 200 t × 10 EU/t; crushed iron keeps 400 t × 16 EU/t); the basic metal former's operation length follows the
 * recipe (iron plate to casing 300 t: 147 ticks with 2 overclockers, it showed 98 from the stale stock 200); the block
 * cutter cuts metal blocks in 150 t × hardness at 16 EU/t (iron 750 t). (The mod's common classes by reflection: addon
 * examples do not compile against them.)
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111117GameTests {
    private Round111117GameTests() {}

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

    /** Crushed niobium washes in 200 t at 10 EU/t (its recipe); crushed iron in the stock 400 t at 16 EU/t. */
    @GameTest(template = "empty", timeoutTicks = 300)
    public static void washerRunsTheRecipe(GameTestHelper h) {
        var niobium = place(h, new BlockPos(1, 2, 1), "producer/block_washer_elc");
        var iron = place(h, new BlockPos(3, 2, 1), "producer/block_washer_elc");
        for (var m : new AbstractProcessingMachineBlockEntity[]{niobium, iron}) {
            ((IFluidHandler) call(m, "getFluidHandler")).fill(new FluidStack(Fluids.WATER, 8000), IFluidHandler.FluidAction.EXECUTE);
            keepCharged(h, m);
        }
        IItemHandlerModifiable items = (IItemHandlerModifiable) niobium.getItemHandler();
        items.setStackInSlot(0, item("resource/item_niobium_ore_crushed", 4));
        ((IItemHandlerModifiable) iron.getItemHandler()).setStackInSlot(0, item("resource/item_iron_ore_crushed", 4));
        h.runAfterDelay(3, () -> {
            h.assertTrue(niobium.getMaxProgress() == 200 && niobium.getEffectiveEnergyPerTick() == 10,
                "niobium: " + niobium.getMaxProgress() + " t at " + niobium.getEffectiveEnergyPerTick() + " EU/t");
            h.assertTrue(iron.getMaxProgress() == 400 && iron.getEffectiveEnergyPerTick() == 16,
                "iron: " + iron.getMaxProgress() + " t at " + iron.getEffectiveEnergyPerTick() + " EU/t");
            h.runAfterDelay(205, () -> {
                int done = 4 - items.getStackInSlot(0).getCount();
                h.assertTrue(done == 1, "niobium washings in 208 ticks: " + done);
                h.succeed();
            });
        });
    }

    /** Iron plate to casing (300 t) with 2 overclockers: ⌈300 × 0.49⌉ = 147 ticks per operation. */
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void metalFormerLengthFollowsTheRecipe(GameTestHelper h) {
        var m = place(h, new BlockPos(1, 2, 1), "producer/block_metal_former");
        IItemHandlerModifiable items = (IItemHandlerModifiable) m.getItemHandler();
        items.setStackInSlot(m.getUpgradeSlotStart(), item("upgrade/overclocker_upgrade", 2));
        items.setStackInSlot(0, item("resource/item_iron_plate", 8));
        keepCharged(h, m);
        h.runAfterDelay(5, () -> {
            h.assertTrue(m.getMaxProgress() == 300 && m.getTicksPerOperation() == 147,
                "casing: length " + m.getMaxProgress() + ", " + m.getTicksPerOperation() + " ticks per operation with 2 overclockers");
            h.succeed();
        });
    }

    /** An iron block cuts in 750 t at 16 EU/t (hardness 5), obsidian in 1,200 t. */
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void cutterTimesMetalBlocksByHardness(GameTestHelper h) {
        var iron = place(h, new BlockPos(1, 2, 1), "producer/block_block_cutter");
        var obsidian = place(h, new BlockPos(3, 2, 1), "producer/block_block_cutter");
        ((IItemHandlerModifiable) iron.getItemHandler()).setStackInSlot(0, new ItemStack(Items.IRON_BLOCK, 2));
        ((IItemHandlerModifiable) iron.getItemHandler()).setStackInSlot(1, item("resource/item_diamond_cut_blade", 1));
        ((IItemHandlerModifiable) obsidian.getItemHandler()).setStackInSlot(0, new ItemStack(Items.OBSIDIAN, 2));
        ((IItemHandlerModifiable) obsidian.getItemHandler()).setStackInSlot(1, item("resource/item_diamond_cut_blade", 1));
        keepCharged(h, iron);
        keepCharged(h, obsidian);
        h.runAfterDelay(5, () -> {
            h.assertTrue(iron.getMaxProgress() == 750 && iron.getEffectiveEnergyPerTick() == 16,
                "iron block: " + iron.getMaxProgress() + " t at " + iron.getEffectiveEnergyPerTick() + " EU/t");
            h.assertTrue(obsidian.getMaxProgress() == 1200, "obsidian: " + obsidian.getMaxProgress() + " t");
            h.succeed();
        });
    }
}
