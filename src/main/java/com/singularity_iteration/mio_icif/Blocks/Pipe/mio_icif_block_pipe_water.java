package com.singularity_iteration.mio_icif.Blocks.Pipe;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.entity.pipe.mio_icif_pipe_fluid;
import com.singularity_iteration.mio_icif.Blocks.mio_icif_entity_block;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

/**
 * 水管方方块?
 * 用于传输流体的管道方法?
 * 继承自实体方块基类，不包含能量系�?
 */
@SuppressWarnings("null")
public class mio_icif_block_pipe_water extends mio_icif_entity_block {

    // 方块编码�?
    public static final MapCodec<mio_icif_block_pipe_water> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(propertiesCodec()).apply(instance, mio_icif_block_pipe_water::new));

    // 6个方向的连接状态属性?
    public static final BooleanProperty DOWN = BooleanProperty.create("down");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty EAST = BooleanProperty.create("east");

    // 碰撞箱定�?
    // 中心部分 (8x8x8 像素，对�?4-12 的坐�?
    private static final VoxelShape CENTER = Block.box(4, 4, 4, 12, 12, 12);
    // 各个方向的连接部分（包含中心部分，避免缝隙）
    private static final VoxelShape DOWN_SHAPE = Block.box(4, 0, 4, 12, 12, 12);
    private static final VoxelShape UP_SHAPE = Block.box(4, 4, 4, 12, 16, 12);
    private static final VoxelShape NORTH_SHAPE = Block.box(4, 4, 0, 12, 12, 12);
    private static final VoxelShape SOUTH_SHAPE = Block.box(4, 4, 4, 12, 12, 16);
    private static final VoxelShape WEST_SHAPE = Block.box(0, 4, 4, 12, 12, 12);
    private static final VoxelShape EAST_SHAPE = Block.box(4, 4, 4, 16, 12, 12);

    public mio_icif_block_pipe_water(Properties properties) {
        super(properties);
        // 注册默认状态：所有方向都不连�?
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(DOWN, false)
            .setValue(UP, false)
            .setValue(NORTH, false)
            .setValue(SOUTH, false)
            .setValue(WEST, false)
            .setValue(EAST, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        // 添加6个方向的连接状态属性?
        builder.add(DOWN, UP, NORTH, SOUTH, WEST, EAST);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    /**
     * 获取方块的碰撞箱形状
     * 根据连接状态动态组合碰撞箱
     */
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CENTER;
        
        if (state.getValue(DOWN)) {
            shape = Shapes.or(shape, DOWN_SHAPE);
        }
        if (state.getValue(UP)) {
            shape = Shapes.or(shape, UP_SHAPE);
        }
        if (state.getValue(NORTH)) {
            shape = Shapes.or(shape, NORTH_SHAPE);
        }
        if (state.getValue(SOUTH)) {
            shape = Shapes.or(shape, SOUTH_SHAPE);
        }
        if (state.getValue(WEST)) {
            shape = Shapes.or(shape, WEST_SHAPE);
        }
        if (state.getValue(EAST)) {
            shape = Shapes.or(shape, EAST_SHAPE);
        }
        
        return shape;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // 使用模型渲染
        return RenderShape.MODEL;
    }

    @Override
    public boolean skipRendering(BlockState state, BlockState adjacentBlockState, Direction side) {
        // 确保透明部分正确渲染
        return false;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
        // 允许光线穿过透明部分
        return true;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new mio_icif_pipe_fluid(mio_icif_block_entities.PIPE_WATER_ENTITY_TYPE.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, mio_icif_block_entities.PIPE_WATER_ENTITY_TYPE.get(),
            (lvl, pos, blockState, blockEntity) -> {
                if (blockEntity instanceof mio_icif_pipe_fluid pipe) {
                    mio_icif_pipe_fluid.tick(lvl, pos, blockState, pipe);
                }
            });
    }

    /**
     * 更新方块的连接状态?
     * 根据方块实体的连接状态更新方块状态?
     */
    public static void updateBlockState(Level level, BlockPos pos, BlockState state, mio_icif_pipe_fluid pipe) {
        if (level.isClientSide()) return;

        BlockState newState = state
            .setValue(DOWN, pipe.isConnected(Direction.DOWN))
            .setValue(UP, pipe.isConnected(Direction.UP))
            .setValue(NORTH, pipe.isConnected(Direction.NORTH))
            .setValue(SOUTH, pipe.isConnected(Direction.SOUTH))
            .setValue(WEST, pipe.isConnected(Direction.WEST))
            .setValue(EAST, pipe.isConnected(Direction.EAST));

        if (newState != state) {
            level.setBlock(pos, newState, 3);
        }
    }

    /**
     * 检查是否可以连接到指定方向的方法?
     * @param level 世界
     * @param pos 当前位置
     * @param direction 方向
     * @return 是否可以连接
     */
    public static boolean canConnectTo(Level level, BlockPos pos, Direction direction) {
        BlockPos adjacentPos = pos.relative(direction);

        // 检查相邻方块是否有流体处理能力
        IFluidHandler handler = level.getCapability(
            Capabilities.FluidHandler.BLOCK, adjacentPos, direction.getOpposite()
        );

        if (handler != null) {
            return true;
        }

        // 检查是否是其他流体管道
        BlockEntity adjacentEntity = level.getBlockEntity(adjacentPos);
        if (adjacentEntity instanceof mio_icif_pipe_fluid) {
            return true;
        }

        return false;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            // 清理流体（如果有的话）?
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof mio_icif_pipe_fluid pipe) {
                FluidStack fluid = pipe.getFluid();
                if (!fluid.isEmpty()) {
                    // 流体管道被破坏时，流体消失（不掉落）
                    // 如果需要掉落流体，可以在这里添加逻辑
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /**
     * 邻居方块改变时触�?
     * 用于更新连接状态?
     */
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);

        if (!level.isClientSide()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof mio_icif_pipe_fluid pipe) {
                // 标记需要更新连接状态?
                pipe.markForUpdate();
            }
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);

        if (!level.isClientSide()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof mio_icif_pipe_fluid pipe) {
                // 标记需要更新连接状态?
                pipe.markForUpdate();
            }
        }
    }
}

