package example.sicore;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.169: SCXSYGL's machine pack gives the water and wind generators open models (cut-out, the wind generator's rotor
 * reaching past the block), so they no longer hide their neighbours' faces: without that, the faces behind the gaps vanished and
 * the world showed through. Their blocks still collide as full blocks.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111169GameTests {
    private Round111169GameTests() {}

    @GameTest(template = "empty", batch = "round111169_pack")
    public static void openGeneratorsDoNotHideTheirNeighbours(GameTestHelper h) {
        for (String id : new String[]{"generator/block_water_generator", "generator/block_wind_generator"}) {
            BlockState state = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", id)).defaultBlockState();
            h.assertTrue(!state.canOcclude(), id + " still hides its neighbours' faces");
            h.assertTrue(state.getCollisionShape(h.getLevel(), h.absolutePos(net.minecraft.core.BlockPos.ZERO)).bounds().getSize() > 0.99,
                id + " lost its full-block collision");
        }
        h.succeed();
    }
}
