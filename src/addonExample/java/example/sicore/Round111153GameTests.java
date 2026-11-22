package example.sicore;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.153: the silver plate and the dense silver plate, after lead's, and the first recipes of the silver casing: the items and
 * their tags, and every recipe with its result.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111153GameTests {
    private Round111153GameTests() {}

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }
    private static Item item(String path) { return BuiltInRegistries.ITEM.get(id(path)); }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void silverPlatesAndTheirRecipes(GameTestHelper h) {
        List<String> wrong = new ArrayList<>();
        Item plate = item("resource/item_silver_plate"), dense = item("resource/item_silver_denseplate"), casing = item("resource/item_silver_casing");
        if (plate == Items.AIR || dense == Items.AIR || casing == Items.AIR) wrong.add("an item is missing");
        if (!new ItemStack(plate).is(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "plates/silver")))) wrong.add("the plate is not c:plates/silver");
        if (!new ItemStack(dense).is(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "dense_plates/silver")))) wrong.add("the dense plate is not c:dense_plates/silver");
        var recipes = h.getLevel().getRecipeManager();
        var registries = h.getLevel().registryAccess();
        Object[][] expected = {
            {"resource/item_silver_plate", plate, 1}, {"resource/item_silver_casing", casing, 2},
            {"metal_former/rolling/silver_ingot_to_plate", plate, 1}, {"metal_former/rolling/silver_plate_to_casing", casing, 2},
            {"compressor/silver_plate_to_dense", dense, 1}, {"block_cutter/silver_block_to_plate", plate, 9},
            {"powder/silver_plate_to_silver_dust_small", item("resource/item_silver_dust_small"), 8},
            {"powder/silver_denseplate_to_dust", item("resource/item_silver_dust"), 9}};
        for (Object[] e : expected) {
            var r = recipes.byKey(id((String) e[0]));
            if (r.isEmpty()) { wrong.add("no recipe " + e[0]); continue; }
            ItemStack out = r.get().value().getResultItem(registries);
            if (!out.is((Item) e[1]) || out.getCount() != (Integer) e[2]) wrong.add(e[0] + " gives " + out);
        }
        h.assertTrue(wrong.isEmpty(), "silver plates: " + wrong);
        h.succeed();
    }
}
