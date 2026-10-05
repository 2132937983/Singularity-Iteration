// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.client.mio_icif_ClientEvents;
import com.miophas.singularity_iteration.common.client.render.SiRenderTypes;
import com.miophas.singularity_iteration.common.suit.SuitModules;
import com.miophas.singularity_iteration.common.suit.ViltrumFlight;
import com.miophas.singularity_iteration.core.prefab.item.AbstractElectricArmor;
import com.miophas.singularity_iteration.core.prefab.item.ArmorFeatures;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
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
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * Viltrum flight, client half. Original implementation for SI:
 * <ul>
 *   <li>Inertial model: thrust along the full 3D view axis, separate cruise and boost
 *       envelopes, velocity that turns towards the view axis instead of snapping to it, and a
 *       long glide when the stick is released.</li>
 *   <li>The vanilla creative-flight input is neutralised in {@link MovementInputUpdateEvent},
 *       so the integrator here owns the motion completely (no double acceleration).</li>
 *   <li>Feedback: speed FOV, banked camera, horizontal body pose along the flight path, vapour
 *       trail, sonic boom ring and sound at {@link ViltrumFlight#BOOM_SPEED}, speed streaks and
 *       an air-data panel on the HUD.</li>
 * </ul>
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
@SuppressWarnings("null")
public final class ViltrumFlightClient {
    private ViltrumFlightClient() {}

    private static final double CRUISE_ACCEL = 0.075, BOOST_ACCEL = 0.14;
    private static final double TURN_RATE = 0.24, BOOST_TURN_RATE = 0.13;
    private static final double GLIDE_DRAG = 0.93, LATERAL = 0.05, VERTICAL = 0.06;

    private static boolean flying;
    private static Vec3 velocity = Vec3.ZERO;
    private static double speed, prevSpeed;
    private static boolean boosting;
    private static float roll, prevRoll;
    private static float lastYaw;
    private static float fovBlend;

    /** Shock rings (sonic boom) in the world. */
    private record Boom(Vec3 pos, Vec3 axis, long start) { }
    private static final List<Boom> BOOMS = new ArrayList<>();
    private static final Int2DoubleOpenHashMap REMOTE_SPEED = new Int2DoubleOpenHashMap();

    public static boolean isFlying() { return flying; }
    public static double speed() { return speed; }

    static boolean viltrumChest(Player player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        return ViltrumFlight.isFlightChest(chest) && ArmorFeatures.isEnabled(chest, ViltrumFlight.FEATURE);
    }

    private static boolean active(LocalPlayer player) {
        return player.getAbilities().flying && !player.isSpectator() && !player.isPassenger()
            && viltrumChest(player) && (player.isCreative() || SuitModules.energy(player.getItemBySlot(EquipmentSlot.CHEST)) >= ViltrumFlight.MIN_ENERGY);
    }

    // ------------------------------------------------------------------ movement

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onInput(MovementInputUpdateEvent event) {
        if (!(event.getEntity() instanceof LocalPlayer player)) return;
        boolean now = active(player);
        if (now && !flying) velocity = player.getDeltaMovement();
        flying = now;
        if (!flying) return;
        Input input = event.getInput();
        float forward = input.forwardImpulse, strafe = input.leftImpulse;
        boolean up = input.jumping, down = input.shiftKeyDown;
        boosting = forward > 0 && (mio_icif_ClientEvents.BOOST_KEY != null && mio_icif_ClientEvents.BOOST_KEY.isDown()
            || Minecraft.getInstance().options.keySprint.isDown());

        Vec3 look = player.getViewVector(1.0F);
        float yaw = (float) Math.toRadians(player.getYRot());
        Vec3 right = new Vec3(-Mth.cos(yaw), 0, -Mth.sin(yaw));
        double max = boosting ? ViltrumFlight.BOOST_SPEED : ViltrumFlight.CRUISE_SPEED;

        Vec3 v = velocity;
        if (forward > 0) {
            // thrust: steer the momentum vector towards the view axis, then build speed along it
            double cur = v.length();
            Vec3 dir = cur > 1.0E-3 ? v.scale(1 / cur) : look;
            double k = (boosting ? BOOST_TURN_RATE : TURN_RATE) * (cur > ViltrumFlight.CRUISE_SPEED ? 0.7 : 1.0);
            dir = dir.add(look.subtract(dir).scale(k)).normalize();
            double align = Math.max(0.2, dir.dot(look));
            double spd = cur > max ? Math.max(max, cur * 0.97) : Math.min(max, cur + (boosting ? BOOST_ACCEL : CRUISE_ACCEL) * align);
            v = dir.scale(spd);
        } else if (forward < 0) {
            v = v.scale(0.82);   // air brake
        } else {
            v = v.scale(GLIDE_DRAG);
        }
        if (strafe != 0) v = v.add(right.scale(-strafe * LATERAL));
        if (up) v = v.add(0, VERTICAL, 0);
        if (down) v = v.add(0, -VERTICAL, 0);
        if (forward <= 0 && !up && !down && v.lengthSqr() < 0.0009) v = Vec3.ZERO;
        double len = v.length();
        if (len > max) v = v.scale(Math.max(max, len * 0.97) / len);
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
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player p = event.getEntity();
        if (!p.level().isClientSide) return;
        if (p instanceof LocalPlayer player) {
            if (!flying) {
                prevSpeed = speed = 0;
                return;
            }
            if (player.horizontalCollision) velocity = new Vec3(velocity.x * 0.15, velocity.y, velocity.z * 0.15);
            if (player.verticalCollision) velocity = new Vec3(velocity.x, 0, velocity.z);
            prevSpeed = speed;
            speed = player.position().subtract(player.xo, player.yo, player.zo).length();
            if (prevSpeed < ViltrumFlight.BOOM_SPEED && speed >= ViltrumFlight.BOOM_SPEED) boom(player, velocity, true);
            trail(player, speed);
            if (player.onGround()) flying = false;
        } else if (viltrumChest(p) && !p.onGround()) {
            double s = p.position().subtract(p.xo, p.yo, p.zo).length();
            double before = REMOTE_SPEED.put(p.getId(), s);
            if (before < ViltrumFlight.BOOM_SPEED && s >= ViltrumFlight.BOOM_SPEED) boom(p, p.position().subtract(p.xo, p.yo, p.zo), false);
            if (s > 0.6) trail(p, s);
        } else {
            REMOTE_SPEED.remove(p.getId());
        }
    }

    private static void boom(Player player, Vec3 dir, boolean local) {
        Vec3 axis = dir.lengthSqr() > 1.0E-4 ? dir.normalize() : player.getViewVector(1.0F);
        BOOMS.add(new Boom(player.position().add(0, player.getBbHeight() * 0.5, 0), axis, System.currentTimeMillis()));
        if (BOOMS.size() > 8) BOOMS.remove(0);
        var level = player.level();
        level.playLocalSound(player.getX(), player.getY(), player.getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS,
            local ? 0.55F : 1.2F, 1.35F, false);
        level.playLocalSound(player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS,
            local ? 0.25F : 0.6F, 1.8F, false);
        for (int i = 0; i < 24; i++) {
            double a = i * Math.PI * 2 / 24;
            Vec3 t = Math.abs(axis.y) < 0.95 ? axis.cross(new Vec3(0, 1, 0)).normalize() : axis.cross(new Vec3(1, 0, 0)).normalize();
            Vec3 b = axis.cross(t).normalize();
            Vec3 d = t.scale(Math.cos(a)).add(b.scale(Math.sin(a)));
            level.addParticle(ParticleTypes.CLOUD, player.getX() + d.x * 0.6, player.getY() + 1 + d.y * 0.6, player.getZ() + d.z * 0.6,
                d.x * 0.35, d.y * 0.35, d.z * 0.35);
        }
    }

    /** Vapour trail from the shoulders and feet; denser with speed. */
    private static void trail(Player player, double s) {
        if (s < 0.5) return;
        var level = player.level();
        int n = s > ViltrumFlight.BOOM_SPEED ? 3 : s > 1.4 ? 2 : 1;
        Vec3 back = player.position().subtract(player.xo, player.yo, player.zo).normalize().scale(-0.6);
        for (int i = 0; i < n; i++) {
            double ox = (level.random.nextDouble() - 0.5) * 0.5, oz = (level.random.nextDouble() - 0.5) * 0.5;
            level.addParticle(s > ViltrumFlight.BOOM_SPEED ? ParticleTypes.CLOUD : ParticleTypes.WHITE_ASH,
                player.getX() + back.x + ox, player.getY() + 0.9 + back.y, player.getZ() + back.z + oz, back.x * 0.05, back.y * 0.05, back.z * 0.05);
        }
    }

    // ------------------------------------------------------------------ camera

    @SubscribeEvent
    public static void onFov(ComputeFovModifierEvent event) {
        float target = flying && FcsClientConfig.speedFov() ? (float) Math.min(0.32, speed * 0.1) : 0;
        fovBlend += (target - fovBlend) * 0.15F;
        if (fovBlend > 0.001F) event.setNewFovModifier(event.getNewFovModifier() * (1 + fovBlend));
    }

    @SubscribeEvent
    public static void onCamera(ViewportEvent.ComputeCameraAngles event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        float target = 0;
        if (flying && FcsClientConfig.cameraRoll()) {
            float yawRate = Mth.wrapDegrees(player.getYRot() - lastYaw);
            target = Mth.clamp(yawRate * 1.6F, -24, 24) * (float) Math.min(1, speed / ViltrumFlight.CRUISE_SPEED);
        }
        lastYaw = player.getYRot();
        prevRoll = roll;
        roll += (target - roll) * 0.08F;
        if (Math.abs(roll) > 0.05F) event.setRoll(event.getRoll() + roll);
    }

    // ------------------------------------------------------------------ body pose

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player p = event.getEntity();
        Vec3 v = p.position().subtract(p.xo, p.yo, p.zo);
        double s = v.length();
        boolean local = p == Minecraft.getInstance().player;
        if (local ? !flying : !(viltrumChest(p) && !p.onGround() && s > 0.45)) return;
        double hs = v.horizontalDistance();
        float lean = (float) Mth.clamp((s - 0.2) / 0.9, 0, 1);
        if (lean <= 0.01F || hs < 1.0E-3) return;
        double pathPitch = Math.toDegrees(Math.atan2(v.y, hs));      // +up
        float tilt = (float) ((90 - pathPitch) * lean);              // 0 upright .. 90 horizontal .. 180 diving
        tilt = Mth.clamp(tilt, 0, 165);
        float fx = (float) (v.x / hs), fz = (float) (v.z / hs);
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        float mid = p.getBbHeight() * 0.5F;
        pose.translate(0, mid, 0);
        pose.mulPose(new Quaternionf().rotateAxis((float) Math.toRadians(tilt), fz, 0, -fx));
        pose.translate(0, -mid, 0);
        POSED.add(p.getId());
    }

    private static final it.unimi.dsi.fastutil.ints.IntOpenHashSet POSED = new it.unimi.dsi.fastutil.ints.IntOpenHashSet();

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        if (POSED.remove(event.getEntity().getId())) event.getPoseStack().popPose();
    }

    // ------------------------------------------------------------------ world + HUD

    static void renderWorld(RenderLevelStageEvent event) {
        if (BOOMS.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        long now = System.currentTimeMillis();
        Vec3 cam = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buffers.getBuffer(SiRenderTypes.FIELD_GLOW);
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f m = pose.last().pose();
        for (Iterator<Boom> it = BOOMS.iterator(); it.hasNext(); ) {
            Boom b = it.next();
            float age = (now - b.start) / 650F;
            if (age >= 1) { it.remove(); continue; }
            float radius = 0.8F + age * 7F;
            float alpha = (1 - age) * (1 - age) * 0.8F;
            Vec3 t = Math.abs(b.axis.y) < 0.95 ? b.axis.cross(new Vec3(0, 1, 0)).normalize() : b.axis.cross(new Vec3(1, 0, 0)).normalize();
            Vec3 bn = b.axis.cross(t).normalize();
            // shock cone: a ring that trails behind the boom point as it widens
            Vec3 c = b.pos.subtract(b.axis.scale(age * 2.5));
            int steps = 48;
            for (int i = 0; i < steps; i++) {
                double a0 = i * Math.PI * 2 / steps, a1 = (i + 1) * Math.PI * 2 / steps;
                Vec3 d0 = t.scale(Math.cos(a0)).add(bn.scale(Math.sin(a0))), d1 = t.scale(Math.cos(a1)).add(bn.scale(Math.sin(a1)));
                Vec3 o0 = c.add(d0.scale(radius)), o1 = c.add(d1.scale(radius));
                Vec3 i0 = c.add(d0.scale(radius * 0.86)).add(b.axis.scale(0.5)), i1 = c.add(d1.scale(radius * 0.86)).add(b.axis.scale(0.5));
                put(vc, m, o0, 0.8F, 0.9F, 1F, alpha);
                put(vc, m, o1, 0.8F, 0.9F, 1F, alpha);
                put(vc, m, i1, 0, 0, 0, 0);
                put(vc, m, i0, 0, 0, 0, 0);
            }
        }
        buffers.endBatch(SiRenderTypes.FIELD_GLOW);
        pose.popPose();
    }

    private static void put(VertexConsumer vc, Matrix4f m, Vec3 p, float r, float g, float b, float a) {
        vc.addVertex(m, (float) p.x, (float) p.y, (float) p.z).setColor(r, g, b, a);
    }

    static void renderHud(GuiGraphics g, int w, int h, int primary, float partial) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        double s = Mth.lerp(partial, prevSpeed, speed);
        // speed streaks from the screen edge towards the centre
        if (s > 1.2) {
            float k = (float) Math.min(1, (s - 1.2) / 2.0);
            long t = System.currentTimeMillis();
            java.util.Random rnd = new java.util.Random(t / 50);
            for (int i = 0; i < (int) (14 * k) + 2; i++) {
                double a = rnd.nextDouble() * Math.PI * 2;
                float r0 = (float) (Math.min(w, h) * (0.32 + rnd.nextDouble() * 0.25)), len = 10 + 30 * k;
                float x0 = w / 2F + (float) Math.sin(a) * r0, y0 = h / 2F - (float) Math.cos(a) * r0;
                float x1 = w / 2F + (float) Math.sin(a) * (r0 + len), y1 = h / 2F - (float) Math.cos(a) * (r0 + len);
                FcsDraw.line(g, x0, y0, x1, y1, 0.8F, FcsDraw.argb(0xFFFFFF, 0.18F + 0.25F * k));
            }
        }
        float[] c = FcsHud.centre(FcsClientConfig.Panel.FLIGHT, w, h);
        int pw = 74, ph = 66;
        int x = (int) (c[0] - pw / 2F), y = (int) (c[1] - ph / 2F);
        int col = boosting ? 0xFF9F1C : primary;
        FcsDraw.panel(g, x, y, pw, ph, "VILTRUM  " + (boosting ? "BOOST" : "CRUISE"), col);
        double ms = s * 20;
        FcsDraw.text(g, String.format(Locale.ROOT, "%5.1f", ms), x + 4, y + 12, FcsDraw.argb(FcsDraw.WHITE, 1F), 1.0F);
        FcsDraw.text(g, "M/S", x + 46, y + 14, FcsDraw.argb(col, 0.9F), 0.55F);
        FcsDraw.bar(g, x + 4, y + 23, pw - 8, 5, (float) (s / ViltrumFlight.BOOST_SPEED), col);
        // boom marker on the bar
        int mx = x + 4 + (int) ((pw - 8) * ViltrumFlight.BOOM_SPEED / ViltrumFlight.BOOST_SPEED);
        g.fill(mx, y + 21, mx + 1, y + 30, FcsDraw.argb(FcsDraw.RED, 0.9F));
        double vs = (player.getY() - player.yo) * 20;
        FcsDraw.text(g, String.format(Locale.ROOT, "SHOCK %.2f", s / ViltrumFlight.BOOM_SPEED), x + 4, y + 32,
            FcsDraw.argb(s >= ViltrumFlight.BOOM_SPEED ? FcsDraw.RED : FcsDraw.WHITE, 0.95F), 0.55F);
        FcsDraw.text(g, String.format(Locale.ROOT, "ALT %4.0f  VS %+4.0f", player.getY(), vs), x + 4, y + 40, FcsDraw.argb(FcsDraw.WHITE, 0.95F), 0.55F);
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        long e = SuitModules.energy(chest), max = chest.getItem() instanceof AbstractElectricArmor a ? a.getMaxEnergy(chest) : 1;
        FcsDraw.text(g, String.format(Locale.ROOT, "%.0f EU/t", ViltrumFlight.costPerTick(s)), x + 4, y + 48, FcsDraw.argb(col, 0.9F), 0.55F);
        FcsDraw.bar(g, x + 4, y + 56, pw - 8, 5, e / (float) Math.max(1, max), primary);
    }
}
