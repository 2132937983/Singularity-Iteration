package com.miophas.singularity_iteration.common.item.tools;

import com.miophas.singularity_iteration.core.api.item.IWeaponItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

@SuppressWarnings("null")
public class mio_icif_rocket_launcher extends mio_icif_tool_elc implements IWeaponItem, com.miophas.singularity_iteration.core.api.item.IBallisticWeapon {
    /**
     * 0.1.11.3: blocks per tick. The rocket used to move twice per tick at 2.5 (about 5 blocks); it now moves once, with
     * every block of the way checked, so the launch speed is 5 to keep its range and flight time.
     */
    public static final float ROCKET_SPEED = 5.0F;

    public static final int MAX_ENERGY = 1000000;
    public static final int CHARGE_RATE = 512;
    public static final int TIER = 3;
    /** 0.1.11.197 (T4, D5): 20,000 EU a rocket (it was 50,000), one shot a second (it was two). */
    public static final int ENERGY_PER_SHOT = 20000;
    public static final int COOLDOWN_TICKS = 20;
    /** 0.1.11.197 (T4, D5): blast power 3, about 43 at the centre (power 5 gave about 71, plus 10 more in a ring). */
    public static final float EXPLOSION_POWER = 3.0F;

    public static EntityType<mio_icif_rocket_entity> ROCKET_ENTITY;

    public static void setRocketEntity(EntityType<mio_icif_rocket_entity> entityType) {
        ROCKET_ENTITY = entityType;
    }

    public mio_icif_rocket_launcher(Properties properties) {
        super(properties, MAX_ENERGY, 0L, "rocket_launcher", CHARGE_RATE, 100, TIER);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!hasEnoughEnergy(stack, ENERGY_PER_SHOT)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.mio_icif.rocket_launcher.no_energy"), true);
            }
            return InteractionResultHolder.fail(stack);
        }

        ItemStack ammo = findAmmo(player);
        if (ammo.isEmpty()) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.mio_icif.rocket_launcher.no_ammo"), true);
            }
            return InteractionResultHolder.fail(stack);
        }

        if (!level.isClientSide) {
            if (ROCKET_ENTITY == null) {
                return InteractionResultHolder.fail(stack);
            }

            if (!consumeEnergy(stack, ENERGY_PER_SHOT)) return InteractionResultHolder.fail(stack);
            if (!player.hasInfiniteMaterials()) ammo.shrink(1);   // 0.1.11.197 (T4, W-17): creative keeps its rockets

            mio_icif_rocket_entity rocket = new mio_icif_rocket_entity(ROCKET_ENTITY, player, level);
            rocket.setDamage(0);
            rocket.setExplosionPower(EXPLOSION_POWER);
            rocket.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, ROCKET_SPEED, 0.0F);
            level.addFreshEntity(rocket);

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                com.miophas.singularity_iteration.common.registry.mio_icif_sounds.ROCKET_LAUNCH.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.95F + level.random.nextFloat() * 0.1F);
        }

        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private ItemStack findAmmo(Player player) {
        if (mio_icif_items_tools.ROCKET.get() == null) return ItemStack.EMPTY;

        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!mainHand.isEmpty() && mainHand.getItem() == mio_icif_items_tools.ROCKET.get()) return mainHand;

        ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);
        if (!offHand.isEmpty() && offHand.getItem() == mio_icif_items_tools.ROCKET.get()) return offHand;

        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && s.getItem() == mio_icif_items_tools.ROCKET.get()) {
                return s;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.mio_icif.rocket_launcher.ammo"));
    }

    @Override
    public float getDamage() {
        return 0;
    }

    @Override
    public float getRange() {
        return ROCKET_SPEED * 20;   // 0.1.11.197 (T4, W-15): the real launch speed (it was the old 2.5)
    }

    @Override
    public long getEnergyPerShot() {
        return ENERGY_PER_SHOT;
    }

    @Override
    public boolean isRanged() {
        return true;
    }

    /** 0.1.8.0 ballistic calculator profile (see IBallisticWeapon). */
    @Override
    public com.miophas.singularity_iteration.core.api.item.BallisticProfile ballistics(ItemStack stack, net.minecraft.world.entity.LivingEntity shooter, int useTicks) {
        return com.miophas.singularity_iteration.core.api.item.BallisticProfile.thrown("ROCKET", ROCKET_SPEED, canShoot(stack));   // 0.1.11.3: the real speed
    }

    /**
     * 0.1.8.4: shot of an AI shooter (Wingman). No rocket item is used; the shot costs the
     * launcher charge. The blast hurts entities but breaks no blocks.
     */
    @Override
    public int fireFrom(ItemStack stack, net.minecraft.world.entity.LivingEntity shooter, net.minecraft.world.phys.Vec3 direction, boolean mainHand) {
        if (shooter.level().isClientSide || ROCKET_ENTITY == null || direction.lengthSqr() < 1.0E-6) return -1;
        if (!hasEnoughEnergy(stack, ENERGY_PER_SHOT) || !consumeEnergy(stack, ENERGY_PER_SHOT)) return -1;
        mio_icif_rocket_entity rocket = new mio_icif_rocket_entity(ROCKET_ENTITY, shooter, shooter.level());
        rocket.setDamage(0);
        rocket.setExplosionPower(EXPLOSION_POWER);
        rocket.setTerrainDamage(false);
        var d = direction.normalize();
        rocket.shoot(d.x, d.y, d.z, ROCKET_SPEED, 0.0F);
        shooter.level().addFreshEntity(rocket);
        shooter.level().playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(),
            com.miophas.singularity_iteration.common.registry.mio_icif_sounds.ROCKET_LAUNCH.get(), net.minecraft.sounds.SoundSource.NEUTRAL, 1.0F, 1.0F);
        return COOLDOWN_TICKS * 3;
    }

    @Override
    public double projectileSpeed() { return ROCKET_SPEED; }   // 0.1.11.197 (T4, W-15): Wingmen led their aim at half speed

    @Override
    public double[] engagementRange() { return new double[]{12, 24}; }
}
