// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.molecular;

import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_molecular_transformer;
import com.miophas.singularity_iteration.common.client.render.SiFxBuffers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;

/**
 * 0.1.11.213 (MT1 X1): draws the inside of the molecular transformer's glass chamber: the input as its pixel atoms at the
 * sample position (the chamber centre), standing still while the machine waits. 0.1.11.214 (X2): while it works, each item
 * is taken apart and rebuilt into the product the way its process works ({@link MolecularMotion}); atoms in the cloud carry
 * a glow. 0.1.11.215 (X3): the confinement field (corner columns, a scanning ring, breathing contacts), a flash as each
 * item finishes, specks drifting out of the eight edge notches, and a continuous stream when one item takes under 2 s. Nothing is drawn beyond {@link #VIEW_DISTANCE} blocks, and the "Fast"
 * graphics setting halves the atoms.
 */
public class MolecularTransformerRenderer implements BlockEntityRenderer<mio_icif_molecular_transformer> {
    public MolecularTransformerRenderer(BlockEntityRendererProvider.Context context) { }

    /** Blocks within which the chamber is drawn. */
    public static final int VIEW_DISTANCE = 32;
    /** The sample is 5/16 of a block wide (the model leaves 5.5..10.5 free), so one atom is 5/256 of a block. */
    static final float SAMPLE = 5F / 16F, PIXEL = SAMPLE / MolecularAtoms.GRID;
    /** Turn of a working sample, radians per tick (1.25 rad/s). */
    static final float SPIN = 0.0625F;
    /** Faces' shade: up, down, north/south, east/west (as the vanilla block shading). */
    private static final float UP = 1F, DOWN = 0.5F, NS = 0.8F, EW = 0.6F;

    @Override
    public void render(mio_icif_molecular_transformer machine, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Level level = machine.getLevel();
        ItemStack input = machine.viewInput();
        if (level == null || input.isEmpty()) return;
        boolean fast = Minecraft.getInstance().options.graphicsMode().get() == GraphicsStatus.FAST;
        List<MolecularAtoms.Atom> atoms = MolecularAtomCache.of(input, level);
        if (fast) atoms = MolecularAtoms.thin(atoms, 24);
        if (atoms.isEmpty()) return;

        double ticks = level.getGameTime() + partial;
        boolean working = machine.viewWorking();
        float angle = working ? (float) (ticks * SPIN % (Math.PI * 2)) : 0.6F;
        float bright = brightness(level, light, working);
        Matrix4f m = pose.last().pose();

        if (!working) {
            // a standing sample: input waiting for power or for room in the output
            VertexConsumer vc = SiFxBuffers.translucent(buffers);
            float cos = (float) Math.cos(angle), sin = (float) Math.sin(angle), h = PIXEL / 2;
            for (MolecularAtoms.Atom a : atoms) {
                float lx = (a.i() - 7.5F) * PIXEL, ly = (7.5F - a.j()) * PIXEL;
                cube(vc, m, 0.5F + lx * cos, 0.5F + ly, 0.5F + lx * sin, h, a.rgb(), bright, 1F);
            }
            return;
        }

        ItemStack result = machine.viewResult();
        List<MolecularAtoms.Atom> product = result.isEmpty() ? atoms : MolecularAtomCache.of(result, level);
        if (fast) product = MolecularAtoms.thin(product, 24);
        MolecularMotion.Kind kind = MolecularMotion.Kind.of(machine.viewKind());
        double t = ticks / 20.0;
        boolean stream = machine.viewStream();
        double p = stream ? 0 : machine.viewProgress(ticks);
        MolecularMotion.State s = STATE;
        List<float[]> glows = new java.util.ArrayList<>();

        VertexConsumer solid = SiFxBuffers.translucent(buffers);
        if (stream) {
            // 0.1.11.215 (X3): under 2 s an item, a continuous stream from the input (low) up to the product (high)
            float[] q = new float[3];
            for (MolecularAtoms.Atom a : atoms) {
                if (!MolecularMotion.streamInputShows(a, t)) continue;
                MolecularMotion.streamSample(a, false, angle, q);
                cube(solid, m, q[0] / 16F, q[1] / 16F, q[2] / 16F, PIXEL * MolecularMotion.STREAM_SCALE / 2, a.rgb(), bright, 1F);
            }
            for (MolecularAtoms.Atom a : product) {
                MolecularMotion.streamSample(a, true, angle, q);
                cube(solid, m, q[0] / 16F, q[1] / 16F, q[2] / 16F, PIXEL * MolecularMotion.STREAM_SCALE / 2, a.rgb(), bright,
                    MolecularMotion.streamProductAlpha(a, t));
            }
            int n = fast ? MolecularMotion.STREAM_ATOMS / 2 : MolecularMotion.STREAM_ATOMS;
            for (int k = 0; k < n; k++) {
                MolecularMotion.streamAtom(kind, k, atoms, product, t, angle, s);
                if (!s.visible) continue;
                cubeRgb(solid, m, s.x / 16F, s.y / 16F, s.z / 16F, s.size / 32F, s.r, s.g, s.b, 1F);
                float[] c = MolecularMotion.magentaGlow(kind, k, t) ? MolecularMotion.MAGENTA : MolecularMotion.CYAN;
                glows.add(new float[]{s.x, s.y, s.z, s.size, c[0], c[1], c[2], 0.22F * s.cloud, Float.NaN, 0, 0});
            }
        } else {
            // 0.1.11.214 (X2): one item at a time, taken apart and rebuilt the way its process works
            List<MolecularAtoms.Atom> in = shuffledIn(atoms), out = shuffledOut(product);
            int n = Math.max(in.size(), out.size());
            for (int k = 0; k < n; k++) {
                MolecularMotion.single(kind, k, in, out, p, t, angle, s);
                if (!s.visible || s.alpha <= 0.01F) continue;
                float lit = Math.max(bright, s.cloud);   // atoms in the cloud carry their own light
                cubeRgb(solid, m, s.x / 16F, s.y / 16F, s.z / 16F, s.size / 32F, s.r * lit, s.g * lit, s.b * lit, s.alpha);
                if (s.cloud > 0.3F) {
                    float[] c = MolecularMotion.magentaGlow(kind, k, t) ? MolecularMotion.MAGENTA : MolecularMotion.CYAN;
                    glows.add(new float[]{s.x, s.y, s.z, s.size, c[0], c[1], c[2], 0.28F * s.cloud * s.alpha, s.ax, s.ay, s.az});
                }
            }
        }

        VertexConsumer glow = SiFxBuffers.glow(buffers);
        for (float[] g : glows) {
            cubeRgb(glow, m, g[0] / 16F, g[1] / 16F, g[2] / 16F, g[3] * 0.95F / 16F, g[4], g[5], g[6], g[7]);
            if (!Float.isNaN(g[8]))   // molecular: the bond to the molecule's centre
                line(glow, m, g[0], g[1], g[2], g[8], g[9], g[10], 0.05F, MolecularMotion.CYAN, 0.35F * g[7] / 0.28F);
        }
        if (!stream) extras(glow, m, kind, p, t);
        field(glow, m, t, stream);
        if (!stream) {
            float f = MolecularMotion.flash(p);
            if (f > 0) {   // the finishing flash: a ring running out past the glass, a glow in the middle
                square(glow, m, MolecularMotion.CENTRE, MolecularMotion.flashRadius(p), MolecularMotion.WHITE, f * 0.9F);
                float r = MolecularMotion.flashRadius(p) * 0.45F / 16F;
                cubeRgb(glow, m, 0.5F, 0.5F, 0.5F, r, MolecularMotion.CYAN[0], MolecularMotion.CYAN[1], MolecularMotion.CYAN[2], f * 0.3F);
            }
        }
        float[] q = new float[3];
        for (int v = 0; v < MolecularMotion.VENTS.length; v++) {
            for (int i = 0; i < MolecularMotion.VENT_SPECKS; i++) {
                float a = MolecularMotion.vent(v, i, p, t, stream, q);
                if (a > 0) cubeRgb(glow, m, q[0] / 16F, q[1] / 16F, q[2] / 16F, 0.11F / 16F,
                    MolecularMotion.CYAN[0], MolecularMotion.CYAN[1], MolecularMotion.CYAN[2], a);
            }
        }
    }

    /** The confinement field: four corner columns, a scanning ring, the two contacts breathing. */
    private static void field(VertexConsumer glow, Matrix4f m, double t, boolean stream) {
        float breathe = MolecularMotion.breathe(t), strength = stream ? 1F : 0.75F;
        float lo = MolecularMotion.IN0, hi = MolecularMotion.IN1;
        for (float[] c : new float[][]{{lo, lo}, {hi, lo}, {lo, hi}, {hi, hi}})
            line(glow, m, c[0], MolecularMotion.LO, c[1], c[0], MolecularMotion.HI, c[1], 0.14F, MolecularMotion.CYAN,
                (0.22F + 0.22F * breathe) * strength);
        square(glow, m, MolecularMotion.sweepHeight(t, stream), 3.65F, MolecularMotion.CYAN2, 0.55F * strength);
        plate(glow, m, MolecularMotion.LO + 0.05F, 3.2F, MolecularMotion.CYAN, 0.12F + 0.12F * breathe);
        plate(glow, m, MolecularMotion.HI - 0.05F, 3.2F, MolecularMotion.CYAN, 0.12F + 0.12F * (1 - breathe));
    }

    private static final MolecularMotion.State STATE = new MolecularMotion.State();
    private static final java.util.Map<List<MolecularAtoms.Atom>, List<MolecularAtoms.Atom>> IN = new java.util.IdentityHashMap<>(),
        OUT = new java.util.IdentityHashMap<>();

    private static List<MolecularAtoms.Atom> shuffledIn(List<MolecularAtoms.Atom> atoms) {
        if (IN.size() > 64) IN.clear();
        return IN.computeIfAbsent(atoms, a -> MolecularMotion.shuffled(a, 0));
    }

    private static List<MolecularAtoms.Atom> shuffledOut(List<MolecularAtoms.Atom> atoms) {
        if (OUT.size() > 64) OUT.clear();
        return OUT.computeIfAbsent(atoms, a -> MolecularMotion.shuffled(a, 977));
    }

    /** Per-process extras: lattice press flashes, the nuclear beam and its tracks, the exotic broken tracks. */
    private static void extras(VertexConsumer glow, Matrix4f m, MolecularMotion.Kind kind, double p, double t) {
        switch (kind) {
            case LATTICE -> {
                float press = MolecularMotion.latticePress(p, t);
                if (press > 0.02F) {
                    plate(glow, m, MolecularMotion.HI - 0.15F, 3.4F, MolecularMotion.WHITE, press * 0.45F);
                    plate(glow, m, MolecularMotion.LO + 0.15F, 3.4F, MolecularMotion.WHITE, press * 0.45F);
                }
                if (p > 0.45 && p < 0.75) {   // nucleation front: a square growing out from the centre
                    float r = (float) ((p - 0.45) / 0.3 * 3.2);
                    square(glow, m, MolecularMotion.CENTRE, Math.min(3.6F, r), MolecularMotion.CYAN, 0.35F);
                }
            }
            case NUCLEAR -> {
                float beam = MolecularMotion.nuclearBeam(p, t);
                if (beam > 0) {
                    line(glow, m, 8, MolecularMotion.HI - 0.05F, 8, 8, 8, 8, 0.22F, MolecularMotion.WHITE, beam);
                    line(glow, m, 8, MolecularMotion.HI - 0.05F, 8, 8, 8, 8, 0.6F, MolecularMotion.CYAN, beam * 0.4F);
                }
                if (p > 0.15 && p < 0.8) {
                    int pulse = (int) Math.floor(t / 0.6);
                    float[] q = new float[3];
                    for (int back = 0; back < 2; back++) {
                        int mIdx = pulse - back;
                        double age = t - mIdx * 0.6;
                        for (int s = 0; s < 4; s++) {
                            float[] c = s == 0 ? YELLOW : MolecularMotion.CYAN;
                            for (int i = 0; i < 18; i++) {
                                float a = MolecularMotion.nuclearTrack(mIdx, s, i, age, q);
                                if (a <= 0) break;
                                cubeRgb(glow, m, q[0] / 16F, q[1] / 16F, q[2] / 16F, 0.14F / 16F, c[0], c[1], c[2], a);
                            }
                        }
                    }
                }
            }
            case EXOTIC -> {
                float[][] pts = new float[5][3];
                boolean[] magenta = new boolean[1];
                for (int q = 0; q < 3; q++) {
                    if (!MolecularMotion.exoticTrack(p, t, q, pts, magenta)) continue;
                    float[] c = magenta[0] ? MolecularMotion.MAGENTA : MolecularMotion.WHITE;
                    for (int i = 0; i < 4; i++)
                        line(glow, m, pts[i][0], pts[i][1], pts[i][2], pts[i + 1][0], pts[i + 1][1], pts[i + 1][2], 0.08F, c, 0.7F);
                }
            }
            default -> { }
        }
    }

    private static final float[] YELLOW = {1F, 214 / 255F, 90 / 255F};

    /** A glowing line in model units: two crossed quads along the segment. */
    static void line(VertexConsumer vc, Matrix4f m, float x0, float y0, float z0, float x1, float y1, float z1, float width,
                     float[] c, float a) {
        float dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-4F || a <= 0.005F) return;
        dx /= len; dy /= len; dz /= len;
        // u = d x up (or d x east when d is vertical), v = d x u
        float ux = -dz, uy = 0, uz = dx;
        if (Math.abs(dy) > 0.95F) { ux = 0; uy = dz; uz = -dy; }
        float ul = (float) Math.sqrt(ux * ux + uy * uy + uz * uz);
        ux /= ul; uy /= ul; uz /= ul;
        float vx = dy * uz - dz * uy, vy = dz * ux - dx * uz, vz = dx * uy - dy * ux;
        float w = width / 2;
        for (int pass = 0; pass < 2; pass++) {
            float ox = (pass == 0 ? ux : vx) * w, oy = (pass == 0 ? uy : vy) * w, oz = (pass == 0 ? uz : vz) * w;
            vc.addVertex(m, (x0 - ox) / 16F, (y0 - oy) / 16F, (z0 - oz) / 16F).setColor(c[0], c[1], c[2], a);
            vc.addVertex(m, (x0 + ox) / 16F, (y0 + oy) / 16F, (z0 + oz) / 16F).setColor(c[0], c[1], c[2], a);
            vc.addVertex(m, (x1 + ox) / 16F, (y1 + oy) / 16F, (z1 + oz) / 16F).setColor(c[0], c[1], c[2], a);
            vc.addVertex(m, (x1 - ox) / 16F, (y1 - oy) / 16F, (z1 - oz) / 16F).setColor(c[0], c[1], c[2], a);
        }
    }

    /** A flat glowing square of half-size {@code half} at height {@code y} round the chamber axis (model units). */
    static void plate(VertexConsumer vc, Matrix4f m, float y, float half, float[] c, float a) {
        float x0 = (8 - half) / 16F, x1 = (8 + half) / 16F, yy = y / 16F;
        vc.addVertex(m, x0, yy, x0).setColor(c[0], c[1], c[2], a);
        vc.addVertex(m, x1, yy, x0).setColor(c[0], c[1], c[2], a);
        vc.addVertex(m, x1, yy, x1).setColor(c[0], c[1], c[2], a);
        vc.addVertex(m, x0, yy, x1).setColor(c[0], c[1], c[2], a);
    }

    /** A square outline of half-size {@code half} at height {@code y} (model units). */
    static void square(VertexConsumer vc, Matrix4f m, float y, float half, float[] c, float a) {
        float lo = 8 - half, hi = 8 + half;
        line(vc, m, lo, y, lo, hi, y, lo, 0.1F, c, a);
        line(vc, m, hi, y, lo, hi, y, hi, 0.1F, c, a);
        line(vc, m, hi, y, hi, lo, y, hi, 0.1F, c, a);
        line(vc, m, lo, y, hi, lo, y, lo, 0.1F, c, a);
    }

    /** World light on the atoms: block or sky light (darkened at night); a working chamber lights its sample. */
    static float brightness(Level level, int packedLight, boolean working) {
        int block = LightTexture.block(packedLight), sky = Math.max(0, LightTexture.sky(packedLight) - level.getSkyDarken());
        float f = 0.25F + 0.75F * Math.max(block, sky) / 15F;
        return working ? Math.max(f, 0.85F) : f;
    }

    static void cube(VertexConsumer vc, Matrix4f m, float x, float y, float z, float h, int rgb, float bright, float alpha) {
        cubeRgb(vc, m, x, y, z, h, (rgb >> 16 & 255) / 255F * bright, (rgb >> 8 & 255) / 255F * bright, (rgb & 255) / 255F * bright, alpha);
    }

    /** A small cube of half-size {@code h} (block units) with vanilla face shading. */
    static void cubeRgb(VertexConsumer vc, Matrix4f m, float x, float y, float z, float h, float r, float g, float b, float a) {
        float x0 = x - h, x1 = x + h, y0 = y - h, y1 = y + h, z0 = z - h, z1 = z + h;
        quad(vc, m, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, r * UP, g * UP, b * UP, a);
        quad(vc, m, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, r * DOWN, g * DOWN, b * DOWN, a);
        quad(vc, m, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0, r * NS, g * NS, b * NS, a);
        quad(vc, m, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, r * NS, g * NS, b * NS, a);
        quad(vc, m, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, r * EW, g * EW, b * EW, a);
        quad(vc, m, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1, r * EW, g * EW, b * EW, a);
    }

    private static void quad(VertexConsumer vc, Matrix4f m, float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz, float r, float g, float b, float a) {
        vc.addVertex(m, ax, ay, az).setColor(r, g, b, a);
        vc.addVertex(m, bx, by, bz).setColor(r, g, b, a);
        vc.addVertex(m, cx, cy, cz).setColor(r, g, b, a);
        vc.addVertex(m, dx, dy, dz).setColor(r, g, b, a);
    }

    @Override
    public int getViewDistance() {
        return VIEW_DISTANCE;
    }
}
