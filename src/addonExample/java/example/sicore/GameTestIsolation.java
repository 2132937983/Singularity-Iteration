package example.sicore;

import java.lang.reflect.Field;
import net.minecraft.gametest.framework.GameTestBatch;
import net.minecraft.gametest.framework.GameTestBatchListener;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.gametest.framework.GameTestTicker;
import net.minecraft.gametest.framework.StructureUtils;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 0.1.11.52: every batch of the GameTest server starts on a clean world.
 *
 * <p>The vanilla GameTest server ({@code StructureGridSpawner(pos, 8, false)}) never clears the structures of a finished
 * batch: the next batch is laid out further along the same grid, and its forced chunks (one chunk before and three
 * after each new structure) load the leftovers of earlier batches again. Their machines, generators and cables then
 * tick next to the running test in the one energy grid of the dimension and in the global counters
 * ({@code PerfCounters}, {@code optimizationCounters}). Which leftovers come back depends only on the batch order, and
 * that is the iteration order of a {@code HashMap} of batch names: adding a few batches anywhere reorders all of them.
 * Energy tests then failed or passed with the number of tests, never with the code.
 *
 * <p>This listener clears the space of each structure of a batch when the batch has finished (as
 * {@code StructureGridSpawner} does with {@code clearOnBatch}), before the runner unforces the chunks and starts the
 * next batch. Tests that run together in one batch still share the world.
 */
final class GameTestIsolation {
    private GameTestIsolation() {}

    private static boolean hooked;

    static void register() {
        NeoForge.EVENT_BUS.addListener(GameTestIsolation::onTick);
    }

    private static void onTick(ServerTickEvent.Pre event) {
        if (hooked || !(event.getServer() instanceof GameTestServer)) return;
        GameTestRunner runner = runner();
        if (runner == null) return;   // the server starts its runner at the end of its first tick
        hooked = true;
        runner.addListener(new GameTestBatchListener() {
            @Override public void testBatchStarting(GameTestBatch batch) {}

            @Override public void testBatchFinished(GameTestBatch batch) {
                for (GameTestInfo info : batch.gameTestInfos()) {
                    try {
                        var structure = info.getStructureBlockEntity();
                        StructureUtils.clearSpaceForStructure(StructureUtils.getStructureBoundingBox(structure), info.getLevel());
                    } catch (IllegalStateException notSpawned) {
                        // a structure that was never placed leaves nothing to clear
                    }
                }
            }
        });
    }

    private static GameTestRunner runner() {
        try {
            Field field = GameTestTicker.class.getDeclaredField("runner");
            field.setAccessible(true);
            return (GameTestRunner) field.get(GameTestTicker.SINGLETON);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("GameTestTicker.runner not found", e);
        }
    }
}
