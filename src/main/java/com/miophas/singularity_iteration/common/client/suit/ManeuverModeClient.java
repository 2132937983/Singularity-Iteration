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
 * Special Maneuver Mode, client half (0.1.7.34). Original SI implementation. Everything here is
 * armed by the one console switch; with the switch on and flight active, all of it applies:
 *
 * <ul>
 *   <li><b>Throttle flight model.</b> The sprint / boost key opens a throttle (0..1); release closes
 *       it twice as fast; a tap on sneak locks it. Cruise speed is throttle x max speed. Each tick the
 *       mode rewrites the player's movement vector towards the view axis with a speed-dependent grip,
 *       so momentum carries through turns and the flyer drifts wide at speed. At throttle 0 the
 *       mode rewrites the hover movement too: a soft-start, soft-stop drift instead of creative
 *       flight's instant stop.</li>
 *   <li><b>Take-off.</b> A double jump from the ground launches with a ground shock (dust ring,
 *       thump).</li>
 *   <li><b>Pose.</b> Hover: the body leans slightly forward with a slow heave, arms held low and back,
 *       fists out (the mixin {@code PlayerManeuverPoseMixin} sets the limbs). Cruise: the body lines up
 *       with the flight path and banks into turns; at full throttle one arm leads.</li>
 *   <li><b>Effects.</b> Speed FOV, banking camera, wake trails from hands and feet, a vapour cone
 *       around the shock barrier, a shock ring and boom when the throttle crosses
 *       {@link ManeuverMode#SHOCK_THROTTLE}, a wind loop, and the aircraft-style flight HUD.</li>
 * </ul>
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
@SuppressWarnings("null")
public final class ManeuverModeClient {
    private ManeuverModeClient() {}

    private static final double HOVER_ACCEL = 0.075, HOVER_DRAG = 0.86, HOVER_MAX = 0.62;
    private static final double LATERAL = 0.035;

    // ---- local flyer
    private static boolean flying;
    private static Vec3 velocity = Vec3.ZERO;
    private static float throttle, prevThrottle;
    private static boolean locked, sneakWasDown;
    private static float stickForward, stickSideways;
    private static int takeoffTicks, groundTicks;
    private static double speed, prevSpeed;
    private static float roll;
    private static float lastYaw;
    private static float fovBlend;
    private static int sendTimer;
    private static SuitPackets.ManeuverInput lastSent;
    private static WindSound wind;

    /** Shock rings in the world. */
    private record Boom(Vec3 pos, Vec3 axis, long start) { }
    private static final List<Boom> BOOMS = new ArrayList<>();

    /** Per-flyer visual state (local and remote players). */
    static final class Visual {
        float pitch, roll, turnSpeed, lastYaw, heave, transition, lean;
        boolean wasShock;
        final ArrayDeque<Vec3[]> trail = new ArrayDeque<>();
        long lastTick;
    }
    static final Int2ObjectOpenHashMap<Visual> VISUALS = new Int2ObjectOpenHashMap<>();

    public static boolean isFlying() { return flying; }
    public static double speed() { return speed; }
    public static float throttle() { return throttle; }
    public static boolean locked() { return locked; }
    public static Vec3 velocity() { return velocity; }

    static boolean armedChest(Player player) {
        return ManeuverMode.isArmed(player);
    }

    private static boolean active(LocalPlayer player) {
        return player.getAbilities().flying && !player.isSpectator() && !player.isPassenger() && armedChest(player)
            && (player.isCreative() || SuitModules.energy(player.getItemBySlot(EquipmentSlot.CHEST)) >= ManeuverMode.MIN_ENERGY);
    }

    /** Throttle of any player: local value, or the relayed one for others (0 when not flying). */
    public static float throttleOf(Player p, float partial) {
        if (p == Minecraft.getInstance().player) return flying ? Mth.lerp(partial, prevThrottle, throttle) : 0;
        SuitPackets.ManeuverState s = SuitSensorData.maneuver(p.getId());
        return s == null ? 0 : s.throttle();
    }

    /** Is this player (local or remote) in maneuver flight right now? */
    public static boolean inManeuver(Player p) {
        if (p == Minecraft.getInstance().player) return flying;
        return armedChest(p) && !p.onGround() && SuitSensorData.maneuver(p.getId()) != null;
    }

    static float[] stickOf(Player p) {
        if (p == Minecraft.getInstance().player) return new float[]{stickForward, stickSideways};
        SuitPackets.ManeuverState s = SuitSensorData.maneuver(p.getId());
        return s == null ? new float[]{0, 0} : new float[]{s.forward(), s.sideways()};
    }

    // ------------------------------------------------------------------ movement

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
        if (player.onGround()) groundTicks = 4; else if (groundTicks > 0) groundTicks--;
        if (!flying) return;

        Input input = event.getInput();
        float forward = input.forwardImpulse, strafe = input.leftImpulse;
        boolean up = input.jumping, down = input.shiftKeyDown;
        stickForward = Math.signum(forward);
        stickSideways = Math.signum(strafe);
        boolean accelerate = (mio_icif_ClientEvents.BOOST_KEY != null && mio_icif_ClientEvents.BOOST_KEY.isDown())
            || Minecraft.getInstance().options.keySprint.isDown();

        // sneak tap while cruising locks / unlocks the throttle
        if (down && !sneakWasDown && throttle > 0.2F) locked = !locked;
        sneakWasDown = down;

        prevThrottle = throttle;
        if (player.horizontalCollision && throttle > 0) {
            throttle = 0;
            locked = false;
        } else if (!locked) {
            throttle = accelerate ? Math.min(1F, throttle + ManeuverMode.THROTTLE_UP) : Math.max(0F, throttle - ManeuverMode.THROTTLE_DOWN);
        }

        Vec3 look = player.getViewVector(1.0F);
        float yaw = (float) Math.toRadians(player.getYRot());
        Vec3 fwdFlat = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
        Vec3 right = new Vec3(-Mth.cos(yaw), 0, -Mth.sin(yaw));
        Vec3 v = velocity;
        if (takeoffTicks > 0) {
            takeoffTicks--;
            v = new Vec3(v.x * 0.6, 1.25, v.z * 0.6);
        } else if (throttle > 0) {
            // cruise: rewrite the movement vector towards the view axis; grip falls with speed (drift)
            Vec3 target = look.scale(throttle * ManeuverMode.MAX_SPEED);
            double grip = ManeuverMode.grip(throttle);
            v = v.add(target.subtract(v).scale(grip));
            if (strafe != 0) v = v.add(right.scale(-strafe * LATERAL));
        } else {
            // hover: soft-start, soft-stop drift on the stick
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

        // hand the motion to vanilla travel unchanged: no horizontal input, and cancel the
        // vertical step aiStep adds for jump / sneak while flying
        input.forwardImpulse = 0;
        input.leftImpulse = 0;
        input.up = input.down = input.left = input.right = false;
        double fs = player.getAbilities().getFlyingSpeed() * 3.0;
        double vy = v.y - (up ? fs : 0) + (down ? fs : 0);
        player.setDeltaMovement(v.x, vy, v.z);
        player.hasImpulse = true;
        sendState(false);
    }

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
                if (p.verticalCollision && takeoffTicks == 0) velocity = new Vec3(velocity.x, 0, velocity.z);
                prevSpeed = speed;
                speed = p.position().subtract(p.xo, p.yo, p.zo).length();
                if (p.onGround() && throttle == 0) flying = false;
                startWind((LocalPlayer) p);
            }
        }
        if (!inManeuver(p)) {
            Visual vis = VISUALS.get(p.getId());
            if (vis != null) vis.trail.clear();
            return;
        }
        Visual vis = VISUALS.computeIfAbsent(p.getId(), k -> new Visual());
        float thr = throttleOf(p, 1.0F);
        boolean shock = thr >= ManeuverMode.SHOCK_THROTTLE;
        if (shock && !vis.wasShock) boom(p, p.position().subtract(p.xo, p.yo, p.zo), local);
        vis.wasShock = shock;
        recordTrail(p, vis, thr);
        if (thr > 0.55F) vapour(p, thr);
    }

    // ------------------------------------------------------------------ effects

    private static void boom(Player player, Vec3 dir, boolean local) {
        Vec3 axis = dir.lengthSqr() > 1.0E-4 ? dir.normalize() : player.getViewVector(1.0F);
        BOOMS.add(new Boom(player.position().add(0, player.getBbHeight() * 0.5, 0), axis, System.currentTimeMillis()));
        if (BOOMS.size() > 8) BOOMS.remove(0);
        player.level().playLocalSound(player.getX(), player.getY(), player.getZ(), mio_icif_sounds.MANEUVER_BOOM.get(), SoundSource.PLAYERS,
            local ? 1.0F : 2.0F, 0.95F + player.level().random.nextFloat() * 0.1F, false);
    }

    /** Condensation puffs that shed off the cone edge while above the barrier. */
    private static void vapour(Player p, float thr) {
        var level = p.level();
        Vec3 v = p.position().subtract(p.xo, p.yo, p.zo);
        if (v.lengthSqr() < 0.25) return;
        Vec3 axis = v.normalize();
        Vec3 t = perp(axis), b = axis.cross(t).normalize();
        if (level.random.nextFloat() > (thr > 0.8F ? 0.6F : 0.3F)) return;
        for (int i = 0; i < 1; i++) {
            double a = level.random.nextDouble() * Math.PI * 2;
            double r = 0.9 + level.random.nextDouble() * 0.4;
            Vec3 at = p.position().add(0, p.getBbHeight() * 0.55, 0).subtract(axis.scale(0.8)).add(t.scale(Math.cos(a) * r)).add(b.scale(Math.sin(a) * r));
            level.addParticle(ParticleTypes.POOF, at.x, at.y, at.z, v.x * 0.1, v.y * 0.1, v.z * 0.1);
        }
    }

    /** Emitters: both hands and both feet, in world space, from the pose of this tick. */
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
        // a flyer lies along the path: hands ahead/out, feet behind
        Vec3[] pts = {
            centre.add(side.scale(0.42)).add(axis.scale(0.15)),
            centre.add(side.scale(-0.42)).add(axis.scale(0.15)),
            centre.add(side.scale(0.13)).subtract(axis.scale(0.95)).add(up.scale(-0.05)),
            centre.add(side.scale(-0.13)).subtract(axis.scale(0.95)).add(up.scale(-0.05))};
        vis.trail.addLast(pts);
        while (vis.trail.size() > 28) vis.trail.removeFirst();
    }

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

    @SubscribeEvent
    public static void onFov(ComputeFovModifierEvent event) {
        float target = flying && FcsClientConfig.speedFov() ? (float) Math.min(0.30, speed * 0.075) : 0;
        fovBlend += (target - fovBlend) * 0.12F;
        if (fovBlend > 0.001F) event.setNewFovModifier(event.getNewFovModifier() * (1 + fovBlend));
    }

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

    /** Whole-body transform: hover lean + heave, cruise alignment + bank. */
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

        // hover: a slight, menacing forward lean that deepens with the stick; strafing banks a little
        float leanTarget = 7.0F + 9.0F * stick[0];
        vis.lean += (leanTarget - vis.lean) * Math.min(1, dt * 5);
        float hoverPitch = vis.lean, hoverRoll = -stick[1] * 10.0F;
        // cruise: body along the flight path, banked into the turn
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
        // heave: a slow rise and settle (eased sine), only while hovering
        float h = (float) Math.sin(vis.heave * 1.7) ;
        float heave = (h * h * h * 0.35F + h * 0.65F) * 0.055F * (1 - cruise) * vis.transition;
        pose.translate(0, heave, 0);
        pose.translate(0, mid, 0);
        float bodyYaw = (float) Math.toRadians(-Mth.lerp(partial, p.yBodyRotO, p.yBodyRot));
        // rotate in body space: yaw to body, pitch forward around body X, roll around body Z
        pose.mulPose(new Quaternionf().rotateY(bodyYaw).rotateX((float) Math.toRadians(vis.pitch)).rotateZ((float) Math.toRadians(vis.roll)).rotateY(-bodyYaw));
        pose.translate(0, -mid, 0);
        POSED.add(p.getId());
    }

    private static final it.unimi.dsi.fastutil.ints.IntOpenHashSet POSED = new it.unimi.dsi.fastutil.ints.IntOpenHashSet();

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        if (POSED.remove(event.getEntity().getId())) event.getPoseStack().popPose();
    }

    /**
     * Limb pose (called by PlayerManeuverPoseMixin after vanilla setupAnim). Angles in radians.
     * Hover: arms low and slightly back, fists out; legs together, toes down. Cruise at full
     * throttle: the right arm leads, the left arm lies along the body.
     */
    public static void applyLimbPose(net.minecraft.client.model.HumanoidModel<?> model, Player p) {
        Visual vis = VISUALS.get(p.getId());
        if (vis == null || vis.transition <= 0.001F) return;
        float t = vis.transition;
        float thr = throttleOf(p, 1.0F);
        float cruise = Mth.clamp(thr / 0.35F, 0, 1);
        float lead = Mth.clamp((thr - 0.75F) / 0.2F, 0, 1);
        float breath = (float) Math.sin(vis.heave * 1.7) * 0.03F;
        // hover stance
        float armX = 0.16F + breath, armZ = 0.20F;
        float legX = 0.10F, legZ = 0.035F;
        // cruise: arms swept back along the body
        armX = Mth.lerp(cruise, armX, 0.25F);
        armZ = Mth.lerp(cruise, armZ, 0.10F);
        legX = Mth.lerp(cruise, legX, 0.05F);
        mix(model.rightArm, armX, 0.04F, armZ, t);
        mix(model.leftArm, armX, -0.04F, -armZ, t);
        if (lead > 0) mix(model.rightArm, -2.95F, 0.0F, 0.12F, lead * t);
        mix(model.rightLeg, legX, 0.03F, legZ, t);
        mix(model.leftLeg, legX + 0.08F, -0.03F, -legZ, t);
        model.head.xRot = Mth.lerp(cruise * t, model.head.xRot, -1.25F);
        model.hat.copyFrom(model.head);
    }

    private static void mix(net.minecraft.client.model.geom.ModelPart part, float x, float y, float z, float w) {
        part.xRot = Mth.lerp(w, part.xRot, x);
        part.yRot = Mth.lerp(w, part.yRot, y);
        part.zRot = Mth.lerp(w, part.zRot, z);
    }

    // ------------------------------------------------------------------ world pass

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

        VertexConsumer vc = buffers.getBuffer(SiRenderTypes.VAPOR);
        for (Player p : mc.level.players()) {
            Visual vis = VISUALS.get(p.getId());
            if (vis == null) continue;
            if (!vis.trail.isEmpty()) wake(vc, m, vis, cam);
        }
        buffers.endBatch(SiRenderTypes.VAPOR);
        // the cone is a glow: additive, so overlapping shells brighten softly instead of turning opaque
        VertexConsumer glowCone = buffers.getBuffer(SiRenderTypes.FIELD_GLOW);
        for (Player p : mc.level.players()) {
            if (!VISUALS.containsKey(p.getId())) continue;
            float thr = throttleOf(p, partial);
            if (thr > 0.5F && !(p == mc.player && firstPerson)) cone(glowCone, m, p, thr, partial, now);
        }
        buffers.endBatch(SiRenderTypes.FIELD_GLOW);

        if (!BOOMS.isEmpty()) {
            VertexConsumer glow = buffers.getBuffer(SiRenderTypes.VAPOR);
            for (Iterator<Boom> it = BOOMS.iterator(); it.hasNext(); ) {
                Boom b = it.next();
                float age = (now - b.start) / 900F;
                if (age >= 1) { it.remove(); continue; }
                shockRing(glow, m, b, age);
            }
            buffers.endBatch(SiRenderTypes.VAPOR);
        }
        pose.popPose();
    }

    /** Wake trails: camera-facing ribbons that widen and fade with age. */
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
                // fade out next to the camera, where the ribbon would cover the view
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
     * Vapour cone: a translucent bell that opens backwards from the shoulders; strongest right at
     * the barrier, thinning above it; the rim is broken up by an animated noise so it reads as
     * condensation, not a solid shell.
     */
    private static void cone(VertexConsumer vc, Matrix4f m, Player p, float thr, float partial, long now) {
        Vec3 v = p.getPosition(partial).subtract(p.xo, p.yo, p.zo);
        Vec3 axis = v.lengthSqr() > 0.04 ? v.normalize() : p.getViewVector(partial);
        Vec3 t = perp(axis), b = axis.cross(t).normalize();
        Vec3 apex = p.getPosition(partial).add(0, p.getBbHeight() * 0.55, 0).add(axis.scale(0.45));
        float peak = 1 - Math.abs(thr - ManeuverMode.SHOCK_THROTTLE - 0.08F) / 0.45F;
        float strength = Mth.clamp(peak, 0.15F, 1F) * Mth.clamp((thr - 0.5F) / 0.1F, 0, 1);
        int seg = 32, rings = 6;
        // wrap the clock: noise() floors its input to int, and epoch seconds x 2 overflow it
        double time = (now % 600_000L) / 1000.0;
        for (int r = 0; r < rings; r++) {
            double z0 = r * 0.32, z1 = (r + 1) * 0.32;
            double rad0 = 0.15 + Math.pow(z0 / 1.9, 0.6) * 1.05, rad1 = 0.15 + Math.pow(z1 / 1.9, 0.6) * 1.05;
            float fade0 = (float) Math.sin(Math.PI * (r / (double) rings)), fade1 = (float) Math.sin(Math.PI * ((r + 1) / (double) rings));
            for (int s = 0; s < seg; s++) {
                double a0 = s * Math.PI * 2 / seg, a1 = (s + 1) * Math.PI * 2 / seg;
                float n0 = soft(a0, z0, time), n1 = soft(a1, z0, time);
                float n2 = soft(a1, z1, time), n3 = soft(a0, z1, time);
                Vec3 d0 = t.scale(Math.cos(a0)).add(b.scale(Math.sin(a0))), d1 = t.scale(Math.cos(a1)).add(b.scale(Math.sin(a1)));
                Vec3 p0 = apex.subtract(axis.scale(z0)).add(d0.scale(rad0)), p1 = apex.subtract(axis.scale(z0)).add(d1.scale(rad0));
                Vec3 p2 = apex.subtract(axis.scale(z1)).add(d1.scale(rad1)), p3 = apex.subtract(axis.scale(z1)).add(d0.scale(rad1));
                float base = 0.22F * strength;
                put(vc, m, p0, 1, 1, 1, base * fade0 * n0);
                put(vc, m, p1, 1, 1, 1, base * fade0 * n1);
                put(vc, m, p2, 1, 1, 1, base * fade1 * n2);
                put(vc, m, p3, 1, 1, 1, base * fade1 * n3);
            }
        }
    }

    /** Cone density at an angle / distance: smooth, slowly streaming, never fully empty or full. */
    private static float soft(double angle, double z, double time) {
        // wrap the angle seamlessly: sample the noise on a circle
        double cx = Math.cos(angle) * 1.6, cy = Math.sin(angle) * 1.6;
        float n = noise(cx + z * 1.3 - time * 2.2, cy + 7.1) * 0.6F + noise(cy * 2 + 3.3, cx * 2 - time * 3.1 + z) * 0.4F;
        return 0.35F + 0.65F * n;
    }

    /** Smooth value noise in 0..1 (cheap, deterministic). */
    private static float noise(double x, double y) {
        int xi = Mth.floor(x), yi = Mth.floor(y);
        double fx = x - xi, fy = y - yi;
        double u = fx * fx * (3 - 2 * fx), w = fy * fy * (3 - 2 * fy);
        double a = hash(xi, yi), b = hash(xi + 1, yi), c = hash(xi, yi + 1), d = hash(xi + 1, yi + 1);
        return (float) Mth.clamp(a + (b - a) * u + (c - a) * w + (a - b - c + d) * u * w, 0, 1);
    }

    private static double hash(int x, int y) {
        int h = x * 374761393 + y * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        return ((h ^ (h >>> 16)) & 0xFFFF) / 65535.0;
    }

    /** Shock ring: a thin condensation disc that expands across the path and dissolves. */
    private static void shockRing(VertexConsumer vc, Matrix4f m, Boom b, float age) {
        Vec3 t = perp(b.axis), bn = b.axis.cross(t).normalize();
        Vec3 c = b.pos.subtract(b.axis.scale(age * 3.0));
        float r0 = 0.6F + age * 9F, r1 = r0 * 0.78F;
        float alpha = (1 - age) * (1 - age) * 0.55F;
        int steps = 64;
        for (int i = 0; i < steps; i++) {
            double a0 = i * Math.PI * 2 / steps, a1 = (i + 1) * Math.PI * 2 / steps;
            Vec3 d0 = t.scale(Math.cos(a0)).add(bn.scale(Math.sin(a0))), d1 = t.scale(Math.cos(a1)).add(bn.scale(Math.sin(a1)));
            float n = 0.55F + 0.45F * noise(a0 * 4, age * 8);
            put(vc, m, c.add(d0.scale(r0)), 1, 1, 1, 0);
            put(vc, m, c.add(d1.scale(r0)), 1, 1, 1, 0);
            put(vc, m, c.add(d1.scale(r1)).add(b.axis.scale(0.4)), 1, 1, 1, alpha * n);
            put(vc, m, c.add(d0.scale(r1)).add(b.axis.scale(0.4)), 1, 1, 1, alpha * n);
        }
    }

    private static void put(VertexConsumer vc, Matrix4f m, Vec3 p, float r, float g, float b, float a) {
        vc.addVertex(m, (float) p.x, (float) p.y, (float) p.z).setColor(r, g, b, Math.max(0, Math.min(1, a)));
    }

    // ------------------------------------------------------------------ HUD (aircraft style)

    /**
     * Flight HUD: boresight, flight path marker, horizon and pitch ladder that roll with the
     * camera, airspeed tape (km/h) on the left, altitude tape on the right, Mach / G / throttle
     * read-outs. Green phosphor (the FCS palette colour), thin strokes, no panels.
     */
    static void renderHud(GuiGraphics g, int w, int h, int primary, float partial) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        double s = Mth.lerp(partial, prevSpeed, speed);
        float thr = Mth.lerp(partial, prevThrottle, throttle);
        int col = FcsDraw.argb(primary, 0.95F), dim = FcsDraw.argb(primary, 0.55F);
        float cx = w / 2F, cy = h / 2F;
        float pitch = player.getViewXRot(partial);
        float pxPerDeg = h / 70F;   // close to the vanilla FOV mapping at default settings
        float rollRad = (float) Math.toRadians(-roll);

        boolean cockpit = Minecraft.getInstance().options.getCameraType().isFirstPerson();
        // pitch ladder and horizon (rotated with the camera roll); first person only, like a cockpit HUD
        if (cockpit) {
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        g.pose().mulPose(new Quaternionf().rotateZ(rollRad));
        for (int deg = -60; deg <= 60; deg += 10) {
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

        // flight path marker: where the suit is actually going
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

        // Mach (barrier speed = M1.0), G (from the change of speed), throttle
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
        // power
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        long e = SuitModules.energy(chest), max = chest.getItem() instanceof AbstractElectricArmor a ? a.getMaxEnergy(chest) : 1;
        FcsDraw.textCentered(g, String.format(Locale.ROOT, "%.0f EU/t   %.0f%%", ManeuverMode.costPerTick(s), 100.0 * e / Math.max(1, max)),
            cx, cy + tapeH / 2 + 21, dim, 0.55F);
    }

    /** A moving scale with a boxed read-out at its middle. */
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
}
