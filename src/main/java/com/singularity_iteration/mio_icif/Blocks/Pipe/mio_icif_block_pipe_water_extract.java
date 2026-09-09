package com.singularity_iteration.mio_icif.Blocks.Pipe;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.entity.pipe.mio_icif_pipe_fluid_extract;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

/**
 * 抽水管道方块（单向泵设计算?
 *
 * 设计参考：机械动力的动力泵
 * - 正面（facing方向）：输出流体到相邻容纳?
 * - 背面（facing的反方向）：从相邻容器抽取流�?
 * - 其他4个侧面：不连接、不传输
 */
@SuppressWarnings("null")
public class mio_icif_block_pipe_water_extract extends BaseEntityBlock {

    public static final MapCodec<mio_icif_block_pipe_water_extract> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(propertiesCodec()).apply(instance, mio_icif_block_pipe_water_extract::new));

    // facing = 正面（输出方向），背�?= facing.getOpposite()（抽取方向）
    public static final DirectionProperty FACING = DirectionProperty.create("facing", Direction.values());

    // 碰撞箱：6×6×16 的泵体，占满整个方块长度
    private static final VoxelShape SHAPE_NS = Block.box(5, 5, 0, 11, 11, 16);
    private static final VoxelShape SHAPE_EW = Block.box(0, 5, 5, 16, 11, 11);
    private static final VoxelShape SHAPE_UD = Block.box(5, 0, 5, 11, 16, 11);

    public mio_icif_block_pipe_water_extract(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        return switch (facing) {
            case NORTH, SOUTH -> SHAPE_NS;
            case WEST, EAST -> SHAPE_EW;
            case UP, DOWN -> SHAPE_UD;
        };
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public boolean skipRendering(BlockState state, BlockState adjacentBlockState, Direction side) {
        return false;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
        return true;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new mio_icif_pipe_fluid_extract(mio_icif_block_entities.PIPE_WATER_EXTRACT_ENTITY_TYPE.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type, mio_icif_block_entities.PIPE_WATER_EXTRACT_ENTITY_TYPE.get(),
            (lvl, pos, blockState, blockEntity) -> {
                if (blockEntity instanceof mio_icif_pipe_fluid_extract pipe) {
                    mio_icif_pipe_fluid_extract.tick(lvl, pos, blockState, pipe);
                }
            });
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // 玩家面对的方法?= 正面（输出方向）
        return this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    public static void updateBlockState(Level level, BlockPos pos, BlockState state, mio_icif_pipe_fluid_extract pipe) {
        // 流体状态更新已禁用（不渲染流体�?
    }

    /**
     * 检查指定方向是否可以作为输出端连接
     */
    public static boolean canOutputTo(Level level, BlockPos pos, Direction direction) {
        BlockPos adjacentPos = pos.relative(direction);
        IFluidHandler handler = level.getCapability(
            Capabilities.FluidHandler.BLOCK, adjacentPos, direction.getOpposite()
        );
        return handler != null;
    }

    /**
     * 检查指定方向是否可以作为输入端连接
     */
    public static boolean canExtractFrom(Level level, BlockPos pos, Direction direction) {
        BlockPos adjacentPos = pos.relative(direction);
        IFluidHandler handler = level.getCapability(
            Capabilities.FluidHandler.BLOCK, adjacentPos, direction.getOpposite()
        );
        return handler != null;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);

        if (!level.isClientSide()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof mio_icif_pipe_fluid_extract pipe) {
                pipe.markForUpdate();
            }
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);

        if (!level.isClientSide()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof mio_icif_pipe_fluid_extract pipe) {
                Direction facing = state.getValue(FACING);
                pipe.setExtractFacing(facing.getOpposite()); // 背面是抽取方法?
                pipe.markForUpdate();
            }
        }
    }
}

