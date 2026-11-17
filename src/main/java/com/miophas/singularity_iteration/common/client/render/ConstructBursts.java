// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.render;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/**
 * 0.1.11.63 (hit feel and constructivist FX phase Q2): the constructivist bursts: flat rings that grow and are cut, in
 * place of dust, sparks, vapour and shock shells. A take-off is a small white ring on the ground, a landing a larger one
 * with a red ring inside, the sound barrier a large white ring across the flight with a black one behind it. Drawn after
 * the particles into {@link SiRenderTypes#FLAT}.
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
public final class ConstructBursts {
    private ConstructBursts() {}

    private record Burst(Vec3 at, Vec3 normal, float from, float to, float width, long start, int life, float[] outer, float[] inner, int follow) { }

    private static final List<Burst> BURSTS = new ArrayList<>();

    private static long now() {
        var level = Minecraft.getInstance().level;
        return level == null ? 0 : level.getGameTime();
    }

    private static void add(Burst b) {
        BURSTS.add(b);
        if (BURSTS.size() > 32) BURSTS.remove(0);
    }

    /** A take-off ({@code takeoff}) or a landing at {@code at} on the ground, {@code strength} 0..1. */
    public static void ground(Vec3 at, float strength, boolean takeoff) {
        float k = 0.5F + 0.5F * Math.min(1, Math.max(0, strength));
        add(new Burst(at.add(0, 0.04, 0), new Vec3(0, 1, 0), 0.3F, (takeoff ? 1.4F : 2.4F) * k, takeoff ? 0.12F : 0.18F, now(), takeoff ? 5 : 7,
            ConstructFx.WHITE, takeoff ? null : ConstructFx.RED, -1));
    }

    /**
     * The sound barrier at {@code centre}, across the flight {@code axis}. The ring rides with the flyer {@code entityId}
     * (it would be behind a chase camera a few ticks later: past the barrier a flyer covers 2.4 blocks a tick).
     */
    public static void boom(Vec3 centre, Vec3 axis, int entityId) {
        add(new Burst(centre, axis, 0.8F, 6.5F, 0.35F, now(), 9, ConstructFx.WHITE, ConstructFx.BLACK, entityId));
    }

    @SubscribeEvent
    static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || BURSTS.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float t = now() + partial;
        Vec3 cam = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f m = pose.last().pose();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buffers.getBuffer(SiRenderTypes.FLAT);
        for (Iterator<Burst> it = BURSTS.iterator(); it.hasNext(); ) {
            Burst b = it.next();
            float age = (t - b.start) / b.life;
            if (age >= 1 || age < -0.5F) { it.remove(); continue; }
            float k = Math.max(0, age);
            float r = b.from + (b.to - b.from) * (1 - (1 - k) * (1 - k));   // fast out, then slower: a hard shape
            Vec3 at = b.at;
            if (b.follow >= 0 && mc.level != null && mc.level.getEntity(b.follow) instanceof net.minecraft.world.entity.Entity e)
                at = e.getPosition(partial).add(0, e.getBbHeight() * 0.5, 0).subtract(b.normal.normalize().scale(0.6));
            if (b.inner != null) ConstructFx.ring(vc, m, at, b.normal, r * 0.72F, b.width * 0.7F, b.inner, 1F, 40);
            ConstructFx.ring(vc, m, at, b.normal, r, b.width, b.outer, 1F, 48);
        }
        buffers.endBatch(SiRenderTypes.FLAT);
        pose.popPose();
    }
}
