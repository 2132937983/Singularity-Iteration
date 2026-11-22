package example.sicore;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.156: a survival player building into a solar panel's cells keeps the block. The right click is refused before
 * anything is placed (on the client as on the server, so the client neither shows the block nor takes it); placements by
 * other paths are undone and the stack stays whole. A cable under a wing is placed and used up as usual.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111156GameTests {
    private Round111156GameTests() {}

    private static final String METS = "generator/block_mets_advanced_solar_generator", CABLE = "wiring/cable/block_glass_cable";

    private static Block block(String path) { return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", path)); }

    private static BlockHitResult topOf(GameTestHelper h, BlockPos below) {
        BlockPos abs = h.absolutePos(below);
        return new BlockHitResult(Vec3.atCenterOf(abs).add(0, 0.5, 0), Direction.UP, abs, false);
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void survivalPlayerKeepsBlocksRefusedByAPanel(GameTestHelper h) {
        BlockPos at = new BlockPos(3, 2, 3);
        for (int x = 1; x <= 5; x++) for (int z = 1; z <= 5; z++) h.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
        h.setBlock(at, block(METS).defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        Vec3 aside = Vec3.atCenterOf(h.absolutePos(new BlockPos(7, 2, 7)));
        player.moveTo(aside.x, aside.y, aside.z);
        List<String> wrong = new ArrayList<>();

        // a right click (the way a player places): stone under the wing is refused and stays in the hand
        ItemStack stone = new ItemStack(Items.STONE, 4);
        player.setItemInHand(InteractionHand.MAIN_HAND, stone);
        player.gameMode.useItemOn(player, h.getLevel(), stone, InteractionHand.MAIN_HAND, topOf(h, at.east().below()));
        if (!h.getBlockState(at.east()).isAir()) wrong.add("stone went under the wing by a right click");
        if (stone.getCount() != 4) wrong.add("the right click took the stone: " + stone.getCount() + " left of 4");

        // another path (the placement event undoes it): the stack stays whole too
        stone.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, topOf(h, at.west().below())));
        if (!h.getBlockState(at.west()).isAir()) wrong.add("stone went under the west wing");
        if (stone.getCount() != 4) wrong.add("the undone placement took the stone: " + stone.getCount() + " left of 4");

        // a cable under the wing is placed and used up
        ItemStack cable = new ItemStack(block(CABLE).asItem(), 4);
        player.setItemInHand(InteractionHand.MAIN_HAND, cable);
        player.gameMode.useItemOn(player, h.getLevel(), cable, InteractionHand.MAIN_HAND, topOf(h, at.east().below()));
        if (h.getBlockState(at.east()).getBlock() != block(CABLE)) wrong.add("a cable did not go under the wing");
        if (cable.getCount() != 3) wrong.add("placing a cable left " + cable.getCount() + " of 4");

        // stone in a free cell (in front) is placed and used up
        player.setItemInHand(InteractionHand.MAIN_HAND, stone);
        player.gameMode.useItemOn(player, h.getLevel(), stone, InteractionHand.MAIN_HAND, topOf(h, at.north().below()));
        if (h.getBlockState(at.north()).getBlock() != Blocks.STONE) wrong.add("stone did not go in front (a free cell)");
        if (stone.getCount() != 3) wrong.add("placing stone in a free cell left " + stone.getCount() + " of 4");

        h.assertTrue(wrong.isEmpty(), "survival building around a panel: " + wrong);
        h.succeed();
    }
}
