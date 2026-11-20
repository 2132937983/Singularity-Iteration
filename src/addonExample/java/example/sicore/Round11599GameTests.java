package example.sicore;

import com.miophas.singularity_iteration.core.api.reactor.IReactorController;
import com.miophas.singularity_iteration.core.runtime.energy.IndependentSiEnergy;
import java.util.ArrayList;
import java.util.List;
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
 * 0.1.11.99: the energy management terminal measures the generation of the nuclear reactor and of the fusion reactor's power
 * output port (it showed none). (The mod's common classes by reflection: addon examples do not compile against them.)
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11599GameTests {
    private Round11599GameTests() {}

    private static Block block(String path) { return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", path)); }
    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    private static Object field(Object o, String name) {
        try { return o.getClass().getField(name).get(o); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }

    /** What the terminal shows: every device with its category and measured output, and the total generation. */
    private static String seen(GameTestHelper h, BlockEntity terminal, BlockPos generator) {
        Object s = call(terminal, "snapshot");
        List<String> rows = new ArrayList<>();
        for (Object d : (List<?>) field(s, "devices"))
            rows.add(call(d, "blockId") + "@" + call(d, "pos") + " " + call(d, "category") + " out " + call(d, "output") + " rated " + call(d, "rated"));
        var engine = IndependentSiEnergy.current(h.getLevel().getServer());
        Object stats = engine == null ? "no engine" : engine.nodeStats(h.getLevel(), generator);
        return "generation " + field(s, "generation") + ", devices " + rows + ", engine at the generator " + stats;
    }

    private static float generation(BlockEntity terminal) {
        return (float) field(call(terminal, "snapshot"), "generation");
    }

    /** generator - 3 cables - sink, the terminal on the middle cable. */
    private static BlockEntity rig(GameTestHelper h, BlockPos generator) {
        for (int x = 2; x <= 4; x++) h.setBlock(new BlockPos(x, 2, 1), block("wiring/cable/block_cable"));
        h.setBlock(new BlockPos(5, 2, 1), CoreExampleMod.SINK.get());
        h.setBlock(new BlockPos(3, 2, 2), block("wiring/block_energy_terminal"));
        return h.getBlockEntity(new BlockPos(3, 2, 2));
    }

    @GameTest(template = "empty", batch = "round11599_terminal", timeoutTicks = 200)
    public static void terminalMeasuresTheNuclearReactor(GameTestHelper h) {
        BlockPos at = new BlockPos(1, 2, 1);
        h.setBlock(at, block("generator/block_nuclear_reactor_generator"));
        IReactorController reactor = (IReactorController) h.getBlockEntity(at);
        reactor.getItemHandler().insertItem(10, new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif",
            "reactor/item_reactor_uranium_simple"))), false);
        h.setBlock(at.below(), Blocks.REDSTONE_BLOCK);
        BlockEntity terminal = rig(h, at);
        BlockPos abs = h.absolutePos(at);
        h.succeedWhen(() -> h.assertTrue(generation(terminal) > 0, "the terminal sees no reactor generation: " + seen(h, terminal, abs)));
    }

    /** The usual build: the cable on a reactor chamber, not on the reactor (the terminal missed the reactor before 0.1.11.99). */
    @GameTest(template = "empty", batch = "round11599_terminal", timeoutTicks = 200)
    public static void terminalMeasuresANuclearReactorCabledAtAChamber(GameTestHelper h) {
        BlockPos at = new BlockPos(1, 2, 0), chamber = new BlockPos(1, 2, 1);
        h.setBlock(at, block("generator/block_nuclear_reactor_generator"));
        h.setBlock(chamber, block("reactor/block_reactor_chamber"));
        IReactorController reactor = (IReactorController) h.getBlockEntity(at);
        reactor.getItemHandler().insertItem(10, new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif",
            "reactor/item_reactor_uranium_simple"))), false);
        h.setBlock(at.below(), Blocks.REDSTONE_BLOCK);
        BlockEntity terminal = rig(h, at);
        BlockPos abs = h.absolutePos(at);
        h.succeedWhen(() -> {
            h.assertTrue(generation(terminal) > 0, "the terminal sees no generation of a reactor behind a chamber: " + seen(h, terminal, abs));
            boolean listed = false;
            for (Object d : (List<?>) field(call(terminal, "snapshot"), "devices"))
                if (call(d, "pos").equals(abs) && String.valueOf(call(d, "category")).equals("GENERATOR")) listed = true;
            h.assertTrue(listed, "the reactor is not listed as a generator at its own position: " + seen(h, terminal, abs));
        });
    }

    @GameTest(template = "empty", batch = "round11599_terminal", timeoutTicks = 200)
    public static void terminalMeasuresTheFusionPowerPort(GameTestHelper h) {
        BlockPos at = new BlockPos(1, 2, 1);
        h.setBlock(at, block("fusion/block_fusion_power_port"));
        BlockEntity port = h.getBlockEntity(at);
        BlockEntity terminal = rig(h, at);
        BlockPos abs = h.absolutePos(at);
        // an unformed port sends out what it holds: 24 EU a tick, below the sink's 32 EU/t and the cable's voltage
        h.onEachTick(() -> call(port, "apiGenerateEnergy", 24L, false));
        h.succeedWhen(() -> h.assertTrue(generation(terminal) > 0, "the terminal sees no fusion port generation: " + seen(h, terminal, abs)));
    }
}
