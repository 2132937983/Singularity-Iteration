package com.miophas.singularity_iteration.common.blockentity.pipe;

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
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * 流体体管道方块实体
 * 用于传输流体体（水、岩浆等 * 继承自管道基 *
 * 参IC2 1.12.2 TileEntityFluidPipe 实现
 *
 * 核心设计理念（来自IC2）：
 * 1. 管道作为被动传输通道，不主动从源抽取流体体
 * 2. 流体体由外部源/机器通过 fill 推入管道
 * 3. 管道间采用均衡分配算法，防止循环和死 * 4. 使用简单的冲存储，容量基于传输速率
 */
@SuppressWarnings("null")
public class mio_icif_pipe_fluid extends mio_icif_pipe_default {

    public static final String PIPE_TYPE = "fluid";

 // 管道内部冲容量（mb）- 参考IC2 bronze pipe: transferRate=2400, small multiplier=4
    // 我们设置基础容量1000mb，适合小型管道
    public static final int FLUID_CAPACITY = 1000;

    // tick 最大传输量（mb/tick）- 参考IC2: transferRate/20 = 120 mb/t for bronze small
    // 我们设置200 mb/tick，适中的传输速率
    public static final int TRANSFER_RATE = 200;

    protected FluidStack bufferFluid = FluidStack.EMPTY;

    private final com.miophas.singularity_iteration.core.runtime.processing.FluidTransferBuffer[] transfers = java.util.stream.IntStream.range(0, 6)
        .mapToObj(i -> new com.miophas.singularity_iteration.core.runtime.processing.FluidTransferBuffer(this::setChanged))
        .toArray(com.miophas.singularity_iteration.core.runtime.processing.FluidTransferBuffer[]::new);
    private final IFluidHandler[] sidedHandlers = new IFluidHandler[6];
    private final IFluidHandler internalHandler = new PipeFluidHandler(null);
    private long retryAfter;
    private boolean transferring;
    private final int capacity;
    private final int outputRate;
    private final com.miophas.singularity_iteration.core.runtime.processing.TickTransferBudget externalBudget;

    protected final Set<Direction> extractDirections = Collections.newSetFromMap(new java.util.EnumMap<>(Direction.class));

    private static final Random RANDOM = new Random();

    public mio_icif_pipe_fluid(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }

    public mio_icif_pipe_fluid(@Nullable BlockEntityType<?> type, BlockPos pos, BlockState state) {
        this(type, pos, state, FLUID_CAPACITY, TRANSFER_RATE);
    }

    protected mio_icif_pipe_fluid(@Nullable BlockEntityType<?> type, BlockPos pos, BlockState state,
            int capacity, int outputRate) {
        super(type, pos, state);
        this.capacity = capacity;
        this.outputRate = outputRate;
        this.externalBudget = new com.miophas.singularity_iteration.core.runtime.processing.TickTransferBudget(outputRate);
    }

    @Override
    protected void doTransfer() {
        if (transferring || !liveOwner()) return;

        if (level.getGameTime() < retryAfter) return;
        boolean wasEmpty = bufferFluid.isEmpty();
        externalBudget.begin(level.getGameTime());

        extractDirections.clear();

        transferring = true;
        try {
            // Previously rejected fluid remains owned by this pipe, even when its
            // ordinary tank is empty. Retry the original face without extracting again.
            for (Direction side : Direction.values()) {
                var transfer = transfers[side.ordinal()];
                if (!transfer.hasPending() || transfer.isBlocked() || !isConnected(side)
                        || isDirectionBlocked(side)) continue;
                var at = worldPosition.relative(side);
                if (!level.getChunkSource().hasChunk(at.getX() >> 4, at.getZ() >> 4)) continue;
                var target = level.getCapability(Capabilities.FluidHandler.BLOCK, at, side.getOpposite());
                if (!liveOwner()) return;
                if (target != null) deliver(side, target, outputRate);
                if (!liveOwner()) return;
            }
            transferFluidIC2Style();
        } catch (RuntimeException failure) {
            // Transfer intent is already persistent. Never automatically refund or
            // replay an external call whose receipt is unknown; retry queries slowly.
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

    /** Reject stale references after foreign callbacks without requesting a chunk. */
    protected final boolean liveOwner() {
        if (isRemoved() || !(level instanceof net.minecraft.server.level.ServerLevel server)
                || !server.getServer().isSameThread()) return false;
        var chunk = server.getChunkSource().getChunkNow(worldPosition.getX() >> 4, worldPosition.getZ() >> 4);
        return chunk != null && chunk.getBlockEntity(worldPosition,
            net.minecraft.world.level.chunk.LevelChunk.EntityCreationType.CHECK) == this;
    }

    private int deliver(Direction side, IFluidHandler target, int requested) {
        if (!liveOwner() || isDirectionBlocked(side)) return 0;
        var at = worldPosition.relative(side);
        var server = (net.minecraft.server.level.ServerLevel) level;
        var chunk = server.getChunkSource().getChunkNow(at.getX() >> 4, at.getZ() >> 4);
        if (chunk == null) return 0;
        var neighbor = chunk.getBlockEntity(at, net.minecraft.world.level.chunk.LevelChunk.EntityCreationType.CHECK);
        // A capability can be exposed by a block without a BlockEntity.  The
        // null identity alone is not enough in that case: the block may have
        // been replaced while a foreign handler was in flight.  Pin the state
        // as well and re-check both before every external callback.
        var neighborState = chunk.getBlockState(at);
        boolean external = !(neighbor instanceof mio_icif_pipe_fluid);
        int reserved = external ? externalBudget.reserve(requested) : requested;
        if (reserved <= 0) return 0;
        java.util.function.BooleanSupplier current = () -> {
            if (!liveOwner() || isDirectionBlocked(side)) return false;
            var now = server.getChunkSource().getChunkNow(at.getX() >> 4, at.getZ() >> 4);
            return now == chunk && now.getBlockState(at) == neighborState
                && now.getBlockEntity(at,
                    net.minecraft.world.level.chunk.LevelChunk.EntityCreationType.CHECK) == neighbor;
        };
        // A thrown call may have accepted; keep its reservation for this Tick.
        int moved = transfers[side.ordinal()].move(internalHandler, target, reserved, current);
        if (external) externalBudget.settle(reserved, moved);
        return moved;
    }

    /**
     * IC2风格的流体体传输算
 *
     * 核心逻辑（来自IC2 TileEntityFluidPipe.updateEntityServer）：
     * 1. 如果管道有流体体，收集所有可用的相邻目标标
     * 2. 对于管道目标标：只有当前流>= 目标标流体体时才参与均分（防止循环）
     * 3. 计算总流体体量和管道数量，求平均
 * 4. 先向管道传输达到均衡状
 * 5. 再向非管道目标标（机器/容器）传输剩余流
 * 6. 余量随机分配或加给非管道目标标
     */
    protected void transferFluidIC2Style() {
        if (bufferFluid.isEmpty()) return;

        int availableFluidAmount = bufferFluid.getAmount();
        int pipeCount = 1; // 包含当前管道

        List<AdjacentFluidHandler> adjacentHandlers = new ArrayList<>();

        // 收集相邻的流体体处理目
        var server = (net.minecraft.server.level.ServerLevel) level;
        for (Direction facing : Direction.values()) {
            if (!liveOwner()) return;
            if (!isConnected(facing)) continue;
            if (!canOutputTo(facing)) continue;

            BlockPos adjacentPos = worldPosition.relative(facing);
            var adjacentChunk = server.getChunkSource().getChunkNow(
                adjacentPos.getX() >> 4, adjacentPos.getZ() >> 4);
            if (adjacentChunk == null) continue;
            BlockEntity adjacentEntity = adjacentChunk.getBlockEntity(adjacentPos,
                net.minecraft.world.level.chunk.LevelChunk.EntityCreationType.CHECK);

            IFluidHandler handler = level.getCapability(
                Capabilities.FluidHandler.BLOCK, adjacentPos, facing.getOpposite()
            );

            if (!liveOwner()) return;

            if (handler == null) continue;

            // 检查能否从本管道输出（模拟drain
        FluidStack drainTest = drainFromPipeInternal(Integer.MAX_VALUE, true);
            if (drainTest.isEmpty()) continue;

            if (adjacentEntity instanceof mio_icif_pipe_fluid otherPipe) {
                if (!otherPipe.bufferFluid.isEmpty() && !FluidStack.isSameFluidSameComponents(bufferFluid, otherPipe.bufferFluid)) continue;
                // IC2关键设计：只有当前管道流体体更多时才参与均
            // 这避免了两个管道之间来回传输导致的死
            if (bufferFluid.getAmount() > otherPipe.getFluidAmount()
                    || (includeEqualPipes() && bufferFluid.getAmount() == otherPipe.getFluidAmount())) {
                    availableFluidAmount += otherPipe.getFluidAmount();
                    pipeCount++;
                    adjacentHandlers.add(new AdjacentFluidHandler(adjacentPos, facing, handler, true));
                }
            } else {
                // 非管道目标标：检查是否能接收流体体（模拟fill
            FluidStack fillTest = bufferFluid.copy();
                fillTest.setAmount(1);
                int fillResult = handler.fill(fillTest, IFluidHandler.FluidAction.SIMULATE);
                if (fillResult > 0) {
                    adjacentHandlers.add(new AdjacentFluidHandler(adjacentPos, facing, handler, false));
                }
            }
        }

        if (adjacentHandlers.isEmpty()) return;

        // 计算每个管道应分配的目标标
    int extraFluid = availableFluidAmount % pipeCount;
        int perPipeAmount = availableFluidAmount / pipeCount;

        // 第一步：处理管道间的均衡分配
        List<AdjacentFluidHandler> adjacentPipes = new ArrayList<>();
        Iterator<AdjacentFluidHandler> it = adjacentHandlers.iterator();
        while (it.hasNext()) {
            AdjacentFluidHandler target = it.next();
            if (target.isPipe) {
                BlockEntity be = level.getBlockEntity(target.pos);
                if (be instanceof mio_icif_pipe_fluid otherPipe) {
                    // 向管道传输：使目标标达到平均量
                    int transferAmount = perPipeAmount - otherPipe.getFluidAmount();
                    if (transferAmount > 0) {
                        FluidStack transferred = transferTo(target.facing, target.handler, transferAmount);
                        if (!liveOwner()) return;
                        if (transferred != null && !transferred.isEmpty()) {
                            adjacentPipes.add(target);
                        }
                    } else {
                        adjacentPipes.add(target); // 已达均衡，可能接收余
                }
                }
                it.remove(); // 从列表中移除管道
            }
        }

        // 处理余量分配
        if (adjacentHandlers.isEmpty()) {
            // 没有非管道目标标，余量随机分配给管
        while (extraFluid > 0 && !adjacentPipes.isEmpty()) {
                int index = RANDOM.nextInt(adjacentPipes.size());
                AdjacentFluidHandler target = adjacentPipes.get(index);
                FluidStack transferred = transferTo(target.facing, target.handler, 1);
                if (!liveOwner()) return;
                if (transferred != null && !transferred.isEmpty()) {
                    extraFluid--;
                    if (includeEqualPipes()) adjacentPipes.remove(index);
                } else {
                    adjacentPipes.remove(index); // 无法接收，移
            }
            }
        } else {
            // 有非管道目标标，余量加给它
        perPipeAmount += extraFluid;
        }

        // 第二步：向非管道目标标传输
        if (!adjacentHandlers.isEmpty()) {
            // 应用传输速率限制（参考IC2: transferRate * multiplier / 20
        int maxTransfer = Math.min(perPipeAmount, externalBudget.remaining());
            int maxAmountPerOutput = maxTransfer / adjacentHandlers.size();

            if (maxAmountPerOutput <= 0) {
                // 每个输出分不mb，随机逐个分配
                while (maxTransfer > 0 && !adjacentHandlers.isEmpty()) {
                    int index = RANDOM.nextInt(adjacentHandlers.size());
                    AdjacentFluidHandler target = adjacentHandlers.get(index);
                    FluidStack transferred = transferTo(target.facing, target.handler, 1);
                    if (!liveOwner()) return;
                    if (transferred != null && !transferred.isEmpty()) {
                        maxTransfer--;
                        if (includeEqualPipes()) adjacentHandlers.remove(index);
                    } else {
                        adjacentHandlers.remove(index);
                    }
                }
            } else {
                // 平均分配给各输出
        for (AdjacentFluidHandler target : adjacentHandlers) {
                    transferTo(target.facing, target.handler, maxAmountPerOutput);
                    if (!liveOwner()) return;
                }
            }
        }
    }

    /**
     * 执行实际的流体体传
 * 参IC2 LiquidUtil.transfer()
     *
     * 使用协商机制
 * 1. 先模drain fill，确定双方都接受的传输量
     * 2. 然后实际执行 drain fill
     * 3. 如果结果不一致则抛出异常
     *
     * @param dir    输出方向
     * @param target 目标标流体体处理器
 * @param amount 最大传输量
     * @return 实际传输的流体体栈，失败返EMPTY
     */
    @Nullable
    private FluidStack transferTo(Direction dir, IFluidHandler target, int amount) {
        if (amount <= 0 || bufferFluid.isEmpty() || level == null || !liveOwner()) return FluidStack.EMPTY;
        if (isDirectionBlocked(dir)) return FluidStack.EMPTY;
        var transfer = transfers[dir.ordinal()];
        var identity = transfer.pending().isEmpty() ? bufferFluid.copy() : transfer.pending();
        int moved = deliver(dir, target, amount);
        return identity.copyWithAmount(moved);
    }

    /**
     * 内部drain方法（用于传输逻辑
 */
    private FluidStack drainFromPipeInternal(int maxAmount, boolean simulate) {
        if (bufferFluid.isEmpty() || maxAmount <= 0) return FluidStack.EMPTY;

        int toDrain = Math.min(maxAmount, bufferFluid.getAmount());
        FluidStack result = bufferFluid.copyWithAmount(toDrain);

        if (!simulate) {
            bufferFluid.shrink(toDrain);
            if (bufferFluid.isEmpty()) {
                bufferFluid = FluidStack.EMPTY;
            }
            setChanged();
        }

        return result;
    }

    /**
     * 内部fill方法（用于外部调用）
     */
    private int fillToPipeInternal(FluidStack resource, boolean simulate) {
        if (resource.isEmpty()) return 0;

        if (!bufferFluid.isEmpty() && !FluidStack.isSameFluidSameComponents(bufferFluid, resource)) {
            return 0;
        }

        int canFill = Math.min(resource.getAmount(), getFluidCapacity() - bufferFluid.getAmount());
        if (canFill <= 0) return 0;

        if (!simulate) {
            if (bufferFluid.isEmpty()) {
                bufferFluid = resource.copy();
                bufferFluid.setAmount(canFill);
            } else {
                bufferFluid.grow(canFill);
            }
            setChanged();
        }

        return canFill;
    }

    protected boolean canOutputTo(Direction direction) {
        return !extractDirections.contains(direction);
    }

    @Override
    protected boolean canConnectTo(BlockPos pos, Direction direction) {
        if (level == null) return false;
        if (!level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) return false;

        // 检查相邻方块是否是管道，如果是，检查该管道是否屏蔽了对应方
    if (level.getBlockEntity(pos) instanceof mio_icif_pipe_fluid otherPipe) {
            // 如果相邻管道屏蔽了朝向当前管道的方向，则不连
        Direction opposite = direction.getOpposite();
            if (otherPipe.isDirectionBlocked(opposite)) {
                return false;
            }
            // 输水管道可以连接其他输水管道和抽水管
        // 抽水管道有自己的连接限制（只连接正面和背面），由抽水管道的canConnectTo处理
            return true;
        }

        // 检查是否有流体体处理能力（标准Capability检查）
        IFluidHandler handler = level.getCapability(
            Capabilities.FluidHandler.BLOCK, pos, direction.getOpposite()
        );

        if (handler != null) {
            return true;
        }

        return false;
    }

    @Override
    public String getPipeTypeString() {
        return PIPE_TYPE;
    }

    /**
     * 获取指定方向的流体体处理器（用于Capability系统）
 * 参IC2 PipeFluidHandler 设计
     *
     * 只在连接的方向提供能
 * 内部类包装了管道的冲区操作
     */
    @Nullable
    public IFluidHandler getFluidHandlerCapability(@Nullable Direction side) {
        if (side == null) return internalHandler;
        if (isDirectionBlocked(side)) return null;
        int index = side.ordinal();
        if (sidedHandlers[index] == null) sidedHandlers[index] = new PipeFluidHandler(side);
        return sidedHandlers[index];
    }

    public int getFluidAmount() {
        return bufferFluid.getAmount();
    }

    public int getFluidCapacity() {
        return capacity;
    }

    /** Preserve the legacy pipe's balancing behaviour; IC2 variants opt into equality. */
    protected boolean includeEqualPipes() { return false; }

    public FluidStack getFluid() {
        return bufferFluid.copy();
    }

    public boolean isEmpty() {
        return bufferFluid.isEmpty();
    }

    public boolean isFull() {
        return bufferFluid.getAmount() >= getFluidCapacity();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        for (int i = 0; i < transfers.length; i++) tag.put("scex_transfer_" + i, transfers[i].save(registries));
        if (!bufferFluid.isEmpty()) {
            tag.put("buffer", bufferFluid.save(registries));
        }
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        bufferFluid = FluidStack.EMPTY;
        for (int i = 0; i < transfers.length; i++) transfers[i].load(registries, tag.getCompound("scex_transfer_" + i));
        if (tag.contains("buffer", CompoundTag.TAG_COMPOUND)) {
            bufferFluid = FluidStack.parse(registries, tag.getCompound("buffer")).orElse(FluidStack.EMPTY);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide() && !bufferFluid.isEmpty()) {
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        if (!bufferFluid.isEmpty()) {
            tag.put("buffer", bufferFluid.save(registries));
        }
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        bufferFluid = FluidStack.EMPTY;
        if (tag.contains("buffer", CompoundTag.TAG_COMPOUND)) {
            bufferFluid = FluidStack.parse(registries, tag.getCompound("buffer")).orElse(FluidStack.EMPTY);
        }
    }

    /**
     * 管道流体体处理器
 * 参IC2 TileEntityFluidPipe.PipeFluidHandler
     *
 * 内部类，包装管道的冲区，暴露为标准的IFluidHandler接口
     * 支持从任意连接方向的fill和drain操作
     */
    private class PipeFluidHandler implements IFluidHandler {
        @Nullable
        private final Direction side;

        public PipeFluidHandler(@Nullable Direction side) {
            this.side = side;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return mio_icif_pipe_fluid.this.getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return mio_icif_pipe_fluid.this.getFluidCapacity();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            if (stack.isEmpty()) return false;
            if (mio_icif_pipe_fluid.this.bufferFluid.isEmpty()) return true;
            return FluidStack.isSameFluidSameComponents(mio_icif_pipe_fluid.this.bufferFluid, stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (!accessible()) return 0;
            return fillToPipeInternal(resource, action.simulate());
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (!accessible() || resource.isEmpty() || !FluidStack.isSameFluidSameComponents(bufferFluid, resource)) return FluidStack.EMPTY;
            return drainFromPipeInternal(resource.getAmount(), action.simulate());
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (!accessible()) return FluidStack.EMPTY;
            return drainFromPipeInternal(maxDrain, action.simulate());
        }

        private boolean accessible() {
            return !isRemoved() && (side == null || !isDirectionBlocked(side));
        }
    }

    /**
     * 相邻流体体处理器信息
 * 参IC2 LiquidUtil.AdjacentFluidHandler
     */
    private static class AdjacentFluidHandler {
        final BlockPos pos;
        final Direction facing;
        final IFluidHandler handler;
        final boolean isPipe;

        AdjacentFluidHandler(BlockPos pos, Direction facing, IFluidHandler handler, boolean isPipe) {
            this.pos = pos;
            this.facing = facing;
            this.handler = handler;
            this.isPipe = isPipe;
        }
    }
}
