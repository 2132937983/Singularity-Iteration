package com.miophas.singularity_iteration.common.item.tools;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * 地磁探测器
 * 用于探测当前位置的地磁强度
 * 右键点击方块探测地磁强度
 */
@SuppressWarnings("null")
/**
 * 0.1.11.35: an electric tool ({@link com.miophas.singularity_iteration.core.api.item.IElectricToolItem}), as the electric
 * wireless manager: charging batteries and machines charge it, and it is no longer a source a machine or another mod
 * drains (on the battery base alone it was a battery, so a charging battery skipped it).
 */
public class mio_icif_geomagnetic_detector extends com.miophas.singularity_iteration.core.prefab.item.AbstractElectricTool {

    private static final int MAX_ENERGY = 50000;
    private static final int TRANSFER_SPEED = 128;
    private static final int ENERGY_COST = 100;

    public mio_icif_geomagnetic_detector() {
        // 0.1.11.194 (T1, I-10): on the shared tool base (pack pull, holder tracking, atomic debit), no longer on the battery base
        super(new Item.Properties(), MAX_ENERGY, 0L, TRANSFER_SPEED, ENERGY_COST, 1);
    }



    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        if (player == null || level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        // 0.1.11.194 (T1, I-09, U-20): shared atomic debit, and a word when the detector is flat
        if (!consumeEnergy(stack, ENERGY_COST)) {
            player.displayClientMessage(Component.translatable("message.mio_icif.tool.no_energy"), true);
            return InteractionResult.SUCCESS;
        }

        MeterReadings.geomagnetic(player, level, pos);   // 0.1.11.200 (T5c): the multimeter's reading
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("tooltip.mio_icif.geomagnetic_detector.desc"));
    }

}
