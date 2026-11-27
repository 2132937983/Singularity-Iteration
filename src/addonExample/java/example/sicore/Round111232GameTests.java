package example.sicore;

import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.232 (SlashBlade collab, art batch SB1): the three named blades render their own models. Each blade's
 * {@code render.model} and {@code render.texture} point at a shipped file under {@code assets/mio_icif/model/slashblade/},
 * the OBJ holds every object SlashBlade draws (the eight states and the seven luminous layers), and each blade ships the
 * {@code charge.png} its scabbard glow will scroll. The rest is checked offline by {@code tools/art/check_sb1.py}.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111232GameTests {
    private Round111232GameTests() {}

    private static final String[] OBJECTS = {"blade", "blade_damaged", "blade_fragment", "sheath", "effect", "item_blade",
        "item_damaged", "item_bladens", "blade_luminous", "blade_damaged_luminous", "blade_fragment_luminous", "sheath_luminous",
        "item_blade_luminous", "item_damaged_luminous", "item_bladens_luminous"};

    private static InputStream open(String path) {
        return Round33GameTests.common("armory.WingmanStrikes").getResourceAsStream("/" + path);
    }

    private static String asset(GameTestHelper h, String b, String key) {
        JsonObject render = Round111231GameTests.json("data/mio_icif/slashblade/named_blades/" + b + ".json").getAsJsonObject("render");
        h.assertTrue(render.has(key), b + " has no render." + key);
        ResourceLocation id = ResourceLocation.parse(render.get(key).getAsString());
        h.assertTrue("mio_icif".equals(id.getNamespace()), b + " draws " + id + ", not its own " + key);
        return "assets/" + id.getNamespace() + "/" + id.getPath();
    }

    @GameTest(template = "empty", batch = "round111232_models", timeoutTicks = 20)
    public static void eachBladeDrawsItsOwnModel(GameTestHelper h) {
        for (String b : Round111231GameTests.BLADES) {
            String model = asset(h, b, "model"), texture = asset(h, b, "texture");
            h.assertTrue(model.endsWith("/model.obj") && texture.endsWith("/texture.png"), b + " draws " + model + " / " + texture);
            Set<String> objects = new HashSet<>();
            try (InputStream in = open(model)) {
                h.assertTrue(in != null, b + ": " + model + " is not shipped");
                BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                for (String line; (line = r.readLine()) != null; )
                    if (line.startsWith("o ") || line.startsWith("g ")) objects.add(line.substring(2).trim());
            } catch (java.io.IOException e) {
                throw new AssertionError(model, e);
            }
            for (String o : OBJECTS) h.assertTrue(objects.contains(o), b + ": " + model + " has no object " + o);
            for (String f : new String[]{texture, texture.replace("texture.png", "charge.png")})
                try (InputStream in = open(f)) {
                    h.assertTrue(in != null, b + ": " + f + " is not shipped");
                } catch (java.io.IOException e) {
                    throw new AssertionError(f, e);
                }
        }
        h.succeed();
    }
}
