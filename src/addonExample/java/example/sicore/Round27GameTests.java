package example.sicore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.nio.file.Files;
import java.util.List;

/**
 * 0.1.7.27: the voltage detector / terminal see through transformers and storage boxes, network
 * scans are cached against the topology clock, "EU/p" is gone, and the appearance styles swap
 * (classic = default in assets/, refined = built-in experimental pack, items follow blocks).
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round27GameTests {
    private Round27GameTests() {}

    private static final String WALKER = "com.miophas.singularity_iteration" + ".common.blockentity.wiring.terminal.NetworkWalker";

    private static Block block(String path) { return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", path)); }
    private static Object call(Object target, String name, Object... args) { return ArmoryServiceGameTests.call(target, name, args); }
    private static Object field(Object target, String name) { return ArmoryServiceGameTests.field(target, name); }

    private static Class<?> walker() {
        try { return Class.forName(WALKER); } catch (ClassNotFoundException e) { throw new AssertionError(e); }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object scope(String name) {
        try { return Enum.valueOf((Class) Class.forName(WALKER + "$Scope"), name); }
        catch (ClassNotFoundException e) { throw new AssertionError(e); }
    }

    private static Object walk(GameTestHelper h, BlockPos at, String scope) {
        try {
            return walker().getMethod("walkUncached", ServerLevel.class, BlockPos.class, Class.forName(WALKER + "$Scope"), int.class, int.class)
                .invoke(null, h.getLevel(), h.absolutePos(at), scope(scope), 4096, 256);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

    private static Object walkCached(GameTestHelper h, BlockPos at) {
        try {
            return walker().getMethod("walk", ServerLevel.class, BlockPos.class, Class.forName(WALKER + "$Scope"), int.class, int.class)
                .invoke(null, h.getLevel(), h.absolutePos(at), scope("SYSTEM"), 4096, 256);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

    private static boolean sees(GameTestHelper h, Object result, BlockPos rel) {
        return ((List<?>) field(result, "devices")).contains(h.absolutePos(rel));
    }

    /** Symptom 1: transformer - superconducting alloy cable (probe here) - transformer - consumer. */
    @GameTest(batch = "round27_topology", template = "reactor_loop", timeoutTicks = 40)
    public static void probeBetweenTwoTransformersSeesBothSides(GameTestHelper h) {
        BlockPos source = new BlockPos(0, 2, 1), t1 = new BlockPos(2, 2, 1), t2 = new BlockPos(6, 2, 1), sink = new BlockPos(8, 2, 1);
        BlockPos probe = new BlockPos(4, 2, 1), terminal = new BlockPos(4, 2, 2);
        h.setBlock(source, CoreExampleMod.SOURCE.get());
        h.setBlock(new BlockPos(1, 2, 1), block("wiring/cable/block_tin_cable"));
        h.setBlock(t1, block("wiring/transformer_lv_mv"));
        for (int x = 3; x <= 5; x++) h.setBlock(new BlockPos(x, 2, 1), block("wiring/cable/block_superconducting_cable"));
        h.setBlock(t2, block("wiring/transformer_lv_mv"));
        h.setBlock(new BlockPos(7, 2, 1), block("wiring/cable/block_tin_cable"));
        h.setBlock(sink, CoreExampleMod.SINK.get());
        h.setBlock(terminal, block("wiring/block_energy_terminal"));
        h.runAfterDelay(3, () -> {
            Object segment = walk(h, probe, "SEGMENT");
            h.assertTrue(sees(h, segment, t1) && sees(h, segment, t2) && !sees(h, segment, sink), "SEGMENT: just the two transformers");
            Object system = walk(h, probe, "SYSTEM");
            h.assertTrue(sees(h, system, sink), "The consumer behind the second transformer is found");
            h.assertTrue(sees(h, system, source), "The generator behind the first transformer is found");
            h.assertTrue(((List<?>) field(system, "subnets")).size() == 3, "Three voltage segments, got " + ((List<?>) field(system, "subnets")).size());
            h.assertTrue((int) field(system, "conductors") == 5, "5 cables, got " + field(system, "conductors"));
            // a fresh terminal scans the whole system by default
            BlockEntity term = h.getBlockEntity(terminal);
            h.assertTrue((boolean) call(term, "globalMode"), "New terminals start in GLOBAL");
            h.succeed();
        });
    }

    /** Symptom 2: transformer - cable (probe) - storage box - cable - consumer / generator. */
    @GameTest(batch = "round27_topology", template = "reactor_loop", timeoutTicks = 40)
    public static void probeBeforeAStorageBoxSeesThroughIt(GameTestHelper h) {
        BlockPos source = new BlockPos(0, 2, 1), xfmr = new BlockPos(2, 2, 1), box = new BlockPos(5, 2, 1), sink = new BlockPos(8, 2, 1);
        BlockPos farSource = new BlockPos(7, 2, 2), probe = new BlockPos(3, 2, 1);
        h.setBlock(source, CoreExampleMod.SOURCE.get());
        h.setBlock(new BlockPos(1, 2, 1), block("wiring/cable/block_tin_cable"));
        h.setBlock(xfmr, block("wiring/transformer_lv_mv"));
        for (int x : new int[]{3, 4, 6, 7}) h.setBlock(new BlockPos(x, 2, 1), block("wiring/cable/block_tin_cable"));
        h.setBlock(box, block("wiring/block_bat_box"));
        h.setBlock(sink, CoreExampleMod.SINK.get());
        h.setBlock(farSource, CoreExampleMod.SOURCE.get());
        h.runAfterDelay(3, () -> {
            Object segment = walk(h, probe, "SEGMENT");
            h.assertTrue(sees(h, segment, xfmr) && sees(h, segment, box) && !sees(h, segment, sink), "SEGMENT ends at the transformer and the BatBox");
            Object system = walk(h, probe, "SYSTEM");
            h.assertTrue(sees(h, system, sink), "Consumer behind the BatBox is found");
            h.assertTrue(sees(h, system, farSource), "Generator behind the BatBox is found");
            h.assertTrue(sees(h, system, source), "Generator behind the transformer is found");
            var origin = (it.unimi.dsi.fastutil.ints.IntOpenHashSet) field(system, "originSubnets");
            h.assertTrue(origin.size() == 1, "The probe sits on exactly one segment");
            h.succeed();
        });
    }

    /** The meter on a cable feeding a storage box reads the energy going in. */
    @GameTest(batch = "round27_meter", template = "reactor_loop", timeoutTicks = 160)
    public static void meterReadsFlowIntoAStorageBox(GameTestHelper h) {
        BlockPos source = new BlockPos(1, 2, 1), probe = new BlockPos(3, 2, 1), box = new BlockPos(5, 2, 1);
        h.setBlock(source, CoreExampleMod.SOURCE.get());
        for (int x = 2; x <= 4; x++) h.setBlock(new BlockPos(x, 2, 1), block("wiring/cable/block_tin_cable"));
        h.setBlock(box, block("wiring/block_bat_box"));
        InheritedSource src = h.getBlockEntity(source);
        var player = h.makeMockServerPlayerInLevel();
        AbstractContainerMenu meter;
        try {
            meter = (AbstractContainerMenu) Class.forName("com.miophas.singularity_iteration" + ".common.menu.tool.mio_icif_meter_menu")
                .getConstructor(int.class, Inventory.class, BlockPos.class, int.class)
                .newInstance(9, player.getInventory(), h.absolutePos(probe), 0);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
        player.containerMenu = meter;
        h.onEachTick(() -> {
            src.generate(32);
            if (player.containerMenu == meter) call(meter, "sampleCompletedTick", player);
        });
        h.succeedWhen(() -> {
            h.assertTrue(player.containerMenu == meter, "Meter stays open");
            h.assertTrue((double) call(meter, "getResultMax") > 0, "Energy into the BatBox is measured, max " + call(meter, "getResultMax"));
        });
    }

    /** A repeated scan of an unchanged network is served from the cache; an edit invalidates it. */
    @GameTest(batch = "round27_cache", template = "reactor_loop", timeoutTicks = 40)
    public static void scansAreCachedUntilTheTopologyChanges(GameTestHelper h) {
        BlockPos source = new BlockPos(1, 2, 1), sink = new BlockPos(5, 2, 1);
        h.setBlock(source, CoreExampleMod.SOURCE.get());
        for (int x = 2; x <= 4; x++) h.setBlock(new BlockPos(x, 2, 1), block("wiring/cable/block_tin_cable"));
        h.setBlock(sink, CoreExampleMod.SINK.get());
        h.runAfterDelay(2, () -> {
            Object a = walkCached(h, new BlockPos(3, 2, 1)), b = walkCached(h, new BlockPos(3, 2, 1));
            h.assertTrue(a == b, "Second scan reuses the cached result");
            h.setBlock(new BlockPos(3, 2, 2), block("wiring/cable/block_tin_cable"));
            h.setBlock(new BlockPos(3, 2, 3), CoreExampleMod.SINK.get());
            Object c = walkCached(h, new BlockPos(3, 2, 1));
            h.assertTrue(c != b, "A placed cable invalidates the cache");
            h.assertTrue(sees(h, c, new BlockPos(3, 2, 3)), "...and the new consumer is found");
            h.succeed();
        });
    }

    private static String read(String... path) {
        var file = net.neoforged.fml.ModList.get().getModFileById("mio_icif").getFile();
        try { return Files.readString(file.findResource(path)); } catch (java.io.IOException e) { throw new AssertionError(e); }
    }

    @GameTest(batch = "round27_assets", template = "empty")
    public static void unitsAndLanguageFilesAreComplete(GameTestHelper h) {
        for (String lang : List.of("en_us", "zh_cn", "ja_jp")) {
            String text = read("assets", "mio_icif", "lang", lang + ".json");
            h.assertTrue(!text.matches("(?s).*EU/[pP]\\b.*"), lang + " no longer uses the EU/p unit");
            h.assertTrue(text.contains("gui.mio_icif.equipment_console.appearance.experimental"), lang + " names the Experimental style");
        }
        var en = com.google.gson.JsonParser.parseString(read("assets", "mio_icif", "lang", "en_us.json")).getAsJsonObject();
        var zh = com.google.gson.JsonParser.parseString(read("assets", "mio_icif", "lang", "zh_cn.json")).getAsJsonObject();
        h.assertTrue(en.size() > 2100 && zh.size() > 2100, "Language files did not lose keys (en " + en.size() + ", zh " + zh.size() + ")");
        List<String> missing = en.keySet().stream().filter(k -> !zh.has(k)).toList();
        h.assertTrue(missing.size() <= 2, "zh_cn covers en_us, missing " + missing);
        h.assertTrue(en.get("gui.mio_icif.equipment_console.appearance.default").getAsString().equalsIgnoreCase("default"), "Default style name");
        h.succeed();
    }

    @GameTest(batch = "round27_assets", template = "empty")
    public static void classicIsDefaultAndItemsFollowTheBlocks(GameTestHelper h) {
        var file = net.neoforged.fml.ModList.get().getModFileById("mio_icif").getFile();
        // base = classic: the item IS the block model, so it can never disagree with the placed block
        String baseItem = read("assets", "mio_icif", "models", "item", "producer", "block_compressor_elc.json");
        h.assertTrue(baseItem.contains("\"mio_icif:block/producer/block_compressor_elc\"") && !baseItem.contains("item/icon"),
            "Default machine item renders its block model: " + baseItem);
        h.assertTrue(read("assets", "mio_icif", "si_status_lamps.json").contains("\"blocks\": []"), "Default look has no status lamps");
        h.assertTrue(!read("assets", "mio_icif", "models", "block", "producer", "block_compressor_elc.json").contains("panels_ns"),
            "Default compressor is the classic full cube");
        h.assertTrue(!Files.exists(file.findResource("resourcepacks", "si_classic", "pack.mcmeta")), "The old classic pack is gone");
        // experimental pack: refined block + DSP icon item, both switched together
        String expItem = read("resourcepacks", "si_experimental", "assets", "mio_icif", "models", "item", "producer", "block_compressor_elc.json");
        h.assertTrue(expItem.contains("item/icon/producer/block_compressor_elc"), "Experimental item uses the DSP icon");
        h.assertTrue(Files.exists(file.findResource("resourcepacks", "si_experimental", "assets", "mio_icif", "textures", "item", "icon", "producer", "block_compressor_elc.png")),
            "Experimental pack carries the icon texture");
        h.assertTrue(read("resourcepacks", "si_experimental", "assets", "mio_icif", "models", "block", "producer", "block_compressor_elc.json").contains("panels_ns"),
            "Experimental compressor is the refined model");
        h.succeed();
    }
}
