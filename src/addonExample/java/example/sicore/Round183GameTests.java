package example.sicore;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** GameTests of the 0.1.8.3 visual fixes (shipped models and textures, weapon data). */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round183GameTests {
    /** Blockbench pieces: model name -> item path. */
    private static final String[][] BAKED = {
        {"nano_helmet", "armor/item_armor_nano_helmet"}, {"nano_chestplate", "armor/item_armor_nano_chestplate"},
        {"nano_leggings", "armor/item_armor_nano_leggings"}, {"nano_boots", "armor/item_armor_nano_boots"},
        {"quantum_helmet", "armor/item_armor_quantum_helmet"}, {"quantum_chestplate", "armor/item_armor_quantum_chestplate"},
        {"quantum_leggings", "armor/item_armor_quantum_leggings"}, {"quantum_boots", "armor/item_armor_quantum_boots"},
        {"advanced_quantum_chestplate", "armor/item_armor_advanced_quantum_chestplate"},
        {"heavy_quantum_chestplate", "armor/item_armor_heavy_quantum_chestplate"},
        {"solar_helmet", "normal/item_solar_helmet"},
        {"advanced_solar_helmet", "armor/item_armor_advanced_solar_helmet"},
        {"hybrid_solar_helmet", "armor/item_armor_hybrid_solar_helmet"},
        {"ultimate_solar_helmet", "armor/item_armor_ultimate_solar_helmet"}};

    /**
     * Checks the item model of a Blockbench armor piece: separate transforms, a 3D base model
     * (builtin/entity) and a flat inventory icon that exists. Returns null when it is right.
     */
    static String splitArmorItemModel(String itemPath) {
        String json = Round33GameTests.resource("/assets/mio_icif/models/item/" + itemPath + ".json");
        if (json == null) return "item model missing";
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        if (!"neoforge:separate_transforms".equals(root.has("loader") ? root.get("loader").getAsString() : "")) return "item model is not separate_transforms";
        String base = root.getAsJsonObject("base").get("parent").getAsString();
        if (!base.equals("mio_icif:item/3d/" + itemPath)) return "base model is " + base;
        String model3d = Round33GameTests.resource("/assets/mio_icif/models/item/3d/" + itemPath + ".json");
        if (model3d == null || !model3d.contains("builtin/entity")) return "3D model is not builtin/entity";
        JsonObject gui = root.getAsJsonObject("perspectives").getAsJsonObject("gui");
        String icon = gui.getAsJsonObject("textures").get("layer0").getAsString();
        if (!icon.equals("mio_icif:item/" + itemPath)) return "inventory icon is " + icon;
        if (!Round180GameTests.modResourceExists("assets/mio_icif/textures/item/" + itemPath + ".png")) return "icon texture missing";
        return null;
    }

    /** Every Blockbench piece: geometry, texture, worn texture path, 3D/2D item model split. */
    @GameTest(template = "empty", batch = "round183_data")
    public static void bakedArmorPiecesAreComplete(GameTestHelper h) {
        List<String> problems = new ArrayList<>();
        for (String[] piece : BAKED) {
            String name = piece[0];
            String json = Round33GameTests.resource("/assets/mio_icif/armor_models/" + name + ".json");
            if (json == null) { problems.add(name + ": model missing"); continue; }
            JsonObject parts = JsonParser.parseString(json).getAsJsonObject().getAsJsonObject("parts");
            String main = name.endsWith("helmet") ? "head" : name.endsWith("chestplate") ? "body" : "right_leg";
            if (!parts.has(main) || parts.getAsJsonArray(main).size() < 6) problems.add(name + ": lacks " + main);
            if (!Round180GameTests.modResourceExists("assets/mio_icif/textures/models/armor/" + name + ".png")) problems.add(name + ": texture missing");
            String split = splitArmorItemModel(piece[1]);
            if (split != null) problems.add(name + ": " + split);
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", piece[1]));
            ItemStack stack = new ItemStack(item);
            if (item instanceof ArmorItem armor) {
                var texture = item.getArmorTexture(stack, null, armor.getEquipmentSlot(), armor.getMaterial().value().layers().get(0), false);
                if (texture == null || !texture.getPath().equals("textures/models/armor/" + name + ".png")) problems.add(name + ": worn texture " + texture);
            } else problems.add(name + ": " + piece[1] + " is not armor");
        }
        h.assertTrue(problems.isEmpty(), "baked armor: " + problems);
        h.succeed();
    }

    /** No arm or hand vertex of a chestplate lies on the skin or sleeve planes (first-person flicker). */
    @GameTest(template = "empty", batch = "round183_data")
    public static void gauntletsClearTheSkin(GameTestHelper h) {
        List<String> problems = new ArrayList<>();
        double[][] right = {{0, -3, -1}, {0, 1, 1}, {1, 10, 1}, {2, -2, -1}, {2, 2, 1}};
        double[][] left = {{0, -1, -1}, {0, 3, 1}, {1, 10, 1}, {2, -2, -1}, {2, 2, 1}};
        for (String chest : new String[]{"nano_chestplate", "quantum_chestplate", "advanced_quantum_chestplate", "heavy_quantum_chestplate"}) {
            String json = Round33GameTests.resource("/assets/mio_icif/armor_models/" + chest + ".json");
            if (json == null) { problems.add(chest + " missing"); continue; }
            JsonObject parts = JsonParser.parseString(json).getAsJsonObject().getAsJsonObject("parts");
            for (String part : new String[]{"right_arm", "left_arm", "right_hand", "left_hand"}) {
                if (!parts.has(part)) continue;
                double[][] planes = part.startsWith("right") ? right : left;
                for (var q : parts.getAsJsonArray(part)) {
                    var v = q.getAsJsonObject().getAsJsonArray("v");
                    for (int i = 0; i < 4; i++) for (double[] p : planes) {
                        double off = (v.get(i * 5 + (int) p[0]).getAsDouble() - p[1]) * p[2];
                        if (off >= -0.02 && off < 0.30) { problems.add(chest + "/" + part + " on plane " + p[1]); break; }
                    }
                }
            }
        }
        h.assertTrue(problems.isEmpty(), "gauntlet clearance: " + problems.stream().distinct().toList());
        h.succeed();
    }

    /**
     * The bronze sword and the nano bow are flat item icons (no 3D elements), and their textures exist.
     * 0.1.10.4: the nanosaber and the quantum sword carry the user's 3D models in the hand and their
     * 2D icons in the inventory (Round1104GameTests checks those).
     */
    @GameTest(template = "empty", batch = "round183_data")
    public static void weaponsAreFlatIcons(GameTestHelper h) {
        List<String> problems = new ArrayList<>();
        String[][] models = {
            {"item_tool_bronze_sword", "minecraft:item/handheld", "weapon/bronze_sword"},
            {"item_tool_nano_bow", "minecraft:item/bow", "item_tool_nano_bow"},
            {"item_tool_nano_bow_pull2", "minecraft:item/bow", "item_tool_nano_bow_pull2"}};
        for (String[] m : models) {
            String json = Round33GameTests.resource("/assets/mio_icif/models/item/" + m[0] + ".json");
            if (json == null) { problems.add(m[0] + " missing"); continue; }
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            if (root.has("elements")) problems.add(m[0] + " still has 3D elements");
            if (!m[1].equals(root.get("parent").getAsString())) problems.add(m[0] + " parent " + root.get("parent"));
            String layer = root.getAsJsonObject("textures").get("layer0").getAsString();
            if (!layer.equals("mio_icif:item/" + m[2])) problems.add(m[0] + " layer0 " + layer);
            if (!Round180GameTests.modResourceExists("assets/mio_icif/textures/item/" + m[2] + ".png")) problems.add(m[2] + " texture missing");
        }
        for (String animated : new String[]{"weapon/quantum_sword", "weapon/quantum_sword_hyper"})
            if (!Round180GameTests.modResourceExists("assets/mio_icif/textures/item/" + animated + ".png.mcmeta")) problems.add(animated + " mcmeta missing");
        String saber = Round33GameTests.resource("/assets/mio_icif/models/item/item_tool_nanosaber.json");
        if (saber == null || !saber.contains("mio_icif:nanosaber_active")) problems.add("nanosaber lost its active override");
        h.assertTrue(problems.isEmpty(), "weapon icons: " + problems);
        h.succeed();
    }

    /** The nano bow draw progress follows the arrow charge: 0 at rest, 1 at full power, never down. */
    @GameTest(template = "empty", batch = "round183_data")
    public static void nanoBowDrawProgress(GameTestHelper h) {
        Class<?> bow = Round33GameTests.common("item.tools.mio_icif_nano_bow");
        float last = -1;
        for (int t = 0; t <= 20; t++) {
            float p = (Float) Round33GameTests.call(bow, "drawProgress", t);
            h.assertTrue(p >= last && p >= 0 && p <= 1.0001F, "draw progress not monotonic in 0..1 at tick " + t + ": " + p);
            last = p;
        }
        h.assertTrue((Float) Round33GameTests.call(bow, "drawProgress", 0) == 0F, "drawn at rest");
        h.assertTrue((Float) Round33GameTests.call(bow, "drawProgress", 4) >= 0.999F, "not full at full power");
        h.succeed();
    }

    /** An energy bolt carries the colour and power of its gun to the client renderer. */
    @GameTest(template = "empty", batch = "round183_data")
    public static void energyBoltCarriesBeamColour(GameTestHelper h) {
        var type = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "energy_bullet"));
        var bolt = type.create(h.getLevel());
        if (bolt == null || !Round33GameTests.common("item.tools.mio_icif_energy_bullet").isInstance(bolt)) {
            h.fail("energy_bullet is " + bolt);
            return;
        }
        Round33GameTests.call(bolt, "setBeam", 0xFF4D4D, 10_000L);
        int colour = (Integer) Round33GameTests.call(bolt, "beamColor");
        float power = (Float) Round33GameTests.call(bolt, "beamPower");
        h.assertTrue(colour == 0xFF4D4D, "colour " + Integer.toHexString(colour));
        h.assertTrue(power > 0.75F && power <= 1.0F, "power " + power);
        bolt.discard();
        h.succeed();
    }
}
