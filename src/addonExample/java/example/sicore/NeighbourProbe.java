package example.sicore;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 0.1.11.52: a test block that counts the neighbour updates it gets ({@code onNeighborChange}, which
 * {@code BlockEntity.setChanged()} sends to all six neighbours through {@code updateNeighbourForOutputSignal}). A test
 * reads the count of its own probe; a global counter also counts every other block entity that ticks in the world.
 */
final class NeighbourProbe extends Block {
    private static final Long2IntOpenHashMap UPDATES = new Long2IntOpenHashMap();

    NeighbourProbe() { super(BlockBehaviour.Properties.of().strength(2)); }

    /** The neighbour updates the probe at {@code pos} (absolute) received since it was placed. */
    static int updates(BlockPos pos) { return UPDATES.get(pos.asLong()); }

    @Override
    public void onPlace(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, BlockState old, boolean moved) {
        if (!level.isClientSide) UPDATES.remove(pos.asLong());
        super.onPlace(state, level, pos, old, moved);
    }

    @Override
    public void onNeighborChange(BlockState state, LevelReader level, BlockPos pos, BlockPos neighbor) {
        if (!level.isClientSide()) UPDATES.addTo(pos.asLong(), 1);
    }
}
