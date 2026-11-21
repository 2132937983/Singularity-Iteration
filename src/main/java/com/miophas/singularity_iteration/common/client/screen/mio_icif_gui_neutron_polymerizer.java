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
 * frame lit for the current moment of the step), a streak-camera trace of radius against time below, and a log card.
 * The compression comes from the recipe's densities (radius ratio = cube root of the density ratio; a step that makes
 * a lighter material expands, and says so). The strip under the window chooses the chain target and shows the
 * accelerator's warm-up.
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_neutron_polymerizer extends mio_icif_screen<NeutronPolymerizerMenu> {

    private static final int WX = 34, WY = 15, WW = 207, WH = 95;
    private static final int STRIP_Y = 113;
    private static final int CHAIN_X = 34, CHAIN_W = 100;
    private static final int ENERGY_X = 13, ENERGY_Y = 50, ENERGY_W = 10, ENERGY_H = 28;
    private static final int PAPER = 0xFFE9E5DA, FILM = 0xFF16140F, EMULSION = 0xF0ECDC, INK = 0x24324A, RED = 0x9C2F2A, FADED = 0x6F7A88;
    private static final String K = "gui.mio_icif.polymerizer.";

    public mio_icif_gui_neutron_polymerizer(NeutronPolymerizerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = NeutronPolymerizerMenu.GUI_WIDTH;
        this.imageHeight = NeutronPolymerizerMenu.GUI_HEIGHT;
    }

    private int v(int i) { return menu.value(i); }

    private float u() {
        return (float) Math.min(1.0, 2.0 / Math.max(1.0, Minecraft.getInstance().getWindow().getGuiScale()));
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        SiGuiTheme.panel(g, x, y, imageWidth, imageHeight);
        SiGuiTheme.slot(g, x + NeutronPolymerizerMenu.INPUT_X - 1, y + NeutronPolymerizerMenu.INPUT_Y - 1);
        SiGuiTheme.slot(g, x + NeutronPolymerizerMenu.OUTPUT_X - 1, y + NeutronPolymerizerMenu.OUTPUT_Y - 1);
        SiGuiTheme.slot(g, x + NeutronPolymerizerMenu.BATTERY_X - 1, y + NeutronPolymerizerMenu.BATTERY_Y - 1);
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++)
            SiGuiTheme.slot(g, x + NeutronPolymerizerMenu.INVENTORY_X - 1 + c * 18, y + NeutronPolymerizerMenu.INVENTORY_Y - 1 + r * 18);
        for (int c = 0; c < 9; c++) SiGuiTheme.slot(g, x + NeutronPolymerizerMenu.INVENTORY_X - 1 + c * 18, y + NeutronPolymerizerMenu.HOTBAR_Y - 1);
        int ex = x + ENERGY_X, ey = y + ENERGY_Y;
        g.fill(ex - 1, ey - 1, ex + ENERGY_W + 1, ey + ENERGY_H + 1, SiGuiTheme.SLOT_SHADOW);
        g.fill(ex, ey, ex + ENERGY_W, ey + ENERGY_H, SiGuiTheme.SCREEN_BG);
        long e = Integer.toUnsignedLong(menu.getEnergy()), max = Math.max(1, Integer.toUnsignedLong(menu.getMaxEnergy()));
        int filled = (int) Math.round(ENERGY_H * Math.min(1.0, e / (double) max));
        if (filled > 0) g.fillGradient(ex, ey + ENERGY_H - filled, ex + ENERGY_W, ey + ENERGY_H, SiGuiTheme.BAR_FROM, SiGuiTheme.BAR_TO);

        List<PolymerizerChain.Stage> job = job();
        g.fill(x + WX - 1, y + WY - 1, x + WX + WW + 1, y + WY + WH + 1, 0xFF7A7F85);
        g.fill(x + WX, y + WY, x + WX + WW, y + WY + WH, PAPER);
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
        // six shadowgraph frames
        int frames = 6, fw = 31, fh = 38;
        int current = Math.min(frames - 1, (int) (frac * frames));
        for (int k = 0; k < frames; k++) {
            float fx = x + 4 + k * (fw + 2), fy = y + 4;
            fill(g, fx, fy, fw, fh, FILM);
            if (st != null) {
                float q = k / (frames - 1F);
                float rr = Math.min(15, 11 / lerp(1, comp, q * q));
                float cx = fx + fw / 2F, cy = fy + fh / 2F - 2;
                float shock = lerp(15, rr + 0.8F, q);
                FcsDraw.circle(g, cx, cy, shock, 0.7F, FcsDraw.argb(EMULSION, 0.85F - q * 0.3F));
                FcsDraw.disc(g, cx, cy, rr, 0xFF4A453B);
                FcsDraw.disc(g, cx, cy, rr * 0.82F, 0xFF2B2822);
                FcsDraw.disc(g, cx, cy, rr * 0.5F, 0xFF000000);
            }
            PumpCrossSection.text(g, Component.translatable(K + "frame_time", k * 15), fx + 2, fy + fh - 9 * u, 0xD8D2C0, u);
            if (st != null && working && k == current) outline(g, fx - 0.5F, fy - 0.5F, fw + 1, fh + 1, 0xFFE8C547);
            if (st != null && k > current) fill(g, fx, fy, fw, fh, 0x8CE9E5DA);
        }
        // streak camera: radius against time over the current step
        float sx = x + 4, sy = y + 47, sw = 120, sh = 44;
        fill(g, sx, sy, sw, sh, FILM);
        if (st != null) {
            float upto = working ? frac : 0;
            for (int c = 0; c < sw && c / sw <= upto; c++) {
                float q = c / sw;
                float rr = Math.min(sh / 2 - 7, (sh / 2 - 8) / lerp(1, comp, q * q));   // clear of the label at the top
                float mid = sy + sh / 2 + 3;
                fill(g, sx + c, mid - rr, 1, rr * 2, 0xFF3C372D);
                fill(g, sx + c, mid - rr - 1, 1, 1.2F, FcsDraw.argb(EMULSION, 0.8F));
                fill(g, sx + c, mid + rr - 0.2F, 1, 1.2F, FcsDraw.argb(EMULSION, 0.8F));
            }
        }
        PumpCrossSection.text(g, Component.translatable(K + "streak"), sx + 2, sy + 1.5F, 0xD8D2C0, u);
        // the log card
        log(g, x + 130, sy, WW - 132, job, st, stage, ratio, working);
    }

    private void log(GuiGraphics g, float x, float y, float w, List<PolymerizerChain.Stage> job, PolymerizerChain.Stage st, int stage,
                     double ratio, boolean working) {
        float u = u(), lh = 9 * u + 0.6F;
        fill(g, x - 2, y, 0.4F, 44, 0xFFD29B9B);
        float ramp = v(5) / 1000F;
        String stateKey = !working ? "state.idle" : ramp < 0.99F ? "state.warming" : "state.run";
        Component state = Component.translatable(K + stateKey);
        line(g, Component.translatable(K + "log"), x, y, INK, w - PumpCrossSection.width(state, u) - 2);
        PumpCrossSection.text(g, state, x + w - PumpCrossSection.width(state, u), y, working ? INK : FADED, u);
        y += lh;
        if (st == null) {
            line(g, Component.translatable(K + "no_recipe"), x, y, FADED, w);
            return;
        }
        PolymerizerChain.Stage last = job.get(job.size() - 1);
        line(g, Component.empty().append(job.get(0).input().getHoverName()).append(" → ").append(last.result().getHoverName()), x, y, INK, w);
        y += lh;
        Component step = job.size() > 1 ? Component.translatable(K + "stage", stage + 1, job.size(), st.result().getHoverName())
            : Component.translatable(K + "kind." + kindKey(st.process()));
        line(g, step, x, y, INK, w);
        y += lh;
        PolymerizerProcess p = st.process();
        if (p.densityFrom() > 0 && p.densityTo() > 0) {
            line(g, Component.translatable(K + "ratio", fmt(ratio), fmt(p.densityTo() / (double) p.densityFrom())), x, y, INK, w);
            y += lh;
        }
        int radius = v(9);
        Component flag = radius > 0 ? Component.translatable(K + "radiation", radius)
            : radius == 0 ? Component.translatable(K + "shielded")
            : Component.translatable(K + (p.real() ? "real" : "fictional"));
        line(g, flag, x, y, radius > 0 || !p.real() ? RED : FADED, w);
        y += lh;
        int speed = Math.max(1, v(12));
        double secs = Math.max(0, v(1) - v(0)) / (speed / 1000.0) / 20.0;
        line(g, Component.translatable(K + "eta", time(secs)), x, y, FADED, w);
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
        float u = u();
        int bx = x + CHAIN_X;
        boolean hover = mouseX >= bx && mouseX < bx + CHAIN_W && mouseY >= y && mouseY < y + 12;
        g.fill(bx, y, bx + CHAIN_W, y + 12, SiGuiTheme.OUTLINE);
        g.fill(bx + 1, y + 1, bx + CHAIN_W - 1, y + 11, hover ? 0xFFF4F6F8 : 0xFFE6E8EA);
        int t = v(10);
        // a target this input cannot reach (left over from another input) reads as off: the job is the first step alone
        if (t >= 0 && (job.size() < 2 || !job.get(job.size() - 1).result().is(BuiltInRegistries.ITEM.byId(t)))) t = -1;
        Component label = t < 0 ? Component.translatable(K + "chain.off")
            : Component.translatable(K + "chain.to", new ItemStack(BuiltInRegistries.ITEM.byId(t)).getHoverName());
        line(g, label, bx + 3, y + 6 - 4.5F * u, 0x2A2E33, CHAIN_W - 12);
        PumpCrossSection.text(g, Component.literal("▾"), bx + CHAIN_W - 8, y + 6 - 4.5F * u, 0x80868D, u);
        Component acc = Component.translatable(K + "accelerator");
        float ax = x + 140;
        PumpCrossSection.text(g, acc, ax, y + 6 - 4.5F * u, 0x2A2E33, u);
        float barX = ax + PumpCrossSection.width(acc, u) + 3, barW = x + 241 - 22 - barX;
        g.fill((int) barX - 1, y + 2, (int) (barX + barW) + 1, y + 10, SiGuiTheme.SLOT_SHADOW);
        g.fill((int) barX, y + 3, (int) (barX + barW), y + 9, SiGuiTheme.SCREEN_BG);
        float ramp = v(5) / 1000F;
        if (ramp > 0) g.fillGradient((int) barX, y + 3, (int) (barX + barW * ramp), y + 9, 0xFFD2902A, 0xFFC8423C);
        Component pct = Component.literal(Math.round(ramp * 100) + "%");
        PumpCrossSection.text(g, pct, x + 241 - PumpCrossSection.width(pct, u), y + 6 - 4.5F * u, 0x2A2E33, u);
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
        if (rx >= ENERGY_X && rx < ENERGY_X + ENERGY_W && ry >= ENERGY_Y && ry < ENERGY_Y + ENERGY_H) {
            g.renderComponentTooltip(this.font, List.of(
                Component.translatable(K + "tip.energy", Integer.toUnsignedLong(menu.getEnergy()), Integer.toUnsignedLong(menu.getMaxEnergy())),
                Component.translatable(K + "tip.draw", v(8))), mouseX, mouseY);
        } else if (rx >= CHAIN_X && rx < CHAIN_X + CHAIN_W && ry >= STRIP_Y && ry < STRIP_Y + 12) {
            g.renderComponentTooltip(this.font, List.of(Component.translatable(K + "tip.chain"), Component.translatable(K + "tip.chain2")), mouseX, mouseY);
        } else if (rx >= 140 && rx < 241 && ry >= STRIP_Y && ry < STRIP_Y + 12) {
            g.renderComponentTooltip(this.font, List.of(Component.translatable(K + "tip.warmup")), mouseX, mouseY);
        }
    }

    // ------------------------------------------------------------------ helpers

    private void line(GuiGraphics g, Component c, float x, float y, int rgb, float maxW) {
        float u = u();
        if (PumpCrossSection.width(c, u) > maxW) {
            String s = c.getString();
            int n = s.length();
            while (n > 1 && PumpCrossSection.width(s.substring(0, n) + "…", u) > maxW) n--;
            c = Component.literal(s.substring(0, n) + "…");
        }
        PumpCrossSection.text(g, c, x, y, rgb, u);
    }

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
