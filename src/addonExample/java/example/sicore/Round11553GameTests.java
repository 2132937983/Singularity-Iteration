package example.sicore;

import java.util.List;
import java.util.Map;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.53 (gaze and Iron Man phase P1, decision two A): the SlashBlade combo chains of a Wingman click inside the
 * windows of SlashBlade: Resharped 2.1.9 (read from its {@code ComboStateRegistry}: from the click that started the state,
 * the first and last tick at which a click leads to the next one); the scheduler runs a chain to its finisher, rests for
 * the finisher's length, ends a chain on a click that led elsewhere, and extends the powered ground chain to a5ex.
 * SlashBlade is not in the test server: the chains themselves were checked in the capture scene {@code wingman_blade}.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11553GameTests {
    private Round11553GameTests() {}

    private static final String CHAIN = "armory.BladeChain";

    /** For each combo reached by a click: the window {first, last} of the click that leads on to the next combo. */
    private static final Map<String, int[]> WINDOWS = Map.ofEntries(
        Map.entry("aerial_rave_a1>aerial_rave_a2", new int[]{3, 14}),
        Map.entry("aerial_rave_a2>aerial_rave_a3", new int[]{3, 6}),
        Map.entry("aerial_rave_a2>aerial_rave_b3", new int[]{7, 21}),
        Map.entry("aerial_rave_b3>aerial_rave_b4", new int[]{6, 24}),
        Map.entry("combo_a1>combo_a2", new int[]{3, 14}),
        Map.entry("combo_a2>combo_a3", new int[]{3, 10}),
        Map.entry("combo_a2>combo_c", new int[]{11, 22}),
        Map.entry("combo_a3>combo_a4", new int[]{6, 12}),
        Map.entry("combo_a3>combo_b1", new int[]{13, 21}),
        Map.entry("combo_a4_ex>combo_a5ex", new int[]{14, 26}),
        Map.entry("combo_b1>combo_b2", new int[]{8, 13}),
        Map.entry("combo_b2>combo_b3", new int[]{4, 6}),
        Map.entry("combo_b3>combo_b4", new int[]{4, 6}),
        Map.entry("combo_b4>combo_b5", new int[]{4, 6}),
        Map.entry("combo_b5>combo_b6", new int[]{4, 6}),
        Map.entry("combo_b6>combo_b7", new int[]{4, 6}));

    private static Object[] kinds() { return Round33GameTests.common(CHAIN + "$Kind").getEnumConstants(); }

    private static String[] expect(Object step) { return (String[]) Round33GameTests.call(step, "expect"); }

    /** Every click of every chain lands inside the window of the combo before it. */
    @GameTest(template = "empty", batch = "round11553_windows", timeoutTicks = 20)
    public static void chainsClickInsideTheWindows(GameTestHelper h) {
        for (Object kind : kinds()) {
            List<?> steps = (List<?>) Round33GameTests.call(Round33GameTests.common(CHAIN), "steps", kind);
            h.assertTrue(!steps.isEmpty(), kind + " has no steps");
            h.assertTrue((Integer) Round33GameTests.call(steps.get(0), "after") == 0, kind + ": the first click is not at once");
            for (int i = 1; i < steps.size(); i++) {
                String before = expect(steps.get(i - 1))[0], next = expect(steps.get(i))[0];
                int[] window = WINDOWS.get(before + ">" + next);
                h.assertTrue(window != null, kind + ": no known window from " + before + " to " + next);
                int after = (Integer) Round33GameTests.call(steps.get(i), "after");
                h.assertTrue(after >= window[0] && after <= window[1],
                    kind + ": " + before + " > " + next + " clicked at " + after + ", window " + window[0] + ".." + window[1]);
            }
        }
        h.succeed();
    }

    private static Object chain() {
        try {
            return Round33GameTests.common(CHAIN).getConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(CHAIN, e);
        }
    }

    private static Object kind(String name) {
        for (Object k : kinds()) if (((Enum<?>) k).name().equals(name)) return k;
        throw new AssertionError(name);
    }

    /** Plays a chain by its schedule with the combos it expects; returns the tick of the last click. */
    private static long play(GameTestHelper h, Object chain, long now, String... combos) {
        long t = now;
        for (String combo : combos) {
            int guard = 0;
            while (!(Boolean) Round33GameTests.call(chain, "due", t)) {
                t++;
                h.assertTrue(++guard < 40, "a click never came due for " + combo);
            }
            h.assertTrue((Boolean) Round33GameTests.call(chain, "clicked", t, combo), combo + " was not accepted");
        }
        return t;
    }

    /** A chain runs to its finisher and rests for it; a wrong combo ends it; the powered chain goes on to a5ex. */
    @GameTest(template = "empty", batch = "round11553_schedule", timeoutTicks = 20)
    public static void schedulerRunsChainsToTheirFinishers(GameTestHelper h) {
        Object c = chain();
        h.assertTrue((Boolean) Round33GameTests.call(c, "free", 0L), "a new scheduler is not free");
        Round33GameTests.call(c, "start", kind("AERIAL_B"), 100L);
        long last = play(h, c, 100, "aerial_rave_a1", "aerial_rave_a2", "aerial_rave_b3", "aerial_rave_b4");
        h.assertTrue(last == 100 + 4 + 11 + 8, "aerial B clicked over " + (last - 100) + " ticks");
        h.assertTrue((Boolean) Round33GameTests.call(c, "resting", last + 1) && !(Boolean) Round33GameTests.call(c, "free", last + 24),
            "the finisher b4 is cut");
        h.assertTrue((Boolean) Round33GameTests.call(c, "free", last + 25), "no new chain after b4 (25 ticks)");

        Round33GameTests.call(c, "start", kind("GROUND_A"), 500L);
        last = play(h, c, 500, "combo_a1", "combo_a2", "combo_a3", "combo_a4_ex", "combo_a5ex");
        h.assertTrue((Integer) Round33GameTests.call(c, "clicks") == 5, "the powered chain did not go on to a5ex");
        h.assertTrue(!(Boolean) Round33GameTests.call(c, "free", last + 75) && (Boolean) Round33GameTests.call(c, "free", last + 76),
            "a5ex rests 76 ticks");

        Round33GameTests.call(c, "start", kind("GROUND_B"), 1000L);
        play(h, c, 1000, "combo_a1", "combo_a2");
        long t = 1000;
        while (!(Boolean) Round33GameTests.call(c, "due", t)) t++;
        h.assertTrue(!(Boolean) Round33GameTests.call(c, "clicked", t, "combo_a1"), "a click that restarted the chain was accepted");
        h.assertTrue(!(Boolean) Round33GameTests.call(c, "free", t + 9) && (Boolean) Round33GameTests.call(c, "free", t + 10),
            "a broken chain rests 10 ticks");
        h.succeed();
    }
}
