package example.sicore;

import com.miophas.singularity_iteration.core.platform.neoforge.energy.IndependentSpecialCableBlockEntity;
import com.miophas.singularity_iteration.core.runtime.energy.IndependentSiEnergy;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class EnergyBoundaryGameTests {
    @GameTest(template = "empty", batch = "energy_foreign_policy", timeoutTicks = 165)
    public static void zeroPacketsDoNotFailForeignExportAndMultiPacketsResume(GameTestHelper h) {
        var p = new BlockPos(1,2,2);
        h.setBlock(p, CoreExampleMod.PACKET_SOURCE.get()); h.setBlock(p.east(), CoreExampleMod.FOREIGN_BATTERY.get());
        ComposedPacketSource source = h.getBlockEntity(p); ForeignBattery sink = h.getBlockEntity(p.east());
        source.configure(0,true); source.energyComponent().generate(65536,false);
        var engine = IndependentSiEnergy.current(h.getLevel().getServer());
        int[] tick = {0};
        h.onEachTick(() -> {
            int now = ++tick[0];
            if (now == 35) {
                h.assertTrue(sink.storage.getEnergyStored() == 0 && engine.metrics().failure().isEmpty(), "Zero packets exported or disabled the grid");
                source.configure(8,true);
            }
            if (now > 60 && now <= 75) h.assertTrue(sink.storage.getEnergyStored() == 1024,
                "8-packet foreign output was " + sink.storage.getEnergyStored() + " FE instead of 1024");
            if (now == 75) source.configure(64,true);
            if (now > 100 && now <= 120) h.assertTrue(sink.storage.getEnergyStored() == 8192,
                "64-packet foreign output was " + sink.storage.getEnergyStored() + " FE instead of 8192");
            if (now <= 120) { sink.storage.extractEnergy(100000,false); source.energyComponent().generate(2048,false); }
            if (now == 120) for (var at : List.of(p,p.east())) h.setBlock(at,Blocks.AIR);
            if (now == 145) h.succeed();
        });
    }

    @GameTest(template = "empty", batch = "energy_foreign_full_packet", timeoutTicks = 100)
    public static void automaticFeExportWaitsUntilFullPacketIsAvailable(GameTestHelper h) {
        var p = new BlockPos(1,2,2);
        h.setBlock(p,CoreExampleMod.PACKET_SOURCE.get()); h.setBlock(p.east(),CoreExampleMod.FOREIGN_BATTERY.get());
        ComposedPacketSource source = h.getBlockEntity(p); ForeignBattery sink = h.getBlockEntity(p.east());
        source.configure(1,false); source.energyComponent().generate(31,false);
        h.runAfterDelay(35,() -> {
            h.assertTrue(sink.storage.getEnergyStored() == 0 && source.energyComponent().stored() == 31,
                "Automatic FE export bypassed the full-packet rule");
            source.energyComponent().generate(1,false);
        });
        h.runAfterDelay(65,() -> {
            h.assertTrue(sink.storage.getEnergyStored() == 128 && source.energyComponent().stored() == 0,
                "Completed full packet was not exported exactly once");
            h.setBlock(p,Blocks.AIR); h.setBlock(p.east(),Blocks.AIR);
        });
        h.runAfterDelay(85,h::succeed);
    }

    @GameTest(template = "reactor", batch = "energy_detector_sampling", timeoutTicks = 190)
    public static void detectorPollsLatestTickInsteadOfLatchingOldPulses(GameTestHelper h) {
        var p = new BlockPos(1,2,2); var wire = p.east(); var load = wire.east();
        h.setBlock(p,CoreExampleMod.SOURCE.get()); h.setBlock(wire,special("detector")); h.setBlock(load,CoreExampleMod.SINK.get());
        InheritedSource source = h.getBlockEntity(p); InheritedSink sink = h.getBlockEntity(load);
        IndependentSpecialCableBlockEntity detector = h.getBlockEntity(wire);
        int[] tick = {0};
        h.onEachTick(() -> {
            int now = ++tick[0];
            if (now == 40 || now >= 70 && now < 110) source.generate(32);
            if (now == 70) h.assertTrue(detector.signal() == 0, "An old isolated pulse was latched at the next 32-tick poll");
            if (now == 110) h.assertTrue(detector.signal() == 15, "Continuous actual transfer did not activate the detector");
            sink.getEnergyStorageInternal().setStored(0);
            if (now == 150) {
                h.assertTrue(detector.signal() == 0, "Idle detector failed to switch off");
                for (var at : List.of(p,wire,load)) h.setBlock(at,Blocks.AIR);
            }
            if (now == 175) h.succeed();
        });
    }

    @GameTest(template = "reactor", batch = "energy_splitter_switch", timeoutTicks = 145)
    public static void poweredSplitterStopsFlowAndResumesWhenSignalRemoved(GameTestHelper h) {
        var p = new BlockPos(1,2,2); var wire = p.east(); var load = wire.east();
        h.setBlock(p,CoreExampleMod.SOURCE.get()); h.setBlock(wire,special("splitter")); h.setBlock(load,CoreExampleMod.SINK.get());
        InheritedSource source = h.getBlockEntity(p); InheritedSink sink = h.getBlockEntity(load);
        int[] tick = {0};
        h.onEachTick(() -> {
            int now = ++tick[0];
            if ((now > 25 && now <= 35) || (now > 90 && now <= 110)) h.assertTrue(sink.stored() == 32, "Unpowered splitter failed to conduct continuously");
            if (now > 55 && now <= 70) h.assertTrue(sink.stored() == 0, "Redstone-powered splitter still conducted");
            if (now <= 110) { source.generate(32); sink.getEnergyStorageInternal().setStored(0); }
            if (now == 35) h.setBlock(wire.above(),Blocks.REDSTONE_BLOCK);
            if (now == 70) h.setBlock(wire.above(),Blocks.AIR);
            if (now == 110) for (var at : List.of(p,wire,load)) h.setBlock(at,Blocks.AIR);
            if (now == 135) h.succeed();
        });
    }
    private static BlockState special(String type) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.parse("mio_icif:wiring/block_eu_" + type + "_cable")).defaultBlockState();
    }
}
