package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.client.screen.panel.FuturePanel;
import com.miophas.singularity_iteration.common.client.screen.panel.PanelCanvas;
import com.miophas.singularity_iteration.common.client.screen.panel.PanelCharts;
import com.miophas.singularity_iteration.common.exchange.ExchangeClientState;
import com.miophas.singularity_iteration.common.exchange.ExchangePackets;
import com.miophas.singularity_iteration.common.exchange.ExchangeService;
import com.miophas.singularity_iteration.common.exchange.CommissionHours;
import com.miophas.singularity_iteration.common.exchange.MinPrice;
import com.miophas.singularity_iteration.common.exchange.ShopRules;
import com.miophas.singularity_iteration.common.exchange.UuBoard;
import com.miophas.singularity_iteration.common.menu.producer.FutureElcMenu;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.function.IntConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
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
 * 0.1.10.21 (futures M4): the exchange panel of the futures machine, a full-screen data panel in eight tabs (market,
 * commissions, futures, orders and mail, treasury, sell page, bounties, settlement).
 *
 * <p>0.1.10.33: the pages are drawn by {@link FuturePanel} through {@link PanelCanvas}; this screen is the canvas (the game's
 * font, {@code GuiGraphics}, the number boxes, the click / wheel / tooltip areas) and keeps the slots, the packets and the
 * requests. The offline layout harness draws the same pages with the vanilla font's advances and checks them at eight sizes
 * in three languages (before, 19 places overlapped or left the screen at the small sizes).
 *
 * <p>The panel shows the {@link com.miophas.singularity_iteration.common.exchange.ExchangeView} that the server sends (on
 * request, once per second while open); every action is a packet that the server checks again.
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_future_elc extends mio_icif_screen<FutureElcMenu> implements PanelCanvas {
    private static final long MAX_COINS = 999_999_999L;

    private record Region(int x, int y, int w, int h, Runnable left, Runnable right) {
        boolean inside(double mx, double my) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    }

    private record Wheel(int x, int y, int w, int h, IntConsumer scroll) {
        boolean inside(double mx, double my) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    }

    /** The pages and their state (kept when the window is resized). */
    private final FuturePanel panel = new FuturePanel();
    private final List<Region> regions = new ArrayList<>();
    private final List<Wheel> wheels = new ArrayList<>();
    private final EnumMap<Box, NumberBox> boxes = new EnumMap<>(Box.class);
    private int requestTimer;
    private String hoverText;
    /** 0.1.10.29: a quote request waits for the next free tick (box and container changes come in bursts). */
    private boolean requestPending;
    // 0.1.10.25 (X1): the sell box as last requested (a change asks the server for a new quote)
    private String boxSeen = "";
    // 0.1.10.27 (X2): the settlement slot as last requested
    private String settleSeen = "";
    // while drawing
    private GuiGraphics g;
    private int mx, my;

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
        box(Box.PRICE, panel.pricePercent ? 0 : 1, panel.pricePercent ? MinPrice.MAX_PERCENT : MAX_COINS, 1);
        box(Box.QTY, 1, ExchangeService.MAX_QUANTITY, 1);
        box(Box.POST_QTY, 1, ExchangeService.MAX_QUANTITY, 64);
        box(Box.POST_AMOUNT, 1, MAX_COINS, 100);
        box(Box.POST_DAYS, 1, CommissionHours.MAX_HOURS, 24);   // 0.1.10.35: hours (the server clamps to its maximum)
        box(Box.FUT_QTY, 1, ExchangeService.FUTURES_MAX_QUANTITY, 16);
        box(Box.MIN, 0, panel.minCoins ? MAX_COINS : MinPrice.MAX_PERCENT, 0);
        box(Box.COIN, 0, MAX_COINS, 0);
        // 0.1.10.35: the shop form and the trades of an open shop
        box(Box.SHOP_GOODS, 1, ShopRules.MAX_COUNT, 1);
        box(Box.SHOP_COST, 1, ShopRules.MAX_COUNT, 1);
        box(Box.SHOP_PRICE, 1, ShopRules.MAX_PRICE, 10);
        box(Box.SHOP_UNITS, 1, ShopRules.MAX_UNITS, 16);
        box(Box.SHOP_N, 1, ShopRules.MAX_COUNT, 8);
        request();
    }

    private void box(Box which, long min, long max, long value) {
        NumberBox old = boxes.get(which);
        NumberBox b = new NumberBox(this.font, min, max, old == null ? value : old.value());
        addRenderableWidget(b);
        boxes.put(which, b);
    }

    /** The number of a box (0.1.10.29: clamped to its range), {@code fallback} when it is empty. */
    private static long num(EditBox b, long fallback) {
        if (b instanceof NumberBox n) return n.getValue().isEmpty() ? fallback : n.value();
        try { return b.getValue().isEmpty() ? fallback : Long.parseLong(b.getValue()); } catch (NumberFormatException e) { return fallback; }
    }

    // ------------------------------------------------------------------ network

    @Override
    public void request() {
        PacketDistributor.sendToServer(new ExchangePackets.Request(panel.selected, panel.shopQuery()));   // 0.1.10.35: + the shops
        requestTimer = 0;
        requestPending = false;
    }

    @Override
    public void act(int kind, String id, long a, long b, int c, int d, boolean flag, String text) {
        PacketDistributor.sendToServer(new ExchangePackets.Action(kind, id, a, b, c, d, flag, text, panel.selected));
    }

    /** 0.1.10.27: the server learns whether the settlement page is shown (shift-clicks go to its slot). */
    @Override
    public void pageChanged(int from, int to) {
        if ((from == 7) != (to == 7) && minecraft != null && minecraft.gameMode != null)
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, to == 7 ? FutureElcMenu.BUTTON_SETTLE_PAGE : FutureElcMenu.BUTTON_OTHER_PAGE);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        // 0.1.10.25: a new quote as soon as the sell box changes
        if (panel.tab == 5) {
            StringBuilder seen = new StringBuilder();
            for (int i = 0; i < FutureElcMenu.SELL_BOX_SIZE; i++) {
                ItemStack s = menu.sellBox().getItem(i);
                if (!s.isEmpty()) seen.append(i).append(':').append(BuiltInRegistries.ITEM.getKey(s.getItem())).append('x').append(s.getCount()).append(';');
            }
            if (!seen.toString().equals(boxSeen)) { boxSeen = seen.toString(); requestPending = true; }
        }
        if (panel.tab == 7) {
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
        for (EditBox b : boxes.values()) {
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
        return (panel.tab == 5 || panel.tab == 7) && super.mouseClicked(mx, my, button);
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
        for (NumberBox b : boxes.values()) if (b.isVisible() && b.mouseScrolled(mx, my, sx, sy)) return true;
        // 0.1.10.33: the wheel areas of the pages (lists, the inventory picker, the tabs on a narrow screen)
        if (sy != 0) {
            for (int i = wheels.size() - 1; i >= 0; i--) {
                Wheel w = wheels.get(i);
                if (w.inside(mx, my)) { w.scroll().accept(sy > 0 ? -1 : 1); return true; }
            }
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    // ------------------------------------------------------------------ render

    /**
     * 0.1.10.25: the whole panel is drawn as the background, so the vanilla screen draws the widgets, the slots of the sell
     * page, the carried stack and the item tooltips on top of it (before, the panel drew everything itself and had no slots).
     */
    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mx, int my) {
        this.g = g;
        this.mx = mx;
        this.my = my;
        panel.render(this, ExchangeClientState.view());
        this.g = null;
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) { }

    @Override
    protected int statusLampColor() { return 0; }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        regions.clear();
        wheels.clear();
        hoverText = null;
        for (EditBox b : boxes.values()) b.setVisible(false);
        menu.sellView = panel.tab == 5;
        menu.settleView = panel.tab == 7;
        layoutSellSlots();
        super.render(g, mx, my, pt);
        if (hoverText != null) g.renderTooltip(font, Component.literal(hoverText), mx, my);
    }

    /**
     * Places the sell box and inventory slots for the window (the slot positions are mutable through the mod's access
     * transformer: the panel is the whole window and changes with it). Off the sell page they are inactive.
     */
    private void layoutSellSlots() {
        for (int i = 0; i < FutureElcMenu.SELL_BOX_SIZE; i++) {
            Slot slot = menu.slots.get(FutureElcMenu.SELL_BOX_FIRST + i);
            slot.x = FuturePanel.boxSlotX(i);
            slot.y = FuturePanel.boxSlotY(i);
        }
        for (int i = 0; i < 36; i++) {
            Slot slot = menu.slots.get(FutureElcMenu.INVENTORY_FIRST + i);
            slot.x = FuturePanel.invSlotX(i);
            slot.y = FuturePanel.invSlotY(i);
        }
        Slot settle = menu.slots.get(FutureElcMenu.SETTLEMENT_SLOT);
        settle.x = FuturePanel.BOX_LEFT;
        settle.y = FuturePanel.boxTop();
    }

    // ------------------------------------------------------------------ PanelCanvas: text

    @Override
    public int screenWidth() { return width; }

    @Override
    public int screenHeight() { return height; }

    @Override
    public int width(String text) { return font.width(text); }

    @Override
    public String fit(String text, int w) { return w <= 0 ? "" : font.plainSubstrByWidth(text, w); }

    @Override
    public List<String> wrap(String text, int w) {
        return font.getSplitter().splitLines(text, Math.max(1, w), Style.EMPTY).stream().map(FormattedText::getString).toList();
    }

    @Override
    public String tr(String key, Object... args) { return Component.translatable(key, args).getString(); }

    private static Item item(String id) {
        // 0.1.10.28 (X4): the coin <-> UU board shows the UU-matter bucket
        if (UuBoard.is(id)) return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "uumatter_bucket"));
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl == null || !BuiltInRegistries.ITEM.containsKey(rl) ? Items.BARRIER : BuiltInRegistries.ITEM.get(rl);
    }

    @Override
    public String itemName(String id) { return new ItemStack(item(id)).getHoverName().getString(); }

    @Override
    public List<String> inventory() {
        List<String> ids = new ArrayList<>(36);
        if (minecraft == null || minecraft.player == null) return ids;
        var inv = minecraft.player.getInventory();
        for (int s = 0; s < 36; s++) {
            ItemStack stack = inv.items.get(s);
            ids.add(stack.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        }
        return ids;
    }

    // ------------------------------------------------------------------ PanelCanvas: drawing

    @Override
    public void fill(int x0, int y0, int x1, int y1, int colour) { g.fill(x0, y0, x1, y1, colour); }

    @Override
    public void shade(int x0, int y0, int x1, int y1, int colour) {
        g.pose().pushPose();
        g.pose().translate(0, 0, 200);
        g.fill(x0, y0, x1, y1, colour);
        g.pose().popPose();
    }

    @Override
    public void outline(int x, int y, int w, int h, int colour) { g.renderOutline(x, y, w, h, colour); }

    @Override
    public void panel(int x, int y, int w, int h) { PanelCharts.panel(g, x, y, w, h); }

    @Override
    public void strip(int x, int y, int w, int h, int colour) { g.fill(x, y, x + w, y + h, colour); }

    @Override
    public void text(String text, int x, int y, int colour) { g.drawString(font, text, x, y, colour, false); }

    @Override
    public void item(String id, int x, int y) { g.renderItem(new ItemStack(item(id)), x, y); }

    @Override
    public void slot(int x, int y) {
        g.fill(x - 1, y - 1, x + 17, y + 17, PanelCharts.PANEL_LINE);
        g.fill(x, y, x + 16, y + 16, 0xFF0D1117);
    }

    @Override
    public void candles(int x, int y, int w, int h, long[] open, long[] high, long[] low, long[] close, long[] volume) {
        PanelCharts.candles(g, x, y, w, h, open, high, low, close, volume, 0, 0);
    }

    @Override
    public void line(int x, int y, int w, int h, double[] values, long min, long max, int colour, boolean dashed) {
        PanelCharts.line(g, x, y, w, h, values, min, max, colour, dashed);
    }

    @Override
    public void sparkline(int x, int y, int w, int h, long[] values) { PanelCharts.sparkline(g, x, y, w, h, values); }

    @Override
    public void bar(int x, int y, int w, int h, double fraction, int colour, boolean right) { PanelCharts.bar(g, x, y, w, h, fraction, colour, right); }

    // ------------------------------------------------------------------ PanelCanvas: widgets

    @Override
    public void button(int x, int y, int w, int h, String label, boolean on, boolean enabled, Runnable click) {
        boolean hot = enabled && mx >= x && mx < x + w && my >= y && my < y + h;
        g.fill(x, y, x + w, y + h, on ? 0xFF2F81F7 : hot ? 0xFF2D3748 : enabled ? 0xFF1F2630 : 0xFF161B22);
        g.renderOutline(x, y, w, h, on ? 0xFF58A6FF : PanelCharts.PANEL_LINE);
        String text = label;
        if (font.width(text) > w - 4) text = font.plainSubstrByWidth(text, w - 6);
        g.drawString(font, text, x + (w - font.width(text)) / 2, y + (h - 8) / 2, enabled ? PanelCharts.TEXT : PanelCharts.DIM, false);
        if (!text.equals(label)) hover(x, y, w, h, label);
        if (enabled) regions.add(new Region(x, y, w, h, click, null));
    }

    @Override
    public void region(int x, int y, int w, int h, Runnable left, Runnable right) { regions.add(new Region(x, y, w, h, left, right)); }

    @Override
    public void hover(int x, int y, int w, int h, String text) {
        if (mx >= x && mx < x + w && my >= y && my < y + h) hoverText = text;
    }

    @Override
    public void wheel(int x, int y, int w, int h, IntConsumer scroll) { wheels.add(new Wheel(x, y, w, h, scroll)); }

    @Override
    public void box(Box which, int x, int y, int w) {
        NumberBox b = boxes.get(which);
        b.setX(x);
        b.setY(y);
        b.setWidth(w);
        b.setVisible(true);
    }

    @Override
    public long num(Box which, long fallback) {
        NumberBox b = boxes.get(which);
        return b == null ? fallback : num(b, fallback);
    }

    @Override
    public void set(Box which, long value) {
        NumberBox b = boxes.get(which);
        if (b != null) b.set(value);
    }

    @Override
    public void range(Box which, long min, long max) {
        NumberBox b = boxes.get(which);
        if (b != null) b.setRange(min, max);
    }

    /** 0.1.10.29: a +/- button: steps the box and gives it the focus with the number selected (typing replaces it). */
    @Override
    public void step(Box which, int steps) {
        NumberBox b = boxes.get(which);
        if (b == null) return;
        b.step(steps);
        setFocused(b);
        b.selectAll();
    }
}
