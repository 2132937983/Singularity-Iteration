package com.singularity_iteration.mio_icif.Items.Tools;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import com.singularity_iteration.mio_icif.api.item.IWeaponItem;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Predicate;

@SuppressWarnings("null")
public class mio_icif_nano_bow extends mio_icif_tool_elc implements IWeaponItem {
    public static final int MAX_ENERGY = 500000;
    public static final int ENERGY_PER_SHOT = 300;
    public static final int TIER = 3;
    public static final float MAX_VELOCITY = 5.0F;

    public mio_icif_nano_bow(Properties properties) {
        super(properties, MAX_ENERGY, MAX_ENERGY, "nano_bow", MAX_ENERGY, 512, TIER);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // 没电时也可以拉弓，但不能射出箭
        // 没箭时也可以拉弓（创造模式或特殊情况下）
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entityLiving, int timeLeft) {
        if (!(entityLiving instanceof Player player)) return;

        ItemStack arrowStack = findAmmo(player);
        if (arrowStack.isEmpty()) return;
        if (!hasEnoughEnergy(stack, ENERGY_PER_SHOT)) return;

        int chargeTicks = this.getUseDuration(stack, entityLiving) - timeLeft;
        chargeTicks = net.neoforged.neoforge.event.EventHooks.onArrowLoose(stack, level, player, chargeTicks, true);
        if (chargeTicks < 0) return;

        // 使用METS原版的箭矢速度计算方法
        float arrowVelocity = getNanoArrowVelocity(chargeTicks);

        if ((double) arrowVelocity < 0.1) return;

        if (!level.isClientSide) {
            // 消耗能量
            consumeEnergy(stack, ENERGY_PER_SHOT);

            // 创建并射出箭矢
            List<ItemStack> list = draw(stack, arrowStack, player);
            if (level instanceof ServerLevel serverlevel && !list.isEmpty()) {
                this.shoot(serverlevel, player, player.getUsedItemHand(), stack, list, arrowVelocity * 3.0F, 1.0F, true, null);
            }

            // 如果不是创造模式，消耗箭矢
            if (!player.hasInfiniteMaterials()) {
                arrowStack.shrink(1);
                if (arrowStack.isEmpty()) {
                    player.getInventory().removeItem(arrowStack);
                }
            }
        }

        level.playSound(
            null,
            player.getX(),
            player.getY(),
            player.getZ(),
            SoundEvents.ARROW_SHOOT,
            SoundSource.PLAYERS,
            1.0F,
            1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F) + arrowVelocity * 0.5F
        );
        player.awardStat(Stats.ITEM_USED.get(this));
    }

    protected void shoot(
        ServerLevel level,
        LivingEntity shooter,
        InteractionHand hand,
        ItemStack weapon,
        List<ItemStack> projectileItems,
        float velocity,
        float inaccuracy,
        boolean isCrit,
        @Nullable LivingEntity target
    ) {
        float f = 0.0F;
        float f1 = projectileItems.size() == 1 ? 0.0F : 2.0F * f / (float)(projectileItems.size() - 1);
        float f2 = (float)((projectileItems.size() - 1) % 2) * f1 / 2.0F;
        float f3 = 1.0F;

        for (int i = 0; i < projectileItems.size(); i++) {
            ItemStack itemstack = projectileItems.get(i);
            if (!itemstack.isEmpty()) {
                float f4 = f2 + f3 * (float)((i + 1) / 2) * f1;
                f3 = -f3;
                Projectile projectile = this.createProjectile(level, shooter, weapon, itemstack, isCrit);
                this.shootProjectile(shooter, projectile, i, velocity, inaccuracy, f4, target);
                level.addFreshEntity(projectile);
            }
        }
    }

    protected void shootProjectile(
        LivingEntity shooter, Projectile projectile, int index, float velocity, float inaccuracy, float angle, @Nullable LivingEntity target
    ) {
        // 使用shoot而不是shootFromRotation，避免添加射击者的移动速度导致箭矢轨道偏移
        float yaw = shooter.getYRot() + angle;
        float pitch = shooter.getXRot();
        float f = -net.minecraft.util.Mth.sin(yaw * 0.017453292F) * net.minecraft.util.Mth.cos(pitch * 0.017453292F);
        float f1 = -net.minecraft.util.Mth.sin(pitch * 0.017453292F);
        float f2 = net.minecraft.util.Mth.cos(yaw * 0.017453292F) * net.minecraft.util.Mth.cos(pitch * 0.017453292F);
        projectile.shoot((double)f, (double)f1, (double)f2, velocity, inaccuracy);
    }

    protected Projectile createProjectile(Level level, LivingEntity shooter, ItemStack weapon, ItemStack ammo, boolean isCrit) {
        net.minecraft.world.entity.projectile.AbstractArrow abstractarrow = 
            ((net.minecraft.world.item.ArrowItem) ammo.getItem()).createArrow(level, ammo, shooter, weapon);
        abstractarrow.setCritArrow(true);
        abstractarrow.setNoGravity(true);
        return customArrow(abstractarrow, ammo, weapon);
    }

    protected static List<ItemStack> draw(ItemStack weapon, ItemStack ammo, LivingEntity shooter) {
        if (ammo.isEmpty()) {
            return List.of();
        } else {
            int i = 1;
            List<ItemStack> list = new java.util.ArrayList<>(i);
            ItemStack itemstack1 = ammo.copy();

            for (int j = 0; j < i; j++) {
                ItemStack itemstack = useAmmo(weapon, j == 0 ? ammo : itemstack1, shooter, j > 0);
                if (!itemstack.isEmpty()) {
                    list.add(itemstack);
                }
            }

            return list;
        }
    }

    protected static ItemStack useAmmo(ItemStack weapon, ItemStack ammo, LivingEntity shooter, boolean intangible) {
        if (!intangible && shooter.level() instanceof ServerLevel && 
            !(shooter.hasInfiniteMaterials() || (ammo.getItem() instanceof net.minecraft.world.item.ArrowItem ai && ai.isInfinite(ammo, weapon, shooter)))) {
            // 原版附魔处理
        }
        
        ItemStack itemstack1 = ammo.copyWithCount(1);
        itemstack1.set(net.minecraft.core.component.DataComponents.INTANGIBLE_PROJECTILE, net.minecraft.util.Unit.INSTANCE);
        return itemstack1;
    }

    public net.minecraft.world.entity.projectile.AbstractArrow customArrow(
        net.minecraft.world.entity.projectile.AbstractArrow arrow, ItemStack projectileStack, ItemStack weaponStack
    ) {
        return arrow;
    }

    /**
     * 使用METS原版的箭矢速度计算方法
     */
    public static float getNanoArrowVelocity(int charge) {
        float v = (float) charge;
        v = (v * v + v * 2.0F) / 5.0F;
        if (v > MAX_VELOCITY) {
            v = MAX_VELOCITY;
        }
        return v;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    public Predicate<ItemStack> getAllSupportedProjectiles() {
        return p -> p.is(net.minecraft.tags.ItemTags.ARROWS);
    }

    public int getDefaultProjectileRange() {
        return 15;
    }

    /**
     * 自定义查找箭矢的方法（模仿METS原版）
     * 因为继承的是mio_icif_tool_elc而不是ProjectileWeaponItem，所以player.getProjectile()无法工作
     */
    protected ItemStack findAmmo(Player player) {
        if (isArrow(player.getItemInHand(InteractionHand.OFF_HAND))) {
            return player.getItemInHand(InteractionHand.OFF_HAND);
        } else if (isArrow(player.getItemInHand(InteractionHand.MAIN_HAND))) {
            return player.getItemInHand(InteractionHand.MAIN_HAND);
        } else {
            for (int i = 0; i < player.getInventory().getContainerSize(); ++i) {
                ItemStack itemstack = player.getInventory().getItem(i);
                if (isArrow(itemstack)) {
                    return itemstack;
                }
            }
            return ItemStack.EMPTY;
        }
    }

    protected boolean isArrow(ItemStack stack) {
        return stack.is(ItemTags.ARROWS);
    }

    // ==================== IWeaponItem API ====================

    @Override
    public float getDamage() {
        return 6.0F;
    }

    @Override
    public float getRange() {
        return getDefaultProjectileRange();
    }

    @Override
    public long getEnergyPerShot() {
        return ENERGY_PER_SHOT;
    }

    @Override
    public boolean isRanged() {
        return true;
    }
}