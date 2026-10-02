package example.sicore;

import com.miophas.singularity_iteration.core.api.energy.IRemoteSwitchable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * Energy Management Terminal: network discovery and telemetry over a real cable run,
 * and the remote switch that cuts an endpoint from the grid. The terminal's snapshot
 * is read reflectively because example code may only use the public core API.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class EnergyTerminalGameTests {
    private static Block block(String path) { return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", path)); }

    private static Object snapshot(BlockEntity terminal) {
        try { return terminal.getClass().getMethod("snapshot").invoke(terminal); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }

    private static Object field(Object o, String name) {
        try { return o.getClass().getField(name).get(o); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }

    /** source -(3 cables)- sink, terminal attached to the middle cable. */
    private record Rig(BlockPos source, BlockPos sink, BlockPos terminal, InheritedSource src, InheritedSink dst, BlockEntity term) { }

    private static Rig build(GameTestHelper h) {
        BlockPos source = new BlockPos(1, 2, 1), sink = new BlockPos(5, 2, 1), terminal = new BlockPos(3, 2, 2);
        h.setBlock(source, CoreExampleMod.SOURCE.get());
        for (int x = 2; x <= 4; x++) h.setBlock(new BlockPos(x, 2, 1), block("wiring/cable/block_cable"));
        h.setBlock(sink, CoreExampleMod.SINK.get());
        h.setBlock(terminal, block("wiring/block_energy_terminal"));
        return new Rig(source, sink, terminal, h.getBlockEntity(source), h.getBlockEntity(sink), h.getBlockEntity(terminal));
    }

    @GameTest(template = "reactor_loop", batch = "energy_terminal_scan", timeoutTicks = 160)
    public static void terminalDiscoversNetworkAndMeasuresFlow(GameTestHelper h) {
        Rig rig = build(h);
        h.onEachTick(() -> rig.src().generate(32));
        h.succeedWhen(() -> {
            Object s = snapshot(rig.term());
            List<?> devices = (List<?>) field(s, "devices");
            h.assertTrue(devices.size() == 2, "Terminal must see the source and the sink, saw " + devices.size());
            h.assertTrue((int) field(s, "conductors") == 3, "Terminal must count 3 cables, got " + field(s, "conductors"));
            float generation = (float) field(s, "generation"), consumption = (float) field(s, "consumption");
            h.assertTrue(generation > 0 && consumption > 0, "Flow must be measured, gen=" + generation + " use=" + consumption);
        });
    }

    @GameTest(template = "reactor_loop", batch = "energy_terminal_switch", timeoutTicks = 200)
    public static void remoteSwitchCutsEndpointFromGrid(GameTestHelper h) {
        Rig rig = build(h);
        IRemoteSwitchable sink = (IRemoteSwitchable) rig.dst();
        long[] frozen = {-1};
        int[] tick = {0};
        h.onEachTick(() -> {
            int now = ++tick[0];
            rig.src().generate(32);
            if (now == 30) {
                h.assertTrue(rig.dst().stored() > 0, "Sink must charge while switched on");
                sink.setRemotelyDisabled(true);
            }
            if (now == 40) frozen[0] = rig.dst().stored();
            if (now > 40 && now <= 80) h.assertTrue(rig.dst().stored() == frozen[0],
                "Switched-off sink still received energy: " + frozen[0] + " -> " + rig.dst().stored());
            if (now == 80) sink.setRemotelyDisabled(false);
            if (now == 110) {
                h.assertTrue(rig.dst().stored() > frozen[0], "Sink must charge again after switching on");
                h.succeed();
            }
        });
    }
}
