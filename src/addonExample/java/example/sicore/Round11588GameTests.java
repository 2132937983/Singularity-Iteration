package example.sicore;

import com.miophas.singularity_iteration.core.api.reactor.IReactorController;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.88: the nuclear reactor sends up to its capacity, 1,000,000 EU/t (it was 64 IV packets, 524,288 EU/t). The engine
 * takes at most 64 packets per source, so above 524,288 EU/t of output the packets are LuV (32,768 EU); at or below it they
 * stay IV (8,192 EU), and IV cables carry them as before.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11588GameTests {
    private Round11588GameTests() {}

    private static final BlockPos REACTOR = new BlockPos(1, 2, 1);
    private static final long IV_CEILING = 64L * 8_192L, CAPACITY = 1_000_000L;

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }
    private static Block block(String path) { return BuiltInRegistries.BLOCK.get(id(path)); }
    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }
    private static long stored(Object block) { return ((Number) call(call(block, "getEnergyStorageInternal"), "getAmount")).longValue(); }

    private static Object field(Object target, String name) {
        try {
            for (Class<?> k = target.getClass(); k != null; k = k.getSuperclass()) {
                try { var f = k.getDeclaredField(name); f.setAccessible(true); return f.get(target); }
                catch (NoSuchFieldException next) { /* superclass */ }
            }
            throw new AssertionError("no field " + name);
        } catch (IllegalAccessException e) { throw new AssertionError(name, e); }
    }

    private static void setRate(Object reactor, long euPerTick) {
        Object rate = field(reactor, "rate");
        Object value = call(rate.getClass(), "of", euPerTick);
        try {
            for (Class<?> k = reactor.getClass(); k != null; k = k.getSuperclass()) {
                try { var f = k.getDeclaredField("rate"); f.setAccessible(true); f.set(reactor, value); return; }
                catch (NoSuchFieldException next) { /* superclass */ }
            }
        } catch (IllegalAccessException e) { throw new AssertionError(e); }
        throw new AssertionError("no field rate");
    }

    private static int tier(Object reactor) { return ((Number) call(reactor, "getSourceTier")).intValue(); }

    /** A running reactor (one simple uranium rod, redstone above) with {@code between} (or nothing) and a GESU input module east. */
    private static IReactorController reactor(GameTestHelper h, String between) {
        h.setBlock(REACTOR, block("generator/block_nuclear_reactor_generator"));
        if (between != null) h.setBlock(REACTOR.east(), block(between));
        h.setBlock(between != null ? REACTOR.east(2) : REACTOR.east(), block("wiring/block_gesu_input_iv"));
        h.setBlock(REACTOR.above(), Blocks.REDSTONE_BLOCK);
        IReactorController reactor = (IReactorController) h.getBlockEntity(REACTOR);
        h.assertTrue(reactor.getItemHandler().insertItem(10, new ItemStack(BuiltInRegistries.ITEM.get(id("reactor/item_reactor_uranium_simple"))), false).isEmpty(),
            "fuel rejected");
        return reactor;
    }

    @GameTest(batch = "round11588_reactor_output", template = "empty", timeoutTicks = 80)
    public static void reactorStaysIvUpToTheIvCeiling(GameTestHelper h) {
        IReactorController reactor = reactor(h, "wiring/cable/block_glass_cable");
        BlockEntity sink = h.getBlockEntity(REACTOR.east(2));
        h.runAfterDelay(25, () -> {
            h.assertTrue(reactor.getCurrentEnergyGeneration() > 0, "the reactor did not run a cycle");
            setRate(reactor, IV_CEILING);
            h.assertTrue(tier(reactor) == 5, "at " + IV_CEILING + " EU/t the reactor is tier " + tier(reactor) + ", expected IV (5)");
            setRate(reactor, 500_000L);
            long before = stored(sink);
            h.runAfterDelay(2, () -> {
                h.assertTrue(h.getBlockState(REACTOR.east()).is(block("wiring/cable/block_glass_cable")), "the IV cable melted at 500,000 EU/t");
                long perTick = (stored(sink) - before) / 2;
                h.assertTrue(perTick > IV_CEILING / 2, "500,000 EU/t through an IV cable delivered only " + perTick + " EU/t");
                h.succeed();
            });
        });
    }

    @GameTest(batch = "round11588_reactor_output", template = "empty", timeoutTicks = 80)
    public static void reactorMeltsAnIvCableAboveTheIvCeiling(GameTestHelper h) {
        IReactorController reactor = reactor(h, "wiring/cable/block_glass_cable");
        h.runAfterDelay(25, () -> {
            h.assertTrue(reactor.getCurrentEnergyGeneration() > 0, "the reactor did not run a cycle");
            setRate(reactor, IV_CEILING + 1);
            h.assertTrue(tier(reactor) == 6, "at " + (IV_CEILING + 1) + " EU/t the reactor is tier " + tier(reactor) + ", expected LuV (6)");
            setRate(reactor, 600_000L);
            h.runAfterDelay(3, () -> {
                h.assertTrue(!h.getBlockState(REACTOR.east()).is(block("wiring/cable/block_glass_cable")),
                    "the IV cable carried 600,000 EU/t: the reactor did not send LuV packets");
                h.succeed();
            });
        });
    }

    @GameTest(batch = "round11588_reactor_output", template = "empty", timeoutTicks = 80)
    public static void reactorSendsUpToItsCapacityPerTick(GameTestHelper h) {
        IReactorController reactor = reactor(h, null);
        BlockEntity sink = h.getBlockEntity(REACTOR.east());
        h.runAfterDelay(25, () -> {
            h.assertTrue(reactor.getCurrentEnergyGeneration() > 0, "the reactor did not run a cycle");
            setRate(reactor, 2_000_000L);
            long before = stored(sink);
            h.runAfterDelay(2, () -> {
                long perTick = (stored(sink) - before) / 2;
                h.assertTrue(perTick > IV_CEILING, "the reactor delivered only " + perTick + " EU/t (the IV ceiling is " + IV_CEILING + ")");
                h.assertTrue(perTick <= CAPACITY, "the reactor delivered " + perTick + " EU/t, more than its " + CAPACITY + " EU/t budget");
                h.succeed();
            });
        });
    }
}
