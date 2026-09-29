package example.sicore;

import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public final class InheritedSource extends AbstractEnergyBlockEntity {
    public InheritedSource(BlockPos pos, BlockState state) {
        super(pos, state, CoreExampleMod.SOURCE_ENTITY.get(), 1024, 0, 32, CableTier.LV);
        setAsPowerSource(32);
    }
    @Override protected boolean supportsFeCapability() { return true; }
    public long generate(long amount) { return apiGenerateEnergy(amount, false); }
    public long stored() { return apiGetStoredEnergy(); }
}
