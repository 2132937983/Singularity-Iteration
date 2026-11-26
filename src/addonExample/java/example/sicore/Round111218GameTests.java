package example.sicore;

import static example.sicore.Round33GameTests.call;
import static example.sicore.Round33GameTests.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.218: the Scanner priced an EESU and a TESU Energy Cell at 0.33 mB (less than an iron ingot, 0.75 mB). Their recipes
 * use SI's electric ingredient (and the Super Lapotron Crystal a charge-carrying recipe), which the recipe reader skipped, and
 * the EESU needs an MFSU and an advanced machine casing, which IC2 observed as not replicable; the chain broke and the Scanner
 * fell back to the formula for a natural block (tool tier x hardness). Now those recipes are read, a not-replicable part takes
 * the cost of its own recipes where another item uses it (it stays not replicable), and a crafted block is never estimated as
 * a natural block. The published generation is unchanged.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111218GameTests {
    private Round111218GameTests() {}

    private static Class<?> core(String name) {
        try {
            return Class.forName("com.miophas.singularity_iteration.core." + name);
        } catch (ClassNotFoundException e) {
            throw new AssertionError(name, e);
        }
    }

    private static Class<?> pricing() { return common("processing.scanner.ScannerPricing"); }
    private static Class<?> book() { return core("runtime.uu.UuQuoteBook"); }

    private static ItemStack si(String path) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", path)).getDefaultInstance();
    }

    /** The Scanner's price of a stack in buckets, asserting it comes from recipes. */
    private static double recipePrice(GameTestHelper h, MinecraftServer server, ItemStack stack) {
        Object price = call(pricing(), "price", server, stack);
        h.assertTrue(price != null, "the Scanner cannot price " + stack);
        h.assertTrue("RECIPE".equals(call(price, "source").toString()), stack + " is " + call(price, "source") + ", not priced from its recipe");
        return ((Number) call(price, "buckets")).doubleValue();
    }

    private static double published(MinecraftServer server, ItemStack stack) {
        Object quote = call(book(), "publishedQuote", server, stack);
        return quote == null ? -1 : ((Number) call(quote, "buckets")).doubleValue();
    }

    @GameTest(template = "empty", batch = "round111218_uu_scanner", timeoutTicks = 200)
    public static void theEesuAndTheTesuCellArePricedFromTheirParts(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        ItemStack eesu = si("wiring/block_eesu"), cell = si("wiring/block_tesu_cell"), slc = si("normal/item_super_lapotron_crystal");
        // the published generation is not changed: these stay Scanner prices
        h.assertTrue(published(server, eesu) < 0 && published(server, cell) < 0, "the EESU or the cell got a published price");
        double lapotron = published(server, si("normal/item_lapotron_crystal_lev0")), iron = published(server, new ItemStack(Items.IRON_INGOT));
        h.assertTrue(lapotron > 0 && iron > 0, "no published price for the lapotron crystal or the iron ingot");
        double superLapotron = recipePrice(h, server, slc), eesuPrice = recipePrice(h, server, eesu), cellPrice = recipePrice(h, server, cell);
        // recipes: crystal = lapotron + 4 iridium plates + 4 elite circuits; EESU = 6 crystals + MFSU + casing + circuit;
        // cell = 4 crystals + 2 EESU + 2 iridium plates + casing
        h.assertTrue(superLapotron > lapotron, "Super Lapotron Crystal " + superLapotron + " B <= its lapotron crystal " + lapotron + " B");
        h.assertTrue(eesuPrice >= 6 * superLapotron, "EESU " + eesuPrice + " B < 6 crystals " + 6 * superLapotron + " B");
        h.assertTrue(cellPrice >= 2 * eesuPrice + 4 * superLapotron, "cell " + cellPrice + " B < 2 EESU + 4 crystals");
        h.assertTrue(eesuPrice > 1000 * iron, "EESU " + eesuPrice + " B is not far above an iron ingot " + iron + " B");
        com.mojang.logging.LogUtils.getLogger().info("[UU Scanner] Super Lapotron Crystal {} B, EESU {} B, TESU Energy Cell {} B (iron ingot {} B)",
            superLapotron, eesuPrice, cellPrice, iron);
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round111218_uu_scanner", timeoutTicks = 200)
    public static void notReplicablePartsStayNotReplicable(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        for (String path : List.of("wiring/block_mfsu", "producer/block_machine_hull_advanced")) {
            ItemStack part = si(path);
            Object assessment = call(book(), "classify", server, part);
            h.assertTrue("KNOWN_DENIED".equals(call(assessment, "disposition").toString()), path + " is no longer denied: " + assessment);
            h.assertTrue(call(pricing(), "price", server, part) == null, "the Scanner prices the not-replicable " + path);
        }
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round111218_uu_scanner", timeoutTicks = 200)
    public static void craftedBlocksAreNeverEstimatedAsNaturalBlocks(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        Object policy = call(common("processing.scanner.ScannerPolicy"), "load", server.getResourceManager());
        @SuppressWarnings("unchecked")
        Map<Object, Double> published = (Map<Object, Double>) call(book(), "publishedPrices", server);
        Set<?> denied = (Set<?>) call(book(), "publishedDenied", server);
        Class<?> index = core("runtime.uu.IndependentUuValueIndex"), estimates = common("processing.scanner.ScannerEstimates");
        List<String> wrong = new ArrayList<>();
        int estimated = 0;
        for (RecipeHolder<?> recipe : h.getLevel().getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
            ItemStack out = recipe.value().getResultItem(h.getLevel().registryAccess());
            if (out.isEmpty() || !(out.getItem() instanceof BlockItem)) continue;
            Item item = out.getItem();
            if (!BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("mio_icif")) continue;
            ItemStack stack = item.getDefaultInstance();
            if (published.containsKey(call(index, "keyOf", stack)) || denied.contains(call(index, "keyOf", stack))) continue;
            Object price = call(pricing(), "price", server, stack);
            if (price == null || !"ESTIMATE".equals(call(price, "source").toString())) continue;
            estimated++;
            double buckets = ((Number) call(price, "buckets")).doubleValue();
            double expected = ((Number) call(estimates, "estimate", item, policy, published, true)).doubleValue();
            if (Math.abs(buckets - expected) > expected * 1e-9) wrong.add(BuiltInRegistries.ITEM.getKey(item) + " " + buckets + " B, crafted estimate " + expected + " B");
        }
        h.assertTrue(wrong.isEmpty(), "crafted blocks estimated as natural blocks: " + wrong);
        // before 0.1.11.218, 91 crafted SI blocks were estimated (79 below 1 mB); most are priced from recipes now
        h.assertTrue(estimated < 40, estimated + " crafted SI blocks are still estimated");
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round111218_uu_scanner", timeoutTicks = 200)
    public static void savedPricesFromBeforeTheFixArePricedAgain(GameTestHelper h) throws ReflectiveOperationException {
        MinecraftServer server = h.getLevel().getServer();
        Class<?> saved = common("processing.scanner.ScannedPrices");
        // a save without a version is from before the fix
        Object old = call(saved, "load", new CompoundTag(), h.getLevel().registryAccess());
        h.assertTrue(((Number) call(old, "version")).intValue() == 1, "an old save does not read as version 1");
        // the EESU as the old pricing saved it: 0.33 mB, estimated as a natural block
        Object data = saved.getDeclaredConstructor().newInstance();
        Class<?> source = common("processing.scanner.ScannerPricing$Source");
        Object estimate = source.getMethod("valueOf", String.class).invoke(null, "ESTIMATE");
        Object oldPrice = common("processing.scanner.ScannerPricing$Price").getDeclaredConstructor(double.class, source).newInstance(1.0 / 3000, estimate);
        ItemStack eesu = si("wiring/block_eesu");
        call(data, "put", eesu, oldPrice);
        int changed = ((Number) call(data, "reprice", server)).intValue();
        Object entry = call(data, "get", eesu);
        double now = recipePrice(h, server, eesu);
        h.assertTrue(changed == 1 && "RECIPE".equals(call(entry, "source").toString())
            && Math.abs(((Number) call(entry, "buckets")).doubleValue() - now) <= now * 1e-9, "the saved EESU price was not priced again: " + entry);
        CompoundTag written = (CompoundTag) call(data, "save", new CompoundTag(), h.getLevel().registryAccess());
        h.assertTrue(written.getInt("version") == ((Number) saved.getField("VERSION").get(null)).intValue(), "the repriced save has no current version");
        h.succeed();
    }
}
