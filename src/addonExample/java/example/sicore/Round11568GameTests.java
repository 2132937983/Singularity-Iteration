package example.sicore;

import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.68 (quantum flight phase R2): the rules of the impacts. A landing is hard only from a real fall and grows with
 * it; a wall hit needs speed, a wall and most of the speed lost; the shake goes with the square of the trauma, fades and
 * is off at scale 0; a FOV kick comes at once and settles to nothing; the cracks of a landing are fixed by their seed and
 * grow with the strength; a hard brake fires its jets together and steady.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11568GameTests {
    private Round11568GameTests() {}

    private static Class<?> impact() { return Round33GameTests.common("suit.FlightImpact"); }

    private static float f(String name, Object... args) { return (Float) Round33GameTests.call(impact(), name, args); }

    @GameTest(template = "empty", batch = "round11568_landing", timeoutTicks = 20)
    public static void landingsAndWallHits(GameTestHelper h) {
        h.assertTrue(f("landing", 0.3F) == 0 && f("landing", -0.3F) == 0, "a soft landing is hard");
        float hard = f("landing", -0.8F), full = f("landing", -1.6F);
        h.assertTrue(hard >= 0.35F && hard < full && full >= 0.999F && f("landing", -9F) >= 0.999F, "the landing does not grow with the fall");
        h.assertTrue(f("landing", Float.NaN) == 0, "NaN landing");
        h.assertTrue(f("wallHit", 0.8F, 0F, true) == 0, "a slow hit counts");
        h.assertTrue(f("wallHit", 2F, 1.5F, true) == 0, "a graze counts");
        h.assertTrue(f("wallHit", 2F, 0F, false) == 0, "a stop without a wall counts");
        float hit = f("wallHit", 2F, 0.1F, true), fast = f("wallHit", 3F, 0.1F, true);
        h.assertTrue(hit >= 0.4F && hit < fast && fast >= 0.999F, "the wall hit does not grow with the speed");
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round11568_shake", timeoutTicks = 20)
    public static void shakeAndKick(GameTestHelper h) {
        float[] none = (float[]) Round33GameTests.call(impact(), "shake", 0F, 1.3F, 1F);
        float[] off = (float[]) Round33GameTests.call(impact(), "shake", 1F, 1.3F, 0F);
        h.assertTrue(none[0] == 0 && none[1] == 0 && none[2] == 0 && off[0] == 0 && off[2] == 0, "a shake without trauma or with the option off");
        float half = 0, whole = 0;
        for (int i = 0; i < 40; i++) {
            float t = i * 0.037F;
            float[] a = (float[]) Round33GameTests.call(impact(), "shake", 0.5F, t, 1F), b = (float[]) Round33GameTests.call(impact(), "shake", 1F, t, 1F);
            h.assertTrue(Math.abs(b[0]) <= 2.2F && Math.abs(b[1]) <= 2.6F && Math.abs(b[2]) <= 4F, "the shake is unbounded");
            half += Math.abs(a[2]); whole += Math.abs(b[2]);
        }
        h.assertTrue(Math.abs(half * 4 - whole) < 1e-2F * whole + 1e-4F, "the shake does not go with the square of the trauma");
        h.assertTrue(f("decay", 1F) < 1F && f("decay", 0.03F) == 0 && f("add", 0.8F, 0.5F) == 1F, "the trauma does not fade or stack");
        float k0 = f("fovKick", 0F, 0.09F, 1F), k5 = f("fovKick", 5F, 0.09F, 1F), k10 = f("fovKick", 10F, 0.09F, 1F);
        h.assertTrue(k0 == 0.09F && k5 > 0 && k5 < k0 && k10 == 0 && f("fovKick", -1F, 0.09F, 1F) == 0, "the kick does not come at once and settle");
        h.assertTrue(f("fovKick", 0F, -0.06F, 2F) == -0.12F && f("fovKick", 0F, 0.09F, 0F) == 0, "the kick scale");
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round11568_cracks", timeoutTicks = 20)
    public static void cracksAreFixedAndGrow(GameTestHelper h) {
        float[][] a = (float[][]) Round33GameTests.call(impact(), "cracks", 42L, 1F), b = (float[][]) Round33GameTests.call(impact(), "cracks", 42L, 1F);
        float[][] weak = (float[][]) Round33GameTests.call(impact(), "cracks", 42L, 0F);
        h.assertTrue(java.util.Arrays.deepEquals(a, b), "the same seed gives other cracks");
        h.assertTrue(a.length == 9 && weak.length == 5, "the number of cracks: " + a.length + "/" + weak.length);
        float longest = 0, longestWeak = 0, prev = -1;
        for (float[] c : a) {
            h.assertTrue(c[0] > prev && c[0] < Math.PI * 2 && c[1] > 0 && c[1] <= 3F, "a crack out of order or reach");
            prev = c[0];
            longest = Math.max(longest, c[1]);
        }
        for (float[] c : weak) longestWeak = Math.max(longestWeak, c[1]);
        h.assertTrue(longest > longestWeak * 2, "the cracks do not grow with the strength");
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round11568_brake", timeoutTicks = 20)
    public static void hardBrakeFiresTogether(GameTestHelper h) {
        @SuppressWarnings({"unchecked", "rawtypes"})
        Class<Enum> variant = (Class<Enum>) Round33GameTests.common("suit.QuantumNozzles$Variant");
        List<?> base = (List<?>) Round33GameTests.call(Round33GameTests.common("suit.QuantumNozzles"), "chest", Enum.valueOf(variant, "BASE"));
        Class<?> rcs = Round33GameTests.common("suit.RcsAllocator");
        Object weak = Round33GameTests.call(rcs, "command", 1F, 0F, 0F, new float[3], 0F, 0F, 0.3F);
        Object hard = Round33GameTests.call(rcs, "command", 1F, 0F, 0F, new float[3], 0F, 0F, 1F);
        var names = base.stream().map(port -> String.valueOf(Round33GameTests.call(port, "name"))).toList();
        int left = names.indexOf("rcs_locator_shoulder_left_up"), right = names.indexOf("rcs_locator_shoulder_right_up");
        h.assertTrue(left >= 0 && right >= 0, "missing paired cruise brakes: " + names);
        int weakLeft = 0, weakRight = 0;
        for (int tick = 0; tick < 4; tick++) {
            float[] w = (float[]) Round33GameTests.call(rcs, "allocate", base, weak, new float[]{1, 1, 1, 1}, 1F, 0.3F, tick);
            float[] s = (float[]) Round33GameTests.call(rcs, "allocate", base, hard, new float[]{1, 1, 1, 1}, 1F, 1F, tick);
            if (w[left] > 0) weakLeft++;
            if (w[right] > 0) weakRight++;
            h.assertTrue(s[left] == 1F && s[right] == 1F, "both shoulder brakes must burn together on tick " + tick);
        }
        h.assertTrue(weakLeft == 1 && weakRight == 1, "30% brake should pulse once per four ticks on each side");
        h.succeed();
    }
}
