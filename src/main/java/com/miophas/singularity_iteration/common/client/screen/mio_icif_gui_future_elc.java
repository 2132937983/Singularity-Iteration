package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.client.screen.panel.PanelCharts;
import com.miophas.singularity_iteration.common.exchange.ExchangeClientState;
import com.miophas.singularity_iteration.common.exchange.ExchangePackets;
import com.miophas.singularity_iteration.common.exchange.ExchangeService;
import com.miophas.singularity_iteration.common.exchange.ExchangeView;
import com.miophas.singularity_iteration.common.exchange.MinPrice;
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
import net.minecraft.world.inventory.Slot;
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
 *   <li><b>Bounties</b> (0.1.10.26): the system bounties of three tiers (needs, rewards, days left, the player's share),
 *       deliver from the inventory, the weekly budgets of issued rewards and the vault.</li>
 *   <li><b>Settlement</b> (0.1.10.27): a container in the settlement slot (shulker box, backpack, AE2 cell, UU-matter
 *       container): its quote, sell now or into the auction, deliver to bounties and commissions, UU-matter onto the account,
 *       buy the selected market into it.</li>
 * </ul>
 * The panel shows the {@link ExchangeView} that the server sends (on request, once per second while open); every action is a
 * packet that the server checks again. Before 0.1.10.21 the screen was a price chart with buy and sell buttons.
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_future_elc extends mio_icif_screen<FutureElcMenu> {
    private static final int TABS = 8, ROW = 20;   // 0.1.10.25: tab 5 = the sell page; 0.1.10.26: tab 6 = bounties; 0.1.10.27: tab 7 = settlement
    private static final String K = "gui.mio_icif.exchange.";

    private record Region(int x, int y, int w, int h, Runnable left, Runnable right) {
        boolean inside(double mx, double my) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    }

    private final List<Region> regions = new ArrayList<>();
    private int tab;
    private String selected = "";
    private boolean prefilled;
    private int listScroll, boardScroll, bountyScroll;
    private boolean buySide = true, resting;
    private int requestTimer;
    private String hoverText;
    // commission form
    private Item target = Items.AIR, rewardItem = Items.AIR;
    private int rewardKind, pickMode;

    // 0.1.10.29: number boxes (NumberBox); coinBox = coins to deposit / withdraw (0 = all)
    private NumberBox priceBox, qtyBox, postQty, postAmount, postDays, futQty, minBox, coinBox;
    /** 0.1.10.29: the minimum of the sell and settlement pages is in coins (else percent of the bid without pressure). */
    private boolean minCoins;
    /** 0.1.10.29: the ticket's limit of a sale is a percent of the bid without pressure. */
    private boolean pricePercent;
    /** 0.1.10.29: a quote request waits for the next free tick (box and container changes come in bursts). */
    private boolean requestPending;
    // 0.1.10.25 (X1): the sell box as last requested (a change asks the server for a new quote)
    private String boxSeen = "";
    // 0.1.10.27 (X2): the settlement slot as last requested
    private String settleSeen = "";

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
        // 0.1.10.29: the ranges of the actions; a new init (window resized) keeps what was typed
        priceBox = box(priceBox, pricePercent ? 0 : 1, pricePercent ? MinPrice.MAX_PERCENT : MAX_COINS, 1);
        qtyBox = box(qtyBox, 1, ExchangeService.MAX_QUANTITY, 1);
        postQty = box(postQty, 1, ExchangeService.MAX_QUANTITY, 64);
        postAmount = box(postAmount, 1, MAX_COINS, 100);
        postDays = box(postDays, 1, ExchangeService.COMMISSION_MAX_DAYS, 3);
        futQty = box(futQty, 1, ExchangeService.FUTURES_MAX_QUANTITY, 16);
        minBox = box(minBox, 0, minCoins ? MAX_COINS : MinPrice.MAX_PERCENT, 0);
        coinBox = box(coinBox, 0, MAX_COINS, 0);
        request();
    }

    private static final long MAX_COINS = 999_999_999L;

    private NumberBox box(NumberBox old, long min, long max, long value) {
        NumberBox b = new NumberBox(this.font, min, max, old == null ? value : old.value());
        addRenderableWidget(b);
        return b;
    }

    private List<NumberBox> boxes() { return List.of(priceBox, qtyBox, postQty, postAmount, postDays, futQty, minBox, coinBox); }

    /** The number of a box (0.1.10.29: clamped to its range), {@code fallback} when it is empty. */
    private static long num(EditBox b, long fallback) {
        if (b instanceof NumberBox n) return n.getValue().isEmpty() ? fallback : n.value();
        try { return b.getValue().isEmpty() ? fallback : Long.parseLong(b.getValue()); } catch (NumberFormatException e) { return fallback; }
    }

    /** 0.1.10.29: a +/- button: steps the box and gives it the focus with the number selected (typing replaces it). */
    private void stepBox(NumberBox b, int steps) {
        b.step(steps);
        setFocused(b);
        b.selectAll();
    }

    // ------------------------------------------------------------------ network

    private void request() {
        PacketDistributor.sendToServer(new ExchangePackets.Request(selected));
        requestTimer = 0;
        requestPending = false;
    }

    private void act(int kind, String id, long a, long b, int c, int d, boolean flag, String text) {
        PacketDistributor.sendToServer(new ExchangePackets.Action(kind, id, a, b, c, d, flag, text, selected));
    }

    @Override
    public void containerTick() {
        super.containerTick();
        // 0.1.10.25: a new quote as soon as the sell box changes
        if (tab == 5) {
            StringBuilder seen = new StringBuilder();
            for (int i = 0; i < FutureElcMenu.SELL_BOX_SIZE; i++) {
                ItemStack s = menu.sellBox().getItem(i);
                if (!s.isEmpty()) seen.append(i).append(':').append(BuiltInRegistries.ITEM.getKey(s.getItem())).append('x').append(s.getCount()).append(';');
            }
            if (!seen.toString().equals(boxSeen)) { boxSeen = seen.toString(); requestPending = true; }
        }
        if (tab == 7) {
            ItemStack s = menu.slots.get(FutureElcMenu.SETTLEMENT_SLOT).getItem();
            String seen = s.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(s.getItem()) + "#" + s.getComponents().hashCode();
            if (!seen.equals(settleSeen)) { settleSeen = seen; requestPending = true; }
        }
        // 0.1.10.29: a change asks at most every 4 ticks (a drag over the box changed it every tick, a request each)
        ++requestTimer;
        if (requestPending && requestTimer >= 4 || requestTimer >= 20) request();
    }

    @Override
    public void removed() {
        super.removed();
        ExchangeClientState.clear();
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for (EditBox b : boxes()) {
            if (b.isVisible() && b.mouseClicked(mx, my, button)) { setFocused(b); return true; }
        }
        if (getFocused() instanceof EditBox b) { b.setFocused(false); setFocused(null); }
        for (int i = regions.size() - 1; i >= 0; i--) {
            Region r = regions.get(i);
            if (!r.inside(mx, my)) continue;
            Runnable action = button == 1 ? r.right() : r.left();
            if (action != null) { action.run(); return true; }
        }
        // 0.1.10.25: the slots of the sell page (vanilla clicks, drags, shift-clicks); 0.1.10.27: and of the settlement page
        return (tab == 5 || tab == 7) && super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (getFocused() instanceof EditBox b && b.isVisible() && key != GLFW.GLFW_KEY_ESCAPE) return b.keyPressed(key, scan, modifiers) || true;
        return super.keyPressed(key, scan, modifiers);
    }

    /** 0.1.10.29: typed characters go to the focused box and stop there. */
    @Override
    public boolean charTyped(char c, int modifiers) {
        if (getFocused() instanceof EditBox b && b.isVisible()) return b.charTyped(c, modifiers) || true;
        return super.charTyped(c, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        // 0.1.10.29: the wheel over a number box steps it
        for (NumberBox b : boxes()) if (b.mouseScrolled(mx, my, sx, sy)) return true;
        if (tab == 0 && mx < leftW() + 6) { listScroll = Math.max(0, listScroll - (int) Math.signum(sy)); return true; }
        if (tab == 1) { boardScroll = Math.max(0, boardScroll - (int) Math.signum(sy)); return true; }
        if (tab == 6) { bountyScroll = Math.max(0, bountyScroll - (int) Math.signum(sy)); return true; }
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
        // 0.1.10.28 (X4): the coin <-> UU board shows the UU-matter bucket
        if (com.miophas.singularity_iteration.common.exchange.UuBoard.is(id)) return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "uumatter_bucket"));
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl == null || !BuiltInRegistries.ITEM.containsKey(rl) ? Items.BARRIER : BuiltInRegistries.ITEM.get(rl);
    }

    private static String name(String id) {
        if (com.miophas.singularity_iteration.common.exchange.UuBoard.is(id)) return Component.translatable(K + "uu_board").getString();
        return new ItemStack(item(id)).getHoverName().getString();
    }

    // 0.1.10.28 (X4): the scrap sink of the instant sales (sell page, settlement page)
    private boolean scrapRest;

    private Component unit(int currency) { return Component.translatable(currency == 1 ? K + "uu" : K + "coins"); }

    private void hover(int x, int y, int w, int h, String text, int mx, int my) {
        if (mx >= x && mx < x + w && my >= y && my < y + h) hoverText = text;
    }

    // ------------------------------------------------------------------ render

    /**
     * 0.1.10.25: the whole panel is drawn as the background, so the vanilla screen draws the widgets, the slots of the sell
     * page, the carried stack and the item tooltips on top of it (before, the panel drew everything itself and had no slots).
     */
    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mx, int my) {
        g.fill(0, 0, width, height, 0xE00D1117);
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
                case 4 -> treasury(g, v, mx, my);
                case 5 -> sell(g, v, mx, my);
                case 7 -> settlement(g, v, mx, my);
                default -> bounties(g, v, mx, my);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) { }

    @Override
    protected int statusLampColor() { return 0; }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        regions.clear();
        hoverText = null;
        for (EditBox b : boxes()) b.setVisible(false);
        menu.sellView = tab == 5;
        menu.settleView = tab == 7;
        layoutSellSlots();
        super.render(g, mx, my, pt);
        if (hoverText != null) g.renderTooltip(font, Component.literal(hoverText), mx, my);
    }

    /** 0.1.10.27: changes the page; the server learns whether the settlement page is shown (shift-clicks go to its slot). */
    private void page(int t) {
        boolean was = tab == 7;
        tab = t;
        if (t != 0) pricePercent(false);
        if (was != (t == 7) && minecraft != null && minecraft.gameMode != null)
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, t == 7 ? FutureElcMenu.BUTTON_SETTLE_PAGE : FutureElcMenu.BUTTON_OTHER_PAGE);
    }

    private void header(GuiGraphics g, ExchangeView v, int mx, int my) {
        g.fill(0, 0, width, 22, 0xFF161B22);
        g.drawString(font, Component.translatable(K + "title"), 8, 7, PanelCharts.ACCENT, false);
        int x = 8 + font.width(Component.translatable(K + "title")) + 12;
        for (int i = 0; i < TABS; i++) {
            Component label = Component.translatable(K + "tab." + i);
            int w = font.width(label) + 12;
            final int t = i;
            button(g, x, 4, w, 14, label, tab == i, () -> { page(t); request(); }, mx, my);
            x += w + 3;
        }
        if (v != null) {
            // 0.1.10.29: the coins of the inventory and of the coin account (B = bank mode)
            String right = Component.translatable(K + "header_account", v.day() + 1, PanelCharts.number(v.coins()), PanelCharts.number(v.mailCoins())
                + (v.bankMode() ? " B" : ""), PanelCharts.number(v.uu())).getString();
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
        if (pricePercent && (buySide || !d.system() || curveOf(v, d.id()) == null)) pricePercent(false);
        if (!prefilled && !pricePercent) {
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
            // 0.1.10.25 (X1): the sell pressure and this player's sales today (or the buy room when there is a buy limit)
            line(g, cx + 4, ty + 30, half - 8, K + "pressure", PanelCharts.number(d.pressure()), PanelCharts.TEXT);
            if (d.buyLeft() >= 0) line(g, cx + 4, ty + 40, half - 8, K + "buy_left", PanelCharts.number(d.buyLeft()), PanelCharts.TEXT);
            else line(g, cx + 4, ty + 40, half - 8, K + "sold_today", PanelCharts.number(d.mine()), PanelCharts.TEXT);
            hover(cx + 4, ty + 20, half - 8, 30, Component.translatable(K + "sell_curve_help").getString(), mx, my);
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
            regions.add(new Region(x + 2, y, w - 4, 9, () -> { buySide = true; pricePercent(false); priceBox.set(price); }, null));
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
            regions.add(new Region(x + 2, y, w - 4, 9, () -> { buySide = false; pricePercent(false); priceBox.set(price); }, null));
        }
        // ticket
        int ty = top + depthH + 4;
        PanelCharts.panel(g, x, ty, w, bottom - ty);
        int half = (w - 12) / 2;
        button(g, x + 4, ty + 4, half, 14, Component.translatable(K + "buy"), buySide, () -> { buySide = true; prefilled = false; }, mx, my);
        button(g, x + 8 + half, ty + 4, half, 14, Component.translatable(K + "sell"), !buySide, () -> { buySide = false; prefilled = false; }, mx, my);
        button(g, x + 4, ty + 21, half, 14, Component.translatable(K + "now"), !resting, () -> resting = false, mx, my);
        button(g, x + 8 + half, ty + 21, half, 14, Component.translatable(K + "rest"), resting, () -> resting = true, mx, my);
        g.drawString(font, font.plainSubstrByWidth(Component.translatable(pricePercent ? K + "price_percent" : K + "price").getString(), Math.max(10, w - 4 - 70 - 15 - 10)),
            x + 4, ty + 42, PanelCharts.DIM, false);
        place(priceBox, x + w - 4 - 70, ty + 40, 70);
        // 0.1.10.29: a sale on the market may name its limit in percent of the bid without pressure
        ExchangeView.CurveLine curve = curveOf(v, d.id());
        if (!buySide && d.system() && curve != null) {
            button(g, x + w - 4 - 70 - 15, ty + 40, 13, 12, Component.literal(pricePercent ? "%" : "c"), pricePercent,
                () -> pricePercent(!pricePercent), mx, my);
            hover(x + w - 4 - 70 - 15, ty + 40, 13, 12, Component.translatable(K + "percent_toggle_help").getString(), mx, my);
        }
        g.drawString(font, Component.translatable(K + "quantity"), x + 4, ty + 58, PanelCharts.DIM, false);
        place(qtyBox, x + w - 4 - 70, ty + 56, 70);
        button(g, x + w - 4 - 70 - 30, ty + 56, 13, 12, Component.literal("-"), false, () -> stepBox(qtyBox, -1), mx, my);
        button(g, x + w - 4 - 70 - 15, ty + 56, 13, 12, Component.literal("+"), false, () -> stepBox(qtyBox, 1), mx, my);
        long price = ticketLimit(v, d), qty = num(qtyBox, 0);
        line(g, x + 4, ty + 74, w - 8, K + "total", PanelCharts.number(price * qty) + " " + unit(d.currency()).getString(), PanelCharts.TEXT);
        if (com.miophas.singularity_iteration.common.exchange.UuBoard.is(d.id())) {
            // 0.1.10.28 (X4): one unit of the board is a lot of 100 mB of the UU account
            String hint = Component.translatable(K + "uu_lot", PanelCharts.number(qty * com.miophas.singularity_iteration.common.exchange.UuBoard.LOT_MB),
                PanelCharts.number(v.uu())).getString();
            g.drawString(font, font.plainSubstrByWidth(hint, w - 8), x + 4, ty + 64 + 1, PanelCharts.WARN, false);
            hover(x + 4, ty + 64, w - 8, 10, Component.translatable(K + "uu_board_help").getString(), mx, my);
        }
        String note = Component.translatable(resting ? K + "rest_help" : K + "now_help").getString();
        if (!buySide && d.system() && curve != null) {
            // 0.1.10.29: what the market part of this sale pays now (every item at the limit or above, one payout)
            MinPrice.Quote q = MinPrice.quote(curve.curve(), curve.mine(), curve.pressure(), price, qty, v.allowance() < 0 ? Long.MAX_VALUE : v.allowance());
            String est = Component.translatable(K + "ticket_estimate", PanelCharts.number(q.quantity()), PanelCharts.number(qty),
                PanelCharts.number(q.coins()), PanelCharts.number(q.quantity() > 0 ? q.coins() / q.quantity() : 0)).getString();
            g.drawString(font, font.plainSubstrByWidth(est, w - 8), x + 4, ty + 86, q.quantity() < qty ? PanelCharts.WARN : PanelCharts.up(), false);
            hover(x + 4, ty + 84, w - 8, 11, note + " | " + Component.translatable(K + "ticket_estimate_help", PanelCharts.number(price),
                PanelCharts.number(Math.round(q.last())), PanelCharts.number(Math.round(q.next()))).getString(), mx, my);
        } else {
            g.drawString(font, font.plainSubstrByWidth(note, w - 8), x + 4, ty + 86, PanelCharts.DIM, false);
        }
        button(g, x + 4, ty + 98, w - 8, 16, Component.translatable(buySide ? K + "submit_buy" : K + "submit_sell"), false,
            () -> act(ExchangePackets.PLACE, d.id(), ticketLimit(v, d), 0, resting ? 1 : 0, (int) Math.min(ExchangeService.MAX_QUANTITY, num(qtyBox, 0)), buySide, ""), mx, my);
    }

    /** 0.1.10.29: the curve of a commodity in the view, or null. */
    private static ExchangeView.CurveLine curveOf(ExchangeView v, String id) {
        return v == null ? null : ExchangeView.CurveLine.find(v.curves(), id);
    }

    /** 0.1.10.29: the ticket's limit in coins (a percent limit turned into coins with the curve now). */
    private long ticketLimit(ExchangeView v, ExchangeView.Detail d) {
        ExchangeView.CurveLine c = curveOf(v, d.id());
        if (pricePercent && c != null) return MinPrice.limit(c.unit(), true, priceBox.value());
        return num(priceBox, 0);
    }

    /** 0.1.10.29: switches the ticket's limit between coins and percent (90 % to start; back in coins the price is filled in again). */
    private void pricePercent(boolean on) {
        if (on == pricePercent || priceBox == null) return;
        pricePercent = on;
        if (on) { priceBox.setRange(0, MinPrice.MAX_PERCENT); priceBox.set(90); }
        else { priceBox.setRange(1, MAX_COINS); prefilled = false; }
    }

    /**
     * 0.1.10.29: the minimum row of the sell and settlement pages: the box (coins or percent), the unit toggle and the scrap toggle;
     * returns the minimum as the action fields {coins (a), percent (c)}.
     */
    private long[] minRow(GuiGraphics g, int x, int y, int w, int mx, int my) {
        int boxX = x + w - 4 - 56, toggleX = boxX - 2 - 14, scrapX = toggleX - 6 - 96;
        g.drawString(font, font.plainSubstrByWidth(Component.translatable(minCoins ? K + "box.min_coins" : K + "box.min").getString(), Math.max(10, scrapX - x - 6)),
            x + 4, y + 2, PanelCharts.DIM, false);
        place(minBox, boxX, y, 56);
        button(g, toggleX, y, 14, 13, Component.literal(minCoins ? "c" : "%"), minCoins, () -> {
            minCoins = !minCoins;
            minBox.setRange(0, minCoins ? MAX_COINS : MinPrice.MAX_PERCENT);
            minBox.set(0);
        }, mx, my);
        hover(toggleX, y, 14, 13, Component.translatable(K + "box.min_toggle_help").getString(), mx, my);
        hover(boxX, y, 56, 13, Component.translatable(K + "box.min_help").getString(), mx, my);
        scrapToggle(g, scrapX, y, mx, my);
        long value = num(minBox, 0);
        return minCoins ? new long[] {value, 0} : new long[] {0, Math.min(MinPrice.MAX_PERCENT, value)};
    }

    /**
     * 0.1.10.29: the instant sale of a quote line at the page's minimum, from the line's curve (the allowance as the earlier lines
     * leave it, in the order the server sells them); the server's own quote (limit 1) when the view has no curve.
     */
    private MinPrice.Quote nowQuote(ExchangeView v, ExchangeView.BoxLine l, long[] min, long[] allowance) {
        ExchangeView.CurveLine c = curveOf(v, l.id());
        if (c == null) return new MinPrice.Quote(l.nowQuantity(), l.nowCoins(), 0, 0);
        long limit = min[0] > 0 ? min[0] : MinPrice.limit(c.unit(), true, min[1]);
        MinPrice.Quote q = MinPrice.quote(c.curve(), c.mine(), c.pressure(), limit, l.quantity(), allowance[0]);
        if (allowance[0] != Long.MAX_VALUE) allowance[0] = Math.max(0, allowance[0] - q.coins());
        return q;
    }

    private void place(EditBox b, int x, int y, int w) {
        b.setX(x); b.setY(y); b.setWidth(w); b.setVisible(true);
    }

    // ------------------------------------------------------------------ sell page (0.1.10.25, X1)

    private static final int SELL_TOP = 26, BOX_LEFT = 12;

    private int boxTop() { return SELL_TOP + 16; }

    private int invTop() { return boxTop() + 2 * 18 + 4 + 14 + 16; }

    /**
     * Places the sell box and inventory slots for the window (the slot positions are mutable through the mod's access
     * transformer: the panel is the whole window and changes with it). Off the sell page they are inactive.
     */
    private void layoutSellSlots() {
        for (int i = 0; i < FutureElcMenu.SELL_BOX_SIZE; i++) {
            Slot slot = menu.slots.get(FutureElcMenu.SELL_BOX_FIRST + i);
            slot.x = BOX_LEFT + (i % 9) * 18;
            slot.y = boxTop() + (i / 9) * 18;
        }
        for (int i = 0; i < 36; i++) {
            Slot slot = menu.slots.get(FutureElcMenu.INVENTORY_FIRST + i);
            slot.x = BOX_LEFT + (i % 9) * 18;
            slot.y = i < 27 ? invTop() + (i / 9) * 18 : invTop() + 3 * 18 + 4;
        }
        Slot settle = menu.slots.get(FutureElcMenu.SETTLEMENT_SLOT);
        settle.x = BOX_LEFT;
        settle.y = boxTop();
    }

    private void slotFrame(GuiGraphics g, int x, int y) {
        g.fill(x - 1, y - 1, x + 17, y + 17, PanelCharts.PANEL_LINE);
        g.fill(x, y, x + 16, y + 16, 0xFF0D1117);
    }

    private void sell(GuiGraphics g, ExchangeView v, int mx, int my) {
        int top = SELL_TOP, bottom = height - 6, lw = 9 * 18 + 12;
        // left: the box and the inventory (the slots themselves are drawn by the screen on top)
        PanelCharts.panel(g, 6, top, lw, bottom - top);
        g.drawString(font, Component.translatable(K + "box.title"), BOX_LEFT, top + 4, PanelCharts.ACCENT, false);
        for (int i = 0; i < FutureElcMenu.SELL_BOX_SIZE; i++) slotFrame(g, BOX_LEFT + (i % 9) * 18, boxTop() + (i / 9) * 18);
        int by = boxTop() + 2 * 18 + 4, half = (lw - 16) / 2;
        button(g, BOX_LEFT, by, half, 14, Component.translatable(K + "box.fill"), false,
            () -> act(ExchangePackets.BOX_FILL, "", 0, 0, 0, 0, false, ""), mx, my);
        hover(BOX_LEFT, by, half, 14, Component.translatable(K + "box.fill_help").getString(), mx, my);
        button(g, BOX_LEFT + half + 4, by, half, 14, Component.translatable(K + "box.return"), false,
            () -> act(ExchangePackets.BOX_RETURN, "", 0, 0, 0, 0, false, ""), mx, my);
        g.drawString(font, Component.translatable(K + "box.inventory"), BOX_LEFT, invTop() - 11, PanelCharts.DIM, false);
        for (int i = 0; i < 36; i++) slotFrame(g, BOX_LEFT + (i % 9) * 18, i < 27 ? invTop() + (i / 9) * 18 : invTop() + 3 * 18 + 4);

        // right: the quote, the actions and the auction orders
        int x = 6 + lw + 6, w = width - x - 6;
        int quoteH = Math.max(120, (bottom - top) * 3 / 5);
        PanelCharts.panel(g, x, top, w, quoteH);
        g.drawString(font, Component.translatable(K + "box.quote"), x + 4, top + 4, PanelCharts.ACCENT, false);
        int cQty = x + w * 45 / 100, cNow = x + w * 70 / 100, cAuc = x + w - 6;
        PanelCharts.right(g, font, Component.translatable(K + "box.qty").getString(), cQty, top + 16, PanelCharts.DIM);
        PanelCharts.right(g, font, Component.translatable(K + "box.now").getString(), cNow, top + 16, PanelCharts.DIM);
        PanelCharts.right(g, font, Component.translatable(K + "box.auction").getString(), cAuc, top + 16, PanelCharts.DIM);
        int y = top + 27;
        long nowCoins = 0, aucCoins = 0;
        long[] min = minimum();
        long[] allowance = {v.allowance() < 0 ? Long.MAX_VALUE : v.allowance()};
        if (v.box().isEmpty()) {
            for (var line : font.split(Component.translatable(K + "box.empty"), w - 8)) {
                g.drawString(font, line, x + 4, y, PanelCharts.DIM, false);
                y += 10;
            }
        }
        for (ExchangeView.BoxLine l : v.box()) {
            MinPrice.Quote q = nowQuote(v, l, min, allowance);
            if (y > top + quoteH - 60) break;
            g.renderItem(new ItemStack(item(l.id())), x + 4, y - 4);
            g.drawString(font, font.plainSubstrByWidth(name(l.id()), Math.max(20, cQty - x - 60)), x + 24, y, PanelCharts.TEXT, false);
            PanelCharts.right(g, font, PanelCharts.number(l.quantity()), cQty, y, PanelCharts.TEXT);
            String now = (q.quantity() < l.quantity() ? PanelCharts.number(q.quantity()) + ": " : "") + PanelCharts.number(q.coins());
            String auc = (l.auctionQuantity() < l.quantity() ? PanelCharts.number(l.auctionQuantity()) + ": " : "") + PanelCharts.number(l.auctionCoins());
            PanelCharts.right(g, font, now, cNow, y, q.quantity() < l.quantity() ? PanelCharts.WARN : PanelCharts.up());
            PanelCharts.right(g, font, auc, cAuc, y, l.auctionQuantity() < l.quantity() ? PanelCharts.WARN : PanelCharts.up());
            hover(x + 2, y - 4, w - 4, 16, Component.translatable(K + "box.line_help", PanelCharts.number(l.quantity()), PanelCharts.number(q.quantity()),
                PanelCharts.number(q.coins()), PanelCharts.number(l.auctionQuantity()), PanelCharts.number(l.auctionCoins())).getString()
                + " | " + Component.translatable(K + "box.item_prices", PanelCharts.number(Math.round(q.last())), PanelCharts.number(Math.round(q.next()))).getString(), mx, my);
            nowCoins += q.coins();
            aucCoins += l.auctionCoins();
            y += 18;
        }
        int fy = top + quoteH - 56;
        g.fill(x + 2, fy - 3, x + w - 2, fy - 2, PanelCharts.PANEL_LINE);
        g.drawString(font, Component.translatable(K + "box.total"), x + 4, fy, PanelCharts.DIM, false);
        PanelCharts.right(g, font, PanelCharts.number(nowCoins), cNow, fy, PanelCharts.TEXT);
        PanelCharts.right(g, font, PanelCharts.number(aucCoins), cAuc, fy, PanelCharts.TEXT);
        line(g, x + 4, fy + 11, w - 8, K + "box.allowance",
            v.allowance() < 0 ? Component.translatable(K + "box.unlimited").getString() : PanelCharts.number(v.allowance()), PanelCharts.TEXT);
        long[] mr = minRow(g, x, fy + 22, w, mx, my);
        int bw = (w - 12) / 2;
        button(g, x + 4, fy + 38, bw, 16, Component.translatable(K + "box.sell_now"), false,
            () -> act(ExchangePackets.BOX_SELL, "", mr[0], scrapRest ? 1 : 0, (int) mr[1], 0, false, ""), mx, my);
        hover(x + 4, fy + 38, bw, 16, Component.translatable(K + "box.sell_now_help").getString(), mx, my);
        button(g, x + 8 + bw, fy + 38, bw, 16, Component.translatable(K + "box.sell_auction"), false,
            () -> act(ExchangePackets.BOX_SELL, "", mr[0], 0, (int) mr[1], 0, true, ""), mx, my);
        hover(x + 8 + bw, fy + 38, bw, 16, Component.translatable(K + "box.sell_auction_help").getString(), mx, my);

        int ay = top + quoteH + 4;
        PanelCharts.panel(g, x, ay, w, bottom - ay);
        g.drawString(font, Component.translatable(K + "auction.title"), x + 4, ay + 4, PanelCharts.ACCENT, false);
        int row = ay + 16;
        if (v.auctions().isEmpty()) g.drawString(font, Component.translatable(K + "auction.none"), x + 4, row, PanelCharts.DIM, false);
        for (ExchangeView.AuctionLine a : v.auctions()) {
            if (row > bottom - 12) break;
            String text = Component.translatable(K + "auction.line", name(a.item()), PanelCharts.number(a.quantity()), a.minPercent(),
                PanelCharts.number(a.estQuantity()), PanelCharts.number(a.estCoins())).getString();
            g.drawString(font, font.plainSubstrByWidth(text, w - 60), x + 4, row, PanelCharts.TEXT, false);
            final long id = a.id();
            button(g, x + w - 52, row - 2, 48, 12, Component.translatable(K + "auction.cancel"), false,
                () -> act(ExchangePackets.AUCTION_CANCEL, "", id, 0, 0, 0, false, ""), mx, my);
            row += 13;
        }
    }

    /** 0.1.10.29: the page's minimum as {coins, percent} (one of them 0). */
    private long[] minimum() {
        long value = num(minBox, 0);
        return minCoins ? new long[] {value, 0} : new long[] {0, Math.min(MinPrice.MAX_PERCENT, value)};
    }

    /** 0.1.10.28 (X4): the scrap sink toggle of an instant sale. */
    private void scrapToggle(GuiGraphics g, int x, int y, int mx, int my) {
        button(g, x, y, 96, 13, Component.translatable(K + (scrapRest ? "scrap_on" : "scrap_off")), scrapRest, () -> scrapRest = !scrapRest, mx, my);
        hover(x, y, 96, 13, Component.translatable(K + "scrap_help").getString(), mx, my);
    }

    // ------------------------------------------------------------------ settlement page (0.1.10.27, X2)

    private void settlement(GuiGraphics g, ExchangeView v, int mx, int my) {
        int top = SELL_TOP, bottom = height - 6, lw = 9 * 18 + 12;
        ExchangeView.SettleView st = v.settle();
        PanelCharts.panel(g, 6, top, lw, bottom - top);
        g.drawString(font, Component.translatable(K + "settle.title"), BOX_LEFT, top + 4, PanelCharts.ACCENT, false);
        slotFrame(g, BOX_LEFT, boxTop());
        Component kind = Component.translatable(K + "settle.kind." + Math.max(0, Math.min(3, st.kind())));
        g.drawString(font, font.plainSubstrByWidth(kind.getString(), lw - 34), BOX_LEFT + 22, boxTop() + 4, st.kind() == 0 ? PanelCharts.DIM : PanelCharts.TEXT, false);
        int hy = boxTop() + 22;
        for (var line : font.split(Component.translatable(K + "settle.help"), lw - 12)) {
            if (hy > invTop() - 14) break;
            g.drawString(font, line, BOX_LEFT, hy, PanelCharts.DIM, false);
            hy += 10;
        }
        g.drawString(font, Component.translatable(K + "box.inventory"), BOX_LEFT, invTop() - 11, PanelCharts.DIM, false);
        for (int i = 0; i < 36; i++) slotFrame(g, BOX_LEFT + (i % 9) * 18, i < 27 ? invTop() + (i / 9) * 18 : invTop() + 3 * 18 + 4);

        int x = 6 + lw + 6, w = width - x - 6;
        int quoteH = Math.max(120, (bottom - top) / 2);
        PanelCharts.panel(g, x, top, w, quoteH);
        g.drawString(font, Component.translatable(K + "settle.quote"), x + 4, top + 4, PanelCharts.ACCENT, false);
        int cQty = x + w * 45 / 100, cNow = x + w * 70 / 100, cAuc = x + w - 6;
        PanelCharts.right(g, font, Component.translatable(K + "box.qty").getString(), cQty, top + 16, PanelCharts.DIM);
        PanelCharts.right(g, font, Component.translatable(K + "box.now").getString(), cNow, top + 16, PanelCharts.DIM);
        PanelCharts.right(g, font, Component.translatable(K + "box.auction").getString(), cAuc, top + 16, PanelCharts.DIM);
        int y = top + 27;
        long nowCoins = 0, aucCoins = 0;
        long[] min = minimum();
        long[] allowance = {v.allowance() < 0 ? Long.MAX_VALUE : v.allowance()};
        if (st.lines().isEmpty())
            g.drawString(font, Component.translatable(st.kind() == 0 ? K + "settle.empty" : K + "settle.no_commodity"), x + 4, y, PanelCharts.DIM, false);
        for (ExchangeView.BoxLine l : st.lines()) {
            MinPrice.Quote q = nowQuote(v, l, min, allowance);
            nowCoins += q.coins();
            aucCoins += l.auctionCoins();
            if (y > top + quoteH - 60) continue;
            g.renderItem(new ItemStack(item(l.id())), x + 4, y - 4);
            g.drawString(font, font.plainSubstrByWidth(name(l.id()), Math.max(20, cQty - x - 60)), x + 24, y, PanelCharts.TEXT, false);
            PanelCharts.right(g, font, PanelCharts.number(l.quantity()), cQty, y, PanelCharts.TEXT);
            String now = (q.quantity() < l.quantity() ? PanelCharts.number(q.quantity()) + ": " : "") + PanelCharts.number(q.coins());
            String auc = (l.auctionQuantity() < l.quantity() ? PanelCharts.number(l.auctionQuantity()) + ": " : "") + PanelCharts.number(l.auctionCoins());
            PanelCharts.right(g, font, now, cNow, y, q.quantity() < l.quantity() ? PanelCharts.WARN : PanelCharts.up());
            PanelCharts.right(g, font, auc, cAuc, y, l.auctionQuantity() < l.quantity() ? PanelCharts.WARN : PanelCharts.up());
            y += 18;
        }
        int fy = top + quoteH - 56;
        g.fill(x + 2, fy - 3, x + w - 2, fy - 2, PanelCharts.PANEL_LINE);
        g.drawString(font, Component.translatable(K + "box.total"), x + 4, fy, PanelCharts.DIM, false);
        PanelCharts.right(g, font, PanelCharts.number(nowCoins), cNow, fy, PanelCharts.TEXT);
        PanelCharts.right(g, font, PanelCharts.number(aucCoins), cAuc, fy, PanelCharts.TEXT);
        line(g, x + 4, fy + 11, w - 8, K + "settle.other", PanelCharts.number(st.other()), PanelCharts.TEXT);
        long[] mr = minRow(g, x, fy + 22, w, mx, my);
        int bw = (w - 12) / 2;
        button(g, x + 4, fy + 38, bw, 16, Component.translatable(K + "box.sell_now"), false,
            () -> act(ExchangePackets.SETTLE_SELL, "", mr[0], scrapRest ? 1 : 0, (int) mr[1], 0, false, ""), mx, my);
        hover(x + 4, fy + 38, bw, 16, Component.translatable(K + "settle.sell_now_help").getString(), mx, my);
        button(g, x + 8 + bw, fy + 38, bw, 16, Component.translatable(K + "box.sell_auction"), false,
            () -> act(ExchangePackets.SETTLE_SELL, "", mr[0], 0, (int) mr[1], 0, true, ""), mx, my);
        hover(x + 8 + bw, fy + 38, bw, 16, Component.translatable(K + "box.sell_auction_help").getString(), mx, my);

        // deliveries, UU-matter, buying into the container
        int ay = top + quoteH + 4;
        PanelCharts.panel(g, x, ay, w, bottom - ay);
        g.drawString(font, Component.translatable(K + "settle.uu", PanelCharts.number(st.uu())), x + 4, ay + 5, st.uu() > 0 ? PanelCharts.TEXT : PanelCharts.DIM, false);
        button(g, x + w - 92, ay + 2, 88, 13, Component.translatable(K + "settle.uu_in"), false,
            () -> act(ExchangePackets.SETTLE_UU, "", 0, 0, 0, 0, false, ""), mx, my);
        int by = ay + 19;
        if (st.coins() > 0) {
            // 0.1.10.29: the coins of the container onto the coin account
            g.drawString(font, Component.translatable(K + "settle.coins", PanelCharts.number(st.coins())), x + 4, by + 3, PanelCharts.TEXT, false);
            button(g, x + w - 92, by, 88, 13, Component.translatable(K + "settle.coins_in"), false,
                () -> act(ExchangePackets.SETTLE_COINS, "", 0, 0, 0, 0, false, ""), mx, my);
            by += 16;
        }
        g.drawString(font, Component.translatable(K + "settle.buy", name(selected.isEmpty() ? "minecraft:air" : selected)), x + 4, by + 3, PanelCharts.TEXT, false);
        hover(x + 4, by, w / 2, 12, Component.translatable(K + "settle.buy_help").getString(), mx, my);
        int bx = x + w - 4 - 64;
        button(g, bx, by, 64, 13, Component.translatable(K + "settle.buy_go"), false, () -> {
            if (!selected.isEmpty()) act(ExchangePackets.SETTLE_BUY, selected, num(priceBox, 1), 0, 0, (int) Math.max(1, Math.min(ExchangeService.MAX_QUANTITY, num(qtyBox, 1))), true, "");
        }, mx, my);
        place(qtyBox, bx - 4 - 44, by, 44);
        place(priceBox, bx - 8 - 44 - 60, by, 60);
        hover(bx - 8 - 44 - 60, by, 60, 12, Component.translatable(K + "settle.limit_help").getString(), mx, my);
        int ty = by + 18;
        g.drawString(font, Component.translatable(K + "settle.targets"), x + 4, ty, PanelCharts.ACCENT, false);
        ty += 12;
        if (st.targets().isEmpty()) g.drawString(font, Component.translatable(K + "settle.no_targets"), x + 4, ty, PanelCharts.DIM, false);
        for (ExchangeView.SettleTarget t : st.targets()) {
            if (ty > bottom - 14) break;
            g.renderItem(new ItemStack(item(t.item())), x + 4, ty - 4);
            String text = Component.translatable(K + (t.type() == 0 ? "settle.target_bounty" : "settle.target_commission"), name(t.item()),
                PanelCharts.number(t.count())).getString();
            g.drawString(font, font.plainSubstrByWidth(text, w - 90), x + 24, ty, PanelCharts.TEXT, false);
            final long id = t.id();
            final int count = (int) Math.min(Integer.MAX_VALUE, t.count());
            final boolean bounty = t.type() == 0;
            button(g, x + w - 62, ty - 2, 58, 13, Component.translatable(K + "deliver"), false,
                () -> act(bounty ? ExchangePackets.SETTLE_BOUNTY : ExchangePackets.SETTLE_COMMISSION, "", id, 0, 0, count, false, ""), mx, my);
            ty += 16;
        }
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
                (int) Math.min(ExchangeService.MAX_QUANTITY, num(postQty, 0)), false, rewardItem == Items.AIR ? "" : BuiltInRegistries.ITEM.getKey(rewardItem).toString());
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
            // 0.1.10.29: the collateral comes from the inventory and the coin account together
            g.drawString(font, font.plainSubstrByWidth(Component.translatable(K + "pay_from", PanelCharts.number(v.coins()), PanelCharts.number(v.mailCoins())).getString(), w - 8),
                x + 4, y, PanelCharts.DIM, false);
            y += 14;
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
        // 0.1.10.29: the coin account: deposit / withdraw (0 = all), bank mode
        g.drawString(font, Component.translatable(K + "account_coins", PanelCharts.number(v.mailCoins())), x + 4, y, PanelCharts.TEXT, false);
        hover(x + 4, y - 2, half - 80, 12, Component.translatable(K + "account_help").getString(), mx, my);
        button(g, x + half - 70, y - 2, 64, 14, Component.translatable(K + "collect"), false,
            () -> act(ExchangePackets.COLLECT, "", 0, 0, 0, 0, false, ""), mx, my);
        y += 16;
        int cw = Math.max(36, (half - 20) / 3);
        place(coinBox, x + 4, y, cw);
        hover(x + 4, y, cw, 12, Component.translatable(K + "coin_amount_help").getString(), mx, my);
        button(g, x + 8 + cw, y - 1, cw, 14, Component.translatable(K + "coin_in"), false,
            () -> act(ExchangePackets.COIN_IN, "", num(coinBox, 0), 0, 0, 0, false, ""), mx, my);
        button(g, x + 12 + 2 * cw, y - 1, cw, 14, Component.translatable(K + "coin_out"), false,
            () -> act(ExchangePackets.COIN_OUT, "", num(coinBox, 0), 0, 0, 0, false, ""), mx, my);
        y += 16;
        button(g, x + 4, y - 1, half - 8, 14, Component.translatable(v.bankMode() ? K + "bank_on" : K + "bank_off"), v.bankMode(),
            () -> act(ExchangePackets.BANK_MODE, "", 0, 0, 0, 0, !v.bankMode(), ""), mx, my);
        hover(x + 4, y - 1, half - 8, 14, Component.translatable(K + "bank_help").getString(), mx, my);
        y += 18;
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
        // 0.1.10.28 (X4): the stabiliser and the scrap sink of the last 7 days
        ExchangeView.StabilizerLine st = v.stabilizer();
        String stab = st.enabled()
            ? Component.translatable(K + "stabilizer_on", String.format("%.0f%%", st.factor() * 100), PanelCharts.number(st.net7()), PanelCharts.number(st.target())).getString()
            : Component.translatable(K + "stabilizer_off", PanelCharts.number(st.net7())).getString();
        g.drawString(font, font.plainSubstrByWidth(stab, half - 8), x + 4, bottom - 38, st.enabled() && st.factor() < 1 ? PanelCharts.WARN : PanelCharts.TEXT, false);
        hover(x + 4, bottom - 39, half - 8, 10, Component.translatable(K + "stabilizer_help", PanelCharts.number(st.supply()), st.active()).getString(), mx, my);
        g.drawString(font, font.plainSubstrByWidth(Component.translatable(K + "scrap_week", PanelCharts.number(st.scrapIn()), PanelCharts.number(st.scrapOut())).getString(),
            half - 8), x + 4, bottom - 26, PanelCharts.DIM, false);
    }

    // ------------------------------------------------------------------ 0.1.10.26 (X3): bounties tab

    private static final int[] TIER_COLOR = {0xFF57606A, 0xFF1F6FEB, 0xFFBB8009};

    private void bounties(GuiGraphics g, ExchangeView v, int mx, int my) {
        int top = 26, bottom = height - 6, sideW = Math.max(170, Math.min(230, width / 4)), listW = width - sideW - 18;
        PanelCharts.panel(g, 6, top, listW, bottom - top);
        g.drawString(font, Component.translatable(K + "bounty.board"), 10, top + 4, PanelCharts.ACCENT, false);
        int rowH = 40, rows = Math.max(1, (bottom - top - 18) / rowH);
        List<ExchangeView.BountyLine> list = v.bounties();
        bountyScroll = Math.min(bountyScroll, Math.max(0, list.size() - rows));
        if (list.isEmpty()) g.drawString(font, Component.translatable(K + "bounty.none"), 10, top + 20, PanelCharts.DIM, false);
        int buttonX = 6 + listW - 64;
        for (int i = 0; i < rows && i + bountyScroll < list.size(); i++) {
            ExchangeView.BountyLine b = list.get(i + bountyScroll);
            int y = top + 16 + i * rowH;
            g.fill(7, y, 6 + listW - 1, y + rowH - 1, i % 2 == 0 ? 0x10FFFFFF : 0);
            int tier = Math.max(0, Math.min(2, b.tier()));
            Component tierName = Component.translatable(K + "bounty.tier." + tier);
            int tw = font.width(tierName) + 6;
            g.fill(10, y + 2, 10 + tw, y + 12, TIER_COLOR[tier]);
            g.drawString(font, tierName, 13, y + 3, 0xFFFFFFFF, false);
            hover(10, y + 2, tw, 10, Component.translatable(K + "bounty.tier_help." + tier).getString(), mx, my);
            long count = 0, done = 0;
            for (ExchangeView.NeedLine n : b.needs()) { count += n.count(); done += Math.min(n.delivered(), n.count()); }
            StringBuilder info = new StringBuilder(Component.translatable(K + "days_left", Math.max(0, b.expiry() - v.day())).getString());
            info.append(" \u00b7 ").append(count <= 0 ? 0 : done * 100 / count).append('%');
            if (b.split() == 1) info.append(" \u00b7 ").append(Component.translatable(K + "bounty.shared").getString());
            if (b.contributors() > 0) info.append(" \u00b7 ").append(Component.translatable(K + "bounty.contributors", b.contributors()).getString());
            g.drawString(font, font.plainSubstrByWidth(info.toString(), listW / 2 - tw - 12), 16 + tw, y + 3, PanelCharts.DIM, false);
            if (b.split() == 1) hover(16 + tw, y + 2, listW / 2 - tw - 12, 10, Component.translatable(K + "bounty.shared_help").getString(), mx, my);
            // needs
            int nx = 10;
            for (ExchangeView.NeedLine n : b.needs()) {
                String c = PanelCharts.number(n.delivered()) + "/" + PanelCharts.number(n.count());
                if (nx + 20 + font.width(c) > 6 + listW / 2) break;
                g.renderItem(new ItemStack(item(n.item())), nx, y + 15);
                g.drawString(font, c, nx + 18, y + 20, n.delivered() >= n.count() ? PanelCharts.up() : PanelCharts.TEXT, false);
                hover(nx, y + 15, 18 + font.width(c), 16, name(n.item()) + "  " + c, mx, my);
                nx += 24 + font.width(c);
            }
            // rewards
            int rx = 6 + listW / 2 + 4, rEnd = buttonX - 6;
            g.drawString(font, Component.translatable(K + "bounty.reward"), rx, y + 3, PanelCharts.WARN, false);
            for (ExchangeView.RewardLine r : b.rewards()) {
                String amount, tip;
                String source = Component.translatable(K + "bounty.source." + Math.max(0, Math.min(3, r.source()))).getString();
                String paid = r.paid() > 0 ? "  " + Component.translatable(K + "bounty.paid", PanelCharts.number(r.paid()), PanelCharts.number(r.amount())).getString() : "";
                if (r.kind() == 1) {
                    amount = "x" + PanelCharts.number(r.amount());
                    if (rx + 18 + font.width(amount) > rEnd) break;
                    g.renderItem(new ItemStack(item(r.item())), rx, y + 15);
                    g.drawString(font, amount, rx + 17, y + 20, PanelCharts.WARN, false);
                    tip = name(r.item()) + " " + amount + " (" + source + ")" + paid;
                    hover(rx, y + 15, 17 + font.width(amount), 16, tip, mx, my);
                    rx += 22 + font.width(amount);
                } else {
                    amount = r.kind() == 2 ? Component.translatable(K + "bounty.uu_amount", PanelCharts.number(r.amount())).getString()
                        : Component.translatable(K + "bounty.coin_amount", PanelCharts.number(r.amount())).getString();
                    if (rx + font.width(amount) > rEnd) break;
                    g.drawString(font, amount, rx, y + 20, PanelCharts.WARN, false);
                    tip = amount + " (" + source + ")" + paid;
                    hover(rx, y + 18, font.width(amount), 12, tip, mx, my);
                    rx += 8 + font.width(amount);
                }
            }
            // deliver, and this player's part
            final long id = b.id();
            button(g, buttonX, y + 3, 58, 15, Component.translatable(K + "deliver"), false,
                () -> act(ExchangePackets.BOUNTY_DELIVER, "", id, 0, 0, 0, false, ""), mx, my);
            hover(buttonX, y + 3, 58, 15, Component.translatable(K + "bounty.deliver_help").getString(), mx, my);
            String mine = b.room() >= 0
                ? Component.translatable(K + "bounty.mine_room", PanelCharts.number(b.mine()), PanelCharts.number(b.room()), b.sharePermille() / 10).getString()
                : b.mine() > 0 ? Component.translatable(K + "bounty.mine", PanelCharts.number(b.mine())).getString() : "";
            if (!mine.isEmpty()) {
                String fit = font.plainSubstrByWidth(mine, 6 + listW - 4 - rx);
                PanelCharts.right(g, font, fit, 6 + listW - 4, y + 22, PanelCharts.DIM);
                hover(6 + listW - 4 - font.width(fit), y + 21, font.width(fit), 10, mine + " - " + Component.translatable(K + "bounty.value_help").getString(), mx, my);
            }
        }
        // weekly budgets, vault, help
        int x = width - sideW - 6;
        PanelCharts.panel(g, x, top, sideW, bottom - top);
        g.drawString(font, Component.translatable(K + "bounty.budget"), x + 4, top + 4, PanelCharts.ACCENT, false);
        ExchangeView.BountyBudget q = v.budget();
        int y = top + 18;
        for (String line : new String[] {
            Component.translatable(K + "bounty.budget_advanced", PanelCharts.number(q.advancedIssued()), PanelCharts.number(q.advancedCap())).getString(),
            Component.translatable(K + "bounty.budget_special", PanelCharts.number(q.specialIssued()), PanelCharts.number(q.specialCap())).getString(),
            Component.translatable(K + "bounty.budget_uu", PanelCharts.number(q.uuIssued()), PanelCharts.number(q.uuCap())).getString(),
            Component.translatable(K + "bounty.vault", q.vaultTypes(), PanelCharts.number(q.vaultItems())).getString()}) {
            g.drawString(font, font.plainSubstrByWidth(line, sideW - 8), x + 4, y, PanelCharts.TEXT, false);
            y += 11;
        }
        hover(x, top + 16, sideW, 46, Component.translatable(K + "bounty.budget_help").getString(), mx, my);
        y += 6;
        for (var seq : font.split(Component.translatable(K + "bounty.help"), sideW - 8)) {
            if (y > bottom - 12) break;
            g.drawString(font, seq, x + 4, y, PanelCharts.DIM, false);
            y += 10;
        }
    }
}
