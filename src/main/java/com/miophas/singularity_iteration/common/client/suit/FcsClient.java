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
    /** 0.1.10.0: the HUD was on last tick (the boot sequence runs on the rising edge). */
    private static boolean visorOn;
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
        event.registerAbove(VanillaGuiLayers.CROSSHAIR, ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "smart_guidance"), SmartGuidanceClient::render);
    }

    /** 0.1.8.0: projectiles of the local player feed the learned ballistic profiles. */
    @SubscribeEvent
    public static void onEntityJoin(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() && event.getEntity() instanceof net.minecraft.world.entity.projectile.Projectile p) {
            LearnedBallistics.onProjectile(p);
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        FcsState.clear();
        SensorTracker.clear();
        OreScanner.reset();
        SuitSensorData.clear();
        SmartGuidanceClient.clear();
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        // 0.1.9.1: Wingman and quantum suit effects (they read synced state; run before the HUD gates)
        com.miophas.singularity_iteration.common.client.armory.WingmanFx.tick(mc);
        if (player != null && mc.level != null) QuantumFx.tick(mc);
        if (player == null || mc.level == null) {
            FcsState.clear();
            visorOn = false;
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
        LearnedBallistics.tick(player);                    // 0.1.8.0: learn the ballistics of any fired weapon
        SmartGuidanceClient.tick(mc, player);              // 0.1.8.0: smart trajectory guidance lock frame
        FcsState.update(player);
        boolean hud = FcsClientConfig.hudEnabled();
        // 0.1.10.0: the visor boots when the HUD comes on (helmet on, HUD key, first HUD unit)
        boolean visor = hud && FcsState.anyHud();
        if (visor && !visorOn && FcsClientConfig.visorBoot()) {
            FcsHud.boot();
            play(mc, com.miophas.singularity_iteration.common.registry.mio_icif_sounds.VISOR_BOOT.get(), 1.0F, 0.5F);
        }
        visorOn = visor;
        // 0.1.10.0: a decoherent quantum core refreshes the sensors at half rate; 0.1.10.1: so does a saturated suit computer
        if ((com.miophas.singularity_iteration.common.client.suit.nano.QuantumClient.decoherent()
                || com.miophas.singularity_iteration.common.client.suit.nano.QuantumClient.saturated()) && (tick & 1) == 1) return;
        boolean esp = hud && FcsState.active(SuitModuleType.ENTITY_ESP);
        boolean predict = hud && FcsState.active(SuitModuleType.BEHAVIOR_PREDICTOR);
        boolean blast = hud && FcsState.active(SuitModuleType.BLAST_WARNING);
        SensorTracker.tick(mc.level, player, esp, predict, blast);
        updateGlow(esp);

        if (hud && FcsState.active(SuitModuleType.ORE_SCANNER)) OreScanner.tick(mc.level, player.blockPosition());
        else OreScanner.reset();

        if (hud && FcsState.active(SuitModuleType.HOLOMAP)) HoloMap.tick(mc.level, player);

        FcsState.ballistic = null;
        FcsState.spread = java.util.List.of();
        FcsState.leadTarget = null;
        FcsState.leadPoint = null;
        if (hud && FcsState.active(SuitModuleType.BALLISTIC)) solveBallistics(mc, player);
        cues(mc, player, blast);

        if (hud && FcsState.active(SuitModuleType.GRID_TELEMETRY)) {
            telemetryPos = null;
            // 0.1.10.1 Mk.II: the sensor reads along the line of sight up to 36 blocks (Mk.I: the block in reach)
            boolean mk2 = com.miophas.singularity_iteration.common.suit.SuitModules.tier(player, SuitModuleType.GRID_TELEMETRY) >= 2;
            HitResult look = mk2 ? player.pick(36, 1.0F, false) : mc.hitResult;
            double reach = mk2 ? 36 : 24;
            if (look instanceof BlockHitResult bhr && bhr.getType() == HitResult.Type.BLOCK
                    && mc.level.getBlockEntity(bhr.getBlockPos()) != null
                    && bhr.getBlockPos().distToCenterSqr(player.position()) < reach * reach) {
                telemetryPos = bhr.getBlockPos();
                if (tick % 10 == 0) send(new MeterHudPacket.Request(telemetryPos));
            }
            if (tick % 20 == 5) send(new SuitPackets.ChunkGridRequest());
        } else {
            telemetryPos = null;
        }
    }

    private static boolean wasOnTarget;
    private static int beepCooldown;

    /** Audio cues: lock chirp when the ballistic path first hits a creature, blast warning beeps. */
    private static void cues(Minecraft mc, LocalPlayer player, boolean blast) {
        boolean onTarget = FcsState.ballistic != null && FcsState.ballistic.target() != null;
        if (onTarget && !wasOnTarget) play(mc, com.miophas.singularity_iteration.common.registry.mio_icif_sounds.FCS_LOCK.get(), 1.0F, 0.45F);
        wasOnTarget = onTarget;
        if (beepCooldown > 0) beepCooldown--;
        if (!blast || beepCooldown > 0) return;
        float soonest = Float.MAX_VALUE;
        boolean close = false;
        for (SensorTracker.Blast b : SensorTracker.blasts()) {
            double d = Math.sqrt(b.entity().distanceToSqr(player));
            if (d > b.radius() * 2.5) continue;
            close = true;
            soonest = Math.min(soonest, b.timed() ? b.seconds() : 3F);   // 0.1.10.5: a charge without a timer beeps slowly
        }
        if (!close) return;
        // cadence speeds up as the fuse burns: 12 ticks apart at 3 s, 2 ticks apart near zero
        beepCooldown = Math.max(2, Math.min(12, Math.round(soonest * 4)));
        play(mc, com.miophas.singularity_iteration.common.registry.mio_icif_sounds.FCS_BLAST_BEEP.get(), soonest < 1 ? 1.25F : 1.0F, 0.5F);
    }

    private static void play(Minecraft mc, net.minecraft.sounds.SoundEvent sound, float pitch, float volume) {
        mc.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(sound, pitch, volume));
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
        double range = SensorTracker.espRange();                      // 0.1.10.1 Mk.II range
        for (SensorTracker.Track t : SensorTracker.tracks()) {
            if (t.entity().distanceToSqr(Minecraft.getInstance().player) > range * range) continue;
            int threat = SuitSensorData.threatLevel(t.entity().getId());
            int color = threat == 2 ? FcsDraw.RED : threat == 1 ? FcsDraw.YELLOW : SensorTracker.color(t.kind());
            FcsState.GLOW.add(t.entity().getId());
            FcsState.GLOW_COLOR.put(t.entity().getId(), color);
        }
        // 0.1.10.1 Threat Sight synergy: every creature that hunts the wearer glows red through walls, out of ESP range too
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && com.miophas.singularity_iteration.common.suit.quantum.QuantumLinks.active(player,
                com.miophas.singularity_iteration.common.suit.quantum.QuantumSynergy.THREAT_SIGHT)) {
            for (var e : SuitSensorData.threats().int2ByteEntrySet()) {
                if (e.getByteValue() < 2) continue;
                FcsState.GLOW.add(e.getIntKey());
                FcsState.GLOW_COLOR.put(e.getIntKey(), FcsDraw.RED);
            }
        }
    }

    /** True when {@code stack} has the multishot enchantment. */
    private static boolean multishot(LocalPlayer player, net.minecraft.world.item.ItemStack stack) {
        try {
            var holder = player.level().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                .getHolderOrThrow(net.minecraft.world.item.enchantment.Enchantments.MULTISHOT);
            return net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(holder, stack) > 0;
        } catch (RuntimeException e) {
            return false;
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
        // 0.1.10.1 Mk.II: the two side shots of a multishot weapon (vanilla spread: 10 degrees)
        FcsState.spread = java.util.List.of();
        if (com.miophas.singularity_iteration.common.suit.SuitModules.tier(player, SuitModuleType.BALLISTIC) >= 2 && multishot(player, stack)) {
            FcsState.spread = java.util.List.of(BallisticSolver.solve(player, profile, -10F), BallisticSolver.solve(player, profile, 10F));
        }
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
            FcsState.leadTarget = best;
            FcsState.leadPoint = aimPipper(player, profile, best);
        } else {
            leadTracker = null;
        }
    }

    // ------------------------------------------------------------------ 0.1.10.20: fire control (P1, P3)

    /** The motion estimate of the lead target (one target; a new target starts a new estimate). */
    private static com.miophas.singularity_iteration.common.fcs.TargetTracker leadTracker;
    private static int leadTrackerId = Integer.MIN_VALUE;
    private static com.miophas.singularity_iteration.common.fcs.Intercept.Solution leadSolution;

    /**
     * The aim pipper: the point on the launch direction that hits {@code target} (the diamond "LEAD" of the HUD; put the
     * crosshair on it), or null when the shot cannot reach it. The solution includes gravity, drag, the player's own
     * velocity and the target motion during the flight ({@code common.fcs.Intercept}); the target motion comes from a
     * {@code TargetTracker} (three-step velocity, arcs for a circling target). Before 0.1.10.20 the diamond was the target
     * position after a flight time that ignored gravity and the needed pitch (one pass, a one-tick velocity): at 30 m with
     * a full bow it was about 2.5 blocks off, at 60 m about 10.
     */
    private static Vec3 aimPipper(LocalPlayer player, BallisticSolver.Profile profile, LivingEntity target) {
        if (leadTracker == null || leadTrackerId != target.getId()) {
            leadTracker = new com.miophas.singularity_iteration.common.fcs.TargetTracker();
            leadTrackerId = target.getId();
            leadSolution = null;
        }
        Vec3 feet = target.position();
        leadTracker.update(feet.x, feet.y, feet.z);
        double lift = target.getBbHeight() * 0.5, y0 = feet.y + lift;
        double[] v = new double[3];
        leadTracker.velocity(v);
        boolean flying = !target.onGround() && target.isNoGravity();
        double vy = flying ? v[1] : 0;
        var tracker = leadTracker;
        double[] horizontal = new double[2];
        com.miophas.singularity_iteration.common.fcs.Intercept.TargetPath path = (ticks, out) -> {
            tracker.predictHorizontal(ticks, horizontal);
            out[0] = horizontal[0]; out[1] = y0 + vy * ticks; out[2] = horizontal[1];
        };
        Vec3 own = player.getDeltaMovement();
        double[] launch = {player.getX(), player.getEyeY() - 0.1, player.getZ()};
        double[] shooter = {own.x, player.onGround() ? 0.0 : own.y, own.z};
        var solution = com.miophas.singularity_iteration.common.fcs.Intercept.solve(
            new com.miophas.singularity_iteration.common.fcs.Intercept.Profile(profile.speed(), profile.gravity(), profile.drag()),
            launch, shooter, path, leadSolution);
        leadSolution = solution;
        if (solution == null) return null;
        double[] hit = new double[3];
        path.at(solution.ticks(), hit);
        double range = Math.sqrt((hit[0] - launch[0]) * (hit[0] - launch[0]) + (hit[1] - launch[1]) * (hit[1] - launch[1])
            + (hit[2] - launch[2]) * (hit[2] - launch[2]));
        return new Vec3(launch[0] + solution.dx() * range, launch[1] + solution.dy() * range, launch[2] + solution.dz() * range);
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
            ManeuverModeClient.renderWorld(event);
            QuantumFx.render(event);                                   // 0.1.9.1 photon barrier, active scan
            com.miophas.singularity_iteration.common.client.armory.WingmanFx.render(event);   // 0.1.9.1 Wingman lock-on, flashes, jets
            com.miophas.singularity_iteration.common.client.armory.CommandFx.render(event);   // 0.1.10.2 orders, markers, recall
            com.miophas.singularity_iteration.common.client.armory.HoloTable.render(event);   // 0.1.10.2 3D holo table
        }
    }

    private static void renderSensors(RenderLevelStageEvent event, Minecraft mc) {
        Vec3 cam = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float time = (mc.level.getGameTime() % 40 + partial) / 40F;
        float pulse = 0.8F + 0.2F * (float) Math.sin(time * Math.PI * 2);

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

        if (FcsState.active(SuitModuleType.GRID_TELEMETRY) && telemetryPos != null) {
            float r = ((primary >> 16) & 255) / 255F, g = ((primary >> 8) & 255) / 255F, b = (primary & 255) / 255F;
            LevelRenderer.renderLineBox(pose, lines, new AABB(telemetryPos).inflate(0.01), r, g, b, 0.9F);
        }
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
