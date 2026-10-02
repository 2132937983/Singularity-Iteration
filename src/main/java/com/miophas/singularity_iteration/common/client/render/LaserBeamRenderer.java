package com.miophas.singularity_iteration.common.client.render;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_laser_tower;
import com.miophas.singularity_iteration.common.network.LaserTowerBeamPacket;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Client-side lightning-laser beams for the laser defence towers.
 *
 * <p>Design goals (up to 10 beams per tower volley without an FPS hit):
 * <ul>
 *   <li><b>No per-tick sync.</b> The server sends one packet per volley (tower position +
 *       entity ids). Beams live here for {@link #LIFETIME} ticks, start at the turret's
 *       matrix core emitter (published every frame by {@link MatrixTowerRenderer}) and end
 *       at the target's <i>interpolated</i> render position.</li>
 *   <li><b>No per-frame allocation of beam state.</b> Beams sit in a fixed-size ring of
 *       primitive arrays; bolt geometry is computed into reusable scratch arrays and
 *       streamed straight into one batch.</li>
 *   <li><b>One draw per frame.</b> Haze, glow, core, forks and impact/muzzle flares all use
 *       {@link SiRenderTypes#ENERGY_BEAM} (position+colour, additive, cull-free, no depth
 *       write) and are flushed with a single {@code endBatch}. No VBOs are created.</li>
 *   <li><b>Culling.</b> Beams farther than {@link #MAX_DISTANCE} or outside the frustum
 *       emit no vertices; segment count scales with beam length.</li>
 * </ul>
 * The bolt shape is a hash of (beam seed, segment, game tick), blended across the tick
 * with the partial tick, so it crackles every tick yet moves smoothly.
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
@SuppressWarnings("null")
public final class LaserBeamRenderer {
    private LaserBeamRenderer() {}

    private static final int CAPACITY = 256;
    private static final int LIFETIME = 8;          // ticks; tower volleys every 15
    private static final double MAX_DISTANCE = 160.0;
    private static final int MAX_SEGMENTS = 24;
    private static final int FORK_SEGMENTS = 5;
    /** Muzzle positions older than this are stale (turret not rendered, e.g. culled). */
    private static final long MUZZLE_FRESH_NANOS = 250_000_000L;

    // --- beam ring (structure of arrays) -------------------------------------
    private static final long[] towerPos = new long[CAPACITY];
    private static final byte[] barrel = new byte[CAPACITY];
    private static final double[] originX = new double[CAPACITY], originY = new double[CAPACITY], originZ = new double[CAPACITY];
    private static final double[] endX = new double[CAPACITY], endY = new double[CAPACITY], endZ = new double[CAPACITY];
    private static final int[] entityId = new int[CAPACITY];
    private static final int[] style = new int[CAPACITY];
    private static final int[] seed = new int[CAPACITY];
    private static final long[] born = new long[CAPACITY];
    private static int head;       // next write slot
    private static int live;       // number of possibly-live slots (<= CAPACITY)
    private static ClientLevel boundLevel;

    // --- scratch geometry (reused every frame) --------------------------------
    private static final float[] px = new float[MAX_SEGMENTS + 1], py = new float[MAX_SEGMENTS + 1], pz = new float[MAX_SEGMENTS + 1];
    private static final Vector3f camLeft = new Vector3f(), camUp = new Vector3f();

    /** Colours per style: {core r,g,b, glow r,g,b}. Ground = electric cyan-white, sky = violet-white. */
    private static final float[][] PALETTE = {
        {0.88F, 0.98F, 1.00F, 0.18F, 0.60F, 1.00F},
        {1.00F, 0.92F, 1.00F, 0.70F, 0.28F, 1.00F},
    };

    /** Called on the client thread for every tower combat packet. */
    public static void accept(LaserTowerBeamPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;
        bindLevel(level);
        long now = level.getGameTime();
        BlockPos pos = packet.pos();
        mio_icif_laser_tower tower = level.getBlockEntity(pos) instanceof mio_icif_laser_tower t ? t : null;
        if (tower != null) {
            if (packet.kind() == LaserTowerBeamPacket.KIND_IDLE) {
                tower.clientAimTarget = -1;
            } else if (packet.targets().length > 0) {
                tower.clientAimTarget = packet.targets()[0];
                tower.clientAimTime = now;
            }
        }
        if (packet.kind() != LaserTowerBeamPacket.KIND_FIRE) return;
        if (tower != null) tower.clientFireTime = now;

        double cx = pos.getX() + 0.5, cy = pos.getY() + MatrixTowerRenderer.CORE_Y, cz = pos.getZ() + 0.5;
        int barrels = packet.style() == LaserTowerBeamPacket.STYLE_SKY ? 4 : 2;
        int[] targets = packet.targets();
        for (int n = 0; n < targets.length; n++) {
            int id = targets[n];
            Entity target = level.getEntity(id);
            int slot = head;
            head = (head + 1) % CAPACITY;
            if (live < CAPACITY) live++;
            towerPos[slot] = pos.asLong();
            barrel[slot] = (byte) (n % barrels);
            originX[slot] = cx;
            originY[slot] = cy;
            originZ[slot] = cz;
            entityId[slot] = id;
            style[slot] = Mth.clamp(packet.style(), 0, PALETTE.length - 1);
            born[slot] = now;
            seed[slot] = (int) mix(id * 0x9E3779B9L + now * 31L + slot);
            if (target != null) {
                endX[slot] = target.getX();
                endY[slot] = target.getY() + target.getBbHeight() * 0.6;
                endZ[slot] = target.getZ();
                // A few sparks at the impact point; vanilla particles are pooled and cheap.
                for (int i = 0; i < 4; i++) {
                    level.addParticle(ParticleTypes.ELECTRIC_SPARK, endX[slot], endY[slot], endZ[slot],
                        (level.random.nextDouble() - 0.5) * 0.5, level.random.nextDouble() * 0.3,
                        (level.random.nextDouble() - 0.5) * 0.5);
                }
            } else {
                endX[slot] = cx;
                endY[slot] = cy;
                endZ[slot] = cz;
            }
        }
    }

    private static void bindLevel(ClientLevel level) {
        if (boundLevel != level) {
            boundLevel = level;
            head = 0;
            live = 0;
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || live == 0) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || level != boundLevel) {
            live = 0;
            return;
        }
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        long tick = level.getGameTime();
        Camera camera = event.getCamera();
        Vec3 cam = camera.getPosition();
        camLeft.set(camera.getLeftVector());
        camUp.set(camera.getUpVector());
        Matrix4f pose = event.getPoseStack().last().pose();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer out = null;
        long nowNanos = System.nanoTime();

        int alive = 0;
        for (int n = 0; n < live; n++) {
            int slot = (head - 1 - n + CAPACITY) % CAPACITY;
            float age = (tick - born[slot]) + partial;
            if (age < 0 || age >= LIFETIME) continue;
            alive++;

            // origin: the matrix core emitter if the tower was rendered this frame
            if (level.getBlockEntity(BlockPos.of(towerPos[slot])) instanceof mio_icif_laser_tower tower
                    && nowNanos - tower.clientMuzzleFrame < MUZZLE_FRESH_NANOS && tower.clientMuzzleCount > 0) {
                int b = (barrel[slot] % tower.clientMuzzleCount) * 3;
                originX[slot] = tower.clientMuzzles[b];
                originY[slot] = tower.clientMuzzles[b + 1];
                originZ[slot] = tower.clientMuzzles[b + 2];
            }
            Entity target = level.getEntity(entityId[slot]);
            if (target != null && target.isAlive()) {
                Vec3 p = target.getPosition(partial);
                endX[slot] = p.x;
                endY[slot] = p.y + target.getBbHeight() * 0.6;
                endZ[slot] = p.z;
            }
            double ox = originX[slot], oy = originY[slot], oz = originZ[slot];
            double ex = endX[slot], ey = endY[slot], ez = endZ[slot];
            double mx = (ox + ex) * 0.5 - cam.x, my = (oy + ey) * 0.5 - cam.y, mz = (oz + ez) * 0.5 - cam.z;
            if (mx * mx + my * my + mz * mz > MAX_DISTANCE * MAX_DISTANCE) continue;
            if (!event.getFrustum().isVisible(new AABB(Math.min(ox, ex), Math.min(oy, ey), Math.min(oz, ez),
                    Math.max(ox, ex), Math.max(oy, ey), Math.max(oz, ez)).inflate(1.0))) continue;

            if (out == null) out = buffers.getBuffer(SiRenderTypes.ENERGY_BEAM);
            // Flash on the first tick, full for two ticks, then fade out.
            float fade = age < 2 ? 1.0F : 1.0F - (age - 2) / (LIFETIME - 2);
            float flash = age < 1.5F ? 1.0F - age / 1.5F : 0.0F;
            float ax = (float) (ox - cam.x), ay = (float) (oy - cam.y), az = (float) (oz - cam.z);
            float bx = (float) (ex - cam.x), by = (float) (ey - cam.y), bz = (float) (ez - cam.z);
            drawBolt(out, pose, slot, ax, ay, az, bx, by, bz, tick, partial, fade);
            float[] c = PALETTE[style[slot]];
            // impact and muzzle flares (camera-facing)
            flare(out, pose, bx, by, bz, 0.30F + 0.45F * flash, c[3], c[4], c[5], 0.45F * fade);
            flare(out, pose, bx, by, bz, 0.12F + 0.20F * flash, c[0], c[1], c[2], 0.9F * fade);
            if (flash > 0) flare(out, pose, ax, ay, az, 0.18F + 0.25F * flash, c[0], c[1], c[2], 0.8F * flash);
        }
        if (alive == 0) live = 0;       // every beam expired: skip the loop until the next volley
        if (out != null) buffers.endBatch(SiRenderTypes.ENERGY_BEAM);
    }

    private static void drawBolt(VertexConsumer out, Matrix4f pose, int slot,
                                 float ax, float ay, float az, float bx, float by, float bz,
                                 long tick, float partial, float fade) {
        float dx = bx - ax, dy = by - ay, dz = bz - az;
        float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.05F) return;
        float inv = 1.0F / len;
        float fx = dx * inv, fy = dy * inv, fz = dz * inv;
        // Two axes perpendicular to the beam for the lightning displacement.
        float ux, uy, uz;
        if (Math.abs(fy) < 0.9F) { ux = fz; uy = 0; uz = -fx; } else { ux = 0; uy = -fz; uz = fy; }
        float ul = Mth.invSqrt(ux * ux + uy * uy + uz * uz);
        ux *= ul; uy *= ul; uz *= ul;
        float vx = fy * uz - fz * uy, vy = fz * ux - fx * uz, vz = fx * uy - fy * ux;

        int segments = Mth.clamp((int) (len / 1.2F), 4, MAX_SEGMENTS);
        float amplitude = Math.min(0.55F, 0.10F + len * 0.02F);
        int s = seed[slot];
        for (int i = 0; i <= segments; i++) {
            float t = (float) i / segments;
            float envelope = Mth.sin(t * Mth.PI) * amplitude;
            float o1 = crackle(s, i, tick, partial) * envelope;
            float o2 = crackle(s ^ 0x5bd1e995, i, tick, partial) * envelope;
            px[i] = ax + dx * t + ux * o1 + vx * o2;
            py[i] = ay + dy * t + uy * o1 + vy * o2;
            pz[i] = az + dz * t + uz * o1 + vz * o2;
        }

        float[] c = PALETTE[style[slot]];
        float wide = style[slot] == LaserTowerBeamPacket.STYLE_SKY ? 0.22F : 0.17F;
        ribbon(out, pose, segments, wide * 1.9F, c[3], c[4], c[5], 0.10F * fade);  // outer haze
        ribbon(out, pose, segments, wide, c[3], c[4], c[5], 0.45F * fade);         // glow
        ribbon(out, pose, segments, wide * 0.28F, c[0], c[1], c[2], 1.0F * fade);  // white-hot core

        // One or two short side forks for the "electric" look.
        if (segments >= 6) {
            int forks = 1 + ((s >>> 7) & 1);
            for (int f = 0; f < forks; f++) {
                int h = (int) mix(s + f * 7919L + tick / 2);
                int from = 1 + Math.floorMod(h, segments - 2);
                float sx = px[from], sy = py[from], sz = pz[from];
                float bl = Math.min(2.5F, len * 0.25F);
                float ka = ((h >>> 8) & 255) / 127.5F - 1.0F, kb = ((h >>> 16) & 255) / 127.5F - 1.0F;
                float gx = fx * 0.6F + ux * ka + vx * kb;
                float gy = fy * 0.6F + uy * ka + vy * kb;
                float gz = fz * 0.6F + uz * ka + vz * kb;
                float gl = bl * Mth.invSqrt(gx * gx + gy * gy + gz * gz);
                for (int i = 0; i <= FORK_SEGMENTS; i++) {
                    float t = (float) i / FORK_SEGMENTS;
                    float j = crackle(h, i, tick, partial) * 0.18F * t;
                    px[i] = sx + gx * gl * t + ux * j;
                    py[i] = sy + gy * gl * t + uy * j;
                    pz[i] = sz + gz * gl * t + uz * j;
                }
                ribbon(out, pose, FORK_SEGMENTS, wide * 0.35F, c[3], c[4], c[5], 0.6F * fade);
            }
        }
    }

    /**
     * Camera-facing strip through {@code px/py/pz[0..segments]}. Coordinates are
     * camera-relative, so the point itself is the view vector; the strip's side is
     * {@code tangent x view}. Tips taper.
     */
    private static void ribbon(VertexConsumer out, Matrix4f pose, int segments, float width,
                               float r, float g, float b, float a) {
        if (a <= 0.01F) return;
        float prevLx = 0, prevLy = 0, prevLz = 0, prevRx = 0, prevRy = 0, prevRz = 0;
        for (int i = 0; i <= segments; i++) {
            int i0 = Math.max(0, i - 1), i1 = Math.min(segments, i + 1);
            float tx = px[i1] - px[i0], ty = py[i1] - py[i0], tz = pz[i1] - pz[i0];
            float sx = ty * pz[i] - tz * py[i];
            float sy = tz * px[i] - tx * pz[i];
            float sz = tx * py[i] - ty * px[i];
            float sl = sx * sx + sy * sy + sz * sz;
            float taper = (i == 0 || i == segments) ? 0.35F : 1.0F;
            float w = sl > 1.0E-12F ? width * taper * Mth.invSqrt(sl) : 0.0F;
            sx *= w; sy *= w; sz *= w;
            float lx = px[i] - sx, ly = py[i] - sy, lz = pz[i] - sz;
            float rx = px[i] + sx, ry = py[i] + sy, rz = pz[i] + sz;
            if (i > 0) {
                out.addVertex(pose, prevLx, prevLy, prevLz).setColor(r, g, b, a);
                out.addVertex(pose, lx, ly, lz).setColor(r, g, b, a);
                out.addVertex(pose, rx, ry, rz).setColor(r, g, b, a);
                out.addVertex(pose, prevRx, prevRy, prevRz).setColor(r, g, b, a);
            }
            prevLx = lx; prevLy = ly; prevLz = lz;
            prevRx = rx; prevRy = ry; prevRz = rz;
        }
    }

    /** Camera-facing diamond-shaped flare (two quads: plus and cross) at a camera-relative point. */
    private static void flare(VertexConsumer out, Matrix4f pose, float x, float y, float z, float size,
                              float r, float g, float b, float a) {
        if (a <= 0.01F) return;
        float lx = camLeft.x * size, ly = camLeft.y * size, lz = camLeft.z * size;
        float ux = camUp.x * size, uy = camUp.y * size, uz = camUp.z * size;
        out.addVertex(pose, x + lx, y + ly, z + lz).setColor(r, g, b, 0.0F);
        out.addVertex(pose, x + ux * 0.25F, y + uy * 0.25F, z + uz * 0.25F).setColor(r, g, b, a);
        out.addVertex(pose, x - lx, y - ly, z - lz).setColor(r, g, b, 0.0F);
        out.addVertex(pose, x - ux * 0.25F, y - uy * 0.25F, z - uz * 0.25F).setColor(r, g, b, a);
        out.addVertex(pose, x + ux, y + uy, z + uz).setColor(r, g, b, 0.0F);
        out.addVertex(pose, x + lx * 0.25F, y + ly * 0.25F, z + lz * 0.25F).setColor(r, g, b, a);
        out.addVertex(pose, x - ux, y - uy, z - uz).setColor(r, g, b, 0.0F);
        out.addVertex(pose, x - lx * 0.25F, y - ly * 0.25F, z - lz * 0.25F).setColor(r, g, b, a);
    }

    /** Per-tick pseudo-random offset in [-1, 1], blended into the next tick for smooth motion. */
    private static float crackle(int seed, int index, long tick, float partial) {
        float a = unit(mix(seed * 0x632BE59BD9B4E019L + index * 0x9E3779B97F4A7C15L + tick));
        float b = unit(mix(seed * 0x632BE59BD9B4E019L + index * 0x9E3779B97F4A7C15L + tick + 1));
        return a + (b - a) * partial;
    }

    private static float unit(long h) {
        return ((h >>> 40) & 0xFFFF) / 32767.5F - 1.0F;
    }

    /** SplitMix64 finaliser. */
    private static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
}
