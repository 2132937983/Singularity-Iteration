package example.sicore;

import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Set;

/**
 * 0.1.11.38 (chunk loader audit C2): the map's area selection (nearest first, up to the limit; clearing keeps the own
 * chunk) and the bits of the chunks that hold a ticket, which the screen draws.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11538GameTests {
    private Round11538GameTests() {}

    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    private static int heldBits(BlockEntity loader) {
        ContainerData data = (ContainerData) call(loader, "getContainerData");
        return Integer.bitCount(data.get(12)) + Integer.bitCount(data.get(13)) + Integer.bitCount(data.get(14));
    }

    /**
     * Without energy (so it loads at most its own chunk: a test that loaded 25 chunks, 7 x 7 running, made later tests
     * nearby flaky when they stopped ticking): select all gives the 25 nearest; clear keeps the own chunk; with energy and
     * two more chunks the screen's held bits are the three chunks that hold a ticket.
     */
    @GameTest(template = "empty", batch = "round11538_area", timeoutTicks = 40)
    public static void areaSelectionAndHeldBits(GameTestHelper h) {
        BlockPos at = new BlockPos(1, 2, 1);
        h.setBlock(at, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "producer/block_chunk_loader")));
        BlockEntity loader = h.getBlockEntity(at);
        var storage = ((AbstractEnergyBlockEntity) loader).getEnergyStorageInternal();
        storage.setStored(0);
        ChunkPos self = new ChunkPos(h.absolutePos(at));
        h.runAfterDelay(2, () -> {
            int changed = (int) call(loader, "selectArea", -4, -4, 4, 4, true);
            @SuppressWarnings("unchecked") Set<Long> chosen = (Set<Long>) call(loader, "getLoadedChunks");
            h.assertTrue(chosen.size() == 25 && changed == 24, "select all: " + chosen.size() + " changed " + changed);
            // nearest first: everything within distance 2 is in, the corners are out
            h.assertTrue(chosen.contains(ChunkPos.asLong(self.x + 2, self.z)) && chosen.contains(ChunkPos.asLong(self.x + 1, self.z + 1))
                && !chosen.contains(ChunkPos.asLong(self.x + 4, self.z + 4)), "not nearest first");
            int removed = (int) call(loader, "selectArea", -4, -4, 4, 4, false);
            h.assertTrue(removed == 24 && (int) call(loader, "getLoadedChunkCount") == 1, "clear: removed " + removed);
            call(loader, "selectArea", 1, 0, 1, 1, true);   // two more: (1, 0) and (1, 1)
            storage.setStored(2500);
        });
        h.runAfterDelay(8, () -> {
            h.assertTrue(heldBits(loader) == 3 && (int) call(loader, "getForcedChunkCount") == 3, "held bits " + heldBits(loader) + " held " + call(loader, "getForcedChunkCount"));
            h.setBlock(at, net.minecraft.world.level.block.Blocks.AIR);   // release its tickets before other batches run
            h.succeed();
        });
    }
}
