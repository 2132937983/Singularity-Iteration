package example.sicore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.11: generator fixes: epic drops worth more than rare ones, the quantum generator generates, the advanced
 * Stirling generator is an endpoint of the independent network, the geomagnetic generator works below Y 0, a drop
 * generator with a full storage leaves the items alone, and a generator's removeItem goes through its slot.
 * Common classes are reached by reflection (core must not depend on common).
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11511GameTests {
    private Round11511GameTests() {}

    static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    private static BlockEntity place(GameTestHelper h, BlockPos at, String id) {
        h.setBlock(at, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", id)));
        return h.getBlockEntity(at);
    }

    private static Object storage(BlockEntity be) { return call(be, "getEnergyStorageInternal"); }

    /** Epic items are worth more than rare ones (5000 and 10000 were the other way round). */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void dropGeneratorRanksEpicAboveRare(GameTestHelper h) {
        for (String id : new String[]{"generator/block_drop_generator", "generator/block_advanced_drop_generator"}) {
            BlockEntity be = place(h, new BlockPos(1, 2, 1), id);
            int rare = (int) call(be, "getItemValue", Rarity.RARE), epic = (int) call(be, "getItemValue", Rarity.EPIC);
            h.assertTrue(epic > rare && rare > (int) call(be, "getItemValue", Rarity.UNCOMMON), id + ": rare " + rare + ", epic " + epic);
        }
        h.succeed();
    }

    /** The quantum generator makes energy on its own (its switch could never be turned on). */
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void quantumGeneratorGenerates(GameTestHelper h) {
        BlockEntity be = place(h, new BlockPos(1, 2, 1), "generator/block_quantum_generator");
        h.assertTrue((boolean) call(be, "isActive"), "on when placed");
        h.succeedWhen(() -> h.assertTrue((long) call(storage(be), "getAmount") > 0, "no energy yet"));
    }

    /** The advanced Stirling generator is an endpoint of the independent network (it was only ever on the old grid). */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void advancedStirlingJoinsTheNetwork(GameTestHelper h) throws Exception {
        BlockEntity be = place(h, new BlockPos(1, 2, 1), "generator/block_advanced_stirling_generator");
        Class<?> demand = Class.forName("com.miophas.singularity_iteration.core.runtime.energy.DemandEnergySource");
        h.assertTrue(demand.isInstance(be), "not a network source");
        boolean independent = Boolean.parseBoolean(System.getProperty("scex.independent.energy", "true"));
        h.assertTrue((boolean) call(call(be, "ownedEnergy"), "scexNetworkControlled") == independent, "network ownership follows the energy mode");
        h.succeed();
    }

    /** Below Y 0 the field is measured from the bottom of the world (it went negative and gave nothing). */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void geomagneticWorksBelowZero(GameTestHelper h) {
        BlockPos at = new BlockPos(1, 2, 1);
        BlockEntity be = place(h, at, "generator/block_geomagnetic_generator");
        int y = h.absolutePos(at).getY();
        float source = (float) call(be, "getMagneticSource");
        h.assertTrue(y >= 0 || source > 0, "at Y " + y + " the source is " + source);
        h.assertTrue((long) call(be, "getPowerOutput") == 0, "an idle generator reports no output (it showed its 30720 rating)");
        h.succeed();
    }

    /** A drop generator with a full storage leaves the items around it alone (it ate them for nothing). */
    @GameTest(template = "empty", timeoutTicks = 60)
    public static void fullDropGeneratorLeavesItems(GameTestHelper h) {
        BlockPos at = new BlockPos(2, 2, 2);
        BlockEntity be = place(h, at, "generator/block_drop_generator");
        call(storage(be), "setStored", (long) call(storage(be), "getCapacity"));
        var abs = h.absolutePos(at);
        ItemEntity item = new ItemEntity(h.getLevel(), abs.getX() + 0.5, abs.getY() + 0.5, abs.getZ() + 0.5, new ItemStack(Items.COBBLESTONE, 8));
        item.setDeltaMovement(0, 0, 0);
        item.setNoGravity(true);
        h.getLevel().addFreshEntity(item);
        h.runAfterDelay(40, () -> {
            call(storage(be), "setStored", (long) call(storage(be), "getCapacity"));
            h.assertTrue(item.isAlive() && item.getItem().getCount() == 8, "items taken while full: " + item.getItem().getCount() + " left");
            h.succeed();
        });
    }

    /** removeItem goes through the slot (it shrank the live stack, bypassing the handler). */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void generatorRemoveItemWritesTheSlot(GameTestHelper h) {
        BlockEntity be = place(h, new BlockPos(1, 2, 1), "generator/block_thermal_generator");
        Object handler = call(be, "getItemHandler");
        call(handler, "setStackInSlot", 0, new ItemStack(Items.COAL, 3));
        ItemStack before = (ItemStack) call(handler, "getStackInSlot", 0);
        ItemStack taken = (ItemStack) call(be, "removeItem", 0, 1);
        ItemStack after = (ItemStack) call(handler, "getStackInSlot", 0);
        h.assertTrue(taken.getCount() == 1 && after.getCount() == 2, "took " + taken.getCount() + ", left " + after.getCount());
        h.assertTrue(before.getCount() == 3, "the old stack object is not changed in place");
        h.succeed();
    }
}
