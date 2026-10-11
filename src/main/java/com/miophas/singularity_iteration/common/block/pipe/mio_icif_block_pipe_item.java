package com.miophas.singularity_iteration.common.block.pipe;

import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.common.blockentity.pipe.mio_icif_pipe_item;
import com.miophas.singularity_iteration.common.blockentity.pipe.mio_icif_pipe_item_extract;
import com.miophas.singularity_iteration.common.block.mio_icif_entity_block;
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
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Item pipe block.
 * This pipe block moves items.
 * It extends the block entity base class. It has no energy system.
 */
@SuppressWarnings("null")
public class mio_icif_block_pipe_item extends mio_icif_entity_block {

    // Block codec.
    public static final MapCodec<mio_icif_block_pipe_item> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(propertiesCodec()).apply(instance, mio_icif_block_pipe_item::new));

    // Connection state properties for the 6 directions.
    public static final BooleanProperty DOWN = BooleanProperty.create("down");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty EAST = BooleanProperty.create("east");

    // Collision shape definitions.
    // Center part: 8x8x8 pixels, from coordinate 4 to coordinate 12.
    private static final VoxelShape CENTER = Block.box(4, 4, 4, 12, 12, 12);
    // Connection part for each direction. Each part includes the center part to prevent gaps.
    private static final VoxelShape DOWN_SHAPE = Block.box(4, 0, 4, 12, 12, 12);
    private static final VoxelShape UP_SHAPE = Block.box(4, 4, 4, 12, 16, 12);
    private static final VoxelShape NORTH_SHAPE = Block.box(4, 4, 0, 12, 12, 12);
    private static final VoxelShape SOUTH_SHAPE = Block.box(4, 4, 4, 12, 12, 16);
    private static final VoxelShape WEST_SHAPE = Block.box(0, 4, 4, 12, 12, 12);
    private static final VoxelShape EAST_SHAPE = Block.box(4, 4, 4, 16, 12, 12);

    // 管道模式
    private final mio_icif_pipe_item.PipeMode mode;

    public mio_icif_block_pipe_item(Properties properties) {
        this(properties, mio_icif_pipe_item.PipeMode.TRANSPORT);
    }

    public mio_icif_block_pipe_item(Properties properties, mio_icif_pipe_item.PipeMode mode) {
        super(properties);
        this.mode = mode != null ? mode : mio_icif_pipe_item.PipeMode.TRANSPORT;
        // Register the default state: no direction is connected.
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
        // Add the connection state properties for the 6 directions.
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
        // 根据模式选择正确的实体类型和实例
        return switch (mode) {
            case INPUT -> new mio_icif_pipe_item_extract(pos, state);
            case TRANSPORT -> new mio_icif_pipe_item(mio_icif_block_entities.PIPE_ITEM_TRANSPORT_ENTITY_TYPE.get(), pos, state, mode);
        };
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        // Select the correct block entity type for the mode.
        BlockEntityType<?> entityType = switch (mode) {
            case INPUT -> mio_icif_block_entities.PIPE_ITEM_INPUT_ENTITY_TYPE.get();
            case TRANSPORT -> mio_icif_block_entities.PIPE_ITEM_TRANSPORT_ENTITY_TYPE.get();
        };
        return createTickerHelper(type, entityType,
            (lvl, pos, blockState, blockEntity) -> {
                if (blockEntity instanceof mio_icif_pipe_item pipe) {
                    mio_icif_pipe_item.tick(lvl, pos, blockState, pipe);
                }
            });
    }

    /**
     * Updates the connection state of the block.
     * Sets the block state from the connection state of the block entity.
     */
    public static void updateBlockState(Level level, BlockPos pos, BlockState state, mio_icif_pipe_item pipe) {
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
     * Checks if the pipe can connect to the block in the given direction.
     * @param level the level
     * @param pos the current position
     * @param direction the direction to check
     * @return true if the pipe can connect
     */
    public static boolean canConnectTo(Level level, BlockPos pos, Direction direction) {
        BlockPos adjacentPos = pos.relative(direction);

        // 检查相邻方块是否有物品处理能力
        IItemHandler handler = level.getCapability(
            Capabilities.ItemHandler.BLOCK, adjacentPos, direction.getOpposite()
        );

        if (handler != null) {
            return true;
        }

        // 检查是否是其他物品管道
        BlockEntity adjacentEntity = level.getBlockEntity(adjacentPos);
        if (adjacentEntity instanceof mio_icif_pipe_item) {
            return true;
        }

        return false;
    }

    @Override
    public java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder params) {
        var tile=params.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY);
        // The single packed pipe is emitted by removal, which also covers replacement
        // without a loot call. Pure loot queries must never consume or copy custody.
        if(tile instanceof mio_icif_pipe_item pipe && requiresPackedDrop(pipe))return java.util.List.of();
        return super.getDrops(state,params);
    }

    @Override
    protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,
            net.minecraft.world.entity.player.Player player,net.minecraft.world.phys.BlockHitResult hit) {
        if(level.getBlockEntity(pos) instanceof mio_icif_pipe_item pipe && pipe.hasUncertainTransfer()) {
            if(!level.isClientSide())player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                "管道已暂停：外部库存的转移结果不确定。拆除可保留记录；管理员核对库存后使用 /mio_icif pipe inspect。"),false);
            return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide());
        }
        return net.minecraft.world.InteractionResult.PASS;
    }

    @Override
    protected void addBlockTooltip(net.minecraft.world.item.ItemStack stack,java.util.List<net.minecraft.network.chat.Component> tooltip) {
        super.addBlockTooltip(stack,tooltip);
        var data=stack.get(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA);
        if(data!=null && data.copyTag().contains("scex_pipe_phase"))tooltip.add(net.minecraft.network.chat.Component.literal(
            "包含待核对的物品转移记录；重新放置后仍暂停。").withStyle(net.minecraft.ChatFormatting.YELLOW));
    }

    private static boolean requiresPackedDrop(mio_icif_pipe_item pipe) {
        var buffer=pipe.getBufferItem();
        return pipe.hasUncertainTransfer() || buffer.getCount()>buffer.getMaxStackSize();
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide() && !state.is(newState.getBlock())) {
            // Drop the items, if there are items.
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof mio_icif_pipe_item pipe) {
                if(requiresPackedDrop(pipe)) {
                    var packed=new net.minecraft.world.item.ItemStack(this);
                    packed.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,
                        net.minecraft.world.item.component.CustomData.of(pipe.saveWithId(level.registryAccess())));
                    level.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(level,pos.getX()+0.5,pos.getY()+0.5,pos.getZ()+0.5,packed));
                    super.onRemove(state,level,pos,newState,movedByPiston);
                    return;
                }
 // When the item pipe is broken, drop the buffered item.
                net.minecraft.world.item.ItemStack bufferItem = pipe.getBufferItem();
                if (!bufferItem.isEmpty()) {
                    double x = pos.getX() + 0.5;
                    double y = pos.getY() + 0.5;
                    double z = pos.getZ() + 0.5;
                    net.minecraft.world.entity.item.ItemEntity itemEntity = new net.minecraft.world.entity.item.ItemEntity(level, x, y, z, bufferItem);
                    level.addFreshEntity(itemEntity);
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /**
     * Called when a neighbor block changes.
     * Updates the connection state.
     */
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);

        if (!level.isClientSide()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof mio_icif_pipe_item pipe) {
                // Mark the pipe so that it updates its connection state.
                pipe.markForUpdate();
            }
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);

        if (!level.isClientSide()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof mio_icif_pipe_item pipe) {
                // Mark the pipe so that it updates its connection state.
                pipe.markForUpdate();
            }
        }
    }
}

