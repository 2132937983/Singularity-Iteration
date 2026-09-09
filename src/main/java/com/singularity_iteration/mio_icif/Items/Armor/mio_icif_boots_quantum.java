package com.singularity_iteration.mio_icif.Items.Armor;

import com.singularity_iteration.mio_icif.api.item.ArmorFeatureInfo;
import com.singularity_iteration.mio_icif.util.JetpackKeyHandler;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Quantum Boots (QuantumSuit Boots)
 * Based on IC2 ItemArmorQuantumSuit.class (armorType = FEET)
 *
 * Features:
 * - High jump (hold Jump + Boost while in air, consumes energy)
 * - Fire resistance (toggleable)
 * - Fall damage absorption (consumes energy)
 */
@SuppressWarnings({"null", "deprecation"})
public class mio_icif_boots_quantum extends mio_icif_armor_elc {

    public static final int MAX_ENERGY = 10000000;
    public static final int EFFECT_DURATION = 100;
    public static final int GROUND_JUMP_ENERGY_COST = 4000;

    // NBT keys
    private static final String TOGGLE_TIMER_KEY = "toggleTimer";
    private static final String WAS_ON_GROUND_KEY = "wasOnGround";

    // Jump charge tracking - per instance field like IC2 original
    private float jumpCharge = 0.0F;

    public mio_icif_boots_quantum(Holder<ArmorMaterial> material, Properties properties) {
        super(material, Type.BOOTS, properties, MAX_ENERGY, 0, "quantum", 12000, 0, 6);
    }

    @Override
    public float getDamageAbsorptionRatio(EquipmentSlot slot) {
        return slot == EquipmentSlot.FEET ? 0.15F : 0.0F;
    }

    /**
     * Get toggle timer from NBT
     */
    public int getToggleTimer(ItemStack stack) {
        CompoundTag nbtData = getNbtData(stack);
        if (nbtData != null && nbtData.contains(TOGGLE_TIMER_KEY)) {
            return nbtData.getByte(TOGGLE_TIMER_KEY);
        }
        return 0;
    }

    /**
     * Set toggle timer to NBT
     */
    public void setToggleTimer(ItemStack stack, int timer) {
        CompoundTag nbtData = getOrCreateNbtData(stack);
        nbtData.putByte(TOGGLE_TIMER_KEY, (byte) timer);
        saveNbtData(stack, nbtData);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        if (!(entity instanceof Player player)) return;

        // Check if wearing this boots
        if (player.getItemBySlot(EquipmentSlot.FEET) != stack) {
            return;
        }

        CompoundTag nbtData = getOrCreateNbtData(stack);
        int toggleTimer = getToggleTimer(stack);

        // Fire resistance effect
        if (!level.isClientSide) {
            if (isEmpty(stack)) {
                player.removeEffect(MobEffects.FIRE_RESISTANCE);
            } else if (ArmorFeatureToggle.isEnabled(stack, "fire_resistance")) {
                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, EFFECT_DURATION, 0, false, false, false));
            } else {
                player.removeEffect(MobEffects.FIRE_RESISTANCE);
            }
        }

        // Underwater acceleration (Deep Sea Explorer trait)
        if (player.isInWater() && ArmorFeatureToggle.isEnabled(stack, "underwater_acceleration")) {
            applyUnderwaterAcceleration(player);
        }

        // Handle quantum jump logic - exactly like IC2 1.12.2 original
        handleQuantumJump(player, stack, level, nbtData);

        // Decrement toggle timer
        if (toggleTimer > 0) {
            toggleTimer--;
            setToggleTimer(stack, toggleTimer);
        }
    }

    /**
     * Handle quantum jump logic - ported exactly from IC2 1.12.2 ItemArmorQuantumSuit
     *
     * Original IC2 logic:
     * 1. Server-side: When player jumps from ground with Jump+Boost keys, consume 4000 EU
     * 2. Client-side: When on ground with enough energy, set jumpCharge = 1.0F
     * 3. Client-side: When in air with Jump+Boost keys, apply boost based on jumpCharge
     *    - First tick (jumpCharge == 1.0F): multiply horizontal speed by 3.5
     *    - Every tick: add jumpCharge * 0.3F to vertical speed
     *    - Every tick: jumpCharge *= 0.75 (decay)
     * 4. If keys released and charge partially used, reset to 0
     */
    private void handleQuantumJump(Player player, ItemStack stack, Level level, CompoundTag nbtData) {
        // 检查大跳功能是否启用
        if (!ArmorFeatureToggle.isEnabled(stack, "quantum_jump")) {
            return;
        }

        boolean isOnGround = player.onGround();
        boolean wasOnGround = nbtData.contains(WAS_ON_GROUND_KEY) ? nbtData.getBoolean(WAS_ON_GROUND_KEY) : true;

        boolean isJumpKeyDown = JetpackKeyHandler.isJumpKeyDown(player);
        boolean isBoostKeyDown = JetpackKeyHandler.isBoostKeyDown(player);

        // Server-side: handle energy consumption when jumping from ground
        if (!level.isClientSide) {
            if (wasOnGround && !isOnGround && isJumpKeyDown && isBoostKeyDown) {
                if (hasEnoughEnergy(stack, GROUND_JUMP_ENERGY_COST)) {
                    consumeEnergy(stack, GROUND_JUMP_ENERGY_COST);
                }
            }

            // Update wasOnGround state
            if (isOnGround != wasOnGround) {
                nbtData.putBoolean(WAS_ON_GROUND_KEY, isOnGround);
                saveNbtData(stack, nbtData);
            }
        } else {
            // Client-side: handle jump charge and motion boost
            // IC2 original: if (ElectricItem.manager.canUse(stack, 4000.0D) && player.onGround) this.jumpCharge = 1.0F;
            if (hasEnoughEnergy(stack, GROUND_JUMP_ENERGY_COST) && isOnGround) {
                this.jumpCharge = 1.0F;
            }

            // IC2 original: if (player.motionY >= 0.0D && this.jumpCharge > 0.0F && !player.inWater)
            if (player.getDeltaMovement().y >= 0.0D && this.jumpCharge > 0.0F && !player.isInWater()) {
                if (isJumpKeyDown && isBoostKeyDown) {
                    // IC2 original: if (this.jumpCharge == 1.0F) { player.motionX *= 3.5D; player.motionZ *= 3.5D; }
                    if (this.jumpCharge == 1.0F) {
                        player.setDeltaMovement(
                            player.getDeltaMovement().x * 3.5D,
                            player.getDeltaMovement().y,
                            player.getDeltaMovement().z * 3.5D
                        );
                    }

                    // IC2 original: player.motionY += (this.jumpCharge * 0.3F);
                    player.setDeltaMovement(
                        player.getDeltaMovement().x,
                        player.getDeltaMovement().y + (this.jumpCharge * 0.3F),
                        player.getDeltaMovement().z
                    );
                    player.hasImpulse = true;

                    // IC2 original: this.jumpCharge = (float)(this.jumpCharge * 0.75D);
                    this.jumpCharge = (float) (this.jumpCharge * 0.75D);
                } else if (this.jumpCharge < 1.0F) {
                    // IC2 original: this.jumpCharge = 0.0F;
                    this.jumpCharge = 0.0F;
                }
            }
        }
    }

    /**
     * Get NBT data from stack
     */
    private CompoundTag getNbtData(ItemStack stack) {
        net.minecraft.world.item.component.CustomData customData = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (customData != null && !customData.isEmpty()) {
            return customData.copyTag();
        }
        return null;
    }

    /**
     * Get or create NBT data
     */
    private CompoundTag getOrCreateNbtData(ItemStack stack) {
        CompoundTag tag = getNbtData(stack);
        if (tag == null) {
            tag = new CompoundTag();
        }
        return tag;
    }

    /**
     * Save NBT data to stack
     */
    private void saveNbtData(ItemStack stack, CompoundTag tag) {
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
    }

    /**
     * Apply underwater acceleration - directly modifies player movement in water
     * This is a trait (not an enchantment), toggleable via feature management
     */
    private void applyUnderwaterAcceleration(Player player) {
        // Get player input
        float moveForward = player.zza;
        float moveStrafe = player.xxa;
        
        // If no movement input, don't apply acceleration
        if (moveForward == 0.0F && moveStrafe == 0.0F) return;
        
        // Calculate acceleration multiplier (similar to sprint speed in water)
        float acceleration = 0.08F;
        
        // Get player facing direction
        float yaw = player.getYRot();
        float rad = (float) Math.toRadians(yaw);
        
        double mx = player.getDeltaMovement().x;
        double mz = player.getDeltaMovement().z;
        
        // Forward direction
        double forwardX = -Math.sin(rad);
        double forwardZ = Math.cos(rad);
        
        // Right direction (perpendicular to forward)
        double rightX = Math.cos(rad);
        double rightZ = Math.sin(rad);
        
        // Apply acceleration to movement
        mx += forwardX * moveForward * acceleration;
        mz += forwardZ * moveForward * acceleration;
        mx += rightX * moveStrafe * acceleration;
        mz += rightZ * moveStrafe * acceleration;
        
        player.setDeltaMovement(mx, player.getDeltaMovement().y, mz);
    }

    /**
     * Absorb fall damage (IC2 logic)
     */
    public boolean absorbFall(ItemStack stack, float distance) {
        int fallDamage = Math.max((int) distance - 10, 0);
        long energyCost = getEnergyPerDamage() * fallDamage;
        if (energyCost > getEnergy(stack)) {
            return false;
        }
        consumeEnergy(stack, energyCost);
        return true;
    }

    @Override
    public long getEnergyPerDamage() {
        return 20000;
    }

    @Override
    public List<ArmorFeatureInfo> getFeatures(ItemStack stack) {
        return List.of(
            new ArmorFeatureInfo(EquipmentSlot.FEET, "fire_resistance", "tooltip.mio_icif.armor.feature_fire_resistance"),
            new ArmorFeatureInfo(EquipmentSlot.FEET, "quantum_jump", "tooltip.mio_icif.armor.feature_high_jump"),
            new ArmorFeatureInfo(EquipmentSlot.FEET, "underwater_acceleration", "tooltip.mio_icif.armor.feature_deep_sea_explorer")
        );
    }
}