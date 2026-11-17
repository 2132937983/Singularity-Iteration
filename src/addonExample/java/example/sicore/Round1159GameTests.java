package example.sicore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.9: the wind machines: the facing factor, Jensen's wake, the power curve with storm protection, a turbine in
 * front of another as a wake instead of a block, and the wind generator on the dimension's wind field.
 * Common classes are reached by reflection (core must not depend on common).
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round1159GameTests {
    private Round1159GameTests() {}

    static Class<?> common(String name) { return Round33GameTests.common(name); }
    static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    private static Object field(Object target, String name) {
        try {
            for (Class<?> k = target.getClass(); k != null; k = k.getSuperclass()) {
                try { var f = k.getDeclaredField(name); f.setAccessible(true); return f.get(target); }
                catch (NoSuchFieldException next) { /* superclass */ }
            }
            throw new AssertionError("no field " + name);
        } catch (IllegalAccessException e) { throw new AssertionError(name, e); }
    }

    private static final String PHYSICS = "blockentity.kuentity.kugenerator.WindPhysics";

    /** Square to the wind (front or back) takes it all, side-on 70 %, up or down always 70 %. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void windFacingFactor(GameTestHelper h) {
        Class<?> p = common(PHYSICS);
        h.assertTrue(Math.abs((double) call(p, "facingFactor", 0, 0) - 1.0) < 1e-9, "head-on is 100 %");
        h.assertTrue(Math.abs((double) call(p, "facingFactor", 180, 0) - 1.0) < 1e-9, "from behind is 100 %");
        h.assertTrue(Math.abs((double) call(p, "facingFactor", 90, 0) - 0.7) < 1e-9, "side-on is 70 %");
        h.assertTrue(Math.abs((double) call(p, "facingFactor", 45, -1) - 0.7) < 1e-9, "a vertical rotor is 70 %");
        h.succeed();
    }

    /** Jensen: 2/3 short right behind a rotor, about 32 % three diameters on. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void windWakeDeficit(GameTestHelper h) {
        Class<?> p = common(PHYSICS);
        double at0 = (double) call(p, "wakeDeficit", 0.0, 11.0), at3d = (double) call(p, "wakeDeficit", 33.0, 11.0);
        h.assertTrue(Math.abs(at0 - 2.0 / 3.0) < 1e-9, "right behind: 2/3, got " + at0);
        h.assertTrue(Math.abs(at3d - 0.317) < 0.002, "three diameters on: about 0.317, got " + at3d);
        h.succeed();
    }

    /** Carbon rotor (20..110 MCU, efficiency 1): linear in between, feathered above with storm protection, overloaded without. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void windPowerCurve(GameTestHelper h) {
        Class<?> p = common(PHYSICS);
        h.assertTrue((int) call(p, "output", 10.0, 20, 110, 1.0F, true) == 0, "below cut-in: nothing");
        h.assertTrue((int) call(p, "output", 50.0, 20, 110, 1.0F, true) == 500, "50 MCU: 500 KU/t");
        h.assertTrue((int) call(p, "output", 120.0, 20, 110, 1.0F, true) == 0, "above cut-out with storm protection: feathered");
        h.assertTrue((int) call(p, "output", 120.0, 20, 110, 1.0F, false) == 1200, "above cut-out without: keeps going");
        h.succeed();
    }

    /** Two turbines in a row: the rear one is in the wake of the front one (wind cut, not blocked); new machines are protected. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void windTurbineInFrontIsAWake(GameTestHelper h) {
        var block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "kugenerator/block_wind_kinetic_generator"));
        var rotor = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "rotors/item_carbon_rotor"));
        BlockPos rear = new BlockPos(1, 2, 7), front = new BlockPos(1, 2, 2);
        h.setBlock(rear, block.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.NORTH));
        h.setBlock(front, block.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.NORTH));
        BlockEntity r = h.getBlockEntity(rear), f = h.getBlockEntity(front);
        call(r, "setRotorStack", new ItemStack(rotor));
        call(f, "setRotorStack", new ItemStack(rotor));
        h.assertTrue((boolean) call(r, "hasStormProtection"), "a newly placed turbine has storm protection on");
        call(r, "updateWindStrength");
        h.assertTrue((int) field(r, "lastObstruction") >= 0, "the rear turbine is not blocked");
        double wake = (double) call(r, "getWakeFactor");
        h.assertTrue(wake > 0.3 && wake < 0.9, "the rear turbine is in the wake (factor " + wake + ")");
        h.succeed();
    }

    /** The wind generator takes the dimension's wind field: the same base wind as the kinetic generator and the meter. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void windGeneratorUsesTheWindField(GameTestHelper h) throws Exception {
        BlockPos at = new BlockPos(1, 2, 1);
        h.setBlock(at, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "generator/block_wind_generator")));
        BlockEntity g = h.getBlockEntity(at);
        call(g, "updateWindStrength");
        Class<?> sim = Class.forName("com.miophas.singularity_iteration.core.runtime.world.WindSim");
        Object field = call(sim, "get", h.getLevel());
        int expected = (int) call(field, "getWindStrength");
        h.assertTrue((int) call(g, "getWindStrength") == expected, "generator wind " + call(g, "getWindStrength") + " vs field " + expected);
        h.assertTrue((int) call(g, "baseHeight") == h.getLevel().getSeaLevel() + 1, "output starts one above sea level");
        h.succeed();
    }
}
