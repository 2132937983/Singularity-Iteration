package com.singularity_iteration.mio_icif.Blocks;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_Energy_Block;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_wire;
import com.singularity_iteration.mio_icif.api.energy.ICableTier;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;
import com.singularity_iteration.mio_icif.util.mio_icif_tags;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.neoforge.common.Tags;

import java.util.ArrayList;
import java.util.List;

/**
 * 实体方块基类，继承自 BaseEntityBlock
 * 提供通用的方块实体功能，包括物品掉落处理和方向支持
 * 默认仅支持水平方向（北、南、东、西）
 */
@SuppressWarnings("null")
public abstract class mio_icif_entity_block extends BaseEntityBlock {
    
    // 定义方向属性（默认仅支持水平方向：北、南、东、西）
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public mio_icif_entity_block(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canEntityDestroy(BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
        return false;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // 正面朝向玩家（仅水平方向）
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (state.is(mio_icif_tags.MACHINE) || state.is(mio_icif_tags.CABLE)) {
            LootParams lootParams = params.withParameter(LootContextParams.BLOCK_STATE, state).create(LootContextParamSets.BLOCK);
            ItemStack tool = lootParams.getParameter(LootContextParams.TOOL);
            if (!tool.isEmpty() && tool.is(Tags.Items.TOOLS_WRENCH)) {
                List<ItemStack> drops = new ArrayList<>();
                ItemStack dropStack = new ItemStack(this);
                BlockEntity blockEntity = lootParams.getParamOrNull(LootContextParams.BLOCK_ENTITY);
                if (blockEntity instanceof mio_icif_Energy_Block energyBlock) {
                    CompoundTag tag = new CompoundTag();
                    tag.putString("id", BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType()).toString());
                    tag.putLong("energy", energyBlock.getEnergyStorage().getAmount());
                    dropStack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tag));
                }
                drops.add(dropStack);
                return drops;
            }
            if (state.is(mio_icif_tags.MACHINE)) {
                List<ItemStack> drops = new ArrayList<>();
                drops.add(new ItemStack(mio_icif_blocks.MACHINE_HULL_BASIC.get()));
                return drops;
            }
        }
        return super.getDrops(state, params);
    }

    /**
     * 方块被破坏时掉落物品栏中的物品
     * @param state 当前方块状态
     * @param level 世界
     * @param pos 方块位置
     * @param newState 新方块状态
     * @param movedByPiston 是否由活塞移动
     */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof Container container) {
                // 掉落物品栏中的所有物品
                for (int i = 0; i < container.getContainerSize(); i++) {
                    ItemStack itemStack = container.getItem(i);
                    if (!itemStack.isEmpty()) {
                        // 在原位置生成物品实体
                        double x = pos.getX() + 0.5;
                        double y = pos.getY() + 0.5;
                        double z = pos.getZ() + 0.5;
                        ItemEntity itemEntity = new ItemEntity(level, x, y, z, itemStack);
                        level.addFreshEntity(itemEntity);
                    }
                }
                // 清空物品栏，避免重复掉落
                container.clearContent();
            }
            // 调用父类方法处理其他逻辑
            super.onRemove(state, level, pos, newState, movedByPiston);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        addBlockTooltip(stack, tooltip);
        addVoltageTierTooltip(stack, tooltip);
    }

    protected void addBlockTooltip(ItemStack stack, List<Component> tooltip) {
    }

    private void addVoltageTierTooltip(ItemStack stack, List<Component> tooltip) {
        CableTier tier = null;

        CustomData customData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains("cable_tier")) {
                String tierName = tag.getString("cable_tier");
                try {
                    tier = CableTier.getTier(tierName);
                } catch (Exception ignored) {
                }
            }
        }

        if (tier == null) {
            BlockEntity tempEntity = newBlockEntity(BlockPos.ZERO, defaultBlockState());
            if (tempEntity instanceof mio_icif_Energy_Block energyBlock) {
                ICableTier apiTier = energyBlock.getEffectiveCableTier();
                if (apiTier instanceof CableTier cableTier) {
                    tier = cableTier;
                }
            } else if (tempEntity instanceof mio_icif_wire wire) {
                tier = wire.getCableTier();
            }
        }

        if (tier != null) {
            String tierDisplay = tier.shortEnglishName + " (" + tier.powerRating + " EU/t)";
            tooltip.add(Component.translatable("tooltip.mio_icif.voltage_tier", tierDisplay)
                    .withStyle(ChatFormatting.AQUA));
        }
    }

    public static long getStoredEnergyFromStack(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains("energy")) {
                return tag.getLong("energy");
            }
        }
        return -1;
    }
}