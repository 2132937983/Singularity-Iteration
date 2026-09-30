package example.sicore;

import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.api.machine.IMultiblockValidationResult;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Consumer-side contract tests: intentionally no runtime imports or mutable storage escape hatches. */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class PublicApiGameTests {
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void publicEuPortSharesFeBudgetAndExpires(GameTestHelper h) {
        var pos = new BlockPos(1, 1, 1);
        h.setBlock(pos, CoreExampleMod.SOURCE.get());
        h.runAfterDelay(5, () -> {
            InheritedSource source = h.getBlockEntity(pos);
            source.generate(128);
            var eu = source.euPort(Direction.EAST);
            var fe = h.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, h.absolutePos(pos), Direction.EAST);
            h.assertTrue(fe != null, "Missing FE port");
            h.assertTrue(eu.extract(32, true) == 32 && source.stored() == 128, "Public EU simulation changed balance");
            h.assertTrue(eu.extract(16, false) == 16 && fe.extractEnergy(64, false) == 64, "Public EU/FE budget sharing failed");
            h.assertTrue(eu.extract(1, false) == 0 && source.stored() == 96, "Public EU port renewed the budget");
            h.setBlock(pos, Blocks.AIR);
            h.assertTrue(eu.extract(1, false) == 0 && fe.extractEnergy(4, false) == 0, "Cached public port survived removal");
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void publicDiagnosticsAreReadOnlyAndThreadConfined(GameTestHelper h) {
        var pos = new BlockPos(1, 1, 1);
        h.setBlock(pos, CoreExampleMod.SOURCE.get());
        h.runAfterDelay(5, () -> {
            var api = MioIcifAPI.instance();
            h.assertTrue("1.0".equals(api.getApiVersion()), "Unexpected public API version");
            InheritedSource source = h.getBlockEntity(pos);
            source.generate(128);
            var energy = api.getEnergyNetAPI();
            var first = energy.diagnostics(h.getLevel());
            h.assertTrue(first.available(), "Diagnostics did not find the running engine");
            h.assertTrue(first.equals(energy.diagnostics(h.getLevel())) && source.stored() == 128,
                "Reading diagnostics changed counters or energy");
            boolean rejected = CompletableFuture.supplyAsync(() -> {
                try { energy.diagnostics(h.getLevel()); return false; }
                catch (IllegalStateException expected) { return true; }
            }).join();
            h.assertTrue(rejected, "Off-thread diagnostics were allowed");
            boolean colorRejected = CompletableFuture.supplyAsync(() -> {
                try { energy.conductorColorChanged(source); return false; }
                catch (IllegalStateException expected) { return true; }
            }).join();
            h.assertTrue(colorRejected, "Off-thread topology mutation was allowed");
            energy.conductorColorChanged(source);
            h.assertTrue(source.stored() == 128, "Color invalidation changed balance");
            h.succeed();
        });
    }

    private static final class TestController extends Block {
        private TestController() { super(Properties.of()); }
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void publicControllerRegistrationReachesDiscoveryRegistry(GameTestHelper h) {
        var api = MioIcifAPI.instance().getMultiblockAPI();
        api.registerController(TestController.class,
            () -> (level, pos) -> IMultiblockValidationResult.failure("Contract test"), "PublicApiTest");
        h.assertTrue(api.isControllerRegistered(TestController.class), "Controller registration was not retained");
        boolean rejected = false;
        try { api.registerController(TestController.class, null, "Invalid"); }
        catch (IllegalArgumentException expected) { rejected = true; }
        h.assertTrue(rejected && api.isControllerRegistered(TestController.class), "Invalid registration erased existing controller");
        h.succeed();
    }
}
