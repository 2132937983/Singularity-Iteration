package com.miophas.singularity_iteration.common.block.reactor;

import com.miophas.singularity_iteration.common.blockentity.reactor.mio_icif_reactor_chamber;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.common.multiblock.mio_icif_multiblock_manager;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** One-core reactor chamber with normal placement, item drop and block-entity lifecycle. */
@SuppressWarnings("null")
public class mio_icif_Block_Reactor_Chamber extends BaseEntityBlock {

    public mio_icif_Block_Reactor_Chamber(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(properties -> new mio_icif_Block_Reactor_Chamber(properties));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return com.miophas.singularity_iteration.common.reactor.ChamberTopology.nearbyCores(context.getLevel(),context.getClickedPos())==1
            ?super.getStateForPlacement(context):null;
    }
    @Override
    public boolean canSurvive(BlockState state,net.minecraft.world.level.LevelReader world,BlockPos at){
        return com.miophas.singularity_iteration.common.reactor.ChamberTopology.nearbyCores(world,at)==1;
    }
    @Override
    public void onPlace(BlockState state,Level level,BlockPos pos,BlockState oldState,boolean moving){
        super.onPlace(state,level,pos,oldState,moving);
        if(!level.isClientSide()){
            com.miophas.singularity_iteration.common.reactor.ChamberTopology.checkAndDrop(level,pos);
            mio_icif_multiblock_manager.notifyBlockChanged(level,pos);
        }
    }
    @Override
    public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        super.onRemove(state,level,pos,next,moving);
        if(!level.isClientSide()&&!state.is(next.getBlock()))mio_icif_multiblock_manager.notifyBlockChanged(level,pos);
    }

    @Override
    public void neighborChanged(BlockState state,Level level,BlockPos pos,net.minecraft.world.level.block.Block neighbor,BlockPos neighborPos,boolean moving){
        super.neighborChanged(state,level,pos,neighbor,neighborPos,moving);
        com.miophas.singularity_iteration.common.reactor.ChamberTopology.checkAndDrop(level,pos);
    }

    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == mio_icif_block_entities.REACTOR_CHAMBER_ENTITY_TYPE.get() ?
            (l, p, s, be) -> {
                if (be instanceof mio_icif_reactor_chamber chamber) {
                    mio_icif_reactor_chamber.tick(l, p, s, chamber);
                }
            } : null;
    }

    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return mio_icif_block_entities.REACTOR_CHAMBER_ENTITY_TYPE.get().create(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof MenuProvider menuProvider) {
                player.openMenu(menuProvider);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
