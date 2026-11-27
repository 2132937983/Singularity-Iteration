package example.sicore;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforgespi.language.IModInfo;

/**
 * 0.1.11.225 (Wingman motion A0): the keyframe rig and the GeckoLib bridge. {@code WingmanRig} flattens the seven AN1 bones
 * onto the six vanilla parts (the rest pose is the vanilla one, the calibration pose of the spec comes out as authored, a
 * bent body carries its arms); {@code WingmanKeyframeChoice} plays keyframes on the ground only and picks the hit reaction
 * by side; the shipped rig and animation file hold the 7 bones and the 24 animations of part 1; GeckoLib is an optional
 * dependency. The common classes are reached by reflection.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111225GameTests {
    private Round111225GameTests() {}

    private static final double EPS = 1.0E-3;

    private static Class<?> rig() { return Round33GameTests.common("armory.WingmanRig"); }

    private static Class<?> choice() { return Round33GameTests.common("armory.WingmanKeyframeChoice"); }

    /** Flattened parts from authored degrees and pixels (7 bones x 3). */
    private static Object[] flat(float[][] deg, float[][] px) {
        float[][][] local = (float[][][]) Round33GameTests.call(rig(), "fromAuthored", deg, px);
        return (Object[]) Round33GameTests.call(rig(), "flatten", local[0], local[1]);
    }

    private static float f(Object part, String c) { return (Float) Round33GameTests.call(part, c); }

    private static void near(GameTestHelper h, Object part, String what, float x, float y, float z, float xr, float yr, float zr) {
        float[] got = {f(part, "x"), f(part, "y"), f(part, "z"), f(part, "xRot"), f(part, "yRot"), f(part, "zRot")};
        float[] want = {x, y, z, xr, yr, zr};
        for (int i = 0; i < 6; i++)
            h.assertTrue(Math.abs(got[i] - want[i]) < EPS, what + ": " + java.util.Arrays.toString(got) + " instead of " + java.util.Arrays.toString(want));
    }

    private static float[][] zero() { return new float[7][3]; }

    @GameTest(template = "empty", batch = "round111225_rest", timeoutTicks = 20)
    public static void theRestPoseIsTheVanillaOne(GameTestHelper h) {
        Object[] p = flat(zero(), zero());
        near(h, p[1], "body", 0, 0, 0, 0, 0, 0);
        near(h, p[2], "head", 0, 0, 0, 0, 0, 0);
        near(h, p[3], "right arm", -5, 2, 0, 0, 0, 0);
        near(h, p[4], "left arm", 5, 2, 0, 0, 0, 0);
        near(h, p[5], "right leg", -1.9F, 12, 0, 0, 0, 0);
        near(h, p[6], "left leg", 1.9F, 12, 0, 0, 0, 0);
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round111225_calibrate", timeoutTicks = 20)
    public static void theCalibrationPoseComesOutAsAuthored(GameTestHelper h) {
        // the last frame of animation.wingman.calibrate (docs/art/an1/spec.json)
        float[][] deg = zero(), px = zero();
        px[0][1] = 2;          // root up 2 pixels
        deg[3][0] = -90;       // right arm held forward
        deg[6][0] = 30;        // left leg back
        deg[2][1] = -45;       // head to its own left
        Object[] p = flat(deg, px);
        float q = (float) (Math.PI / 2);
        near(h, p[3], "right arm", -5, 0, 0, -q, 0, 0);
        near(h, p[6], "left leg", 1.9F, 10, 0, (float) Math.toRadians(30), 0, 0);
        near(h, p[2], "head", 0, -2, 0, 0, (float) Math.toRadians(-45), 0);
        near(h, p[1], "body", 0, -2, 0, 0, 0, 0);
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round111225_bend", timeoutTicks = 20)
    public static void aBentBodyCarriesItsArms(GameTestHelper h) {
        float[][] deg = zero();
        deg[1][0] = 30;        // the body bends 30 degrees about x
        Object[] p = flat(deg, zero());
        float a = (float) Math.toRadians(30);
        // the right arm's pivot (-5, 2, 0) turns about the body's pivot (0, 0, 0): y = 2 cos 30, z = 2 sin 30
        near(h, p[3], "right arm", -5, (float) (2 * Math.cos(a)), (float) (2 * Math.sin(a)), a, 0, 0);
        near(h, p[2], "head", 0, 0, 0, a, 0, 0);
        near(h, p[5], "right leg", -1.9F, 12, 0, 0, 0, 0);   // the legs hang from the root, not from the body
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round111225_choice", timeoutTicks = 20)
    public static void keyframesOnTheGroundAndHitsBySide(GameTestHelper h) {
        h.assertTrue(Round33GameTests.call(choice(), "base", false, 100f, 7, 500) == null, "a flying Wingman plays keyframes");
        h.assertTrue("land_light".equals(Round33GameTests.call(choice(), "base", true, 2f, 7, 500)), "no landing animation");
        String idle = (String) Round33GameTests.call(choice(), "base", true, 100f, 7, 500);
        h.assertTrue(List.of("idle_stand", "idle_look", "idle_check").contains(idle), "a landed Wingman plays " + idle);
        int stands = 0;
        for (int t = 0; t < 120 * 50; t += 120) if ("idle_stand".equals(Round33GameTests.call(choice(), "base", true, 100f, 7, t))) stands++;
        h.assertTrue(stands > 25 && stands < 50, "idle_stand in " + stands + " of 50 windows (the look round and the check are the exception)");
        h.assertTrue("hit_front".equals(Round33GameTests.call(choice(), "hit", 30f, 40f)), "a blow from the front");
        h.assertTrue("hit_back".equals(Round33GameTests.call(choice(), "hit", 30f, 200f)), "a blow from behind");
        h.assertTrue("hit_right".equals(Round33GameTests.call(choice(), "hit", 0f, 90f)), "a blow from the right");
        h.assertTrue("hit_left".equals(Round33GameTests.call(choice(), "hit", 0f, -90f)), "a blow from the left");
        h.succeed();
    }

    private static JsonObject json(String path) {
        try (InputStream in = rig().getResourceAsStream("/assets/mio_icif/" + path)) {   // the mod's own module
            if (in == null) throw new AssertionError("missing asset " + path);
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (java.io.IOException e) {
            throw new AssertionError(path, e);
        }
    }

    @GameTest(template = "empty", batch = "round111225_assets", timeoutTicks = 20)
    public static void theRigAndPartOneAreShipped(GameTestHelper h) {
        JsonObject geo = json("geo/entity/si_humanoid_rig.geo.json").getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        java.util.Set<String> bones = new java.util.HashSet<>();
        geo.getAsJsonArray("bones").forEach(b -> bones.add(b.getAsJsonObject().get("name").getAsString()));
        h.assertTrue(bones.equals(java.util.Set.of(WINGMAN_BONES)), "rig bones " + bones);
        JsonObject anims = json("animations/entity/wingman.animation.json").getAsJsonObject("animations");
        for (String n : PART_ONE) h.assertTrue(anims.has("animation.wingman." + n), "missing animation.wingman." + n);
        // the animations the code plays now
        for (String n : new String[]{"land_light", "idle_stand", "idle_look", "idle_check", "hit_front", "hit_back", "hit_left", "hit_right"})
            h.assertTrue(anims.has("animation.wingman." + n), "the code plays animation.wingman." + n + ", which is missing");
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round111225_optional", timeoutTicks = 20)
    public static void geckoLibIsOptional(GameTestHelper h) {
        IModInfo info = ModList.get().getModContainerById("mio_icif").orElseThrow().getModInfo();
        var dep = info.getDependencies().stream().filter(d -> d.getModId().equals("geckolib")).findFirst();
        h.assertTrue(dep.isPresent(), "GeckoLib is not declared");
        h.assertTrue(dep.get().getType() == IModInfo.DependencyType.OPTIONAL, "GeckoLib is declared " + dep.get().getType());
        h.succeed();
    }

    private static final String[] WINGMAN_BONES = {"root", "body", "head", "right_arm", "left_arm", "right_leg", "left_leg"};
    private static final String[] PART_ONE = {"calibrate", "idle_stand", "idle_look", "idle_check", "walk", "run", "skate", "jump_takeoff",
        "thrust_hop", "air_rise", "air_fall", "land_light", "land_heavy", "step_up", "turn_l", "turn_r", "cover_crouch", "hit_front", "hit_back",
        "hit_left", "hit_right", "stagger", "lowpower_slump", "lowpower_wake"};
}
