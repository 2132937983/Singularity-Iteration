package example.sicore;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.netty.buffer.Unpooled;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.40 (Wingman audit W1): the squad view carries the configured quantum recall cooldown (the wheel's sweep), and
 * every text the squad HUD and the command wheel draw has a value in the four languages, with the old words gone. The
 * screens themselves (key repeats, layout) are client code and were checked in the capture scene wingman_ui.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11540GameTests {
    private Round11540GameTests() {}

    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    /** The squad view of a commander has the configured cooldown and survives the codec. */
    @GameTest(template = "empty", batch = "round11540_squad", timeoutTicks = 20)
    @SuppressWarnings("unchecked")
    public static void squadCarriesTheRecallCooldown(GameTestHelper h) {
        ServerPlayer owner = Round1102GameTests.commander(h);
        Class<?> commands = Round33GameTests.common("armory.WingmanCommands");
        Object state = owner.getData((net.neoforged.neoforge.attachment.AttachmentType<Object>) staticGet(
            Round33GameTests.common("suit.SuitRegistry"), "WINGMAN_COMMAND"));
        Object squad = call(commands, "squad", owner, state);
        int max = (Integer) call(Round33GameTests.common("suit.quantum.QuantumConfig"), "recallCooldown");
        h.assertTrue((Integer) call(squad, "recallCooldownMax") == max, "cooldown max " + call(squad, "recallCooldownMax"));
        var codec = (StreamCodec<FriendlyByteBuf, Object>) staticGet(squad.getClass(), "CODEC");
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        codec.encode(buf, squad);
        Object back = codec.decode(buf);
        h.assertTrue(back.equals(squad) && buf.readableBytes() == 0, "round trip: " + back + " / " + buf.readableBytes() + " bytes left");
        Round33GameTests.remove(owner);
        h.succeed();
    }

    private static Object staticGet(Class<?> type, String name) {
        try {
            var f = type.getField(name);
            Object v = f.get(null);
            return v instanceof java.util.function.Supplier<?> s && !(v instanceof StreamCodec<?, ?>) ? s.get() : v;
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(name, e);
        }
    }

    private static final List<String> KEYS = List.of(
        "gui.mio_icif.command.where.unloaded", "gui.mio_icif.command.where.other_dim",
        "gui.mio_icif.command.hud.datalink_on", "gui.mio_icif.command.hud.datalink_off", "gui.mio_icif.command.hud.recall_cd",
        "gui.mio_icif.wingman.mode_short.offensive", "gui.mio_icif.wingman.mode_short.dispersal", "gui.mio_icif.wingman.mode_short.escort",
        "gui.mio_icif.wingman.order.none", "gui.mio_icif.wingman.order.move",
        "gui.mio_icif.wingman.order.hold", "gui.mio_icif.wingman.order.suppress", "gui.mio_icif.wingman.order.focus",
        "gui.mio_icif.wingman.order.rtb", "gui.mio_icif.wingman.order_short.move", "gui.mio_icif.wingman.order_short.hold",
        "gui.mio_icif.wingman.order_short.suppress", "gui.mio_icif.wingman.order_short.focus");

    /** Every HUD and wheel text has a value in each language; zh, ja and ru use one word for the Wingman and the Armory. */
    @GameTest(template = "empty", batch = "round11540_lang", timeoutTicks = 20)
    public static void hudTextsAreTranslated(GameTestHelper h) {
        String[][] old = {{"zh_cn", "返回军械库"}, {"ja_jp", "ウィングマン"}, {"ja_jp", "アーモリー"}, {"ru_ru", "оружейн"}};
        for (String lang : new String[]{"en_us", "zh_cn", "ja_jp", "ru_ru"}) {
            JsonObject json = lang(lang);
            for (String key : KEYS) h.assertTrue(json.has(key), lang + " lacks " + key);
            // every value the order enum names (WingmanOrder.key()) exists too
            for (String o : new String[]{"none", "move", "hold", "suppress", "focus"}) h.assertTrue(json.has("gui.mio_icif.wingman.order." + o), lang + " order " + o);
            for (String[] word : old) {
                if (!word[0].equals(lang)) continue;
                for (var e : json.entrySet()) {
                    h.assertTrue(!e.getValue().getAsString().contains(word[1]) || e.getKey().contains("material_interface"),
                        lang + " still says " + word[1] + " in " + e.getKey());
                }
            }
        }
        h.succeed();
    }

    private static JsonObject lang(String lang) {
        String path = "/assets/mio_icif/lang/" + lang + ".json";
        try (InputStream in = Round33GameTests.common("armory.WingmanEntity").getResourceAsStream(path)) {
            if (in == null) throw new AssertionError("no " + path);
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (java.io.IOException e) {
            throw new AssertionError(path, e);
        }
    }
}
