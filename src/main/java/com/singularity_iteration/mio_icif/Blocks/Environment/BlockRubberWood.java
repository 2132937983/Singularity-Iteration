package com.singularity_iteration.mio_icif.Blocks.Environment;

import com.singularity_iteration.mio_icif.Blocks.mio_icif_blocks;
import com.singularity_iteration.mio_icif.Items.Resource.mio_icif_resources;
import com.singularity_iteration.mio_icif.Items.Tools.mio_icif_treetap;
import com.singularity_iteration.mio_icif.Items.Tools.mio_icif_treetap_elc;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.ItemAbilities;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null")
public class BlockRubberWood extends HorizontalDirectionalBlock {

    public static final BooleanProperty HAS_HARZ = BooleanProperty.create("has_harz");
    public static final BooleanProperty HAS_SPOT = BooleanProperty.create("has_spot");

    public BlockRubberWood(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(HAS_HARZ, false)
            .setValue(HAS_SPOT, false)
            .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return null;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HAS_HARZ, HAS_SPOT, FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
            .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!state.getValue(HAS_SPOT) || !state.getValue(HAS_HARZ)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        String itemName = stack.getItem().toString();

        if (itemName.contains("treetap_elc") || stack.getItem() instanceof mio_icif_treetap_elc) {
            return useTreetapElc(stack, state, level, pos, player, hand);
        }

        if (itemName.contains("treetap") || stack.getItem() instanceof mio_icif_treetap) {
            return useTreetap(stack, state, level, pos, player, hand);
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    private ItemInteractionResult useTreetap(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (!level.isClientSide) {
            EquipmentSlot slot = hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
            stack.hurtAndBreak(1, player, slot);

            int count = 1 + level.getRandom().nextInt(3);
            dropHarz(level, pos, player, count);

            level.setBlock(pos, state.setValue(HAS_HARZ, false), 3);
        }

        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private ItemInteractionResult useTreetapElc(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand) {
        mio_icif_treetap_elc treetapElc = (mio_icif_treetap_elc) stack.getItem();

        if (!treetapElc.hasEnoughEnergy(stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (!level.isClientSide) {
            if (!player.isCreative()) {
                treetapElc.consumeEnergy(stack);
            }

            int count = 2 + level.getRandom().nextInt(3);
            dropHarz(level, pos, player, count);

            level.setBlock(pos, state.setValue(HAS_HARZ, false), 3);

            player.setItemInHand(hand, stack);
        }

        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private void dropHarz(Level level, BlockPos pos, Player player, int count) {
        ItemStack harzStack = new ItemStack(mio_icif_resources.HARZ.get(), count);

        double midX = (pos.getX() + 0.5 + player.getX()) / 2.0;
        double midZ = (pos.getZ() + 0.5 + player.getZ()) / 2.0;
        double midY = (pos.getY() + 0.5 + player.getY()) / 2.0 + 1.0;

        ItemEntity itemEntity = new ItemEntity(level, midX, midY, midZ, harzStack);
        itemEntity.setDefaultPickUpDelay();
        level.addFreshEntity(itemEntity);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(HAS_SPOT) && !state.getValue(HAS_HARZ)) {
            if (random.nextFloat() < 0.05F) {
                level.setBlock(pos, state.setValue(HAS_HARZ, true), 3);
            }
        }
    }

    @Override
    public @Nullable BlockState getToolModifiedState(BlockState state, UseOnContext context, net.neoforged.neoforge.common.ItemAbility itemAbility, boolean simulate) {
        // 检查是否是斧头剥皮动作
        if (itemAbility == ItemAbilities.AXE_STRIP) {
            // 返回被剥皮的橡胶木，保持朝向
            return mio_icif_blocks.BLOCK_STRIPPED_RUBBER_WOOD.get().defaultBlockState()
                .setValue(FACING, state.getValue(FACING));
        }
        return super.getToolModifiedState(state, context, itemAbility, simulate);
    }
}