// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.builder;

import com.miophas.singularity_iteration.common.builder.RtsDroneAssist;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * RTS Building drone visuals (0.1.8.0). Up to {@link #MAX_DRONES} drones per player launch from
 * the player, fly to the work sites that the server reports ({@link RtsDroneAssist.Sites}),
 * show the placement beam and sparks for {@link #WORK_TICKS} ticks per site, and fly back and
 * fold away after {@link #IDLE_TICKS} ticks without work. Client only; no server state.
 */
@OnlyIn(Dist.CLIENT)
public final class RtsDroneVisuals {
    private RtsDroneVisuals() {}

    static final int MAX_DRONES = 3, MAX_QUEUE = 24, WORK_TICKS = 6, IDLE_TICKS = 60;
    static final double SPEED = 0.9;

    private static final class Drone {
        Vec3 pos, prev;
        BlockPos site;
        int work, idle;
        boolean returning;
        Drone(Vec3 at) { pos = prev = at; }
    }

    private static final class Swarm {
        final List<Drone> drones = new ArrayList<>();
        final ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        ItemStack cargo = ItemStack.EMPTY;
    }

    private static final Map<Integer, Swarm> SWARMS = new HashMap<>();

    /** Installed by the client setup: called on the client thread for each received packet. */
    public static void accept() {
        RtsDroneAssist.Sites sites = RtsDroneAssist.LAST;
        if (sites == null) return;
        Swarm s = SWARMS.computeIfAbsent(sites.playerId(), k -> new Swarm());
        s.cargo = sites.mining() ? ItemStack.EMPTY : BlueprintBuilderRenderer.cargo(sites.itemId());
        for (BlockPos p : sites.sites()) {
            s.queue.addLast(p);
            while (s.queue.size() > MAX_QUEUE) s.queue.pollFirst();
        }
    }

    public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.isPaused()) { if (mc.level == null) SWARMS.clear(); return; }
        Iterator<Map.Entry<Integer, Swarm>> it = SWARMS.entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            Entity owner = mc.level.getEntity(e.getKey());
            Swarm s = e.getValue();
            if (owner == null) { it.remove(); continue; }
            Vec3 home = owner.position().add(0, owner.getBbHeight() + 0.6, 0);
            // launch one more drone while there is more work than drones
            if (!s.queue.isEmpty() && s.drones.size() < MAX_DRONES && s.queue.size() > s.drones.size()) s.drones.add(new Drone(home));
            Iterator<Drone> di = s.drones.iterator();
            while (di.hasNext()) {
                Drone d = di.next();
                d.prev = d.pos;
                if (d.site == null && !d.returning) {
                    d.site = s.queue.pollFirst();
                    if (d.site == null && ++d.idle > IDLE_TICKS) d.returning = true;
                    if (d.site != null) d.idle = 0;
                }
                Vec3 goal = d.returning ? home : d.site != null ? Vec3.atCenterOf(d.site).add(0, 1.1, 0) : d.pos;
                Vec3 to = goal.subtract(d.pos);
                double dist = to.length();
                if (dist > 0.05) d.pos = d.pos.add(to.scale(Math.min(1, SPEED / dist)));
                if (d.returning && dist < 0.6) { di.remove(); continue; }
                if (d.site != null && dist < 0.3) {
                    if (d.work++ == 0) {
                        Vec3 c = Vec3.atCenterOf(d.site);
                        for (int k = 0; k < 4; k++) mc.level.addParticle(ParticleTypes.ELECTRIC_SPARK, c.x, c.y + 0.4, c.z,
                            (mc.level.random.nextDouble() - 0.5) * 0.1, 0.05, (mc.level.random.nextDouble() - 0.5) * 0.1);
                    }
                    if (d.work >= WORK_TICKS) { d.site = null; d.work = 0; }
                }
            }
            if (s.drones.isEmpty() && s.queue.isEmpty()) it.remove();
        }
    }

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || SWARMS.isEmpty()) return;
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Camera camera = event.getCamera();
        Vec3 cam = camera.getPosition();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        float time = mc.level.getGameTime() + partial;
        for (Swarm s : SWARMS.values()) {
            for (int i = 0; i < s.drones.size(); i++) {
                Drone d = s.drones.get(i);
                Vec3 at = d.prev.lerp(d.pos, partial);
                int light = LevelRenderer.getLightColor(mc.level, BlockPos.containing(at));
                pose.pushPose();
                pose.translate(at.x - cam.x, at.y - cam.y, at.z - cam.z);
                Vec3 heading = d.pos.subtract(d.prev);
                BlueprintBuilderRenderer.face(pose, heading);
                BuilderDroneModel.render(pose, buffers, light, time + i * 5, d.returning ? ItemStack.EMPTY : s.cargo, mc.level);
                pose.popPose();
                if (d.site != null && d.work > 0) {
                    pose.pushPose();
                    pose.translate(-cam.x, -cam.y, -cam.z);
                    BuilderDroneModel.beam(pose, buffers, at.add(0, -0.1, 0), Vec3.atCenterOf(d.site), 1 - (float) d.work / WORK_TICKS * 0.6F);
                    pose.popPose();
                }
            }
        }
        buffers.endBatch();
    }
}
