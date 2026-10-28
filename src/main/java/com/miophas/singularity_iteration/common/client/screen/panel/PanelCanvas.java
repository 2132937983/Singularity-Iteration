// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.screen.panel;

import java.util.List;

/**
 * 0.1.10.33: what the exchange panel's pages ({@link FuturePanel}) need from the screen, without Minecraft types: text
 * measures and translations, drawing, widgets and the actions. The futures machine screen implements it with the game's
 * font and {@code GuiGraphics}; the offline layout harness implements it with the vanilla font's advances and the language
 * files, records every element and checks that none overlaps another or leaves the screen or its panel.
 *
 * <p>Elements: <b>containers</b> ({@link #panel}, {@link #strip}) hold the elements drawn after them; <b>backgrounds</b>
 * ({@link #fill}, {@link #outline}, {@link #candles}, {@link #line}, {@link #bar}) may lie under anything; everything else
 * (text 8 px high, items and slots 16 x 16, sparklines, buttons, number boxes 12 px high) is <b>foreground</b> and must not
 * overlap other foreground elements.
 */
public interface PanelCanvas {
    /**
     * The number boxes of the panel (the screen owns the edit widgets). 0.1.10.35: POST_DAYS holds the hours of a commission
     * (the name stays for the older layout harnesses); the shop boxes: goods and cost items per trade, coins per trade, trades
     * to stock (and to restock), trades to take (xN).
     */
    enum Box { PRICE, QTY, POST_QTY, POST_AMOUNT, POST_DAYS, FUT_QTY, MIN, COIN, SHOP_GOODS, SHOP_COST, SHOP_PRICE, SHOP_UNITS, SHOP_N }

    int screenWidth();

    int screenHeight();

    // ------------------------------------------------------------------ text

    int width(String text);

    /** The longest start of {@code text} not wider than {@code w}. */
    String fit(String text, int w);

    /** {@code text} broken into lines not wider than {@code w}. */
    List<String> wrap(String text, int w);

    /** A translated text of the panel ({@code key} is a full language key). */
    String tr(String key, Object... args);

    /** The name of an item id. */
    String itemName(String id);

    /** The player's main inventory: 36 item ids, "" for an empty slot. */
    List<String> inventory();

    // ------------------------------------------------------------------ drawing

    void fill(int x0, int y0, int x1, int y1, int colour);

    /** A fill above items (a lock shade over an icon). */
    void shade(int x0, int y0, int x1, int y1, int colour);

    void outline(int x, int y, int w, int h, int colour);

    /** A panel: background and frame; it contains the elements drawn after it. */
    void panel(int x, int y, int w, int h);

    /** A coloured strip without frame (header, status bar); it contains the elements drawn after it. */
    void strip(int x, int y, int w, int h, int colour);

    void text(String text, int x, int y, int colour);

    void item(String id, int x, int y);

    /** The frame of an inventory slot at the slot's item position. */
    void slot(int x, int y);

    void candles(int x, int y, int w, int h, long[] open, long[] high, long[] low, long[] close, long[] volume);

    void line(int x, int y, int w, int h, double[] values, long min, long max, int colour, boolean dashed);

    void sparkline(int x, int y, int w, int h, long[] values);

    void bar(int x, int y, int w, int h, double fraction, int colour, boolean right);

    // ------------------------------------------------------------------ widgets

    /** A button; a disabled one is drawn dim and does nothing. */
    void button(int x, int y, int w, int h, String label, boolean on, boolean enabled, Runnable click);

    /** A click area (left / right button; null = none). */
    void region(int x, int y, int w, int h, Runnable left, Runnable right);

    /** A tooltip while the mouse is over the area. */
    void hover(int x, int y, int w, int h, String text);

    /** A wheel area: {@code scroll} receives +1 (down) or -1 (up). */
    void wheel(int x, int y, int w, int h, java.util.function.IntConsumer scroll);

    /** Shows a number box at the place (12 px high). */
    void box(Box box, int x, int y, int w);

    long num(Box box, long fallback);

    void set(Box box, long value);

    void range(Box box, long min, long max);

    /** A +/- button of a box: steps it and gives it the focus. */
    void step(Box box, int steps);

    /** Notes for the layout harness: {@code shown} of {@code total} entries of a list fit (the rest scroll or are summed up). */
    default void hidden(String what, int shown, int total) { }

    /**
     * Notes for the layout harness: {@code full} did not fit; {@code shown} (a start of {@code from}: {@code full}, or
     * {@code full} with compact numbers) is drawn instead and the tooltip has {@code full}.
     */
    default void cut(String full, String from, String shown) { }

    // ------------------------------------------------------------------ actions

    /** Asks the server for a new view. */
    void request();

    void act(int kind, String id, long a, long b, int c, int d, boolean flag, String text);

    /** The page changed (the server learns whether the settlement page is shown). */
    void pageChanged(int from, int to);
}
