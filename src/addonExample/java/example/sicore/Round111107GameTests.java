package example.sicore;

import java.util.OptionalDouble;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.107 (processing overhaul M0): the material table (Bond work indices from data packs), the process cost of an
 * operation, and the two macerators reporting their unchanged cost (600 EU at 2 EU/t; 750 EU at 5 EU/t). (The mod's
 * common classes by reflection: addon examples do not compile against them.)
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111107GameTests {
    private Round111107GameTests() {}

    private static Class<?> common(String name) { return Round33GameTests.common(name); }
    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    private static Class<?> cost() {
        try { return Class.forName("com.miophas.singularity_iteration.core.api.machine.ProcessCost"); }
        catch (ClassNotFoundException e) { throw new AssertionError(e); }
    }

    /** Items and tags resolve; an item without an entry has none and falls back to the default (12.7, hematite). */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void materialTableLoads(GameTestHelper h) {
        Class<?> m = common("processing.SiMaterials");
        h.assertTrue((int) call(m, "generation") > 0, "the material table was never loaded");
        Object[][] expected = {{Items.GLASS, 3.1}, {Items.CLAY_BALL, 7.1}, {Items.COAL, 11.4}, {Items.IRON_ORE, 12.7},
            {Items.DEEPSLATE_COPPER_ORE, 13.1}, {Items.GOLD_ORE, 14.8}, {Items.COBBLESTONE, 14.4}, {Items.GRAVEL, 25.2}, {Items.NETHERRACK, 8.0}};
        for (Object[] row : expected) {
            ItemStack stack = new ItemStack((net.minecraft.world.item.Item) row[0]);
            OptionalDouble wi = (OptionalDouble) call(m, "workIndex", stack);
            h.assertTrue(wi.isPresent() && Math.abs(wi.getAsDouble() - (double) row[1]) < 1e-9, stack + ": work index " + wi + ", expected " + row[1]);
        }
        h.assertTrue(((OptionalDouble) call(m, "workIndex", new ItemStack(Items.STICK))).isEmpty(), "a stick has no work index");
        h.assertTrue((double) call(m, "workIndexOrDefault", new ItemStack(Items.STICK)) == 12.7, "the default is 12.7");
        h.assertTrue(((OptionalDouble) call(m, "workIndex", ItemStack.EMPTY)).isEmpty(), "an empty stack has none");
        h.succeed();
    }

    /** Energy and power give the time: ceil(energy / power); a fixed time and draw give ticks × power. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void processCostArithmetic(GameTestHelper h) {
        Class<?> c = cost();
        Object fixed = call(c, "fixed", 300, 2L);
        h.assertTrue((long) call(fixed, "energy") == 600 && (long) call(fixed, "power") == 2 && (int) call(fixed, "ticks") == 300, "fixed " + fixed);
        Object odd;
        try { odd = c.getConstructors()[0].newInstance(601L, 2L); } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
        h.assertTrue((int) call(odd, "ticks") == 301, "601 EU at 2 EU/t: " + call(odd, "ticks") + " ticks");
        boolean refused = false;
        try { c.getConstructors()[0].newInstance(0L, 2L); }
        catch (java.lang.reflect.InvocationTargetException e) { refused = e.getCause() instanceof IllegalArgumentException; }
        catch (ReflectiveOperationException e) { throw new AssertionError(e); }
        h.assertTrue(refused, "a cost of 0 EU is refused");
        h.succeed();
    }

    /** The macerators keep their cost and time in M0: 600 EU at 2 EU/t (300 t) and 750 EU at 5 EU/t (150 t). */
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void maceratorsKeepTheirCost(GameTestHelper h) {
        BlockPos basic = new BlockPos(1, 2, 1), advanced = new BlockPos(3, 2, 1);
        h.setBlock(basic, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "producer/block_powder_elc")));
        h.setBlock(advanced, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "producer/block_powder_advanced_elc")));
        BlockEntity a = h.getBlockEntity(basic), b = h.getBlockEntity(advanced);
        h.runAfterDelay(5, () -> {
            Object ca = call(a, "processCost"), cb = call(b, "processCost");
            h.assertTrue((long) call(ca, "energy") == 600 && (long) call(ca, "power") == 2, "macerator " + ca);
            h.assertTrue((long) call(cb, "energy") == 750 && (long) call(cb, "power") == 5, "advanced macerator " + cb);
            h.assertTrue((int) call(a, "getMaxProgress") == 300, "macerator ticks " + call(a, "getMaxProgress"));
            h.assertTrue((long) call(a, "getEffectiveEnergyPerTick") == 2, "macerator EU/t " + call(a, "getEffectiveEnergyPerTick"));
            h.assertTrue((int) call(b, "getMaxProgress") == 150, "advanced macerator ticks " + call(b, "getMaxProgress"));
            h.succeed();
        });
    }
}
