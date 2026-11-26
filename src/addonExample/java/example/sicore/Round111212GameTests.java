package example.sicore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests of 0.1.11.212 (art batch MT1): the molecular transformer's four-corner chamber model. The block's outline is
 * the three layers of the model (not a full cube) and it no longer hides its neighbours' faces; the models and textures it
 * names ship in the mod file and the old 96x16 strip is gone.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111212GameTests {
    private Round111212GameTests() {}

    private static final String ID = "producer/block_molecular_transformer";

    private static Block block() {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", ID));
    }

    /** Whether the SI mod file has {@code path} (Round33GameTests.resource reads text, so it cannot see a PNG). */
    private static boolean ships(String path) {
        try {
            var file = net.neoforged.fml.ModList.get().getModFileById("mio_icif").getFile();
            return java.nio.file.Files.exists(file.findResource(path.substring(1)));
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static boolean solidAt(VoxelShape shape, double x, double y, double z) {
        double e = 0.25 / 16;
        return Shapes.joinIsNotEmpty(shape, Shapes.box(x / 16 - e, y / 16 - e, z / 16 - e, x / 16 + e, y / 16 + e, z / 16 + e), BooleanOp.AND);
    }

    /** Outline and collision follow the model: two full layers and the 8x8 chamber between them, the same for every facing. */
    @GameTest(template = "empty", batch = "round111212_mt1", timeoutTicks = 20)
    public static void shapeFollowsTheChamberModel(GameTestHelper h) {
        BlockPos pos = new BlockPos(1, 2, 1);
        h.setBlock(pos, block());
        BlockPos abs = h.absolutePos(pos);
        for (var facing : new net.minecraft.core.Direction[]{net.minecraft.core.Direction.NORTH, net.minecraft.core.Direction.EAST,
                net.minecraft.core.Direction.SOUTH, net.minecraft.core.Direction.WEST}) {
            BlockState state = block().defaultBlockState().setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, facing);
            h.getLevel().setBlock(abs, state, 3);
            VoxelShape outline = state.getShape(h.getLevel(), abs);
            VoxelShape collision = state.getCollisionShape(h.getLevel(), abs);
            for (VoxelShape shape : new VoxelShape[]{outline, collision}) {
                h.assertTrue(solidAt(shape, 1, 1, 1), "lower layer is solid (" + facing + ")");
                h.assertTrue(solidAt(shape, 15, 15, 15), "upper layer is solid (" + facing + ")");
                h.assertTrue(solidAt(shape, 8, 8, 8), "the chamber is solid (" + facing + ")");
                h.assertTrue(!solidAt(shape, 2, 8, 2), "beside the chamber is open (" + facing + ")");
                h.assertTrue(!solidAt(shape, 14, 8, 8), "beside the chamber is open (" + facing + ")");
            }
            h.assertTrue(!Shapes.block().equals(outline) && !Block.isShapeFullBlock(outline), "not a full cube");
            h.assertTrue(state.getOcclusionShape(h.getLevel(), abs).isEmpty(), "does not hide neighbour faces");
        }
        h.succeed();
    }

    /** The models name only textures that ship; the item inherits the idle block; the old strip is gone. */
    @GameTest(template = "empty", batch = "round111212_mt1", timeoutTicks = 20)
    public static void modelsAndTexturesShip(GameTestHelper h) {
        String models = "/assets/mio_icif/models/block/producer/";
        for (String model : new String[]{"block_molecular_transformer.json", "block_molecular_transformer_on.json"}) {
            String json = Round33GameTests.resource(models + model);
            h.assertTrue(json != null, model + " ships");
            h.assertTrue(json.contains("neoforge:composite"), model + " is a composite model (solid, cutout, glass)");
            var m = java.util.regex.Pattern.compile("mio_icif:block/producer/molecular_transformer/([a-z_]+)").matcher(json);
            int found = 0;
            while (m.find()) {
                found++;
                h.assertTrue(ships("/assets/mio_icif/textures/block/producer/molecular_transformer/" + m.group(1) + ".png"),
                    model + " texture " + m.group(1) + " ships");
            }
            h.assertTrue(found >= 6, model + " names its part textures");
        }
        String item = Round33GameTests.resource("/assets/mio_icif/models/item/producer/block_molecular_transformer.json");
        h.assertTrue(item != null && item.contains("\"mio_icif:block/producer/block_molecular_transformer\""), "the item shows the idle block");
        h.assertTrue(!ships("/assets/mio_icif/textures/block/producer/block_molecular_transformer.png"),
            "the old 96x16 strip is removed");
        h.succeed();
    }
}
