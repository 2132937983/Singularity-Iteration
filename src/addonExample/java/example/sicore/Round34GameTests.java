package example.sicore;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.netty.buffer.Unpooled;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.7.34: Special Maneuver Mode (rename, throttle model constants, state relay), the nano suit
 * Blockbench models and icons, the new sounds, language keys and manual pages.
 * Common classes are reached by reflection (core must not depend on common).
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round34GameTests {
    private Round34GameTests() {}

    static Class<?> common(String name) { return Round33GameTests.common(name); }
    static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    static Object constant(String cls, String field) {
        try {
            Field f = common(cls).getField(field);
            return f.get(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(cls + "." + field, e);
        }
    }

    // ------------------------------------------------------------------ Special Maneuver Mode

    /** Throttle model: shock barrier at 48 m/s, grip falls with throttle, power curve as documented. */
    @GameTest(batch = "round34_maneuver", template = "empty", timeoutTicks = 20)
    public static void maneuverFlightModel(GameTestHelper h) {
        h.assertTrue("special_maneuver".equals(constant("suit.ManeuverMode", "FEATURE")), "feature key not renamed");
        double max = (double) constant("suit.ManeuverMode", "MAX_SPEED");
        float shock = (float) constant("suit.ManeuverMode", "SHOCK_THROTTLE");
        float up = (float) constant("suit.ManeuverMode", "THROTTLE_UP"), down = (float) constant("suit.ManeuverMode", "THROTTLE_DOWN");
        h.assertTrue(Math.abs(max * 20 - 80) < 1e-6, "top speed " + max * 20 + " m/s");
        h.assertTrue(Math.abs(max * shock * 20 - 48) < 1e-4, "shock barrier at " + max * shock * 20 + " m/s");
        h.assertTrue(Math.abs(down - up * 2) < 1e-6, "throttle must close twice as fast as it opens");
        double gripIdle = (double) call(common("suit.ManeuverMode"), "grip", 0F);
        double gripFull = (double) call(common("suit.ManeuverMode"), "grip", 1F);
        double gripMid = (double) call(common("suit.ManeuverMode"), "grip", 0.5F);
        h.assertTrue(gripIdle > gripMid && gripMid > gripFull && gripFull > 0, "grip must fall with throttle: " + gripIdle + " " + gripMid + " " + gripFull);
        double full = (double) call(common("suit.ManeuverMode"), "costPerTick", max);
        h.assertTrue(Math.abs(full - 486) < 1, "full throttle cost " + full + " (manual says 486 EU/t)");
        double cruise = (double) call(common("suit.ManeuverMode"), "costPerTick", 1.1);
        h.assertTrue(Math.abs(cruise - 42.3) < 1, "slow cruise cost " + cruise + " (manual says 42 EU/t)");
        h.succeed();
    }

    /** The relayed state survives the wire; the server clamps the throttle and drops it when the switch is off. */
    @SuppressWarnings("unchecked")
    @GameTest(batch = "round34_maneuver", template = "empty", timeoutTicks = 20)
    public static void maneuverStateRelay(GameTestHelper h) throws Exception {
        Class<?> state = common("suit.SuitPackets$ManeuverState");
        Object msg = state.getConstructors()[0].newInstance(42, 0.75F, 1F, -1F, true);
        StreamCodec<io.netty.buffer.ByteBuf, Object> codec = (StreamCodec<io.netty.buffer.ByteBuf, Object>) state.getField("CODEC").get(null);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        codec.encode(buf, msg);
        Object back = codec.decode(buf);
        h.assertTrue(msg.equals(back), "ManeuverState round trip: " + msg + " -> " + back);
        Class<?> input = common("suit.SuitPackets$ManeuverInput");
        Object in = input.getConstructors()[0].newInstance(0.5F, 0F, 1F, false);
        StreamCodec<io.netty.buffer.ByteBuf, Object> inCodec = (StreamCodec<io.netty.buffer.ByteBuf, Object>) input.getField("CODEC").get(null);
        FriendlyByteBuf buf2 = new FriendlyByteBuf(Unpooled.buffer());
        inCodec.encode(buf2, in);
        h.assertTrue(in.equals(inCodec.decode(buf2)), "ManeuverInput round trip");
        // relay from a player without the chestplate: no exception, nothing to send (no viewers)
        ServerPlayer player = Round33GameTests.survivalPlayer(h);
        call(common("suit.ManeuverMode"), "relayState", player, 5F, 3F, -3F, true);
        h.assertTrue(!(boolean) call(common("suit.ManeuverMode"), "isArmed", player), "unarmed player reported armed");
        player.setItemSlot(EquipmentSlot.CHEST, Round33GameTests.charged("armor/item_armor_quantum_chestplate"));
        com.miophas.singularity_iteration.core.prefab.item.ArmorFeatures.setEnabled(player.getItemBySlot(EquipmentSlot.CHEST), "special_maneuver", true);
        h.assertTrue((boolean) call(common("suit.ManeuverMode"), "isArmed", player), "switch on: not armed");
        Round33GameTests.remove(player);
        h.succeed();
    }

    // ------------------------------------------------------------------ nano suit

    private static final String[] NANO = {"helmet", "chestplate", "leggings", "boots"};

    /** Each nano piece ships its Blockbench geometry and texture, and the item points the armor layer at it. */
    @GameTest(batch = "round34_nano", template = "empty", timeoutTicks = 20)
    public static void nanoSuitModelsAndTextures(GameTestHelper h) {
        List<String> problems = new ArrayList<>();
        String[][] expected = {{"head"}, {"body", "right_arm", "left_arm"}, {"body", "right_leg", "left_leg"}, {"right_leg", "left_leg"}};
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (int i = 0; i < NANO.length; i++) {
            String piece = NANO[i];
            String json = Round33GameTests.resource("/assets/mio_icif/armor_models/nano_" + piece + ".json");
            if (json == null) { problems.add("model missing: " + piece); continue; }
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonObject parts = root.getAsJsonObject("parts");
            Set<String> keys = parts.keySet();
            for (String part : expected[i]) {
                if (!keys.contains(part)) { problems.add(piece + " lacks part " + part); continue; }
                int quads = 0;
                for (JsonElement q : parts.getAsJsonArray(part)) {
                    var o = q.getAsJsonObject();
                    if (o.getAsJsonArray("v").size() != 20 || o.getAsJsonArray("n").size() != 3) problems.add(piece + "/" + part + " bad quad");
                    for (JsonElement v : o.getAsJsonArray("v")) if (!Float.isFinite(v.getAsFloat())) problems.add(piece + "/" + part + " NaN");
                    quads++;
                }
                if (quads < 6) problems.add(piece + "/" + part + " has only " + quads + " quads");
            }
            for (String k : keys) if (!Set.of(expected[i]).contains(k)) problems.add(piece + " has unexpected part " + k);
            if (bytes("/assets/mio_icif/textures/models/armor/nano_" + piece + ".png") == null)
                problems.add("texture missing: " + piece);
            ItemStack stack = Round33GameTests.item("armor/item_armor_nano_" + piece);
            if (stack.getItem() instanceof ArmorItem armor) {
                var layer = armor.getMaterial().value().layers().get(0);
                ResourceLocation tex = stack.getItem().getArmorTexture(stack, null, slots[i], layer, false);
                if (tex == null || !tex.getPath().equals("textures/models/armor/nano_" + piece + ".png")) problems.add(piece + " armor texture " + tex);
            } else {
                problems.add(piece + " is not an armor item");
            }
            // the redrawn 16x16 inventory icon
            byte[] png = bytes("/assets/mio_icif/textures/item/armor/item_armor_nano_" + piece + ".png");
            if (png == null || png.length < 24) problems.add("icon missing: " + piece);
            else {
                int w = ((png[16] & 255) << 24) | ((png[17] & 255) << 16) | ((png[18] & 255) << 8) | (png[19] & 255);
                int hgt = ((png[20] & 255) << 24) | ((png[21] & 255) << 16) | ((png[22] & 255) << 8) | (png[23] & 255);
                if (w != 16 || hgt != 16) problems.add("icon " + piece + " is " + w + "x" + hgt);
            }
        }
        h.assertTrue(problems.isEmpty(), "nano suit: " + problems);
        h.succeed();
    }

    static byte[] bytes(String path) {
        try {
            var file = net.neoforged.fml.ModList.get().getModFileById("mio_icif").getFile();
            var p = file.findResource(path.substring(1));
            return java.nio.file.Files.exists(p) ? java.nio.file.Files.readAllBytes(p) : null;
        } catch (java.io.IOException | RuntimeException e) {
            return null;
        }
    }

    // ------------------------------------------------------------------ sounds, language, manual

    /** The six new sound events are registered, defined in sounds.json and have their OGG files. */
    @GameTest(batch = "round34_data", template = "empty", timeoutTicks = 20)
    public static void newSoundsExist(GameTestHelper h) {
        List<String> missing = new ArrayList<>();
        String sounds = Round33GameTests.resource("/assets/mio_icif/sounds.json");
        h.assertTrue(sounds != null, "sounds.json missing");
        String[][] events = {{"maneuver.boom", "maneuver_boom"}, {"maneuver.wind", "maneuver_wind"}, {"maneuver.takeoff", "maneuver_takeoff"},
            {"fcs.echo", "fcs_echo"}, {"fcs.lock", "fcs_lock"}, {"fcs.blast_beep", "fcs_blast_beep"}};
        for (String[] e : events) {
            if (!BuiltInRegistries.SOUND_EVENT.containsKey(ResourceLocation.fromNamespaceAndPath("mio_icif", e[0]))) missing.add("registry:" + e[0]);
            if (!sounds.contains("\"" + e[0] + "\"")) missing.add("sounds.json:" + e[0]);
            byte[] ogg = bytes("/assets/mio_icif/sounds/" + e[1] + ".ogg");
            if (ogg == null || ogg.length < 1000 || ogg[0] != 'O' || ogg[1] != 'g' || ogg[2] != 'g') missing.add("ogg:" + e[1]);
        }
        h.assertTrue(missing.isEmpty(), "sounds: " + missing);
        h.succeed();
    }

    /** New keys exist in all three languages; the old Viltrum keys are gone; the manual page moved. */
    @GameTest(batch = "round34_data", template = "empty", timeoutTicks = 20)
    public static void languageAndManual(GameTestHelper h) {
        List<String> problems = new ArrayList<>();
        String[] keys = {"tooltip.mio_icif.armor.feature_special_maneuver", "mio_icif.configuration.show_compass", "screen.mio_icif.hud_layout.compass",
            "subtitles.mio_icif.maneuver.boom", "subtitles.mio_icif.maneuver.wind", "subtitles.mio_icif.maneuver.takeoff",
            "subtitles.mio_icif.fcs.echo", "subtitles.mio_icif.fcs.lock", "subtitles.mio_icif.fcs.blast_beep"};
        for (String lang : new String[]{"en_us", "zh_cn", "ja_jp"}) {
            String json = Round33GameTests.resource("/assets/mio_icif/lang/" + lang + ".json");
            if (json == null) { problems.add(lang + " missing"); continue; }
            for (String k : keys) if (!json.contains("\"" + k + "\"")) problems.add(lang + ":" + k);
            if (json.toLowerCase(java.util.Locale.ROOT).contains("viltrum") || json.contains("ヴィルトラム") || json.contains("维尔特鲁姆"))
                problems.add(lang + " still names Viltrum");
        }
        String base = "/assets/mio_icif/guides/mio_icif/manual/";
        for (String lang : new String[]{"", "_zh_cn/", "_ja_jp/"}) {
            String page = Round33GameTests.resource(base + lang + "suit/maneuver_mode.md");
            if (page == null) problems.add("manual page missing: " + lang + "suit/maneuver_mode.md");
            if (Round33GameTests.resource(base + lang + "suit/viltrum_flight.md") != null) problems.add("old page left: " + lang);
        }
        h.assertTrue(problems.isEmpty(), "lang/manual: " + problems);
        h.succeed();
    }
}
