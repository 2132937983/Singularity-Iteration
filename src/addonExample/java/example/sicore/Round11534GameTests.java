package example.sicore;

import com.miophas.singularity_iteration.core.platform.neoforge.energy.IndependentSpecialCableBlockEntity;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import com.miophas.singularity_iteration.core.runtime.energy.IndependentSiEnergy;
import com.miophas.singularity_iteration.core.runtime.energy.engine.ProtectionRelay;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.34 (cable audit W5): protective cables start with no rated flow and an automatic rated voltage (the line's
 * packet limit), old saves with the old defaults load that way, and an overvoltage opens the cable before the commit.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11534GameTests {
    private Round11534GameTests() {}

    private static Block block(String path) { return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", path)); }
    private static BlockState facing(String id, Direction side) {
        var state = block(id).defaultBlockState();
        return state.setValue((DirectionProperty) state.getBlock().getStateDefinition().getProperty("facing"), side);
    }

    /** New relays: flow not set (no overload element), voltage automatic; the old saved defaults load the same way. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void defaultsAndOldSaves(GameTestHelper h) {
        for (var kind : ProtectionRelay.Kind.values()) {
            var relay = new ProtectionRelay(kind);
            h.assertTrue(relay.flowUnset() && relay.ratedVoltage() == ProtectionRelay.VOLTAGE_AUTO, kind + " defaults");
            for (long t = 0; t < 40; t++) { relay.carry(t, 1_000_000, 32); h.assertTrue(relay.evaluate(t) == ProtectionRelay.Event.NONE, kind + " opened for flow without a rating"); }
            relay.setLineVoltage(32);
            h.assertTrue(relay.effectiveVoltage() == 32 && relay.voltageExceeded(33) && !relay.voltageExceeded(32), kind + " automatic voltage");
            relay.configure(0, ProtectionRelay.VOLTAGE_OFF, 5, false);
            h.assertTrue(!relay.voltageExceeded(1_000_000), kind + " voltage off");
        }
        // the old format (9 values): the old defaults become not set / automatic; a chosen flow stays and its 0 V stays off
        var fuse = new ProtectionRelay(ProtectionRelay.Kind.FUSE);
        fuse.load(new long[]{128, 0, 5, 0, 0, 0, 0, 0, 0});
        h.assertTrue(fuse.flowUnset() && fuse.ratedVoltage() == ProtectionRelay.VOLTAGE_AUTO, "old default fuse");
        var breaker = new ProtectionRelay(ProtectionRelay.Kind.BREAKER);
        breaker.load(new long[]{2000, 0, 20, 1, 0, 0, 0, 0, 0});
        h.assertTrue(breaker.ratedFlow() == 2000 && breaker.ratedVoltage() == ProtectionRelay.VOLTAGE_OFF, "configured old breaker");
        var again = new ProtectionRelay(ProtectionRelay.Kind.BREAKER);
        again.load(breaker.save());
        h.assertTrue(again.ratedFlow() == 2000 && again.ratedVoltage() == ProtectionRelay.VOLTAGE_OFF, "new format round trip");
        h.succeed();
    }

    /**
     * MFE -> glass fibre -> breaker -> tin -> LV machine: the breaker follows the tin line (32 EU), opens for the 512 EU
     * packet before the commit, and the machine gets nothing (it got the packet before; only the explosion was held back).
     */
    @GameTest(template = "empty", batch = "round11534_hold", timeoutTicks = 80)
    public static void overvoltageOpensBeforeTheCommit(GameTestHelper h) {
        BlockPos source = new BlockPos(0, 2, 1), glass = source.east(), breakerPos = glass.east(), tin = breakerPos.east(), sinkPos = tin.east();
        h.setBlock(source, facing("wiring/block_mfe", Direction.EAST));
        h.setBlock(glass, block("wiring/cable/block_glass_cable"));
        h.setBlock(breakerPos, block("wiring/block_eu_breaker_cable"));
        h.setBlock(tin, block("wiring/cable/block_tin_cable_1"));
        h.setBlock(sinkPos, CoreExampleMod.SINK.get());
        InheritedSink sink = (InheritedSink) h.getBlockEntity(sinkPos);
        var breaker = (IndependentSpecialCableBlockEntity) h.getBlockEntity(breakerPos);
        var engine = IndependentSiEnergy.current(h.getLevel().getServer());
        long holds = engine.protectionHolds();
        ((AbstractEnergyBlockEntity) h.getBlockEntity(source)).getEnergyStorageInternal().setStored(100_000);
        h.onEachTick(() -> h.assertTrue(sink.stored() == 0, "the machine got energy through the overvoltage: " + sink.stored()
            + " breaker " + breaker.relay().state() + " " + breaker.relay().lastCause() + " line " + breaker.relay().lineVoltage()
            + " holds " + (engine.protectionHolds() - holds) + " flow " + breaker.relay().lastFlow() + " volts " + breaker.relay().lastVoltage()));
        h.succeedWhen(() -> {
            var relay = breaker.relay();
            h.assertTrue(relay.state() != ProtectionRelay.State.CLOSED && relay.lastCause() == ProtectionRelay.Cause.OVERVOLTAGE,
                "breaker " + relay.state() + " / " + relay.lastCause() + " line " + relay.lineVoltage());
            h.assertTrue(relay.lineVoltage() == 32, "line voltage " + relay.lineVoltage());
            h.assertTrue(engine.protectionHolds() > holds, "no window was held");
            h.assertTrue(h.getBlockState(tin).is(block("wiring/cable/block_tin_cable_1")) && h.getBlockEntity(sinkPos) == sink, "the line behind it was hurt");
        });
    }

    /**
     * 0.1.11.34: a cable placed next to a working line does not cut it for 40 ticks (the E-3 reactivation delay of
     * 0.1.11.32 also took a chunk that was not ready in the topology for a moment, as after any cable edit).
     */
    @GameTest(template = "empty", batch = "round11534_edit", timeoutTicks = 80)
    public static void cableEditDoesNotCutTheLine(GameTestHelper h) {
        BlockPos source = new BlockPos(1, 2, 1), sinkPos = new BlockPos(5, 2, 1);
        h.setBlock(source, CoreExampleMod.SOURCE.get());
        for (int x = 2; x <= 4; x++) h.setBlock(new BlockPos(x, 2, 1), block("wiring/cable/block_tin_cable"));
        h.setBlock(sinkPos, CoreExampleMod.SINK.get());
        InheritedSource src = (InheritedSource) h.getBlockEntity(source);
        InheritedSink sink = (InheritedSink) h.getBlockEntity(sinkPos);
        int[] tick = {0}, fed = {0};
        h.onEachTick(() -> {
            int now = ++tick[0];
            src.generate(32);
            if (now == 20) h.setBlock(new BlockPos(3, 3, 1), block("wiring/cable/block_tin_cable"));
            if (now >= 26 && now <= 45 && sink.stored() > 0) fed[0]++;
            sink.getEnergyStorageInternal().setStored(0);
            if (now == 46) {
                h.assertTrue(fed[0] >= 18, "the line was cut after a cable edit: fed in " + fed[0] + " of 20 ticks");
                h.succeed();
            }
        });
    }
}
