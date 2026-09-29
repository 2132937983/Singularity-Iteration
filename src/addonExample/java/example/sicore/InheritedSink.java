package example.sicore;

import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public final class InheritedSink extends AbstractEnergyBlockEntity {
    public InheritedSink(BlockPos pos, BlockState state) {
        super(pos, state, CoreExampleMod.SINK_ENTITY.get(), 1024, 32, 0, CableTier.LV);
    }
    @Override protected boolean supportsFeCapability() { return true; }
    public long stored() { return apiGetStoredEnergy(); }
}
