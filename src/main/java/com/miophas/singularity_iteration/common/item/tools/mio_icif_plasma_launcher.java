package com.miophas.singularity_iteration.common.item.tools;

import com.miophas.singularity_iteration.core.api.item.IWeaponItem;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 等离子发射器 - 对齐 IC2 1.7.10 的 {@code PlasmaLauncher}。
 *
 * <p>原版没有开关状态：右键直接发射一颗等离子弹（对应原版 {@code EntityParticle}），
 * 弹体沿视线直线无重力飞行，路径上熔炼方块 / 清除水与岩浆 / 点燃易燃方块，命中时产生 Heat 爆炸。
 *
 * <p>各项数值与原版一致：最大储能 40000EU，传输限制 128EU/t，等级 3（HV），每次发射消耗 100EU。
 *
 * <p>0.1.10.4: the bolt flies exactly along the view (it no longer takes the shooter's movement,
 * which pushed it sideways while strafing or flying) and keeps that direction on the client (see
 * {@link IndependentElectricProjectile}). A charge flash at the muzzle. An Armory Wingman fires it
 * too ({@link IWeaponItem#fireFrom}): its bolts hit for {@link #AI_HIT_DAMAGE} and set the target
 * on fire, but never melt, burn or blast terrain.
 */
@SuppressWarnings("null")
public class mio_icif_plasma_launcher extends mio_icif_tool_elc implements IWeaponItem {

    public static final int PLASMA_MAX_ENERGY = 40000;
    public static final int PLASMA_CHARGE_RATE = 128;
    public static final int PLASMA_TIER = 3;
    public static final int PLASMA_ENERGY_PER_USE = 100;
    /** 0.1.10.4: colour of the charge flash and the bolt (violet plasma). */
    public static final int PLASMA_COLOR = 0xB45CFF;
    /** 0.1.10.4: damage of a bolt that may not blast (a Wingman's, or a player's where he may not build). */
    public static final float AI_HIT_DAMAGE = 24.0F;
    /** 0.1.10.4: ticks between two shots of an AI shooter. */
    public static final int AI_COOLDOWN = 20;

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
            // 0.1.10.4: straight along the view; shootFromRotation added the shooter's motion
            launch(level, player, player.getLookAngle());
            WeaponChargeFx.emit(player, hand, PLASMA_ENERGY_PER_USE * 40L, PLASMA_COLOR);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                com.miophas.singularity_iteration.common.registry.mio_icif_sounds.PLASMA_SHOT.get(), SoundSource.PLAYERS, 1.1F, 0.75F + level.random.nextFloat() * 0.1F);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private static void launch(Level level, LivingEntity shooter, Vec3 direction) {
        mio_icif_plasma_bullet bullet = new mio_icif_plasma_bullet(PLASMA_BULLET_ENTITY, shooter, level);
        Vec3 d = direction.normalize();
        bullet.shoot(d.x, d.y, d.z, (float) mio_icif_plasma_bullet.PLASMA_SPEED, 0.0F);
        level.addFreshEntity(bullet);
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue() {
        return 15;
    }

    // ---- 0.1.10.4: AI shooters (IWeaponItem)

    @Override public float getDamage() { return AI_HIT_DAMAGE; }
    @Override public float getRange() { return 48.0F; }
    @Override public long getEnergyPerShot() { return PLASMA_ENERGY_PER_USE; }
    @Override public boolean isRanged() { return true; }
    @Override public int getFireRate() { return AI_COOLDOWN; }
    @Override public double projectileSpeed() { return mio_icif_plasma_bullet.PLASMA_SPEED; }
    @Override public double[] engagementRange() { return new double[]{8, 26}; }

    @Override
    public int fireFrom(ItemStack stack, LivingEntity shooter, Vec3 direction, boolean mainHand) {
        Level level = shooter.level();
        if (level.isClientSide || PLASMA_BULLET_ENTITY == null || direction.lengthSqr() < 1.0E-6) return -1;
        if (!hasEnoughEnergy(stack, PLASMA_ENERGY_PER_USE) || !consumeEnergy(stack, PLASMA_ENERGY_PER_USE)) return -1;
        launch(level, shooter, direction);
        WeaponChargeFx.emit(shooter, mainHand, PLASMA_ENERGY_PER_USE * 40L, PLASMA_COLOR);
        level.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(),
            com.miophas.singularity_iteration.common.registry.mio_icif_sounds.PLASMA_SHOT.get(), SoundSource.NEUTRAL, 1.0F, 0.8F + level.random.nextFloat() * 0.1F);
        return AI_COOLDOWN;
    }
}
