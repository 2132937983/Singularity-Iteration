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
import net.neoforged.neoforge.common.ItemAbility;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null")
public class mio_icif_have_rub_wood extends HorizontalDirectionalBlock {

    // 定义有胶属性?
    public static final BooleanProperty HAS_HARZ = BooleanProperty.create("has_harz");

    public mio_icif_have_rub_wood(Properties properties) {
        super(properties);
        // 默认状态为有胶，朝向北
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(HAS_HARZ, true)
            .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return null;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HAS_HARZ, FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // 根据玩家朝向设置方块方向
        return this.defaultBlockState()
            .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        // 检查是否有�?
        if (!state.getValue(HAS_HARZ)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        // 获取物品类型名称用于调试
        String itemName = stack.getItem().toString();

        // 检查是否使用电动树脂提取器（必须先检查，因为 "treetap_elc" 也包含?"treetap"�?
        if (itemName.contains("treetap_elc") || stack.getItem() instanceof mio_icif_treetap_elc) {
            return useTreetapElc(stack, state, level, pos, player, hand);
        }

        // 检查是否使用普通树胶提取器
        if (itemName.contains("treetap") || stack.getItem() instanceof mio_icif_treetap) {
            return useTreetap(stack, state, level, pos, player, hand);
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /**
     * 使用普通树胶提取器
     */
    private ItemInteractionResult useTreetap(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand) {
        // 只在服务端执行掉落和状态修复?
        if (!level.isClientSide) {
            // 消耗树胶提取器耐久�?
            EquipmentSlot slot = hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
            stack.hurtAndBreak(1, player, slot);

            // 随机掉落1-3个树�?
            int count = 1 + level.getRandom().nextInt(3);
            dropHarz(level, pos, player, count);

            // 设置方块为无胶状态?
            level.setBlock(pos, state.setValue(HAS_HARZ, false), 3);
        }

        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * 使用电动树脂提取得?
     */
    private ItemInteractionResult useTreetapElc(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand) {
        mio_icif_treetap_elc treetapElc = (mio_icif_treetap_elc) stack.getItem();

        // 检查是否有足够能量
        if (!treetapElc.hasEnoughEnergy(stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        // 只在服务端执行掉落和状态修复?
        if (!level.isClientSide) {
            // 消耗能量?
            if (!player.isCreative()) {
                treetapElc.consumeEnergy(stack);
            }

            // 电动提取器效率更高，掉落2-4个树�?
            int count = 2 + level.getRandom().nextInt(3);
            dropHarz(level, pos, player, count);

            // 设置方块为无胶状态?
            level.setBlock(pos, state.setValue(HAS_HARZ, false), 3);

            // 同步修改后的ItemStack到玩家手�?
            player.setItemInHand(hand, stack);
        }

        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * 掉落树脂
     */
    private void dropHarz(Level level, BlockPos pos, Player player, int count) {
        ItemStack harzStack = new ItemStack(mio_icif_resources.HARZ.get(), count);

        // 计算方块中心与玩家之间的位置（中点上�?格）
        double midX = (pos.getX() + 0.5 + player.getX()) / 2.0;
        double midZ = (pos.getZ() + 0.5 + player.getZ()) / 2.0;
        double midY = (pos.getY() + 0.5 + player.getY()) / 2.0 + 1.0; // 中点上方1�?

        ItemEntity itemEntity = new ItemEntity(
            level,
            midX,
            midY,
            midZ,
            harzStack
        );
        itemEntity.setDefaultPickUpDelay();
        level.addFreshEntity(itemEntity);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(HAS_HARZ)) {
            if (random.nextFloat() < 0.05F) {
                level.setBlock(pos, state.setValue(HAS_HARZ, true), 3);
            }
        }
    }

    /**
     * 处理工具交互（斧头剥皮）
     */
    @Override
    public @Nullable BlockState getToolModifiedState(BlockState state, UseOnContext context, ItemAbility itemAbility, boolean simulate) {
        // 检查是否是斧头剥皮动作
        if (itemAbility == ItemAbilities.AXE_STRIP) {
            // 获取剥皮后的方块状态，保持朝向
            BlockState strippedState = mio_icif_blocks.BLOCK_STRIPPED_RUBBER_WOOD.get().defaultBlockState();
            return strippedState.setValue(FACING, state.getValue(FACING));
        }
        return super.getToolModifiedState(state, context, itemAbility, simulate);
    }
}