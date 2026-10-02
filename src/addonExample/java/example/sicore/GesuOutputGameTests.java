package example.sicore;

import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import com.miophas.singularity_iteration.core.prefab.blockentity.MultiblockEnergyCore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** GESU: a formed core must discharge through its output ports, not only charge. */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class GesuOutputGameTests {
    private static BlockState block(String id) { return BuiltInRegistries.BLOCK.get(ResourceLocation.parse("mio_icif:" + id)).defaultBlockState(); }
    private static BlockState facing(String id, Direction side) {
        BlockState s = block(id);
        var p = s.getBlock().getStateDefinition().getProperty("facing");
        return p instanceof DirectionProperty d && d.getPossibleValues().contains(side) ? s.setValue(d, side) : s;
    }

    @GameTest(template = "reactor_loop", batch = "gesu_output", timeoutTicks = 160)
    public static void formedGesuDischargesThroughOutputPort(GameTestHelper h) {
        BlockPos core = new BlockPos(3, 2, 3);
        for (Direction d : Direction.values()) {
            h.setBlock(core.relative(d), block(d == Direction.UP ? "wiring/block_gesu_output_iv" : "wiring/block_gesu_input_iv"));
        }
        h.setBlock(core, block("wiring/block_gesu_core"));
        BlockPos sinkPos = core.above(2);
        h.setBlock(sinkPos, facing("wiring/block_eesu", Direction.UP));
        int[] tick = {0};
        long[] start = {0, 0};
        h.onEachTick(() -> {
            int now = ++tick[0];
            MultiblockEnergyCore gesu = h.getBlockEntity(core);
            AbstractEnergyBlockEntity sink = h.getBlockEntity(sinkPos);
            if (now == 5) {
                h.assertTrue(gesu.isStructureComplete(), "GESU cross must form");
                gesu.getEnergyStorageInternal().setStored(5_000_000);
                start[0] = gesu.getStoredEnergy();
                start[1] = sink.getEnergyStorageInternal().getAmount();
            }
            if (now == 100) {
                long received = sink.getEnergyStorageInternal().getAmount() - start[1];
                long drained = start[0] - gesu.getStoredEnergy();
                h.assertTrue(received > 0, "GESU never discharged into the EESU");
                h.assertTrue(drained >= received, "Energy appeared from nowhere: drained " + drained + " received " + received);
                h.assertTrue(received >= 8192L * 20, "Discharge too slow: " + received + " EU in ~95 ticks");
                h.succeed();
            }
        });
    }
}
