// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.armory;

import com.miophas.singularity_iteration.common.armory.WingmanCommandPackets.Unit;
import com.miophas.singularity_iteration.common.armory.WingmanCommands;
import com.miophas.singularity_iteration.common.armory.WingmanOrder;
import com.miophas.singularity_iteration.common.client.render.FxDraw;
import com.miophas.singularity_iteration.common.client.render.SiRenderTypes;
import com.miophas.singularity_iteration.common.client.suit.FcsClientConfig;
import com.miophas.singularity_iteration.common.suit.nano.NanoPackets;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * World effects of the quantum command (0.1.10.2), additive light after the particles.
 *
 * <ul>
 *   <li><b>Order.</b> Interference threads (a double helix, the order colour and white) run from
 *       the owner's chest to every ordered Wingman with a travelling pulse; the order point flashes
 *       with a ground ring and a short column.</li>
 *   <li><b>Markers</b> (the owner only, client option {@code order_markers}): a hold point is three
 *       stacked chevrons over a turning ground ring with the hold radius as a faint dashed circle; an
 *       area suppression is a large red ring with a radar sweep; a focus target wears turning red
 *       brackets and a diamond, with faint tethers from the Wingmen on it. A Wingman on its way to a
 *       far point shows a dashed line to it.</li>
 *   <li><b>Quantum recall.</b> The suit's column of light collapses inward, a white streak shoots
 *       up into the sky (the transfer to the Armory) and a lens star flashes.</li>
 * </ul>
 */
public final class CommandFx {
    private CommandFx() {}

    private record Order(int action, Vec3 point, int[] wingmen, int focus, int born) { }
    private record Recall(Vec3 pos, float height, int born) { }

    private static final List<Order> ORDERS = new ArrayList<>();
    private static final List<Recall> RECALLS = new ArrayList<>();

    static void clear() {
        ORDERS.clear();
        RECALLS.clear();
    }

    /** FX_ORDER (the owner only). */
    public static void order(NanoPackets.Fx fx) {
        ORDERS.add(new Order((int) fx.a(), new Vec3(fx.x(), fx.y(), fx.z()), fx.targets(), (int) fx.c(), SquadClient.ticks));
        if (ORDERS.size() > 8) ORDERS.remove(0);
    }

    /** FX_RECALL. */
    public static void recall(NanoPackets.Fx fx) {
        Vec3 pos = new Vec3(fx.x(), fx.y(), fx.z());
        RECALLS.add(new Recall(pos, Math.max(1, fx.a()), SquadClient.ticks));
        if (RECALLS.size() > 12) RECALLS.remove(0);
        var level = Minecraft.getInstance().level;
        if (level != null) {
            for (int i = 0; i < 24; i++) {
                double a = i * Math.PI * 2 / 24;
                level.addParticle(ParticleTypes.END_ROD, pos.x + Math.cos(a) * 0.5, pos.y + level.random.nextDouble() * 1.8, pos.z + Math.sin(a) * 0.5,
                    -Math.cos(a) * 0.05, 0.25 + level.random.nextDouble() * 0.2, -Math.sin(a) * 0.05);
            }
        }
    }

    static void tick() {
        int now = SquadClient.ticks;
        ORDERS.removeIf(o -> now - o.born > 30);
        RECALLS.removeIf(r -> now - r.born > 26);
    }

    static float[] color(int action) {
        return switch (action) {
            case WingmanCommands.A_MOVE -> new float[]{0.36F, 0.92F, 1.0F};
            case WingmanCommands.A_HOLD -> new float[]{1.0F, 0.76F, 0.3F};
            case WingmanCommands.A_SUPPRESS -> new float[]{1.0F, 0.42F, 0.3F};
            case WingmanCommands.A_FOCUS -> new float[]{1.0F, 0.3F, 0.43F};
            case WingmanCommands.A_FORM_UP -> new float[]{0.72F, 0.85F, 1.0F};
            default -> new float[]{0.73F, 0.55F, 1.0F};
        };
    }

    static float[] orderColor(WingmanOrder o) {
        return switch (o) {
            case MOVE -> color(WingmanCommands.A_MOVE);
            case HOLD -> color(WingmanCommands.A_HOLD);
            case SUPPRESS -> color(WingmanCommands.A_SUPPRESS);
            case FOCUS -> color(WingmanCommands.A_FOCUS);
            case NONE -> color(-1);
        };
    }

    /** Render pass (AFTER_PARTICLES). */
    public static void render(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        boolean markers = FcsClientConfig.orderMarkers() && SquadClient.commander() && !SquadClient.units().isEmpty();
        if (ORDERS.isEmpty() && RECALLS.isEmpty() && !markers) return;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float t = SquadClient.ticks + partial;
        Vec3 cam = event.getCamera().getPosition();
        Vector3f left = event.getCamera().getLeftVector(), up = event.getCamera().getUpVector();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buffers.getBuffer(SiRenderTypes.FIELD_GLOW);
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f m = pose.last().pose();
        float flash = FcsClientConfig.flashScale();
        for (Order o : ORDERS) orderGlow(vc, m, o, cam, t, partial, mc, flash);
        if (markers) markers(vc, m, cam, left, up, t, partial, mc);
        for (Recall r : RECALLS) recallGlow(vc, m, r, cam, left, up, t, flash);
        buffers.endBatch(SiRenderTypes.FIELD_GLOW);
        pose.popPose();
    }

    private static void orderGlow(VertexConsumer vc, Matrix4f m, Order o, Vec3 cam, float t, float partial, Minecraft mc, float flash) {
        float age = t - o.born;
        float fade = Mth.clamp(1 - age / 30F, 0, 1);
        if (fade <= 0) return;
        float[] c = color(o.action);
        Vec3 chest = mc.player.getPosition(partial).add(0, 1.2, 0);
        for (int id : o.wingmen) {
            Entity w = mc.level.getEntity(id);
            if (w == null) continue;
            Vec3 to = w.getPosition(partial).add(0, 1.1, 0);
            helix(vc, m, chest, to, cam, c, fade, age);
        }
        boolean point = o.action == WingmanCommands.A_MOVE || o.action == WingmanCommands.A_SUPPRESS;
        if (point) {
            float k = Mth.clamp(age / 10F, 0, 1);
            Vec3 g = o.point.add(0, 0.05, 0);
            FxDraw.ring(vc, m, g, new Vec3(0, 1, 0), 0.4F + 2.6F * k, 0.25F, c[0], c[1], c[2], 0.85F * fade * flash, 40);
            FxDraw.beam(vc, m, g, g.add(0, 6 * (1 - k * 0.5F), 0), cam, 0.12F, c[0], c[1], c[2], 0.8F * fade * flash, 0);
        }
        if (o.action == WingmanCommands.A_FOCUS) {
            Entity e = mc.level.getEntity(o.focus);
            if (e != null) {
                float snap = 1 + Mth.clamp(1 - age / 6F, 0, 1) * 1.5F;
                Vec3 cc = e.getPosition(partial).add(0, e.getBbHeight() * 0.5, 0);
                FxDraw.ring(vc, m, cc, cc.subtract(cam), (e.getBbWidth() + 0.6F) * snap, 0.12F, c[0], c[1], c[2], 0.9F * fade, 36);
            }
        }
    }

    /** Two strands winding around the line from a to b; a pulse runs along them. */
    private static void helix(VertexConsumer vc, Matrix4f m, Vec3 a, Vec3 b, Vec3 cam, float[] c, float fade, float age) {
        Vec3 d = b.subtract(a);
        double len = d.length();
        if (len < 0.3) return;
        Vec3 axis = d.scale(1 / len), n1 = FxDraw.perp(axis), n2 = axis.cross(n1).normalize();
        int steps = Mth.clamp((int) (len * 3), 8, 90);
        float head = Mth.clamp(age / 10F, 0, 1);                       // the threads reach the Wingman in half a second
        for (int strand = 0; strand < 2; strand++) {
            Vec3 prev = null;
            for (int i = 0; i <= steps; i++) {
                float u = i / (float) steps;
                if (u > head) break;
                double ph = u * len * 2.2 - age * 0.5 + strand * Math.PI;
                double amp = 0.18 * Math.sin(u * Math.PI);
                Vec3 p = a.add(d.scale(u)).add(n1.scale(Math.cos(ph) * amp)).add(n2.scale(Math.sin(ph) * amp));
                if (prev != null) {
                    float pulse = 0.5F + 0.5F * (float) Math.max(0, Math.cos((u - head) * 18));
                    float al = fade * pulse * (strand == 0 ? 0.9F : 0.5F);
                    if (strand == 0) FxDraw.line(vc, m, prev, p, cam, 0.045F, c[0], c[1], c[2], al);
                    else FxDraw.line(vc, m, prev, p, cam, 0.03F, 1F, 1F, 1F, al);
                }
                prev = p;
            }
        }
    }

    /** Hold points, suppression rings, focus brackets and travel lines of the owner's squad. */
    private static void markers(VertexConsumer vc, Matrix4f m, Vec3 cam, Vector3f leftV, Vector3f upV, float t, float partial, Minecraft mc) {
        Vec3 l = new Vec3(leftV.x(), leftV.y(), leftV.z()), u = new Vec3(upV.x(), upV.y(), upV.z());
        Map<Long, Vec3> drawn = new HashMap<>();
        for (Unit unit : SquadClient.units()) {
            if (unit.where() != 0) continue;
            WingmanOrder o = SquadClient.order(unit);
            float[] c = orderColor(o);
            Vec3 at = SquadClient.position(unit, partial);
            if (o.anchored()) {
                Vec3 p = SquadClient.orderPoint(unit);
                if (p == null || p.distanceToSqr(cam) > 256 * 256) continue;
                if (at.distanceToSqr(p) > 7 * 7) dashedLine(vc, m, at.add(0, 1, 0), p.add(0, 1.5, 0), cam, c, 0.35F, t);
                long key = Double.doubleToLongBits(p.x) * 31 + Double.doubleToLongBits(p.z) * 17 + o.ordinal();
                if (drawn.put(key, p) != null) continue;                       // one marker per point
                if (o == WingmanOrder.SUPPRESS) suppressMarker(vc, m, p, cam, c, t); else holdMarker(vc, m, p, cam, l, u, c, t, o);
            } else if (o == WingmanOrder.FOCUS) {
                Entity e = mc.level.getEntity(unit.focus());
                if (!(e instanceof LivingEntity target) || !target.isAlive()) continue;
                Vec3 tc = target.getPosition(partial).add(0, target.getBbHeight() * 0.55, 0);
                FxDraw.line(vc, m, at.add(0, 1.2, 0), tc, cam, 0.015F, c[0], c[1], c[2], 0.22F);
                long key = -1 - unit.focus();
                if (drawn.put(key, tc) != null) continue;
                focusMarker(vc, m, target, tc, cam, l, u, c, t, partial);
            }
        }
    }

    private static void holdMarker(VertexConsumer vc, Matrix4f m, Vec3 p, Vec3 cam, Vec3 l, Vec3 u, float[] c, float t, WingmanOrder o) {
        Vec3 g = p.add(0, 0.06, 0);
        FxDraw.ring(vc, m, g, new Vec3(0, 1, 0), 1.0F, 0.18F, c[0], c[1], c[2], 0.7F, 36);
        for (int k = 0; k < 6; k++) {
            double a = t * 0.04 + k * Math.PI / 3;
            Vec3 dir = new Vec3(Math.cos(a), 0, Math.sin(a));
            FxDraw.line(vc, m, g.add(dir.scale(1.25)), g.add(dir.scale(1.6)), cam, 0.05F, c[0], c[1], c[2], 0.8F);
        }
        // the hold radius as a faint dashed circle
        int r = SquadClient.holdRadius();
        int seg = 48;
        for (int k = 0; k < seg; k += 2) {
            double a0 = k * Math.PI * 2 / seg + t * 0.005, a1 = (k + 1) * Math.PI * 2 / seg + t * 0.005;
            FxDraw.line(vc, m, g.add(Math.cos(a0) * r, 0, Math.sin(a0) * r), g.add(Math.cos(a1) * r, 0, Math.sin(a1) * r), cam, 0.06F, c[0], c[1], c[2], 0.16F);
        }
        // three stacked chevrons pointing down at the point, bobbing; a faint column
        float bob = 0.15F * Mth.sin(t * 0.12F);
        Vec3 flat = new Vec3(l.x, 0, l.z);
        if (flat.lengthSqr() < 1.0E-4) flat = new Vec3(1, 0, 0);
        flat = flat.normalize();
        for (int k = 0; k < 3; k++) {
            Vec3 tip = p.add(0, 2.4 + k * 0.45 + bob, 0);
            float a = 0.95F - k * 0.25F;
            FxDraw.line(vc, m, tip.add(flat.scale(0.45)).add(0, 0.38, 0), tip, cam, 0.07F, c[0], c[1], c[2], a);
            FxDraw.line(vc, m, tip.subtract(flat.scale(0.45)).add(0, 0.38, 0), tip, cam, 0.07F, c[0], c[1], c[2], a);
        }
        FxDraw.beam(vc, m, g, g.add(0, 2.3, 0), cam, 0.05F, c[0], c[1], c[2], 0.45F, 0.05F);
    }

    private static void suppressMarker(VertexConsumer vc, Matrix4f m, Vec3 p, Vec3 cam, float[] c, float t) {
        Vec3 g = p.add(0, 0.08, 0);
        float r = SquadClient.suppressRadius();
        FxDraw.ring(vc, m, g, new Vec3(0, 1, 0), r - 0.35F, 0.35F, c[0], c[1], c[2], 0.55F, 96);
        FxDraw.ring(vc, m, g, new Vec3(0, 1, 0), r * 0.5F, 0.12F, c[0], c[1], c[2], 0.25F, 64);
        // radar sweep: a bright spoke with a fading wake
        double sweep = t * 0.06;
        for (int k = 0; k < 10; k++) {
            double a = sweep - k * 0.06;
            Vec3 dir = new Vec3(Math.cos(a), 0, Math.sin(a));
            FxDraw.line(vc, m, g, g.add(dir.scale(r)), cam, 0.08F, c[0], c[1], c[2], 0.55F * (1 - k / 10F));
        }
        FxDraw.beam(vc, m, g, g.add(0, 4, 0), cam, 0.1F, c[0], c[1], c[2], 0.6F, 0);
    }

    private static void focusMarker(VertexConsumer vc, Matrix4f m, LivingEntity target, Vec3 tc, Vec3 cam, Vec3 l, Vec3 u, float[] c, float t,
                                    float partial) {
        float size = Math.max(target.getBbWidth(), target.getBbHeight()) * 0.7F + 0.3F;
        float spin = -t * 0.06F;
        for (int i = 0; i < 4; i++) {
            double a = spin + i * Math.PI / 2;
            Vec3 radial = l.scale(Math.cos(a)).add(u.scale(Math.sin(a)));
            Vec3 tangent = l.scale(-Math.sin(a)).add(u.scale(Math.cos(a)));
            Vec3 corner = tc.add(radial.scale(size));
            FxDraw.line(vc, m, corner, corner.subtract(radial.scale(size * 0.3)).add(tangent.scale(size * 0.25)), cam, 0.04F, c[0], c[1], c[2], 0.9F);
            FxDraw.line(vc, m, corner, corner.subtract(radial.scale(size * 0.3)).subtract(tangent.scale(size * 0.25)), cam, 0.04F, c[0], c[1], c[2], 0.9F);
        }
        Vec3 top = target.getPosition(partial).add(0, target.getBbHeight() + 0.6 + 0.08 * Mth.sin(t * 0.25F), 0);
        float d = 0.18F;
        Vec3 n = top.add(u.scale(d)), s = top.subtract(u.scale(d)), e = top.add(l.scale(d)), w = top.subtract(l.scale(d));
        FxDraw.line(vc, m, n, e, cam, 0.04F, c[0], c[1], c[2], 1F);
        FxDraw.line(vc, m, e, s, cam, 0.04F, c[0], c[1], c[2], 1F);
        FxDraw.line(vc, m, s, w, cam, 0.04F, c[0], c[1], c[2], 1F);
        FxDraw.line(vc, m, w, n, cam, 0.04F, c[0], c[1], c[2], 1F);
        FxDraw.line(vc, m, n, s, cam, 0.03F, 1F, 1F, 1F, 0.7F);
    }

    private static void dashedLine(VertexConsumer vc, Matrix4f m, Vec3 a, Vec3 b, Vec3 cam, float[] c, float alpha, float t) {
        Vec3 d = b.subtract(a);
        double len = d.length();
        if (len < 0.5) return;
        int n = Math.min(120, (int) (len / 0.8));
        double shift = (t * 0.08) % 1.0;
        for (int i = 0; i < n; i++) {
            double u0 = (i + shift) / n, u1 = (i + shift + 0.45) / n;
            if (u1 > 1) break;
            FxDraw.line(vc, m, a.add(d.scale(u0)), a.add(d.scale(u1)), cam, 0.04F, c[0], c[1], c[2], alpha);
        }
    }

    private static void recallGlow(VertexConsumer vc, Matrix4f m, Recall r, Vec3 cam, Vector3f left, Vector3f up, float t, float flash) {
        float age = t - r.born;
        if (age < 0 || age > 26) return;
        float[] v = {0.73F, 0.55F, 1.0F};
        Vec3 base = r.pos;
        if (age < 10) {
            // the column collapses inward, scan rings sweep it
            float k = age / 10F;
            float rad = 0.6F * (1 - k * k);
            for (int i = 0; i < 3; i++) {
                float y = (float) ((age * 0.25 + i * 0.33) % 1.0) * r.height;
                FxDraw.ring(vc, m, base.add(0, y, 0), new Vec3(0, 1, 0), rad + 0.05F, 0.12F, v[0], v[1], v[2], 0.8F * (1 - k), 28);
            }
            FxDraw.beam(vc, m, base, base.add(0, r.height, 0), cam, 0.9F * (1 - k) + 0.1F, 0.9F, 0.85F, 1.0F, 0.5F * flash, 0.5F * flash);
        }
        // the transfer streak: up into the sky, then it thins out
        float s = Mth.clamp((age - 6) / 6F, 0, 1);
        float fade = Mth.clamp(1 - (age - 12) / 14F, 0, 1);
        if (s > 0 && fade > 0) {
            Vec3 top = base.add(0, r.height * 0.6 + 40 * s, 0);
            FxDraw.beam(vc, m, base.add(0, r.height * 0.6, 0), top, cam, 0.22F * fade, 1F, 1F, 1F, 0.9F * fade * flash, 0.1F * fade);
        }
        if (age > 5 && age < 13) {
            float f = 1 - Math.abs(age - 9) / 4F;
            FxDraw.star(vc, m, base.add(0, r.height * 0.6, 0), left, up, 1.4F * f, v[0], v[1], v[2], 0.9F * f * flash);
        }
    }
}
