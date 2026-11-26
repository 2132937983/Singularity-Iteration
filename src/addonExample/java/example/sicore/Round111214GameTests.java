package example.sicore;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests of 0.1.11.214 (MT1 X2): the motion of the molecular transformer's sample atoms (MolecularMotion). An item
 * comes apart, moves the way its process does inside the glass chamber, and settles into the product; each of the four
 * processes moves differently, and only the exotic one turns magenta.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111214GameTests {
    private Round111214GameTests() {}

    private static final String PKG = "com.miophas.singularity_iteration" + ".common.client.molecular.";

    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    private static Class<?> type(String name) {
        try {
            return Class.forName(PKG + name);
        } catch (ClassNotFoundException e) {
            throw new AssertionError(name, e);
        }
    }

    private static Object kind(String name) {
        return call(type("MolecularMotion$Kind"), "of", name);
    }

    private static Object state() {
        try {
            var c = type("MolecularMotion$State").getDeclaredConstructor();
            c.setAccessible(true);
            return c.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private static float f(Object state, String field) {
        try {
            var fl = state.getClass().getField(field);
            return fl.getFloat(state);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(field, e);
        }
    }

    private static boolean visible(Object state) {
        try {
            return state.getClass().getField("visible").getBoolean(state);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    /** A solid 10x10 square of one colour, shuffled as the renderer does. */
    private static List<?> square(int rgb, int salt) {
        int[] px = new int[256];
        for (int j = 3; j < 13; j++) for (int i = 3; i < 13; i++) px[j * 16 + i] = 0xFF000000 | rgb;
        return (List<?>) call(type("MolecularMotion"), "shuffled", call(type("MolecularAtoms"), "sample", px, 16, 16), salt);
    }

    private static Object at(String kind, int k, double p, double t) {
        Object s = state();
        call(type("MolecularMotion"), "single", kind(kind), k, square(0x804020, 0), square(0x2060A0, 977), p, t, 0.3F, s);
        return s;
    }

    private static double dist(Object s) {
        float dx = f(s, "x") - 8, dy = f(s, "y") - 8, dz = f(s, "z") - 8;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /** Every process: on the input at the start, in the chamber in the middle, on the product near the end, gone at the end. */
    @GameTest(template = "empty", batch = "round111214_mt_motion", timeoutTicks = 20)
    public static void itemComesApartAndSettlesIntoTheProduct(GameTestHelper h) {
        for (String kind : new String[]{"molecular", "lattice", "nuclear", "exotic", "unknown"}) {
            for (int k = 0; k < 100; k += 7) {
                Object start = at(kind, k, 0.02, 3.0);
                h.assertTrue(visible(start) && f(start, "cloud") == 0 && dist(start) < 2.6, kind + ": atom " + k + " starts on the input");
                Object mid = at(kind, k, 0.5, 3.0);
                h.assertTrue(visible(mid) && f(mid, "cloud") == 1, kind + ": atom " + k + " is in the cloud at the middle");
                h.assertTrue(f(mid, "x") >= 4.3 && f(mid, "x") <= 11.7 && f(mid, "z") >= 4.3 && f(mid, "z") <= 11.7
                    && f(mid, "y") >= 3.7 && f(mid, "y") <= 12.3, kind + ": atom " + k + " stays inside the glass");
                Object end = at(kind, k, 0.95, 3.0);
                h.assertTrue(visible(end) && f(end, "cloud") == 0 && dist(end) < 2.6, kind + ": atom " + k + " ends on the product");
                h.assertTrue(Math.abs(f(end, "b") - 0xA0 / 255F) < 0.02, kind + ": atom " + k + " has the product's colour at the end");
                h.assertTrue(f(at(kind, k, 0.99, 3.0), "alpha") < 0.7, kind + ": the product fades out at the end");
            }
        }
        h.succeed();
    }

    /** The processes move differently: nuclear closes into a nucleus, lattice keeps a grid, exotic jumps, molecular bonds. */
    @GameTest(template = "empty", batch = "round111214_mt_motion", timeoutTicks = 20)
    public static void eachProcessMovesItsOwnWay(GameTestHelper h) {
        double nuclear = 0, molecular = 0;
        int bonded = 0, jumped = 0, magenta = 0, otherMagenta = 0;
        for (int k = 0; k < 100; k++) {
            nuclear = Math.max(nuclear, dist(at("nuclear", k, 0.5, 3.0)));
            molecular = Math.max(molecular, dist(at("molecular", k, 0.5, 3.0)));
            if (!Float.isNaN(f(at("molecular", k, 0.5, 3.0), "ax"))) bonded++;
            Object a = at("exotic", k, 0.5, 3.0), b = at("exotic", k, 0.5, 3.4);
            if (Math.abs(f(a, "x") - f(b, "x")) + Math.abs(f(a, "y") - f(b, "y")) > 0.5) jumped++;
            for (double t = 0; t < 2; t += 0.1) {
                if ((boolean) call(type("MolecularMotion"), "magentaGlow", kind("exotic"), k, t)) magenta++;
                for (String other : new String[]{"molecular", "lattice", "nuclear"})
                    if ((boolean) call(type("MolecularMotion"), "magentaGlow", kind(other), k, t)) otherMagenta++;
            }
        }
        h.assertTrue(nuclear < 1.6, "nuclear atoms close into a nucleus, farthest " + nuclear);
        h.assertTrue(molecular > 2.0, "molecules spread round the chamber, farthest " + molecular);
        h.assertTrue(bonded == 100, "every molecular atom is bonded to its molecule");
        h.assertTrue(jumped > 50, "exotic atoms jump about, " + jumped + " of 100 moved");
        h.assertTrue(magenta > 0 && otherMagenta == 0, "only the exotic process glows magenta");

        Class<?> motion = type("MolecularMotion");
        float maxPress = 0, maxBeam = 0;
        for (double t = 0; t < 2; t += 0.01) {
            maxPress = Math.max(maxPress, (float) call(motion, "latticePress", 0.4, t));
            maxBeam = Math.max(maxBeam, (float) call(motion, "nuclearBeam", 0.4, t));
        }
        h.assertTrue(maxPress > 0.9 && maxPress <= 1, "the lattice is pressed");
        h.assertTrue(maxBeam > 0.8 && (float) call(motion, "nuclearBeam", 0.05, 0.0) == 0, "the nuclear beam fires only mid-process");
        float[] q = new float[3];
        int drawn = 0;
        for (int s = 0; s < 4; s++) for (int i = 0; i < 18; i++) if ((float) call(motion, "nuclearTrack", 5, s, i, 0.5, q) > 0) drawn++;
        h.assertTrue(drawn > 20 && (float) call(motion, "nuclearTrack", 5, 0, 0, 1.5, q) == 0, "tracks are drawn, then fade");
        List<Integer> phases = new ArrayList<>();
        for (double p : new double[]{0, 0.3, 0.6, 0.9, 1.0}) phases.add((int) call(motion, "phase", p));
        h.assertTrue(phases.equals(List.of(0, 1, 2, 3, 3)), "the four steps of the screen, got " + phases);
        double hv = (double) call(motion, "hash", 12345);
        h.assertTrue(hv >= 0 && hv < 1 && hv == (double) call(motion, "hash", 12345), "hash is a fixed 0..1 value");
        h.succeed();
    }
}
