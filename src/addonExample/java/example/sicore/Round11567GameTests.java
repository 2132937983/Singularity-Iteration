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
        h.assertTrue(left.contains("pod_right_side_upper") && left.contains("forearm_right"), "strafe left does not push from the right: " + left);
        h.assertTrue(left.stream().noneMatch(n -> n.contains("left_side") || n.equals("forearm_left")), "strafe left fires the left jets: " + left);
        var forward = fired(base, command(0, 1, 0, STILL, 0, 0, 0), false, 0);
        h.assertTrue(forward.contains("spine") && !forward.contains("chest_right"), "forward stick: " + forward);
        // the stick released while drifting forwards (back velocity negative): the chest jets stop the drift
        var stop = fired(base, command(0, 0, 0, new float[]{0, 0, -0.3F}, 0, 0, 0), false, 0);
        h.assertTrue(stop.contains("chest_right") && stop.contains("chest_left") && !stop.contains("spine"), "the hover does not brake: " + stop);
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round11567_yaw", timeoutTicks = 20)
    public static void yawFiresTheLevers(GameTestHelper h) {
        List<Object> base = suit("BASE");
        var hover = fired(base, command(0, 0, 0, STILL, 90, 0, 0), false, 0);
        h.assertTrue(hover.contains("pod_right_side_upper") && hover.contains("pod_right_side_lower"), "hover yaw right: " + hover);
        h.assertTrue(hover.stream().noneMatch(n -> n.startsWith("pod_left_side")), "hover yaw right fires the left pods: " + hover);
        var hoverLeft = fired(base, command(0, 0, 0, STILL, -90, 0, 0), false, 0);
        h.assertTrue(hoverLeft.contains("pod_left_side_upper") && hoverLeft.stream().noneMatch(n -> n.startsWith("pod_right_side")), "hover yaw left: " + hoverLeft);
        // the cruise: head first, so the jets above the waist on the left swing the head to the right
        var cruise = fired(base, command(1, 0, 0, STILL, 90, 0, 0), true, 0);
        h.assertTrue(cruise.contains("pod_left_side_upper") && !cruise.contains("pod_right_side_upper"), "cruise yaw right: " + cruise);
        var prop = fired(suit("PROPULSION"), command(0, 0, 0, STILL, 90, 0, 0), false, 0);
        h.assertTrue(prop.contains("booster_right_side") && !prop.contains("booster_left_side"), "propulsion yaw right: " + prop);
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round11567_pitch", timeoutTicks = 20)
    public static void pitchFiresTheLevers(GameTestHelper h) {
        List<Object> base = suit("BASE");
        var up = fired(base, command(1, 0, 0, STILL, 0, 90, 0), true, 0);
        h.assertTrue(up.contains("crown") && !up.contains("spine"), "nose up: " + up);
        var down = fired(base, command(1, 0, 0, STILL, 0, -90, 0), true, 0);
        h.assertTrue(down.contains("spine") && !down.contains("crown"), "nose down: " + down);
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round11567_brake", timeoutTicks = 20)
    public static void cruiseBrakeCutsTheMainAndFiresForward(GameTestHelper h) {
        List<Object> base = suit("BASE");
        Object brake = command(1, 0, 0, STILL, 0, 0, 1);
        var fired = fired(base, brake, true, 1);
        h.assertTrue(fired.contains("crown") && fired.contains("retro_right") && fired.contains("retro_left"), "the brake jets: " + fired);
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
