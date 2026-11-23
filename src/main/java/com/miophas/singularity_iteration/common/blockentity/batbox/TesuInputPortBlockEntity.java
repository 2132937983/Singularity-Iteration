package com.miophas.singularity_iteration.common.blockentity.batbox;

import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.api.energy.ICableTier;
import com.miophas.singularity_iteration.core.prefab.blockentity.MultiblockEnergyPart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.11.157: the TESU input port, as the GESU input module: it takes packets of any voltage (up to MAX) into a buffer of two
 * MAX packets and hands up to one MAX packet a tick to its controller. Unbound (no formed TESU) it only fills its buffer.
 */
@SuppressWarnings("null")
public class TesuInputPortBlockEntity extends MultiblockEnergyPart implements com.miophas.singularity_iteration.core.api.machine.IStorageMember {

    private static final ICableTier MAX_TIER = MioIcifAPI.instance().getEnergyNetAPI().getCableTier("max");
    public static final long RATE = MAX_TIER.getPowerRating();
    private static final long BUFFER = RATE * 2;

    public TesuInputPortBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state, mio_icif_block_entities.TESU_INPUT.get(), BUFFER, RATE, 0, MAX_TIER);
    }

    /** The formed controller this port feeds (terminal storage reading). */
    @Override
    public BlockEntity storageOwner() { return controller(); }

    @Nullable
    TesuControllerBlockEntity controller() {
        Level level = getLevel();
        if (level == null || !isStructureCompleted() || getCorePosition() == null || !level.isLoaded(getCorePosition())) return null;
        return level.getBlockEntity(getCorePosition()) instanceof TesuControllerBlockEntity core && core.isStructureComplete() ? core : null;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TesuInputPortBlockEntity port) {
        if (level.isClientSide() || port.getStoredEnergy() <= 0) return;
        TesuControllerBlockEntity core = port.controller();
        if (core == null) return;
        long accepted = core.generateEnergyInternal(Math.min(port.getStoredEnergy(), RATE), false);
        if (accepted > 0) {
            port.consumeEnergy(accepted, false);
            core.setChanged();
        }
    }

    @Override
    public double getDemandedEnergy() {
        if (isPowerSource()) return 0.0D;
        long local = getEnergyCapacity() - getStoredEnergy();
        if (local <= 0) return 0.0D;
        TesuControllerBlockEntity core = controller();
        if (core != null) {
            long room = core.getEnergyCapacity() - core.getStoredEnergy();
            if (room <= 0) return 0.0D;
            return Math.min(room + local, RATE);
        }
        return Math.min(local, RATE);
    }

    @Override
    public double injectEnergy(Direction direction, double amount, double voltage) {
        if (isPowerSource()) return amount;
        long accepted = Math.min(Math.min((long) amount, RATE), getEnergyCapacity() - getStoredEnergy());
        if (accepted > 0) energyStorage.setEnergy(getStoredEnergy() + accepted);
        return amount - accepted;
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable("container.mio_icif.tesu_input");
    }
}
