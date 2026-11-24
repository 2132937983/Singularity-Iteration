package com.miophas.singularity_iteration.common.item.tools;

import com.miophas.singularity_iteration.common.item.normal.mio_icif_bat;
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
public class mio_icif_geomagnetic_detector extends mio_icif_bat implements com.miophas.singularity_iteration.core.api.item.IElectricToolItem {

    private static final int MAX_ENERGY = 50000;
    private static final int TRANSFER_SPEED = 128;
    private static final int ENERGY_COST = 100;

    public mio_icif_geomagnetic_detector() {
        super(new Item.Properties(), MAX_ENERGY, 0L, "item_geomagnetic_detector", TRANSFER_SPEED);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        com.miophas.singularity_iteration.core.runtime.energy.EnergyPackCharging.trackHolder(stack, entity);
    }

    @Override
    public long getEnergy(ItemStack stack) {
        com.miophas.singularity_iteration.core.runtime.energy.EnergyPackCharging.pullFromArmor(stack);
        return super.getEnergy(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        if (player == null || level.isClientSide() || getEnergy(stack) < ENERGY_COST) {
            return InteractionResult.SUCCESS;
        }

        extractEnergy(stack, com.miophas.singularity_iteration.core.api.item.EnergySaving.apply(stack, ENERGY_COST));

        // 计算地磁强度
        float ratio = getMagneticSource(level, pos);

        // 检查生物群系
        Biome biome = level.getBiome(pos).value();
        String biomeKey = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME).getKey(biome).toString();

        // 特殊群系加成
        if (biomeKey.contains("nether") || biomeKey.contains("hell")) {
            ratio *= 1.2F;
        } else if (biomeKey.contains("mountain") || biomeKey.contains("hill") || biomeKey.contains("peaks")) {
            ratio *= 1.1F;
        }

        player.sendSystemMessage(Component.translatable("message.mio_icif.geomagnetic_detector.value",
            String.format("%.2f", ratio)));
        // 0.1.11.20: the field's disturbance now (the daily variation and storms)
        long seed = com.miophas.singularity_iteration.common.energy.GeomagPhysics.seed(level);
        player.sendSystemMessage(Component.translatable("message.mio_icif.geomagnetic_detector.kp",
            String.format("%.1f", com.miophas.singularity_iteration.common.energy.GeomagPhysics.kp(seed, level.getDayTime())),
            String.format("%.2f", com.miophas.singularity_iteration.common.energy.GeomagPhysics.disturbance(seed, level.getDayTime()))));

        return InteractionResult.SUCCESS;
    }

    /** 0.1.11.20: the generator's rule (from the bottom of the world; the old one gave 0 or less below Y 0). */
    private float getMagneticSource(Level world, BlockPos pos) {
        return com.miophas.singularity_iteration.common.energy.GeomagPhysics.source(world, pos);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("tooltip.mio_icif.geomagnetic_detector.desc"));
    }

    @Override
    public long getEnergyPerUse() { return ENERGY_COST; }

    @Override
    public int getToolTier() { return 1; }

    /** A tool on the battery base: right click keeps its tool action, no inventory auto-charge. */
    @Override
    public boolean supportsAutoCharge() {
        return false;
    }
}
