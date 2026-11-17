package example.sicore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * 0.1.11.30 (cable audit W1): the cable tooltip, a cable's own flow from the engine's per-conductor samples, the grid
 * health of /mio_icif grid and the walk's count of cables in chunks that do not tick. Reached by registry id and
 * reflection (example code uses the public API only).
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11530GameTests {
    private Round11530GameTests() {}

    private static Block block(String path) { return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", path)); }
    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    private static List<String> keys(Block block) {
        List<Component> lines = new ArrayList<>();
        block.appendHoverText(new ItemStack(block), Item.TooltipContext.EMPTY, lines, TooltipFlag.NORMAL);
        List<String> out = new ArrayList<>();
        for (Component c : lines) if (c.getContents() instanceof TranslatableContents t) {
            StringBuilder s = new StringBuilder(t.getKey());
            for (Object a : t.getArgs()) s.append('|').append(a instanceof Component ac ? ac.getString() : String.valueOf(a));
            out.add(s.toString());
        }
        return out;
    }

    /** The tooltip names the largest packet, no line loss by default, and when the cable shocks. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void cableTooltips(GameTestHelper h) {
        var tin = keys(block("wiring/cable/block_tin_cable"));
        h.assertTrue(tin.stream().anyMatch(k -> k.startsWith("tooltip.mio_icif.cable.packet|32|")), "tin packet " + tin);
        h.assertTrue(tin.contains("tooltip.mio_icif.cable.lossless"), "tin lossless " + tin);
        h.assertTrue(tin.contains("tooltip.mio_icif.cable.shock|8|1"), "bare tin shocks above 8 EU, full at 1 layer " + tin);
        var gold1 = keys(block("wiring/cable/block_gold_cable_1"));
        h.assertTrue(gold1.contains("tooltip.mio_icif.cable.shock|128|2"), "one-layer HV shocks above 128 EU, full at 2 " + gold1);
        h.assertTrue(keys(block("wiring/cable/block_iron_cable_3")).contains("tooltip.mio_icif.cable.insulated_full|3"), "triple EV is full");
        h.assertTrue(keys(block("wiring/cable/block_glass_cable")).contains("tooltip.mio_icif.cable.no_shock"), "glass fibre does not shock");
        h.assertTrue(keys(block("wiring/block_eu_fuse_cable")).contains("tooltip.mio_icif.cable.no_shock"), "fuse does not shock");
        h.assertTrue(keys(block("wiring/cable/block_superconducting_cable")).contains("tooltip.mio_icif.cable.any_packet"), "superconductor any packet");
        h.succeed();
    }

    /** A cable carrying energy has its own flow and packet in the engine's samples; the grid health counts it, nothing idle. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @GameTest(template = "empty", batch = "round11530_flow", timeoutTicks = 120)
    public static void cableReadsItsOwnFlow(GameTestHelper h) {
        // all inside the 5 x 5 "empty" structure (until 0.1.11.52 the sink stood at x 5, outside it: in the gap to the next
        // structure, and in a chunk the test server does not force when x 5 begins a new chunk)
        BlockPos source = new BlockPos(0, 2, 1), sink = new BlockPos(4, 2, 1), cable = new BlockPos(2, 2, 1);
        h.setBlock(source, CoreExampleMod.SOURCE.get());
        for (int x = 1; x <= 3; x++) h.setBlock(new BlockPos(x, 2, 1), block("wiring/cable/block_tin_cable"));
        h.setBlock(sink, CoreExampleMod.SINK.get());
        InheritedSource src = (InheritedSource) h.getBlockEntity(source);
        h.onEachTick(() -> src.generate(32));
        h.succeedWhen(() -> {
            var stats = com.miophas.singularity_iteration.core.runtime.energy.grid.EnergyNetGlobal.getNodeStats(h.getLevel(), h.absolutePos(cable));
            h.assertTrue(stats != null && stats.energyIn() > 0 && stats.voltage() > 0 && stats.voltage() <= 32, "cable flow " + stats);
            var engine = com.miophas.singularity_iteration.core.runtime.energy.IndependentSiEnergy.current(h.getLevel().getServer());
            var health = engine.gridHealth(h.getLevel());
            h.assertTrue(health.conductors() >= 3 && health.endpoints() >= 2 && health.idleChunks() == 0, "grid health " + health);
            try {
                Class<?> walker = Round33GameTests.common("blockentity.wiring.terminal.NetworkWalker");
                Class<?> scope = Round33GameTests.common("blockentity.wiring.terminal.NetworkWalker$Scope");
                Object walk = walker.getMethod("walk", net.minecraft.server.level.ServerLevel.class, BlockPos.class, scope, int.class, int.class)
                    .invoke(null, h.getLevel(), h.absolutePos(cable), Enum.valueOf((Class) scope, "SYSTEM"), 4096, 256);
                int conductors = walk.getClass().getField("conductors").getInt(walk), idle = walk.getClass().getField("idleConductors").getInt(walk);
                h.assertTrue(conductors >= 3 && idle == 0, "walk " + conductors + " / idle " + idle);
            } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
        });
    }
}
