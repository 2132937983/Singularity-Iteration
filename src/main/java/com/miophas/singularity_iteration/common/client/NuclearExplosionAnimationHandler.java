package com.miophas.singularity_iteration.common.client;

import com.miophas.singularity_iteration.common.Singularity_Iteration_Config;
import com.miophas.singularity_iteration.core.runtime.reactor.NuclearCloudSimulation;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.world.level.material.FogType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import com.miophas.singularity_iteration.common.registry.mio_icif_sounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Independent cloud rendering, delayed pressure-front audio and transient camera effects.
 * One soft procedural texture; no HBM assets, shaders, entities or source code are reused.
 */
public final class NuclearExplosionAnimationHandler {
    private static final int MAX_SCENES = 4;
    private static final List<Scene> SCENES = new ArrayList<>();
    private static final List<Billboard> SORTED = new ArrayList<>();
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("mio_icif", "runtime/nuclear_cloud");
    private static final RenderType CLOUD = RenderType.entityTranslucentEmissive(TEXTURE, false);
    private static ClientLevel boundLevel;
    private static boolean registered, textureRegistered;
    private record Billboard(Scene scene, NuclearCloudSimulation.Parcel parcel, double distance) {}
    private static final class Scene {
        final Vec3 center;
        final int radius;
        final NuclearCloudSimulation simulation;
        final boolean flashVisible;
        int arrival = -1;
        Scene(Vec3 center, int radius, double density, boolean flashVisible) {
            this.center = center; this.radius = radius; this.flashVisible = flashVisible;
            simulation = new NuclearCloudSimulation(radius, density, Double.doubleToLongBits(center.x) ^ Double.doubleToLongBits(center.z));
        }
    }
    public static void register() {
        if (!registered) { NeoForge.EVENT_BUS.register(NuclearExplosionAnimationHandler.class); registered = true; }
    }
    public static void startAnimation(double x, double y, double z, int radius) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z) || radius < 1 || radius > 2000) return;
        register();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        bind(mc.level);
        Vec3 center = new Vec3(x, y, z), eye = mc.gameRenderer.getMainCamera().getPosition();
        Vec3 view = center.subtract(eye);
        // Bound the one-time flash occlusion check; nearby solid shelter blocks the glare.
        Vec3 end = view.length() > 128 ? eye.add(view.normalize().scale(128)) : center;
        boolean visible = mc.level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE, mc.player)).getType() == HitResult.Type.MISS;
        if (SCENES.size() == MAX_SCENES) SCENES.remove(0);
        SCENES.add(new Scene(center, radius, Singularity_Iteration_Config.MUSHROOM_CLOUD_PARTICLE_MULTIPLIER.get(), visible));
    }
    private static void bind(ClientLevel level) {
        if (boundLevel != level) { SCENES.clear(); SORTED.clear(); boundLevel = level; }
    }
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        bind(mc.level);
        if (mc.level == null || mc.player == null || mc.isPaused()) return;
        Vec3 eye = mc.gameRenderer.getMainCamera().getPosition();
        for (Scene scene : SCENES) {
            scene.simulation.tick();
            int age = scene.simulation.age();
            if (scene.arrival < 0 && eye.distanceTo(scene.center) <= NuclearCloudSimulation.shockRadius(age)) {
                scene.arrival = age;
                float volume = (float) Math.max(.12, Math.min(1, scene.radius * 3.0 / Math.max(1, eye.distanceTo(scene.center))));
                play(mc, mio_icif_sounds.NUCLEAR_EXPLOSION.get(), volume, 1);
            }
        }
        SCENES.removeIf(scene -> scene.simulation.isComplete());
    }
    private static void play(Minecraft mc, SoundEvent sound, float volume, float pitch) {
        // Listener-relative but still obeys the BLOCKS sound slider. Distance is applied explicitly.
        mc.getSoundManager().play(new SimpleSoundInstance(sound.getLocation(), SoundSource.BLOCKS,
            volume, pitch, SoundInstance.createUnseededRandom(), false, 0, SoundInstance.Attenuation.NONE, 0, 0, 0, true));
    }
    private static void texture(Minecraft mc) {
        if (textureRegistered) return;
        NativeImage pixels = new NativeImage(64, 64, false);
        for (int y = 0; y < 64; y++) for (int x = 0; x < 64; x++) {
            double dx = (x - 31.5) / 31.5, dy = (y - 31.5) / 31.5;
            double r = dx * dx + dy * dy;
            double cloud = Math.max(0, 1 - r) * Math.exp(-r * 3);
            cloud *= .78 + .22 * Math.sin(x * .37 + Math.sin(y * .23) * 2) * Math.cos(y * .31);
            int alpha = (int) (255 * cloud);
            pixels.setPixelRGBA(x, y, alpha << 24 | 0xFFFFFF);
        }
        mc.getTextureManager().register(TEXTURE, new DynamicTexture(pixels));
        textureRegistered = true;
    }
    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft mc = Minecraft.getInstance();
        bind(mc.level);
        if (mc.level == null || SCENES.isEmpty()) return;
        texture(mc);
        Vec3 camera = event.getCamera().getPosition();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        SORTED.clear();
        for (Scene scene : SCENES) for (var parcel : scene.simulation.parcels()) {
            if (parcel.alpha > .001F) SORTED.add(new Billboard(scene, parcel,
                camera.distanceToSqr(scene.center.add(parcel.x, parcel.y, parcel.z))));
        }
        SORTED.sort(Comparator.comparingDouble(Billboard::distance).reversed());
        Vector3f right = new Vector3f(1, 0, 0).rotate(event.getCamera().rotation());
        Vector3f up = new Vector3f(0, 1, 0).rotate(event.getCamera().rotation());
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        var buffers = mc.renderBuffers().bufferSource();
        VertexConsumer vertices = buffers.getBuffer(CLOUD);
        for (Billboard billboard : SORTED) {
            var p = billboard.parcel;
            Vec3 position = billboard.scene.center.subtract(camera).add(p.oldX + (p.x - p.oldX) * partial,
                p.oldY + (p.y - p.oldY) * partial, p.oldZ + (p.z - p.oldZ) * partial);
            quad(vertices, pose, position, right, up, p.size, p.red, p.green, p.blue, p.alpha);
        }
        // An expanding, emissive fireball at the base accompanies the first flash.
        for (Scene scene : SCENES) {
            float light = (float) NuclearCloudSimulation.flash(scene.simulation.age() + partial);
            if (light > .01F) quad(vertices, pose, scene.center.subtract(camera), right, up,
                (float) (8 + Math.sqrt(scene.radius) * 3), 1, .87F, .59F, light);
        }
        float fogStart = RenderSystem.getShaderFogStart(), fogEnd = RenderSystem.getShaderFogEnd();
        try {
            if (event.getCamera().getFluidInCamera() == FogType.NONE) {
                RenderSystem.setShaderFogStart(Math.max(fogStart, 3072));
                RenderSystem.setShaderFogEnd(Math.max(fogEnd, 4096));
            }
            buffers.endBatch(CLOUD);
        } finally {
            RenderSystem.setShaderFogStart(fogStart); RenderSystem.setShaderFogEnd(fogEnd);
            pose.popPose();
        }
    }
    private static void quad(VertexConsumer v, PoseStack pose, Vec3 center, Vector3f right, Vector3f up,
                             float size, float red, float green, float blue, float alpha) {
        for (int i = 0; i < 4; i++) {
            float sx = i == 0 || i == 1 ? -1 : 1, sy = i == 0 || i == 3 ? -1 : 1;
            v.addVertex(pose.last().pose(), (float) center.x + (right.x * sx + up.x * sy) * size,
                (float) center.y + (right.y * sx + up.y * sy) * size,
                (float) center.z + (right.z * sx + up.z * sy) * size)
                .setColor(red, green, blue, alpha).setUv((sx + 1) / 2, (1 - sy) / 2)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 1, 0);
        }
    }
    @SubscribeEvent
    public static void flash(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        bind(mc.level);
        if (!Singularity_Iteration_Config.NUCLEAR_FLASH.get() || mc.player == null) return;
        double brightness = 0;
        for (Scene scene : SCENES) if (scene.flashVisible) {
            double distance = mc.player.position().distanceTo(scene.center);
            brightness = Math.max(brightness, NuclearCloudSimulation.flash(scene.simulation.age())
                * Math.min(.85, scene.radius * 2.0 / Math.max(1, distance)));
        }
        int alpha = (int) (brightness * 255);
        if (alpha > 0) event.getGuiGraphics().fill(0, 0, mc.getWindow().getGuiScaledWidth(),
            mc.getWindow().getGuiScaledHeight(), alpha << 24 | 0xFFF2DD);
    }
    @SubscribeEvent
    public static void shake(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        bind(mc.level);
        if (!Singularity_Iteration_Config.NUCLEAR_CAMERA_SHAKE.get() || mc.player == null) return;
        double amplitude = 0;
        for (Scene scene : SCENES) if (scene.arrival >= 0) {
            amplitude = Math.max(amplitude, NuclearCloudSimulation.shake(scene.simulation.age() - scene.arrival + event.getPartialTick())
                * Math.min(2, scene.radius * 2.0 / Math.max(1, mc.player.position().distanceTo(scene.center))));
        }
        double t = (mc.level == null ? 0 : mc.level.getGameTime()) + event.getPartialTick();
        event.setPitch(event.getPitch() + (float) (Math.sin(t * 2.1) * amplitude));
        event.setYaw(event.getYaw() + (float) (Math.sin(t * 1.7) * amplitude * .6));
        event.setRoll(event.getRoll() + (float) (Math.sin(t * 1.3) * amplitude * .35));
    }
}
