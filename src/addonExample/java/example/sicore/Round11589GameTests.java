package example.sicore;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.89: the fusion reactor (tokamak) blocks. Each one is registered with its item, its name in three languages, its
 * blockstate, block and item models and the textures they use; the controller and the ports face a direction and turn with a
 * rotated structure. (Loot tables and mining tags are checked for every block by DataIntegrityGameTests.)
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11589GameTests {
    private Round11589GameTests() {}

    static final String[] BLOCKS = {"controller", "central_solenoid", "toroidal_coil", "poloidal_coil", "vacuum_vessel", "blanket", "divertor", "cryostat",
        "nbi_port", "ecrh_port", "fuel_port", "exhaust_port", "cryo_port", "power_port", "mitigation_port"};
    static final List<String> FACING = List.of("controller", "toroidal_coil", "nbi_port", "ecrh_port", "fuel_port", "exhaust_port", "cryo_port", "power_port", "mitigation_port");

    /** A file of the mod (binary files too: Round33GameTests.resource reads text). */
    private static boolean exists(String path) {
        try {
            var file = net.neoforged.fml.ModList.get().getModFileById("mio_icif").getFile();
            return java.nio.file.Files.exists(file.findResource(path));
        } catch (RuntimeException e) {
            return false;
        }
    }

    @GameTest(template = "empty", batch = "round11589_fusion_blocks", timeoutTicks = 20)
    public static void fusionBlocksHaveTheirResources(GameTestHelper h) {
        List<String> wrong = new ArrayList<>();
        String[] langs = {"en_us", "zh_cn", "ja_jp"};
        String[] lang = new String[langs.length];
        for (int i = 0; i < langs.length; i++) lang[i] = Round33GameTests.resource("/assets/mio_icif/lang/" + langs[i] + ".json");
        Pattern texture = Pattern.compile("\"mio_icif:(block/fusion/[a-z_]+)\"");
        for (String name : BLOCKS) {
            String path = "fusion/block_fusion_" + name;
            var id = ResourceLocation.fromNamespaceAndPath("mio_icif", path);
            var block = BuiltInRegistries.BLOCK.get(id);
            if (block == Blocks.AIR) { wrong.add(path + ": not registered"); continue; }
            if (!(BuiltInRegistries.ITEM.get(id) instanceof BlockItem item) || item.getBlock() != block) wrong.add(path + ": no block item");
            String key = "\"block.mio_icif." + path.replace('/', '.') + "\"";
            for (int i = 0; i < langs.length; i++) if (lang[i] == null || !lang[i].contains(key)) wrong.add(path + ": no name in " + langs[i]);
            if (Round33GameTests.resource("/assets/mio_icif/blockstates/" + path + ".json") == null) wrong.add(path + ": no blockstate");
            if (Round33GameTests.resource("/assets/mio_icif/models/item/" + path + ".json") == null) wrong.add(path + ": no item model");
            String model = Round33GameTests.resource("/assets/mio_icif/models/block/" + path + ".json");
            if (model == null) { wrong.add(path + ": no block model"); continue; }
            Matcher m = texture.matcher(model);
            int textures = 0;
            while (m.find()) {
                textures++;
                if (!exists("assets/mio_icif/textures/" + m.group(1) + ".png")) wrong.add(path + ": missing texture " + m.group(1));
            }
            if (textures == 0) wrong.add(path + ": the model names no fusion texture");
            BlockState state = block.defaultBlockState();
            boolean faces = state.hasProperty(BlockStateProperties.HORIZONTAL_FACING);
            if (faces != FACING.contains(name)) wrong.add(path + (faces ? ": faces a direction" : ": has no facing"));
            if (faces && state.rotate(Rotation.CLOCKWISE_90).getValue(BlockStateProperties.HORIZONTAL_FACING)
                    != state.getValue(BlockStateProperties.HORIZONTAL_FACING).getClockWise()) wrong.add(path + ": does not turn with the structure");
            if (!state.requiresCorrectToolForDrops()) wrong.add(path + ": drops without a pickaxe");
        }
        h.assertTrue(wrong.isEmpty(), "fusion blocks: " + wrong);
        h.assertTrue(Direction.NORTH.getClockWise() == Direction.EAST, "direction sanity");
        h.succeed();
    }
}
