package com.miophas.singularity_iteration.core.prefab.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 可垂直堆叠连通的储罐骨架。
 *
 * <p>容量与方块实体类型由子类提供，连通组默认按具体子类判定，因此不同材质的
 * 储罐不会串成一组。NBT 仍使用 {@code FluidTank} 键，旧存档可直接读取。
 *
 * @param <T> 自身类型，用于让 {@link #getConnectedTanks()} 返回具体子类型
 */
@SuppressWarnings("null")
public abstract class AbstractTankBlockEntity<T extends AbstractTankBlockEntity<T>> extends BlockEntity {

    public final int tankCapacity;

    protected final FluidTank fluidTank;

    protected AbstractTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int capacity) {
        super(type, pos, state);
        this.tankCapacity = capacity;
        this.fluidTank = new FluidTank(capacity) {
            @Override
            protected void onContentsChanged() {
                setChanged();
                // 服务端内容变化时同步到客户端，供 HUD 显示流体信息
                // Merged to at most one packet per few ticks: a pipe-fed tank, or a multi-tank
                // fill touching every tank in the column, used to send one packet per change.
                if (level != null && !level.isClientSide()) {
                    com.miophas.singularity_iteration.core.runtime.sync.ClientSyncThrottle
                        .request(AbstractTankBlockEntity.this);
                }
            }
        };
    }

    /** 连通组判定；默认要求同一具体储罐类型。 */
    protected boolean sameTankGroup(@Nullable BlockEntity other) {
        return other != null && getClass() == other.getClass();
    }

    @SuppressWarnings("unchecked")
    public List<T> getConnectedTanks() {
        List<T> tanks = new ArrayList<>();
        Level level = getLevel();
        if (level == null) {
            tanks.add((T) this);
            return tanks;
        }
        BlockPos checkPos = worldPosition;
        while (true) {
            BlockEntity be = level.getBlockEntity(checkPos.below());
            if (sameTankGroup(be)) {
                checkPos = checkPos.below();
            } else break;
        }
        while (true) {
            BlockEntity be = level.getBlockEntity(checkPos);
            if (sameTankGroup(be)) {
                tanks.add((T) be);
                checkPos = checkPos.above();
            } else break;
        }
        return tanks;
    }

    public int getConnectedTankCount() {
        return getConnectedTanks().size();
    }

    public int getTotalCapacity() {
        return tankCapacity * getConnectedTankCount();
    }

    public int getTotalFluidAmount() {
        int total = 0;
        for (T tank : getConnectedTanks()) {
            total += tank.fluidTank.getFluidAmount();
        }
        return total;
    }

    @Nullable
    public FluidStack getCommonFluid() {
        FluidStack found = null;
        for (T tank : getConnectedTanks()) {
            FluidStack fs = tank.fluidTank.getFluid();
            if (fs.isEmpty()) continue;
            if (found == null) {
                found = fs;
            } else if (fs.getFluid() != found.getFluid()) {
                return null;
            }
        }
        return found;
    }

    public void onBlockPlaced(Level level) {
        if (level.isClientSide()) return;
        balanceTankFluids();
    }

    public void balanceTankFluids() {
        Level level = getLevel();
        if (level == null || level.isClientSide()) return;
        List<T> tanks = getConnectedTanks();
        FluidStack commonFluid = null;
        for (T tank : tanks) {
            FluidStack held = tank.fluidTank.getFluid();
            if (held.isEmpty()) continue;
            if (commonFluid == null) {
                commonFluid = held.copy();
            } else if (held.getFluid() != commonFluid.getFluid()) {
                return;
            }
        }
        if (commonFluid == null) return;
        for (int i = 0; i < tanks.size() - 1; i++) {
            T current = tanks.get(i);
            T next = tanks.get(i + 1);
            int currentAmount = current.fluidTank.getFluidAmount();
            int nextAmount = next.fluidTank.getFluidAmount();
            if (nextAmount > 0 && currentAmount < tankCapacity) {
                int toMove = Math.min(nextAmount, tankCapacity - currentAmount);
                if (toMove > 0) {
                    current.fluidTank.fill(next.fluidTank.drain(toMove, IFluidHandler.FluidAction.EXECUTE),
                        IFluidHandler.FluidAction.EXECUTE);
                }
            }
        }
    }

    public FluidTank getFluidTank() {
        return fluidTank;
    }

    public IFluidHandler getMultiTankHandler() {
        return new MultiTankHandler();
    }

    public IFluidHandler getFluidHandlerCapability(@Nullable net.minecraft.core.Direction side) {
        return getMultiTankHandler();
    }

    private final class MultiTankHandler implements IFluidHandler {
        private List<T> tanks;

        private List<T> resolveTanks() {
            if (tanks == null) {
                tanks = getConnectedTanks();
            }
            return tanks;
        }

        @Override
        public int getTanks() { return 1; }

        @Override
        public FluidStack getFluidInTank(int tank) {
            FluidStack common = getCommonFluid();
            if (common == null) return FluidStack.EMPTY;
            return common.copyWithAmount(getTotalFluidAmount());
        }

        @Override
        public int getTankCapacity(int tank) { return getTotalCapacity(); }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) { return true; }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource == null || resource.isEmpty()) return 0;
            for (T t : resolveTanks()) {
                FluidStack current = t.fluidTank.getFluid();
                if (!current.isEmpty() && current.getFluid() != resource.getFluid()) {
                    return 0;
                }
            }
            FluidStack toFill = resource.copy();
            int totalFilled = 0;
            for (T t : resolveTanks()) {
                int filled = t.fluidTank.fill(toFill, action);
                if (filled > 0) {
                    toFill.shrink(filled);
                    totalFilled += filled;
                    if (toFill.isEmpty()) break;
                }
            }
            return totalFilled;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource == null || resource.isEmpty()) return FluidStack.EMPTY;
            List<T> reversed = new ArrayList<>(resolveTanks());
            Collections.reverse(reversed);
            FluidStack totalDrained = null;
            int remaining = resource.getAmount();
            for (T t : reversed) {
                if (remaining <= 0) break;
                FluidStack toDrain = resource.copyWithAmount(remaining);
                FluidStack drained = t.fluidTank.drain(toDrain, action);
                if (!drained.isEmpty()) {
                    if (totalDrained == null) {
                        totalDrained = drained.copy();
                        totalDrained.setAmount(0);
                    }
                    totalDrained.grow(drained.getAmount());
                    remaining -= drained.getAmount();
                }
            }
            return totalDrained != null ? totalDrained : FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (maxDrain <= 0) return FluidStack.EMPTY;
            List<T> reversed = new ArrayList<>(resolveTanks());
            Collections.reverse(reversed);
            FluidStack totalDrained = null;
            int remaining = maxDrain;
            for (T t : reversed) {
                if (remaining <= 0) break;
                FluidStack drained = t.fluidTank.drain(remaining, action);
                if (!drained.isEmpty()) {
                    if (totalDrained == null) {
                        totalDrained = drained.copy();
                        totalDrained.setAmount(0);
                    }
                    totalDrained.grow(drained.getAmount());
                    remaining -= drained.getAmount();
                }
            }
            return totalDrained != null ? totalDrained : FluidStack.EMPTY;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        CompoundTag fluidTag = new CompoundTag();
        fluidTank.writeToNBT(registries, fluidTag);
        tag.put("FluidTank", fluidTag);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("FluidTank")) {
            CompoundTag fluidTag = tag.getCompound("FluidTank");
            fluidTank.readFromNBT(registries, fluidTag);
        }
    }

    // ==================== 客户端同步 ====================

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        CompoundTag fluidTag = new CompoundTag();
        fluidTank.writeToNBT(registries, fluidTag);
        tag.put("FluidTank", fluidTag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        if (tag.contains("FluidTank")) {
            CompoundTag fluidTag = tag.getCompound("FluidTank");
            fluidTank.readFromNBT(registries, fluidTag);
        }
    }
}
