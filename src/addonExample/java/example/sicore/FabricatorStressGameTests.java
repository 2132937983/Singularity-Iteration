package example.sicore;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.slf4j.Logger;

/**
 * 0.1.10.15 stress tests of the IC2-style fabricator intake (no per-tick rate, only the buffer room):
 * a Mass Fabricator with four Transformer Upgrades fed 1,000,000 EU/t by the energy net, and a Large
 * Fabricator with four IV input modules kept full. Both must make exactly 1 mB per tick, keep their
 * EU within the buffers (no overflow), and stay cheap per tick. The measured times are logged.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class FabricatorStressGameTests {
    private FabricatorStressGameTests() {}

    private static final Logger LOG = LogUtils.getLogger();

    private static ResourceLocation si(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }
    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }
    private static Block block(String path) { return BuiltInRegistries.BLOCK.get(si(path)); }
    private static Object storage(BlockEntity be) { return call(be, "getEnergyStorageInternal"); }
    private static long amount(BlockEntity be) { return (Long) call(storage(be), "getAmount"); }

    /** Wall time between server ticks while a test runs: count, sum and worst, in nanoseconds. */
    private static final class TickClock {
        long last, count, sum, worst;
        void tick() {
            long now = System.nanoTime();
            if (last != 0) { long d = now - last; count++; sum += d; worst = Math.max(worst, d); }
            last = now;
        }
        String report() { return String.format("%d ticks, average %.2f ms, worst %.2f ms", count, count == 0 ? 0 : sum / 1e6 / count, worst / 1e6); }
    }

    /** A Mass Fabricator takes 1,000,000 EU (its whole room) in one call and its tier rises with Transformer Upgrades. */
    @GameTest(batch = "fabricator_stress", template = "empty", timeoutTicks = 40)
    public static void massFabricatorTakesItsRoomAndTransformersRaiseItsTier(GameTestHelper h) {
        BlockPos pos = new BlockPos(1, 1, 1);
        h.setBlock(pos, block("producer/block_matter_elc"));
        BlockEntity fab = h.getBlockEntity(pos);
        int[] tier = new int[1];
        h.startSequence()
            .thenIdle(2)
            .thenExecute(() -> {
                long took = (Long) call(storage(fab), "receive", Long.MAX_VALUE, false);
                h.assertTrue(took == 1_000_000L, "took " + took + " EU of a Long.MAX_VALUE offer (room 1,000,000)");
                var fe = h.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, h.absolutePos(pos), Direction.UP);
                h.assertTrue(fe != null && fe.receiveEnergy(Integer.MAX_VALUE, true) == 0, "a full fabricator still takes FE");
                tier[0] = (Integer) call(fab, "getSinkTier");
                var slots = (IItemHandlerModifiable) call(fab, "getItemHandler");
                for (int slot = 3; slot < 7; slot++) slots.setStackInSlot(slot, new ItemStack(BuiltInRegistries.ITEM.get(si("upgrade/transformer_upgrade"))));
            })
            .thenIdle(2)
            .thenExecute(() -> {
                int fluid = ((FluidTank) call(fab, "getUuMatterTank")).getFluidAmount();
                h.assertTrue(fluid == 1, "1,000,000 EU made " + fluid + " mB");
                int raised = (Integer) call(fab, "getSinkTier");
                h.assertTrue(raised > tier[0], "four Transformer Upgrades left the tier at " + raised + " (was " + tier[0] + ")");
                LOG.info("[fabricator stress] Mass Fabricator sink tier {} -> {} with four Transformer Upgrades", tier[0], raised);
            })
            .thenSucceed();
    }

    /** 1,000,000 EU/t from a creative generator into a Mass Fabricator with four Transformer Upgrades: 1 mB every tick. */
    @GameTest(batch = "fabricator_stress", template = "empty", timeoutTicks = 400)
    public static void massFabricatorUnderAMillionEuPerTick(GameTestHelper h) {
        BlockPos fabPos = new BlockPos(1, 1, 1), genPos = new BlockPos(2, 1, 1);
        h.setBlock(fabPos, block("producer/block_matter_elc"));
        h.setBlock(genPos, block("generator/block_creative_generator"));
        BlockEntity fab = h.getBlockEntity(fabPos), gen = h.getBlockEntity(genPos);
        var slots = (IItemHandlerModifiable) call(fab, "getItemHandler");
        for (int slot = 3; slot < 7; slot++) slots.setStackInSlot(slot, new ItemStack(BuiltInRegistries.ITEM.get(si("upgrade/transformer_upgrade"))));
        call(gen, "configure", 1_000_000L, true);
        FluidTank matter = (FluidTank) call(fab, "getUuMatterTank");
        TickClock clock = new TickClock();
        int[] mark = new int[1];
        long[] peak = new long[1];
        h.onEachTick(() -> { clock.tick(); peak[0] = Math.max(peak[0], amount(fab)); });
        h.startSequence()
            .thenWaitUntil(() -> h.assertTrue(matter.getFluidAmount() >= 5, "no UU-matter yet: " + matter.getFluidAmount() + " mB"))
            .thenExecute(() -> mark[0] = matter.getFluidAmount())
            .thenIdle(100)
            .thenExecute(() -> {
                int made = matter.getFluidAmount() - mark[0];
                LOG.info("[fabricator stress] Mass Fabricator at 1,000,000 EU/t: {} mB in 100 ticks, peak stored {} EU, {}", made, peak[0], clock.report());
                h.assertTrue(made >= 99 && made <= 100, made + " mB in 100 ticks at 1,000,000 EU/t (1 mB per tick expected)");
                h.assertTrue(peak[0] <= 1_000_000L, "stored " + peak[0] + " EU, above the 1,000,000 EU buffer");
                h.assertTrue(h.getBlockState(fabPos).is(block("producer/block_matter_elc")), "the fabricator is gone");
                h.assertTrue(clock.worst < 1_000_000_000L, "a tick took " + clock.worst / 1e6 + " ms");
            })
            .thenSucceed();
    }

    /** A Large Fabricator with four IV input modules kept full: 1 mB per tick, modules drained in step, no stall. */
    @GameTest(batch = "fabricator_stress", template = "empty", timeoutTicks = 400)
    public static void largeFabricatorWithFourFullIvModules(GameTestHelper h) {
        BlockPos core = new BlockPos(2, 2, 2);
        BlockPos[] inputs = {core.north(), core.south(), core.east(), core.west()};
        for (BlockPos p : inputs) h.setBlock(p, block("producer/block_large_fabricator_input_iv"));
        h.setBlock(core.above(), block("producer/block_large_fabricator_tank"));
        h.setBlock(core.below(), block("producer/block_large_fabricator_scrap"));
        h.setBlock(core, block("producer/block_large_fabricator_core"));
        BlockEntity coreBe = h.getBlockEntity(core);
        BlockEntity[] modules = new BlockEntity[inputs.length];
        for (int i = 0; i < inputs.length; i++) modules[i] = h.getBlockEntity(inputs[i]);
        TickClock clock = new TickClock();
        int[] mark = new int[1];
        long[] peak = new long[1];
        h.startSequence()
            .thenWaitUntil(() -> h.assertTrue((Boolean) call(coreBe, "isStructureComplete"), "the structure did not form"))
            .thenExecute(() -> {
                // No overflow at the limits: Long.MAX_VALUE EU and Integer.MAX_VALUE FE into empty modules
                long eu = (Long) call(modules[0], "receiveEnergy", Long.MAX_VALUE, false);
                var fe = h.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, h.absolutePos(inputs[1]), Direction.SOUTH);   // the outer face of the south module
                int took = fe == null ? -1 : fe.receiveEnergy(Integer.MAX_VALUE, false);
                h.assertTrue(eu == 800_000L && took == 3_200_000, "EU offer took " + eu + ", FE offer took " + took);
                for (BlockEntity m : modules) call(m, "receiveEnergy", 800_000L, false);
                // keep every module full from now on: four IV modules at their limit
                h.onEachTick(() -> {
                    clock.tick();
                    for (BlockEntity m : modules) {
                        call(m, "receiveEnergy", 800_000L, false);
                        peak[0] = Math.max(peak[0], (Long) call(m, "getStoredEnergy"));
                    }
                });
                mark[0] = (Integer) call(coreBe, "getTotalUuMatterAmount");
            })
            .thenIdle(100)
            .thenExecute(() -> {
                int made = (Integer) call(coreBe, "getTotalUuMatterAmount") - mark[0];
                // the cost of one fabrication step with full modules, repeated away from the tick loop
                long t0 = System.nanoTime();
                for (int i = 0; i < 2_000; i++) {
                    for (BlockEntity m : modules) call(m, "receiveEnergy", 800_000L, false);
                    call(coreBe, "processFabrication", h.getLevel(), coreBe.getBlockPos());
                }
                double perStep = (System.nanoTime() - t0) / 2_000.0 / 1e3;
                LOG.info("[fabricator stress] Large Fabricator, four full IV modules: {} mB in 100 ticks, peak module {} EU, {}, {} us per step (2,000 steps)",
                    made, peak[0], clock.report(), String.format("%.1f", perStep));
                h.assertTrue(made >= 99 && made <= 101, made + " mB in 100 ticks (1 mB per tick expected)");
                h.assertTrue(peak[0] <= 800_000L, "a module held " + peak[0] + " EU, above its 800,000 EU buffer");
                h.assertTrue(perStep < 1_000.0, "one fabrication step took " + perStep + " us");
                h.assertTrue(clock.worst < 1_000_000_000L, "a tick took " + clock.worst / 1e6 + " ms");
            })
            .thenSucceed();
    }
}
