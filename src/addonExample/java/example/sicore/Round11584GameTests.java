package example.sicore;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.84: the three kinetic generators size their packets by their output (getSourceTier). The basic one sends MV
 * packets, HV above 128 EU/t. The turbocharged one sends HV, EV above 512 EU/t. The twin turbocharged one sends EV, IV
 * above 2,048 EU/t. The manual and the energy table gave only the lower tier, so a player who wired the twin with EV
 * cable melted it at full wind or water input. The pages now give both tiers and the KU input where the packet grows;
 * this test reads those numbers from the block entities.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11584GameTests {
    private Round11584GameTests() {}

    private static final String[] TIER = {"ULV", "LV", "MV", "HV", "EV", "IV", "LuV"};

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }
    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }
    private static String n(long value) { return String.format(Locale.ROOT, "%,d", value); }
    private static long power(int tier) { return 8L << (2 * tier); }

    private static Field field(Class<?> type, String name) {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {
                // look in the superclass
            }
        }
        throw new AssertionError("no field " + name + " in " + type.getName());
    }

    private static void set(Object target, String name, Object value) {
        try {
            field(target.getClass(), name).set(target, value);
        } catch (IllegalAccessException e) {
            throw new AssertionError(name, e);
        }
    }

    private static double constant(Object target, String name) {
        try {
            return ((Number) field(target.getClass(), name).get(null)).doubleValue();
        } catch (IllegalAccessException e) {
            throw new AssertionError(name, e);
        }
    }

    private static int tierAt(BlockEntity generator, int ku) {
        set(generator, "lastKuInputRate", ku);
        return ((Number) call(generator, "getSourceTier")).intValue();
    }

    /** The largest KU input in [0, max] that keeps the packet at {@code floor} (the tier only grows with the input). */
    private static int lastKuAt(BlockEntity generator, int floor, int max) {
        int lo = 0, hi = max;
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            if (tierAt(generator, mid) == floor) lo = mid; else hi = mid - 1;
        }
        return lo;
    }

    private static String page(String lang, String machine) {
        String text = Round33GameTests.resource("/assets/mio_icif/guides/mio_icif/manual/" + lang + "machines/generator_block_" + machine + ".md");
        if (text == null) throw new AssertionError("no " + lang + " page for " + machine);
        return text;
    }

    @GameTest(template = "empty", batch = "round11584_kinetic", timeoutTicks = 40)
    public static void kineticGeneratorPagesGiveBothPacketTiers(GameTestHelper h) {
        String[] machines = {"kinetic_generator", "turbo_kinetic_generator", "twin_turbo_kinetic_generator"};
        int[] floors = {2, 3, 4};
        for (int i = 0; i < machines.length; i++) {
            h.setBlock(new BlockPos(1 + 2 * i, 1, 1), BuiltInRegistries.BLOCK.get(id("generator/block_" + machines[i])));
        }
        h.runAfterDelay(2, () -> {
            List<String> wrong = new ArrayList<>();
            for (int i = 0; i < machines.length; i++) {
                String machine = machines[i];
                int floor = floors[i], up = floor + 1;
                BlockEntity generator = h.getBlockEntity(new BlockPos(1 + 2 * i, 1, 1));
                // the front face takes one source, at most this much per tick
                int maxKu = (int) ((Number) call(call(generator, "getKineticStorage"), "receiveKinetic", 1_000_000L, true)).longValue();
                if (tierAt(generator, 0) != floor) wrong.add(machine + ": idle tier " + tierAt(generator, 0) + ", expected " + TIER[floor]);
                if (tierAt(generator, maxKu) != up) wrong.add(machine + ": tier " + tierAt(generator, maxKu) + " at " + maxKu + " KU/t, expected " + TIER[up]);
                int natural = lastKuAt(generator, floor, maxKu);

                String en = page("", machine), zh = page("_zh_cn/", machine), ja = page("_ja_jp/", machine);
                String enRow = "| Output voltage tier | " + TIER[floor] + " (" + n(power(floor)) + " EU); " + TIER[up] + " (" + n(power(up)) + " EU) above ";
                String zhRow = "| 输出电压等级 | " + TIER[floor] + " (" + n(power(floor)) + " EU)；产出超过 ";
                if (!en.contains(enRow)) wrong.add(machine + " en: no row \"" + enRow + "\"");
                if (!zh.contains(zhRow) || !zh.contains("时为 " + TIER[up] + " (" + n(power(up)) + " EU)")) wrong.add(machine + " zh: tier row");
                if (!ja.contains("| 出力電圧Tier | " + TIER[floor] + " (" + n(power(floor)) + " EU)、") || !ja.contains(TIER[up] + " (" + n(power(up)) + " EU) |"))
                    wrong.add(machine + " ja: tier row");
                if (!en.contains("Connect an " + TIER[up] + " cable")) wrong.add(machine + " en: cable step does not name " + TIER[up]);
                String above = "above " + n(natural) + " KU/t";
                if (!en.contains(above)) wrong.add(machine + " en: no \"" + above + "\" (the code switches at " + n(natural + 1) + ")");
                if (!zh.contains("超过 " + n(natural) + " KU/t")) wrong.add(machine + " zh: no 超过 " + n(natural) + " KU/t");
                if (!ja.contains(n(natural) + " KU/t")) wrong.add(machine + " ja: no " + n(natural) + " KU/t");

                if (!machine.equals("kinetic_generator")) {
                    // any source but a wind or water kinetic generator in front converts at the lower rate
                    set(generator, "effectivePercent", constant(generator, "EFFECTIVE_PERCENT_ARTIFICIAL"));
                    if (tierAt(generator, maxKu) == floor) {
                        String always = "always stay at " + TIER[floor];
                        if (!en.contains(always)) wrong.add(machine + " en: other sources never leave " + TIER[floor] + ", no \"" + always + "\"");
                    } else {
                        int other = lastKuAt(generator, floor, maxKu);
                        String otherText = "above " + n(other) + " KU/t from other sources";
                        if (!en.contains(otherText)) wrong.add(machine + " en: no \"" + otherText + "\"");
                        if (!zh.contains("其他来源超过 " + n(other) + " KU/t")) wrong.add(machine + " zh: no 其他来源超过 " + n(other) + " KU/t");
                    }
                    set(generator, "effectivePercent", 1.0D);
                }
            }
            h.assertTrue(wrong.isEmpty(), "kinetic generator pages and packet tiers disagree: " + wrong);
            h.succeed();
        });
    }
}
