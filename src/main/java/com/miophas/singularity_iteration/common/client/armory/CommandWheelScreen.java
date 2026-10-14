// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.armory;

import com.miophas.singularity_iteration.common.armory.WingmanCommandPackets;
import com.miophas.singularity_iteration.common.armory.WingmanCommandPackets.Unit;
import com.miophas.singularity_iteration.common.armory.WingmanCommands;
import com.miophas.singularity_iteration.common.armory.WingmanMode;
import com.miophas.singularity_iteration.common.armory.WingmanOrder;
import com.miophas.singularity_iteration.common.client.suit.FcsDraw;
import com.miophas.singularity_iteration.common.registry.mio_icif_sounds;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Quantum command wheel (0.1.10.2). Opens while the wheel key is held; the sector under the mouse
 * runs when the key is released (or on a click). Ten sectors: move, hold, area suppression, focus
 * fire, entangled volley, form up, squad control (a fan with the four modes, "follow squad" and
 * "return home"), data link, holo table and quantum recall. The chips above the wheel select the
 * whole squad or one Wingman (the mouse wheel steps through them). The centre cancels.
 *
 * <p>Look: a hologram of two counter-turning interference rings, sectors that unfold one after the
 * other, a hovered sector that lifts out with a glowing rim, line icons drawn per order, and the
 * target and the selection in the centre read-out.
 */
public final class CommandWheelScreen extends Screen {
    private static final float R_IN = 30, R_OUT = 86, R_SUB_IN = 93, R_SUB_OUT = 132, GAP = 0.035F;
    private static final int SECTORS = 10;
    private static final float STEP = (float) (Math.PI * 2 / SECTORS);
    private static final int CYAN = 0x5CEBFF, VIOLET = 0xB98CFF;

    enum Entry {
        MOVE(0x5CEBFF), HOLD(0xFFC24D), SUPPRESS(0xFF6A4D), FOCUS(0xFF4D6D), VOLLEY(0x6FF3D6),
        FORM_UP(0xB8D8FF), CONTROL(0xB98CFF), DATALINK(0x6FF3D6), HOLO(0x4DE1C1), RECALL(0x9B7CFF);
        final int color;
        Entry(int color) { this.color = color; }
        String key() { return "gui.mio_icif.command.entry." + name().toLowerCase(Locale.ROOT); }
    }

    /** Squad control fan: the four modes, follow squad, return home. */
    private static final int[] SUB_COLORS = {0xFF5238, 0xFFB333, 0x4DE6FF, 0x66FF80, 0xDDDDDD, 0x9B7CFF};
    private static final String[] SUB_KEYS = {"gui.mio_icif.wingman.mode.offensive", "gui.mio_icif.wingman.mode.dispersal",
        "gui.mio_icif.wingman.mode.escort", "gui.mio_icif.wingman.mode.fulton", "gui.mio_icif.command.sub.squad", "gui.mio_icif.command.sub.return"};
    private static final float SUB_STEP = (float) Math.toRadians(23);

    private final CommandWheelClient.Target target;
    private final int openedAt = SquadClient.ticks;
    private int hover = -1, sub = -1, lastHover = -2;
    private boolean subOpen, done;
    /** Unit chip rectangles of the last frame: x, y, w, h per chip; ids parallel. */
    private final List<float[]> chips = new ArrayList<>();
    private final List<UUID> chipIds = new ArrayList<>();

    public CommandWheelScreen(CommandWheelClient.Target target) {
        super(Component.translatable("gui.mio_icif.command.title"));
        this.target = target;
    }

    @Override
    protected void init() {
        play(mio_icif_sounds.WHEEL_OPEN.get(), 1.0F, 0.55F);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partial) {
        g.fillGradient(0, 0, width, height, 0x38000000, 0x60000814);
    }

    // ------------------------------------------------------------------ state

    private float cx() { return width / 2F; }
    private float cy() { return height / 2F + 10; }

    private static float centre(int i) { return i * STEP; }

    private boolean enabled(Entry e) {
        List<Unit> sel = selection();
        boolean anyOrderable = !sel.isEmpty();
        return switch (e) {
            case MOVE, HOLD, SUPPRESS, CONTROL -> anyOrderable;
            case FOCUS -> anyOrderable && target.entityId() >= 0;
            case FORM_UP -> sel.stream().anyMatch(u -> SquadClient.order(u) != WingmanOrder.NONE);
            case VOLLEY, DATALINK -> true;
            case HOLO -> HoloTable.available();
            case RECALL -> SquadClient.recallCooldown() <= 0 && !SquadClient.units().isEmpty();
        };
    }

    /** The orderable Wingmen of the selection. */
    private static List<Unit> selection() {
        Unit one = SquadClient.selectedUnit();
        List<Unit> out = new ArrayList<>();
        for (Unit u : SquadClient.orderable()) if (one == null || u.id().equals(one.id())) out.add(u);
        return out;
    }

    private void updateHover(double mouseX, double mouseY) {
        float dx = (float) (mouseX - cx()), dy = (float) (mouseY - cy());
        float r = Mth.sqrt(dx * dx + dy * dy);
        float a = (float) Math.atan2(dx, -dy);
        if (a < 0) a += (float) (Math.PI * 2);
        hover = -1;
        sub = -1;
        if (r < R_IN * 0.7F) { subOpen = false; return; }
        float fanCentre = centre(Entry.CONTROL.ordinal());
        float fanHalf = SUB_STEP * SUB_KEYS.length / 2;
        float off = Mth.wrapDegrees((float) Math.toDegrees(a - fanCentre));
        if (subOpen && r >= R_SUB_IN * 0.92F && Math.abs(off) <= Math.toDegrees(fanHalf)) {
            sub = Mth.clamp((int) ((Math.toRadians(off) + fanHalf) / SUB_STEP), 0, SUB_KEYS.length - 1);
            hover = Entry.CONTROL.ordinal();
            return;
        }
        hover = Math.floorMod(Math.round(a / STEP), SECTORS);
        subOpen = hover == Entry.CONTROL.ordinal() && enabled(Entry.CONTROL);
    }

    // ------------------------------------------------------------------ render

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g, mouseX, mouseY, partial);
        updateHover(mouseX, mouseY);
        int key = hover * 10 + sub;
        if (key != lastHover && hover >= 0) play(mio_icif_sounds.WHEEL_SELECT.get(), 1.4F + 0.05F * hover, 0.25F);
        lastHover = key;
        float t = SquadClient.ticks + partial;
        float open = Mth.clamp((t - openedAt) / 5F, 0, 1);
        float ease = 1 - (1 - open) * (1 - open) * (1 - open);
        float cx = cx(), cy = cy();
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        float scale = 0.72F + 0.28F * ease;
        g.pose().scale(scale, scale, 1);
        g.pose().translate(-cx, -cy, 0);
        rings(g, cx, cy, t, ease);
        for (Entry e : Entry.values()) sector(g, e, cx, cy, t, ease);
        if (subOpen) fan(g, cx, cy, t);
        centrePanel(g, cx, cy, t, ease);
        g.pose().popPose();
        unitChips(g, cx, t, ease, mouseX, mouseY);
        footer(g, cx, ease);
    }

    private void rings(GuiGraphics g, float cx, float cy, float t, float a) {
        FcsDraw.disc(g, cx, cy, R_OUT + 8, FcsDraw.argb(0x020A12, 0.55F * a));
        // two interference rings turning against each other, cyan and violet
        FcsDraw.segRing(g, cx, cy, R_OUT + 5, 36, 0.55F, t * 0.012F, 1.2F, FcsDraw.argb(CYAN, 0.55F * a));
        FcsDraw.segRing(g, cx, cy, R_OUT + 6.5F, 36, 0.55F, -t * 0.015F, 1.0F, FcsDraw.argb(VIOLET, 0.4F * a));
        FcsDraw.segRing(g, cx, cy, R_IN - 4, 12, 0.6F, -t * 0.03F, 1.0F, FcsDraw.argb(CYAN, 0.5F * a));
        // slow fringe bands across the disc
        for (int k = 0; k < 4; k++) {
            float r = R_IN + (R_OUT - R_IN) * ((t * 0.01F + k * 0.25F) % 1F);
            FcsDraw.circle(g, cx, cy, r, 0.6F, FcsDraw.argb(CYAN, 0.06F * a));
        }
    }

    private void sector(GuiGraphics g, Entry e, float cx, float cy, float t, float ease) {
        int i = e.ordinal();
        // sectors unfold one after the other while the wheel opens
        float k = Mth.clamp(ease * 1.6F - i * 0.06F, 0, 1);
        if (k <= 0) return;
        boolean on = enabled(e), hot = hover == i && sub < 0;
        float a0 = centre(i) - STEP / 2 + GAP, a1 = centre(i) + STEP / 2 - GAP;
        float lift = hot && on ? 5 : 0;
        int col = on ? e.color : 0x5A6670;
        float fill = (hot ? 0.42F : on ? 0.16F : 0.07F) * k;
        if (e == Entry.DATALINK && !SquadClient.datalink()) col = 0x7A8A94;
        FcsDraw.sector(g, cx, cy, R_IN + lift, R_OUT + lift, a0, a1, FcsDraw.argb(col, fill));
        FcsDraw.arc(g, cx, cy, R_OUT + lift, a0, a1, hot ? 2.2F : 1.0F, FcsDraw.argb(col, (hot ? 0.95F : 0.5F) * k));
        FcsDraw.arc(g, cx, cy, R_IN + lift, a0, a1, 0.8F, FcsDraw.argb(col, 0.35F * k));
        if (hot && on) {
            FcsDraw.arc(g, cx, cy, R_OUT + lift + 3, a0, a1, 3F, FcsDraw.argb(col, 0.18F));
            float mx = cx + (float) Math.sin(centre(i)) * (R_IN - 6), my = cy - (float) Math.cos(centre(i)) * (R_IN - 6);
            FcsDraw.line(g, cx, cy, mx, my, 1F, FcsDraw.argb(col, 0.6F));
        }
        // the recall cooldown sweeps the sector
        if (e == Entry.RECALL && SquadClient.recallCooldown() > 0) {
            float f = Mth.clamp(SquadClient.recallCooldown() / 600F, 0, 1);
            FcsDraw.sector(g, cx, cy, R_IN, R_IN + (R_OUT - R_IN) * f, a0, a1, FcsDraw.argb(0x000000, 0.35F));
        }
        float rm = (R_IN + R_OUT) / 2 + lift;
        float ix = cx + (float) Math.sin(centre(i)) * rm, iy = cy - (float) Math.cos(centre(i)) * rm - 4;
        icon(g, e, ix, iy, 8.5F, FcsDraw.argb(on ? (hot ? 0xFFFFFF : col) : 0x6A7680, k), t);
        String label = Component.translatable(e.key()).getString();
        FcsDraw.textCentered(g, label, ix, iy + 11, FcsDraw.argb(on ? col : 0x6A7680, 0.9F * k), 0.5F);
    }

    private void fan(GuiGraphics g, float cx, float cy, float t) {
        float fanCentre = centre(Entry.CONTROL.ordinal());
        float start = fanCentre - SUB_STEP * SUB_KEYS.length / 2;
        Unit one = SquadClient.selectedUnit();
        int current = one != null ? one.mode() : SquadClient.squadMode();
        for (int s = 0; s < SUB_KEYS.length; s++) {
            float a0 = start + s * SUB_STEP + 0.02F, a1 = start + (s + 1) * SUB_STEP - 0.02F;
            boolean hot = sub == s;
            int col = SUB_COLORS[s];
            FcsDraw.sector(g, cx, cy, R_SUB_IN, R_SUB_OUT + (hot ? 4 : 0), a0, a1, FcsDraw.argb(col, hot ? 0.45F : 0.15F));
            FcsDraw.arc(g, cx, cy, R_SUB_OUT + (hot ? 4 : 0), a0, a1, hot ? 2F : 1F, FcsDraw.argb(col, hot ? 1F : 0.6F));
            if (s < 4 && s == current) FcsDraw.arc(g, cx, cy, R_SUB_IN + 2, a0, a1, 2F, FcsDraw.argb(0xFFFFFF, 0.8F));
            float am = (a0 + a1) / 2, rm = (R_SUB_IN + R_SUB_OUT) / 2;
            float x = cx + (float) Math.sin(am) * rm, y = cy - (float) Math.cos(am) * rm;
            String label = Component.translatable(SUB_KEYS[s]).getString();
            FcsDraw.textCentered(g, label, x, y - 2, FcsDraw.argb(hot ? 0xFFFFFF : col, 1F), 0.45F);
        }
        // a bracket joins the fan to its sector
        FcsDraw.arc(g, cx, cy, R_SUB_IN - 3, start, start + SUB_STEP * SUB_KEYS.length, 0.8F, FcsDraw.argb(VIOLET, 0.5F));
    }

    private void centrePanel(GuiGraphics g, float cx, float cy, float t, float a) {
        FcsDraw.disc(g, cx, cy, R_IN - 7, FcsDraw.argb(0x01060C, 0.85F * a));
        FcsDraw.circle(g, cx, cy, R_IN - 7, 0.8F, FcsDraw.argb(CYAN, 0.45F * a));
        Unit one = SquadClient.selectedUnit();
        String who = one == null ? Component.translatable("gui.mio_icif.command.all", selection().size()).getString() : SquadClient.label(one);
        FcsDraw.textCentered(g, who, cx, cy - 10, FcsDraw.argb(0xDFFBFF, a), 0.55F);
        String what;
        if (hover < 0) what = Component.translatable("gui.mio_icif.command.cancel").getString();
        else if (sub >= 0) what = Component.translatable(SUB_KEYS[sub]).getString();
        else what = Component.translatable(Entry.values()[hover].key()).getString();
        FcsDraw.textCentered(g, what, cx, cy - 2, FcsDraw.argb(hover >= 0 ? Entry.values()[hover].color : 0x9AA8B0, a), 0.5F);
        String tgt = target.entityId() >= 0 && target.entityName() != null
            ? String.format(Locale.ROOT, "%s %.0fm", target.entityName().getString(), target.distance())
            : target.sky() ? Component.translatable("gui.mio_icif.command.sky").getString()
            : String.format(Locale.ROOT, "%.0fm", target.distance());
        FcsDraw.textCentered(g, tgt, cx, cy + 6, FcsDraw.argb(target.entityId() >= 0 ? 0xFF8A7A : 0x9FDFFF, 0.9F * a), 0.42F);
    }

    /** Selection chips above the wheel: ALL, then each Wingman (mode colour, health, order, unloaded). */
    private void unitChips(GuiGraphics g, float cx, float t, float a, int mouseX, int mouseY) {
        chips.clear();
        chipIds.clear();
        List<Unit> units = SquadClient.units();
        float y = cy() - R_SUB_OUT - 30;
        float w = 38, h = 18, gap = 4;
        int n = units.size() + 1;
        float x = cx - (n * w + (n - 1) * gap) / 2;
        UUID sel = SquadClient.selected();
        for (int i = 0; i < n; i++) {
            Unit u = i == 0 ? null : units.get(i - 1);
            UUID id = u == null ? WingmanCommandPackets.ALL : u.id();
            boolean selected = id.equals(sel);
            boolean usable = u == null || u.where() == 0;
            boolean hot = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
            int col = u == null ? CYAN : u.mode() >= 0 ? modeColor(u.mode()) : 0x8A96A0;
            FcsDraw.quad(g, x, y, x + w, y, x + w - 3, y + h, x - 3, y + h, FcsDraw.argb(col, (selected ? 0.38F : hot ? 0.22F : 0.1F) * a));
            FcsDraw.line(g, x, y, x + w, y, selected ? 1.6F : 0.8F, FcsDraw.argb(col, (selected ? 1F : 0.6F) * a));
            String name = u == null ? Component.translatable("gui.mio_icif.command.chip_all").getString() : SquadClient.label(u);
            FcsDraw.text(g, name, x + 3, y + 3, FcsDraw.argb(usable ? 0xEFFFFF : 0x7A8890, a), 0.55F);
            if (u != null) {
                if (u.where() == 2) FcsDraw.text(g, "UNLOADED", x + 3, y + 11, FcsDraw.argb(0xFF9F4D, a), 0.35F);
                else if (u.where() == 1) FcsDraw.text(g, "OTHER DIM", x + 3, y + 11, FcsDraw.argb(0xFF9F4D, a), 0.35F);
                else {
                    g.fill((int) x + 3, (int) y + 12, (int) (x + 3 + 30 * Mth.clamp(u.health(), 0, 1)), (int) y + 14, FcsDraw.argb(0x5CFF9A, 0.9F * a));
                    WingmanOrder o = SquadClient.order(u);
                    if (o != WingmanOrder.NONE) FcsDraw.textRight(g, o.name().substring(0, Math.min(4, o.name().length())), x + w - 3, y + 3,
                        FcsDraw.argb(0xFFE08A, a), 0.38F);
                }
            }
            if (usable) {
                chips.add(new float[]{x, y, w, h});
                chipIds.add(id);
            }
            x += w + gap;
        }
        FcsDraw.textCentered(g, Component.translatable("gui.mio_icif.command.select_hint").getString(), cx, y - 9, FcsDraw.argb(0x9AA8B0, 0.7F * a), 0.4F);
    }

    private void footer(GuiGraphics g, float cx, float a) {
        float y = cy() + R_SUB_OUT + 12;
        String dl = Component.translatable(SquadClient.datalink() ? "gui.mio_icif.command.datalink_on" : "gui.mio_icif.command.datalink_off").getString();
        FcsDraw.textCentered(g, dl, cx, y, FcsDraw.argb(SquadClient.datalink() ? 0x6FF3D6 : 0x7A8A94, 0.85F * a), 0.45F);
        int cd = SquadClient.recallCooldown();
        if (cd > 0) FcsDraw.textCentered(g, Component.translatable("gui.mio_icif.command.recall_cd", (cd + 19) / 20).getString(), cx, y + 8,
            FcsDraw.argb(0x9B7CFF, 0.85F * a), 0.45F);
    }

    static int modeColor(int mode) {
        return mode >= 0 && mode < 4 ? SUB_COLORS[mode] : 0xDDDDDD;
    }

    // ------------------------------------------------------------------ icons (line drawings, s = half size)

    private static void icon(GuiGraphics g, Entry e, float x, float y, float s, int c, float t) {
        switch (e) {
            case MOVE -> {
                FcsDraw.line(g, x, y + s, x, y - s * 0.6F, 1.4F, c);
                FcsDraw.line(g, x - s * 0.7F, y - s * 0.05F, x, y - s, 1.4F, c);
                FcsDraw.line(g, x + s * 0.7F, y - s * 0.05F, x, y - s, 1.4F, c);
                FcsDraw.circle(g, x, y + s, 1.6F, 1F, c);
            }
            case HOLD -> {
                FcsDraw.line(g, x - s * 0.35F, y - s * 0.7F, x - s * 0.35F, y + s * 0.7F, 2F, c);
                FcsDraw.line(g, x + s * 0.35F, y - s * 0.7F, x + s * 0.35F, y + s * 0.7F, 2F, c);
                FcsDraw.circle(g, x, y, s, 0.8F, c);
            }
            case SUPPRESS -> {
                FcsDraw.circle(g, x, y, s, 1.1F, c);
                for (int k = 0; k < 4; k++) {
                    double a = k * Math.PI / 2 + t * 0.03;
                    FcsDraw.line(g, x + (float) Math.sin(a) * s, y - (float) Math.cos(a) * s, x + (float) Math.sin(a) * s * 0.45F,
                        y - (float) Math.cos(a) * s * 0.45F, 1.2F, c);
                }
                FcsDraw.disc(g, x, y, 1.5F, c);
            }
            case FOCUS -> {
                FcsDraw.circle(g, x, y, s * 0.7F, 1F, c);
                FcsDraw.line(g, x - s, y, x - s * 0.35F, y, 1.2F, c);
                FcsDraw.line(g, x + s, y, x + s * 0.35F, y, 1.2F, c);
                FcsDraw.line(g, x, y - s, x, y - s * 0.35F, 1.2F, c);
                FcsDraw.line(g, x, y + s, x, y + s * 0.35F, 1.2F, c);
                FcsDraw.quad(g, x, y - 1.6F, x + 1.6F, y, x, y + 1.6F, x - 1.6F, y, c);
            }
            case VOLLEY -> {
                for (int k = -1; k <= 1; k++) FcsDraw.line(g, x + k * s * 0.8F, y + s, x, y - s * 0.8F, 1.1F, c);
                FcsDraw.disc(g, x, y - s * 0.8F, 1.8F, c);
            }
            case FORM_UP -> {
                float[][] v = {{0, -s * 0.7F}, {-s * 0.7F, s * 0.1F}, {s * 0.7F, s * 0.1F}, {-s * 0.35F, s * 0.8F}, {s * 0.35F, s * 0.8F}};
                for (float[] p : v) FcsDraw.disc(g, x + p[0], y + p[1], 1.6F, c);
                FcsDraw.line(g, x - s * 0.7F, y + s * 0.1F, x, y - s * 0.7F, 0.6F, c);
                FcsDraw.line(g, x + s * 0.7F, y + s * 0.1F, x, y - s * 0.7F, 0.6F, c);
            }
            case CONTROL -> {
                FcsDraw.circle(g, x, y, s * 0.45F, 1.2F, c);
                for (int k = 0; k < 6; k++) {
                    double a = k * Math.PI / 3 + t * 0.02;
                    FcsDraw.line(g, x + (float) Math.sin(a) * s * 0.55F, y - (float) Math.cos(a) * s * 0.55F, x + (float) Math.sin(a) * s,
                        y - (float) Math.cos(a) * s, 1.6F, c);
                }
            }
            case DATALINK -> {
                FcsDraw.line(g, x, y + s, x, y - s * 0.1F, 1.2F, c);
                FcsDraw.disc(g, x, y - s * 0.1F, 1.5F, c);
                for (int k = 1; k <= 2; k++) {
                    FcsDraw.arc(g, x, y - s * 0.1F, s * 0.4F * k, -0.9F, -0.25F, 1F, c);
                    FcsDraw.arc(g, x, y - s * 0.1F, s * 0.4F * k, 0.25F, 0.9F, 1F, c);
                }
            }
            case HOLO -> {
                float h = s * 0.6F;
                float[][] top = {{0, -s}, {s, -s + h * 0.6F}, {0, -s + h * 1.2F}, {-s, -s + h * 0.6F}};
                for (int k = 0; k < 4; k++) {
                    float[] p = top[k], q = top[(k + 1) % 4];
                    FcsDraw.line(g, x + p[0], y + p[1], x + q[0], y + q[1], 1F, c);
                    FcsDraw.line(g, x + p[0], y + p[1] + s, x + q[0], y + q[1] + s, 0.7F, c);
                    FcsDraw.line(g, x + p[0], y + p[1], x + p[0], y + p[1] + s, 0.7F, c);
                }
            }
            case RECALL -> {
                FcsDraw.arc(g, x, y, s * 0.8F, 0.6F, (float) (Math.PI * 2 - 0.6), 1.2F, c);
                FcsDraw.line(g, x, y - s, x, y + s * 0.3F, 1.3F, c);
                FcsDraw.line(g, x - s * 0.35F, y - s * 0.05F, x, y + s * 0.35F, 1.3F, c);
                FcsDraw.line(g, x + s * 0.35F, y - s * 0.05F, x, y + s * 0.35F, 1.3F, c);
            }
        }
    }

    // ------------------------------------------------------------------ input

    private void run() {
        if (done) return;
        done = true;
        if (hover < 0) { onClose(); return; }
        Entry e = Entry.values()[hover];
        if (!enabled(e)) { onClose(); return; }
        UUID unit = SquadClient.selected();
        switch (e) {
            case MOVE -> SquadClient.send(WingmanCommands.A_MOVE, unit, (byte) 0, target.point(), -1);
            case HOLD -> SquadClient.send(WingmanCommands.A_HOLD, unit, (byte) 1, null, -1);
            case SUPPRESS -> SquadClient.send(WingmanCommands.A_SUPPRESS, unit, (byte) 0, target.point(), -1);
            case FOCUS -> SquadClient.send(WingmanCommands.A_FOCUS, unit, (byte) 0, null, target.entityId());
            case VOLLEY -> SquadClient.send(WingmanCommands.A_VOLLEY, WingmanCommandPackets.ALL, (byte) 0, null, -1);
            case FORM_UP -> SquadClient.send(WingmanCommands.A_FORM_UP, unit, (byte) 0, null, -1);
            case CONTROL -> {
                if (sub < 0) { done = false; return; }                     // the fan stays open until a choice
                if (sub < WingmanMode.VALUES.length) SquadClient.send(WingmanCommands.A_MODE, unit, (byte) sub, null, -1);
                else if (sub == 4) SquadClient.send(WingmanCommands.A_SQUAD, unit, (byte) 0, null, -1);
                else SquadClient.send(WingmanCommands.A_RETURN, unit, (byte) 0, null, -1);
            }
            case DATALINK -> SquadClient.send(WingmanCommands.A_DATALINK, WingmanCommandPackets.ALL, (byte) (SquadClient.datalink() ? 0 : 1), null, -1);
            case HOLO -> {
                onClose();
                HoloTable.open();
                return;
            }
            case RECALL -> SquadClient.send(WingmanCommands.A_QUANTUM_RECALL, WingmanCommandPackets.ALL, (byte) 0, null, -1);
        }
        onClose();
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (CommandWheelClient.KEY != null && CommandWheelClient.KEY.matches(keyCode, scanCode)) {
            // a quick tap leaves the wheel open for a click
            if (SquadClient.ticks - openedAt > 4 || hover >= 0) run();
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (CommandWheelClient.KEY != null && CommandWheelClient.KEY.matches(keyCode, scanCode)) {
            run();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (int i = 0; i < chips.size(); i++) {
            float[] c = chips.get(i);
            if (mouseX >= c[0] && mouseX < c[0] + c[2] && mouseY >= c[1] && mouseY < c[1] + c[3]) {
                SquadClient.select(chipIds.get(i));
                play(mio_icif_sounds.WHEEL_SELECT.get(), 1.0F, 0.4F);
                return true;
            }
        }
        if (button == 0) { updateHover(mouseX, mouseY); run(); return true; }
        if (button == 1) { onClose(); return true; }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (CommandWheelClient.KEY != null && CommandWheelClient.KEY.matchesMouse(button)) {
            updateHover(mouseX, mouseY);
            run();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        SquadClient.cycle(scrollY > 0 ? -1 : 1);
        play(mio_icif_sounds.WHEEL_SELECT.get(), 1.0F, 0.3F);
        return true;
    }

    private void play(net.minecraft.sounds.SoundEvent sound, float pitch, float volume) {
        if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
    }
}
