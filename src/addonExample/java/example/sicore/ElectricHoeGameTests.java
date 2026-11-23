package example.sicore;

import com.miophas.singularity_iteration.core.api.item.IBatteryItem;
import com.miophas.singularity_iteration.core.api.tool.IToolModeProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.182: the electric hoe's click decides by the clicked block whether it tills. Seeding a field from its farmland
 * (seeds in the off hand, 5x5 range) plants the farmland only and leaves the grass round it as it was (it used to till
 * that grass and plant it too); clicking grass still tills the range and plants it.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class ElectricHoeGameTests {
    private ElectricHoeGameTests() {}

    /** The middle of the soil layer; the 5x5 range covers x and z 4..8 round it. */
    private static final BlockPos MID = new BlockPos(6, 2, 6);
    private static final int RANGE_5X5 = 2;

    private static Player farmer(GameTestHelper h, int seeds) {
        ItemStack hoe = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "item_tool_iron_hoe")));
        ((IBatteryItem) hoe.getItem()).setEnergy(hoe, 10_000);
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        ((IToolModeProvider) hoe.getItem()).selectToolMode(hoe, player, RANGE_5X5);
        player.setItemInHand(InteractionHand.MAIN_HAND, hoe);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.WHEAT_SEEDS, seeds));
        return player;
    }

    /** Right-clicks the top of {@code rel} with the hoe in the main hand. */
    private static InteractionResult click(GameTestHelper h, Player player, BlockPos rel) {
        BlockPos at = h.absolutePos(rel);
        UseOnContext context = new UseOnContext(player, InteractionHand.MAIN_HAND,
            new BlockHitResult(Vec3.atCenterOf(at).add(0, 0.5, 0), Direction.UP, at, false));
        return player.getMainHandItem().useOn(context);
    }

    private static boolean ring(int x, int z) { return x == 4 || x == 8 || z == 4 || z == 8; }

    @GameTest(template = "reactor_loop", batch = "hoe182", timeoutTicks = 20)
    public static void plantingFromFarmlandLeavesTheGrassAlone(GameTestHelper h) {
        // a 3x3 field of farmland in a ring of grass, the ring inside the 5x5 range
        for (int x = 4; x <= 8; x++)
            for (int z = 4; z <= 8; z++)
                h.setBlock(new BlockPos(x, 2, z), ring(x, z) ? Blocks.GRASS_BLOCK.defaultBlockState()
                    : Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7));
        Player player = farmer(h, 64);
        h.assertTrue(click(h, player, MID).consumesAction(), "the click on the farmland did nothing");
        for (int x = 4; x <= 8; x++)
            for (int z = 4; z <= 8; z++) {
                BlockPos soil = new BlockPos(x, 2, z);
                if (ring(x, z)) {
                    h.assertBlockPresent(Blocks.GRASS_BLOCK, soil);   // not tilled
                    h.assertBlockPresent(Blocks.AIR, soil.above());   // not planted
                } else {
                    h.assertBlockPresent(Blocks.FARMLAND, soil);
                    h.assertBlockPresent(Blocks.WHEAT, soil.above());
                }
            }
        int used = 64 - player.getOffhandItem().getCount();
        h.assertTrue(used == 9, "seeds used " + used + ", 9 expected (the field only)");
        h.succeed();
    }

    @GameTest(template = "reactor_loop", batch = "hoe182", timeoutTicks = 20)
    public static void tillingFromGrassStillTillsAndPlants(GameTestHelper h) {
        for (int x = 4; x <= 8; x++)
            for (int z = 4; z <= 8; z++)
                h.setBlock(new BlockPos(x, 2, z), Blocks.GRASS_BLOCK);
        Player player = farmer(h, 64);
        h.assertTrue(click(h, player, MID).consumesAction(), "the click on the grass did nothing");
        for (int x = 4; x <= 8; x++)
            for (int z = 4; z <= 8; z++) {
                h.assertBlockPresent(Blocks.FARMLAND, new BlockPos(x, 2, z));
                h.assertBlockPresent(Blocks.WHEAT, new BlockPos(x, 3, z));
            }
        int used = 64 - player.getOffhandItem().getCount();
        h.assertTrue(used == 25, "seeds used " + used + ", 25 expected");
        h.succeed();
    }
}
