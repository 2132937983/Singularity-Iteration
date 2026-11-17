package example.sicore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.33 (cable audit W4): the cable casing is one property ({@code casing}, 10 values, 1280 states per cable) and
 * the four old booleans of earlier saves are converted when a chunk or a state tag is read.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11533GameTests {
    private Round11533GameTests() {}

    private static final String TIN = "mio_icif:wiring/cable/block_tin_cable_1";
    private static Block block(String id) { return BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id)); }

    /** An old state tag: the four booleans and the other properties as before 0.1.11.33. */
    private static CompoundTag oldState(boolean foam, boolean reinforced, boolean hardened, boolean disguised) {
        var properties = new CompoundTag();
        for (String side : new String[]{"north", "south", "east", "west", "up", "down"}) properties.putString(side, side.equals("north") ? "true" : "false");
        properties.putString("waterlogged", "false");
        properties.putString("foamlogged", String.valueOf(foam));
        properties.putString("foam_reinforced", String.valueOf(reinforced));
        properties.putString("foam_hardened", String.valueOf(hardened));
        properties.putString("disguised", String.valueOf(disguised));
        var tag = new CompoundTag();
        tag.putString("Name", TIN);
        tag.put("Properties", properties);
        return tag;
    }

    private static String expected(boolean foam, boolean reinforced, boolean hardened, boolean disguised) {
        if (!foam) return reinforced ? "scaffold" : "none";
        String base = (hardened ? (reinforced ? "construction_wall" : "cf_wall") : (reinforced ? "reinforced_foam" : "foam"));
        return disguised ? "disguised_" + base : base;
    }

    /** One property of 10 values: 1280 states per plain cable (2048 before). */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void casingIsOneProperty(GameTestHelper h) {
        var tin = block(TIN);
        var casing = tin.getStateDefinition().getProperty("casing");
        h.assertTrue(casing != null && casing.getPossibleValues().size() == 10, "casing values " + casing);
        for (String old : new String[]{"foamlogged", "foam_reinforced", "foam_hardened", "disguised"})
            h.assertTrue(tin.getStateDefinition().getProperty(old) == null, "old property " + old + " still there");
        h.assertTrue(tin.getStateDefinition().getPossibleStates().size() == 1280, "states " + tin.getStateDefinition().getPossibleStates().size());
        h.succeed();
    }

    /** Every combination of the old booleans read through NbtUtils (structures and the like) maps to its casing. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void oldStateTagsConvert(GameTestHelper h) {
        var blocks = h.getLevel().holderLookup(Registries.BLOCK);
        for (int bits = 0; bits < 16; bits++) {
            boolean f = (bits & 1) != 0, r = (bits & 2) != 0, d = (bits & 4) != 0, g = (bits & 8) != 0;
            var state = NbtUtils.readBlockState(blocks, oldState(f, r, d, g));
            h.assertTrue(state.is(block(TIN)), "not a cable: " + state);
            h.assertTrue(expected(f, r, d, g).equals(Round33GameTests.casingOf(state)),
                "foam " + f + " reinforced " + r + " hardened " + d + " disguised " + g + " -> " + state);
            h.assertTrue(state.getValue(state.getBlock().getStateDefinition().getProperty("north")).equals(true), "other properties lost: " + state);
        }
        h.succeed();
    }

    /** A saved chunk with old cable states loads with their casings (ChunkSerializer.read). */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void oldChunkConverts(GameTestHelper h) {
        var level = h.getLevel();
        BlockPos at = h.absolutePos(new BlockPos(1, 2, 1));
        h.setBlock(new BlockPos(1, 2, 1), Round33GameTests.casing(block(TIN).defaultBlockState(), "construction_wall"));
        var chunk = level.getChunkAt(at);
        CompoundTag tag = ChunkSerializer.write(level, chunk);
        int rewritten = 0;
        var sections = tag.getList("sections", Tag.TAG_COMPOUND);
        for (int i = 0; i < sections.size(); i++) {
            var palette = sections.getCompound(i).getCompound("block_states").getList("palette", Tag.TAG_COMPOUND);
            for (int j = 0; j < palette.size(); j++) {
                var entry = palette.getCompound(j);
                if (!TIN.equals(entry.getString("Name"))) continue;
                var properties = entry.getCompound("Properties");
                properties.remove("casing");
                properties.putString("foamlogged", "true"); properties.putString("foam_reinforced", "true");
                properties.putString("foam_hardened", "true"); properties.putString("disguised", "false");
                rewritten++;
            }
        }
        h.assertTrue(rewritten > 0, "the cable is not in the written chunk");
        var info = new RegionStorageInfo("test", level.dimension(), "chunk");
        var loaded = ChunkSerializer.read(level, level.getPoiManager(), info, new ChunkPos(at), tag);
        var state = loaded.getBlockState(at);
        h.assertTrue(state.is(block(TIN)) && "construction_wall".equals(Round33GameTests.casingOf(state)), "loaded " + state);
        h.succeed();
    }
}
