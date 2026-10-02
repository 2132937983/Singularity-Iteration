package com.miophas.singularity_iteration.common.armory;

import com.miophas.singularity_iteration.common.block.mio_icif_entity_block;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** Armory machine block: classic IC2 cube, lit while bound and powered. */
@SuppressWarnings("null")
public class ArmoryBlock extends mio_icif_entity_block {
    public static final BooleanProperty LIT = BooleanProperty.create("lit");
    public static final MapCodec<ArmoryBlock> CODEC = simpleCodec(ArmoryBlock::new);

    public ArmoryBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LIT);
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof mio_icif_armory armory)) return InteractionResult.PASS;
        if (armory.owner() == null) {
            Component message = armory.tryBind(player);
            if (message != null) player.displayClientMessage(message, true);
            if (armory.owner() == null) return InteractionResult.CONSUME;   // unpowered: no GUI until bound
        }
        if (!armory.mayAccess(player)) {
            player.displayClientMessage(Component.translatable("message.mio_icif.armory.owned", armory.ownerName()), true);
            return InteractionResult.CONSUME;
        }
        player.openMenu(armory, buf -> buf.writeBlockPos(pos));
        return InteractionResult.CONSUME;
    }

    // Contents drop through mio_icif_entity_block.onRemove (the entity is a Container).

    /** Only the owner (or an operator) can break a bound Armory. */
    @Override
    public float getDestroyProgress(BlockState state, Player player, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof mio_icif_armory armory && !armory.mayAccess(player)) return 0.0F;
        return super.getDestroyProgress(state, player, level, pos);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new mio_icif_armory(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, ArmoryRegistry.ARMORY_ENTITY.get(), mio_icif_armory::tick);
    }
}
