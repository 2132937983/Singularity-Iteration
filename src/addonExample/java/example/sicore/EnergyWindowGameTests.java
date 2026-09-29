package example.sicore;

import com.miophas.singularity_iteration.core.api.energy.EnergyPacketPolicy;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import com.miophas.singularity_iteration.core.runtime.energy.engine.DomainDistributor;
import com.miophas.singularity_iteration.core.runtime.energy.engine.EnergyAmount;
import com.miophas.singularity_iteration.core.runtime.energy.engine.FractionalDistributor;
import com.miophas.singularity_iteration.core.runtime.energy.engine.PacketAllowance;
import com.miophas.singularity_iteration.core.runtime.energy.engine.RouteCosts;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class EnergyWindowGameTests {
    @GameTest(template = "empty", batch = "packet_arithmetic")
    public static void windowRemaindersKeepTheirOriginalAllowance(GameTestHelper h) {
        var policy = new EnergyPacketPolicy(32, 1, false);
        var allowance = new PacketAllowance(policy, EnergyAmount.of(32));
        allowance.debit(EnergyAmount.of(1));
        var quoted = allowance.quote(policy, EnergyAmount.of(1024), 1);
        h.assertTrue(quoted.equals(EnergyAmount.of(31)), "A new window renewed or discarded the original budget");
        RouteCosts zero = new RouteCosts() {
            public boolean reaches(int id) { return true; }
            public long lossMilliTo(int id) { return 0; }
        };
        var sources = List.of(new DomainDistributor.Source(31, 32, false, 1));
        var domains = List.of(new DomainDistributor.Domain(new int[]{0}, List.of(zero), new int[][]{{0}}));
        var integer = DomainDistributor.allocateQuotedTraced(sources, domains, new int[]{0}, new long[]{1000},
            List.of(), new long[]{31}, new Random(7));
        var exact = FractionalDistributor.allocateQuotedTraced(sources, List.of(EnergyAmount.fromDouble(31.5)), domains,
            new int[]{0}, List.of(EnergyAmount.of(1000)), List.of(), List.of(quoted), new Random(7));
        h.assertTrue(integer.credit(0) == 31 && exact.credit(0).equals(quoted), "Full-packet threshold reapplied to a packet remainder");
        allowance.debit(exact.debit(0));
        h.assertTrue(allowance.quote(policy, EnergyAmount.of(1024), 1).isZero(), "Incoming energy renewed a spent allowance");
        h.assertTrue(new PacketAllowance(policy, EnergyAmount.of(31)).remaining().isZero(), "Incomplete initial packet escaped");
        h.assertTrue(new PacketAllowance(policy, EnergyAmount.of(32)).quote(new EnergyPacketPolicy(32, 2, false), EnergyAmount.of(32), 1).isZero(), "Policy change reused a stale allowance");
        h.assertTrue(new EnergyPacketPolicy(Long.MAX_VALUE, 64, true).maximumTransfer() == Long.MAX_VALUE,
            "Packet ceiling overflowed");
        for (int count : new int[]{0, 1, 8, 64}) {
            var multi = List.of(new DomainDistributor.Source(65536, 32, true, count));
            var round = FractionalDistributor.allocateTraced(multi, List.of(EnergyAmount.fromDouble(65536.5)), domains,
                new int[]{0}, List.of(EnergyAmount.of(10000)), List.of(), new Random(1));
            h.assertTrue(round.credit(0).equals(EnergyAmount.of(32L * count)), "Wrong multi-packet allowance: " + count);
            for (var delivery : round.deliveries()) h.assertTrue(delivery.sourceDebit().compareTo(EnergyAmount.of(32)) <= 0,
                "Multi-packet throughput became a high-voltage packet");
        }
        h.succeed();
    }

    @GameTest(template = "reactor_loop", batch = "energy_source_windows", timeoutTicks = 100)
    public static void thirtyOneSources(GameTestHelper h) { sources(h, 31); }
    @GameTest(template = "reactor_loop", batch = "energy_source_windows", timeoutTicks = 100)
    public static void thirtyTwoSources(GameTestHelper h) { sources(h, 32); }
    @GameTest(template = "reactor_loop", batch = "energy_source_windows", timeoutTicks = 100)
    public static void thirtyThreeSources(GameTestHelper h) { sources(h, 33); }

    private static void sources(GameTestHelper h, int count) {
        var placed = new ArrayList<BlockPos>();
        for (int x = 1; x <= 8; x++) for (int z = 1; z <= 6; z++) {
            var p = new BlockPos(x, 2, z); h.setBlock(p, block("wiring/cable/block_glass_cable")); placed.add(p);
        }
        var sources = new ArrayList<InheritedSource>();
        for (int i = 0; i < count; i++) {
            var p = new BlockPos(1 + i % 6, 3, 1 + i / 6);
            h.setBlock(p, CoreExampleMod.SOURCE.get()); placed.add(p); sources.add(h.getBlockEntity(p));
        }
        var load = new BlockPos(8, 3, 3);
        h.setBlock(load, facing("wiring/block_bat_box", Direction.EAST)); placed.add(load);
        AbstractEnergyBlockEntity sink = h.getBlockEntity(load);
        int[] tick = {0};
        h.onEachTick(() -> {
            int now = ++tick[0];
            if (now > 20 && now <= 60) h.assertTrue(sink.getEnergyStorageInternal().getAmount() == count * 32L,
                count + " sources delivered " + sink.getEnergyStorageInternal().getAmount() + " instead of " + count * 32L);
            if (now <= 60) {
                sink.getEnergyStorageInternal().setStored(0);
                for (var source : sources) source.generate(32);
            }
            if (now == 80) for (var p : placed) h.setBlock(p, Blocks.AIR);
            if (now == 95) h.succeed();
        });
    }

    @GameTest(template = "reactor_loop", batch = "energy_receiver_windows", timeoutTicks = 190)
    public static void oneHundredTwentySevenReceivers(GameTestHelper h) { receivers(h, 127); }
    @GameTest(template = "reactor_loop", batch = "energy_receiver_windows", timeoutTicks = 190)
    public static void oneHundredTwentyEightReceivers(GameTestHelper h) { receivers(h, 128); }
    @GameTest(template = "reactor_loop", batch = "energy_receiver_windows", timeoutTicks = 190)
    public static void oneHundredTwentyNineReceivers(GameTestHelper h) { receivers(h, 129); }

    /** Move a 31 EU deficit across every endpoint while a fixed endpoint needs 1 EU. */
    private static void receivers(GameTestHelper h, int count) {
        var placed = new ArrayList<BlockPos>();
        for (int x = 1; x <= 12; x++) for (int z = 1; z <= 11; z++) {
            var p = new BlockPos(x, 2, z); h.setBlock(p, block("wiring/cable/block_glass_cable")); placed.add(p);
        }
        var sinks = new ArrayList<InheritedSink>();
        for (int i = 0; i < count; i++) {
            var p = new BlockPos(1 + i % 12, 3, 1 + i / 12);
            h.setBlock(p, CoreExampleMod.SINK.get()); placed.add(p);
            InheritedSink sink = h.getBlockEntity(p); sink.getEnergyStorageInternal().setStored(1024); sinks.add(sink);
        }
        var sourcePos = new BlockPos(0, 2, 1);
        h.setBlock(sourcePos, CoreExampleMod.PACKET_SOURCE.get()); placed.add(sourcePos);
        ComposedPacketSource source = h.getBlockEntity(sourcePos); source.configure(1, false);
        int[] tick = {0}; int end = 20 + count - 1;
        h.onEachTick(() -> {
            int now = ++tick[0];
            if (now > 20 && now <= end) {
                long missing = sinks.stream().mapToLong(s -> 1024 - s.stored()).sum();
                h.assertTrue(missing == 0 && source.energyComponent().stored() == 0,
                    count + " receiver window left " + missing + " EU missing, source=" + source.energyComponent().stored() + ", tick=" + now);
            }
            if (now >= 20 && now < end) {
                sinks.get(0).getEnergyStorageInternal().setStored(1023);
                sinks.get(1 + now - 20).getEnergyStorageInternal().setStored(993);
                source.energyComponent().generate(32, false);
            }
            if (now == 170) for (var p : placed) h.setBlock(p, Blocks.AIR);
            if (now == 185) h.succeed();
        });
    }

    @GameTest(template = "empty", batch = "addon_legacy_multi_packet", timeoutTicks = 100)
    public static void inheritedSourceEmitsEightLowVoltagePackets(GameTestHelper h) {
        var p = new BlockPos(1, 1, 1);
        h.setBlock(p, CoreExampleMod.MULTI_SOURCE.get());
        h.setBlock(p.east(), CoreExampleMod.SINK.get());
        InheritedMultiSource source = h.getBlockEntity(p); source.generate(65536);
        InheritedSink sink = h.getBlockEntity(p.east()); int[] tick = {0};
        h.onEachTick(() -> {
            int now = ++tick[0];
            if (now > 20 && now <= 60) h.assertTrue(sink.stored() == 256, "Legacy packet count was ignored or enlarged into an overvoltage packet");
            if (now <= 60) sink.getEnergyStorageInternal().setStored(0);
            if (now == 60) { h.setBlock(p, Blocks.AIR); h.setBlock(p.east(), Blocks.AIR); }
            if (now == 80) h.succeed();
        });
    }

    @GameTest(template = "empty", batch = "addon_multi_packet", timeoutTicks = 150)
    public static void composedSourceChangesPolicyAndSharesFeLimit(GameTestHelper h) {
        var p = new BlockPos(1, 1, 1);
        h.setBlock(p, CoreExampleMod.PACKET_SOURCE.get());
        h.setBlock(p.east(), facing("wiring/block_bat_box", Direction.EAST));
        ComposedPacketSource source = h.getBlockEntity(p);
        source.energyComponent().generate(65536, false);
        AbstractEnergyBlockEntity sink = h.getBlockEntity(p.east());
        int[] tick = {0};
        h.onEachTick(() -> {
            int now = ++tick[0];
            long expected = now <= 30 ? 256 : now <= 50 ? 0 : now <= 70 ? 64 : now <= 95 ? 2048 : 240;
            if ((now > 20 && now <= 30) || (now > 40 && now <= 50) || (now > 60 && now <= 70)
                || (now > 80 && now <= 95) || (now > 106 && now <= 120))
                h.assertTrue(sink.getEnergyStorageInternal().getAmount() == expected,
                    "Explicit policy/shared FE mismatch tick=" + now + ": " + sink.getEnergyStorageInternal().getAmount() + ", expected=" + expected);
            if (now <= 120) {
                sink.getEnergyStorageInternal().setStored(0);
                source.energyComponent().generate(4096, false);
            }
            if (now == 30) source.configure(0, true);
            if (now == 50) source.configure(2, true);
            if (now == 70) source.configure(64, true);
            if (now == 95) { source.configure(8, true); source.energyComponent().internalStorage().setMaxExtract(256); }
            if (now == 105) source.requestFeProbe(64);
            if (now > 106 && now <= 120) h.assertTrue(source.extractedFe() == 64,
                "FE draw tick=" + now + ": expected 64 FE, received " + source.extractedFe());
            if (now == 120) { h.setBlock(p, Blocks.AIR); h.setBlock(p.east(), Blocks.AIR); }
            if (now == 135) h.succeed();
        });
    }

    private static BlockState block(String path) {
        var id = ResourceLocation.fromNamespaceAndPath("mio_icif", path);
        if (!BuiltInRegistries.BLOCK.containsKey(id)) throw new IllegalArgumentException("Missing block " + id);
        return BuiltInRegistries.BLOCK.get(id).defaultBlockState();
    }
    private static BlockState facing(String path, Direction facing) {
        var state = block(path);
        return state.setValue((DirectionProperty) state.getBlock().getStateDefinition().getProperty("facing"), facing);
    }
}
