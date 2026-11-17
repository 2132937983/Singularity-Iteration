package example.sicore;

import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import com.miophas.singularity_iteration.core.api.energy.tile.IOverloadHandler;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public final class InheritedSink extends AbstractEnergyBlockEntity implements IOverloadHandler {
    /** Test instrument (0.1.11.32): the overload callback throws, as a faulty addon's could. */
    public boolean failOverload;
    public int overloads;

    public InheritedSink(BlockPos pos, BlockState state) {
        super(pos, state, CoreExampleMod.SINK_ENTITY.get(), 1024, 32, 0, CableTier.LV);
    }
    @Override protected boolean supportsFeCapability() { return true; }
    public long stored() { return apiGetStoredEnergy(); }
    /** Lets the default explosion happen, unless {@link #failOverload} makes it throw. */
    @Override public boolean onOverload(int tier) {
        overloads++;
        if (failOverload) throw new IllegalStateException("Injected overload handler failure");
        return false;
    }
}
