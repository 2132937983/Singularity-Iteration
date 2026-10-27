package example.sicore;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import javax.imageio.ImageIO;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests of 0.1.10.30 (visuals, plan A): the quantum suit takes a palette (S-3) with a two-layer material and the palette
 * textures; the glow masks and the luminance copies of the attachment textures ship in the jar and keep every light pixel; the
 * glow colour rules. The rendering itself runs on the client and is checked by the offline harness. Internals are reached by
 * reflection.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round1130GameTests {
    private Round1130GameTests() {}

    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    private static Class<?> common(String name) { return Round33GameTests.common(name); }

    private static Item item(String path) { return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", path)); }

    private static final String[] QUANTUM = {"armor/item_armor_quantum_helmet", "armor/item_armor_quantum_chestplate",
        "armor/item_armor_quantum_leggings", "armor/item_armor_quantum_boots"};

    /** The quantum suit takes a palette (the nano suit still does); other armour does not. */
    @GameTest(batch = "round1130_palette", template = "empty", timeoutTicks = 20)
    public static void quantumSuitTakesAPalette(GameTestHelper h) {
        Class<?> palette = common("suit.SuitPalette");
        for (String path : QUANTUM) {
            ItemStack stack = new ItemStack(item(path));
            h.assertTrue((boolean) call(palette, "supports", stack), path + " takes no palette");
            Object p = newPalette(palette, 0x2F5FD0, 0xE8F4FF);
            call(palette, "set", stack, p);
            Object back = call(palette, "of", stack);
            h.assertTrue(back != null && (int) call(back, "accent") == 0xE8F4FF, path + ": palette not kept " + back);
            h.assertTrue((int) call(palette, "tint", stack, 1) == (0xFF000000 | 0xE8F4FF), path + ": accent layer tint");
        }
        h.assertTrue((boolean) call(palette, "supports", new ItemStack(item("armor/item_armor_nano_chestplate"))), "nano chestplate lost its palette");
        h.assertTrue(!(boolean) call(palette, "supports", new ItemStack(net.minecraft.world.item.Items.DIAMOND_CHESTPLATE)), "diamond armour takes a palette");
        ItemStack plain = new ItemStack(item("armor/item_armor_quantum_chestplate"));
        h.assertTrue((int) call(palette, "tint", plain, 1) == 0 && (int) call(palette, "tint", plain, 0) == 0xFFFFFFFF, "without a palette: original texture, no accent layer");
        h.succeed();
    }

    private static Object newPalette(Class<?> type, int primary, int accent) {
        try {
            var c = type.getDeclaredConstructor(int.class, int.class);
            c.setAccessible(true);
            return c.newInstance(primary, accent);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("palette", e);
        }
    }

    /** The quantum material has the accent layer; the worn textures follow the palette (original / plating, accent). */
    @GameTest(batch = "round1130_palette", template = "empty", timeoutTicks = 20)
    public static void quantumMaterialHasAccentLayer(GameTestHelper h) {
        Class<?> materials = common("item.armor.MioIcifArmorMaterials");
        Item chest = item("armor/item_armor_quantum_chestplate");
        ArmorMaterial material = ((ArmorItem) chest).getMaterial().value();
        h.assertTrue(material.layers().size() == 2, "quantum material layers: " + material.layers().size());
        ArmorMaterial.Layer base = material.layers().get(0), accent = material.layers().get(1);
        h.assertTrue(!(boolean) call(materials, "isAccentLayer", base) && (boolean) call(materials, "isAccentLayer", accent), "accent layer not recognised");
        ItemStack stack = new ItemStack(chest);
        ResourceLocation t0 = chest.getArmorTexture(stack, null, EquipmentSlot.CHEST, base, false);
        ResourceLocation t1 = chest.getArmorTexture(stack, null, EquipmentSlot.CHEST, accent, false);
        h.assertTrue(t0 != null && t0.getPath().equals("textures/models/armor/quantum_chestplate.png"), "base texture " + t0);
        h.assertTrue(t1 != null && t1.getPath().equals("textures/models/armor/quantum_chestplate_accent.png"), "accent texture " + t1);
        Class<?> palette = common("suit.SuitPalette");
        call(palette, "set", stack, newPalette(palette, 0xC8323C, 0xFFD24A));
        ResourceLocation p0 = chest.getArmorTexture(stack, null, EquipmentSlot.CHEST, base, false);
        h.assertTrue(p0 != null && p0.getPath().equals("textures/models/armor/quantum_chestplate_plating.png"), "plating texture " + p0);
        for (String piece : new String[]{"quantum_helmet", "quantum_chestplate", "quantum_leggings", "quantum_boots"}) {
            for (String suffix : new String[]{"_plating", "_accent"}) h.assertTrue(image("textures/models/armor/" + piece + suffix + ".png") != null, piece + suffix + " missing");
        }
        h.succeed();
    }

    /** The glow masks and the luminance copies ship; every light pixel of an accent mask is lit in the glow mask. */
    @GameTest(batch = "round1130_glow", template = "empty", timeoutTicks = 20)
    public static void glowMasksShip(GameTestHelper h) {
        for (String piece : new String[]{"quantum_helmet", "quantum_chestplate", "quantum_leggings", "quantum_boots"}) {
            BufferedImage glow = image("textures/models/armor/" + piece + "_glow.png"), accent = image("textures/models/armor/" + piece + "_accent.png");
            h.assertTrue(glow != null && accent != null, piece + ": masks missing");
            int lit = 0;
            for (int y = 0; y < glow.getHeight(); y++) {
                for (int x = 0; x < glow.getWidth(); x++) {
                    boolean g = (glow.getRGB(x, y) >>> 24) > 0;
                    if (g) lit++;
                    h.assertTrue(g || (accent.getRGB(x, y) >>> 24) == 0, piece + ": light pixel " + x + "," + y + " dark in the glow mask");
                }
            }
            h.assertTrue(lit > 60, piece + ": only " + lit + " lit pixels");
        }
        for (String tex : new String[]{"advanced_quantum_chestplate", "heavy_quantum_chestplate", "advanced_solar_helmet", "hybrid_solar_helmet",
                "ultimate_solar_helmet"}) {
            BufferedImage lum = image("textures/models/armor/" + tex + "_lum.png");
            h.assertTrue(lum != null, tex + "_lum missing");
            int rgb = lum.getRGB(lum.getWidth() / 2, lum.getHeight() / 2);
            h.assertTrue(((rgb >> 16) & 255) == (rgb & 255) && ((rgb >> 8) & 255) == (rgb & 255), tex + "_lum is not grey");
        }
        h.succeed();
    }

    /** The glow colour rules: the light colour down to half charge, amber at a quarter, red when empty; an empty piece is dark. */
    @GameTest(batch = "round1130_glow", template = "empty", timeoutTicks = 20)
    public static void glowRules(GameTestHelper h) {
        Class<?> rules = common("client.suit.SuitGlowRules");
        int quantum = 0x3CB4FF;
        h.assertTrue((int) call(rules, "colour", quantum, 0.8F) == quantum, "full charge colour");
        h.assertTrue((int) call(rules, "colour", quantum, 0.25F) == 0xFFA838, "quarter charge amber");
        h.assertTrue((int) call(rules, "colour", quantum, 0F) == 0xFF3B30, "empty red");
        h.assertTrue((float) call(rules, "strength", 0F, 1F, 0, 0F, 3) == 0F, "an empty piece glows");
        float s = (float) call(rules, "strength", 1F, 1F, 0, 0F, 3);
        h.assertTrue(s >= 0.79F && s <= 1.01F, "breathing strength " + s);
        h.succeed();
    }

    private static BufferedImage image(String path) {
        try (InputStream in = Round1130GameTests.class.getClassLoader().getResourceAsStream("assets/mio_icif/" + path)) {
            return in == null ? null : ImageIO.read(in);
        } catch (java.io.IOException e) {
            return null;
        }
    }
}
