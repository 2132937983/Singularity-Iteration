package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.client.screen.panel.PanelCharts;
import com.miophas.singularity_iteration.common.exchange.ExchangeClientState;
import com.miophas.singularity_iteration.common.exchange.ExchangePackets;
import com.miophas.singularity_iteration.common.exchange.ExchangeView;
import com.miophas.singularity_iteration.common.item.normal.mio_icif_normal;
import com.miophas.singularity_iteration.common.menu.producer.FutureElcMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * 0.1.10.21 (futures M4): the exchange panel of the futures machine, a full-screen data panel in five tabs.
 *
 * <ul>
 *   <li><b>Market</b>: watchlist (sparkline, change, volume), daily candles with volume, the 5-day mean, the fundamental
 *       value and the price band, the depth of the order book, the last trades, the market's own quotes (treasury supply,
 *       daily buy room) and an order ticket (buy / sell, now / resting order, price, quantity, cost).</li>
 *   <li><b>Commissions</b>: the board (deliver, withdraw), a form to post one (item and reward picked from the inventory),
 *       and the progress of items toward a listing.</li>
 *   <li><b>Futures</b>: contract prices for 3 and 7 days, open long or short, the open positions.</li>
 *   <li><b>Orders and mail</b>: resting orders (cancel), the mailbox (collect), the UU-matter account (deposit, withdraw).</li>
 *   <li><b>Treasury</b>: coins created and destroyed per day, the market's stock, listings.</li>
 * </ul>
 * The panel shows the {@link ExchangeView} that the server sends (on request, once per second while open); every action is a
 * packet that the server checks again. Before 0.1.10.21 the screen was a price chart with buy and sell buttons.
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_future_elc extends mio_icif_screen<FutureElcMenu> {
    private static final int TABS = 5, ROW = 20;
    private static final String K = "gui.mio_icif.exchange.";

    private record Region(int x, int y, int w, int h, Runnable left, Runnable right) {
        boolean inside(double mx, double my) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    }

    private final List<Region> regions = new ArrayList<>();
    private int tab;
    private String selected = "";
    private boolean prefilled;
    private int listScroll, boardScroll;
    private boolean buySide = true, resting;
    private int requestTimer;
    private String hoverText;
    // commission form
    private Item target = Items.AIR, rewardItem = Items.AIR;
    private int rewardKind, pickMode;

    private EditBox priceBox, qtyBox, postQty, postAmount, postDays, futQty;

    public mio_icif_gui_future_elc(FutureElcMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = this.width;
        this.imageHeight = this.height;
    }

    @Override
    protected void init() {
        super.init();
        this.imageWidth = this.width;
        this.imageHeight = this.height;
        // 0.1.10.24: super.init() centred the panel with the old imageWidth (0 on first open), so leftPos was width / 2 and
        // JEI / EMI took the left half of the screen as free space for their bookmark / favourite lists (clicks went to them)
        this.leftPos = 0;
        this.topPos = 0;
        priceBox = box("1", 9);
        qtyBox = box("1", 4);
        postQty = box("64", 4);
        postAmount = box("100", 9);
        postDays = box("3", 1);
        futQty = box("16", 4);
        request();
    }

    private EditBox box(String value, int digits) {
        EditBox b = new EditBox(this.font, 0, 0, 50, 12, Component.empty());
        b.setMaxLength(digits);
        b.setFilter(s -> s.chars().allMatch(Character::isDigit));
        b.setValue(value);
        b.setBordered(true);
        addRenderableWidget(b);
        return b;
    }

    private static long num(EditBox b, long fallback) {
        try { return b.getValue().isEmpty() ? fallback : Long.parseLong(b.getValue()); } catch (NumberFormatException e) { return fallback; }
    }

    // ------------------------------------------------------------------ network

    private void request() {
        PacketDistributor.sendToServer(new ExchangePackets.Request(selected));
        requestTimer = 0;
    }

    private void act(int kind, String id, long a, long b, int c, int d, boolean flag, String text) {
        PacketDistributor.sendToServer(new ExchangePackets.Action(kind, id, a, b, c, d, flag, text, selected));
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (++requestTimer >= 20) request();
    }

    @Override
    public void removed() {
        super.removed();
        ExchangeClientState.clear();
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for (EditBox b : List.of(priceBox, qtyBox, postQty, postAmount, postDays, futQty)) {
            if (b.isVisible() && b.mouseClicked(mx, my, button)) { setFocused(b); return true; }
        }
        if (getFocused() instanceof EditBox b) { b.setFocused(false); setFocused(null); }
        for (int i = regions.size() - 1; i >= 0; i--) {
            Region r = regions.get(i);
            if (!r.inside(mx, my)) continue;
            Runnable action = button == 1 ? r.right() : r.left();
            if (action != null) { action.run(); return true; }
        }
        return false;
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (getFocused() instanceof EditBox b && b.isVisible() && key != GLFW.GLFW_KEY_ESCAPE) return b.keyPressed(key, scan, modifiers) || true;
        return super.keyPressed(key, scan, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (tab == 0 && mx < leftW() + 6) { listScroll = Math.max(0, listScroll - (int) Math.signum(sy)); return true; }
        if (tab == 1) { boardScroll = Math.max(0, boardScroll - (int) Math.signum(sy)); return true; }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    private void button(GuiGraphics g, int x, int y, int w, int h, Component label, boolean on, Runnable click, int mx, int my) {
        boolean hot = mx >= x && mx < x + w && my >= y && my < y + h;
        g.fill(x, y, x + w, y + h, on ? 0xFF2F81F7 : hot ? 0xFF2D3748 : 0xFF1F2630);
        g.renderOutline(x, y, w, h, on ? 0xFF58A6FF : PanelCharts.PANEL_LINE);
        String text = label.getString();
        if (font.width(text) > w - 4) text = font.plainSubstrByWidth(text, w - 6);
        g.drawString(font, text, x + (w - font.width(text)) / 2, y + (h - 8) / 2, PanelCharts.TEXT, false);
        regions.add(new Region(x, y, w, h, click, null));
    }

    // ------------------------------------------------------------------ layout helpers

    private int leftW() { return Math.max(110, Math.min(170, width / 4)); }

    private int rightW() { return Math.max(140, Math.min(190, width * 3 / 10)); }

    private static Item item(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl == null || !BuiltInRegistries.ITEM.containsKey(rl) ? Items.BARRIER : BuiltInRegistries.ITEM.get(rl);
    }

    private static String name(String id) { return new ItemStack(item(id)).getHoverName().getString(); }

    private Component unit(int currency) { return Component.translatable(currency == 1 ? K + "uu" : K + "coins"); }

    private void hover(int x, int y, int w, int h, String text, int mx, int my) {
        if (mx >= x && mx < x + w && my >= y && my < y + h) hoverText = text;
    }

    // ------------------------------------------------------------------ render

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) { }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) { }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g, mx, my, pt);
        g.fill(0, 0, width, height, 0xE00D1117);
        regions.clear();
        hoverText = null;
        for (EditBox b : List.of(priceBox, qtyBox, postQty, postAmount, postDays, futQty)) b.setVisible(false);
        ExchangeView v = ExchangeClientState.view();
        header(g, v, mx, my);
        if (v == null) {
            g.drawCenteredString(font, Component.translatable(K + "loading"), width / 2, height / 2, PanelCharts.DIM);
        } else {
            switch (tab) {
                case 0 -> market(g, v, mx, my);
                case 1 -> commissions(g, v, mx, my);
                case 2 -> futures(g, v, mx, my);
                case 3 -> orders(g, v, mx, my);
                default -> treasury(g, v, mx, my);
            }
        }
        for (var widget : this.renderables) widget.render(g, mx, my, pt);
        if (hoverText != null) g.renderTooltip(font, Component.literal(hoverText), mx, my);
    }

    private void header(GuiGraphics g, ExchangeView v, int mx, int my) {
        g.fill(0, 0, width, 22, 0xFF161B22);
        g.drawString(font, Component.translatable(K + "title"), 8, 7, PanelCharts.ACCENT, false);
        int x = 8 + font.width(Component.translatable(K + "title")) + 12;
        for (int i = 0; i < TABS; i++) {
            Component label = Component.translatable(K + "tab." + i);
            int w = font.width(label) + 12;
            final int t = i;
            button(g, x, 4, w, 14, label, tab == i, () -> { tab = t; request(); }, mx, my);
            x += w + 3;
        }
        if (v != null) {
            String right = Component.translatable(K + "header", v.day() + 1, PanelCharts.number(v.coins()), PanelCharts.number(v.uu())).getString();
            PanelCharts.right(g, font, right, width - 8, 7, v.energy() ? PanelCharts.TEXT : PanelCharts.down());
        }
    }

    // ------------------------------------------------------------------ market tab

    private void market(GuiGraphics g, ExchangeView v, int mx, int my) {
        int lw = leftW(), rw = rightW(), top = 26, bottom = height - 6;
        // watchlist
        PanelCharts.panel(g, 6, top, lw, bottom - top);
        int rows = (bottom - top - 4) / ROW;
        listScroll = Math.min(listScroll, Math.max(0, v.markets().size() - rows));
        for (int i = 0; i < rows && i + listScroll < v.markets().size(); i++) {
            ExchangeView.Row r = v.markets().get(i + listScroll);
            int y = top + 2 + i * ROW;
            boolean sel = r.id().equals(selected);
            if (sel) g.fill(7, y, 6 + lw - 1, y + ROW - 1, 0x402F81F7);
            g.renderItem(new ItemStack(item(r.id())), 9, y + 2);
            String price = PanelCharts.number(r.price()) + (r.currency() == 1 ? "u" : "");
            g.drawString(font, font.plainSubstrByWidth(name(r.id()), lw - 70), 28, y + 2, r.locked() ? PanelCharts.DIM : PanelCharts.TEXT, false);
            g.drawString(font, price, 28, y + 11, PanelCharts.DIM, false);
            PanelCharts.right(g, font, PanelCharts.percent(r.changePermille()), 6 + lw - 4, y + 2, r.changePermille() >= 0 ? PanelCharts.up() : PanelCharts.down());
            PanelCharts.sparkline(g, 6 + lw - 34, y + 11, 30, 7, r.spark());
            if (!r.system()) g.drawString(font, "P", 6 + lw - 42, y + 2, PanelCharts.WARN, false);
            if (r.locked()) { g.pose().pushPose(); g.pose().translate(0, 0, 200); g.fill(9, y + 2, 25, y + 18, 0xA0000000); g.pose().popPose(); }
            final String id = r.id();
            regions.add(new Region(7, y, lw - 2, ROW, () -> { if (!id.equals(selected)) { selected = id; prefilled = false; request(); } }, null));
            hover(7, y, lw - 2, ROW, name(r.id()) + (r.system() ? "" : " - " + Component.translatable(K + "player_listing").getString())
                + (r.locked() ? " - " + Component.translatable(K + "locked").getString() : "")
                + " | " + Component.translatable(K + "volume", PanelCharts.number(r.volume())).getString(), mx, my);
        }
        if (selected.isEmpty() && !v.markets().isEmpty()) { selected = v.markets().get(0).id(); request(); }
        ExchangeView.Detail d = v.detail();
        int cx = 12 + lw, cw = width - lw - rw - 24;
        if (d == null || !d.id().equals(selected)) {
            PanelCharts.panel(g, cx, top, cw, bottom - top);
            return;
        }
        if (!prefilled) {
            long p = buySide ? firstPositive(d.ask(), d.asks().isEmpty() ? 0 : d.asks().get(0).price(), d.price())
                : firstPositive(d.bid(), d.bids().isEmpty() ? 0 : d.bids().get(0).price(), d.price());
            priceBox.setValue(Long.toString(p));
            prefilled = true;
        }
        // chart
        int chartH = (bottom - top) * 3 / 5;
        PanelCharts.panel(g, cx, top, cw, chartH);
        String title = name(d.id()) + "  " + PanelCharts.number(d.price()) + " " + unit(d.currency()).getString();
        g.drawString(font, font.plainSubstrByWidth(title, cw - 8), cx + 4, top + 4, PanelCharts.TEXT, false);
        String sub = Component.translatable(K + "meta", d.depth(), d.fundamental() > 0 ? PanelCharts.number(d.fundamental()) : "-",
            d.system() ? Component.translatable(K + "system_market").getString() : Component.translatable(K + "player_listing").getString()).getString();
        g.drawString(font, font.plainSubstrByWidth(sub, cw - 8), cx + 4, top + 14, PanelCharts.DIM, false);
        int n = d.bars().size();
        long[] o = new long[n], h = new long[n], l = new long[n], c = new long[n], vol = new long[n];
        for (int i = 0; i < n; i++) { var b = d.bars().get(i); o[i] = b.open(); h[i] = b.high(); l[i] = b.low(); c[i] = b.close(); vol[i] = b.volume(); }
        int gx = cx + 4, gy = top + 26, gw = cw - 8, gh = chartH - 30;
        long fundamental = d.fundamental();
        PanelCharts.candles(g, gx, gy, gw, gh, o, h, l, c, vol, 0, 0);
        long[] sc = PanelCharts.scale(l, h, 0, 0);
        double[] ma = new double[n];
        for (int i = 0; i < n; i++) {
            if (i < 4) { ma[i] = Double.NaN; continue; }
            double s = 0;
            for (int k = i - 4; k <= i; k++) s += c[k];
            ma[i] = s / 5;
        }
        PanelCharts.line(g, gx, gy, gw, gh, ma, sc[0], sc[1], PanelCharts.ACCENT, false);
        if (fundamental > 0 && fundamental >= sc[0] && fundamental <= sc[1]) {
            double[] f = new double[n];
            java.util.Arrays.fill(f, fundamental);
            PanelCharts.line(g, gx, gy, gw, gh, f, sc[0], sc[1], PanelCharts.WARN, true);
        }
        PanelCharts.right(g, font, PanelCharts.number(sc[1]), cx + cw - 4, gy, PanelCharts.DIM);
        PanelCharts.right(g, font, PanelCharts.number(sc[0]), cx + cw - 4, gy + gh * 4 / 5 - 10, PanelCharts.DIM);
        hover(gx, gy, gw, gh, Component.translatable(K + "chart_help").getString(), mx, my);
        // below the chart: quotes and trades
        int by = top + chartH + 4, bh = bottom - by;
        int half = cw / 2 - 2;
        PanelCharts.panel(g, cx, by, half, bh);
        int ty = by + 4;
        g.drawString(font, Component.translatable(K + "quotes"), cx + 4, ty, PanelCharts.ACCENT, false);
        ty += 11;
        if (d.system()) {
            line(g, cx + 4, ty, half - 8, K + "ask", d.ask() > 0 ? PanelCharts.number(d.ask()) : "-", d.ask() > 0 ? PanelCharts.down() : PanelCharts.DIM);
            line(g, cx + 4, ty + 10, half - 8, K + "supply", PanelCharts.number(d.supply()), PanelCharts.TEXT);
            line(g, cx + 4, ty + 20, half - 8, K + "bid", d.bid() > 0 ? PanelCharts.number(d.bid()) : "-", d.bid() > 0 ? PanelCharts.up() : PanelCharts.DIM);
            line(g, cx + 4, ty + 30, half - 8, K + "budget", PanelCharts.number(d.budgetLeft()), PanelCharts.TEXT);
            line(g, cx + 4, ty + 40, half - 8, K + "daily_left", PanelCharts.number(d.dailyLeft()), PanelCharts.TEXT);
        } else {
            g.drawString(font, font.plainSubstrByWidth(Component.translatable(K + "listing_only_book").getString(), half - 8), cx + 4, ty, PanelCharts.DIM, false);
        }
        line(g, cx + 4, ty + 52, half - 8, K + "band", PanelCharts.number(d.low()) + " - " + PanelCharts.number(d.high()), PanelCharts.DIM);
        int tx = cx + half + 4;
        PanelCharts.panel(g, tx, by, cw - half - 4, bh);
        g.drawString(font, Component.translatable(K + "trades"), tx + 4, by + 4, PanelCharts.ACCENT, false);
        int row = by + 15;
        for (ExchangeView.TradeLine t : d.trades()) {
            if (row > bottom - 10) break;
            g.drawString(font, PanelCharts.number(t.price()), tx + 4, row, t.mine() ? PanelCharts.WARN : PanelCharts.TEXT, false);
            PanelCharts.right(g, font, "x" + t.quantity(), tx + cw - half - 8, row, PanelCharts.DIM);
            row += 9;
        }
        // right column: depth and ticket
        depthAndTicket(g, v, d, width - rw - 6, top, rw, bottom, mx, my);
    }

    private static long firstPositive(long... values) {
        for (long v : values) if (v > 0) return v;
        return 1;
    }

    private void line(GuiGraphics g, int x, int y, int w, String key, String value, int colour) {
        g.drawString(font, Component.translatable(key), x, y, PanelCharts.DIM, false);
        PanelCharts.right(g, font, value, x + w, y, colour);
    }

    private void depthAndTicket(GuiGraphics g, ExchangeView v, ExchangeView.Detail d, int x, int top, int w, int bottom, int mx, int my) {
        int levels = height < 300 ? 5 : 8;
        int depthH = 14 + levels * 18 + 4;
        PanelCharts.panel(g, x, top, w, depthH);
        g.drawString(font, Component.translatable(K + "depth"), x + 4, top + 4, PanelCharts.ACCENT, false);
        long maxQ = 1;
        for (var l : d.asks()) maxQ = Math.max(maxQ, l.quantity());
        for (var l : d.bids()) maxQ = Math.max(maxQ, l.quantity());
        int y = top + 14;
        for (int i = levels - 1; i >= 0; i--, y += 9) {
            if (i >= d.asks().size()) continue;
            var l = d.asks().get(i);
            PanelCharts.bar(g, x + 2, y, w - 4, 8, l.quantity() / (double) maxQ, (PanelCharts.down() & 0x00FFFFFF) | 0x50000000, true);
            g.drawString(font, PanelCharts.number(l.price()), x + 4, y, PanelCharts.down(), false);
            PanelCharts.right(g, font, PanelCharts.number(l.quantity()), x + w - 4, y, PanelCharts.TEXT);
            final long price = l.price();
            regions.add(new Region(x + 2, y, w - 4, 9, () -> { buySide = true; priceBox.setValue(Long.toString(price)); }, null));
        }
        y = top + 14 + levels * 9;
        g.fill(x + 2, y - 1, x + w - 2, y, PanelCharts.PANEL_LINE);
        for (int i = 0; i < levels; i++, y += 9) {
            if (i >= d.bids().size()) continue;
            var l = d.bids().get(i);
            PanelCharts.bar(g, x + 2, y, w - 4, 8, l.quantity() / (double) maxQ, (PanelCharts.up() & 0x00FFFFFF) | 0x50000000, true);
            g.drawString(font, PanelCharts.number(l.price()), x + 4, y, PanelCharts.up(), false);
            PanelCharts.right(g, font, PanelCharts.number(l.quantity()), x + w - 4, y, PanelCharts.TEXT);
            final long price = l.price();
            regions.add(new Region(x + 2, y, w - 4, 9, () -> { buySide = false; priceBox.setValue(Long.toString(price)); }, null));
        }
        // ticket
        int ty = top + depthH + 4;
        PanelCharts.panel(g, x, ty, w, bottom - ty);
        int half = (w - 12) / 2;
        button(g, x + 4, ty + 4, half, 14, Component.translatable(K + "buy"), buySide, () -> { buySide = true; prefilled = false; }, mx, my);
        button(g, x + 8 + half, ty + 4, half, 14, Component.translatable(K + "sell"), !buySide, () -> { buySide = false; prefilled = false; }, mx, my);
        button(g, x + 4, ty + 21, half, 14, Component.translatable(K + "now"), !resting, () -> resting = false, mx, my);
        button(g, x + 8 + half, ty + 21, half, 14, Component.translatable(K + "rest"), resting, () -> resting = true, mx, my);
        g.drawString(font, Component.translatable(K + "price"), x + 4, ty + 42, PanelCharts.DIM, false);
        place(priceBox, x + w - 4 - 70, ty + 40, 70);
        g.drawString(font, Component.translatable(K + "quantity"), x + 4, ty + 58, PanelCharts.DIM, false);
        place(qtyBox, x + w - 4 - 70, ty + 56, 70);
        button(g, x + w - 4 - 70 - 30, ty + 56, 13, 12, Component.literal("-"), false,
            () -> qtyBox.setValue(Long.toString(Math.max(1, num(qtyBox, 1) - (hasShiftDown() ? 64 : 1)))), mx, my);
        button(g, x + w - 4 - 70 - 15, ty + 56, 13, 12, Component.literal("+"), false,
            () -> qtyBox.setValue(Long.toString(Math.min(1024, num(qtyBox, 1) + (hasShiftDown() ? 64 : 1)))), mx, my);
        long price = num(priceBox, 0), qty = num(qtyBox, 0);
        line(g, x + 4, ty + 74, w - 8, K + "total", PanelCharts.number(price * qty) + " " + unit(d.currency()).getString(), PanelCharts.TEXT);
        String note = Component.translatable(resting ? K + "rest_help" : K + "now_help").getString();
        g.drawString(font, font.plainSubstrByWidth(note, w - 8), x + 4, ty + 86, PanelCharts.DIM, false);
        button(g, x + 4, ty + 98, w - 8, 16, Component.translatable(buySide ? K + "submit_buy" : K + "submit_sell"), false,
            () -> act(ExchangePackets.PLACE, d.id(), num(priceBox, 0), 0, resting ? 1 : 0, (int) Math.min(1024, num(qtyBox, 0)), buySide, ""), mx, my);
    }

    private void place(EditBox b, int x, int y, int w) {
        b.setX(x); b.setY(y); b.setWidth(w); b.setVisible(true);
    }

    // ------------------------------------------------------------------ commissions tab

    private void commissions(GuiGraphics g, ExchangeView v, int mx, int my) {
        int top = 26, bottom = height - 6, formW = Math.max(180, Math.min(220, width / 3)), listW = width - formW - 18;
        PanelCharts.panel(g, 6, top, listW, bottom - top);
        g.drawString(font, Component.translatable(K + "board"), 10, top + 4, PanelCharts.ACCENT, false);
        int rows = (bottom - top - 18) / 22;
        boardScroll = Math.min(boardScroll, Math.max(0, v.commissions().size() - rows));
        for (int i = 0; i < rows && i + boardScroll < v.commissions().size(); i++) {
            ExchangeView.CommissionLine c = v.commissions().get(i + boardScroll);
            int y = top + 16 + i * 22;
            g.fill(7, y, 6 + listW - 1, y + 21, i % 2 == 0 ? 0x10FFFFFF : 0);
            g.renderItem(new ItemStack(item(c.item())), 10, y + 2);
            String what = name(c.item()) + "  " + c.delivered() + "/" + c.quantity();
            g.drawString(font, font.plainSubstrByWidth(what, listW / 2), 30, y + 2, PanelCharts.TEXT, false);
            String who = c.system() ? Component.translatable(K + "market_request").getString() : c.requester();
            g.drawString(font, font.plainSubstrByWidth(who + " · " + Component.translatable(K + "days_left", Math.max(0, c.expiry() - v.day())).getString(),
                listW / 2), 30, y + 11, PanelCharts.DIM, false);
            int rx = 6 + listW / 2 + 10;
            if (c.kind() == 1) {
                g.renderItem(new ItemStack(item(c.rewardItem())), rx, y + 2);
                g.drawString(font, "x" + c.rewardTotal(), rx + 18, y + 6, PanelCharts.WARN, false);
            } else {
                g.drawString(font, PanelCharts.number(c.rewardTotal()) + " " + unit(c.kind() == 2 ? 1 : 0).getString(), rx, y + 6, PanelCharts.WARN, false);
            }
            final long id = c.id();
            if (c.mine()) {
                button(g, 6 + listW - 64, y + 3, 58, 15, Component.translatable(K + "withdraw"), false,
                    () -> act(ExchangePackets.WITHDRAW, "", id, 0, 0, 0, false, ""), mx, my);
            } else {
                final int left = c.quantity() - c.delivered();
                button(g, 6 + listW - 64, y + 3, 58, 15, Component.translatable(K + "deliver"), false,
                    () -> act(ExchangePackets.DELIVER, "", id, 0, 0, left, false, ""), mx, my);
            }
        }
        // post form
        int fx = width - formW - 6;
        PanelCharts.panel(g, fx, top, formW, bottom - top);
        g.drawString(font, Component.translatable(K + "post"), fx + 4, top + 4, PanelCharts.ACCENT, false);
        int y = top + 16;
        button(g, fx + 4, y, formW / 2 - 6, 18, Component.translatable(K + "pick_target"), pickMode == 0, () -> pickMode = 0, mx, my);
        if (target != Items.AIR) g.renderItem(new ItemStack(target), fx + formW / 2, y + 1);
        y += 22;
        g.drawString(font, Component.translatable(K + "quantity"), fx + 4, y + 2, PanelCharts.DIM, false);
        place(postQty, fx + formW - 64, y, 60);
        y += 16;
        for (int k = 0; k < 3; k++) {
            final int kind = k;
            button(g, fx + 4 + k * ((formW - 8) / 3), y, (formW - 8) / 3 - 2, 14, Component.translatable(K + "reward." + k), rewardKind == k,
                () -> rewardKind = kind, mx, my);
        }
        y += 18;
        if (rewardKind == 1) {
            button(g, fx + 4, y, formW / 2 - 6, 18, Component.translatable(K + "pick_reward"), pickMode == 1, () -> pickMode = 1, mx, my);
            if (rewardItem != Items.AIR) g.renderItem(new ItemStack(rewardItem), fx + formW / 2, y + 1);
            y += 22;
        }
        g.drawString(font, Component.translatable(rewardKind == 1 ? K + "reward_count" : K + "reward_amount"), fx + 4, y + 2, PanelCharts.DIM, false);
        place(postAmount, fx + formW - 64, y, 60);
        y += 16;
        g.drawString(font, Component.translatable(K + "days"), fx + 4, y + 2, PanelCharts.DIM, false);
        place(postDays, fx + formW - 64, y, 60);
        y += 16;
        String fee = rewardKind == 0 ? Component.translatable(K + "fee_coin").getString() : Component.translatable(K + "fee_flat").getString();
        g.drawString(font, font.plainSubstrByWidth(fee, formW - 8), fx + 4, y, PanelCharts.DIM, false);
        y += 11;
        button(g, fx + 4, y, formW - 8, 16, Component.translatable(K + "post_button"), false, () -> {
            if (target == Items.AIR) return;
            act(ExchangePackets.POST, BuiltInRegistries.ITEM.getKey(target).toString(), num(postAmount, 0), num(postDays, 3), rewardKind,
                (int) Math.min(1024, num(postQty, 0)), false, rewardItem == Items.AIR ? "" : BuiltInRegistries.ITEM.getKey(rewardItem).toString());
        }, mx, my);
        y += 20;
        // inventory picker
        g.drawString(font, Component.translatable(K + "inventory_pick"), fx + 4, y, PanelCharts.DIM, false);
        y += 10;
        var inv = minecraft.player.getInventory();
        int cols = Math.max(1, Math.min(9, (formW - 8) / 18));
        for (int s = 0; s < 36; s++) {
            int sx = fx + 4 + (s % cols) * 18, sy = y + (s / cols) * 18;
            if (sy + 18 > bottom - 40) break;
            g.fill(sx, sy, sx + 17, sy + 17, 0x40000000);
            ItemStack stack = inv.items.get(s);
            if (stack.isEmpty()) continue;
            g.renderItem(stack, sx, sy);
            final Item it = stack.getItem();
            regions.add(new Region(sx, sy, 17, 17, () -> { if (pickMode == 0) target = it; else rewardItem = it; }, null));
            hover(sx, sy, 17, 17, stack.getHoverName().getString(), mx, my);
        }
        // listing progress
        int cy = bottom - 36;
        g.drawString(font, Component.translatable(K + "candidates"), fx + 4, cy, PanelCharts.ACCENT, false);
        int k = 0;
        for (ExchangeView.Candidate c : v.candidates()) {
            if (k >= 2) break;
            g.drawString(font, font.plainSubstrByWidth(name(c.item()) + "  " + Component.translatable(K + "candidate", c.requesters(), c.fulfilled(),
                c.deliverers()).getString(), formW - 8), fx + 4, cy + 11 + k * 10, PanelCharts.DIM, false);
            k++;
        }
    }

    // ------------------------------------------------------------------ futures tab

    private void futures(GuiGraphics g, ExchangeView v, int mx, int my) {
        int top = 26, bottom = height - 6, w = Math.min(320, width - 12);
        int x = 6;
        PanelCharts.panel(g, x, top, w, bottom - top);
        g.drawString(font, Component.translatable(K + "futures"), x + 4, top + 4, PanelCharts.ACCENT, false);
        ExchangeView.Detail d = v.detail();
        int y = top + 18;
        if (d == null || !d.system()) {
            g.drawString(font, font.plainSubstrByWidth(Component.translatable(K + "futures_pick").getString(), w - 8), x + 4, y, PanelCharts.DIM, false);
        } else {
            g.renderItem(new ItemStack(item(d.id())), x + 4, y);
            g.drawString(font, name(d.id()) + "  " + PanelCharts.number(d.price()), x + 24, y + 4, PanelCharts.TEXT, false);
            y += 22;
            g.drawString(font, Component.translatable(K + "quantity"), x + 4, y + 2, PanelCharts.DIM, false);
            place(futQty, x + w - 70, y, 66);
            y += 18;
            long q = Math.min(1024, num(futQty, 0));
            long[][] prices = {{d.futLong3(), d.futShort3()}, {d.futLong7(), d.futShort7()}};
            int[] days = {3, 7};
            for (int i = 0; i < 2; i++) {
                final int dd = days[i];
                g.drawString(font, Component.translatable(K + "contract_days", dd), x + 4, y + 3, PanelCharts.TEXT, false);
                int bw = (w - 90) / 2;
                button(g, x + 82, y, bw, 15, Component.translatable(K + "long", PanelCharts.number(prices[i][0])), false,
                    () -> act(ExchangePackets.FUTURE, d.id(), 0, 0, dd, (int) Math.min(1024, num(futQty, 0)), true, ""), mx, my);
                button(g, x + 86 + bw, y, bw, 15, Component.translatable(K + "short", PanelCharts.number(prices[i][1])), false,
                    () -> act(ExchangePackets.FUTURE, d.id(), 0, 0, dd, (int) Math.min(1024, num(futQty, 0)), false, ""), mx, my);
                y += 18;
                g.drawString(font, Component.translatable(K + "collateral", PanelCharts.number(prices[i][0] * q), PanelCharts.number(prices[i][1] * q)),
                    x + 82, y, PanelCharts.DIM, false);
                y += 12;
            }
            y += 4;
            for (String line : font.getSplitter().splitLines(Component.translatable(K + "futures_help").getString(), w - 8,
                    net.minecraft.network.chat.Style.EMPTY).stream().map(net.minecraft.network.chat.FormattedText::getString).toList()) {
                if (y > bottom - 12) break;
                g.drawString(font, line, x + 4, y, PanelCharts.DIM, false);
                y += 9;
            }
        }
        int px = x + w + 6, pw = width - px - 6;
        if (pw < 120) return;
        PanelCharts.panel(g, px, top, pw, bottom - top);
        g.drawString(font, Component.translatable(K + "positions"), px + 4, top + 4, PanelCharts.ACCENT, false);
        int ry = top + 18;
        for (ExchangeView.PositionLine p : v.positions()) {
            g.renderItem(new ItemStack(item(p.commodity())), px + 4, ry);
            String side = Component.translatable(p.isLong() ? K + "side_long" : K + "side_short").getString();
            g.drawString(font, side + " x" + p.quantity() + " @ " + PanelCharts.number(p.price()), px + 24, ry, PanelCharts.TEXT, false);
            long collateral = p.price() * p.quantity();
            g.drawString(font, Component.translatable(K + "position_value", PanelCharts.number(p.valueNow()), Math.max(0, p.expiry() - v.day())),
                px + 24, ry + 9, p.valueNow() >= collateral ? PanelCharts.up() : PanelCharts.down(), false);
            ry += 22;
        }
    }

    // ------------------------------------------------------------------ orders and mail tab

    private void orders(GuiGraphics g, ExchangeView v, int mx, int my) {
        int top = 26, bottom = height - 6, half = (width - 18) / 2;
        PanelCharts.panel(g, 6, top, half, bottom - top);
        g.drawString(font, Component.translatable(K + "my_orders"), 10, top + 4, PanelCharts.ACCENT, false);
        int y = top + 18;
        for (ExchangeView.MyOrder o : v.orders()) {
            if (y > bottom - 18) break;
            g.renderItem(new ItemStack(item(o.commodity())), 10, y);
            String side = Component.translatable(o.buy() ? K + "buy" : K + "sell").getString();
            g.drawString(font, side + " x" + o.remaining() + " @ " + PanelCharts.number(o.price()), 30, y + 4, o.buy() ? PanelCharts.up() : PanelCharts.down(), false);
            final long id = o.id();
            button(g, 6 + half - 60, y + 1, 54, 14, Component.translatable(K + "cancel"), false,
                () -> act(ExchangePackets.CANCEL, "", id, 0, 0, 0, false, ""), mx, my);
            y += 18;
        }
        int x = 12 + half;
        PanelCharts.panel(g, x, top, half, bottom - top);
        g.drawString(font, Component.translatable(K + "mailbox"), x + 4, top + 4, PanelCharts.ACCENT, false);
        y = top + 18;
        g.drawString(font, Component.translatable(K + "mail_coins", PanelCharts.number(v.mailCoins())), x + 4, y, PanelCharts.TEXT, false);
        button(g, x + half - 70, y - 2, 64, 14, Component.translatable(K + "collect"), false,
            () -> act(ExchangePackets.COLLECT, "", 0, 0, 0, 0, false, ""), mx, my);
        y += 16;
        for (ExchangeView.MailLine m : v.mail()) {
            if (y > bottom - 50) break;
            g.renderItem(new ItemStack(item(m.item())), x + 4, y);
            g.drawString(font, name(m.item()) + " x" + PanelCharts.number(m.count()), x + 24, y + 4, PanelCharts.TEXT, false);
            y += 18;
        }
        int uy = bottom - 40;
        g.drawString(font, Component.translatable(K + "uu_account", PanelCharts.number(v.uu())), x + 4, uy, PanelCharts.ACCENT, false);
        button(g, x + 4, uy + 12, half / 2 - 6, 16, Component.translatable(K + "uu_deposit"), false,
            () -> act(ExchangePackets.UU_IN, "", 0, 0, 0, 0, false, ""), mx, my);
        button(g, x + half / 2, uy + 12, half / 2 - 6, 16, Component.translatable(K + "uu_withdraw"), false,
            () -> act(ExchangePackets.UU_OUT, "", 0, 0, 0, 0, false, ""), mx, my);
    }

    // ------------------------------------------------------------------ treasury tab

    private void treasury(GuiGraphics g, ExchangeView v, int mx, int my) {
        int top = 26, bottom = height - 6, half = (width - 18) / 2;
        PanelCharts.panel(g, 6, top, half, bottom - top);
        g.drawString(font, Component.translatable(K + "ledger"), 10, top + 4, PanelCharts.ACCENT, false);
        long max = 1, minted = 0, burned = 0;
        for (var l : v.ledger()) { max = Math.max(max, Math.max(l.minted(), l.burned())); minted += l.minted(); burned += l.burned(); }
        int gx = 12, gy = top + 18, gw = half - 12, gh = bottom - top - 50;
        int n = Math.max(1, v.ledger().size());
        double step = gw / (double) n;
        for (int i = 0; i < v.ledger().size(); i++) {
            var l = v.ledger().get(i);
            int bx = gx + (int) (step * i), bw = Math.max(2, (int) (step / 2) - 1);
            int mh = (int) (gh * l.minted() / (double) max), bh = (int) (gh * l.burned() / (double) max);
            g.fill(bx, gy + gh - mh, bx + bw, gy + gh, PanelCharts.up());
            g.fill(bx + bw + 1, gy + gh - bh, bx + 2 * bw + 1, gy + gh, PanelCharts.down());
            hover(bx, gy, 2 * bw + 2, gh, Component.translatable(K + "ledger_day", l.day() + 1, PanelCharts.number(l.minted()), PanelCharts.number(l.burned())).getString(), mx, my);
        }
        g.drawString(font, Component.translatable(K + "ledger_total", PanelCharts.number(minted), PanelCharts.number(burned), PanelCharts.number(minted - burned)),
            10, bottom - 26, PanelCharts.TEXT, false);
        g.drawString(font, font.plainSubstrByWidth(Component.translatable(K + "ledger_help").getString(), half - 8), 10, bottom - 14, PanelCharts.DIM, false);
        int x = 12 + half;
        PanelCharts.panel(g, x, top, half, bottom - top);
        g.drawString(font, Component.translatable(K + "stock"), x + 4, top + 4, PanelCharts.ACCENT, false);
        int y = top + 18;
        for (ExchangeView.StockLine s : v.stocks()) {
            if (y > bottom - 16) break;
            g.renderItem(new ItemStack(item(s.item())), x + 4, y);
            g.drawString(font, name(s.item()) + " x" + PanelCharts.number(s.stock()), x + 24, y + 4, PanelCharts.TEXT, false);
            y += 18;
        }
        long listings = v.markets().stream().filter(r -> !r.system()).count();
        g.drawString(font, Component.translatable(K + "listings", listings), x + 4, bottom - 14, PanelCharts.DIM, false);
    }
}
