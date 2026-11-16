// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.client.suit.AsciiHud;
import com.miophas.singularity_iteration.common.client.suit.FcsDraw;
import com.miophas.singularity_iteration.common.recipe.molecular_transformer.MolecularProcess;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * 0.1.11.6: the observation window of the Molecular Transformer screen, drawn as photographic film: the record a real
 * laboratory would keep of the process. The left two thirds are a strip of film (sprocket holes, fiducial crosses, the
 * frame number printed on the edge, grain); the right third is the scanner's log on lined paper.
 *
 * <ul>
 *   <li><b>Nuclear</b> ({@link MolecularProcess} {@code nuclear}): a bubble chamber photograph. Each accelerator pulse
 *       sends a beam track in from the left; at the vertex charged secondaries curl in the magnetic field (an electron
 *       losing energy winds into a tight spiral, now and then a neutral particle decays into a V). Tracks are rows of
 *       bubbles; they pile up like repeated exposures and fade over four seconds. A yellow grease-pencil ring marks some
 *       vertices with the atomic number reached, as a human scanner would.</li>
 *   <li><b>Lattice</b>: an X-ray diffraction strip. Debye-Scherrer rings of the old phase (graphite d-spacings) fade
 *       while the rings of the new phase (diamond) come in; the strip edges cut the large rings as on real film.</li>
 *   <li><b>Molecular</b> and unmarked: a mass spectrograph plate. One row per exposure, lines placed by the square root
 *       of the mass; the input's lines fade and the product's darken.</li>
 *   <li><b>Exotic</b>: bubble tracks that break the rules (kinks both ways, tracks that stop dead) and a light leak.</li>
 * </ul>
 * Idle: the shutter closes. A recipe under 2 s an item runs as a continuous process (the log shows items per second).
 * Text uses the HUD font at 2 / GUI scale, so one font texel lands on whole screen pixels.
 */
public final class MolecularObservationWindow {

    /** What the screen knows this frame. */
    public record Snapshot(MolecularProcess process, boolean hasRecipe, boolean working, float progress, long recipeEU, long euPerTick,
                           Component inName, Component outName, int inHash, int outHash) { }

    private static final float[] EDGES = {0F, 0.12F, 0.22F, 0.88F, 1F};
    /** The machine's nominal draw (DEFAULT_ENERGY_PER_TICK); below a quarter of it the log reads "low power". */
    private static final long NOMINAL_EU_T = 32768;
    private static final double CONTINUOUS_SECONDS = 2.0;
    private static final String K = "gui.mio_icif.mt.";

    private static final int FILM = 0xFF0E0D0B, EMULSION = 0xECE8DC, AMBER = 0xECBE6E, PENCIL = 0xE8C547, LEAK = 0xFFAA5A;
    private static final int PAPER = 0xFFE9E4D6, RULE = 0xFFB8C6D6, MARGIN = 0xFFD29B9B, INK = 0x24324A, RED = 0x9C2F2A, FADED = 0x6F7A88;
    /** Bubble tracks fade over this many seconds (an exposure that keeps adding up). */
    private static final double TRACK_LIFE = 4.0;
    private static final int MAX_BUBBLES = 6000;

    private final Random rnd = new Random();
    private final List<Track> tracks = new ArrayList<>();
    private final List<Mark> marks = new ArrayList<>();
    private final List<Pulse> pulses = new ArrayList<>();
    private double nextTrain, now;
    private long lastNanos;
    private float shown = -1, flash;
    private int frameNo = 412, bubbles;
    private float u = 0.667F;
    private boolean compact;

    private static final class Pulse { double t; float s; boolean fired; }
    /** One bubble track, relative to the film's top-left corner. */
    private record Track(float[] x, float[] y, double born, float bright) { }
    /** A grease-pencil ring with a note. */
    private record Mark(float x, float y, double born, String label) { }

    // ------------------------------------------------------------------ entry

    /** Draws the window into (x, y, w, h), GUI coordinates. */
    public void render(GuiGraphics g, int x, int y, int w, int h, Snapshot s) {
        long t = System.nanoTime();
        float dt = lastNanos == 0 ? 0.016F : Math.min(0.05F, (t - lastNanos) / 1.0E9F);
        lastNanos = t;
        now += dt;
        double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
        u = (float) Math.min(1.0, 2.0 / Math.max(1.0, guiScale));
        compact = u > 0.7F;
        MolecularProcess proc = s.process() == null ? MolecularProcess.UNKNOWN : s.process();
        String kind = proc.kind();
        boolean idle = !s.working() || s.recipeEU() <= 0;
        double secs = s.euPerTick() > 0 ? s.recipeEU() / (double) s.euPerTick() / 20.0 : Double.POSITIVE_INFINITY;
        boolean continuous = !idle && secs < CONTINUOUS_SECONDS;
        float level = idle ? 0 : (float) Math.min(1.0, s.euPerTick() / (double) NOMINAL_EU_T);
        boolean low = !idle && s.euPerTick() < NOMINAL_EU_T / 4;

        float target = Math.max(0, Math.min(1, s.progress()));
        if (shown < 0 || idle || target < shown - 0.3F) shown = target;   // a drop means an item finished
        else shown += (target - shown) * Math.min(1F, dt * 10F);
        float p = continuous ? 0.55F : shown;
        int ph = continuous ? 2 : phase(p);
        float conv = continuous ? 0.5F : ph < 2 ? 0 : ph == 2 ? phaseFrac(p) : 1;

        boolean bubbleKind = "nuclear".equals(kind) || "exotic".equals(kind);
        schedule(!idle && ("lattice".equals(kind) ? ph >= 1 && ph <= 3 : ph == 2), level, low);
        int filmW = Math.round(w * 0.66F);
        fire(bubbleKind, kind, proc, conv, filmW, h);
        flash = Math.max(0, flash - dt * 3);
        tracks.removeIf(tr -> now - tr.born > TRACK_LIFE);
        marks.removeIf(m -> now - m.born > 4);

        // ---- the film strip
        g.fill(x, y, x + w, y + h, FILM);
        g.enableScissor(x, y, x + filmW, y + h);
        if (!s.hasRecipe()) { tracks.clear(); marks.clear(); }       // nothing loaded: a blank frame
        else switch (kind) {
            case "nuclear", "exotic" -> drawTracks(g, x, y);
            case "lattice" -> diffraction(g, x, y, filmW, h, conv, idle);
            default -> plate(g, x, y, filmW, h, s, idle);
        }
        if ("exotic".equals(kind) && !idle) {                       // a light leak at the edge of the strip
            float leak = 0.06F + 0.05F * (float) Math.sin(now * 1.7);
            for (int i = 0; i < 6; i++) fillF(g, x + i * 3, y, 3, h, FcsDraw.argb(LEAK, leak * (1 - i / 6F)));
        }
        if (!idle) for (Mark m : marks) pencil(g, x + m.x, y + m.y, m);
        grain(g, x, y, filmW, h);
        for (float[] f : new float[][]{{0.2F, 0.25F}, {0.8F, 0.25F}, {0.2F, 0.78F}, {0.8F, 0.78F}}) {
            float px = x + filmW * f[0], py = y + h * f[1];
            fillF(g, px - 2.5F, py - 0.2F, 5, 0.4F, FcsDraw.argb(EMULSION, 0.5F));
            fillF(g, px - 0.2F, py - 2.5F, 0.4F, 5, FcsDraw.argb(EMULSION, 0.5F));
        }
        for (int sx = x + 3; sx < x + filmW - 4; sx += 8) {
            g.fill(sx, y + 2, sx + 4, y + 5, 0xFF2B2925);
            g.fill(sx, y + h - 5, sx + 4, y + h - 2, 0xFF2B2925);
        }
        text(g, Component.translatable(K + "film.edge", String.format(Locale.ROOT, "%04d", frameNo)), x + 4, y + 6, AMBER, 0.75F, u);
        if (idle) {
            g.fill(x, y, x + filmW, y + h, 0x8C0E0D0B);
            Component shut = Component.translatable(K + "idle.shutter");
            text(g, shut, x + filmW / 2F - AsciiHud.width(shut, u) / 2, y + h / 2F - 4 * u, EMULSION, 0.6F, u);
        }
        if (flash > 0.01F) g.fill(x, y, x + filmW, y + h, FcsDraw.argb(EMULSION, flash * 0.06F));
        g.flush();
        g.disableScissor();

        // ---- the scanner's log
        log(g, x + filmW, y, w - filmW, h, kind, proc, s, p, ph, conv, idle, low, continuous, secs);

        // frame
        g.fill(x - 1, y - 1, x + w + 1, y, 0xFF7A7F85);
        g.fill(x - 1, y + h, x + w + 1, y + h + 1, 0xFF7A7F85);
        g.fill(x - 1, y, x, y + h, 0xFF7A7F85);
        g.fill(x + w, y, x + w + 1, y + h, 0xFF7A7F85);
    }

    // ------------------------------------------------------------------ pulse clock

    /** Trains of 1 .. 5 accelerator pulses, then a pause for the chamber to recompress; less power, sparser trains. */
    private void schedule(boolean pulsing, float level, boolean low) {
        if (!pulsing || now < nextTrain) return;
        int n = low ? 1 + rnd.nextInt(2) : 2 + Math.round(level * 2) + rnd.nextInt(2);
        double t = now;
        for (int i = 0; i < n; i++) {
            t += (low ? 0.45 : 0.17) + rnd.nextDouble() * (low ? 0.3 : 0.15);
            Pulse p = new Pulse();
            p.t = t;
            p.s = (low ? 0.5F : 0.85F) + rnd.nextFloat() * 0.15F;
            pulses.add(p);
        }
        nextTrain = t + 0.8 + (1 - level) * 1.8 + rnd.nextDouble() * 0.8;
    }

    private void fire(boolean bubbleKind, String kind, MolecularProcess proc, float conv, int filmW, int h) {
        for (Pulse p : pulses) {
            if (p.fired || now < p.t) continue;
            p.fired = true;
            frameNo++;
            flash = Math.min(1, flash + p.s);
            if (bubbleKind) event(kind, proc, conv, p.s, filmW, h);
        }
        pulses.removeIf(p -> now - p.t > 1.5);
    }

    private static int phase(float p) { return p < EDGES[1] ? 0 : p < EDGES[2] ? 1 : p < EDGES[3] ? 2 : 3; }

    private static float phaseFrac(float p) {
        int i = phase(p);
        return (p - EDGES[i]) / (EDGES[i + 1] - EDGES[i]);
    }

    // ------------------------------------------------------------------ bubble chamber

    /** One interaction: the beam track, the secondaries, sometimes a spiral and a V0, sometimes a grease-pencil ring. */
    private void event(String kind, MolecularProcess proc, float conv, float s, int filmW, int h) {
        boolean wild = "exotic".equals(kind);
        float vx = filmW * (0.35F + rnd.nextFloat() * 0.35F), vy = h * (0.3F + rnd.nextFloat() * 0.4F);
        // the beam: almost straight (high momentum), from the left edge to the vertex
        int nb = (int) (vx / 0.9F);
        float[] bx = new float[nb], by = new float[nb];
        float bend = (rnd.nextFloat() - 0.5F) * 0.0006F;
        for (int i = 0; i < nb; i++) {
            float d = i * 0.9F - vx;
            bx[i] = i * 0.9F;
            by[i] = vy + bend * d * d;
        }
        addTrack(bx, by, 0.85F);
        int secondaries = 2 + rnd.nextInt(Math.max(1, Math.round(4 * s)));
        for (int i = 0; i < secondaries; i++) {
            float sign = rnd.nextBoolean() ? -1 : 1;
            particle(vx, vy, (rnd.nextFloat() - 0.5F) * 1.6F, sign * (0.01F + rnd.nextFloat() * 0.05F), 40 + rnd.nextInt(90), 1.004F, wild, 0.8F);
        }
        if (rnd.nextFloat() < 0.45F) {                                // an electron losing energy: a tightening spiral
            particle(vx, vy, rnd.nextFloat() * 6.283F, (rnd.nextBoolean() ? -1 : 1) * 0.12F, 160, 1.012F, wild, 0.7F);
        }
        if (rnd.nextFloat() < 0.35F) {                                // a neutral decays a little downstream: a V
            float dx = vx + 10 + rnd.nextFloat() * 18, dy = vy + (rnd.nextFloat() - 0.5F) * 16;
            particle(dx, dy, -0.4F, 0.02F, 50, 1.003F, wild, 0.75F);
            particle(dx, dy, 0.45F, -0.03F, 45, 1.003F, wild, 0.75F);
        }
        if (rnd.nextFloat() < 0.4F) {
            String label;
            if (wild) label = "??";
            else {
                Elements.E a = Elements.of(proc.from()), b = Elements.of(proc.to());
                label = a == null || b == null ? "?" : "Z " + Math.round(a.z() + (b.z() - a.z()) * conv);
            }
            marks.add(new Mark(vx, vy, now, label));
        }
    }

    /** A charged particle in the field: the curvature k grows by the factor dk per step as it slows (a spiral). */
    private void particle(float x0, float y0, float ang, float k, int len, float dk, boolean wild, float bright) {
        float[] xs = new float[len], ys = new float[len];
        float x = x0, y = y0, a = ang;
        int n = 0;
        for (int i = 0; i < len; i++) {
            x += (float) Math.cos(a) * 0.9F;
            y += (float) Math.sin(a) * 0.9F;
            a += k + (wild ? (rnd.nextFloat() - 0.5F) * 0.4F : 0);
            k *= dk;
            xs[n] = x;
            ys[n] = y;
            n++;
            if (wild && rnd.nextFloat() < 0.01F) break;               // tracks that stop dead
        }
        addTrack(Arrays.copyOf(xs, n), Arrays.copyOf(ys, n), bright);
    }

    /** Keeps a track as bubbles: about one point in six is missing, as in a real chamber. */
    private void addTrack(float[] xs, float[] ys, float bright) {
        int keep = 0;
        float[] kx = new float[xs.length], ky = new float[ys.length];
        for (int i = 0; i < xs.length; i++) {
            if (rnd.nextFloat() < 0.18F) continue;
            kx[keep] = xs[i] + (rnd.nextFloat() - 0.5F) * 0.3F;
            ky[keep] = ys[i] + (rnd.nextFloat() - 0.5F) * 0.3F;
            keep++;
        }
        tracks.add(new Track(Arrays.copyOf(kx, keep), Arrays.copyOf(ky, keep), now, bright));
        bubbles += keep;
        while (bubbles > MAX_BUBBLES && !tracks.isEmpty()) bubbles -= tracks.remove(0).x.length;
    }

    private void drawTracks(GuiGraphics g, int x, int y) {
        int total = 0;
        for (Track tr : tracks) {
            float age = (float) ((now - tr.born) / TRACK_LIFE);
            float a = tr.bright * (1 - age) * (1 - age);
            total += tr.x.length;
            if (a < 0.02F) continue;
            int col = FcsDraw.argb(EMULSION, a);
            for (int i = 0; i < tr.x.length; i++) {
                float sz = 0.45F + ((i * 7919) % 5) * 0.08F;
                fillF(g, x + tr.x[i] - sz / 2, y + tr.y[i] - sz / 2, sz, sz, col);
            }
        }
        bubbles = total;
    }

    /** A wobbly grease-pencil ring and its note. */
    private void pencil(GuiGraphics g, float cx, float cy, Mark m) {
        float age = (float) (now - m.born);
        float a = age < 3 ? 0.9F : 0.9F * (4 - age);
        int col = FcsDraw.argb(PENCIL, a);
        float px = 0, py = 0;
        for (int i = 0; i <= 24; i++) {
            float t = i / 24F * 6.6F, rr = 5 + (float) Math.sin(t * 3 + m.x) * 0.6F;
            float qx = cx + (float) Math.cos(t) * rr, qy = cy + (float) Math.sin(t) * rr * 0.85F;
            if (i > 0) FcsDraw.line(g, px, py, qx, qy, 0.6F, col);
            px = qx;
            py = qy;
        }
        text(g, Component.literal(m.label), cx + 6, cy - 6 - 6 * u, PENCIL, a, u);
    }

    private void grain(GuiGraphics g, int x, int y, int w, int h) {
        for (int i = 0; i < 160; i++) {
            float gx = x + rnd.nextFloat() * w, gy = y + rnd.nextFloat() * h;
            fillF(g, gx, gy, 0.5F, 0.5F, FcsDraw.argb(EMULSION, 0.04F + rnd.nextFloat() * 0.08F));
        }
    }

    // ------------------------------------------------------------------ diffraction strip

    /** Debye-Scherrer rings: radius proportional to 1 / d (Bragg); the old phase fades, the new phase comes in. */
    private void diffraction(GuiGraphics g, int x, int y, int w, int h, float conv, boolean idle) {
        float cx = x + w / 2F, cy = y + h / 2F;
        float[] oldD = {3.35F, 2.13F, 2.03F, 1.68F, 1.23F}, newD = {2.06F, 1.26F, 1.08F, 0.89F};   // graphite, diamond (Å)
        float k = 16.1F, exp = (idle ? 0.4F : 1F) * (0.85F + flash * 0.25F);
        for (float d : oldD) ring(g, cx, cy, k * 2.06F / d, (1 - conv) * 0.55F * exp);
        for (float d : newD) ring(g, cx, cy, k * 2.06F / d, conv * 0.7F * exp);
        FcsDraw.disc(g, cx, cy, 2.2F, FcsDraw.argb(EMULSION, 0.8F));      // the hole of the beam stop
        FcsDraw.disc(g, cx, cy, 1.2F, FILM);
        text(g, Component.translatable(K + "film.old_phase"), cx + k * 2.06F / 3.35F + 1, cy - 4 - 8 * u, AMBER, 0.7F * (1 - conv), u);
        text(g, Component.translatable(K + "film.new_phase"), cx + k + 1, cy + 3, AMBER, 0.8F * conv, u);
    }

    private static void ring(GuiGraphics g, float cx, float cy, float r, float a) {
        if (a < 0.02F) return;
        FcsDraw.circle(g, cx, cy, r, 0.9F, FcsDraw.argb(EMULSION, a));
    }

    // ------------------------------------------------------------------ mass spectrograph plate

    /** One row per exposure (older above); lines at sqrt(m/z); input lines fade, product lines darken. */
    private void plate(GuiGraphics g, int x, int y, int w, int h, Snapshot s, boolean idle) {
        int rows = 5;
        float top = y + 8 + 9 * u, rh = (h - 22F - 13 * u) / rows;
        float[] in = masses(s.inHash()), out = masses(s.outHash());
        for (int i = 0; i < rows; i++) {
            float q = Math.max(0, shown - (rows - 1 - i) * 0.12F);
            int pp = phase(q);
            float conv = pp < 2 ? 0 : pp == 2 ? phaseFrac(q) : 1;
            float yy = top + i * rh;
            text(g, Component.literal(String.valueOf(i + 1)), x + 1.5F, yy + rh / 2 - 4 * u, AMBER, 0.6F, u);
            for (float m : in) fillF(g, plateX(x, w, m) - 0.4F, yy + 1, 0.8F, rh - 2, FcsDraw.argb(EMULSION, (idle ? 0.15F : 0.75F) * (1 - conv)));
            for (float m : out) fillF(g, plateX(x, w, m) - 0.45F, yy + 1, 0.9F, rh - 2, FcsDraw.argb(EMULSION, (idle ? 0.15F : 0.85F) * conv + flash * 0.1F));
        }
        for (float m : out) {
            String lbl = String.valueOf((int) m);
            text(g, Component.literal(lbl), plateX(x, w, m) - AsciiHud.width(lbl, u) / 2, top + rows * rh + 1, AMBER, 0.75F, u);
        }
    }

    private static float plateX(int x, int w, float m) {
        return x + 8 + (float) ((Math.sqrt(m) - 2.5) / (14.6 - 2.5)) * (w - 16);
    }

    /** Three stable line masses (12 .. 211) for an item: the plate shows the same lines for the same item. */
    private static float[] masses(int hash) {
        float[] m = new float[3];
        for (int i = 0; i < 3; i++) {
            int h = hash * 31 + i * 0x9E3779B1;
            h ^= h >>> 15;
            m[i] = 12 + Math.floorMod(h, 200);
        }
        return m;
    }

    // ------------------------------------------------------------------ the scanner's log

    private void log(GuiGraphics g, int x, int y, int w, int h, String kind, MolecularProcess proc, Snapshot s, float p, int ph,
                     float conv, boolean idle, boolean low, boolean continuous, double secs) {
        g.fill(x + 1, y, x + w, y + h, PAPER);
        float lh = 9 * u + 1;
        for (float ry = y + 2 + lh; ry < y + h; ry += lh) fillF(g, x + 1, ry - 0.5F, w - 1, 0.3F, RULE);
        fillF(g, x + 4, y, 0.3F, h, MARGIN);
        float x0 = x + 6, x1 = x + w - 2, colW = x1 - x0, yy = y + 2;
        Component film = Component.translatable(K + "film." + filmKind(kind));
        Component state = Component.translatable(K + (idle ? "state.idle" : low ? "state.low" : "state.run"));
        if (s.hasRecipe()) {
            fit(g, film, x0, yy, colW - AsciiHud.width(state, u) - 2, INK, u);
            text(g, state, x1 - AsciiHud.width(state, u), yy, idle ? FADED : low ? RED : INK, 1, u);
        } else {
            text(g, state, x0, yy, FADED, 1, u);
        }
        yy += lh;
        if (!s.hasRecipe()) {
            fit(g, Component.translatable(K + "idle.insert"), x0, yy, colW, FADED, u);
            return;
        }
        if (idle) {
            fit(g, Component.translatable(K + "idle.wait"), x0, yy, colW, FADED, u);
            return;
        }
        List<Component> lines = new ArrayList<>();
        switch (kind) {
            case "nuclear" -> {
                Elements.E a = Elements.of(proc.from()), b = Elements.of(proc.to());
                int za = a == null ? 0 : a.z(), zb = b == null ? 0 : b.z();
                lines.add(Component.literal(proc.from() + " → " + proc.to()));
                if (!compact) lines.add(pathLine(za, zb));
                lines.add(Component.translatable(K + "log.frame", String.valueOf(frameNo)));
                lines.add(Component.translatable(K + "log.z", String.valueOf(Math.round(za + (zb - za) * conv)), String.valueOf(zb)));
            }
            case "lattice" -> {
                lines.add(names(s));
                if (!compact) lines.add(Component.translatable(K + "line.lattice_note"));
                lines.add(Component.translatable(K + "log.new_rings", Math.round(conv * 100) + "%"));
            }
            case "exotic" -> {
                lines.add(names(s));
                if (!compact) lines.add(Component.translatable(K + "line.exotic_note"));
                lines.add(Component.translatable(K + "log.frame", String.valueOf(frameNo)));
                lines.add(Component.translatable(K + "log.unconserved"));
            }
            default -> {
                lines.add(names(s));
                if (!compact) lines.add(Component.translatable("unknown".equals(kind) ? K + "path.unknown" : K + "line.molecular_note"));
                lines.add(Component.translatable(K + "log.peak", String.valueOf((int) masses(s.outHash())[0]), Math.round(conv * 100) + "%"));
            }
        }
        for (Component c : lines) {
            fit(g, c, x0, yy, colW, "exotic".equals(kind) ? RED : INK, u);
            yy += lh;
        }
        Component phaseLine = continuous ? Component.translatable(K + "phase.continuous")
            : Component.translatable(K + "log.phase", Component.translatable(K + "phase." + phaseSet(kind) + "." + ph), String.valueOf(ph + 1));
        fit(g, phaseLine, x0, yy, colW, RED, u);
        yy += lh;
        if (continuous) {
            double perSec = 1 / secs;
            String rate = perSec >= 10 ? String.format(Locale.ROOT, "%.0f", perSec) : String.format(Locale.ROOT, "%.1f", perSec);
            fit(g, Component.translatable(K + "line.rate", rate), x0, yy, colW, INK, u);
        } else {
            fit(g, Component.literal(Math.round(p * 100) + "% · ").append(Component.translatable(K + "line.power", si(s.euPerTick()))),
                x0, yy, colW, low ? RED : INK, u);
        }
        yy += lh;
        if (yy + lh <= y + h + 1) {
            String time = secs < 60 ? String.format(Locale.ROOT, "%.1f", secs) : String.format(Locale.ROOT, "%.1f", secs / 60);
            fit(g, Component.translatable(K + (secs < 60 ? "line.per_item_s" : "line.per_item_min"), time), x0, yy, colW, FADED, u);
        }
    }

    private static String filmKind(String kind) {
        return switch (kind) { case "nuclear", "exotic" -> "chamber"; case "lattice" -> "diffraction"; default -> "plate"; };
    }

    private static String phaseSet(String kind) {
        return switch (kind) { case "lattice" -> "lattice"; case "molecular", "unknown" -> "molecular"; case "exotic" -> "exotic"; default -> "nuclear"; };
    }

    private static Component pathLine(int za, int zb) {
        int dz = zb - za;
        if (dz < 0) return Component.translatable(K + "path.strip", String.valueOf(-dz));
        if (dz > 0) return Component.translatable(K + "path.fuse");
        return Component.translatable(K + "path.isotope");
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

    // ------------------------------------------------------------------ drawing helpers

    /** Text at whole screen pixels: a fractional position cuts rows and columns out of the pixel font, like a fractional scale. */
    private static void text(GuiGraphics g, Component c, float x, float y, int rgb, float alpha, float scale) {
        if (alpha < 0.05F) return;
        float gs = (float) Minecraft.getInstance().getWindow().getGuiScale();
        FcsDraw.text(g, AsciiHud.t(c), Math.round(x * gs) / gs, Math.round(y * gs) / gs, FcsDraw.argb(rgb, Math.min(1, alpha)), scale);
    }

    /** Left-aligned text, cut with an ellipsis to fit {@code maxW} (never scaled: fractional scales break the font). */
    private static void fit(GuiGraphics g, Component c, float x, float y, float maxW, int rgb, float scale) {
        if (AsciiHud.width(c, scale) <= maxW) { text(g, c, x, y, rgb, 1, scale); return; }
        String str = c.getString();
        int n = str.length();
        while (n > 1 && AsciiHud.width(str.substring(0, n) + "…", scale) > maxW) n--;
        text(g, Component.literal(str.substring(0, n) + "…"), x, y, rgb, 1, scale);
    }

    private static void fillF(GuiGraphics g, float x, float y, float w, float h, int argb) {
        FcsDraw.quad(g, x, y, x, y + h, x + w, y + h, x + w, y, argb);
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
