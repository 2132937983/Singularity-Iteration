package example.sicore;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.140: the connected models of the fusion reactor's components (loader mio_icif:connected): the vacuum vessel wall, the
 * viewport, the blanket module and the two field coils name the loader and a valid joining rule, their element rules name only
 * sides, the connected textures they use have all four tiles (tools/fusion_ctm_tiles.py), and the coils keep their facing rule.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111140GameTests {
    private Round111140GameTests() {}

    static final String[] CONNECTED = {"vacuum_vessel", "viewport", "blanket", "toroidal_coil", "poloidal_coil"};
    static final Set<String> SIDES = Set.of("north", "south", "west", "east", "up", "down");

    private static boolean exists(String path) {
        try {
            var file = net.neoforged.fml.ModList.get().getModFileById("mio_icif").getFile();
            return java.nio.file.Files.exists(file.findResource(path));
        } catch (RuntimeException e) {
            return false;
        }
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void fusionComponentsConnect(GameTestHelper h) {
        List<String> wrong = new ArrayList<>();
        for (String name : CONNECTED) {
            String path = "/assets/mio_icif/models/block/fusion/block_fusion_" + name + ".json";
            String text = Round33GameTests.resource(path);
            if (text == null) { wrong.add(name + ": no model"); continue; }
            JsonObject model = JsonParser.parseString(text).getAsJsonObject();
            if (!model.has("loader") || !"mio_icif:connected".equals(model.get("loader").getAsString())) wrong.add(name + ": not the connected loader");
            String connect = model.has("connect") ? model.get("connect").getAsString() : "self";
            if (!connect.equals("self") && !connect.equals("same_facing")) wrong.add(name + ": connect " + connect);
            if (name.equals("toroidal_coil") && !connect.equals("same_facing")) wrong.add("the toroidal coil joins regardless of its facing");
            int rules = 0;
            for (JsonElement e : model.getAsJsonArray("elements")) {
                JsonObject o = e.getAsJsonObject();
                for (String key : new String[]{"drop_if", "extend"}) {
                    if (!o.has(key)) continue;
                    rules++;
                    for (JsonElement s : o.getAsJsonArray(key)) if (!SIDES.contains(s.getAsString())) wrong.add(name + ": " + key + " side " + s);
                }
            }
            if (rules == 0) wrong.add(name + ": no element joins anything");
            if (model.has("ctm"))
                for (var entry : model.getAsJsonObject("ctm").entrySet()) {
                    if (!model.getAsJsonObject("textures").has(entry.getKey())) wrong.add(name + ": ctm for an unknown texture " + entry.getKey());
                    String prefix = entry.getValue().getAsString().replace("mio_icif:", "");
                    for (String suffix : new String[]{"_h", "_v", "_none", "_inner"})
                        if (!exists("assets/mio_icif/textures/" + prefix + suffix + ".png")) wrong.add(name + ": missing tile " + prefix + suffix);
                }
        }
        h.assertTrue(wrong.isEmpty(), "connected models: " + wrong);
        h.succeed();
    }
}
