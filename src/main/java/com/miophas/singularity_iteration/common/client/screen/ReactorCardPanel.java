// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.item.reactor.ReactorConfigCardItem;
import com.miophas.singularity_iteration.common.item.reactor.mio_icif_reactors;
import com.miophas.singularity_iteration.common.menu.generator.ReactorGuideMenu;
import com.miophas.singularity_iteration.common.reactor.ReactorLayoutGuide;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.11.76: the reactor config card controls of a reactor screen ({@link ReactorLayoutGuide}): three square buttons
 * (single down arrow: import the reactor layout into the card; single up arrow: export the card layout onto the
 * reactor, Shift clears it; double up arrow: fill the empty guided slots from the player), the faded parts of the
 * guide in empty slots, a red wash over a slot that holds another part, the tooltips and a short status line after
 * each button.
 *
 * <p>Up points at the reactor grid (the card slot sits below it), down at the card.
 */
@OnlyIn(Dist.CLIENT)
final class ReactorCardPanel {
    static final int SIZE = 14, IMPORT = 0, EXPORT = 1, FILL = 2;
    private static final String K = "gui.mio_icif.reactor_card.";
    private static final long STATUS_MS = 4000;
    private static final int OUTLINE = 0xFF585D63, FACE = 0xFFD9DDE1, FACE_HOVER = 0xFFF4F6F8, FACE_OFF = 0xFFC9CED1,
        BEVEL_HI = 0xFFF8F9F9, BEVEL_LO = 0xFFAAAEB3, INK = 0xFF2A2E33, INK_OFF = 0xFF9AA0A6, MISMATCH = 0x66E0402E;

    private final AbstractContainerMenu menu;
    private final ReactorGuideMenu guideMenu;
    /** Button positions {x, y} relative to the screen, in the order import, export, fill. */
    private final int[][] buttons;
    /** The wash drawn over a faded part: the slot colour of the screen, mostly opaque. */
    private final int wash;
    private int seenSeq;
    private long statusUntil;
    private Component status = Component.empty();
    private boolean statusBad;

    <M extends AbstractContainerMenu & ReactorGuideMenu> ReactorCardPanel(M menu, int[][] buttons, int wash) {
        this.menu = menu;
        this.guideMenu = menu;
        this.buttons = buttons;
        this.wash = wash;
    }

    private ReactorLayoutGuide.Data data() { return guideMenu.guideData(); }

    private Slot cardSlot() { return menu.slots.get(guideMenu.cardSlotIndex()); }

    private boolean enabled(int button) {
        ItemStack card = cardSlot().getItem();
        return switch (button) {
            case IMPORT -> ReactorConfigCardItem.isCard(card);
            case EXPORT -> Screen.hasShiftDown() ? data().hasGuide() : ReactorConfigCardItem.layout(card) != null;
            default -> data().hasGuide();
        };
    }

    // ------------------------------------------------------------------ drawing

    /** The three buttons (fills only, so it may run inside a managed batch). */
    void drawButtons(GuiGraphics g, int left, int top, int mouseX, int mouseY) {
        for (int b = 0; b < buttons.length; b++) {
            int x = left + buttons[b][0], y = top + buttons[b][1];
            boolean on = enabled(b), hover = mouseX >= x && mouseX < x + SIZE && mouseY >= y && mouseY < y + SIZE;
            g.fill(x - 1, y - 1, x + SIZE + 1, y + SIZE + 1, OUTLINE);
            g.fill(x, y, x + SIZE, y + SIZE, !on ? FACE_OFF : hover ? FACE_HOVER : FACE);
            g.fill(x, y, x + SIZE, y + 1, BEVEL_HI);
            g.fill(x, y, x + 1, y + SIZE, BEVEL_HI);
            g.fill(x, y + SIZE - 1, x + SIZE, y + SIZE, BEVEL_LO);
            g.fill(x + SIZE - 1, y, x + SIZE, y + SIZE, BEVEL_LO);
            icon(g, b, x, y, on ? INK : INK_OFF);
        }
    }

    /** A triangle head of rows 2, 4, 6, 8 pixels wide, pointing up (first row at y) or down (last row at y + 3). */
    private static void head(GuiGraphics g, int x, int y, boolean up, int color) {
        for (int r = 0; r < 4; r++) {
            int row = up ? y + r : y + 3 - r;
            g.fill(x + 6 - r, row, x + 8 + r, row + 1, color);
        }
    }

    private static void icon(GuiGraphics g, int button, int x, int y, int color) {
        switch (button) {
            case IMPORT -> { g.fill(x + 6, y + 3, x + 8, y + 7, color); head(g, x, y + 7, false, color); }   // down arrow
            case EXPORT -> { head(g, x, y + 3, true, color); g.fill(x + 6, y + 7, x + 8, y + 11, color); }    // up arrow
            default -> { head(g, x, y + 2, true, color); head(g, x, y + 7, true, color); }                    // double up
        }
    }

    /**
     * The faded parts of the guide in the empty guided slots of the reactor's columns, and a faded card in the empty
     * card slot. Draws items, so it must run outside a managed batch.
     */
    void drawGhosts(GuiGraphics g, int left, int top) {
        ReactorLayoutGuide.Data d = data();
        int columns = d.columns();
        for (int i = 0; i < 54 && i < menu.slots.size(); i++) {
            if (i % 9 >= columns) continue;
            Item item = d.guided(i);
            Slot slot = menu.slots.get(i);
            if (item != null && !slot.hasItem()) ghost(g, new ItemStack(item), left + slot.x, top + slot.y);
        }
        Slot card = cardSlot();
        if (!card.hasItem()) ghost(g, new ItemStack(mio_icif_reactors.REACTOR_CONFIG_CARD.get()), left + card.x, top + card.y);
    }

    private void ghost(GuiGraphics g, ItemStack stack, int x, int y) {
        g.renderFakeItem(stack, x, y);
        // the item sits near z 150; the wash goes above it (real items of other slots come later at their own z)
        g.pose().pushPose();
        g.pose().translate(0, 0, 200);
        g.fill(x, y, x + 16, y + 16, wash);
        g.pose().popPose();
    }

    /** A red wash over each guided slot that holds another part. Call from renderLabels (screen-relative pose). */
    void drawMismatches(GuiGraphics g) {
        g.pose().pushPose();
        g.pose().translate(0, 0, 250);
        for (int i = 0; i < 54 && i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            if (mismatch(i, slot)) g.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, MISMATCH);
        }
        g.pose().popPose();
    }

    private boolean mismatch(int index, Slot slot) {
        if (index >= 54 || !slot.hasItem()) return false;
        Item item = data().guided(index);
        return item != null && !slot.getItem().is(item);
    }

    // ------------------------------------------------------------------ input and text

    private int buttonAt(int left, int top, double mouseX, double mouseY) {
        for (int b = 0; b < buttons.length; b++) {
            int x = left + buttons[b][0], y = top + buttons[b][1];
            if (mouseX >= x && mouseX < x + SIZE && mouseY >= y && mouseY < y + SIZE) return b;
        }
        return -1;
    }

    /** Sends the button under the mouse to the server (it answers with a status). True when a button was hit. */
    boolean mouseClicked(int left, int top, double mouseX, double mouseY, int mouseButton) {
        Minecraft mc = Minecraft.getInstance();
        int b = mouseButton == 0 ? buttonAt(left, top, mouseX, mouseY) : -1;
        if (b < 0 || mc.gameMode == null) return false;
        int id = b == IMPORT ? ReactorLayoutGuide.IMPORT : b == FILL ? ReactorLayoutGuide.FILL
            : Screen.hasShiftDown() ? ReactorLayoutGuide.CLEAR : ReactorLayoutGuide.EXPORT;
        mc.gameMode.handleInventoryButtonClick(menu.containerId, id);
        return true;
    }

    /** Tooltip lines for a button, the empty card slot or an empty guided slot under the mouse; null when none. */
    @Nullable List<Component> tooltip(int left, int top, int mouseX, int mouseY, @Nullable Slot hovered) {
        int b = buttonAt(left, top, mouseX, mouseY);
        if (b >= 0) {
            String key = K + (b == IMPORT ? "import" : b == EXPORT ? "export" : "fill");
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable(key));
            lines.add(Component.translatable(key + ".tip").withStyle(ChatFormatting.GRAY));
            if (b == EXPORT) lines.add(Component.translatable(key + ".shift").withStyle(ChatFormatting.DARK_GRAY));
            return lines;
        }
        if (hovered == null || hovered.hasItem()) return null;
        if (hovered == cardSlot()) return List.of(Component.translatable(K + "slot"), Component.translatable(K + "slot.tip").withStyle(ChatFormatting.GRAY));
        int index = menu.slots.indexOf(hovered);
        Item item = index >= 0 && index < 54 && index % 9 < data().columns() ? data().guided(index) : null;
        if (item == null) return null;
        return List.of(item.getDescription(), Component.translatable(K + "ghost").withStyle(ChatFormatting.GRAY));
    }

    /** The item tooltip of a hovered reactor slot, with the part the guide asks for when the slot holds another. */
    List<Component> withMismatch(List<Component> lines, @Nullable Slot hovered) {
        int index = hovered == null ? -1 : menu.slots.indexOf(hovered);
        if (index < 0 || !mismatch(index, hovered)) return lines;
        List<Component> out = new ArrayList<>(lines);
        out.add(Component.translatable(K + "mismatch", data().guided(index).getDescription()).withStyle(ChatFormatting.RED));
        return out;
    }

    /** The status of the last button for a few seconds after it, else null. */
    @Nullable Component status() {
        ReactorLayoutGuide.Data d = data();
        int seq = d.get(ReactorLayoutGuide.Data.SEQ);
        if (seq != seenSeq) {
            seenSeq = seq;
            if (seq != 0) {
                statusUntil = System.currentTimeMillis() + STATUS_MS;
                int a = d.get(ReactorLayoutGuide.Data.A), b = d.get(ReactorLayoutGuide.Data.B), c = d.get(ReactorLayoutGuide.Data.C);
                int code = d.get(ReactorLayoutGuide.Data.CODE);
                statusBad = code > ReactorLayoutGuide.FILLED || b > 0 || c > 0;
                status = switch (code) {
                    case ReactorLayoutGuide.IMPORTED -> Component.translatable(K + "status.imported", a);
                    case ReactorLayoutGuide.EXPORTED -> b > 0 ? Component.translatable(K + "status.exported_columns", a, b)
                        : Component.translatable(K + "status.exported", a);
                    case ReactorLayoutGuide.CLEARED -> Component.translatable(K + "status.cleared");
                    case ReactorLayoutGuide.FILLED -> c > 0 ? Component.translatable(K + "status.filled_blocked", a, b, c)
                        : b > 0 ? Component.translatable(K + "status.filled_missing", a, b) : Component.translatable(K + "status.filled", a);
                    case ReactorLayoutGuide.NO_CARD -> Component.translatable(K + "status.no_card");
                    case ReactorLayoutGuide.BLANK_CARD -> Component.translatable(K + "status.blank_card");
                    case ReactorLayoutGuide.EMPTY_REACTOR -> Component.translatable(K + "status.empty_reactor");
                    default -> Component.translatable(K + "status.no_guide");
                };
            }
        }
        return System.currentTimeMillis() < statusUntil ? status : null;
    }

    /** True when the shown status is a warning (nothing done, or parts missing). */
    boolean statusBad() { return statusBad; }
}
