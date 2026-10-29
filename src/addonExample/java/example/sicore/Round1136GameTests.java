package example.sicore;

import com.miophas.singularity_iteration.core.api.reactor.IReactorController;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests of 0.1.10.36. Phase 0: a breeder that finishes in a reactor (lithium cell -> tritium cell, isotope rod ->
 * re-enriched uranium) no longer stalls the reactor; the tritium cell stays in its slot (IC2 isUsefulItem), re-enriched
 * uranium is thrown out at the start of the next cycle. Internals are reached by reflection.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round1136GameTests {
    private Round1136GameTests() {}

    private static final BlockPos REACTOR = new BlockPos(2, 1, 2);

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }

    private static ItemStack item(String path) { return new ItemStack(BuiltInRegistries.ITEM.get(id(path))); }

    private static ItemStack breeder(GameTestHelper h, String suffix, int stored) {
        CompoundTag tag = (CompoundTag) item("reactor/item_reactor_" + suffix).save(h.getLevel().registryAccess());
        CompoundTag components = tag.getCompound("components");
        CompoundTag data = new CompoundTag();
        data.putInt("stored_value", stored); data.putInt("max_value", 10000);
        components.put("mio_icif:reactor_component_data", data); tag.put("components", components);
        return ItemStack.parse(h.getLevel().registryAccess(), tag).orElseThrow();
    }

    private static int fuelRemaining(GameTestHelper h, ItemStack stack) {
        return ((CompoundTag) stack.save(h.getLevel().registryAccess())).getCompound("components")
            .getCompound("mio_icif:fuel_rod_durability").getInt("remaining_uses");
    }

    /** A running reactor: one uranium rod (slot 10) next to a breeder one gain short of its threshold (slot 11). */
    private static IReactorController reactor(GameTestHelper h, String breeder) {
        h.setBlock(REACTOR, BuiltInRegistries.BLOCK.get(id("generator/block_nuclear_reactor_generator")));
        BlockEntity entity = h.getBlockEntity(REACTOR);
        IReactorController reactor = (IReactorController) entity;
        h.assertTrue(reactor.getItemHandler().insertItem(10, item("reactor/item_reactor_uranium_simple"), false).isEmpty(), "Fuel rejected");
        h.assertTrue(reactor.getItemHandler().insertItem(11, breeder(h, breeder, 9999), false).isEmpty(), "Breeder rejected");
        // hull 3000 HU: a lithium cell gains 3000 / 3000 = 1 per pulse, so the first cycle finishes it (IC2 ItemReactorLithiumCell)
        ArmoryServiceGameTests.call(ArmoryServiceGameTests.call(entity, "getHeatStorage"), "setHeat", 3000L);
        h.setBlock(REACTOR.east(), Blocks.REDSTONE_BLOCK);
        return reactor;
    }

    @GameTest(batch = "round1136_breeding", template = "empty", timeoutTicks = 80)
    public static void finishedLithiumCellStaysAsTritiumAndTheReactorKeepsRunning(GameTestHelper h) {
        IReactorController reactor = reactor(h, "lithium_cell");
        h.runAfterDelay(25, () -> {
            var slot = reactor.getItemHandler().getStackInSlot(11);
            h.assertTrue(slot.is(BuiltInRegistries.ITEM.get(id("normal/item_tritium_cell"))), "Lithium cell did not become a tritium cell: " + slot);
            h.assertTrue(fuelRemaining(h, reactor.getItemHandler().getStackInSlot(10)) == 19999, "The breeding cycle did not commit");
            h.runAfterDelay(20, () -> {
                h.assertTrue(reactor.getItemHandler().getStackInSlot(11).is(BuiltInRegistries.ITEM.get(id("normal/item_tritium_cell"))),
                    "The tritium cell must stay in the reactor");
                h.assertTrue(fuelRemaining(h, reactor.getItemHandler().getStackInSlot(10)) == 19998, "The reactor stalled after breeding");
                h.succeed();
            });
        });
    }

    @GameTest(batch = "round1136_breeding", template = "empty", timeoutTicks = 80)
    public static void finishedIsotopeRodIsThrownOutAndTheReactorKeepsRunning(GameTestHelper h) {
        IReactorController reactor = reactor(h, "isotope_rod");
        h.runAfterDelay(25, () -> {
            h.assertTrue(reactor.getItemHandler().getStackInSlot(11).is(BuiltInRegistries.ITEM.get(id("normal/item_re_enriched_uranium"))),
                "Isotope rod did not become re-enriched uranium");
            h.runAfterDelay(20, () -> {
                h.assertTrue(reactor.getItemHandler().getStackInSlot(11).isEmpty(), "Re-enriched uranium must leave the reactor");
                var near = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(REACTOR)).inflate(2),
                    e -> e.getItem().is(BuiltInRegistries.ITEM.get(id("normal/item_re_enriched_uranium"))));
                h.assertTrue(!near.isEmpty(), "Re-enriched uranium was not dropped next to the reactor");
                h.assertTrue(fuelRemaining(h, reactor.getItemHandler().getStackInSlot(10)) == 19998, "The reactor stalled after breeding");
                h.succeed();
            });
        });
    }
}
