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
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.66 (quantum flight phase R0): the nozzle table of the quantum suit matches its models. Every port sits on its part
 * (inside the bounds of the part in the armour models, attachments included), every exhaust is a unit vector, the sets
 * mirror left and right, and each frame core has its own set: pods on the base, boosters under the propulsion core, field
 * rings only under the graviton core. In the hover the back main thrust is dark and the lift burns; in the cruise it burns.
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

    /** Part bounds (pixels) {minX, maxX, minY, maxY, minZ, maxZ} from a model file, merged into {@code into}. */
    private static void bounds(String file, Map<String, float[]> into) {
        try (var in = Round33GameTests.common("suit.QuantumNozzles").getResourceAsStream(ROOT + file)) {
            if (in == null) throw new IllegalStateException("no model " + file);
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            if (root.has("parts")) {
                for (var e : root.getAsJsonObject("parts").entrySet()) {
                    JsonElement p = e.getValue();
                    merge(into, e.getKey(), p.isJsonObject() ? p.getAsJsonObject().getAsJsonArray("quads") : p.getAsJsonArray());
                }
            }
            if (root.has("segments")) {
                for (JsonElement seg : root.getAsJsonArray("segments")) {
                    JsonObject o = seg.getAsJsonObject();
                    if (o.get("quads").isJsonArray()) merge(into, o.get("part").getAsString(), o.getAsJsonArray("quads"));
                }
            }
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void merge(Map<String, float[]> into, String part, JsonArray quads) {
        float[] b = into.computeIfAbsent(part, k -> new float[]{Float.MAX_VALUE, -Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE});
        for (JsonElement q : quads) {
            JsonArray v = q.getAsJsonObject().getAsJsonArray("v");
            for (int i = 0; i < 4; i++) for (int k = 0; k < 3; k++) {
                float c = v.get(i * 5 + k).getAsFloat();
                b[k * 2] = Math.min(b[k * 2], c);
                b[k * 2 + 1] = Math.max(b[k * 2 + 1], c);
            }
        }
    }

    private static String partKey(Object n) {
        return s(n, "part").toLowerCase(java.util.Locale.ROOT);
    }

    private static void onModel(GameTestHelper h, List<?> nozzles, Map<String, float[]> parts, String what) {
        for (Object n : nozzles) {
            float[] b = parts.get(partKey(n));
            h.assertTrue(b != null, what + ": no model part " + partKey(n));
            float x = f(n, "x"), y = f(n, "y"), z = f(n, "z"), m = 0.6F;
            h.assertTrue(x >= b[0] - m && x <= b[1] + m && y >= b[2] - m && y <= b[3] + m && z >= b[4] - m && z <= b[5] + m,
                what + ": " + s(n, "name") + " is off its part at " + x + "," + y + "," + z);
            float l = f(n, "dx") * f(n, "dx") + f(n, "dy") * f(n, "dy") + f(n, "dz") * f(n, "dz");
            h.assertTrue(Math.abs(l - 1) < 1e-4F, what + ": " + s(n, "name") + " exhaust is not a unit vector");
        }
    }

    /** Each nozzle on the right has its mirror on the left. */
    private static void mirrored(GameTestHelper h, List<?> nozzles, String what) {
        for (Object n : nozzles) {
            String name = s(n, "name");
            if (!name.contains("right")) continue;
            Object twin = nozzles.stream().filter(o -> s(o, "name").equals(name.replace("right", "left"))).findFirst().orElse(null);
            h.assertTrue(twin != null, what + ": " + name + " has no left twin");
            h.assertTrue(Math.abs(f(n, "x") + f(twin, "x")) < 0.45F && Math.abs(f(n, "dx") + f(twin, "dx")) < 1e-4F
                && f(n, "dy") == f(twin, "dy") && f(n, "dz") == f(twin, "dz"), what + ": " + name + " is not mirrored");
        }
    }

    @GameTest(template = "empty", batch = "round11566_nozzles", timeoutTicks = 20)
    public static void nozzlesSitOnTheModels(GameTestHelper h) {
        Map<String, float[]> base = new HashMap<>(), propulsion = new HashMap<>(), graviton = new HashMap<>(), feet = new HashMap<>();
        bounds("quantum_chestplate.json", base);
        bounds("quantum_chestplate.json", propulsion);
        bounds("attachments/propulsion_quantum_chestplate.json", propulsion);
        bounds("quantum_chestplate.json", graviton);
        bounds("attachments/graviton_quantum_chestplate.json", graviton);
        bounds("quantum_boots.json", feet);
        onModel(h, chest("BASE"), base, "base");
        onModel(h, chest("PROPULSION"), propulsion, "propulsion");
        onModel(h, chest("GRAVITON"), graviton, "graviton");
        onModel(h, boots("BASE"), feet, "boots");
        onModel(h, boots("GRAVITON"), feet, "graviton boots");
        for (String v : new String[]{"BASE", "PROPULSION", "GRAVITON"}) {
            mirrored(h, chest(v), v);
            mirrored(h, boots(v), v + " boots");
        }
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
        h.assertTrue(base.stream().filter(n -> s(n, "name").startsWith("pod_")).count() == 4 && count(base, "MAIN") == 4, "the base has not four back pods");
        h.assertTrue(count(prop, "MAIN") == 2 && prop.stream().filter(n -> s(n, "kind").equals("MAIN")).allMatch(n -> f(n, "y") > 12 && f(n, "z") > 7),
            "the propulsion main thrust is not the two boosters");
        h.assertTrue(prop.stream().noneMatch(n -> s(n, "name").startsWith("pod_") || s(n, "name").equals("spine")), "the boosters do not cover the pods");
        h.assertTrue(count(grav, "FIELD") == grav.size() && count(boots("GRAVITON"), "FIELD") == 2, "a graviton suit has flames");
        h.assertTrue(count(base, "RCS") >= 4, "too few attitude jets");
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
        Object pod = named(base, "pod_right_upper"), tail = named(base, "tail"), palm = named(base, "palm_left"), crown = named(base, "crown");
        h.assertTrue(out(pod, hover, 0F) == 0 && out(named(feet, "heel_right"), hover, 0F) == 0, "the main thrust burns in the hover");
        h.assertTrue(out(tail, hover, 0F) > 0.5F && out(named(feet, "sole_left"), hover, 0F) == 0.75F && out(palm, hover, 0F) == 0.45F,
            "the lift does not hold the hover");
        h.assertTrue(out(pod, cruise, 1F) > 0.9F && out(tail, cruise, 1F) < out(tail, hover, 0F), "the cruise does not push");
        h.assertTrue(out(crown, cruise, 1F) == 0, "an attitude jet burns before the allocator");
        float nan = out(pod, new float[]{Float.NaN}, Float.NaN);
        h.assertTrue(nan == 0 && out(pod, null, 2F) <= 1F, "the output is not bounded");
        h.succeed();
    }
}
