package com.singularity_iteration.mio_icif.Items.Crop.Enriched;

import com.singularity_iteration.mio_icif.Blocks.Crop.Enriched.UniformCropTemplate;
import com.singularity_iteration.mio_icif.Blocks.Crop.mio_icif_crop_stick;
import com.singularity_iteration.mio_icif.api.crop.IPlanter;
import com.singularity_iteration.mio_icif.api.internal.crop.PlantRegistry;
import com.singularity_iteration.mio_icif.api.crop.PlantType;
import com.singularity_iteration.mio_icif.api.item.ICropSeedItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 富集作物种子物品
 * 支持两种种植方式：
 * 1. 原版耕地 → 放置富集作物方块
 * 2. IC2作物架 → 使用IC2植物系统
 */
public class RichSeedItem extends Item implements ICropSeedItem {
    private final UniformCropTemplate cropBlock;
    private final String plantTypeId;

    public RichSeedItem(Properties properties, UniformCropTemplate cropBlock, String plantTypeId) {
        super(properties);
        this.cropBlock = cropBlock;
        this.plantTypeId = plantTypeId;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        // 检查是否是IC2作物架
        if (state.getBlock() instanceof mio_icif_crop_stick || 
            state.getBlock() instanceof com.singularity_iteration.mio_icif.Blocks.Crop.mio_icif_crop_stick_upgraded) {
            return tryPlantOnCropStick(level, pos, state, player, stack);
        }

        // 否则尝试在耕地上种植富集作物
        return tryPlantOnFarmland(level, pos, state, player, stack);
    }

    /**
     * 在IC2作物架上种植
     */
    private InteractionResult tryPlantOnCropStick(Level level, BlockPos pos, BlockState state, Player player, ItemStack stack) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (!(level.getBlockEntity(pos) instanceof IPlanter planter)) {
            return InteractionResult.FAIL;
        }

        if (planter.getPlant() != null) {
            return InteractionResult.FAIL;
        }

        // 获取对应的IC2植物类型
        PlantType plantType = PlantRegistry.instance.getPlant("mio_icif", plantTypeId);
        if (plantType == null) {
            return InteractionResult.FAIL;
        }

        // 种植作物
        planter.setPlant(plantType);
        planter.setGrowthStage(1);
        planter.updateState();

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        return InteractionResult.SUCCESS;
    }

    /**
     * 在原版耕地上种植富集作物
     */
    private InteractionResult tryPlantOnFarmland(Level level, BlockPos pos, BlockState state, Player player, ItemStack stack) {
        if (!state.is(net.minecraft.world.level.block.Blocks.FARMLAND)) {
            return InteractionResult.FAIL;
        }

        BlockPos cropPos = pos.above();
        if (!level.getBlockState(cropPos).canBeReplaced()) {
            return InteractionResult.FAIL;
        }

        if (!level.setBlock(cropPos, this.cropBlock.defaultBlockState(), Block.UPDATE_ALL)) {
            return InteractionResult.FAIL;
        }

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        return InteractionResult.SUCCESS;
    }
}