package example.sicore;

import com.miophas.singularity_iteration.core.prefab.item.AbstractBattery;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.172: mio_icif:wrenchable is the one tag the wrenches read (c:machine no longer decides, c:wrench_can_damaged is gone).
 * 0.1.11.173: a sneaking click with an SI wrench turns the block away again (the front to the back); other mods' wrenches still
 * dismantle with it. A storage box comes apart with any click.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111172GameTests {
    private Round111172GameTests() {}

    private static final BlockPos AT = new BlockPos(2, 1, 2);
    private static final TagKey<Block> WRENCHABLE = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("mio_icif", "wrenchable"));

    private static Block block(String path) { return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", path)); }
    private static Item item(String path) { return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", path)); }

    private static ItemStack electricWrench() {
        ItemStack wrench = new ItemStack(item("item_tool_wrench_elc"));
        ((AbstractBattery) wrench.getItem()).setEnergy(wrench, 50_000L);
        return wrench;
    }

    private static void sneakClick(GameTestHelper h, ItemStack wrench, Direction face) {
        click(h, wrench, face, true);
    }

    private static void click(GameTestHelper h, ItemStack wrench, Direction face, boolean sneak) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        BlockPos abs = h.absolutePos(AT);
        player.setShiftKeyDown(sneak);
        player.setItemInHand(InteractionHand.MAIN_HAND, wrench);
        player.gameMode.useItemOn(player, h.getLevel(), wrench, InteractionHand.MAIN_HAND,
            new BlockHitResult(Vec3.atCenterOf(abs).relative(face, 0.5), face, abs, false));
        player.setShiftKeyDown(false);
    }

    /** Every SI machine and cable is in mio_icif:wrenchable (a machine added later must be put there too). */
    @GameTest(template = "empty", batch = "round111172_wrench")
    public static void everyMachineAndCableIsWrenchable(GameTestHelper h) {
        TagKey<Block> machine = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "machine"));
        TagKey<Block> cable = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "cable"));
        List<String> missing = new ArrayList<>();
        for (Block b : BuiltInRegistries.BLOCK) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(b);
            if (!id.getNamespace().equals("mio_icif")) continue;
            BlockState state = b.defaultBlockState();
            if ((state.is(machine) || state.is(cable)) && !state.is(WRENCHABLE)) missing.add(id.getPath());
        }
        h.assertTrue(missing.isEmpty(), "machines or cables a wrench cannot take apart: " + missing);
        h.assertTrue(BuiltInRegistries.BLOCK.getTag(TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "wrench_can_damaged"))).isEmpty(),
            "c:wrench_can_damaged still exists");
        h.succeed();
    }

    /**
     * 0.1.11.173: with either SI wrench a sneaking click on an oil rig module's front turns the front to the back, and a click on
     * the (new) front takes it apart.
     */
    @GameTest(template = "empty", batch = "round111172_wrench")
    public static void siWrenchesTurnThenTakeApartOilRigModules(GameTestHelper h) {
        String[] modules = {"oilrig/block_oil_rig_input", "oilrig/block_oil_rig_output"};
        ItemStack[] wrenches = {new ItemStack(item("item_tool_wrench")), electricWrench()};
        for (int i = 0; i < 2; i++) {
            h.setBlock(AT, block(modules[i]).defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING, Direction.NORTH));
            click(h, wrenches[i], Direction.NORTH, true);
            BlockState turned = h.getBlockState(AT);
            h.assertTrue(turned.is(block(modules[i])) && turned.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING) == Direction.SOUTH,
                wrenches[i].getItem() + ": a sneaking click on the front left " + turned);
            click(h, wrenches[i], Direction.SOUTH, false);
            h.assertTrue(h.getBlockState(AT).isAir(), wrenches[i].getItem() + ": a click on the front left " + h.getBlockState(AT));
            h.assertItemEntityPresent(item(modules[i]), AT, 2.0);
            h.killAllEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class);
        }
        h.succeed();
    }

    /** A storage box (it was only in c:wrench_can_damaged, which the machine path never read) comes apart too. */
    @GameTest(template = "empty", batch = "round111172_wrench")
    public static void aSneakingWrenchTakesAStorageBoxApart(GameTestHelper h) {
        h.setBlock(AT, block("build/block_wood_storage"));
        sneakClick(h, new ItemStack(item("item_tool_wrench")), Direction.UP);
        h.assertBlockPresent(Blocks.AIR, AT);
        h.assertItemEntityPresent(item("build/block_wood_storage"), AT, 2.0);
        h.succeed();
    }
}
