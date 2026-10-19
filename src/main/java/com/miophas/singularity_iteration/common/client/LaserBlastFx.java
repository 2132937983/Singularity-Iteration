// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client;

import com.miophas.singularity_iteration.common.Singularity_Iteration_Config;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * 0.1.10.11: the plasma blast of the mining laser's Ultra-High Energy Blast (it used the vanilla
 * explosion puffs). An energy blast, not a fire ball:
 * <ol>
 *   <li>0-3 ticks: a white-hot core and a plasma column shoot up; a white screen flash for the
 *       players who see the centre; the warden's sonic ring, flashes and a burst of sparks.</li>
 *   <li>0-24 ticks: two shock rings race over the ground (cyan, then violet) and a spherical shock
 *       shell grows; the core cools from white to cyan to violet.</li>
 *   <li>The pressure front arrives at the observer (5 blocks per tick) and shakes the camera.</li>
 *   <li>Up to 5 s: arcs crawl over the blast area, a violet afterglow flickers at the centre and an
 *       ion haze rises and fades; smoke columns drift up.</li>
 * </ol>
 * Additive glow (no depth write) on two procedural textures; the screen flash and the shake obey
 * the nuclear flash / camera shake options; particle counts follow the particle setting.
 * At most {@link #MAX_BLASTS} blasts at once.
 */
@OnlyIn(Dist.CLIENT)
public final class LaserBlastFx {
    private LaserBlastFx() {}

    private static final int MAX_BLASTS = 6, LIFE = 100;
    private static final ResourceLocation GLOW = ResourceLocation.fromNamespaceAndPath("mio_icif", "runtime/laser_blast_glow");
    private static final ResourceLocation RING = ResourceLocation.fromNamespaceAndPath("mio_icif", "runtime/laser_blast_ring");
    private static final List<Blast> BLASTS = new ArrayList<>();
    private static boolean registered, textures;
    private static ClientLevel boundLevel;

    private static final class Blast {
        final Vec3 center;
        final float radius;
        final boolean visible;
        final double[][] haze;
        final Random random;
        int age;

        Blast(Vec3 center, float radius, boolean visible, long seed) {
            this.center = center;
            this.radius = radius;
            this.visible = visible;
            this.random = new Random(seed);
            haze = new double[7][];
            for (int i = 0; i < haze.length; i++) {
                double a = random.nextDouble() * Math.PI * 2, d = random.nextDouble() * 0.6 * radius;
                haze[i] = new double[] {Math.cos(a) * d, random.nextDouble() * 0.3 * radius, Math.sin(a) * d, 0.5 + random.nextDouble() * 0.3};
            }
        }
    }

    /** Starts a blast at the position (explosion strength as the laser fired it). */
    public static void start(double x, double y, double z, float strength) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z) || !(strength > 0) || strength > 100) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        if (!registered) { NeoForge.EVENT_BUS.register(LaserBlastFx.class); registered = true; }
        bind(mc.level);
        Vec3 center = new Vec3(x, y, z), eye = mc.gameRenderer.getMainCamera().getPosition();
        Vec3 view = center.subtract(eye);
        Vec3 end = view.length() > 160 ? eye.add(view.normalize().scale(160)) : center;
        boolean visible = mc.level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player))
            .getType() == HitResult.Type.MISS;
        if (BLASTS.size() == MAX_BLASTS) BLASTS.remove(0);
        Blast blast = new Blast(center, Mth.clamp(strength * 1.3F, 4, 40), visible,
            Double.doubleToLongBits(x) ^ Double.doubleToLongBits(z) * 31 ^ mc.level.getGameTime());
        BLASTS.add(blast);
        burst(mc, blast);
    }

    private static void bind(ClientLevel level) {
        if (boundLevel != level) { BLASTS.clear(); boundLevel = level; }
    }

    private static float particleScale(Minecraft mc) {
        return switch (mc.options.particles().get()) {
            case ALL -> 1.0F;
            case DECREASED -> 0.5F;
            case MINIMAL -> 0.2F;
        };
    }

    private static void add(ClientLevel level, ParticleOptions type, Vec3 at, double vx, double vy, double vz) {
        level.addParticle(type, true, at.x, at.y, at.z, vx, vy, vz);
    }

    /** The first-tick burst: sonic ring, flashes, radial sparks, arcs and smoke. */
    private static void burst(Minecraft mc, Blast b) {
        ClientLevel level = mc.level;
        Random r = b.random;
        float k = particleScale(mc), speed = b.radius / 24F;
        add(level, ParticleTypes.SONIC_BOOM, b.center.add(0, 0.5, 0), 0, 0, 0);
        add(level, ParticleTypes.FLASH, b.center, 0, 0, 0);
        add(level, ParticleTypes.FLASH, b.center.add(0, b.radius * 0.25, 0), 0, 0, 0);
        for (int i = 0; i < 90 * k; i++) {
            Vec3 dir = new Vec3(r.nextGaussian(), Math.abs(r.nextGaussian()) * 0.7 + 0.1, r.nextGaussian()).normalize();
            Vec3 v = dir.scale((0.35 + r.nextDouble() * 0.55) * speed);
            add(level, ParticleTypes.END_ROD, b.center, v.x, v.y, v.z);
        }
        for (int i = 0; i < 70 * k; i++) {
            Vec3 dir = new Vec3(r.nextGaussian(), r.nextGaussian(), r.nextGaussian()).normalize();
            Vec3 at = b.center.add(dir.scale(r.nextDouble() * b.radius * 0.5));
            Vec3 v = dir.scale(0.2 * speed);
            add(level, ParticleTypes.ELECTRIC_SPARK, at, v.x, v.y, v.z);
        }
        for (int i = 0; i < 18 * k; i++) {
            double a = r.nextDouble() * Math.PI * 2, d = (0.3 + r.nextDouble() * 0.5) * b.radius;
            Vec3 at = b.center.add(Math.cos(a) * d, 0.2, Math.sin(a) * d);
            add(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, at, 0, 0.05 + r.nextDouble() * 0.07, 0);
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        bind(mc.level);
        if (mc.level == null || BLASTS.isEmpty() || mc.isPaused()) return;
        float k = particleScale(mc);
        for (Blast b : BLASTS) {
            b.age++;
            if (b.age > 22) continue;
            Random r = b.random;
            // arcs crawl over the blast area while it cools; embers rain down from the column
            double reach = b.radius * (0.3 + 0.03 * b.age);
            for (int i = 0; i < 5 * k; i++) {
                double a = r.nextDouble() * Math.PI * 2, d = r.nextDouble() * reach;
                add(mc.level, ParticleTypes.ELECTRIC_SPARK, b.center.add(Math.cos(a) * d, r.nextDouble() * 1.5, Math.sin(a) * d),
                    r.nextGaussian() * 0.08, 0.05, r.nextGaussian() * 0.08);
            }
            if (b.age < 14 && r.nextFloat() < 0.8F * k) {
                add(mc.level, ParticleTypes.END_ROD, b.center.add(r.nextGaussian() * 0.6, b.radius * (0.4 + r.nextDouble()), r.nextGaussian() * 0.6),
                    r.nextGaussian() * 0.04, -0.05, r.nextGaussian() * 0.04);
            }
        }
        BLASTS.removeIf(b -> b.age > LIFE);
    }

    // ------------------------------------------------------------------ drawing

    private static void textures(Minecraft mc) {
        if (textures) return;
        NativeImage glow = new NativeImage(64, 64, false);
        for (int y = 0; y < 64; y++) for (int x = 0; x < 64; x++) {
            double dx = (x - 31.5) / 31.5, dy = (y - 31.5) / 31.5, r2 = dx * dx + dy * dy;
            int v = (int) (255 * Math.max(0, 1 - r2) * Math.exp(-r2 * 3.2));
            glow.setPixelRGBA(x, y, 0xFF000000 | v << 16 | v << 8 | v);
        }
        NativeImage ring = new NativeImage(128, 128, false);
        for (int y = 0; y < 128; y++) for (int x = 0; x < 128; x++) {
            double dx = (x - 63.5) / 63.5, dy = (y - 63.5) / 63.5, r = Math.sqrt(dx * dx + dy * dy);
            double band = Math.exp(-Math.pow((r - 0.84) / 0.07, 2)) + 0.18 * Math.exp(-Math.pow((r - 0.62) / 0.22, 2));
            int v = (int) (255 * Math.min(1, band) * (r < 1 ? 1 : 0));
            ring.setPixelRGBA(x, y, 0xFF000000 | v << 16 | v << 8 | v);
        }
        mc.getTextureManager().register(GLOW, new DynamicTexture(glow));
        mc.getTextureManager().register(RING, new DynamicTexture(ring));
        textures = true;
    }

    private static float easeOut(float t) {
        t = Mth.clamp(t, 0, 1);
        return 1 - (1 - t) * (1 - t) * (1 - t);
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft mc = Minecraft.getInstance();
        bind(mc.level);
        if (mc.level == null || BLASTS.isEmpty()) return;
        textures(mc);
        Vec3 camera = event.getCamera().getPosition();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vector3f right = new Vector3f(1, 0, 0).rotate(event.getCamera().rotation());
        Vector3f up = new Vector3f(0, 1, 0).rotate(event.getCamera().rotation());
        PoseStack pose = event.getPoseStack();
        Matrix4f m = pose.last().pose();
        var buffers = mc.renderBuffers().bufferSource();
        RenderType glowType = RenderType.eyes(GLOW), ringType = RenderType.eyes(RING);
        VertexConsumer glow = buffers.getBuffer(glowType);
        for (Blast b : BLASTS) {
            float t = b.age + partial, r = b.radius;
            Vec3 c = b.center.subtract(camera);
            // white-hot core cooling to cyan, then violet
            if (t < 18) {
                float size = r * (0.25F + 0.55F * easeOut(t / 4));
                float i = t < 3 ? 1 : (float) Math.exp(-(t - 3) / 4.5);
                float cool = Mth.clamp(t / 6, 0, 1), late = Mth.clamp((t - 8) / 10, 0, 1);
                billboard(glow, m, c, right, up, size, Mth.lerp(late, Mth.lerp(cool, 1, 0.45F), 0.6F),
                    Mth.lerp(late, Mth.lerp(cool, 1, 0.85F), 0.35F), 1, i);
                if (t < 10) billboard(glow, m, c, right, up, r * 0.35F, 1, 1, 1, t < 2 ? 1 : (float) Math.exp(-(t - 2) / 3));
            }
            // plasma column
            if (t < 14) {
                float height = 2.2F * r * easeOut(t / 5), width = 0.10F * r * (1 - t / 14) + 0.5F;
                float i = t < 2 ? 1 : (float) Math.exp(-(t - 2) / 4);
                column(glow, m, c, camera, b.center, width, height, 0.7F, 0.9F, 1, i);
            }
            // afterglow
            if (t > 8 && t < 80) {
                float i = 0.5F * (float) Math.exp(-(t - 8) / 25) * (0.85F + 0.15F * Mth.sin(t * 1.7F));
                billboard(glow, m, c.add(0, 0.3, 0), right, up, r * 0.45F, 1.0F, 0.45F, 0.75F, i);
            }
            // rising ion haze
            if (t > 6) {
                float life = Mth.clamp((t - 6) / 90, 0, 1);
                float i = 0.18F * Mth.sin(life * Mth.PI);
                if (i > 0.005F) for (double[] h : b.haze) {
                    Vec3 at = c.add(h[0], h[1] + t * 0.03 * r, h[2]);
                    billboard(glow, m, at, right, up, (float) (r * h[3]), 0.35F, 0.25F, 0.6F, i);
                }
            }
        }
        buffers.endBatch(glowType);
        VertexConsumer ring = buffers.getBuffer(ringType);
        for (Blast b : BLASTS) {
            float t = b.age + partial, r = b.radius;
            Vec3 c = b.center.subtract(camera);
            if (t < 24) {
                float size = 1.7F * r * easeOut(t / 12), i = 0.9F * (float) Math.pow(1 - t / 24, 1.5);
                flat(ring, m, c.add(0, 0.2, 0), size, 0.65F, 0.95F, 1, i);
            }
            if (t > 3 && t < 25) {
                float s = (t - 3) / 22, size = 1.25F * r * easeOut((t - 3) / 15), i = 0.7F * (1 - s) * (1 - s);
                flat(ring, m, c.add(0, 0.35, 0), size, 0.7F, 0.4F, 1, i);
            }
            if (t < 16) {
                float size = 1.3F * r * easeOut(t / 10), i = 0.6F * (1 - t / 16) * (1 - t / 16);
                billboard(ring, m, c, right, up, size, 0.75F, 0.95F, 1, i);
            }
        }
        buffers.endBatch(ringType);
    }

    /** Camera-facing quad; additive colour (scaled by {@code i}). Both windings: the render type culls back faces. */
    private static void billboard(VertexConsumer v, Matrix4f m, Vec3 c, Vector3f right, Vector3f up, float size,
                                  float red, float green, float blue, float i) {
        if (i <= 0.003F) return;
        for (int pass = 0; pass < 2; pass++) {
            for (int n = 0; n < 4; n++) {
                int k = pass == 0 ? n : 3 - n;
                float sx = k == 0 || k == 1 ? -1 : 1, sy = k == 0 || k == 3 ? -1 : 1;
                vertex(v, m, (float) c.x + (right.x * sx + up.x * sy) * size, (float) c.y + (right.y * sx + up.y * sy) * size,
                    (float) c.z + (right.z * sx + up.z * sy) * size, red * i, green * i, blue * i, (sx + 1) / 2, (1 - sy) / 2);
            }
        }
    }

    /** Horizontal quad (shock ring on the ground), both sides. */
    private static void flat(VertexConsumer v, Matrix4f m, Vec3 c, float size, float red, float green, float blue, float i) {
        if (i <= 0.003F) return;
        float x = (float) c.x, y = (float) c.y, z = (float) c.z;
        float[][] corners = {{-1, -1}, {-1, 1}, {1, 1}, {1, -1}};
        for (int pass = 0; pass < 2; pass++) {
            for (int k = 0; k < 4; k++) {
                float[] p = corners[pass == 0 ? k : 3 - k];
                vertex(v, m, x + p[0] * size, y, z + p[1] * size, red * i, green * i, blue * i, (p[0] + 1) / 2, (p[1] + 1) / 2);
            }
        }
    }

    /** Vertical quad that turns about its axis to the camera (the plasma column). */
    private static void column(VertexConsumer v, Matrix4f m, Vec3 c, Vec3 camera, Vec3 world, float width, float height,
                               float red, float green, float blue, float i) {
        if (i <= 0.003F) return;
        double dx = world.x - camera.x, dz = world.z - camera.z, len = Math.sqrt(dx * dx + dz * dz);
        float sx = len < 1.0E-4 ? 1 : (float) (-dz / len), sz = len < 1.0E-4 ? 0 : (float) (dx / len);
        float x = (float) c.x, y = (float) c.y, z = (float) c.z;
        float bottom = y - height * 0.15F, top = y + height;
        float[][] q = {{-1, bottom, 0, 1}, {-1, top, 0, 0}, {1, top, 1, 0}, {1, bottom, 1, 1}};
        for (int pass = 0; pass < 2; pass++) {
            for (int n = 0; n < 4; n++) {
                float[] p = q[pass == 0 ? n : 3 - n];
                vertex(v, m, x + p[0] * sx * width, p[1], z + p[0] * sz * width, red * i, green * i, blue * i, p[2], p[3]);
            }
        }
    }

    private static void vertex(VertexConsumer v, Matrix4f m, float x, float y, float z, float red, float green, float blue, float u, float w) {
        v.addVertex(m, x, y, z).setColor(Mth.clamp(red, 0, 1), Mth.clamp(green, 0, 1), Mth.clamp(blue, 0, 1), 1.0F)
            .setUv(u, w).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 1, 0);
    }

    // ------------------------------------------------------------------ screen flash and shake

    @SubscribeEvent
    public static void flash(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        bind(mc.level);
        if (BLASTS.isEmpty() || mc.player == null || !Singularity_Iteration_Config.NUCLEAR_FLASH.get()) return;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        double brightness = 0;
        for (Blast b : BLASTS) {
            if (!b.visible) continue;
            float t = b.age + partial;
            double curve = t < 2 ? 1 : Math.exp(-(t - 2) / 3.5);
            brightness = Math.max(brightness, 0.8 * curve * Math.min(1, b.radius * 2.2 / Math.max(1, mc.player.position().distanceTo(b.center))));
        }
        int alpha = (int) (brightness * 255);
        if (alpha > 2) event.getGuiGraphics().fill(0, 0, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(),
            alpha << 24 | 0xE6F4FF);
    }

    @SubscribeEvent
    public static void shake(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        bind(mc.level);
        if (BLASTS.isEmpty() || mc.player == null || !Singularity_Iteration_Config.NUCLEAR_CAMERA_SHAKE.get()) return;
        double amplitude = 0;
        for (Blast b : BLASTS) {
            double distance = mc.player.position().distanceTo(b.center);
            double since = b.age + event.getPartialTick() - distance / 5.0;   // the pressure front: 5 blocks per tick
            if (since < 0 || since > 30) continue;
            amplitude = Math.max(amplitude, 2.2 * Math.min(1.5, b.radius * 2.5 / Math.max(1, distance)) * Math.exp(-since / 8));
        }
        if (amplitude < 0.01) return;
        double t = (mc.level == null ? 0 : mc.level.getGameTime()) + event.getPartialTick();
        event.setPitch(event.getPitch() + (float) (Math.sin(t * 2.3) * amplitude));
        event.setYaw(event.getYaw() + (float) (Math.sin(t * 1.9) * amplitude * 0.6));
        event.setRoll(event.getRoll() + (float) (Math.sin(t * 1.4) * amplitude * 0.4));
    }
}
