package com.miophas.singularity_iteration.common.block.kugenerator;

import com.miophas.singularity_iteration.common.blockentity.kuentity.kugenerator.mio_icif_Stirling_Kinetic_Generator;
import com.miophas.singularity_iteration.common.block.mio_icif_entity_block;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 斯特林动能发生机方块
 * 使用热能加热流体产生动能，需要水冷却保持运转
 */
@SuppressWarnings("null")
public class mio_icif_Block_Stirling_Kinetic_Generator extends mio_icif_entity_block {

    // 六面朝向属性（上、下、北、南、东、西）
    public static final DirectionProperty FACING = DirectionalBlock.FACING;

    // 定义方块状态：是否正在工作
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public mio_icif_Block_Stirling_Kinetic_Generator(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        // 六面 FACING 与 ACTIVE（不再使用父类的水平朝向）
        builder.add(FACING, ACTIVE);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(properties -> new mio_icif_Block_Stirling_Kinetic_Generator(properties));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // 正面朝向玩家，支持六个方向放置
        return this.defaultBlockState()
                .setValue(FACING, context.getNearestLookingDirection().getOpposite())
                .setValue(ACTIVE, false);
    }

    /**
     * 设置渲染形状为模型渲染（方块形式）
     */
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new mio_icif_Stirling_Kinetic_Generator(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return level.isClientSide() ? null : (level1, pos, state1, blockEntity) -> {
            if (blockEntity instanceof mio_icif_Stirling_Kinetic_Generator generator) {
                mio_icif_Stirling_Kinetic_Generator.tick(level1, pos, state1, generator);
            }
        };
    }

    /**
     * 右键点击打开GUI
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof MenuProvider menuProvider) {
                player.openMenu(menuProvider);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
