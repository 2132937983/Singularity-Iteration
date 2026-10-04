package example.sicore;

import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.api.block.IRubberWood;
import com.miophas.singularity_iteration.core.prefab.inventory.MachineItemHandler;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

/** Tests the registered collector through world interactions, inventories and the public rubber API. */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class ResinCollectorGameTests {
    private static final String COLLECTOR = "producer/block_resin_collector";
    private static final BlockPos POS = new BlockPos(4, 2, 4);
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }
    private static Block block(String path) { return BuiltInRegistries.BLOCK.get(id(path)); }
    private static ItemStack resin(int count) { return new ItemStack(BuiltInRegistries.ITEM.get(id("resource/item_harz")), count); }
    private static BlockState log(Direction front, boolean wet, boolean spot) {
        BlockState state = block("block_rub_wood").defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, front);
        return state.setValue((BooleanProperty) state.getBlock().getStateDefinition().getProperty("has_harz"), wet)
            .setValue((BooleanProperty) state.getBlock().getStateDefinition().getProperty("has_spot"), spot);
    }
    private static BlockEntity collector(GameTestHelper h, BlockPos pos, Direction front) {
        h.setBlock(pos, block(COLLECTOR).defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, front));
        return h.getBlockEntity(pos);
    }
    private static IItemHandler inventory(GameTestHelper h, BlockPos pos) {
        return h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(pos), Direction.DOWN);
    }
    private static boolean collect(BlockEntity entity) {
        try { return (boolean) entity.getClass().getMethod("collectResin").invoke(entity); }
        catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
    }
    private static boolean wet(GameTestHelper h, BlockPos pos) {
        BlockState state = h.getBlockState(pos);
        return state.getBlock() instanceof IRubberWood wood && wood.hasResin(state);
    }

    @GameTest(template="reactor_loop", batch="resin_collector", timeoutTicks=40)
    public static void clickingEveryResinFaceAlignsTheBackAndCollectsOne(GameTestHelper h) {
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        int i = 0;
        for (Direction front : Direction.Plane.HORIZONTAL) {
            BlockPos tree = new BlockPos(3 + (i % 2) * 5, 2, 3 + (i / 2) * 5); i++;
            h.setBlock(tree, log(front, true, true));
            BlockPos absolute = h.absolutePos(tree);
            ItemStack stack = new ItemStack(block(COLLECTOR));
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            Vec3 hitPos = Vec3.atCenterOf(absolute).add(Vec3.atLowerCornerOf(front.getNormal()).scale(0.5));
            var context = new BlockPlaceContext(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(hitPos, front, absolute, false)));
            h.assertTrue(((BlockItem) stack.getItem()).place(context).consumesAction(), "Placement failed on " + front);
            BlockPos pos = tree.relative(front);
            h.assertTrue(h.getBlockState(pos).getValue(BlockStateProperties.HORIZONTAL_FACING) == front, "Back not aligned on " + front);
            h.assertTrue(collect(h.getBlockEntity(pos)), "Could not collect from " + front);
            h.assertTrue(inventory(h, pos).getStackInSlot(0).getCount() == 1 && !wet(h, tree), "One wet spot must yield exactly one resin");
            h.assertTrue(!collect(h.getBlockEntity(pos)), "Dry spot was collected twice");
            h.assertTrue(((IRubberWood) h.getBlockState(tree).getBlock()).isTappable(h.getBlockState(tree)), "Collector destroyed the regenerating spot");
        }
        h.succeed();
    }

    @GameTest(template="reactor_loop", batch="resin_collector", timeoutTicks=40)
    public static void wrongFaceDrySpotAndOrdinaryLogsCannotBeCollected(GameTestHelper h) {
        BlockEntity entity = collector(h, POS, Direction.NORTH);
        BlockPos tree = POS.south();
        h.setBlock(tree, log(Direction.SOUTH, true, true));
        h.assertTrue(!collect(entity) && wet(h, tree), "Wrong-facing log consumed resin");
        h.setBlock(tree, log(Direction.NORTH, false, true));
        h.assertTrue(!collect(entity), "Dry log yielded resin");
        h.setBlock(tree, log(Direction.NORTH, true, false));
        h.assertTrue(!collect(entity) && wet(h, tree), "Log without a tapping spot was collected");
        h.setBlock(tree, Blocks.OAK_LOG);
        h.assertTrue(!collect(entity) && inventory(h, POS).getStackInSlot(0).isEmpty(), "Ordinary wood yielded resin");
        h.setBlock(tree, Blocks.AIR);
        h.setBlock(POS.north(), log(Direction.SOUTH, true, true));
        h.assertTrue(!collect(entity), "Collector extracted from its front instead of its back");
        h.succeed();
    }

    @GameTest(template="reactor_loop", batch="resin_collector", timeoutTicks=80)
    public static void automaticCollectionResumesAfterFullStorageAndNaturalRegrowth(GameTestHelper h) {
        collector(h, POS, Direction.EAST);
        BlockPos tree = POS.west();
        IItemHandler items = inventory(h, POS);
        h.assertTrue(items.insertItem(0, resin(64), false).isEmpty(), "Cannot fill resin storage");
        h.setBlock(tree, log(Direction.EAST, true, true));
        h.runAfterDelay(4, () -> {
            h.assertTrue(wet(h, tree) && items.getStackInSlot(0).getCount() == 64, "Full collector consumed wet spot");
            items.extractItem(0, 1, false);
            h.runAfterDelay(4, () -> {
                h.assertTrue(!wet(h, tree) && items.getStackInSlot(0).getCount() == 64, "Freed slot did not trigger collection");
                items.extractItem(0, 64, false);
                long seed = 0;
                var api = MioIcifAPI.instance().getRubberTreeAPI();
                while (RandomSource.create(seed).nextFloat() >= api.getResinRegrowChance()) seed++;
                h.assertTrue(api.tryRegrowResin(h.getLevel(), h.absolutePos(tree), h.getBlockState(tree), RandomSource.create(seed)), "Natural regrowth failed");
                h.runAfterDelay(4, () -> {
                    h.assertTrue(!wet(h, tree) && items.getStackInSlot(0).getCount() == 1, "Regrown resin was not automatically collected once");
                    h.runAfterDelay(6, () -> {
                        h.assertTrue(items.getStackInSlot(0).getCount() == 1, "Dry-state polling duplicated resin"); h.succeed();
                    });
                });
            });
        });
    }

    @GameTest(template="reactor_loop", batch="resin_collector", timeoutTicks=40)
    public static void extraSlotRejectsOtherItemsSimulatesAndSurvivesSaveLoad(GameTestHelper h) {
        BlockEntity entity = collector(h, POS, Direction.WEST);
        IItemHandler items = inventory(h, POS);
        h.assertTrue(items instanceof MachineItemHandler handler && handler.getLayout().getInternalSlotType(0) == SlotType.EXTRA,
            "Collector must use a single EXTRA slot");
        h.assertTrue(items.getSlots() == 1 && items.getSlotLimit(0) == 64, "Wrong inventory size or capacity");
        for (Direction side : Direction.values()) h.assertTrue(h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(POS), side) != null, "Automation missing on " + side);
        h.assertTrue(items.insertItem(0, new ItemStack(Items.DIRT, 3), false).getCount() == 3, "Non-resin item was accepted");
        h.assertTrue(items.insertItem(0, resin(20), true).isEmpty() && items.getStackInSlot(0).isEmpty(), "Simulation changed storage");
        items.insertItem(0, resin(20), false);
        var tag = entity.saveWithFullMetadata(h.getLevel().registryAccess());
        BlockEntity loaded = BlockEntity.loadStatic(entity.getBlockPos(), entity.getBlockState(), tag, h.getLevel().registryAccess());
        try {
            var restored = (IItemHandler) loaded.getClass().getMethod("getItemHandler").invoke(loaded);
            h.assertTrue(restored.getStackInSlot(0).getCount() == 20 && restored.getStackInSlot(0).is(resin(1).getItem()), "Save/load lost resin");
        } catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
        h.assertTrue(items.extractItem(0, 5, true).getCount() == 5 && items.getStackInSlot(0).getCount() == 20, "Simulated extraction changed storage");
        h.assertTrue(items.extractItem(0, 5, false).getCount() == 5 && items.getStackInSlot(0).getCount() == 15, "Real extraction lost or duplicated resin");
        h.succeed();
    }

    @GameTest(template="reactor_loop", batch="resin_collector", timeoutTicks=40)
    public static void menuShiftClickFiltersResinAndClosesWhenBlockIsRemoved(GameTestHelper h) {
        BlockEntity entity = collector(h, POS, Direction.NORTH);
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(Vec3.atCenterOf(h.absolutePos(POS)));
        AbstractContainerMenu menu = ((MenuProvider) entity).createMenu(1, player.getInventory(), player);
        h.assertTrue(menu.stillValid(player) && menu.slots.size() == 37, "Menu inventory or validity incorrect");
        player.getInventory().setItem(9, new ItemStack(Items.DIRT, 3));
        h.assertTrue(menu.quickMoveStack(player, 1).isEmpty() && inventory(h, POS).getStackInSlot(0).isEmpty(), "Shift-click accepted dirt");
        player.getInventory().setItem(9, resin(12));
        menu.quickMoveStack(player, 1);
        h.assertTrue(inventory(h, POS).getStackInSlot(0).getCount() == 12 && player.getInventory().getItem(9).isEmpty(), "Shift-click into collector incorrect");
        menu.quickMoveStack(player, 0);
        h.assertTrue(inventory(h, POS).getStackInSlot(0).isEmpty() && player.getInventory().countItem(resin(1).getItem()) == 12, "Shift-click out lost resin");
        h.setBlock(POS, Blocks.AIR);
        h.assertTrue(!menu.stillValid(player), "Removed collector left menu valid");
        h.succeed();
    }

    @GameTest(template="reactor_loop", batch="resin_collector", timeoutTicks=40)
    public static void axeTagRecipeAndRemovalDropsAreRegistered(GameTestHelper h) {
        BlockEntity entity = collector(h, POS, Direction.NORTH);
        inventory(h, POS).insertItem(0, resin(9), false);
        h.assertTrue(h.getBlockState(POS).is(BlockTags.MINEABLE_WITH_AXE), "Collector lacks the composter axe tag");
        var recipe = (CraftingRecipe) h.getLevel().getRecipeManager().byKey(id("resin_collector")).orElseThrow().value();
        var input = new java.util.ArrayList<ItemStack>();
        for (int i = 0; i < 9; i++) input.add(new ItemStack(Items.OAK_SLAB));
        input.set(1, new ItemStack(BuiltInRegistries.ITEM.get(id("item_tool_wooden_treetap"))));
        input.set(4, ItemStack.EMPTY);
        h.assertTrue(recipe.matches(CraftingInput.of(3, 3, input), h.getLevel()), "Seven slabs plus wooden tap recipe missing");
        input.set(0, new ItemStack(Items.COMPOSTER));
        h.assertTrue(!recipe.matches(CraftingInput.of(3, 3, input), h.getLevel()), "Recipe accepts composter instead of slab ingredients");
        h.getLevel().destroyBlock(h.absolutePos(POS), true);
        var drops = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(POS)).inflate(2));
        int resinCount = drops.stream().filter(e -> e.getItem().is(resin(1).getItem())).mapToInt(e -> e.getItem().getCount()).sum();
        int blockCount = drops.stream().filter(e -> e.getItem().is(block(COLLECTOR).asItem())).mapToInt(e -> e.getItem().getCount()).sum();
        h.assertTrue(resinCount == 9 && blockCount == 1 && h.getLevel().getBlockEntity(h.absolutePos(POS)) == null, "Removal must drop contents and one collector exactly once");
        h.succeed();
    }
}
