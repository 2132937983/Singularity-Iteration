package example.sicore;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.miophas.singularity_iteration.core.runtime.uu.UuQuoteBook;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.161: electric items in the creative tab come empty and full, each of them. Many tools hold their full capacity as the
 * default charge (a stack nobody charged is full), so "the item as registered" was the full one and the tab had no empty one;
 * now both are written out. The crafting recipes of those items carry the charge of their ingredients over (the result gets
 * exactly that charge, an explicit 0 included), checked here, so crafted tools are not full for free; none of them has a UU
 * price either (the replicator would make them full). The report {@code electric_defaults.txt} lists those items and every
 * recipe that makes them.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111161GameTests {
    private Round111161GameTests() {}

    private static DataComponentType<?> batteryEnergy() {
        return BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "battery_energy"));
    }

    /** {energy, max} of the stack's battery component (its default one included), or {@code null} without one. */
    static long[] charge(ItemStack stack) {
        Object data = stack.get(batteryEnergy());
        if (data == null) return null;
        return new long[]{(Long) Round33GameTests.call(data, "energy"), (Long) Round33GameTests.call(data, "maxEnergy")};
    }

    private static String id(Item item) { return BuiltInRegistries.ITEM.getKey(item).toString(); }

    /** Every electric item of the tab is there empty (0 EU) and full. */
    @GameTest(template = "empty", batch = "round111161_electric")
    public static void electricItemsComeEmptyAndFull(GameTestHelper h) {
        Map<Item, long[]> seen = new LinkedHashMap<>();   // {max, has empty, has full}
        for (ItemStack s : Round111160GameTests.tab(h)) {
            long[] c = charge(s);
            long[] state = seen.computeIfAbsent(s.getItem(), i -> new long[3]);
            if (c == null) { state[1] = 1; continue; }   // a stackable battery at 0 carries no component
            state[0] = Math.max(state[0], c[1]);
            if (c[0] <= 0) state[1] = 1;
            if (c[1] > 0 && c[0] >= c[1]) state[2] = 1;
        }
        List<String> missing = new ArrayList<>();
        seen.forEach((item, s) -> {
            if (s[0] <= 0) return;   // not electric
            if (s[1] == 0) missing.add(id(item) + " (no empty one)");
            if (s[2] == 0) missing.add(id(item) + " (no full one)");
        });
        h.assertTrue(missing.isEmpty(), "electric items without both forms in the tab: " + missing);
        h.succeed();
    }

    /**
     * The obscurator keeps its energy in the battery component: a new one is empty, a charged one runs down as it works, and an
     * empty one refuses. Before 0.1.11.161 it kept it in the damage value of an item without durability: always full.
     */
    @GameTest(template = "empty", batch = "round111161_electric")
    public static void obscuratorRunsDown(GameTestHelper h) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "build/item_obscurator"));
        ItemStack stack = new ItemStack(item);
        h.assertTrue((Long) Round33GameTests.call(item, "getEnergy", stack) == 0, "a new obscurator holds " + Round33GameTests.call(item, "getEnergy", stack));
        Round33GameTests.call(item, "setEnergy", stack, 100_000L);
        long full = (Long) Round33GameTests.call(item, "getEnergy", stack);
        h.assertTrue(full == 100_000L, "a charged obscurator holds " + full);
        h.assertTrue((Boolean) Round33GameTests.call(item, "consumeEnergy", stack, 1000), "a full obscurator refused to work");
        long after = (Long) Round33GameTests.call(item, "getEnergy", stack);
        h.assertTrue(after < full, "the obscurator did not run down: " + after);
        Round33GameTests.call(item, "setEnergy", stack, 0L);
        h.assertTrue(!(Boolean) Round33GameTests.call(item, "hasEnoughEnergy", stack, 1000), "an empty obscurator still works");
        h.succeed();
    }

    /**
     * The items full by default, and the recipes that make them: every crafting recipe among them must carry the charge over
     * (else a crafted tool would be full for nothing). Written to {@code electric_defaults.txt}.
     */
    @GameTest(template = "empty", batch = "round111161_electric")
    public static void fullByDefaultToolsAreCraftedWithTheirCharge(GameTestHelper h) {
        Set<Item> full = new LinkedHashSet<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (!BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("mio_icif")) continue;
            long[] c = charge(new ItemStack(item));
            if (c != null && c[1] > 0 && c[0] >= c[1]) full.add(item);
        }
        StringBuilder report = new StringBuilder("# items full by default: " + full.size() + "\n");
        Map<Item, List<String>> recipes = new LinkedHashMap<>();
        for (Item item : full) recipes.put(item, new ArrayList<>());
        List<String> plain = new ArrayList<>();
        var access = h.getLevel().registryAccess();
        for (RecipeHolder<?> r : h.getLevel().getRecipeManager().getRecipes()) {
            ItemStack out;
            try { out = r.value().getResultItem(access); } catch (RuntimeException e) { continue; }
            if (out == null || out.isEmpty() || !full.contains(out.getItem())) continue;
            String type = String.valueOf(BuiltInRegistries.RECIPE_TYPE.getKey(r.value().getType()));
            String serializer = String.valueOf(BuiltInRegistries.RECIPE_SERIALIZER.getKey(r.value().getSerializer()));
            Boolean transfer = null;
            try { transfer = (Boolean) r.value().getClass().getMethod("shouldTransferCharge").invoke(r.value()); } catch (ReflectiveOperationException ignored) { }
            String line = r.id() + "  type=" + type + "  serializer=" + serializer + "  transfer=" + transfer;
            recipes.get(out.getItem()).add(line);
            boolean crafting = type.equals("minecraft:crafting");
            if (crafting && !Boolean.TRUE.equals(transfer)) plain.add(id(out.getItem()) + " <- " + r.id() + " (" + serializer + ")");
        }
        recipes.forEach((item, lines) -> {
            report.append(id(item)).append('\n');
            if (lines.isEmpty()) report.append("    (no recipe)\n");
            for (String l : lines) report.append("    ").append(l).append('\n');
        });
        report.append("# crafting recipes that do not carry the charge: ").append(plain.size()).append('\n');
        for (String p : plain) report.append(p).append('\n');
        // the replicator makes an item as registered (full, for these): none of them may have a UU price
        var server = h.getLevel().getServer();
        if (UuQuoteBook.quote(server, new ItemStack(Items.IRON_INGOT)) == null)
            Round33GameTests.call(Round33GameTests.common("processing.UuPricingLifecycle"), "rebuild", server);
        h.assertTrue(UuQuoteBook.quote(server, new ItemStack(Items.IRON_INGOT)) != null, "no UU prices loaded to check against");
        List<String> priced = new ArrayList<>();
        for (Item item : full) if (UuQuoteBook.quote(server, new ItemStack(item)) != null) priced.add(id(item));
        report.append("# with a UU price (replicable): ").append(priced.size()).append('\n');
        for (String p : priced) report.append(p).append('\n');
        try {
            Files.writeString(Path.of("electric_defaults.txt"), report, StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            throw new net.minecraft.gametest.framework.GameTestAssertException("could not write the report: " + e);
        }
        h.assertTrue(plain.isEmpty(), "crafting recipes make full-by-default items without carrying the charge: " + plain);
        h.assertTrue(priced.isEmpty(), "full-by-default items the replicator could make full: " + priced);
        h.succeed();
    }
}
