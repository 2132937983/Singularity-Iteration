package com.singularity_iteration.mio_icif.Items.Build;

import com.singularity_iteration.mio_icif.Blocks.Build.mio_icif_block_foam;
import com.singularity_iteration.mio_icif.Blocks.Wire.mio_icif_block_wire;
import com.singularity_iteration.mio_icif.Blocks.entity.build.mio_icif_block_foam_entity;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_wire;
import com.singularity_iteration.mio_icif.Items.Tools.mio_icif_tool_elc;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Obscurator（遮蔽器）电力工具
 * 继承 mio_icif_tool_elc 使用电力工具的能量系统
 * Shift+右键：扫描方块纹理并存储到物品中（消耗5000 EU）
 * 普通右键：将扫描的纹理应用到目标建筑泡沫方块上（消耗1000 EU）
 *
 * 修改后功能：只改变建筑泡沫的外观渲染，不改变方块本质
 * 打掉泡沫后仍然掉落建筑泡沫本身
 */
@SuppressWarnings("null")
public class ObscuratorItem extends mio_icif_tool_elc {

    private static final int SCAN_ENERGY_COST = 5000;
    private static final int APPLY_ENERGY_COST = 1000;
    private static final int MAX_ENERGY = 100000;

    // 存储扫描数据的NBT键名
    private static final String TAG_SCANNED_BLOCK_ID = "ObscuratorScannedBlockId";

    public ObscuratorItem(Properties properties) {
        super(properties, MAX_ENERGY, 0, "obscurator", 100, 100, 1);
    }

    /**
     * 执行使用操作
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockPos clickedPos = context.getClickedPos();

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (player == null) {
            return InteractionResult.PASS;
        }

        // Shift+右键：扫描方块纹理
        if (player.isShiftKeyDown()) {
            return scanBlockTexture(level, player, stack, clickedPos);
        }

        // 普通右键：应用纹理到建筑泡沫方块
        return applyTextureToFoam(level, player, stack, clickedPos);
    }

    /**
     * 扫描方块纹理 - 将方块ID存储到物品的CustomData中
     */
    private InteractionResult scanBlockTexture(Level level, Player player, ItemStack stack, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();

        // 检查能量
        if (!hasEnoughEnergy(stack, SCAN_ENERGY_COST)) {
            player.displayClientMessage(Component.translatable("message.mio_icif.obscurator.no_energy"), false);
            return InteractionResult.FAIL;
        }

        // 获取方块注册ID
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);

        // 如果是空气或未注册的方块，跳过
        if (block == Blocks.AIR || blockId == null || blockId.equals(BuiltInRegistries.BLOCK.getDefaultKey())) {
            player.displayClientMessage(Component.translatable("message.mio_icif.obscurator.invalid_scan"), false);
            return InteractionResult.FAIL;
        }

        // 消耗能量
        consumeEnergy(stack, SCAN_ENERGY_COST);

        // 将扫描到的方块ID存入CustomData
        CompoundTag tag = new CompoundTag();
        tag.putString(TAG_SCANNED_BLOCK_ID, blockId.toString());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

        // 发送消息到聊天栏（false = 显示在聊天栏，不是热键栏上方）
        player.displayClientMessage(Component.translatable("message.mio_icif.obscurator.scanned", block.getName()), false);

        return InteractionResult.SUCCESS;
    }

    /**
     * 将扫描的纹理应用到建筑泡沫方块
     * 只修改BlockEntity中的伪装数据，不改变方块本身
     * 打掉后仍然掉落建筑泡沫
     */
    private InteractionResult applyTextureToFoam(Level level, Player player, ItemStack stack, BlockPos pos) {
        BlockState clickedState = level.getBlockState(pos);
        Block clickedBlock = clickedState.getBlock();

        // 检查是否有扫描数据
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null || customData.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.mio_icif.obscurator.no_scan_data"), false);
            return InteractionResult.FAIL;
        }

        CompoundTag tag = customData.copyTag();
        if (!tag.contains(TAG_SCANNED_BLOCK_ID)) {
            player.displayClientMessage(Component.translatable("message.mio_icif.obscurator.no_scan_data"), false);
            return InteractionResult.FAIL;
        }

        // 读取扫描到的方块ID
        String blockIdStr = tag.getString(TAG_SCANNED_BLOCK_ID);
        ResourceLocation scannedBlockId = ResourceLocation.tryParse(blockIdStr);
        if (scannedBlockId == null) {
            player.displayClientMessage(Component.translatable("message.mio_icif.obscurator.invalid_scan"), false);
            return InteractionResult.FAIL;
        }

        // 通过ID获取方块
        Block scannedBlock = BuiltInRegistries.BLOCK.get(scannedBlockId);
        if (scannedBlock == null || scannedBlock == Blocks.AIR) {
            player.displayClientMessage(Component.translatable("message.mio_icif.obscurator.invalid_scan"), false);
            return InteractionResult.FAIL;
        }

        // 检查能量
        if (!hasEnoughEnergy(stack, APPLY_ENERGY_COST)) {
            player.displayClientMessage(Component.translatable("message.mio_icif.obscurator.no_energy"), false);
            return InteractionResult.FAIL;
        }

        BlockState currentState = level.getBlockState(pos);
        boolean isFoamBlock = clickedBlock instanceof mio_icif_block_foam;
        boolean isFoamloggedWire = clickedBlock instanceof mio_icif_block_wire && currentState.getValue(mio_icif_block_wire.FOAMLOGGED);

        if (!isFoamBlock && !isFoamloggedWire) {
            player.displayClientMessage(Component.translatable("message.mio_icif.obscurator.not_applicable"), false);
            return InteractionResult.FAIL;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof mio_icif_block_foam_entity) && !(blockEntity instanceof mio_icif_wire)) {
            player.displayClientMessage(Component.translatable("message.mio_icif.obscurator.not_applicable"), false);
            return InteractionResult.FAIL;
        }

        consumeEnergy(stack, APPLY_ENERGY_COST);

        if (isFoamBlock) {
            level.setBlockAndUpdate(pos, currentState.setValue(mio_icif_block_foam.DISGUISED, true));
            BlockEntity newBlockEntity = level.getBlockEntity(pos);
            if (newBlockEntity instanceof mio_icif_block_foam_entity newFoamEntity) {
                newFoamEntity.setDisguisedBlockId(scannedBlockId);
            }
        } else {
            level.setBlockAndUpdate(pos, currentState.setValue(mio_icif_block_wire.DISGUISED, true));
            BlockEntity newBlockEntity = level.getBlockEntity(pos);
            if (newBlockEntity instanceof mio_icif_wire wireEntity) {
                wireEntity.setDisguisedBlockId(scannedBlockId);
            }
        }

        // 发送消息到聊天栏
        player.displayClientMessage(Component.translatable("message.mio_icif.obscurator.applied", scannedBlock.getName()), false);

        return InteractionResult.SUCCESS;
    }

    /**
     * 添加tooltip信息
     */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide && isSelected && entity instanceof Player player) {
            long currentEnergy = getEnergy(stack);
            player.displayClientMessage(
                Component.translatable("tooltip.mio_icif.obscurator.energy", currentEnergy, getMaxEnergy()),
                true
            );
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, @Nullable List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        // 显示扫描到的方块信息
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null && !customData.isEmpty()) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains(TAG_SCANNED_BLOCK_ID)) {
                String blockIdStr = tag.getString(TAG_SCANNED_BLOCK_ID);
                ResourceLocation scannedBlockId = ResourceLocation.tryParse(blockIdStr);
                if (scannedBlockId != null) {
                    Block scannedBlock = BuiltInRegistries.BLOCK.get(scannedBlockId);
                    if (scannedBlock != null && scannedBlock != Blocks.AIR) {
                        tooltip.add(Component.translatable("tooltip.mio_icif.obscurator.scanned_block", scannedBlock.getName()));
                    } else {
                        tooltip.add(Component.translatable("tooltip.mio_icif.obscurator.no_scan"));
                    }
                } else {
                    tooltip.add(Component.translatable("tooltip.mio_icif.obscurator.no_scan"));
                }
            } else {
                tooltip.add(Component.translatable("tooltip.mio_icif.obscurator.no_scan"));
            }
        } else {
            tooltip.add(Component.translatable("tooltip.mio_icif.obscurator.no_scan"));
        }

        // 显示使用说明
        tooltip.add(Component.translatable("tooltip.mio_icif.obscurator.usage"));
        tooltip.add(Component.translatable("tooltip.mio_icif.obscurator.scan_usage"));
    }

    /**
     * 获取当前存储的能量（EU）
     */
    public long getEnergy(ItemStack stack) {
        return getMaxEnergy() - stack.getDamageValue();
    }

    /**
     * 获取最大能量
     */
    public long getMaxEnergy() {
        return MAX_ENERGY;
    }

    /**
     * 检查是否有足够能量
     */
    public boolean hasEnoughEnergy(ItemStack stack, int amount) {
        return getEnergy(stack) >= amount;
    }

    /**
     * 消耗能量
     */
    public boolean consumeEnergy(ItemStack stack, int amount) {
        if (hasEnoughEnergy(stack, amount)) {
            extractEnergy(stack, amount);
            return true;
        }
        return false;
    }

    /**
     * 提取能量（重写父类方法以支持更大的能量容量）
     */
    @Override
    public long extractEnergy(ItemStack stack, long amount) {
        long currentEnergy = getEnergy(stack);
        long newEnergy = Math.max(0, currentEnergy - amount);
        setEnergy(stack, newEnergy);
        return currentEnergy - newEnergy;
    }

    /**
     * 设置能量
     */
    public void setEnergy(ItemStack stack, long energy) {
        energy = Math.max(0, Math.min(getMaxEnergy(), energy));
        stack.setDamageValue((int) (getMaxEnergy() - energy));
    }
}