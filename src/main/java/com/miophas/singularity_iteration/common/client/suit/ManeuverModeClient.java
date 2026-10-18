// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.client.mio_icif_ClientEvents;
import com.miophas.singularity_iteration.common.client.render.SiRenderTypes;
import com.miophas.singularity_iteration.common.registry.mio_icif_sounds;
import com.miophas.singularity_iteration.common.suit.ManeuverMode;
import com.miophas.singularity_iteration.common.suit.SuitModules;
import com.miophas.singularity_iteration.common.suit.SuitPackets;
import com.miophas.singularity_iteration.common.suit.SuitSensorData;
import com.miophas.singularity_iteration.core.prefab.item.AbstractElectricArmor;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * Special Maneuver Mode, client half (0.1.7.34). Original SI implementation. The one console
 * switch arms all parts of this class. When the switch is on and the player flies, all parts apply.
 *
 * <ul>
 *   <li><b>Throttle flight model.</b> The sprint or boost key opens a throttle (0..1). When the key
 *       is released, the throttle closes two times as fast. A tap on sneak locks the throttle. The
 *       target speed is throttle x {@link ManeuverMode#MAX_SPEED}. Each tick the mode moves the
 *       velocity of the player towards the view axis by the grip fraction (see
 *       {@link ManeuverMode#grip}). Thus the momentum continues through turns, and the flyer drifts
 *       wide at high speed. At throttle 0 the mode also controls the hover movement. The hover
 *       starts and stops smoothly. Vanilla creative flight stops at once.</li>
 *   <li><b>Take-off.</b> When flight starts within 4 ticks after the player was on the ground (a
 *       double jump), the mode launches the player upwards with a ground shock (dust ring and
 *       sound).</li>
 *   <li><b>Pose.</b> Hover: the body leans a little forward and moves up and down slowly (heave).
 *       The arms are low and back, the fists out. The mixin {@code PlayerManeuverPoseMixin} sets the
 *       limbs. Cruise: the body lines up with the flight path and banks into turns. At full throttle
 *       one arm points forward.</li>
 *   <li><b>Effects.</b> Speed FOV, camera bank, wake trails from hands and feet, a vapour cone near
 *       the shock barrier, a shock ring and boom when the throttle crosses
 *       {@link ManeuverMode#SHOCK_THROTTLE}, a wind sound loop, and the aircraft-style flight HUD.</li>
 * </ul>
 *
 * <p><b>Ownership.</b> The local client owns the motion of the local player. It sends the throttle
 * and stick to the server ({@link #sendState}). The server relays them to other clients. This class
 * uses the relayed state of remote players only for the pose and the effects.
 *
 * <p><b>Timing.</b> Movement and trails run per client tick (20 Hz). The pose, the cone, the
 * rings and the HUD run per frame. The pose uses wall-clock time steps, so it is the same at all
 * frame rates.
 *
 * <p><b>Thread.</b> All methods run on the client thread. The level and the local player must not
 * be null, unless a method tests for null itself.
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
@SuppressWarnings("null")
public final class ManeuverModeClient {
    private ManeuverModeClient() {}

    /**
     * Hover model, per tick. HOVER_ACCEL: added velocity in blocks per tick along the stick.
     * HOVER_DRAG: horizontal velocity multiplier. HOVER_MAX: horizontal speed limit in blocks per
     * tick. With full stick, the speed settles at {@code HOVER_ACCEL / (1 - HOVER_DRAG)} = 0.54
     * blocks per tick. Thus HOVER_MAX applies only when the flyer comes into hover faster than that.
     */
    private static final double HOVER_ACCEL = 0.075, HOVER_DRAG = 0.86, HOVER_MAX = 0.62;
    /**
     * Sideways velocity in blocks per tick that the strafe keys add in cruise. The grip pulls it
     * back. Thus the steady sideways speed is about LATERAL / grip.
     */
    private static final double LATERAL = 0.035;

    // ---- local flyer
    /** True while the local player is in maneuver flight. */
    private static boolean flying;
    /**
     * Velocity of the local flyer in blocks per tick. The mode keeps its own copy, because vanilla
     * travel applies flight friction to the delta movement after each move. This copy does not get
     * that friction.
     */
    private static Vec3 velocity = Vec3.ZERO;
    /** Throttle of this tick and of the previous tick, 0..1. The HUD and the pose interpolate them. */
    private static float throttle, prevThrottle;
    private static boolean locked, sneakWasDown;
    /** Stick input of this tick: the sign of the forward and strafe impulses, -1, 0 or 1. */
    private static float stickForward, stickSideways;
    /** takeoffTicks: launch ticks left. groundTicks: ticks left in which a flight start counts as take-off. */
    private static int takeoffTicks, groundTicks;
    /** Measured speed (distance moved in the tick) of this tick and of the previous tick, in blocks per tick. */
    private static double speed, prevSpeed;
    /** Camera roll in degrees, smoothed. */
    private static float roll;
    /** View yaw of the last camera frame, in degrees. */
    private static float lastYaw;
    /** FOV increase as a fraction, smoothed. 0..0.30. */
    private static float fovBlend;
    /** Ticks since the last state packet. */
    private static int sendTimer;
    private static SuitPackets.ManeuverInput lastSent;
    private static WindSound wind;

    /**
     * One shock ring in the world.
     *
     * @param pos   the ring start position, at the body centre of the flyer.
     * @param axis  the unit flight direction at the time of the boom.
     * @param start the wall-clock start time in milliseconds.
     * @param own   true for the boom of the local player (no flash at the camera in first person).
     */
    private record Boom(Vec3 pos, Vec3 axis, long start, boolean own) { }
    /** Live shock rings. At most 8. A ring lives 900 ms. */
    private static final List<Boom> BOOMS = new ArrayList<>();

    /**
     * Visual state of one flyer (local or remote player).
     *
     * <p>pitch, roll: body rotation in degrees, smoothed. turnSpeed: yaw rate in degrees per
     * second, smoothed. lastYaw: render yaw of the last frame in degrees. heave: time in seconds for
     * the heave and breath waves. transition: blend from 0 (normal pose) to 1 (maneuver pose).
     * lean: hover lean in degrees, smoothed. wasShock: throttle was at or above the shock barrier in
     * the last tick. trail: wake emitter points, oldest first, one entry (4 points) per tick, at
     * most 28. lastTick: wall-clock time of the last pose frame in milliseconds, 0 before the first
     * frame.
     */
    static final class Visual {
        float pitch, roll, turnSpeed, lastYaw, heave, transition, lean;
        boolean wasShock;
        /** 0.1.10.4: wall-clock time of the last barrier crossing (start of the vapour-cone burst), 0 none. */
        long coneAt;
        /** 0.1.10.2: in maneuver flight in the last tick (for the landing ring); the last fall speed. */
        boolean wasFlying;
        double lastVy;
        final ArrayDeque<Vec3[]> trail = new ArrayDeque<>();
        long lastTick;
    }
    /**
     * Visual state per entity id. {@link #pruneVisuals} removes the entries of players that are
     * no longer in the client level (0.1.7.36; before, the map only grew).
     */
    static final Int2ObjectOpenHashMap<Visual> VISUALS = new Int2ObjectOpenHashMap<>();

    /**
     * Removes the visual state of entity ids that are not a player in {@code level}. With a
     * null level (no world) it removes all entries. Called by {@code ClientSessionCleanup}.
     */
    public static void pruneVisuals(@org.jetbrains.annotations.Nullable net.minecraft.client.multiplayer.ClientLevel level) {
        if (level == null) { VISUALS.clear(); return; }
        if (VISUALS.isEmpty()) return;
        VISUALS.keySet().removeIf((int id) -> !(level.getEntity(id) instanceof Player));
    }

    /** True while the local player is in maneuver flight. */
    public static boolean isFlying() { return flying; }
    /** Measured speed of the local player in the last tick, in blocks per tick. */
    public static double speed() { return speed; }
    /** Throttle of the local player, 0..1. */
    public static float throttle() { return throttle; }
    /** True when the throttle of the local player is locked. */
    public static boolean locked() { return locked; }
    /** Commanded velocity of the local player, in blocks per tick. */
    public static Vec3 velocity() { return velocity; }

    static boolean armedChest(Player player) {
        return ManeuverMode.isArmed(player);
    }

    /**
     * True when the local player can use the mode now: vanilla flight is on, the player is not a
     * spectator and not a passenger, the chestplate is armed, and the chestplate has at least
     * {@link ManeuverMode#MIN_ENERGY} (not tested in creative mode).
     */
    private static boolean active(LocalPlayer player) {
        return player.getAbilities().flying && !player.isSpectator() && !player.isPassenger() && armedChest(player)
            && (player.isCreative() || SuitModules.energy(player, player.getItemBySlot(EquipmentSlot.CHEST)) >= ManeuverMode.MIN_ENERGY);
    }

    /**
     * Throttle of a player, 0..1. For the local player: the interpolated local value. For other
     * players: the last relayed value, with no interpolation. It is 0 when the player does not fly
     * or when the relayed state is older than 3 seconds.
     */
    public static float throttleOf(Player p, float partial) {
        if (p == Minecraft.getInstance().player) return flying ? Mth.lerp(partial, prevThrottle, throttle) : 0;
        SuitPackets.ManeuverState s = SuitSensorData.maneuver(p.getId());
        return s == null ? 0 : s.throttle();
    }

    /**
     * True when a player (local or remote) is in maneuver flight now. For a remote player, the
     * client cannot see the flight state. Thus it uses this test: the chestplate is armed, the
     * player is not on the ground, and a relayed state is less than 3 seconds old.
     */
    public static boolean inManeuver(Player p) {
        if (p == Minecraft.getInstance().player) return flying;
        return armedChest(p) && !p.onGround() && !p.isPassenger() && SuitSensorData.maneuver(p.getId()) != null;
    }

    /** Stick of a player as {forward, sideways}, each -1..1. {0, 0} when no state is known. */
    static float[] stickOf(Player p) {
        if (p == Minecraft.getInstance().player) return new float[]{stickForward, stickSideways};
        SuitPackets.ManeuverState s = SuitSensorData.maneuver(p.getId());
        return s == null ? new float[]{0, 0} : new float[]{s.forward(), s.sideways()};
    }

    // ------------------------------------------------------------------ movement

    /**
     * Velocity rewrite of the local flyer, one time per client tick.
     *
     * <p>Timing: vanilla fires this event in {@code LocalPlayer.aiStep}, after it reads the keys and
     * before it calls {@code travel}. Thus the velocity that this method sets is the velocity of the
     * move in this tick. The LOW priority lets other handlers change the input first.
     *
     * <p>Order of the steps:
     * <ol>
     *   <li>Start and stop: at flight start, the method copies the vanilla velocity and resets the
     *       throttle. It launches when the player was on the ground in the last 4 ticks. At flight
     *       end, it resets the throttle and sends the state at once.</li>
     *   <li>Throttle: a sneak tap at a throttle above 0.2 toggles the lock. A horizontal collision
     *       closes the throttle at once and unlocks it. Otherwise, when the throttle is not locked,
     *       the boost key opens it by {@link ManeuverMode#THROTTLE_UP} and its release closes it by
     *       {@link ManeuverMode#THROTTLE_DOWN}.</li>
     *   <li>Velocity, one of three modes:
     *     <ul>
     *       <li>Take-off (4 ticks): vertical speed 1.25 blocks per tick. The horizontal speed falls
     *           by 40 % per tick.</li>
     *       <li>Cruise (throttle above 0): {@code v = v + (target - v) * grip}, with
     *           {@code target = look * throttle * MAX_SPEED}. The strafe keys add
     *           {@link #LATERAL} sideways. Jump and sneak do not change the height in cruise.</li>
     *       <li>Hover (throttle 0): drag, then stick acceleration, then the speed limits. Vertical:
     *           drag 0.8, jump or sneak adds 0.09 per tick, limit 0.55 blocks per tick. When there is
     *           no input and the speed is below 0.01 blocks per tick, the velocity becomes zero.</li>
     *     </ul>
     *   </li>
     *   <li>Hand-off to vanilla: the method clears the horizontal input. Thus vanilla adds no
     *       walk acceleration. Vanilla flight adds {@code 3 x flyingSpeed} to the vertical speed for
     *       jump and removes it for sneak, later in aiStep. The method removes that step here in
     *       advance. Then it sets the delta movement and sends the state.</li>
     * </ol>
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onInput(MovementInputUpdateEvent event) {
        if (!(event.getEntity() instanceof LocalPlayer player)) return;
        boolean now = active(player);
        if (now && !flying) {
            velocity = player.getDeltaMovement();
            throttle = prevThrottle = 0;
            locked = false;
            if (groundTicks > 0) launch(player);
        }
        if (!now && flying) {
            throttle = prevThrottle = 0;
            locked = false;
            sendState(true);
        }
        flying = now;
        // take-off window: 4 ticks after the player leaves the ground
        if (player.onGround()) groundTicks = 4; else if (groundTicks > 0) groundTicks--;
        if (!flying) return;

        Input input = event.getInput();
        float forward = input.forwardImpulse, strafe = input.leftImpulse;
        boolean up = input.jumping, down = input.shiftKeyDown;
        stickForward = Math.signum(forward);
        stickSideways = Math.signum(strafe);
        boolean accelerate = (mio_icif_ClientEvents.BOOST_KEY != null && mio_icif_ClientEvents.BOOST_KEY.isDown())
            || Minecraft.getInstance().options.keySprint.isDown();

        // a sneak tap in cruise locks or unlocks the throttle (edge detection: key down now, up in the last tick)
        if (down && !sneakWasDown && throttle > 0.2F) locked = !locked;
        sneakWasDown = down;

        prevThrottle = throttle;
        if (player.horizontalCollision && throttle > 0) {
            throttle = 0;
            locked = false;
        } else if (!locked) {
            // 0.1.9.0 advanced propulsion core: the throttle spools up faster
            float spool = propulsionVectoring(player) > 1 ? 1.3F : 1F;
            throttle = accelerate ? Math.min(1F, throttle + ManeuverMode.THROTTLE_UP * spool) : Math.max(0F, throttle - ManeuverMode.THROTTLE_DOWN);
        }

        Vec3 look = player.getViewVector(1.0F);
        // heading vectors in the horizontal plane. Minecraft yaw 0 looks to +Z. right is the right-hand side.
        float yaw = (float) Math.toRadians(player.getYRot());
        Vec3 fwdFlat = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
        Vec3 right = new Vec3(-Mth.cos(yaw), 0, -Mth.sin(yaw));
        Vec3 v = velocity;
        if (takeoffTicks > 0) {
            takeoffTicks--;
            v = new Vec3(v.x * 0.6, 1.25, v.z * 0.6);
        } else if (throttle > 0) {
            // cruise: move the velocity towards the view axis by the grip fraction. The grip falls
            // when the throttle rises. Thus the old velocity stays longer at high speed (drift).
            Vec3 target = look.scale(throttle * ManeuverMode.MAX_SPEED);
            // 0.1.9.0 advanced propulsion core: vector thrusters pull the velocity onto the view axis faster
        double grip = Math.min(0.6, ManeuverMode.grip(throttle) * propulsionVectoring(player));
            v = v.add(target.subtract(v).scale(grip));
            // leftImpulse is positive for left. -strafe * right points to the left for a left key.
            if (strafe != 0) v = v.add(right.scale(-strafe * LATERAL));
        } else {
            // hover: smooth start and smooth stop on the stick
            Vec3 wish = fwdFlat.scale(forward).add(right.scale(-strafe));
            if (wish.lengthSqr() > 1) wish = wish.normalize();
            double vy = (up ? 1 : 0) - (down ? 1 : 0);
            v = new Vec3(v.x * HOVER_DRAG, v.y * 0.8, v.z * HOVER_DRAG).add(wish.scale(HOVER_ACCEL)).add(0, vy * 0.09, 0);
            double h = v.horizontalDistance();
            if (h > HOVER_MAX) v = new Vec3(v.x / h * HOVER_MAX, v.y, v.z / h * HOVER_MAX);
            v = new Vec3(v.x, Mth.clamp(v.y, -0.55, 0.55), v.z);
            if (wish.lengthSqr() == 0 && vy == 0 && v.lengthSqr() < 1.0E-4) v = Vec3.ZERO;
        }
        velocity = v;

        // give the motion to vanilla travel with no change: no horizontal input. Also cancel in
        // advance the vertical step that aiStep adds for jump and sneak in flight.
        input.forwardImpulse = 0;
        input.leftImpulse = 0;
        input.up = input.down = input.left = input.right = false;
        double fs = player.getAbilities().getFlyingSpeed() * 3.0;
        double vy = v.y - (up ? fs : 0) + (down ? fs : 0);
        player.setDeltaMovement(v.x, vy, v.z);
        player.hasImpulse = true;
        sendState(false);
    }

    /**
     * Take-off. It sets 4 launch ticks (see {@link #onInput}). It plays the take-off sound and
     * makes a ring of 40 cloud particles and 40 block particles of the block below. The particles
     * are local to this client.
     */
    private static void launch(LocalPlayer player) {
        takeoffTicks = 4;
        var level = player.level();
        level.playLocalSound(player.getX(), player.getY(), player.getZ(), mio_icif_sounds.MANEUVER_TAKEOFF.get(), SoundSource.PLAYERS, 1.0F, 1.0F, false);
        BlockPos below = player.blockPosition().below();
        var state = level.getBlockState(below);
        for (int i = 0; i < 40; i++) {
            double a = i * Math.PI * 2 / 40;
            double c = Math.cos(a), s = Math.sin(a);
            if (!state.isAir()) level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, state), player.getX() + c * 0.6, player.getY() + 0.05,
                player.getZ() + s * 0.6, c * 0.45, 0.12, s * 0.45);
            level.addParticle(ParticleTypes.CLOUD, player.getX() + c * 0.5, player.getY() + 0.1, player.getZ() + s * 0.5, c * 0.32, 0.02, s * 0.32);
        }
    }

    /**
     * Sends the throttle and stick to the server. Rate: one packet every 20 ticks (1 second) as a
     * heartbeat. Also one packet when the state changes, at most every 2 ticks. A change is a
     * throttle change of more than 0.04, a stick change or a lock change. The heartbeat keeps the
     * relayed state on other clients younger than their 3-second limit. The method sends nothing
     * when the server does not have the channel.
     *
     * @param force true to send now, for example at flight end.
     */
    private static void sendState(boolean force) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection == null || !connection.hasChannel(SuitPackets.ManeuverInput.TYPE)) return;
        var msg = new SuitPackets.ManeuverInput(flying ? throttle : 0, stickForward, stickSideways, locked);
        boolean changed = lastSent == null || Math.abs(lastSent.throttle() - msg.throttle()) > 0.04F
            || lastSent.forward() != msg.forward() || lastSent.sideways() != msg.sideways() || lastSent.locked() != msg.locked();
        if (++sendTimer >= 20 || force || (changed && sendTimer >= 2)) {
            sendTimer = 0;
            lastSent = msg;
            PacketDistributor.sendToServer(msg);
        }
    }

    /**
     * Client tick of each player, after the move.
     *
     * <p>For the local player: a vertical collision stops the vertical velocity (not during the
     * take-off). The speed is the distance moved in this tick. On the ground at throttle 0, the
     * flight ends. The wind sound runs while the player flies.
     *
     * <p>For each player in maneuver flight: the boom starts when the throttle crosses
     * {@link ManeuverMode#SHOCK_THROTTLE} upwards. The trail gets a new entry. Above throttle 0.55,
     * vapour puffs can appear.
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player p = event.getEntity();
        if (!p.level().isClientSide) return;
        boolean local = p instanceof LocalPlayer;
        if (local) {
            if (!flying) {
                prevSpeed = speed = 0;
                stopWind();
            } else {
                // stop the commanded vertical speed at a floor or a ceiling. Otherwise it stays and pushes into the block.
                if (p.verticalCollision && takeoffTicks == 0) velocity = new Vec3(velocity.x, 0, velocity.z);
                prevSpeed = speed;
                speed = p.position().subtract(p.xo, p.yo, p.zo).length();
                if (p.onGround() && throttle == 0) flying = false;
                startWind((LocalPlayer) p);
            }
        }
        if (!inManeuver(p)) {
            Visual vis = VISUALS.get(p.getId());
            if (vis != null) {
                vis.trail.clear();
                // 0.1.10.2: a touchdown from maneuver flight raises a dust ring
                if (vis.wasFlying && p.onGround()) {
                    float strength = (float) Mth.clamp(0.55 + Math.abs(vis.lastVy) * 1.6, 0.55, 1.4);
                    com.miophas.singularity_iteration.common.client.suit.nano.ThrusterFx.landing(p, strength);
                }
                vis.wasFlying = false;
            }
            return;
        }
        Visual vis = VISUALS.computeIfAbsent(p.getId(), k -> new Visual());
        vis.wasFlying = true;
        vis.lastVy = p.getY() - p.yo;
        float thr = throttleOf(p, 1.0F);
        boolean shock = thr >= ManeuverMode.SHOCK_THROTTLE;
        if (shock && !vis.wasShock) {
            boom(p, p.position().subtract(p.xo, p.yo, p.zo), local);
            vis.coneAt = System.currentTimeMillis();           // 0.1.10.4: the vapour cone bursts only now
        }
        // 0.1.10.4: re-armed only below the barrier minus a margin, so a throttle that hovers at the
        // barrier does not boom and burst again and again
        vis.wasShock = shock || vis.wasShock && thr >= ManeuverMode.SHOCK_THROTTLE - REARM_MARGIN;
        recordTrail(p, vis, thr);
        // condensation puffs only during the burst (they used to stream all the time above throttle 0.55)
        if (FcsClientConfig.vaporEffects() && coneAge(vis, System.currentTimeMillis()) < 0.45F) vapour(p, thr);
    }

    /** 0.1.10.4: throttle below the barrier at which a new barrier crossing counts again. */
    private static final float REARM_MARGIN = 0.06F;
    /** 0.1.10.4: life of the vapour-cone burst at a barrier crossing, in milliseconds. */
    private static final long CONE_BURST_MS = 650;

    /** Age of the vapour-cone burst of a flyer, 0..1, or 1 when there is none. */
    private static float coneAge(Visual vis, long now) {
        if (vis.coneAt == 0) return 1F;
        float age = (now - vis.coneAt) / (float) CONE_BURST_MS;
        return age < 0 ? 1F : Math.min(1F, age);
    }

    // ------------------------------------------------------------------ effects

    /**
     * Starts a shock ring and plays the boom sound. The ring axis is the flight direction, or the
     * view direction when the player does not move. A remote boom plays at volume 2. Volume above 1
     * makes the sound audible from further away.
     *
     * @param dir the movement in the last tick, in blocks.
     */
    private static void boom(Player player, Vec3 dir, boolean local) {
        Vec3 axis = dir.lengthSqr() > 1.0E-4 ? dir.normalize() : player.getViewVector(1.0F);
        Vec3 centre = player.position().add(0, player.getBbHeight() * 0.5, 0);
        BOOMS.add(new Boom(centre, axis, System.currentTimeMillis(), local));
        if (BOOMS.size() > 8) BOOMS.remove(0);
        // 0.1.8.3: a burst of condensation around the ring plane, thrown outward and left behind
        if (FcsClientConfig.vaporEffects()) {
            var level = player.level();
            Vec3 t = perp(axis), b = axis.cross(t).normalize();
            int n = 26;
            for (int i = 0; i < n; i++) {
                double a = (i + level.random.nextDouble() * 0.6) * Math.PI * 2 / n;
                Vec3 radial = t.scale(Math.cos(a)).add(b.scale(Math.sin(a)));
                Vec3 at = centre.subtract(axis.scale(0.6)).add(radial.scale(0.9 + level.random.nextDouble() * 0.3));
                Vec3 vel = radial.scale(0.22 + level.random.nextDouble() * 0.12).subtract(axis.scale(0.08));
                level.addParticle(i % 3 == 0 ? ParticleTypes.POOF : ParticleTypes.CLOUD, at.x, at.y, at.z, vel.x, vel.y, vel.z);
            }
        }
        player.level().playLocalSound(player.getX(), player.getY(), player.getZ(), mio_icif_sounds.MANEUVER_BOOM.get(), SoundSource.PLAYERS,
            local ? 1.0F : 2.0F, 0.95F + player.level().random.nextFloat() * 0.1F, false);
    }

    /**
     * Condensation puffs that come off the cone edge during the burst at the barrier crossing
     * (0.1.10.4; before, they streamed all the time above throttle 0.55). They need a speed of at
     * least 0.5 blocks per tick. In each tick of the burst, two puffs appear with a probability of
     * 70 %. A puff is on a circle of radius 0.9..1.3 blocks around the flight axis, 0.8 blocks behind
     * the body centre.
     */
    private static void vapour(Player p, float thr) {
        var level = p.level();
        Vec3 v = p.position().subtract(p.xo, p.yo, p.zo);
        if (v.lengthSqr() < 0.25) return;
        Vec3 axis = v.normalize();
        Vec3 t = perp(axis), b = axis.cross(t).normalize();
        if (level.random.nextFloat() > 0.7F) return;
        for (int i = 0; i < 2; i++) {
            double a = level.random.nextDouble() * Math.PI * 2;
            double r = 0.9 + level.random.nextDouble() * 0.4;
            Vec3 at = p.position().add(0, p.getBbHeight() * 0.55, 0).subtract(axis.scale(0.8)).add(t.scale(Math.cos(a) * r)).add(b.scale(Math.sin(a) * r));
            level.addParticle(ParticleTypes.POOF, at.x, at.y, at.z, v.x * 0.1, v.y * 0.1, v.z * 0.1);
        }
    }

    /**
     * Adds one trail entry: 4 emitter points (right hand, left hand, right foot, left foot) in world
     * blocks. The points are fixed offsets in a frame aligned with the flight direction. They do not
     * come from the rendered limb pose. Below throttle 0.45, the method removes the oldest entry
     * per tick, so the trail shrinks from its tail. The trail holds at most 28 entries (1.4 s).
     */
    private static void recordTrail(Player p, Visual vis, float thr) {
        if (thr < 0.45F) {
            if (!vis.trail.isEmpty()) vis.trail.removeFirst();
            return;
        }
        Vec3 v = p.position().subtract(p.xo, p.yo, p.zo);
        Vec3 axis = v.lengthSqr() > 1.0E-4 ? v.normalize() : p.getViewVector(1.0F);
        Vec3 side = perp(axis);
        Vec3 up = axis.cross(side).normalize();
        Vec3 centre = p.position().add(0, p.getBbHeight() * 0.5, 0);
        // a flyer lies along the path: hands forward and out, feet behind
        Vec3[] pts = {
            centre.add(side.scale(0.42)).add(axis.scale(0.15)),
            centre.add(side.scale(-0.42)).add(axis.scale(0.15)),
            centre.add(side.scale(0.13)).subtract(axis.scale(0.95)).add(up.scale(-0.05)),
            centre.add(side.scale(-0.13)).subtract(axis.scale(0.95)).add(up.scale(-0.05))};
        vis.trail.addLast(pts);
        while (vis.trail.size() > 28) vis.trail.removeFirst();
    }

    /**
     * A unit vector at 90 degrees to a unit axis. It uses world up as the helper vector. When the
     * axis is almost vertical (|y| >= 0.95), it uses world +X, because the cross product with up is
     * then too short.
     */
    static Vec3 perp(Vec3 axis) {
        return Math.abs(axis.y) < 0.95 ? axis.cross(new Vec3(0, 1, 0)).normalize() : axis.cross(new Vec3(1, 0, 0)).normalize();
    }

    // ------------------------------------------------------------------ sound

    private static void startWind(LocalPlayer p) {
        if (wind == null || wind.isStopped()) {
            wind = new WindSound(p);
            Minecraft.getInstance().getSoundManager().play(wind);
        }
    }

    private static void stopWind() {
        if (wind != null) wind.end();
        wind = null;
    }

    /**
     * Wind loop, relative to the listener. Each tick sets the volume (0.08..0.98) and the pitch
     * (0.7..1.4) from the speed as a fraction of {@link ManeuverMode#MAX_SPEED}. The sound stops
     * when the flight ends.
     */
    static final class WindSound extends AbstractTickableSoundInstance {
        private final LocalPlayer player;
        WindSound(LocalPlayer player) {
            super(mio_icif_sounds.MANEUVER_WIND.get(), SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
            this.player = player;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.01F;
            this.relative = true;
        }
        void end() { stop(); }
        @Override public void tick() {
            if (player.isRemoved() || !flying) { stop(); return; }
            float s = (float) Math.min(1, speed / ManeuverMode.MAX_SPEED);
            volume = 0.08F + s * 0.9F;
            pitch = 0.7F + s * 0.7F;
        }
    }

    // ------------------------------------------------------------------ camera

    /**
     * Speed FOV. The target increase is {@code 0.075 x speed} (speed in blocks per tick), at most
     * 0.30 (30 % at 4 blocks per tick). The value moves 12 % of the way to the target on each call.
     * Vanilla calls this event one time per tick.
     */
    @SubscribeEvent
    public static void onFov(ComputeFovModifierEvent event) {
        float target = flying && FcsClientConfig.speedFov() ? (float) Math.min(0.30, speed * 0.075) : 0;
        fovBlend += (target - fovBlend) * 0.12F;
        if (fovBlend > 0.001F) event.setNewFovModifier(event.getNewFovModifier() * (1 + fovBlend));
    }

    /**
     * Camera bank. In cruise, the roll is 1.4 x the yaw change since the last frame, in degrees,
     * limited to +-22 degrees. In hover, the strafe key gives 2.5 degrees. The two parts blend
     * with the cruise factor {@code throttle / 0.35}. The roll moves 8 % of the way to the target in
     * each frame.
     *
     * <p>Vanilla fires this event one time per frame. The yaw change and the smoothing are per
     * frame, not per second. Thus the bank depends on the frame rate.
     */
    @SubscribeEvent
    public static void onCamera(ViewportEvent.ComputeCameraAngles event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        float target = 0;
        if (flying && FcsClientConfig.cameraRoll()) {
            float yawRate = Mth.wrapDegrees(player.getYRot() - lastYaw);
            target = Mth.clamp(yawRate * 1.4F, -22, 22) * Math.min(1, throttle / 0.35F) - stickSideways * 2.5F * (1 - Math.min(1, throttle / 0.35F));
        }
        lastYaw = player.getYRot();
        roll += (target - roll) * 0.08F;
        if (Math.abs(roll) > 0.05F) event.setRoll(event.getRoll() + roll);
    }

    // ------------------------------------------------------------------ body pose

    /**
     * Whole-body transform: hover lean and heave, or cruise alignment and bank. It runs per frame
     * for each rendered player.
     *
     * <p>Time step: {@code dt} is the wall-clock time since the last frame of this player, in
     * seconds, at most 0.1. The smoothing uses {@code 1 - exp(-rate * dt)} or
     * {@code min(1, rate * dt)}. Thus the result is the same at all frame rates.
     *
     * <p>Pose math, in degrees:
     * <ul>
     *   <li>transition: 0..1 at 2.2 per second (about 0.45 s to blend in or out).</li>
     *   <li>cruise factor: {@code clamp(throttle / 0.35, 0, 1)}. 0 = hover pose, 1 = cruise pose.</li>
     *   <li>Hover pitch (lean): {@code 7 + 9 x forward stick}, so -2..16. Hover roll:
     *       {@code -10 x sideways stick}.</li>
     *   <li>Cruise pitch: {@code 90 + view pitch}. A level view gives a horizontal body. A view
     *       straight down (+90) gives 180, head first down. A view straight up (-90) gives 0, upright.</li>
     *   <li>Cruise roll (bank): {@code -0.28 x yaw rate} in degrees per second, limited to +-38.</li>
     *   <li>Heave: {@code (0.35 h^3 + 0.65 h) x 0.055} blocks, with {@code h = sin(1.7 t)}. The period
     *       is about 3.7 s. It applies only in hover.</li>
     * </ul>
     *
     * <p>Rotation: the pivot is at 0.55 x body height. The quaternion is
     * {@code Ry(b) * Rx(pitch) * Rz(roll) * Ry(-b)}, with {@code b} the body yaw. JOML post-multiplies,
     * so a vertex gets the rightmost rotation first. Ry(-b) turns the vertex into the body frame
     * (body forward = +Z). Then roll turns around the body Z axis, and pitch turns around the body X
     * axis. A positive pitch tips the head forward. Ry(b) turns the vertex back to the world frame.
     * Because roll comes before pitch, the roll axis also turns with the pitch. At a cruise pitch near
     * 90 degrees, the body Z axis points down. Then the roll turns the body around the vertical axis.
     *
     * <p>The method pushes the pose stack. {@link #onRenderPlayerPost} pops it. {@link #POSED}
     * records the push.
     */
    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player p = event.getEntity();
        Visual vis = VISUALS.get(p.getId());
        boolean flyingNow = inManeuver(p);
        if (vis == null) {
            if (!flyingNow) return;
            vis = VISUALS.computeIfAbsent(p.getId(), k -> new Visual());
        }
        float partial = event.getPartialTick();
        long nowMs = System.currentTimeMillis();
        float dt = vis.lastTick == 0 ? 0.05F : Math.min(0.1F, (nowMs - vis.lastTick) / 1000F);
        vis.lastTick = nowMs;
        vis.transition = Mth.clamp(vis.transition + (flyingNow ? dt * 2.2F : -dt * 2.2F), 0, 1);
        if (vis.transition <= 0.001F) return;

        float thr = throttleOf(p, partial);
        float cruise = Mth.clamp(thr / 0.35F, 0, 1);
        float[] stick = stickOf(p);
        float yaw = Mth.lerp(partial, p.yRotO, p.getYRot());
        float yawDelta = Mth.wrapDegrees(yaw - vis.lastYaw);
        vis.lastYaw = yaw;
        float turn = dt > 0 ? yawDelta / dt : 0;
        vis.turnSpeed += (turn - vis.turnSpeed) * Math.min(1, dt * 8);

        // hover: a small forward lean. The forward stick makes it deeper. The strafe keys bank a little.
        float leanTarget = 7.0F + 9.0F * stick[0];
        vis.lean += (leanTarget - vis.lean) * Math.min(1, dt * 5);
        float hoverPitch = vis.lean, hoverRoll = -stick[1] * 10.0F;
        // cruise: body along the view axis, banked into the turn
        float lookPitch = Mth.lerp(partial, p.xRotO, p.getXRot());
        float cruisePitch = 90.0F + lookPitch;
        float cruiseRoll = Mth.clamp(-vis.turnSpeed * 0.28F, -38, 38);
        float targetPitch = Mth.lerp(cruise, hoverPitch, cruisePitch) * vis.transition;
        float targetRoll = Mth.lerp(cruise, hoverRoll, cruiseRoll) * vis.transition;
        float k = 1 - (float) Math.exp(-5.0 * dt);
        vis.pitch += (targetPitch - vis.pitch) * k;
        vis.roll += (targetRoll - vis.roll) * k;
        vis.heave += dt;

        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        float mid = p.getBbHeight() * 0.55F;
        // heave: a slow rise and fall (shaped sine), only in hover
        float h = (float) Math.sin(vis.heave * 1.7) ;
        float heave = (h * h * h * 0.35F + h * 0.65F) * 0.055F * (1 - cruise) * vis.transition;
        pose.translate(0, heave, 0);
        pose.translate(0, mid, 0);
        float bodyYaw = (float) Math.toRadians(-Mth.lerp(partial, p.yBodyRotO, p.yBodyRot));
        // rotate in body space. Vertex order: undo the body yaw, roll around body Z, pitch around body X, apply the body yaw.
        pose.mulPose(new Quaternionf().rotateY(bodyYaw).rotateX((float) Math.toRadians(vis.pitch)).rotateZ((float) Math.toRadians(vis.roll)).rotateY(-bodyYaw));
        pose.translate(0, -mid, 0);
        POSED.add(p.getId());
    }

    /** Entity ids of the players with a pushed pose. Each id is removed in the Post event. */
    private static final it.unimi.dsi.fastutil.ints.IntOpenHashSet POSED = new it.unimi.dsi.fastutil.ints.IntOpenHashSet();

    /** Pops the pose that {@link #onRenderPlayerPre} pushed. It does nothing when no push was made. */
    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        if (POSED.remove(event.getEntity().getId())) event.getPoseStack().popPose();
    }

    /**
     * Limb pose (0.1.8.0: thrust-vector attitude control, "Iron Man" style).
     * PlayerManeuverPoseMixin calls it after vanilla {@code setupAnim}. Angles are in radians.
     * The method blends from the vanilla angles to the maneuver angles by the transition value.
     *
     * <p>The palms and the soles carry the thrusters. Each limb points its thruster against the
     * motion that it makes:
     * <ul>
     *   <li><b>Hover.</b> The legs point straight down (lift). The arms hang a little out, palms
     *       down. Forward stick swings both arms back (thrust to the rear) and the legs trail; back
     *       stick swings the arms forward (brake). A strafe to one side opens the arm of the other
     *       side (side thrust). Climbing brings the arms in; sinking opens them. Small fast motions
     *       of the arms show the attitude corrections of the thrusters.</li>
     *   <li><b>Cruise.</b> The body lies along the path (see {@link #onRenderPlayerPre}). The arms
     *       lie along the body, palms aft; the legs are straight back and together. In a turn, the
     *       arm on the inside of the turn opens as a control surface; the other arm stays in. A
     *       strafe opens the arm on the far side.</li>
     *   <li><b>Full burn</b> (throttle above 0.75): the arms press against the body and move a
     *       little back, the legs close fully. The 0.1.7.x "lead arm" (one arm above the head) is
     *       removed.</li>
     *   <li>The head turns up by 1.25 rad in cruise, so the flyer looks forward while it lies flat.</li>
     * </ul>
     */
    public static void applyLimbPose(net.minecraft.client.model.HumanoidModel<?> model, Player p) {
        Visual vis = VISUALS.get(p.getId());
        if (vis == null || vis.transition <= 0.001F) return;
        float t = vis.transition;
        float thr = throttleOf(p, 1.0F);
        float cruise = Mth.clamp(thr / 0.35F, 0, 1);
        float burn = Mth.clamp((thr - 0.75F) / 0.2F, 0, 1);
        float[] stick = stickOf(p);
        float f = stick[0], s = stick[1];   // forward stick, strafe (+1 = left)
        double climb = p.getY() - p.yo;
        float time = vis.heave;
        // attitude corrections: small, fast, different on each arm
        float jx = (float) Math.sin(time * 9.1) * 0.022F, jz = (float) Math.sin(time * 7.3 + 1.0) * 0.018F;

        // ---- hover: legs give lift, arms vector the thrust against the motion
        float hoverArmX = 0.05F + 0.55F * f;
        float open = (float) Mth.clamp(1.0 - climb * 2.5, 0.6, 1.3);
        float hoverRightZ = (0.30F + 0.45F * Math.max(s, 0) - 0.15F * Math.max(-s, 0)) * open;
        float hoverLeftZ = -(0.30F + 0.45F * Math.max(-s, 0) - 0.15F * Math.max(s, 0)) * open;
        float hoverLegX = 0.06F + 0.25F * Math.max(f, 0);

        // ---- cruise: arms along the body; the inside arm opens in a turn
        float turn = Mth.clamp(vis.turnSpeed / 120F, -1, 1);     // + = turning right
        float tight = Mth.lerp(burn, 0.14F, 0.06F);
        float cruiseRightZ = tight + 0.85F * Math.max(turn, 0) + 0.3F * Math.max(s, 0);
        float cruiseLeftZ = -(tight + 0.85F * Math.max(-turn, 0) + 0.3F * Math.max(-s, 0));
        float cruiseArmX = Mth.lerp(burn, 0.0F, 0.12F);
        float cruiseLegZ = Mth.lerp(burn, 0.03F, 0.0F);

        float rightX = Mth.lerp(cruise, hoverArmX + jx, cruiseArmX + jx * 0.4F);
        float leftX = Mth.lerp(cruise, hoverArmX - jx, cruiseArmX - jx * 0.4F);
        float rightZ = Mth.lerp(cruise, hoverRightZ + jz, cruiseRightZ);
        float leftZ = Mth.lerp(cruise, hoverLeftZ - jz, cruiseLeftZ);
        mix(model.rightArm, rightX, 0.10F, rightZ, t);
        mix(model.leftArm, leftX, -0.10F, leftZ, t);
        float legX = Mth.lerp(cruise, hoverLegX, 0.02F);
        float legZ = Mth.lerp(cruise, 0.03F, cruiseLegZ);
        mix(model.rightLeg, legX, 0.0F, legZ, t);
        mix(model.leftLeg, legX + Mth.lerp(cruise, 0.04F, 0.02F), 0.0F, -legZ, t);
        model.head.xRot = Mth.lerp(cruise * t, model.head.xRot, -1.25F);
        model.hat.copyFrom(model.head);
    }

    /**
     * Thruster output of each limb for the flight layer, 0..1: right hand, left hand, right foot,
     * left foot. It follows the same rules as {@link #applyLimbPose}.
     */
    public static float[] thrusterOutput(Player p) {
        Visual vis = VISUALS.get(p.getId());
        if (vis == null || vis.transition <= 0.001F) return null;
        float thr = throttleOf(p, 1.0F);
        float cruise = Mth.clamp(thr / 0.35F, 0, 1);
        float[] stick = stickOf(p);
        float turn = Mth.clamp(vis.turnSpeed / 120F, -1, 1);
        float hoverHands = 0.45F + 0.25F * Math.abs(stick[0]);
        float hoverFeet = 0.75F;
        float right = Mth.lerp(cruise, hoverHands + 0.3F * Math.max(stick[1], 0), 0.35F + 0.4F * thr + 0.3F * Math.max(turn, 0));
        float left = Mth.lerp(cruise, hoverHands + 0.3F * Math.max(-stick[1], 0), 0.35F + 0.4F * thr + 0.3F * Math.max(-turn, 0));
        float feet = Mth.lerp(cruise, hoverFeet, 0.5F + 0.5F * thr);
        float t = vis.transition;
        return new float[]{Math.min(1, right) * t, Math.min(1, left) * t, feet * t, feet * t};
    }

    /** Strength of the aerodynamic deflection field, 0..1 (cruise only). */
    public static float deflectionField(Player p, float partial) {
        Visual vis = VISUALS.get(p.getId());
        if (vis == null) return 0;
        float thr = throttleOf(p, partial);
        float x = Mth.clamp((thr - 0.3F) / 0.4F, 0, 1);
        return x * x * (3 - 2 * x) * vis.transition;
    }

    /** Animation time of a flyer in seconds (for the flight layer). */
    static float visualTime(Player p) {
        Visual vis = VISUALS.get(p.getId());
        return vis == null ? 0 : vis.heave;
    }

    /** Blends the rotation of a model part to (x, y, z) radians by the weight w (0 = no change, 1 = full). */
    private static void mix(net.minecraft.client.model.geom.ModelPart part, float x, float y, float z, float w) {
        part.xRot = Mth.lerp(w, part.xRot, x);
        part.yRot = Mth.lerp(w, part.yRot, y);
        part.zRot = Mth.lerp(w, part.zRot, z);
    }

    // ------------------------------------------------------------------ world pass

    /**
     * World pass of the effects. {@code FcsClient} calls it at the {@code AFTER_PARTICLES} stage, so
     * the effects draw over the particles. The pose matrix moves world coordinates to camera-relative
     * coordinates. Draw order: wake trails, vapour cones (additive glow), shock rings. The method
     * does not draw the cone of the local player in first person, because the cone would cover the
     * view.
     */
    static void renderWorld(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        long now = System.currentTimeMillis();
        Vec3 cam = event.getCamera().getPosition();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f m = pose.last().pose();
        boolean firstPerson = mc.options.getCameraType().isFirstPerson();
        boolean vapour = FcsClientConfig.vaporEffects();
        alphaScale = FcsClientConfig.vaporIntensity();

        VertexConsumer vc = buffers.getBuffer(SiRenderTypes.VAPOR);
        for (Player p : mc.level.players()) {
            Visual vis = VISUALS.get(p.getId());
            if (vis == null || !vapour) continue;
            if (!vis.trail.isEmpty()) wake(vc, m, vis, cam);
        }
        buffers.endBatch(SiRenderTypes.VAPOR);
        // 0.1.8.0: the condensation cone is an alpha-blended white veil, so it shows on a bright sky.
        // (0.1.7.x drew it as an additive glow at about 6 % alpha at full throttle: it was not visible.)
        // In first person the own cone fades near the camera and shows only at the edge of the view.
        // 0.1.10.4: the cone is a short burst at the barrier crossing, not a lasting cloud.
        VertexConsumer veil = buffers.getBuffer(SiRenderTypes.VAPOR);
        for (Player p : mc.level.players()) {
            Visual vis = VISUALS.get(p.getId());
            if (!vapour || vis == null) continue;
            float age = coneAge(vis, now);
            if (age < 1F) cone(veil, m, p, age, partial, now, p == mc.player && firstPerson ? cam : null);
        }
        buffers.endBatch(SiRenderTypes.VAPOR);
        // supersonic shock shell (additive) and the first-person rim of the deflection field
        VertexConsumer glowCone = buffers.getBuffer(SiRenderTypes.FIELD_GLOW);
        for (Player p : mc.level.players()) {
            if (!VISUALS.containsKey(p.getId()) || p == mc.player && firstPerson) continue;
            float thr = throttleOf(p, partial);
            if (thr > ManeuverMode.SHOCK_THROTTLE) machShell(glowCone, m, p, thr, partial, now);
        }
        if (firstPerson && mc.player != null && VISUALS.containsKey(mc.player.getId())) {
            float field = deflectionField(mc.player, partial);
            if (field > 0.02F) fieldRim(glowCone, m, mc.player, field, partial, now);
        }
        buffers.endBatch(SiRenderTypes.FIELD_GLOW);

        if (!vapour) BOOMS.clear();
        if (!BOOMS.isEmpty()) {
            // 0.1.8.3: condensation ring (alpha veil), then the additive shock front and flash
            Vec3 ownCam = firstPerson ? cam : null;
            VertexConsumer veilRing = buffers.getBuffer(SiRenderTypes.VAPOR);
            for (Iterator<Boom> it = BOOMS.iterator(); it.hasNext(); ) {
                Boom b = it.next();
                float age = (now - b.start) / (float) BOOM_LIFE_MS;
                if (age >= 1) { it.remove(); continue; }
                shockRing(veilRing, m, b, age, b.own ? ownCam : null);
            }
            buffers.endBatch(SiRenderTypes.VAPOR);
            VertexConsumer front = buffers.getBuffer(SiRenderTypes.FIELD_GLOW);
            for (Boom b : BOOMS) {
                float age = (now - b.start) / (float) BOOM_LIFE_MS;
                shockFront(front, m, b, age, b.own ? ownCam : null);
            }
            buffers.endBatch(SiRenderTypes.FIELD_GLOW);
        }
        pose.popPose();
    }

    /**
     * Wake trails: ribbons that face the camera. A ribbon gets wider and fainter with age. The
     * half width is 0.028 blocks (hands) or 0.02 blocks (feet) at the newest point. It grows by
     * 0.09 blocks at the oldest point. The alpha is {@code (1 - age)^2 x 0.18}. The side vector is
     * {@code segment x toCamera}, so the ribbon is flat to the view.
     */
    private static void wake(VertexConsumer vc, Matrix4f m, Visual vis, Vec3 cam) {
        Vec3[][] pts = vis.trail.toArray(new Vec3[0][]);
        int n = pts.length;
        for (int e = 0; e < 4; e++) {
            float width0 = e < 2 ? 0.028F : 0.02F;
            for (int i = 1; i < n; i++) {
                Vec3 a = pts[i - 1][e], b = pts[i][e];
                float ageA = (n - i) / (float) n, ageB = (n - 1 - i) / (float) n;
                Vec3 dir = b.subtract(a);
                if (dir.lengthSqr() < 1.0E-6) continue;
                Vec3 toCam = cam.subtract(a).normalize();
                Vec3 side = dir.cross(toCam).normalize();
                float wa = width0 + ageA * 0.09F, wb = width0 + ageB * 0.09F;
                float aa = (1 - ageA) * (1 - ageA) * 0.30F, ab = (1 - ageB) * (1 - ageB) * 0.30F;
                // fade out near the camera (from 7.5 blocks to 1.5 blocks), where the ribbon would cover the view
                aa *= (float) Mth.clamp((a.distanceTo(cam) - 1.5) / 6.0, 0, 1);
                ab *= (float) Mth.clamp((b.distanceTo(cam) - 1.5) / 6.0, 0, 1);
                put(vc, m, a.add(side.scale(wa)), 0.95F, 0.97F, 1F, aa * 0.6F);
                put(vc, m, a.subtract(side.scale(wa)), 0.95F, 0.97F, 1F, aa * 0.6F);
                put(vc, m, b.subtract(side.scale(wb)), 0.95F, 0.97F, 1F, ab * 0.6F);
                put(vc, m, b.add(side.scale(wb)), 0.95F, 0.97F, 1F, ab * 0.6F);
            }
        }
    }

    /**
     * Vapour cone (0.1.8.3 redesign): a condensation bell behind a bright collar.
     *
     * <p>0.1.8.0 drew a noisy veil of even density: it read as a grey lump. The cone now has
     * the structure of a real Prandtl-Glauert cloud:
     * <ul>
     *   <li>Collar: the densest part is the leading edge just behind the apex,
     *       {@code exp(-z / 0.22)}; the shell then thins out toward the tail,
     *       {@code 0.32 (1 - z / L)^1.6}.</li>
     *   <li>Shape: radius {@code 0.18 + 1.1 (1 - exp(-2.4 z))}, so the bell opens quickly and then
     *       runs almost parallel. Length L = 2.2 blocks, 10 rings (denser at the front), 40 segments.</li>
     *   <li>Striations: fine bright streaks run back along the shell
     *       ({@code cos(14 a + drift)}), broken up by the slow noise of {@link #soft}.</li>
     *   <li>Two shells: the outer one white, an inner one at 0.82 of the radius with a third of the
     *       alpha and a cold blue tint, which gives the cloud depth.</li>
     * </ul>
     *
     * <p>Strength (0.1.10.4): a burst of {@link #CONE_BURST_MS} at the barrier crossing. It snaps
     * in over the first 10 % of its life, then fades with {@code (1 - t)^1.8} while the bell opens
     * 50 % wider and 60 % longer and drifts back along the path, so the cloud is torn off behind the
     * flyer. Up to 0.1.10.3 the cone peaked at throttle 0.66 and kept a thin veil at any higher
     * throttle: at cruise it hung in front of the view all the time. The axis is the movement from
     * the last tick position to the interpolated position, or the view vector below 0.2 blocks of
     * movement.
     *
     * @param age    age of the burst, 0..1
     * @param camera the camera position for the own cone in first person (near fade), else null
     */
    private static void cone(VertexConsumer vc, Matrix4f m, Player p, float age, float partial, long now, Vec3 camera) {
        Vec3 v = p.getPosition(partial).subtract(p.xo, p.yo, p.zo);
        Vec3 axis = v.lengthSqr() > 0.04 ? v.normalize() : p.getViewVector(partial);
        Vec3 t = perp(axis), b = axis.cross(t).normalize();
        float open = 1 - (float) Math.pow(1 - age, 2);
        Vec3 apex = p.getPosition(partial).add(0, p.getBbHeight() * 0.55, 0).add(axis.scale(0.5 - 1.6 * open));
        float strength = age < 0.1F ? age / 0.1F : (float) Math.pow(1 - (age - 0.1F) / 0.9F, 1.8);
        if (strength <= 0.01F) return;
        int seg = 40, rings = 10;
        double length = 2.2 * (1 + 0.6 * open);
        double widen = 1 + 0.5 * open;
        // wrap the clock to 600 s: noise() floors its input to int, and epoch seconds x 2 overflow it
        double time = (now % 600_000L) / 1000.0;
        for (int shell = 0; shell < 2; shell++) {
            double scaleR = (shell == 0 ? 1.0 : 0.82) * widen;
            float shellAlpha = shell == 0 ? 1.0F : 0.34F;
            float cr = shell == 0 ? 1.0F : 0.82F, cg = shell == 0 ? 1.0F : 0.92F;
            for (int r = 0; r < rings; r++) {
                double z0 = length * Math.pow(r / (double) rings, 1.5), z1 = length * Math.pow((r + 1) / (double) rings, 1.5);
                double rad0 = scaleR * (0.18 + 1.1 * (1 - Math.exp(-2.4 * z0))), rad1 = scaleR * (0.18 + 1.1 * (1 - Math.exp(-2.4 * z1)));
                float den0 = coneDensity(z0, length), den1 = coneDensity(z1, length);
                for (int s = 0; s < seg; s++) {
                    double a0 = s * Math.PI * 2 / seg, a1 = (s + 1) * Math.PI * 2 / seg;
                    Vec3 d0 = t.scale(Math.cos(a0)).add(b.scale(Math.sin(a0))), d1 = t.scale(Math.cos(a1)).add(b.scale(Math.sin(a1)));
                    Vec3 p0 = apex.subtract(axis.scale(z0)).add(d0.scale(rad0)), p1 = apex.subtract(axis.scale(z0)).add(d1.scale(rad0));
                    Vec3 p2 = apex.subtract(axis.scale(z1)).add(d1.scale(rad1)), p3 = apex.subtract(axis.scale(z1)).add(d0.scale(rad1));
                    float base = 0.5F * strength * shellAlpha;
                    put(vc, m, p0, cr, cg, 1, base * den0 * streak(a0, z0, time) * near(p0, camera));
                    put(vc, m, p1, cr, cg, 1, base * den0 * streak(a1, z0, time) * near(p1, camera));
                    put(vc, m, p2, cr, cg, 1, base * den1 * streak(a1, z1, time) * near(p2, camera));
                    put(vc, m, p3, cr, cg, 1, base * den1 * streak(a0, z1, time) * near(p3, camera));
                }
            }
        }
    }

    /** Density along the cone: a bright collar right behind the apex, then a thin, fading veil. */
    private static float coneDensity(double z, double length) {
        double collar = Math.exp(-z / 0.22);
        double veil = 0.32 * Math.pow(Math.max(0, 1 - z / length), 1.6);
        double lead = Mth.clamp(z / 0.05, 0, 1);   // the apex itself is open
        return (float) Math.min(1.0, (collar + veil) * lead);
    }

    /** Streaks that run back along the shell, broken up by the slow noise. Range about 0.25..1. */
    private static float streak(double angle, double z, double time) {
        double lines = 0.5 + 0.5 * Math.cos(angle * 14 + time * 0.8 + z * 1.4);
        return (float) (0.25 + 0.75 * (0.55 * lines + 0.45 * soft(angle, z, time)));
    }

    /** First-person near fade: 0 within 0.7 blocks of the camera, 1 beyond 2.2 blocks. 1 without a camera. */
    private static float near(Vec3 point, Vec3 camera) {
        if (camera == null) return 1;
        return (float) Mth.clamp((point.distanceTo(camera) - 0.7) / 1.5, 0, 1);
    }

    /**
     * Supersonic shock shell (0.1.8.0): a thin additive cone that starts at the nose and opens
     * backwards. Its half angle follows the Mach angle {@code asin(1 / M)}; M is 1.0 at the shock
     * throttle and 2.5 at full throttle. Bright bands run back along the shell.
     */
    private static void machShell(VertexConsumer vc, Matrix4f m, Player p, float thr, float partial, long now) {
        Vec3 v = p.getPosition(partial).subtract(p.xo, p.yo, p.zo);
        if (v.lengthSqr() < 0.25) return;
        Vec3 axis = v.normalize();
        Vec3 t = perp(axis), b = axis.cross(t).normalize();
        float x = Mth.clamp((thr - ManeuverMode.SHOCK_THROTTLE) / (1 - ManeuverMode.SHOCK_THROTTLE), 0, 1);
        double mach = 1.0 + 1.5 * x;
        double half = Math.asin(Math.min(1.0, 1.0 / Math.max(1.02, mach)));
        double length = 2.6, radiusEnd = Math.tan(half) * length;
        radiusEnd = Math.min(radiusEnd, 3.2);
        Vec3 nose = p.getPosition(partial).add(0, p.getBbHeight() * 0.55, 0).add(axis.scale(0.95));
        float strength = 0.16F * Mth.clamp(x * 4, 0, 1);
        double time = (now % 600_000L) / 1000.0;
        int seg = 28, rings = 5;
        for (int r = 0; r < rings; r++) {
            double z0 = length * r / rings, z1 = length * (r + 1) / rings;
            double r0 = radiusEnd * z0 / length, r1 = radiusEnd * z1 / length;
            float band0 = 0.4F + 0.6F * (float) Math.max(0, Math.sin(z0 * 3.1 - time * 14));
            float band1 = 0.4F + 0.6F * (float) Math.max(0, Math.sin(z1 * 3.1 - time * 14));
            float fade0 = 1 - (float) (z0 / length), fade1 = 1 - (float) (z1 / length);
            for (int s = 0; s < seg; s++) {
                double a0 = s * Math.PI * 2 / seg, a1 = (s + 1) * Math.PI * 2 / seg;
                Vec3 d0 = t.scale(Math.cos(a0)).add(b.scale(Math.sin(a0))), d1 = t.scale(Math.cos(a1)).add(b.scale(Math.sin(a1)));
                put(vc, m, nose.subtract(axis.scale(z0)).add(d0.scale(r0)), 0.75F, 0.9F, 1, strength * fade0 * band0);
                put(vc, m, nose.subtract(axis.scale(z0)).add(d1.scale(r0)), 0.75F, 0.9F, 1, strength * fade0 * band0);
                put(vc, m, nose.subtract(axis.scale(z1)).add(d1.scale(r1)), 0.75F, 0.9F, 1, strength * fade1 * band1);
                put(vc, m, nose.subtract(axis.scale(z1)).add(d0.scale(r1)), 0.75F, 0.9F, 1, strength * fade1 * band1);
            }
        }
    }

    /**
     * First-person view of the aerodynamic deflection field (0.1.8.0): a faint hexagonal rim
     * 1.5 blocks ahead of the eye, across the flight path. The flight layer draws the full field on
     * the body in third person and for other players.
     */
    private static void fieldRim(VertexConsumer vc, Matrix4f m, Player p, float field, float partial, long now) {
        Vec3 v = p.getPosition(partial).subtract(p.xo, p.yo, p.zo);
        Vec3 axis = v.lengthSqr() > 0.04 ? v.normalize() : p.getViewVector(partial);
        Vec3 t = perp(axis), b = axis.cross(t).normalize();
        Vec3 centre = p.getEyePosition(partial).add(axis.scale(1.5));
        double time = (now % 600_000L) / 1000.0;
        int seg = 6 * 8;
        double rOut = 1.15, rIn = 1.05;
        for (int s = 0; s < seg; s++) {
            double a0 = s * Math.PI * 2 / seg, a1 = (s + 1) * Math.PI * 2 / seg;
            Vec3 d0 = t.scale(Math.cos(a0)).add(b.scale(Math.sin(a0))), d1 = t.scale(Math.cos(a1)).add(b.scale(Math.sin(a1)));
            float pulse = 0.55F + 0.45F * (float) Math.sin(a0 * 6 + time * 5);
            float alpha = 0.16F * field * pulse;
            put(vc, m, centre.add(d0.scale(rOut)).subtract(axis.scale(0.35)), 0.45F, 0.85F, 1, 0);
            put(vc, m, centre.add(d1.scale(rOut)).subtract(axis.scale(0.35)), 0.45F, 0.85F, 1, 0);
            put(vc, m, centre.add(d1.scale(rIn)), 0.45F, 0.85F, 1, alpha);
            put(vc, m, centre.add(d0.scale(rIn)), 0.45F, 0.85F, 1, alpha);
        }
    }

    /**
     * Cone density at an angle and a distance along the axis. The value is smooth and moves
     * slowly. It is in the range 0.35..1, so the cone is never fully empty.
     */
    private static float soft(double angle, double z, double time) {
        // sample the noise on a circle, so the value at angle 0 and at angle 2 pi is the same (no seam)
        double cx = Math.cos(angle) * 1.6, cy = Math.sin(angle) * 1.6;
        float n = noise(cx + z * 1.3 - time * 2.2, cy + 7.1) * 0.6F + noise(cy * 2 + 3.3, cx * 2 - time * 3.1 + z) * 0.4F;
        return 0.35F + 0.65F * n;
    }

    /**
     * Smooth 2D value noise in 0..1. The same inputs always give the same value. It interpolates the
     * hash values of the 4 grid corners with the smoothstep curve {@code 3t^2 - 2t^3}.
     */
    private static float noise(double x, double y) {
        int xi = Mth.floor(x), yi = Mth.floor(y);
        double fx = x - xi, fy = y - yi;
        double u = fx * fx * (3 - 2 * fx), w = fy * fy * (3 - 2 * fy);
        double a = hash(xi, yi), b = hash(xi + 1, yi), c = hash(xi, yi + 1), d = hash(xi + 1, yi + 1);
        return (float) Mth.clamp(a + (b - a) * u + (c - a) * w + (a - b - c + d) * u * w, 0, 1);
    }

    /** Integer hash of a grid corner, in 0..1. */
    private static double hash(int x, int y) {
        int h = x * 374761393 + y * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        return ((h ^ (h >>> 16)) & 0xFFFF) / 65535.0;
    }

    /** Life of a boom (shock ring, front and flash) in milliseconds. */
    private static final long BOOM_LIFE_MS = 1100;

    /**
     * Condensation ring of a boom (0.1.8.3 redesign): a soft, slightly cupped torus of vapour
     * across the path. The radius eases out ({@code 0.6 + 8.4 (1 - (1 - age)^2.4)}), so the ring
     * bursts out and then slows like a real shock cloud; it drifts 3 blocks back along the axis.
     * Across the ring the alpha rises from 0 at the outer rim to a peak at 0.86 of the radius and
     * falls to 0 at 0.62, in three bands, so there is no hard edge. Turbulence (noise along the ring)
     * breaks up the density; the alpha is {@code (1 - age)^1.6 x 0.5}.
     *
     * @param camera camera position for the own boom in first person (near fade), else null
     */
    private static void shockRing(VertexConsumer vc, Matrix4f m, Boom b, float age, Vec3 camera) {
        Vec3 t = perp(b.axis), bn = b.axis.cross(t).normalize();
        Vec3 c = b.pos.subtract(b.axis.scale(age * 3.0));
        float ease = 1 - (float) Math.pow(1 - age, 2.4);
        float rOut = 0.6F + 8.4F * ease;
        float[] radius = {rOut, rOut * 0.86F, rOut * 0.74F, rOut * 0.62F};
        float[] weight = {0F, 1F, 0.55F, 0F};
        float[] cup = {0F, 0.22F, 0.36F, 0.42F};        // the inner part trails behind: a shallow cup
        float alpha = (float) Math.pow(1 - age, 1.6) * 0.5F;
        int steps = 72;
        for (int i = 0; i < steps; i++) {
            double a0 = i * Math.PI * 2 / steps, a1 = (i + 1) * Math.PI * 2 / steps;
            Vec3 d0 = t.scale(Math.cos(a0)).add(bn.scale(Math.sin(a0))), d1 = t.scale(Math.cos(a1)).add(bn.scale(Math.sin(a1)));
            float n0 = 0.45F + 0.55F * noise(a0 * 3.2 + 11, age * 6), n1 = 0.45F + 0.55F * noise(a1 * 3.2 + 11, age * 6);
            for (int k = 0; k < 3; k++) {
                Vec3 o0 = c.add(d0.scale(radius[k])).add(b.axis.scale(cup[k])), o1 = c.add(d1.scale(radius[k])).add(b.axis.scale(cup[k]));
                Vec3 i1 = c.add(d1.scale(radius[k + 1])).add(b.axis.scale(cup[k + 1])), i0 = c.add(d0.scale(radius[k + 1])).add(b.axis.scale(cup[k + 1]));
                put(vc, m, o0, 1, 1, 1, alpha * weight[k] * n0 * near(o0, camera));
                put(vc, m, o1, 1, 1, 1, alpha * weight[k] * n1 * near(o1, camera));
                put(vc, m, i1, 0.9F, 0.95F, 1, alpha * weight[k + 1] * n1 * near(i1, camera));
                put(vc, m, i0, 0.9F, 0.95F, 1, alpha * weight[k + 1] * n0 * near(i0, camera));
            }
        }
    }

    /**
     * Shock front and flash of a boom (0.1.8.3, additive). The front is a thin cyan-white ring that
     * runs ahead of the vapour ring (radius {@code 0.8 + 15 x easeOut}, alive for the first 60 % of
     * the life) with a sharp leading edge and a soft trailing glow. The flash is a radial glow disc
     * in the first 15 %; it is skipped for the own boom in first person, where it would sit on the
     * camera.
     */
    private static void shockFront(VertexConsumer vc, Matrix4f m, Boom b, float age, Vec3 camera) {
        Vec3 t = perp(b.axis), bn = b.axis.cross(t).normalize();
        int steps = 72;
        if (age < 0.6F) {
            float f = age / 0.6F;
            float ease = 1 - (1 - f) * (1 - f) * (1 - f);
            float r = 0.8F + 15F * ease, w = 0.12F + 0.55F * f;
            float a = (1 - f) * (1 - f) * 0.85F;
            Vec3 c = b.pos.subtract(b.axis.scale(age * 1.5));
            for (int i = 0; i < steps; i++) {
                double a0 = i * Math.PI * 2 / steps, a1 = (i + 1) * Math.PI * 2 / steps;
                Vec3 d0 = t.scale(Math.cos(a0)).add(bn.scale(Math.sin(a0))), d1 = t.scale(Math.cos(a1)).add(bn.scale(Math.sin(a1)));
                Vec3 o0 = c.add(d0.scale(r)), o1 = c.add(d1.scale(r));
                Vec3 i0 = c.add(d0.scale(r - w)), i1 = c.add(d1.scale(r - w));
                Vec3 e0 = c.add(d0.scale(r + 0.06F)), e1 = c.add(d1.scale(r + 0.06F));
                // trailing glow (inner -> crest)
                put(vc, m, i0, 0.45F, 0.8F, 1, 0);
                put(vc, m, i1, 0.45F, 0.8F, 1, 0);
                put(vc, m, o1, 0.8F, 0.95F, 1, a * near(o1, camera));
                put(vc, m, o0, 0.8F, 0.95F, 1, a * near(o0, camera));
                // sharp leading edge (crest -> just outside)
                put(vc, m, o0, 1, 1, 1, a * near(o0, camera));
                put(vc, m, o1, 1, 1, 1, a * near(o1, camera));
                put(vc, m, e1, 1, 1, 1, 0);
                put(vc, m, e0, 1, 1, 1, 0);
            }
        }
        if (age < 0.15F && camera == null) {
            float f = age / 0.15F;
            float r = 1.2F + 2.5F * f, a = (1 - f) * 0.7F;
            for (int i = 0; i < 24; i++) {
                double a0 = i * Math.PI * 2 / 24, a1 = (i + 1) * Math.PI * 2 / 24;
                Vec3 d0 = t.scale(Math.cos(a0)).add(bn.scale(Math.sin(a0))), d1 = t.scale(Math.cos(a1)).add(bn.scale(Math.sin(a1)));
                put(vc, m, b.pos, 1, 1, 1, a);
                put(vc, m, b.pos.add(d0.scale(r)), 0.6F, 0.85F, 1, 0);
                put(vc, m, b.pos.add(d1.scale(r)), 0.6F, 0.85F, 1, 0);
                put(vc, m, b.pos, 1, 1, 1, a);
            }
        }
    }

    /** Opacity scale of the flight effects for this frame (client option vapor_intensity). */
    private static float alphaScale = 1.0F;

    /** Adds one vertex at a world point. The alpha is scaled by {@link #alphaScale} and clamped to 0..1. */
    private static void put(VertexConsumer vc, Matrix4f m, Vec3 p, float r, float g, float b, float a) {
        vc.addVertex(m, (float) p.x, (float) p.y, (float) p.z).setColor(r, g, b, Math.max(0, Math.min(1, a * alphaScale)));
    }

    // ------------------------------------------------------------------ HUD (aircraft style)

    /**
     * Flight HUD. Call it in the HUD pass, after {@link FcsProjection#capture} for this frame.
     *
     * <p>Parts: boresight, flight path marker, horizon and pitch ladder that roll with the camera
     * (first person only), airspeed tape (km/h) on the left, altitude tape (m) on the right, and
     * Mach, G and throttle read-outs. The colour is the FCS palette colour. Thin strokes, no panels.
     *
     * <p>Units: 1 block = 1 m and 20 ticks = 1 s. Thus km/h = blocks per tick x 20 x 3.6.
     * Mach = speed / {@link ManeuverMode#BOOM_SPEED}. G = 1 + speed change per tick x 20 x 20 / 9.81.
     * G shows only the change of speed. A turn at constant speed shows 1 G.
     *
     * <p>Pitch ladder: one line every 10 degrees, {@code h / 70} GUI pixels per degree. This is
     * near the vanilla mapping at the default vertical FOV of 70 degrees, at the screen centre.
     *
     * @param w       the GUI width in pixels.
     * @param h       the GUI height in pixels.
     * @param primary the HUD colour, 0xRRGGBB.
     * @param partial the partial tick.
     */
    static void renderHud(GuiGraphics g, int w, int h, int primary, float partial) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        double s = Mth.lerp(partial, prevSpeed, speed);
        float thr = Mth.lerp(partial, prevThrottle, throttle);
        int col = FcsDraw.argb(primary, 0.95F), dim = FcsDraw.argb(primary, 0.55F);
        float cx = w / 2F, cy = h / 2F;
        float pitch = player.getViewXRot(partial);
        float pxPerDeg = h / 70F;   // near the vanilla FOV mapping at default settings
        float rollRad = (float) Math.toRadians(-roll);

        boolean cockpit = Minecraft.getInstance().options.getCameraType().isFirstPerson();
        // pitch ladder and horizon, rotated with the camera roll. First person only, as in a cockpit HUD.
        if (cockpit) {
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        g.pose().mulPose(new Quaternionf().rotateZ(rollRad));
        for (int deg = -60; deg <= 60; deg += 10) {
            // the first value is not used. The second line sets the value: y = -(deg + pitch) * pxPerDeg.
            // The horizon (deg 0) moves up when the view pitch goes down (positive pitch).
            float y = (pitch - deg) * pxPerDeg * -1F;
            y = -(deg - (-pitch)) * pxPerDeg;
            if (Math.abs(y) > h * 0.42F) continue;
            float half = deg == 0 ? w * 0.22F : 38;
            int c = deg == 0 ? col : dim;
            if (deg == 0) {
                FcsDraw.line(g, -half, y, -14, y, 1.2F, c);
                FcsDraw.line(g, 14, y, half, y, 1.2F, c);
            } else if (deg > 0) {
                FcsDraw.line(g, -half, y, -14, y, 1F, c);
                FcsDraw.line(g, 14, y, half, y, 1F, c);
                FcsDraw.line(g, -half, y, -half, y + 4, 1F, c);
                FcsDraw.line(g, half, y, half, y + 4, 1F, c);
                FcsDraw.textRight(g, String.valueOf(deg), -half - 3, y - 3, c, 0.55F);
                FcsDraw.text(g, String.valueOf(deg), half + 3, y - 3, c, 0.55F);
            } else {
                for (float x = -half; x < -14; x += 7) FcsDraw.line(g, x, y, Math.min(-14, x + 4), y, 1F, c);
                for (float x = 14; x < half; x += 7) FcsDraw.line(g, x, y, Math.min(half, x + 4), y, 1F, c);
                FcsDraw.line(g, -half, y, -half, y - 4, 1F, c);
                FcsDraw.line(g, half, y, half, y - 4, 1F, c);
                FcsDraw.textRight(g, String.valueOf(deg), -half - 3, y - 3, c, 0.55F);
                FcsDraw.text(g, String.valueOf(deg), half + 3, y - 3, c, 0.55F);
            }
        }
        g.pose().popPose();

        // boresight (gun cross / waterline)
        FcsDraw.line(g, cx - 10, cy, cx - 4, cy, 1.2F, col);
        FcsDraw.line(g, cx + 4, cy, cx + 10, cy, 1.2F, col);
        FcsDraw.line(g, cx - 4, cy, cx - 2, cy + 3, 1.2F, col);
        FcsDraw.line(g, cx + 4, cy, cx + 2, cy + 3, 1.2F, col);

        // flight path marker: the direction of the commanded velocity, projected from a point 40
        // blocks ahead of the eye. It is clamped to 30 GUI pixels from the screen edge.
        Vec3 vel = velocity;
        if (vel.lengthSqr() > 1.0E-3) {
            Vec3 eye = player.getEyePosition(partial);
            float[] fpm = FcsProjection.project(eye.add(vel.normalize().scale(40)));
            if (fpm != null) {
                float x = Mth.clamp(fpm[0], 30, w - 30), y = Mth.clamp(fpm[1], 30, h - 30);
                FcsDraw.circle(g, x, y, 4.5F, 1.2F, col);
                FcsDraw.line(g, x - 12, y, x - 4.5F, y, 1.2F, col);
                FcsDraw.line(g, x + 4.5F, y, x + 12, y, 1.2F, col);
                FcsDraw.line(g, x, y - 4.5F, x, y - 9, 1.2F, col);
            }
        }
        }

        // airspeed tape (km/h)
        double kmh = s * 20 * 3.6;
        float tapeX = cx - Math.min(w * 0.2F, 130), tapeH = h * 0.36F;
        tape(g, tapeX, cy, tapeH, kmh, 50, 10, true, col, dim);
        FcsDraw.text(g, "IAS km/h", tapeX - 30, cy - tapeH / 2 - 10, dim, 0.55F);
        // altitude tape (m)
        float altX = cx + Math.min(w * 0.2F, 130);
        tape(g, altX, cy, tapeH, player.getY(), 10, 2, false, col, dim);
        FcsDraw.text(g, "ALT m", altX + 6, cy - tapeH / 2 - 10, dim, 0.55F);
        double vs = (player.getY() - player.yo) * 20;
        FcsDraw.text(g, String.format(Locale.ROOT, "%+.0f m/s", vs), altX + 6, cy + tapeH / 2 + 4, col, 0.6F);

        // Mach (barrier speed = M1.0), G (from the change of speed), throttle.
        // s is interpolated by partial, but prevSpeed is not. Thus G is 1 at partial 0 and the full value at partial 1.
        double mach = s / ManeuverMode.BOOM_SPEED;
        double gload = 1 + (s - prevSpeed) * 20 / 9.81 * 20;
        float ry = cy + tapeH / 2 + 4;
        FcsDraw.text(g, String.format(Locale.ROOT, "M %.2f", mach), tapeX - 30, ry, mach >= 1 ? FcsDraw.argb(FcsDraw.YELLOW, 1F) : col, 0.6F);
        FcsDraw.text(g, String.format(Locale.ROOT, "G %.1f", gload), tapeX - 30, ry + 8, col, 0.6F);
        // throttle gauge (vertical, left of the speed tape)
        float gx = tapeX - 44, gy = cy - tapeH / 2;
        g.fill((int) gx, (int) gy, (int) gx + 3, (int) (gy + tapeH), FcsDraw.argb(primary, 0.15F));
        int filled = (int) (tapeH * thr);
        int tc = thr >= ManeuverMode.SHOCK_THROTTLE ? FcsDraw.argb(0xFF9F1C, 0.95F) : col;
        g.fill((int) gx, (int) (gy + tapeH - filled), (int) gx + 3, (int) (gy + tapeH), tc);
        float shockY = gy + tapeH * (1 - ManeuverMode.SHOCK_THROTTLE);
        FcsDraw.line(g, gx - 3, shockY, gx + 6, shockY, 1F, FcsDraw.argb(FcsDraw.RED, 0.9F));
        FcsDraw.textRight(g, String.format(Locale.ROOT, "THR %d%%", Math.round(thr * 100)), gx - 3, gy + tapeH + 4, tc, 0.55F);
        if (locked) FcsDraw.textRight(g, "LOCK", gx - 3, gy - 9, FcsDraw.argb(FcsDraw.YELLOW, 1F), 0.6F);
        String mode = thr <= 0 ? "HOVER" : thr >= ManeuverMode.SHOCK_THROTTLE ? "SUPERSONIC" : "CRUISE";
        FcsDraw.textCentered(g, "MNVR  " + mode, cx, cy + tapeH / 2 + 12, thr >= ManeuverMode.SHOCK_THROTTLE ? FcsDraw.argb(0xFF9F1C, 1F) : col, 0.65F);
        // power: cost per tick at the measured speed (the server bills the same formula), and the charge in percent
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        long e = SuitModules.energy(chest), max = chest.getItem() instanceof AbstractElectricArmor a ? a.getMaxEnergy(chest) : 1;
        FcsDraw.textCentered(g, String.format(Locale.ROOT, "%.0f EU/t   %.0f%%", ManeuverMode.costPerTick(s), 100.0 * e / Math.max(1, max)),
            cx, cy + tapeH / 2 + 21, dim, 0.55F);
    }

    /**
     * A moving scale with a boxed read-out at its middle. The visible range is 5 major steps. Minor
     * ticks are every {@code minor} units, major ticks with labels every {@code major} units. Higher
     * values are higher on the screen.
     *
     * @param x        the x of the scale line, in GUI pixels.
     * @param cy       the y of the scale middle, in GUI pixels.
     * @param height   the scale height in GUI pixels.
     * @param value    the value at the middle.
     * @param leftSide true to draw ticks and labels to the left of the line.
     */
    private static void tape(GuiGraphics g, float x, float cy, float height, double value, double major, double minor, boolean leftSide,
                             int col, int dim) {
        float pxPerUnit = height / (float) (major * 5);
        double lo = value - height / 2 / pxPerUnit, hi = value + height / 2 / pxPerUnit;
        FcsDraw.line(g, x, cy - height / 2, x, cy + height / 2, 1F, dim);
        for (double v = Math.ceil(lo / minor) * minor; v <= hi; v += minor) {
            float y = cy - (float) ((v - value) * pxPerUnit);
            boolean isMajor = Math.abs(v / major - Math.rint(v / major)) < 1.0E-6;
            float len = isMajor ? 6 : 3;
            FcsDraw.line(g, x, y, leftSide ? x - len : x + len, y, 1F, isMajor ? col : dim);
            if (isMajor && Math.abs(y - cy) > 7) {
                String t = String.format(Locale.ROOT, "%.0f", v);
                if (leftSide) FcsDraw.textRight(g, t, x - 8, y - 3, dim, 0.55F); else FcsDraw.text(g, t, x + 8, y - 3, dim, 0.55F);
            }
        }
        String box = String.format(Locale.ROOT, "%.0f", value);
        float bw = FcsDraw.width(box, 0.75F) + 6;
        float bx = leftSide ? x - 8 - bw : x + 8;
        g.fill((int) bx, (int) cy - 5, (int) (bx + bw), (int) cy + 6, 0x90000000);
        FcsDraw.line(g, bx, cy - 5, bx + bw, cy - 5, 1F, col);
        FcsDraw.line(g, bx, cy + 6, bx + bw, cy + 6, 1F, col);
        FcsDraw.line(g, bx, cy - 5, bx, cy + 6, 1F, col);
        FcsDraw.line(g, bx + bw, cy - 5, bx + bw, cy + 6, 1F, col);
        FcsDraw.text(g, box, bx + 3, cy - 3, col, 0.75F);
        FcsDraw.triangle(g, leftSide ? x - 1 : x + 1, cy, leftSide ? x - 7 : x + 7, cy - 4, leftSide ? x - 7 : x + 7, cy + 4, col);
    }

    /** 0.1.9.0: vectoring multiplier of the worn chestplate (the advanced propulsion core sharpens the vectors). */
    static double propulsionVectoring(net.minecraft.world.entity.player.Player player) {
        var chest = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
        return com.miophas.singularity_iteration.common.suit.core.FrameCores.core(chest) == com.miophas.singularity_iteration.common.suit.core.FrameCore.PROPULSION
            ? com.miophas.singularity_iteration.common.suit.core.FrameCores.PROPULSION_VECTORING : 1.0;
    }
}
