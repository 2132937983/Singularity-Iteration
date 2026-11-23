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
 * 0.1.11.179: the refined iron cutting blade goes into the block cutter (it was refused: the cutter knew only the iron and
 * the diamond blade). It is IC2's middle blade: hardness 5, so it cuts the iron block (hardness 5), which the iron blade
 * (2) cannot; it lasts 256 cuts of hardness 5 (a wear of 1 a cut).
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111179GameTests {
    private Round111179GameTests() {}

    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    private static AbstractProcessingMachineBlockEntity cutter(GameTestHelper h, BlockPos at) {
        h.setBlock(at, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "producer/block_block_cutter")));
        return (AbstractProcessingMachineBlockEntity) h.getBlockEntity(at);
    }

    private static ItemStack item(String path) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", path)));
    }

    @GameTest(template = "empty", batch = "round111179", timeoutTicks = 40)
    public static void refinedIronBladeCutsTheIronBlock(GameTestHelper h) {
        var refined = cutter(h, new BlockPos(1, 2, 1));
        var iron = cutter(h, new BlockPos(3, 2, 1));
        IItemHandlerModifiable a = (IItemHandlerModifiable) refined.getItemHandler(), b = (IItemHandlerModifiable) iron.getItemHandler();
        ItemStack blade = item("resource/item_adviron_cutblade");
        h.assertTrue(blade.getMaxDamage() == 256, "refined iron blade durability " + blade.getMaxDamage());
        h.assertTrue(a.isItemValid(1, blade) && a.insertItem(1, blade.copy(), true).isEmpty(), "the blade slot refuses the refined iron blade");
        a.insertItem(1, blade.copy(), false);
        b.setStackInSlot(1, item("resource/item_iron_cut_blade"));
        a.setStackInSlot(0, new ItemStack(Items.IRON_BLOCK, 2));
        b.setStackInSlot(0, new ItemStack(Items.IRON_BLOCK, 2));
        h.onEachTick(() -> {
            refined.getEnergyStorageInternal().setStored(refined.getEnergyStorageInternal().getCapacity());
            iron.getEnergyStorageInternal().setStored(iron.getEnergyStorageInternal().getCapacity());
        });
        h.runAfterDelay(5, () -> {
            h.assertTrue((int) call(refined, "processStatus") == 0, "refined iron blade on the iron block: status " + call(refined, "processStatus") + " (running is 0)");
            h.assertTrue(Math.abs((double) call(refined, "wearPerCut") - 1.0) < 1e-9, "wear a cut " + call(refined, "wearPerCut"));
            h.assertTrue(refined.getProgress() > 0, "no progress with the refined iron blade");
            h.assertTrue((int) call(iron, "processStatus") == 9, "iron blade on the iron block: status " + call(iron, "processStatus") + " (no blade is 9)");
            h.succeed();
        });
    }
}
