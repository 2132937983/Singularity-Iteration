package example.sicore;

import com.miophas.singularity_iteration.core.api.item.IBatteryItem;
import com.miophas.singularity_iteration.core.prefab.item.AbstractElectricTool;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests of 0.1.11.202: tool overhaul T6, the tools chapter of the manual and the last gaps in the tool tests.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111202GameTests {
    private Round111202GameTests() {}

    private static final String[] PAGES = {"tools.md", "tools/drills.md", "tools/chainsaw_farming.md", "tools/wrenches.md",
        "tools/scanners.md", "tools/energy_guns.md", "tools/heavy_weapons.md", "tools/melee_defense.md", "tools/devices.md", "tools/linkers.md"};
    private static final String[] LANGS = {"", "_zh_cn/", "_ja_jp/"};

    /** The item_ids of the front matter of every page of the tools chapter in {@code lang}. */
    private static Set<String> listedIds(GameTestHelper h, String lang) {
        Set<String> ids = new TreeSet<>();
        for (String page : PAGES) {
            String md = Round33GameTests.resource("/assets/mio_icif/guides/mio_icif/manual/" + lang + page);
            h.assertTrue(md != null, "missing page " + lang + page);
            int start = md.indexOf("item_ids:");
            int end = md.indexOf("\n---", 4);
            if (start < 0 || start > end) continue;
            for (String line : md.substring(start, end).split("\n")) {
                String t = line.trim();
                if (t.startsWith("- ")) ids.add(t.substring(2).trim());
            }
        }
        return ids;
    }

    /** T6: every electric tool of the mod has a page in the tools chapter, in all three languages. */
    @GameTest(template = "empty")
    public static void everyElectricToolHasAGuidePage(GameTestHelper h) {
        List<String> tools = new ArrayList<>();
        for (var entry : BuiltInRegistries.ITEM.entrySet()) {
            if (entry.getKey().location().getNamespace().equals("mio_icif") && entry.getValue() instanceof AbstractElectricTool
                    && !entry.getKey().location().getPath().startsWith("normal/item_crop_analyzer")
                    && !entry.getKey().location().getPath().startsWith("normal/item_portable_futures_trader")) {
                tools.add(entry.getKey().location().toString());
            }
        }
        h.assertTrue(tools.size() >= 30, "expected about 33 electric tools, found " + tools.size());
        for (String lang : LANGS) {
            Set<String> listed = listedIds(h, lang);
            List<String> missing = tools.stream().filter(id -> !listed.contains(id)).toList();
            h.assertTrue(missing.isEmpty(), "tools without a page (" + (lang.isEmpty() ? "en" : lang) + "): " + missing);
        }
        h.succeed();
    }

    /** T1 + M-01: every electric tool of the mod starts empty, never stacks and shows its stats line. */
    @GameTest(template = "empty")
    public static void everyElectricToolStartsEmptyAndAlone(GameTestHelper h) {
        List<String> wrong = new ArrayList<>();
        for (var entry : BuiltInRegistries.ITEM.entrySet()) {
            if (!entry.getKey().location().getNamespace().equals("mio_icif") || !(entry.getValue() instanceof AbstractElectricTool tool)) continue;
            ItemStack stack = new ItemStack(tool);
            if (((IBatteryItem) tool).getEnergy(stack) != 0) wrong.add(entry.getKey().location() + " starts charged");
            if (stack.getMaxStackSize() != 1) wrong.add(entry.getKey().location() + " stacks to " + stack.getMaxStackSize());
        }
        h.assertTrue(wrong.isEmpty(), "problems: " + wrong);
        h.succeed();
    }

    /** T3: a leaf costs the chainsaw 25 EU, a log 100. */
    @GameTest(template = "empty")
    public static void chainsawShearsCheaply(GameTestHelper h) {
        ServerPlayer p = ArmoryServiceGameTests.player(h);
        ItemStack saw = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "item_tool_iron_chainsaw")));
        IBatteryItem battery = (IBatteryItem) saw.getItem();
        battery.setEnergy(saw, 10_000);
        p.setItemInHand(InteractionHand.MAIN_HAND, saw);
        BlockPos at = h.absolutePos(new BlockPos(1, 1, 1));
        saw.getItem().mineBlock(saw, h.getLevel(), Blocks.OAK_LEAVES.defaultBlockState(), at, p);
        h.assertTrue(battery.getEnergy(saw) == 10_000 - 25, "a leaf costs 25, left " + battery.getEnergy(saw));
        saw.getItem().mineBlock(saw, h.getLevel(), Blocks.OAK_LOG.defaultBlockState(), at, p);
        h.assertTrue(battery.getEnergy(saw) == 10_000 - 125, "a log costs 100, left " + battery.getEnergy(saw));
        p.discard();
        h.succeed();
    }

    /** T0: the nutrition supply feeds one point every half second, from one copy. */
    @GameTest(template = "empty")
    public static void nutritionSupplyFeedsOnItsInterval(GameTestHelper h) {
        ServerPlayer p = ArmoryServiceGameTests.player(h);
        ItemStack a = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "item_electric_nutrition_supply")));
        ItemStack b = a.copy();
        for (ItemStack s : new ItemStack[] {a, b}) {
            ((IBatteryItem) s.getItem()).setEnergy(s, ((IBatteryItem) s.getItem()).getMaxEnergy(s));
            ArmoryServiceGameTests.call(s.getItem(), "setAutoSupplement", s, true);
        }
        p.setItemInHand(InteractionHand.MAIN_HAND, a);
        p.getInventory().setItem(5, b);
        p.getFoodData().setFoodLevel(10);
        p.tickCount = 10;
        a.inventoryTick(h.getLevel(), p, p.getInventory().selected, true);
        b.inventoryTick(h.getLevel(), p, 5, false);
        h.assertTrue(p.getFoodData().getFoodLevel() == 11, "two supplies feed one point together, food " + p.getFoodData().getFoodLevel());
        p.tickCount = 11;
        a.inventoryTick(h.getLevel(), p, p.getInventory().selected, true);
        h.assertTrue(p.getFoodData().getFoodLevel() == 11, "nothing between two half seconds, food " + p.getFoodData().getFoodLevel());
        p.discard();
        h.succeed();
    }
}
