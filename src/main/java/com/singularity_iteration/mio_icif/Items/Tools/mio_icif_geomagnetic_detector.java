package com.singularity_iteration.mio_icif.Items.Tools;

import com.singularity_iteration.mio_icif.Items.Normal.mio_icif_bat;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
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
public class mio_icif_geomagnetic_detector extends mio_icif_bat {

    private static final int MAX_ENERGY = 50000;
    private static final int TRANSFER_SPEED = 128;
    private static final int ENERGY_COST = 100;

    public mio_icif_geomagnetic_detector() {
        super(new Item.Properties(), MAX_ENERGY, MAX_ENERGY, "item_geomagnetic_detector", TRANSFER_SPEED);
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

        extractEnergy(stack, ENERGY_COST);

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

        return InteractionResult.SUCCESS;
    }

    /**
     * 计算地磁强度
     * 基于高度和周围方块计算
     */
    private float getMagneticSource(Level world, BlockPos pos) {
        float source = 1.0F;
        int seaLevel = world.getSeaLevel();
        int seaLevelDelta = pos.getY() - seaLevel;

        // 海平面以下，越低强度越低
        if (seaLevelDelta < 0) {
            source = (float) pos.getY() / (float) (seaLevel + 1);
        }

        // 计算下方实心方块比例
        int baseDelta = pos.getY() >= 20 ? 20 : pos.getY();
        int airBlockCount = 0;

        for (int i = 0; i < baseDelta; i++) {
            BlockState state = world.getBlockState(pos.below(i));
            if (state.is(Blocks.AIR) || state.is(Blocks.WATER)) {
                airBlockCount++;
            }
        }

        if (baseDelta != 0) {
            source *= (float) (baseDelta - airBlockCount) / (float) baseDelta;
        } else {
            source = 0;
        }

        return source;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("tooltip.mio_icif.geomagnetic_detector.desc"));
    }
}
