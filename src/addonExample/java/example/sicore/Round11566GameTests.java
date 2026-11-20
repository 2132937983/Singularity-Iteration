package example.sicore;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Checks the flight allocator against the shipped rig: ten independent RCS apertures, two opening
 * acceleration pods, heel lift and the mutually exclusive graviton field emitters.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11566GameTests {
    private Round11566GameTests() {}

    private static final String ROOT = "/assets/mio_icif/armor_models/";

    private static Object variant(String name) {
        return Enum.valueOf(cast(Round33GameTests.common("suit.QuantumNozzles$Variant")), name);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Class<Enum> cast(Class<?> c) { return (Class<Enum>) c; }

    private static List<?> chest(String v) {
        return (List<?>) Round33GameTests.call(Round33GameTests.common("suit.QuantumNozzles"), "chest", variant(v));
    }

    private static List<?> boots(String v) {
        return (List<?>) Round33GameTests.call(Round33GameTests.common("suit.QuantumNozzles"), "boots", variant(v));
    }

    private static Object get(Object n, String accessor) { return Round33GameTests.call(n, accessor); }

    private static float f(Object n, String accessor) { return (Float) get(n, accessor); }

    private static String s(Object n, String accessor) { return String.valueOf(get(n, accessor)); }

    private static JsonObject asset(String file) {
        try (var in = Round33GameTests.common("suit.QuantumNozzles").getResourceAsStream(ROOT + file)) {
            if (in == null) throw new IllegalStateException("no model " + file);
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (java.io.IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static Vec3 vec(JsonArray a, int i) {
        return new Vec3(a.get(i).getAsDouble(), a.get(i + 1).getAsDouble(), a.get(i + 2).getAsDouble());
    }

    private static Vec3 point(Object nozzle) {
        return new Vec3(f(nozzle, "x"), f(nozzle, "y"), f(nozzle, "z"));
    }

    private static Vec3 axis(Object nozzle) {
        return new Vec3(f(nozzle, "dx"), f(nozzle, "dy"), f(nozzle, "dz"));
    }

    private static JsonObject face(JsonObject parts, String part, Vec3 normal) {
        for (JsonElement element : parts.getAsJsonArray(part)) {
            var quad = element.getAsJsonObject();
            if (vec(quad.getAsJsonArray("n"), 0).distanceTo(normal) < .0001) return quad;
        }
        throw new AssertionError("No " + normal + " face in " + part);
    }

    private static Vec3 centre(JsonObject quad) {
        var vertices = quad.getAsJsonArray("v");
        return vec(vertices, 0).add(vec(vertices, 5)).add(vec(vertices, 10)).add(vec(vertices, 15)).scale(.25);
    }

    private static void matches(GameTestHelper h, Vec3 actual, Vec3 expected, String what) {
        h.assertTrue(actual.distanceTo(expected) < .001, what + ": " + actual + " vs " + expected);
    }

    /** Project the actual mark's UV centre onto its native quad, including flipped UV axes. */
    private static Vec3 aperture(JsonObject quad, JsonArray rect) {
        var v = quad.getAsJsonArray("v");
        double du = (rect.get(0).getAsDouble() + .5) / 128 - v.get(3).getAsDouble();
        double dv = (rect.get(1).getAsDouble() + .5) / 128 - v.get(4).getAsDouble();
        double u1 = v.get(8).getAsDouble() - v.get(3).getAsDouble();
        double v1 = v.get(9).getAsDouble() - v.get(4).getAsDouble();
        double u3 = v.get(18).getAsDouble() - v.get(3).getAsDouble();
        double v3 = v.get(19).getAsDouble() - v.get(4).getAsDouble();
        double det = u1 * v3 - u3 * v1;
        double a = (du * v3 - u3 * dv) / det, b = (u1 * dv - du * v1) / det;
        if (!(a > 0 && a < 1 && b > 0 && b < 1)) throw new AssertionError("RCS texel outside face");
        Vec3 p = vec(v, 0);
        return p.add(vec(v, 5).subtract(p).scale(a)).add(vec(v, 15).subtract(p).scale(b));
    }

    /** Each nozzle on the right has its mirror on the left. */
    private static void mirrored(GameTestHelper h, List<?> nozzles, String what) {
        for (Object n : nozzles) {
            String name = s(n, "name");
            if (!name.contains("right")) continue;
            Object twin = nozzles.stream().filter(o -> s(o, "name").equals(name.replace("right", "left"))).findFirst().orElse(null);
            h.assertTrue(twin != null, what + ": " + name + " has no left twin");
            h.assertTrue(Math.abs(f(n, "x") + f(twin, "x")) < 1e-4F && f(n, "y") == f(twin, "y") && f(n, "z") == f(twin, "z")
                && Math.abs(f(n, "dx") + f(twin, "dx")) < 1e-4F
                && f(n, "dy") == f(twin, "dy") && f(n, "dz") == f(twin, "dz"), what + ": " + name + " is not mirrored");
        }
    }

    @GameTest(template = "empty", batch = "round11566_nozzles", timeoutTicks = 20)
    public static void nozzlesSitOnTheModels(GameTestHelper h) {
        JsonObject model = asset("quantum_mounts.json"), parts = model.getAsJsonObject("parts");
        Map<String, String> limbs = new HashMap<>();
        for (var entry : model.getAsJsonArray("segments")) {
            var data = entry.getAsJsonObject();
            limbs.put(data.get("name").getAsString(), data.get("part").getAsString());
        }
        var bindings = asset("quantum_bindings.json").getAsJsonObject("flight");
        var sockets = bindings.getAsJsonArray("sockets");
        h.assertTrue(sockets.size() == 10, "expected ten individually addressed RCS outlets");
        for (String v : List.of("BASE", "PROPULSION")) {
            for (var raw : sockets) {
                var socket = raw.getAsJsonObject();
                String name = socket.get("bone").getAsString();
                var list = socket.get("equipment_slot").getAsString().equals("feet") ? boots(v) : chest(v);
                Object nozzle = named(list, name);
                h.assertTrue(s(nozzle, "part").equalsIgnoreCase(limbs.get(name)), "wrong posed limb for " + name);
                Vec3 normal = axis(nozzle);
                var quad = face(parts, name, normal);
                var rect = socket.getAsJsonArray("atlas_rect");
                h.assertTrue(rect.get(2).getAsInt() == 1 && rect.get(3).getAsInt() == 1, "port is not a single texel: " + name);
                matches(h, point(nozzle), aperture(quad, rect).add(normal.scale(.03)), name);
            }
            for (String side : List.of("left", "right")) {
                String name = "thruster_nozzle_" + side;
                Vec3 down = new Vec3(0, 1, 0);
                Vec3 closed = centre(face(parts, "mach_rail_" + side, down));
                Vec3 hinge = centre(face(parts, "mach_pod_" + side, down.scale(-1)));
                for (float opening : new float[]{0F, .5F, 1F}) {
                    var nozzles = (List<?>) Round33GameTests.call(Round33GameTests.common("suit.QuantumNozzles"), "chest", variant(v), opening);
                    Object nozzle = named(nozzles, name);
                    double angle = Math.toRadians(38 * opening);
                    Vec3 direction = new Vec3(0, Math.cos(angle), Math.sin(angle));
                    Vec3 expected = hinge.add(0, .55 * opening, .42 * opening).add(direction.scale(closed.y - hinge.y));
                    matches(h, point(nozzle), expected, name + " at " + opening);
                    matches(h, axis(nozzle), direction, name + " exhaust axis");
                }
            }
        }
        for (String v : List.of("BASE", "PROPULSION", "GRAVITON")) {
            mirrored(h, chest(v), v);
            mirrored(h, boots(v), v + " boots");
            for (String side : List.of("left", "right")) {
                Object heel = named(boots(v), "sole_" + side);
                matches(h, point(heel), centre(face(parts, "hover_strip_" + side, axis(heel))), "heel " + side);
                Object pad = named(chest("GRAVITON"), "pad_" + side);
                Object shoulder = named(chest("BASE"), "rcs_locator_shoulder_" + side + "_up");
                matches(h, point(pad), point(shoulder), "graviton pad " + side);
                matches(h, axis(pad), axis(shoulder), "graviton field normal " + side);
            }
        }
        Object gyro = named(chest("GRAVITON"), "gyro");
        matches(h, point(gyro), centre(face(parts, "module_graviton", axis(gyro))).add(axis(gyro).scale(.03)), "gyro");
        h.succeed();
    }

    private static long count(List<?> nozzles, String kind) {
        return nozzles.stream().filter(n -> s(n, "kind").equals(kind)).count();
    }

    @GameTest(template = "empty", batch = "round11566_variants", timeoutTicks = 20)
    public static void eachCoreHasItsNozzles(GameTestHelper h) {
        Class<?> variant = Round33GameTests.common("suit.QuantumNozzles$Variant");
        Class<?> core = Round33GameTests.common("suit.core.FrameCore");
        h.assertTrue(variant(("BASE")) == Round33GameTests.call(variant, "of", (Object) null), "no core is not the base set");
        h.assertTrue(variant("PROPULSION") == Round33GameTests.call(variant, "of", Enum.valueOf(cast(core), "PROPULSION")), "propulsion");
        h.assertTrue(variant("GRAVITON") == Round33GameTests.call(variant, "of", Enum.valueOf(cast(core), "GRAVITON")), "graviton");
        List<?> base = chest("BASE"), prop = chest("PROPULSION"), grav = chest("GRAVITON");
        for (List<?> jets : List.of(base, prop)) {
            h.assertTrue(jets.size() == 8 && count(jets, "RCS") == 6 && count(jets, "MAIN") == 2, "wrong chest exhausts");
            for (String side : List.of("left", "right")) {
                Object nozzle = named(jets, "thruster_nozzle_" + side);
                h.assertTrue(s(nozzle, "kind").equals("MAIN") && s(nozzle, "part").equals("BODY"), "unbound main nozzle");
            }
        }
        h.assertTrue(f(named(prop, "thruster_nozzle_left"), "size") > f(named(base, "thruster_nozzle_left"), "size"),
            "the propulsion frame lost its larger main exhaust");
        for (String v : List.of("BASE", "PROPULSION")) {
            List<?> feet = boots(v);
            h.assertTrue(feet.size() == 6 && count(feet, "RCS") == 4 && count(feet, "LIFT") == 2, "wrong boot ports");
        }
        h.assertTrue(grav.size() == 3 && count(grav, "FIELD") == 3 && boots("GRAVITON").size() == 2 && count(boots("GRAVITON"), "FIELD") == 2,
            "the graviton rig must emit fields only");
        h.succeed();
    }

    private static float out(Object n, float[] limbs, float throttle) {
        return (Float) Round33GameTests.call(Round33GameTests.common("suit.QuantumNozzles"), "output", n, limbs, throttle);
    }

    private static Object named(List<?> nozzles, String name) {
        return nozzles.stream().filter(n -> s(n, "name").equals(name)).findFirst().orElseThrow();
    }

    @GameTest(template = "empty", batch = "round11566_output", timeoutTicks = 20)
    public static void hoverLiftsAndCruisePushes(GameTestHelper h) {
        List<?> base = chest("BASE"), feet = boots("BASE");
        float[] hover = {0.45F, 0.45F, 0.75F, 0.75F}, cruise = {0.75F, 0.75F, 1F, 1F};
        Object pod = named(base, "thruster_nozzle_right");
        h.assertTrue(out(pod, hover, 0) == 0 && out(named(base, "thruster_nozzle_left"), hover, 0) == 0, "main thrust burns in hover");
        float[] asymmetrical = {0, 0, .2F, .8F};
        h.assertTrue(out(named(feet, "sole_right"), asymmetrical, 0) == .2F && out(named(feet, "sole_left"), asymmetrical, 0) == .8F,
                "heel lift does not follow the individual foot");
        h.assertTrue(out(pod, cruise, 1F) > .9F && out(named(base, "thruster_nozzle_left"), cruise, 1F) > .9F, "cruise does not push");
        h.assertTrue(out(named(base, "rcs_locator_shoulder_right_up"), cruise, 1F) == 0, "RCS fires before allocation");
        float nan = out(pod, new float[]{Float.NaN}, Float.NaN);
        h.assertTrue(nan == 0 && out(pod, null, 2F) <= 1F, "the output is not bounded");
        h.succeed();
    }
}
