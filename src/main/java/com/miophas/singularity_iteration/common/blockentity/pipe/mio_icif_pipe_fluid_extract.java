package com.miophas.singularity_iteration.common.blockentity.pipe;

import com.miophas.singularity_iteration.common.block.pipe.mio_icif_block_pipe_water_extract;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import com.miophas.singularity_iteration.core.api.energy.grid.*;
import com.miophas.singularity_iteration.core.runtime.energy.grid.*;
import com.miophas.singularity_iteration.core.runtime.processing.FluidTransferBuffer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.EmptyFluidHandler;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null")
public class mio_icif_pipe_fluid_extract extends mio_icif_pipe_fluid implements IEnergySink {

    public static final String PIPE_TYPE = "fluid_extract";

    private Direction extractFacing = Direction.NORTH;


    public static final int BASE_TRANSFER_RATE = 10;
    public static final int MAX_TRANSFER_RATE = Integer.MAX_VALUE;

    public static final double EU_PER_MB = 0.168;
    public static final int MAX_EU_CONSUMPTION = 32;
    public static final long ENERGY_CAPACITY = 64;
    public static final long MAX_RECEIVE = 32;

    private int currentTransferRate = BASE_TRANSFER_RATE;
    private long storedEnergy = 0;
    private boolean energyRegistered = false;
    private final FluidTransferBuffer inputTransfer = new FluidTransferBuffer(this::setChanged);
    private final FluidTransferBuffer outputTransfer = new FluidTransferBuffer(this::setChanged);
    private long retryAfter;
    private boolean transferring;

    public mio_icif_pipe_fluid_extract(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }

    public mio_icif_pipe_fluid_extract(@Nullable BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type != null ? type : mio_icif_block_entities.PIPE_WATER_EXTRACT_ENTITY_TYPE.get(), pos, state);
    }


    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide() && !energyRegistered) {
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new EnergyTileLoadEvent(this, level));
            energyRegistered = true;
        }
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide() && energyRegistered) {
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this, level));
            energyRegistered = false;
        }
        super.setRemoved();
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        if (level != null && !level.isClientSide() && !energyRegistered) {
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new EnergyTileLoadEvent(this, level));
            energyRegistered = true;
        }
    }

    @Override
    protected void doTransfer() {
        if (!liveOwner() || transferring) return;
        if (level.getGameTime() < retryAfter || inputTransfer.isBlocked() || outputTransfer.isBlocked()) return;

        syncFacingFromBlockState(getBlockState());
        boolean wasEmpty = bufferFluid.isEmpty();
        transferring = true;
        try {
            updatePowerState();
            extractFromBack();
            if (!liveOwner()) return;
            outputToFront();
        } catch (RuntimeException failure) {
            // A recipient may commit before throwing. The transfer buffer keeps the
            // uncertain call durable and blocked; never refund or replay it here.
            deferRetryAfterFailure();
        } finally {
            transferring = false;
        }

        if (!liveOwner()) return;
        boolean isEmptyNow = bufferFluid.isEmpty();
        if (wasEmpty != isEmptyNow) {
            updateBlockState();
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private void deferRetryAfterFailure() {
        if (level instanceof net.minecraft.server.level.ServerLevel server
                && !isRemoved() && server.getServer().isSameThread()) {
            retryAfter = server.getGameTime() + 20;
        }
    }


    private void updatePowerState() {
        if (storedEnergy > 0) {
            int speedBoost = (int) (storedEnergy / EU_PER_MB);
            currentTransferRate = Math.min(BASE_TRANSFER_RATE + speedBoost, MAX_TRANSFER_RATE);
            
            int speedAboveBase = currentTransferRate - BASE_TRANSFER_RATE;
            if (speedAboveBase > 0) {
                long energyConsumed = (long) (speedAboveBase * EU_PER_MB);
                storedEnergy = Math.max(0, storedEnergy - energyConsumed);
            }
        } else {
            currentTransferRate = BASE_TRANSFER_RATE;
        }
    }


    public int getCurrentTransferRate() {
        return currentTransferRate;
    }

    public String getInputTransferPhase() {
        return inputTransfer.isBlocked() && inputTransfer.uncertainPhase().isEmpty()
                ? "unrecognized-save" : inputTransfer.uncertainPhase();
    }

    public String getOutputTransferPhase() {
        return outputTransfer.isBlocked() && outputTransfer.uncertainPhase().isEmpty()
                ? "unrecognized-save" : outputTransfer.uncertainPhase();
    }

    public FluidStack getPendingInputFluid() { return inputTransfer.pending(); }
    public FluidStack getPendingOutputFluid() { return outputTransfer.pending(); }


    private void extractFromBack() {
        if (bufferFluid.getAmount() >= FLUID_CAPACITY) return;
        // Already extracted fluid remains ours even if its source is removed.
        BoundFluidHandler bound = inputTransfer.pending().isEmpty()
                ? getAdjacentHandler(getExtractFacing())
                : new BoundFluidHandler(EmptyFluidHandler.INSTANCE, this::liveOwner);
        if (bound == null) return;
        inputTransfer.move(bound.handler(), getFluidHandlerCapability(null),
                Math.min(FLUID_CAPACITY - bufferFluid.getAmount(), currentTransferRate), bound.current());
    }


    private void outputToFront() {
        if (bufferFluid.isEmpty() && outputTransfer.pending().isEmpty()) return;
        BoundFluidHandler bound = getAdjacentHandler(getExtractFacing().getOpposite());
        if (bound == null) return;
        outputTransfer.move(getFluidHandlerCapability(null), bound.handler(), currentTransferRate, bound.current());
    }

    @Nullable
    private BoundFluidHandler getAdjacentHandler(Direction direction) {
        if (level == null || isDirectionBlocked(direction) || !liveOwner()) return null;
        BlockPos adjacentPos = worldPosition.relative(direction);
        var server = (net.minecraft.server.level.ServerLevel) level;
        var chunk = server.getChunkSource().getChunkNow(adjacentPos.getX() >> 4, adjacentPos.getZ() >> 4);
        if (chunk == null) return null;
        BlockEntity adjacentEntity = chunk.getBlockEntity(adjacentPos,
                net.minecraft.world.level.chunk.LevelChunk.EntityCreationType.CHECK);
        if (adjacentEntity instanceof mio_icif_pipe_fluid_extract) return null;
        var adjacentState = chunk.getBlockState(adjacentPos);
        IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, adjacentPos, direction.getOpposite());
        if (handler == null || !liveOwner()) return null;
        java.util.function.BooleanSupplier current = () -> {
            if (!liveOwner() || isDirectionBlocked(direction)) return false;
            var now = server.getChunkSource().getChunkNow(adjacentPos.getX() >> 4, adjacentPos.getZ() >> 4);
            return now == chunk && now.getBlockState(adjacentPos) == adjacentState
                    && now.getBlockEntity(adjacentPos,
                        net.minecraft.world.level.chunk.LevelChunk.EntityCreationType.CHECK) == adjacentEntity;
        };
        return new BoundFluidHandler(handler, current);
    }

    private record BoundFluidHandler(IFluidHandler handler, java.util.function.BooleanSupplier current) { }

    @Override
    public double getDemandedEnergy() {
        long spaceAvailable = ENERGY_CAPACITY - storedEnergy;
        if (spaceAvailable <= 0) return 0;
        return Math.min(spaceAvailable, MAX_EU_CONSUMPTION);
    }

    @Override
    public double injectEnergy(Direction directionFrom, double amount, double voltage) {
        if (!Double.isFinite(amount) || amount <= 0.0D) return amount;
        long requested = Math.min((long) Math.floor(amount), MAX_RECEIVE);
        long accepted = Math.min(requested, Math.max(0L, ENERGY_CAPACITY - storedEnergy));
        storedEnergy += accepted;
        return amount - accepted;
    }

    @Override
    public int getSinkTier() {
        return EnergyNetGlobal.cableTierToSourceTier(CableTier.LV);
    }

    @Override
    public boolean acceptsEnergyFrom(IEnergyEmitter emitter, Direction direction) {
        return true;
    }


    @Override
    protected boolean canConnectTo(BlockPos pos, Direction direction) {
        Direction extractDir = getExtractFacing();
        Direction outputDir = extractDir.getOpposite();

        if (direction != extractDir && direction != outputDir) {
            return false;
        }

        if (level == null) return false;

        BlockEntity adjacentEntity = level.getBlockEntity(pos);

        if (direction == extractDir) {
            if (adjacentEntity instanceof mio_icif_pipe_fluid_extract) {
                return false;
            }
            IFluidHandler handler = level.getCapability(
                Capabilities.FluidHandler.BLOCK, pos, direction.getOpposite()
            );
            return handler != null || adjacentEntity instanceof mio_icif_pipe_fluid;
        }

        if (direction == outputDir) {
            if (adjacentEntity instanceof mio_icif_pipe_fluid_extract) {
                return false;
            }
            IFluidHandler handler = level.getCapability(
                Capabilities.FluidHandler.BLOCK, pos, direction.getOpposite()
            );
            return handler != null || adjacentEntity instanceof mio_icif_pipe_fluid;
        }

        return false;
    }

    public Direction getExtractFacing() {
        return extractFacing;
    }

    public void setExtractFacing(Direction facing) {
        this.extractFacing = facing;
        setChanged();
    }

    @Override
    public String getPipeTypeString() {
        return PIPE_TYPE;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("extract_facing", extractFacing.getName());
        tag.putInt("current_transfer_rate", currentTransferRate);
        tag.putLong("stored_energy", storedEnergy);
        tag.put("scex_extract_transfer", inputTransfer.save(registries));
        tag.put("scex_output_transfer", outputTransfer.save(registries));
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inputTransfer.load(registries, tag.getCompound("scex_extract_transfer"));
        outputTransfer.load(registries, tag.getCompound("scex_output_transfer"));
        retryAfter = 0;
        if (tag.contains("extract_facing", CompoundTag.TAG_STRING)) {
            extractFacing = Direction.byName(tag.getString("extract_facing"));
            if (extractFacing == null) {
                extractFacing = Direction.NORTH;
            }
        }
        if (tag.contains("current_transfer_rate", CompoundTag.TAG_INT)) {
            // Treat saved values as untrusted input.  Valid runtime values are
            // always in [BASE_TRANSFER_RATE, MAX_TRANSFER_RATE]; clamping here
            // prevents a damaged save from feeding a negative request or an
            // overflowing transfer calculation before the next server tick.
            long savedRate = tag.getInt("current_transfer_rate");
            currentTransferRate = (int) Math.max(BASE_TRANSFER_RATE,
                    Math.min((long) MAX_TRANSFER_RATE, savedRate));
        }
        if (tag.contains("stored_energy", CompoundTag.TAG_LONG)) {
            storedEnergy = Math.max(0L, Math.min(ENERGY_CAPACITY, tag.getLong("stored_energy")));
        }
        this.needsUpdate = true;
    }

    public void syncFacingFromBlockState(BlockState state) {
        if (state.hasProperty(mio_icif_block_pipe_water_extract.FACING)) {
            Direction facing = state.getValue(mio_icif_block_pipe_water_extract.FACING);
            Direction expectedExtract = facing.getOpposite();
            if (this.extractFacing != expectedExtract) {
                this.extractFacing = expectedExtract;
                this.needsUpdate = true;
                setChanged();
            }
        }
    }
}

