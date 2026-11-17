package example.sicore;

import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * 0.1.11.36 (chunk loader audit C0): suitable upgrades only, a loader not placed by a player selects its own chunk, the
 * player chunk limit keeps the own chunk and the nearest ones, and an offline owner parks the loader. The rules are
 * static, so each rule test runs in its own batch that sets them before and restores the defaults after.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11536GameTests {
    private Round11536GameTests() {}

    private static final int STATUS_ACTIVE = 1, STATUS_LIMITED = 2, STATUS_OWNER_OFFLINE = 7;

    private static Block block(String path) { return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", path)); }
    private static ItemStack item(String path) { return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", path))); }
    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    private static void rules(int perPlayer, int offlineMinutes) {
        call(Round33GameTests.common("energy.ChunkLoaderPolicy"), "configure", true, 25, perPlayer, 0, offlineMinutes, 1.0, true, false);
    }
    @BeforeBatch(batch = "round11536_limit") public static void limitOn(ServerLevel level) { rules(3, -1); }
    @AfterBatch(batch = "round11536_limit") public static void limitOff(ServerLevel level) { rules(0, -1); }
    @BeforeBatch(batch = "round11536_offline") public static void offlineOn(ServerLevel level) { rules(0, 0); }
    @AfterBatch(batch = "round11536_offline") public static void offlineOff(ServerLevel level) { rules(0, -1); }

    private static BlockEntity loader(GameTestHelper h, BlockPos at) {
        h.setBlock(at, block("producer/block_chunk_loader"));
        BlockEntity loader = h.getBlockEntity(at);
        ((AbstractEnergyBlockEntity) loader).getEnergyStorageInternal().setStored(2500);
        return loader;
    }

    /** The upgrade slots take only upgrades that act on a chunk loader; an overclocker does not. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void onlySuitableUpgrades(GameTestHelper h) {
        BlockEntity loader = loader(h, new BlockPos(1, 2, 1));
        var items = (IItemHandler) call(loader, "getItemHandler");
        h.assertTrue(!items.isItemValid(1, item("upgrade/overclocker_upgrade")), "the loader takes an overclocker");
        h.assertTrue(items.isItemValid(1, item("upgrade/energy_storage_upgrade")), "the loader refuses an energy storage upgrade");
        h.succeed();
    }

    /** Placed without a player (a command, a structure): it selects its own chunk; selecting another adds it too. */
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void ownChunkWithoutAPlayer(GameTestHelper h) {
        BlockPos at = new BlockPos(1, 2, 1);
        BlockEntity loader = loader(h, at);
        h.runAfterDelay(3, () -> {
            h.assertTrue((int) call(loader, "getLoadedChunkCount") == 1, "own chunk not selected: " + call(loader, "getLoadedChunkCount"));
            ChunkPos self = new ChunkPos(h.absolutePos(at));
            call(loader, "removeChunkFromLoaded", self);
            call(loader, "toggleChunk", new ChunkPos(self.x + 1, self.z));
            h.assertTrue((int) call(loader, "getLoadedChunkCount") == 2, "the neighbour came without the own chunk");
        });
        h.runAfterDelay(8, () -> {
            h.assertTrue((int) call(loader, "status") == STATUS_ACTIVE && (int) call(loader, "getForcedChunkCount") == 2,
                "status " + call(loader, "status") + " held " + call(loader, "getForcedChunkCount"));
            h.succeed();
        });
    }

    /** Player limit 3, five chunks selected: it holds its own chunk and the two nearest (limited). */
    @GameTest(template = "empty", batch = "round11536_limit", timeoutTicks = 60)
    public static void playerLimitKeepsTheNearest(GameTestHelper h) {
        var player = h.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        BlockPos at = new BlockPos(1, 2, 1);
        BlockEntity loader = loader(h, at);
        call(loader, "setOwner", player);
        ChunkPos self = new ChunkPos(h.absolutePos(at));
        int[][] offsets = {{3, 3}, {1, 0}, {-2, 0}, {0, 1}};
        h.runAfterDelay(2, () -> { for (int[] o : offsets) call(loader, "addChunkToLoaded", new ChunkPos(self.x + o[0], self.z + o[1])); });
        h.runAfterDelay(30, () -> {
            h.assertTrue((int) call(loader, "status") == STATUS_LIMITED, "status " + call(loader, "status"));
            h.assertTrue((int) call(loader, "getForcedChunkCount") == 3, "held " + call(loader, "getForcedChunkCount"));
            h.succeed();
        });
    }

    /** An owner who is not online with no offline time allowed: the loader stops and parks. */
    @GameTest(template = "empty", batch = "round11536_offline", timeoutTicks = 60)
    public static void offlineOwnerParks(GameTestHelper h) {
        BlockPos at = new BlockPos(1, 2, 1);
        BlockEntity loader = loader(h, at);
        call(loader, "setOwner", h.makeMockPlayer(GameType.SURVIVAL));   // not on the server: offline
        h.runAfterDelay(30, () -> {
            h.assertTrue((int) call(loader, "status") == STATUS_OWNER_OFFLINE && (int) call(loader, "getForcedChunkCount") == 0,
                "status " + call(loader, "status") + " held " + call(loader, "getForcedChunkCount"));
            var parked = (java.util.List<?>) call(Round33GameTests.common("energy.ChunkLoaderPolicy"), "parked", h.getLevel().getServer());
            long here = h.absolutePos(at).asLong();
            h.assertTrue(parked.stream().anyMatch(p -> ((net.minecraft.core.GlobalPos) call(p, "at")).pos().asLong() == here), "not parked");
            h.setBlock(at, net.minecraft.world.level.block.Blocks.AIR);
            h.succeed();
        });
    }
}
