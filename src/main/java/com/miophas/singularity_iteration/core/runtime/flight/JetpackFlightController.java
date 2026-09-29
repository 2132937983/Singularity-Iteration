// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.flight;

import com.miophas.singularity_iteration.core.api.item.IJetpackItem;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class JetpackFlightController {
    private static final Map<UUID, Long> ACTIVE_TICKS = new ConcurrentHashMap<>();
    private static final Map<Player, Boolean> flyingPlayers = new WeakHashMap<>();

    private JetpackFlightController() {}

    public static void tick(Player player, ItemStack stack, IJetpackItem jetpack,
            long hoverCost, double maxAscentSpeed, double hoverAscentSpeed,
            double hoverDescentSpeed, boolean advanced) {
        if (stack.isEmpty() || stack.getCount() != 1) return;

        int currentMode = jetpack.getMode(stack) == IJetpackItem.JetpackMode.HOVER ? 1 : 0;
        int MODE_JETPACK = 0;
        int MODE_HOVER = 1;

        long currentEnergy = jetpack.getEnergy(stack);
        long maxEnergy = jetpack.getMaxEnergy(stack);
        float jetpackPower = jetpack.getThrust();
        long flightCost = jetpack.getEnergyPerTickFlying();

        int requiredEnergy = (currentMode == MODE_HOVER) ? (int) hoverCost : (int) flightCost;
        if (currentEnergy < requiredEnergy) {
            flyingPlayers.remove(player);
            ACTIVE_TICKS.remove(player.getUUID());
            return;
        }

        boolean isJumping = JetpackKeyHandler.isJumpKeyDown(player);

        boolean jetpackUsed = false;

        if (currentMode == MODE_JETPACK) {
            if (isJumping) {
                jetpackUsed = useJetpack(player, stack, jetpack, (int) currentEnergy,
                    maxEnergy, jetpackPower, maxAscentSpeed, flightCost, advanced);
            }
        } else if (currentMode == MODE_HOVER) {
            if (isJumping || !player.onGround()) {
                jetpackUsed = useJetpackHover(player, stack, jetpack, (int) currentEnergy,
                    maxEnergy, jetpackPower, maxAscentSpeed, hoverAscentSpeed,
                    hoverDescentSpeed, hoverCost, isJumping, advanced);
            }
        }

        if (jetpackUsed) {
            flyingPlayers.put(player, true);
            player.fallDistance = 0.0F;
            ACTIVE_TICKS.put(player.getUUID(), player.level().getGameTime());
        } else {
            flyingPlayers.remove(player);
            ACTIVE_TICKS.remove(player.getUUID());
        }

        if (currentMode == MODE_HOVER && player.onGround() && !player.level().isClientSide) {
            Boolean wasFlying = flyingPlayers.get(player);
            if (wasFlying != null && wasFlying) {
                jetpack.setMode(stack, IJetpackItem.JetpackMode.FLIGHT);
                player.sendSystemMessage(Component.translatable("hud.mio_icif.jetpack.hover_disabled"));
            }
        }
    }

    private static boolean useJetpack(Player player, ItemStack stack, IJetpackItem jetpack,
            int currentEnergy, long maxEnergy, float jetpackPower,
            double maxAscentSpeed, long flightCost, boolean advanced) {
        float power = calculatePower(currentEnergy, maxEnergy, jetpackPower);

        applyForwardThrust(player, power, false, advanced);

        power = applyHeightLimit(player, jetpack, power);

        double currentY = player.getDeltaMovement().y;

        double thrust = power * 0.2F;

        double maxDeltaV = 0.25;
        double targetY = Math.min(currentY + thrust, maxAscentSpeed);

        if (targetY > currentY + maxDeltaV) {
            targetY = currentY + maxDeltaV;
        }

        double newY = targetY;

        player.setDeltaMovement(player.getDeltaMovement().x, newY, player.getDeltaMovement().z);
        player.hasImpulse = true;

        if (!player.onGround()) {
            consumeExact(stack, jetpack, flightCost);
        }

        return true;
    }

    private static boolean useJetpackHover(Player player, ItemStack stack, IJetpackItem jetpack,
            int currentEnergy, long maxEnergy, float jetpackPower,
            double maxAscentSpeed, double hoverAscentSpeed,
            double hoverDescentSpeed, long hoverCost,
            boolean isJumping, boolean advanced) {
        boolean isDescending = JetpackKeyHandler.isSneakKeyDown(player);

        if (player.onGround() && !isJumping) {
            return false;
        }

        float power = calculatePower(currentEnergy, maxEnergy, jetpackPower);

        applyForwardThrust(player, power, true, advanced);

        power = applyHeightLimit(player, jetpack, power);

        double currentMotionY = player.getDeltaMovement().y;

        double newMotionY = Math.min(currentMotionY + power * 0.2F, maxAscentSpeed);

        double maxHoverY = 0.0D;
        if (isJumping) {
            maxHoverY += hoverAscentSpeed;
        }
        if (isDescending) {
            maxHoverY += hoverDescentSpeed;
        }

        if (newMotionY > maxHoverY) {
            newMotionY = maxHoverY;
            if (currentMotionY > newMotionY) {
                newMotionY = currentMotionY;
            }
        }

        player.setDeltaMovement(player.getDeltaMovement().x, newMotionY, player.getDeltaMovement().z);
        player.hasImpulse = true;

        if (!player.onGround()) {
            consumeExact(stack, jetpack, hoverCost);
        }

        return true;
    }

    private static float calculatePower(int currentEnergy, long maxEnergy, float jetpackPower) {
        float power = jetpackPower;
        float chargeLevel = (float) currentEnergy / maxEnergy;

        if (chargeLevel <= 0.05F) {
            power = power * (chargeLevel / 0.05F);
        }

        return power;
    }

    private static void applyForwardThrust(Player player, float power, boolean hoverMode, boolean advanced) {
        if (advanced) {
            float moveForward = player.zza;
            float moveStrafe = player.xxa;
            if (moveForward == 0.0F && moveStrafe == 0.0F) return;

            boolean isBoosting = JetpackKeyHandler.isBoostKeyDown(player);

            float thruster;
            float thrustMultiplier;
            float friction;

            if (hoverMode) {
                thruster = isBoosting ? 2.0F : 1.0F;
                thrustMultiplier = 0.6F;
                friction = 0.03F;
            } else {
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
        } else {
            if (!JetpackKeyHandler.isForwardKeyDown(player)) return;

            float thruster = hoverMode ? 1.0F : 0.15F;
            float forwardPower = power * thruster * 2.0F;
            if (forwardPower <= 0.0F) return;

            float thrust = 0.4F * forwardPower;
            float friction = 0.02F;

            float yaw = player.getYRot();
            float rad = (float) Math.toRadians(yaw);

            double mx = player.getDeltaMovement().x;
            double mz = player.getDeltaMovement().z;

            double forwardX = -Math.sin(rad);
            double forwardZ = Math.cos(rad);

            mx += forwardX * thrust * friction;
            mz += forwardZ * thrust * friction;

            player.setDeltaMovement(mx, player.getDeltaMovement().y, mz);
        }
    }

    private static float applyHeightLimit(Player player, IJetpackItem jetpack, float power) {
        if (!jetpack.hasHeightLimit()) {
            return power;
        }
        int maxFlightHeight = (int) jetpack.getMaxHeight();
        double y = player.getY();

        if (y > (maxFlightHeight - 25)) {
            if (y > maxFlightHeight) {
                y = maxFlightHeight;
            }
            power = (float) (power * (maxFlightHeight - y) / 25.0D);
        }

        return power;
    }

    private static boolean consumeExact(ItemStack stack, IJetpackItem jetpack, long cost) {
        if (cost == 0L) return true;
        if (jetpack.getEnergy(stack) < cost) return false;
        long extracted = jetpack.extractEnergy(stack, cost);
        if (extracted == cost) return true;
        if (extracted > 0L) jetpack.addEnergy(stack, extracted);
        return false;
    }

    public static boolean isFlying(Player player) {
        if (player == null) return false;
        Long tick = ACTIVE_TICKS.get(player.getUUID());
        return tick != null && tick == player.level().getGameTime();
    }

    public static void removePlayer(Player player) {
        if (player != null && !player.level().isClientSide) {
            ACTIVE_TICKS.remove(player.getUUID());
            flyingPlayers.remove(player);
        }
    }

    public static void clearServerStates() {
        ACTIVE_TICKS.clear();
        flyingPlayers.clear();
    }
}