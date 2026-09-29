package example.sicore;

import com.miophas.singularity_iteration.core.platform.neoforge.energy.IndependentTransformerBlockEntity;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import com.miophas.singularity_iteration.core.runtime.energy.engine.DomainDistributor;
import com.miophas.singularity_iteration.core.runtime.energy.engine.EnergyAmount;
import com.miophas.singularity_iteration.core.runtime.energy.engine.FractionalDistributor;
import com.miophas.singularity_iteration.core.runtime.energy.engine.RouteCosts;
import com.miophas.singularity_iteration.core.runtime.energy.engine.TransformerAccounting;
import com.miophas.singularity_iteration.core.runtime.energy.engine.TransformerBatch;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** IC2 packet regressions, promoted from the audit's real-world reproduction. */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class EnergyEngineAuditGameTests {
    @GameTest(template = "reactor_loop", batch = "energy_engine_audit", timeoutTicks = 100)
    public static void characterizeBoostThroughputAndPacketAllocation(GameTestHelper h) {
        numericAudit(h);
        List<Scenario> scenarios = List.of(
            create(h, new BlockPos(3, 3, 3), 512, 1),
            create(h, new BlockPos(9, 3, 3), 512, 4),
            create(h, new BlockPos(3, 3, 9), 2048, 1),
            create(h, new BlockPos(9, 3, 9), 2048, 4));
        int[] tick = {0};
        h.onEachTick(() -> {
            int now = ++tick[0];
            if (now == 20) for (Scenario s : scenarios) s.begin();
            if (now > 20 && now <= 60) for (Scenario s : scenarios) s.sample(h);
            if (now == 60) {
                for (Scenario s : scenarios) s.report(h);
                legacyFractionAudit(h);
                for (Scenario s : scenarios) {
                    BlockPos at = h.relativePos(s.transformer.getBlockPos());
                    h.setBlock(at, Blocks.AIR);
                    for (Direction side : Direction.values()) h.setBlock(at.relative(side), Blocks.AIR);
                }
                h.setBlock(new BlockPos(6, 3, 6), Blocks.AIR);
            }
            if (now == 80) h.succeed(); // Let removals publish before another batch observes this world.
        });
    }

    @GameTest(template = "empty", batch = "energy_engine_effect_audit", timeoutTicks = 45)
    public static void characterizeHighTierSourceWithOneEuDemand(GameTestHelper h) {
        BlockPos sourcePos = new BlockPos(1, 1, 1), sinkPos = sourcePos.east();
        h.setBlock(sourcePos, facing("wiring/block_mfsu", Direction.EAST));
        h.setBlock(sinkPos, facing("wiring/block_mfe", Direction.EAST));
        AbstractEnergyBlockEntity source = h.getBlockEntity(sourcePos), sink = h.getBlockEntity(sinkPos);
        source.getEnergyStorageInternal().setStored(100_000);
        sink.getEnergyStorageInternal().setStored(sink.getEnergyStorageInternal().getCapacity() - 1);
        h.runAfterDelay(20, () -> {
            long debit = 100_000 - source.getEnergyStorageInternal().getAmount();
            h.assertTrue(debit == 1, "Partial-demand audit did not transfer exactly 1 EU: " + debit);
            h.assertTrue(h.getLevel().getBlockEntity(h.absolutePos(sinkPos)) == sink,
                "Nominal 2048 EU source destroyed a sink after delivering only 1 EU");
            System.out.println("ENERGY_AUDIT partial_demand nominalPacket=2048 actualDebit=" + debit
                + " sinkSurvived=" + (h.getLevel().getBlockEntity(h.absolutePos(sinkPos)) == sink) + " ic2ExpectedSurvival=true");
            h.setBlock(sourcePos, Blocks.AIR);
            h.setBlock(sinkPos, Blocks.AIR);
        });
        h.runAfterDelay(28, h::succeed);
    }

    private static Scenario create(GameTestHelper h, BlockPos center, long low, int count) {
        h.setBlock(center, facing(low == 512 ? "wiring/transformer_hv_ev" : "wiring/transformer_ev_sc", Direction.EAST));
        IndependentTransformerBlockEntity transformer = h.getBlockEntity(center);
        transformer.setSavedMode(0); // Existing SI persisted mode: forced step-up.
        var sources = new ArrayList<AbstractEnergyBlockEntity>();
        Direction[] sides = {Direction.WEST, Direction.NORTH, Direction.SOUTH, Direction.DOWN};
        for (int i = 0; i < count; i++) {
            BlockPos at = center.relative(sides[i]);
            h.setBlock(at, facing(low == 512 ? "wiring/block_mfe" : "wiring/block_mfsu", sides[i].getOpposite()));
            AbstractEnergyBlockEntity source = h.getBlockEntity(at);
            source.getEnergyStorageInternal().setStored(1_000_000);
            sources.add(source);
        }
        h.setBlock(center.east(), facing("wiring/block_eesu", Direction.EAST));
        AbstractEnergyBlockEntity sink = h.getBlockEntity(center.east());
        return new Scenario(low, transformer, sources, sink);
    }

    private static BlockState facing(String id, Direction side) {
        BlockState state = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("mio_icif:" + id)).defaultBlockState();
        return state.setValue((DirectionProperty) state.getBlock().getStateDefinition().getProperty("facing"), side);
    }

    private static final class Scenario {
        final long low;
        final IndependentTransformerBlockEntity transformer;
        final List<AbstractEnergyBlockEntity> sources;
        final AbstractEnergyBlockEntity sink;
        final List<Long> increments = new ArrayList<>();
        long initialTotal, previous, startSink, startSources, startBuffer;
        Scenario(long low, IndependentTransformerBlockEntity transformer, List<AbstractEnergyBlockEntity> sources, AbstractEnergyBlockEntity sink) {
            this.low = low; this.transformer = transformer; this.sources = sources; this.sink = sink;
        }
        long sourceTotal() { return sources.stream().mapToLong(s -> s.getEnergyStorageInternal().getAmount()).sum(); }
        long buffer() { return transformer.snapshot(true).orElseThrow().energy().amount(); }
        void begin() {
            startSources = sourceTotal(); startBuffer = buffer();
            startSink = previous = sink.getEnergyStorageInternal().getAmount();
            initialTotal = startSources + startBuffer + startSink;
        }
        void sample(GameTestHelper h) {
            long current = sink.getEnergyStorageInternal().getAmount();
            h.assertTrue(sourceTotal() + buffer() + current == initialTotal, "Direct boost circuit violated conservation");
            h.assertTrue(current >= previous, "Unconnected output load lost energy");
            increments.add(current - previous); previous = current;
        }
        void report(GameTestHelper h) {
            h.assertTrue(increments.size() == 40 && previous > startSink, "Audit did not observe a live 40-tick circuit");
            long expected = low * sources.size();
            h.assertTrue(previous - startSink == expected * 40, "Boost throughput: expected " + expected + " EU/t");
            if (sources.size() == 4)
                h.assertTrue(increments.stream().allMatch(v -> v == low * 4), "Sufficient four-source boost still skips ticks");
            else h.assertTrue(increments.stream().filter(v -> v == 0).count() == 30, "Single-source boost changed its packet threshold");
            System.out.println("ENERGY_AUDIT boost low=" + low + " sources=" + sources.size()
                + " samples=" + increments.size() + " sourceDebit=" + (startSources - sourceTotal())
                + " bufferDelta=" + (buffer() - startBuffer) + " sinkCredit=" + (previous - startSink)
                + " zeroTicks=" + increments.stream().filter(v -> v == 0).count()
                + " average=" + ((previous - startSink) / 40.0) + " increments=" + increments);
        }
    }

    private static void numericAudit(GameTestHelper h) {
        RouteCosts direct = new RouteCosts() {
            public boolean reaches(int receiver) { return receiver == 0; }
            public long lossMilliTo(int receiver) { return 0; }
        };
        for (long low : new long[]{512, 2048}) {
            var quotes = new ArrayList<DomainDistributor.Source>();
            var amounts = new ArrayList<EnergyAmount>();
            for (int i = 0; i < 4; i++) {
                quotes.add(new DomainDistributor.Source(low * 8, low, false));
                amounts.add(EnergyAmount.of(low * 8));
            }
            var domains = List.of(new DomainDistributor.Domain(new int[]{0, 1, 2, 3},
                List.of(direct, direct, direct, direct), new int[][]{{0}, {0}, {0}, {0}}));
            var integers = DomainDistributor.allocateTraced(quotes, domains, new int[]{0}, new long[]{low * 8}, new Random(1));
            // Fractional room forces the exact planner instead of its integer fast path.
            var fractions = FractionalDistributor.allocateTraced(quotes, amounts, domains, new int[]{0},
                List.of(EnergyAmount.fromDouble(low * 8 - 0.5)), List.of(), new Random(1));
            long debit = 0;
            EnergyAmount exactDebit = EnergyAmount.ZERO;
            for (int i = 0; i < 4; i++) { debit += integers.debit(i); exactDebit = exactDebit.add(fractions.debit(i)); }
            h.assertTrue(debit == integers.credit(0) + integers.dissipated(), "Integer allocator violated conservation");
            h.assertTrue(exactDebit.equals(fractions.credit(0).add(fractions.dissipated())), "Exact allocator violated conservation");
            h.assertTrue(integers.credit(0) == low * 4 && fractions.credit(0).equals(EnergyAmount.of(low * 4)),
                "Receiver did not accept all four sources in integer and fractional paths");
            System.out.println("ENERGY_AUDIT four_sources low=" + low + " integralCredit=" + integers.credit(0)
                + " fractionalCredit=" + fractions.credit(0).toDouble() + " ic2SufficientRoomExpected=" + low * 4);
            var batch = TransformerBatch.allocate(new TransformerAccounting.Configuration(low, false), low * 8,
                new long[]{low * 16}, new int[]{0});
            h.assertTrue(batch.debit() == batch.credit(0) + batch.dissipated(), "Transformer batch violated conservation");
            h.assertTrue(batch.credit(0) == low * 4 && batch.deliveries().size() == 4, "Step-down did not deliver four separate packets to one sink");
            System.out.println("ENERGY_AUDIT step_down low=" + low + " credit=" + batch.credit(0)
                + " packets=" + batch.deliveries().size() + " ic2SufficientRoomExpected=" + low * 4);
        }
    }

    private static void legacyFractionAudit(GameTestHelper h) {
        BlockPos at = new BlockPos(6, 3, 6);
        h.setBlock(at, facing("wiring/transformer_hv_ev", Direction.EAST));
        IndependentTransformerBlockEntity transformer = h.getBlockEntity(at);
        CompoundTag tag = new CompoundTag();
        tag.putLong("buffer_long", 100);
        tag.putDouble("buffer_fraction", 0.75);
        tag.putInt("mode", 0);
        transformer.loadWithComponents(tag, h.getLevel().registryAccess());
        h.assertTrue(transformer.snapshot(true).orElseThrow().energy().exactAmount().equals(EnergyAmount.fromDouble(100.75)),
            "Legacy transformer fraction was lost");
        transformer.loadWithComponents(transformer.saveWithoutMetadata(h.getLevel().registryAccess()), h.getLevel().registryAccess());
        h.assertTrue(transformer.snapshot(true).orElseThrow().energy().exactAmount().equals(EnergyAmount.fromDouble(100.75)),
            "Transformer fraction changed on second save/load");
        System.out.println("ENERGY_AUDIT legacy_fraction input=100.75 actual="
            + transformer.snapshot(true).orElseThrow().energy().exactAmount().toDouble());
        for (double fraction : new double[]{0, 0.125, 0.75, Math.nextDown(1.0), -0.5, Double.NaN, Double.POSITIVE_INFINITY}) {
            tag.putDouble("buffer_fraction", fraction);
            transformer.loadWithComponents(tag, h.getLevel().registryAccess());
            var expected = EnergyAmount.of(100).add(Double.isFinite(fraction) && fraction >= 0 && fraction < 1
                ? EnergyAmount.fromDouble(fraction) : EnergyAmount.ZERO);
            h.assertTrue(transformer.snapshot(true).orElseThrow().energy().exactAmount().equals(expected), "Legacy fractional boundary: " + fraction);
            transformer.loadWithComponents(transformer.saveWithoutMetadata(h.getLevel().registryAccess()), h.getLevel().registryAccess());
            h.assertTrue(transformer.snapshot(true).orElseThrow().energy().exactAmount().equals(expected), "Fractional boundary changed on reload");
        }
    }
}
