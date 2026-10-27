// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.client.screen.panel.PanelCharts;
import com.miophas.singularity_iteration.common.exchange.CardClientState;
import com.miophas.singularity_iteration.common.exchange.CardPackets;
import com.miophas.singularity_iteration.common.exchange.CardView;
import com.miophas.singularity_iteration.common.exchange.UuBoard;
import com.miophas.singularity_iteration.common.item.normal.IdCardItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 0.1.10.28 (X4): the account screen of a personal ID card, opened by using the card anywhere. Two pages: <b>Account</b>
 * (mailbox, allowance left, the stabiliser's factor, resting orders and auction orders with cancel; refunds go to the
 * mailbox) and <b>Cards</b> (the delegate cards the holder issued, revoke one or all). The server checks the card on every
 * request and action.
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class IdCardScreen extends Screen {
    private static final String K = "gui.mio_icif.card.";
    private static final int W = 300, H = 210;

    private record Region(int x, int y, int w, int h, Runnable click) {
        boolean inside(double mx, double my) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    }

    private final List<Region> regions = new ArrayList<>();
    private int tab, timer, scroll;

    public IdCardScreen() {
        super(Component.translatable(K + "title"));
    }

    /** Installs the hook of the card item (client setup). */
    @EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
    public static final class Hook {
        @SubscribeEvent
        static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
            IdCardItem.OPEN_ACCOUNT = () -> Minecraft.getInstance().setScreen(new IdCardScreen());
        }
    }

    @Override
    protected void init() {
        super.init();
        PacketDistributor.sendToServer(new CardPackets.Request());
    }

    @Override
    public void tick() {
        if (++timer >= 20) { timer = 0; PacketDistributor.sendToServer(new CardPackets.Request()); }
    }

    @Override
    public void removed() {
        super.removed();
        CardClientState.clear();
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private void act(int kind, long a) { PacketDistributor.sendToServer(new CardPackets.CAction(kind, a)); }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) for (int i = regions.size() - 1; i >= 0; i--) if (regions.get(i).inside(mx, my)) { regions.get(i).click().run(); return true; }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        scroll = Math.max(0, scroll - (int) Math.signum(sy));
        return true;
    }

    private static Item item(String id) {
        if (UuBoard.is(id)) return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "uumatter_bucket"));
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl == null || !BuiltInRegistries.ITEM.containsKey(rl) ? Items.BARRIER : BuiltInRegistries.ITEM.get(rl);
    }

    private static String name(String id) {
        return UuBoard.is(id) ? Component.translatable("gui.mio_icif.exchange.uu_board").getString() : new ItemStack(item(id)).getHoverName().getString();
    }

    private void button(GuiGraphics g, int x, int y, int w, int h, Component label, boolean on, Runnable click, int mx, int my) {
        boolean hot = mx >= x && mx < x + w && my >= y && my < y + h;
        g.fill(x, y, x + w, y + h, on ? 0xFF2F81F7 : hot ? 0xFF2D3748 : 0xFF1F2630);
        g.renderOutline(x, y, w, h, on ? 0xFF58A6FF : PanelCharts.PANEL_LINE);
        String text = font.plainSubstrByWidth(label.getString(), w - 4);
        g.drawString(font, text, x + (w - font.width(text)) / 2, y + (h - 8) / 2, PanelCharts.TEXT, false);
        regions.add(new Region(x, y, w, h, click));
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        regions.clear();
        int x = (width - W) / 2, y = (height - H) / 2;
        g.fill(x, y, x + W, y + H, 0xF00D1117);
        g.renderOutline(x, y, W, H, PanelCharts.PANEL_LINE);
        g.fill(x, y, x + W, y + 20, 0xFF161B22);
        g.drawString(font, title, x + 6, y + 6, PanelCharts.ACCENT, false);
        int tx = x + 12 + font.width(title);
        for (int i = 0; i < 2; i++) {
            Component label = Component.translatable(K + "tab." + i);
            int w = font.width(label) + 10;
            final int t = i;
            button(g, tx, y + 3, w, 14, label, tab == i, () -> { tab = t; scroll = 0; }, mx, my);
            tx += w + 2;
        }
        CardView v = CardClientState.view();
        if (v == null) {
            g.drawCenteredString(font, Component.translatable(K + "loading"), x + W / 2, y + H / 2, PanelCharts.DIM);
            return;
        }
        PanelCharts.right(g, font, v.holder(), x + W - 6, y + 6, PanelCharts.TEXT);
        if (tab == 0) account(g, v, x, y, mx, my); else cards(g, v, x, y, mx, my);
    }

    private void account(GuiGraphics g, CardView v, int x, int y, int mx, int my) {
        int ly = y + 26;
        g.drawString(font, Component.translatable(K + "mail", PanelCharts.number(v.mailCoins()), PanelCharts.number(v.mailUu()),
            PanelCharts.number(v.mailItems())), x + 6, ly, PanelCharts.TEXT, false);
        ly += 11;
        String allowance = v.allowance() < 0 ? Component.translatable("gui.mio_icif.exchange.box.unlimited").getString() : PanelCharts.number(v.allowance());
        g.drawString(font, Component.translatable(K + "allowance", allowance), x + 6, ly, PanelCharts.DIM, false);
        if (v.factor() < 1) PanelCharts.right(g, font, Component.translatable(K + "factor", String.format("%.0f%%", v.factor() * 100)).getString(),
            x + W - 6, ly, PanelCharts.WARN);
        ly += 13;
        g.drawString(font, Component.translatable(K + "orders"), x + 6, ly, PanelCharts.ACCENT, false);
        ly += 11;
        int max = (y + H - 8 - ly) / 13;
        List<Object> lines = new ArrayList<>(v.orders());
        lines.addAll(v.auctions());
        if (lines.isEmpty()) g.drawString(font, Component.translatable(K + "none"), x + 8, ly, PanelCharts.DIM, false);
        scroll = Math.min(scroll, Math.max(0, lines.size() - max));
        for (int i = scroll; i < lines.size() && i - scroll < max; i++) {
            Object o = lines.get(i);
            int ry = ly + (i - scroll) * 13;
            String text;
            String id;
            int kind;
            long key;
            if (o instanceof CardView.OrderLine l) {
                id = l.commodity();
                text = Component.translatable(K + (l.buy() ? "order_buy" : "order_sell"), name(id), PanelCharts.number(l.remaining()),
                    PanelCharts.number(l.price())).getString();
                kind = CardPackets.CANCEL_ORDER;
                key = l.id();
            } else {
                CardView.AuctionLine a = (CardView.AuctionLine) o;
                id = a.item();
                text = Component.translatable(K + "auction", name(id), PanelCharts.number(a.quantity()), a.minPercent()).getString();
                kind = CardPackets.CANCEL_AUCTION;
                key = a.id();
            }
            g.renderItem(new ItemStack(item(id)), x + 6, ry - 4);
            g.drawString(font, font.plainSubstrByWidth(text, W - 90), x + 26, ry, PanelCharts.TEXT, false);
            final int k = kind;
            final long a = key;
            button(g, x + W - 58, ry - 2, 52, 12, Component.translatable(K + "cancel"), false, () -> act(k, a), mx, my);
        }
        g.drawString(font, font.plainSubstrByWidth(Component.translatable(K + "remote_help").getString(), W - 12), x + 6, y + H - 11, PanelCharts.DIM, false);
    }

    private void cards(GuiGraphics g, CardView v, int x, int y, int mx, int my) {
        int ly = y + 26;
        g.drawString(font, Component.translatable(K + "issued", v.cards().size()), x + 6, ly, PanelCharts.ACCENT, false);
        if (!v.cards().isEmpty()) button(g, x + W - 86, ly - 3, 80, 13, Component.translatable(K + "revoke_all"), false, () -> act(CardPackets.REVOKE_ALL, 0), mx, my);
        ly += 14;
        if (v.cards().isEmpty()) g.drawString(font, Component.translatable(K + "no_cards"), x + 8, ly, PanelCharts.DIM, false);
        int max = (y + H - 20 - ly) / 13;
        scroll = Math.min(scroll, Math.max(0, v.cards().size() - max));
        for (int i = scroll; i < v.cards().size() && i - scroll < max; i++) {
            CardView.CardLine c = v.cards().get(i);
            int ry = ly + (i - scroll) * 13;
            String text = Component.translatable(K + "card_line", c.holder(), Component.translatable("gui.mio_icif.terminal.level." + Math.max(0, Math.min(4, c.level()))),
                c.expiry() + 1, Math.max(0, c.expiry() - v.day() + 1), c.serial()).getString();
            g.drawString(font, font.plainSubstrByWidth(text, W - 72), x + 8, ry, PanelCharts.TEXT, false);
            final long serial = c.serial();
            button(g, x + W - 58, ry - 2, 52, 12, Component.translatable(K + "revoke"), false, () -> act(CardPackets.REVOKE, serial), mx, my);
        }
        for (var line : font.split(Component.translatable(K + "cards_help"), W - 12)) {
            g.drawString(font, line, x + 6, y + H - 22, PanelCharts.DIM, false);
            break;
        }
    }
}
