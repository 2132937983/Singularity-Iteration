package example.sicore;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.122: the light trail of the special flight (a short red line with a red curtain hanging under it, one behind each
 * back nozzle). The numbers the client draws with (LightTrailRules) and the path a nozzle leaves (LightTrail). (The mod's
 * common classes by reflection: addon examples do not compile against them.)
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111122GameTests {
    private Round111122GameTests() {}

    private static final Class<?> RULES = Round33GameTests.common("suit.LightTrailRules"), TRAIL = Round33GameTests.common("suit.LightTrail");

    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    private static Object constant(String name) {
        try { return RULES.getField(name).get(null); } catch (ReflectiveOperationException e) { throw new AssertionError(name, e); }
    }

    private static float f(Object o) { return ((Number) o).floatValue(); }

    /** Short (a tenth to a twentieth of the 36-tick demo), grows with the speed, fades out along its length. */
    @GameTest(template = "empty", timeoutTicks = 5)
    public static void trailIsShortAndGrowsWithSpeed(GameTestHelper h) {
        float age = f(constant("MAX_AGE_TICKS"));
        h.assertTrue(age >= 36 / 20F && age <= 36 / 10F, "trail length " + age + " ticks");
        h.assertTrue(f(call(RULES, "level", 0.3)) == 0 && f(call(RULES, "level", 0.9)) == 1 && f(call(RULES, "level", 2.0)) == 1,
            "levels at Mach 0.3/0.9/2: " + call(RULES, "level", 0.3) + ", " + call(RULES, "level", 0.9) + ", " + call(RULES, "level", 2.0));
        float mid = f(call(RULES, "level", 0.6));
        h.assertTrue(mid > 0.3F && mid < 0.7F, "level at Mach 0.6: " + mid);
        h.assertTrue(f(call(RULES, "along", 0F)) == 1 && f(call(RULES, "along", 1F)) == 0 && f(call(RULES, "along", 0.5F)) == 0.5F,
            "fade along the trail: " + call(RULES, "along", 0F) + ", " + call(RULES, "along", 0.5F) + ", " + call(RULES, "along", 1F));
        h.succeed();
    }

    /** The curtain: 0.35 to 0.55 deep, shallower at the end, bright at the line and nothing at its lower edge. */
    @GameTest(template = "empty", timeoutTicks = 5)
    public static void curtainHangsAndFades(GameTestHelper h) {
        float top = f(call(RULES, "curtainHeight", 0F)), end = f(call(RULES, "curtainHeight", 1F));
        h.assertTrue(top >= 0.35F && top <= 0.55F && end < top && end > 0.5F * top, "curtain depth " + top + " to " + end);
        float alpha = f(constant("CURTAIN_TOP_ALPHA"));
        h.assertTrue(alpha >= 0.45F && alpha <= 0.55F, "curtain alpha at the line " + alpha);
        h.assertTrue(f(call(RULES, "curtainFade", 0F)) == 1 && f(call(RULES, "curtainFade", 1F)) == 0, "curtain fade ends");
        float prev = 2;
        for (int i = 0; i <= 20; i++) {
            float v = f(call(RULES, "curtainFade", i / 20F));
            h.assertTrue(v <= prev, "the curtain gets brighter lower down at " + i / 20F);
            prev = v;
        }
        // it hangs straight down under a level path, square to a climbing one, and to the side of a vertical one
        Vec3 side = new Vec3(1, 0, 0);
        Vec3 level = (Vec3) call(RULES, "hang", new Vec3(0, 0, 1), side);
        h.assertTrue(level.distanceTo(new Vec3(0, -1, 0)) < 1.0E-6, "under a level path: " + level);
        Vec3 climb = new Vec3(0, 1, 1), hang = (Vec3) call(RULES, "hang", climb, side);
        h.assertTrue(Math.abs(hang.dot(climb.normalize())) < 1.0E-6 && hang.y < 0 && Math.abs(hang.length() - 1) < 1.0E-6, "under a climb: " + hang);
        Vec3 up = (Vec3) call(RULES, "hang", new Vec3(0, 1, 0), side);
        h.assertTrue(Math.abs(up.y) < 1.0E-6 && Math.abs(up.length() - 1) < 1.0E-6, "beside a vertical path: " + up);
        h.succeed();
    }

    /** One point a tick (the first frame of each tick), the newest sample as the head, nothing older than the trail kept. */
    @GameTest(template = "empty", timeoutTicks = 5)
    public static void nozzlePathKeepsOnlyTheTrail(GameTestHelper h) {
        Object trail;
        try { trail = TRAIL.getConstructor().newInstance(); } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
        int max = ((Number) constant("MAX_POINTS")).intValue();
        float age = f(constant("MAX_AGE_TICKS"));
        // 20 ticks at 4 blocks a tick, 6 frames a tick
        for (int f = 0; f <= 120; f++) {
            double time = 100 + f / 6.0;
            call(trail, "sample", time, new Vec3(0, 64, -4 * (time - 100)), 1F);
        }
        int kept = (int) call(trail, "size");
        h.assertTrue(kept >= 2 && kept <= max, "points kept: " + kept + " of at most " + max);
        double[][] path = (double[][]) call(trail, "path");
        h.assertTrue(path[0][3] == 120 && path[0][2] == -80, "head " + java.util.Arrays.toString(path[0]));
        for (int i = 1; i < path.length; i++) h.assertTrue(path[i][3] < path[i - 1][3], "not newest first at " + i);
        h.assertTrue(path[path.length - 1][3] >= 120 - age - 1 - 1.0E-9, "an old point stayed: " + path[path.length - 1][3]);
        h.assertTrue(path.length >= 2 && 120 - path[path.length - 2][3] <= age + 1.0E-9, "the end of the trail is missing");
        // time going back (a new world) starts over
        call(trail, "sample", 5.0, new Vec3(0, 64, 0), 1F);
        h.assertTrue((int) call(trail, "size") == 1 && ((double[][]) call(trail, "path")).length == 1, "after a rewind: " + call(trail, "size"));
        h.succeed();
    }
}
