package example.sicore;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.136 (fusion overhaul F4): the fuel cycle: a 3% burn fraction with the exhaust returning the rest (and nothing without
 * it), lithium breeding 1 unit per fusion neutron (five cells in column 1 just self-sufficient, the bred tritium reported per
 * cycle), isotope rods still at 40, heavy water in place of the unstable hot water, the synced fuel readouts and texts.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111136GameTests {
    private Round111136GameTests() {}

    private static final String PLASMA = "fusion.FusionPlasma", BLANKET = "fusion.FusionBlanket";

    private static Class<?> cls(String name) { return Round33GameTests.common(name); }
    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    private static double field(String cls, String name) {
        try {
            var f = cls(cls).getDeclaredField(name);
            f.setAccessible(true);
            return ((Number) f.get(null)).doubleValue();
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(name, e);
        }
    }

    private static Object make(String cls, Object... args) {
        try {
            for (Constructor<?> c : cls(cls).getDeclaredConstructors()) {
                if (c.getParameterCount() != args.length) continue;
                c.setAccessible(true);
                return c.newInstance(args);
            }
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(cls, e);
        }
        throw new AssertionError("no constructor of " + cls);
    }

    private static Object in(boolean exhaust) {
        return make(PLASMA + "$Inputs", true, true, 16, 4, 2, 1_000_000_000_000L, 100_000_000L, 100_000_000L, exhaust, false);
    }

    @GameTest(template = "empty", batch = "round111136_fuel", timeoutTicks = 20)
    public static void burnFractionAndExhaust(GameTestHelper h) {
        List<String> wrong = new ArrayList<>();
        if (field(PLASMA, "BURN_FRACTION") != 0.03) wrong.add("burn fraction " + field(PLASMA, "BURN_FRACTION"));
        Object p = make(PLASMA);
        call(p, "random", new java.util.SplittableRandom(5));
        for (int t = 0; t < 2_000; t++) call(p, "tick", in(true));
        long used = 0;
        double injected = 0;
        for (int t = 0; t < 1_000; t++) {
            used += ((Number) call(call(p, "tick", in(true)), "fuelUsed")).longValue();
            injected += ((Number) call(p, "injected")).doubleValue();
        }
        if (Math.abs(injected / 1_000 - 16) > 0.8) wrong.add("injected " + injected / 1_000 + " a tick");
        if (Math.abs(used - 0.03 * injected) > 2) wrong.add("used " + used + " of " + injected + " injected");
        // without an exhaust port nothing comes back (until the ash disrupts it)
        long lost = 0;
        for (int t = 0; t < 100; t++) lost += ((Number) call(call(p, "tick", in(false)), "fuelUsed")).longValue();
        if (lost < 1_400) wrong.add("without exhaust only " + lost + " used in 100 ticks");
        h.assertTrue(wrong.isEmpty(), "fuel cycle: " + wrong);
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round111136_fuel", timeoutTicks = 20)
    public static void lithiumBreedsOnePerNeutron(GameTestHelper h) {
        List<String> wrong = new ArrayList<>();
        if (field(BLANKET, "LITHIUM_BREED_PER_NEUTRON") != 1 || field(BLANKET, "BREED_PER_NEUTRON") != 40) wrong.add("breeding rates");
        ItemStack lithium = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "reactor/item_reactor_lithium_cell")));
        int slots = (int) field(BLANKET, "SLOTS"), cols = (int) field(BLANKET, "COLUMNS");
        for (int cells : new int[]{5, 6}) {
            ItemStack[] grid = new ItemStack[slots];
            for (int i = 0; i < slots; i++) grid[i] = i % cols == 0 && i / cols < cells ? lithium.copy() : ItemStack.EMPTY;
            Object c = call(cls(BLANKET), "cycle", grid, 0L, (long) field(BLANKET, "FULL_NEUTRONS"));
            long bred = ((Number) call(c, "bred")).longValue();
            double tbr = bred / (16 * 0.03 * 20);
            if (bred != 2L * cells) wrong.add(cells + " cells bred " + bred);
            if (cells == 5 && (tbr < 1.0 || tbr > 1.1)) wrong.add("five cells TBR " + tbr);
        }
        h.assertTrue(wrong.isEmpty(), "breeding: " + wrong);
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round111136_fuel", timeoutTicks = 20)
    public static void heavyWaterAndTexts(GameTestHelper h) throws Exception {
        int values = cls("menu.fusion.FusionControllerMenu").getDeclaredField("VALUES").getInt(null);
        h.assertTrue(values >= 52, "menu values " + values);
        String[][] names = {{"en_us", "Heavy Water Cell"}, {"zh_cn", "重水单元"}, {"ja_jp", "重水セル"}, {"ru_ru", "тяжёлой водой"}};
        for (String[] n : names) {
            String json = Round33GameTests.resource("/assets/mio_icif/lang/" + n[0] + ".json");
            h.assertTrue(json != null && json.contains(n[1]) && json.contains("\"gui.mio_icif.fusion.balance.fuel_cycle\""), n[0] + " lacks heavy water or the fuel cycle");
            h.assertTrue(!json.contains("\"item.mio_icif.cell.item_cell_unstable_hotwater\": \"Unstable") && !json.contains("不稳定的热水单元"), n[0] + " still says unstable hot water");
        }
        String[][] pages = {{"", "3%"}, {"_zh_cn/", "3%"}, {"_ja_jp/", "3%"}};
        for (String[] p : pages) {
            String md = Round33GameTests.resource("/assets/mio_icif/guides/mio_icif/manual/" + p[0] + "machines/fusion_block_fusion_controller.md");
            h.assertTrue(md != null && md.contains("D₂O") && md.contains("0.48") && md.contains("1.04"), p[0] + " manual lacks the fuel cycle");
        }
        h.succeed();
    }
}
