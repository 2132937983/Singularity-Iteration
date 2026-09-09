package com.singularity_iteration.mio_icif.Blocks.Producer;

import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_large_fabricator_input_iv_entity;
import com.singularity_iteration.mio_icif.api.MioIcifAPI;
import com.singularity_iteration.mio_icif.api.energy.ICableTier;
import com.singularity_iteration.mio_icif.api.machine.IEnergyContainerBlock;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import com.mojang.serialization.MapCodec;

import org.jetbrains.annotations.Nullable;

import java.util.List;

@SuppressWarnings("null")
public class mio_icif_block_large_fabricator_input_iv extends mio_icif_Block_Fabricator_Multiblock_Base {

    private static final ICableTier TIER = MioIcifAPI.instance().getEnergyNetAPI().getCableTier("iv");
    private static final long INPUT_RATE = TIER.getPowerRating();

    public static final MapCodec<mio_icif_block_large_fabricator_input_iv> CODEC = simpleCodec(mio_icif_block_large_fabricator_input_iv::new);

    public mio_icif_block_large_fabricator_input_iv(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new mio_icif_large_fabricator_input_iv_entity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) {
            return null;
        }
        return (lvl, pos, blockState, blockEntity) -> {
            if (blockEntity instanceof mio_icif_large_fabricator_input_iv_entity entity) {
                mio_icif_large_fabricator_input_iv_entity.tick(lvl, pos, blockState, entity);
            }
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof mio_icif_large_fabricator_input_iv_entity part) {
                if (part.isStructureCompleted() && part.getCorePosition() != null) {
                    BlockEntity coreBe = level.getBlockEntity(part.getCorePosition());
                    if (coreBe instanceof MenuProvider menuProvider) {
                        player.openMenu(menuProvider);
                        return InteractionResult.sidedSuccess(level.isClientSide);
                    }
                }
            }
            if (blockEntity instanceof MenuProvider menuProvider) {
                player.openMenu(menuProvider);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void addBlockTooltip(ItemStack stack, List<Component> tooltip) {
        tooltip.add(Component.translatable("tooltip.mio_icif.large_fabricator.input_rate", INPUT_RATE)
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.mio_icif.large_fabricator.input_module")
                .withStyle(ChatFormatting.AQUA));
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, Direction direction) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof IEnergyContainerBlock container) {
            return container.getRedstoneSignalStrength();
        }
        return 0;
    }

    @Override
    protected int getDirectSignal(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, Direction direction) {
        return getSignal(state, level, pos, direction);
    }
}