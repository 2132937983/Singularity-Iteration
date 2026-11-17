package example.sicore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.7: the pump's survey (buckets in the pool, a pool that refills itself), the keep level with the comparator
 * output, and the discharge mode. Common classes are reached by reflection (core must not depend on common).
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round1157GameTests {
    private Round1157GameTests() {}

    static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    private static final BlockPos PUMP = new BlockPos(3, 4, 3);

    /** A stone basin 3 x 3 one layer deep under (3, 3, 3), filled with {@code fill} (air leaves it empty). */
    private static void basin(GameTestHelper h, BlockState fill) {
        for (int x = 1; x <= 5; x++) for (int z = 1; z <= 5; z++) {
            h.setBlock(new BlockPos(x, 2, z), Blocks.STONE);
            boolean inside = x >= 2 && x <= 4 && z >= 2 && z <= 4;
            h.setBlock(new BlockPos(x, 3, z), inside ? fill : Blocks.STONE.defaultBlockState());
        }
    }

    /** A pump above the basin facing down, kept powered. */
    private static BlockEntity pump(GameTestHelper h) {
        var block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "producer/block_pump_elc"));
        h.setBlock(PUMP, block.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.DOWN));
        BlockEntity pump = h.getBlockEntity(PUMP);
        h.onEachTick(() -> {
            var storage = call(pump, "getEnergyStorageInternal");
            call(storage, "setStored", 20L);
        });
        return pump;
    }

    private static int count(GameTestHelper h, net.minecraft.world.level.material.Fluid fluid) {
        int n = 0;
        for (int x = 2; x <= 4; x++) for (int z = 2; z <= 4; z++) {
            var fs = h.getBlockState(new BlockPos(x, 3, z)).getFluidState();
            if (fs.isSource() && fs.getType().isSame(fluid)) n++;
        }
        return n;
    }

    /** Nine lava sources: the survey counts nine buckets, lava does not refill, the comparator reads full. */
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void pumpSurveysAPool(GameTestHelper h) {
        basin(h, Blocks.LAVA.defaultBlockState());
        BlockEntity pump = pump(h);
        call(pump, "handleButton", 3);                        // keep 1 bucket: the pump surveys but takes almost nothing yet
        h.succeedWhen(() -> {
            Object survey = call(pump, "getSurvey");
            h.assertTrue((boolean) call(survey, "hasResult"), "no survey yet");
            int counted = (int) call(survey, "count");
            h.assertTrue(counted >= 8 && counted <= 9, "the pool holds 9 buckets (8 once one is taken), counted " + counted);
            h.assertTrue(!(boolean) call(survey, "infinite"), "lava does not refill");
            h.assertTrue((int) call(pump, "comparatorSignal") >= 14, "a full pool reads 14-15");
        });
    }

    /** Nine water sources over stone refill when one is taken: the pool is infinite and never runs dry. */
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void pumpSeesInfiniteWater(GameTestHelper h) {
        basin(h, Blocks.WATER.defaultBlockState());
        BlockEntity pump = pump(h);
        h.succeedWhen(() -> {
            Object survey = call(pump, "getSurvey");
            h.assertTrue((boolean) call(survey, "hasResult"), "no survey yet");
            h.assertTrue((boolean) call(survey, "infinite"), "a 3 x 3 water pool over stone refills itself");
            h.assertTrue((int) call(pump, "comparatorSignal") == 15, "an infinite pool reads 15");
        });
    }

    /** Keep 6 of 9 lava buckets: the pump stops at 6, and the comparator reads 6 / 9 of 15 = 10. */
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void pumpKeepsALevel(GameTestHelper h) {
        basin(h, Blocks.LAVA.defaultBlockState());
        BlockEntity pump = pump(h);
        for (int i = 0; i < 6; i++) call(pump, "handleButton", 3);
        h.assertTrue((int) call(pump, "getKeepBuckets") == 6, "keep was set to 6");
        h.succeedWhen(() -> {
            h.assertTrue(count(h, Fluids.LAVA) == 6, "6 lava sources left, found " + count(h, Fluids.LAVA));
            Object survey = call(pump, "getSurvey");
            h.assertTrue((int) call(survey, "count") == 6, "the survey follows the pool down to 6");
            h.assertTrue((int) call(pump, "comparatorSignal") == 10, "6 of 9 reads 10, got " + call(pump, "comparatorSignal"));
        });
    }

    /** Discharge: 3 buckets of lava piped in are poured into the empty basin, lowest and nearest first. */
    @GameTest(template = "empty", timeoutTicks = 300)
    public static void pumpDischargesIntoABasin(GameTestHelper h) {
        basin(h, Blocks.AIR.defaultBlockState());
        BlockEntity pump = pump(h);
        call(pump, "handleButton", 0);                        // discharge
        call(pump, "handleButton", 8);                        // target one below the pump: the basin layer
        h.assertTrue((boolean) call(pump, "discharging"), "the pump discharges");
        h.assertTrue((int) call(pump, "getTargetOffset") == -1, "target is the layer below the pump");
        IFluidHandler port = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(PUMP), Direction.UP);
        h.assertTrue(port != null, "the pump has a fluid port");
        int accepted = port.fill(new FluidStack(Fluids.LAVA, 3000), IFluidHandler.FluidAction.EXECUTE);
        h.assertTrue(accepted == 3000, "a discharging pump takes fluid from pipes, took " + accepted);
        h.assertTrue(port.drain(1000, IFluidHandler.FluidAction.SIMULATE).isEmpty(), "and gives none back");
        h.succeedWhen(() -> {
            h.assertTrue(count(h, Fluids.LAVA) == 3, "3 lava sources poured, found " + count(h, Fluids.LAVA));
            h.assertTrue((int) call(pump, "getFluidAmount") == 0, "the tank is empty");
        });
    }
}
