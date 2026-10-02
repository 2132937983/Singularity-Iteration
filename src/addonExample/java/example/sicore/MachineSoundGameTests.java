package example.sicore;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.miophas.singularity_iteration.core.prefab.blockentity.MachinePresentation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Machine sound set: every registered mio_icif sound event is defined in sounds.json with a
 * subtitle, every referenced file exists and is a mono Ogg Vorbis stream (positional audio), and
 * the host no longer broadcasts a periodic server-side work sound (loops are client-side).
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class MachineSoundGameTests {

    @GameTest(template = "reactor_loop", batch = "machine_sounds", timeoutTicks = 20)
    public static void soundAssetsAreCompleteAndMono(GameTestHelper h) {
        JsonObject sounds;
        try (InputStream in = MachineSoundGameTests.class.getClassLoader().getResourceAsStream("assets/mio_icif/sounds.json")) {
            h.assertTrue(in != null, "assets/mio_icif/sounds.json must be on the classpath");
            sounds = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        List<String> problems = new ArrayList<>();
        BuiltInRegistries.SOUND_EVENT.keySet().stream().filter(k -> k.getNamespace().equals("mio_icif")).forEach(key -> {
            JsonObject def = sounds.getAsJsonObject(key.getPath());
            if (def == null) { problems.add("no sounds.json entry for " + key); return; }
            if (!def.has("subtitle")) problems.add("no subtitle for " + key);
            for (JsonElement s : def.getAsJsonArray("sounds")) {
                String name = s.isJsonObject() ? s.getAsJsonObject().get("name").getAsString() : s.getAsString();
                String file = "assets/mio_icif/sounds/" + name.substring(name.indexOf(':') + 1) + ".ogg";
                int channels = vorbisChannels(file);
                if (channels < 0) problems.add("missing or unreadable " + file);
                else if (channels != 1) problems.add(file + " has " + channels + " channels (positional audio needs mono)");
            }
        });
        h.assertTrue(problems.isEmpty(), String.join("; ", problems));
        h.assertTrue(BuiltInRegistries.SOUND_EVENT.keySet().stream().filter(k -> k.getNamespace().equals("mio_icif")
            && k.getPath().startsWith("machine.loop.")).count() >= 12, "expected the 12 machine loop sounds");
        h.assertTrue(MachinePresentation.workSound() == null,
            "host machines must not broadcast a periodic server-side work sound (loops are client-side)");
        h.succeed();
    }

    /** Channel count from the Vorbis identification header, or -1. */
    private static int vorbisChannels(String resource) {
        try (InputStream in = MachineSoundGameTests.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) return -1;
            byte[] head = in.readNBytes(128);
            for (int i = 0; i + 11 < head.length; i++) {
                if (head[i] == 1 && head[i + 1] == 'v' && head[i + 2] == 'o' && head[i + 3] == 'r'
                        && head[i + 4] == 'b' && head[i + 5] == 'i' && head[i + 6] == 's') {
                    return head[i + 11] & 0xFF;
                }
            }
            return -1;
        } catch (Exception e) {
            return -1;
        }
    }
}
