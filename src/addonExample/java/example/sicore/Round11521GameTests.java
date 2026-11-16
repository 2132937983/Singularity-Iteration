package example.sicore;

import com.miophas.singularity_iteration.core.api.reactor.IReactorController;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.21: the reactor's SCRAM (manual trip, the setpoint, reset only below it, saved) and the control room's record of
 * the last cycle (heat made, hull record).
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11521GameTests {
    private Round11521GameTests() {}

    static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    private static IReactorController reactor(GameTestHelper h, BlockPos pos) {
        h.setBlock(pos, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "generator/block_nuclear_reactor_generator")));
        IReactorController reactor = (IReactorController) h.getBlockEntity(pos);
        h.assertTrue(reactor.getItemHandler().insertItem(10, new ItemStack(BuiltInRegistries.ITEM.get(
            ResourceLocation.fromNamespaceAndPath("mio_icif", "reactor/item_reactor_uranium_simple"))), false).isEmpty(), "Fuel rejected");
        h.setBlock(pos.above(), Blocks.REDSTONE_BLOCK);
        return reactor;
    }

    /** SCRAM stops the fuel from the next cycle and says so; a reset (below the setpoint) runs it again. */
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void scramStopsTheFuelUntilReset(GameTestHelper h) {
        var r = reactor(h, new BlockPos(1, 1, 1));
        h.runAfterDelay(25, () -> {
            h.assertTrue(r.getCurrentEnergyGeneration() == 5, "running: " + r.getCurrentEnergyGeneration());
            call(r, "scram");
            h.runAfterDelay(21, () -> {
                h.assertTrue(r.getCurrentEnergyGeneration() == 0, "tripped still makes " + r.getCurrentEnergyGeneration());
                h.assertTrue((int) call(r, "statusCode") == 5, "status " + call(r, "statusCode"));
                h.assertTrue((boolean) call(r, "resetTrip"), "reset below the setpoint");
                h.runAfterDelay(21, () -> {
                    h.assertTrue(r.getCurrentEnergyGeneration() == 5, "after reset: " + r.getCurrentEnergyGeneration());
                    h.succeed();
                });
            });
        });
    }

    /** At or above the setpoint a cycle trips it; a reset is refused until the hull is below it; both are saved. */
    @GameTest(template = "empty", timeoutTicks = 60)
    public static void setpointTripsAndHoldsAboveIt(GameTestHelper h) {
        BlockPos pos = new BlockPos(1, 1, 1);
        var r = reactor(h, pos);
        call(r, "setScramSetpoint", 50);
        Object heat = call(r, "getHeatStorage");
        call(heat, "setHeat", 6000L);
        h.runAfterDelay(25, () -> {
            h.assertTrue((boolean) call(r, "isTripped"), "6,000 of 10,000 HU at a 50 % setpoint must trip");
            h.assertTrue(!(boolean) call(r, "resetTrip"), "reset above the setpoint must be refused");
            var be = h.getBlockEntity(pos);
            CompoundTag tag = be.saveWithoutMetadata(h.getLevel().registryAccess());
            h.assertTrue(tag.getInt("scex_scram_setpoint") == 50 && tag.getBoolean("scex_scram_tripped"), "saved");
            call(heat, "setHeat", 1000L);
            h.assertTrue((boolean) call(r, "resetTrip"), "reset below the setpoint");
            h.succeed();
        });
    }

    /** One uranium rod alone: the last cycle made 4 HU, all into the hull, and the record's newest entry is that hull. */
    @GameTest(template = "empty", timeoutTicks = 60)
    public static void controlRoomRecordsTheCycle(GameTestHelper h) {
        var r = reactor(h, new BlockPos(1, 1, 1));
        h.runAfterDelay(25, () -> {
            long made = (long) call(r, "getBalanceGenerated"), hull = (long) call(r, "getBalanceHull");
            h.assertTrue(made == 4 && hull == 4, "made " + made + ", hull change " + hull);
            int newest = (int) call(r, "hullRecord", 59), eu = (int) call(r, "euRecord", 59);
            h.assertTrue(newest == (int) (r.getCurrentHeat() * 1000 / r.getMaxHeat()) && eu == 5, "record " + newest + " / " + eu);
            h.succeed();
        });
    }
}
