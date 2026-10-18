// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.item.tools;

import com.miophas.singularity_iteration.common.Singularity_Iteration_Config;
import com.miophas.singularity_iteration.core.api.tool.ToggleableElectricTool;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Ten SI-declared modes with independent, explicit candidate tuning. */
@SuppressWarnings("deprecation")
public class mio_icif_laser_miner extends mio_icif_tool_elc implements com.miophas.singularity_iteration.core.api.tool.IToolModeProvider,
        com.miophas.singularity_iteration.core.api.item.IWeaponItem {
    public static final long LASER_MAX_ENERGY = 300_000;
    public static EntityType<mio_icif_laser_bullet> LASER_BULLET_ENTITY;
    private static final String MODE_KEY = "ScexLaserMode";

    // Mode names come from admitted SI resources; numeric parity still requires gameplay observation.
    public enum Mode {
        MINING("mining", 1_250, 8, 64, 8, 0),
        LOW_FOCUS("low_focus", 100, 1, 16, 2, 0),
        LONG_RANGE("long_range", 5_000, 16, 128, 12, 0),
        HORIZONTAL("horizontal", 3_000, 8, 64, 8, 0),
        COMBAT("combat", 10_000, 0, 64, 20, 0),
        SUPER_HEAT("super_heat", 2_500, 8, 64, 8, 0),
        SCATTER("scatter", 10_000, 3, 32, 4, 0),
        EXPLOSIVE("explosive", 5_000, 0, 64, 8, 2),
        THREE_BY_THREE("3x3", 10_000, 1, 64, 4, 0),
        NUCLEAR_EXPLOSIVE("nuclear_explosive", 100_000, 0, 64, 20, 18.566355F);

        final String translation;
        final long cost;
        final int blocks;
        final double range;
        final float damage;
        final float explosion;
        Mode(String translation, long cost, int blocks, double range, float damage, float explosion) {
            this.translation = translation; this.cost = cost; this.blocks = blocks;
            this.range = range; this.damage = damage; this.explosion = explosion;
        }
        public static Mode byIndex(int index) {
            return index >= 0 && index < values().length ? values()[index] : MINING;
        }
        public Component displayName() { return Component.translatable("hud.mio_icif.laser_miner.mode_" + translation); }
    }
    public mio_icif_laser_miner(Properties properties) {
        super(properties, LASER_MAX_ENERGY, 0, "laser_miner", 512, Mode.MINING.cost, 3);
    }
    public static void setLaserBulletEntity(EntityType<mio_icif_laser_bullet> type) { LASER_BULLET_ENTITY = type; }
    public static Mode getMode(ItemStack stack) {
        return Mode.byIndex(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt(MODE_KEY));
    }
    @Override public void toggleActive(ItemStack stack, Player player) {
        if (player.level().isClientSide) return;
        Mode next = Mode.byIndex((getMode(stack).ordinal() + 1) % Mode.values().length);
        var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        data.putInt(MODE_KEY, next.ordinal());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        player.displayClientMessage(next.displayName(), true);
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            toggleActive(stack, player);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        Mode mode = getMode(stack);
        if (mode == Mode.NUCLEAR_EXPLOSIVE && !Singularity_Iteration_Config.ENABLE_LASER_NUCLEAR_EXPLOSIVE.get()) {
            if (!level.isClientSide) player.displayClientMessage(Component.translatable("hud.mio_icif.laser_miner.disabled", mode.displayName()), true);
            return InteractionResultHolder.fail(stack);
        }
        if (!hasEnoughEnergy(stack, mode.cost) || LASER_BULLET_ENTITY == null) return InteractionResultHolder.fail(stack);
        if (level.isClientSide) return InteractionResultHolder.consume(stack);   // no melee swing: the gun recoils instead
        if (!fire(stack, player, mode, player.getYRot(), player.getXRot())) return InteractionResultHolder.fail(stack);
        player.getCooldowns().addCooldown(this, 4);
        WeaponChargeFx.emit(player, hand, mode.cost, 0xFF9F1C);   // 0.1.8.0 charge / compression effect
        level.playSound(null, player.getX(), player.getY(), player.getZ(), com.miophas.singularity_iteration.common.registry.mio_icif_sounds.LASER_SHOT.get(), SoundSource.PLAYERS, 0.7F, 0.95F + level.random.nextFloat() * 0.1F);
        return InteractionResultHolder.success(stack);
    }

    /** The bolts of one shot from {@code shooter} along yaw/pitch; pays {@code mode.cost}. False: nothing fired. */
    private boolean fire(ItemStack stack, net.minecraft.world.entity.LivingEntity shooter, Mode mode, float yawDeg, float pitchDeg) {
        Level level = shooter.level();
        var bullets = new ArrayList<mio_icif_laser_bullet>();
        boolean multiple = mode == Mode.SCATTER || mode == Mode.THREE_BY_THREE;
        int radius = multiple ? 1 : 0;
        Vec3 direction = Vec3.directionFromRotation(pitchDeg, yawDeg).normalize();
        Vec3 right = Vec3.directionFromRotation(0, yawDeg + 90);
        Vec3 up = right.cross(direction).normalize();
        for (int x = -radius; x <= radius; x++) for (int y = -radius; y <= radius; y++) {
            var bullet = new mio_icif_laser_bullet(LASER_BULLET_ENTITY, shooter, level);
            bullet.configure(mode);
            if (mode == Mode.THREE_BY_THREE) bullet.setPos(bullet.position().add(right.scale(x)).add(up.scale(y)));
            float pitch = mode == Mode.HORIZONTAL ? 0 : pitchDeg;
            float yaw = yawDeg;
            if (mode == Mode.SCATTER) { pitch += y * 5; yaw += x * 5; }
            // A straight beam does not inherit the shooter's movement.
            Vec3 velocity = Vec3.directionFromRotation(pitch, yaw);
            bullet.shoot(velocity.x, velocity.y, velocity.z, 3.0F, 0.0F);
            bullets.add(bullet);
        }
        if (!consumeEnergy(stack, mode.cost)) return false;
        for (var bullet : bullets) {
            if (!level.addFreshEntity(bullet)) {
                for (var spawned : bullets) spawned.discard();
                addEnergy(stack, mode.cost);
                return false;
            }
        }
        return true;
    }

    // ---- 0.1.10.4: AI shooters (Armory Wingman, IWeaponItem)
    //
    // A Wingman fires the selected mode. Its bolts never mine or blast (the laser bolt breaks blocks
    // and explodes only for a player who may build), so the mining, explosive and nuclear modes hit
    // living targets with their damage only; a mode without damage is fired as the combat mode.

    /** Ticks between two shots of an AI shooter (a player: 4). */
    public static final int AI_COOLDOWN = 10;

    private static Mode aiMode(ItemStack stack) {
        Mode mode = getMode(stack);
        return mode.damage <= 0 || mode == Mode.NUCLEAR_EXPLOSIVE ? Mode.COMBAT : mode;
    }

    @Override public float getDamage() { return Mode.COMBAT.damage; }
    @Override public float getRange() { return (float) Mode.MINING.range; }
    @Override public long getEnergyPerShot() { return Mode.MINING.cost; }
    @Override public boolean isRanged() { return true; }
    @Override public int getFireRate() { return AI_COOLDOWN; }
    @Override public double projectileSpeed() { return 3.0; }
    @Override public double[] engagementRange() { return new double[]{8, 24}; }
    @Override public boolean canShoot(ItemStack stack) { return hasEnoughEnergy(stack, aiMode(stack).cost); }

    @Override
    public int fireFrom(ItemStack stack, net.minecraft.world.entity.LivingEntity shooter, Vec3 direction, boolean mainHand) {
        Level level = shooter.level();
        if (level.isClientSide || LASER_BULLET_ENTITY == null || direction.lengthSqr() < 1.0E-6) return -1;
        Mode mode = aiMode(stack);
        if (!hasEnoughEnergy(stack, mode.cost)) return -1;
        Vec3 d = direction.normalize();
        float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
        if (!fire(stack, shooter, mode, yaw, pitch)) return -1;
        WeaponChargeFx.emit(shooter, mainHand, mode.cost, 0xFF9F1C);
        level.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(), com.miophas.singularity_iteration.common.registry.mio_icif_sounds.LASER_SHOT.get(),
            SoundSource.NEUTRAL, 0.7F, 0.95F + level.random.nextFloat() * 0.1F);
        return AI_COOLDOWN;
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(getMode(stack).displayName());
    }

    // ---- equipment console (IToolModeProvider)
    @Override public java.util.List<net.minecraft.network.chat.Component> toolModes(ItemStack stack) {
        java.util.List<net.minecraft.network.chat.Component> out = new java.util.ArrayList<>();
        for (Mode mode : Mode.values()) out.add(mode.displayName());
        return out;
    }
    @Override public int toolModeIndex(ItemStack stack) { return getMode(stack).ordinal(); }
    @Override public java.util.List<net.minecraft.network.chat.Component> toolModeDetails(ItemStack stack, int index) {
        Mode mode = Mode.byIndex(index);
        java.util.List<net.minecraft.network.chat.Component> out = new java.util.ArrayList<>();
        out.add(net.minecraft.network.chat.Component.translatable("tool_mode.mio_icif.detail.cost", mode.cost));
        out.add(net.minecraft.network.chat.Component.translatable("tool_mode.mio_icif.detail.range", (int) mode.range));
        if (mode.blocks > 0) out.add(net.minecraft.network.chat.Component.translatable("tool_mode.mio_icif.detail.blocks", mode.blocks));
        if (mode.damage > 0) out.add(net.minecraft.network.chat.Component.translatable("tool_mode.mio_icif.detail.damage", (int) mode.damage));
        if (mode.explosion > 0) out.add(net.minecraft.network.chat.Component.translatable("tool_mode.mio_icif.detail.explosion", String.format("%.1f", mode.explosion)));
        return out;
    }
    @Override public void selectToolMode(ItemStack stack, Player player, int index) {
        if (index < 0 || index >= Mode.values().length) return;
        var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        data.putInt(MODE_KEY, index);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
    }
}
