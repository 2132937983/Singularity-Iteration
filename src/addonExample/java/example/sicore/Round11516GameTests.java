package example.sicore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.16: the steam chain's enthalpy model (SteamPhysics), the steam turbine with and without a condenser taking its
 * exhaust, and the steam kinetic generator's condenser gain. Common classes are reached by reflection.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11516GameTests {
    private Round11516GameTests() {}

    static Class<?> common(String name) { return Round33GameTests.common(name); }
    static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    private static final String SP = "energy.SteamPhysics";

    private static Object constant(String type, String name) {
        try {
            var f = common(type).getDeclaredField(name);
            f.setAccessible(true);
            return f.get(null);
        } catch (ReflectiveOperationException e) { throw new AssertionError(type + "." + name, e); }
    }

    private static double d(Object o) { return ((Number) o).doubleValue(); }

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }

    private static BlockEntity place(GameTestHelper h, BlockPos at, String id) {
        h.setBlock(at, BuiltInRegistries.BLOCK.get(id(id)));
        return h.getBlockEntity(at);
    }

    private static double euPerMb(Object state, double back) {
        return d(call(common(SP), "euPerMb", state, back, d(constant(SP, "TURBINE_EFFICIENCY"))));
    }

    /** Saturation 180 °C at 10 bar; steam into the air 0.5 EU/mB, condensed about 0.8; superheated about twice, more condensed. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void steamTableValues(GameTestHelper h) {
        Object steam = constant(SP, "STEAM"), hot = constant(SP, "SUPERHEATED");
        double tsat = d(call(common(SP), "tsat", 10.0));
        double sv = euPerMb(steam, 1.0), sc = euPerMb(steam, 0.1), hv = euPerMb(hot, 1.0), hc = euPerMb(hot, 0.1);
        h.assertTrue(Math.abs(tsat - 177.8) < 1, "tsat(10 bar) " + tsat);
        h.assertTrue(Math.abs(sv - 0.5) < 1e-9, "steam vented " + sv);
        h.assertTrue(sc > 0.75 && sc < 0.85, "steam condensed " + sc);
        h.assertTrue(hv > 0.95 && hv < 1.1 && hc > hv * 1.2, "superheated vented " + hv + ", condensed " + hc);
        double q = d(call(common(SP), "exhaustQuality", steam, 1.0, 0.75));
        h.assertTrue(q > 0.88 && q < 1, "steam exhaust dryness into the air " + q);
        h.succeed();
    }

    /** Alone the turbine gives 5 EU per 10 mB of steam; with a condenser beside it 8, and its exhaust goes into the condenser. */
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void condenserRaisesTurbineOutput(GameTestHelper h) {
        BlockEntity turbine = place(h, new BlockPos(1, 2, 1), "generator/block_steam_turbine");
        var steam = BuiltInRegistries.FLUID.get(id("steam"));
        call(turbine, "beforeBurn");
        long alone = (long) call(call(turbine, "fuelValue", steam), "energyPerMb");
        h.assertTrue(alone == 5, "vented: " + alone + " EU per 10 mB");
        place(h, new BlockPos(2, 2, 1), "producer/block_condenser");
        call(turbine, "beforeBurn");
        long condensed = (long) call(call(turbine, "fuelValue", steam), "energyPerMb");
        h.assertTrue(condensed == 8, "condensed: " + condensed + " EU per 10 mB");
        IFluidHandler tank;
        try {
            var f = turbine.getClass().getSuperclass().getDeclaredField("fuelTank");
            f.setAccessible(true);
            tank = (IFluidHandler) f.get(turbine);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
        tank.fill(new FluidStack(steam, 4000), IFluidHandler.FluidAction.EXECUTE);
        Object data = call(turbine, "getTurbineData");
        h.succeedWhen(() -> {
            int toCondenser = (int) call(data, "get", 6), vented = (int) call(data, "get", 7);
            h.assertTrue(toCondenser > 0 && vented == 0, "exhaust: " + toCondenser + " mB/t to the condenser, " + vented + " vented");
        });
    }

    /** The kinetic turbine gives 2 KU/mB of steam vented and about 1.6 times that when a condenser takes the exhaust. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void condenserRaisesKineticTurbine(GameTestHelper h) {
        BlockEntity be = place(h, new BlockPos(1, 2, 1), "producer/block_steam_kinetic_generator");
        double vented = d(call(be, "kuPerMb", false)), hotVented = d(call(be, "kuPerMb", true));
        try {
            var f = be.getClass().getDeclaredField("exhaustCondensed");
            f.setAccessible(true);
            f.setBoolean(be, true);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
        double condensed = d(call(be, "kuPerMb", false)), hotCondensed = d(call(be, "kuPerMb", true));
        h.assertTrue(vented == 2 && hotVented == 4, "vented " + vented + " / " + hotVented);
        h.assertTrue(condensed / vented > 1.5 && condensed / vented < 1.7, "steam gain " + condensed / vented);
        h.assertTrue(hotCondensed / hotVented > 1.2 && hotCondensed / hotVented < 1.4, "superheated gain " + hotCondensed / hotVented);
        h.succeed();
    }
}
