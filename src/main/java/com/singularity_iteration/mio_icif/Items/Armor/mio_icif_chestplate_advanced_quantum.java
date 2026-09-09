package com.singularity_iteration.mio_icif.Items.Armor;

import com.singularity_iteration.mio_icif.api.item.ArmorFeatureInfo;
import com.singularity_iteration.mio_icif.api.item.IJetpackItem;
import com.singularity_iteration.mio_icif.util.JetpackKeyHandler;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 进阶量子胸甲 (Advanced Quantum Chestplate)
 * 基于METS AdvancedQuantumSuit重构
 * - 最大电量: 100,000,000 EU (Tier 5)
 * - 充电速率: 8,192 EU/t
 * - 飞行(喷气/悬停模式)、生命恢复、击退抗性、防火
 */
@SuppressWarnings({"null", "deprecation"})
public class mio_icif_chestplate_advanced_quantum extends mio_icif_armor_elc implements IJetpackItem {

    public static final int MAX_ENERGY = 100000000;
    public static final int ENERGY_PER_DAMAGE = 10000;
    public static final int CHARGE_RATE = 8192;
    public static final int ARMOR_TIER = 5;

    public static final int HEAL_COST = 30000;
    public static final int JETPACK_CONSUME_PER_TICK = 8;
    public static final int HOVER_CONSUME_PER_TICK = 10;

    private static final String MODE_KEY = "JetpackMode";
    private static final String TOGGLE_TIMER_KEY = "AdvQuantumToggleTimer";
    public static final int MODE_JETPACK = 0;
    public static final int MODE_HOVER = 1;
    private static final int TOGGLE_COOLDOWN = 10;

    public static final float JETPACK_POWER = 1.0F;
    public static final float MAX_ASCENT_SPEED = 0.8F;
    public static final float HOVER_ASCENT_SPEED = 0.6F;
    public static final float HOVER_DESCENT_SPEED = -0.6F;
    public static final float WORLD_HEIGHT_DIVISOR = 1.28F;
    public static final float DROP_PERCENTAGE = 0.05F;

    private static final ResourceLocation KNOCKBACK_RESISTANCE_ID = ResourceLocation.fromNamespaceAndPath("mio_icif", "advanced_quantum_knockback_resistance");

    private static final Map<Player, Boolean> flyingPlayers = new WeakHashMap<>();

    public mio_icif_chestplate_advanced_quantum(Holder<ArmorMaterial> material, Properties properties) {
        super(material, Type.CHESTPLATE, properties, MAX_ENERGY, 0, "advanced_quantum", CHARGE_RATE, 0, ARMOR_TIER);
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

        ItemStack actualStack = player.getItemBySlot(EquipmentSlot.CHEST);
        boolean isWearing = !actualStack.isEmpty() && actualStack.getItem() == this;

        if (!isWearing) {
            flyingPlayers.remove(player);
            return;
        }

        int currentMode = getModeInternal(actualStack);
        long currentEnergy = getEnergy(actualStack);
        int toggleTimer = getToggleTimer(actualStack);

        if (level.isClientSide && player.tickCount % 20 == 0) {
            Component modeName = getModeName(currentMode);
            player.displayClientMessage(
                Component.translatable("hud.mio_icif.jetpack.display",
                    modeName, getEnergy(actualStack), MAX_ENERGY), true);
        }

        // 生命恢复
        if (!level.isClientSide && ArmorFeatureToggle.isEnabled(actualStack, "healing")
                && player.getHealth() < player.getMaxHealth()) {
            if (consumeEnergy(actualStack, HEAL_COST)) {
                player.heal(1.0F);
            }
        }

        // 防火效果
        if (!level.isClientSide) {
            if (!isEmpty(actualStack) && ArmorFeatureToggle.isEnabled(actualStack, "fire_resistance")) {
                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 2, 0, false, false, false));
            } else {
                player.removeEffect(MobEffects.FIRE_RESISTANCE);
            }
        }

        // 击退抗性
        if (!isEmpty(actualStack)) {
            AttributeInstance attr = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
            if (ArmorFeatureToggle.isEnabled(actualStack, "knockback_resistance")) {
                if (attr != null && attr.getModifier(KNOCKBACK_RESISTANCE_ID) == null) {
                    attr.addTransientModifier(new AttributeModifier(
                        KNOCKBACK_RESISTANCE_ID,
                        0.5,
                        AttributeModifier.Operation.ADD_VALUE
                    ));
                }
            } else {
                if (attr != null) {
                    attr.removeModifier(KNOCKBACK_RESISTANCE_ID);
                }
            }
        }

        if (!ArmorFeatureToggle.isEnabled(actualStack, "flight")) {
            flyingPlayers.remove(player);
            return;
        }

        int requiredEnergy = (currentMode == MODE_HOVER) ? HOVER_CONSUME_PER_TICK : JETPACK_CONSUME_PER_TICK;
        if (currentEnergy < requiredEnergy) {
            flyingPlayers.remove(player);
            return;
        }

        boolean isJumping = JetpackKeyHandler.isJumpKeyDown(player);

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

        if (toggleTimer > 0) {
            toggleTimer--;
            setToggleTimer(actualStack, toggleTimer);
        }

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

        if (jetpackUsed) {
            flyingPlayers.put(player, true);
            player.fallDistance = 0.0F;
        } else {
            flyingPlayers.remove(player);
        }

        if (currentMode == MODE_HOVER && player.onGround() && !level.isClientSide) {
            Boolean wasFlying = flyingPlayers.get(player);
            if (wasFlying != null && wasFlying) {
                setModeInternal(actualStack, MODE_JETPACK);
                player.sendSystemMessage(Component.translatable("hud.mio_icif.jetpack.hover_disabled"));
            }
        }
    }

    private boolean useJetpack(Player player, ItemStack stack, int currentEnergy) {
        if (currentEnergy < JETPACK_CONSUME_PER_TICK) {
            return false;
        }

        float power = calculatePower(currentEnergy);
        applyForwardThrust(player, power, false);
        power = applyHeightLimit(player, power);

        double currentY = player.getDeltaMovement().y;
        double thrust = power * 0.2F;
        double maxDeltaV = 0.25;
        double targetY = Math.min(currentY + thrust, MAX_ASCENT_SPEED);

        if (targetY > currentY + maxDeltaV) {
            targetY = currentY + maxDeltaV;
        }

        player.setDeltaMovement(player.getDeltaMovement().x, targetY, player.getDeltaMovement().z);
        player.hasImpulse = true;

        if (!player.onGround()) {
            consumeEnergy(stack, JETPACK_CONSUME_PER_TICK);
        }
        return true;
    }

    private boolean useJetpackHover(Player player, ItemStack stack, int currentEnergy, boolean isJumping) {
        if (currentEnergy < HOVER_CONSUME_PER_TICK) {
            return false;
        }

        boolean isDescending = JetpackKeyHandler.isSneakKeyDown(player);

        if (player.onGround() && !isJumping) {
            return false;
        }

        float power = calculatePower(currentEnergy);
        applyForwardThrust(player, power, true);
        power = applyHeightLimit(player, power);

        double currentMotionY = player.getDeltaMovement().y;
        double newMotionY = Math.min(currentMotionY + power * 0.2F, MAX_ASCENT_SPEED);

        float maxHoverY = 0.0F;
        if (isJumping) maxHoverY += HOVER_ASCENT_SPEED;
        if (isDescending) maxHoverY += HOVER_DESCENT_SPEED;

        if (newMotionY > maxHoverY) {
            newMotionY = maxHoverY;
            if (currentMotionY > newMotionY) {
                newMotionY = currentMotionY;
            }
        }

        player.setDeltaMovement(player.getDeltaMovement().x, newMotionY, player.getDeltaMovement().z);
        player.hasImpulse = true;

        if (!player.onGround()) {
            consumeEnergy(stack, HOVER_CONSUME_PER_TICK);
        }
        return true;
    }

    private float calculatePower(int currentEnergy) {
        float power = JETPACK_POWER;
        float chargeLevel = (float) currentEnergy / MAX_ENERGY;
        if (chargeLevel <= DROP_PERCENTAGE) {
            power = power * (chargeLevel / DROP_PERCENTAGE);
        }
        return power;
    }

    private void applyForwardThrust(Player player, float power, boolean hoverMode) {
        float moveForward = player.zza;
        float moveStrafe = player.xxa;
        if (moveForward == 0.0F && moveStrafe == 0.0F) return;

        // 检测boost键状态
        boolean isBoosting = JetpackKeyHandler.isBoostKeyDown(player);

        // 推力参数
        float thruster;
        float thrustMultiplier;
        float friction;
        
        if (hoverMode) {
            // 悬浮模式
            thruster = isBoosting ? 2.0F : 1.0F;
            thrustMultiplier = 0.6F;
            friction = 0.03F;
        } else {
            // 喷气模式
            thruster = 0.15F;
            thrustMultiplier = isBoosting ? 0.6F : 0.4F;
            friction = 0.02F;
        }
        
        float forwardPower = power * thruster * 2.0F;
        if (forwardPower <= 0.0F) return;

        float thrust = thrustMultiplier * forwardPower;

        float yaw = player.getYRot();
        float rad = (float) Math.toRadians(yaw);

        double mx = player.getDeltaMovement().x;
        double mz = player.getDeltaMovement().z;

        double forwardX = -Math.sin(rad);
        double forwardZ = Math.cos(rad);
        double rightX = Math.cos(rad);
        double rightZ = Math.sin(rad);

        mx += forwardX * moveForward * thrust * friction;
        mz += forwardZ * moveForward * thrust * friction;
        mx += rightX * moveStrafe * thrust * friction;
        mz += rightZ * moveStrafe * thrust * friction;

        player.setDeltaMovement(mx, player.getDeltaMovement().y, mz);
    }

    private float applyHeightLimit(Player player, float power) {
        if (!hasHeightLimit()) {
            return power;
        }
        int maxFlightHeight = (int) getMaxHeight();
        double y = player.getY();

        if (y > (maxFlightHeight - 25)) {
            if (y > maxFlightHeight) y = maxFlightHeight;
            power = (float) (power * (maxFlightHeight - y) / 25.0D);
        }
        return power;
    }

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
            new ArmorFeatureInfo(EquipmentSlot.CHEST, "fire_resistance", "tooltip.mio_icif.armor.feature_fire_resistance"),
            new ArmorFeatureInfo(EquipmentSlot.CHEST, "healing", "tooltip.mio_icif.armor.feature_healing"),
            new ArmorFeatureInfo(EquipmentSlot.CHEST, "knockback_resistance", "tooltip.mio_icif.armor.feature_knockback_resistance")
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