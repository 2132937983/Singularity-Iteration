package example.sicore;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Data-pack self audit (0.1.7.26). One unknown, required id in a tag JSON makes Minecraft drop the
 * WHOLE tag - a stale entry in {@code #minecraft:mineable/pickaxe} broke pickaxe mining of every
 * stone and ore. These tests scan every tag, loot table and recipe the mod jar ships and fail on
 * any id the registries do not know.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class DataIntegrityGameTests {
    private DataIntegrityGameTests() {}

    private static Path dataRoot() {
        return net.neoforged.fml.ModList.get().getModFileById("mio_icif").getFile().findResource("data");
    }

    private static List<Path> json(Path root) {
        if (!Files.isDirectory(root)) return List.of();
        try (Stream<Path> s = Files.walk(root)) {
            return s.filter(p -> p.toString().endsWith(".json")).toList();
        } catch (IOException e) { throw new AssertionError(e); }
    }

    private static JsonObject read(Path p) {
        try { return JsonParser.parseString(Files.readString(p).replace("﻿", "")).getAsJsonObject(); }
        catch (Exception e) { throw new AssertionError("Unreadable JSON " + p + ": " + e.getMessage()); }
    }

    /** Registry key for a tag folder (block, item, fluid, entity_type, worldgen/biome ...). */
    private static Registry<?> registryFor(GameTestHelper h, String folder) {
        ResourceLocation id = ResourceLocation.withDefaultNamespace(folder);
        Registry<?> builtin = BuiltInRegistries.REGISTRY.get(id);
        if (builtin != null) return builtin;
        return h.getLevel().registryAccess().registry(ResourceKey.<Registry<Object>>createRegistryKey(id)).orElse(null);
    }

    @GameTest(batch = "data_integrity", template = "empty")
    public static void everyTagEntryResolves(GameTestHelper h) {
        List<String> problems = new ArrayList<>();
        int entries = 0;
        Path data = dataRoot();
        try (Stream<Path> namespaces = Files.list(data)) {
            for (Path ns : namespaces.toList()) {
                String owner = ns.getFileName().toString();
                // a tag owned by another mod (curios, ...) only matters when that mod is loaded; our
                // entries in it may be registered only alongside it
                if (!List.of("minecraft", "c", "neoforge", "mio_icif").contains(owner)
                        && !net.neoforged.fml.ModList.get().isLoaded(owner)) continue;
                Path tags = ns.resolve("tags");
                for (Path file : json(tags)) {
                    String rel = tags.relativize(file).toString().replace('\\', '/');
                    // registry folder: first segment, or worldgen/<x>
                    String[] seg = rel.split("/");
                    String folder = seg[0].equals("worldgen") ? seg[0] + "/" + seg[1] : seg[0];
                    Registry<?> registry = registryFor(h, folder);
                    if (registry == null) continue;          // registry of an absent mod
                    String tagFolder = folder;
                    JsonArray values = read(file).getAsJsonArray("values");
                    if (values == null) continue;
                    for (JsonElement v : values) {
                        String ref; boolean required = true;
                        if (v.isJsonObject()) {
                            ref = v.getAsJsonObject().get("id").getAsString();
                            required = !v.getAsJsonObject().has("required") || v.getAsJsonObject().get("required").getAsBoolean();
                        } else ref = v.getAsString();
                        entries++;
                        String where = ns.getFileName() + "/tags/" + rel;
                        if (ref.startsWith("#")) continue;    // nested tags: checked by Minecraft, optional where foreign
                        ResourceLocation id = ResourceLocation.parse(ref);
                        boolean known = registry.containsKey(id);
                        // vanilla ids must always resolve; our own ids too, even when marked optional
                        if (!known && (required || id.getNamespace().equals("mio_icif") || id.getNamespace().equals("minecraft")))
                            problems.add(where + " -> " + ref + (required ? " (REQUIRED: would void the tag)" : " (optional, dead entry)"));
                        if (required && !id.getNamespace().equals("minecraft") && !tagFolder.isEmpty()
                                && !ns.getFileName().toString().equals("mio_icif") )
                            problems.add(where + " -> " + ref + " must be {\"id\":..., \"required\": false} in a shared tag");
                    }
                }
            }
        } catch (IOException e) { throw new AssertionError(e); }
        h.assertTrue(entries > 1000, "Scanned the tags (" + entries + " entries)");
        h.assertTrue(problems.isEmpty(), problems.size() + " tag problems:\n" + String.join("\n", problems.subList(0, Math.min(60, problems.size()))));
        h.succeed();
    }

    @GameTest(batch = "data_integrity", template = "empty")
    public static void vanillaMiningTagsSurvive(GameTestHelper h) {
        h.assertTrue(Blocks.STONE.defaultBlockState().is(BlockTags.MINEABLE_WITH_PICKAXE), "#minecraft:mineable/pickaxe still holds stone");
        h.assertTrue(Blocks.DIAMOND_ORE.defaultBlockState().is(BlockTags.MINEABLE_WITH_PICKAXE), "... and vanilla ores");
        h.assertTrue(Blocks.IRON_ORE.defaultBlockState().is(BlockTags.NEEDS_STONE_TOOL), "#minecraft:needs_stone_tool intact");
        h.assertTrue(Blocks.DIRT.defaultBlockState().is(BlockTags.MINEABLE_WITH_SHOVEL), "#minecraft:mineable/shovel intact");
        h.assertTrue(Blocks.OAK_LOG.defaultBlockState().is(BlockTags.MINEABLE_WITH_AXE), "#minecraft:mineable/axe intact");
        Block terminal = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "wiring/block_energy_terminal"));
        h.assertTrue(terminal != Blocks.AIR, "The energy terminal is registered");
        h.assertTrue(terminal.defaultBlockState().is(BlockTags.MINEABLE_WITH_PICKAXE), "The energy terminal is pickaxe-mineable");
        h.succeed();
    }

    /** Every SI block that needs the right tool must be in a mineable tag, or it can never drop. */
    @GameTest(batch = "data_integrity", template = "empty")
    public static void blocksNeedingAToolAreMineableAndHaveLoot(GameTestHelper h) {
        List<String> noTool = new ArrayList<>(), noLoot = new ArrayList<>();
        var server = h.getLevel().getServer();
        for (var e : BuiltInRegistries.BLOCK.entrySet()) {
            if (!e.getKey().location().getNamespace().equals("mio_icif")) continue;
            Block b = e.getValue();
            var state = b.defaultBlockState();
            if (state.requiresCorrectToolForDrops()
                    && !(state.is(BlockTags.MINEABLE_WITH_PICKAXE) || state.is(BlockTags.MINEABLE_WITH_AXE)
                         || state.is(BlockTags.MINEABLE_WITH_SHOVEL) || state.is(BlockTags.MINEABLE_WITH_HOE)))
                noTool.add(e.getKey().location().toString());
            var lootKey = b.getLootTable();
            boolean codeDrops;
            try {
                codeDrops = b.getClass().getMethod("getDrops", net.minecraft.world.level.block.state.BlockState.class,
                    net.minecraft.world.level.storage.loot.LootParams.Builder.class).getDeclaringClass()
                    != net.minecraft.world.level.block.state.BlockBehaviour.class;
            } catch (NoSuchMethodException ex) { codeDrops = false; }
            if (!codeDrops && lootKey != null && b.asItem() != net.minecraft.world.item.Items.AIR
                    && server.reloadableRegistries().getLootTable(lootKey) == LootTable.EMPTY)
                noLoot.add(e.getKey().location().toString());
        }
        h.assertTrue(noTool.isEmpty(), noTool.size() + " blocks need a tool but are in no mineable tag (they never drop): " + noTool);
        h.assertTrue(noLoot.isEmpty(), noLoot.size() + " placeable blocks without a loot table: " + noLoot);
        h.succeed();
    }

    private static void collectNames(JsonElement e, List<String> items, List<String> blocks) {
        if (e == null) return;
        if (e.isJsonArray()) { for (JsonElement c : e.getAsJsonArray()) collectNames(c, items, blocks); return; }
        if (!e.isJsonObject()) return;
        JsonObject o = e.getAsJsonObject();
        String type = o.has("type") ? o.get("type").getAsString() : "";
        if ((type.equals("minecraft:item") || type.equals("item")) && o.has("name")) items.add(o.get("name").getAsString());
        if (o.has("block") && o.get("block").isJsonPrimitive()) blocks.add(o.get("block").getAsString());
        for (var kv : o.entrySet()) collectNames(kv.getValue(), items, blocks);
    }

    @GameTest(batch = "data_integrity", template = "empty")
    public static void lootTablesReferenceRealBlocksAndItems(GameTestHelper h) {
        List<String> problems = new ArrayList<>();
        Path tables = dataRoot().resolve("mio_icif").resolve("loot_table");
        for (Path file : json(tables)) {
            String rel = tables.relativize(file).toString().replace('\\', '/');
            if (rel.startsWith("blocks/")) {
                String block = rel.substring(7, rel.length() - 5);
                if (!BuiltInRegistries.BLOCK.containsKey(ResourceLocation.fromNamespaceAndPath("mio_icif", block)))
                    problems.add(rel + ": no block mio_icif:" + block);
            }
            List<String> items = new ArrayList<>(), blocks = new ArrayList<>();
            collectNames(read(file), items, blocks);
            for (String i : items) if (!BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(i))) problems.add(rel + ": unknown item " + i);
            for (String b : blocks) if (!BuiltInRegistries.BLOCK.containsKey(ResourceLocation.parse(b))) problems.add(rel + ": unknown block " + b);
        }
        h.assertTrue(problems.isEmpty(), problems.size() + " loot problems: " + problems);
        h.succeed();
    }

    /** A recipe with an unknown item is silently dropped at load; every shipped recipe must load. */
    @GameTest(batch = "data_integrity", template = "empty")
    public static void everyShippedRecipeLoads(GameTestHelper h) {
        List<String> missing = new ArrayList<>();
        Path recipes = dataRoot().resolve("mio_icif").resolve("recipe");
        var manager = h.getLevel().getRecipeManager();
        int checked = 0;
        for (Path file : json(recipes)) {
            JsonObject o = read(file);
            if (o.has("neoforge:conditions")) continue;          // conditional on another mod
            String rel = recipes.relativize(file).toString().replace('\\', '/');
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath("mio_icif", rel.substring(0, rel.length() - 5));
            checked++;
            if (manager.byKey(id).isEmpty()) missing.add(id.toString());
        }
        h.assertTrue(checked > 100, "Scanned " + checked + " recipes");
        h.assertTrue(missing.isEmpty(), missing.size() + " recipes failed to load: " + missing.subList(0, Math.min(40, missing.size())));
        h.succeed();
    }
}
