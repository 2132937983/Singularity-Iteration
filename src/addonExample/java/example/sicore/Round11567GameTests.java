package example.sicore;

import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.67 (quantum flight phase R1): the reaction control. The hover stands on the lift nozzles with every attitude jet
 * dark; the stick and the drift fire the jets that push the right way and never their opposites; yaw and pitch fire the
 * jets whose lever turns the body the right way, in the hover and in the cruise; the brake of the cruise cuts the main
 * thrust and fires the forward-facing jets; the jets burn in on/off pulses whose width follows the demand.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11567GameTests {
    private Round11567GameTests() {}

    private static final float[] HOVER_LIMBS = {0.45F, 0.45F, 0.75F, 0.75F}, CRUISE_LIMBS = {0.75F, 0.75F, 1F, 1F};

    private static Class<?> rcs() { return Round33GameTests.common("suit.RcsAllocator"); }

    private static Object variant(String name) {
        @SuppressWarnings({"unchecked", "rawtypes"})
        Class<Enum> c = (Class<Enum>) Round33GameTests.common("suit.QuantumNozzles$Variant");
        return Enum.valueOf(c, name);
    }

    private static List<Object> suit(String v) {
        Class<?> table = Round33GameTests.common("suit.QuantumNozzles");
        List<Object> all = new java.util.ArrayList<>((List<?>) Round33GameTests.call(table, "chest", variant(v)));
        all.addAll((List<?>) Round33GameTests.call(table, "boots", variant(v)));
        return all;
    }

    /** cruise, stick forward, stick left, view velocity {left, down, back}, yaw rate, pitch rate, brake. */
    private static Object command(float cruise, float forward, float left, float[] velocity, float yaw, float pitch, float brake) {
        return Round33GameTests.call(rcs(), "command", cruise, forward, left, velocity, yaw, pitch, brake);
    }

    private static float[] allocate(List<Object> nozzles, Object command, boolean cruise, float brake, int tick) {
        return (float[]) Round33GameTests.call(rcs(), "allocate", nozzles, command, cruise ? CRUISE_LIMBS : HOVER_LIMBS, cruise ? 1F : 0F, brake, tick);
    }

    private static String name(Object n) { return String.valueOf(Round33GameTests.call(n, "name")); }

    private static String kind(Object n) { return String.valueOf(Round33GameTests.call(n, "kind")); }

    /** Names of the jets that burn in any tick of one pulse frame. */
    private static java.util.Set<String> fired(List<Object> nozzles, Object command, boolean cruise, float brake) {
        java.util.Set<String> out = new java.util.TreeSet<>();
        for (int t = 0; t < 4; t++) {
            float[] o = allocate(nozzles, command, cruise, brake, t);
            for (int i = 0; i < o.length; i++) if (o[i] > 0 && kind(nozzles.get(i)).equals("RCS")) out.add(name(nozzles.get(i)));
        }
        return out;
    }

    /** Exact active ports over one full PWM frame; an opposite firing is a failure. */
    private static java.util.Set<String> ports(String... names) {
        return java.util.Arrays.stream(names).map(name -> "rcs_locator_" + name).collect(java.util.stream.Collectors.toSet());
    }

    private static final float[] STILL = {0, 0, 0};

    @GameTest(template = "empty", batch = "round11567_hover", timeoutTicks = 20)
    public static void hoverStandsOnTheLift(GameTestHelper h) {
        List<Object> base = suit("BASE");
        Object still = command(0, 0, 0, STILL, 0, 0, 0);
        h.assertTrue(fired(base, still, false, 0).isEmpty(), "attitude jets burn in a still hover: " + fired(base, still, false, 0));
        float[] o = allocate(base, still, false, 0, 0);
        for (int i = 0; i < o.length; i++) {
            String k = kind(base.get(i));
            if (k.equals("MAIN")) h.assertTrue(o[i] == 0, "main thrust in the hover: " + name(base.get(i)));
            if (k.equals("LIFT")) h.assertTrue(o[i] > 0.3F, "a lift nozzle is dark in the hover: " + name(base.get(i)));
        }
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round11567_strafe", timeoutTicks = 20)
    public static void strafeAndDriftFireTheRightSide(GameTestHelper h) {
        List<Object> base = suit("BASE");
        var left = fired(base, command(0, 0, 1, STILL, 0, 0, 0), false, 0);
        h.assertTrue(left.equals(ports("waist_right_out")), "strafe left: " + left);
        var right = fired(base, command(0, 0, -1, STILL, 0, 0, 0), false, 0);
        h.assertTrue(right.equals(ports("waist_left_out")), "strafe right: " + right);
        var forward = fired(base, command(0, 1, 0, STILL, 0, 0, 0), false, 0);
        h.assertTrue(forward.equals(ports("calf_left_back", "calf_right_back")), "forward stick: " + forward);
        var stop = fired(base, command(0, 0, 0, new float[]{0, 0, -0.3F}, 0, 0, 0), false, 0);
        h.assertTrue(stop.equals(ports("shoulder_left_front", "shoulder_right_front")), "the hover does not brake: " + stop);
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round11567_yaw", timeoutTicks = 20)
    public static void yawFiresTheLevers(GameTestHelper h) {
        List<Object> base = suit("BASE");
        var hover = fired(base, command(0, 0, 0, STILL, 90, 0, 0), false, 0);
        h.assertTrue(hover.equals(ports("calf_left_back", "shoulder_right_front", "waist_right_out")), "hover yaw right: " + hover);
        var hoverLeft = fired(base, command(0, 0, 0, STILL, -90, 0, 0), false, 0);
        h.assertTrue(hoverLeft.equals(ports("calf_right_back", "shoulder_left_front", "waist_left_out")), "hover yaw left: " + hoverLeft);
        var cruise = fired(base, command(1, 0, 0, STILL, 90, 0, 0), true, 0);
        h.assertTrue(cruise.equals(ports("waist_left_out", "calf_left_down", "shoulder_right_up")), "cruise yaw right: " + cruise);
        var prop = fired(suit("PROPULSION"), command(0, 0, 0, STILL, 90, 0, 0), false, 0);
        h.assertTrue(prop.equals(hover), "the propulsion frame changes the direction of the RCS response: " + prop);
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round11567_pitch", timeoutTicks = 20)
    public static void pitchFiresTheLevers(GameTestHelper h) {
        List<Object> base = suit("BASE");
        var up = fired(base, command(1, 0, 0, STILL, 0, 90, 0), true, 0);
        h.assertTrue(up.equals(ports("shoulder_left_up", "shoulder_right_up", "shoulder_left_front", "shoulder_right_front", "calf_left_back", "calf_right_back")), "nose up: " + up);
        var down = fired(base, command(1, 0, 0, STILL, 0, -90, 0), true, 0);
        h.assertTrue(down.equals(ports("calf_left_down", "calf_right_down")), "nose down: " + down);
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round11567_brake", timeoutTicks = 20)
    public static void cruiseBrakeCutsTheMainAndFiresForward(GameTestHelper h) {
        List<Object> base = suit("BASE");
        Object brake = command(1, 0, 0, STILL, 0, 0, 1);
        var fired = fired(base, brake, true, 1);
        h.assertTrue(fired.equals(ports("shoulder_right_up", "shoulder_left_up")), "the brake jets: " + fired);
        float[] braking = allocate(base, brake, true, 1, 0), cruising = allocate(base, command(1, 0, 0, STILL, 0, 0, 0), true, 0, 0);
        for (int i = 0; i < braking.length; i++) {
            if (!kind(base.get(i)).equals("MAIN")) continue;
            h.assertTrue(braking[i] == 0 && cruising[i] > 0.5F, "the brake does not cut the main thrust: " + name(base.get(i)));
        }
        h.succeed();
    }

    private static float pulse(float score, int tick, int phase) {
        return (Float) Round33GameTests.call(rcs(), "pulse", score, tick, phase);
    }

    @GameTest(template = "empty", batch = "round11567_pulse", timeoutTicks = 20)
    public static void jetsBurnInPulses(GameTestHelper h) {
        int half = 0, full = 0, weak = 0;
        for (int t = 0; t < 8; t++) {
            float a = pulse(0.5F, t, 1), b = pulse(1F, t, 2), c = pulse(0.1F, t, 0);
            h.assertTrue((a == 0 || a == 1) && (b == 0 || b == 1), "a pulse is not on or off");
            half += (int) a; full += (int) b; weak += (int) c;
        }
        h.assertTrue(half == 4 && full == 8 && weak == 0, "the pulse width does not follow the demand: " + half + "/" + full + "/" + weak);
        h.assertTrue(pulse(Float.NaN, 0, 0) == 0 && pulse(0.2F, -3, 0) + pulse(0.2F, -2, 0) + pulse(0.2F, -1, 0) + pulse(0.2F, 0, 0) == 1,
            "a pulse with odd input");
        h.succeed();
    }
}
