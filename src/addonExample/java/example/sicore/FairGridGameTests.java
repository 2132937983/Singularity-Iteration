package example.sicore;

import com.miophas.singularity_iteration.core.platform.neoforge.energy.IndependentTransformerBlockEntity;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import com.miophas.singularity_iteration.core.runtime.energy.engine.IndependentEnergyMode;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests of the 0.1.8.0 fair-share distribution (smart grid mode).
 *
 * <p>The test server starts in legacy mode, so that the older engine tests
 * keep their IC2-style expectations. The batches of this class select the
 * fair mode before they start and select legacy mode again after they end.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class FairGridGameTests {
    private static final String KEY = "scex.independent.distribution";

    @BeforeBatch(batch = "fair_grid")
    public static void fairOn(ServerLevel level) { System.setProperty(KEY, "fair"); }

    @AfterBatch(batch = "fair_grid")
    public static void fairOff(ServerLevel level) { System.setProperty(KEY, "legacy"); }

    @BeforeBatch(batch = "fair_grid_transformer")
    public static void fairTransformerOn(ServerLevel level) { System.setProperty(KEY, "fair"); }

    @AfterBatch(batch = "fair_grid_transformer")
    public static void fairTransformerOff(ServerLevel level) { System.setProperty(KEY, "legacy"); }

    /**
     * One BatBox feeds three electric furnaces on one tin cable, at distances
     * 1, 3 and 5. The legacy walk gives the first furnace the full packet. The
     * fair share gives each furnace one third of the 32 EU per tick.
     */
    @GameTest(template = "reactor_loop", batch = "fair_grid", timeoutTicks = 120)
    public static void seriesFurnacesGetEqualShares(GameTestHelper h) {
        h.assertTrue(IndependentEnergyMode.fairDistribution(), "Fair batch did not select the fair distributor");
        var furnaces = line(h, 3, new int[]{1, 3, 5});
        // A furnace holds 300 EU. Empty all three after the topology settles,
        // then measure 10 ticks: about 107 EU each, below the capacity.
        h.runAfterDelay(20, () -> { for (var furnace : furnaces) furnace.getEnergyStorageInternal().setStored(0); });
        h.runAfterDelay(30, () -> {
            long[] gained = new long[3];
            for (int i = 0; i < 3; i++) gained[i] = amount(furnaces.get(i));
            long min = Math.min(gained[0], Math.min(gained[1], gained[2]));
            long max = Math.max(gained[0], Math.max(gained[1], gained[2]));
            h.assertTrue(min > 0, "A furnace got no energy: " + java.util.Arrays.toString(gained));
            h.assertTrue(max - min <= 2, "Series furnaces did not get equal shares: " + java.util.Arrays.toString(gained));
            long total = gained[0] + gained[1] + gained[2];
            h.assertTrue(Math.abs(total - 10L * 32) <= 3, "The BatBox did not deliver 32 EU per tick: " + total);
            clear(h, 3);
        });
        h.runAfterDelay(50, h::succeed);
    }

    /** A full (idle) furnace does not take a share. The other furnaces get 16 EU per tick each. */
    @GameTest(template = "reactor_loop", batch = "fair_grid", timeoutTicks = 120)
    public static void idleFurnaceDoesNotAttenuateOthers(GameTestHelper h) {
        var furnaces = line(h, 9, new int[]{1, 3, 5});
        var idle = furnaces.get(0).getEnergyStorageInternal();
        h.runAfterDelay(20, () -> {
            idle.setStored(idle.getCapacity());
            furnaces.get(1).getEnergyStorageInternal().setStored(0);
            furnaces.get(2).getEnergyStorageInternal().setStored(0);
        });
        h.runAfterDelay(30, () -> {
            long second = amount(furnaces.get(1)), third = amount(furnaces.get(2));
            h.assertTrue(Math.abs(second - third) <= 2, "Furnaces after an idle furnace were attenuated: " + second + " / " + third);
            h.assertTrue(Math.abs(second + third - 10L * 32) <= 3, "Idle furnace absorbed energy: " + (second + third));
            clear(h, 9);
        });
        h.runAfterDelay(50, h::succeed);
    }

    /** Loads get energy before storage: an MFE on the same cable charges only after the furnace is full. */
    @GameTest(template = "reactor_loop", batch = "fair_grid", timeoutTicks = 120)
    public static void loadsBeforeStorage(GameTestHelper h) {
        int z = 15;
        BlockPos source = new BlockPos(0, 2, z);
        h.setBlock(source, facing("wiring/block_bat_box", Direction.EAST));
        for (int x = 1; x <= 3; x++) h.setBlock(new BlockPos(x, 2, z), block("wiring/cable/block_tin_cable"));
        h.setBlock(new BlockPos(1, 2, z + 1), block("producer/block_furnace_elc"));
        h.setBlock(new BlockPos(3, 2, z + 1), facing("wiring/block_mfe", Direction.UP));
        AbstractEnergyBlockEntity batbox = h.getBlockEntity(source);
        batbox.getEnergyStorageInternal().setStored(batbox.getEnergyStorageInternal().getCapacity());
        AbstractEnergyBlockEntity furnace = h.getBlockEntity(new BlockPos(1, 2, z + 1));
        AbstractEnergyBlockEntity storage = h.getBlockEntity(new BlockPos(3, 2, z + 1));
        h.runAfterDelay(4, () -> { furnace.getEnergyStorageInternal().setStored(0); storage.getEnergyStorageInternal().setStored(0); });
        h.runAfterDelay(9, () -> {
            boolean furnaceFull = furnace.getEnergyStorageInternal().getAmount() >= furnace.getEnergyStorageInternal().getCapacity();
            if (!furnaceFull) h.assertTrue(storage.getEnergyStorageInternal().getAmount() == 0,
                "Storage charged before the load was full");
        });
        h.runAfterDelay(60, () -> {
            for (int x = 0; x <= 3; x++) h.setBlock(new BlockPos(x, 2, z), Blocks.AIR);
            h.setBlock(new BlockPos(1, 2, z + 1), Blocks.AIR); h.setBlock(new BlockPos(3, 2, z + 1), Blocks.AIR);
        });
        h.runAfterDelay(80, h::succeed);
    }

    /** A full 2048 EU packet still destroys a 512 EU MFE in fair mode. */
    @GameTest(template = "empty", batch = "fair_grid", timeoutTicks = 60)
    public static void overvoltageStillDestroysLowTier(GameTestHelper h) {
        BlockPos sourcePos = new BlockPos(1, 1, 21), sinkPos = sourcePos.east();
        h.setBlock(sourcePos, facing("wiring/block_mfsu", Direction.EAST));
        h.setBlock(sinkPos, facing("wiring/block_mfe", Direction.EAST));
        AbstractEnergyBlockEntity source = h.getBlockEntity(sourcePos);
        source.getEnergyStorageInternal().setStored(100_000);
        h.runAfterDelay(20, () -> {
            h.assertTrue(h.getLevel().getBlockEntity(h.absolutePos(sinkPos)) == null, "A 2048 EU packet no longer destroys an MFE");
            h.setBlock(sourcePos, Blocks.AIR); h.setBlock(sinkPos, Blocks.AIR);
        });
        h.runAfterDelay(32, h::succeed);
    }

    /** Step-down HV/EV: an MFSU feeds an MFE through the transformer with 4 x 512 EU per tick. */
    @GameTest(template = "reactor_loop", batch = "fair_grid_transformer", timeoutTicks = 100)
    public static void stepDownPassesFourLowPacketsPerTick(GameTestHelper h) {
        var at = new BlockPos(3, 3, 3);
        h.setBlock(at.west(), facing("wiring/block_mfsu", Direction.EAST));
        h.setBlock(at, facing("wiring/transformer_hv_ev", Direction.WEST));
        h.setBlock(at.east(), facing("wiring/block_mfe", Direction.EAST));
        AbstractEnergyBlockEntity source = h.getBlockEntity(at.west());
        source.getEnergyStorageInternal().setStored(1_000_000);
        AbstractEnergyBlockEntity sink = h.getBlockEntity(at.east());
        IndependentTransformerBlockEntity transformer = h.getBlockEntity(at);
        transformer.setSavedMode(1);
        long[] previous = {0}; int[] tick = {0};
        h.onEachTick(() -> {
            int now = ++tick[0];
            if (now >= 20 && now <= 60) {
                long current = sink.getEnergyStorageInternal().getAmount();
                if (now > 20) h.assertTrue(current - previous[0] == 2048, "Fair step-down did not pass 2048 EU per tick: " + (current - previous[0]));
                previous[0] = current;
            }
            if (now == 60) { h.setBlock(at.west(), Blocks.AIR); h.setBlock(at, Blocks.AIR); h.setBlock(at.east(), Blocks.AIR); }
            if (now == 80) h.succeed();
        });
    }

    /** Step-up HV/EV: one MFE (512 EU per tick) through the transformer gives a steady 512 EU per tick, not bursts of 2048. */
    @GameTest(template = "reactor_loop", batch = "fair_grid_transformer", timeoutTicks = 100)
    public static void stepUpRelaysWithoutPacketBursts(GameTestHelper h) {
        var at = new BlockPos(3, 3, 9);
        h.setBlock(at.west(), facing("wiring/block_mfe", Direction.EAST));
        h.setBlock(at, facing("wiring/transformer_hv_ev", Direction.EAST));
        h.setBlock(at.east(), facing("wiring/block_eesu", Direction.EAST));
        AbstractEnergyBlockEntity source = h.getBlockEntity(at.west());
        source.getEnergyStorageInternal().setStored(1_000_000);
        AbstractEnergyBlockEntity sink = h.getBlockEntity(at.east());
        IndependentTransformerBlockEntity transformer = h.getBlockEntity(at);
        transformer.setSavedMode(0);
        long[] previous = {0}; int[] tick = {0};
        h.onEachTick(() -> {
            int now = ++tick[0];
            if (now >= 25 && now <= 60) {
                long current = sink.getEnergyStorageInternal().getAmount();
                if (now > 25) h.assertTrue(current - previous[0] == 512, "Fair step-up was not a steady relay: " + (current - previous[0]));
                previous[0] = current;
            }
            if (now == 60) { h.setBlock(at.west(), Blocks.AIR); h.setBlock(at, Blocks.AIR); h.setBlock(at.east(), Blocks.AIR); }
            if (now == 80) h.succeed();
        });
    }

    /** Places a BatBox, a tin cable and furnaces at the given cable distances. Returns the furnaces. */
    private static List<AbstractEnergyBlockEntity> line(GameTestHelper h, int z, int[] distances) {
        BlockPos source = new BlockPos(0, 2, z);
        h.setBlock(source, facing("wiring/block_bat_box", Direction.EAST));
        for (int x = 1; x <= 5; x++) h.setBlock(new BlockPos(x, 2, z), block("wiring/cable/block_tin_cable"));
        AbstractEnergyBlockEntity batbox = h.getBlockEntity(source);
        batbox.getEnergyStorageInternal().setStored(batbox.getEnergyStorageInternal().getCapacity());
        var furnaces = new ArrayList<AbstractEnergyBlockEntity>();
        for (int distance : distances) {
            var at = new BlockPos(distance, 2, z + 1);
            h.setBlock(at, block("producer/block_furnace_elc"));
            AbstractEnergyBlockEntity furnace = h.getBlockEntity(at);
            furnace.getEnergyStorageInternal().setStored(0);
            furnaces.add(furnace);
        }
        return furnaces;
    }

    private static void clear(GameTestHelper h, int z) {
        for (int x = 0; x <= 5; x++) { h.setBlock(new BlockPos(x, 2, z), Blocks.AIR); h.setBlock(new BlockPos(x, 2, z + 1), Blocks.AIR); }
    }

    private static long amount(AbstractEnergyBlockEntity entity) { return entity.getEnergyStorageInternal().getAmount(); }

    private static BlockState block(String id) { return BuiltInRegistries.BLOCK.get(ResourceLocation.parse("mio_icif:" + id)).defaultBlockState(); }

    private static BlockState facing(String id, Direction side) {
        var state = block(id);
        return state.setValue((DirectionProperty) state.getBlock().getStateDefinition().getProperty("facing"), side);
    }
}
