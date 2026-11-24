// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.item.tools;

import com.miophas.singularity_iteration.core.api.item.BallisticProfile;
import com.miophas.singularity_iteration.core.api.item.IBallisticWeapon;
import com.miophas.singularity_iteration.core.api.item.IWeaponItem;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The energy-bolt guns (electric rifle, advanced rifle, tactical laser rifle, electric plasma gun, tachyon disruptor),
 * 0.1.11.197 (tool overhaul T4). They were five copies of the same use / fireFrom / ballistics code with other
 * constants. A gun now declares a {@link Spec}; a player's shot and an AI shooter's shot (Wingman) build the same bolt.
 */
public abstract class AbstractEnergyGun extends mio_icif_tool_elc implements IWeaponItem, IBallisticWeapon {

    /**
     * @param name     the gun's id stem (message and tooltip keys {@code message.mio_icif.<name>.no_energy}, {@code tooltip.mio_icif.<name>.damage})
     * @param profile  label of the ballistic calculator
     * @param color    beam and charge-flash colour
     * @param damage   damage of a bolt
     * @param speed    bolt speed (blocks a tick)
     * @param maxLife  ticks before the bolt fades
     * @param cooldown ticks between shots
     */
    public record Spec(String name, String profile, int color, float damage, float speed, int maxLife, int cooldown,
                       Supplier<SoundEvent> sound, float volume, float pitch) {}

    private final Spec spec;

    protected AbstractEnergyGun(Properties properties, long maxEnergy, long chargeRate, int tier, long energyPerShot, Spec spec) {
        super(properties, maxEnergy, 0L, spec.name(), chargeRate, energyPerShot, tier);
        this.spec = spec;
    }

    public Spec spec() {
        return spec;
    }

    /** The bolt entity type, set at registration. */
    protected abstract EntityType<mio_icif_energy_bullet> bulletType();

    /** Damage of one bolt from {@code stack} (the tachyon disruptor's tuning changes it). */
    public float damage(ItemStack stack) {
        return spec.damage();
    }

    /** EU a shot takes before Energy Saving (the tachyon disruptor's tuning changes it). */
    public long shotCost(ItemStack stack) {
        return getEnergyPerUse();
    }

    /** What a gun adds to its bolt (area, explosion, piercing). */
    protected void configure(mio_icif_energy_bullet bullet, ItemStack stack) {}

    /** The shot sound of a player's shot; the tachyon disruptor makes a heavier shot louder. */
    protected void playShot(Level level, Player player, ItemStack stack, long cost) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(), spec.sound().get(), SoundSource.PLAYERS,
            spec.volume(), spec.pitch() + level.random.nextFloat() * 0.08F);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        long cost = shotCost(stack);
        if (!hasEnoughEnergy(stack, cost)) {
            // 0.1.11.197 (W-18): on the action bar; holding use filled the chat
            if (!level.isClientSide) player.displayClientMessage(Component.translatable("message.mio_icif." + spec.name() + ".no_energy"), true);
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide) {
            if (bulletType() == null || !consumeEnergy(stack, cost)) return InteractionResultHolder.fail(stack);
            mio_icif_energy_bullet bullet = new mio_icif_energy_bullet(bulletType(), player, level);
            bullet.setBeam(spec.color(), getEnergyPerUse());
            bullet.setDamage(damage(stack));
            bullet.setMaxLife(spec.maxLife());
            configure(bullet, stack);
            bullet.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, spec.speed(), 0.0F);
            level.addFreshEntity(bullet);
            WeaponChargeFx.emit(player, hand, getEnergyPerUse(), spec.color());
            playShot(level, player, stack, cost);
        }
        player.getCooldowns().addCooldown(this, spec.cooldown());
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** An AI shooter's shot (Wingman): the same bolt as {@link #use}. */
    @Override
    public int fireFrom(ItemStack stack, LivingEntity shooter, Vec3 direction, boolean mainHand) {
        return MobWeaponFire.energyBolt(this, stack, shooter, direction, mainHand, bulletType(), shotCost(stack), spec.color(),
            damage(stack), spec.maxLife(), spec.speed(), bullet -> configure(bullet, stack), spec.sound().get(), spec.volume(),
            spec.pitch(), spec.cooldown());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.mio_icif." + spec.name() + ".damage", String.format("%.0f", damage(stack))));
    }

    /**
     * 0.1.11.197 (T4, W-08): melee attributes of a gun that hits in close quarters: a sword's attack speed (1.6 a second)
     * and {@code damage} added while a hit can be paid. The bonus used to be a hidden constant that needed no energy.
     */
    protected static net.minecraft.world.item.component.ItemAttributeModifiers meleeAttributes(double damage) {
        var builder = net.minecraft.world.item.component.ItemAttributeModifiers.builder()
            .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,
                new net.minecraft.world.entity.ai.attributes.AttributeModifier(BASE_ATTACK_SPEED_ID, -2.4,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND);
        if (damage > 0) {
            builder.add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
                new net.minecraft.world.entity.ai.attributes.AttributeModifier(BASE_ATTACK_DAMAGE_ID, damage,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND);
        }
        return builder.build();
    }

    @Override
    public float getDamage() {
        return spec.damage();
    }

    /** The range an AI shooter engages at (a second of bolt flight), not the bolt's full reach. */
    @Override
    public float getRange() {
        return spec.speed() * 20;
    }

    @Override
    public long getEnergyPerShot() {
        return getEnergyPerUse();
    }

    @Override
    public boolean isRanged() {
        return true;
    }

    @Override
    public BallisticProfile ballistics(ItemStack stack, LivingEntity shooter, int useTicks) {
        return BallisticProfile.straight(spec.profile(), spec.speed(), 0.99, 0.8, canShoot(stack));
    }

    @Override
    public double projectileSpeed() {
        return spec.speed();
    }
}
