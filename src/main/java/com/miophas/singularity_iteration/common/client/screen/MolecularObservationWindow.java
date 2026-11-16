// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.client.suit.AsciiHud;
import com.miophas.singularity_iteration.common.client.suit.FcsDraw;
import com.miophas.singularity_iteration.common.recipe.molecular_transformer.MolecularProcess;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.joml.Matrix4f;

/**
 * 0.1.11.6: the observation window of the Molecular Transformer screen. It shows the process of the running recipe
 * ({@link MolecularProcess}) as a real machine would:
 *
 * <ul>
 *   <li><b>Core.</b> Nuclear, molecular, exotic and unmarked recipes: a Penning ion trap. The tilted ellipses are the
 *       field lines; the ions follow the three motions of a real trap (fast cyclotron circles, the slow magnetron drift,
 *       the axial bob) and change from the input colour to the product colour as the dose rises. Lattice recipes: a
 *       rotating atom lattice that turns from graphite layers into diamond, nucleating from the centre outward.</li>
 *   <li><b>Beat.</b> The accelerator (or the press) fires in trains of pulses and pauses for the trap to cool. The ring
 *       of read-outs, the outward rays and the beam all follow it. Less power gives sparser, weaker trains.</li>
 *   <li><b>Read-out ring.</b> Eight small sensor panels turning around the core (atomic number, mass spectrum, beam
 *       current, field, vacuum, trap temperature, dose, nuclide; or pressure, temperature, sp3 share for a lattice: order, pressure, temperature, nuclei, grain).
 *       Every value comes from the recipe and the progress.</li>
 *   <li><b>Text column.</b> Process class, path, element or structure change, the phase strip, the dose bar, EU, power
 *       and the time per item.</li>
 * </ul>
 * A recipe that finishes in under 2 s runs as a continuous process: no phases, the dose bar shows items per second.
 * Idle: the core dims, the ring stops, the panels read STBY. All drawing is client side; the screen only needs the
 * progress, the power and the recipe (the client has the recipes).
 */
public final class MolecularObservationWindow {

    /** What the screen knows this frame. */
    public record Snapshot(MolecularProcess process, boolean hasRecipe, boolean working, float progress, long recipeEU, long euPerTick,
                           Component inName, Component outName, int inHash, int outHash) { }

    private static final float[] EDGES = {0F, 0.12F, 0.22F, 0.88F, 1F};
    /** The machine's nominal draw (DEFAULT_ENERGY_PER_TICK); below a quarter of it the window reads "low power". */
    private static final long NOMINAL_EU_T = 32768;
    /** Faster than this per item the window shows a continuous process. */
    private static final double CONTINUOUS_SECONDS = 2.0;

    private static final int ACC = 0x6FE3C8, TXT = 0xE9FFF6, DIM = 0x9FC9BB, WARN = 0xFFD166, CRIT = 0xFF6B5E, MUTE = 0x506468;
    private static final String K = "gui.mio_icif.mt.";

    private final Random rnd = new Random();
    private final Ion[] ions = new Ion[46];
    private float ringAngle, ringVel = 0.15F, trapHeat;
    private final List<Pulse> pulses = new ArrayList<>();
    private final List<Ray> rays = new ArrayList<>();
    private final float[] beamHist = new float[24];
    private Pulse lastPulse;
    private double nextTrain, doneFlashAt = -10, now;
    private long lastNanos;
    private float shown = -1;
    /** Text scale of this frame: one font texel on whole screen pixels (2 / GUI scale); fractional scales drop rows. */
    private float u = 0.667F;
    /** GUI scale 2 or less: the text is large for the window, so half the panels and no notes. */
    private boolean compact;
    private String ionsFor = "";

    private static final class Pulse { double t; float s; int i, n; boolean fired; }
    private record Ray(double born, float ang, int layer, float len, float s, float life) { }
    private record Ion(float rm, float thm, float wm, float rc, float thc, float wc, float az, float tha, float wa, float thr, int sp) { }
    private record Atom(float[] g, float[] d, float r) { }
    private record Readout(String label, String value, int kind, boolean warn, float bar, float[][] spec) {
        static final int VALUE = 0, SPEC = 1, SPARK = 2, BAR = 3;
    }

    private static final Atom[] LATTICE = buildLattice();

    // ------------------------------------------------------------------ entry

    /** Draws the window into (x, y, w, h), GUI coordinates. */
    public void render(GuiGraphics g, int x, int y, int w, int h, Snapshot s) {
        long t = System.nanoTime();
        float dt = lastNanos == 0 ? 0.016F : Math.min(0.05F, (t - lastNanos) / 1.0E9F);
        lastNanos = t;
        now += dt;
        double guiScale = net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScale();
        u = (float) Math.min(1.0, 2.0 / Math.max(1.0, guiScale));
        compact = u > 0.7F;
        MolecularProcess proc = s.process() == null ? MolecularProcess.UNKNOWN : s.process();
        String kind = proc.kind();
        boolean idle = !s.working() || s.recipeEU() <= 0;
        double secs = s.euPerTick() > 0 ? s.recipeEU() / (double) s.euPerTick() / 20.0 : Double.POSITIVE_INFINITY;
        boolean continuous = !idle && secs < CONTINUOUS_SECONDS;
        float level = idle ? 0 : (float) Math.min(1.0, s.euPerTick() / (double) NOMINAL_EU_T);
        boolean low = !idle && s.euPerTick() < NOMINAL_EU_T / 4;
        float live = idle ? 0.25F : low ? 0.6F : 1F;
        String key = kind + s.inHash();
        if (!key.equals(ionsFor)) { buildIons(key.hashCode()); ionsFor = key; }

        // progress: smoothed; a drop means an item finished
        float target = Math.max(0, Math.min(1, s.progress()));
        if (shown < 0 || idle) shown = target;
        else if (target < shown - 0.3F) { doneFlashAt = now; shown = target; }
        else shown += (target - shown) * Math.min(1F, dt * 10F);
        float p = continuous ? 0.55F : shown;
        int ph = continuous ? 2 : phase(p);
        float conv = continuous ? 0.5F : ph < 2 ? 0 : ph == 2 ? phaseFrac(p) : 1;

        // the pulse clock
        boolean pulsing = !idle && ("lattice".equals(kind) ? ph >= 1 && ph <= 3 : ph == 2);
        schedule(pulsing, level, low);
        fire(live);
        float beat = idle ? 0 : beatEnv();
        if (!idle) {
            ringVel += (0.12F - ringVel) * Math.min(1F, dt * 0.6F);
            ringVel *= (float) Math.exp(-dt * 1.6F);
            ringAngle += (ringVel + 0.08F) * dt;
        }
        trapHeat = Math.max(0, trapHeat - dt * (low ? 0.18F : 0.1F));

        int cin = colourIn(kind), cout = colourOut(kind);
        // glass and grid
        g.fill(x, y, x + w, y + h, 0xFF05090B);
        g.enableScissor(x, y, x + w, y + h);
        float cx = x + w * 0.33F, cy = y + h * 0.47F;
        float R = Math.min(h * 0.27F, w * 0.22F);
        FcsDraw.disc(g, cx, cy, R * 2.2F, FcsDraw.argb(0x0D1A1D, 0.9F));
        FcsDraw.disc(g, cx, cy, R * 1.4F, FcsDraw.argb(0x10211F, 0.7F));
        for (int gx = x + 8; gx < x + w; gx += 8) g.fill(gx, y, gx + 1, y + h, FcsDraw.argb(ACC, 0.04F));
        for (int gy = y + 8; gy < y + h; gy += 8) g.fill(x, gy, x + w, gy + 1, FcsDraw.argb(ACC, 0.04F));
        float ringRx = Math.min(R * 2.0F, w * 0.33F - 20), ringRy = R * 0.85F;

        // the beamline from the left edge into the core
        FcsDraw.line(g, x, cy - 1.5F, cx - R * 1.1F, cy - 1.5F, 0.6F, FcsDraw.argb(DIM, 0.18F));
        FcsDraw.line(g, x, cy + 1.5F, cx - R * 1.1F, cy + 1.5F, 0.6F, FcsDraw.argb(DIM, 0.18F));
        if (beat > 0.05F && !"lattice".equals(kind)) {
            gradLine(g, x, cy, cx, cy, 0.6F + beat * 1.2F, FcsDraw.argb(0x8CDCFF, 0), FcsDraw.argb(0xC8F5FF, Math.min(1, 0.9F * beat * live)));
        }
        ellipse(g, cx, cy, ringRx, ringRy, 0, (float) Math.PI, (float) (Math.PI * 2), 0.5F, FcsDraw.argb(ACC, 0.16F * live + beat * 0.15F));
        // rays
        int rayCol = "exotic".equals(kind) ? 0xFF96BE : "lattice".equals(kind) ? 0xC8FFAA : 0xBEEBFF;
        for (Ray ray : rays) {
            float age = (float) ((now - ray.born) / ray.life);
            float ca = (float) Math.cos(ray.ang), sa = (float) Math.sin(ray.ang);
            float x0 = cx + ringRx * ca, y0 = cy + ringRy * sa;
            float len = R * 2.6F * ray.len * (0.4F + 0.6F * Math.min(1, age * 3));
            float a = (1 - age) * (1 - age) * new float[]{0.25F, 0.45F, 0.8F}[ray.layer] * ray.s * live;
            gradLine(g, x0, y0, x0 + ca * len, y0 + sa * len * 0.6F, new float[]{0.4F, 0.6F, 0.9F}[ray.layer], FcsDraw.argb(rayCol, a), FcsDraw.argb(rayCol, 0));
        }

        List<Readout> rd = readouts(kind, proc, p, ph, conv, beat, s);
        // six panels around the ring (the field and the last read-out stay off: eight overlapped), four when compact
        int[] pick = compact ? new int[]{0, 1, 2, 6} : new int[]{0, 1, 2, 4, 5, 6};
        List<Readout> shownRd = new ArrayList<>(pick.length);
        for (int i : pick) shownRd.add(rd.get(i));
        rd = shownRd;
        int n = rd.size();
        for (int pass = 0; pass < 2; pass++) {
            if (pass == 1) {
                // the core between the back and the front of the ring
                if ("lattice".equals(kind)) lattice(g, cx, cy, R, p, conv, live, beat, cin, cout);
                else trap(g, cx, cy, R, ph, phaseFrac(p), conv, live, beat, kind, idle, cin, cout);
                ellipse(g, cx, cy, ringRx, ringRy, 0, 0, (float) Math.PI, 0.5F, FcsDraw.argb(ACC, 0.35F * live + beat * 0.15F));
            }
            for (int i = 0; i < n; i++) {
                float ang = ringAngle + i * (float) Math.PI * 2 / n;
                float depth = (float) Math.sin(ang);
                if ((pass == 0) == (depth < 0)) panel(g, rd.get(i), cx + ringRx * (float) Math.cos(ang), cy + ringRy * depth, depth, live, idle);
            }
        }
        // deposition stream down to the output slot side
        if (!idle && (ph == 3 || continuous)) {
            float tx = x + 2, ty = y + h * 0.82F;
            for (int i = 0; i < 6; i++) {
                float k = (float) ((now * 0.9 + i / 6.0) % 1.0);
                float px = cx + (tx - cx) * k, py = cy + (ty - cy) * k + (float) Math.sin(k * 9 + i) * 2;
                fillF(g, px - 0.6F, py - 0.6F, 1.2F, 1.2F, FcsDraw.argb(cout, (1 - k) * 0.9F * live));
            }
        }
        float fl = (float) (now - doneFlashAt);
        if (fl < 0.5F) g.fill(x, y, x + w, y + h, FcsDraw.argb(cout, (1 - fl / 0.5F) * 0.22F));

        textColumn(g, x, y, w, h, kind, proc, p, ph, conv, s, idle, low, continuous, secs);

        // scan lines
        for (int sy = y; sy < y + h; sy += 2) g.fill(x, sy, x + w, sy + 1, 0x14000000);
        g.flush();
        g.disableScissor();
        // frame and corner ticks
        g.fill(x - 1, y - 1, x + w + 1, y, 0xFF2B3A40);
        g.fill(x - 1, y + h, x + w + 1, y + h + 1, 0xFF2B3A40);
        g.fill(x - 1, y, x, y + h, 0xFF2B3A40);
        g.fill(x + w, y, x + w + 1, y + h, 0xFF2B3A40);
        int tick = FcsDraw.argb(ACC, 0.6F);
        g.fill(x, y, x + 5, y + 1, tick); g.fill(x, y, x + 1, y + 5, tick);
        g.fill(x + w - 5, y, x + w, y + 1, tick); g.fill(x + w - 1, y, x + w, y + 5, tick);
        g.fill(x, y + h - 1, x + 5, y + h, tick); g.fill(x, y + h - 5, x + 1, y + h, tick);
        g.fill(x + w - 5, y + h - 1, x + w, y + h, tick); g.fill(x + w - 1, y + h - 5, x + w, y + h, tick);
    }

    // ------------------------------------------------------------------ pulse clock

    private void schedule(boolean pulsing, float level, boolean low) {
        if (!pulsing || now < nextTrain) return;
        int n = low ? 1 + rnd.nextInt(2) : 2 + Math.round(level * 2) + rnd.nextInt(2);
        double t = now;
        for (int i = 0; i < n; i++) {
            t += (low ? 0.4 : 0.16) + rnd.nextDouble() * (low ? 0.3 : 0.16);
            Pulse p = new Pulse();
            p.t = t; p.s = (low ? 0.45F : 0.8F) + rnd.nextFloat() * 0.2F; p.i = i + 1; p.n = n;
            pulses.add(p);
        }
        nextTrain = t + 0.7 + (1 - level) * 1.9 + rnd.nextDouble() * 0.9;
    }

    private void fire(float live) {
        for (Pulse p : pulses) {
            if (p.fired || now < p.t) continue;
            p.fired = true;
            ringVel += 0.9F * p.s * (rnd.nextFloat() < 0.25F ? -0.6F : 1F);   // a stutter now and then
            trapHeat = Math.min(1, trapHeat + 0.08F * p.s);
            System.arraycopy(beamHist, 1, beamHist, 0, beamHist.length - 1);
            beamHist[beamHist.length - 1] = p.s * (0.75F + rnd.nextFloat() * 0.25F);
            lastPulse = p;
            int k = 8 + rnd.nextInt(8);
            for (int i = 0; i < k; i++) {
                int layer = rnd.nextFloat() < 0.35F ? 0 : rnd.nextFloat() < 0.6F ? 1 : 2;   // far, mid, near
                rays.add(new Ray(now, rnd.nextFloat() * (float) Math.PI * 2, layer,
                    new float[]{0.35F, 0.6F, 0.9F}[layer] * (0.6F + rnd.nextFloat() * 0.6F), p.s, 0.35F + rnd.nextFloat() * 0.35F));
            }
        }
        pulses.removeIf(p -> now - p.t > 1.5);
        rays.removeIf(r -> now - r.born >= r.life);
        if (rays.size() > 96) rays.subList(0, rays.size() - 96).clear();
    }

    private float beatEnv() {
        float e = 0;
        for (Pulse p : pulses) {
            double d = now - p.t;
            if (d >= 0 && d < 1.2) e += p.s * (float) Math.exp(-d * 7);
        }
        return Math.min(1.4F, e);
    }

    private static int phase(float p) { return p < EDGES[1] ? 0 : p < EDGES[2] ? 1 : p < EDGES[3] ? 2 : 3; }

    private static float phaseFrac(float p) {
        int i = phase(p);
        return (p - EDGES[i]) / (EDGES[i + 1] - EDGES[i]);
    }

    // ------------------------------------------------------------------ core: ion trap

    private void buildIons(int seed) {
        Random r = new Random(seed);
        for (int i = 0; i < ions.length; i++) {
            ions[i] = new Ion(0.25F + r.nextFloat() * 0.62F, r.nextFloat() * 6.283F, 0.35F + r.nextFloat() * 0.25F,
                0.05F + r.nextFloat() * 0.07F, r.nextFloat() * 6.283F, 9 + r.nextFloat() * 6,
                0.12F + r.nextFloat() * 0.18F, r.nextFloat() * 6.283F, 2.2F + r.nextFloat() * 1.2F, r.nextFloat(), r.nextInt(3));
        }
    }

    private void trap(GuiGraphics g, float cx, float cy, float R, int ph, float f, float conv, float live, float beat,
                      String kind, boolean idle, int cin, int cout) {
        float tight = ph == 0 ? 0.4F + f * 0.4F : 1F;          // the trap closes during dissociation
        boolean exo = "exotic".equals(kind);
        boolean mol = "molecular".equals(kind) || "unknown".equals(kind);
        float t = (float) now;
        for (int k = 0; k < 12; k++) {
            float rot = k * (float) Math.PI / 12 + t * (idle ? 0.03F : 0.12F);
            float ry = R * (0.22F + 0.72F * Math.abs((float) Math.sin(k * 1.7F + t * (idle ? 0.1F : 0.35F))));
            float a = (0.08F + 0.1F * tight + beat * 0.12F) * live;
            float jx = exo && rnd.nextFloat() < 0.06F ? (rnd.nextFloat() - 0.5F) * 3 : 0;
            ellipse(g, cx + jx, cy, R * (1.02F - 0.1F * tight), ry * (1.1F - 0.15F * tight), rot, 0, (float) Math.PI * 2, 0.35F,
                FcsDraw.argb(exo && k % 3 == 0 ? 0xFF78AA : ACC, a));
        }
        int glow = mix(cin, cout, conv);
        FcsDraw.disc(g, cx, cy, R * 0.85F, FcsDraw.argb(glow, (0.06F + beat * 0.18F) * live));
        FcsDraw.disc(g, cx, cy, R * 0.45F, FcsDraw.argb(glow, (0.08F + beat * 0.25F) * live));
        if (idle) return;
        float visible = ph == 0 ? f : 1;
        for (int i = 0; i < ions.length; i++) {
            Ion ion = ions[i];
            if (i / (float) ions.length > visible) continue;
            boolean done = mol ? ion.sp == 0 && ph >= 2 && conv > ion.thr * 0.8F : conv > ion.thr;
            int col = mol ? (done ? cout : 0xD27864) : done ? cout : cin;
            if (exo && rnd.nextFloat() < 0.04F) col = 0xFF508C;
            float px = 0, py = 0;
            for (int k = 5; k >= 0; k--) {
                float tt = t - k * 0.018F;
                float thm = ion.thm + ion.wm * tt * (1 + beat * 0.6F), thc = ion.thc + ion.wc * tt, tha = ion.tha + ion.wa * tt;
                float rm = ion.rm * R * (1.05F - 0.25F * tight);
                float qx = cx + rm * (float) Math.cos(thm) + ion.rc * R * (float) Math.cos(thc);
                float qy = cy + rm * (float) Math.sin(thm) * 0.42F + ion.rc * R * (float) Math.sin(thc) * 0.6F + ion.az * R * (float) Math.sin(tha);
                if (k < 5) FcsDraw.line(g, px, py, qx, qy, 0.35F, FcsDraw.argb(col, 0.35F * live));
                px = qx; py = qy;
            }
            fillF(g, px - 0.55F, py - 0.55F, 1.1F, 1.1F, FcsDraw.argb(col, 0.95F * live));
        }
    }

    // ------------------------------------------------------------------ core: lattice

    private static Atom[] buildLattice() {
        List<float[]> gr = new ArrayList<>(), di = new ArrayList<>();
        float a = 0.62F;
        for (int layer = -1; layer <= 1; layer++) {
            float shift = layer == 0 ? a * 0.5F : 0;
            for (int i = -4; i <= 4; i++) for (int j = -4; j <= 4; j++) {
                float x = (i + j * 0.5F) * a + shift, y = j * a * 0.866F;
                if (Math.hypot(x, y) < 1.75) gr.add(new float[]{x, layer * 0.62F, y});
            }
        }
        float c = 0.9F;
        float[][] base = {{0, 0, 0}, {0, .5F, .5F}, {.5F, 0, .5F}, {.5F, .5F, 0}, {.25F, .25F, .25F}, {.25F, .75F, .75F}, {.75F, .25F, .75F}, {.75F, .75F, .25F}};
        for (int i = -2; i <= 1; i++) for (int j = -2; j <= 1; j++) for (int k = -2; k <= 1; k++) for (float[] b : base) {
            float x = (i + b[0]) * c + 0.2F, y = (j + b[1]) * c + 0.2F, z = (k + b[2]) * c + 0.2F;
            if (Math.sqrt(x * x + y * y + z * z) < 1.7) di.add(new float[]{x, y, z});
        }
        java.util.Comparator<float[]> byR = java.util.Comparator.comparingDouble(v -> Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]));
        gr.sort(byR);
        di.sort(byR);
        int n = Math.min(gr.size(), di.size());
        Atom[] out = new Atom[n];
        for (int i = 0; i < n; i++) {
            float[] d = di.get(i);
            out[i] = new Atom(gr.get(i), d, (float) Math.sqrt(d[0] * d[0] + d[1] * d[1] + d[2] * d[2]));
        }
        return out;
    }

    private void lattice(GuiGraphics g, float cx, float cy, float R, float p, float conv, float live, float beat, int cin, int cout) {
        float front = conv * 1.9F;                                   // nucleation front from the centre outward
        float squeeze = 1 - beat * 0.06F;
        float ay = (float) now * 0.35F, ax = 0.45F;
        int n = LATTICE.length;
        float[] sx = new float[n], sy = new float[n], sz = new float[n], kk = new float[n];
        float[][] w = new float[n][];
        for (int i = 0; i < n; i++) {
            Atom a = LATTICE[i];
            float k = Math.max(0, Math.min(1, (front - a.r) / 0.25F + 0.5F));
            float[] s = {a.g[0] + (a.d[0] - a.g[0]) * k, a.g[1] + (a.d[1] - a.g[1]) * k, a.g[2] + (a.d[2] - a.g[2]) * k};
            float x = s[0] * (float) Math.cos(ay) - s[2] * (float) Math.sin(ay), z = s[0] * (float) Math.sin(ay) + s[2] * (float) Math.cos(ay);
            float y2 = s[1] * (float) Math.cos(ax) - z * (float) Math.sin(ax), z2 = s[1] * (float) Math.sin(ax) + z * (float) Math.cos(ax);
            float sc = R * 0.78F * squeeze * (1 + z2 * 0.08F);
            sx[i] = cx + x * sc; sy[i] = cy + y2 * sc; sz[i] = z2; kk[i] = k; w[i] = s;
        }
        for (int i = 0; i < n; i++) for (int j = i + 1; j < n; j++) {
            float dx = w[i][0] - w[j][0], dy = w[i][1] - w[j][1], dz = w[i][2] - w[j][2];
            float d = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            float k = (kk[i] + kk[j]) / 2;
            float lim = k > 0.5F ? 0.42F : 0.66F;
            if (d > lim || (k <= 0.5F && Math.abs(dy) > 0.1F)) continue;
            float depth = 0.6F + 0.4F * (Math.min(sz[i], sz[j]) + 1.5F) / 3F;
            FcsDraw.line(g, sx[i], sy[i], sx[j], sy[j], 0.4F, FcsDraw.argb(mix(cin, cout, k), (0.22F + 0.25F * k + beat * 0.2F) * live * depth));
        }
        for (int i = 0; i < n; i++) {
            float s = 0.7F + kk[i] * 0.4F;
            fillF(g, sx[i] - s / 2, sy[i] - s / 2, s, s, FcsDraw.argb(mix(cin, cout, kk[i]), (0.6F + 0.4F * kk[i]) * live));
        }
        if (beat > 0.05F) {                                          // press anvils close in on each stroke
            for (int k = 0; k < 6; k++) {
                float ang = k * (float) Math.PI / 3 + 0.3F, d = R * (1.25F - beat * 0.15F);
                float px = cx + (float) Math.cos(ang) * d, py = cy + (float) Math.sin(ang) * d * 0.7F;
                float ix = -(float) Math.cos(ang), iy = -(float) Math.sin(ang) * 0.7F;
                int col = FcsDraw.argb(0xC8FFAA, Math.min(1, 0.6F * beat * live));
                FcsDraw.line(g, px - iy * 2.5F - ix * 2, py + ix * 2.5F - iy * 2, px, py, 0.6F, col);
                FcsDraw.line(g, px, py, px + iy * 2.5F - ix * 2, py - ix * 2.5F - iy * 2, 0.6F, col);
            }
        }
    }

    // ------------------------------------------------------------------ read-outs

    private List<Readout> readouts(String kind, MolecularProcess proc, float p, int ph, float conv, float beat, Snapshot s) {
        List<Readout> out = new ArrayList<>(8);
        float heat = trapHeat;
        String beam = String.format(Locale.ROOT, "%.1fµA", beamHist[beamHist.length - 1] * 18);
        String dose = Math.round(p * 100) + "%";
        switch (kind) {
            case "nuclear" -> {
                Elements.E a = Elements.of(proc.from()), b = Elements.of(proc.to());
                int za = a == null ? 0 : a.z(), zb = b == null ? 0 : b.z();
                int ma = a == null ? 0 : a.a(), mb = b == null ? 0 : b.a();
                int z = Math.round(za + (zb - za) * conv), m = Math.round(ma + (mb - ma) * conv);
                String sym = conv >= 1 ? proc.to() : conv <= 0 ? proc.from() : z == za ? proc.from() : z == zb ? proc.to() : "·";
                out.add(value("z", za + "→" + z, false));
                out.add(new Readout(tr("r.spectrum"), "", Readout.SPEC, false, 0,
                    new float[][]{{ma, 1 - conv}, {m, conv > 0 && conv < 1 ? 0.35F : 0}, {mb, conv}}));
                out.add(new Readout(tr("r.beam"), beam, Readout.SPARK, false, 0, null));
                out.add(value("field", "7.00 T", false));
                out.add(value("vacuum", String.format(Locale.ROOT, "%.1fe-9 Pa", 1.8F + heat * 2.6F), heat > 0.75F));
                out.add(value("trap_temp", String.format(Locale.ROOT, "%.1f K", 4.2F + heat * 38), heat > 0.75F));
                out.add(new Readout(tr("r.dose"), dose, Readout.BAR, false, p, null));
                out.add(value("nuclide", m + sym, false));
            }
            case "lattice" -> {
                float f = phaseFrac(p);
                float press = ph == 0 ? 0.1F + f * 0.4F : Math.min(5.5F, 0.5F + (ph == 1 ? f : 1) * 5.0F) + beat * 0.4F;
                int temp = ph == 0 ? Math.round(300 + f * 1400) : 1700 + Math.round((float) Math.sin(now * 1.3) * 12);
                out.add(value("order", Math.round(conv * 100) + "%", false));
                out.add(value("pressure", String.format(Locale.ROOT, "%.2f GPa", press), false));
                out.add(value("temperature", temp + " K", false));
                out.add(value("nuclei", ph >= 2 ? String.valueOf(1 + (int) (conv * 37)) : "—", false));
                out.add(value("lattice", tr(conv < 0.5F ? "v.hexagonal" : "v.cubic"), false));
                out.add(new Readout(tr("r.press"), lastPulse == null ? "—" : lastPulse.i + "/" + lastPulse.n, Readout.SPARK, false, 0, null));
                out.add(new Readout(tr("r.progress"), dose, Readout.BAR, false, p, null));
                out.add(value("grain", String.format(Locale.ROOT, "%.1f µm", 0.1F + conv * 40), false));
            }
            case "exotic" -> {
                String glyph = String.valueOf("?#%&@".charAt(rnd.nextInt(5)));
                out.add(value("z", "?" + glyph, true));
                out.add(new Readout(tr("r.spectrum"), "", Readout.SPEC, false, 0,
                    new float[][]{{40, rnd.nextFloat()}, {90, rnd.nextFloat()}, {140, rnd.nextFloat()}}));
                out.add(new Readout(tr("r.beam"), String.format(Locale.ROOT, "%.1fµA", beamHist[beamHist.length - 1] * 30), Readout.SPARK, false, 0, null));
                out.add(value("field", String.format(Locale.ROOT, "%.2f T", 7 + (float) Math.sin(now * 9) * 0.4F), true));
                out.add(value("process", tr("v.unknown"), true));
                out.add(value("trap_temp", String.format(Locale.ROOT, "%.0f K", 4.2F + heat * 60), heat > 0.5F));
                out.add(new Readout(tr("r.progress"), dose, Readout.BAR, false, p, null));
                out.add(value("conservation", tr("v.broken"), true));
            }
            default -> {                                               // molecular and unmarked
                float[][] peaks = new float[6][];
                for (int i = 0; i < 3; i++) {
                    float in = ph == 0 ? 1 - phaseFrac(p) * 0.3F : ph == 1 ? 0.7F : Math.max(0.05F, 0.7F * (1 - (ph == 2 ? phaseFrac(p) : 1)));
                    float prod = ph >= 2 ? (ph == 2 ? phaseFrac(p) : 1) : 0.05F;
                    peaks[i] = new float[]{mass(s.inHash(), i), in * (1 - i * 0.25F)};
                    peaks[3 + i] = new float[]{mass(s.outHash(), i), prod * (1 - i * 0.25F)};
                }
                int top = (int) (ph >= 2 ? mass(s.outHash(), 0) : mass(s.inHash(), 0));
                out.add(value("mz", String.valueOf(top), false));
                out.add(new Readout(tr("r.spectrum"), "", Readout.SPEC, false, 0, peaks));
                out.add(new Readout(tr("r.beam"), String.format(Locale.ROOT, "%.1fµA", beamHist[beamHist.length - 1] * 4), Readout.SPARK, false, 0, null));
                out.add(value("nucleus", tr("v.unchanged"), false));
                out.add(value("vacuum", String.format(Locale.ROOT, "%.1fe-7 Pa", 3.0F + heat * 2), false));
                out.add(value("purity", Math.round(40 + conv * 59) + "%", false));
                out.add(new Readout(tr("r.progress"), dose, Readout.BAR, false, p, null));
                out.add(value("fragments", String.valueOf(Math.round(12 + 30 * (1 - conv))), false));
            }
        }
        return out;
    }

    /** A stable fragment mass for an item (12 .. 211) for the mass spectrum of a molecular recipe. */
    private static float mass(int hash, int i) {
        int h = hash * 31 + i * 0x9E3779B1;
        h ^= h >>> 15;
        return 12 + Math.floorMod(h, 200);
    }

    private Readout value(String label, String v, boolean warn) {
        return new Readout(tr("r." + label), v, Readout.VALUE, warn, 0, null);
    }

    private void panel(GuiGraphics g, Readout d, float x, float y, float depth, float live, boolean idle) {
        float lh = 9 * u;
        boolean graph = d.kind == Readout.SPEC || d.kind == Readout.SPARK;
        float w = Math.max(18 * u / 0.667F, Math.max(AsciiHud.width(d.label, u), idle ? 0 : AsciiHud.width(d.value, u)) + 4);
        if (graph || d.kind == Readout.BAR) w = Math.max(w, AsciiHud.width(d.label, u) + AsciiHud.width(d.value, u) + 7);
        float h = lh * 2 + 2;
        float a = (depth < 0 ? 0.35F : 0.85F) * live;
        int edge = d.warn ? CRIT : ACC;
        float x0 = x - w / 2, y0 = y - h / 2;
        fillF(g, x0, y0, w, h, FcsDraw.argb(0x040A0C, 0.72F * (depth < 0 ? 0.7F : 1)));
        outline(g, x0, y0, w, h, FcsDraw.argb(edge, a * 0.7F));
        fillF(g, x0, y0, 1.2F, h, FcsDraw.argb(edge, a));
        if (idle) {
            text(g, "STBY", x - AsciiHud.width("STBY", u) / 2, y - lh / 2 + 0.5F, DIM, a * 0.8F, u);
            return;
        }
        float lx = x0 + 2.5F, top = y0 + 1, bw = w - 4.5F, by = y0 + h - 1.5F;
        text(g, d.label, lx, top, DIM, a, u);
        switch (d.kind) {
            case Readout.SPEC -> {
                fillF(g, lx, by, bw, 0.4F, FcsDraw.argb(DIM, a * 0.4F));
                float mMax = 0;
                for (float[] sp : d.spec) mMax = Math.max(mMax, sp[0]);
                mMax *= 1.15F;
                for (float[] sp : d.spec) {
                    float hh = Math.max(0, Math.min(1, sp[1])) * (lh - 1.5F);
                    fillF(g, lx + bw * (sp[0] / mMax) - 0.4F, by - hh, 0.8F, hh, FcsDraw.argb(TXT, a));
                }
            }
            case Readout.SPARK -> {
                float px = 0, py = 0;
                for (int i = 0; i < beamHist.length; i++) {
                    float qx = lx + bw * i / (beamHist.length - 1), qy = by - beamHist[i] * (lh - 1.5F);
                    if (i > 0) FcsDraw.line(g, px, py, qx, qy, 0.4F, FcsDraw.argb(TXT, a));
                    px = qx; py = qy;
                }
                text(g, d.value, x0 + w - 1.5F - AsciiHud.width(d.value, u), top, TXT, a, u);
            }
            case Readout.BAR -> {
                text(g, d.value, x0 + w - 1.5F - AsciiHud.width(d.value, u), top, TXT, a, u);
                fillF(g, lx, by - 2, bw, 1.6F, FcsDraw.argb(DIM, a * 0.3F));
                fillF(g, lx, by - 2, bw * d.bar, 1.6F, FcsDraw.argb(ACC, a));
            }
            default -> text(g, d.value, lx, top + lh, d.warn ? CRIT : TXT, a, u);
        }
    }

    // ------------------------------------------------------------------ text column

    private void textColumn(GuiGraphics g, int x, int y, int w, int h, String kind, MolecularProcess proc, float p, int ph,
                            float conv, Snapshot s, boolean idle, boolean low, boolean continuous, double secs) {
        float x0 = x + w * 0.63F, x1 = x + w - 4;
        float colW = x1 - x0;
        g.fill((int) (x0 - 3), y + 1, x + w - 1, y + h - 1, 0xC7030809);
        fillF(g, x0 - 3, y + 1, 0.5F, h - 2, FcsDraw.argb(ACC, 0.25F));
        float sc = u, lh = 9 * u + 1.5F, yy = y + 3;
        int tagCol = switch (kind) { case "nuclear" -> 0x7FD8FF; case "lattice" -> 0xB9F18A; case "molecular" -> 0xFFD166; case "exotic" -> 0xFF8FB1; default -> DIM; };
        Component state = Component.translatable(K + (idle ? "state.idle" : low ? "state.low" : "state.run"));
        if (!s.hasRecipe()) {                                        // nothing to run: only the stand-by lines
            text(g, state, x0, yy, DIM, 1, sc);
            yy += lh + 2;
            fit(g, Component.translatable(K + "idle.insert"), x0, yy, colW, DIM, sc);
            yy += lh;
            fit(g, Component.translatable(K + "idle.trap"), x0, yy, colW, MUTE, sc);
            return;
        }
        text(g, Component.translatable(K + "kind." + kind), x0, yy, tagCol, 1, sc);
        rightText(g, state, x1, yy, idle ? DIM : low ? WARN : ACC, sc);
        yy += lh;
        if (!compact) {
            fit(g, pathLine(kind, proc), x0, yy, colW, DIM, sc);
            yy += lh;
        }
        if (idle) {
            yy += 2;
            fit(g, Component.translatable(K + "idle.wait"), x0, yy, colW, DIM, sc);
            yy += lh;
            fit(g, Component.translatable(K + "idle.trap"), x0, yy, colW, MUTE, sc);
            return;
        }
        switch (kind) {
            case "nuclear" -> {
                Elements.E a = Elements.of(proc.from()), b = Elements.of(proc.to());
                int za = a == null ? 0 : a.z(), zb = b == null ? 0 : b.z();
                fit(g, Component.literal(proc.from() + " " + za + " → " + proc.to() + " " + zb), x0, yy, colW * 0.7F, TXT, sc);
                rightText(g, Component.literal("ΔZ " + (zb - za > 0 ? "+" : "") + (zb - za)), x1, yy, DIM, sc);
                int z = Math.round(za + (zb - za) * conv);
                if (!compact) { yy += lh; fit(g, Component.translatable(K + "line.z", String.valueOf(z)), x0, yy, colW, DIM, sc); }
            }
            case "lattice" -> {
                fit(g, names(s), x0, yy, colW, TXT, sc);
                if (!compact) { yy += lh; fit(g, Component.translatable(K + "line.lattice_note"), x0, yy, colW, DIM, sc); }
            }
            case "exotic" -> {
                fit(g, names(s), x0, yy, colW, CRIT, sc);
                if (!compact) { yy += lh; fit(g, Component.translatable(K + "line.exotic_note"), x0, yy, colW, CRIT, sc); }
            }
            default -> {
                fit(g, names(s), x0, yy, colW, TXT, sc);
                if (!compact) { yy += lh; fit(g, Component.translatable(K + "line.molecular_note"), x0, yy, colW, DIM, sc); }
            }
        }
        yy += lh + 1;
        // phase strip
        if (continuous) {
            text(g, Component.translatable(K + "phase.continuous"), x0, yy, ACC, 1, sc);
        } else {
            MutableComponent[] parts = new MutableComponent[4];
            float total = 0, ps = sc;
            for (int i = 0; i < 4; i++) {
                parts[i] = Component.literal(i > 0 ? "›" : "").append(Component.translatable(K + "phase." + phaseSet(kind) + "." + i));
                total += AsciiHud.width(parts[i], ps) + 1;
            }
            if (total > colW) {                                      // long words (ru): only the current phase, with its number
                text(g, Component.translatable(K + "phase." + phaseSet(kind) + "." + ph).append(" " + (ph + 1) + "/4"), x0, yy, ACC, 1, ps);
            } else {
                float px = x0;
                for (int i = 0; i < 4; i++) {
                    int col = i < ph ? DIM : i == ph ? ACC : MUTE;
                    text(g, parts[i], px, yy, col, 1, ps);
                    px += AsciiHud.width(parts[i], ps) + 1;
                }
            }
        }
        yy += lh;
        // dose bar or rate
        if (continuous) {
            double perSec = 1 / secs;
            String rate = perSec >= 10 ? String.format(Locale.ROOT, "%.0f", perSec) : String.format(Locale.ROOT, "%.1f", perSec);
            fit(g, Component.translatable(K + "line.rate", rate), x0, yy, colW, TXT, sc);
        } else {
            String pct = Math.round(p * 100) + "%";
            int cells = Math.max(4, (int) ((colW - AsciiHud.width(pct, sc) - 4) / Math.max(1, AsciiHud.width("█", sc))) - 2);
            text(g, AsciiHud.bar(p, cells), x0, yy, ACC, 0.95F, sc);
            rightText(g, Component.literal(pct), x1, yy, TXT, sc);
        }
        yy += lh;
        Component eu = Component.translatable(K + "line.eu", si(continuous ? s.recipeEU() : (long) (p * s.recipeEU())), si(s.recipeEU()));
        Component pw = Component.translatable(K + "line.power", si(s.euPerTick()));
        fit(g, eu, x0, yy, colW - AsciiHud.width(pw, sc) - 3, DIM, sc);
        rightText(g, pw, x1, yy, low ? WARN : DIM, sc);
        yy += lh;
        String pulse = lastPulse == null ? "—" : lastPulse.i + "/" + lastPulse.n;
        text(g, Component.translatable(K + ("lattice".equals(kind) ? "line.press" : "line.pulse"), pulse), x0, yy, DIM, 1, sc);
        String time = secs == Double.POSITIVE_INFINITY ? "—" : secs < 60 ? String.format(Locale.ROOT, "%.1f", secs) : String.format(Locale.ROOT, "%.1f", secs / 60);
        rightText(g, Component.translatable(K + (secs < 60 ? "line.per_item_s" : "line.per_item_min"), time), x1, yy, DIM, sc);
    }

    private static String phaseSet(String kind) {
        return switch (kind) { case "lattice" -> "lattice"; case "molecular", "unknown" -> "molecular"; case "exotic" -> "exotic"; default -> "nuclear"; };
    }

    private static Component pathLine(String kind, MolecularProcess proc) {
        if ("nuclear".equals(kind)) {
            Elements.E a = Elements.of(proc.from()), b = Elements.of(proc.to());
            int dz = a == null || b == null ? 0 : b.z() - a.z();
            if (dz < 0) return Component.translatable(K + "path.strip", String.valueOf(-dz));
            if (dz > 0) return Component.translatable(K + "path.fuse");
            return Component.translatable(K + "path.isotope");
        }
        return Component.translatable(K + "path." + kind);
    }

    private static Component names(Snapshot s) {
        return Component.empty().append(s.inName()).append(" → ").append(s.outName());
    }

    private static String si(long v) {
        if (v >= 1_000_000_000L) return String.format(Locale.ROOT, "%.2fG", v / 1.0E9);
        if (v >= 1_000_000L) return String.format(Locale.ROOT, "%.2fM", v / 1.0E6);
        if (v >= 10_000L) return String.format(Locale.ROOT, "%.0fk", v / 1.0E3);
        return String.valueOf(v);
    }

    private static String tr(String key) {
        return Component.translatable(K + key).getString();
    }

    // ------------------------------------------------------------------ drawing helpers

    private static int colourIn(String kind) {
        return switch (kind) { case "nuclear" -> 0xB9C3CF; case "lattice" -> 0x78787F; case "exotic" -> 0x5A5A62; default -> 0xD27864; };
    }

    private static int colourOut(String kind) {
        return switch (kind) { case "nuclear" -> 0xDCE6FF; case "lattice" -> 0xBEF0FF; case "exotic" -> 0xEBE6FF; default -> 0xE8E8E8; };
    }

    private static int mix(int a, int b, float f) {
        f = Math.max(0, Math.min(1, f));
        int r = Math.round(((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * f);
        int gg = Math.round(((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * f);
        int bb = Math.round((a & 255) + ((b & 255) - (a & 255)) * f);
        return (r << 16) | (gg << 8) | bb;
    }

    private static void text(GuiGraphics g, String s, float x, float y, int rgb, float alpha, float scale) {
        text(g, Component.literal(s), x, y, rgb, alpha, scale);
    }

    private static void text(GuiGraphics g, Component c, float x, float y, int rgb, float alpha, float scale) {
        if (alpha < 0.05F) return;
        FcsDraw.text(g, AsciiHud.t(c), x, y, FcsDraw.argb(rgb, alpha), scale);
    }

    private static void rightText(GuiGraphics g, Component c, float right, float y, int rgb, float scale) {
        text(g, c, right - AsciiHud.width(c, scale), y, rgb, 1, scale);
    }

    /** Left-aligned text, cut with an ellipsis to fit {@code maxW} (never scaled: fractional scales break the font). */
    private static void fit(GuiGraphics g, Component c, float x, float y, float maxW, int rgb, float scale) {
        if (AsciiHud.width(c, scale) <= maxW) { text(g, c, x, y, rgb, 1, scale); return; }
        String str = c.getString();
        int n = str.length();
        while (n > 1 && AsciiHud.width(str.substring(0, n) + "…", scale) > maxW) n--;
        text(g, str.substring(0, n) + "…", x, y, rgb, 1, scale);
    }

    private static void fillF(GuiGraphics g, float x, float y, float w, float h, int argb) {
        FcsDraw.quad(g, x, y, x, y + h, x + w, y + h, x + w, y, argb);
    }

    private static void outline(GuiGraphics g, float x, float y, float w, float h, int argb) {
        fillF(g, x, y, w, 0.4F, argb);
        fillF(g, x, y + h - 0.4F, w, 0.4F, argb);
        fillF(g, x, y, 0.4F, h, argb);
        fillF(g, x + w - 0.4F, y, 0.4F, h, argb);
    }

    /** Ellipse arc from a0 to a1 (radians, standard orientation) rotated by {@code rot}. */
    private static void ellipse(GuiGraphics g, float cx, float cy, float rx, float ry, float rot, float a0, float a1, float width, int argb) {
        int steps = Math.max(8, (int) (Math.abs(a1 - a0) / (Math.PI * 2) * 40));
        float cr = (float) Math.cos(rot), sr = (float) Math.sin(rot);
        float px = 0, py = 0;
        for (int i = 0; i <= steps; i++) {
            float a = a0 + (a1 - a0) * i / steps;
            float ex = rx * (float) Math.cos(a), ey = ry * (float) Math.sin(a);
            float qx = cx + ex * cr - ey * sr, qy = cy + ex * sr + ey * cr;
            if (i > 0) FcsDraw.line(g, px, py, qx, qy, width, argb);
            px = qx; py = qy;
        }
    }

    /** A line whose colour runs from {@code from} at (x1, y1) to {@code to} at (x2, y2). */
    private static void gradLine(GuiGraphics g, float x1, float y1, float x2, float y2, float width, int from, int to) {
        float dx = x2 - x1, dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1.0E-3F) return;
        float nx = -dy / len * width * 0.5F, ny = dx / len * width * 0.5F;
        VertexConsumer vc = g.bufferSource().getBuffer(RenderType.gui());
        Matrix4f m = g.pose().last().pose();
        float ax = x1 + nx, ay = y1 + ny, bx = x1 - nx, by = y1 - ny, cxx = x2 - nx, cyy = y2 - ny, ex = x2 + nx, ey = y2 + ny;
        // the GUI pass culls back faces: keep the front-facing winding
        if ((bx - ax) * (cyy - ay) - (by - ay) * (cxx - ax) > 0) {
            vc.addVertex(m, ax, ay, 0).setColor(from);
            vc.addVertex(m, ex, ey, 0).setColor(to);
            vc.addVertex(m, cxx, cyy, 0).setColor(to);
            vc.addVertex(m, bx, by, 0).setColor(from);
        } else {
            vc.addVertex(m, ax, ay, 0).setColor(from);
            vc.addVertex(m, bx, by, 0).setColor(from);
            vc.addVertex(m, cxx, cyy, 0).setColor(to);
            vc.addVertex(m, ex, ey, 0).setColor(to);
        }
    }

    // ------------------------------------------------------------------ elements

    /** Element symbols 1..94 with the mass number of the most common isotope. */
    static final class Elements {
        record E(int z, int a) { }

        private static final String[] SYM = ("H He Li Be B C N O F Ne Na Mg Al Si P S Cl Ar K Ca Sc Ti V Cr Mn Fe Co Ni Cu Zn Ga Ge As Se Br Kr "
            + "Rb Sr Y Zr Nb Mo Tc Ru Rh Pd Ag Cd In Sn Sb Te I Xe Cs Ba La Ce Pr Nd Pm Sm Eu Gd Tb Dy Ho Er Tm Yb Lu Hf Ta W Re Os Ir "
            + "Pt Au Hg Tl Pb Bi Po At Rn Fr Ra Ac Th Pa U Np Pu").split(" ");
        private static final int[] A = {1, 4, 7, 9, 11, 12, 14, 16, 19, 20, 23, 24, 27, 28, 31, 32, 35, 40, 39, 40, 45, 48, 51, 52, 55, 56,
            59, 58, 63, 64, 69, 74, 75, 80, 79, 84, 85, 88, 89, 90, 93, 98, 98, 102, 103, 106, 107, 114, 115, 120, 121, 130, 127, 132, 133,
            138, 139, 140, 141, 142, 145, 152, 153, 158, 159, 164, 165, 166, 169, 174, 175, 180, 181, 184, 187, 192, 193, 195, 197, 202, 205,
            208, 209, 209, 210, 222, 223, 226, 227, 232, 231, 238, 237, 244};

        static E of(String sym) {
            for (int i = 0; i < SYM.length; i++) if (SYM[i].equals(sym)) return new E(i + 1, A[i]);
            return null;
        }
    }
}
