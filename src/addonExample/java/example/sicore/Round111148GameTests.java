package example.sicore;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.148: the fusion reactor's block models and textures are back to those of before the component redraw (as of 0.1.11.139):
 * no fusion model uses the connected loader (mio_icif:connected, 0.1.11.140), the redraw's textures under
 * textures/block/fusion/component are gone, and every texture and parent a fusion model names exists. Replaces the 0.1.11.140
 * connected-model test.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111148GameTests {
    private Round111148GameTests() {}

    private static Path resource(String path) {
        return net.neoforged.fml.ModList.get().getModFileById("mio_icif").getFile().findResource(path);
    }

    private static boolean exists(String ref, String folder, String suffix) {
        String[] parts = ref.contains(":") ? ref.split(":", 2) : new String[]{"minecraft", ref};
        return !parts[0].equals("mio_icif") || Files.exists(resource("assets/mio_icif/" + folder + "/" + parts[1] + suffix));
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void fusionModelsAreBackToTheOriginalOnes(GameTestHelper h) throws java.io.IOException {
        List<String> wrong = new ArrayList<>();
        int models = 0;
        for (String folder : new String[]{"assets/mio_icif/models/block/fusion", "assets/mio_icif/models/item/fusion"}) {
            Path dir = resource(folder);
            if (!Files.isDirectory(dir)) { wrong.add("no " + folder); continue; }
            List<Path> files;
            try (Stream<Path> list = Files.list(dir)) { files = list.filter(p -> p.toString().endsWith(".json")).toList(); }
            for (Path file : files) {
                models++;
                String name = file.getFileName().toString();
                JsonObject model = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
                // 0.1.11.149: the viewport connects again, on its own texture (Round111149GameTests)
                if (model.has("loader") && !name.equals("block_fusion_viewport.json")) wrong.add(name + " uses the loader " + model.get("loader").getAsString());
                if (model.has("parent") && !exists(model.get("parent").getAsString(), "models", ".json"))
                    wrong.add(name + ": no parent " + model.get("parent").getAsString());
                if (model.has("textures"))
                    for (var entry : model.getAsJsonObject("textures").entrySet()) {
                        String ref = entry.getValue().getAsString();
                        if (!ref.startsWith("#") && !exists(ref, "textures", ".png")) wrong.add(name + ": no texture " + ref);
                        if (ref.contains("fusion/component/")) wrong.add(name + " still uses the redraw's texture " + ref);
                    }
            }
        }
        if (models < 20) wrong.add("only " + models + " fusion models");
        if (Files.exists(resource("assets/mio_icif/textures/block/fusion/component"))) wrong.add("the redraw's textures/block/fusion/component is still there");
        // the original look: the blanket a plain cube of its own texture, the coils columns with their own ends
        String blanket = Round33GameTests.resource("/assets/mio_icif/models/block/fusion/block_fusion_blanket.json");
        if (blanket == null || !blanket.contains("mio_icif:block/fusion/blanket")) wrong.add("the blanket is not its original cube");
        String solenoid = Round33GameTests.resource("/assets/mio_icif/models/block/fusion/block_fusion_central_solenoid.json");
        if (solenoid == null || !solenoid.contains("central_solenoid_end")) wrong.add("the central solenoid is not its original column");
        h.assertTrue(wrong.isEmpty(), "fusion models: " + wrong);
        h.succeed();
    }
}
