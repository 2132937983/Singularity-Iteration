// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.armory;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.armory.SuitUpFxRules;
import com.miophas.singularity_iteration.common.client.render.FxDraw;
import com.miophas.singularity_iteration.common.client.render.SiRenderTypes;
import com.miophas.singularity_iteration.common.client.suit.FcsClientConfig;
import com.miophas.singularity_iteration.common.suit.SuitModules;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Client-side 3D world and first-person HUD spectacles for Armory suit-up & drawn-blade arrivals:
 * <ul>
 *   <li><b>Lock-In &amp; Purge Rings:</b> contracting magnetic lock rings on piece latch and outward decoupling rings on purge.</li>
 *   <li><b>Blade Catch Resonance:</b> diamond palm docking ring + crescent blade gleam in the SlashBlade's own aura color.</li>
 *   <li><b>3-Beat Core Ignition Full-Suit Boot:</b> chest core starburst, dual-helix body scan + ground electromagnetic
 *       shockwave, and a 0.55 s Quantum Suit exclusive dorsal Phase-Wing self-test flex.</li>
 *   <li><b>First-Person Visor Boot Sweep:</b> crisp peripheral optical corner glow + thin horizontal telemetry scanline
 *       that never obstructs the crosshair.</li>
 * </ul>
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
public final class SuitUpFx {
    private SuitUpFx() {}

    private record LockRing(Vec3 at, float startTime, SuitUpFxRules.Tier tier, boolean purge) {}
    private record BladeCatch(int entityId, Vec3 fallbackPos, float startTime, float[] color) {}
    private record FullBoot(int entityId, long startMs, SuitUpFxRules.Tier tier) {}

    private static final List<LockRing> RINGS = new ArrayList<>();
    private static final List<BladeCatch> BLADES = new ArrayList<>();
    private static final List<FullBoot> BOOTS = new ArrayList<>();
    private static long localVisorBootMs;
    private static SuitUpFxRules.Tier localVisorTier = SuitUpFxRules.Tier.QUANTUM;
    private static ClientLevel lastLevel;

    /** Spawns an inward-contracting magnetic lock-in ring at {@code at}. */
    public static void lockRing(Vec3 at, SuitUpFxRules.Tier tier) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !FcsClientConfig.armoryFx()) return;
        RINGS.add(new LockRing(at, mc.level.getGameTime(), tier, false));
    }

    /** Spawns an outward-expanding electromagnetic decoupling ring when a piece purges off the body. */
    public static void purgeRing(Vec3 at, SuitUpFxRules.Tier tier) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !FcsClientConfig.armoryFx()) return;
        RINGS.add(new LockRing(at, mc.level.getGameTime(), tier, true));
    }

    /** Spawns the drawn-blade palm diamond docking ring & crescent blade gleam in {@code bladeRgb}. */
    public static void bladeCatch(Entity wearer, Vec3 palmPos, int bladeRgb) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !FcsClientConfig.armoryFx()) return;
        BLADES.add(new BladeCatch(wearer != null ? wearer.getId() : -1, palmPos, mc.level.getGameTime(), SuitUpFxRules.unpackRgb(bladeRgb)));
    }

    /** Triggers the 3-beat Core Ignition spectacle (and first-person visor sweep for the local player). */
    public static void fullBoot(LivingEntity wearer) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !FcsClientConfig.armoryFx()) return;
        SuitUpFxRules.Tier tier = suitTier(wearer);
        long nowMs = System.currentTimeMillis();
        BOOTS.removeIf(b -> b.entityId == wearer.getId());
        BOOTS.add(new FullBoot(wearer.getId(), nowMs, tier));
        if (wearer == mc.player) {
            localVisorBootMs = nowMs;
            localVisorTier = tier;
        }
    }

    private static SuitUpFxRules.Tier suitTier(LivingEntity wearer) {
        boolean quantum = false, nano = false, living = false;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack s = wearer.getItemBySlot(slot);
            if (SuitModules.isQuantumPiece(s)) quantum = true;
            if (SuitModules.isNanoPiece(s)) nano = true;
            if (SuitModules.hasLivingLining(s)) living = true;
        }
        return SuitUpFxRules.tierOf(quantum, nano, living);
    }

    // ------------------------------------------------------------------ 3D world pass

    @SubscribeEvent
    static void renderWorld(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != lastLevel) { clear(); lastLevel = mc.level; }
        if (mc.level == null || (RINGS.isEmpty() && BLADES.isEmpty() && BOOTS.isEmpty())) return;

        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float nowTick = mc.level.getGameTime() + partial;
        long nowMs = System.currentTimeMillis();

        RINGS.removeIf(r -> nowTick - r.startTime > (r.purge ? SuitUpFxRules.PURGE_RING_TICKS : SuitUpFxRules.LOCK_RING_TICKS) + 1F);
        BLADES.removeIf(b -> nowTick - b.startTime > SuitUpFxRules.BLADE_CATCH_TICKS + 1F);
        BOOTS.removeIf(b -> (nowMs - b.startMs) / 1000F > SuitUpFxRules.BOOT_TOTAL_SEC + 0.05F);
        if (RINGS.isEmpty() && BLADES.isEmpty() && BOOTS.isEmpty()) return;

        Vec3 cam = event.getCamera().getPosition();
        boolean firstPerson = mc.options.getCameraType().isFirstPerson();
        Vector3f camLeft = new Vector3f(event.getCamera().getLeftVector());
        Vector3f camUp = new Vector3f(event.getCamera().getUpVector());

        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f m = pose.last().pose();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buffers.getBuffer(SiRenderTypes.FIELD_GLOW);

        for (LockRing r : RINGS) {
            float dur = r.purge ? SuitUpFxRules.PURGE_RING_TICKS : SuitUpFxRules.LOCK_RING_TICKS;
            float prog = (nowTick - r.startTime) / dur;
            if (prog <= 0F || prog >= 1F) continue;
            float rad = r.purge ? SuitUpFxRules.purgeRingRadius(prog) : SuitUpFxRules.lockRingRadius(prog);
            float alpha = r.purge ? (1F - prog) * (1F - prog) * 0.78F : SuitUpFxRules.lockRingAlpha(prog);
            float[] pri = SuitUpFxRules.primaryColor(r.tier);
            float[] core = SuitUpFxRules.coreColor(r.tier);
            horizontalRing(vc, m, r.at, rad, 0.075F, pri[0], pri[1], pri[2], alpha, 28);
            horizontalRing(vc, m, r.at, rad * 0.88F, 0.035F, core[0], core[1], core[2], alpha * 0.85F, 24);
        }

        for (BladeCatch bc : BLADES) {
            float prog = (nowTick - bc.startTime) / SuitUpFxRules.BLADE_CATCH_TICKS;
            if (prog <= 0F || prog >= 1F) continue;
            Entity wearer = bc.entityId >= 0 ? mc.level.getEntity(bc.entityId) : null;
            Vec3 at = bc.fallbackPos;
            double yawRad = 0;
            if (wearer instanceof LivingEntity living && living.isAlive()) {
                yawRad = Math.toRadians(Mth.rotLerp(partial, living.yBodyRotO, living.yBodyRot));
                double rx = -Math.cos(yawRad), rz = -Math.sin(yawRad);
                double fx = -Math.sin(yawRad), fz = Math.cos(yawRad);
                at = living.getPosition(partial).add(rx * 0.38 + fx * 0.25, living.getBbHeight() * 0.54, rz * 0.38 + fz * 0.25);
            }
            drawBladeCatch(vc, m, at, yawRad, prog, bc.color, camLeft, camUp);
        }

        for (FullBoot b : BOOTS) {
            Entity e = mc.level.getEntity(b.entityId);
            if (!(e instanceof LivingEntity wearer) || !wearer.isAlive()) continue;
            float sec = (nowMs - b.startMs) / 1000F;
            if (sec < 0F || sec > SuitUpFxRules.BOOT_TOTAL_SEC) continue;
            boolean ownFirstPerson = (wearer == mc.player && firstPerson);
            drawFullBoot(vc, m, wearer, partial, sec, b.tier, ownFirstPerson, camLeft, camUp);
        }

        buffers.endBatch(SiRenderTypes.FIELD_GLOW);
        pose.popPose();
    }

    /** Drawn-blade palm diamond docking ring + upward crescent blade gleam in the blade's own color. */
    private static void drawBladeCatch(VertexConsumer vc, Matrix4f m, Vec3 at, double yawRad, float prog, float[] col,
                                       Vector3f camLeft, Vector3f camUp) {
        float ease = 1F - (1F - prog) * (1F - prog);
        float fade = (1F - prog) * (1F - prog);
        // 1. Expanding 4-point diamond ring around the grip
        float rad = 0.16F + 0.44F * ease;
        Vec3 fwd = new Vec3(-Math.sin(yawRad), 0, Math.cos(yawRad));
        Vec3 right = new Vec3(-Math.cos(yawRad), 0, -Math.sin(yawRad));
        Vec3 up = new Vec3(0, 1, 0);
        Vec3[] pts = {
            at.add(up.scale(rad)), at.add(right.scale(rad * 0.78)),
            at.subtract(up.scale(rad)), at.subtract(right.scale(rad * 0.78))
        };
        float thick = 0.032F * (1F - 0.5F * prog);
        for (int i = 0; i < 4; i++) {
            Vec3 a = pts[i], b = pts[(i + 1) & 3];
            Vec3 off = fwd.scale(thick);
            put(vc, m, a.add(off), col[0], col[1], col[2], fade * 0.92F);
            put(vc, m, b.add(off), col[0], col[1], col[2], fade * 0.92F);
            put(vc, m, b.subtract(off), 1F, 0.98F, 1F, fade * 0.55F);
            put(vc, m, a.subtract(off), 1F, 0.98F, 1F, fade * 0.55F);
        }
        // 2. Crisp starburst flash on catch
        if (prog < 0.45F) {
            float sf = 1F - prog / 0.45F;
            FxDraw.star(vc, m, at, camLeft, camUp, 0.58F * sf + 0.12F, col[0], col[1], col[2], sf * 0.90F);
        }
        // 3. Crescent blade-gleam arc sweeping upward along the blade's ready tilt
        int seg = 14;
        float arcHead = Math.min(1F, prog * 1.65F);
        for (int i = 0; i < seg; i++) {
            float k0 = i / (float) seg, k1 = (i + 1F) / seg;
            if (k1 > arcHead) break;
            float f0 = Mth.clamp(1F - (arcHead - k0) * 2.2F, 0F, 1F) * fade;
            float f1 = Mth.clamp(1F - (arcHead - k1) * 2.2F, 0F, 1F) * fade;
            double a0 = Math.toRadians(-35 + 130 * k0), a1 = Math.toRadians(-35 + 130 * k1);
            Vec3 d0 = fwd.scale(Math.cos(a0)).add(up.scale(Math.sin(a0)));
            Vec3 d1 = fwd.scale(Math.cos(a1)).add(up.scale(Math.sin(a1)));
            put(vc, m, at.add(d0.scale(0.32)), col[0], col[1], col[2], 0F);
            put(vc, m, at.add(d0.scale(1.08)), col[0], col[1], col[2], f0 * 0.85F);
            put(vc, m, at.add(d1.scale(1.08)), 1F, 0.98F, 1F, f1 * 0.90F);
            put(vc, m, at.add(d1.scale(0.32)), col[0], col[1], col[2], 0F);
        }
    }

    /** 3-beat Core Ignition full-suit boot: chest starburst, dual-helix scan + ground shockwave, Quantum wing flex. */
    private static void drawFullBoot(VertexConsumer vc, Matrix4f m, LivingEntity wearer, float partial, float sec,
                                     SuitUpFxRules.Tier tier, boolean ownFirstPerson, Vector3f camLeft, Vector3f camUp) {
        Vec3 feet = wearer.getPosition(partial);
        float h = wearer.getBbHeight();
        double yawRad = Math.toRadians(Mth.rotLerp(partial, wearer.yBodyRotO, wearer.yBodyRot));
        Vec3 fwd = new Vec3(-Math.sin(yawRad), 0, Math.cos(yawRad));
        Vec3 right = new Vec3(-Math.cos(yawRad), 0, -Math.sin(yawRad));
        float[] pri = SuitUpFxRules.primaryColor(tier);
        float[] secCol = SuitUpFxRules.secondaryColor(tier);
        float[] core = SuitUpFxRules.coreColor(tier);

        // Beat 1 (0.00s -> 0.35s): Chest core starburst ignition (skip in 1st-person so it doesn't sit in camera)
        if (!ownFirstPerson && sec < 0.35F) {
            float bf = 1F - sec / 0.35F;
            Vec3 chest = feet.add(0, h * 0.68, 0).add(fwd.scale(0.24));
            FxDraw.star(vc, m, chest, camLeft, camUp, 0.95F * bf + 0.18F, pri[0], pri[1], pri[2], bf * 0.92F);
            FxDraw.star(vc, m, chest, camLeft, camUp, 0.52F * bf + 0.10F, core[0], core[1], core[2], bf);
        }

        // Beat 2 (0.05s -> 0.65s): Dual-helix body scan + ground electromagnetic shockwave + 6 radial spikes
        float scan = SuitUpFxRules.bootScanProgress(sec);
        if (scan >= 0F) {
            float fade = (1F - scan) * (1F - scan);
            float ease = 1F - (1F - scan) * (1F - scan) * (1F - scan);
            // Ground shockwave (visible in both 1st and 3rd person!)
            float rOut = 0.55F + 2.65F * ease;
            float rIn = rOut * 0.68F;
            Vec3 ground = feet.add(0, 0.06, 0);
            horizontalRing(vc, m, ground, rOut, 0.14F + 0.14F * scan, pri[0], pri[1], pri[2], fade * 0.80F, 40);
            horizontalRing(vc, m, ground.add(0, 0.03, 0), rIn, 0.09F, secCol[0], secCol[1], secCol[2], fade * 0.68F, 36);
            if (scan < 0.72F) {
                float sf = 1F - scan / 0.72F;
                float sa = sf * sf * 0.78F;
                for (int i = 0; i < 6; i++) {
                    double ang = i * Math.PI / 3.0 + yawRad;
                    Vec3 dir = new Vec3(Math.cos(ang), 0, Math.sin(ang));
                    Vec3 side = new Vec3(-Math.sin(ang), 0, Math.cos(ang)).scale(0.045 * sf);
                    put(vc, m, ground.add(dir.scale(rIn)), pri[0], pri[1], pri[2], 0F);
                    put(vc, m, ground.add(dir.scale(rOut)).add(side), core[0], core[1], core[2], sa);
                    put(vc, m, ground.add(dir.scale(rOut + 0.55F * ease + 0.2F)), secCol[0], secCol[1], secCol[2], 0F);
                    put(vc, m, ground.add(dir.scale(rOut)).subtract(side), core[0], core[1], core[2], sa);
                }
            }
            // Dual counter-sweeping helix body scan rings (chest -> head and chest -> feet)
            if (!ownFirstPerson) {
                float yUp = h * (0.55F + 0.48F * scan);
                float yDown = h * (0.55F - 0.52F * scan);
                float ringA = (1F - Math.abs(scan - 0.35F) * 1.2F) * 0.82F;
                if (ringA > 0.02F) {
                    horizontalRing(vc, m, feet.add(0, yUp, 0), 0.54F, 0.075F, core[0], core[1], core[2], ringA, 28);
                    horizontalRing(vc, m, feet.add(0, yDown, 0), 0.54F, 0.075F, pri[0], pri[1], pri[2], ringA * 0.88F, 28);
                }
            }
        }

        // Beat 3 (0.20s -> 0.75s): Quantum Suit exclusive dorsal Phase-Wing self-test flex!
        if (!ownFirstPerson && (tier == SuitUpFxRules.Tier.QUANTUM || tier == SuitUpFxRules.Tier.QUANTUM_LIVING)) {
            float flex = SuitUpFxRules.bootWingFlex(sec);
            if (flex > 0.01F) {
                drawBootWingFlex(vc, m, feet.add(0, h * 0.72, 0).subtract(fwd.scale(0.24)), fwd, right, flex, pri, secCol, core);
            }
        }
    }

    /** Quantum Suit exclusive brief dorsal phase-wing self-test flex (3 sleek geometric blades per side). */
    private static void drawBootWingFlex(VertexConsumer vc, Matrix4f m, Vec3 backRoot, Vec3 fwd, Vec3 right,
                                         float flex, float[] pri, float[] sec, float[] core) {
        Vec3 up = new Vec3(0, 1, 0);
        float alpha = Math.min(0.82F, flex * 1.35F);
        float span = 0.75F + 0.85F * flex;
        for (int side : new int[]{-1, 1}) {
            Vec3 root = backRoot.add(right.scale(side * 0.22));
            Vec3 outDir = right.scale(side * 0.90).subtract(fwd.scale(0.38)).add(up.scale(0.18)).normalize();
            // Primary upper vane
            wingBlade(vc, m, root, outDir, up, span, 0.28F, 0.30F, core, pri, alpha);
            // Mid swept fin
            Vec3 midDir = right.scale(side * 0.86).subtract(fwd.scale(0.45)).add(up.scale(-0.08)).normalize();
            wingBlade(vc, m, root.subtract(up.scale(0.08)), midDir, up, span * 0.82F, 0.22F, 0.14F, pri, sec, alpha * 0.78F);
            // Lower stabilizer feather
            Vec3 lowDir = right.scale(side * 0.78).subtract(fwd.scale(0.50)).add(up.scale(-0.34)).normalize();
            wingBlade(vc, m, root.subtract(up.scale(0.16)), lowDir, up, span * 0.64F, 0.18F, 0.04F, sec, pri, alpha * 0.62F);
        }
    }

    private static void wingBlade(VertexConsumer vc, Matrix4f m, Vec3 root, Vec3 outDir, Vec3 up,
                                  float len, float chord, float lift, float[] inner, float[] outer, float alpha) {
        Vec3 mid = root.add(outDir.scale(len * 0.56)).add(up.scale(lift * 0.65));
        Vec3 tip = root.add(outDir.scale(len)).add(up.scale(lift));
        Vec3 rootLow = root.subtract(up.scale(chord));
        Vec3 midLow = mid.subtract(up.scale(chord * 0.62));
        quadBothSides(vc, m, root, inner, alpha, mid, outer, alpha * 0.75F, midLow, outer, alpha * 0.20F, rootLow, outer, alpha * 0.25F);
        quadBothSides(vc, m, mid, outer, alpha * 0.75F, tip, inner, alpha * 0.12F, tip.subtract(up.scale(0.04)), outer, 0F, midLow, outer, alpha * 0.20F);
    }

    // ------------------------------------------------------------------ 2D first-person HUD visor sweep

    @SubscribeEvent
    static void renderHud(RenderGuiEvent.Post event) {
        if (localVisorBootMs == 0L) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.options.getCameraType().isFirstPerson() || !FcsClientConfig.armoryFx()) {
            localVisorBootMs = 0L;
            return;
        }
        float sec = (System.currentTimeMillis() - localVisorBootMs) / 1000F;
        float prog = SuitUpFxRules.bootVisorScan(sec);
        if (prog < 0F) {
            localVisorBootMs = 0L;
            return;
        }
        GuiGraphics g = event.getGuiGraphics();
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        float[] col = localVisorTier == SuitUpFxRules.Tier.QUANTUM_LIVING
            ? SuitUpFxRules.coreColor(localVisorTier)
            : SuitUpFxRules.primaryColor(localVisorTier);
        int rgb = (((int) (col[0] * 255)) << 16) | (((int) (col[1] * 255)) << 8) | ((int) (col[2] * 255));

        // 1. Peripheral optical visor corner brackets (strictly at the screen corners, never near crosshair)
        float cornerFade = prog < 0.18F ? prog / 0.18F : (1F - prog) / 0.82F;
        int cornerAlpha = Mth.clamp((int) (cornerFade * 155), 0, 255);
        if (cornerAlpha > 4) {
            int c = (cornerAlpha << 24) | rgb;
            int pad = 14, len = 24, t = 2;
            // Top-left
            g.fill(pad, pad, pad + len, pad + t, c);
            g.fill(pad, pad, pad + t, pad + len, c);
            // Top-right
            g.fill(w - pad - len, pad, w - pad, pad + t, c);
            g.fill(w - pad - t, pad, w - pad, pad + len, c);
            // Bottom-left
            g.fill(pad, h - pad - t, pad + len, h - pad, c);
            g.fill(pad, h - pad - len, pad + t, h - pad, c);
            // Bottom-right
            g.fill(w - pad - len, h - pad - t, w - pad, h - pad, c);
            g.fill(w - pad - t, h - pad - len, w - pad, h - pad, c);
        }

        // 2. Razor-thin high-speed horizontal optical scanline sweeping top -> bottom
        int scanY = (int) (h * prog);
        int lineAlpha = Mth.clamp((int) ((1F - Math.abs(prog - 0.5F) * 1.6F) * 110), 0, 255);
        if (lineAlpha > 4) {
            int cLine = (lineAlpha << 24) | rgb;
            int cSoft = ((lineAlpha / 3) << 24) | rgb;
            g.fill(0, scanY, w, scanY + 1, cLine);
            if (scanY >= 2) g.fill(0, scanY - 2, w, scanY, cSoft);
        }
    }

    // ------------------------------------------------------------------ geometry helpers

    private static void horizontalRing(VertexConsumer vc, Matrix4f m, Vec3 c, float r, float w,
                                       float cr, float cg, float cb, float alpha, int seg) {
        if (alpha <= 0.004F) return;
        float rIn = Math.max(0.03F, r - w);
        float rOut = r + 0.04F;
        for (int i = 0; i < seg; i++) {
            double a0 = i * Math.PI * 2 / seg, a1 = (i + 1) * Math.PI * 2 / seg;
            Vec3 d0 = new Vec3(Math.cos(a0), 0, Math.sin(a0));
            Vec3 d1 = new Vec3(Math.cos(a1), 0, Math.sin(a1));
            put(vc, m, c.add(d0.scale(rIn)), cr, cg, cb, 0F);
            put(vc, m, c.add(d1.scale(rIn)), cr, cg, cb, 0F);
            put(vc, m, c.add(d1.scale(r)), cr, cg, cb, alpha);
            put(vc, m, c.add(d0.scale(r)), cr, cg, cb, alpha);
            put(vc, m, c.add(d0.scale(r)), cr, cg, cb, alpha);
            put(vc, m, c.add(d1.scale(r)), cr, cg, cb, alpha);
            put(vc, m, c.add(d1.scale(rOut)), cr, cg, cb, 0F);
            put(vc, m, c.add(d0.scale(rOut)), cr, cg, cb, 0F);
        }
    }

    private static void quadBothSides(VertexConsumer vc, Matrix4f m,
                                      Vec3 p0, float[] c0, float a0, Vec3 p1, float[] c1, float a1,
                                      Vec3 p2, float[] c2, float a2, Vec3 p3, float[] c3, float a3) {
        put(vc, m, p0, c0[0], c0[1], c0[2], a0);
        put(vc, m, p1, c1[0], c1[1], c1[2], a1);
        put(vc, m, p2, c2[0], c2[1], c2[2], a2);
        put(vc, m, p3, c3[0], c3[1], c3[2], a3);

        put(vc, m, p3, c3[0], c3[1], c3[2], a3);
        put(vc, m, p2, c2[0], c2[1], c2[2], a2);
        put(vc, m, p1, c1[0], c1[1], c1[2], a1);
        put(vc, m, p0, c0[0], c0[1], c0[2], a0);
    }

    private static void put(VertexConsumer vc, Matrix4f m, Vec3 p, float r, float g, float b, float a) {
        vc.addVertex(m, (float) p.x, (float) p.y, (float) p.z).setColor(r, g, b, Math.max(0F, Math.min(1F, a)));
    }

    static void clear() {
        RINGS.clear();
        BLADES.clear();
        BOOTS.clear();
        localVisorBootMs = 0L;
    }
}
