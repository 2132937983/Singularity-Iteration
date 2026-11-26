// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.molecular;

import java.util.ArrayList;
import java.util.List;

/**
 * 0.1.11.214 (MT1 X2): where each atom of the molecular transformer's sample is, and its colour, while one item is made.
 * Plain Java (no client classes), so the server-side tests can run it; the renderer only draws what this returns.
 *
 * <p>Coordinates are model units (0..16 across the block), with the sample centre at (8, 8, 8). Time {@code t} is in seconds,
 * progress {@code p} runs 0..1 per item. One item:
 * <ol>
 *   <li>the input's pixels come loose one by one (p 0.05..0.34) and fly into the process "cloud";</li>
 *   <li>the cloud moves the way its process does (below);</li>
 *   <li>the atoms settle into the product's pixels (p 0.66..0.93), turning from the input's colours to the product's;</li>
 *   <li>the finished product fades out at the end (p 0.975..1) and the next input pops in.</li>
 * </ol>
 * The four processes (MolecularProcess kinds) follow the four steps the machine screen already names:
 * <ul>
 *   <li><b>molecular</b> (dissociate, sort, synthesise, crystallise): six molecules turn round the centre; halfway their
 *       members change molecule; each atom is bonded to its molecule's centre;</li>
 *   <li><b>lattice</b> (heat, press, nucleate, grow): the atoms stay together as a layered lattice that shakes with heat and is
 *       pressed flat every half second, then turns into a cubic lattice;</li>
 *   <li><b>nuclear</b> (dissociate, confine, transmute, deposit): the atoms close into one nucleus; a beam from the upper
 *       contact strikes it every 0.6 s and throws out tracks that curl in the field;</li>
 *   <li><b>exotic</b> (no real process): atoms jump about, flicker cyan, white and magenta, and broken tracks kink across the
 *       chamber. Only this process shows a colour other than cyan.</li>
 * </ul>
 * A recipe with no process (unknown) moves as molecular.
 */
public final class MolecularMotion {
    private MolecularMotion() {}

    public static final float CENTRE = 8F, LO = 3.6F, HI = 12.4F, IN0 = 4.35F, IN1 = 11.65F;
    /** One atom: the 5-unit sample over 16 pixels. */
    public static final float PIX = 5F / MolecularAtoms.GRID;
    /** Accent colours: cyan (the machine), white, magenta (exotic only). */
    public static final float[] CYAN = {42 / 255F, 222 / 255F, 211 / 255F}, CYAN2 = {2 / 255F, 209 / 255F, 243 / 255F},
        WHITE = {232 / 255F, 1F, 1F}, MAGENTA = {1F, 92 / 255F, 196 / 255F}, HEAT = {1F, 170 / 255F, 110 / 255F};

    /** Process kinds; unknown moves as molecular. */
    public enum Kind {
        MOLECULAR, LATTICE, NUCLEAR, EXOTIC;

        public static Kind of(String kind) {
            return switch (kind == null ? "" : kind) {
                case "lattice" -> LATTICE;
                case "nuclear" -> NUCLEAR;
                case "exotic" -> EXOTIC;
                default -> MOLECULAR;
            };
        }
    }

    /** One atom this frame. */
    public static final class State {
        public float x, y, z, r, g, b, alpha, size;
        /** 0 on the input or the product, 1 in the process cloud. */
        public float cloud;
        /** The molecule centre the atom is bonded to (molecular only), or NaN. */
        public float ax = Float.NaN, ay, az;
        public boolean visible;
    }

    // ------------------------------------------------------------------ small maths

    /** A deterministic 0..1 value for {@code n} (the same as the demo page). */
    public static double hash(int n) {
        n += 0x6D2B79F5;
        n = (n ^ (n >>> 15)) * (n | 1);
        n ^= n + (n ^ (n >>> 7)) * (n | 61);
        return ((n ^ (n >>> 14)) & 0xFFFFFFFFL) / 4294967296.0;
    }

    public static float ease(double t) {
        if (t <= 0) return 0;
        if (t >= 1) return 1;
        return (float) (t * t * (3 - 2 * t));
    }

    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }

    private static float clamp(float v, float a, float b) { return v < a ? a : v > b ? b : v; }

    /** The atoms in a fixed shuffled order (the order they come loose / settle in). */
    public static List<MolecularAtoms.Atom> shuffled(List<MolecularAtoms.Atom> atoms, int salt) {
        List<MolecularAtoms.Atom> out = new ArrayList<>(atoms);
        out.sort((a, b) -> Double.compare(hash(a.i() * 41 + a.j() * 7 + salt), hash(b.i() * 41 + b.j() * 7 + salt)));
        return out;
    }

    /** Pixel (i, j) of a sample of scale {@code sc} turned by {@code angle} round the vertical axis through the centre. */
    static void spritePos(MolecularAtoms.Atom a, float angle, float sc, float[] out) {
        float lx = (a.i() - 7.5F) * PIX * sc, ly = (7.5F - a.j()) * PIX * sc;
        out[0] = CENTRE + lx * (float) Math.cos(angle);
        out[1] = CENTRE + ly;
        out[2] = CENTRE + lx * (float) Math.sin(angle);
    }

    static void inBox(float[] p) {
        p[0] = clamp(p[0], IN0, IN1);
        p[1] = clamp(p[1], LO + 0.2F, HI - 0.2F);
        p[2] = clamp(p[2], IN0, IN1);
    }

    // ------------------------------------------------------------------ the process clouds

    /** Lattice press pulse 0..1 (every 0.5 s, strong while pressing, p 0.22..0.55). */
    public static float latticePress(double p, double t) {
        double s = Math.max(0, Math.sin(t * Math.PI * 2 / 0.5));
        return (float) (Math.pow(s, 6) * (p > 0.22 && p < 0.55 ? 1 : 0.25));
    }

    /** Where atom {@code k} of {@code n} is in the cloud of {@code kind}; {@code anchor} gets the molecule centre (molecular). */
    static void cloud(Kind kind, int k, int n, double p, double t, float[] out, float[] anchor) {
        double r = hash(k * 7 + 1), r2 = hash(k * 13 + 5), r3 = hash(k * 29 + 11);
        anchor[0] = Float.NaN;
        switch (kind) {
            case MOLECULAR -> {
                int molecules = 6, c1 = k % molecules, c2 = (int) (hash(k * 3 + 2) * molecules);
                float w = ease((p - 0.42) / 0.16);
                float[] a = new float[3], b = new float[3];
                molecule(c1, t, a);
                molecule(c2, t, b);
                for (int i = 0; i < 3; i++) anchor[i] = lerp(a[i], b[i], w);
                double s = t * 2.2 + r * 6.28;
                out[0] = anchor[0] + (float) (Math.cos(s) * 0.75 * r2);
                out[1] = anchor[1] + (float) (Math.sin(s * 1.3) * 0.75 * r3);
                out[2] = anchor[2] + (float) (Math.sin(s) * 0.75 * r2);
            }
            case LATTICE -> {
                int side = 4, g = k % (side * side * side), ix = g % side, iy = (g / side) % side, iz = g / (side * side);
                float cubic = ease((p - 0.45) / 0.25);
                float sx = lerp(0.95F, 1.2F, cubic), sy = lerp(1.75F, 1.2F, cubic);
                float press = latticePress(p, t);
                double turn = t * 0.45 + cubic * Math.PI / 4;
                float x = (ix - 1.5F) * sx, y = (iy - 1.5F) * sy * (1 - 0.18F * press), z = (iz - 1.5F) * sx;
                float heat = p < 0.3 ? 0.12F : 0.03F;
                float rx = (float) (x * Math.cos(turn) - z * Math.sin(turn)), rz = (float) (x * Math.sin(turn) + z * Math.cos(turn));
                out[0] = CENTRE + rx + (float) Math.sin(t * 31 + k) * heat;
                out[1] = CENTRE + y + (float) Math.sin(t * 27 + k * 2) * heat;
                out[2] = CENTRE + rz + (float) Math.sin(t * 23 + k * 3) * heat;
            }
            case NUCLEAR -> {
                double phi = Math.acos(1 - 2 * (k + 0.5) / Math.max(1, n)), th = Math.PI * (1 + Math.sqrt(5)) * k;
                double pulse = Math.pow(Math.max(0, 1 - ((t % 0.6) / 0.6) * 4), 2);
                double rad = 1.0 + 0.35 * pulse + 0.05 * Math.sin(t * 9 + k);
                out[0] = CENTRE + (float) (rad * Math.sin(phi) * Math.cos(th + t * 1.5));
                out[1] = CENTRE + (float) (rad * Math.cos(phi));
                out[2] = CENTRE + (float) (rad * Math.sin(phi) * Math.sin(th + t * 1.5));
            }
            case EXOTIC -> {
                int slot = (int) Math.floor(t / 0.16 + r * 3);
                out[0] = lerp(IN0 + 0.3F, IN1 - 0.3F, (float) hash(slot * 97 + k));
                out[1] = lerp(LO + 0.6F, HI - 0.6F, (float) hash(slot * 61 + k * 3));
                out[2] = lerp(IN0 + 0.3F, IN1 - 0.3F, (float) hash(slot * 43 + k * 5));
            }
        }
    }

    private static void molecule(int c, double t, float[] out) {
        double a = t * (0.7 + 0.12 * c) + c * Math.PI / 3, rad = 2.0 + 0.7 * (c % 2);
        out[0] = CENTRE + (float) (Math.cos(a) * rad);
        out[1] = CENTRE + (float) (Math.sin(t * 1.1 + c) * 1.6);
        out[2] = CENTRE + (float) (Math.sin(a) * rad);
    }

    // ------------------------------------------------------------------ one atom

    /**
     * Atom {@code k} while one item is made. {@code in} and {@code out} are the shuffled atoms of input and product; atoms
     * beyond the input's count appear in the cloud, atoms beyond the product's count fade out before it settles.
     */
    public static void single(Kind kind, int k, List<MolecularAtoms.Atom> in, List<MolecularAtoms.Atom> out, double p, double t,
                              float angle, State s) {
        int n = Math.max(in.size(), out.size());
        MolecularAtoms.Atom src = k < in.size() ? in.get(k) : null, dst = k < out.size() ? out.get(k) : null;
        double loose = 0.05 + hash(k * 5 + 3) * 0.2, settled = 0.76 + hash(k * 9 + 1) * 0.17;
        float[] a = {CENTRE, CENTRE, CENTRE}, b = {CENTRE, CENTRE, CENTRE}, c = new float[3], anchor = new float[3];
        if (src != null) spritePos(src, angle, ease(p / 0.05), a);
        if (dst != null) spritePos(dst, angle, 1, b);
        cloud(kind, k, n, p, t, c, anchor);
        inBox(c);
        float cloudW, alpha = 1;
        float[] pos;
        s.visible = true;
        if (p < loose) {
            if (src == null) { s.visible = false; return; }
            pos = a; cloudW = 0;
        } else if (p < loose + 0.09) {
            float w = ease((p - loose) / 0.09);
            pos = mix(src != null ? a : c, c, w); cloudW = w;
            if (src == null) alpha = w;
        } else if (p < settled - 0.1) {
            pos = c; cloudW = 1;
        } else if (p < settled) {
            float w = ease((p - (settled - 0.1)) / 0.1);
            pos = mix(c, b, w); cloudW = 1 - w;
            if (dst == null) alpha = 1 - w;
        } else {
            if (dst == null) { s.visible = false; return; }
            pos = b; cloudW = 0;
        }
        if (p > 0.975) alpha *= (float) (1 - (p - 0.975) / 0.025);
        s.x = pos[0]; s.y = pos[1]; s.z = pos[2];
        s.alpha = alpha;
        s.cloud = cloudW;
        s.size = PIX * lerp(1, 1.45F, cloudW);
        s.ax = Float.NaN;
        if (kind == Kind.MOLECULAR && !Float.isNaN(anchor[0])) { s.ax = anchor[0]; s.ay = anchor[1]; s.az = anchor[2]; }
        colour(kind, src != null ? src.rgb() : dst.rgb(), dst != null ? dst.rgb() : src.rgb(), k, p, t, cloudW, s);
    }

    private static float[] mix(float[] a, float[] b, float w) {
        return new float[]{lerp(a[0], b[0], w), lerp(a[1], b[1], w), lerp(a[2], b[2], w)};
    }

    /** Input colour to product colour, tinted by the process while in the cloud. */
    static void colour(Kind kind, int src, int dst, int k, double p, double t, float cloudW, State s) {
        float w = ease((p - 0.55) / 0.35);
        float r = lerp((src >> 16 & 255) / 255F, (dst >> 16 & 255) / 255F, w);
        float g = lerp((src >> 8 & 255) / 255F, (dst >> 8 & 255) / 255F, w);
        float b = lerp((src & 255) / 255F, (dst & 255) / 255F, w);
        float[] tint;
        float amount;
        switch (kind) {
            case NUCLEAR -> { tint = WHITE; amount = 0.75F; }
            case LATTICE -> { tint = p < 0.3 ? HEAT : CYAN; amount = 0.35F; }
            case EXOTIC -> {
                double f = hash((int) Math.floor(t * 12) + k * 7);
                tint = f < 0.33 ? CYAN : f < 0.66 ? WHITE : MAGENTA; amount = 0.8F;
            }
            default -> { tint = CYAN; amount = 0.4F; }
        }
        r = lerp(r, tint[0], cloudW * amount); g = lerp(g, tint[1], cloudW * amount); b = lerp(b, tint[2], cloudW * amount);
        float lift = cloudW * 0.18F;
        s.r = lerp(r, WHITE[0], lift); s.g = lerp(g, WHITE[1], lift); s.b = lerp(b, WHITE[2], lift);
    }

    /** Whether atom {@code k}'s glow is magenta this frame (exotic only). */
    public static boolean magentaGlow(Kind kind, int k, double t) {
        return kind == Kind.EXOTIC && hash(k + (int) Math.floor(t * 12)) > 0.7;
    }

    // ------------------------------------------------------------------ process extras

    /** The nuclear beam's strength 0..1 (a flash at the start of each 0.6 s pulse), during p 0.15..0.8. */
    public static float nuclearBeam(double p, double t) {
        if (p <= 0.15 || p >= 0.8) return 0;
        double since = t - Math.floor(t / 0.6) * 0.6;
        return since < 0.09 ? (float) (1 - since / 0.09) : 0;
    }

    /**
     * Point {@code q} (0..17) of track {@code s} (0..3) thrown out by pulse {@code m}, {@code age} seconds after it, into
     * {@code out}; returns the point's brightness, or 0 when the point is not drawn.
     */
    public static float nuclearTrack(int m, int s, int q, double age, float[] out) {
        if (age < 0 || age > 1) return 0;
        double u = q / 18.0;
        if (u > age * 1.6) return 0;
        double dir = hash(m * 31 + s * 7) * 6.283, curl = (hash(m * 7 + s) > 0.5 ? 1 : -1) * (1.5 + hash(s + m) * 2.5);
        boolean tight = hash(m * 13 + s) > 0.65;
        double len = u * (tight ? 3.6 : 5.2), rad = tight ? Math.max(0.15, 1.6 * (1 - u)) : len, ang = dir + curl * u * (tight ? 3 : 1);
        out[0] = CENTRE + (float) (Math.cos(ang) * rad * 0.9);
        out[1] = CENTRE + (float) ((hash(m + s * 3) - 0.5) * u * 3);
        out[2] = CENTRE + (float) (Math.sin(ang) * rad * 0.9);
        inBox(out);
        return (float) ((1 - age) * (0.5 + 0.5 * (1 - u)));
    }

    /**
     * Glitch track {@code q} (0..2) of the exotic process this frame: five points in {@code out} (5 x 3), or false when this
     * track is not drawn. {@code magenta[0]} says whether it is drawn magenta.
     */
    public static boolean exoticTrack(double p, double t, int q, float[][] out, boolean[] magenta) {
        if (p <= 0.15 || p >= 0.8) return false;
        int s = (int) Math.floor(t * 9);
        double h = hash(s * 5 + q);
        if (h < 0.4) return false;
        magenta[0] = h > 0.8;
        out[0][0] = lerp(IN0, IN1, (float) hash(s + q * 9));
        out[0][1] = lerp(LO + 1, HI - 1, (float) hash(s * 3 + q));
        out[0][2] = lerp(IN0, IN1, (float) hash(s * 7 + q));
        for (int seg = 1; seg < 5; seg++) {
            out[seg][0] = out[seg - 1][0] + (float) ((hash(s + seg * 11 + q) - 0.5) * 3);
            out[seg][1] = out[seg - 1][1] + (float) ((hash(s * 2 + seg) - 0.5) * 3);
            out[seg][2] = out[seg - 1][2] + (float) ((hash(s + seg * 5) - 0.5) * 3);
            inBox(out[seg]);
        }
        return true;
    }

    /** The phase 0..3 of the machine screen's four steps at progress {@code p}. */
    public static int phase(double p) {
        return Math.min(3, (int) Math.floor(Math.max(0, p) * 4));
    }
}
