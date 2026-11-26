package example.sicore;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests of 0.1.11.215 (MT1 X3): the molecular transformer's field, finishing flash, vents and continuous stream
 * (MolecularMotion). The flash and the vent puff come only as an item finishes; vent specks leave through the edge notches;
 * a stream carries atoms from the low input up to the high product inside the glass.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111215GameTests {
    private Round111215GameTests() {}

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

    /** The flash and the vents come as an item finishes; the specks leave the block through the notches. */
    @GameTest(template = "empty", batch = "round111215_mt_field", timeoutTicks = 20)
    public static void flashAndVentsComeAsAnItemFinishes(GameTestHelper h) {
        Class<?> m = type("MolecularMotion");
        h.assertTrue((float) call(m, "flash", 0.5) == 0 && (float) call(m, "flash", 0.9) == 0, "no flash before 90 %");
        h.assertTrue((float) call(m, "flash", 0.92) > 0.7, "a flash just after 90 %");
        h.assertTrue((float) call(m, "flashRadius", 1.0) > 4, "the flash ring runs out past the glass");
        float[] q = new float[3];
        int puffs = 0, outside = 0, early = 0;
        for (int v = 0; v < 8; v++) {
            for (int i = 0; i < 4; i++) {
                if ((float) call(m, "vent", v, i, 0.5, 3.0, false, q) > 0) early++;
                for (double p = 0.93; p < 1.0; p += 0.01) {
                    if ((float) call(m, "vent", v, i, p, 3.0, false, q) <= 0) continue;
                    puffs++;
                    if (q[0] < 0.5 || q[0] > 15.5 || q[2] < 0.5 || q[2] > 15.5) outside++;
                    h.assertTrue(Math.abs(q[0] - 8) < 1.5 || Math.abs(q[2] - 8) < 1.5, "specks leave through the middle of an edge");
                }
            }
        }
        h.assertTrue(early == 0, "no vent specks mid-item");
        h.assertTrue(puffs > 100 && outside > 10, "specks puff out of the notches, " + puffs + " drawn, " + outside + " outside");
        int steady = 0;
        for (int v = 0; v < 8; v++) for (int i = 0; i < 4; i++) if ((float) call(m, "vent", v, i, 0.0, 3.0, true, q) > 0) steady++;
        h.assertTrue(steady == 32, "a stream vents all the time");
        float lo = 99, hi = -99;
        for (double t = 0; t < 4; t += 0.05) {
            float y = (float) call(m, "sweepHeight", t, false);
            lo = Math.min(lo, y); hi = Math.max(hi, y);
            float b = (float) call(m, "breathe", t);
            h.assertTrue(b >= 0 && b <= 1, "breathing stays 0..1");
        }
        h.assertTrue(lo < 4.5 && hi > 11.5, "the scanning ring runs the chamber's height, " + lo + ".." + hi);
        h.succeed();
    }

    /** A stream: atoms flow inside the glass from the input (low) to the product (high) for every process. */
    @GameTest(template = "empty", batch = "round111215_mt_field", timeoutTicks = 20)
    public static void streamFlowsFromInputToProduct(GameTestHelper h) {
        Class<?> m = type("MolecularMotion");
        List<?> in = square(0x804020, 0), out = square(0x2060A0, 977);
        for (String kind : new String[]{"molecular", "lattice", "nuclear", "exotic"}) {
            double low = 0, high = 0;
            int lowN = 0, highN = 0;
            for (int k = 0; k < 44; k++) {
                for (double t = 0; t < 3; t += 0.05) {
                    Object s = state();
                    call(m, "streamAtom", kind(kind), k, in, out, t, 0.3F, s);
                    h.assertTrue(visible(s), kind + ": stream atoms show");
                    h.assertTrue(f(s, "x") >= 4.3 && f(s, "x") <= 11.7 && f(s, "z") >= 4.3 && f(s, "z") <= 11.7
                        && f(s, "y") >= 3.7 && f(s, "y") <= 12.3, kind + ": stream atoms stay inside the glass");
                    double u = (t * 0.85 + (double) call(m, "hash", k * 19)) % 1;
                    if (u < 0.1) { low += f(s, "y"); lowN++; }
                    if (u > 0.9) { high += f(s, "y"); highN++; }
                }
            }
            h.assertTrue(lowN > 0 && highN > 0 && high / highN - low / lowN > 3, kind + ": atoms rise from the input to the product");
        }
        Object empty = state();
        call(m, "streamAtom", kind("molecular"), 0, List.of(), out, 1.0, 0F, empty);
        h.assertTrue(!visible(empty), "no stream without atoms");
        h.succeed();
    }
}
