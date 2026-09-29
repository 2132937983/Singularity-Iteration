package example.sicore;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;

/** A genuinely foreign FE-only receiver, with no owned EU storage or profile. */
public final class ForeignBattery extends BlockEntity {
    public final EnergyStorage storage = new EnergyStorage(100000, 100000, 100000);
    public ForeignBattery(BlockPos at, BlockState state) { super(CoreExampleMod.FOREIGN_BATTERY_ENTITY.get(), at, state); }
}
