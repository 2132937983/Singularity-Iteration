package example.sicore;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.162: the creative tab grouped by kind. Every tool (hand and electric tools, scanners, remotes, the TESU connector, the
 * building tools, the lathing tools, boxes) and the weapons after them form one run, and every building block (the build/
 * blocks: scaffolds, storage boxes, tanks, glass, doors, construction foam, rubber wood) forms one run. In 0.1.11.160 the
 * connector stood by the TESU, the remote by the teleporter, the storage boxes among the pipes.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111162GameTests {
    private Round111162GameTests() {}

    private static final Set<String> TOOLS = Set.of("item_geomagnetic_detector", "item_rocket", "normal/item_crop_analyzer",
        "normal/item_weeding_trowel", "normal/item_armory_remote", "normal/item_freq", "normal/item_tesu_connector",
        "normal/item_portable_futures_trader", "normal/item_contain_ment_box");

    static boolean tool(String path) {
        return path.contains("tool") || path.startsWith("build/item_") || path.startsWith("item_electric_") || TOOLS.contains(path);
    }

    static boolean buildBlock(String path) { return path.startsWith("build/") && !path.startsWith("build/item_"); }

    /** The distinct item ids of the tab, in order. */
    private static List<String> order(GameTestHelper h) {
        Set<String> seen = new LinkedHashSet<>();
        for (ItemStack s : Round111160GameTests.tab(h)) {
            var id = BuiltInRegistries.ITEM.getKey(s.getItem());
            if (id.getNamespace().equals("mio_icif")) seen.add(id.getPath());
        }
        return new ArrayList<>(seen);
    }

    /** Fails unless the ids {@code kind} takes stand next to each other. */
    private static void oneRun(GameTestHelper h, List<String> ids, Predicate<String> kind, String what) {
        int first = -1, last = -1, count = 0;
        for (int i = 0; i < ids.size(); i++) if (kind.test(ids.get(i))) { if (first < 0) first = i; last = i; count++; }
        h.assertTrue(count > 0, "no " + what + " in the tab");
        List<String> intruders = new ArrayList<>();
        for (int i = first; i <= last; i++) if (!kind.test(ids.get(i))) intruders.add(ids.get(i));
        h.assertTrue(intruders.isEmpty(), what + " are split by " + intruders);
    }

    @GameTest(template = "empty", batch = "round111162_tab")
    public static void toolsAndBuildingBlocksStayTogether(GameTestHelper h) {
        List<String> ids = order(h);
        oneRun(h, ids, Round111162GameTests::tool, "the tools");
        oneRun(h, ids, Round111162GameTests::buildBlock, "the building blocks");
        h.assertTrue(ids.indexOf("item_tool_bronze_pickaxe") == 0, "the tab does not open with the tools: " + ids.get(0));
        h.succeed();
    }
}
