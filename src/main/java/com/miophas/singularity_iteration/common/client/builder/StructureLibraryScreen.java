// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.builder;

import com.miophas.singularity_iteration.common.builder.BlueprintBuilderBlockEntity;
import com.miophas.singularity_iteration.common.builder.BlueprintBuilderMenu;
import com.miophas.singularity_iteration.common.builder.Blueprint;
import com.miophas.singularity_iteration.common.builder.BuilderStructurePacket;
import com.miophas.singularity_iteration.common.builder.BuilderStructures;
import com.miophas.singularity_iteration.common.client.multiblock.StructureView;
import com.miophas.singularity_iteration.common.client.screen.SiGuiTheme;
import com.miophas.singularity_iteration.common.client.screen.VanillaFrame;
import com.miophas.singularity_iteration.common.client.screen.widget.SiButton;
import com.miophas.singularity_iteration.core.api.multiblock.StructureBlueprint;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.11.189: the Blueprint Builder's structure catalog, opened from its screen (the builder's menu stays open behind it, as
 * with JEI). The multiblock structures of this mod (and of addons) on the left; on the right the chosen one in 3D as JEI's
 * "Multiblock Structures" shows it ({@link StructureView}: drag turns, the wheel zooms, the layer label shows one layer), but
 * as the builder will place it (each fusion port in its slot, the TESU's ports input and output in turn), with the blocks it
 * needs. "Build this" makes it the builder's blueprint ({@link BuilderStructurePacket}) and goes back; Esc goes back too.
 * <pre>
 *   Title
 *   +--------+ +------------------------+
 *   | list   | |[size, blocks]  [layer] |
 *   |        | |  3D view               |
 *   |        | +------------------------+
 *   |        | [mat][mat][mat]...
 *   |        |                [Build][Back]
 *   +--------+
 * </pre>
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public final class StructureLibraryScreen extends Screen {
    private static final int ROW_H = 12, MAT = 18, MAT_ROWS = 2;

    private final Screen parent;
    private final BlueprintBuilderMenu menu;
    private final List<StructureBlueprint> plans;
    private int selected, scroll;
    @Nullable private StructureView view;
    private List<Map.Entry<Item, Integer>> materials = List.of();
    private int blockCount, sizeX, sizeY, sizeZ;
    private boolean turning;
    private int left, top, w, h, listX, listY, listW, listH, viewX, viewY, viewW, viewH, matY, perRow;
    @Nullable private SiButton build;

    /** {@code current}: the title key of the structure the builder has now (it is chosen first), or null. */
    public StructureLibraryScreen(Screen parent, BlueprintBuilderMenu menu, @Nullable String current) {
        super(Component.translatable("gui.mio_icif.builder.structures.title"));
        this.parent = parent;
        this.menu = menu;
        this.plans = BuilderStructures.all();
        for (int i = 0; i < plans.size(); i++) if (plans.get(i).titleKey().equals(current)) selected = i;
    }

    @Override
    protected void init() {
        w = Math.min(340, width - 16);
        h = Math.min(226, height - 16);
        left = (width - w) / 2;
        top = (height - h) / 2;
        listW = w >= 320 ? 116 : Math.max(80, w / 3);
        listX = left + 7;
        listY = top + 18;
        listH = h - 18 - 7;
        viewX = listX + listW + 5;
        viewY = listY;
        viewW = left + w - 7 - viewX;
        int buttonsY = top + h - 7 - 14;
        matY = buttonsY - 3 - MAT_ROWS * MAT;
        viewH = matY - 3 - viewY;
        perRow = Math.max(1, viewW / MAT);
        build = addRenderableWidget(new SiButton(left + w - 7 - 46 - 3 - 76, buttonsY, 76, 14, Component.translatable("gui.mio_icif.builder.structures.build"), b -> build()));
        addRenderableWidget(new SiButton(left + w - 7 - 46, buttonsY, 46, 14, Component.translatable("gui.mio_icif.builder.structures.back"), b -> onClose()));
        if (view == null) select(selected);
        scroll = Math.max(0, Math.min(scroll, Math.max(0, plans.size() - rows())));
        if (selected >= scroll + rows()) scroll = Math.max(0, selected - rows() + 1);
        updateBuild();
    }

    private int rows() { return Math.max(1, (listH - 4) / ROW_H); }

    /** Shows structure {@code i}: its view as the builder places it, its size and the blocks it needs (most first). */
    private void select(int i) {
        selected = i;
        view = null;
        materials = List.of();
        blockCount = 0;
        if (i < 0 || i >= plans.size()) return;
        StructureBlueprint plan = plans.get(i);
        Blueprint bp = BuilderStructures.blueprint(plan.id());
        if (bp == null) return;
        List<BlockPos> positions = new ArrayList<>(bp.entries().size());
        List<BlockState> states = new ArrayList<>(bp.entries().size());
        Map<Item, Integer> counts = new LinkedHashMap<>();
        for (Blueprint.Entry e : bp.entries()) {
            positions.add(e.pos());
            states.add(e.state());
            Item item = e.state().getBlock().asItem();
            if (item != Items.AIR) counts.merge(item, 1, Integer::sum);
        }
        sizeX = bp.size().getX();
        sizeY = bp.size().getY();
        sizeZ = bp.size().getZ();
        blockCount = positions.size();
        view = StructureView.of(plan.id(), sizeX, sizeY, sizeZ, positions, states);
        List<Map.Entry<Item, Integer>> sorted = new ArrayList<>(counts.entrySet());
        sorted.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        materials = sorted;
        updateBuild();
    }

    private void updateBuild() {
        if (build == null) return;
        boolean running = menu.status() == BlueprintBuilderBlockEntity.RUNNING;
        build.active = view != null && !running;
        build.setTooltip(running ? Tooltip.create(Component.translatable("gui.mio_icif.builder.structures.running")) : null);
    }

    private void build() {
        if (selected < 0 || selected >= plans.size() || view == null) return;
        PacketDistributor.sendToServer(new BuilderStructurePacket(menu.containerId, plans.get(selected).id()));
        onClose();
    }

    @Override
    public void tick() {
        super.tick();
        // the builder's menu went (the block broke, the player walked away): nothing to go back to
        if (minecraft != null && (minecraft.player == null || minecraft.player.containerMenu != menu)) {
            minecraft.setScreen(null);
            return;
        }
        updateBuild();
    }

    @Override public void onClose() { if (minecraft != null) minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }

    // ------------------------------------------------------------------ drawing
    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        VanillaFrame.panel(g, left, top, w, h);
        SiGuiTheme.well(g, listX, listY, listW, listH);
        VanillaFrame.inset(g, viewX, viewY, viewW, viewH, VanillaFrame.SLOT);
        for (int k = 0; k < perRow * MAT_ROWS; k++) VanillaFrame.slot(g, viewX + (k % perRow) * MAT, matY + (k / perRow) * MAT);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawString(font, title, left + 8, top + 6, VanillaFrame.TITLE, false);
        int hover = rowAt(mouseX, mouseY);
        if (plans.isEmpty()) g.drawString(font, Component.translatable("gui.mio_icif.builder.structures.none"), listX + 4, listY + 4, SiGuiTheme.TEXT_SCREEN_DIM, false);
        for (int r = 0; r < rows() && scroll + r < plans.size(); r++) {
            int i = scroll + r, ry = listY + 2 + r * ROW_H;
            if (i == selected) g.fill(listX + 1, ry, listX + listW - 1, ry + ROW_H, 0x553A6EA5);
            else if (i == hover) g.fill(listX + 1, ry, listX + listW - 1, ry + ROW_H, 0x30000000);
            String name = Component.translatable(plans.get(i).titleKey()).getString();
            g.drawString(font, fit(name, listW - 8), listX + 4, ry + 2, i == selected ? SiGuiTheme.ACCENT : SiGuiTheme.TEXT_SCREEN, false);
        }
        if (plans.size() > rows()) {   // a scroll bar
            int track = listH - 4, bar = Math.max(8, track * rows() / plans.size());
            int by = listY + 2 + (track - bar) * scroll / Math.max(1, plans.size() - rows());
            g.fill(listX + listW - 4, by, listX + listW - 2, by + bar, 0x803A6EA5);
        }
        if (view != null) {
            view.render(g, viewX + 1, viewY + 1, viewW - 2, viewH - 2);
            Component label = layerLabel();
            int lw = font.width(label) + 6;
            g.fill(viewX + viewW - 1 - lw, viewY + 1, viewX + viewW - 1, viewY + 12, onLabel(mouseX, mouseY) ? 0xC0405060 : 0x80303840);
            g.drawString(font, label, viewX + viewW - 1 - lw + 3, viewY + 3, 0xFFFFFFFF, false);
            // 0.1.11.190: the size and the block count at the view's top left (they overlapped the Build button under the materials)
            Component info = Component.translatable("gui.mio_icif.builder.structures.info", sizeX, sizeY, sizeZ, String.format("%,d", blockCount));
            int iw = font.width(info) + 6, iy = iw + lw + 2 > viewW - 2 ? viewY + 13 : viewY + 1;   // a narrow view: under the layer label's row
            g.fill(viewX + 1, iy, viewX + 1 + Math.min(iw, viewW - 2), iy + 11, 0x80303840);
            g.drawString(font, fit(info.getString(), viewW - 8), viewX + 4, iy + 2, 0xFFFFFFFF, false);
        }
        for (int k = 0; k < materials.size() && k < perRow * MAT_ROWS; k++) {
            int mx = viewX + (k % perRow) * MAT + 1, my = matY + (k / perRow) * MAT + 1;
            g.renderItem(new ItemStack(materials.get(k).getKey()), mx, my);
            String count = compact(materials.get(k).getValue());
            g.pose().pushPose();
            g.pose().translate(0, 0, 200);
            float s = 0.75F;
            g.pose().translate(mx + 17 - font.width(count) * s, my + 17 - 8 * s, 0);
            g.pose().scale(s, s, 1);
            g.drawString(font, count, 0, 0, 0xFFFFFFFF, true);
            g.pose().popPose();
        }
        tooltips(g, mouseX, mouseY, hover);
    }

    private Component layerLabel() {
        return view == null || view.layer() < 0 ? Component.translatable("jei.mio_icif.structure.layer_all", sizeY)
            : Component.translatable("jei.mio_icif.structure.layer", view.layer() + 1, sizeY);
    }

    private boolean onLabel(double mx, double my) {
        int lw = font.width(layerLabel()) + 6;
        return view != null && mx >= viewX + viewW - 1 - lw && mx < viewX + viewW - 1 && my >= viewY + 1 && my < viewY + 12;
    }

    private boolean inView(double mx, double my) { return mx >= viewX && mx < viewX + viewW && my >= viewY && my < viewY + viewH; }

    private boolean inList(double mx, double my) { return mx >= listX && mx < listX + listW && my >= listY && my < listY + listH; }

    /** The list row under the mouse (an index into the structures), or -1. */
    private int rowAt(double mx, double my) {
        if (!inList(mx, my) || my < listY + 2) return -1;
        int i = scroll + (int) ((my - listY - 2) / ROW_H);
        return i < plans.size() && i - scroll < rows() ? i : -1;
    }

    /** The material slot under the mouse, or -1. */
    private int materialAt(double mx, double my) {
        if (mx < viewX || my < matY || my >= matY + MAT_ROWS * MAT) return -1;
        int c = (int) ((mx - viewX) / MAT), r = (int) ((my - matY) / MAT);
        if (c >= perRow) return -1;
        int k = r * perRow + c;
        return k < materials.size() ? k : -1;
    }

    private String fit(String text, int width) {
        if (font.width(text) <= width) return text;
        return font.plainSubstrByWidth(text, width - font.width("…")) + "…";
    }

    private static String compact(int n) {
        return n < 1000 ? Integer.toString(n) : n < 10_000 ? String.format(java.util.Locale.ROOT, "%.1fk", n / 1000.0) : (n / 1000) + "k";
    }

    private void tooltips(GuiGraphics g, int mouseX, int mouseY, int hover) {
        if (turning) return;
        if (hover >= 0) {
            StructureBlueprint plan = plans.get(hover);
            g.renderComponentTooltip(font, List.of(Component.translatable(plan.titleKey()),
                Component.literal(plan.sizeX() + "×" + plan.sizeY() + "×" + plan.sizeZ()).withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
            return;
        }
        int m = materialAt(mouseX, mouseY);
        if (m >= 0) {
            ItemStack stack = new ItemStack(materials.get(m).getKey());
            g.renderComponentTooltip(font, List.of(stack.getHoverName(),
                Component.literal("× " + String.format("%,d", materials.get(m).getValue())).withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
            return;
        }
        if (onLabel(mouseX, mouseY)) {
            g.renderTooltip(font, Component.translatable("jei.mio_icif.structure.layer_help"), mouseX, mouseY);
            return;
        }
        if (view != null && inView(mouseX, mouseY)) {
            List<FormattedCharSequence> lines = new ArrayList<>();
            lines.add(Component.translatable("jei.mio_icif.structure.view_help").withStyle(ChatFormatting.GRAY).getVisualOrderText());
            for (String note : plans.get(selected).noteKeys())
                lines.addAll(font.split(Component.translatable(note).withStyle(ChatFormatting.GRAY), 220));
            lines.addAll(font.split(Component.translatable("gui.mio_icif.builder.structures.air").withStyle(ChatFormatting.GOLD), 220));
            g.renderTooltip(font, lines, mouseX, mouseY);
        }
    }

    // ------------------------------------------------------------------ input
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int row = rowAt(mouseX, mouseY);
        if (row >= 0 && button == 0) {
            if (row != selected) select(row);
            return true;
        }
        if (onLabel(mouseX, mouseY) && view != null) {
            view.stepLayer(button == 1 ? -1 : 1);
            return true;
        }
        if (inView(mouseX, mouseY) && button == 0 && view != null) {
            turning = true;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (turning && view != null) {
            view.drag(dragX, dragY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (turning) {
            turning = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (inList(mouseX, mouseY)) {
            scroll = Math.max(0, Math.min(Math.max(0, plans.size() - rows()), scroll - (int) Math.signum(scrollY)));
            return true;
        }
        if (inView(mouseX, mouseY) && view != null) {
            view.zoom(scrollY);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // up and down walk the list
        if ((keyCode == 265 || keyCode == 264) && !plans.isEmpty()) {
            int next = Math.max(0, Math.min(plans.size() - 1, selected + (keyCode == 264 ? 1 : -1)));
            if (next != selected) select(next);
            if (selected < scroll) scroll = selected;
            if (selected >= scroll + rows()) scroll = selected - rows() + 1;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
