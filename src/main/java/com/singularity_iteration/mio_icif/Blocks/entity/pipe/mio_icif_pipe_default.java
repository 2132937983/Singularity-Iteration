package com.singularity_iteration.mio_icif.Blocks.entity.pipe;

import com.singularity_iteration.mio_icif.Blocks.Pipe.mio_icif_block_pipe_item;
import com.singularity_iteration.mio_icif.Blocks.Pipe.mio_icif_block_pipe_water;
import com.singularity_iteration.mio_icif.Blocks.Pipe.mio_icif_block_pipe_water_extract;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.api.machine.IPipeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

/**
 * 管道基类
 * 用于水管、物流管等管道方块实体的基类
 * 继承BlockEntity，不包含能量系统
 */
@SuppressWarnings("null")
public abstract class mio_icif_pipe_default extends BlockEntity implements IPipeBlock {

    // 管道连接状态（6个方向）
    protected boolean[] connections = new boolean[6]; // DOWN, UP, NORTH, SOUTH, WEST, EAST
    
    // 被锻造锤屏蔽的方向（不自动连接）
    protected final EnumSet<Direction> blockedDirections = EnumSet.noneOf(Direction.class);
    
    // 是否需要更新连接状
    protected boolean needsUpdate = true;

    public mio_icif_pipe_default(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }

    public mio_icif_pipe_default(@Nullable BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type != null ? type : mio_icif_block_entities.PIPE_WATER_ENTITY_TYPE.get(), pos, state);
        // 初始化连接状
        for (int i = 0; i < 6; i++) {
            connections[i] = false;
        }
    }

    /**
     * tick 更新逻辑
     * 子类应该覆盖此方法实现具体的传输逻辑
     */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_pipe_default blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        // 更新连接状
        if (blockEntity.needsUpdate) {
            blockEntity.updateConnections();
            blockEntity.needsUpdate = false;
        }

        // 执行传输逻辑（子类实现）
        blockEntity.doTransfer();
    }

    /**
     * 执行传输逻辑
     * 子类必须实现此方
     */
    protected abstract void doTransfer();

    /**
     * 更新管道连接状
     * 检个方向是否有可连接的管道或方
     */
    protected void updateConnections() {
        if (level == null) return;

        Direction[] directions = Direction.values();
        boolean changed = false;

        for (int i = 0; i < directions.length; i++) {
            Direction dir = directions[i];
            BlockPos adjacentPos = worldPosition.relative(dir);
            
            // 如果被锻造锤屏蔽，强制不连接
            if (blockedDirections.contains(dir)) {
                if (connections[i]) {
                    connections[i] = false;
                    changed = true;
                }
                continue;
            }
            
            boolean canConnect = canConnectTo(adjacentPos, dir);
            
            if (connections[i] != canConnect) {
                connections[i] = canConnect;
                changed = true;
            }
        }

        // 如果连接状态改变，更新方块状态并通知相邻方块
        if (changed) {
            setChanged();
            // 更新方块状态（用于渲染
            updateBlockState();
            // 通知相邻方块更新它们的连接状
            notifyNeighbors();
        }
    }

    /**
     * 检查是否可以连接到指定位置的方
     * @param pos 相邻方块位置
     * @param direction 方向
     * @return 是否可以连接
     */
    protected abstract boolean canConnectTo(BlockPos pos, Direction direction);

    /**
     * 通知相邻方块更新连接状
     */
    protected void notifyNeighbors() {
        if (level == null) return;

        for (Direction dir : Direction.values()) {
            BlockPos adjacentPos = worldPosition.relative(dir);
            BlockEntity adjacentEntity = level.getBlockEntity(adjacentPos);
            if (adjacentEntity instanceof mio_icif_pipe_default pipe) {
                pipe.markForUpdate();
            }
        }
    }

    /**
     * 标记需要更新连接状
     */
    public void markForUpdate() {
        this.needsUpdate = true;
    }

    /**
     * 获取指定方向的连接状
     * @param direction 方向
     * @return 是否连接
     */
    public boolean isConnected(Direction direction) {
        return connections[direction.ordinal()];
    }

    /**
     * 获取所有连接状（boolean 数组）
     * @return 连接状态数组
     */
    public boolean[] getConnectionArray() {
        return connections.clone();
    }

    /**
     * 设置指定方向的连接状
     * @param direction 方向
     * @param connected 是否连接
     */
    public void setConnection(Direction direction, boolean connected) {
        connections[direction.ordinal()] = connected;
        setChanged();
    }

    /**
     * 检查指定方向是否被锻造锤屏蔽
     * @param direction 方向
     * @return 是否被屏
     */
    public boolean isDirectionBlocked(Direction direction) {
        return blockedDirections.contains(direction);
    }

    /**
     * 使用锻造锤屏蔽指定方向（不自动连接
     * @param direction 方向
     */
    public void blockDirection(Direction direction) {
        if (blockedDirections.add(direction)) {
            setChanged();
            markForUpdate();
            updateBlockState();
            // 通知相邻管道更新连接状
            refreshNeighborConnections(direction);
        }
    }

    /**
     * 使用锻造锤解除指定方向的屏
     * @param direction 方向
     */
    public void unblockDirection(Direction direction) {
        if (blockedDirections.remove(direction)) {
            setChanged();
            markForUpdate();
            updateBlockState();
            // 通知相邻管道更新连接状
            refreshNeighborConnections(direction);
        }
    }

    /**
     * 通知相邻管道更新连接状
     * 当屏解除屏蔽某个方向时，需要让相邻的管道也重新计算连接
     */
    protected void refreshNeighborConnections(Direction direction) {
        if (level == null || level.isClientSide()) return;
        
        BlockPos neighborPos = worldPosition.relative(direction);
        BlockEntity neighborBe = level.getBlockEntity(neighborPos);
        
        if (neighborBe instanceof mio_icif_pipe_default neighborPipe) {
            // 立即更新相邻管道的连接状态（而不是等待下次tick
            neighborPipe.updateConnections();
            neighborPipe.updateBlockState();
            level.sendBlockUpdated(neighborPos, neighborBe.getBlockState(), neighborBe.getBlockState(), 3);
        }
        
        // 也触发方块更新，让相邻的非管道方块（如储罐）重新评估连接
        level.updateNeighborsAt(neighborPos, getBlockState().getBlock());
    }

    /**
     * 更新方块状
     * 根据连接状态更新方块的 blockstate，用于渲
     */
    protected void updateBlockState() {
        if (level == null || level.isClientSide()) return;

        BlockState state = level.getBlockState(worldPosition);
        if (state.getBlock() instanceof mio_icif_block_pipe_water_extract) {
            mio_icif_block_pipe_water_extract.updateBlockState(level, worldPosition, state, 
                (mio_icif_pipe_fluid_extract) this);
        } else if (state.getBlock() instanceof mio_icif_block_pipe_water) {
            mio_icif_block_pipe_water.updateBlockState(level, worldPosition, state, 
                (mio_icif_pipe_fluid) this);
        } else if (state.getBlock() instanceof mio_icif_block_pipe_item) {
            mio_icif_block_pipe_item.updateBlockState(level, worldPosition, state, 
                (mio_icif_pipe_item) this);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        // 保存连接状
        CompoundTag connectionsTag = new CompoundTag();
        for (int i = 0; i < 6; i++) {
            connectionsTag.putBoolean(String.valueOf(i), connections[i]);
        }
        tag.put("connections", connectionsTag);
        
        // 保存被屏蔽的方向
        int blockedBits = 0;
        for (Direction dir : blockedDirections) {
            blockedBits |= (1 << dir.get3DDataValue());
        }
        tag.putInt("BlockedDirections", blockedBits);

        // 保存抽取状态
        int extractBits = 0;
        for (int i = 0; i < 6; i++) {
            if (extracting[i]) extractBits |= (1 << i);
        }
        tag.putInt("ExtractingDirections", extractBits);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // 加载连接状
        if (tag.contains("connections", CompoundTag.TAG_COMPOUND)) {
            CompoundTag connectionsTag = tag.getCompound("connections");
            for (int i = 0; i < 6; i++) {
                connections[i] = connectionsTag.getBoolean(String.valueOf(i));
            }
        }
        
        // 加载被屏蔽的方向
        blockedDirections.clear();
        if (tag.contains("BlockedDirections", net.minecraft.nbt.Tag.TAG_INT)) {
            int bits = tag.getInt("BlockedDirections");
            for (Direction dir : Direction.values()) {
                if ((bits & (1 << dir.get3DDataValue())) != 0) {
                    blockedDirections.add(dir);
                }
            }
        }

        // 加载抽取状态
        if (tag.contains("ExtractingDirections", net.minecraft.nbt.Tag.TAG_INT)) {
            int bits = tag.getInt("ExtractingDirections");
            for (int i = 0; i < 6; i++) {
                extracting[i] = (bits & (1 << i)) != 0;
            }
        }

        this.needsUpdate = true;
    }

    /**
     * 获取管道类型标识（字符串）
     * 子类应该返回唯一的类型标识
     * @return 管道类型字符串
     */
    public abstract String getPipeTypeString();

    /**
     * 检查两个管道是否可以互相连
     * @param other 另一个管道
     * @return 是否可以连接
     */
    public boolean canConnectToPipe(mio_icif_pipe_default other) {
        return this.getPipeTypeString().equals(other.getPipeTypeString());
    }

    // ==================== IPipeBlock API ====================

    @Override
    public PipeType getPipeType() {
        String type = getPipeTypeString();
        if (type.contains("fluid") || type.contains("water")) return PipeType.FLUID;
        if (type.contains("item")) return PipeType.ITEM;
        return PipeType.UNIVERSAL;
    }

    @Override
    public Collection<Direction> getConnections() {
        Direction[] dirs = Direction.values();
        Set<Direction> result = EnumSet.noneOf(Direction.class);
        for (int i = 0; i < 6; i++) {
            if (connections[i]) result.add(dirs[i]);
        }
        return result;
    }

    private final boolean[] extracting = new boolean[6];

    @Override
    public boolean isExtracting(Direction side) {
        return extracting[side.ordinal()];
    }

    @Override
    public void setExtracting(Direction side, boolean extracting) {
        this.extracting[side.ordinal()] = extracting;
        setChanged();
    }

    @Override
    public int getTransferRate() {
        return 1000;
    }
}

