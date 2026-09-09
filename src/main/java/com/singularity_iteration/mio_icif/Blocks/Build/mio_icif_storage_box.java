package com.singularity_iteration.mio_icif.Blocks.Build;

import com.singularity_iteration.mio_icif.Blocks.entity.build.mio_icif_storage_box_entity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("null")
public class mio_icif_storage_box extends BaseEntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final MapCodec<mio_icif_storage_box> CODEC = simpleCodec(mio_icif_storage_box::new);

    private final int slotCount;
    private final boolean isWood;

    public mio_icif_storage_box(Properties properties, int slotCount, boolean isWood) {
        super(properties);
        this.slotCount = slotCount;
        this.isWood = isWood;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    /**
     * 获取存储箱类型标识（用于 GUI 标题等）
     * 子类/注册时应通过匿名类覆盖此方法
     */
    public String getStorageType() {
        // 根据槽位数量推断类型（青铜和铁质都是45槽，需要覆盖此方法区分）
        return switch (slotCount) {
            case 27 -> "wood";
            case 63 -> "adviron";
            case 126 -> "iridium";
            default -> ""; // 45槽需要覆盖此方法区分 bronze 和 iron
        };
    }

    public mio_icif_storage_box(Properties properties) {
        this(properties, 27, true);
    }

    public int getSlotCount() {
        return slotCount;
    }

    public boolean isWood() {
        return isWood;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new mio_icif_storage_box_entity(pos, state, slotCount);
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

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hitResult) {
        // 非扳手物品，打开 GUI
        // 扳手拆除逻辑由扳手类统一处理（通过 c:machine 标签识别）
        if (!level.isClientSide) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof MenuProvider menuProvider) {
                player.openMenu(menuProvider);
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = new ArrayList<>();
        ItemStack dropStack = new ItemStack(this);
        BlockEntity blockEntity = params.getParameter(LootContextParams.BLOCK_ENTITY);

        // 无论是否使用扳手，都将物品保存在存储箱内不掉落
        if (blockEntity instanceof mio_icif_storage_box_entity storageBox) {
            net.minecraft.core.HolderLookup.Provider registries = storageBox.getLevel() != null ? storageBox.getLevel().registryAccess() : null;
            if (registries != null) {
                CompoundTag blockEntityTag = storageBox.saveWithId(registries);
                dropStack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(blockEntityTag));
            }
        }

        drops.add(dropStack);
        return drops;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            super.onRemove(state, level, pos, newState, movedByPiston);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        CustomData customData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains("Items")) {
                net.minecraft.nbt.ListTag itemsList = tag.getList("Items", net.minecraft.nbt.Tag.TAG_COMPOUND);
                int itemCount = 0;
                for (int i = 0; i < itemsList.size(); i++) {
                    CompoundTag itemTag = itemsList.getCompound(i);
                    if (!itemTag.isEmpty()) {
                        itemCount++;
                    }
                }
                if (itemCount > 0) {
                    tooltip.add(Component.translatable("tooltip.mio_icif.storage_box.items", itemCount, slotCount)
                            .withStyle(net.minecraft.ChatFormatting.GRAY));
                }
            }
        }
        tooltip.add(Component.translatable("tooltip.mio_icif.storage_box.capacity", slotCount)
                .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
    }
}