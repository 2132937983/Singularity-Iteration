package example.sicore;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.127 (armour and flight overhaul E4): the electromagnetic attitude pulses. A pulse is brightest as its port fires
 * and gone two ticks later; its ring snaps out; its micro arcs leave the port, run out along its axis, bulge and are jagged, the
 * same within a tick and new the next. (The mod's class by reflection: addon examples do not compile against it.)
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111127GameTests {
    private Round111127GameTests() {}

    private static final Class<?> RULES = Round33GameTests.common("suit.RcsPulseRules");

    private static Object call(String name, Object... args) { return Round33GameTests.call(RULES, name, args); }

    @GameTest(template = "empty", timeoutTicks = 5)
    public static void pulsesAreSharpAndNewEachTick(GameTestHelper h) {
        h.assertTrue((float) call("fade", 0F) == 1F && (float) call("fade", 2F) == 0F && (float) call("fade", 1F) == 0.25F, "a pulse fades over two ticks");
        h.assertTrue((float) call("ringRadius", 0F) < (float) call("ringRadius", 0.5F), "the ring snaps out");
        float[][] a = (float[][]) call("arc", 1234L, 0), b = (float[][]) call("arc", 1234L, 1), again = (float[][]) call("arc", 1234L, 0),
            next = (float[][]) call("arc", 1235L, 0);
        h.assertTrue(a[0][0] == 0 && a[0][1] == 0 && a[0][2] == 0, "an arc leaves the port");
        h.assertTrue(a[a.length - 1][1] > 0.1F, "an arc runs out along the axis: " + a[a.length - 1][1]);
        h.assertTrue(java.util.Arrays.deepEquals(a, again), "the arcs change within a tick");
        h.assertTrue(!java.util.Arrays.deepEquals(a, next), "the arcs are the same the next tick");
        int mid = a.length / 2;
        float bulgeA = (float) Math.hypot(a[mid][0], a[mid][2]), bulgeB = (float) Math.hypot(b[mid][0], b[mid][2]);
        h.assertTrue(bulgeA > 0.02F && bulgeB > 0.02F && (a[mid][0] * b[mid][0] + a[mid][2] * b[mid][2]) < 0, "the two arcs bulge apart");
        h.succeed();
    }
}
