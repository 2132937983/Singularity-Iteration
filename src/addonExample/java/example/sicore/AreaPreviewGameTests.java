package example.sicore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * Area Preview: ranged machines report their work volume from server state, and the
 * Area Scanner reacts to machines but not to ordinary blocks.
 *
 * <p>Uses reflection for {@code workAreas()} because the example source set may not
 * depend on SI's content package.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class AreaPreviewGameTests {

    private static void place(GameTestHelper h, BlockPos pos, String id) {
        h.setBlock(pos, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", id)));
    }

    @SuppressWarnings("unchecked")
    private static List<Object> areas(BlockEntity entity) {
        try {
            return (List<Object>) entity.getClass().getMethod("workAreas").invoke(entity);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(entity.getClass().getSimpleName() + " has no workAreas()", e);
        }
    }

    private static AABB box(Object area) {
        try {
            return (AABB) area.getClass().getMethod("box").invoke(area);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    @GameTest(template = "reactor_loop", batch = "recovery", timeoutTicks = 40)
    public static void rangedMachinesReportTheirWorkVolume(GameTestHelper h) {
        BlockPos towerPos = new BlockPos(2, 2, 2);
        place(h, towerPos, "producer/block_laser_defense_tower");
        List<Object> tower = areas(h.getBlockEntity(towerPos));
        h.assertTrue(tower.size() == 1, "Tower reports one scan box");
        AABB scan = box(tower.get(0));
        BlockPos abs = h.absolutePos(towerPos);
        h.assertTrue(scan.getXsize() == 33 && scan.getZsize() == 33 && scan.getYsize() == 33,
            "Default ground tower scans +/-16 around its block, got " + scan);
        h.assertTrue(scan.contains(Vec3.atCenterOf(abs)), "Scan box must contain the tower");

        BlockPos minerPos = new BlockPos(5, 2, 5);
        place(h, minerPos, "producer/block_miner_elc");
        BlockEntity miner = h.getBlockEntity(minerPos);
        List<Object> mine = areas(miner);
        h.assertTrue(!mine.isEmpty(), "Miner reports its column");
        AABB column = box(mine.get(0));
        h.assertTrue(column.minY == h.getLevel().getMinBuildHeight() && column.maxY == h.absolutePos(minerPos).getY(),
            "Miner column must run from the machine down to the world bottom, got " + column);

        BlockPos dropPos = new BlockPos(2, 2, 6);
        place(h, dropPos, "generator/block_drop_generator");
        AABB drop = box(areas(h.getBlockEntity(dropPos)).get(0));
        h.assertTrue(drop.getXsize() == 7, "Drop generator collects in a 7x7x7 cube, got " + drop);
        h.succeed();
    }

    @GameTest(template = "reactor_loop", batch = "recovery", timeoutTicks = 40)
    public static void areaScannerTogglesOnMachinesOnly(GameTestHelper h) {
        BlockPos towerPos = new BlockPos(2, 2, 2);
        place(h, towerPos, "producer/block_laser_defense_tower");
        BlockPos stone = new BlockPos(4, 2, 2);
        h.setBlock(stone, Blocks.STONE);
        var player = h.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        ItemStack scanner = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "item_tool_area_scanner")));
        h.assertTrue(!scanner.isEmpty(), "Area scanner item must be registered");
        player.setItemInHand(InteractionHand.MAIN_HAND, scanner);

        BlockPos absTower = h.absolutePos(towerPos);
        InteractionResult onTower = scanner.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
            new BlockHitResult(Vec3.atCenterOf(absTower), Direction.UP, absTower, false)));
        BlockPos absStone = h.absolutePos(stone);
        InteractionResult onStone = scanner.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
            new BlockHitResult(Vec3.atCenterOf(absStone), Direction.UP, absStone, false)));
        player.discard();
        h.assertTrue(onTower.consumesAction(), "Scanner must react to a ranged machine, got " + onTower);
        h.assertTrue(onStone == InteractionResult.PASS, "Scanner must ignore plain blocks, got " + onStone);
        h.succeed();
    }
}
