package com.miophas.singularity_iteration.common.item.crop.enriched;

import com.miophas.singularity_iteration.common.block.crop.enriched.UniformCropTemplate;
import com.miophas.singularity_iteration.common.block.crop.mio_icif_crop_stick;
import com.miophas.singularity_iteration.core.api.crop.IPlanter;
import com.miophas.singularity_iteration.core.runtime.crop.PlantRegistry;
import com.miophas.singularity_iteration.core.api.crop.PlantType;
import com.miophas.singularity_iteration.core.api.item.ICropSeedItem;
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
        if (stack.isEmpty() || player == null || player.isSpectator() || !level.mayInteract(player, pos)
                || !player.mayUseItemAt(pos, context.getClickedFace(), stack)) return InteractionResult.PASS;

        // 检查是否是IC2作物架
        if (state.getBlock() instanceof mio_icif_crop_stick || 
            state.getBlock() instanceof com.miophas.singularity_iteration.common.block.crop.mio_icif_crop_stick_upgraded) {
            return tryPlantOnCropStick(level, pos, state, player, stack);
        }

        // 否则尝试在耕地上种植富集作物
        return tryPlantOnFarmland(level, pos, state, player, stack);
    }

    /**
     * 在IC2作物架上种植
     */
    private InteractionResult tryPlantOnCropStick(Level level, BlockPos pos, BlockState state, Player player, ItemStack stack) {
        if (!(level.getBlockEntity(pos) instanceof IPlanter planter)) {
            return InteractionResult.FAIL;
        }

        if (planter.getPlant() != null || planter.isHybridBase()) {
            return InteractionResult.FAIL;
        }

        // 获取对应的IC2植物类型
        PlantType plantType = PlantRegistry.instance.getPlant("mio_icif", plantTypeId);
        if (plantType == null || !plantType.canGrow(planter)) {
            return InteractionResult.FAIL;
        }
        if(level.isClientSide)return InteractionResult.SUCCESS;

        // 种植作物
        planter.setPlant(plantType);
        planter.setGrowthStage(1);
        planter.setGrowthSpeed(0);
        planter.setYield(0);
        planter.setResilience(0);
        planter.setScanLevel(0);
        planter.setProgress(0);
        planter.setHybridBase(false);
        planter.updateState();

        if (player==null || !player.getAbilities().instabuild) {
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
        BlockState cropState=this.cropBlock.defaultBlockState();
        if(!cropState.canSurvive(level,cropPos))return InteractionResult.FAIL;
        if(level.isClientSide)return InteractionResult.SUCCESS;

        if (!level.setBlock(cropPos, cropState, Block.UPDATE_ALL)) {
            return InteractionResult.FAIL;
        }

        if (player==null || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        return InteractionResult.SUCCESS;
    }
    @Override public String cropModId(ItemStack stack) { return "mio_icif"; }
    @Override public String cropId(ItemStack stack) { return plantTypeId; }
}
