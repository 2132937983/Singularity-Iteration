// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.crop;

import com.miophas.singularity_iteration.common.blockentity.crop.mio_icif_crop_entity;
import com.miophas.singularity_iteration.common.block.crop.mio_icif_crop_stick;
import com.miophas.singularity_iteration.common.block.crop.mio_icif_crop_stick_upgraded;
import com.miophas.singularity_iteration.common.item.crop.CropSeedItem;
import com.miophas.singularity_iteration.common.item.resource.MatronFertilizerItem;
import com.miophas.singularity_iteration.common.item.normal.MatronHerbicideItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public final class CropInteractions {
    private CropInteractions() {}
    public static ItemInteractionResult use(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos, hit.getDirection(), stack)
                || !(level.getBlockEntity(pos) instanceof mio_icif_crop_entity crop)) return ItemInteractionResult.FAIL;
        if (stack.getItem() instanceof com.miophas.singularity_iteration.core.api.item.ICropSeedItem
                || stack.getItem() instanceof com.miophas.singularity_iteration.common.item.tools.CropAnalyzerItem
                || stack.getItem() instanceof com.miophas.singularity_iteration.common.item.tools.WeedingTrowelItem)
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        if (stack.getItem() instanceof BlockItem item && (item.getBlock() instanceof mio_icif_crop_stick
                || item.getBlock() instanceof mio_icif_crop_stick_upgraded) && crop.getPlant() == null && !crop.isHybridBase()) {
            if (!level.isClientSide) {
                crop.setHybridBase(true); crop.updateState();
                if (!player.isCreative()) stack.shrink(1);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (MatronFertilizerItem.isFertilizer(stack) && crop.getNutrients() < 100) {
            if (!level.isClientSide) {
                crop.setNutrients(crop.getNutrients() + 100); crop.updateState();
                if (!player.isCreative()) stack.shrink(1);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        ItemStack working = stack.copyWithCount(1);
        var fluid = working.getCapability(Capabilities.FluidHandler.ITEM);
        if (fluid != null) {
            var contents = fluid.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
            boolean weedEx = contents.getFluid().isSame(com.miophas.singularity_iteration.common.block.environment.fluid.mio_icif_fluids.WEED_EX.get());
            if (!weedEx && !contents.getFluid().isSame(net.minecraft.world.level.material.Fluids.WATER))
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            int needed = weedEx ? 100 - crop.getWeedControl() : 200 - crop.getWater();
            if (needed <= 0) return ItemInteractionResult.sidedSuccess(level.isClientSide);
            FluidStack request = contents.copyWithAmount(needed);
            FluidStack simulated = fluid.drain(request, IFluidHandler.FluidAction.SIMULATE);
            if (!simulated.isEmpty() && simulated.getAmount() <= request.getAmount()) {
                if (!level.isClientSide) {
                    FluidStack drained = fluid.drain(request, IFluidHandler.FluidAction.EXECUTE);
                    if (drained.isEmpty()) return ItemInteractionResult.FAIL;
                    if (weedEx) crop.setWeedControl(crop.getWeedControl() + drained.getAmount());
                    else crop.setWater(crop.getWater() + drained.getAmount());
                    crop.updateState();
                    if (!player.isCreative()) {
                        ItemStack container = fluid.getContainer();
                        stack.shrink(1);
                        if (stack.isEmpty()) player.setItemInHand(hand, container);
                        else if (!player.getInventory().add(container)) player.drop(container, false);
                    }
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return CropSeedItem.tryPlantBaseSeed(stack, state, level, pos, player, hit.getDirection());
    }
    public static InteractionResult emptyHand(BlockState state, Level level, BlockPos pos, Player player) {
        if (!level.mayInteract(player, pos) || !(level.getBlockEntity(pos) instanceof mio_icif_crop_entity crop)) return InteractionResult.PASS;
        if (crop.getPlant() == null) return InteractionResult.PASS;
        if (!level.isClientSide) crop.getPlant().onInteract(crop, player);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
