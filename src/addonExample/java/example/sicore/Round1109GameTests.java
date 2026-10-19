package example.sicore;

import com.miophas.singularity_iteration.core.api.capability.IMioIcifCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests of 0.1.10.9: the Stirling Generator takes heat through its front face (the copper plate)
 * and, as before, through its back face; no other face takes heat.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round1109GameTests {
    private Round1109GameTests() {}

    private static BlockState stirling(Direction facing) {
        BlockState state = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "generator/block_stirling_generator"))
            .defaultBlockState();
        var property = (DirectionProperty) state.getBlock().getStateDefinition().getProperty("facing");
        return state.setValue(property, facing);
    }

    @GameTest(batch = "round1109_stirling", template = "empty", timeoutTicks = 20)
    public static void stirlingTakesHeatAtFrontAndBack(GameTestHelper h) {
        BlockPos at = new BlockPos(2, 1, 2);
        h.setBlock(at, stirling(Direction.NORTH));   // front (copper plate) north, back south
        BlockPos abs = h.absolutePos(at);
        // the test structure may be rotated: compare with the facing the block really has
        Direction front = h.getBlockState(at).getValue((DirectionProperty) h.getBlockState(at).getBlock().getStateDefinition().getProperty("facing"));
        for (Direction side : Direction.values()) {
            var heat = h.getLevel().getCapability(IMioIcifCapabilities.HEAT_STORAGE_BLOCK, abs, side);
            boolean input = side == front || side == front.getOpposite();
            h.assertTrue((heat != null) == input, "heat input on " + side + " (front " + front + "): " + (heat != null));
        }
        var atFront = h.getLevel().getCapability(IMioIcifCapabilities.HEAT_STORAGE_BLOCK, abs, front);
        long before = atFront.getHeatStored();
        long taken = atFront.receiveHeat(100, false);
        h.assertTrue(taken == 100 && atFront.getHeatStored() == before + 100, "the front face did not take heat: " + taken);
        var atBack = h.getLevel().getCapability(IMioIcifCapabilities.HEAT_STORAGE_BLOCK, abs, front.getOpposite());
        h.assertTrue(atBack.receiveHeat(50, false) == 50, "the back face no longer takes heat");
        h.succeed();
    }
}
