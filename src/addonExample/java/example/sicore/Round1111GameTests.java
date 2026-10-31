package example.sicore;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.miophas.singularity_iteration.core.api.fluid.IFluidPorts;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * GameTests of 0.1.10.11: the 3D sword models in the hand (items 1 and 6), the Creative Generator
 * (item 3), canner to canner fluid ejection (item 4), the chunk loader server rules (item 5), the
 * Wingman idle landing and blade slashes (items 7 to 9) and the Ultra-High Energy Blast sound (item 10).
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round1111GameTests {
    private Round1111GameTests() {}

    private static final String COMMON = "com.miophas.singularity_iteration" + ".common.";

    private static ResourceLocation rl(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }
    private static Block block(String path) { return BuiltInRegistries.BLOCK.get(rl(path)); }
    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    private static Class<?> common(String name) {
        try {
            return Class.forName(COMMON + name);
        } catch (ClassNotFoundException e) {
            throw new AssertionError(name, e);
        }
    }

    // ------------------------------------------------------------------ items 1 and 6

    /**
     * The sword models are the element models themselves (no separate_transforms wrapper that showed
     * the flat icon), every element rotation is one the 1.21.1 model loader accepts (a free rotation
     * made the model fail to load: the purple-black missing model in the hand), the quantum swords
     * carry their own GUI transform, and models/item/3d is gone.
     */
    @GameTest(batch = "round1111_models", template = "empty", timeoutTicks = 20)
    public static void swordModelsLoadInTheHand(GameTestHelper h) {
        List<String> problems = new ArrayList<>();
        Set<Double> angles = Set.of(-45.0, -22.5, 0.0, 22.5, 45.0);
        for (String name : new String[]{"item_tool_nanosaber", "item_tool_nanosaber_active", "item_tool_quantum_sword", "item_tool_quantum_sword_hyper"}) {
            String json = Round33GameTests.resource("/assets/mio_icif/models/item/" + name + ".json");
            if (json == null) { problems.add(name + " missing"); continue; }
            JsonObject model = JsonParser.parseString(json).getAsJsonObject();
            if (json.contains("separate_transforms")) problems.add(name + " is still a wrapper");
            if (!model.has("elements") || model.getAsJsonArray("elements").isEmpty()) problems.add(name + " has no elements");
            else for (JsonElement e : model.getAsJsonArray("elements")) {
                JsonObject element = e.getAsJsonObject();
                if (!element.has("rotation")) continue;
                JsonObject r = element.getAsJsonObject("rotation");
                if (!r.has("axis") || !angles.contains(r.get("angle").getAsDouble())) problems.add(name + " rotation " + r);
            }
            if (name.startsWith("item_tool_quantum") && !(model.has("display") && model.getAsJsonObject("display").has("gui")))
                problems.add(name + " has no gui transform");
            // 0.1.10.42 (from the removed Round1104 test): textures exist, the active/hyper overrides stay, no keys 1.21.1 ignores
            if (model.has("textures")) for (var t : model.getAsJsonObject("textures").entrySet()) {
                var tex = net.minecraft.resources.ResourceLocation.parse(t.getValue().getAsString());
                if (!Round180GameTests.modResourceExists("assets/" + tex.getNamespace() + "/textures/" + tex.getPath() + ".png")) problems.add(name + " texture " + tex);
            }
            if (name.equals("item_tool_nanosaber") && !json.contains("mio_icif:nanosaber_active")) problems.add(name + " lost its active override");
            if (name.equals("item_tool_quantum_sword") && !json.contains("mio_icif:quantum_sword_hyper")) problems.add(name + " lost its hyper override");
            if (json.contains("light_emission") || json.contains("on_shelf")) problems.add(name + " keeps keys 1.21.1 does not read");
        }
        if (Round33GameTests.resource("/assets/mio_icif/models/item/3d/weapon/nanosaber.json") != null) problems.add("models/item/3d is still there");
        h.assertTrue(problems.isEmpty(), "sword models: " + problems);
        h.succeed();
    }

    // ------------------------------------------------------------------ item 3

    /** Output parsing of the Creative Generator screen: plain, grouped, suffixed, out of range. */
    @GameTest(batch = "round1111_creative", template = "empty", timeoutTicks = 20)
    public static void creativeGeneratorParsesTheOutput(GameTestHelper h) {
        Class<?> menu = common("menu.generator.CreativeGeneratorMenu");
        h.assertTrue((Long) call(menu, "parseRate", "8192") == 8192L, "plain");
        h.assertTrue((Long) call(menu, "parseRate", "1,048,576") == 1_048_576L, "grouped");
        h.assertTrue((Long) call(menu, "parseRate", "2.5M") == 2_500_000L, "suffix M");
        h.assertTrue((Long) call(menu, "parseRate", "1T") == 1_000_000_000_000L, "suffix T");
        h.assertTrue((Long) call(menu, "parseRate", "2T") == -1L, "above the maximum");
        h.assertTrue((Long) call(menu, "parseRate", "abc") == -1L, "not a number");
        h.assertTrue((Long) call(menu, "parseRate", "-5") == -1L, "negative");
        h.succeed();
    }

    /**
     * 100,000 EU/t through a tin cable (fuse 33 EU) into an LV machine: the cable does not melt, the
     * machine does not explode, and energy arrives.
     */
    @GameTest(batch = "round1111_creative", template = "empty", timeoutTicks = 120)
    public static void creativeGeneratorFeedsWithoutOverload(GameTestHelper h) {
        BlockPos gen = new BlockPos(1, 1, 2), cable = gen.east(), machine = cable.east();
        Block tin = block("wiring/cable/block_tin_cable"), furnace = block("producer/block_furnace_elc");
        h.setBlock(gen, block("generator/block_creative_generator"));
        h.setBlock(cable, tin);
        h.setBlock(machine, furnace);
        BlockEntity generator = h.getBlockEntity(gen);
        call(generator, "configure", 100_000L, true);
        h.runAfterDelay(60, () -> {
            h.assertTrue(h.getBlockState(cable).is(tin), "the tin cable melted");
            h.assertTrue(h.getBlockState(machine).is(furnace), "the machine exploded");
            long stored = ((AbstractEnergyBlockEntity) h.getBlockEntity(machine)).getEnergyStorageInternal().getAmount();
            h.assertTrue(stored > 0, "no energy arrived");
            h.assertTrue((Long) call(generator, "rate") == 100_000L, "output not kept");
            h.succeed();
        });
    }

    /** Only a creative player (or an operator) may use the generator; its output is clamped. */
    @GameTest(batch = "round1111_creative", template = "empty", timeoutTicks = 20)
    public static void creativeGeneratorIsForCreativePlayers(GameTestHelper h) {
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(h.getLevel());
        Class<?> blockType = common("block.generator.mio_icif_block_creative_generator");
        player.setGameMode(GameType.SURVIVAL);
        h.assertFalse((Boolean) call(blockType, "allowed", player), "a survival player may use it");
        player.setGameMode(GameType.CREATIVE);
        h.assertTrue((Boolean) call(blockType, "allowed", player), "a creative player may not use it");
        player.setGameMode(GameType.SURVIVAL);
        BlockPos at = new BlockPos(2, 1, 2);
        h.setBlock(at, block("generator/block_creative_generator"));
        BlockEntity generator = h.getBlockEntity(at);
        call(generator, "configure", Long.MAX_VALUE, true);
        h.assertTrue((Long) call(generator, "rate") == 1_000_000_000_000L, "output not clamped");
        h.assertTrue(block("generator/block_creative_generator").defaultBlockState().getDestroySpeed(h.getLevel(), h.absolutePos(at)) < 0,
            "the generator can be mined");
        h.succeed();
    }

    // ------------------------------------------------------------------ item 4

    /** A canner that ejects into the next canner fills that canner's input (left) tank, not its output tank. */
    @GameTest(batch = "round1111_canner", template = "empty", timeoutTicks = 120)
    public static void cannerEjectsIntoTheLeftTankOfTheNextCanner(GameTestHelper h) {
        BlockPos a = new BlockPos(1, 1, 2), b = a.east();
        h.setBlock(a, block("producer/block_canner_elc"));
        h.setBlock(b, block("producer/block_canner_elc"));
        BlockEntity first = h.getBlockEntity(a), second = h.getBlockEntity(b);
        IItemHandler items = (IItemHandler) call(first, "getItemHandler");
        ItemStack ejector = Round33GameTests.item("upgrade/fluid_ejector_upgrade");
        h.assertTrue(items.insertItem(4, ejector, false).isEmpty(), "the fluid ejector upgrade was rejected");
        int filled = ((IFluidPorts) first).getOutputFluidTank().fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
        h.assertTrue(filled > 0, "could not fill the first canner's output tank");
        h.runAfterDelay(80, () -> {
            IFluidPorts next = (IFluidPorts) second;
            h.assertTrue(next.getInputFluidTank().getFluidInTank(0).getAmount() > 0, "the left tank of the next canner got nothing");
            h.assertTrue(next.getOutputFluidTank().getFluidInTank(0).isEmpty(), "the right tank of the next canner was filled");
            h.succeed();
        });
    }

    // ------------------------------------------------------------------ item 5

    /** A powered loader holds its own chunk; an admin lock releases it; unlocking brings it back. */
    @GameTest(batch = "round1111_chunks", template = "empty", timeoutTicks = 160)
    public static void chunkLoaderAdminLock(GameTestHelper h) {
        BlockPos at = new BlockPos(2, 1, 2);
        h.setBlock(at, block("producer/block_chunk_loader"));
        BlockEntity loader = h.getBlockEntity(at);
        var self = new net.minecraft.world.level.ChunkPos(h.absolutePos(at));
        call(loader, "addChunkToLoaded", self);
        ((AbstractEnergyBlockEntity) loader).getEnergyStorageInternal().setEnergy(2500);
        h.runAfterDelay(30, () -> {
            h.assertTrue((Integer) call(loader, "getForcedChunkCount") == 1, "the loader does not hold its chunk");
            h.assertTrue((Integer) call(loader, "status") == 1, "not active: " + call(loader, "status"));
            call(loader, "setAdminDisabled", true);
        });
        h.runAfterDelay(60, () -> {
            h.assertTrue((Integer) call(loader, "getForcedChunkCount") == 0, "the admin lock did not release the chunk");
            h.assertTrue((Integer) call(loader, "status") == 6, "not admin_off: " + call(loader, "status"));
            call(loader, "setAdminDisabled", false);
            ((AbstractEnergyBlockEntity) loader).getEnergyStorageInternal().setEnergy(2500);
        });
        h.runAfterDelay(100, () -> {
            h.assertTrue((Integer) call(loader, "getForcedChunkCount") == 1, "the loader did not resume");
            h.succeed();
        });
    }

    // ------------------------------------------------------------------ items 7 to 9

    /**
     * The landing spot is the ground below the post; water below or no ground within 8 blocks: no landing. Sky access: without
     * it the test is closed by a barrier ceiling at relative y=4, where the posts are.
     */
    @GameTest(batch = "round1111_wingman", template = "empty", timeoutTicks = 20, skyAccess = true)
    public static void wingmanFindsItsLandingSpot(GameTestHelper h) {
        Entity wingman = BuiltInRegistries.ENTITY_TYPE.get(rl("wingman")).create(h.getLevel());
        h.assertTrue(wingman != null, "no wingman entity type");
        BlockPos floor = new BlockPos(2, 1, 2);
        h.setBlock(floor, Blocks.STONE);
        Vec3 post = Vec3.atBottomCenterOf(h.absolutePos(floor.above(4)));
        Vec3 spot = (Vec3) call(wingman, "findLanding", post);
        h.assertTrue(spot != null && Math.abs(spot.y - (h.absolutePos(floor).getY() + 1)) < 1.0E-6, "landing spot " + spot);
        BlockPos pool = new BlockPos(4, 1, 2);
        h.setBlock(pool, Blocks.WATER);
        Vec3 wet = (Vec3) call(wingman, "findLanding", Vec3.atBottomCenterOf(h.absolutePos(pool.above(3))));
        h.assertTrue(wet == null, "landed on water: " + wet);
        wingman.discard();
        h.succeed();
    }

    /** A slash advances the synced counter and keeps its kind; combo inputs come every 8 ticks. */
    @GameTest(batch = "round1111_wingman", template = "empty", timeoutTicks = 20)
    public static void wingmanSlashIsSynced(GameTestHelper h) {
        Entity wingman = BuiltInRegistries.ENTITY_TYPE.get(rl("wingman")).create(h.getLevel());
        int before = (Integer) call(wingman, "slashData");
        call(wingman, "slash", 2);
        int after = (Integer) call(wingman, "slashData");
        h.assertTrue((after >>> 2) == (before >>> 2) + 1 && (after & 3) == 2, "slash data " + before + " -> " + after);
        try {
            var field = wingman.getClass().getDeclaredField("BLADE_COMBO_TICKS");
            field.setAccessible(true);
            h.assertTrue(field.getInt(null) == 8, "combo inputs every " + field.getInt(null) + " ticks");
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("BLADE_COMBO_TICKS", e);
        }
        h.assertFalse((Boolean) call(wingman, "landed"), "a new Wingman stands on the ground");
        wingman.discard();
        h.succeed();
    }

    // ------------------------------------------------------------------ item 10 and languages

    /** The blast sound is registered and has its file; the new keys exist in all three languages. */
    @GameTest(batch = "round1111_assets", template = "empty", timeoutTicks = 20)
    public static void blastSoundAndLanguages(GameTestHelper h) {
        h.assertTrue(BuiltInRegistries.SOUND_EVENT.containsKey(rl("weapon.laser.ultra_blast")), "sound event missing");
        String sounds = Round33GameTests.resource("/assets/mio_icif/sounds.json");
        h.assertTrue(sounds != null && sounds.contains("sfx_laser_ultra_blast"), "sounds.json entry missing");
        try {
            var file = net.neoforged.fml.ModList.get().getModFileById("mio_icif").getFile();
            h.assertTrue(java.nio.file.Files.exists(file.findResource("assets/mio_icif/sounds/sfx_laser_ultra_blast.ogg")), "ogg missing");
        } catch (RuntimeException e) {
            throw new AssertionError("ogg lookup", e);
        }
        List<String> missing = new ArrayList<>();
        String[] keys = {"block.mio_icif.generator.block_creative_generator", "gui.mio_icif.creative_generator.output",
            "gui.mio_icif.chunk_loader.status.limited", "command.mio_icif.chunkloaders.header",
            "mio_icif.configuration.chunk_loaders.randomTicks", "mio_icif.subtitle.weapon.laser.ultra_blast"};
        for (String lang : new String[]{"en_us", "zh_cn", "ja_jp"}) {
            String json = Round33GameTests.resource("/assets/mio_icif/lang/" + lang + ".json");
            for (String k : keys) if (json == null || !json.contains("\"" + k + "\"")) missing.add(lang + ":" + k);
        }
        h.assertTrue(missing.isEmpty(), "missing keys: " + missing);
        h.succeed();
    }
}
