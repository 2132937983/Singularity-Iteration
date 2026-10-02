package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.client.screen.widget.SiButton;
import com.miophas.singularity_iteration.common.item.armor.ArmorFeatureSlots;
import com.miophas.singularity_iteration.common.item.tuning.ITunableItem;
import com.miophas.singularity_iteration.common.network.EquipmentConsolePacket;
import com.miophas.singularity_iteration.common.network.mio_icif_Network;
import com.miophas.singularity_iteration.common.registry.mio_icif_sounds;
import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.api.item.ArmorFeatureInfo;
import com.miophas.singularity_iteration.core.api.item.ChargePriority;
import com.miophas.singularity_iteration.core.api.item.IItemAPI;
import com.miophas.singularity_iteration.core.api.tool.IToolModeProvider;
import com.miophas.singularity_iteration.core.prefab.item.ArmorFeatures;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * Equipment console (opened with the armor-feature key).
 *
 * <p>Left: the equipment rack (armor, optional back slot, main and off hand) with
 * charge bars. Right: the selected item's energy, its <b>energy allocation</b>
 * priority for shared chargers ({@link ChargePriority}), its armor module features
 * (switches and mode selectors from {@link ArmorFeatures}) and, for tools and
 * weapons, the operating modes ({@link IToolModeProvider}) with per-mode details
 * (cost, range, area, output), and the tuning sliders of {@link ITunableItem}s (weapon
 * output, accessory efficiency...) with their live cost summary. This console is the
 * single place where equipment, tools, weapons and accessories are configured.
 *
 * <p>Everything is data driven from the item APIs, so addon equipment that declares
 * features or modes appears automatically.
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_armor_features extends Screen {
    private static final int W = 300, H = 196;
    private static final int LIST_X = 7, LIST_Y = 18, LIST_W = 112, ROW_H = 22;
    private static final int PX = 124, PW = 169;          // detail panel
    private static final int FEATURE_Y = 74, FEATURE_ROW = 13, FEATURE_ROWS = 8;
    private static final String[] PRIORITY_KEYS = {"off", "low", "normal", "high"};

    private record Entry(String id, ItemStack stack) { }

    private final List<Entry> entries = new ArrayList<>();
    private final SiButton[] priorityButtons = new SiButton[4];
    private int selected;
    private int featureScroll;
    private int rackStart;
    private int draggingTune = -1;
    private static final int RACK_ROWS = 7;
    private int left, top;

    private String preselect;

    public mio_icif_gui_armor_features() {
        super(Component.translatable("gui.mio_icif.equipment_console.title"));
    }

    /** Opens with the given slot id (e.g. {@code mainhand}) selected. */
    public mio_icif_gui_armor_features select(String slotId) {
        this.preselect = slotId;
        return this;
    }

    @Override
    protected void init() {
        super.init();
        left = (width - W) / 2;
        top = (height - H) / 2;
        for (int i = 0; i < 4; i++) {
            int value = i;
            priorityButtons[i] = addRenderableWidget(new SiButton(left + PX + 4 + i * 41, top + 56, 40, 11,
                Component.translatable("gui.mio_icif.equipment_console.priority." + PRIORITY_KEYS[i]), b -> setPriority(value)));
        }
        refreshEntries();
        if (preselect != null) {
            for (int i = 0; i < entries.size(); i++) if (entries.get(i).id().equals(preselect)) selected = i;
            preselect = null;
            refreshEntries();
        }
    }

    private void refreshEntries() {
        entries.clear();
        Player player = minecraft == null ? null : minecraft.player;
        if (player == null) return;
        for (var slot : ArmorFeatureSlots.equipped(player)) entries.add(new Entry(slot.id(), slot.stack()));
        entries.add(new Entry(EquipmentConsolePacket.MAINHAND, player.getMainHandItem()));
        entries.add(new Entry(EquipmentConsolePacket.OFFHAND, player.getOffhandItem()));
        selected = Mth.clamp(selected, 0, Math.max(0, entries.size() - 1));
        rackStart = Mth.clamp(rackStart, Math.max(0, selected - RACK_ROWS + 1), Math.min(selected, Math.max(0, entries.size() - RACK_ROWS)));
    }

    @Override
    public void tick() {
        super.tick();
        refreshEntries();
        ItemStack stack = current();
        boolean chargeable = isElectric(stack);
        ChargePriority p = ChargePriority.of(stack);
        for (int i = 0; i < 4; i++) {
            priorityButtons[i].visible = chargeable;
            priorityButtons[i].selected(p.ordinal() == i);
        }
    }

    private ItemStack current() {
        return selected < entries.size() ? entries.get(selected).stack() : ItemStack.EMPTY;
    }

    // ------------------------------------------------------------------ energy helpers
    private static boolean isElectric(ItemStack s) {
        if (s.isEmpty()) return false;
        IItemAPI api = MioIcifAPI.instance().getItemAPI();
        return api.isElectricArmor(s) || api.isElectricTool(s) || api.isBattery(s);
    }

    private static long[] energy(ItemStack s) {
        IItemAPI api = MioIcifAPI.instance().getItemAPI();
        if (api.isElectricArmor(s)) return new long[]{api.getElectricArmorStored(s), api.getElectricArmorMaxEnergy(s)};
        if (api.isElectricTool(s)) return new long[]{api.getElectricToolStored(s), api.getElectricToolMaxEnergy(s)};
        if (api.isBattery(s)) return new long[]{api.getBatteryStored(s), api.getBatteryCapacity(s)};
        return new long[]{0, 0};
    }

    // ------------------------------------------------------------------ actions
    private void setPriority(int value) {
        Entry e = selected < entries.size() ? entries.get(selected) : null;
        if (e == null || e.stack().isEmpty()) return;
        ChargePriority.set(e.stack(), ChargePriority.values()[value]);   // optimistic
        PacketDistributor.sendToServer(new EquipmentConsolePacket(e.id(), EquipmentConsolePacket.SET_PRIORITY, value));
        sound(true);
    }

    private void toggleFeature(Entry entry, ArmorFeatureInfo info) {
        if (info.isMode()) {
            ArmorFeatures.cycleMode(entry.stack(), info.featureKey());
            mio_icif_Network.sendArmorFeatureCycle(entry.id(), info.featureKey());
        } else {
            boolean state = ArmorFeatures.toggle(entry.stack(), info.featureKey());
            mio_icif_Network.sendArmorFeatureToggle(entry.id(), info.featureKey(), state);
        }
        sound(true);
    }

    private void selectMode(Entry entry, int index) {
        PacketDistributor.sendToServer(new EquipmentConsolePacket(entry.id(), EquipmentConsolePacket.SELECT_MODE, index));
        sound(true);
    }

    private void sound(boolean toggle) {
        if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(
            (toggle ? mio_icif_sounds.UI_TOGGLE : mio_icif_sounds.UI_CLICK).get(), 1.0F, 0.6F));
    }

    // ------------------------------------------------------------------ rendering
    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g, mouseX, mouseY, partialTick);
        SiGuiTheme.panel(g, left, top, W, H);
        SiGuiTheme.well(g, left + LIST_X, top + LIST_Y, LIST_W, H - LIST_Y - 7);
        SiGuiTheme.well(g, left + PX, top + LIST_Y, PW, H - LIST_Y - 7);
        g.drawString(font, title, left + 8, top + 6, SiGuiTheme.TEXT, false);
        String hint = Component.translatable("gui.mio_icif.equipment_console.hint").getString();
        small(g, hint, left + W - 8 - (int) (font.width(hint) * 0.75F), top + 7, SiGuiTheme.TEXT);

        renderRack(g, mouseX, mouseY);
        renderDetail(g, mouseX, mouseY);
        // widgets only: Screen#render would draw (and blur) the background a second time over the panel
        for (var renderable : renderables) renderable.render(g, mouseX, mouseY, partialTick);
        renderTooltips(g, mouseX, mouseY);
    }

    private void renderRack(GuiGraphics g, int mouseX, int mouseY) {
        for (int i = rackStart; i < Math.min(entries.size(), rackStart + RACK_ROWS); i++) {
            Entry e = entries.get(i);
            int x = left + LIST_X + 1, y = top + LIST_Y + 1 + (i - rackStart) * ROW_H;
            boolean hot = in(mouseX, mouseY, x, y, LIST_W - 2, ROW_H);
            if (i == selected) {
                g.fill(x, y, x + LIST_W - 2, y + ROW_H - 1, 0x503A6EA5);
                g.fill(x, y, x + 2, y + ROW_H - 1, SiGuiTheme.ACCENT);
            } else if (hot) {
                g.fill(x, y, x + LIST_W - 2, y + ROW_H - 1, 0x263A6EA5);
            }
            g.renderItem(e.stack(), x + 4, y + 2);
            String name = e.stack().isEmpty()
                ? Component.translatable("gui.mio_icif.equipment_console.empty_" + (e.id().startsWith(ArmorFeatureSlots.CURIO_PREFIX) ? "accessory" : e.id())).getString()
                : e.stack().getHoverName().getString();
            small(g, font.plainSubstrByWidth(name, 104), x + 24, y + 3, e.stack().isEmpty() ? SiGuiTheme.TEXT_SCREEN_DIM : SiGuiTheme.TEXT_SCREEN);
            if (isElectric(e.stack())) {
                long[] en = energy(e.stack());
                double f = en[1] > 0 ? (double) en[0] / en[1] : 0;
                SiGuiTheme.bar(g, x + 24, y + 13, 60, 3, f, SiGuiTheme.BAR_FROM, SiGuiTheme.BAR_TO);
                ChargePriority p = ChargePriority.of(e.stack());
                if (p != ChargePriority.NORMAL) small(g, Component.translatable("gui.mio_icif.equipment_console.priority_short." + PRIORITY_KEYS[p.ordinal()]).getString(),
                    x + 88, y + 12, p == ChargePriority.OFF ? SiGuiTheme.BAD : p == ChargePriority.HIGH ? SiGuiTheme.GOOD : SiGuiTheme.WARN);
            }
        }
    }

    private void renderDetail(GuiGraphics g, int mouseX, int mouseY) {
        int x = left + PX + 4, y = top + LIST_Y + 4;
        ItemStack stack = current();
        if (stack.isEmpty()) {
            g.drawString(font, Component.translatable("gui.mio_icif.equipment_console.nothing"), x, y + 4, SiGuiTheme.TEXT_SCREEN_DIM, false);
            return;
        }
        g.renderItem(stack, x, y);
        g.drawString(font, font.plainSubstrByWidth(stack.getHoverName().getString(), PW - 30), x + 20, y + 1, SiGuiTheme.TEXT_SCREEN, false);
        IItemAPI api = MioIcifAPI.instance().getItemAPI();
        String kind = api.isElectricArmor(stack) ? "armor" : stack.getItem() instanceof IToolModeProvider || api.isElectricTool(stack) ? "tool" : "item";
        small(g, Component.translatable("gui.mio_icif.equipment_console.kind." + kind).getString(), x + 20, y + 10, SiGuiTheme.TEXT_SCREEN_DIM);

        if (isElectric(stack)) {
            long[] en = energy(stack);
            SiGuiTheme.bar(g, x, y + 20, PW - 10, 4, en[1] > 0 ? (double) en[0] / en[1] : 0, SiGuiTheme.BAR_FROM, SiGuiTheme.BAR_TO);
            small(g, String.format("%,d / %,d EU", en[0], en[1]), x, y + 26, SiGuiTheme.TEXT_SCREEN);
            String alloc = Component.translatable("gui.mio_icif.equipment_console.allocation").getString();
            small(g, alloc, x + PW - 10 - (int) (font.width(alloc) * 0.75F), y + 10, SiGuiTheme.ACCENT);
        }

        // features, tool modes and tuning sliders share the list area below the allocation buttons
        List<Row> rows = rows(stack);
        featureScroll = Mth.clamp(featureScroll, 0, Math.max(0, rows.size() - FEATURE_ROWS));
        int ly = top + FEATURE_Y;
        if (rows.isEmpty()) {
            small(g, Component.translatable("gui.mio_icif.equipment_console.no_features").getString(), x, ly + 2, SiGuiTheme.TEXT_SCREEN_DIM);
        }
        int currentMode = stack.getItem() instanceof IToolModeProvider tool ? tool.toolModeIndex(stack) : -1;
        for (int i = featureScroll; i < Math.min(rows.size(), featureScroll + FEATURE_ROWS); i++) {
            Row row = rows.get(i);
            int ry = ly + (i - featureScroll) * FEATURE_ROW;
            boolean hot = in(mouseX, mouseY, x - 2, ry - 1, PW - 6, FEATURE_ROW);
            switch (row.kind()) {
                case FEATURE -> {
                    ArmorFeatureInfo info = (ArmorFeatureInfo) row.ref();
                    if (hot) g.fill(x - 2, ry - 1, x + PW - 8, ry + FEATURE_ROW - 2, 0x263A6EA5);
                    g.drawString(font, font.plainSubstrByWidth(Component.translatable(info.featureNameKey()).getString(), 96), x, ry + 1, SiGuiTheme.TEXT_SCREEN, false);
                    if (info.isMode()) {
                        Component mode = ArmorFeatures.find(stack, info.featureKey()) instanceof ArmorFeatureInfo live && live.currentModeName() != null
                            ? live.currentModeName() : info.currentModeName();
                        String label = "◀ " + (mode == null ? "?" : mode.getString()) + " ▶";
                        g.drawString(font, label, x + PW - 10 - font.width(label), ry + 1, SiGuiTheme.ACCENT, false);
                    } else {
                        drawSwitch(g, x + PW - 34, ry, ArmorFeatures.isEnabled(stack, info.featureKey()));
                    }
                }
                case HEADER -> small(g, ((Component) row.ref()).getString(), x, ry + 3, SiGuiTheme.ACCENT);
                case MODE -> {
                    int m = row.index();
                    if (m == currentMode) g.fill(x - 2, ry - 1, x + PW - 8, ry + FEATURE_ROW - 2, 0x403A6EA5);
                    else if (hot) g.fill(x - 2, ry - 1, x + PW - 8, ry + FEATURE_ROW - 2, 0x1E3A6EA5);
                    g.fill(x + 1, ry + 2, x + 6, ry + 7, SiGuiTheme.OUTLINE);
                    if (m == currentMode) g.fill(x + 2, ry + 3, x + 5, ry + 6, SiGuiTheme.ACCENT);
                    g.drawString(font, font.plainSubstrByWidth(((Component) row.ref()).getString(), PW - 24), x + 10, ry + 1,
                        m == currentMode ? 0xFF1E3E62 : SiGuiTheme.TEXT_SCREEN, false);
                }
                case TUNE -> drawTune(g, stack, (ITunableItem.Spec) row.ref(), x, ry, hot || draggingTune == row.index());
                case INFO -> small(g, font.plainSubstrByWidth(((Component) row.ref()).getString(), (int) ((PW - 12) / 0.75F)), x + 2, ry + 3, SiGuiTheme.TEXT_SCREEN);
            }
        }
        if (rows.size() > FEATURE_ROWS) {
            int barH = FEATURE_ROWS * FEATURE_ROW;
            int knob = Math.max(8, barH * FEATURE_ROWS / rows.size());
            int ky = ly + (barH - knob) * featureScroll / Math.max(1, rows.size() - FEATURE_ROWS);
            g.fill(left + PX + PW - 4, ly, left + PX + PW - 2, ly + barH, SiGuiTheme.SCREEN_GRID);
            g.fill(left + PX + PW - 4, ky, left + PX + PW - 2, ky + knob, SiGuiTheme.ACCENT_DIM);
        }
    }

    // ------------------------------------------------------------------ row model (render + hit testing)
    private enum Kind { FEATURE, HEADER, MODE, TUNE, INFO }
    private record Row(Kind kind, int index, Object ref) { }

    private List<Row> rows(ItemStack stack) {
        List<Row> rows = new ArrayList<>();
        if (stack.isEmpty()) return rows;
        List<ArmorFeatureInfo> features = ArmorFeatures.features(stack);
        for (int i = 0; i < features.size(); i++) rows.add(new Row(Kind.FEATURE, i, features.get(i)));
        if (stack.getItem() instanceof IToolModeProvider tool) {
            List<Component> modes = tool.toolModes(stack);
            if (!modes.isEmpty()) rows.add(new Row(Kind.HEADER, 0, Component.translatable("gui.mio_icif.equipment_console.modes")));
            for (int i = 0; i < modes.size(); i++) rows.add(new Row(Kind.MODE, i, modes.get(i)));
        }
        if (stack.getItem() instanceof ITunableItem tunable) {
            List<ITunableItem.Spec> specs = tunable.tuningSpecs(stack);
            if (!specs.isEmpty()) rows.add(new Row(Kind.HEADER, 0, Component.translatable("gui.mio_icif.equipment_console.tuning")));
            for (int i = 0; i < specs.size(); i++) rows.add(new Row(Kind.TUNE, i, specs.get(i)));
            for (Component line : tunable.tuningSummary(stack)) rows.add(new Row(Kind.INFO, 0, line));
        }
        return rows;
    }

    /** Visible row under the mouse, or null. */
    private Row rowAt(double mouseX, double mouseY) {
        int x = left + PX + 4, ly = top + FEATURE_Y;
        List<Row> rows = rows(current());
        for (int i = featureScroll; i < Math.min(rows.size(), featureScroll + FEATURE_ROWS); i++) {
            if (in(mouseX, mouseY, x - 2, ly + (i - featureScroll) * FEATURE_ROW - 1, PW - 6, FEATURE_ROW)) return rows.get(i);
        }
        return null;
    }

    // tuning slider geometry inside a row
    private static final int TUNE_TRACK_X = 72, TUNE_TRACK_W = 52;

    private void drawTune(GuiGraphics g, ItemStack stack, ITunableItem.Spec spec, int x, int ry, boolean hot) {
        int value = ITunableItem.get(stack, spec);
        if (hot) g.fill(x - 2, ry - 1, x + PW - 8, ry + FEATURE_ROW - 2, 0x1E3A6EA5);
        g.drawString(font, font.plainSubstrByWidth(spec.label().getString(), TUNE_TRACK_X - 4), x, ry + 1, SiGuiTheme.TEXT_SCREEN, false);
        int tx = x + TUNE_TRACK_X, ty = ry + 3;
        g.fill(tx, ty, tx + TUNE_TRACK_W, ty + 5, SiGuiTheme.OUTLINE);
        g.fill(tx + 1, ty + 1, tx + TUNE_TRACK_W - 1, ty + 4, SiGuiTheme.SCREEN_GRID);
        float f = (value - spec.min()) / (float) Math.max(1, spec.max() - spec.min());
        int fill = Math.round(f * (TUNE_TRACK_W - 2));
        if (fill > 0) g.fillGradient(tx + 1, ty + 1, tx + 1 + fill, ty + 4, SiGuiTheme.BAR_FROM, f > 0.75F ? SiGuiTheme.BAD : SiGuiTheme.BAR_TO);
        int kx = tx + 1 + fill;
        g.fill(kx - 2, ty - 2, kx + 2, ty + 7, SiGuiTheme.OUTLINE);
        g.fill(kx - 1, ty - 1, kx + 1, ty + 6, hot ? 0xFFFFFFFF : 0xFFE8EAEC);
        String v = value + spec.unit();
        g.drawString(font, v, x + PW - 10 - font.width(v), ry + 1, value == spec.def() ? SiGuiTheme.TEXT_SCREEN : SiGuiTheme.ACCENT, false);
    }

    private void setTuneFromMouse(Row row, double mouseX) {
        Entry entry = entries.get(selected);
        ItemStack stack = entry.stack();
        if (!(stack.getItem() instanceof ITunableItem)) return;
        ITunableItem.Spec spec = (ITunableItem.Spec) row.ref();
        float f = Mth.clamp((float) (mouseX - (left + PX + 4 + TUNE_TRACK_X + 1)) / (TUNE_TRACK_W - 2), 0, 1);
        setTune(entry, row.index(), spec, spec.min() + Math.round(f * (spec.max() - spec.min())));
    }

    private void setTune(Entry entry, int index, ITunableItem.Spec spec, int raw) {
        int value = spec.clamp(raw);
        if (value == ITunableItem.get(entry.stack(), spec)) return;
        ITunableItem.set(entry.stack(), spec, value);   // optimistic; the server clamps and stores it
        PacketDistributor.sendToServer(new EquipmentConsolePacket(entry.id(), EquipmentConsolePacket.SET_TUNING,
            (index << 16) | (value & 0xFFFF)));
        if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(mio_icif_sounds.UI_CLICK.get(),
            0.8F + 0.6F * (value - spec.min()) / Math.max(1F, spec.max() - spec.min()), 0.35F));
    }

    private void drawSwitch(GuiGraphics g, int sx, int sy, boolean on) {
        g.fill(sx, sy, sx + 22, sy + 10, SiGuiTheme.OUTLINE);
        g.fill(sx + 1, sy + 1, sx + 21, sy + 9, on ? 0xFF3C9A52 : 0xFFB0B4B9);
        int knob = on ? sx + 12 : sx + 1;
        g.fillGradient(knob, sy + 1, knob + 9, sy + 9, 0xFFFFFFFF, 0xFFC8CCD0);
        g.fill(on ? sx + 3 : sx + 13, sy + 4, on ? sx + 8 : sx + 18, sy + 6, on ? SiGuiTheme.GOOD : SiGuiTheme.BAD);
    }

    private void renderTooltips(GuiGraphics g, int mouseX, int mouseY) {
        ItemStack stack = current();
        if (stack.isEmpty()) return;
        // tool mode details / tuning hints
        Row hover = rowAt(mouseX, mouseY);
        if (hover != null && hover.kind() == Kind.MODE && stack.getItem() instanceof IToolModeProvider tool) {
            List<Component> lines = new ArrayList<>();
            lines.add(tool.toolModes(stack).get(hover.index()).copy().withStyle(ChatFormatting.AQUA));
            for (Component c : tool.toolModeDetails(stack, hover.index())) lines.add(c.copy().withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("gui.mio_icif.equipment_console.click_select").withStyle(ChatFormatting.DARK_GRAY));
            g.renderComponentTooltip(font, lines, mouseX, mouseY);
            return;
        }
        if (hover != null && hover.kind() == Kind.TUNE && draggingTune < 0) {
            ITunableItem.Spec spec = (ITunableItem.Spec) hover.ref();
            List<Component> lines = new ArrayList<>();
            lines.add(spec.label().copy().withStyle(ChatFormatting.AQUA));
            for (var seq : font.getSplitter().splitLines(spec.hint(), 200, net.minecraft.network.chat.Style.EMPTY))
                lines.add(Component.literal(seq.getString()).withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("gui.mio_icif.equipment_console.tuning_tip").withStyle(ChatFormatting.DARK_GRAY));
            g.renderComponentTooltip(font, lines, mouseX, mouseY);
            return;
        }
        for (int i = 0; i < 4; i++) {
            if (priorityButtons[i].visible && priorityButtons[i].isHovered()) {
                g.renderComponentTooltip(font, List.of(
                    Component.translatable("gui.mio_icif.equipment_console.allocation"),
                    Component.translatable("gui.mio_icif.equipment_console.priority_tip." + PRIORITY_KEYS[i]).withStyle(ChatFormatting.GRAY)),
                    mouseX, mouseY);
                return;
            }
        }
        for (int i = rackStart; i < Math.min(entries.size(), rackStart + RACK_ROWS); i++) {
            int x = left + LIST_X + 1, y = top + LIST_Y + 1 + (i - rackStart) * ROW_H;
            if (in(mouseX, mouseY, x + 4, y + 2, 16, 16) && !entries.get(i).stack().isEmpty()) {
                g.renderTooltip(font, entries.get(i).stack(), mouseX, mouseY);
                return;
            }
        }
    }

    private void small(GuiGraphics g, String text, int x, int y, int color) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(0.75F, 0.75F, 1);
        g.drawString(font, text, 0, 0, color, false);
        g.pose().popPose();
    }

    private static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    // ------------------------------------------------------------------ input
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (button != 0) return false;
        for (int i = rackStart; i < Math.min(entries.size(), rackStart + RACK_ROWS); i++) {
            int x = left + LIST_X + 1, y = top + LIST_Y + 1 + (i - rackStart) * ROW_H;
            if (in(mouseX, mouseY, x, y, LIST_W - 2, ROW_H)) {
                if (selected != i) { selected = i; featureScroll = 0; sound(false); }
                return true;
            }
        }
        ItemStack stack = current();
        if (stack.isEmpty()) return false;
        Entry entry = entries.get(selected);
        Row row = rowAt(mouseX, mouseY);
        if (row == null) return false;
        switch (row.kind()) {
            case FEATURE -> { toggleFeature(entry, (ArmorFeatureInfo) row.ref()); return true; }
            case MODE -> { selectMode(entry, row.index()); return true; }
            case TUNE -> { draggingTune = row.index(); setTuneFromMouse(row, mouseX); return true; }
            default -> { return false; }
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingTune >= 0) {
            for (Row row : rows(current())) if (row.kind() == Kind.TUNE && row.index() == draggingTune) setTuneFromMouse(row, mouseX);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingTune = -1;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        Row row = rowAt(mouseX, mouseY);
        if (row != null && row.kind() == Kind.TUNE && mouseX >= left + PX + 4 + TUNE_TRACK_X - 4) {   // wheel over a slider steps it
            ITunableItem.Spec spec = (ITunableItem.Spec) row.ref();
            Entry entry = entries.get(selected);
            setTune(entry, row.index(), spec, ITunableItem.get(entry.stack(), spec) + (int) Math.signum(scrollY) * spec.step());
            return true;
        }
        if (in(mouseX, mouseY, left + PX, top + FEATURE_Y, PW, FEATURE_ROWS * FEATURE_ROW)) {
            featureScroll = Math.max(0, featureScroll - (int) Math.signum(scrollY));
            return true;
        }
        if (in(mouseX, mouseY, left + LIST_X, top + LIST_Y, LIST_W, Math.min(entries.size(), RACK_ROWS) * ROW_H)) {
            selected = Mth.clamp(selected - (int) Math.signum(scrollY), 0, Math.max(0, entries.size() - 1));
            featureScroll = 0;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 || (minecraft != null && minecraft.options.keyInventory.matches(keyCode, scanCode))) {
            onClose();
            return true;
        }
        if (keyCode == 265 || keyCode == 264) {   // up / down
            selected = Mth.clamp(selected + (keyCode == 264 ? 1 : -1), 0, Math.max(0, entries.size() - 1));
            featureScroll = 0;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
