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
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.37 (chunk loader audit C1, decision D1): out of energy a loader keeps only its own chunk for the grace and takes
 * all its chunks back when energy returns; at the end of the grace it stops; with a grace of 0 it stops at once.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11537GameTests {
    private Round11537GameTests() {}

    private static final int STATUS_ACTIVE = 1, STATUS_NO_ENERGY = 3, STATUS_GRACE = 10;

    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }
    private static Class<?> policy() { return Round33GameTests.common("energy.ChunkLoaderPolicy"); }
    private static int held(BlockEntity loader) { return ((java.util.Set<?>) ArmoryServiceGameTests.field(loader, "forcedChunks")).size(); }
    private static long energy(BlockEntity loader) { return ((AbstractEnergyBlockEntity) loader).getEnergyStorageInternal().getAmount(); }
    private static void setEnergy(BlockEntity loader, long eu) { ((AbstractEnergyBlockEntity) loader).getEnergyStorageInternal().setStored(eu); }

    @BeforeBatch(batch = "round11537_none") public static void noGrace(ServerLevel level) { call(policy(), "setGraceMinutes", 0); }
    @AfterBatch(batch = "round11537_none") public static void graceBack(ServerLevel level) { call(policy(), "setGraceMinutes", 10); }

    private static BlockEntity loaderWithThreeChunks(GameTestHelper h, BlockPos at) {
        h.setBlock(at, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "producer/block_chunk_loader")));
        BlockEntity loader = h.getBlockEntity(at);
        setEnergy(loader, 2500);
        ChunkPos self = new ChunkPos(h.absolutePos(at));
        call(loader, "addChunkToLoaded", self);
        call(loader, "addChunkToLoaded", new ChunkPos(self.x + 1, self.z));
        call(loader, "addChunkToLoaded", new ChunkPos(self.x, self.z + 1));
        return loader;
    }

    /** Out of energy: own chunk only; energy back: all three; the grace over: stopped, nothing held. */
    @GameTest(template = "empty", batch = "round11537_grace", timeoutTicks = 90)
    public static void graceKeepsTheOwnChunk(GameTestHelper h) {
        BlockEntity loader = loaderWithThreeChunks(h, new BlockPos(1, 2, 1));
        h.runAfterDelay(5, () -> {
            h.assertTrue((int) call(loader, "status") == STATUS_ACTIVE && held(loader) == 3, "running with three: " + call(loader, "status"));
            setEnergy(loader, 0);
        });
        h.runAfterDelay(10, () -> {
            h.assertTrue((int) call(loader, "status") == STATUS_GRACE && held(loader) == 1 && (boolean) call(loader, "inGrace"),
                "out of energy: status " + call(loader, "status") + " held " + held(loader));
            setEnergy(loader, 2500);
        });
        h.runAfterDelay(15, () -> {
            h.assertTrue((int) call(loader, "status") == STATUS_ACTIVE && held(loader) == 3 && !(boolean) call(loader, "inGrace"),
                "energy back: status " + call(loader, "status") + " held " + held(loader));
            setEnergy(loader, 0);
        });
        h.runAfterDelay(20, () -> {
            h.assertTrue((boolean) call(loader, "inGrace"), "second outage not in grace");
            // the grace is over: move its start back past the 10 minutes
            try {
                var field = loader.getClass().getDeclaredField("noEnergySince");
                field.setAccessible(true);
                field.setLong(loader, h.getLevel().getGameTime() - 10 * 1200L - 1);
            } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
        });
        h.runAfterDelay(30, () -> {
            h.assertTrue((int) call(loader, "status") == STATUS_NO_ENERGY && held(loader) == 0 && !(boolean) call(loader, "inGrace"),
                "after the grace: status " + call(loader, "status") + " held " + held(loader) + " since " + ArmoryServiceGameTests.field(loader, "noEnergySince")
                + " now " + h.getLevel().getGameTime() + " grace " + call(policy(), "graceMinutes"));
            setEnergy(loader, 2500);   // energy back after the stop: it runs again (its chunk is still loaded here), a new grace is allowed
        });
        h.runAfterDelay(36, () -> {
            h.assertTrue((int) call(loader, "status") == STATUS_ACTIVE && held(loader) == 3 && !(boolean) ArmoryServiceGameTests.field(loader, "graceSpent"),
                "after a stop with energy back: status " + call(loader, "status") + " held " + held(loader));
            h.succeed();
        });
    }

    /** A grace of 0 minutes: the loader stops at once, as before 0.1.11.37. */
    @GameTest(template = "empty", batch = "round11537_none", timeoutTicks = 40)
    public static void noGraceStopsAtOnce(GameTestHelper h) {
        BlockEntity loader = loaderWithThreeChunks(h, new BlockPos(1, 2, 1));
        h.runAfterDelay(5, () -> setEnergy(loader, 0));
        h.runAfterDelay(10, () -> {
            h.assertTrue((int) call(loader, "status") == STATUS_NO_ENERGY && held(loader) == 0, "status " + call(loader, "status") + " held " + held(loader));
            h.succeed();
        });
    }
}
