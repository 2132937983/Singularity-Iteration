// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.client.flight.JetpackSoundClient;
import com.miophas.singularity_iteration.common.client.render.SiRenderTypes;
import com.miophas.singularity_iteration.common.client.suit.nano.ThrusterFx;
import com.miophas.singularity_iteration.common.item.armor.ArmorFeatureToggle;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.joml.Matrix4f;

/**
 * 0.1.10.31 (S-4): the quantum suit in normal mode, on every client and for every nearby player.
 * <ul>
 *   <li><b>Boost jump</b> (quantum boots): a ring and a dust ring at the take-off point, sparks and a pop
 *       ({@link ThrusterFx#ground}); the soles burn for {@link #BURST_TICKS} ticks ({@link #soleBurst}, drawn by
 *       {@link SuitThrustLayer}). The own player starts it from the boots at the boost ({@link #ownBoostJump}); other players
 *       are recognised by {@link MotionFxRules} from the synced motion.</li>
 *   <li><b>Hard landing</b> (quantum boots): the landing ring of {@link ThrusterFx} at the landed point.</li>
 *   <li><b>Sprint</b> (quantum leggings with speed boost on): thin air streaks trail from the calves (alpha veil).</li>
 * </ul>
 * Option {@code motion_fx}. The pieces must hold charge; Special Maneuver flight, jetpack thrust, elytra, water and riding
 * pause the tracking.
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
public final class SuitMotionFx {
    private SuitMotionFx() {}

    static final int BURST_TICKS = 8;
    private static final int STREAK_POINTS = 9;
    private static final ResourceLocation BOOTS = id("armor/item_armor_quantum_boots"), LEGGINGS = id("armor/item_armor_quantum_leggings");
    private static final Map<Player, State> STATES = new WeakHashMap<>();

    static {
        com.miophas.singularity_iteration.common.item.armor.mio_icif_boots_quantum.BOOST_JUMP = SuitMotionFx::ownBoostJump;
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, path);
    }

    private static final class State {
        final MotionFxRules.Tracker tracker = new MotionFxRules.Tracker();
        long burstTick = Long.MIN_VALUE / 2;
        float burstStrength;
        boolean ownJumpPending;
        /** Calf streak points, newest first: {x, y, z} per streak (4 streaks). */
        final double[][] streak = new double[STREAK_POINTS][12];
        int streakCount;
        float streakAlpha;
    }

    static boolean wearing(Player p, EquipmentSlot slot, ResourceLocation item) {
        ItemStack stack = p.getItemBySlot(slot);
        return !stack.isEmpty() && item.equals(BuiltInRegistries.ITEM.getKey(stack.getItem())) && SuitGlowLayer.charge(stack) > 0F;
    }

    /** Hook of the quantum boots (client): the own player starts a boost jump now. */
    public static void ownBoostJump(Player player) {
        if (!(player instanceof LocalPlayer) || !FcsClientConfig.motionFx()) return;
        State s = STATES.computeIfAbsent(player, k -> new State());
        if (!s.ownJumpPending) {
            s.ownJumpPending = true;               // the tracker must not raise it again
            boostJump(player, s, player.position(), 0.85F);
        }
    }

    private static void boostJump(Player p, State s, Vec3 at, float strength) {
        s.burstTick = p.level().getGameTime();
        s.burstStrength = strength;
        ThrusterFx.ground(p, at, strength, true);
    }

    /** Sole burn of a boost jump, 0..1 (strong at once, fading over {@link #BURST_TICKS} ticks). */
    public static float soleBurst(Player p, float partial) {
        State s = STATES.get(p);
        if (s == null) return 0F;
        float age = p.level().getGameTime() - s.burstTick + partial;
        if (age < 0 || age >= BURST_TICKS) return 0F;
        float k = 1 - age / BURST_TICKS;
        return s.burstStrength * k * k;
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        Player p = event.getEntity();
        if (!p.level().isClientSide || !FcsClientConfig.motionFx()) return;
        boolean boots = wearing(p, EquipmentSlot.FEET, BOOTS);
        ItemStack legs = p.getItemBySlot(EquipmentSlot.LEGS);
        boolean leggings = wearing(p, EquipmentSlot.LEGS, LEGGINGS) && ArmorFeatureToggle.isEnabled(legs, "speed_boost");
        State s = STATES.get(p);
        if (!boots && !leggings) {
            if (s != null) STATES.remove(p);
            return;
        }
        if (s == null) STATES.put(p, s = new State());
        boolean local = p instanceof LocalPlayer;
        if (boots) {
            boolean flight = p.isFallFlying() || p.isInWater() || p.isPassenger() || p.isSpectator() || ManeuverModeClient.inManeuver(p)
                || JetpackSoundClient.throttle(p) > 0F || local && p.getAbilities().flying;
            double target = local ? p.getY() : p.lerpTargetY();
            int e = s.tracker.tick(p.getY(), p.getY() - p.yo, target, p.onGround(), flight);
            if (e == MotionFxRules.JUMP && !s.ownJumpPending) {
                boostJump(p, s, new Vec3(local ? p.getX() : p.lerpTargetX(), s.tracker.eventY, local ? p.getZ() : p.lerpTargetZ()),
                    s.tracker.strength);
            } else if (e == MotionFxRules.LAND) {
                ThrusterFx.ground(p, new Vec3(local ? p.getX() : p.lerpTargetX(), s.tracker.eventY, local ? p.getZ() : p.lerpTargetZ()),
                    s.tracker.strength, false);
            }
            if (p.onGround()) s.ownJumpPending = false;
        }
        // sprint streaks: four points per tick (two per calf), newest first
        double speed = Math.sqrt((p.getX() - p.xo) * (p.getX() - p.xo) + (p.getZ() - p.zo) * (p.getZ() - p.zo));
        boolean run = leggings && MotionFxRules.streaks(p.isSprinting(), p.onGround(), speed);
        s.streakAlpha = Mth.clamp(s.streakAlpha + (run ? 0.25F : -0.2F), 0F, 1F);
        if (s.streakAlpha > 0F) {
            for (int i = STREAK_POINTS - 1; i > 0; i--) System.arraycopy(s.streak[i - 1], 0, s.streak[i], 0, 12);
            double yaw = Math.toRadians(p.yBodyRot);
            double rx = -Math.cos(yaw), rz = -Math.sin(yaw);          // body right
            double fx = -Math.sin(yaw), fz = Math.cos(yaw);           // body forward
            double[] now = s.streak[0];
            int k = 0;
            for (int side = -1; side <= 1; side += 2) {
                for (int level = 0; level < 2; level++) {
                    double lateral = 0.19 * side, height = level == 0 ? 0.24 : 0.42, back = -0.12;
                    now[k++] = p.getX() + rx * lateral + fx * back;
                    now[k++] = p.getY() + height;
                    now[k++] = p.getZ() + rz * lateral + fz * back;
                }
            }
            s.streakCount = run ? Math.min(STREAK_POINTS, s.streakCount + 1) : s.streakCount;
        } else {
            s.streakCount = 0;
        }
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.isPaused()) return;
        ThrusterFx.tick(mc);                                        // 0.1.10.31 (S-5) sparks of full-thrust jets
    }

    /** World pass (after particles): the calf streaks, an alpha veil that thins out behind the runner. */
    public static void render(RenderLevelStageEvent event) {
        if (STATES.isEmpty() || !FcsClientConfig.motionFx()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Vec3 cam = event.getCamera().getPosition();
        boolean firstPerson = mc.options.getCameraType().isFirstPerson();
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f m = pose.last().pose();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer vc = null;
        for (Map.Entry<Player, State> e : STATES.entrySet()) {
            State s = e.getValue();
            if (s.streakCount < 2 || s.streakAlpha <= 0F || e.getKey() == mc.player && firstPerson) continue;
            if (vc == null) vc = buffers.getBuffer(SiRenderTypes.VAPOR);
            for (int k = 0; k < 4; k++) {
                for (int i = 0; i + 1 < s.streakCount; i++) {
                    double[] a = s.streak[i], b = s.streak[i + 1];
                    float a0 = 0.22F * s.streakAlpha * (1 - i / (float) (s.streakCount - 1));
                    float a1 = 0.22F * s.streakAlpha * (1 - (i + 1) / (float) (s.streakCount - 1));
                    float w = 0.018F * (1 - 0.5F * i / (float) s.streakCount);
                    streak(vc, m, a[k * 3], a[k * 3 + 1], a[k * 3 + 2], b[k * 3], b[k * 3 + 1], b[k * 3 + 2], cam, w, a0, a1);
                }
            }
        }
        if (vc != null) buffers.endBatch(SiRenderTypes.VAPOR);
        pose.popPose();
    }

    /** One camera-facing streak segment, alpha {@code a0} at a and {@code a1} at b (pale blue-white). */
    private static void streak(VertexConsumer vc, Matrix4f m, double ax, double ay, double az, double bx, double by, double bz, Vec3 cam,
                               float half, float a0, float a1) {
        double dx = bx - ax, dy = by - ay, dz = bz - az;
        double mx = (ax + bx) * 0.5 - cam.x, my = (ay + by) * 0.5 - cam.y, mz = (az + bz) * 0.5 - cam.z;
        double sx = dy * mz - dz * my, sy = dz * mx - dx * mz, sz = dx * my - dy * mx;
        double len = Math.sqrt(sx * sx + sy * sy + sz * sz);
        if (len < 1.0E-8) return;
        double k = half / len;
        sx *= k; sy *= k; sz *= k;
        vc.addVertex(m, (float) (ax + sx), (float) (ay + sy), (float) (az + sz)).setColor(0.88F, 0.95F, 1F, a0);
        vc.addVertex(m, (float) (ax - sx), (float) (ay - sy), (float) (az - sz)).setColor(0.88F, 0.95F, 1F, a0);
        vc.addVertex(m, (float) (bx - sx), (float) (by - sy), (float) (bz - sz)).setColor(0.88F, 0.95F, 1F, a1);
        vc.addVertex(m, (float) (bx + sx), (float) (by + sy), (float) (bz + sz)).setColor(0.88F, 0.95F, 1F, a1);
    }
}
