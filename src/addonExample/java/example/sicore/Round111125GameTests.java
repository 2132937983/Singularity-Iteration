package example.sicore;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Map;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.125 (armour and flight overhaul E2): the quantum suit's own animation clips drive its moving parts (no GeckoLib).
 * The clip player reads the shipped {@code quantum_armor.animation.json}; the flight state machine picks stowed, hover,
 * maneuver or high speed, crossfades, and folds the pods back after a high-speed flight. (The mod's classes by reflection:
 * addon examples do not compile against them.)
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111125GameTests {
    private Round111125GameTests() {}

    private static final Class<?> ANIM = Round33GameTests.common("suit.SuitAnimation"), VISUAL = Round33GameTests.common("suit.FlightVisual");

    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> clips() {
        String json = Round33GameTests.resource("/assets/mio_icif/animations/quantum_armor.animation.json");
        if (json == null) throw new AssertionError("the animation file does not ship");
        return (Map<String, Object>) call(ANIM, "parse", JsonParser.parseString(json).getAsJsonObject());
    }

    private static float[] sample(Object clip, String bone, String channel, double t) {
        return (float[]) call(clip, "sample", bone, channel, t);
    }

    private static boolean near(float a, float b) { return Math.abs(a - b) < 1.0E-3F; }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object mode(String name) {
        try {
            return Enum.valueOf((Class) Class.forName(VISUAL.getName() + "$Mode"), name);
        } catch (ClassNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    /** The six clips load; the hover bobs and loops, the boost swings the pods 38° in 0.5 s and holds, the glows switch. */
    @GameTest(template = "empty", timeoutTicks = 5)
    public static void clipsPlayFromTheShippedFile(GameTestHelper h) {
        Map<String, Object> clips = clips();
        h.assertTrue(clips.size() == 6, "clips: " + clips.keySet());
        Object hover = clips.get("animation.quantum.hover"), boost = clips.get("animation.quantum.mach4_boost"),
            retract = clips.get("animation.quantum.mach4_retract");
        h.assertTrue(near(sample(hover, "armorHead_float", "position", 0.8)[1], 0.1F), "head ring at 0.8 s");
        h.assertTrue(near(sample(hover, "armorHead_float", "position", 0.8 + 3.2)[1], 0.1F), "the hover does not loop");
        h.assertTrue(near(sample(hover, "armorBody_float", "position", 0.4)[1], -0.06F), "the wing half way down at 0.4 s");
        h.assertTrue(near(sample(boost, "module_mach_pod_left", "rotation", 0.25)[0], 19F)
            && near(sample(boost, "module_mach_pod_left", "rotation", 1.5)[0], 38F), "the pods swing out and hold");
        h.assertTrue(sample(boost, "module_mach_pod_left_glow", "scale", 0.4)[1] == 0F
            && sample(boost, "module_mach_pod_left_glow", "scale", 0.5)[1] == 1F, "the pod glow comes on at 0.5 s");
        h.assertTrue(near(sample(retract, "module_mach_pod_right", "rotation", 0.58)[0], 0F)
            && near(sample(retract, "module_mach_pod_right", "position", 0.04)[2], 0.42F), "the retract folds the pods");
        h.assertTrue(sample(hover, "module_mach_pod_left", "rotation", 1.0) == null, "the hover moves the pods");
        h.succeed();
    }

    /** The mode from flight, throttle, the barrier and the pods. */
    @GameTest(template = "empty", timeoutTicks = 5)
    public static void flightModes(GameTestHelper h) {
        h.assertTrue(call(VISUAL, "mode", false, 1F, true, true) == mode("STOWED"), "not flying");
        h.assertTrue(call(VISUAL, "mode", true, 0.2F, false, true) == mode("HOVER"), "low throttle");
        h.assertTrue(call(VISUAL, "mode", true, 0.6F, false, true) == mode("MANEUVER"), "below the barrier");
        h.assertTrue(call(VISUAL, "mode", true, 0.6F, true, true) == mode("HIGH_SPEED"), "past the barrier");
        h.assertTrue(call(VISUAL, "mode", true, 0.6F, true, false) == mode("MANEUVER"), "a graviton frame has no pods");
        h.succeed();
    }

    /** Hover crossfades in; the barrier swings the pods out; leaving flight folds them back first, then they rest. */
    @GameTest(template = "empty", timeoutTicks = 5)
    public static void flightStateTimeline(GameTestHelper h) {
        Map<String, Object> clips = clips();
        Object s;
        try { s = VISUAL.getConstructor().newInstance(); } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
        call(s, "update", mode("HOVER"), 1.0);
        float start = ((float[]) call(s, "sample", clips, "armorHead_float", "position", 1.0))[1];
        float full = ((float[]) call(s, "sample", clips, "armorHead_float", "position", 1.8))[1];
        h.assertTrue(near(start, 0F) && near(full, 0.1F), "hover ring: " + start + " at entry, " + full + " at 0.8 s");
        call(s, "update", mode("HIGH_SPEED"), 5.0);
        float out = ((float[]) call(s, "sample", clips, "module_mach_pod_left", "rotation", 5.6))[0];
        h.assertTrue(near(out, 38F), "pods 0.6 s past the barrier: " + out);
        float sustained = ((float[]) call(s, "sample", clips, "module_mach_pod_left", "rotation", 9.0))[0];
        h.assertTrue(near(sustained, 38F), "pods while sustained: " + sustained);
        call(s, "update", mode("STOWED"), 10.0);
        float folding = ((float[]) call(s, "sample", clips, "module_mach_pod_left", "rotation", 10.33))[0];
        float folded = ((float[]) call(s, "sample", clips, "module_mach_pod_left", "rotation", 10.7))[0];
        h.assertTrue(folding > 10F && folding < 30F && near(folded, 0F), "retract: " + folding + " then " + folded);
        h.assertTrue((boolean) call(s, "posed", 10.7, clips) && !(boolean) call(s, "posed", 11.0, clips), "the parts are handed back at rest");
        h.succeed();
    }
}
