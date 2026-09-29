package example.sicore;

import com.miophas.singularity_iteration.core.api.energy.EnergyPortPolicy;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import com.miophas.singularity_iteration.core.prefab.component.EnergyComponent;
import com.miophas.singularity_iteration.core.prefab.component.EnergyComponentHost;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Composition also works when a third-party superclass prevents inheriting a core entity. */
public final class ComposedStorage extends BlockEntity implements EnergyComponentHost {
    private EnergyPortPolicy ports = new EnergyPortPolicy(63 ^ (1 << Direction.EAST.ordinal()), 1 << Direction.EAST.ordinal(), false);
    private final EnergyComponent energy = new EnergyComponent(this, 1024, 32, 32, CableTier.LV, () -> ports);

    public ComposedStorage(BlockPos pos, BlockState state) { super(CoreExampleMod.STORAGE_ENTITY.get(), pos, state); }
    @Override public EnergyComponent energyComponent() { return energy; }
    public void closePorts() { ports = new EnergyPortPolicy(0, 0, false); energy.portsChanged(); }
    @Override public void setLevel(Level level) { super.setLevel(level); energy.setLevel(level); }
    @Override public void onLoad() { super.onLoad(); energy.onLoad(); }
    @Override public void setRemoved() { energy.setRemoved(); super.setRemoved(); }
    @Override public void clearRemoved() { super.clearRemoved(); energy.clearRemoved(); }
    @Override public void onChunkUnloaded() { energy.onChunkUnloaded(); super.onChunkUnloaded(); }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        energy.save(tag);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energy.load(tag);
    }
}
