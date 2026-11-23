package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.client.suit.AsciiHud;
import com.miophas.singularity_iteration.common.client.suit.FcsDraw;
import com.miophas.singularity_iteration.common.menu.producer.NeutronPolymerizerMenu;
import com.miophas.singularity_iteration.common.recipe.generic.PolymerizerChain;
import com.miophas.singularity_iteration.common.recipe.generic.PolymerizerProcess;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Particle Polymerizer screen. 0.1.11.8: 248 x 212 with an implosion record in place of the arrow, as a laboratory would
 * photograph a compression: a row of six shadowgraph frames (the sample shrinking inside a converging shock front, one
 * frame lit for the current moment of the step). 0.1.11.147 (calm layout): the frames' times under the film, the
 * streak camera gone (it told the same story), the log card moved under the window as readouts: the kind of step twice
 * the font size with the state beside it, then the input and product, the stage and compression, the flag and time left.
 * The compression comes from the recipe's densities (radius ratio = cube root of the density ratio; a step that makes
 * a lighter material expands, and says so). The strip under the window chooses the chain target and shows the
 * accelerator's warm-up.
 *
 * <p>0.1.11.181: the vanilla 176 x 240, merged (docs/gui_processing_machines.md, "合并布局"): the polymerizer drawn across the
 * top: the input in its feed collar, the battery under it with the bolt beside, the implosion chamber (eight drivers closing
 * on the sample as the step runs), the outlet into the output tray. Under it the six frames (24 wide, were 31) on paper, the
 * log and the strip; the accelerator's bar is named in its tooltip.
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_neutron_polymerizer extends mio_icif_screen<NeutronPolymerizerMenu> {

    /** 0.1.11.181: the film under the machine (x 8..168), the log under it, the strip. */
    private static final int WX = 8, WY = 59, WW = 160;
    private static final int STRIP_Y = 145;
    private static final int CHAIN_X = 8, CHAIN_W = 84;
    private static final int ENERGY_X = 35, ENERGY_Y = 43, ENERGY_W = 9, ENERGY_H = 13;   // the bolt beside the battery
    private static final int FILM_H = 24, FRAME_W = 24, FRAMES_H = 37, HEAD_Y = 98, LINE_Y = 115, LINE_H = 10;
    private static final int CHAMBER_X = 50, CHAMBER_Y = 16, CHAMBER_W = 82, CHAMBER_H = 42, CORE_X = 91, CORE_Y = 37;
    private static final int PAPER = 0xFFE9E5DA, FILM = 0xFF16140F, EMULSION = 0xF0ECDC, INK = 0x24324A, RED = 0x9C2F2A, FADED = 0x6F7A88;
    private static final String K = "gui.mio_icif.polymerizer.";

    public mio_icif_gui_neutron_polymerizer(NeutronPolymerizerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = NeutronPolymerizerMenu.GUI_WIDTH;
        this.imageHeight = NeutronPolymerizerMenu.GUI_HEIGHT;
    }

    private int v(int i) { return menu.value(i); }

    private float u() { return WindRoseView.u(); }

    /** 0.1.11.147: the calm layout (title on the left, no lamp or upgrade hint, window text in the vanilla font). */
    @Override
    protected boolean calmChrome() { return true; }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        VanillaFrame.panel(g, x, y, imageWidth, imageHeight);
        List<PolymerizerChain.Stage> job = job();
        machine(g, x, y, job);
        for (var slot : menu.slots) {
            if (slot.x < 0 || slot.x > imageWidth) continue;   // docked upgrade slots are drawn by the dock
            VanillaFrame.slot(g, x + slot.x - 1, y + slot.y - 1);
        }
        long e = Integer.toUnsignedLong(menu.getEnergy()), max = Math.max(1, Integer.toUnsignedLong(menu.getMaxEnergy()));
        VanillaFrame.bolt(g, x + ENERGY_X, y + ENERGY_Y, e / (double) max);

        VanillaFrame.inset(g, x + WX - 1, y + WY - 1, WW + 2, FRAMES_H + 2, PAPER);
        window(g, x + WX, y + WY, job);
        strip(g, x, y + STRIP_Y, job, mouseX, mouseY);
    }

    private List<PolymerizerChain.Stage> job() {
        var level = Minecraft.getInstance().level;
        ItemStack input = menu.getSlot(0).getItem();
        for (var slot : menu.slots) if (slot.x == NeutronPolymerizerMenu.INPUT_X && slot.y == NeutronPolymerizerMenu.INPUT_Y) { input = slot.getItem(); break; }
        int t = v(10);
        Item target = t < 0 ? null : BuiltInRegistries.ITEM.byId(t);
        return level == null ? List.of() : PolymerizerChain.job(level, input, target);
    }

    // ------------------------------------------------------------------ the machine (0.1.11.181)

    /**
     * The feed collar round the input slot and its pipe into the implosion chamber; the chamber's eight drivers close on the
     * sample as the step runs (the sample shrinking by the step's compression, lit while it works); the outlet into the tray.
     */
    private void machine(GuiGraphics g, int x, int y, List<PolymerizerChain.Stage> job) {
        boolean working = v(2) != 0;
        int stage = Math.max(0, Math.min(v(6), job.size() - 1));
        PolymerizerChain.Stage st = job.isEmpty() ? null : job.get(stage);
        float frac = st == null ? 0 : Math.max(0, Math.min(1, (v(0) - v(11)) / (float) Math.max(1, st.ticks())));
        float comp = st == null ? 1 : (float) Math.max(0.6, Math.min(6, 1 / radiusRatio(st.process())));
        int ix = NeutronPolymerizerMenu.INPUT_X, iy = NeutronPolymerizerMenu.INPUT_Y;
        MachineArt.steel(g, x + ix - 3, y + iy - 3, 22, 22);
        MachineArt.pipe(g, x + ix + 19, x + CHAMBER_X, y + iy + 8, 0, -1);
        MachineArt.iron(g, x + CHAMBER_X, y + CHAMBER_Y, CHAMBER_W, CHAMBER_H);
        float cx = x + CORE_X, cy = y + CORE_Y, push = working ? frac : 0;
        FcsDraw.disc(g, cx, cy, 17, MachineArt.IRON_DARK);
        FcsDraw.disc(g, cx, cy, 15, MachineArt.WINDOW);
        for (int k = 0; k < 8; k++) {
            double a = k * Math.PI / 4 + Math.PI / 8;
            float r0 = 15 - 6 * push, r1 = 21 - 6 * push;
            FcsDraw.line(g, cx + (float) Math.cos(a) * r0, cy + (float) Math.sin(a) * r0, cx + (float) Math.cos(a) * r1, cy + (float) Math.sin(a) * r1, 3F,
                MachineArt.STEEL_LIGHT);
        }
        if (st != null) {
            float rr = Math.max(1.5F, Math.min(7, 6 / lerp(1, comp, push * push)));
            FcsDraw.disc(g, cx, cy, rr, working ? 0xFFE8C547 : 0xFF4A453B);
        }
        MachineArt.pipe(g, x + CHAMBER_X + CHAMBER_W, x + NeutronPolymerizerMenu.OUTPUT_X - 5, y + CORE_Y, 0, -1);
        MachineArt.tray(g, x + NeutronPolymerizerMenu.OUTPUT_X, y + NeutronPolymerizerMenu.OUTPUT_Y);
    }

    // ------------------------------------------------------------------ the implosion record

    private void window(GuiGraphics g, int x, int y, List<PolymerizerChain.Stage> job) {
        float u = u();
        boolean working = v(2) != 0;
        int stage = Math.max(0, Math.min(v(6), job.size() - 1));
        PolymerizerChain.Stage st = job.isEmpty() ? null : job.get(stage);
        float frac = 0;
        if (st != null) frac = Math.max(0, Math.min(1, (v(0) - v(11)) / (float) Math.max(1, st.ticks())));
        double ratio = st == null ? 1 : radiusRatio(st.process());
        float comp = (float) Math.max(0.6, Math.min(6, 1 / ratio));          // how far the sample shrinks (or grows)
        // six shadowgraph frames, their times under the film (the unit once, on the last)
        int frames = 6, fw = FRAME_W, fh = FILM_H;
        int current = Math.min(frames - 1, (int) (frac * frames));
        for (int k = 0; k < frames; k++) {
            float fx = x + 2 + k * (fw + 2), fy = y + 2;
            fill(g, fx, fy, fw, fh, FILM);
            if (st != null) {   // 0.1.11.181: the frames are 24 square (they were 31 x 30): everything three quarters
                float q = k / (frames - 1F);
                float rr = Math.min(9, 8 / lerp(1, comp, q * q));
                float cx = fx + fw / 2F, cy = fy + fh / 2F;
                float shock = lerp(10.5F, rr + 0.8F, q);
                FcsDraw.circle(g, cx, cy, shock, 0.7F, FcsDraw.argb(EMULSION, 0.85F - q * 0.3F));
                FcsDraw.disc(g, cx, cy, rr, 0xFF4A453B);
                FcsDraw.disc(g, cx, cy, rr * 0.82F, 0xFF2B2822);
                FcsDraw.disc(g, cx, cy, rr * 0.5F, 0xFF000000);
            }
            if (st != null && working && k == current) outline(g, fx - 0.5F, fy - 0.5F, fw + 1, fh + 1, 0xFFE8C547);
            if (st != null && k > current) fill(g, fx, fy, fw, fh, 0x8CE9E5DA);
            Component t = k == frames - 1 ? Component.translatable(K + "frame_time", k * 15) : Component.literal(String.valueOf(k * 15));
            PumpCrossSection.text(g, t, fx + fw / 2F - PumpCrossSection.width(t, u) / 2, fy + fh + 2, 0x5B6867, u);
        }
        log(g, x - 1, y + FRAMES_H + 1, WW + 2, job, st, stage, ratio, working);   // 0.1.11.181: the log at HEAD_Y / LINE_Y
    }

    /** 0.1.11.147: the log as readouts under the window, from (x, y) (y is the window's lower edge). */
    private void log(GuiGraphics g, float x, float y, float w, List<PolymerizerChain.Stage> job, PolymerizerChain.Stage st, int stage,
                     double ratio, boolean working) {
        int left = (int) x, right = (int) (x + w), top = topPos;
        float ramp = v(5) / 1000F;
        String stateKey = !working ? "state.idle" : ramp < 0.99F ? "state.warming" : "state.run";
        Component state = Component.translatable(K + stateKey);
        int stateColour = !working ? 0x5B6867 : ramp < 0.99F ? 0xA0602A : 0x2F6E4F;
        Component head = st == null ? Component.translatable(K + "log") : Component.translatable(K + "kind." + kindKey(st.process()));
        if (2 * font.width(head) + 6 + font.width(state) <= right - left) VanillaFrame.bigText(g, head, left, top + HEAD_Y, 0x1C2524);
        else g.drawString(font, fit(head, right - left - font.width(state) - 6), left, top + HEAD_Y + 4, 0x1C2524, false);   // 0.1.11.183
        g.drawString(font, state, right - font.width(state), top + HEAD_Y + 4, stateColour, false);
        int ly = top + LINE_Y;
        if (st == null) {
            g.drawString(font, Component.translatable(K + "no_recipe"), left, ly, ReadoutPanel.TEXT, false);
            return;
        }
        PolymerizerChain.Stage last = job.get(job.size() - 1);
        g.drawString(font, fit(Component.empty().append(job.get(0).input().getHoverName()).append(" \u2192 ").append(last.result().getHoverName()), right - left),
            left, ly, ReadoutPanel.TEXT, false);
        PolymerizerProcess p = st.process();
        net.minecraft.network.chat.MutableComponent l2 = job.size() > 1
            ? Component.translatable(K + "stage", stage + 1, job.size(), st.result().getHoverName()).copy() : Component.empty();
        if (p.densityFrom() > 0 && p.densityTo() > 0) {
            if (!l2.getString().isEmpty()) l2.append(" \u00B7 ");
            l2.append(Component.translatable(K + "ratio", fmt(ratio), fmt(p.densityTo() / (double) p.densityFrom())));
        }
        g.drawString(font, fit(l2, right - left), left, ly + LINE_H, ReadoutPanel.MUTED, false);
        int radius = v(9);
        Component flag = radius > 0 ? Component.translatable(K + "radiation", radius)
            : radius == 0 ? Component.translatable(K + "shielded")
            : Component.translatable(K + (p.real() ? "real" : "fictional"));
        int speed = Math.max(1, v(12));
        double secs = Math.max(0, v(1) - v(0)) / (speed / 1000.0) / 20.0;
        Component l3 = flag.copy().append(" \u00B7 ").append(Component.translatable(K + "eta", time(secs)));
        g.drawString(font, fit(l3, right - left), left, ly + 2 * LINE_H, radius > 0 || !p.real() ? RED : ReadoutPanel.MUTED, false);
    }

    private Component fit(Component c, int maxW) {
        if (font.width(c) <= maxW) return c;
        return Component.literal(font.plainSubstrByWidth(c.getString(), Math.max(0, maxW - font.width("\u2026"))) + "\u2026");
    }

    private static String kindKey(PolymerizerProcess p) {
        return switch (p.kind()) { case "densify", "transmute", "phase", "structure" -> p.kind(); default -> "unknown"; };
    }

    /** Radius ratio of the sample: the cube root of the density ratio (1 when the densities are unknown). */
    private static double radiusRatio(PolymerizerProcess p) {
        if (p.densityFrom() <= 0 || p.densityTo() <= 0) return 1 / 1.5;
        return Math.cbrt(p.densityFrom() / (double) p.densityTo());
    }

    private static String fmt(double v) {
        if (v >= 1000 || v < 0.01) {
            int exp = (int) Math.floor(Math.log10(v));
            return String.format(Locale.ROOT, "%.1fe%d", v / Math.pow(10, exp), exp);
        }
        return String.format(Locale.ROOT, "%.2f", v);
    }

    private static String time(double seconds) {
        long s = Math.round(seconds);
        if (s >= 3600) return Component.translatable("gui.mio_icif.pump.time.hm", s / 3600, (s % 3600) / 60).getString();
        if (s >= 60) return Component.translatable("gui.mio_icif.pump.time.ms", s / 60, s % 60).getString();
        return Component.translatable("gui.mio_icif.pump.time.s", s).getString();
    }

    // ------------------------------------------------------------------ the strip: chain target and warm-up

    private void strip(GuiGraphics g, int x, int y, List<PolymerizerChain.Stage> job, int mouseX, int mouseY) {
        int bx = x + CHAIN_X;
        boolean hover = mouseX >= bx && mouseX < bx + CHAIN_W && mouseY >= y && mouseY < y + 12;
        // 0.1.11.147: a vanilla raised button
        g.fill(bx, y, bx + CHAIN_W, y + 12, VanillaFrame.SHADOW);
        g.fill(bx, y, bx + CHAIN_W - 1, y + 11, VanillaFrame.LIGHT);
        g.fill(bx + 1, y + 1, bx + CHAIN_W - 1, y + 11, hover ? 0xFFD8D8E8 : 0xFFB8B8B8);
        int t = v(10);
        // a target this input cannot reach (left over from another input) reads as off: the job is the first step alone
        if (t >= 0 && (job.size() < 2 || !job.get(job.size() - 1).result().is(BuiltInRegistries.ITEM.byId(t)))) t = -1;
        Component label = t < 0 ? Component.translatable(K + "chain.off")
            : Component.translatable(K + "chain.to", new ItemStack(BuiltInRegistries.ITEM.byId(t)).getHoverName());
        g.drawString(font, fit(label, CHAIN_W - 14), bx + 4, y + 2, 0x404040, false);
        g.drawString(font, "\u25BE", bx + CHAIN_W - 9, y + 2, 0x5B6867, false);
        float ramp = v(5) / 1000F;
        Component pct = Component.literal(Math.round(ramp * 100) + "%");
        int barX = x + CHAIN_X + CHAIN_W + 6, barW = x + 168 - font.width(pct) - 4 - barX;   // 0.1.11.181: the name went to the tooltip
        VanillaFrame.flatBar(g, barX, y + 2, barW, ramp);
        g.drawString(font, pct, x + 168 - font.width(pct), y + 2, 0x404040, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int bx = leftPos + CHAIN_X, by = topPos + STRIP_Y;
        if (button == 0 && mouseX >= bx && mouseX < bx + CHAIN_W && mouseY >= by && mouseY < by + 12) {
            Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, 0);
            playClick();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int rx = mouseX - leftPos, ry = mouseY - topPos;
        if (hoveredSlot != null) return;   // 0.1.11.181: a slot shows its own tooltip only
        if (rx >= ENERGY_X && rx < ENERGY_X + ENERGY_W && ry >= ENERGY_Y && ry < ENERGY_Y + ENERGY_H) {
            g.renderComponentTooltip(this.font, List.of(
                Component.translatable(K + "tip.energy", Integer.toUnsignedLong(menu.getEnergy()), Integer.toUnsignedLong(menu.getMaxEnergy())),
                Component.translatable(K + "tip.draw", v(8))), mouseX, mouseY);
        } else if (rx >= CHAIN_X && rx < CHAIN_X + CHAIN_W && ry >= STRIP_Y && ry < STRIP_Y + 12) {
            g.renderComponentTooltip(this.font, List.of(Component.translatable(K + "tip.chain"), Component.translatable(K + "tip.chain2")), mouseX, mouseY);
        } else if (rx >= CHAIN_X + CHAIN_W + 6 && rx < 168 && ry >= STRIP_Y && ry < STRIP_Y + 12) {
            // EXPLAIN_TIP_HIDDEN (0.1.11.182): only the bar's name; the warm-up text is hidden for now
            // g.renderComponentTooltip(this.font, List.of(Component.translatable(K + "accelerator"), Component.translatable(K + "tip.warmup")), mouseX, mouseY);
            g.renderComponentTooltip(this.font, List.of(Component.translatable(K + "accelerator")), mouseX, mouseY);
        }
    }

    // ------------------------------------------------------------------ helpers

    private static float lerp(float a, float b, float f) { return a + (b - a) * f; }

    private static void fill(GuiGraphics g, float x, float y, float w, float h, int argb) {
        if (w <= 0 || h <= 0) return;
        FcsDraw.quad(g, x, y, x, y + h, x + w, y + h, x + w, y, argb);
    }

    private static void outline(GuiGraphics g, float x, float y, float w, float h, int argb) {
        fill(g, x, y, w, 0.8F, argb);
        fill(g, x, y + h - 0.8F, w, 0.8F, argb);
        fill(g, x, y, 0.8F, h, argb);
        fill(g, x + w - 0.8F, y, 0.8F, h, argb);
    }
}
