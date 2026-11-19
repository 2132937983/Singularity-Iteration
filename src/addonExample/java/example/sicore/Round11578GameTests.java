package example.sicore;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.78: the advancement tree. Every advancement loads (an unknown item drops it silently); since 0.1.11.81 it is
 * one tab (root, Singularity Iteration) and the former tab roots industrial_age (Energy, under Safe Transmission),
 * wrench (Tools and Gear, under the root) and ejector (Automation and Technology, under the Forge Hammer) are branches,
 * each still branching; a parent is a crafting prerequisite (the Electric Furnace comes after the
 * Forge Hammer and the Plate Cutter, steel after the Blast Furnace); frames, experience and hidden flags follow the
 * table; and the item criteria fire (any of a list, a full set, a tag, an enchantment).
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11578GameTests {
    private Round11578GameTests() {}

    /** 0.1.11.81: the only tab root, and the former tab roots that are its branches now (each still with several branches). */
    private static final String ROOT = "root";
    private static final List<String> BRANCH_ROOTS = List.of("root", "industrial_age", "wrench", "ejector");
    /** 114 with Curios; the flight ring advancement needs Curios (its item is a Curios trinket). 0.1.11.92: +3 for the fusion reactor. */
    private static boolean curios() { return net.neoforged.fml.ModList.get().isLoaded("curios"); }
    private static int expected() { return curios() ? 114 : 113; }

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }

    /** The mod's advancements without the recipe-book ones, by path. */
    private static Map<String, AdvancementHolder> tree(GameTestHelper h) {
        Map<String, AdvancementHolder> out = new HashMap<>();
        for (AdvancementHolder holder : h.getLevel().getServer().getAdvancements().getAllAdvancements()) {
            if (holder.id().getNamespace().equals("mio_icif") && !holder.id().getPath().startsWith("recipes/")) out.put(holder.id().getPath(), holder);
        }
        return out;
    }

    private static Optional<String> parent(AdvancementHolder holder) { return holder.value().parent().map(ResourceLocation::getPath); }

    /** True when {@code ancestor} is on the parent chain of {@code path}. */
    private static boolean after(Map<String, AdvancementHolder> tree, String path, String ancestor) {
        Optional<String> at = parent(tree.get(path));
        while (at.isPresent()) {
            if (at.get().equals(ancestor)) return true;
            at = parent(tree.get(at.get()));
        }
        return false;
    }

    @GameTest(template = "empty", batch = "round11578_tree", timeoutTicks = 20)
    public static void theTreeIsOneBranchingTab(GameTestHelper h) {
        Map<String, AdvancementHolder> tree = tree(h);
        h.assertTrue(tree.size() == expected(), "expected " + expected() + " advancements, loaded " + tree.size());
        h.assertTrue(tree.containsKey("flight_ring") == curios(), "the flight ring advancement does not follow Curios");
        Set<String> roots = new HashSet<>();
        Map<String, Integer> children = new HashMap<>();
        for (var e : tree.entrySet()) {
            Optional<String> p = parent(e.getValue());
            if (p.isEmpty()) roots.add(e.getKey());
            else {
                h.assertTrue(tree.containsKey(p.get()), e.getKey() + " has a missing parent " + p.get());
                children.merge(p.get(), 1, Integer::sum);
            }
            h.assertTrue(e.getValue().value().display().isPresent(), e.getKey() + " has no display");
        }
        h.assertTrue(roots.equals(Set.of(ROOT)), "tab roots (one expected): " + roots);
        for (String root : BRANCH_ROOTS)
            h.assertTrue(children.getOrDefault(root, 0) >= 4, root + " has only " + children.getOrDefault(root, 0) + " branches");
        // the former tab roots hang under their crafting prerequisite
        h.assertTrue(parent(tree.get("industrial_age")).equals(Optional.of("industrial_prologue")), "the Generator is not under Safe Transmission");
        h.assertTrue(parent(tree.get("wrench")).equals(Optional.of(ROOT)), "the Wrench is not under the root");
        h.assertTrue(parent(tree.get("ejector")).equals(Optional.of("hammer_time")), "the ejector upgrades are not under the Forge Hammer");
        for (String former : List.of("industrial_age", "wrench", "ejector"))
            h.assertTrue(tree.get(former).value().display().get().getBackground().isEmpty(), former + " still has a tab background");
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round11578_order", timeoutTicks = 20)
    public static void parentsAreCraftingPrerequisites(GameTestHelper h) {
        Map<String, AdvancementHolder> tree = tree(h);
        String[][] rules = {
            {"electric_furnace", "wire_cutter"}, {"electric_furnace", "hammer_time"}, {"electric_furnace", "circuit"},
            {"macerator", "wire_cutter"}, {"compressor", "industrial_prologue"}, {"extractor", "any_cable"},
            {"circuit", "industrial_prologue"}, {"industrial_prologue", "any_cable"}, {"any_cable", "wire_cutter"},
            {"wire_cutter", "hammer_time"}, {"induction_furnace", "electric_furnace"}, {"refined_steel", "blast_furnace"},
            {"advanced_machine", "refined_steel"}, {"recycler", "compressor"}, {"scrap_box", "recycler"},
            {"thermal_centrifuge", "ore_washer"}, {"canned_food", "canner"}, {"quantum_solar", "solar"},
            {"super_rotor", "rotor"}, {"geomagnetic", "kinetic_generator"}, {"gesu", "storage_upgrade"},
            {"energy_terminal", "eu_meter"}, {"fluid_reactor", "clean_energy"}, {"meltdown", "clean_energy"},
            {"iridium_drill", "diamond_drill"}, {"diamond_drill", "gold_miner"}, {"flight_ring", "advanced_jetpack"},
            {"like_a_boss", "laser"}, {"chainsaw_creeper", "chainsaw"}, {"know_nothing", "quantum_sword"},
            {"forgot_charge", "quantum_set"}, {"wingman", "armory_remote"}, {"phase_shift", "suit_module"},
            {"advanced_miner", "miner"}, {"far_teleport", "teleporter"}, {"end_paradise", "terraformer"},
            {"living_circuit", "nano_metal"}, {"picard_order", "ex_nihilo"},
            {"industrial_age", "industrial_prologue"}, {"solar", "wire_cutter"}, {"batbox", "any_cable"}, {"ejector", "hammer_time"},
            {"miner", "hammer_time"}, {"jetpack", "wrench"}};
        for (String[] r : rules) {
            if (r[0].equals("flight_ring") && !curios()) continue;
            h.assertTrue(tree.containsKey(r[0]) && tree.containsKey(r[1]), "missing " + r[0] + " or " + r[1]);
            h.assertTrue(after(tree, r[0], r[1]), r[0] + " does not come after " + r[1]);
        }
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round11578_frames", timeoutTicks = 20)
    public static void framesExperienceAndHiddenFollowTheTable(GameTestHelper h) {
        Map<String, AdvancementHolder> tree = tree(h);
        Object[][] challenges = {{"quantum_solar", 500}, {"clean_ambassador", 500}, {"gesu", 500}, {"industrial_diamond", 100},
            {"flight_ring", 100}, {"like_a_boss", 200}, {"forgot_charge", 100}, {"iridium_drill", 100}, {"nano_set", 100},
            {"far_teleport", 100}, {"end_paradise", 100}, {"super_rotor", 200}};
        for (Object[] c : challenges) {
            if (!tree.containsKey((String) c[0]) && c[0].equals("flight_ring") && !curios()) continue;
            var a = tree.get((String) c[0]).value();
            h.assertTrue(a.display().get().getType() == AdvancementType.CHALLENGE, c[0] + " is not a challenge");
            h.assertTrue(a.rewards().experience() == (int) c[1], c[0] + " gives " + a.rewards().experience() + " xp, not " + c[1]);
        }
        for (String goal : List.of("iridium_ore", "induction_furnace", "scrap_box", "advanced_machine", "kinetic_running", "storage_upgrade",
                "fluid_reactor", "chainsaw_creeper", "advanced_jetpack", "armory", "advanced_miner", "teleporter", "terraformer", "tesla", "futures"))
            h.assertTrue(tree.get(goal).value().display().get().getType() == AdvancementType.GOAL, goal + " is not a goal");
        for (String hidden : List.of("cable_burnt", "broken_machine", "industrial_accident", "meltdown", "know_nothing", "forgot_charge"))
            h.assertTrue(tree.get(hidden).value().display().get().isHidden(), hidden + " is not hidden");
        h.assertTrue(!tree.get("electric_furnace").value().display().get().isHidden(), "a plain advancement is hidden");
        h.succeed();
    }

    private static boolean done(ServerPlayer player, AdvancementHolder holder) { return player.getAdvancements().getOrStartProgress(holder).isDone(); }

    private static void give(ServerPlayer player, ItemStack stack) {
        player.getInventory().add(stack.copy());
        CriteriaTriggers.INVENTORY_CHANGED.trigger(player, player.getInventory(), stack);
    }

    @GameTest(template = "empty", batch = "round11578_criteria", timeoutTicks = 20)
    public static void itemCriteriaFire(GameTestHelper h) {
        Map<String, AdvancementHolder> tree = tree(h);
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        give(player, new ItemStack(BuiltInRegistries.ITEM.get(id("producer/block_furnace_elc"))));
        h.assertTrue(done(player, tree.get("electric_furnace")), "getting an Electric Furnace did not grant Hot Topic");
        give(player, new ItemStack(BuiltInRegistries.ITEM.get(id("wiring/block_mfsu"))));
        h.assertTrue(done(player, tree.get("storage_upgrade")), "an MFSU did not count as a storage upgrade");
        give(player, new ItemStack(BuiltInRegistries.ITEM.get(id("module/item_module_phase_modulator"))));
        h.assertTrue(done(player, tree.get("suit_module")), "a suit module (tag) did not grant The Mask");
        for (String piece : List.of("helmet", "chestplate", "leggings")) give(player, new ItemStack(BuiltInRegistries.ITEM.get(id("armor/item_armor_nano_" + piece))));
        h.assertTrue(!done(player, tree.get("nano_set")), "three nano pieces granted the full set");
        give(player, new ItemStack(BuiltInRegistries.ITEM.get(id("armor/item_armor_nano_boots"))));
        h.assertTrue(done(player, tree.get("nano_set")), "the full nano set did not grant Nanoscale Materials");
        var energySaving = h.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT)
            .getHolderOrThrow(ResourceKey.create(Registries.ENCHANTMENT, id("energy_saving")));
        ItemStack drill = new ItemStack(BuiltInRegistries.ITEM.get(id("item_tool_iron_driller")));
        h.assertTrue(!done(player, tree.get("energy_saving")), "Magic in Technology granted early");
        drill.enchant(energySaving, 1);
        give(player, drill);
        h.assertTrue(done(player, tree.get("energy_saving")), "an Energy Saving tool did not grant Magic in Technology");
        h.assertTrue(done(player, tree.get("gold_miner")), "an iron drill did not grant Meet the Dentist");
        h.succeed();
    }
}
