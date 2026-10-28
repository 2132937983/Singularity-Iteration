// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.screen.panel;

import com.miophas.singularity_iteration.common.client.screen.panel.PanelCanvas.Box;
import com.miophas.singularity_iteration.common.exchange.CommissionLots;
import com.miophas.singularity_iteration.common.exchange.ExchangePackets;
import com.miophas.singularity_iteration.common.exchange.ExchangeService;
import com.miophas.singularity_iteration.common.exchange.ExchangeView;
import com.miophas.singularity_iteration.common.exchange.MinPrice;
import com.miophas.singularity_iteration.common.exchange.UuBoard;
import java.util.ArrayList;
import java.util.List;

/**
 * 0.1.10.33: the pages of the exchange panel of the futures machine (eight tabs), drawn through a {@link PanelCanvas}
 * without Minecraft types: the screen ({@code mio_icif_gui_future_elc}) draws them with the game, the offline layout
 * harness with the vanilla font's advances, so the harness checks exactly what the game draws.
 *
 * <p>Layout rules (bug 1 of 0.1.10.32: 19 places where text overlapped, a button left the screen or a list ran into the rows
 * below it):
 * <ul>
 *   <li>The account line moved from the header into a status bar at the bottom ({@link #STATUS}); the tabs fall back to short
 *       labels, then to short labels without the title, then to scrolling with arrows.</li>
 *   <li>A label and its value share a row: the value keeps its width, the label is cut to the rest ({@link #labelValue}).</li>
 *   <li>Lists stop above the fixed rows at the bottom of their panel and say how many more there are ({@link #more}).</li>
 *   <li>Heights come from what must fit: the ticket first, the order book gets the rest; the quotes first, the chart the rest.</li>
 *   <li>Narrow screens: the quotes take the whole width (the last trades are not shown), the futures positions go under the
 *       contracts, the bounty side panel becomes a tooltip of the board title.</li>
 * </ul>
 */
public final class FuturePanel {
    public static final int TABS = 8, ROW = 20, TOP = 26;
    /** The status bar at the bottom (the account line, before 0.1.10.33 in the header). */
    public static final int STATUS = 11;
    static final String K = "gui.mio_icif.exchange.";
    static final long MAX_COINS = 999_999_999L;

    private PanelCanvas c;
    private int W, H;

    // state of the panel (kept while the screen is open; a resize keeps it)
    public int tab;
    public String selected = "";
    public boolean prefilled;
    int listScroll, boardScroll, bountyScroll, positionScroll, pickScroll, tabScroll;
    public boolean buySide = true, resting;
    /** Commission form: the asked item and the reward item ("" = none picked), reward kind, which one the picker sets. */
    public String target = "", rewardItem = "";
    public int rewardKind, pickMode;
    /** 0.1.10.29: the minimum of the sell and settlement pages is in coins (else percent of the bid without pressure). */
    public boolean minCoins;
    /** 0.1.10.29: the ticket's limit of a sale is a percent of the bid without pressure. */
    public boolean pricePercent;
    /** 0.1.10.28 (X4): the scrap sink of the instant sales (sell page, settlement page). */
    public boolean scrapRest;
    /** 0.1.10.33: the first entry shown of the lists that scroll with the wheel (orders, mail, stock, auctions, quotes, targets). */
    final java.util.Map<String, Integer> scrolls = new java.util.HashMap<>();

    // ------------------------------------------------------------------ common pieces

    String tr(String key, Object... args) { return c.tr(K + key, args); }

    static String n(long v) { return PanelStyle.number(v); }

    int bottom() { return H - 6 - STATUS + 3; }

    int leftW() { return Math.max(110, Math.min(170, W / 4)); }

    int rightW() { return Math.max(140, Math.min(190, W * 3 / 10)); }

    String name(String id) {
        if (UuBoard.is(id)) return tr("uu_board");
        return c.itemName(id);
    }

    String unit(int currency) { return tr(currency == 1 ? "uu" : "coins"); }

    /** Text fitted to {@code w} ({@link #fitText}); the whole text as tooltip when it changed. Returns the width drawn. */
    int text(String s, int x, int y, int w, int colour) {
        String fit = fitText(s, w);
        if (!fit.isEmpty()) c.text(fit, x, y, colour);
        if (!fit.equals(s) && w > 0) c.hover(x, y - 1, Math.max(1, c.width(fit)), 10, s);
        return c.width(fit);
    }

    static final java.util.regex.Pattern NUMBER = java.util.regex.Pattern.compile("\\d{1,3}(?:,\\d{3})+(?![\\d.])|(?<![\\d.,])\\d{5,}(?![\\d.,])");

    /** 0.1.10.33: the numbers of a text with thousands separators (or of five digits and more) in compact form (1.73M). */
    static String compactNumbers(String s) {
        java.util.regex.Matcher m = NUMBER.matcher(s);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String digits = m.group().replace(",", "");
            String r = digits.length() > 18 ? m.group() : PanelStyle.compact(Long.parseLong(digits));
            m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(r));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    static boolean numeric(char ch) { return Character.isDigit(ch) || ch == ',' || ch == '.'; }

    /**
     * 0.1.10.33: {@code s} within {@code w}: as it is when it fits; else with compact numbers; else the longest start of that
     * which does not end inside a number (before, "1,234,567" could read "1,2"). The harness hears of every change.
     */
    String fitText(String s, int w) {
        if (c.width(s) <= w) return s;
        String from = compactNumbers(s);
        String f = from;
        if (w <= 0) f = "";
        else if (c.width(from) > w) {
            f = c.fit(from, w);
            int at = f.length();
            if (at > 0 && at < from.length() && numeric(from.charAt(at - 1)) && (numeric(from.charAt(at)) || "kMB%u".indexOf(from.charAt(at)) >= 0)) {
                int k = at;
                while (k > 0 && (numeric(from.charAt(k - 1)) || "/+-".indexOf(from.charAt(k - 1)) >= 0)) k--;
                f = from.substring(0, k).stripTrailing();
            }
        }
        c.cut(s, from, f);
        return f;
    }

    /** A button label that fits the button (the canvas cuts nothing then). */
    String label(String s, int w) { return c.width(s) > w - 4 ? fitText(s, w - 6) : s; }

    void right(String s, int right, int y, int colour) { c.text(s, right - c.width(s), y, colour); }

    /**
     * 0.1.10.33: a text with numbers that must not be cut: drawn with full numbers when it fits, else with compact numbers
     * (1.73M), else cut (the tooltip has the full text). {@code make} builds the text with the given number format.
     */
    int textNum(java.util.function.Function<java.util.function.LongFunction<String>, String> make, int x, int y, int w, int colour) {
        String full = make.apply(PanelStyle::number);
        if (c.width(full) <= w) { c.text(full, x, y, colour); return c.width(full); }
        String compact = make.apply(PanelStyle::compact);
        int drawn = text(compact, x, y, w, colour);
        c.hover(x, y - 1, Math.max(1, drawn), 10, full);
        return drawn;
    }

    /** 0.1.10.33: a number right-aligned at {@code right} in at most {@code w}: full, else compact. Returns its width. */
    int rightNum(String prefix, long value, int right, int y, int w, int colour) {
        String s = fitText(prefix + n(value), w);
        right(s, right, y, colour);
        return c.width(s);
    }

    /** A label and its value in a row of width {@code w}: the value at the right keeps its width, the label gets the rest. */
    void labelValue(int x, int y, int w, String label, String value, int colour) {
        String val = fitText(value, w);
        int vw = c.width(val);
        right(val, x + w, y, colour);
        int lw = w - vw - 4;
        String fit = lw <= 0 ? "" : fitText(label, lw);
        if (!fit.isEmpty()) c.text(fit, x, y, PanelStyle.DIM);
        if (fit.length() < label.length()) c.hover(x, y - 1, w, 10, label + ": " + value);
    }

    /** "+N more" in a row (a list that does not fit; the wheel scrolls it). */
    void more(int x, int y, int w, int hidden) {
        if (hidden <= 0) return;
        int drawn = text(tr("more", hidden), x, y, w, PanelStyle.DIM);
        c.hover(x, y - 1, Math.max(1, drawn), 10, tr("more_help"));
    }

    /** 0.1.10.33: the first entry shown of the list {@code key} of {@code size} entries; the wheel over the area scrolls it. */
    int first(String key, int size, int x, int y, int w, int h) {
        int s = Math.max(0, Math.min(scrolls.getOrDefault(key, 0), size - 1));
        scrolls.put(key, s);
        c.wheel(x, y, w, h, d -> scrolls.merge(key, d, Integer::sum));
        return s;
    }

    void button(int x, int y, int w, int h, String label, boolean on, Runnable click) {
        String l = label(label, w);
        c.button(x, y, w, h, l, on, true, click);
        if (!l.equals(label)) c.hover(x, y, w, h, label);
    }

    void button(int x, int y, int w, int h, String label, Runnable click) { button(x, y, w, h, label, false, click); }

    void place(Box b, int x, int y, int w) { c.box(b, x, y, w); }

    /** Positions of the sell box / inventory / settlement slots (the screen moves the menu's slots there). */
    public static final int SELL_TOP = TOP, BOX_LEFT = 12;

    public static int boxTop() { return SELL_TOP + 16; }

    public static int invTop() { return boxTop() + 2 * 18 + 4 + 14 + 16; }

    public static int boxSlotX(int i) { return BOX_LEFT + (i % 9) * 18; }

    public static int boxSlotY(int i) { return boxTop() + (i / 9) * 18; }

    public static int invSlotX(int i) { return BOX_LEFT + (i % 9) * 18; }

    public static int invSlotY(int i) { return i < 27 ? invTop() + (i / 9) * 18 : invTop() + 3 * 18 + 4; }

    // ------------------------------------------------------------------ page and frame

    /** Draws the whole panel for the view {@code v} (null: loading). */
    public void render(PanelCanvas canvas, ExchangeView v) {
        this.c = canvas;
        this.W = canvas.screenWidth();
        this.H = canvas.screenHeight();
        c.fill(0, 0, W, H, 0xE00D1117);
        header(v);
        status(v);
        if (v == null) {
            String s = tr("loading");
            int lw = Math.min(W - 12, c.width(s) + 16);
            c.panel((W - lw) / 2, H / 2 - 6, lw, 20);
            text(s, (W - lw) / 2 + 8, H / 2, lw - 16, PanelStyle.DIM);
            return;
        }
        switch (tab) {
            case 0 -> market(v);
            case 1 -> commissions(v);
            case 2 -> futures(v);
            case 3 -> orders(v);
            case 4 -> treasury(v);
            case 5 -> sell(v);
            case 7 -> settlement(v);
            default -> bounties(v);
        }
    }

    /** Changes the page (0.1.10.27: the server learns whether the settlement page is shown). */
    public void page(int t) {
        int was = tab;
        tab = t;
        if (t != 0) pricePercent(false);
        if (c != null) c.pageChanged(was, t);
    }

    /**
     * The tabs: full labels after the title; short labels; short labels without the title; else the short labels that fit
     * with arrows to scroll them (the current tab is always shown).
     */
    void header(ExchangeView v) {
        c.strip(0, 0, W, 22, 0xFF161B22);
        String title = tr("title");
        String[] full = new String[TABS], brief = new String[TABS];
        for (int i = 0; i < TABS; i++) { full[i] = tr("tab." + i); brief[i] = tr("tab_short." + i); }
        int[][] modes = {{1, 0, 12}, {1, 1, 12}, {0, 1, 8}};   // {title, short, padding}
        for (int[] m : modes) {
            String[] labels = m[1] == 1 ? brief : full;
            int x0 = m[0] == 1 ? 8 + c.width(title) + 12 : 6;
            int end = x0;
            for (int i = 0; i < TABS; i++) end += c.width(labels[i]) + m[2] + (i > 0 ? 3 : 0);
            if (end > W - 6) continue;
            if (m[0] == 1) c.text(title, 8, 7, PanelStyle.ACCENT);
            int x = x0;
            for (int i = 0; i < TABS; i++) {
                int w = c.width(labels[i]) + m[2];
                tabButton(i, x, w, labels[i], full[i]);
                x += w + 3;
            }
            return;
        }
        // scrolling: arrows at the right, as many short labels as fit from tabScroll
        int arrows = W - 6 - 27;
        tabScroll = Math.max(0, Math.min(tabScroll, tab));
        while (true) {
            int x = 6, last = tabScroll - 1;
            for (int i = tabScroll; i < TABS; i++) {
                int w = c.width(brief[i]) + 8;
                if (x + w > arrows - 3) break;
                last = i;
                x += w + 3;
            }
            if (tab <= last || tabScroll >= tab) break;
            tabScroll++;
        }
        int x = 6;
        for (int i = tabScroll; i < TABS; i++) {
            int w = c.width(brief[i]) + 8;
            if (x + w > arrows - 3) break;
            tabButton(i, x, w, brief[i], full[i]);
            x += w + 3;
        }
        c.button(arrows, 4, 12, 14, "<", false, tabScroll > 0, () -> tabScroll = Math.max(0, tabScroll - 1));
        c.button(arrows + 15, 4, 12, 14, ">", false, tabScroll < TABS - 1, () -> tabScroll = Math.min(TABS - 1, tabScroll + 1));
        c.wheel(0, 0, W, 22, d -> tabScroll = Math.max(0, Math.min(TABS - 1, tabScroll + d)));
    }

    void tabButton(int i, int x, int w, String label, String full) {
        button(x, 4, w, 14, label, tab == i, () -> { page(i); c.request(); });
        if (!label.equals(full)) c.hover(x, 4, w, 14, full);
    }

    /** 0.1.10.33: the account line in a status bar at the bottom; its tooltip says that the day is the market's day. */
    void status(ExchangeView v) {
        int y = H - STATUS;
        c.strip(0, y, W, STATUS, 0xFF161B22);
        if (v == null) return;
        String line = tr("header_account", v.day() + 1, n(v.coins()), n(v.mailCoins()) + (v.bankMode() ? " B" : ""), n(v.uu()));
        String fit = fitText(line, W - 16);
        c.text(fit, 8, y + 2, v.energy() ? PanelStyle.TEXT : PanelStyle.down());
        c.hover(0, y, W, STATUS, (fit.length() < line.length() ? line + " | " : "") + tr("day_help"));
    }

    // ------------------------------------------------------------------ market tab

    void market(ExchangeView v) {
        int lw = leftW(), rw = rightW(), top = TOP, bottom = bottom();
        // watchlist
        c.panel(6, top, lw, bottom - top);
        int rows = Math.max(1, (bottom - top - 4) / ROW);
        listScroll = Math.max(0, Math.min(listScroll, v.markets().size() - rows));
        c.wheel(6, top, lw, bottom - top, d -> listScroll = Math.max(0, listScroll + d));
        for (int i = 0; i < rows && i + listScroll < v.markets().size(); i++) {
            ExchangeView.Row r = v.markets().get(i + listScroll);
            int y = top + 2 + i * ROW;
            if (r.id().equals(selected)) c.fill(7, y, 6 + lw - 1, y + ROW - 1, 0x402F81F7);
            c.item(r.id(), 9, y + 2);
            String change = PanelStyle.percent(r.changePermille());
            int changeX = 6 + lw - 4 - c.width(change);
            int nameW = changeX - 4 - 28 - (r.system() ? 0 : 8);
            text(name(r.id()), 28, y + 2, nameW, r.locked() ? PanelStyle.DIM : PanelStyle.TEXT);
            if (!r.system()) c.text("P", changeX - 10, y + 2, PanelStyle.WARN);
            right(change, 6 + lw - 4, y + 2, r.changePermille() >= 0 ? PanelStyle.up() : PanelStyle.down());
            int sparkX = 6 + lw - 34;
            textNum(f -> f.apply(r.price()) + (r.currency() == 1 ? "u" : ""), 28, y + 11, sparkX - 4 - 28, PanelStyle.DIM);
            c.sparkline(sparkX, y + 11, 30, 7, r.spark());
            if (r.locked()) c.shade(9, y + 2, 25, y + 18, 0xA0000000);
            final String id = r.id();
            c.region(7, y, lw - 2, ROW, () -> { if (!id.equals(selected)) { selected = id; prefilled = false; c.request(); } }, null);
            c.hover(7, y, lw - 2, ROW, name(r.id()) + (r.system() ? "" : " - " + tr("player_listing"))
                + (r.locked() ? " - " + tr("locked") : "") + " | " + tr("volume", n(r.volume())));
        }
        c.hidden("watchlist", Math.min(rows, v.markets().size()), v.markets().size());
        if (selected.isEmpty() && !v.markets().isEmpty()) { selected = v.markets().get(0).id(); c.request(); }
        ExchangeView.Detail d = v.detail();
        int cx = 12 + lw, cw = W - lw - rw - 24;
        if (d == null || !d.id().equals(selected)) {
            c.panel(cx, top, cw, bottom - top);
            return;
        }
        ExchangeView.CurveLine curve = ExchangeView.CurveLine.find(v.curves(), d.id());
        if (pricePercent && (buySide || !d.system() || curve == null)) pricePercent(false);
        if (!prefilled && !pricePercent) {
            // 0.1.10.33: a sale takes the market's bid even under the band (an instant sale may go there)
            long p = buySide ? firstPositive(d.ask(), d.asks().isEmpty() ? 0 : d.asks().get(0).price(), d.price())
                : d.system() ? Math.max(1, d.bid()) : firstPositive(d.bids().isEmpty() ? 0 : d.bids().get(0).price(), d.price());
            c.set(Box.PRICE, p);
            prefilled = true;
        }
        // quotes first (7 rows), the chart takes the rest of the height
        int quotesH = 4 + 11 + 6 * 10 + 2;
        int chartH = Math.max(56, Math.min((bottom - top) * 3 / 5, bottom - top - 4 - quotesH));
        chart(d, cx, top, cw, chartH);
        int by = top + chartH + 4, bh = bottom - by;
        boolean wide = cw >= 240;
        int qw = wide ? cw / 2 - 2 : cw;
        quotes(v, d, cx, by, qw, bh);
        if (wide) trades(d, cx + qw + 4, by, cw - qw - 4, bh, bottom);
        else c.hidden("last trades (narrow)", 0, d.trades().size());
        depthAndTicket(v, d, curve, W - rw - 6, top, rw, bottom);
    }

    void chart(ExchangeView.Detail d, int cx, int top, int cw, int chartH) {
        c.panel(cx, top, cw, chartH);
        String title = name(d.id()) + "  " + n(d.price()) + " " + unit(d.currency());
        text(title, cx + 4, top + 4, cw - 8, PanelStyle.TEXT);
        String sub = tr("meta", d.depth(), d.fundamental() > 0 ? n(d.fundamental()) : "-",
            d.system() ? tr("system_market") : tr("player_listing"));
        text(sub, cx + 4, top + 14, cw - 8, PanelStyle.DIM);
        int bars = d.bars().size();
        long[] o = new long[bars], h = new long[bars], l = new long[bars], cl = new long[bars], vol = new long[bars];
        for (int i = 0; i < bars; i++) { var b = d.bars().get(i); o[i] = b.open(); h[i] = b.high(); l[i] = b.low(); cl[i] = b.close(); vol[i] = b.volume(); }
        int gx = cx + 4, gy = top + 26, gw = cw - 8, gh = chartH - 30;
        if (gh < 8) return;
        c.candles(gx, gy, gw, gh, o, h, l, cl, vol);
        long[] sc = PanelStyle.scale(l, h, 0, 0);
        double[] ma = new double[bars];
        for (int i = 0; i < bars; i++) {
            if (i < 4) { ma[i] = Double.NaN; continue; }
            double s = 0;
            for (int k = i - 4; k <= i; k++) s += cl[k];
            ma[i] = s / 5;
        }
        c.line(gx, gy, gw, gh, ma, sc[0], sc[1], PanelStyle.ACCENT, false);
        long fundamental = d.fundamental();
        if (fundamental > 0 && fundamental >= sc[0] && fundamental <= sc[1]) {
            double[] f = new double[bars];
            java.util.Arrays.fill(f, fundamental);
            c.line(gx, gy, gw, gh, f, sc[0], sc[1], PanelStyle.WARN, true);
        }
        right(n(sc[1]), cx + cw - 4, gy, PanelStyle.DIM);
        if (gh * 4 / 5 - 10 >= 10) right(n(sc[0]), cx + cw - 4, gy + gh * 4 / 5 - 10, PanelStyle.DIM);
        c.hover(gx, gy, gw, gh, tr("chart_help"));
    }

    void quotes(ExchangeView v, ExchangeView.Detail d, int x, int y, int w, int h) {
        c.panel(x, y, w, h);
        int ty = y + 4;
        text(tr("quotes"), x + 4, ty, w - 8, PanelStyle.ACCENT);
        ty += 11;
        int iw = w - 8;
        if (d.system()) {
            // 0.1.10.33: a bounty that needs the item raises the ask to its reward per item (before: the market did not sell)
            boolean floored = v.bountyFloor() > 0 && d.ask() > 0;
            String ask = d.ask() > 0 ? n(d.ask()) + (floored ? "*" : "") : "-";
            labelValue(x + 4, ty, iw, tr("ask"), ask, d.ask() <= 0 ? PanelStyle.DIM : floored ? PanelStyle.WARN : PanelStyle.down());
            if (floored) c.hover(x + 4, ty - 1, iw, 10, tr("bounty_floor_help", n(v.bountyFloor())));
            labelValue(x + 4, ty + 10, iw, tr("supply"), n(d.supply()), PanelStyle.TEXT);
            // 0.1.10.33: a bid under one coin shows "<1" (before "-", as if the market did not buy)
            labelValue(x + 4, ty + 20, iw, tr("bid"), d.bid() > 0 ? n(d.bid()) : "<1", d.bid() > 0 ? PanelStyle.up() : PanelStyle.DIM);
            labelValue(x + 4, ty + 30, iw, tr("pressure"), n(d.pressure()), PanelStyle.TEXT);
            if (d.buyLeft() >= 0) labelValue(x + 4, ty + 40, iw, tr("buy_left"), n(d.buyLeft()), PanelStyle.TEXT);
            else labelValue(x + 4, ty + 40, iw, tr("sold_today"), n(d.mine()), PanelStyle.TEXT);
            c.hover(x + 4, ty + 20, iw, 30, tr("sell_curve_help"));
        } else {
            text(tr("listing_only_book"), x + 4, ty, iw, PanelStyle.DIM);
        }
        labelValue(x + 4, ty + 50, iw, tr("band"), n(d.low()) + " - " + n(d.high()), PanelStyle.DIM);
    }

    void trades(ExchangeView.Detail d, int x, int y, int w, int h, int bottom) {
        c.panel(x, y, w, h);
        text(tr("trades"), x + 4, y + 4, w - 8, PanelStyle.ACCENT);
        int row = y + 15, shown = 0;
        for (ExchangeView.TradeLine t : d.trades()) {
            if (row + 8 > bottom - 2) break;
            String q = "x" + t.quantity();
            right(q, x + w - 4, row, PanelStyle.DIM);
            text(n(t.price()), x + 4, row, w - 12 - c.width(q), t.mine() ? PanelStyle.WARN : PanelStyle.TEXT);
            row += 9;
            shown++;
        }
        c.hidden("last trades", shown, d.trades().size());
    }

    static long firstPositive(long... values) {
        for (long v : values) if (v > 0) return v;
        return 1;
    }

    /**
     * The order book and the ticket. 0.1.10.33: the ticket's height is fixed (it fits at 240 px high), the book shows the
     * levels that fit above it (before: 5 or 8 levels, and the submit button left the screen at 240 and 300 px high).
     */
    void depthAndTicket(ExchangeView v, ExchangeView.Detail d, ExchangeView.CurveLine curve, int x, int top, int w, int bottom) {
        boolean uu = UuBoard.is(d.id());
        int ticketH = 104 + (uu ? 10 : 0);
        int levels = Math.max(1, Math.min(ExchangeView.MAX_LEVELS, (bottom - top - 4 - ticketH - 18) / 18));
        int depthH = 18 + levels * 18;
        c.panel(x, top, w, depthH);
        text(tr("depth"), x + 4, top + 4, w - 8, PanelStyle.ACCENT);
        long maxQ = 1;
        for (var l : d.asks()) maxQ = Math.max(maxQ, l.quantity());
        for (var l : d.bids()) maxQ = Math.max(maxQ, l.quantity());
        int y = top + 14;
        for (int i = levels - 1; i >= 0; i--, y += 9) {
            if (i >= d.asks().size()) continue;
            var l = d.asks().get(i);
            depthRow(x, y, w, l, (PanelStyle.down() & 0x00FFFFFF) | 0x50000000, PanelStyle.down(), maxQ, true);
        }
        y = top + 14 + levels * 9;
        c.fill(x + 2, y - 1, x + w - 2, y, PanelStyle.PANEL_LINE);
        for (int i = 0; i < levels; i++, y += 9) {
            if (i >= d.bids().size()) continue;
            var l = d.bids().get(i);
            depthRow(x, y, w, l, (PanelStyle.up() & 0x00FFFFFF) | 0x50000000, PanelStyle.up(), maxQ, false);
        }
        c.hidden("book levels", Math.min(levels, d.asks().size()) + Math.min(levels, d.bids().size()), d.asks().size() + d.bids().size());
        // ticket
        int ty = top + depthH + 4;
        c.panel(x, ty, w, bottom - ty);
        int half = (w - 12) / 2;
        button(x + 4, ty + 3, half, 13, tr("buy"), buySide, () -> { buySide = true; prefilled = false; });
        button(x + 8 + half, ty + 3, half, 13, tr("sell"), !buySide, () -> { buySide = false; prefilled = false; });
        button(x + 4, ty + 18, half, 13, tr("now"), !resting, () -> resting = false);
        button(x + 8 + half, ty + 18, half, 13, tr("rest"), resting, () -> resting = true);
        int bx = x + w - 4 - 70;
        boolean toggle = !buySide && d.system() && curve != null;
        text(tr(pricePercent ? "price_percent" : "price"), x + 4, ty + 36, (toggle ? bx - 15 : bx) - 3 - (x + 4), PanelStyle.DIM);
        place(Box.PRICE, bx, ty + 34, 70);
        if (toggle) {
            // 0.1.10.29: a sale on the market may name its limit in percent of the bid without pressure
            button(bx - 15, ty + 34, 13, 12, pricePercent ? "%" : "c", pricePercent, () -> pricePercent(!pricePercent));
            c.hover(bx - 15, ty + 34, 13, 12, tr("percent_toggle_help"));
        }
        text(tr("quantity"), x + 4, ty + 51, bx - 30 - 3 - (x + 4), PanelStyle.DIM);
        place(Box.QTY, bx, ty + 49, 70);
        button(bx - 30, ty + 49, 13, 12, "-", false, () -> c.step(Box.QTY, -1));
        button(bx - 15, ty + 49, 13, 12, "+", false, () -> c.step(Box.QTY, 1));
        long price = ticketLimit(v, d), qty = c.num(Box.QTY, 0);
        int ry = ty + 64;
        if (uu) {
            // 0.1.10.28 (X4): one unit of the board is a lot of 100 mB of the UU account (0.1.10.33: its own row)
            text(tr("uu_lot", n(qty * UuBoard.LOT_MB), n(v.uu())), x + 4, ry, w - 8, PanelStyle.WARN);
            c.hover(x + 4, ry - 1, w - 8, 10, tr("uu_board_help"));
            ry += 10;
        }
        labelValue(x + 4, ry, w - 8, tr("total"), n(price * qty) + " " + unit(d.currency()), PanelStyle.TEXT);
        String note = tr(resting ? "rest_help" : "now_help");
        boolean belowBand = !buySide && d.system() && price < d.low();
        if (belowBand) {
            // 0.1.10.33: an instant sale may go under the band; a resting order may not
            text(tr(resting ? "below_band_rest" : "below_band", n(d.low())), x + 4, ry + 11, w - 8, resting ? PanelStyle.down() : PanelStyle.WARN);
            c.hover(x + 4, ry + 10, w - 8, 10, note + " | " + tr("below_band_help", n(d.low()), n(d.high())));
        } else if (toggle) {
            // 0.1.10.29: what the market part of this sale pays now (every item at the limit or above, one payout)
            MinPrice.Quote q = MinPrice.quote(curve.curve(), curve.mine(), curve.pressure(), price, qty, v.allowance() < 0 ? Long.MAX_VALUE : v.allowance());
            String est = tr("ticket_estimate", n(q.quantity()), n(qty), n(q.coins()), n(q.quantity() > 0 ? q.coins() / q.quantity() : 0));
            text(est, x + 4, ry + 11, w - 8, q.quantity() < qty ? PanelStyle.WARN : PanelStyle.up());
            c.hover(x + 4, ry + 10, w - 8, 10, note + " | " + tr("ticket_estimate_help", n(price), n(Math.round(q.last())), n(Math.round(q.next()))));
        } else {
            text(note, x + 4, ry + 11, w - 8, PanelStyle.DIM);
        }
        button(x + 4, ry + 22, w - 8, 15, tr(buySide ? "submit_buy" : "submit_sell"),
            false, () -> c.act(ExchangePackets.PLACE, d.id(), ticketLimit(v, d), 0, resting ? 1 : 0,
                (int) Math.min(ExchangeService.MAX_QUANTITY, c.num(Box.QTY, 0)), buySide, ""));
    }

    void depthRow(int x, int y, int w, ExchangeView.Level l, int barColour, int colour, long maxQ, boolean ask) {
        c.bar(x + 2, y, w - 4, 8, l.quantity() / (double) maxQ, barColour, true);
        String q = n(l.quantity());
        right(q, x + w - 4, y, PanelStyle.TEXT);
        text(n(l.price()), x + 4, y, w - 12 - c.width(q), colour);
        final long price = l.price();
        c.region(x + 2, y, w - 4, 9, () -> { buySide = ask; pricePercent(false); c.set(Box.PRICE, price); }, null);
    }

    /** 0.1.10.29: the ticket's limit in coins (a percent limit turned into coins with the curve now). */
    long ticketLimit(ExchangeView v, ExchangeView.Detail d) {
        ExchangeView.CurveLine curve = ExchangeView.CurveLine.find(v.curves(), d.id());
        if (pricePercent && curve != null) return MinPrice.limit(curve.unit(), true, c.num(Box.PRICE, 0));
        return c.num(Box.PRICE, 0);
    }

    /** 0.1.10.29: switches the ticket's limit between coins and percent (90 % to start; back in coins the price is filled in again). */
    public void pricePercent(boolean on) {
        if (on == pricePercent || c == null) { pricePercent = on && pricePercent; return; }
        pricePercent = on;
        if (on) { c.range(Box.PRICE, 0, MinPrice.MAX_PERCENT); c.set(Box.PRICE, 90); }
        else { c.range(Box.PRICE, 1, MAX_COINS); prefilled = false; }
    }

    // ------------------------------------------------------------------ commissions tab

    void commissions(ExchangeView v) {
        int top = TOP, bottom = bottom(), formW = Math.max(180, Math.min(220, W / 3)), listW = W - formW - 18;
        c.panel(6, top, listW, bottom - top);
        text(tr("board"), 10, top + 4, listW - 8, PanelStyle.ACCENT);
        int rows = Math.max(1, (bottom - top - 18) / 22);
        boardScroll = Math.max(0, Math.min(boardScroll, v.commissions().size() - rows));
        c.wheel(6, top, listW, bottom - top, d -> boardScroll = Math.max(0, boardScroll + d));
        int buttonX = 6 + listW - 64;
        for (int i = 0; i < rows && i + boardScroll < v.commissions().size(); i++) {
            ExchangeView.CommissionLine cl = v.commissions().get(i + boardScroll);
            int y = top + 16 + i * 22;
            c.fill(7, y, 6 + listW - 1, y + 21, i % 2 == 0 ? 0x10FFFFFF : 0);
            c.item(cl.item(), 10, y + 2);
            // 0.1.10.33: the reward sits right before the button and the texts get what is left (before: fixed halves overlapped)
            String reward = cl.kind() == 1 ? "x" + n(cl.rewardTotal()) : n(cl.rewardTotal()) + " " + unit(cl.kind() == 2 ? 1 : 0);
            int rewardW = (cl.kind() == 1 ? 18 : 0) + c.width(reward);
            int rx = Math.max(30 + 24, buttonX - 6 - rewardW);
            if (cl.kind() == 1) {
                c.item(cl.rewardItem(), rx, y + 2);
                text(reward, rx + 18, y + 6, buttonX - 6 - rx - 18, PanelStyle.WARN);
            } else {
                text(reward, rx, y + 6, buttonX - 6 - rx, PanelStyle.WARN);
            }
            int nameW = rx - 4 - 30;
            // 0.1.10.33: the progress and the days stand right (a long item or player name is cut, never them)
            String prog = cl.delivered() + "/" + cl.quantity(), days = tr("days_left", Math.max(0, cl.expiry() - v.day()));
            int progW = c.width(prog), daysW = c.width(days);
            if (progW + 30 <= nameW) {
                right(prog, 30 + nameW, y + 2, PanelStyle.TEXT);
                text(name(cl.item()), 30, y + 2, nameW - progW - 6, PanelStyle.TEXT);
            } else {
                text(name(cl.item()) + "  " + prog, 30, y + 2, nameW, PanelStyle.TEXT);
            }
            String who = cl.system() ? tr("market_request") : cl.requester();
            if (daysW + 30 <= nameW) {
                right(days, 30 + nameW, y + 11, PanelStyle.DIM);
                text(who, 30, y + 11, nameW - daysW - 6, PanelStyle.DIM);
            } else {
                text(who + " \u00b7 " + days, 30, y + 11, nameW, PanelStyle.DIM);
            }
            // 0.1.10.33 (Q20): item and UU rewards are delivered in whole lots
            long lot = CommissionLots.lot(cl.quantity(), cl.rewardTotal(), cl.kind() == 0);
            if (lot > 1)
                c.hover(30, y, rx - 30, 21, tr("commission_lot_help", n(lot), n(CommissionLots.perLot(cl.quantity(), cl.rewardTotal(), lot))));
            final long id = cl.id();
            if (cl.mine()) {
                button(buttonX, y + 3, 58, 15, tr("withdraw"), () -> c.act(ExchangePackets.WITHDRAW, "", id, 0, 0, 0, false, ""));
            } else {
                final int left = cl.quantity() - cl.delivered();
                button(buttonX, y + 3, 58, 15, tr("deliver"), () -> c.act(ExchangePackets.DELIVER, "", id, 0, 0, left, false, ""));
            }
        }
        c.hidden("commissions", Math.min(rows, v.commissions().size()), v.commissions().size());
        commissionForm(v, W - formW - 6, top, formW, bottom);
    }

    void commissionForm(ExchangeView v, int fx, int top, int formW, int bottom) {
        c.panel(fx, top, formW, bottom - top);
        text(tr("post"), fx + 4, top + 4, formW - 8, PanelStyle.ACCENT);
        int y = top + 15;
        button(fx + 4, y, formW / 2 - 6, 16, tr("pick_target"), pickMode == 0, () -> pickMode = 0);
        if (!target.isEmpty()) c.item(target, fx + formW / 2, y);
        y += 19;
        int boxX = fx + formW - 64;
        text(tr("quantity"), fx + 4, y + 2, boxX - 3 - (fx + 4), PanelStyle.DIM);
        place(Box.POST_QTY, boxX, y, 60);
        y += 15;
        for (int k = 0; k < 3; k++) {
            final int kind = k;
            button(fx + 4 + k * ((formW - 8) / 3), y, (formW - 8) / 3 - 2, 13, tr("reward." + k), rewardKind == k, () -> rewardKind = kind);
        }
        y += 16;
        if (rewardKind == 1) {
            button(fx + 4, y, formW / 2 - 6, 16, tr("pick_reward"), pickMode == 1, () -> pickMode = 1);
            if (!rewardItem.isEmpty()) c.item(rewardItem, fx + formW / 2, y);
            y += 19;
        }
        text(tr(rewardKind == 1 ? "reward_count" : "reward_amount"), fx + 4, y + 2, boxX - 3 - (fx + 4), PanelStyle.DIM);
        place(Box.POST_AMOUNT, boxX, y, 60);
        y += 15;
        text(tr("days"), fx + 4, y + 2, boxX - 3 - (fx + 4), PanelStyle.DIM);
        place(Box.POST_DAYS, boxX, y, 60);
        y += 15;
        text(tr(rewardKind == 0 ? "fee_coin" : "fee_flat"), fx + 4, y, formW - 8, PanelStyle.DIM);
        y += 10;
        button(fx + 4, y, formW - 8, 15, tr("post_button"), () -> {
            if (target.isEmpty()) return;
            c.act(ExchangePackets.POST, target, c.num(Box.POST_AMOUNT, 0), c.num(Box.POST_DAYS, 3), rewardKind,
                (int) Math.min(ExchangeService.MAX_QUANTITY, c.num(Box.POST_QTY, 0)), false, rewardItem);
        });
        y += 18;
        // inventory picker: the rows that fit, scrolled with the wheel (0.1.10.33: before, 0 to 9 of 36 slots at 240 px high)
        text(tr("inventory_pick"), fx + 4, y, formW - 8, PanelStyle.DIM);
        y += 10;
        List<String> inv = c.inventory();
        int cols = Math.max(1, Math.min(9, (formW - 8) / 18));
        int allRows = (inv.size() + cols - 1) / cols;
        // listing progress below the picker while the picker keeps two rows (or all of its rows)
        int avail = bottom - 2 - y;
        int candidatesH = v.candidates().isEmpty() ? 0 : 2 + 11 + Math.min(2, v.candidates().size()) * 10;
        int fitRows = Math.min(allRows, avail / 18);
        if (candidatesH > 0 && Math.min(allRows, (avail - candidatesH) / 18) >= Math.min(allRows, 2)) fitRows = Math.min(allRows, (avail - candidatesH) / 18);
        else candidatesH = 0;
        fitRows = Math.max(1, fitRows);
        pickScroll = Math.max(0, Math.min(pickScroll, allRows - fitRows));
        c.wheel(fx + 4, y, cols * 18, fitRows * 18, d -> pickScroll = Math.max(0, pickScroll + d));
        for (int r = 0; r < fitRows; r++) {
            for (int col = 0; col < cols; col++) {
                int s = (r + pickScroll) * cols + col;
                if (s >= inv.size()) break;
                int sx = fx + 4 + col * 18, sy = y + r * 18;
                c.fill(sx, sy, sx + 17, sy + 17, 0x40000000);
                String it = inv.get(s);
                if (it.isEmpty()) continue;
                c.item(it, sx, sy);
                c.region(sx, sy, 17, 17, () -> { if (pickMode == 0) target = it; else rewardItem = it; }, null);
                c.hover(sx, sy, 17, 17, c.itemName(it));
            }
        }
        c.hidden("inventory picker rows (wheel)", fitRows, allRows);
        if (candidatesH > 0) {
            int cy = y + fitRows * 18 + 2;
            text(tr("candidates"), fx + 4, cy, formW - 8, PanelStyle.ACCENT);
            int k = 0;
            for (ExchangeView.Candidate cand : v.candidates()) {
                if (k >= 2) break;
                text(name(cand.item()) + "  " + tr("candidate", cand.requesters(), cand.fulfilled(), cand.deliverers()), fx + 4, cy + 11 + k * 10,
                    formW - 8, PanelStyle.DIM);
                k++;
            }
        } else {
            c.hidden("listing progress", 0, v.candidates().size());
        }
    }

    // ------------------------------------------------------------------ futures tab

    void futures(ExchangeView v) {
        int top = TOP, bottom = bottom(), x = 6;
        // 0.1.10.33: the positions are always shown: beside the contracts (from 448 px wide; the contracts get at least 280 px),
        // or under them (before: not drawn under 458 px)
        boolean beside = W - 12 - 6 - 280 >= 150;
        int w = beside ? Math.min(320, W - 12 - 6 - 150) : Math.min(320, W - 12);
        int px = x + w + 6, pw = W - px - 6;
        ExchangeView.Detail d = v.detail();
        int contentH = 18 + (d == null || !d.system() ? 12 : 20 + 16 + 2 * 28 + 11) + 4;
        int fh = beside ? bottom - top : Math.min(bottom - top - 40, contentH);
        c.panel(x, top, w, fh);
        text(tr("futures"), x + 4, top + 4, w - 8, PanelStyle.ACCENT);
        int open = v.positions().size();
        long collateral = 0;
        for (ExchangeView.PositionLine p : v.positions()) collateral += p.price() * p.quantity();
        int y = top + 18;
        if (d == null || !d.system()) {
            text(tr("futures_pick"), x + 4, y, w - 8, PanelStyle.DIM);
        } else {
            c.item(d.id(), x + 4, y);
            text(name(d.id()) + "  " + n(d.price()), x + 24, y + 4, w - 28, PanelStyle.TEXT);
            y += 20;
            int boxX = x + w - 70;
            text(tr("quantity"), x + 4, y + 2, boxX - 3 - (x + 4), PanelStyle.DIM);
            place(Box.FUT_QTY, boxX, y, 66);
            y += 16;
            long q = Math.min(ExchangeService.FUTURES_MAX_QUANTITY, c.num(Box.FUT_QTY, 0));
            long[][] prices = {{d.futLong3(), d.futShort3()}, {d.futLong7(), d.futShort7()}};
            int[] days = {3, 7};
            long have = v.coins() + v.mailCoins();
            boolean full = open >= ExchangeService.MAX_POSITIONS;
            for (int i = 0; i < 2; i++) {
                final int dd = days[i];
                text(tr("contract_days", dd), x + 4, y + 3, 76, PanelStyle.TEXT);
                int bw = (w - 90) / 2;
                for (int side = 0; side < 2; side++) {
                    final boolean isLong = side == 0;
                    long need = prices[i][side] * q;
                    // 0.1.10.33: a contract that cannot be opened says why
                    String why = full ? tr("future_full", ExchangeService.MAX_POSITIONS) : q < 1 ? tr("future_no_quantity")
                        : need > have ? tr("future_no_coins", n(need), n(have)) : null;
                    int bx = x + 82 + side * (bw + 4);
                    c.button(bx, y, bw, 15, label(tr(isLong ? "long" : "short", n(prices[i][side])), bw), false, why == null,
                        () -> c.act(ExchangePackets.FUTURE, d.id(), 0, 0, dd, (int) Math.min(ExchangeService.FUTURES_MAX_QUANTITY, c.num(Box.FUT_QTY, 0)), isLong, ""));
                    if (why != null) c.hover(bx, y, bw, 15, why);
                }
                y += 17;
                text(tr("collateral", n(prices[i][0] * q), n(prices[i][1] * q)), x + 82, y, w - 86, PanelStyle.DIM);
                y += 11;
            }
            // 0.1.10.29: the collateral comes from the inventory and the coin account together
            text(tr("pay_from", n(v.coins()), n(v.mailCoins())), x + 4, y, w - 8, PanelStyle.DIM);
            y += 13;
            List<String> help = c.wrap(tr("futures_help"), w - 8);
            int shown = 0;
            for (String line : help) {
                if (y + 8 > top + fh - 3) break;
                c.text(line, x + 4, y, PanelStyle.DIM);
                y += 9;
                shown++;
            }
            c.hidden("futures help lines", shown, help.size());
            if (shown < help.size()) c.hover(x, top, w, 14, tr("futures_help"));
        }
        if (beside) positions(v, px, top, pw, bottom - top, open, collateral, 1);
        else {
            int py = top + fh + 4;
            positions(v, 6, py, W - 12, bottom - py, open, collateral, Math.max(1, (W - 12 - 8) / 200));
        }
    }

    void positions(ExchangeView v, int px, int py, int pw, int ph, int open, long collateral, int cols) {
        c.panel(px, py, pw, ph);
        text(tr("positions_count", open, ExchangeService.MAX_POSITIONS, n(collateral)), px + 4, py + 4, pw - 8, PanelStyle.ACCENT);
        c.hover(px, py, pw, 12, tr("positions_help", ExchangeService.MAX_POSITIONS, n(ExchangeService.FUTURES_MAX_QUANTITY)));
        int rows = Math.max(0, (ph - 16) / 22);
        int perPage = Math.max(1, rows * cols);
        int lines = (v.positions().size() + cols - 1) / cols;
        positionScroll = Math.max(0, Math.min(positionScroll, lines - rows));
        c.wheel(px, py, pw, ph, d -> positionScroll = Math.max(0, positionScroll + d));
        int colW = (pw - 8) / cols;
        int shown = 0;
        for (int i = positionScroll * cols; i < v.positions().size() && shown < perPage && rows > 0; i++, shown++) {
            ExchangeView.PositionLine p = v.positions().get(i);
            int k = i - positionScroll * cols;
            int rx = px + 4 + (k % cols) * colW, ry = py + 16 + (k / cols) * 22;
            c.item(p.commodity(), rx, ry);
            String side = tr(p.isLong() ? "side_long" : "side_short");
            textNum(f -> side + " x" + p.quantity() + " @ " + f.apply(p.price()), rx + 20, ry, colW - 24, PanelStyle.TEXT);
            long coll = p.price() * p.quantity();
            textNum(f -> tr("position_value", f.apply(p.valueNow()), Math.max(0, p.expiry() - v.day())), rx + 20, ry + 9, colW - 24,
                p.valueNow() >= coll ? PanelStyle.up() : PanelStyle.down());
        }
        c.hidden("positions (wheel)", shown, v.positions().size());
    }

    // ------------------------------------------------------------------ orders and mail tab

    void orders(ExchangeView v) {
        int top = TOP, bottom = bottom(), half = (W - 18) / 2;
        c.panel(6, top, half, bottom - top);
        text(tr("my_orders"), 10, top + 4, half - 8, PanelStyle.ACCENT);
        int y = top + 18, shown = 0;
        int s0 = first("orders", v.orders().size(), 6, top, half, bottom - top);
        for (int i = s0; i < v.orders().size(); i++) {
            ExchangeView.MyOrder o = v.orders().get(i);
            if (y + 16 > bottom - 2 - (i < v.orders().size() - 1 || s0 > 0 ? 10 : 0)) break;
            c.item(o.commodity(), 10, y);
            String side = tr(o.buy() ? "buy" : "sell");
            textNum(f -> side + " x" + o.remaining() + " @ " + f.apply(o.price()), 30, y + 4, half - 60 - 30, o.buy() ? PanelStyle.up() : PanelStyle.down());
            final long id = o.id();
            button(6 + half - 60, y + 1, 54, 14, tr("cancel"), () -> c.act(ExchangePackets.CANCEL, "", id, 0, 0, 0, false, ""));
            y += 18;
            shown++;
        }
        more(10, y + 2, half - 8, v.orders().size() - shown);
        c.hidden("orders (wheel)", shown, v.orders().size());
        int x = 12 + half;
        c.panel(x, top, half, bottom - top);
        text(tr("mailbox"), x + 4, top + 4, half - 8, PanelStyle.ACCENT);
        y = top + 18;
        // 0.1.10.29: the coin account: deposit / withdraw (0 = all), bank mode
        text(tr("account_coins", n(v.mailCoins())), x + 4, y, half - 80, PanelStyle.TEXT);
        c.hover(x + 4, y - 2, half - 80, 12, tr("account_help"));
        button(x + half - 70, y - 2, 64, 14, tr("collect"), () -> c.act(ExchangePackets.COLLECT, "", 0, 0, 0, 0, false, ""));
        y += 16;
        int cw = Math.max(36, (half - 20) / 3);
        place(Box.COIN, x + 4, y, cw);
        c.hover(x + 4, y, cw, 12, tr("coin_amount_help"));
        button(x + 8 + cw, y - 1, cw, 14, tr("coin_in"), () -> c.act(ExchangePackets.COIN_IN, "", c.num(Box.COIN, 0), 0, 0, 0, false, ""));
        button(x + 12 + 2 * cw, y - 1, cw, 14, tr("coin_out"), () -> c.act(ExchangePackets.COIN_OUT, "", c.num(Box.COIN, 0), 0, 0, 0, false, ""));
        y += 16;
        button(x + 4, y - 1, half - 8, 14, tr(v.bankMode() ? "bank_on" : "bank_off"), v.bankMode(),
            () -> c.act(ExchangePackets.BANK_MODE, "", 0, 0, 0, 0, !v.bankMode(), ""));
        c.hover(x + 4, y - 1, half - 8, 14, tr("bank_help"));
        y += 18;
        // 0.1.10.33: the mail stops above the UU-matter rows (before, the last line ran into them)
        int uy = bottom - 31;
        shown = 0;
        int m0 = first("mail", v.mail().size(), x, y, half, uy - y);
        for (int i = m0; i < v.mail().size(); i++) {
            ExchangeView.MailLine m = v.mail().get(i);
            if (y + 16 > uy - 2 - (i < v.mail().size() - 1 || m0 > 0 ? 10 : 0)) break;
            c.item(m.item(), x + 4, y);
            int cw2 = rightNum("x", m.count(), x + half - 4, y + 4, (half - 28) / 2, PanelStyle.TEXT);
            text(name(m.item()), x + 24, y + 4, half - 28 - cw2 - 6, PanelStyle.TEXT);
            y += 18;
            shown++;
        }
        more(x + 4, y + 2, half - 8, v.mail().size() - shown);
        c.hidden("mail (wheel)", shown, v.mail().size());
        text(tr("uu_account", n(v.uu())), x + 4, uy, half - 8, PanelStyle.ACCENT);
        button(x + 4, uy + 11, half / 2 - 6, 16, tr("uu_deposit"), () -> c.act(ExchangePackets.UU_IN, "", 0, 0, 0, 0, false, ""));
        button(x + half / 2, uy + 11, half / 2 - 6, 16, tr("uu_withdraw"), () -> c.act(ExchangePackets.UU_OUT, "", 0, 0, 0, 0, false, ""));
    }

    // ------------------------------------------------------------------ treasury tab

    void treasury(ExchangeView v) {
        int top = TOP, bottom = bottom(), half = (W - 18) / 2;
        c.panel(6, top, half, bottom - top);
        text(tr("ledger"), 10, top + 4, half - 8, PanelStyle.ACCENT);
        long max = 1, minted = 0, burned = 0;
        for (var l : v.ledger()) { max = Math.max(max, Math.max(l.minted(), l.burned())); minted += l.minted(); burned += l.burned(); }
        int gx = 12, gy = top + 18, gw = half - 12, gh = bottom - top - 50;
        int count = Math.max(1, v.ledger().size());
        double step = gw / (double) count;
        for (int i = 0; i < v.ledger().size(); i++) {
            var l = v.ledger().get(i);
            int bx = gx + (int) (step * i), bw = Math.max(2, (int) (step / 2) - 1);
            int mh = (int) (gh * l.minted() / (double) max), bh = (int) (gh * l.burned() / (double) max);
            c.fill(bx, gy + gh - mh, bx + bw, gy + gh, PanelStyle.up());
            c.fill(bx + bw + 1, gy + gh - bh, bx + 2 * bw + 1, gy + gh, PanelStyle.down());
            c.hover(bx, gy, 2 * bw + 2, gh, tr("ledger_day", l.day() + 1, n(l.minted()), n(l.burned())));
        }
        // 0.1.10.33: the total is cut to the panel (its tooltip has all of it)
        text(tr("ledger_total", n(minted), n(burned), n(minted - burned)), 10, bottom - 26, half - 8, PanelStyle.TEXT);
        text(tr("ledger_help"), 10, bottom - 14, half - 8, PanelStyle.DIM);
        int x = 12 + half;
        c.panel(x, top, half, bottom - top);
        text(tr("stock"), x + 4, top + 4, half - 8, PanelStyle.ACCENT);
        // 0.1.10.33: the stock stops above the three rows at the bottom (before, it ran into them)
        int footer = bottom - 39, y = top + 18, shown = 0;
        int k0 = first("stock", v.stocks().size(), x, top, half, footer - top);
        for (int i = k0; i < v.stocks().size(); i++) {
            ExchangeView.StockLine s = v.stocks().get(i);
            if (y + 16 > footer - 2 - (i < v.stocks().size() - 1 || k0 > 0 ? 10 : 0)) break;
            c.item(s.item(), x + 4, y);
            int cw2 = rightNum("x", s.stock(), x + half - 4, y + 4, (half - 28) / 2, PanelStyle.TEXT);
            text(name(s.item()), x + 24, y + 4, half - 28 - cw2 - 6, PanelStyle.TEXT);
            y += 18;
            shown++;
        }
        more(x + 4, y + 2, half - 8, v.stocks().size() - shown);
        c.hidden("stock (wheel)", shown, v.stocks().size());
        // 0.1.10.28 (X4): the stabiliser and the scrap sink of the last 7 days
        ExchangeView.StabilizerLine st = v.stabilizer();
        String stab = st.enabled()
            ? tr("stabilizer_on", String.format(java.util.Locale.ROOT, "%.0f%%", st.factor() * 100), n(st.net7()), n(st.target()))
            : tr("stabilizer_off", n(st.net7()));
        text(stab, x + 4, bottom - 37, half - 8, st.enabled() && st.factor() < 1 ? PanelStyle.WARN : PanelStyle.TEXT);
        c.hover(x + 4, bottom - 38, half - 8, 10, tr("stabilizer_help", n(st.supply()), st.active()));
        text(tr("scrap_week", n(st.scrapIn()), n(st.scrapOut())), x + 4, bottom - 26, half - 8, PanelStyle.DIM);
        long listings = v.markets().stream().filter(r -> !r.system()).count();
        text(tr("listings", listings), x + 4, bottom - 14, half - 8, PanelStyle.DIM);
    }

    // ------------------------------------------------------------------ sell page (0.1.10.25, X1)

    void sell(ExchangeView v) {
        int top = SELL_TOP, bottom = bottom(), lw = 9 * 18 + 12;
        // left: the box and the inventory (the slots themselves are drawn by the screen on top)
        c.panel(6, top, lw, bottom - top);
        text(tr("box.title"), BOX_LEFT, top + 4, lw - 12, PanelStyle.ACCENT);
        for (int i = 0; i < 18; i++) c.slot(boxSlotX(i), boxSlotY(i));
        int by = boxTop() + 2 * 18 + 4, half = (lw - 16) / 2;
        button(BOX_LEFT, by, half, 14, tr("box.fill"), () -> c.act(ExchangePackets.BOX_FILL, "", 0, 0, 0, 0, false, ""));
        c.hover(BOX_LEFT, by, half, 14, tr("box.fill_help"));
        button(BOX_LEFT + half + 4, by, half, 14, tr("box.return"), () -> c.act(ExchangePackets.BOX_RETURN, "", 0, 0, 0, 0, false, ""));
        text(tr("box.inventory"), BOX_LEFT, invTop() - 11, lw - 12, PanelStyle.DIM);
        for (int i = 0; i < 36; i++) c.slot(invSlotX(i), invSlotY(i));

        // right: the quote, the actions and the auction orders
        int x = 6 + lw + 6, w = W - x - 6;
        int need = 27 + (v.box().isEmpty() ? c.wrap(tr("box.empty"), w - 8).size() * 10 : v.box().size() * 18) + 60;
        int quoteH = Math.max(120, Math.min(need, bottom - top - 4 - 48));
        c.panel(x, top, w, quoteH);
        int fy = top + quoteH - 56;
        long[] totals = quoteLines(v, v.box(), x, top, w, fy, "box.quote", true);
        g_footer(v, x, w, fy, totals, tr("box.allowance"),
            v.allowance() < 0 ? tr("box.unlimited") : n(v.allowance()), ExchangePackets.BOX_SELL, tr("box.sell_now_help"));

        int ay = top + quoteH + 4;
        c.panel(x, ay, w, bottom - ay);
        text(tr("auction.title"), x + 4, ay + 4, w - 8, PanelStyle.ACCENT);
        int row = ay + 16, shown = 0;
        if (v.auctions().isEmpty()) text(tr("auction.none"), x + 4, row, w - 8, PanelStyle.DIM);
        int a0 = first("auctions", v.auctions().size(), x, ay, w, bottom - ay);
        for (int i = a0; i < v.auctions().size(); i++) {
            ExchangeView.AuctionLine a = v.auctions().get(i);
            if (row + 10 > bottom - 2 - (i < v.auctions().size() - 1 || a0 > 0 ? 10 : 0)) break;
            textNum(f -> tr("auction.line", name(a.item()), f.apply(a.quantity()), a.minPercent(), f.apply(a.estQuantity()), f.apply(a.estCoins())),
                x + 4, row, w - 60, PanelStyle.TEXT);
            final long id = a.id();
            button(x + w - 52, row - 2, 48, 12, tr("auction.cancel"), () -> c.act(ExchangePackets.AUCTION_CANCEL, "", id, 0, 0, 0, false, ""));
            row += 13;
            shown++;
        }
        if (!v.auctions().isEmpty()) more(x + 4, row, w - 8, v.auctions().size() - shown);
        c.hidden("auction orders (wheel)", shown, v.auctions().size());
    }

    /**
     * The quote lines of the sell box or the settlement container from {@code top + 27} down to the footer row {@code fy}:
     * 0.1.10.33: the lines that fit and "+N more" (before, the last line ran into the total row); the totals count every
     * line. Returns {coins now, coins at the auction}.
     */
    long[] quoteLines(ExchangeView v, List<ExchangeView.BoxLine> lines, int x, int top, int w, int fy, String titleKey, boolean emptyHelp) {
        text(tr(titleKey), x + 4, top + 4, w - 8, PanelStyle.ACCENT);
        int cQty = x + w * 45 / 100, cNow = x + w * 70 / 100, cAuc = x + w - 6;
        String hq = tr("box.qty"), hn = tr("box.now"), ha = tr("box.auction");
        right(fitText(hq, cQty - x - 28), cQty, top + 16, PanelStyle.DIM);
        right(fitText(hn, cNow - cQty - 6), cNow, top + 16, PanelStyle.DIM);
        right(fitText(ha, cAuc - cNow - 6), cAuc, top + 16, PanelStyle.DIM);
        int y = top + 27;
        long nowCoins = 0, aucCoins = 0;
        long[] min = minimum();
        long[] allowance = {v.allowance() < 0 ? Long.MAX_VALUE : v.allowance()};
        if (lines.isEmpty() && emptyHelp) {
            for (String line : c.wrap(tr("box.empty"), w - 8)) {
                if (y + 8 > fy - 4) break;
                c.text(line, x + 4, y, PanelStyle.DIM);
                y += 10;
            }
        }
        int shown = 0;
        int q0 = first(titleKey, lines.size(), x, top + 24, w, fy - 4 - (top + 24));
        boolean full = false;
        for (int i = 0; i < lines.size(); i++) {
            ExchangeView.BoxLine l = lines.get(i);
            MinPrice.Quote q = nowQuote(v, l, min, allowance);
            nowCoins += q.coins();
            aucCoins += l.auctionCoins();
            boolean last = i == lines.size() - 1;
            if (i < q0 || full) continue;
            if (y + 12 > fy - 4 - (last && q0 == 0 ? 0 : 10)) { full = true; continue; }
            c.item(l.id(), x + 4, y - 4);
            int qtyW = rightNum("", l.quantity(), cQty, y, (cQty - x - 24) / 2, PanelStyle.TEXT);
            text(name(l.id()), x + 24, y, cQty - qtyW - 4 - (x + 24), PanelStyle.TEXT);
            // 0.1.10.33: "sold: coins" with compact numbers when the column is narrow (before, the digits were cut)
            right(column(q.quantity() < l.quantity(), q.quantity(), q.coins(), cNow - cQty - 6), cNow, y, q.quantity() < l.quantity() ? PanelStyle.WARN : PanelStyle.up());
            right(column(l.auctionQuantity() < l.quantity(), l.auctionQuantity(), l.auctionCoins(), cAuc - cNow - 6), cAuc, y,
                l.auctionQuantity() < l.quantity() ? PanelStyle.WARN : PanelStyle.up());
            c.hover(x + 2, y - 4, w - 4, 16, tr("box.line_help", n(l.quantity()), n(q.quantity()), n(q.coins()), n(l.auctionQuantity()),
                n(l.auctionCoins())) + " | " + tr("box.item_prices", n(Math.round(q.last())), n(Math.round(q.next()))));
            y += 18;
            shown++;
        }
        if (shown < lines.size()) more(x + 24, y - 1, w - 28, lines.size() - shown);
        c.hidden(titleKey + " lines (wheel)", shown, lines.size());
        return new long[] {nowCoins, aucCoins};
    }

    /** A quote column: "quantity: coins" when not all sell, else "coins"; full numbers, else compact, else only the coins. */
    String column(boolean part, long quantity, long coins, int w) {
        String full = (part ? n(quantity) + ": " : "") + n(coins);
        if (c.width(full) <= w) return full;
        String compact = (part ? PanelStyle.compact(quantity) + ": " : "") + PanelStyle.compact(coins);
        if (c.width(compact) <= w) return compact;
        return fitText(PanelStyle.compact(coins), w);
    }

    /** The footer of the sell and settlement quotes: totals, one value row, the minimum row and the two sale buttons. */
    void g_footer(ExchangeView v, int x, int w, int fy, long[] totals, String rowLabel, String rowValue, int sellKind, String nowHelp) {
        int cNow = x + w * 70 / 100, cAuc = x + w - 6;
        c.fill(x + 2, fy - 3, x + w - 2, fy - 2, PanelStyle.PANEL_LINE);
        String tn = n(totals[0]), ta = n(totals[1]);
        right(fitText(tn, cNow - x - 60), cNow, fy, PanelStyle.TEXT);
        right(fitText(ta, cAuc - cNow - 6), cAuc, fy, PanelStyle.TEXT);
        text(tr("box.total"), x + 4, fy, cNow - c.width(tn) - 4 - (x + 4), PanelStyle.DIM);
        labelValue(x + 4, fy + 11, w - 8, rowLabel, rowValue, PanelStyle.TEXT);
        long[] mr = minRow(x, fy + 22, w);
        int bw = (w - 12) / 2;
        button(x + 4, fy + 38, bw, 16, tr("box.sell_now"), () -> c.act(sellKind, "", mr[0], scrapRest ? 1 : 0, (int) mr[1], 0, false, ""));
        c.hover(x + 4, fy + 38, bw, 16, nowHelp);
        button(x + 8 + bw, fy + 38, bw, 16, tr("box.sell_auction"), () -> c.act(sellKind, "", mr[0], 0, (int) mr[1], 0, true, ""));
        c.hover(x + 8 + bw, fy + 38, bw, 16, tr("box.sell_auction_help"));
    }

    /** 0.1.10.29: the page's minimum as {coins, percent} (one of them 0). */
    long[] minimum() {
        long value = c.num(Box.MIN, 0);
        return minCoins ? new long[] {value, 0} : new long[] {0, Math.min(MinPrice.MAX_PERCENT, value)};
    }

    /**
     * 0.1.10.29: the minimum row of the sell and settlement pages: the box (coins or percent), the unit toggle and the scrap toggle;
     * returns the minimum as the action fields {coins (a), percent (c)}.
     */
    long[] minRow(int x, int y, int w) {
        int boxX = x + w - 4 - 56, toggleX = boxX - 2 - 14, scrapW = Math.min(96, Math.max(40, (toggleX - x) / 2)), scrapX = toggleX - 6 - scrapW;
        text(tr(minCoins ? "box.min_coins" : "box.min"), x + 4, y + 2, scrapX - 4 - (x + 4), PanelStyle.DIM);
        place(Box.MIN, boxX, y, 56);
        button(toggleX, y, 14, 13, minCoins ? "c" : "%", minCoins, () -> {
            minCoins = !minCoins;
            c.range(Box.MIN, 0, minCoins ? MAX_COINS : MinPrice.MAX_PERCENT);
            c.set(Box.MIN, 0);
        });
        c.hover(toggleX, y, 14, 13, tr("box.min_toggle_help"));
        c.hover(boxX, y, 56, 13, tr("box.min_help"));
        button(scrapX, y, scrapW, 13, tr(scrapRest ? "scrap_on" : "scrap_off"), scrapRest, () -> scrapRest = !scrapRest);
        c.hover(scrapX, y, scrapW, 13, tr("scrap_help"));
        return minimum();
    }

    /**
     * 0.1.10.29: the instant sale of a quote line at the page's minimum, from the line's curve (the allowance as the earlier lines
     * leave it, in the order the server sells them); the server's own quote (limit 1) when the view has no curve.
     */
    MinPrice.Quote nowQuote(ExchangeView v, ExchangeView.BoxLine l, long[] min, long[] allowance) {
        ExchangeView.CurveLine cv = ExchangeView.CurveLine.find(v.curves(), l.id());
        if (cv == null) return new MinPrice.Quote(l.nowQuantity(), l.nowCoins(), 0, 0);
        long limit = min[0] > 0 ? min[0] : MinPrice.limit(cv.unit(), true, min[1]);
        MinPrice.Quote q = MinPrice.quote(cv.curve(), cv.mine(), cv.pressure(), limit, l.quantity(), allowance[0]);
        if (allowance[0] != Long.MAX_VALUE) allowance[0] = Math.max(0, allowance[0] - q.coins());
        return q;
    }

    // ------------------------------------------------------------------ settlement page (0.1.10.27, X2)

    void settlement(ExchangeView v) {
        int top = SELL_TOP, bottom = bottom(), lw = 9 * 18 + 12;
        ExchangeView.SettleView st = v.settle();
        c.panel(6, top, lw, bottom - top);
        text(tr("settle.title"), BOX_LEFT, top + 4, lw - 12, PanelStyle.ACCENT);
        c.slot(BOX_LEFT, boxTop());
        text(tr("settle.kind." + Math.max(0, Math.min(3, st.kind()))), BOX_LEFT + 22, boxTop() + 4, lw - 34, st.kind() == 0 ? PanelStyle.DIM : PanelStyle.TEXT);
        int hy = boxTop() + 22;
        List<String> help = c.wrap(tr("settle.help"), lw - 12);
        int shownHelp = 0;
        for (String line : help) {
            if (hy + 8 > invTop() - 14) break;
            c.text(line, BOX_LEFT, hy, PanelStyle.DIM);
            hy += 10;
            shownHelp++;
        }
        c.hidden("settlement help lines", shownHelp, help.size());
        if (shownHelp < help.size()) c.hover(BOX_LEFT, boxTop() + 20, lw - 12, Math.max(1, hy - boxTop() - 20), tr("settle.help"));
        text(tr("box.inventory"), BOX_LEFT, invTop() - 11, lw - 12, PanelStyle.DIM);
        for (int i = 0; i < 36; i++) c.slot(invSlotX(i), invSlotY(i));

        int x = 6 + lw + 6, w = W - x - 6;
        // 0.1.10.33: the quote gets what its lines need, at most 3/5, and leaves the deliveries room for three targets
        int need = 27 + Math.max(1, st.lines().size()) * 18 + 60;
        int lowerNeed = 18 + (st.coins() > 0 ? 16 : 0) + 17 + 12 + Math.max(1, Math.min(3, st.targets().size())) * 16 + 4;
        int quoteH = Math.max(120, Math.min(need, Math.min((bottom - top) * 3 / 5, bottom - top - 4 - lowerNeed)));
        c.panel(x, top, w, quoteH);
        int fy = top + quoteH - 56;
        if (st.lines().isEmpty()) text(tr(st.kind() == 0 ? "settle.empty" : "settle.no_commodity"), x + 4, top + 27, w - 8, PanelStyle.DIM);
        long[] totals = quoteLines(v, st.lines(), x, top, w, fy, "settle.quote", false);
        g_footer(v, x, w, fy, totals, tr("settle.other"), n(st.other()), ExchangePackets.SETTLE_SELL, tr("settle.sell_now_help"));

        // deliveries, UU-matter, buying into the container
        int ay = top + quoteH + 4;
        c.panel(x, ay, w, bottom - ay);
        text(tr("settle.uu", n(st.uu())), x + 4, ay + 5, w - 8 - 92, st.uu() > 0 ? PanelStyle.TEXT : PanelStyle.DIM);
        button(x + w - 92, ay + 2, 88, 13, tr("settle.uu_in"), () -> c.act(ExchangePackets.SETTLE_UU, "", 0, 0, 0, 0, false, ""));
        int by = ay + 18;
        if (st.coins() > 0) {
            // 0.1.10.29: the coins of the container onto the coin account
            text(tr("settle.coins", n(st.coins())), x + 4, by + 3, w - 8 - 92, PanelStyle.TEXT);
            button(x + w - 92, by, 88, 13, tr("settle.coins_in"), () -> c.act(ExchangePackets.SETTLE_COINS, "", 0, 0, 0, 0, false, ""));
            by += 16;
        }
        int bx = x + w - 4 - 64, qx = bx - 4 - 44, prx = qx - 4 - 60;
        text(tr("settle.buy", name(selected.isEmpty() ? "minecraft:air" : selected)), x + 4, by + 3, prx - 4 - (x + 4), PanelStyle.TEXT);
        c.hover(x + 4, by, Math.max(1, prx - 4 - (x + 4)), 12, tr("settle.buy_help"));
        button(bx, by, 64, 13, tr("settle.buy_go"), () -> {
            if (!selected.isEmpty()) c.act(ExchangePackets.SETTLE_BUY, selected, c.num(Box.PRICE, 1), 0, 0,
                (int) Math.max(1, Math.min(ExchangeService.MAX_QUANTITY, c.num(Box.QTY, 1))), true, "");
        });
        place(Box.QTY, qx, by, 44);
        place(Box.PRICE, prx, by, 60);
        c.hover(prx, by, 60, 12, tr("settle.limit_help"));
        int ty = by + 17;
        text(tr("settle.targets"), x + 4, ty, w - 8, PanelStyle.ACCENT);
        ty += 12;
        if (st.targets().isEmpty()) text(tr("settle.no_targets"), x + 4, ty, w - 8, PanelStyle.DIM);
        int shown = 0;
        int t0 = first("targets", st.targets().size(), x, ty - 4, w, bottom - ty + 4);
        for (int i = t0; i < st.targets().size(); i++) {
            ExchangeView.SettleTarget t = st.targets().get(i);
            if (ty + 12 > bottom - 2 - (i < st.targets().size() - 1 || t0 > 0 ? 10 : 0)) break;
            c.item(t.item(), x + 4, ty - 4);
            textNum(f -> tr(t.type() == 0 ? "settle.target_bounty" : "settle.target_commission", name(t.item()), f.apply(t.count())), x + 24, ty, w - 90,
                PanelStyle.TEXT);
            final long id = t.id();
            final int count = (int) Math.min(Integer.MAX_VALUE, t.count());
            final boolean bounty = t.type() == 0;
            button(x + w - 62, ty - 2, 58, 13, tr("deliver"),
                () -> c.act(bounty ? ExchangePackets.SETTLE_BOUNTY : ExchangePackets.SETTLE_COMMISSION, "", id, 0, 0, count, false, ""));
            ty += 16;
            shown++;
        }
        if (!st.targets().isEmpty()) more(x + 24, ty - 2, w - 28, st.targets().size() - shown);
        c.hidden("settlement targets (wheel)", shown, st.targets().size());
    }

    // ------------------------------------------------------------------ 0.1.10.26 (X3): bounties tab

    static final int[] TIER_COLOR = {0xFF57606A, 0xFF1F6FEB, 0xFFBB8009};

    /**
     * 0.1.10.33: a bounty row has its rewards on the first line (before: right half, not drawn under 534 px) and its needs on
     * one or two lines below, unfinished first, then "+N" with all of them in the tooltip (before, the needs that did not fit
     * in half the row were left out without a sign: the third need of a bounty could not be seen). Under 560 px the side panel
     * becomes the tooltip of the board title.
     */
    void bounties(ExchangeView v) {
        int top = TOP, bottom = bottom();
        int sideW = W >= 560 ? Math.max(170, Math.min(230, W / 4)) : 0;
        int listW = W - 12 - (sideW > 0 ? sideW + 6 : 0);
        c.panel(6, top, listW, bottom - top);
        String title = tr("bounty.board");
        int tw0 = text(title, 10, top + 4, listW - 8, PanelStyle.ACCENT);
        ExchangeView.BountyBudget q = v.budget();
        String[] budget = {
            tr("bounty.budget_advanced", n(q.advancedIssued()), n(q.advancedCap())),
            tr("bounty.budget_special", n(q.specialIssued()), n(q.specialCap())),
            tr("bounty.budget_uu", n(q.uuIssued()), n(q.uuCap())),
            tr("bounty.vault", q.vaultTypes(), n(q.vaultItems()))};
        if (sideW == 0) {
            String b = tr("bounty.budget");
            int bw = c.width(b);
            if (10 + tw0 + 12 + bw <= 6 + listW - 4) {
                right(b, 6 + listW - 4, top + 4, PanelStyle.DIM);
                c.hover(6 + listW - 4 - bw, top + 3, bw, 10, String.join(" | ", budget) + " | " + tr("bounty.budget_help") + " | " + tr("bounty.help"));
            }
        }
        List<ExchangeView.BountyLine> list = v.bounties();
        if (list.isEmpty()) text(tr("bounty.none"), 10, top + 20, listW - 8, PanelStyle.DIM);
        bountyScroll = Math.max(0, Math.min(bountyScroll, list.size() - 1));
        c.wheel(6, top, listW, bottom - top, d -> bountyScroll = Math.max(0, bountyScroll + d));
        int buttonX = 6 + listW - 64;
        int y = top + 16, shown = 0;
        for (int i = bountyScroll; i < list.size(); i++) {
            int h = bountyRow(v, list.get(i), y, listW, buttonX, bottom, shown % 2 == 0);
            if (h <= 0) break;
            y += h;
            shown++;
        }
        if (bountyScroll + shown < list.size()) more(10, bottom - 11, listW - 8, list.size() - bountyScroll - shown);
        c.hidden("bounties (wheel)", shown, list.size() - bountyScroll);
        if (sideW > 0) {
            int x = W - sideW - 6;
            c.panel(x, top, sideW, bottom - top);
            text(tr("bounty.budget"), x + 4, top + 4, sideW - 8, PanelStyle.ACCENT);
            int by = top + 18;
            for (String line : budget) {
                text(line, x + 4, by, sideW - 8, PanelStyle.TEXT);
                by += 11;
            }
            c.hover(x, top + 16, sideW, 46, tr("bounty.budget_help"));
            by += 6;
            List<String> help = c.wrap(tr("bounty.help"), sideW - 8);
            int shownHelp = 0;
            for (String line : help) {
                if (by + 8 > bottom - 3) break;
                c.text(line, x + 4, by, PanelStyle.DIM);
                by += 10;
                shownHelp++;
            }
            c.hidden("bounty help lines", shownHelp, help.size());
        }
    }

    /**
     * One bounty row from {@code y}; returns its height, or 0 when it does not fit above {@code bottom} (leaving room for
     * "+N more"). The first line (16 px: tier, info, rewards with their icons, the button) and one or two lines of needs.
     */
    int bountyRow(ExchangeView v, ExchangeView.BountyLine b, int y, int listW, int buttonX, int bottom, boolean even) {
        // needs: unfinished first, wrapped on up to two lines before the button, then "+N"
        List<ExchangeView.NeedLine> needs = new ArrayList<>();
        for (ExchangeView.NeedLine nl : b.needs()) if (nl.delivered() < nl.count()) needs.add(nl);
        for (ExchangeView.NeedLine nl : b.needs()) if (nl.delivered() >= nl.count()) needs.add(nl);
        int needEnd = buttonX - 6;
        int[] lineOf = new int[needs.size()], xOf = new int[needs.size()];
        int line = 0, nx = 10, placed = 0;
        for (int k = 0; k < needs.size(); k++) {
            ExchangeView.NeedLine nl = needs.get(k);
            int w = 18 + c.width(n(nl.delivered()) + "/" + n(nl.count()));
            int reserve = line == 1 && k < needs.size() - 1 ? 6 + c.width("+" + (needs.size() - k)) : 0;
            if (nx + w + reserve > needEnd && nx > 10) {
                if (line == 1) break;
                line++;
                nx = 10;
                reserve = k < needs.size() - 1 ? 6 + c.width("+" + (needs.size() - k)) : 0;
                if (nx + w + reserve > needEnd) break;
            }
            lineOf[k] = line;
            xOf[k] = nx;
            nx += w + 6;
            placed++;
        }
        int lines = placed == 0 ? 1 : lineOf[placed - 1] + 1;
        int rowH = 16 + lines * 18;
        if (y + rowH > bottom - 2 - 11) return 0;
        c.fill(7, y, 6 + listW - 1, y + rowH - 1, even ? 0x10FFFFFF : 0);
        int tier = Math.max(0, Math.min(2, b.tier()));
        String tierName = tr("bounty.tier." + tier);
        int tw = c.width(tierName) + 6;
        c.fill(10, y + 2, 10 + tw, y + 12, TIER_COLOR[tier]);
        c.text(tierName, 13, y + 3, 0xFFFFFFFF);
        c.hover(10, y + 2, tw, 10, tr("bounty.tier_help." + tier));
        long count = 0, done = 0;
        for (ExchangeView.NeedLine nl : b.needs()) { count += nl.count(); done += Math.min(nl.delivered(), nl.count()); }
        StringBuilder info = new StringBuilder(tr("days_left", Math.max(0, b.expiry() - v.day())));
        info.append(" \u00b7 ").append(count <= 0 ? 0 : done * 100 / count).append('%');
        if (b.split() == 1) info.append(" \u00b7 ").append(tr("bounty.shared"));
        if (b.contributors() > 0) info.append(" \u00b7 ").append(tr("bounty.contributors", b.contributors()));
        // rewards: the right part of the first line, before the button; the info gets the rest (at least its days)
        String rl = tr("bounty.reward");
        int rewardsW = c.width(rl) + 4;
        for (ExchangeView.RewardLine r : b.rewards()) rewardsW += rewardWidth(r) + 6;
        int infoX = 16 + tw, minInfo = Math.min(c.width(info.toString()), c.width(tr("days_left", 99)) + 4);
        int rEnd = buttonX - 6;
        int rx = Math.max(infoX + minInfo + 8, rEnd - rewardsW);
        text(info.toString(), infoX, y + 4, rx - 8 - infoX, PanelStyle.DIM);
        if (b.split() == 1) c.hover(infoX, y + 3, Math.max(1, rx - 8 - infoX), 10, tr("bounty.shared_help"));
        int lx = rx;
        if (lx + c.width(rl) <= rEnd) { c.text(rl, lx, y + 4, PanelStyle.WARN); lx += c.width(rl) + 4; }
        List<String> tips = new ArrayList<>();
        for (ExchangeView.RewardLine r : b.rewards()) {
            String source = tr("bounty.source." + Math.max(0, Math.min(3, r.source())));
            String paid = r.paid() > 0 ? "  " + tr("bounty.paid", n(r.paid()), n(r.amount())) : "";
            tips.add((r.kind() == 1 ? name(r.item()) + " " : "") + rewardAmount(r) + " (" + source + ")" + paid);
        }
        int drawn = 0;
        for (ExchangeView.RewardLine r : b.rewards()) {
            int rw = rewardWidth(r);
            int rest = b.rewards().size() - drawn - 1;
            int reserve = rest > 0 ? 6 + c.width("+" + rest) : 0;
            if (lx + rw + reserve > rEnd) break;
            String amount = rewardAmount(r);
            if (r.kind() == 1) {
                c.item(r.item(), lx, y);
                c.text(amount, lx + 17, y + 4, PanelStyle.WARN);
            } else {
                c.text(amount, lx, y + 4, PanelStyle.WARN);
            }
            c.hover(lx, y, rw, 16, tips.get(drawn));
            lx += rw + 6;
            drawn++;
        }
        if (drawn < b.rewards().size()) {
            String plus = "+" + (b.rewards().size() - drawn);
            if (lx + c.width(plus) <= rEnd) {
                c.text(plus, lx, y + 4, PanelStyle.WARN);
                c.hover(lx, y + 3, c.width(plus), 10, String.join(" | ", tips.subList(drawn, tips.size())));
            }
            c.hidden("bounty rewards", drawn, b.rewards().size());
        }
        // needs
        StringBuilder all = new StringBuilder();
        for (ExchangeView.NeedLine nl : needs)
            all.append(all.length() == 0 ? "" : " | ").append(name(nl.item())).append("  ").append(n(nl.delivered())).append('/').append(n(nl.count()));
        int lastX = 10;
        for (int k = 0; k < placed; k++) {
            ExchangeView.NeedLine nl = needs.get(k);
            String cnt = n(nl.delivered()) + "/" + n(nl.count());
            int ny = y + 16 + lineOf[k] * 18;
            c.item(nl.item(), xOf[k], ny);
            c.text(cnt, xOf[k] + 18, ny + 5, nl.delivered() >= nl.count() ? PanelStyle.up() : PanelStyle.TEXT);
            c.hover(xOf[k], ny, 18 + c.width(cnt), 16, name(nl.item()) + "  " + cnt);
            if (lineOf[k] == lines - 1) lastX = xOf[k] + 18 + c.width(cnt);
        }
        if (placed < needs.size()) {
            String plus = "+" + (needs.size() - placed);
            int ny = y + 16 + (lines - 1) * 18;
            int px = lastX + 6;
            c.text(plus, px, ny + 5, PanelStyle.WARN);
            c.hover(px, ny, c.width(plus), 16, all.toString());
            lastX = px + c.width(plus);
            c.hidden("bounty needs", placed, needs.size());
        }
        // deliver; this player's part at the right of the last needs line (under the button)
        final long id = b.id();
        button(buttonX, y + 2, 58, 13, tr("deliver"), () -> c.act(ExchangePackets.BOUNTY_DELIVER, "", id, 0, 0, 0, false, ""));
        c.hover(buttonX, y + 2, 58, 13, tr("bounty.deliver_help") + (all.length() > 0 ? " | " + all : ""));
        String mine = b.room() >= 0 ? tr("bounty.mine_room", n(b.mine()), n(b.room()), b.sharePermille() / 10)
            : b.mine() > 0 ? tr("bounty.mine", n(b.mine())) : "";
        if (!mine.isEmpty()) {
            int my = y + 16 + (lines - 1) * 18 + 5;
            String fit = fitText(mine, 6 + listW - 4 - (lastX + 6));
            if (!fit.isEmpty()) {
                right(fit, 6 + listW - 4, my, PanelStyle.DIM);
                c.hover(6 + listW - 4 - c.width(fit), my - 1, c.width(fit), 10, mine + " - " + tr("bounty.value_help"));
            }
        }
        return rowH;
    }

    String rewardAmount(ExchangeView.RewardLine r) {
        if (r.kind() == 1) return "x" + n(r.amount());
        return r.kind() == 2 ? tr("bounty.uu_amount", n(r.amount())) : tr("bounty.coin_amount", n(r.amount()));
    }

    int rewardWidth(ExchangeView.RewardLine r) {
        return (r.kind() == 1 ? 17 : 0) + c.width(rewardAmount(r));
    }
}
