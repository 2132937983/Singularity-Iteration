package example.sicore;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.149: the fusion reactor's diagnostic viewport has connected textures again, on its own texture: its model is one full
 * cube on the mio_icif:connected loader, joining viewports only, every face takes the connected texture viewport, and the four
 * tiles (tools/viewport_ctm_111149.py) are there, 16 x 16 like the texture.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111149GameTests {
    private Round111149GameTests() {}

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void viewportConnects(GameTestHelper h) throws java.io.IOException {
        List<String> wrong = new ArrayList<>();
        String text = Round33GameTests.resource("/assets/mio_icif/models/block/fusion/block_fusion_viewport.json");
        if (text == null) {
            h.fail("no viewport model");
            return;
        }
        JsonObject model = JsonParser.parseString(text).getAsJsonObject();
        if (!model.has("loader") || !"mio_icif:connected".equals(model.get("loader").getAsString())) wrong.add("not the connected loader");
        if (model.has("connect") && !"self".equals(model.get("connect").getAsString())) wrong.add("joins more than viewports");
        if (!model.has("render_type") || !model.get("render_type").getAsString().endsWith("translucent")) wrong.add("not translucent");
        String prefix = model.has("ctm") && model.getAsJsonObject("ctm").has("all") ? model.getAsJsonObject("ctm").get("all").getAsString() : null;
        if (!"mio_icif:block/fusion/viewport".equals(prefix)) wrong.add("the faces do not take the connected texture viewport: " + prefix);
        var elements = model.getAsJsonArray("elements");
        if (elements == null || elements.size() != 1) wrong.add("not one cube");
        else {
            JsonObject cube = elements.get(0).getAsJsonObject();
            if (!cube.get("from").toString().equals("[0,0,0]") || !cube.get("to").toString().equals("[16,16,16]")) wrong.add("not a full cube");
            var faces = cube.getAsJsonObject("faces");
            for (String side : new String[]{"down", "up", "north", "south", "west", "east"})
                if (faces == null || !faces.has(side) || !"#all".equals(faces.getAsJsonObject(side).get("texture").getAsString())) wrong.add("the " + side + " face is not #all");
        }
        var file = net.neoforged.fml.ModList.get().getModFileById("mio_icif").getFile();
        for (String suffix : new String[]{"", "_h", "_v", "_none", "_inner"}) {
            var path = file.findResource("assets/mio_icif/textures/block/fusion/viewport" + suffix + ".png");
            if (!Files.exists(path)) { wrong.add("no tile viewport" + suffix); continue; }
            try (var in = Files.newInputStream(path)) {
                var image = javax.imageio.ImageIO.read(in);
                if (image == null) wrong.add("viewport" + suffix + " is no image");
                else if (image.getWidth() != 16 || image.getHeight() != 16) wrong.add("viewport" + suffix + " is " + image.getWidth() + " x " + image.getHeight());
            }
        }
        h.assertTrue(wrong.isEmpty(), "viewport: " + wrong);
        h.succeed();
    }
}
