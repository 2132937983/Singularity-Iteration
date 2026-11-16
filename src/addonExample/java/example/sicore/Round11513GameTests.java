package example.sicore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.13: the generator framework: every generator block gives a comparator signal, one dispatch table, the rated
 * output the energy terminal reads, the advanced drop and experience generators as subclasses of the basic ones, and
 * the water generator filled by pipes. Common classes are reached by reflection (core must not depend on common).
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11513GameTests {
    private Round11513GameTests() {}

    static Class<?> common(String name) { return Round33GameTests.common(name); }
    static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    private static BlockEntity place(GameTestHelper h, BlockPos at, String id) {
        h.setBlock(at, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", id)));
        return h.getBlockEntity(at);
    }

    private static int analog(GameTestHelper h, BlockPos at) {
        var pos = h.absolutePos(at);
        var state = h.getLevel().getBlockState(pos);
        return state.hasAnalogOutputSignal() ? state.getAnalogOutputSignal(h.getLevel(), pos) : -1;
    }

    /** A generator with half its storage filled gives 8; an empty one 0 (generators had no comparator output). */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void generatorsGiveComparatorSignals(GameTestHelper h) {
        BlockPos at = new BlockPos(1, 2, 1);
        BlockEntity rtg = place(h, at, "generator/block_rt_generator");
        h.assertTrue(analog(h, at) == 0, "empty: " + analog(h, at));
        Object storage = call(rtg, "getEnergyStorageInternal");
        call(storage, "setStored", (long) call(storage, "getCapacity") / 2);
        h.assertTrue(analog(h, at) == 8, "half full: " + analog(h, at));
        BlockPos fluid = new BlockPos(3, 2, 1);
        BlockEntity semifluid = place(h, fluid, "generator/block_semifluid_generator");
        var tank = (IFluidHandler) call(semifluid, "getFuelHandler");
        tank.fill(new FluidStack(BuiltInRegistries.FLUID.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "biogas")), tank.getTankCapacity(0) / 2),
            IFluidHandler.FluidAction.EXECUTE);
        h.assertTrue(analog(h, fluid) == 8, "fuel generator half full: " + analog(h, fluid));
        h.succeed();
    }

    /** Fuel generators are dispatchable, solar is must-run: from one table. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void dispatchClassesFromOneTable(GameTestHelper h) {
        String thermal = String.valueOf(call(place(h, new BlockPos(1, 2, 1), "generator/block_thermal_generator"), "dispatchClass"));
        String diesel = String.valueOf(call(place(h, new BlockPos(3, 2, 1), "generator/block_diesel_generator"), "dispatchClass"));
        String solar = String.valueOf(call(place(h, new BlockPos(5, 2, 1), "generator/block_solar_generator"), "dispatchClass"));
        h.assertTrue(thermal.equals("DISPATCHABLE") && diesel.equals("DISPATCHABLE") && solar.equals("MUST_RUN"),
            "thermal " + thermal + ", diesel " + diesel + ", solar " + solar);
        h.succeed();
    }

    /** Rated output: an RTG without pellets and an incomplete geomagnetic generator are 0; a photon panel its 512 EU/t peak. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void ratedOutputs(GameTestHelper h) {
        long rtg = (long) call(place(h, new BlockPos(1, 2, 1), "generator/block_rt_generator"), "ratedOutput");
        long geo = (long) call(place(h, new BlockPos(3, 2, 1), "generator/block_geomagnetic_generator"), "ratedOutput");
        long photon = (long) call(place(h, new BlockPos(5, 2, 1), "generator/block_photon_resonance_solar_generator"), "ratedOutput");
        h.assertTrue(rtg == 0 && geo == 0 && photon == 512, "rtg " + rtg + ", geomagnetic " + geo + ", photon " + photon);
        h.succeed();
    }

    /** The advanced drop and experience generators are the basic classes with their own numbers. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void advancedWorldFuelGeneratorsExtendTheBasicOnes(GameTestHelper h) {
        BlockEntity drop = place(h, new BlockPos(1, 2, 1), "generator/block_advanced_drop_generator");
        BlockEntity xp = place(h, new BlockPos(3, 2, 1), "generator/block_advanced_experience_generator");
        h.assertTrue(common("blockentity.generator.mio_icif_drop_generator").isInstance(drop), "advanced drop is not a drop generator");
        h.assertTrue(common("blockentity.generator.mio_icif_experience_generator").isInstance(xp), "advanced experience is not an experience generator");
        h.assertTrue((long) call(call(drop, "getEnergyStorageInternal"), "getCapacity") == 100_000L, "advanced drop keeps its 100k storage");
        h.assertTrue((long) call(xp, "getPowerOutput") == 320L, "advanced experience keeps 320 EU/t");
        h.succeed();
    }

    /** Pipes fill the water generator (it took water buckets only). */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void waterGeneratorTakesPipedWater(GameTestHelper h) {
        BlockPos at = new BlockPos(1, 2, 1);
        place(h, at, "generator/block_water_generator");
        IFluidHandler handler = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(at), Direction.UP);
        h.assertTrue(handler != null, "no fluid handler");
        h.assertTrue(handler.fill(new FluidStack(Fluids.WATER, 1500), IFluidHandler.FluidAction.EXECUTE) == 1500, "water refused");
        h.assertTrue(handler.fill(new FluidStack(Fluids.LAVA, 100), IFluidHandler.FluidAction.EXECUTE) == 0, "lava taken");
        h.succeed();
    }
}
