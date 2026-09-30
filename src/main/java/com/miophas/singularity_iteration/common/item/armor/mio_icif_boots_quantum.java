// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.item.armor;

import com.miophas.singularity_iteration.core.api.item.ArmorFeatureInfo;
import com.miophas.singularity_iteration.core.prefab.flight.JetpackKeyHandler;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

@SuppressWarnings("deprecation")
public class mio_icif_boots_quantum extends mio_icif_armor_elc {
    public static final int MAX_ENERGY = 10_000_000;
    public static final int EFFECT_DURATION = 100;
    public static final int GROUND_JUMP_ENERGY_COST = 4000;

    private static final String WAS_ON_GROUND_KEY = "wasOnGround";

    private float jumpCharge = 0.0F;

    public mio_icif_boots_quantum(Holder<ArmorMaterial> material, Properties properties) {
        super(material, Type.BOOTS, properties, MAX_ENERGY, 0, "quantum", 12_000, 0, 6);
    }

    @Override public long getEnergyPerDamage() { return 20_000; }

    @Override public float getDamageAbsorptionRatio(EquipmentSlot slot) {
        return slot == EquipmentSlot.FEET ? 0.15F : 0.0F;
    }

    @Override public List<ArmorFeatureInfo> getFeatures(ItemStack stack) {
        return List.of(
            new ArmorFeatureInfo(EquipmentSlot.FEET, "fire_resistance", "tooltip.mio_icif.armor.feature_fire_resistance"),
            new ArmorFeatureInfo(EquipmentSlot.FEET, "quantum_jump", "tooltip.mio_icif.armor.feature_high_jump"),
            new ArmorFeatureInfo(EquipmentSlot.FEET, "underwater_acceleration", "tooltip.mio_icif.armor.feature_deep_sea_explorer")
        );
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        if (!(entity instanceof Player player)) return;
        if (player.getItemBySlot(EquipmentSlot.FEET) != stack) return;

        if (!level.isClientSide) {
            if (isEmpty(stack)) {
                player.removeEffect(MobEffects.FIRE_RESISTANCE);
            } else if (ArmorFeatureToggle.isEnabled(stack, "fire_resistance")) {
                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, EFFECT_DURATION, 0, false, false, false));
            } else {
                player.removeEffect(MobEffects.FIRE_RESISTANCE);
            }
        }

        if (player.isInWater() && !isEmpty(stack) && ArmorFeatureToggle.isEnabled(stack, "underwater_acceleration")) {
            applyUnderwaterAcceleration(player);
        }

        if (!isEmpty(stack)) {
            handleQuantumJump(player, stack, level);
        }
    }

    private void handleQuantumJump(Player player, ItemStack stack, Level level) {
        if (!ArmorFeatureToggle.isEnabled(stack, "quantum_jump")) {
            return;
        }

        boolean isOnGround = player.onGround();
        CompoundTag nbtData = getOrCreateNbtData(stack);
        boolean wasOnGround = nbtData.contains(WAS_ON_GROUND_KEY) ? nbtData.getBoolean(WAS_ON_GROUND_KEY) : true;

        boolean isJumpKeyDown = JetpackKeyHandler.isJumpKeyDown(player);
        boolean isBoostKeyDown = JetpackKeyHandler.isBoostKeyDown(player);

        if (!level.isClientSide) {
            if (wasOnGround && !isOnGround && isJumpKeyDown && isBoostKeyDown) {
                if (hasEnoughEnergy(stack, GROUND_JUMP_ENERGY_COST)) {
                    consumeEnergy(stack, GROUND_JUMP_ENERGY_COST);
                }
            }

            if (isOnGround != wasOnGround) {
                nbtData.putBoolean(WAS_ON_GROUND_KEY, isOnGround);
                saveNbtData(stack, nbtData);
            }
        } else {
            if (hasEnoughEnergy(stack, GROUND_JUMP_ENERGY_COST) && isOnGround) {
                this.jumpCharge = 1.0F;
            }

            if (player.getDeltaMovement().y >= 0.0D && this.jumpCharge > 0.0F && !player.isInWater()) {
                if (isJumpKeyDown && isBoostKeyDown) {
                    if (this.jumpCharge == 1.0F) {
                        player.setDeltaMovement(
                            player.getDeltaMovement().x * 3.5D,
                            player.getDeltaMovement().y,
                            player.getDeltaMovement().z * 3.5D
                        );
                    }

                    player.setDeltaMovement(
                        player.getDeltaMovement().x,
                        player.getDeltaMovement().y + (this.jumpCharge * 0.3F),
                        player.getDeltaMovement().z
                    );
                    player.hasImpulse = true;

                    this.jumpCharge = (float) (this.jumpCharge * 0.75D);
                } else if (this.jumpCharge < 1.0F) {
                    this.jumpCharge = 0.0F;
                }
            }
        }
    }

    private void applyUnderwaterAcceleration(Player player) {
        float moveForward = player.zza;
        float moveStrafe = player.xxa;

        if (moveForward == 0.0F && moveStrafe == 0.0F) return;

        float acceleration = 0.08F;
        float yaw = player.getYRot();
        float rad = (float) Math.toRadians(yaw);

        double mx = player.getDeltaMovement().x;
        double mz = player.getDeltaMovement().z;

        double forwardX = -Math.sin(rad);
        double forwardZ = Math.cos(rad);
        double rightX = Math.cos(rad);
        double rightZ = Math.sin(rad);

        mx += forwardX * moveForward * acceleration;
        mz += forwardZ * moveForward * acceleration;
        mx += rightX * moveStrafe * acceleration;
        mz += rightZ * moveStrafe * acceleration;

        player.setDeltaMovement(mx, player.getDeltaMovement().y, mz);
    }

    private CompoundTag getNbtData(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null && !customData.isEmpty()) {
            return customData.copyTag();
        }
        return null;
    }

    private CompoundTag getOrCreateNbtData(ItemStack stack) {
        CompoundTag tag = getNbtData(stack);
        if (tag == null) {
            tag = new CompoundTag();
        }
        return tag;
    }

    private void saveNbtData(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
}