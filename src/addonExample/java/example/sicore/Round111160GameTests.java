package example.sicore;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.160: the creative tab, rebuilt in order. Its contents are written to {@code creative_tab_dump.txt} (one stack a line: item
 * id and its data components), so a reordering can be checked against the old contents, stack for stack; the mod's items left
 * out of the tab are listed in {@code creative_tab_hidden.txt}.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111160GameTests {
    private Round111160GameTests() {}

    static List<ItemStack> tab(GameTestHelper h) {
        CreativeModeTabs.tryRebuildTabContents(h.getLevel().enabledFeatures(), true, h.getLevel().registryAccess());
        CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "icif_tab"));
        if (tab == null) throw new net.minecraft.gametest.framework.GameTestAssertException("no mio_icif:icif_tab");
        return new ArrayList<>(tab.getDisplayItems());
    }

    static String line(ItemStack stack) {
        List<String> parts = new ArrayList<>();
        for (var e : stack.getComponentsPatch().entrySet()) parts.add(e.getKey() + "=" + e.getValue().map(Object::toString).orElse("<removed>"));
        parts.sort(null);
        return BuiltInRegistries.ITEM.getKey(stack.getItem()) + " " + stack.getCount() + " " + parts;
    }

    @GameTest(template = "empty", batch = "round111160_tab")
    public static void creativeTabDump(GameTestHelper h) {
        List<ItemStack> stacks = tab(h);
        StringBuilder dump = new StringBuilder();
        Set<Item> shown = new HashSet<>();
        for (ItemStack s : stacks) {
            dump.append(line(s)).append('\n');
            shown.add(s.getItem());
        }
        StringBuilder hidden = new StringBuilder();
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (id.getNamespace().equals("mio_icif") && !shown.contains(item)) hidden.append(id).append('\n');
        }
        try {
            Files.writeString(Path.of("creative_tab_dump.txt"), dump, StandardCharsets.UTF_8);
            Files.writeString(Path.of("creative_tab_hidden.txt"), hidden, StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            throw new net.minecraft.gametest.framework.GameTestAssertException("could not write the dump: " + e);
        }
        h.assertTrue(stacks.size() > 500, "the tab holds only " + stacks.size() + " stacks");
        h.succeed();
    }

    private static String path(ItemStack stack) { return BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath(); }

    /** The suit modification station and the nano / quantum plugins (all "module/..." items) close the tab, Mk.II after Mk.I. */
    @GameTest(template = "empty", batch = "round111160_tab")
    public static void suitPluginsCloseTheTab(GameTestHelper h) {
        List<ItemStack> stacks = tab(h);
        int station = -1;
        for (int i = 0; i < stacks.size(); i++) if (path(stacks.get(i)).equals("producer/block_quantum_modification_station")) station = i;
        h.assertTrue(station > 0, "the suit modification station is not in the tab");
        for (int i = 0; i < stacks.size(); i++) {
            boolean plugin = path(stacks.get(i)).startsWith("module/");
            if (i < station) h.assertTrue(!plugin, path(stacks.get(i)) + " stands before the suit station (index " + i + ")");
            if (i > station) h.assertTrue(plugin, path(stacks.get(i)) + " stands among the suit plugins (index " + i + ")");
        }
        h.assertTrue(stacks.size() - station > 40, "only " + (stacks.size() - station - 1) + " plugins after the station");
        for (int i = station + 1; i < stacks.size(); i++) {
            String p = path(stacks.get(i));
            if (p.endsWith("_mk2")) h.assertTrue(path(stacks.get(i - 1)).equals(p.substring(0, p.length() - 4)),
                p + " does not follow its Mk.I (it follows " + path(stacks.get(i - 1)) + ")");
        }
        h.succeed();
    }
}
