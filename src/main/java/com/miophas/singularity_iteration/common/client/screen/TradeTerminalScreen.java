// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.client.screen.panel.PanelCharts;
import com.miophas.singularity_iteration.common.exchange.CardView;
import com.miophas.singularity_iteration.common.exchange.ExchangeView;
import com.miophas.singularity_iteration.common.exchange.TerminalClientState;
import com.miophas.singularity_iteration.common.exchange.TerminalOrders;
import com.miophas.singularity_iteration.common.exchange.TerminalPackets;
import com.miophas.singularity_iteration.common.exchange.TerminalRules;
import com.miophas.singularity_iteration.common.exchange.TerminalView;
import com.miophas.singularity_iteration.common.future.FutureCommodity;
import com.miophas.singularity_iteration.common.future.FutureCommodityManager;
import com.miophas.singularity_iteration.common.menu.producer.TradeTerminalMenu;
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
 * 0.1.10.27 (X2): the Trade Terminal panel in five pages: <b>Buffer</b> (sell buffer, output buffer, the settlement slot with
 * its actions, the inventory), <b>Orders</b> (the standing orders and an editor), <b>Access</b> (the access list and the team
 * level; only the owner edits), <b>Log</b> (the trades of the orders) and <b>Settlement</b> (the settlement slot as on the
 * futures panel: quote, sell, deliveries to bounties and commissions, UU-matter, purchases into the container). It shows the {@link TerminalView} that the server
 * sends; every action is a packet that the server checks again.
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class TradeTerminalScreen extends mio_icif_screen<TradeTerminalMenu> {
    private static final String K = "gui.mio_icif.terminal.";
    private static final int W = 320, H = 222, TABS = 6, COL_X = 176, SETTLE_PAGE_X = 8, SETTLE_PAGE_Y = 34;

    private record Region(int x, int y, int w, int h, Runnable click) {
        boolean inside(double mx, double my) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    }

    private final List<Region> regions = new ArrayList<>();
    private int tab;
    private int requestTimer;
    private String hoverText;
    private String settleSeen = "";
    // order editor
    private int editing = -1, commodity;
    private boolean editBuy, editEnabled = true;
    /** The sell mode of the editor: 0 auction, 1 instant, 2 instant with the scrap sink (0.1.10.28). */
    private int editMode;
    // 0.1.10.28 (X4): the scrap sink of the settlement sale; the ID card form
    private boolean scrapRest;
    private int cardLevel = TerminalRules.TRADE;
    private EditBox cardDays;
    // access editor
    private int newLevel = TerminalRules.VIEW;
    // settlement page: the commodity to buy into the container
    private int buyCommodity;
    private EditBox amountBox, limitBox, nameBox, minBox, buyQty, buyLimit;

    public TradeTerminalScreen(TradeTerminalMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = W;
        this.imageHeight = H;
    }

    @Override
    protected void init() {
        super.init();
        amountBox = box("64", 7, true);
        limitBox = box("0", 9, true);
        nameBox = box("", 16, false);
        minBox = box("0", 3, true);
        buyQty = box("64", 6, true);
        cardDays = box("7", 2, true);
        buyLimit = box("0", 9, true);
        request();
    }

    private EditBox box(String value, int length, boolean digits) {
        EditBox b = new EditBox(font, 0, 0, 40, 12, Component.empty());
        b.setMaxLength(length);
        if (digits) b.setFilter(s -> s.chars().allMatch(Character::isDigit));
        b.setValue(value);
        addRenderableWidget(b);
        return b;
    }

    private static long num(EditBox b, long fallback) {
        try { return b.getValue().isEmpty() ? fallback : Long.parseLong(b.getValue()); } catch (NumberFormatException e) { return fallback; }
    }

    // ------------------------------------------------------------------ network

    private void request() {
        PacketDistributor.sendToServer(new TerminalPackets.Request());
        requestTimer = 0;
    }

    private void act(int kind, int c, String text, long a, long b, int flags) {
        PacketDistributor.sendToServer(new TerminalPackets.TAction(kind, c, text, a, b, flags));
    }

    @Override
    public void containerTick() {
        super.containerTick();
        ItemStack s = menu.slots.get(TradeTerminalMenu.SETTLE).getItem();
        String seen = s.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(s.getItem()) + "#" + s.getComponents().hashCode();
        if (!seen.equals(settleSeen)) { settleSeen = seen; request(); }
        if (++requestTimer >= 20) request();
    }

    @Override
    public void removed() {
        super.removed();
        TerminalClientState.clear();
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for (EditBox b : List.of(amountBox, limitBox, nameBox, minBox, buyQty, buyLimit, cardDays)) {
            if (b.isVisible() && b.mouseClicked(mx, my, button)) { setFocused(b); return true; }
        }
        if (getFocused() instanceof EditBox b) { b.setFocused(false); setFocused(null); }
        if (button == 0) {
            for (int i = regions.size() - 1; i >= 0; i--) {
                Region r = regions.get(i);
                if (r.inside(mx, my)) { r.click().run(); return true; }
            }
        }
        return (tab == 0 || tab == 4) && super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (getFocused() instanceof EditBox b && b.isVisible() && key != GLFW.GLFW_KEY_ESCAPE) return b.keyPressed(key, scan, modifiers) || true;
        return super.keyPressed(key, scan, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (tab == 1 || tab == 4) {
            List<FutureCommodity> all = FutureCommodityManager.getCommodities();
            if (tab == 1 && !all.isEmpty()) commodity = Math.floorMod(commodity - (int) Math.signum(sy), all.size());
            if (tab == 4 && !all.isEmpty()) buyCommodity = Math.floorMod(buyCommodity - (int) Math.signum(sy), all.size());
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    private void button(GuiGraphics g, int x, int y, int w, int h, Component label, boolean on, boolean enabled, Runnable click, int mx, int my) {
        int ax = leftPos + x, ay = topPos + y;
        boolean hot = enabled && mx >= ax && mx < ax + w && my >= ay && my < ay + h;
        g.fill(ax, ay, ax + w, ay + h, !enabled ? 0xFF161B22 : on ? 0xFF2F81F7 : hot ? 0xFF2D3748 : 0xFF1F2630);
        g.renderOutline(ax, ay, w, h, on ? 0xFF58A6FF : PanelCharts.PANEL_LINE);
        String text = label.getString();
        if (font.width(text) > w - 4) text = font.plainSubstrByWidth(text, w - 6);
        g.drawString(font, text, ax + (w - font.width(text)) / 2, ay + (h - 8) / 2, enabled ? PanelCharts.TEXT : PanelCharts.DIM, false);
        if (enabled) regions.add(new Region(ax, ay, w, h, click));
    }

    private void text(GuiGraphics g, Component c, int x, int y, int colour) { g.drawString(font, c, leftPos + x, topPos + y, colour, false); }

    private void text(GuiGraphics g, String s, int x, int y, int colour) { g.drawString(font, s, leftPos + x, topPos + y, colour, false); }

    private void hover(int x, int y, int w, int h, String text, int mx, int my) {
        int ax = leftPos + x, ay = topPos + y;
        if (mx >= ax && mx < ax + w && my >= ay && my < ay + h) hoverText = text;
    }

    private void place(EditBox b, int x, int y, int w) {
        b.setX(leftPos + x); b.setY(topPos + y); b.setWidth(w); b.setVisible(true);
    }

    private static Item item(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl == null || !BuiltInRegistries.ITEM.containsKey(rl) ? Items.BARRIER : BuiltInRegistries.ITEM.get(rl);
    }

    private static String name(String id) { return new ItemStack(item(id)).getHoverName().getString(); }

    private Component levelName(int level) { return Component.translatable(K + "level." + Math.max(0, Math.min(4, level))); }

    // ------------------------------------------------------------------ render

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mx, int my) {
        g.fill(leftPos, topPos, leftPos + W, topPos + H, 0xF00D1117);
        g.renderOutline(leftPos, topPos, W, H, PanelCharts.PANEL_LINE);
        TerminalView v = TerminalClientState.view();
        header(g, v, mx, my);
        if (v == null) {
            g.drawCenteredString(font, Component.translatable(K + "loading"), leftPos + W / 2, topPos + H / 2, PanelCharts.DIM);
            return;
        }
        switch (tab) {
            case 0 -> buffer(g, v, mx, my);
            case 1 -> orders(g, v, mx, my);
            case 2 -> access(g, v, mx, my);
            case 3 -> log(g, v);
            case 4 -> settlement(g, v, mx, my);
            default -> cards(g, v, mx, my);
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
        for (EditBox b : List.of(amountBox, limitBox, nameBox, minBox, buyQty, buyLimit, cardDays)) b.setVisible(false);
        menu.bufferView = tab == 0;
        menu.settleView = tab == 4;
        // the settlement slot: in the right column of the buffer page, top left on the settlement page
        var settle = menu.slots.get(TradeTerminalMenu.SETTLE);
        settle.x = tab == 4 ? SETTLE_PAGE_X : TradeTerminalMenu.SETTLE_X;
        settle.y = tab == 4 ? SETTLE_PAGE_Y : TradeTerminalMenu.SETTLE_Y;
        super.render(g, mx, my, pt);
        if (hoverText != null) g.renderTooltip(font, font.split(Component.literal(hoverText), 220), mx, my);
    }

    private void header(GuiGraphics g, TerminalView v, int mx, int my) {
        g.fill(leftPos, topPos, leftPos + W, topPos + 20, 0xFF161B22);
        int x = 4;
        for (int i = 0; i < TABS; i++) {
            Component label = Component.translatable(K + "tab." + i);
            int w = font.width(label) + 10;
            final int t = i;
            button(g, x, 3, w, 14, label, tab == i, true, () -> { tab = t; request(); }, mx, my);
            x += w + 2;
        }
        if (v != null) {
            String right = v.owner().isEmpty() ? Component.translatable(K + "no_owner").getString()
                : Component.translatable(K + "owner", v.owner()).getString();
            int colour = v.owner().isEmpty() ? PanelCharts.down() : v.ownerOnline() ? PanelCharts.up() : PanelCharts.WARN;
            PanelCharts.right(g, font, font.plainSubstrByWidth(right, W - x - 8), leftPos + W - 5, topPos + 6, colour);
            hover(x, 3, W - x - 4, 14, Component.translatable(K + "owner_help", levelName(v.level()), PanelCharts.number(v.energy()),
                PanelCharts.number(v.maxEnergy())).getString(), mx, my);
        }
    }

    private void slotFrame(GuiGraphics g, int x, int y) {
        int ax = leftPos + x, ay = topPos + y;
        g.fill(ax - 1, ay - 1, ax + 17, ay + 17, PanelCharts.PANEL_LINE);
        g.fill(ax, ay, ax + 16, ay + 16, 0xFF0D1117);
    }

    // ------------------------------------------------------------------ buffer page

    private void buffer(GuiGraphics g, TerminalView v, int mx, int my) {
        boolean trade = TerminalRules.may(v.level(), TerminalRules.Action.SETTLE);
        boolean config = TerminalRules.may(v.level(), TerminalRules.Action.EDIT_ORDERS);
        text(g, Component.translatable(K + "sell_buffer"), TradeTerminalMenu.SELL_X, 24, PanelCharts.ACCENT);
        hover(TradeTerminalMenu.SELL_X, 22, 160, 10, Component.translatable(K + "sell_buffer_help").getString(), mx, my);
        for (int i = 0; i < TerminalRules.SELL_SLOTS; i++) slotFrame(g, TradeTerminalMenu.SELL_X + (i % 9) * 18, TradeTerminalMenu.SELL_Y + (i / 9) * 18);
        text(g, Component.translatable(K + "output_buffer"), TradeTerminalMenu.SELL_X, TradeTerminalMenu.OUTPUT_Y - 10, PanelCharts.ACCENT);
        for (int i = 0; i < TerminalRules.OUTPUT_SLOTS; i++) slotFrame(g, TradeTerminalMenu.SELL_X + i * 18, TradeTerminalMenu.OUTPUT_Y);
        text(g, Component.translatable("container.inventory"), TradeTerminalMenu.SELL_X, TradeTerminalMenu.INV_Y - 10, PanelCharts.DIM);
        for (int i = 0; i < 36; i++)
            slotFrame(g, TradeTerminalMenu.SELL_X + (i % 9) * 18, i < 27 ? TradeTerminalMenu.INV_Y + (i / 9) * 18 : TradeTerminalMenu.HOTBAR_Y);

        // settlement column
        int x = COL_X, w = W - COL_X - 6;
        text(g, Component.translatable(K + "settle"), x, 24, PanelCharts.ACCENT);
        slotFrame(g, TradeTerminalMenu.SETTLE_X, TradeTerminalMenu.SETTLE_Y);
        ExchangeView.SettleView st = v.settle();
        long now = 0, auc = 0, items = 0;
        for (ExchangeView.BoxLine l : st.lines()) { now += l.nowCoins(); auc += l.auctionCoins(); items += l.quantity(); }
        Component kind = Component.translatable("gui.mio_icif.exchange.settle.kind." + Math.max(0, Math.min(3, st.kind())));
        g.drawString(font, font.plainSubstrByWidth(kind.getString(), W - TradeTerminalMenu.SETTLE_X - 26), leftPos + TradeTerminalMenu.SETTLE_X + 20,
            topPos + TradeTerminalMenu.SETTLE_Y + 4, PanelCharts.DIM, false);
        hover(x, 22, w, 32, Component.translatable(K + "settle_help").getString(), mx, my);
        int y = 56;
        text(g, font.plainSubstrByWidth(Component.translatable(K + "settle_quote", PanelCharts.number(items), PanelCharts.number(now),
            PanelCharts.number(auc)).getString(), w), x, y, PanelCharts.TEXT);
        hover(x, y - 1, w, 10, Component.translatable(K + "settle_quote_help").getString(), mx, my);
        y += 11;
        text(g, font.plainSubstrByWidth(Component.translatable(K + "settle_uu", PanelCharts.number(st.uu())).getString(), w), x, y, PanelCharts.DIM);
        y += 12;
        int half = (w - 2) / 2;
        button(g, x, y, half, 13, Component.translatable(K + "sell_now"), false, trade,
            () -> act(TerminalPackets.SETTLE_SELL, 0, "", 0, 0, scrapRest ? 2 : 0), mx, my);
        button(g, x + half + 2, y, half, 13, Component.translatable(K + "sell_auction"), false, trade,
            () -> act(TerminalPackets.SETTLE_SELL, 0, "", Math.min(100, num(minBox, 0)), 0, 1), mx, my);
        y += 15;
        text(g, Component.translatable(K + "min_percent"), x, y + 2, PanelCharts.DIM);
        place(minBox, x + w - 30, y, 30);
        scrapToggle(g, x + w - 30 - 4 - 64, y, mx, my);
        y += 15;
        button(g, x, y, w, 13, Component.translatable(K + "uu_in"), false, trade, () -> act(TerminalPackets.SETTLE_UU, 0, "", 0, 0, 0), mx, my);
        y += 15;
        button(g, x, y, w, 13, Component.translatable(K + "load"), false, trade, () -> act(TerminalPackets.SETTLE_LOAD, 0, "", 0, 0, 0), mx, my);
        hover(x, y, w, 13, Component.translatable(K + "load_help").getString(), mx, my);
        y += 15;
        button(g, x, y, w, 13, Component.translatable(K + "unload"), false, config, () -> act(TerminalPackets.SETTLE_UNLOAD, 0, "", 0, 0, 0), mx, my);
        hover(x, y, w, 13, Component.translatable(K + "unload_help").getString(), mx, my);
        // the terminal itself
        y = TradeTerminalMenu.INV_Y - 10;
        text(g, Component.translatable(K + "terminal"), x, y, PanelCharts.ACCENT);
        y += 11;
        button(g, x, y, w, 13, Component.translatable(K + (v.outputMode() ? "output_on" : "output_off")), v.outputMode(), config,
            () -> act(TerminalPackets.OUTPUT_MODE, 0, "", 0, 0, v.outputMode() ? 0 : 1), mx, my);
        hover(x, y, w, 13, Component.translatable(K + "output_help").getString(), mx, my);
        y += 15;
        button(g, x, y, w, 13, Component.translatable(K + "run_now"), false, config, () -> act(TerminalPackets.RUN_NOW, 0, "", 0, 0, 0), mx, my);
        y += 16;
        if (v.ownerCoins() >= 0) text(g, font.plainSubstrByWidth(Component.translatable(K + "owner_coins", PanelCharts.number(v.ownerCoins())).getString(), w), x, y, PanelCharts.TEXT);
        y += 11;
        text(g, font.plainSubstrByWidth(Component.translatable(K + "energy", PanelCharts.number(v.energy()), PanelCharts.number(v.maxEnergy())).getString(), w),
            x, y, v.energyOk() ? PanelCharts.DIM : PanelCharts.down());
        y += 11;
        text(g, font.plainSubstrByWidth(Component.translatable(K + "your_level", levelName(v.level())).getString(), w), x, y, PanelCharts.DIM);
    }

    // ------------------------------------------------------------------ orders page

    private void orders(GuiGraphics g, TerminalView v, int mx, int my) {
        boolean config = TerminalRules.may(v.level(), TerminalRules.Action.EDIT_ORDERS);
        text(g, Component.translatable(K + "orders", v.orders().size(), TerminalRules.MAX_ORDERS), 6, 24, PanelCharts.ACCENT);
        hover(6, 22, 200, 10, Component.translatable(K + "orders_help").getString(), mx, my);
        int y = 36;
        if (v.orders().isEmpty()) text(g, Component.translatable(K + "no_orders"), 8, y, PanelCharts.DIM);
        for (int i = 0; i < v.orders().size(); i++) {
            TerminalView.OrderLine o = v.orders().get(i);
            if (i % 2 == 0) g.fill(leftPos + 4, topPos + y - 2, leftPos + W - 4, topPos + y + 13, 0x10FFFFFF);
            g.renderItem(new ItemStack(item(o.item())), leftPos + 6, topPos + y - 2);
            boolean buy = o.side() == 1;
            String what = buy
                ? Component.translatable(K + "order_buy", PanelCharts.number(o.amount()), PanelCharts.number(o.limit())).getString()
                : Component.translatable(o.mode() == 0 ? K + "order_sell_auction" : o.mode() == 2 ? K + "order_sell_scrap" : K + "order_sell_now",
                    PanelCharts.number(o.amount()), o.limit()).getString();
            text(g, font.plainSubstrByWidth(what, 128), 26, y + 2, o.enabled() ? PanelCharts.TEXT : PanelCharts.DIM);
            hover(4, y - 2, 150, 15, name(o.item()) + " - " + what, mx, my);
            String status = o.status() < 0 ? "" : Component.translatable(K + "status." + o.status()).getString();
            int sc = o.status() == 0 ? PanelCharts.up() : o.status() == 1 || o.status() == 2 ? PanelCharts.DIM : PanelCharts.WARN;
            text(g, font.plainSubstrByWidth(status, 62), 158, y + 2, sc);
            if (o.status() >= 0) hover(156, y - 2, 64, 15, Component.translatable(K + "status_help." + o.status()).getString(), mx, my);
            final int index = i;
            button(g, 222, y, 16, 12, Component.literal(o.enabled() ? "\u25CF" : "\u25CB"), o.enabled(), config,
                () -> act(TerminalPackets.ORDER_TOGGLE, index, "", 0, 0, 0), mx, my);
            button(g, 240, y, 18, 12, Component.translatable(K + "edit"), editing == index, config, () -> edit(v, index), mx, my);
            button(g, 260, y, 14, 12, Component.literal("x"), false, config, () -> {
                act(TerminalPackets.ORDER_REMOVE, index, "", 0, 0, 0);
                if (editing == index) editing = -1;
            }, mx, my);
            y += 15;
        }
        // editor
        int ey = 160;
        g.fill(leftPos + 4, topPos + ey - 4, leftPos + W - 4, topPos + ey - 3, PanelCharts.PANEL_LINE);
        text(g, Component.translatable(editing < 0 ? K + "new_order" : K + "edit_order", editing + 1), 6, ey, PanelCharts.ACCENT);
        ey += 12;
        button(g, 6, ey, 40, 14, Component.translatable(K + (editBuy ? "buy" : "sell")), editBuy, config, () -> editBuy = !editBuy, mx, my);
        button(g, 48, ey, 56, 14, Component.translatable(K + (editMode == 2 ? "scrap" : editMode == 1 ? "instant" : "auction")), editMode > 0,
            config && !editBuy, () -> editMode = (editMode + 1) % 3, mx, my);
        hover(48, ey, 56, 14, Component.translatable(K + "mode_help").getString(), mx, my);
        List<FutureCommodity> all = FutureCommodityManager.getCommodities();
        String id = all.isEmpty() ? "" : all.get(Math.floorMod(commodity, all.size())).getItemId();
        button(g, 108, ey, 12, 14, Component.literal("<"), false, config && !all.isEmpty(), () -> commodity--, mx, my);
        if (!id.isEmpty()) g.renderItem(new ItemStack(item(id)), leftPos + 123, topPos + ey - 1);
        text(g, font.plainSubstrByWidth(id.isEmpty() ? "-" : name(id), 112), 142, ey + 3, PanelCharts.TEXT);
        hover(122, ey, 136, 14, Component.translatable(K + "item_help").getString(), mx, my);
        button(g, 260, ey, 14, 14, Component.literal(">"), false, config && !all.isEmpty(), () -> commodity++, mx, my);
        ey += 18;
        text(g, Component.translatable(K + (editBuy ? "target" : "keep")), 6, ey + 2, PanelCharts.DIM);
        place(amountBox, 50, ey, 46);
        text(g, Component.translatable(K + (editBuy ? "max_price" : editMode > 0 ? "min_price" : "min_percent")), 102, ey + 2, PanelCharts.DIM);
        place(limitBox, 168, ey, 46);
        button(g, 218, ey, 56, 13, Component.translatable(K + (editEnabled ? "enabled" : "disabled")), editEnabled, config,
            () -> editEnabled = !editEnabled, mx, my);
        ey += 17;
        button(g, 6, ey, 80, 14, Component.translatable(K + "save"), false, config && !id.isEmpty(), () -> {
            int flags = (editBuy ? 1 : 0) | (editMode == 1 && !editBuy ? 2 : 0) | (editMode == 2 && !editBuy ? 8 : 0) | (editEnabled ? 4 : 0);
            act(TerminalPackets.ORDER_SET, editing, id, num(amountBox, 0), num(limitBox, 0), flags);
            editing = -1;
        }, mx, my);
        button(g, 90, ey, 60, 14, Component.translatable(K + "new"), false, config, () -> editing = -1, mx, my);
        if (v.ownerCoins() >= 0)
            PanelCharts.right(g, font, Component.translatable(K + "owner_coins", PanelCharts.number(v.ownerCoins())).getString(), leftPos + W - 6, topPos + ey + 3, PanelCharts.DIM);
    }

    private void edit(TerminalView v, int index) {
        if (index >= v.orders().size()) return;
        TerminalView.OrderLine o = v.orders().get(index);
        editing = index;
        editBuy = o.side() == 1;
        editMode = Math.max(0, Math.min(2, o.mode()));
        editEnabled = o.enabled();
        amountBox.setValue(Long.toString(o.amount()));
        limitBox.setValue(Long.toString(o.limit()));
        List<FutureCommodity> all = FutureCommodityManager.getCommodities();
        for (int i = 0; i < all.size(); i++) if (all.get(i).getItemId().equals(o.item())) commodity = i;
    }

    // ------------------------------------------------------------------ access page

    private void access(GuiGraphics g, TerminalView v, int mx, int my) {
        boolean owner = TerminalRules.may(v.level(), TerminalRules.Action.EDIT_ACCESS);
        text(g, Component.translatable(K + "access", v.access().size(), TerminalRules.MAX_ACCESS), 6, 24, PanelCharts.ACCENT);
        hover(6, 22, 200, 10, Component.translatable(K + "access_help").getString(), mx, my);
        int y = 36;
        if (v.access().isEmpty()) text(g, Component.translatable(K + "no_access"), 8, y, PanelCharts.DIM);
        for (int i = 0; i < v.access().size(); i++) {
            TerminalView.AccessLine a = v.access().get(i);
            int x = i < 8 ? 6 : 142, ry = y + (i % 8) * 14;
            text(g, font.plainSubstrByWidth(a.name(), 64), x, ry + 2, PanelCharts.TEXT);
            final String who = a.name();
            final int next = a.level() >= TerminalRules.CONFIG ? TerminalRules.VIEW : a.level() + 1;
            button(g, x + 68, ry, 46, 12, levelName(a.level()), false, owner, () -> act(TerminalPackets.ACCESS_SET, 0, who, next, 0, 0), mx, my);
            button(g, x + 116, ry, 14, 12, Component.literal("x"), false, owner, () -> act(TerminalPackets.ACCESS_SET, 0, who, 0, 0, 0), mx, my);
        }
        int ey = 156;
        g.fill(leftPos + 4, topPos + ey - 4, leftPos + W - 4, topPos + ey - 3, PanelCharts.PANEL_LINE);
        text(g, Component.translatable(K + "add_player"), 6, ey + 2, PanelCharts.DIM);
        place(nameBox, 70, ey, 90);
        final int lv = newLevel;
        button(g, 164, ey, 50, 13, levelName(lv), false, owner, () -> newLevel = lv >= TerminalRules.CONFIG ? TerminalRules.VIEW : lv + 1, mx, my);
        button(g, 218, ey, 56, 13, Component.translatable(K + "add"), false, owner && !nameBox.getValue().isBlank(), () -> {
            act(TerminalPackets.ACCESS_SET, 0, nameBox.getValue().trim(), lv, 0, 0);
            nameBox.setValue("");
        }, mx, my);
        ey += 18;
        text(g, Component.translatable(K + "team_level"), 6, ey + 2, PanelCharts.DIM);
        final int team = v.teamLevel();
        button(g, 164, ey, 50, 13, levelName(team), team > 0, owner,
            () -> act(TerminalPackets.TEAM_LEVEL, 0, "", team >= TerminalRules.CONFIG ? TerminalRules.NONE : team + 1, 0, 0), mx, my);
        hover(6, ey, 156, 13, Component.translatable(K + "team_help").getString(), mx, my);
        ey += 18;
        for (var line : font.split(Component.translatable(K + "levels_help"), W - 12)) {
            if (ey > H - 10) break;
            g.drawString(font, line, leftPos + 6, topPos + ey, PanelCharts.DIM, false);
            ey += 10;
        }
    }

    // ------------------------------------------------------------------ settlement page

    private void settlement(GuiGraphics g, TerminalView v, int mx, int my) {
        boolean trade = TerminalRules.may(v.level(), TerminalRules.Action.SETTLE);
        ExchangeView.SettleView st = v.settle();
        text(g, Component.translatable(K + "settle"), SETTLE_PAGE_X, 24, PanelCharts.ACCENT);
        hover(SETTLE_PAGE_X, 22, 130, 10, Component.translatable("gui.mio_icif.exchange.settle.help").getString(), mx, my);
        slotFrame(g, SETTLE_PAGE_X, SETTLE_PAGE_Y);
        Component kind = Component.translatable("gui.mio_icif.exchange.settle.kind." + Math.max(0, Math.min(3, st.kind())));
        text(g, font.plainSubstrByWidth(kind.getString(), 112), SETTLE_PAGE_X + 20, SETTLE_PAGE_Y + 4, st.kind() == 0 ? PanelCharts.DIM : PanelCharts.TEXT);
        // the quote
        long now = 0, auc = 0, items = 0;
        for (ExchangeView.BoxLine l : st.lines()) { now += l.nowCoins(); auc += l.auctionCoins(); items += l.quantity(); }
        int y = 56;
        text(g, font.plainSubstrByWidth(Component.translatable(K + "settle_quote", PanelCharts.number(items), PanelCharts.number(now),
            PanelCharts.number(auc)).getString(), 136), 8, y, PanelCharts.TEXT);
        hover(8, y - 1, 136, 10, Component.translatable(K + "settle_quote_help").getString(), mx, my);
        y += 11;
        text(g, font.plainSubstrByWidth(Component.translatable(K + "settle_uu", PanelCharts.number(st.uu())).getString(), 136), 8, y, PanelCharts.DIM);
        y += 12;
        button(g, 8, y, 50, 13, Component.translatable(K + "sell_now"), false, trade, () -> act(TerminalPackets.SETTLE_SELL, 0, "", 0, 0, scrapRest ? 2 : 0), mx, my);
        button(g, 60, y, 50, 13, Component.translatable(K + "sell_auction"), false, trade,
            () -> act(TerminalPackets.SETTLE_SELL, 0, "", Math.min(100, num(minBox, 0)), 0, 1), mx, my);
        place(minBox, 112, y, 30);
        hover(112, y, 30, 13, Component.translatable(K + "min_percent").getString(), mx, my);
        y += 15;
        scrapToggle(g, 8, y, mx, my);
        button(g, 76, y, 66, 13, Component.translatable(K + "uu_in"), false, trade, () -> act(TerminalPackets.SETTLE_UU, 0, "", 0, 0, 0), mx, my);
        // deliveries
        int x = 150, w = W - x - 6, ty = 24;
        text(g, Component.translatable("gui.mio_icif.exchange.settle.targets"), x, ty, PanelCharts.ACCENT);
        ty += 12;
        if (st.targets().isEmpty())
            for (var line : font.split(Component.translatable("gui.mio_icif.exchange.settle.no_targets"), w)) {
                g.drawString(font, line, leftPos + x, topPos + ty, PanelCharts.DIM, false);
                ty += 10;
            }
        for (ExchangeView.SettleTarget t : st.targets()) {
            if (ty > 100) break;
            g.renderItem(new ItemStack(item(t.item())), leftPos + x, topPos + ty - 3);
            String label = Component.translatable("gui.mio_icif.exchange.settle." + (t.type() == 0 ? "target_bounty" : "target_commission"),
                name(t.item()), PanelCharts.number(t.count())).getString();
            text(g, font.plainSubstrByWidth(label, w - 66), x + 18, ty + 1, PanelCharts.TEXT);
            hover(x, ty - 3, w - 46, 14, label, mx, my);
            final long id = t.id();
            final int count = (int) Math.min(Integer.MAX_VALUE, t.count());
            final boolean bounty = t.type() == 0;
            button(g, x + w - 44, ty - 2, 44, 12, Component.translatable("gui.mio_icif.exchange.deliver"), false, trade,
                () -> act(bounty ? TerminalPackets.SETTLE_BOUNTY : TerminalPackets.SETTLE_COMMISSION, count, "", id, 0, 0), mx, my);
            ty += 14;
        }
        // buying into the container
        int by = 112;
        g.fill(leftPos + 4, topPos + by - 4, leftPos + W - 4, topPos + by - 3, PanelCharts.PANEL_LINE);
        List<FutureCommodity> all = FutureCommodityManager.getCommodities();
        String id = all.isEmpty() ? "" : all.get(Math.floorMod(buyCommodity, all.size())).getItemId();
        text(g, Component.translatable(K + "buy_into"), 8, by + 2, PanelCharts.DIM);
        int bx = 8 + font.width(Component.translatable(K + "buy_into")) + 4;
        button(g, bx, by, 10, 13, Component.literal("<"), false, !all.isEmpty(), () -> buyCommodity--, mx, my);
        if (!id.isEmpty()) g.renderItem(new ItemStack(item(id)), leftPos + bx + 12, topPos + by - 2);
        hover(bx + 12, by - 2, 16, 16, id.isEmpty() ? "-" : name(id) + " - " + Component.translatable(K + "item_help").getString(), mx, my);
        button(g, bx + 30, by, 10, 13, Component.literal(">"), false, !all.isEmpty(), () -> buyCommodity++, mx, my);
        place(buyQty, bx + 44, by, 38);
        hover(bx + 44, by, 38, 13, Component.translatable(K + "buy_qty").getString(), mx, my);
        place(buyLimit, bx + 86, by, 50);
        hover(bx + 86, by, 50, 13, Component.translatable("gui.mio_icif.exchange.settle.limit_help").getString(), mx, my);
        button(g, bx + 140, by, W - 6 - bx - 140, 13, Component.translatable("gui.mio_icif.exchange.settle.buy_go"), false, trade && !id.isEmpty(),
            () -> act(TerminalPackets.SETTLE_BUY, (int) Math.max(1, Math.min(100_000, num(buyQty, 1))), id, num(buyLimit, 0), 0, 0), mx, my);
        hover(bx + 140, by, W - 6 - bx - 140, 13, Component.translatable(K + "buy_into_help").getString(), mx, my);
        text(g, Component.translatable("container.inventory"), TradeTerminalMenu.SELL_X, TradeTerminalMenu.INV_Y - 10, PanelCharts.DIM);
        for (int i = 0; i < 36; i++)
            slotFrame(g, TradeTerminalMenu.SELL_X + (i % 9) * 18, i < 27 ? TradeTerminalMenu.INV_Y + (i / 9) * 18 : TradeTerminalMenu.HOTBAR_Y);
    }

    /** 0.1.10.28 (X4): the scrap sink toggle of the settlement sale. */
    private void scrapToggle(GuiGraphics g, int x, int y, int mx, int my) {
        button(g, x, y, 64, 13, Component.translatable("gui.mio_icif.exchange." + (scrapRest ? "scrap_on" : "scrap_off")), scrapRest, true,
            () -> scrapRest = !scrapRest, mx, my);
        hover(x, y, 64, 13, Component.translatable("gui.mio_icif.exchange.scrap_help").getString(), mx, my);
    }

    // ------------------------------------------------------------------ ID cards page (0.1.10.28, X4)

    private void cards(GuiGraphics g, TerminalView v, int mx, int my) {
        boolean owner = v.level() == TerminalRules.OWNER;
        text(g, Component.translatable(K + "cards", v.cards().size(), com.miophas.singularity_iteration.common.exchange.IdCardRules.MAX_DELEGATES), 6, 24, PanelCharts.ACCENT);
        hover(6, 22, 200, 10, Component.translatable(K + "cards_help").getString(), mx, my);
        if (!owner) {
            int y = 40;
            for (var line : font.split(Component.translatable(K + "cards_owner_only"), W - 12)) {
                g.drawString(font, line, leftPos + 6, topPos + y, PanelCharts.DIM, false);
                y += 10;
            }
            return;
        }
        if (!v.cards().isEmpty()) button(g, W - 6 - 80, 21, 80, 13, Component.translatable(K + "revoke_all"), false, true,
            () -> act(TerminalPackets.REVOKE_ALL_CARDS, 0, "", 0, 0, 0), mx, my);
        // the form: player, level, days, write
        int fy = 38;
        text(g, Component.translatable(K + "add_player"), 6, fy + 2, PanelCharts.DIM);
        place(nameBox, 60, fy, 90);
        final int lv = cardLevel;
        button(g, 154, fy, 50, 13, levelName(lv), false, true, () -> cardLevel = lv >= TerminalRules.CONFIG ? TerminalRules.VIEW : lv + 1, mx, my);
        text(g, Component.translatable(K + "card_days"), 208, fy + 2, PanelCharts.DIM);
        place(cardDays, 240, fy, 22);
        button(g, 266, fy, W - 6 - 266, 13, Component.translatable(K + "card_write"), false, !nameBox.getValue().isBlank(), () -> {
            act(TerminalPackets.ISSUE_CARD, 0, nameBox.getValue().trim(), lv, Math.max(1, Math.min(30, num(cardDays, 7))), 0);
            nameBox.setValue("");
        }, mx, my);
        hover(266, fy, W - 6 - 266, 13, Component.translatable(K + "card_write_help").getString(), mx, my);
        int y = fy + 20;
        g.fill(leftPos + 4, topPos + y - 4, leftPos + W - 4, topPos + y - 3, PanelCharts.PANEL_LINE);
        if (v.cards().isEmpty()) text(g, Component.translatable(K + "no_cards"), 8, y, PanelCharts.DIM);
        for (CardView.CardLine c : v.cards()) {
            if (y > H - 30) break;
            String line = Component.translatable(K + "card_line", c.holder(), levelName(c.level()), c.expiry() + 1, Math.max(0, c.expiry() - v.day() + 1),
                c.serial()).getString();
            text(g, font.plainSubstrByWidth(line, W - 72), 8, y + 2, PanelCharts.TEXT);
            final long serial = c.serial();
            button(g, W - 6 - 56, y, 56, 12, Component.translatable(K + "revoke"), false, true,
                () -> act(TerminalPackets.REVOKE_CARD, 0, "", serial, 0, 0), mx, my);
            y += 14;
        }
        int hy = H - 24;
        for (var line : font.split(Component.translatable(K + "cards_foot"), W - 12)) {
            if (hy > H - 6) break;
            g.drawString(font, line, leftPos + 6, topPos + hy, PanelCharts.DIM, false);
            hy += 10;
        }
    }

    // ------------------------------------------------------------------ log page

    private void log(GuiGraphics g, TerminalView v) {
        text(g, Component.translatable(K + "log"), 6, 24, PanelCharts.ACCENT);
        int y = 36;
        if (v.log().isEmpty()) text(g, Component.translatable(K + "no_log"), 8, y, PanelCharts.DIM);
        for (TerminalView.LogLine l : v.log()) {
            if (y > H - 12) break;
            String key = l.kind() == TerminalOrders.LOG_SOLD ? "log_sold" : l.kind() == TerminalOrders.LOG_AUCTION ? "log_auction"
                : l.kind() == TerminalOrders.LOG_SCRAP ? "log_scrap" : "log_bought";
            String line = Component.translatable(K + key, l.day() + 1, PanelCharts.number(l.quantity()), name(l.item()), PanelCharts.number(l.coins())).getString();
            g.renderItem(new ItemStack(item(l.item())), leftPos + 6, topPos + y - 4);
            text(g, font.plainSubstrByWidth(line, W - 32), 26, y, l.kind() == TerminalOrders.LOG_BOUGHT ? PanelCharts.WARN : PanelCharts.TEXT);
            y += 13;
        }
    }
}
