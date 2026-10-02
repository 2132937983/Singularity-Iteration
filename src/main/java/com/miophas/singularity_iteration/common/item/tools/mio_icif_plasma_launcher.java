package com.miophas.singularity_iteration.common.item.tools;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 等离子发射器 - 对齐 IC2 1.7.10 的 {@code PlasmaLauncher}。
 *
 * <p>原版没有开关状态：右键直接发射一颗等离子弹（对应原版 {@code EntityParticle}），
 * 弹体沿视线直线无重力飞行，路径上熔炼方块 / 清除水与岩浆 / 点燃易燃方块，命中时产生 Heat 爆炸。
 *
 * <p>各项数值与原版一致：最大储能 40000EU，传输限制 128EU/t，等级 3（HV），每次发射消耗 100EU。
 */
@SuppressWarnings("null")
public class mio_icif_plasma_launcher extends mio_icif_tool_elc {

    public static final int PLASMA_MAX_ENERGY = 40000;
    public static final int PLASMA_CHARGE_RATE = 128;
    public static final int PLASMA_TIER = 3;
    public static final int PLASMA_ENERGY_PER_USE = 100;

    public static EntityType<mio_icif_plasma_bullet> PLASMA_BULLET_ENTITY;

    public mio_icif_plasma_launcher(Properties properties) {
        super(properties, PLASMA_MAX_ENERGY, PLASMA_MAX_ENERGY, "plasma_launcher",
            PLASMA_CHARGE_RATE, PLASMA_ENERGY_PER_USE, PLASMA_TIER);
    }

    public static void setPlasmaBulletEntity(EntityType<mio_icif_plasma_bullet> entityType) {
        PLASMA_BULLET_ENTITY = entityType;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!hasEnoughEnergy(stack, PLASMA_ENERGY_PER_USE)) {
            if (!level.isClientSide) {
                player.sendSystemMessage(Component.translatable("message.mio_icif.plasma_launcher.no_energy"));
            }
            return InteractionResultHolder.fail(stack);
        }

        if (!level.isClientSide) {
            if (PLASMA_BULLET_ENTITY == null) {
                return InteractionResultHolder.fail(stack);
            }

            consumeEnergy(stack, PLASMA_ENERGY_PER_USE);

            mio_icif_plasma_bullet bullet = new mio_icif_plasma_bullet(PLASMA_BULLET_ENTITY, player, level);
            bullet.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F,
                (float) mio_icif_plasma_bullet.PLASMA_SPEED, 0.0F);
            level.addFreshEntity(bullet);

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                com.miophas.singularity_iteration.common.registry.mio_icif_sounds.PLASMA_SHOT.get(), SoundSource.PLAYERS, 1.1F, 0.75F + level.random.nextFloat() * 0.1F);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue() {
        return 15;
    }
}
