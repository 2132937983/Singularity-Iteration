// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.client.render.SiRenderTypes;
import com.miophas.singularity_iteration.common.menu.tool.MeterHudPacket;
import com.miophas.singularity_iteration.common.suit.SuitModuleType;
import com.miophas.singularity_iteration.common.suit.SuitPackets;
import com.miophas.singularity_iteration.common.suit.SuitSensorData;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

/**
 * Client controller of the FCS: key bindings, per-tick sensor work, and the world pass
 * (sensor wireframes, ballistic path, predicted tracks, blast zones).
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
@SuppressWarnings("null")
public final class FcsClient {
    private FcsClient() {}

    public static KeyMapping HUD_KEY;
    public static KeyMapping LAYOUT_KEY;
    private static int tick;
    private static BlockPos telemetryPos;

    @SubscribeEvent
    public static void onKeys(RegisterKeyMappingsEvent event) {
        HUD_KEY = new KeyMapping("key.mio_icif.fcs_hud", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, "key.category.mio_icif");
        LAYOUT_KEY = new KeyMapping("key.mio_icif.hud_layout", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), "key.category.mio_icif");
        event.register(HUD_KEY);
        event.register(LAYOUT_KEY);
    }

    @SubscribeEvent
    public static void onLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CROSSHAIR, ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "fcs_hud"), FcsHud::render);
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        FcsState.clear();
        SensorTracker.clear();
        OreScanner.reset();
        SuitSensorData.clear();
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            FcsState.clear();
            return;
        }
        while (HUD_KEY != null && HUD_KEY.consumeClick()) {
            boolean next = !FcsClientConfig.hudEnabled();
            if (FcsClientConfig.loaded()) {
                FcsClientConfig.HUD_ENABLED.set(next);
                FcsClientConfig.save();
            }
            player.displayClientMessage(Component.translatable(next ? "hud.mio_icif.fcs.on" : "hud.mio_icif.fcs.off"), true);
        }
        while (LAYOUT_KEY != null && LAYOUT_KEY.consumeClick()) {
            if (mc.screen == null) mc.setScreen(new HudLayoutScreen(null));
        }
        tick++;
        FcsState.update(player);
        boolean hud = FcsClientConfig.hudEnabled();
        boolean esp = hud && FcsState.active(SuitModuleType.ENTITY_ESP);
        boolean predict = hud && FcsState.active(SuitModuleType.BEHAVIOR_PREDICTOR);
        boolean blast = hud && FcsState.active(SuitModuleType.BLAST_WARNING);
        SensorTracker.tick(mc.level, player, esp, predict, blast);
        updateGlow(esp);

        if (hud && FcsState.active(SuitModuleType.ORE_SCANNER)) OreScanner.tick(mc.level, player.blockPosition());
        else OreScanner.reset();

        if (hud && FcsState.active(SuitModuleType.HOLOMAP)) HoloMap.tick(mc.level, player);

        FcsState.ballistic = null;
        FcsState.leadTarget = null;
        FcsState.leadPoint = null;
        if (hud && FcsState.active(SuitModuleType.BALLISTIC)) solveBallistics(mc, player);

        if (hud && FcsState.active(SuitModuleType.GRID_TELEMETRY)) {
            telemetryPos = null;
            if (mc.hitResult instanceof BlockHitResult bhr && bhr.getType() == HitResult.Type.BLOCK
                    && mc.level.getBlockEntity(bhr.getBlockPos()) != null
                    && bhr.getBlockPos().distToCenterSqr(player.position()) < 24 * 24) {
                telemetryPos = bhr.getBlockPos();
                if (tick % 10 == 0) send(new MeterHudPacket.Request(telemetryPos));
            }
            if (tick % 20 == 5) send(new SuitPackets.ChunkGridRequest());
        } else {
            telemetryPos = null;
        }
    }

    private static void send(net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null && connection.hasChannel(payload.type())) PacketDistributor.sendToServer(payload);
    }

    public static BlockPos telemetryPos() { return telemetryPos; }

    private static void updateGlow(boolean esp) {
        FcsState.GLOW.clear();
        FcsState.GLOW_COLOR.clear();
        if (!esp || !FcsClientConfig.outlines()) return;
        for (SensorTracker.Track t : SensorTracker.tracks()) {
            if (t.entity().distanceToSqr(Minecraft.getInstance().player) > SensorTracker.ESP_RANGE * SensorTracker.ESP_RANGE) continue;
            int threat = SuitSensorData.threatLevel(t.entity().getId());
            int color = threat == 2 ? FcsDraw.RED : threat == 1 ? FcsDraw.YELLOW : SensorTracker.color(t.kind());
            FcsState.GLOW.add(t.entity().getId());
            FcsState.GLOW_COLOR.put(t.entity().getId(), color);
        }
    }

    private static void solveBallistics(Minecraft mc, LocalPlayer player) {
        var stack = player.getMainHandItem();
        BallisticSolver.Profile profile = BallisticSolver.profile(player, stack);
        if (profile == null) {
            stack = player.getOffhandItem();
            profile = BallisticSolver.profile(player, stack);
        }
        if (profile == null) return;
        FcsState.ballistic = BallisticSolver.solve(player, profile);
        // lead: the creature nearest the aim line within 8 degrees and 96 m
        Vec3 eye = player.getEyePosition(), look = player.getViewVector(1.0F);
        LivingEntity best = null;
        double bestDot = Math.cos(Math.toRadians(8));
        for (SensorTracker.Track t : SensorTracker.tracks()) {
            Vec3 to = t.entity().getBoundingBox().getCenter().subtract(eye);
            double d = to.length();
            if (d < 2 || d > 96) continue;
            double dot = look.dot(to.scale(1 / d));
            if (dot > bestDot) { bestDot = dot; best = t.entity(); }
        }
        if (best == null) {
            for (Entity e : mc.level.getEntities(player, player.getBoundingBox().inflate(96), e -> e instanceof LivingEntity && e.isAlive())) {
                Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
                double d = to.length();
                if (d < 2) continue;
                double dot = look.dot(to.scale(1 / d));
                if (dot > bestDot) { bestDot = dot; best = (LivingEntity) e; }
            }
        }
        if (best != null) {
            Vec3 v = best.position().subtract(best.xo, best.yo, best.zo);
            if (best.onGround()) v = new Vec3(v.x, 0, v.z);
            FcsState.leadTarget = best;
            FcsState.leadPoint = BallisticSolver.lead(player, profile, best, v);
        }
    }

    // ------------------------------------------------------------------ world pass

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            FcsProjection.capture(new Matrix4f(event.getProjectionMatrix()), new Matrix4f(event.getModelViewMatrix()),
                event.getCamera().getPosition());
            if (!FcsClientConfig.hudEnabled() || FcsState.activeSet().isEmpty()) return;
            renderSensors(event, mc);
        } else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            ShieldFieldRenderer.render(event);
            ViltrumFlightClient.renderWorld(event);
        }
    }

    private static void renderSensors(RenderLevelStageEvent event, Minecraft mc) {
        Vec3 cam = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float time = (mc.level.getGameTime() % 40 + partial) / 40F;
        float pulse = 0.8F + 0.2F * (float) Math.sin(time * Math.PI * 2);

        if (FcsState.active(SuitModuleType.ORE_SCANNER)) {
            sweepRing(pose, buffers, cam);
        }

        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        VertexConsumer lines = buffers.getBuffer(SiRenderTypes.XRAY_LINES);
        int primary = FcsClientConfig.primary();

        if (FcsState.active(SuitModuleType.BEHAVIOR_PREDICTOR)) {
            for (SensorTracker.Track t : SensorTracker.tracks()) {
                int c = t.intentHot() ? FcsDraw.RED : SensorTracker.color(t.kind());
                List<Vec3> path = t.path();
                for (int i = 1; i < path.size(); i++) {
                    float a = 0.95F - 0.6F * i / path.size();
                    seg(pose, lines, path.get(i - 1).add(0, 0.1, 0), path.get(i).add(0, 0.1, 0), c, a);
                }
                if (!path.isEmpty()) cross(pose, lines, path.get(path.size() - 1).add(0, 0.1, 0), 0.25, c, 0.9F);
                if ("AIM".equals(t.intent()) || "BEAM".equals(t.intent()) || "FIRE".equals(t.intent())) {
                    Vec3 eye = t.entity().getEyePosition(partial);
                    Vec3 end = eye.add(t.entity().getViewVector(partial).scale(16));
                    seg(pose, lines, eye, end, FcsDraw.RED, 0.75F);
                }
            }
        }

        if (FcsState.active(SuitModuleType.BALLISTIC) && FcsState.ballistic != null) {
            BallisticSolver.Solution s = FcsState.ballistic;
            int c = s.profile().ready() ? FcsDraw.RED : 0xFF9F1C;
            List<Vec3> path = s.path();
            for (int i = 2; i < path.size(); i++) {
                if (i % 2 == 0) seg(pose, lines, path.get(i - 1), path.get(i), c, 0.85F);
            }
            if (s.impact() != null) {
                Vec3 n = s.normal() != null ? s.normal() : new Vec3(0, 1, 0);
                ring(pose, lines, s.impact().add(n.scale(0.02)), n, 0.45, c, 0.95F);
                ring(pose, lines, s.impact().add(n.scale(0.02)), n, 0.18 + 0.1 * pulse, c, 0.8F);
            }
            if (s.target() != null) {
                AABB box = s.target().getBoundingBox().inflate(0.08);
                LevelRenderer.renderLineBox(pose, lines, box, 1F, 0.23F, 0.19F, 1F);
            }
            if (FcsState.leadPoint != null && FcsState.leadTarget != null) {
                Vec3 from = FcsState.leadTarget.getBoundingBox().getCenter();
                seg(pose, lines, from, FcsState.leadPoint, 0xFFD23F, 0.8F);
                cross(pose, lines, FcsState.leadPoint, 0.3, 0xFFD23F, 1F);
            }
        }

        if (FcsState.active(SuitModuleType.BLAST_WARNING)) {
            for (SensorTracker.Blast b : SensorTracker.blasts()) {
                Vec3 at = b.entity().getPosition(partial).add(0, 0.05, 0);
                float flash = b.seconds() < 1.0F ? (float) (0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 60.0)) : pulse;
                ring(pose, lines, at, new Vec3(0, 1, 0), b.radius(), FcsDraw.RED, 0.9F * flash);
                ring(pose, lines, at, new Vec3(0, 1, 0), b.radius() * 2, 0xFFD23F, 0.45F * flash);
            }
        }

        if (FcsState.active(SuitModuleType.GRID_TELEMETRY) && telemetryPos != null) {
            float r = ((primary >> 16) & 255) / 255F, g = ((primary >> 8) & 255) / 255F, b = (primary & 255) / 255F;
            LevelRenderer.renderLineBox(pose, lines, new AABB(telemetryPos).inflate(0.01), r, g, b, 0.9F);
        }
        buffers.endBatch(SiRenderTypes.XRAY_LINES);
        pose.popPose();
    }

    /** Expanding horizontal ring at the start of each ore sweep. */
    private static void sweepRing(PoseStack pose, MultiBufferSource.BufferSource buffers, Vec3 cam) {
        long age = System.currentTimeMillis() - OreScanner.sweepStartMs();
        if (age < 0 || age > 1400) return;
        float f = age / 1400F;
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        VertexConsumer lines = buffers.getBuffer(SiRenderTypes.XRAY_LINES);
        Vec3 c = Vec3.atBottomCenterOf(OreScanner.centre());
        ring(pose, lines, c, new Vec3(0, 1, 0), OreScanner.RADIUS * f, 0x3FD4FF, 0.9F * (1 - f));
        ring(pose, lines, c.add(0, -0.5, 0), new Vec3(0, 1, 0), OreScanner.RADIUS * f * 0.92, 0x3FD4FF, 0.5F * (1 - f));
        buffers.endBatch(SiRenderTypes.XRAY_LINES);
        pose.popPose();
    }

    // ------------------------------------------------------------------ line primitives

    static void seg(PoseStack pose, VertexConsumer vc, Vec3 a, Vec3 b, int rgb, float alpha) {
        Vec3 n = b.subtract(a);
        double len = n.length();
        if (len < 1.0E-4) return;
        n = n.scale(1 / len);
        var last = pose.last();
        float r = ((rgb >> 16) & 255) / 255F, g = ((rgb >> 8) & 255) / 255F, bl = (rgb & 255) / 255F;
        vc.addVertex(last, (float) a.x, (float) a.y, (float) a.z).setColor(r, g, bl, alpha).setNormal(last, (float) n.x, (float) n.y, (float) n.z);
        vc.addVertex(last, (float) b.x, (float) b.y, (float) b.z).setColor(r, g, bl, alpha).setNormal(last, (float) n.x, (float) n.y, (float) n.z);
    }

    static void cross(PoseStack pose, VertexConsumer vc, Vec3 c, double s, int rgb, float alpha) {
        seg(pose, vc, c.add(-s, 0, 0), c.add(s, 0, 0), rgb, alpha);
        seg(pose, vc, c.add(0, -s, 0), c.add(0, s, 0), rgb, alpha);
        seg(pose, vc, c.add(0, 0, -s), c.add(0, 0, s), rgb, alpha);
    }

    /** Circle of radius r around c in the plane with normal n. */
    static void ring(PoseStack pose, VertexConsumer vc, Vec3 c, Vec3 n, double r, int rgb, float alpha) {
        Vec3 t = Math.abs(n.y) < 0.9 ? n.cross(new Vec3(0, 1, 0)).normalize() : n.cross(new Vec3(1, 0, 0)).normalize();
        Vec3 b = n.cross(t).normalize();
        int steps = Math.max(16, (int) (r * 12));
        Vec3 prev = null;
        for (int i = 0; i <= steps; i++) {
            double a = i * Math.PI * 2 / steps;
            Vec3 p = c.add(t.scale(Math.cos(a) * r)).add(b.scale(Math.sin(a) * r));
            if (prev != null) seg(pose, vc, prev, p, rgb, alpha);
            prev = p;
        }
    }
}
