package example.sicore;

import com.miophas.singularity_iteration.core.prefab.item.AbstractBattery;
import com.miophas.singularity_iteration.core.prefab.item.ArmorFeatures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.171: every SI wrench follows the same rules (WrenchRules). The electric wrench used to try to turn a block without a
 * facing (casings, ports, the reactor vessel), so it never took one apart, and with front dismantle off it took nothing apart.
 * Now it dismantles them as the manual wrench does; with front dismantle off only a sneaking click dismantles. On a machine with
 * a facing both wrenches turn it with a side click and take it apart with a click on its front, as before.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111171GameTests {
    private Round111171GameTests() {}

    private static final BlockPos AT = new BlockPos(2, 1, 2);

    private static Block block(String path) { return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", path)); }
    private static Item item(String path) { return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", path)); }

    private static ItemStack electricWrench() {
        ItemStack wrench = new ItemStack(item("item_tool_wrench_elc"));
        ((AbstractBattery) wrench.getItem()).setEnergy(wrench, 50_000L);
        return wrench;
    }

    private static void click(GameTestHelper h, ServerPlayer player, ItemStack wrench, Direction face, boolean sneak) {
        BlockPos abs = h.absolutePos(AT);
        player.setShiftKeyDown(sneak);
        player.setItemInHand(InteractionHand.MAIN_HAND, wrench);
        player.gameMode.useItemOn(player, h.getLevel(), wrench, InteractionHand.MAIN_HAND,
            new BlockHitResult(Vec3.atCenterOf(abs).relative(face, 0.5), face, abs, false));
        player.setShiftKeyDown(false);
    }

    @GameTest(template = "empty", batch = "round111171_wrench")
    public static void theElectricWrenchTakesACasingApart(GameTestHelper h) {
        h.setBlock(AT, block("wiring/block_tesu_casing"));
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        click(h, player, electricWrench(), Direction.UP, false);
        h.assertBlockPresent(Blocks.AIR, AT);
        h.assertItemEntityPresent(item("wiring/block_tesu_casing"), AT, 2.0);
        h.succeed();
    }

    /** Front dismantle off: a plain click leaves the casing, a sneaking click takes it apart. */
    @GameTest(template = "empty", batch = "round111171_wrench")
    public static void withoutFrontDismantleOnlySneakingDismantles(GameTestHelper h) {
        h.setBlock(AT, block("reactor/block_reactor_vessel"));
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        ItemStack wrench = electricWrench();
        ArmorFeatures.setEnabled(wrench, "front_dismantle", false);
        click(h, player, wrench, Direction.NORTH, false);
        h.assertBlockPresent(block("reactor/block_reactor_vessel"), AT);
        click(h, player, wrench, Direction.NORTH, true);
        h.assertBlockPresent(Blocks.AIR, AT);
        h.assertItemEntityPresent(item("reactor/block_reactor_vessel"), AT, 2.0);
        h.succeed();
    }

    /** On an electric furnace (it has a facing) each wrench turns it with a side click and takes it apart from the front. */
    @GameTest(template = "empty", batch = "round111171_wrench")
    public static void bothWrenchesTurnAndTakeApartAMachine(GameTestHelper h) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        for (ItemStack wrench : new ItemStack[]{new ItemStack(item("item_tool_wrench")), electricWrench()}) {
            BlockState furnace = block("producer/block_furnace_elc").defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
            h.setBlock(AT, furnace);
            click(h, player, wrench, Direction.EAST, false);
            BlockState turned = h.getBlockState(AT);
            h.assertTrue(turned.is(furnace.getBlock()) && turned.getValue(BlockStateProperties.HORIZONTAL_FACING) == Direction.EAST,
                wrench.getItem() + ": a side click left " + turned);
            click(h, player, wrench, Direction.EAST, false);
            h.assertTrue(h.getBlockState(AT).isAir(), wrench.getItem() + ": a click on the front left " + h.getBlockState(AT));
            h.assertItemEntityPresent(furnace.getBlock().asItem(), AT, 2.0);
            h.killAllEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class);
        }
        h.succeed();
    }
}
