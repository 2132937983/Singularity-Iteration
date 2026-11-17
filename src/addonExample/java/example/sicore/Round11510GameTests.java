package example.sicore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.10: the generator (thermal): the furnace warms up without changing the energy of a lump, the damper banks the
 * fire when the storage is full and opens again below 85 %, the comparator reads the fuel left, and old saves load as a
 * hot furnace. The ticks are driven directly. Common classes are reached by reflection (core must not depend on common).
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11510GameTests {
    private Round11510GameTests() {}

    static Class<?> common(String name) { return Round33GameTests.common(name); }
    static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    private static final String GEN = "blockentity.mio_icif_Thermal_Generator";

    private static BlockEntity place(GameTestHelper h, BlockPos at) {
        h.setBlock(at, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "generator/block_thermal_generator")));
        return h.getBlockEntity(at);
    }

    private static void tick(GameTestHelper h, BlockPos at, BlockEntity be, int n) {
        for (int i = 0; i < n; i++)
            call(common(GEN), "tickThermal", h.getLevel(), h.absolutePos(at), h.getLevel().getBlockState(h.absolutePos(at)), be);
    }

    private static void fuel(BlockEntity be, ItemStack stack) {
        call(call(be, "getItemHandler"), "setStackInSlot", 0, stack);
    }

    private static int fuelCount(BlockEntity be) {
        return ((net.neoforged.neoforge.items.IItemHandler) call(be, "getItemHandler")).getStackInSlot(0).getCount();
    }

    private static Object storage(BlockEntity be) { return call(be, "getEnergyStorageInternal"); }

    private static long stored(BlockEntity be) { return (long) call(storage(be), "getAmount"); }

    private static long capacity(BlockEntity be) { return (long) call(storage(be), "getCapacity"); }

    private static void setStored(BlockEntity be, long eu) { call(storage(be), "setStored", eu); }

    /** Cold start: full temperature after 200 ticks, about half the energy of a hot furnace, and the same EU per tick of fuel. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void thermalWarmUpKeepsTheEnergyOfALump(GameTestHelper h) {
        BlockPos at = new BlockPos(1, 2, 1);
        BlockEntity be = place(h, at);
        int per = (int) call(be, "getFuelBurnTime", new ItemStack(Items.COAL));
        fuel(be, new ItemStack(Items.COAL, 2));
        tick(h, at, be, 200);
        double heat = (double) call(be, "getHeat"), max = (double) call(be, "maxOutput");
        double burned = per - (double) call(be, "getBurnLeft");
        long e = stored(be);
        h.assertTrue(heat > 0.999, "full temperature after 200 ticks, got " + heat);
        h.assertTrue(fuelCount(be) == 1, "one lump taken, " + fuelCount(be) + " left");
        h.assertTrue(Math.abs(e - burned * max) <= 2, "EU " + e + " for " + burned + " fuel ticks at " + max + " EU/t");
        h.assertTrue(e < 200 * max * 0.6, "a cold start gives less in its first 10 s, got " + e);
        h.succeed();
    }

    /** Full storage: the damper shuts and the fuel waits (heat holds at the banked level); below 85 % it fires again. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void thermalDamperBanksTheFire(GameTestHelper h) {
        BlockPos at = new BlockPos(1, 2, 1);
        BlockEntity be = place(h, at);
        fuel(be, new ItemStack(Items.COAL, 3));
        tick(h, at, be, 220);                                                       // hot
        double left = (double) call(be, "getBurnLeft");
        h.assertTrue(left > 0 && fuelCount(be) == 2, "burning one lump");
        long cap = capacity(be);
        setStored(be, cap);
        tick(h, at, be, 400);
        h.assertTrue((boolean) call(be, "isBanked"), "full storage banks the fire");
        h.assertTrue(Math.abs((double) call(be, "getBurnLeft") - left) < 1e-6 && fuelCount(be) == 2, "no fuel burnt while banked");
        double heat = (double) call(be, "getHeat");
        h.assertTrue(heat >= 0.35 - 1e-9 && heat < 0.5, "a banked fire cools to its holding heat, got " + heat);
        setStored(be, (long) (cap * 0.9));
        tick(h, at, be, 1);
        h.assertTrue((boolean) call(be, "isBanked"), "still banked at 90 %");
        setStored(be, cap / 2);
        tick(h, at, be, 5);
        h.assertTrue(!(boolean) call(be, "isBanked") && (double) call(be, "getBurnLeft") < left, "open and burning again at 50 %");
        h.succeed();
    }

    /** Comparator: nothing without fuel, 1 for one lump, 15 for a full stack of coal. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void thermalComparatorReadsTheFuelLeft(GameTestHelper h) {
        BlockEntity be = place(h, new BlockPos(1, 2, 1));
        h.assertTrue((int) call(be, "comparatorSignal") == 0, "no fuel: 0");
        fuel(be, new ItemStack(Items.COAL));
        h.assertTrue((int) call(be, "comparatorSignal") == 1, "one lump: 1");
        fuel(be, new ItemStack(Items.COAL, 64));
        h.assertTrue((int) call(be, "comparatorSignal") == 15, "a full stack: " + call(be, "comparatorSignal"));
        h.succeed();
    }

    /** A save from before 0.1.11.10 with a lump burning loads as a hot furnace with that burn time left. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void thermalOldSaveLoadsHot(GameTestHelper h) {
        BlockEntity be = place(h, new BlockPos(1, 2, 1));
        CompoundTag tag = new CompoundTag();
        tag.putInt("BurnTime", 500);
        tag.putInt("BurnDuration", 1600);
        call(be, "loadAdditional", tag, h.getLevel().registryAccess());
        h.assertTrue((double) call(be, "getHeat") == 1.0, "hot");
        h.assertTrue((double) call(be, "getBurnLeft") == 500.0, "500 ticks left");
        h.assertTrue(!(boolean) call(be, "isBanked"), "damper open");
        h.succeed();
    }
}
