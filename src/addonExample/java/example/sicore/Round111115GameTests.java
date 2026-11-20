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
 * 0.1.11.115 (processing overhaul M8): both metal formers run the recipe's time (the advanced one a fifth of it at
 * 50 EU/t); the block cutter runs the recipe's time and power when it writes them (logs 100 t × 2 EU/t; the copper block
 * 150 t × 16 EU/t since 0.1.11.117, by its hardness), and its blade wears by (material ÷ blade hardness)² a cut and breaks when worn through. (The
 * mod's common classes by reflection: addon examples do not compile against them.)
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111115GameTests {
    private Round111115GameTests() {}

    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    private static AbstractProcessingMachineBlockEntity place(GameTestHelper h, BlockPos at, String id) {
        h.setBlock(at, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", id)));
        return (AbstractProcessingMachineBlockEntity) h.getBlockEntity(at);
    }

    private static ItemStack item(String path, int n) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", path)), n);
    }

    /** Rolling an iron ingot: 200 t at 10 EU/t; the advanced former 40 t at 50 EU/t; an iron plate to a casing 300 t and 60 t. */
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void metalFormersRunTheRecipeTime(GameTestHelper h) {
        var basic = place(h, new BlockPos(1, 2, 1), "producer/block_metal_former");
        var advanced = place(h, new BlockPos(3, 2, 1), "producer/block_metal_former_advanced");
        ((IItemHandlerModifiable) basic.getItemHandler()).setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 4));
        ((IItemHandlerModifiable) advanced.getItemHandler()).setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 4));
        basic.getEnergyStorageInternal().setStored(1000);
        advanced.getEnergyStorageInternal().setStored(1000);
        h.runAfterDelay(3, () -> {
            h.assertTrue(basic.getMaxProgress() == 200 && basic.getEffectiveEnergyPerTick() == 10,
                "former: " + basic.getMaxProgress() + " t, " + basic.getEffectiveEnergyPerTick() + " EU/t");
            h.assertTrue(advanced.getMaxProgress() == 40 && advanced.getEffectiveEnergyPerTick() == 50,
                "advanced former: " + advanced.getMaxProgress() + " t, " + advanced.getEffectiveEnergyPerTick() + " EU/t");
            ((IItemHandlerModifiable) advanced.getItemHandler()).setStackInSlot(0, item("resource/item_iron_plate", 4));
            h.runAfterDelay(3, () -> {
                h.assertTrue(advanced.getMaxProgress() == 60, "advanced former, iron plate to casing: " + advanced.getMaxProgress() + " t");
                h.succeed();
            });
        });
    }

    /** Logs run 100 t at 2 EU/t, the copper block 150 t at 16 EU/t (0.1.11.117); four log cuts wear an iron blade by 1. */
    @GameTest(template = "empty", timeoutTicks = 600)
    public static void cutterRunsRecipesAndWearsTheBlade(GameTestHelper h) {
        var m = place(h, new BlockPos(1, 2, 1), "producer/block_block_cutter");
        IItemHandlerModifiable items = (IItemHandlerModifiable) m.getItemHandler();
        items.setStackInSlot(1, item("resource/item_iron_cut_blade", 1));
        items.setStackInSlot(0, new ItemStack(Items.COPPER_BLOCK, 2));
        h.onEachTick(() -> m.getEnergyStorageInternal().setStored(m.getEnergyStorageInternal().getCapacity()));
        h.runAfterDelay(3, () -> {
            h.assertTrue(m.getMaxProgress() == 150 && m.getEffectiveEnergyPerTick() == 16, "copper block: " + m.getMaxProgress() + " t, " + m.getEffectiveEnergyPerTick() + " EU/t");
            items.setStackInSlot(0, new ItemStack(Items.OAK_LOG, 8));
            h.runAfterDelay(3, () -> {
                h.assertTrue(m.getMaxProgress() == 100 && m.getEffectiveEnergyPerTick() == 2, "oak log: " + m.getMaxProgress() + " t, " + m.getEffectiveEnergyPerTick() + " EU/t");
                h.assertTrue(Math.abs((double) call(m, "wearPerCut") - 0.25) < 1e-9, "wear a log cut with an iron blade: " + call(m, "wearPerCut"));
                h.runAfterDelay(410, () -> {
                    int cut = 8 - items.getStackInSlot(0).getCount();
                    int damage = items.getStackInSlot(1).getDamageValue();
                    h.assertTrue(cut == 4 && damage == 1, cut + " logs cut, blade damage " + damage + " (4 cuts × 0.25)");
                    h.succeed();
                });
            });
        });
    }

    /** An iron blade with one point left breaks on the cut that wears it through. */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void wornBladeBreaks(GameTestHelper h) {
        var m = place(h, new BlockPos(1, 2, 1), "producer/block_block_cutter");
        IItemHandlerModifiable items = (IItemHandlerModifiable) m.getItemHandler();
        ItemStack blade = item("resource/item_iron_cut_blade", 1);
        blade.setDamageValue(63);
        items.setStackInSlot(1, blade);
        CompoundTag tag = new CompoundTag();
        tag.putDouble("blade_wear_carry", 0.75);
        m.loadAdditional(tag, h.getLevel().registryAccess());
        items.setStackInSlot(0, new ItemStack(Items.OAK_LOG, 8));
        h.onEachTick(() -> m.getEnergyStorageInternal().setStored(m.getEnergyStorageInternal().getCapacity()));
        h.runAfterDelay(110, () -> {
            h.assertTrue(items.getStackInSlot(1).isEmpty(), "the worn blade is still there: " + items.getStackInSlot(1));
            h.assertTrue(items.getStackInSlot(0).getCount() == 7, "logs left " + items.getStackInSlot(0).getCount());
            h.assertTrue((int) call(m, "processStatus") == 9, "status " + call(m, "processStatus") + " (no blade is 9)");
            h.succeed();
        });
    }
}
