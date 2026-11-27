package example.sicore;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforgespi.language.IModInfo;

/**
 * 0.1.11.231 (SlashBlade collab B0): the three named blades (nano, quantum, singularity) and their upgrade path. The blade
 * and recipe files only load with SlashBlade (a {@code neoforge:mod_loaded} condition), so these tests read the shipped
 * files: every blade has its name, a slash art and its colour; each upgrade takes the blade before it (by name), more refine
 * and more proud soul than the last, and only items Singularity Iteration already has (no new items, decision 3); SlashBlade
 * is an optional dependency.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111231GameTests {
    private Round111231GameTests() {}

    static final String[] BLADES = {"nano_blade", "quantum_blade", "singularity_blade"};

    static JsonObject json(String path) {
        Class<?> anchor = Round33GameTests.common("armory.WingmanStrikes");   // a class of the mod's own module
        try (InputStream in = anchor.getResourceAsStream("/" + path)) {
            if (in == null) throw new AssertionError("missing " + path);
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (java.io.IOException e) {
            throw new AssertionError(path, e);
        }
    }

    private static boolean onlyWithSlashBlade(JsonObject o) {
        JsonArray c = o.getAsJsonArray("neoforge:conditions");
        if (c == null || c.size() != 1) return false;
        JsonObject first = c.get(0).getAsJsonObject();
        return "neoforge:mod_loaded".equals(first.get("type").getAsString()) && "slashblade".equals(first.get("modid").getAsString());
    }

    @GameTest(template = "empty", batch = "round111231_blades", timeoutTicks = 20)
    public static void theThreeBladesAreShipped(GameTestHelper h) {
        java.util.Set<Integer> colours = new java.util.HashSet<>();
        double attack = 0;
        for (String b : BLADES) {
            JsonObject o = json("data/mio_icif/slashblade/named_blades/" + b + ".json");
            h.assertTrue(onlyWithSlashBlade(o), b + " does not load only with SlashBlade");
            h.assertTrue(("mio_icif:" + b).equals(o.get("name").getAsString()), b + " is named " + o.get("name"));
            JsonObject p = o.getAsJsonObject("properties");
            h.assertTrue(p.get("slash_art").getAsString().startsWith("slashblade:"), b + " has no slash art");
            double a = p.get("attack_base").getAsDouble();
            h.assertTrue(a > attack, b + " hits no harder (" + a + ") than the blade before it");
            attack = a;
            colours.add(o.getAsJsonObject("render").get("summon_sword_color").getAsInt());
        }
        h.assertTrue(colours.size() == 3, "the three blades share a summoned-sword colour");
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round111231_upgrades", timeoutTicks = 20)
    public static void eachUpgradeTakesTheBladeBeforeIt(GameTestHelper h) {
        String before = null;
        int refine = -1, soul = -1;
        for (String b : BLADES) {
            JsonObject r = json("data/mio_icif/recipe/compat/slashblade/" + b + ".json");
            h.assertTrue(onlyWithSlashBlade(r), "the " + b + " recipe does not load only with SlashBlade");
            h.assertTrue("slashblade:shaped_blade".equals(r.get("type").getAsString()), b + " is not a shaped blade recipe");
            h.assertTrue(("mio_icif:" + b).equals(r.get("blade").getAsString()), "the " + b + " recipe makes " + r.get("blade"));
            int blades = 0;
            for (var e : r.getAsJsonObject("key").entrySet()) {
                JsonObject k = e.getValue().getAsJsonObject();
                if (k.has("type") && "slashblade:blade".equals(k.get("type").getAsString())) {
                    blades++;
                    JsonObject req = k.getAsJsonObject("request");
                    JsonElement name = req.get("name");
                    h.assertTrue(before == null ? name == null : name != null && ("mio_icif:" + before).equals(name.getAsString()),
                        b + " is made from " + name + " instead of " + before);
                    h.assertTrue(req.get("refine").getAsInt() > refine && req.get("proud_soul").getAsInt() > soul,
                        b + " asks no more refine or proud soul than the blade before it");
                    refine = req.get("refine").getAsInt();
                    soul = req.get("proud_soul").getAsInt();
                } else {
                    // decision 3: no new items, only what the mod already has
                    ResourceLocation id = ResourceLocation.parse(k.get("item").getAsString());
                    h.assertTrue("mio_icif".equals(id.getNamespace()), b + " takes " + id + ", not one of the mod's items");
                    h.assertTrue(BuiltInRegistries.ITEM.containsKey(id) && BuiltInRegistries.ITEM.get(id) != Items.AIR, b + " takes " + id + ", which does not exist");
                }
            }
            h.assertTrue(blades == 1, b + " takes " + blades + " blades");
            before = b;
        }
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round111231_names", timeoutTicks = 20)
    public static void theBladesAreNamedInEveryLanguage(GameTestHelper h) {
        for (String lang : new String[]{"zh_cn", "en_us", "ja_jp", "ru_ru"}) {
            JsonObject l = json("assets/mio_icif/lang/" + lang + ".json");
            for (String b : BLADES) {
                h.assertTrue(l.has("item.mio_icif." + b), lang + " has no name for " + b);
                h.assertTrue(l.has("item.mio_icif." + b + ".desc"), lang + " has no description for " + b);
            }
        }
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round111231_optional", timeoutTicks = 20)
    public static void slashBladeIsOptional(GameTestHelper h) {
        IModInfo info = ModList.get().getModContainerById("mio_icif").orElseThrow().getModInfo();
        var dep = info.getDependencies().stream().filter(d -> d.getModId().equals("slashblade")).findFirst();
        h.assertTrue(dep.isPresent(), "SlashBlade is not declared");
        h.assertTrue(dep.get().getType() == IModInfo.DependencyType.OPTIONAL, "SlashBlade is declared " + dep.get().getType());
        h.succeed();
    }
}
