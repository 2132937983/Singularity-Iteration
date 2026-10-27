// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.armory;

import com.miophas.singularity_iteration.common.armory.ArmorShowcaseBlockEntity;
import com.miophas.singularity_iteration.common.armory.ArmoryPieceEntity;
import com.miophas.singularity_iteration.common.armory.mio_icif_armory;
import com.miophas.singularity_iteration.common.client.render.FxBudget;
import com.miophas.singularity_iteration.common.client.render.FxDraw;
import com.miophas.singularity_iteration.common.client.render.FxLodRules;
import com.miophas.singularity_iteration.common.client.render.RenderTypeTextures;
import com.miophas.singularity_iteration.common.client.render.SiRenderTypes;
import com.miophas.singularity_iteration.common.client.suit.FcsClientConfig;
import com.miophas.singularity_iteration.common.client.suit.nano.NanoRender;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * 0.1.10.32 (A-4): the transfer gates of the Armory. A summon from far away or from another dimension starts its pieces in the
 * sky (see {@code ArmoryFlight.launchPoint}); there a gate opens around each piece while it pressurises: a dark disc, two
 * counter-rotating violet rings, spiral arms and a flash as the piece leaves. A piece that flies home to the sky goes into a
 * gate that opens in front of it, shrinking ({@link ArmoryFxRules#swallow}). The pieces that leave a gate drag afterimages
 * ({@link #ghosts}, drawn by {@code ArmoryPieceRenderer}). The gates are kept here (not on the piece entity), so a gate closes
 * after its piece is gone. Option {@code armory_fx}.
 */
public final class GateFx {
    private GateFx() {}

    /** One gate: centre, normal (towards the flight), the game tick the piece passes it, returning (a homecoming gate). */
    private record Gate(Vec3 centre, Vec3 normal, long pass, boolean returning) { }

    private static final Int2ObjectOpenHashMap<Gate> GATES = new Int2ObjectOpenHashMap<>();

    /** True when no Armory or holo projector stands under {@code at} (a launch or home point in the sky). */
    static boolean sky(Level level, Vec3 at) {
        for (double dy : new double[]{0.6, 0.7}) {
            var be = level.getBlockEntity(BlockPos.containing(at.x, at.y - dy, at.z));
            if (be instanceof mio_icif_armory || be instanceof ArmorShowcaseBlockEntity) return false;
        }
        return true;
    }

    /** Once a client tick per piece (from its effects): opens its gate when the time comes. */
    static void tick(ArmoryPieceEntity piece) {
        if (!FcsClientConfig.armoryFx() || GATES.containsKey(piece.getId())) return;
        float age = piece.age(0);
        if (piece.isReturn()) {
            int duration = piece.duration();
            if (age < duration - ArmoryFxRules.GATE_OPEN - ArmoryFxRules.GATE_HOLD - 1 || age >= duration) return;
            Vec3 end = piece.destination(0);
            if (!sky(piece.level(), end)) return;
            Vec3 dir = end.subtract(piece.flightPosition(0));
            GATES.put(piece.getId(), new Gate(end, dir.lengthSqr() < 1.0E-6 ? new Vec3(0, 1, 0) : dir.normalize(),
                piece.launchTime() + duration, true));
        } else {
            if (age < -ArmoryFxRules.GATE_OPEN - 1 || age >= 0) return;
            Vec3 start = piece.start();
            if (!sky(piece.level(), start)) return;
            Vec3 dir = piece.destination(0).subtract(start);
            GATES.put(piece.getId(), new Gate(start, dir.lengthSqr() < 1.0E-6 ? new Vec3(0, -1, 0) : dir.normalize(), piece.launchTime(), false));
        }
    }

    /** True when {@code piece} left (or flies into) a transfer gate. */
    static boolean gated(ArmoryPieceEntity piece) {
        Gate g = GATES.get(piece.getId());
        return g != null && g.returning == piece.isReturn();
    }

    /** World pass (after particles): every open gate. */
    public static void render(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (GATES.isEmpty() || mc.level == null) return;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float now = mc.level.getGameTime() + partial;
        GATES.values().removeIf(g -> now - g.pass > ArmoryFxRules.GATE_HOLD + ArmoryFxRules.GATE_SHUT + 1);
        if (GATES.isEmpty() || !FcsClientConfig.armoryFx()) return;
        Vec3 cam = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f m = pose.last().pose();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        // the dark hole first (alpha veil), then the light (additive)
        VertexConsumer veil = buffers.getBuffer(SiRenderTypes.VAPOR);
        for (Gate g : GATES.values()) {
            float open = ArmoryFxRules.gate(now - g.pass, g.returning);
            if (open > 0.01F) hole(veil, m, g, open);
        }
        buffers.endBatch(SiRenderTypes.VAPOR);
        VertexConsumer glow = buffers.getBuffer(SiRenderTypes.FIELD_GLOW);
        Vector3f left = new Vector3f(event.getCamera().getLeftVector()), up = new Vector3f(event.getCamera().getUpVector());
        for (Gate g : GATES.values()) {
            float t = now - g.pass;
            float open = ArmoryFxRules.gate(t, g.returning);
            if (open <= 0.01F) continue;
            int tier = FxLodRules.tier(g.centre.distanceToSqr(cam), FcsClientConfig.fxLod());
            rings(glow, m, g, open, now, tier);
            float flash = g.returning ? 0F : ArmoryFxRules.flash(t);
            if (flash > 0F) FxDraw.star(glow, m, g.centre, left, up, 1.2F * flash + 0.3F, 0.85F, 0.7F, 1F, flash);
        }
        buffers.endBatch(SiRenderTypes.FIELD_GLOW);
        pose.popPose();
    }

    private static Vec3[] frame(Vec3 n) {
        Vec3 t = Math.abs(n.y) < 0.95 ? n.cross(new Vec3(0, 1, 0)).normalize() : n.cross(new Vec3(1, 0, 0)).normalize();
        return new Vec3[]{t, n.cross(t).normalize()};
    }

    private static void hole(VertexConsumer vc, Matrix4f m, Gate g, float open) {
        Vec3[] f = frame(g.normal);
        float r = ArmoryFxRules.GATE_RADIUS * open;
        int seg = 24;
        for (int s = 0; s < seg; s++) {
            double a0 = s * Math.PI * 2 / seg, a1 = (s + 1) * Math.PI * 2 / seg;
            Vec3 p0 = g.centre.add(f[0].scale(Math.cos(a0) * r)).add(f[1].scale(Math.sin(a0) * r));
            Vec3 p1 = g.centre.add(f[0].scale(Math.cos(a1) * r)).add(f[1].scale(Math.sin(a1) * r));
            vertex(vc, m, g.centre, 0.03F, 0.0F, 0.08F, 0.6F * open);
            vertex(vc, m, p0, 0.05F, 0.02F, 0.12F, 0.2F * open);
            vertex(vc, m, p1, 0.05F, 0.02F, 0.12F, 0.2F * open);
            vertex(vc, m, g.centre, 0.03F, 0.0F, 0.08F, 0.6F * open);
        }
    }

    /** Outer and inner ring turning against each other, and three spiral arms drawn into the centre. */
    private static void rings(VertexConsumer vc, Matrix4f m, Gate g, float open, float time, int tier) {
        Vec3[] f = frame(g.normal);
        float r = ArmoryFxRules.GATE_RADIUS * open;
        int seg = FxLodRules.segments(32, tier);
        for (int ring = 0; ring < 2; ring++) {
            float rr = ring == 0 ? r : r * 0.72F, w = ring == 0 ? 0.07F : 0.045F;
            float spin = (ring == 0 ? 1 : -1.6F) * time * 0.09F;
            for (int s = 0; s < seg; s++) {
                double a0 = s * Math.PI * 2 / seg, a1 = (s + 1) * Math.PI * 2 / seg;
                float dash = 0.45F + 0.55F * (float) Math.pow(Math.max(0, Math.cos(a0 * 4 + spin)), 2);
                Vec3 i0 = g.centre.add(f[0].scale(Math.cos(a0) * rr)).add(f[1].scale(Math.sin(a0) * rr));
                Vec3 i1 = g.centre.add(f[0].scale(Math.cos(a1) * rr)).add(f[1].scale(Math.sin(a1) * rr));
                Vec3 o0 = g.centre.add(f[0].scale(Math.cos(a0) * (rr + w))).add(f[1].scale(Math.sin(a0) * (rr + w)));
                Vec3 o1 = g.centre.add(f[0].scale(Math.cos(a1) * (rr + w))).add(f[1].scale(Math.sin(a1) * (rr + w)));
                float a = open * dash * (ring == 0 ? 0.9F : 0.6F);
                vertex(vc, m, i0, 0.75F, 0.55F, 1F, a);
                vertex(vc, m, i1, 0.75F, 0.55F, 1F, a);
                vertex(vc, m, o1, 0.5F, 0.35F, 1F, 0F);
                vertex(vc, m, o0, 0.5F, 0.35F, 1F, 0F);
            }
        }
        if (tier == FxLodRules.FAR) return;
        for (int arm = 0; arm < 3; arm++) {
            Vec3 prev = null;
            for (int k = 0; k <= 10; k++) {
                double u = k / 10.0;
                double ang = arm * Math.PI * 2 / 3 + u * 2.4 - time * 0.15;
                double rad = r * (1 - u);
                Vec3 p = g.centre.add(f[0].scale(Math.cos(ang) * rad)).add(f[1].scale(Math.sin(ang) * rad));
                if (prev != null) FxDraw.line(vc, m, prev, p, Minecraft.getInstance().gameRenderer.getMainCamera().getPosition(), 0.03F,
                    0.85F, 0.6F, 1F, 0.55F * open * (float) (1 - u * 0.6));
                prev = p;
            }
        }
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, Vec3 p, float r, float g, float b, float a) {
        vc.addVertex(m, (float) p.x, (float) p.y, (float) p.z).setColor(r, g, b, Math.max(0F, Math.min(1F, a)));
    }

    /**
     * Buffer source of an afterimage of a piece: every textured render type becomes the additive hologram grid in violet at
     * {@code alpha}; the glint and untextured types draw nothing.
     */
    static MultiBufferSource ghosts(MultiBufferSource delegate, float alpha) {
        return type -> {
            var texture = RenderTypeTextures.of(type);
            if (texture.isEmpty() || RenderTypeTextures.isGlint(texture.get())) return ArmorShowcaseRenderer.NOTHING;
            return new Ghost(delegate.getBuffer(NanoRender.HOLOGRID_FINE), alpha);
        };
    }

    /** Afterimage vertex: violet light at the ghost's strength. */
    static final class Ghost implements VertexConsumer {
        private final VertexConsumer d;
        private final float alpha;

        Ghost(VertexConsumer d, float alpha) {
            this.d = d;
            this.alpha = alpha;
        }

        @Override public VertexConsumer addVertex(float x, float y, float z) { d.addVertex(x, y, z); return this; }
        @Override public VertexConsumer setColor(int r, int g, int b, int a) {
            d.setColor((int) (190 * alpha), (int) (120 * alpha), (int) (255 * alpha), 255);
            return this;
        }
        @Override public VertexConsumer setUv(float u, float v) { d.setUv(u, v); return this; }
        @Override public VertexConsumer setUv1(int u, int v) { d.setUv1(u, v); return this; }
        @Override public VertexConsumer setUv2(int u, int v) { d.setUv2(u, v); return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { d.setNormal(x, y, z); return this; }
    }

    static void clear() {
        GATES.clear();
    }

    /** Detail tier of the afterimages of a piece (none beyond {@link FxLodRules#NEAR}). */
    static boolean ghostsShown(ArmoryPieceEntity piece) {
        return FxBudget.tier(piece) == FxLodRules.NEAR;
    }
}
