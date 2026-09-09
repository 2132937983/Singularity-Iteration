package com.singularity_iteration.mio_icif.Items.Armor;

import com.singularity_iteration.mio_icif.api.item.ArmorFeatureInfo;
import com.singularity_iteration.mio_icif.api.item.IJetpackItem;
import com.singularity_iteration.mio_icif.util.JetpackKeyHandler;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Quantum Chestplate (QuantumSuit Bodyarmor)
 * Based on IC2 ItemArmorQuantumSuit.class (armorType = 1)
 * 
 * Features:
 * - Fire resistance (toggleable, free when powered)
 * - Jetpack mode (switchable Jetpack/Hover, consumes energy when flying)
 * - Damage energy absorption (consumes energy when damaged, handled in mio_icif_events)
 */
@SuppressWarnings("null")
public class mio_icif_chestplate_quantum extends mio_icif_armor_elc implements IJetpackItem {

    public static final int MAX_ENERGY = 10000000;
    public static final int ENERGY_PER_DAMAGE = 20000;

    // Mode constants
    private static final String MODE_KEY = "JetpackMode";
    private static final String TOGGLE_TIMER_KEY = "QuantumToggleTimer";
    public static final int MODE_JETPACK = 0;
    public static final int MODE_HOVER = 1;
    private static final int TOGGLE_COOLDOWN = 10;

    // Energy consumption
    public static final int JETPACK_CONSUME_PER_TICK = 8;
    public static final int HOVER_CONSUME_PER_TICK = 10;

    // Flight parameters
    public static final float JETPACK_POWER = 1.0F;
    public static final float MAX_ASCENT_SPEED = 0.8F;
    public static final float HOVER_ASCENT_SPEED = 0.6F;
    public static final float HOVER_DESCENT_SPEED = -0.6F;
    public static final float WORLD_HEIGHT_DIVISOR = 1.28F;
    public static final float DROP_PERCENTAGE = 0.05F;

    // Flight state tracking
    private static final Map<Player, Boolean> flyingPlayers = new WeakHashMap<>();

    public mio_icif_chestplate_quantum(Holder<ArmorMaterial> material, Properties properties) {
        super(material, Type.CHESTPLATE, properties, MAX_ENERGY, 0, "quantum", 12000, 0, 6);
    }

    @Override
    public float getDamageAbsorptionRatio(EquipmentSlot slot) {
        return slot == EquipmentSlot.CHEST ? 0.48F : 0.0F;
    }

    public int getModeInternal(ItemStack stack) {
        net.minecraft.world.item.component.CustomData customData = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (customData != null) {
            net.minecraft.nbt.CompoundTag tag = customData.copyTag();
            if (tag.contains(MODE_KEY)) {
                return tag.getInt(MODE_KEY);
            }
        }
        return MODE_JETPACK;
    }

    public void setModeInternal(ItemStack stack, int mode) {
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        net.minecraft.world.item.component.CustomData customData = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (customData != null) {
            tag = customData.copyTag();
        }
        tag.putInt(MODE_KEY, mode);
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
    }

    public int getToggleTimer(ItemStack stack) {
        net.minecraft.world.item.component.CustomData customData = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (customData != null) {
            net.minecraft.nbt.CompoundTag tag = customData.copyTag();
            if (tag.contains(TOGGLE_TIMER_KEY)) {
                return tag.getInt(TOGGLE_TIMER_KEY);
            }
        }
        return 0;
    }

    public void setToggleTimer(ItemStack stack, int timer) {
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        net.minecraft.world.item.component.CustomData customData = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (customData != null) {
            tag = customData.copyTag();
        }
        tag.putInt(TOGGLE_TIMER_KEY, timer);
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
    }

    public Component getModeName(int mode) {
        return switch (mode) {
            case MODE_JETPACK -> Component.translatable("hud.mio_icif.jetpack.mode_jetpack");
            case MODE_HOVER -> Component.translatable("hud.mio_icif.jetpack.mode_hover");
            default -> Component.translatable("hud.mio_icif.jetpack.mode_jetpack");
        };
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        if (!(entity instanceof Player player)) return;

        // Get actual equipped chestplate
        ItemStack actualStack = player.getItemBySlot(EquipmentSlot.CHEST);
        boolean isWearing = !actualStack.isEmpty() && actualStack.getItem() == this;
        
        if (!isWearing) {
            flyingPlayers.remove(player);
            return;
        }

        int currentMode = getModeInternal(actualStack);
        long currentEnergy = getEnergy(actualStack);
        int toggleTimer = getToggleTimer(actualStack);

        // Client-side HUD display
        if (level.isClientSide && player.tickCount % 20 == 0) {
            Component modeName = getModeName(currentMode);
            player.displayClientMessage(
                Component.translatable("hud.mio_icif.jetpack.display",
                    modeName, getEnergy(actualStack), MAX_ENERGY), true);
        }

        // Fire resistance effect
        if (!level.isClientSide) {
            if (!isEmpty(actualStack) && ArmorFeatureToggle.isEnabled(actualStack, "fire_resistance")) {
                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 2, 0, false, false, false));
            } else {
                player.removeEffect(MobEffects.FIRE_RESISTANCE);
            }
        }

        // Check if flight is enabled
        if (!ArmorFeatureToggle.isEnabled(actualStack, "flight")) {
            flyingPlayers.remove(player);
            return;
        }

        // Check energy
        int requiredEnergy = (currentMode == MODE_HOVER) ? HOVER_CONSUME_PER_TICK : JETPACK_CONSUME_PER_TICK;
        if (currentEnergy < requiredEnergy) {
            flyingPlayers.remove(player);
            return;
        }

        // Use IC2-style independent key state system
        boolean isJumping = JetpackKeyHandler.isJumpKeyDown(player);

        // Mode switch: Mode Switch key only (with cooldown)
        if (JetpackKeyHandler.isModeSwitchKeyDown(player) && toggleTimer == 0) {
            int newMode = toggleMode(actualStack);
            toggleTimer = TOGGLE_COOLDOWN;
            setToggleTimer(actualStack, toggleTimer);
            if (!level.isClientSide) {
                if (newMode == MODE_HOVER) {
                    player.setDeltaMovement(player.getDeltaMovement().x, 0.0, player.getDeltaMovement().z);
                    player.sendSystemMessage(Component.translatable("hud.mio_icif.jetpack.hover_enabled"));
                } else {
                    player.sendSystemMessage(Component.translatable("hud.mio_icif.jetpack.hover_disabled"));
                }
            }
        }

        // Decrement toggle cooldown
        if (toggleTimer > 0) {
            toggleTimer--;
            setToggleTimer(actualStack, toggleTimer);
        }

        // Execute flight logic (same as electric jetpack)
        boolean jetpackUsed = false;

        if (currentMode == MODE_JETPACK) {
            if (isJumping) {
                jetpackUsed = useJetpack(player, actualStack, (int) currentEnergy);
            }
        } else if (currentMode == MODE_HOVER) {
            if (isJumping || !player.onGround()) {
                jetpackUsed = useJetpackHover(player, actualStack, (int) currentEnergy, isJumping);
            }
        }

        // Update flight state
        if (jetpackUsed) {
            flyingPlayers.put(player, true);
            player.fallDistance = 0.0F;
        } else {
            flyingPlayers.remove(player);
        }

        // IC2 logic: disable hover mode when landing (only when transitioning from air to ground)
        if (currentMode == MODE_HOVER && player.onGround() && !level.isClientSide) {
            Boolean wasFlying = flyingPlayers.get(player);
            if (wasFlying != null && wasFlying) {
                setModeInternal(actualStack, MODE_JETPACK);
                player.sendSystemMessage(Component.translatable("hud.mio_icif.jetpack.hover_disabled"));
            }
        }
    }

    /**
     * Jetpack mode flight logic (copied from electric jetpack)
     */
    private boolean useJetpack(Player player, ItemStack stack, int currentEnergy) {
        // Calculate power (based on energy percentage)
        float power = calculatePower(currentEnergy);

        // Apply forward thrust
        applyForwardThrust(player, power, false);

        // Apply height limit
        power = applyHeightLimit(player, power);

        // Calculate vertical velocity
        double currentY = player.getDeltaMovement().y;

        // Base thrust
        double thrust = power * 0.2F;

        // Key fix: limit velocity change per tick for smoother counter-thrust
        // Max velocity change = 0.25, so from -0.8 to 0 takes at least 4 ticks
        double maxDeltaV = 0.25;
        double targetY = Math.min(currentY + thrust, MAX_ASCENT_SPEED);

        // If target velocity is much higher than current (deceleration or reversal), limit change rate
        if (targetY > currentY + maxDeltaV) {
            targetY = currentY + maxDeltaV;
        }

        double newY = targetY;

        // Apply velocity
        player.setDeltaMovement(player.getDeltaMovement().x, newY, player.getDeltaMovement().z);
        player.hasImpulse = true;

        // Consume energy (IC2 logic: only when not on ground)
        if (!player.onGround()) {
            consumeEnergy(stack, JETPACK_CONSUME_PER_TICK);
        }

        return true;
    }

    /**
     * Hover mode flight logic (copied from electric jetpack)
     */
    private boolean useJetpackHover(Player player, ItemStack stack, int currentEnergy, boolean isJumping) {
        // Check sneak key using IC2-style key state
        boolean isDescending = JetpackKeyHandler.isSneakKeyDown(player);

        // Don't activate when on ground and not jumping
        if (player.onGround() && !isJumping) {
            return false;
        }

        // Calculate power
        float power = calculatePower(currentEnergy);

        // Apply forward thrust (stronger in hover mode)
        applyForwardThrust(player, power, true);

        // Apply height limit
        power = applyHeightLimit(player, power);

        // Get current velocity (after thrust)
        double currentMotionY = player.getDeltaMovement().y;

        // Calculate new velocity
        double newMotionY = Math.min(currentMotionY + power * 0.2F, MAX_ASCENT_SPEED);

        // Hover mode velocity limit and buffer
        float maxHoverY = 0.0F;
        if (isJumping) {
            maxHoverY += HOVER_ASCENT_SPEED;
        }
        if (isDescending) {
            maxHoverY += HOVER_DESCENT_SPEED;
        }

        // IC2 key buffer logic: if new velocity exceeds limit, limit to max
        // But if previous velocity is greater than new, keep previous (prevent sudden drop)
        if (newMotionY > maxHoverY) {
            newMotionY = maxHoverY;
            if (currentMotionY > newMotionY) {
                newMotionY = currentMotionY;
            }
        }

        // Apply velocity
        player.setDeltaMovement(player.getDeltaMovement().x, newMotionY, player.getDeltaMovement().z);
        player.hasImpulse = true;

        // Consume energy (IC2 logic: only when not on ground)
        if (!player.onGround()) {
            consumeEnergy(stack, HOVER_CONSUME_PER_TICK);
        }

        return true;
    }

    /**
     * Calculate power (based on IC2 JetpackLogic.getPower)
     */
    private float calculatePower(int currentEnergy) {
        float power = JETPACK_POWER;
        float chargeLevel = (float) currentEnergy / MAX_ENERGY;

        // Power decreases proportionally when energy below 5%
        if (chargeLevel <= DROP_PERCENTAGE) {
            power = power * (chargeLevel / DROP_PERCENTAGE);
        }

        return power;
    }

    /**
     * Apply forward thrust (based on IC2 JetpackLogic.applyForwardThrust)
     * Quantum suit has much stronger thrust for creative-like flight (hover mode only)
     * 
     * 修改: 支持所有方向(WASD)的加速，而不只是前进(W)
     * 悬浮模式区分 boost 状态：不按 boost 时类似创造模式步行速度，按 boost 时高速飞行
     */
    private void applyForwardThrust(Player player, float power, boolean hoverMode) {
        // 获取玩家输入方向（基于WASD按键）
        float moveForward = player.zza; // 前进/后退输入 (-1.0 到 1.0)
        float moveStrafe = player.xxa;  // 左右输入 (-1.0 到 1.0)
        
        // 如果没有移动输入，不应用推力
        if (moveForward == 0.0F && moveStrafe == 0.0F) return;

        // 悬浮模式下检测 boost 键状态
        boolean isBoosting = hoverMode && JetpackKeyHandler.isBoostKeyDown(player);

        // Base thrust multiplier
        // 喷气模式保持原版: 0.15F
        // 悬浮模式(不按boost): 0.8F (类似创造模式步行速度，精细控制)
        // 悬浮模式(按boost): 2.0F (高速飞行，约20格/秒)
        float thruster;
        if (hoverMode) {
            thruster = isBoosting ? 2.0F : 0.8F;
        } else {
            thruster = 0.15F;
        }
        float forwardPower = power * thruster * 2.0F;

        if (forwardPower <= 0.0F) return;

        // Calculate thrust
        float thrust;
        float friction;
        if (hoverMode) {
            if (isBoosting) {
                thrust = 0.45F * forwardPower;
                friction = 0.05F;
            } else {
                thrust = 0.15F * forwardPower;
                friction = 0.12F;
            }
        } else {
            thrust = 0.4F * forwardPower;
            friction = 0.02F;
        }

        // 获取玩家朝向
        float yaw = player.getYRot();
        float rad = (float) Math.toRadians(yaw);

        double mx = player.getDeltaMovement().x;
        double mz = player.getDeltaMovement().z;

        // 计算前进方向（基于玩家朝向）
        double forwardX = -Math.sin(rad);
        double forwardZ = Math.cos(rad);
        
        // 计算右方向（垂直于前进方向）
        double rightX = Math.cos(rad);
        double rightZ = Math.sin(rad);

        // 应用推力到移动方向
        // 前进/后退方向
        mx += forwardX * moveForward * thrust * friction;
        mz += forwardZ * moveForward * thrust * friction;
        
        // 左/右方向
        mx += rightX * moveStrafe * thrust * friction;
        mz += rightZ * moveStrafe * thrust * friction;

        player.setDeltaMovement(mx, player.getDeltaMovement().y, mz);
    }

    /**
     * Apply max flight height limit (based on IC2 JetpackLogic.applyHeightLimit)
     * 量子胸甲无高度限制，直接返回原始推力
     */
    private float applyHeightLimit(Player player, float power) {
        if (!hasHeightLimit()) {
            return power;
        }
        int maxFlightHeight = (int) getMaxHeight();
        double y = player.getY();

        if (y > (maxFlightHeight - 25)) {
            if (y > maxFlightHeight) {
                y = maxFlightHeight;
            }
            power = (float) (power * (maxFlightHeight - y) / 25.0D);
        }

        return power;
    }

    /**
     * Toggle between jetpack and hover mode
     */
    private int toggleMode(ItemStack stack) {
        int currentMode = getModeInternal(stack);
        int newMode = (currentMode == MODE_JETPACK) ? MODE_HOVER : MODE_JETPACK;
        setModeInternal(stack, newMode);
        return newMode;
    }

    public long getEnergyPerDamage() {
        return ENERGY_PER_DAMAGE;
    }

    @Override
    public List<ArmorFeatureInfo> getFeatures(ItemStack stack) {
        return List.of(
            new ArmorFeatureInfo(EquipmentSlot.CHEST, "flight", "tooltip.mio_icif.armor.feature_flight"),
            new ArmorFeatureInfo(EquipmentSlot.CHEST, "jetpack_mode", "tooltip.mio_icif.armor.feature_jetpack_mode", getModeName(getModeInternal(stack))),
            new ArmorFeatureInfo(EquipmentSlot.CHEST, "fire_resistance", "tooltip.mio_icif.armor.feature_fire_resistance")
        );
    }

    // ==================== IJetpackItem API ====================

    @Override
    public float getThrust() {
        return JETPACK_POWER;
    }

    @Override
    public long getEnergyPerTickFlying() {
        return JETPACK_CONSUME_PER_TICK;
    }

    @Override
    public boolean hasHeightLimit() {
        return false;
    }

    @Override
    public JetpackMode getMode(ItemStack stack) {
        int internal = getModeInternal(stack);
        return internal == MODE_HOVER ? JetpackMode.HOVER : JetpackMode.FLIGHT;
    }

    @Override
    public void setMode(ItemStack stack, JetpackMode mode) {
        int internal = switch (mode) {
            case OFF -> MODE_JETPACK;
            case NORMAL -> MODE_JETPACK;
            case HOVER -> MODE_HOVER;
            case FLIGHT -> MODE_JETPACK;
        };
        setModeInternal(stack, internal);
    }
}