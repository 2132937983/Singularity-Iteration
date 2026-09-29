package example.sicore;

import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Legacy source contract: eight independent LV packets, not one 256 EU packet. */
public final class InheritedMultiSource extends AbstractEnergyBlockEntity {
    public InheritedMultiSource(BlockPos pos, BlockState state) {
        super(pos, state, CoreExampleMod.MULTI_SOURCE_ENTITY.get(), 65536, 0, 256, CableTier.LV);
        setAsPowerSource(32);
    }
    @Override public int getPacketCount() { return 8; }
    @Override protected boolean supportsFeCapability() { return true; }
    public long generate(long amount) { return apiGenerateEnergy(amount, false); }
}
