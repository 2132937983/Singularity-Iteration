// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.client.screen.panel.PanelCharts;
import com.miophas.singularity_iteration.common.client.screen.panel.PanelStyle;
import com.miophas.singularity_iteration.common.exchange.TradeBoxClientState;
import com.miophas.singularity_iteration.common.exchange.TradeBoxPackets;
import com.miophas.singularity_iteration.common.exchange.TradeBoxRules;
import com.miophas.singularity_iteration.common.exchange.TradeBoxView;
import com.miophas.singularity_iteration.common.menu.producer.TradeBoxMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * 0.1.10.42: the Trade Box panel: the box (27 or 54 slots) and the inventory on the left; the owner, the sell mode, the
 * minimum, the bounty deliveries, the filter and the return slots (advanced) and the results of the market day on the right.
 * It shows the {@link TradeBoxView} the server sends once per second; every setting is an action the server checks again.
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class TradeBoxScreen extends mio_icif_screen<TradeBoxMenu> {
    private static final String K = "gui.mio_icif.trade_box.";
    private static final int W = 330, H = 222, COL_X = 178, COL_W = 146, FILTER_X = 196, FILTER_Y = TradeBoxMenu.RETURN_Y;

    private record Region(int x, int y, int w, int h, Runnable click) {
        boolean inside(double mx, double my) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    }

    private final List<Region> regions = new ArrayList<>();
    private String hoverText;
    private int requestTimer, editTimer;
    private NumberBox minBox;
    /** The minimum the box shows: -1 until the first view (then the number box follows the server until it is edited). */
    private long shownMin = -1;
    private boolean shownInCoins;

    public TradeBoxScreen(TradeBoxMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = W;
        this.imageHeight = H;
    }

    @Override
    protected void init() {
        super.init();
        long v = minBox == null ? TradeBoxRules.DEFAULT_PERCENT : minBox.value();
        minBox = new NumberBox(font, 0, shownInCoins ? TradeBoxRules.MAX_COINS : 100, v);
        addRenderableWidget(minBox);
        request();
    }

    // ------------------------------------------------------------------ network

    private void request() {
        PacketDistributor.sendToServer(new TradeBoxPackets.Request());
        requestTimer = 0;
    }

    private void act(int kind, int index, long value) {
        PacketDistributor.sendToServer(new TradeBoxPackets.BAction(kind, index, value));
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (++requestTimer >= 20) request();
        TradeBoxView v = TradeBoxClientState.view();
        if (v == null) return;
        // the number box follows the server until the player types; a typed number is sent half a second after the last key
        if (minBox.isFocused()) {
            long typed = minBox.value();
            if (typed != shownMin && ++editTimer >= 10) {
                act(v.inCoins() ? TradeBoxPackets.COINS : TradeBoxPackets.PERCENT, 0, typed);
                shownMin = typed;
                editTimer = 0;
            }
        } else {
            long server = v.inCoins() ? v.coins() : v.percent();
            if (server != shownMin || v.inCoins() != shownInCoins) {
                shownInCoins = v.inCoins();
                minBox.setRange(shownInCoins ? 1 : 0, shownInCoins ? TradeBoxRules.MAX_COINS : 100);
                minBox.set(server);
                shownMin = server;
            }
        }
    }

    @Override
    public void removed() {
        super.removed();
        TradeBoxClientState.clear();
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (minBox.isVisible() && minBox.mouseClicked(mx, my, button)) { setFocused(minBox); editTimer = 0; return true; }
        if (minBox.isFocused()) { minBox.setFocused(false); setFocused(null); }
        if (button == 0) {
            for (int i = regions.size() - 1; i >= 0; i--) {
                Region r = regions.get(i);
                if (r.inside(mx, my)) { r.click().run(); return true; }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (minBox.isFocused() && key != GLFW.GLFW_KEY_ESCAPE) { editTimer = 0; return minBox.keyPressed(key, scan, modifiers) || true; }
        return super.keyPressed(key, scan, modifiers);
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (minBox.isFocused()) { editTimer = 0; return minBox.charTyped(c, modifiers) || true; }
        return super.charTyped(c, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (minBox.isVisible() && minBox.mouseScrolled(mx, my, sx, sy)) { editTimer = 0; setFocused(minBox); minBox.setFocused(true); return true; }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    // ------------------------------------------------------------------ drawing helpers

    private void button(GuiGraphics g, int x, int y, int w, int h, String label, boolean on, boolean enabled, Runnable click, int mx, int my) {
        int ax = leftPos + x, ay = topPos + y;
        boolean hot = enabled && mx >= ax && mx < ax + w && my >= ay && my < ay + h;
        g.fill(ax, ay, ax + w, ay + h, !enabled ? 0xFF161B22 : on ? 0xFF2F81F7 : hot ? 0xFF2D3748 : 0xFF1F2630);
        g.renderOutline(ax, ay, w, h, on ? 0xFF58A6FF : PanelCharts.PANEL_LINE);
        String text = font.width(label) > w - 4 ? font.plainSubstrByWidth(label, w - 6) : label;
        g.drawString(font, text, ax + (w - font.width(text)) / 2, ay + (h - 8) / 2, enabled ? PanelCharts.TEXT : PanelCharts.DIM, false);
        if (!text.equals(label)) hover(x, y, w, h, label, mx, my);
        if (enabled) regions.add(new Region(ax, ay, w, h, click));
    }

    private void hover(int x, int y, int w, int h, String text, int mx, int my) {
        int ax = leftPos + x, ay = topPos + y;
        if (mx >= ax && mx < ax + w && my >= ay && my < ay + h) hoverText = text;
    }

    /** A line cut to {@code w} (the whole line as tooltip when cut). */
    private void line(GuiGraphics g, String s, int x, int y, int w, int colour, int mx, int my) {
        String fit = font.width(s) > w ? font.plainSubstrByWidth(s, w - font.width("..")) + ".." : s;
        g.drawString(font, fit, leftPos + x, topPos + y, colour, false);
        if (!fit.equals(s)) hover(x, y - 1, w, 10, s, mx, my);
    }

    private static String tr(String key, Object... args) { return Component.translatable(K + key, args).getString(); }

    private static String n(long v) { return PanelCharts.number(v); }

    private void slotFrame(GuiGraphics g, int x, int y) {
        int ax = leftPos + x, ay = topPos + y;
        g.fill(ax - 1, ay - 1, ax + 17, ay + 17, PanelCharts.PANEL_LINE);
        g.fill(ax, ay, ax + 16, ay + 16, 0xFF0D1117);
    }

    private static Item item(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl == null || !BuiltInRegistries.ITEM.containsKey(rl) ? null : BuiltInRegistries.ITEM.get(rl);
    }

    // ------------------------------------------------------------------ render

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mx, int my) {
        g.fill(leftPos, topPos, leftPos + W, topPos + H, 0xF00D1117);
        g.renderOutline(leftPos, topPos, W, H, PanelCharts.PANEL_LINE);
        boolean advanced = menu.advanced();
        g.drawString(font, font.plainSubstrByWidth(title.getString(), COL_X - 12), leftPos + 8, topPos + 6, PanelCharts.ACCENT, false);
        for (int i = 0; i < menu.sellSlots; i++) slotFrame(g, TradeBoxMenu.SELL_X + (i % 9) * 18, TradeBoxMenu.SELL_Y + (i / 9) * 18);
        g.drawString(font, Component.translatable("container.inventory"), leftPos + TradeBoxMenu.SELL_X, topPos + TradeBoxMenu.INV_Y - 10, PanelCharts.DIM, false);
        for (int i = 0; i < 36; i++)
            slotFrame(g, TradeBoxMenu.SELL_X + (i % 9) * 18, i < 27 ? TradeBoxMenu.INV_Y + (i / 9) * 18 : TradeBoxMenu.HOTBAR_Y);
        TradeBoxView v = TradeBoxClientState.view();
        if (!advanced) {
            // the basic box: what it does, under its 27 slots
            int y = TradeBoxMenu.SELL_Y + 3 * 18 + 6;
            for (FormattedCharSequence l : font.split(Component.translatable(K + "basic_help"), COL_X - 16)) {
                if (y + 8 > TradeBoxMenu.INV_Y - 12) break;
                g.drawString(font, l, leftPos + 8, topPos + y, PanelCharts.DIM, false);
                y += 10;
            }
        }
        if (v == null) {
            g.drawString(font, Component.translatable("gui.mio_icif.terminal.loading"), leftPos + COL_X, topPos + 6, PanelCharts.DIM, false);
            return;
        }
        column(g, v, advanced, mx, my);
    }

    private void column(GuiGraphics g, TradeBoxView v, boolean advanced, int mx, int my) {
        int x = COL_X, w = COL_W, y = 6;
        String owner = v.owner().isEmpty() ? tr("no_owner") : tr("owner", v.owner());
        line(g, owner, x, y, w, v.owner().isEmpty() ? PanelCharts.down() : v.ownerOnline() ? PanelCharts.up() : PanelCharts.WARN, mx, my);
        hover(x, y - 1, w, 10, tr("owner_help"), mx, my);
        y = 18;
        // the sell mode
        if (advanced) {
            // each button as wide as its label and an even share of the rest (equal widths when the labels do not fit)
            int gap = 3, sum = 0;
            int[] lw = new int[TradeBoxRules.MODES];
            for (int i = 0; i < TradeBoxRules.MODES; i++) { lw[i] = font.width(tr("mode." + i)); sum += lw[i]; }
            int extra = w - gap * (TradeBoxRules.MODES - 1) - sum, bx = x;
            for (int i = 0; i < TradeBoxRules.MODES; i++) {
                final int mode = i;
                int bw = extra >= 4 * TradeBoxRules.MODES
                    ? lw[i] + extra / TradeBoxRules.MODES + (i == TradeBoxRules.MODES - 1 ? extra % TradeBoxRules.MODES : 0)
                    : (w - gap * (TradeBoxRules.MODES - 1)) / TradeBoxRules.MODES;
                button(g, bx, y, bw, 13, tr("mode." + i), v.mode() == i, true, () -> act(TradeBoxPackets.MODE, 0, mode), mx, my);
                hover(bx, y, bw, 13, tr("mode_help." + i), mx, my);
                bx += bw + gap;
            }
        } else {
            line(g, tr("mode_basic"), x, y + 3, w, PanelCharts.TEXT, mx, my);
            hover(x, y, w, 13, tr("mode_help.0"), mx, my);
        }
        y += 17;
        // the minimum: percent of the bid (auction, instant), or coins per item (instant, advanced)
        boolean coinsAllowed = advanced && v.mode() != TradeBoxRules.AUCTION;
        boolean inCoins = coinsAllowed && v.inCoins();
        String label = tr(inCoins ? "min_coins" : "min_percent");
        int lw = Math.min(font.width(label), w - 60 - 22 - 4);
        line(g, label, x, y + 2, lw, PanelCharts.DIM, mx, my);
        minBox.setX(leftPos + x + lw + 4);
        minBox.setY(topPos + y);
        minBox.setWidth(60);
        minBox.setVisible(true);
        hover(x, y, lw + 64, 12, tr(inCoins ? "min_coins_help" : "min_percent_help"), mx, my);
        if (coinsAllowed) {
            button(g, x + w - 22, y, 22, 12, inCoins ? "c" : "%", false, true, () -> act(TradeBoxPackets.IN_COINS, 0, inCoins ? 0 : 1), mx, my);
            hover(x + w - 22, y, 22, 12, tr("unit_help"), mx, my);
        }
        y += 16;
        if (advanced) {
            button(g, x, y, w, 13, tr(v.autoDeliver() ? "deliver_on" : "deliver_off", v.claimed()), v.autoDeliver(), true,
                () -> act(TradeBoxPackets.AUTO_DELIVER, 0, v.autoDeliver() ? 0 : 1), mx, my);
            hover(x, y, w, 13, tr("deliver_help"), mx, my);
            y += 15;
            boolean instant = v.mode() != TradeBoxRules.AUCTION;
            button(g, x, y, w, 13, tr(v.offlineAuction() ? "offline_on" : "offline_off"), v.offlineAuction() && instant, instant,
                () -> act(TradeBoxPackets.OFFLINE_AUCTION, 0, v.offlineAuction() ? 0 : 1), mx, my);
            hover(x, y, w, 13, tr("offline_help"), mx, my);
            filterAndReturns(g, v, mx, my);
            y = FILTER_Y + 3 * 18 + 6;
        } else {
            y += 4;
        }
        // the results of the market day: the label on the left, the numbers on the right
        int dy = 10;
        result(g, tr("today_sold"), x, y, w, PanelCharts.TEXT, v.counter(TradeBoxRules.SOLD), v.counter(TradeBoxRules.COINS), "", mx, my);
        y += dy;
        result(g, tr("today_auction"), x, y, w, PanelCharts.TEXT, v.counter(TradeBoxRules.AUCTIONED), -1, tr("today_auction_help"), mx, my);
        y += dy;
        if (advanced) {
            result(g, tr("today_scrap"), x, y, w, PanelCharts.DIM, v.counter(TradeBoxRules.SCRAPPED), -1, "", mx, my);
            y += dy;
            result(g, tr("today_bounty"), x, y, w, PanelCharts.TEXT, v.counter(TradeBoxRules.DELIVERED), v.counter(TradeBoxRules.DELIVERED_COINS), "", mx, my);
            y += dy;
        }
        String status = v.status() < 0 ? tr("status_none") : Component.translatable("gui.mio_icif.terminal.status." + v.status()).getString();
        line(g, tr("status", status), x, y, w, v.status() == 0 ? PanelCharts.up() : PanelCharts.DIM, mx, my);
        hover(x, y - 1, w, 10, tr("status_help"), mx, my);
        if (!advanced) {
            y += dy + 4;
            for (FormattedCharSequence l : font.split(Component.translatable(K + "upgrade_hint"), w)) {
                if (y + 8 > H - 4) break;
                g.drawString(font, l, leftPos + x, topPos + y, PanelCharts.DIM, false);
                y += 10;
            }
        }
    }

    /**
     * A result of the market day: the label on the left (cut when it does not fit), the items and, when {@code coins >= 0}, the
     * coins on the right (in the short form 12.3M when the full numbers leave the label too little room). The tooltip has the full
     * numbers and {@code help}.
     */
    private void result(GuiGraphics g, String label, int x, int y, int w, int colour, long items, long coins, String help, int mx, int my) {
        String full = coins < 0 ? n(items) : tr("today_value", n(items), n(coins));
        String value = full;
        if (font.width(label) > w - font.width(full) - 4)
            value = coins < 0 ? PanelStyle.compact(items) : tr("today_value", PanelStyle.compact(items), PanelStyle.compact(coins));
        int vw = font.width(value);
        g.drawString(font, value, leftPos + x + w - vw, topPos + y, colour, false);
        int room = w - vw - 4;
        String shown = font.width(label) <= room ? label : font.plainSubstrByWidth(label, Math.max(0, room - font.width(".."))) + "..";
        g.drawString(font, shown, leftPos + x, topPos + y, PanelCharts.DIM, false);
        String tip = label + ": " + (coins < 0 ? n(items) : tr("today_value_help", n(items), n(coins)));
        hover(x, y - 1, w, 10, help.isEmpty() ? tip : tip + "\n" + help, mx, my);
    }

    /** The filter cells (click with an item on the cursor: that commodity; with an empty cursor: clear) and the return slots. */
    private void filterAndReturns(GuiGraphics g, TradeBoxView v, int mx, int my) {
        // the clear button ends 4 px before the returns label; the filter label takes the rest
        String clear = tr("filter_clear");
        int cw = Math.min(40, Math.max(26, font.width(clear) + 6)), cx = TradeBoxMenu.RETURN_X - 4 - cw, fx0 = FILTER_X - 18;
        line(g, tr("filter"), fx0, FILTER_Y - 11, cx - 2 - fx0, PanelCharts.ACCENT, mx, my);
        hover(fx0, FILTER_Y - 12, cx - 2 - fx0, 10, tr("filter_help"), mx, my);
        button(g, cx, FILTER_Y - 12, cw, 10, clear, false, v.filter().stream().anyMatch(f -> !f.isEmpty()),
            () -> act(TradeBoxPackets.FILTER_CLEAR, 0, 0), mx, my);
        for (int i = 0; i < TradeBoxRules.FILTER_SLOTS; i++) {
            final int index = i;
            int fx = FILTER_X - 18 + (i % 3) * 18, fy = FILTER_Y + (i / 3) * 18;
            slotFrame(g, fx, fy);
            String id = i < v.filter().size() ? v.filter().get(i) : "";
            Item it = id.isEmpty() ? null : item(id);
            if (it != null) {
                g.renderItem(new ItemStack(it), leftPos + fx, topPos + fy);
                hover(fx, fy, 16, 16, new ItemStack(it).getHoverName().getString(), mx, my);
            }
            regions.add(new Region(leftPos + fx, topPos + fy, 16, 16, () -> act(TradeBoxPackets.FILTER, index, 0)));
        }
        line(g, tr("returns"), TradeBoxMenu.RETURN_X, FILTER_Y - 11, 56, PanelCharts.ACCENT, mx, my);
        hover(TradeBoxMenu.RETURN_X, FILTER_Y - 12, 56, 10, tr("returns_help"), mx, my);
        for (int i = 0; i < TradeBoxRules.RETURN_SLOTS; i++) slotFrame(g, TradeBoxMenu.RETURN_X + (i % 3) * 18, TradeBoxMenu.RETURN_Y + (i / 3) * 18);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) { }

    @Override
    protected int statusLampColor() { return 0; }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        regions.clear();
        hoverText = null;
        minBox.setVisible(false);
        super.render(g, mx, my, pt);   // the item tooltips of the slots (mio_icif_screen)
        if (hoverText != null && menu.getCarried().isEmpty() && (hoveredSlot == null || !hoveredSlot.hasItem()))
            g.renderTooltip(font, font.split(Component.literal(hoverText), 220), mx, my);
    }
}
