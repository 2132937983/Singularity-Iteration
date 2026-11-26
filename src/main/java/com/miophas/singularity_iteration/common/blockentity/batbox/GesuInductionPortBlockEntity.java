package com.miophas.singularity_iteration.common.blockentity.batbox;

import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.api.energy.ICableTier;
import com.miophas.singularity_iteration.core.api.energy.grid.IEnergyAcceptor;
import com.miophas.singularity_iteration.core.api.energy.grid.IEnergyEmitter;
import com.miophas.singularity_iteration.core.prefab.blockentity.MultiblockEnergyPart;
import com.miophas.singularity_iteration.core.runtime.machine.ChargepadHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.11.206: the GESU wireless induction port, a GESU module (on a face of the core, as the input and output modules). While the
 * GESU is formed it charges the players within {@link #RANGE} blocks of itself from the core: their armor, hands and inventory,
 * at most {@link #PER_ITEM} EU an item a tick (one LuV packet). It neither takes nor gives energy through cables.
 */
@SuppressWarnings("null")
public class GesuInductionPortBlockEntity extends MultiblockEnergyPart {
    public static final int RANGE = 8;
    private static final ICableTier LUV = MioIcifAPI.instance().getEnergyNetAPI().getCableTier("luv");
    public static final long PER_ITEM = LUV.getPowerRating();

    public GesuInductionPortBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state, mio_icif_block_entities.GESU_INDUCTION_PORT.get(), 1, 0, 0, LUV);
    }

    /** The formed GESU core this port charges from, or null. */
    @Nullable
    public mio_icif_gesu_core_entity core() {
        if (!isStructureCompleted() || getCorePosition() == null || level == null || !level.isLoaded(getCorePosition())) return null;
        BlockEntity be = level.getBlockEntity(getCorePosition());
        return be instanceof mio_icif_gesu_core_entity core && core.isStructureComplete() ? core : null;
    }

    public static void tick(Level level, GesuInductionPortBlockEntity port) {
        if (!(level instanceof ServerLevel server)) return;
        mio_icif_gesu_core_entity core = port.core();
        if (core == null || core.getStoredEnergy() <= 0) return;
        ChargepadHelper.Source source = new ChargepadHelper.Source() {
            @Override public long available() { return core.getStoredEnergy(); }
            @Override public long take(long amount) { return core.consumeEnergy(amount, false); }
            @Override public void refund(long amount) { core.generateEnergyInternal(amount, false); }
        };
        if (WirelessInduction.chargePlayers(server, port.getBlockPos(), RANGE, source, PER_ITEM) > 0) core.setChanged();
    }

    // ---- no part in the energy grid: it charges players only
    @Override public double getOfferedEnergy() { return 0.0D; }
    @Override public void drawEnergy(double amount) { }
    @Override public boolean emitsEnergyTo(IEnergyAcceptor acceptor, Direction direction) { return false; }
    @Override public double getDemandedEnergy() { return 0.0D; }
    @Override public double injectEnergy(Direction direction, double amount, double voltage) { return amount; }
    @Override public boolean acceptsEnergyFrom(IEnergyEmitter emitter, Direction direction) { return false; }
}
