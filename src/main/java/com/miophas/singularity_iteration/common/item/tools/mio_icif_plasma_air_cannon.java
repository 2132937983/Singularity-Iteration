package com.miophas.singularity_iteration.common.item.tools;

import com.miophas.singularity_iteration.core.api.item.IAirCannonItem;

import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

@SuppressWarnings("null")
public class mio_icif_plasma_air_cannon extends mio_icif_tool_elc implements IAirCannonItem {
    public static final int MAX_ENERGY = 200000;
    public static final int CHARGE_RATE = 128;
    public static final int TIER = 2;
    public static final int BASE_COST = 1000;
    public static final float BASE_DAMAGE = 10.0F;
    public static final int MAX_CHARGE = 240;
    public static final int MIN_CHARGE = 10;

    public mio_icif_plasma_air_cannon(Properties properties) {
        super(properties, MAX_ENERGY, MAX_ENERGY, "plasma_air_cannon", CHARGE_RATE, 100, TIER);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!hasEnoughEnergy(stack, BASE_COST)) {
            if (!level.isClientSide) {
                player.sendSystemMessage(Component.translatable("message.mio_icif.plasma_air_cannon.no_energy"));
            }
            return InteractionResultHolder.fail(stack);
        }

        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return MAX_CHARGE;
    }

    /** Compressor whine while charging, re-triggered every 8 ticks with rising pitch. */
    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        int charged = getUseDuration(stack, entity) - remainingUseDuration;
        if (!level.isClientSide && charged % 8 == 0 && charged <= 120) {
            float p = 0.7F + Math.min(1F, charged / 120F) * 0.9F;
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), com.miophas.singularity_iteration.common.registry.mio_icif_sounds.AIR_CANNON_CHARGE.get(),
                net.minecraft.sounds.SoundSource.PLAYERS, 0.45F, p);
        }
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeCharged) {
        if (!(entity instanceof Player player)) return;

        int chargeTicks = getUseDuration(stack, entity) - timeCharged;
        if (chargeTicks < MIN_CHARGE) return;

        float expandSize = chargeTicks;
        
        if (!level.isClientSide) {
            float pitch = player.getXRot();
            float yaw = player.getYRot();
            
            Vec3 currentPosition = player.position();
            Vec3 lookDirection = new Vec3(
                -Mth.sin(yaw * 0.0174F) * Mth.cos(pitch * 0.0174F),
                -Mth.sin(pitch * 0.0174F),
                Mth.cos(yaw * 0.0174F) * Mth.cos(pitch * 0.0174F)
            );
            Vec3 targetPosition = new Vec3(
                player.getX() + lookDirection.x * expandSize,
                player.getY() + lookDirection.y * expandSize,
                player.getZ() + lookDirection.z * expandSize
            );

            AABB area = new AABB(
                currentPosition.x, currentPosition.y, currentPosition.z,
                targetPosition.x, targetPosition.y, targetPosition.z
            ).inflate(1.0f, 1.0f, 1.0f);

            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                e -> e != player && e.isAlive() && e.isPickable());

            double velocity = expandSize / 3.0;
            for (LivingEntity target : targets) {
                double distance = currentPosition.distanceTo(target.position());
                double damage = BASE_DAMAGE + expandSize / (distance == 0 ? 1.0 : distance);

                target.push(lookDirection.x * velocity, lookDirection.y * velocity, lookDirection.z * velocity);
                target.hurt(level.damageSources().playerAttack(player), (float) damage);
                level.playSound(null, target.getX(), target.getY(), target.getZ(), com.miophas.singularity_iteration.common.registry.mio_icif_sounds.AIR_CANNON_HIT.get(),
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.9F, 0.9F + level.random.nextFloat() * 0.2F);
            }

            consumeEnergy(stack, BASE_COST);

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                com.miophas.singularity_iteration.common.registry.mio_icif_sounds.AIR_CANNON_BLAST.get(), net.minecraft.sounds.SoundSource.PLAYERS, 
                Math.min(3.0F, 1.0F + expandSize / 60.0F), 1.05F - Math.min(0.3F, expandSize / 600.0F));

            for (int i = 0; i < 15; i++) {
                float newYaw = yaw + (level.random.nextFloat() - 0.5F) * 16.0F;
                float newPitch = pitch + (level.random.nextFloat() - 0.5F) * 16.0F;
                Vec3 shootDirection = new Vec3(
                    -Mth.sin(newYaw * 0.0174F) * Mth.cos(newPitch * 0.0174F),
                    -Mth.sin(newPitch * 0.0174F),
                    Mth.cos(newYaw * 0.0174F) * Mth.cos(newPitch * 0.0174F)
                );
                // server side: sendParticles reaches every client (addParticle on the server is a no-op)
                if (level instanceof net.minecraft.server.level.ServerLevel server) {
                    server.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,
                        player.getX() + lookDirection.x, player.getEyeY() - 0.2, player.getZ() + lookDirection.z, 0,
                        shootDirection.x, shootDirection.y, shootDirection.z, Math.min(2.0, 0.3 + velocity * 0.1));
                }
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.mio_icif.plasma_air_cannon.damage", BASE_DAMAGE));
    }
}