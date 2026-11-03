package example.sicore;

import com.miophas.singularity_iteration.core.api.machine.IEnergyConverter;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import com.miophas.singularity_iteration.core.runtime.energy.IndependentSiEnergy;
import java.util.ArrayList;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests of 0.1.11.1 (grid phase 2): blocks of other mods are ports of the fair round (a battery of another mod gets its
 * share with SI storage, loads first in every window of the native pass, one input cap with native energy, no line loss in
 * the lossless mode, conservation), the dead band of a battery of another mod (no round trips), the grid-tie interface
 * (export, import and bidirectional within its limit, one input budget, the migration of the Energy Converter), discovery
 * of a new block within three ticks, and the Joule view of an SI machine (Mekanism) through a mock. The production default
 * is the fair distribution with no line loss: the batches here select it (the addon test server runs the legacy walk).
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round1151GameTests {
    private Round1151GameTests() {}

    private static final String DISTRIBUTION = "scex.independent.distribution", LINE_LOSS = "scex.independent.lineLoss";
    private static String lineLossBefore;

    private static void fairLossless() {
        System.setProperty(DISTRIBUTION, "fair");
        lineLossBefore = System.getProperty(LINE_LOSS);
        System.setProperty(LINE_LOSS, "lossless");
    }

    private static void restore() {
        System.setProperty(DISTRIBUTION, "legacy");
        if (lineLossBefore == null) System.clearProperty(LINE_LOSS); else System.setProperty(LINE_LOSS, lineLossBefore);
    }

    @BeforeBatch(batch = "round1151_fair") public static void fairOn(ServerLevel level) { fairLossless(); }
    @AfterBatch(batch = "round1151_fair") public static void fairOff(ServerLevel level) { restore(); }
    @BeforeBatch(batch = "round1151_lossless") public static void losslessOn(ServerLevel level) { fairLossless(); }
    @AfterBatch(batch = "round1151_lossless") public static void losslessOff(ServerLevel level) { restore(); }
    @BeforeBatch(batch = "round1151_merit") public static void meritOn(ServerLevel level) { fairLossless(); }
    @AfterBatch(batch = "round1151_merit") public static void meritOff(ServerLevel level) { restore(); }
    @BeforeBatch(batch = "round1151_deadband") public static void deadBandOn(ServerLevel level) { fairLossless(); }
    @AfterBatch(batch = "round1151_deadband") public static void deadBandOff(ServerLevel level) { restore(); }

    // ------------------------------------------------------------------ helpers

    private static Block block(String path) { return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", path)); }

    private static BlockState facing(Block block, Direction direction) {
        BlockState state = block.defaultBlockState();
        return state.getBlock().getStateDefinition().getProperty("facing") instanceof DirectionProperty facing
            && facing.getPossibleValues().contains(direction) ? state.setValue(facing, direction) : state;
    }

    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    @SuppressWarnings("deprecation")
    private static long eu(GameTestHelper h, BlockPos at) {
        return ((AbstractEnergyBlockEntity) h.getBlockEntity(at)).getEnergyStorageInternal().getAmount();
    }

    @SuppressWarnings("deprecation")
    private static double exactEu(GameTestHelper h, BlockPos at) {
        return ((AbstractEnergyBlockEntity) h.getBlockEntity(at)).getEnergyStorageInternal().scexExactAmount().toDouble();
    }

    @SuppressWarnings("deprecation")
    private static void setEu(GameTestHelper h, BlockPos at, long amount) {
        ((AbstractEnergyBlockEntity) h.getBlockEntity(at)).getEnergyStorageInternal().setStored(amount);
    }

    private static IEnergyStorage battery(GameTestHelper h, BlockPos at) { return ((ForeignBattery) h.getBlockEntity(at)).storage; }

    private static void creative(GameTestHelper h, BlockPos at, long rate) {
        h.setBlock(at, block("generator/block_creative_generator"));
        call(h.getBlockEntity(at), "configure", rate, true);
    }

    /** A grid-tie interface at {@code at} with its front toward {@code front}, {@code modeSteps} mode steps and {@code limitSteps} limit steps. */
    private static BlockEntity gridTie(GameTestHelper h, BlockPos at, Direction front, int modeSteps, int limitSteps) {
        h.setBlock(at, facing(block("energy_converter/energy_converter"), front));
        BlockEntity tie = h.getBlockEntity(at);
        for (int i = 0; i < modeSteps; i++) call(tie, "cycleMode");     // export -> import -> bidirectional 10 / 25 / 50 %
        for (int i = 0; i < limitSteps; i++) call(tie, "cycleLimit");   // 2,048 -> 32 -> 128 -> 512 EU/t
        return tie;
    }

    private static IndependentSiEnergy engine(GameTestHelper h) { return IndependentSiEnergy.current(h.getLevel().getServer()); }

    private static String role(GameTestHelper h, BlockPos at) {
        Map.Entry<String, ?> state = engine(h).foreignPortState(h.getLevel(), h.absolutePos(at));
        return state == null ? null : state.getKey();
    }

    // ------------------------------------------------------------------ (d) one fair round with SI storage

    /**
     * A battery of another mod is in the storage class of the fair round: next to an MFSU it gets half of the surplus of a
     * 64 EU/t generator (max-min share). Until 0.1.11.0 it got only what the native pass left (after every SI storage).
     */
    @GameTest(template = "empty", batch = "round1151_fair", timeoutTicks = 120)
    public static void feBatteryGetsItsFairShareWithSiStorage(GameTestHelper h) {
        BlockPos gen = new BlockPos(1, 1, 2), cable = new BlockPos(2, 1, 2), mfsu = new BlockPos(3, 1, 2), foreign = new BlockPos(2, 1, 3);
        creative(h, gen, 64);
        h.setBlock(cable, block("wiring/cable/block_superconducting_cable"));
        h.setBlock(mfsu, facing(block("wiring/block_mfsu"), Direction.EAST));   // output away from the cable
        h.setBlock(foreign, CoreExampleMod.FOREIGN_BATTERY.get());
        long[] start = new long[2];
        h.runAfterDelay(20, () -> {
            start[0] = eu(h, mfsu); start[1] = battery(h, foreign).getEnergyStored();
            h.runAfterDelay(40, () -> {
                long si = eu(h, mfsu) - start[0], fe = battery(h, foreign).getEnergyStored() - start[1], total = si + fe / 4;
                h.assertTrue(total >= 64 * 40 - 128 && total <= 64 * 40 + 128, "the generator's 2,560 EU went as " + si + " EU to the MFSU and "
                    + fe + " FE to the battery");
                h.assertTrue(si * 10 >= total * 4 && fe / 4 * 10 >= total * 4, "not a fair share: MFSU " + si + " EU, battery " + fe + " FE");
                h.assertTrue("CHARGE".equals(role(h, foreign)), "the battery's role is " + role(h, foreign));
                h.succeed();
            });
        });
    }

    /**
     * The ports of other mods' blocks are spread among the SI ports, so a window of the native pass (at most 128 receivers)
     * mixes both: 128 loads and 20 batteries of another mod on one cable plane, fed by a 32 EU/t generator. The loads take the
     * whole generation while they have room; the batteries take it once the loads are full. Appended after the SI ports (the
     * first build of 0.1.11.1), the batteries formed a window of their own and took the generation every other tick.
     */
    @GameTest(template = "reactor_loop", batch = "round1151_fair", timeoutTicks = 200)
    public static void loadsComeFirstInEveryWindow(GameTestHelper h) {
        var sinks = new ArrayList<BlockPos>(); var batteries = new ArrayList<BlockPos>();
        creative(h, new BlockPos(1, 1, 1), 32);
        for (int x = 1; x <= 11; x++) for (int z = 1; z <= 11; z++) {
            h.setBlock(new BlockPos(x, 2, z), block("wiring/cable/block_superconducting_cable"));
            BlockPos above = new BlockPos(x, 3, z);
            h.setBlock(above, CoreExampleMod.SINK.get()); sinks.add(above);
        }
        // below the plane, on odd columns only (no two of them side by side): 20 batteries, then 7 more loads
        for (int x = 1; x <= 11; x += 2) for (int z = 1; z <= 11; z += 2) {
            if (x == 1 && z == 1) continue;
            BlockPos below = new BlockPos(x, 1, z);
            if (batteries.size() < 20) { h.setBlock(below, CoreExampleMod.FOREIGN_BATTERY.get()); batteries.add(below); }
            else if (sinks.size() < 128) { h.setBlock(below, CoreExampleMod.SINK.get()); sinks.add(below); }
        }
        double[] loads0 = new double[1]; long[] fe0 = new long[1];
        h.runAfterDelay(30, () -> {
            for (BlockPos at : batteries) h.assertTrue("CHARGE".equals(role(h, at)), "battery " + at + " is no port of the grid: " + role(h, at));
            for (BlockPos at : sinks) loads0[0] += exactEu(h, at);
            for (BlockPos at : batteries) fe0[0] += battery(h, at).getEnergyStored();
            h.runAfterDelay(60, () -> {
                double loads = -loads0[0]; long fe = -fe0[0];
                for (BlockPos at : sinks) loads += exactEu(h, at);
                for (BlockPos at : batteries) fe += battery(h, at).getEnergyStored();
                h.assertTrue(fe == 0, "the batteries got " + fe + " FE in 60 ticks while the loads had room (the loads got " + loads + " EU)");
                h.assertTrue(Math.abs(loads - 32 * 60) <= 64, "the loads got " + loads + " EU in 60 ticks, not 1,920");
                for (BlockPos at : sinks) setEu(h, at, 1024);
                long full = fe0[0] + fe;
                h.runAfterDelay(20, () -> {
                    long after = -full;
                    for (BlockPos at : batteries) after += battery(h, at).getEnergyStored();
                    h.assertTrue(after > 0, "the batteries got nothing once the loads were full");
                    h.succeed();
                });
            });
        });
    }

    /**
     * A full battery of another mod supplies loads in the same round as SI sources, in merit order: a generator first (16
     * EU/t), the battery after it, up to the room of the load (1,024 EU, like an SI storage block). At 10 % it stops
     * (dead band) and stays stopped. The engine's account of taken FE equals what the battery lost.
     */
    @GameTest(template = "empty", batch = "round1151_merit", timeoutTicks = 100)
    public static void foreignBatterySuppliesLoadsAfterGeneratorsUntilItsLowerEdge(GameTestHelper h) {
        BlockPos src = new BlockPos(1, 1, 2), cable = new BlockPos(2, 1, 2), snk = new BlockPos(3, 1, 2), foreign = new BlockPos(2, 1, 3);
        h.setBlock(src, CoreExampleMod.SOURCE.get());
        h.setBlock(cable, block("wiring/cable/block_tin_cable_1"));
        h.setBlock(snk, CoreExampleMod.SINK.get());
        h.setBlock(foreign, CoreExampleMod.FOREIGN_BATTERY.get());
        battery(h, foreign).receiveEnergy(100_000, false);
        long taken0 = engine(h).foreignTotals(h.getLevel()).takenFe();
        InheritedSource source = h.getBlockEntity(src); InheritedSink sink = h.getBlockEntity(snk);
        int[] tick = {0}, supplied = {0}, stop = {-1}, odd = {0}; long[] atStop = {-1}; var trace = new StringBuilder();
        h.onEachTick(() -> {
            int now = ++tick[0];
            long got = sink.stored(), fe = battery(h, foreign).getEnergyStored();
            if (now >= 3 && now <= 60) {
                trace.append(got).append(' ');
                if (got > 16) { supplied[0]++; if (got != 1024 || stop[0] >= 0) odd[0]++; }
                else if (supplied[0] > 0 && stop[0] < 0) { stop[0] = now; atStop[0] = fe; }
                if (stop[0] >= 0 && fe != atStop[0]) odd[0]++;
            }
            if (now <= 65) setEu(h, snk, 0);
            if (now <= 62) source.generate(16);
            if (now == 61) {
                h.assertTrue(supplied[0] > 0 && stop[0] > 0, "the battery never supplied or never stopped: " + trace);
                h.assertTrue(odd[0] == 0, "the load got other than its room (1,024 EU) from the battery, or the battery changed after it stopped: " + trace);
                h.assertTrue(atStop[0] <= 10_000, "the battery stopped at " + atStop[0] + " FE, above 10 %");
                h.assertTrue("CHARGE".equals(role(h, foreign)), "the drained battery's role is " + role(h, foreign));
                long taken = engine(h).foreignTotals(h.getLevel()).takenFe() - taken0;
                h.assertTrue(taken == 100_000 - fe, "the engine took " + taken + " FE, the battery lost " + (100_000 - fe));
            }
            if (now == 66) h.succeed();
        });
    }

    // ------------------------------------------------------------------ (i) lossless FE, conservation

    /**
     * Lossless mode: a 32 EU/t generator feeds a battery of another mod through 9 tin cables; the battery gets 128 FE a tick
     * (no line loss), and the engine's account of handed FE equals what the battery got (nothing is dropped).
     */
    @GameTest(template = "reactor_loop", batch = "round1151_lossless", timeoutTicks = 200)
    public static void losslessFeDeliveryKeepsEveryJoule(GameTestHelper h) {
        BlockPos gen = new BlockPos(1, 2, 6), foreign = new BlockPos(11, 2, 6);
        creative(h, gen, 32);
        for (int x = 2; x <= 10; x++) h.setBlock(new BlockPos(x, 2, 6), block("wiring/cable/block_tin_cable_1"));
        h.setBlock(foreign, CoreExampleMod.FOREIGN_BATTERY.get());
        long[] start = new long[2]; Object[] totals = new Object[1];
        h.runAfterDelay(30, () -> {
            var t = engine(h).foreignTotals(h.getLevel());
            start[0] = battery(h, foreign).getEnergyStored(); start[1] = t.handedFe(); totals[0] = t.dropped();
            h.runAfterDelay(100, () -> {
                var now = engine(h).foreignTotals(h.getLevel());
                long gained = battery(h, foreign).getEnergyStored() - start[0], handed = now.handedFe() - start[1];
                h.assertTrue(gained >= 100 * 128 - 128 && gained <= 100 * 128 + 128, "the battery got " + gained + " FE in 100 ticks, not 12,800 (line loss?)");
                h.assertTrue(handed == gained, "the engine handed " + handed + " FE, the battery got " + gained);
                h.assertTrue(now.dropped().equals(totals[0]), "foreign energy was dropped: " + totals[0] + " -> " + now.dropped());
                h.succeed();
            });
        });
    }

    // ------------------------------------------------------------------ (f) dead band, no round trips

    /**
     * A battery of another mod charges from an MFSU until it is 90 % full, then holds (no load); it gives nothing back to
     * the MFSU. With the MFSU emptied and a load added, the battery feeds the load and still charges no SI storage: the MFSU
     * stays empty. Until 0.1.11.0 the import pass moved its FE back into the MFSU and the export pass out again.
     */
    @GameTest(template = "empty", batch = "round1151_deadband", timeoutTicks = 140)
    public static void deadBandMeansNoRoundTrips(GameTestHelper h) {
        BlockPos mfsu = new BlockPos(1, 1, 2), cable = new BlockPos(2, 1, 2), foreign = new BlockPos(3, 1, 2), load = new BlockPos(2, 1, 1);
        h.setBlock(mfsu, facing(block("wiring/block_mfsu"), Direction.EAST));   // output into the cable
        h.setBlock(cable, block("wiring/cable/block_superconducting_cable"));
        h.setBlock(foreign, CoreExampleMod.FOREIGN_BATTERY.get());
        setEu(h, mfsu, 1_000_000);
        int[] tick = {0}, mfsuGains = {0}, batteryGains = {0}; long[] last = {Long.MAX_VALUE, -1}, taken = {0}, fe40 = {0};
        h.onEachTick(() -> {
            int now = ++tick[0];
            long m = eu(h, mfsu), b = battery(h, foreign).getEnergyStored();
            if (now > 2 && m > last[0]) mfsuGains[0]++;
            if (now > 41 && b > last[1]) batteryGains[0]++;
            last[0] = m; last[1] = b;
            if (now == 5) taken[0] = engine(h).foreignTotals(h.getLevel()).takenFe();
            if (now == 40) {
                h.assertTrue(b >= 90_000, "the battery stopped at " + b + " FE, below 90 %");
                h.assertTrue("DISCHARGE".equals(role(h, foreign)), "the full battery's role is " + role(h, foreign));
                h.assertTrue(engine(h).foreignTotals(h.getLevel()).takenFe() == taken[0], "FE was taken from the battery with no load");
                h.assertTrue(mfsuGains[0] == 0, "the MFSU gained energy in " + mfsuGains[0] + " ticks");
                setEu(h, mfsu, 0);
                last[0] = 0;
                h.setBlock(load, CoreExampleMod.SINK.get());
                fe40[0] = b;
            }
            if (now == 100) {
                long fed = ((InheritedSink) h.getBlockEntity(load)).stored();
                h.assertTrue(fed > 0, "the battery fed no load");
                h.assertTrue(m == 0 && mfsuGains[0] == 0, "the battery charged the MFSU (" + m + " EU, " + mfsuGains[0] + " ticks)");
                h.assertTrue(batteryGains[0] == 0, "the battery charged again in " + batteryGains[0] + " ticks");
                long diff = fe40[0] - b - 4 * fed;   // the load may hold a part of an EU (getAmount is whole EU)
                h.assertTrue(diff >= 0 && diff < 4, "the battery gave " + (fe40[0] - b) + " FE for " + fed + " EU");
                h.succeed();
            }
        });
    }

    // ------------------------------------------------------------------ (g) the grid-tie interface

    /**
     * Export at 128 EU/t: a generator's surplus goes through the interface to a battery of another mod in front of it, 512 FE
     * a tick. The block in front is not a contact of the grid (only the interface reaches it).
     */
    @GameTest(template = "empty", batch = "round1151_fair", timeoutTicks = 100)
    public static void gridTieExportsWithinItsLimit(GameTestHelper h) {
        BlockPos gen = new BlockPos(0, 1, 2), cable = new BlockPos(1, 1, 2), tie = new BlockPos(2, 1, 2), foreign = new BlockPos(3, 1, 2);
        creative(h, gen, 2048);
        h.setBlock(cable, block("wiring/cable/block_superconducting_cable"));
        gridTie(h, tie, Direction.EAST, 0, 2);
        h.setBlock(foreign, CoreExampleMod.FOREIGN_BATTERY.get());
        long[] start = new long[1];
        h.runAfterDelay(30, () -> {
            start[0] = battery(h, foreign).getEnergyStored();
            h.runAfterDelay(40, () -> {
                long gained = battery(h, foreign).getEnergyStored() - start[0];
                h.assertTrue(gained >= 40 * 512 - 1024 && gained <= 40 * 512 + 512, "the battery got " + gained + " FE in 40 ticks, not 20,480");
                h.assertTrue(role(h, foreign) == null, "the block in front of the interface is a grid contact (" + role(h, foreign) + ")");
                h.succeed();
            });
        });
    }

    /** Import at 32 EU/t: a full battery of another mod in front of the interface feeds an MFSU, 4 FE per EU, nothing lost. */
    @GameTest(template = "empty", batch = "round1151_fair", timeoutTicks = 100)
    public static void gridTieImportsWithinItsLimit(GameTestHelper h) {
        BlockPos foreign = new BlockPos(0, 1, 2), tie = new BlockPos(1, 1, 2), cable = new BlockPos(2, 1, 2), mfsu = new BlockPos(3, 1, 2);
        h.setBlock(foreign, CoreExampleMod.FOREIGN_BATTERY.get());
        battery(h, foreign).receiveEnergy(100_000, false);
        BlockEntity interfaceEntity = gridTie(h, tie, Direction.WEST, 1, 1);
        h.setBlock(cable, block("wiring/cable/block_tin_cable_1"));
        h.setBlock(mfsu, facing(block("wiring/block_mfsu"), Direction.EAST));   // output away from the cable
        long[] start = new long[3];
        h.runAfterDelay(20, () -> {
            start[0] = battery(h, foreign).getEnergyStored(); start[1] = eu(h, mfsu); start[2] = ((IEnergyConverter) interfaceEntity).getBufferAmount();
            h.runAfterDelay(40, () -> {
                long lost = start[0] - battery(h, foreign).getEnergyStored(), gained = eu(h, mfsu) - start[1];
                long buffered = ((IEnergyConverter) interfaceEntity).getBufferAmount() - start[2];
                h.assertTrue(gained >= 40 * 32 - 64 && gained <= 40 * 32 + 32, "the MFSU got " + gained + " EU in 40 ticks, not 1,280");
                long diff = lost - 4 * (gained + buffered);   // whole EU read, so up to 3 FE of a part of an EU
                h.assertTrue(diff >= 0 && diff < 4, "the battery gave " + lost + " FE; the MFSU got " + gained + " EU, the buffer " + buffered);
                h.assertTrue(!h.getBlockState(cable).isAir(), "the tin cable melted");
                h.succeed();
            });
        });
    }

    /**
     * Bidirectional with a 10 % band (90,000 to 110,000 EU). At 32 EU/t the SI input (a generator's surplus) and the FE
     * pushed into the front share one input budget: 32 EU a tick together. At 2,048 EU/t without SI input, a buffer of
     * 70,000 EU takes FE from a full battery in front up to the lower edge exactly; a buffer of 120,000 EU gives FE out of
     * the front down to the upper edge exactly. Between the edges the front moves nothing.
     */
    @GameTest(template = "empty", batch = "round1151_fair", timeoutTicks = 200)
    public static void gridTieBidirectionalSharesOneBudgetAndKeepsItsBand(GameTestHelper h) {
        BlockPos gen = new BlockPos(0, 1, 2), cable = new BlockPos(1, 1, 2), tie = new BlockPos(2, 1, 2), foreign = new BlockPos(3, 1, 2);
        creative(h, gen, 2048);
        h.setBlock(cable, block("wiring/cable/block_superconducting_cable"));
        BlockEntity interfaceEntity = gridTie(h, tie, Direction.EAST, 2, 1);   // bidirectional 10 %, 32 EU/t
        h.setBlock(foreign, CoreExampleMod.FOREIGN_BATTERY.get());
        IEnergyConverter converter = (IEnergyConverter) interfaceEntity;
        IEnergyStorage front = (IEnergyStorage) call(interfaceEntity, "frontFe", Direction.EAST);
        h.assertTrue(front != null && call(interfaceEntity, "frontFe", Direction.WEST) == null, "the FE face is not the front only");
        int[] tick = {0}; long[] start = {0};
        h.onEachTick(() -> {
            int now = ++tick[0];
            if (now >= 10 && now < 50) front.receiveEnergy(1_000_000, false);
            if (now == 10) start[0] = converter.getBufferAmount();
            if (now == 50) {
                long gained = converter.getBufferAmount() - start[0];
                h.assertTrue(gained >= 40 * 32 - 64 && gained <= 40 * 32 + 32, "the buffer gained " + gained + " EU in 40 ticks with SI and FE input, not 1,280");
                h.setBlock(gen, net.minecraft.world.level.block.Blocks.AIR);
                call(interfaceEntity, "setLimit", 2048L);
                setEu(h, tie, 70_000);
                battery(h, foreign).extractEnergy(1_000_000, false);
                battery(h, foreign).receiveEnergy(100_000, false);
            }
            if (now == 100) {
                h.assertTrue(converter.getBufferAmount() == 90_000 && battery(h, foreign).getEnergyStored() == 20_000, "below the band: the buffer is at "
                    + converter.getBufferAmount() + " EU (not 90,000), the battery at " + battery(h, foreign).getEnergyStored() + " FE (not 20,000)");
                h.assertTrue(front.receiveEnergy(1_000, true) == 0, "the front takes FE at the lower edge");
                setEu(h, tie, 120_000);
            }
            if (now == 150) {
                h.assertTrue(converter.getBufferAmount() == 110_000 && battery(h, foreign).getEnergyStored() == 60_000, "above the band: the buffer is at "
                    + converter.getBufferAmount() + " EU (not 110,000), the battery at " + battery(h, foreign).getEnergyStored() + " FE (not 60,000)");
                h.assertTrue(front.receiveEnergy(1_000, true) == 0 && front.extractEnergy(1_000, true) == 0, "the front moves FE inside the band");
                h.succeed();
            }
        });
    }

    /**
     * An Energy Converter of 0.1.11.0 loads as a grid-tie interface: Mode true (FE to EU) is import, its FE buffer joins the
     * EU buffer in whole EU, and the settings survive a save.
     */
    @GameTest(template = "empty", batch = "round1151_fair", timeoutTicks = 20)
    public static void energyConverterMigratesToTheGridTie(GameTestHelper h) {
        var registries = h.getLevel().registryAccess();
        BlockState state = facing(block("energy_converter/energy_converter"), Direction.EAST);
        BlockEntity old = detachedGridTie(h.absolutePos(new BlockPos(2, 1, 2)), state);
        CompoundTag legacy = new CompoundTag();
        legacy.putBoolean("Mode", true);
        legacy.putInt("FEStored", 4_003);
        legacy.putLong("energy", 1_000L);
        call(old, "loadAdditional", legacy, registries);
        h.assertTrue("IMPORT".equals(String.valueOf(call(old, "mode"))), "Mode true loaded as " + call(old, "mode"));
        h.assertTrue(((IEnergyConverter) old).getBufferAmount() == 2_000, "the buffer is " + ((IEnergyConverter) old).getBufferAmount() + " EU, not 1,000 + 4,003 / 4");
        call(old, "cycleMode"); call(old, "cycleMode"); call(old, "cycleLimit");   // bidirectional 25 %, 32 EU/t
        CompoundTag saved = old.saveWithoutMetadata(registries);
        h.assertTrue(!saved.contains("FEStored") && "BIDIRECTIONAL".equals(saved.getString("GridTieMode")), "save: " + saved);
        BlockEntity copy = detachedGridTie(h.absolutePos(new BlockPos(2, 1, 3)), state);
        call(copy, "loadAdditional", saved, registries);
        h.assertTrue("BIDIRECTIONAL".equals(String.valueOf(call(copy, "mode"))) && (Integer) call(copy, "band") == 25
            && (Long) call(copy, "limit") == 32L, "loaded " + call(copy, "mode") + " " + call(copy, "band") + " % " + call(copy, "limit") + " EU/t");
        h.succeed();
    }

    /** A grid-tie block entity outside the world (no grid, no engine): only its load and save are tested. */
    private static BlockEntity detachedGridTie(BlockPos at, BlockState state) {
        try {
            return (BlockEntity) Round33GameTests.common("blockentity.energy_converter.mio_icif_energy_converter_entity")
                .getConstructor(BlockPos.class, BlockState.class).newInstance(at, state);
        } catch (ReflectiveOperationException e) { throw new AssertionError("the grid-tie block entity", e); }
    }

    // ------------------------------------------------------------------ discovery, Joule view

    /** A battery of another mod placed next to a live cable gets FE within three ticks. */
    @GameTest(template = "empty", batch = "round1151_fair", timeoutTicks = 60)
    public static void newForeignBlockIsServedWithinThreeTicks(GameTestHelper h) {
        BlockPos gen = new BlockPos(1, 1, 2), cable = new BlockPos(2, 1, 2), foreign = new BlockPos(3, 1, 2);
        creative(h, gen, 32);
        h.setBlock(cable, block("wiring/cable/block_superconducting_cable"));
        int[] tick = {0}, served = {-1};
        h.onEachTick(() -> {
            int now = ++tick[0];
            if (now == 10) h.setBlock(foreign, CoreExampleMod.FOREIGN_BATTERY.get());
            if (now > 10 && served[0] < 0 && battery(h, foreign).getEnergyStored() > 0) served[0] = now - 10;
            if (now == 20) {
                h.assertTrue(served[0] >= 0 && served[0] <= 3, "the new battery got FE " + served[0] + " ticks after it was placed");
                h.succeed();
            }
        });
    }

    /**
     * The Joule view of an SI machine (Mekanism's strict energy) through a mock at 10 J per EU: 200 inserts of 3 J arrive in
     * full (60 EU; until 0.1.11.0 each was refused, being below one EU), and 100 extracts of 7 J give 700 J for exactly
     * 70 EU. Nothing stays saved after whole EU.
     */
    @GameTest(template = "empty", batch = "round1151_fair", timeoutTicks = 340)
    public static void jouleViewKeepsTheFractionThroughAMock(GameTestHelper h) {
        BlockPos at = new BlockPos(2, 1, 2);
        h.setBlock(at, facing(block("wiring/block_mfsu"), Direction.EAST));
        AbstractEnergyBlockEntity machine = h.getBlockEntity(at);
        setEu(h, at, 1_000);
        Object in, out;
        try {
            var view = Round33GameTests.common("energy.MekanismStrictEnergyBridge$BlockView")
                .getDeclaredConstructor(AbstractEnergyBlockEntity.class, Direction.class, IEnergyStorage.class);
            view.setAccessible(true);
            in = view.newInstance(machine, Direction.WEST, machine.scexFeCapability(Direction.WEST));
            out = view.newInstance(machine, Direction.EAST, machine.scexFeCapability(Direction.EAST));
        } catch (ReflectiveOperationException e) { throw new AssertionError("the Joule view of the bridge", e); }
        int[] tick = {0}; long[] refused = {0}, given = {0};
        h.onEachTick(() -> {
            int now = ++tick[0];
            if (now <= 200) refused[0] += (Long) call(in, "transferJ", 3L, true, false, 10L);
            if (now == 201) {
                h.assertTrue(refused[0] == 0 && eu(h, at) == 1_060, "200 x 3 J: " + refused[0] + " J refused, the MFSU holds " + eu(h, at) + " EU");
                h.assertTrue(machine.scexFeBridge().joules().saved() == 0, machine.scexFeBridge().joules().saved() + " J stayed saved after 600 J");
            }
            if (now > 201 && now <= 301) given[0] += (Long) call(out, "transferJ", 7L, false, false, 10L);
            if (now == 302) {
                h.assertTrue(given[0] == 700 && eu(h, at) == 990, "100 x 7 J: " + given[0] + " J given, the MFSU holds " + eu(h, at) + " EU, not 990");
                h.succeed();
            }
        });
    }
}
