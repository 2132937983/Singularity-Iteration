// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.prefab.flight;

import com.miophas.singularity_iteration.core.api.item.IJetpackItem;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * <p><b>EN:</b> Jetpack flight controller (a reusable base). An item that implements
 * {@link IJetpackItem} calls {@link #tick(Player, ItemStack, IJetpackItem, long, double, double, double, boolean)}
 * in its own tick. The item then gets the shared lift, hover, forward thrust, height limit and energy use.
 * {@link JetpackKeyHandler} supplies the jump, forward, sneak and boost input (client to server).
 * Since 0.1.8.1 each thrusting tick also records a {@link Thrust} sample (tick, throttle, sound
 * profile) for the thruster sound; the client and server keep separate maps.
 * <p><b>ZH:</b> 喷气背包飞行控制器（可复用基类）。实现 {@link IJetpackItem} 的物品在自己的 tick 中调用 {@code tick(...)}，
 * 即可获得统一的升力、悬停、前向推力、高度限制与能耗。{@link JetpackKeyHandler} 提供跳跃、前进、潜行与加速输入（客户端到服务器）。
 * 自 0.1.8.1 起，每个产生推力的 tick 还记录一个 {@link Thrust} 样本（tick、油门、音效档）用于推进器音效；客户端与服务器各自维护映射。
 * <p><b>JA:</b> ジェットパック飛行制御（再利用できる基底）。{@link IJetpackItem} を実装したアイテムは自分の tick で {@code tick(...)} を呼ぶだけで、
 * 共通の揚力・ホバー・前進推力・高度制限・電力消費を得る。{@link JetpackKeyHandler} がジャンプ・前進・スニーク・ブーストの入力
 * （クライアントからサーバーへ）を提供する。0.1.8.1 以降、推力を出した各 tick で {@link Thrust} サンプル（tick、スロットル、音プロファイル）も
 * 記録し、スラスター音に使う。クライアントとサーバーは別々のマップを持つ。
 */
public final class JetpackFlightController {
    /*
     * Last game tick in which each player's jetpack produced thrust, kept per logical side.
     * In singleplayer the client and server Player share a UUID but tick on different threads
     * against different game clocks; one shared map let each side overwrite the other.
     */
    private static final Map<UUID, Thrust> SERVER_ACTIVE_TICKS = new ConcurrentHashMap<>();
    private static final Map<UUID, Thrust> CLIENT_ACTIVE_TICKS = new ConcurrentHashMap<>();

    private static Map<UUID, Thrust> activeTicks(Player player) {
        return player.level().isClientSide ? CLIENT_ACTIVE_TICKS : SERVER_ACTIVE_TICKS;
    }

    /**
     * <p><b>EN:</b> Last thrust of a player (since 0.1.8.1): game tick, throttle (0..1) and the
     * thruster sound profile ({@link IJetpackItem#getThrusterSound()}). The sound code reads it.
     * <p><b>ZH:</b> 玩家最近一次推力（0.1.8.1 起）：游戏 tick、油门（0..1）与推进器音效档。音效代码读取它。
     * <p><b>JA:</b> プレイヤーの最新の推力（0.1.8.1 以降）：ゲーム tick、スロットル（0..1）、スラスター音プロファイル。音のコードが読む。
     */
    public record Thrust(long tick, float throttle, String sound) {}

    /**
     * <p><b>EN:</b> Last thrust sample of the player on this side, or null.
     * <p><b>ZH:</b> 本端该玩家最近的推力样本，没有则为 null。
     * <p><b>JA:</b> この側でのプレイヤーの最新推力サンプル。なければ null。
     */
    public static Thrust lastThrust(Player player) {
        return player == null ? null : activeTicks(player).get(player.getUUID());
    }

    /**
     * <p><b>EN:</b> Throttle for the sound: 1.0 for full lift, 0.8 for a hover climb, 0.55 for a
     * steady hover, 0.35 for a hover descent, +0.1 with boost and forward input. A nearly empty
     * battery (5 % or less) scales the value down like the lift.
     * <p><b>ZH:</b> 用于音效的油门：全力上升 1.0，悬停爬升 0.8，稳定悬停 0.55，悬停下降 0.35；加速且前进时 +0.1。
     * 电量几乎耗尽（≤ 5%）时与升力一样按比例降低。
     * <p><b>JA:</b> 音用のスロットル：全力上昇 1.0、ホバー上昇 0.8、定常ホバー 0.55、ホバー下降 0.35、ブーストと前進入力で +0.1。
     * 電池がほぼ空（5 % 以下）なら揚力と同じく値を下げる。
     */
    static float throttle(boolean hover, boolean jumping, boolean descending, boolean boostForward, float powerRatio) {
        float t = !hover ? 1.0F : jumping ? 0.8F : descending ? 0.35F : 0.55F;
        if (boostForward) t += 0.1F;
        return Math.max(0F, Math.min(1F, t * powerRatio));
    }

    private JetpackFlightController() {}

    public static void tick(Player player, ItemStack stack, IJetpackItem jetpack,
            long hoverCost, double maxAscentSpeed, double hoverAscentSpeed,
            double hoverDescentSpeed, boolean advanced) {
        if (stack.isEmpty() || stack.getCount() != 1) return;
        // Creative-style flight from another source (flight ring, creative mode) is not jetpack
        // flight: hover mode used to treat "in the air" as hovering and drained the chestplate
        // while the ring was the one keeping the player up.
        if (player.getAbilities().flying) {
            activeTicks(player).remove(player.getUUID());
            return;
        }

        int currentMode = jetpack.getMode(stack) == IJetpackItem.JetpackMode.HOVER ? 1 : 0;
        int MODE_JETPACK = 0;
        int MODE_HOVER = 1;

        long currentEnergy = jetpack.getEnergy(stack);
        long maxEnergy = jetpack.getMaxEnergy(stack);
        float jetpackPower = jetpack.getThrust();
        long flightCost = jetpack.getEnergyPerTickFlying();

        int requiredEnergy = (currentMode == MODE_HOVER) ? (int) hoverCost : (int) flightCost;
        if (currentEnergy < requiredEnergy) {
            activeTicks(player).remove(player.getUUID());
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
            player.fallDistance = 0.0F;
            float ratio = jetpackPower <= 0F ? 0F : calculatePower((int) currentEnergy, maxEnergy, jetpackPower) / jetpackPower;
            boolean boostForward = JetpackKeyHandler.isBoostKeyDown(player) && (player.zza != 0F || JetpackKeyHandler.isForwardKeyDown(player));
            activeTicks(player).put(player.getUUID(), new Thrust(player.level().getGameTime(),
                throttle(currentMode == MODE_HOVER, isJumping, JetpackKeyHandler.isSneakKeyDown(player), boostForward, ratio),
                jetpack.getThrusterSound()));
        } else {
            activeTicks(player).remove(player.getUUID());
        }

        // Former WeakHashMap "flyingPlayers" was written and read back in this same call, so it
        // always equalled jetpackUsed; it also raced between client and server threads.
        if (currentMode == MODE_HOVER && player.onGround() && !player.level().isClientSide) {
            if (jetpackUsed) {
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
        cost = com.miophas.singularity_iteration.core.api.item.EnergySaving.apply(stack, cost);
        if (jetpack.getEnergy(stack) < cost) return false;
        long extracted = jetpack.extractEnergy(stack, cost);
        if (extracted == cost) return true;
        if (extracted > 0L) jetpack.addEnergy(stack, extracted);
        return false;
    }

    public static boolean isFlying(Player player) {
        if (player == null) return false;
        Thrust thrust = activeTicks(player).get(player.getUUID());
        return thrust != null && thrust.tick() == player.level().getGameTime();
    }

    public static void removePlayer(Player player) {
        if (player != null && !player.level().isClientSide) {
            SERVER_ACTIVE_TICKS.remove(player.getUUID());
        }
    }

    public static void clearServerStates() {
        SERVER_ACTIVE_TICKS.clear();
        CLIENT_ACTIVE_TICKS.clear();
    }
}
