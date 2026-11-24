package example.sicore;

import com.miophas.singularity_iteration.core.api.item.IBatteryItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests of 0.1.11.196: tool overhaul T3, the mining tools (drill areas by tier, attacks that cost energy, a flat tool
 * that mines like a hand, chainsaw felling).
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111196GameTests {
    private Round111196GameTests() {}

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", id));
    }

    private static ItemStack charged(String id) {
        ItemStack stack = new ItemStack(item(id));
        IBatteryItem battery = (IBatteryItem) stack.getItem();
        battery.setEnergy(stack, battery.getMaxEnergy(stack));
        return stack;
    }

    private static long energy(ItemStack stack) {
        return ((IBatteryItem) stack.getItem()).getEnergy(stack);
    }

    /** A survival player at x = 6 looking west (along -X), at the wall's height. */
    private static ServerPlayer facingWest(GameTestHelper h) {
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        BlockPos at = h.absolutePos(new BlockPos(6, 1, 2));
        p.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 90, 0);
        return p;
    }

    /** D3: the diamond drill in 3×3 mode mines the wall round the block, each block a use. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void diamondDrillMinesThreeByThree(GameTestHelper h) {
        for (int y = 1; y <= 3; y++) for (int z = 1; z <= 3; z++) h.setBlock(new BlockPos(2, y, z), Blocks.STONE);
        ServerPlayer p = facingWest(h);
        ItemStack drill = charged("item_tool_diamond_driller");
        CustomData.update(DataComponents.CUSTOM_DATA, drill, tag -> tag.putInt("DrillArea", 1));
        p.setItemInHand(InteractionHand.MAIN_HAND, drill);
        long before = energy(drill);
        p.gameMode.destroyBlock(h.absolutePos(new BlockPos(2, 2, 2)));
        for (int y = 1; y <= 3; y++) for (int z = 1; z <= 3; z++) h.assertBlockPresent(Blocks.AIR, new BlockPos(2, y, z));
        h.assertTrue(before - energy(drill) == 9 * 80, "nine blocks cost nine uses, spent " + (before - energy(drill)));
        p.discard();
        h.succeed();
    }

    /** D3: an area never takes a block much harder than the one mined (obsidian next to stone stays). */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void areaSkipsHarderBlocks(GameTestHelper h) {
        h.setBlock(new BlockPos(2, 2, 2), Blocks.STONE);
        h.setBlock(new BlockPos(2, 2, 1), Blocks.OBSIDIAN);
        h.setBlock(new BlockPos(2, 2, 3), Blocks.STONE);
        ServerPlayer p = facingWest(h);
        ItemStack drill = charged("item_tool_iridium_driller");
        CustomData.update(DataComponents.CUSTOM_DATA, drill, tag -> tag.putInt("DrillArea", 1));
        p.setItemInHand(InteractionHand.MAIN_HAND, drill);
        p.gameMode.destroyBlock(h.absolutePos(new BlockPos(2, 2, 2)));
        h.assertBlockPresent(Blocks.OBSIDIAN, new BlockPos(2, 2, 1));
        h.assertBlockPresent(Blocks.AIR, new BlockPos(2, 2, 3));
        p.discard();
        h.succeed();
    }

    /** M-04: a flat drill refused every block, even a torch; now it mines like a hand. */
    @GameTest(template = "empty")
    public static void flatDrillMinesLikeAHand(GameTestHelper h) {
        ServerPlayer p = facingWest(h);
        ItemStack drill = new ItemStack(item("item_tool_iron_driller"));
        p.setItemInHand(InteractionHand.MAIN_HAND, drill);
        BlockState torch = Blocks.TORCH.defaultBlockState();
        h.assertTrue(drill.getItem().canAttackBlock(torch, h.getLevel(), h.absolutePos(new BlockPos(1, 1, 1)), p), "a flat drill may break a torch");
        h.assertTrue(drill.getDestroySpeed(Blocks.STONE.defaultBlockState()) == 1.0F, "at hand speed");
        h.assertTrue(!drill.isCorrectToolForDrops(Blocks.STONE.defaultBlockState()), "and without drops of stone");
        h.assertTrue(!drill.canPerformAction(net.neoforged.neoforge.common.ItemAbilities.AXE_DIG), "no drill is an axe");
        p.discard();
        h.succeed();
    }

    /** D4: a drill's hit costs a use; a flat drill has no attack damage. */
    @GameTest(template = "empty")
    public static void drillHitsCostEnergy(GameTestHelper h) {
        ServerPlayer p = facingWest(h);
        ItemStack drill = charged("item_tool_iron_driller");
        ArmorStand target = new ArmorStand(EntityType.ARMOR_STAND, h.getLevel());
        long before = energy(drill);
        drill.getItem().hurtEnemy(drill, target, p);
        h.assertTrue(before - energy(drill) == 50, "a hit costs 50 EU, cost " + (before - energy(drill)));
        p.discard();
        h.succeed();
    }

    /** T3: felling mode cuts the whole tree from one log, a use per log. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void chainsawFellsTheTree(GameTestHelper h) {
        for (int y = 1; y <= 5; y++) h.setBlock(new BlockPos(2, y, 2), Blocks.OAK_LOG);
        h.setBlock(new BlockPos(3, 5, 2), Blocks.OAK_LOG);   // a branch
        h.setBlock(new BlockPos(2, 6, 2), Blocks.OAK_LEAVES);
        ServerPlayer p = facingWest(h);
        ItemStack saw = charged("item_tool_iron_chainsaw");
        CustomData.update(DataComponents.CUSTOM_DATA, saw, tag -> tag.putBoolean("ChainsawFelling", true));
        p.setItemInHand(InteractionHand.MAIN_HAND, saw);
        long before = energy(saw);
        p.gameMode.destroyBlock(h.absolutePos(new BlockPos(2, 1, 2)));
        for (int y = 1; y <= 5; y++) h.assertBlockPresent(Blocks.AIR, new BlockPos(2, y, 2));
        h.assertBlockPresent(Blocks.AIR, new BlockPos(3, 5, 2));
        h.assertBlockPresent(Blocks.OAK_LEAVES, new BlockPos(2, 6, 2));
        h.assertTrue(before - energy(saw) == 6 * 100, "six logs cost six uses, spent " + (before - energy(saw)));
        p.discard();
        h.succeed();
    }

}
