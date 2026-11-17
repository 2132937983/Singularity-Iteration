package example.sicore;

import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.31 (cable audit W2): overvoltage is judged by the packet the source sends, not by the room in the receiver; a
 * melted cable leaves the scaffold or set wall it was laid in; of two routes with the same effective loss the one with
 * less physical loss carries the energy. Blocks are reached by registry id (example code uses the public API only).
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11531GameTests {
    private Round11531GameTests() {}

    private static Block block(String path) { return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", path)); }
    private static BlockState facing(String id, Direction side) {
        var state = block(id).defaultBlockState();
        return state.setValue((DirectionProperty) state.getBlock().getStateDefinition().getProperty("facing"), side);
    }

    /** An MFE on tin into a BatBox with 1 EU of room: the cable still melts (it melted only for a full receiver before). */
    private static void meltInto(GameTestHelper h, BlockState wire, String expectedDrop) {
        BlockPos sourcePos = new BlockPos(1, 2, 1), wirePos = sourcePos.east(), sinkPos = sourcePos.east(2);
        h.setBlock(sourcePos, facing("wiring/block_mfe", Direction.EAST));
        h.setBlock(wirePos, wire);
        h.setBlock(sinkPos, facing("wiring/block_bat_box", Direction.EAST));
        AbstractEnergyBlockEntity source = h.getBlockEntity(sourcePos), sink = h.getBlockEntity(sinkPos);
        source.getEnergyStorageInternal().setStored(100_000);
        sink.getEnergyStorageInternal().setStored(sink.getEnergyStorageInternal().getCapacity() - 1);
        h.succeedWhen(() -> {
            h.assertTrue(h.getBlockState(wirePos).isAir(), "the tin cable did not melt");
            h.assertTrue(h.getLevel().getBlockEntity(h.absolutePos(sinkPos)) == sink, "the cable did not protect the BatBox");
            var drops = h.getLevel().getEntities(EntityType.ITEM, new AABB(h.absolutePos(wirePos)).inflate(1.5), e -> true);
            if (expectedDrop == null) h.assertTrue(drops.isEmpty(), "a bare cable left " + drops);
            else {
                Block want = block(expectedDrop);
                h.assertTrue(drops.size() == 1 && drops.get(0).getItem().is(want.asItem()) && drops.get(0).getItem().getCount() == 1,
                    "melt left " + drops.stream().map(ItemEntity::getItem).toList() + ", want one " + expectedDrop);
            }
        });
    }

    @GameTest(template = "empty", batch = "round11531_fair", timeoutTicks = 60)
    public static void overvoltageIgnoresReceiverRoom(GameTestHelper h) {
        meltInto(h, block("wiring/cable/block_tin_cable_1").defaultBlockState(), null);
    }

    @GameTest(template = "empty", batch = "round11531_fair", timeoutTicks = 60)
    public static void meltLeavesScaffold(GameTestHelper h) {
        BlockState wire = block("wiring/cable/block_tin_cable_1").defaultBlockState();
        meltInto(h, Round33GameTests.casing(wire, "scaffold"), "build/block_scaffold_iron");
    }

    @GameTest(template = "empty", batch = "round11531_fair", timeoutTicks = 60)
    public static void meltLeavesWall(GameTestHelper h) {
        BlockState wire = block("wiring/cable/block_tin_cable_1").defaultBlockState();
        wire = Round33GameTests.casing(wire, "construction_wall");
        meltInto(h, wire, "build/block_construction_wall");
    }

    /**
     * Lossless (every route costs 0): the source reaches the sink straight through a tin cable (0.2 EU/block) or round a
     * detour of three glass fibre blocks (0.025 each). The detour has less physical loss, so it carries the energy.
     */
    @GameTest(template = "empty", batch = "round11531_fair", timeoutTicks = 120)
    public static void tieBreakPrefersLessPhysicalLoss(GameTestHelper h) {
        BlockPos source = new BlockPos(1, 2, 2), sink = new BlockPos(5, 2, 2), direct = new BlockPos(3, 2, 2), detour = new BlockPos(3, 3, 2);
        Block tin = block("wiring/cable/block_tin_cable"), glass = block("wiring/cable/block_glass_cable");
        h.setBlock(source, CoreExampleMod.SOURCE.get());
        for (int x = 2; x <= 4; x++) h.setBlock(new BlockPos(x, 2, 2), tin);
        for (int x = 2; x <= 4; x++) h.setBlock(new BlockPos(x, 3, 2), glass);
        h.setBlock(sink, CoreExampleMod.SINK.get());
        InheritedSource src = (InheritedSource) h.getBlockEntity(source);
        h.onEachTick(() -> src.generate(32));
        h.succeedWhen(() -> {
            var level = h.getLevel();
            var viaDetour = com.miophas.singularity_iteration.core.runtime.energy.grid.EnergyNetGlobal.getNodeStats(level, h.absolutePos(detour));
            var viaDirect = com.miophas.singularity_iteration.core.runtime.energy.grid.EnergyNetGlobal.getNodeStats(level, h.absolutePos(direct));
            h.assertTrue(viaDetour != null && viaDetour.energyIn() > 0, "the glass detour carries nothing: " + viaDetour);
            h.assertTrue(viaDirect == null || viaDirect.energyIn() == 0, "the lossier tin path still carries energy: " + viaDirect);
        });
    }
}
