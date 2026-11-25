// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.armory;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.armory.WingmanDeployFxPacket;
import com.miophas.singularity_iteration.common.armory.WingmanDeployRules;
import com.miophas.singularity_iteration.common.armory.WingmanEntity;
import com.miophas.singularity_iteration.common.armory.WingmanMode;
import com.miophas.singularity_iteration.common.armory.mio_icif_armory;
import com.miophas.singularity_iteration.common.client.render.FxDraw;
import com.miophas.singularity_iteration.common.client.render.FxLodRules;
import com.miophas.singularity_iteration.common.client.render.IrisCompat;
import com.miophas.singularity_iteration.common.client.render.SiFxBuffers;
import com.miophas.singularity_iteration.common.client.suit.FcsClientConfig;
import com.miophas.singularity_iteration.common.suit.SuitModules;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Client-side spectacle for Wingman Armory deployment and recall:
 * <ul>
 *   <li><b>Armory Silo Linkage:</b> drives the Armory's six-blade iris hatch and 3-stage railgun pulses when a Wingman
 *       launches from or returns to a nearby Armory, plus an ascending mode-tinted electromagnetic accelerator ring.</li>
 *   <li><b>Large Quantum Transfer Gate:</b> opens a 1.45-block dual-ring counter-rotating quantum portal (electric violet
 *       inner core + tactical mode outer ring + 3 spiral arms + starburst flash, with emerald core for Living Nano Lining)
 *       for remote/cross-dimension aerial drops and remote recalls.</li>
 *   <li><b>Orbital Air-Brake Shockwave:</b> fires a dual-ring mode-tinted electromagnetic brake wave when a high-altitude
 *       dropping Wingman arrests its descent near the owner.</li>
 *   <li><b>Phase-Materialization Boot Scan:</b> sweeps dual holographic scan rings and vertical wireframe ribs over a
 *       newly deployed Wingman for 0.7 s.</li>
 * </ul>
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
public final class WingmanDeployFx {
    private WingmanDeployFx() {}

    private record QuantumGate(Vec3 centre, Vec3 normal, float startTime, float[] accent, boolean living, boolean returning) {}
    private record Shockwave(Vec3 centre, float startTime, float maxRadius, float[] accent, boolean living, boolean ascending) {}
    private record BodyScan(int entityId, float startTime, float[] accent, boolean living) {}
    private record PendingAirBrake(int entityId, float startTime, float[] accent, boolean living) {}

    private static final List<QuantumGate> GATES = new ArrayList<>();
    private static final List<Shockwave> SHOCKS = new ArrayList<>();
    private static final List<BodyScan> SCANS = new ArrayList<>();
    private static final Int2ObjectOpenHashMap<PendingAirBrake> PENDING_BRAKES = new Int2ObjectOpenHashMap<>();
    private static final Map<BlockPos, Float> HATCH_PULSES = new HashMap<>();
    private static final Map<BlockPos, Float> RETURN_HATCHES = new HashMap<>();
    private static final IntOpenHashSet RECALL_GATED = new IntOpenHashSet();
    private static ClientLevel lastLevel;

    /** Called on the client main thread when {@link WingmanDeployFxPacket} arrives. */
    public static void onPacket(WingmanDeployFxPacket p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !FcsClientConfig.armoryFx()) return;
        float now = mc.level.getGameTime();
        WingmanMode mode = WingmanMode.byId(p.mode());
        float[] accent = WingmanDeployRules.modeAccentColor(mode);
        float start = now + p.staggerTicks() * 0.35F;

        if (p.launch()) {
            // Near-range Armory railgun launch: open the iris hatch + fire ascending accelerator ring
            HATCH_PULSES.put(p.armoryPos().immutable(), now);
            SHOCKS.add(new Shockwave(p.origin().subtract(0, 0.35, 0), now, 1.55F, accent, p.livingLining(), true));
            SCANS.add(new BodyScan(p.entityId(), now, accent, p.livingLining()));
        } else {
            // Remote / cross-dimension drop: large quantum transfer gate at arrival point
            GATES.add(new QuantumGate(p.origin(), new Vec3(0, -1, 0), start, accent, p.livingLining(), false));
            SCANS.add(new BodyScan(p.entityId(), start + 1F, accent, p.livingLining()));
            if (p.dropHeight() >= 4.0F) {
                PENDING_BRAKES.put(p.entityId(), new PendingAirBrake(p.entityId(), now, accent, p.livingLining()));
            } else {
                // Indoor / tight warp: immediate compact horizontal arrival ring
                SHOCKS.add(new Shockwave(p.origin().subtract(0, 0.6, 0), start + 1F, 1.65F, accent, p.livingLining(), false));
            }
        }
    }

    /** Called by {@link ArmoryRenderer} to query Wingman-driven iris hatch opening (0..1) at {@code pos}. */
    public static float hatchOpen(BlockPos pos, float time) {
        float open = 0F;
        Float launchStart = HATCH_PULSES.get(pos);
        if (launchStart != null) open = Math.max(open, WingmanDeployRules.hatchOpen(time - launchStart));
        Float retSeen = RETURN_HATCHES.get(pos);
        if (retSeen != null) {
            float dt = time - retSeen;
            if (dt <= 8F) open = Math.max(open, 1F);
            else if (dt < 18F) open = Math.max(open, 1F - (dt - 8F) / 10F);
        }
        return open;
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != lastLevel) {
            clear();
            lastLevel = mc.level;
        }
        if (mc.level == null || mc.player == null || !FcsClientConfig.armoryFx()) return;
        float now = mc.level.getGameTime();

        HATCH_PULSES.values().removeIf(t -> now - t > WingmanDeployRules.HATCH_TOTAL_TICKS + 4F);
        RETURN_HATCHES.values().removeIf(t -> now - t > 20F);

        // 1. Check pending orbital air-brake shockwaves for high-altitude dropping Wingmen
        if (!PENDING_BRAKES.isEmpty()) {
            var it = PENDING_BRAKES.values().iterator();
            while (it.hasNext()) {
                PendingAirBrake pb = it.next();
                float age = now - pb.startTime;
                Entity e = mc.level.getEntity(pb.entityId);
                if (e == null || !e.isAlive() || age > 35F) {
                    it.remove();
                    continue;
                }
                // Fire the air-brake shockwave once the Wingman pulls out of its orbital dive (or after 10 ticks)
                if (age >= 4F && (e.getDeltaMovement().y > -0.22 || age >= 12F)) {
                    SHOCKS.add(new Shockwave(e.position().add(0, 0.25, 0), now, 2.65F, pb.accent, pb.living, false));
                    it.remove();
                }
            }
        }

        // 2. Scan nearby returning Wingmen: open nearby Armory hatches OR spawn remote recall quantum gates
        for (WingmanEntity w : mc.level.getEntitiesOfClass(WingmanEntity.class, mc.player.getBoundingBox().inflate(96))) {
            if (w.state() != WingmanEntity.RETURNING) {
                RECALL_GATED.remove(w.getId());
                continue;
            }
            BlockPos nearbyArmory = findNearbyArmory(mc.level, w.blockPosition(), 16);
            if (nearbyArmory != null) {
                RETURN_HATCHES.put(nearbyArmory, now);
                continue;
            }
            // Remote / cross-dim recall: climbing out with no Armory nearby -> open overhead recall quantum gate
            if (!RECALL_GATED.contains(w.getId()) && w.getDeltaMovement().y > 0.04) {
                RECALL_GATED.add(w.getId());
                float[] accent = WingmanDeployRules.modeAccentColor(w.mode());
                boolean living = SuitModules.hasLivingLining(w.getItemBySlot(EquipmentSlot.CHEST));
                GATES.add(new QuantumGate(w.position().add(0, 3.8, 0), new Vec3(0, 1, 0), now, accent, living, true));
            }
        }
        if (RECALL_GATED.size() > 32) RECALL_GATED.clear();
    }

    private static BlockPos findNearbyArmory(ClientLevel level, BlockPos center, int radius) {
        int rChunks = (radius >> 4) + 1;
        int cx0 = center.getX() >> 4, cz0 = center.getZ() >> 4;
        BlockPos best = null;
        double bestDist = radius * radius;
        for (int cx = cx0 - rChunks; cx <= cx0 + rChunks; cx++) {
            for (int cz = cz0 - rChunks; cz <= cz0 + rChunks; cz++) {
                var chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) continue;
                for (var be : chunk.getBlockEntities().values()) {
                    if (!(be instanceof mio_icif_armory)) continue;
                    double d = be.getBlockPos().distSqr(center);
                    if (d < bestDist) {
                        bestDist = d;
                        best = be.getBlockPos().immutable();
                    }
                }
            }
        }
        return best;
    }

    @SubscribeEvent
    static void render(RenderLevelStageEvent event) {
        if (!IrisCompat.isFxStage(event)) return;
        if (GATES.isEmpty() && SHOCKS.isEmpty() && SCANS.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !FcsClientConfig.armoryFx()) return;

        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float now = mc.level.getGameTime() + partial;
        GATES.removeIf(g -> now - g.startTime > WingmanDeployRules.GATE_TOTAL_TICKS + 1F);
        SHOCKS.removeIf(s -> now - s.startTime > WingmanDeployRules.SHOCK_TICKS + 1F);
        SCANS.removeIf(s -> now - s.startTime > WingmanDeployRules.SCAN_TICKS + 1F);
        if (GATES.isEmpty() && SHOCKS.isEmpty() && SCANS.isEmpty()) return;

        Vec3 cam = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f m = pose.last().pose();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

        // Pass 1: dark quantum portal void cores (alpha-blended)
        if (!GATES.isEmpty()) {
            VertexConsumer veil = SiFxBuffers.translucent(buffers);
            for (QuantumGate g : GATES) {
                float open = WingmanDeployRules.gateOpen(now - g.startTime);
                if (open > 0.01F) drawVoidCore(veil, m, g, open);
            }
            SiFxBuffers.endTranslucent(buffers);
        }

        // Pass 2: additive electromagnetic rings, spiral arms, starbursts, shockwaves & body scans
        VertexConsumer glow = SiFxBuffers.glow(buffers);
        Vector3f camLeft = new Vector3f(event.getCamera().getLeftVector());
        Vector3f camUp = new Vector3f(event.getCamera().getUpVector());

        for (QuantumGate g : GATES) {
            float age = now - g.startTime;
            float open = WingmanDeployRules.gateOpen(age);
            if (open <= 0.01F) continue;
            int tier = FxLodRules.tier(g.centre.distanceToSqr(cam), FcsClientConfig.fxLod());
            drawGateRings(glow, m, g, open, now, cam, tier);
            float flash = g.returning ? 0F : WingmanDeployRules.gateFlash(age);
            if (flash > 0.01F) {
                float[] starCol = g.living ? WingmanDeployRules.LIVING_CORE : g.accent;
                FxDraw.star(glow, m, g.centre, camLeft, camUp, 1.85F * flash + 0.35F, starCol[0], starCol[1], starCol[2], flash);
                FxDraw.star(glow, m, g.centre, camLeft, camUp, 0.95F * flash + 0.15F, 1F, 0.98F, 1F, flash * 0.9F);
            }
        }

        for (Shockwave s : SHOCKS) {
            float age = (now - s.startTime) / WingmanDeployRules.SHOCK_TICKS;
            if (age <= 0F || age >= 1F) continue;
            drawShockwave(glow, m, s, age);
        }

        for (BodyScan s : SCANS) {
            float prog = WingmanDeployRules.scanProgress(now - s.startTime);
            if (prog < 0F) continue;
            Entity e = mc.level.getEntity(s.entityId);
            if (e != null && e.isAlive()) {
                drawBodyScan(glow, m, e.getPosition(partial), e.getBbHeight(), prog, s.accent, s.living);
            }
        }

        SiFxBuffers.endGlow(buffers);
        pose.popPose();
    }

    private static Vec3[] frame(Vec3 n) {
        Vec3 t = Math.abs(n.y) < 0.95 ? n.cross(new Vec3(0, 1, 0)).normalize() : n.cross(new Vec3(1, 0, 0)).normalize();
        return new Vec3[]{t, n.cross(t).normalize()};
    }

    private static void drawVoidCore(VertexConsumer vc, Matrix4f m, QuantumGate g, float open) {
        Vec3[] f = frame(g.normal);
        float r = WingmanDeployRules.GATE_RADIUS * open;
        int seg = 28;
        for (int s = 0; s < seg; s++) {
            double a0 = s * Math.PI * 2 / seg, a1 = (s + 1) * Math.PI * 2 / seg;
            Vec3 p0 = g.centre.add(f[0].scale(Math.cos(a0) * r)).add(f[1].scale(Math.sin(a0) * r));
            Vec3 p1 = g.centre.add(f[0].scale(Math.cos(a1) * r)).add(f[1].scale(Math.sin(a1) * r));
            put(vc, m, g.centre, 0.03F, 0.0F, 0.09F, 0.65F * open);
            put(vc, m, p0, 0.05F, 0.02F, 0.14F, 0.18F * open);
            put(vc, m, p1, 0.05F, 0.02F, 0.14F, 0.18F * open);
            put(vc, m, g.centre, 0.03F, 0.0F, 0.09F, 0.65F * open);
        }
    }

    private static void drawGateRings(VertexConsumer vc, Matrix4f m, QuantumGate g, float open, float time, Vec3 cam, int tier) {
        Vec3[] f = frame(g.normal);
        float r = WingmanDeployRules.GATE_RADIUS * open;
        int seg = FxLodRules.segments(36, tier);
        float[] outer = g.accent;
        float[] inner = g.living ? WingmanDeployRules.LIVING_CORE : WingmanDeployRules.VIOLET_INNER;

        for (int ring = 0; ring < 2; ring++) {
            float rr = ring == 0 ? r : r * 0.70F;
            float w = ring == 0 ? 0.11F : 0.075F;
            float spin = (ring == 0 ? 1.1F : -1.8F) * time * 0.11F;
            float[] col = ring == 0 ? outer : inner;
            for (int s = 0; s < seg; s++) {
                double a0 = s * Math.PI * 2 / seg, a1 = (s + 1) * Math.PI * 2 / seg;
                float dash = 0.42F + 0.58F * (float) Math.pow(Math.max(0, Math.cos(a0 * 4 + spin)), 2);
                Vec3 i0 = g.centre.add(f[0].scale(Math.cos(a0) * rr)).add(f[1].scale(Math.sin(a0) * rr));
                Vec3 i1 = g.centre.add(f[0].scale(Math.cos(a1) * rr)).add(f[1].scale(Math.sin(a1) * rr));
                Vec3 o0 = g.centre.add(f[0].scale(Math.cos(a0) * (rr + w))).add(f[1].scale(Math.sin(a0) * (rr + w)));
                Vec3 o1 = g.centre.add(f[0].scale(Math.cos(a1) * (rr + w))).add(f[1].scale(Math.sin(a1) * (rr + w)));
                float a = open * dash * (ring == 0 ? 0.92F : 0.75F);
                put(vc, m, i0, col[0], col[1], col[2], a);
                put(vc, m, i1, col[0], col[1], col[2], a);
                put(vc, m, o1, col[0], col[1], col[2], 0F);
                put(vc, m, o0, col[0], col[1], col[2], 0F);
            }
        }
        if (tier == FxLodRules.FAR) return;
        for (int arm = 0; arm < 3; arm++) {
            Vec3 prev = null;
            for (int k = 0; k <= 10; k++) {
                double u = k / 10.0;
                double ang = arm * Math.PI * 2 / 3 + u * 2.6 - time * 0.18;
                double rad = r * (1.0 - u);
                Vec3 p = g.centre.add(f[0].scale(Math.cos(ang) * rad)).add(f[1].scale(Math.sin(ang) * rad));
                if (prev != null) {
                    float mix = (float) u;
                    float cr = Mth.lerp(mix, outer[0], inner[0]);
                    float cg = Mth.lerp(mix, outer[1], inner[1]);
                    float cb = Mth.lerp(mix, outer[2], inner[2]);
                    FxDraw.line(vc, m, prev, p, cam, 0.042F, cr, cg, cb, 0.68F * open * (float) (1.0 - u * 0.55));
                }
                prev = p;
            }
        }
    }

    /** Mode-tinted orbital air-brake shockwave (or ascending silo accelerator ring) + 6 radial spikes. */
    private static void drawShockwave(VertexConsumer vc, Matrix4f m, Shockwave s, float age) {
        float ease = 1F - (1F - age) * (1F - age) * (1F - age);
        float fade = (1F - age) * (1F - age);
        Vec3 c = s.ascending ? s.centre.add(0, ease * 2.2, 0) : s.centre;
        float rOuter = 0.45F + s.maxRadius * ease;
        float rInner = rOuter * 0.68F;
        float[] col = s.accent;
        float[] core = s.living ? WingmanDeployRules.LIVING_CORE : new float[]{0.92F, 0.97F, 1.0F};

        horizontalRing(vc, m, c, rOuter, 0.14F + 0.16F * age, col[0], col[1], col[2], fade * 0.82F, 36);
        horizontalRing(vc, m, c.add(0, s.ascending ? -0.18 : 0.06, 0), rInner, 0.09F, core[0], core[1], core[2], fade * 0.72F, 32);

        if (age < 0.72F) {
            float sf = (1F - age / 0.72F);
            float sa = sf * sf * 0.80F;
            float halfW = 0.045F * (0.5F + 0.5F * sf);
            for (int i = 0; i < 6; i++) {
                double ang = i * Math.PI / 3.0 + (s.startTime * 0.17);
                Vec3 dir = new Vec3(Math.cos(ang), 0, Math.sin(ang));
                Vec3 side = new Vec3(-Math.sin(ang), 0, Math.cos(ang)).scale(halfW);
                Vec3 p0 = c.add(dir.scale(rInner));
                Vec3 pMid = c.add(dir.scale(rOuter));
                Vec3 pTip = c.add(dir.scale(rOuter + 0.55F * ease + 0.25F));
                put(vc, m, p0, col[0], col[1], col[2], 0F);
                put(vc, m, pMid.add(side), core[0], core[1], core[2], sa);
                put(vc, m, pTip, col[0], col[1], col[2], 0F);
                put(vc, m, pMid.subtract(side), core[0], core[1], core[2], sa);
            }
        }
    }

    /** Dual horizontal holographic scan rings + 4 vertical phase ribs sweeping over a newly deployed Wingman. */
    private static void drawBodyScan(VertexConsumer vc, Matrix4f m, Vec3 feet, float height, float prog, float[] accent, boolean living) {
        float fade = prog < 0.15F ? prog / 0.15F : (prog > 0.75F ? (1F - prog) / 0.25F : 1F);
        float alpha = fade * 0.78F;
        if (alpha <= 0.01F) return;

        // Primary scan plane sweeps bottom -> top; secondary ghost ring sweeps top -> bottom
        float y1 = height * prog;
        float y2 = height * (1F - prog * 0.7F);
        float[] core = living ? WingmanDeployRules.LIVING_CORE : new float[]{0.92F, 0.98F, 1.0F};
        horizontalRing(vc, m, feet.add(0, y1, 0), 0.52F, 0.08F, core[0], core[1], core[2], alpha, 24);
        horizontalRing(vc, m, feet.add(0, y2, 0), 0.46F, 0.06F, accent[0], accent[1], accent[2], alpha * 0.65F, 24);

        // 4 vertical phase wireframe ribs flickering around the silhouette
        float ribHalf = 0.016F;
        for (int i = 0; i < 4; i++) {
            double a = i * Math.PI * 0.5 + prog * 1.4;
            Vec3 radial = new Vec3(Math.cos(a) * 0.48, 0, Math.sin(a) * 0.48);
            Vec3 tang = new Vec3(-Math.sin(a) * ribHalf, 0, Math.cos(a) * ribHalf);
            Vec3 bot = feet.add(radial);
            Vec3 top = feet.add(radial).add(0, height * Math.min(1F, prog * 1.25F), 0);
            put(vc, m, bot.subtract(tang), accent[0], accent[1], accent[2], 0F);
            put(vc, m, bot.add(tang), accent[0], accent[1], accent[2], 0F);
            put(vc, m, top.add(tang), core[0], core[1], core[2], alpha * 0.55F);
            put(vc, m, top.subtract(tang), core[0], core[1], core[2], alpha * 0.55F);
        }
    }

    private static void horizontalRing(VertexConsumer vc, Matrix4f m, Vec3 c, float r, float w,
                                       float cr, float cg, float cb, float alpha, int seg) {
        if (alpha <= 0.004F) return;
        float rIn = Math.max(0.04F, r - w);
        float rOut = r + 0.05F;
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

    private static void put(VertexConsumer vc, Matrix4f m, Vec3 p, float r, float g, float b, float a) {
        vc.addVertex(m, (float) p.x, (float) p.y, (float) p.z).setColor(r, g, b, Math.max(0F, Math.min(1F, a)));
    }

    static void clear() {
        GATES.clear();
        SHOCKS.clear();
        SCANS.clear();
        PENDING_BRAKES.clear();
        HATCH_PULSES.clear();
        RETURN_HATCHES.clear();
        RECALL_GATED.clear();
    }
}
