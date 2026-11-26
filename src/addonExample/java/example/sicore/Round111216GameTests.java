package example.sicore;

import static example.sicore.Round33GameTests.call;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.216: SCXSYGL's grid-tie interface models, one per mode and limit. The block state has {@code mode} (export /
 * import / bidirectional) and {@code limit} (0..3 for 32 / 128 / 512 / 2,048 EU/t) and follows the interface, also when
 * it does not match (a world saved with the old flag {@code mode=true/false}, a blueprint, a command).
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111216GameTests {
    private Round111216GameTests() {}

    private static final String ID = "energy_converter/energy_converter";

    private static Block block() { return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", ID)); }

    private static BlockState facing(Direction direction) {
        BlockState state = block().defaultBlockState();
        return state.getBlock().getStateDefinition().getProperty("facing") instanceof DirectionProperty facing ? state.setValue(facing, direction) : state;
    }

    /** The value of a property as the block state file writes it, or null without that property. */
    private static String value(BlockState state, String name) {
        Property<?> property = state.getBlock().getStateDefinition().getProperty(name);
        return property == null ? null : named(state, property);
    }

    private static <T extends Comparable<T>> String named(BlockState state, Property<T> property) {
        return property.getName(state.getValue(property));
    }

    /** "mode/limit" of a state. */
    private static String look(BlockState state) { return value(state, "mode") + "/" + value(state, "limit"); }

    private static boolean exists(String path) {
        try {
            var file = net.neoforged.fml.ModList.get().getModFileById("mio_icif").getFile();
            return Files.exists(file.findResource(path.startsWith("/") ? path.substring(1) : path));
        } catch (RuntimeException e) {
            return false;
        }
    }

    @GameTest(template = "empty", batch = "round111216_grid_tie_look", timeoutTicks = 20)
    public static void theBlockStateShowsTheModeAndTheLimit(GameTestHelper h) {
        BlockPos at = new BlockPos(2, 1, 2);
        h.setBlock(at, facing(Direction.EAST));
        h.assertTrue("export/3".equals(look(h.getBlockState(at))), "placed: " + look(h.getBlockState(at)));
        BlockEntity tie = h.getBlockEntity(at);
        // limit: 2,048 -> 32 -> 128 -> 512 -> 2,048 EU/t; mode: export -> import -> bidirectional 10 / 25 / 50 % -> export
        String[][] steps = {
            {"cycleLimit", "export/0"}, {"cycleMode", "import/0"}, {"cycleLimit", "import/1"}, {"cycleMode", "bidirectional/1"},
            {"cycleMode", "bidirectional/1"}, {"cycleLimit", "bidirectional/2"}, {"cycleMode", "bidirectional/2"},
            {"cycleLimit", "bidirectional/3"}, {"cycleMode", "export/3"},
        };
        for (String[] step : steps) {
            call(tie, step[0]);
            BlockState state = h.getBlockState(at);
            h.assertTrue(step[1].equals(look(state)), step[0] + ": expected " + step[1] + ", the block shows " + look(state));
            h.assertTrue("east".equals(value(state, "facing")) && h.getBlockEntity(at) == tie, step[0] + ": facing or block entity lost");
        }
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round111216_grid_tie_look", timeoutTicks = 60)
    public static void aStateThatDoesNotMatchFollowsTheInterface(GameTestHelper h) {
        BlockPos at = new BlockPos(2, 1, 2);
        h.setBlock(at, facing(Direction.EAST));
        BlockEntity tie = h.getBlockEntity(at);
        call(tie, "cycleMode");
        call(tie, "cycleLimit");
        call(tie, "cycleLimit");   // import, 128 EU/t
        h.assertTrue("import/1".equals(look(h.getBlockState(at))), "set: " + look(h.getBlockState(at)));
        // as a world saved before 0.1.11.216 loads it (the old flag reads as the default), or a blueprint places it
        h.getLevel().setBlock(h.absolutePos(at), facing(Direction.EAST), 2);
        h.assertTrue(h.getBlockEntity(at) == tie && "export/3".equals(look(h.getBlockState(at))), "reset: " + look(h.getBlockState(at)));
        h.succeedWhen(() -> h.assertTrue("import/1".equals(look(h.getBlockState(at))), "after ticks the block shows " + look(h.getBlockState(at))));
    }

    @GameTest(template = "empty", batch = "round111216_grid_tie_look", timeoutTicks = 20)
    public static void aSavedFlagReadsAsTheDefaultLook(GameTestHelper h) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Name", "mio_icif:" + ID);
        CompoundTag properties = new CompoundTag();
        properties.putString("facing", "east");
        properties.putString("mode", "true");   // 0.1.11.215 and older: true for import and bidirectional
        tag.put("Properties", properties);
        // chunk sections read their palette with this codec
        BlockState state = BlockState.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow();
        h.assertTrue(state.is(block()) && "east".equals(value(state, "facing")) && "export/3".equals(look(state)), "read: " + state);
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round111216_grid_tie_look", timeoutTicks = 20)
    public static void everyLookHasItsModelAndTexture(GameTestHelper h) {
        String states = Round33GameTests.resource("/assets/mio_icif/blockstates/" + ID + ".json");
        h.assertTrue(states != null, "no block state file");
        JsonObject variants = JsonParser.parseString(states).getAsJsonObject().getAsJsonObject("variants");
        List<String> wrong = new ArrayList<>();
        for (BlockState state : block().getStateDefinition().getPossibleStates()) {
            String key = "facing=" + value(state, "facing") + ",limit=" + value(state, "limit") + ",mode=" + value(state, "mode");
            if (!variants.has(key)) {
                wrong.add(key + ": no variant");
                continue;
            }
            String model = variants.getAsJsonObject(key).get("model").getAsString();
            String kind = switch (value(state, "mode")) {
                case "export" -> "eu_to_fe";
                case "import" -> "fe_to_eu";
                default -> "fe_eu";
            };
            if (!model.equals("mio_icif:block/energy_converter/energy_converter_" + kind + "_" + value(state, "limit"))) wrong.add(key + ": shows " + model);
            String json = Round33GameTests.resource("/assets/mio_icif/models/" + model.substring(model.indexOf(':') + 1) + ".json");
            if (json == null) {
                wrong.add(key + ": no model " + model);
                continue;
            }
            for (var texture : JsonParser.parseString(json).getAsJsonObject().getAsJsonObject("textures").entrySet()) {
                String path = texture.getValue().getAsString();
                if (!exists("/assets/mio_icif/textures/" + path.substring(path.indexOf(':') + 1) + ".png")) wrong.add(model + ": no texture " + path);
            }
        }
        String item = Round33GameTests.resource("/assets/mio_icif/models/item/" + ID + ".json");
        String parent = item == null ? null : JsonParser.parseString(item).getAsJsonObject().get("parent").getAsString();
        if (parent == null || Round33GameTests.resource("/assets/mio_icif/models/" + parent.substring(parent.indexOf(':') + 1) + ".json") == null)
            wrong.add("item model: parent " + parent);
        h.assertTrue(variants.size() == 72 && wrong.isEmpty(), variants.size() + " variants; " + wrong);
        h.succeed();
    }
}
