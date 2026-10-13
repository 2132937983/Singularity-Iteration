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

    // ---- sections: equipment settings / energy statistics / armory (summon, return, showcases)
    public static final int TAB_EQUIP = 0, TAB_ENERGY = 1, TAB_ARMORY = 2, TAB_APPEARANCE = 3;
    private static final String[] TAB_KEYS = {"equip", "energy", "armory", "appearance"};
    private static final int TAB_COUNT = TAB_KEYS.length;
    private static final int TAB_W = 46, TAB_H = 13;
    private int tab = TAB_EQUIP;
    private int armoryScroll;
    private int refreshTimer;

    /** Opens on the given section. */
    public mio_icif_gui_armor_features tab(int tab) {
        this.tab = Mth.clamp(tab, 0, TAB_COUNT - 1);
        return this;
    }

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
        addRenderableWidget(new SiButton(left + W - 64, top + H + 3, 64, 12,
            Component.translatable("gui.mio_icif.armor_features.hud_layout"),
            b -> minecraft.setScreen(new com.miophas.singularity_iteration.common.client.suit.HudLayoutScreen(this))));
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
            priorityButtons[i].visible = chargeable && tab == TAB_EQUIP;
            priorityButtons[i].selected(p.ordinal() == i);
        }
        if (tab == TAB_ARMORY && refreshTimer-- <= 0) {
            refreshTimer = 20;
            PacketDistributor.sendToServer(new com.miophas.singularity_iteration.common.armory.ArmoryConsolePacket(
                com.miophas.singularity_iteration.common.armory.ArmoryRemoteService.REQUEST, 0));
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
        // 0.1.8.0: the title stops before the tab strip (long translations of the panel name)
        com.miophas.singularity_iteration.common.client.screen.mio_icif_screen.drawFitted(g, font, title, left + 8, top + 6, tabX(0) - left - 12, SiGuiTheme.TEXT, false);
        renderTabs(g, mouseX, mouseY);
        switch (tab) {
            case TAB_ENERGY -> renderEnergy(g, mouseX, mouseY);
            case TAB_ARMORY -> renderArmory(g, mouseX, mouseY);
            case TAB_APPEARANCE -> renderAppearance(g, mouseX, mouseY);
            default -> {
                SiGuiTheme.well(g, left + LIST_X, top + LIST_Y, LIST_W, H - LIST_Y - 7);
                SiGuiTheme.well(g, left + PX, top + LIST_Y, PW, H - LIST_Y - 7);
                renderRack(g, mouseX, mouseY);
                renderDetail(g, mouseX, mouseY);
            }
        }
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
                case LATTICE -> drawLattice(g, stack, x, ry);
                case CORE -> drawCore(g, x, ry);
                case BURST -> drawBurst(g, row.index(), x, ry, hot);
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
    private enum Kind { FEATURE, HEADER, MODE, TUNE, INFO, LATTICE, CORE, BURST }
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
        // 0.1.8.5 nano lattice: LP bar, nano core gauges and the two burst keys
        if (com.miophas.singularity_iteration.common.suit.SuitModules.isNanoPiece(stack)) {
            rows.add(new Row(Kind.HEADER, 0, Component.translatable("gui.mio_icif.equipment_console.nano")));
            rows.add(new Row(Kind.LATTICE, 0, null));
            rows.add(new Row(Kind.CORE, 0, null));
            rows.add(new Row(Kind.BURST, 0, null));
            rows.add(new Row(Kind.BURST, 1, null));
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

    // ------------------------------------------------------------------ nano lattice rows (0.1.8.5)

    private void drawLattice(GuiGraphics g, ItemStack stack, int x, int ry) {
        var units = com.miophas.singularity_iteration.common.suit.SuitModules.installed(stack).units();
        int cap = com.miophas.singularity_iteration.common.suit.SuitModules.latticeCapacity(stack);
        int used = com.miophas.singularity_iteration.common.suit.SuitModules.latticeUsed(stack);
        small(g, Component.translatable("gui.mio_icif.equipment_console.lattice", used, cap).getString(), x, ry + 3, SiGuiTheme.TEXT_SCREEN);
        int bx = x + 62, bw = PW - 82, by = ry + 3;
        g.fill(bx - 1, by - 1, bx + bw + 1, by + 6, SiGuiTheme.OUTLINE);
        float cell = bw / (float) Math.max(1, cap);
        int at = 0;
        for (var u : units) for (int k = 0; k < u.latticePoints() && at < cap; k++, at++)
            g.fill(bx + Math.round(at * cell), by, bx + Math.round((at + 1) * cell) - 1, by + 5, 0xFF000000 | u.color());
        for (; at < cap; at++) g.fill(bx + Math.round(at * cell), by, bx + Math.round((at + 1) * cell) - 1, by + 5, SiGuiTheme.SCREEN_GRID);
        int level = com.miophas.singularity_iteration.common.suit.SuitModules.latticeLevel(stack);
        for (int k = 0; k < com.miophas.singularity_iteration.common.suit.SuitModules.LATTICE_MAX_LEVEL; k++) {
            int dx = x + PW - 15 + k * 5, dy = ry + 5;
            int c = k < level ? 0xFFE0A21B : 0x60E0A21B;
            g.fill(dx - 1, dy - 2, dx + 1, dy + 2, c);
            g.fill(dx - 2, dy - 1, dx + 2, dy + 1, c);
        }
    }

    private void drawCore(GuiGraphics g, int x, int ry) {
        var sync = com.miophas.singularity_iteration.common.client.suit.nano.NanoClient.sync();
        if (sync == null || !sync.online()) {
            small(g, Component.translatable("gui.mio_icif.equipment_console.nano_offline").getString(), x, ry + 3, SiGuiTheme.TEXT_SCREEN_DIM);
            return;
        }
        small(g, Component.translatable("gui.mio_icif.equipment_console.reserve").getString(), x, ry + 3, SiGuiTheme.TEXT_SCREEN);
        SiGuiTheme.bar(g, x + 34, ry + 4, 40, 4, sync.reserve() / 100.0, 0xFF2DB8D8, 0xFF7FE9FF);
        small(g, Component.translatable("gui.mio_icif.equipment_console.heat").getString(), x + 80, ry + 3, sync.overheated() ? SiGuiTheme.BAD : SiGuiTheme.TEXT_SCREEN);
        SiGuiTheme.bar(g, x + 104, ry + 4, PW - 116, 4, sync.heat() / Math.max(1F, sync.heatCap()), 0xFFE0A21B, sync.overheated() ? 0xFFE04030 : 0xFFF0C060);
    }

    private void drawBurst(GuiGraphics g, int slot, int x, int ry, boolean hot) {
        if (hot) g.fill(x - 2, ry - 1, x + PW - 8, ry + FEATURE_ROW - 2, 0x263A6EA5);
        var sync = com.miophas.singularity_iteration.common.client.suit.nano.NanoClient.sync();
        String key = slot == 0 ? "V" : "B";
        net.minecraft.client.KeyMapping mapping = slot == 0 ? com.miophas.singularity_iteration.common.client.suit.nano.NanoClient.PRIMARY_KEY
            : com.miophas.singularity_iteration.common.client.suit.nano.NanoClient.SECONDARY_KEY;
        if (mapping != null) key = mapping.getTranslatedKeyMessage().getString();
        g.drawString(font, font.plainSubstrByWidth(Component.translatable("gui.mio_icif.equipment_console.burst_" + (slot == 0 ? "primary" : "secondary"), key).getString(), 70),
            x, ry + 1, SiGuiTheme.TEXT_SCREEN, false);
        String choice = sync == null ? "" : slot == 0 ? sync.primaryChoice() : sync.secondaryChoice();
        String effective = sync == null ? "" : slot == 0 ? sync.primaryKey() : sync.secondaryKey();
        String name = effective.isEmpty() ? Component.translatable("gui.mio_icif.equipment_console.burst_none").getString()
            : Component.translatable(burstNameKey(effective)).getString();
        if (choice.isEmpty() && !effective.isEmpty()) name = name + " " + Component.translatable("gui.mio_icif.equipment_console.burst_auto").getString();
        String label = "\u25C0 " + font.plainSubstrByWidth(name, PW - 96) + " \u25B6";
        g.drawString(font, label, x + PW - 10 - font.width(label), ry + 1, SiGuiTheme.ACCENT, false);
    }

    private static String burstNameKey(String key) {
        var unit = com.miophas.singularity_iteration.common.suit.UnitRef.byKey(key);
        return unit == null ? key : unit.nameKey();
    }

    /** Cycles burst key {@code slot} through: automatic, each worn burst unit, nothing. */
    private void cycleBurst(int slot, int direction) {
        if (minecraft == null || minecraft.player == null) return;
        var sync = com.miophas.singularity_iteration.common.client.suit.nano.NanoClient.sync();
        List<String> options = new ArrayList<>();
        options.add("");
        for (var unit : com.miophas.singularity_iteration.common.suit.nano.NanoSuit.availableBursts(minecraft.player)) options.add(unit.key());
        options.add("-");
        String current = sync == null ? "" : slot == 0 ? sync.primaryChoice() : sync.secondaryChoice();
        int i = Math.max(0, options.indexOf(current));
        String next = options.get(Math.floorMod(i + direction, options.size()));
        com.miophas.singularity_iteration.common.client.suit.nano.NanoClient.assign(slot, next);
        sound(true);
    }

    private void drawSwitch(GuiGraphics g, int sx, int sy, boolean on) {
        g.fill(sx, sy, sx + 22, sy + 10, SiGuiTheme.OUTLINE);
        g.fill(sx + 1, sy + 1, sx + 21, sy + 9, on ? 0xFF3C9A52 : 0xFFB0B4B9);
        int knob = on ? sx + 12 : sx + 1;
        g.fillGradient(knob, sy + 1, knob + 9, sy + 9, 0xFFFFFFFF, 0xFFC8CCD0);
        g.fill(on ? sx + 3 : sx + 13, sy + 4, on ? sx + 8 : sx + 18, sy + 6, on ? SiGuiTheme.GOOD : SiGuiTheme.BAD);
    }

    private void renderTooltips(GuiGraphics g, int mouseX, int mouseY) {
        for (int i = 0; i < TAB_COUNT; i++) {
            if (in(mouseX, mouseY, tabX(i), top + 4, TAB_W, TAB_H)) {
                g.renderComponentTooltip(font, List.of(Component.translatable("gui.mio_icif.equipment_console.tab." + TAB_KEYS[i]),
                    Component.translatable("gui.mio_icif.equipment_console.tab." + TAB_KEYS[i] + ".tip").withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
                return;
            }
        }
        if (tab == TAB_ENERGY) { energyTooltips(g, mouseX, mouseY); return; }
        if (tab == TAB_ARMORY) { armoryTooltips(g, mouseX, mouseY); return; }
        if (tab == TAB_APPEARANCE) return;
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
        if (button == 0) {
            for (int i = 0; i < TAB_COUNT; i++) {
                if (in(mouseX, mouseY, tabX(i), top + 4, TAB_W, TAB_H)) {
                    if (tab != i) { tab = i; refreshTimer = 0; armoryScroll = 0; sound(false); }
                    return true;
                }
            }
        }
        if (tab == TAB_ARMORY) return button == 0 && armoryClick(mouseX, mouseY);
        if (tab == TAB_APPEARANCE) return button == 0 && appearanceClick(mouseX, mouseY);
        if (tab == TAB_ENERGY) return false;
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
            case BURST -> { cycleBurst(row.index(), mouseX < left + PX + 4 + PW / 2 + 10 ? -1 : 1); return true; }
            default -> { return false; }
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (paletteDrag >= 0) { paletteSlider(mouseX); return true; }
        if (draggingTune >= 0) {
            for (Row row : rows(current())) if (row.kind() == Kind.TUNE && row.index() == draggingTune) setTuneFromMouse(row, mouseX);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingTune = -1;
        if (paletteDrag >= 0) { paletteDrag = -1; paletteSend(false); }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (tab == TAB_ARMORY) {
            armoryScroll = Math.max(0, armoryScroll - (int) Math.signum(scrollY));
            return true;
        }
        if (tab != TAB_EQUIP) return false;
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
        if (keyCode == 258) {   // tab: next section
            tab = (tab + 1) % TAB_COUNT;
            refreshTimer = 0;
            sound(false);
            return true;
        }
        if (tab == TAB_EQUIP && (keyCode == 265 || keyCode == 264)) {   // up / down
            selected = Mth.clamp(selected + (keyCode == 264 ? 1 : -1), 0, Math.max(0, entries.size() - 1));
            featureScroll = 0;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // ================================================================== section tabs
    private int tabX(int i) {
        return left + W - 7 - (TAB_COUNT - i) * (TAB_W + 2);
    }

    private void renderTabs(GuiGraphics g, int mouseX, int mouseY) {
        for (int i = 0; i < TAB_COUNT; i++) {
            int x = tabX(i), y = top + 4;
            boolean on = tab == i, hot = in(mouseX, mouseY, x, y, TAB_W, TAB_H);
            g.fill(x, y, x + TAB_W, y + TAB_H, SiGuiTheme.OUTLINE);
            if (on) {
                g.fill(x + 1, y + 1, x + TAB_W - 1, y + TAB_H - 1, DspUi.BG);
                g.fill(x + 1, y + TAB_H - 2, x + TAB_W - 1, y + TAB_H - 1, DspUi.CYAN);
            } else {
                g.fillGradient(x + 1, y + 1, x + TAB_W - 1, y + TAB_H - 1, hot ? 0xFFF3F8FC : 0xFFE6E9EC, hot ? 0xFFC9DCEE : 0xFFC4C8CD);
            }
            // pictogram: rack / bolt / suit
            int ix = x + 4, iy = y + 3, c = on ? DspUi.CYAN : 0xFF4A5560;
            switch (i) {
                case 0 -> { g.fill(ix, iy, ix + 2, iy + 7, c); g.fill(ix + 3, iy, ix + 5, iy + 7, c); g.fill(ix + 6, iy, ix + 8, iy + 7, c); }
                case 1 -> { g.fill(ix + 4, iy, ix + 6, iy + 3, c); g.fill(ix + 2, iy + 3, ix + 7, iy + 4, c); g.fill(ix + 2, iy + 4, ix + 4, iy + 7, c); }
                case 3 -> { g.fill(ix, iy, ix + 3, iy + 3, c); g.fill(ix + 5, iy, ix + 8, iy + 3, c); g.fill(ix, iy + 4, ix + 3, iy + 7, c); g.renderOutline(ix + 5, iy + 4, 3, 3, c); }
                default -> { g.fill(ix + 2, iy, ix + 6, iy + 2, c); g.fill(ix, iy + 2, ix + 8, iy + 5, c); g.fill(ix + 1, iy + 5, ix + 3, iy + 7, c); g.fill(ix + 5, iy + 5, ix + 7, iy + 7, c); }
            }
            String label = font.plainSubstrByWidth(Component.translatable("gui.mio_icif.equipment_console.tab." + TAB_KEYS[i]).getString(), (int) ((TAB_W - 16) / 0.75F));
            small(g, label, x + 14, y + 4, on ? DspUi.TEXT : SiGuiTheme.TEXT_SCREEN);
        }
    }

    // ================================================================== ENERGY: DSP-style statistics
    private static final int EX = 7, EY = 18, EW = 286, EH = H - 18 - 7;

    private void renderEnergy(GuiGraphics g, int mouseX, int mouseY) {
        int x0 = left + EX, y0 = top + EY;
        DspUi.screen(g, x0, y0, EW, EH);
        double income = EquipmentEnergyMonitor.income(), drain = EquipmentEnergyMonitor.drain(), netRate = income - drain;
        long stored = EquipmentEnergyMonitor.stored(), cap = EquipmentEnergyMonitor.capacity();
        double peak = Math.max(1, Math.max(income, drain));
        String eus = " EU/s";
        // ---- KPI tiles
        kpi(g, x0 + 4, y0 + 4, "income", DspUi.compact(income), eus, DspUi.GREEN, (float) (income / peak));
        kpi(g, x0 + 74, y0 + 4, "drain", DspUi.compact(drain), eus, DspUi.ORANGE, (float) (drain / peak));
        kpi(g, x0 + 144, y0 + 4, "net", (netRate >= 0 ? "+" : "") + DspUi.compact(netRate), eus, netRate >= 0 ? DspUi.CYAN : DspUi.RED,
            (float) (0.5 + 0.5 * Mth.clamp(netRate / peak, -1, 1)));
        double eta = EquipmentEnergyMonitor.depletionSeconds();
        boolean draining = Double.isFinite(eta);
        double full = EquipmentEnergyMonitor.fullSeconds();
        String etaKey = draining ? "depletion" : Double.isFinite(full) ? "full" : "steady";
        int etaColor = !draining ? DspUi.GREEN : eta < 60 ? DspUi.RED : eta < 600 ? DspUi.ORANGE : DspUi.TEXT;
        String etaValue = draining ? DspUi.duration(eta) : Double.isFinite(full) ? DspUi.duration(full) : "∞";
        kpi(g, x0 + 214, y0 + 4, etaKey, etaValue, "", etaColor, cap > 0 ? (float) stored / cap : 0);
        if (draining && eta < 60 && (System.currentTimeMillis() / 400) % 2 == 0) g.renderOutline(x0 + 214, y0 + 4, 68, 36, DspUi.RED);

        // ---- trend (60 s): income / drain areas, stored charge line
        int cx = x0 + 4, cy = y0 + 46, cw = 152, ch = 58;
        g.fill(cx, cy, cx + cw, cy + ch, 0x40000000);
        DspUi.small(g, font, Component.translatable("gui.mio_icif.energy_stats.trend").getString(), cx + 2, cy + 2, DspUi.CYAN_DIM);
        float[] in = EquipmentEnergyMonitor.incomeHistory(), out = EquipmentEnergyMonitor.drainHistory(), st = EquipmentEnergyMonitor.storedHistory();
        if (in.length > 1) {
            float max = 1;
            for (int i = 0; i < in.length; i++) max = Math.max(max, Math.max(in[i], out[i]));
            int gx = cx + cw - Math.round(cw * in.length / (float) EquipmentEnergyMonitor.HISTORY);
            int gw = cx + cw - gx;
            DspUi.area(g, gx, cy + 10, gw, ch - 12, in, max * 1.1F, DspUi.GREEN);
            DspUi.area(g, gx, cy + 10, gw, ch - 12, out, max * 1.1F, DspUi.ORANGE);
            DspUi.line(g, gx, cy + 10, gw, ch - 12, st, Math.max(1, cap), DspUi.CYAN);
            String peakLabel = DspUi.compact(max) + eus;
            DspUi.small(g, font, peakLabel, cx + cw - 2 - DspUi.smallWidth(font, peakLabel), cy + 2, DspUi.TEXT_DIM);
        } else {
            DspUi.small(g, font, Component.translatable("gui.mio_icif.energy_stats.sampling").getString(), cx + 30, cy + 26, DspUi.TEXT_DIM);
        }
        legend(g, cx + 2, cy + ch + 2, DspUi.GREEN, "income");
        legend(g, cx + 50, cy + ch + 2, DspUi.ORANGE, "drain");
        legend(g, cx + 96, cy + ch + 2, DspUi.CYAN, "charge");

        // ---- biggest drain source
        var ranking = EquipmentEnergyMonitor.ranking();
        EquipmentEnergyMonitor.Source top = null;
        for (var src : ranking) {
            if (src.drainPerSecond() <= 0) break;
            if (!MioIcifAPI.instance().getItemAPI().isBattery(src.stack())) { top = src; break; }
            if (top == null) top = src;
        }
        int bx = x0 + 4, by = y0 + 116, bw = 152, bh = EH - 120;
        g.fill(bx, by, bx + bw, by + bh, 0x40000000);
        g.fill(bx, by, bx + 2, by + bh, DspUi.ORANGE);
        DspUi.small(g, font, Component.translatable("gui.mio_icif.energy_stats.top_drain").getString(), bx + 5, by + 3, DspUi.ORANGE);
        if (top == null) {
            DspUi.small(g, font, Component.translatable("gui.mio_icif.energy_stats.no_drain").getString(), bx + 5, by + 18, DspUi.TEXT_DIM);
        } else {
            g.renderItem(top.stack(), bx + 5, by + 12);
            g.drawString(font, font.plainSubstrByWidth(top.stack().getHoverName().getString(), bw - 28), bx + 24, by + 12, DspUi.TEXT, false);
            String rate = "-" + DspUi.compact(top.drainPerSecond()) + eus + "  " + Math.round(100 * top.drainPerSecond() / Math.max(1e-6, drain)) + "%";
            DspUi.small(g, font, rate, bx + 24, by + 22, DspUi.ORANGE);
            List<String> feats = activeFeatures(top.stack());
            String f = feats.isEmpty() ? Component.translatable("gui.mio_icif.energy_stats.passive").getString() : String.join(" · ", feats);
            int ly = by + 32;
            for (var line : font.getSplitter().splitLines(f, (int) ((bw - 10) / 0.75F), net.minecraft.network.chat.Style.EMPTY)) {
                if (ly > by + bh - 7) break;
                DspUi.small(g, font, line.getString(), bx + 5, ly, DspUi.TEXT_DIM);
                ly += 7;
            }
            long[] en = EquipmentEnergyMonitor.energy(top.stack());
            if (top.drainPerSecond() > top.incomePerSecond()) {
                String own = Component.translatable("gui.mio_icif.energy_stats.item_eta",
                    DspUi.duration(en[0] / (top.drainPerSecond() - top.incomePerSecond()))).getString();
                DspUi.small(g, font, own, bx + bw - 4 - DspUi.smallWidth(font, own), by + 3, DspUi.TEXT);
            }
        }

        // ---- drain ranking (worst first)
        int rx = x0 + 160, ry = y0 + 46, rw = EW - 164, rh = EH - 50;
        g.fill(rx, ry, rx + rw, ry + rh, 0x40000000);
        DspUi.small(g, font, Component.translatable("gui.mio_icif.energy_stats.ranking").getString(), rx + 3, ry + 2, DspUi.CYAN_DIM);
        double maxDrain = 1e-6;
        for (var src : ranking) maxDrain = Math.max(maxDrain, src.drainPerSecond());
        int row = 0;
        for (var src : ranking) {
            int yy = ry + 11 + row * 14;
            if (yy + 13 > ry + rh) break;
            boolean idle = src.drainPerSecond() <= 0;
            g.pose().pushPose();
            g.pose().translate(rx + 2, yy, 0);
            g.pose().scale(0.75F, 0.75F, 1);
            g.renderItem(src.stack(), 0, 0);
            g.pose().popPose();
            String name = font.plainSubstrByWidth(src.stack().getHoverName().getString(), (int) ((rw - 54) / 0.75F));
            DspUi.small(g, font, name, rx + 16, yy + 1, idle ? DspUi.TEXT_DIM : DspUi.TEXT);
            String v = idle ? (src.incomePerSecond() > 0 ? "+" + DspUi.compact(src.incomePerSecond()) : "0") : "-" + DspUi.compact(src.drainPerSecond());
            DspUi.small(g, font, v, rx + rw - 3 - DspUi.smallWidth(font, v), yy + 1, idle ? (src.incomePerSecond() > 0 ? DspUi.GREEN : DspUi.TEXT_DIM) : DspUi.ORANGE);
            DspUi.meter(g, rx + 16, yy + 8, rw - 20, 2, (float) (src.drainPerSecond() / maxDrain), row == 0 && !idle ? DspUi.RED : DspUi.ORANGE);
            row++;
        }
        if (ranking.isEmpty()) DspUi.small(g, font, Component.translatable("gui.mio_icif.energy_stats.no_items").getString(), rx + 4, ry + 16, DspUi.TEXT_DIM);
    }

    private void kpi(GuiGraphics g, int x, int y, String key, String value, String unit, int color, float fraction) {
        g.fill(x, y, x + 68, y + 36, 0x40000000);
        g.fill(x, y, x + 68, y + 1, (color & 0x00FFFFFF) | 0x90000000);
        DspUi.small(g, font, Component.translatable("gui.mio_icif.energy_stats." + key).getString(), x + 3, y + 3, DspUi.TEXT_DIM);
        g.drawString(font, value, x + 3, y + 12, color, false);
        if (!unit.isEmpty()) DspUi.small(g, font, unit.strip(), x + 5 + font.width(value), y + 14, DspUi.TEXT_DIM);
        DspUi.meter(g, x + 3, y + 26, 62, 3, fraction, color);
    }

    private void legend(GuiGraphics g, int x, int y, int color, String key) {
        g.fill(x, y + 2, x + 5, y + 4, color);
        DspUi.small(g, font, Component.translatable("gui.mio_icif.energy_stats." + key).getString(), x + 7, y, DspUi.TEXT_DIM);
    }

    private static List<String> activeFeatures(ItemStack stack) {
        List<String> out = new ArrayList<>();
        for (ArmorFeatureInfo info : ArmorFeatures.features(stack)) {
            if (info.isMode()) continue;
            if (ArmorFeatures.isEnabled(stack, info.featureKey())) out.add(Component.translatable(info.featureNameKey()).getString());
        }
        return out;
    }

    private void energyTooltips(GuiGraphics g, int mouseX, int mouseY) {
        int x0 = left + EX, y0 = top + EY;
        String[] keys = {"income", "drain", "net", "eta"};
        for (int i = 0; i < 4; i++) {
            if (in(mouseX, mouseY, x0 + 4 + i * 70, y0 + 4, 68, 36)) {
                g.renderComponentTooltip(font, List.of(Component.translatable("gui.mio_icif.energy_stats." + keys[i] + ".tip").withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
                return;
            }
        }
        int rx = x0 + 160, ry = y0 + 46;
        var ranking = EquipmentEnergyMonitor.ranking();
        for (int row = 0; row < ranking.size(); row++) {
            if (in(mouseX, mouseY, rx, ry + 11 + row * 14, EW - 164, 14)) {
                var src = ranking.get(row);
                List<Component> lines = new ArrayList<>();
                lines.add(src.stack().getHoverName().copy().withStyle(ChatFormatting.AQUA));
                lines.add(Component.translatable("gui.mio_icif.energy_stats.row", DspUi.compact(src.drainPerSecond()), DspUi.compact(src.incomePerSecond()),
                    String.format("%,d / %,d", src.stored(), src.capacity())).withStyle(ChatFormatting.GRAY));
                for (String f : activeFeatures(src.stack())) lines.add(Component.literal(" • " + f).withStyle(ChatFormatting.DARK_AQUA));
                g.renderComponentTooltip(font, lines, mouseX, mouseY);
                return;
            }
        }
    }

    // ================================================================== ARMORY: remote summon / return / showcases
    private static final int AROW = 18, AROWS = 7;
    // kind: 0 header-suits, 1 suit, 2 header-showcases, 3 showcase, 4 empty hint,
    //       5 header-wingmen (0.1.8.0), 6 wingman
    private record ARow(int kind, int index) { }

    // ---- 0.1.8.0 Wingman: the suit button deploys a Wingman instead of a summon; mode for deploy and for all Wingmen
    /** What the suit button does: 0 = wear (summon), 1 = deploy a Wingman, 2 = equip a companion. */
    private int deployTarget;
    private int wingmanMode;
    private static final int TOGGLE_W = 46, TOGGLE_H = 11;
    private static final String[] TOGGLE_KEYS = {"gui.mio_icif.wingman.toggle.wear", "gui.mio_icif.wingman.toggle.wingman", "gui.mio_icif.wingman.toggle.companion"};
    /** Left edge of toggle button {@code i} (0 = wear, 1 = Wingman, 2 = companion), right-aligned in the header row. */
    private int toggleX(int i) { return left + EX + EW - 4 - (3 - i) * (TOGGLE_W + 2) + 2; }

    private List<ARow> armoryRows(com.miophas.singularity_iteration.common.armory.ArmoryConsoleSyncPacket data) {
        List<ARow> rows = new ArrayList<>();
        var snap = data.snapshot();
        rows.add(new ARow(0, 0));
        if (snap != null && snap.status() == com.miophas.singularity_iteration.common.armory.ArmorySnapshot.OK) {
            // stocked suits, plus the first empty one (a target to fill); the rest stay folded away
            boolean emptyShown = false;
            for (int s = 0; s < snap.suits().size(); s++) {
                if (snap.pieces(s) == 0) { if (emptyShown) continue; emptyShown = true; }
                rows.add(new ARow(1, s));
            }
        }
        if (deployTarget == 1 || !data.wingmen().isEmpty()) {
            rows.add(new ARow(5, 0));
            for (int i = 0; i < data.wingmen().size(); i++) rows.add(new ARow(6, i));
        }
        rows.add(new ARow(2, 0));
        if (data.showcases().isEmpty()) rows.add(new ARow(4, 0));
        for (int i = 0; i < data.showcases().size(); i++) rows.add(new ARow(3, i));
        return rows;
    }

    private void renderArmory(GuiGraphics g, int mouseX, int mouseY) {
        int x0 = left + EX, y0 = top + EY;
        DspUi.screen(g, x0, y0, EW, EH);
        var data = com.miophas.singularity_iteration.common.armory.ArmoryConsoleSyncPacket.latest();
        if (data == null) {
            DspUi.small(g, font, Component.translatable("gui.mio_icif.armory_console.connecting").getString(), x0 + 8, y0 + 10, DspUi.TEXT_DIM);
            return;
        }
        if (!data.hasRemote() || data.snapshot() == null) {
            String key = data.hasRemote() ? "gui.mio_icif.armory_console.unpaired" : "gui.mio_icif.armory_console.no_remote";
            int ly = y0 + 12;
            for (var line : font.getSplitter().splitLines(Component.translatable(key).getString(), EW - 16, net.minecraft.network.chat.Style.EMPTY)) {
                g.drawString(font, line.getString(), x0 + 8, ly, DspUi.ORANGE, false);
                ly += 10;
            }
            ly += 4;
            for (var line : font.getSplitter().splitLines(Component.translatable("gui.mio_icif.armory_console.howto").getString(), (int) ((EW - 16) / 0.75F), net.minecraft.network.chat.Style.EMPTY)) {
                DspUi.small(g, font, line.getString(), x0 + 8, ly, DspUi.TEXT_DIM);
                ly += 8;
            }
            return;
        }
        var snap = data.snapshot();
        // ---- header: link, status, energy, return button
        String where = snap.armory().pos().toShortString() + "  " + snap.armory().dimension().location().getPath();
        DspUi.small(g, font, font.plainSubstrByWidth(Component.translatable("gui.mio_icif.armory_console.linked", where).getString(), (int) (140 / 0.75F)),
            x0 + 5, y0 + 4, DspUi.CYAN);
        String status = Component.translatable("gui.mio_icif.armory_console.status." + snap.status()).getString() + (snap.busy() ? " · " + Component.translatable("gui.mio_icif.armory_console.busy").getString() : "");
        DspUi.small(g, font, status, x0 + 5, y0 + 12, snap.status() == 0 ? (snap.busy() ? DspUi.ORANGE : DspUi.GREEN) : DspUi.RED);
        DspUi.meter(g, x0 + 150, y0 + 6, 62, 3, snap.capacity() > 0 ? (float) snap.energy() / snap.capacity() : 0, DspUi.CYAN);
        DspUi.small(g, font, DspUi.compact(snap.energy()) + " / " + DspUi.compact(snap.capacity()) + " EU", x0 + 150, y0 + 12, DspUi.TEXT_DIM);
        int rbx = x0 + EW - 68, rby = y0 + 4;
        dspButton(g, rbx, rby, 64, 14, Component.translatable("gui.mio_icif.armory_console.return").getString(), in(mouseX, mouseY, rbx, rby, 64, 14), DspUi.VIOLET, snap.status() == 0);

        // ---- list
        List<ARow> rows = armoryRows(data);
        armoryScroll = Mth.clamp(armoryScroll, 0, Math.max(0, rows.size() - AROWS));
        int ly = y0 + 24;
        for (int i = armoryScroll; i < Math.min(rows.size(), armoryScroll + AROWS); i++) {
            ARow r = rows.get(i);
            int yy = ly + (i - armoryScroll) * AROW;
            switch (r.kind()) {
                case 0, 2 -> {
                    String head = Component.translatable(r.kind() == 0 ? "gui.mio_icif.armory_console.suits" : "gui.mio_icif.armory_console.showcases").getString();
                    DspUi.small(g, font, head, x0 + 5, yy + 8, DspUi.CYAN);
                    int lineEnd = r.kind() == 0 ? toggleX(0) - 4 : x0 + EW - 5;
                    g.fill(x0 + 8 + DspUi.smallWidth(font, head), yy + 11, lineEnd, yy + 12, DspUi.FRAME);
                    if (r.kind() == 0) {
                        // 0.1.8.0: what the suit button does: wear the suit, deploy it as a Wingman, or equip a companion
                        int[] colors = {DspUi.CYAN, DspUi.GREEN, DspUi.ORANGE};
                        for (int t = 0; t < 3; t++) {
                            dspButton(g, toggleX(t), yy + 4, TOGGLE_W, TOGGLE_H, Component.translatable(TOGGLE_KEYS[t]).getString(),
                                in(mouseX, mouseY, toggleX(t), yy + 4, TOGGLE_W, TOGGLE_H), deployTarget == t ? colors[t] : DspUi.TEXT_DIM, true);
                        }
                    }
                }
                case 5 -> {
                    String head = Component.translatable("gui.mio_icif.wingman.header", data.wingmen().size(),
                        com.miophas.singularity_iteration.common.armory.WingmanService.MAX_PER_PLAYER).getString();
                    DspUi.small(g, font, head, x0 + 5, yy + 8, DspUi.GREEN);
                    g.fill(x0 + 8 + DspUi.smallWidth(font, head), yy + 11, x0 + EW - 140, yy + 12, DspUi.FRAME);
                    var mode = com.miophas.singularity_iteration.common.armory.WingmanMode.byId(wingmanMode);
                    dspButton(g, x0 + EW - 136, yy + 2, 66, 13, Component.translatable(mode.key()).getString(),
                        in(mouseX, mouseY, x0 + EW - 136, yy + 2, 66, 13), DspUi.GREEN, true);
                    dspButton(g, x0 + EW - 68, yy + 2, 64, 13, Component.translatable("gui.mio_icif.wingman.recall_all").getString(),
                        in(mouseX, mouseY, x0 + EW - 68, yy + 2, 64, 13), DspUi.VIOLET, !data.wingmen().isEmpty());
                }
                case 6 -> {
                    var w = data.wingmen().get(r.index());
                    boolean hot = in(mouseX, mouseY, x0 + 3, yy, EW - 6, AROW - 1);
                    if (hot) g.fill(x0 + 3, yy, x0 + EW - 3, yy + AROW - 1, 0x3050E080);
                    String name = w.set() >= 0 && w.set() < snap.names().size() ? snap.names().get(w.set()) : "#" + (w.set() + 1);
                    g.drawString(font, font.plainSubstrByWidth(name, 70), x0 + 6, yy + 2, DspUi.TEXT, false);
                    String wingState = w.state() == 0
                        ? Component.translatable(com.miophas.singularity_iteration.common.armory.WingmanMode.byId(w.mode()).key()).getString()
                        : Component.translatable("gui.mio_icif.wingman.state." + w.state()).getString();
                    DspUi.small(g, font, font.plainSubstrByWidth(wingState, 90), x0 + 6, yy + 11, w.state() == 0 ? DspUi.GREEN : DspUi.ORANGE);
                    icons(g, w.items(), x0 + 80, yy + 1);
                    DspUi.meter(g, x0 + 192, yy + 4, 22, 3, w.health(), w.health() > 0.35F ? DspUi.GREEN : DspUi.RED);
                    if (w.energy() >= 0) DspUi.meter(g, x0 + 192, yy + 10, 22, 3, w.energy(), DspUi.CYAN);
                    dspButton(g, x0 + EW - 68, yy + 2, 64, 13, Component.translatable("gui.mio_icif.wingman.recall").getString(),
                        in(mouseX, mouseY, x0 + EW - 68, yy + 2, 64, 13), DspUi.VIOLET, w.state() != 1);
                }
                case 4 -> DspUi.small(g, font, Component.translatable("gui.mio_icif.armory_console.no_showcases",
                    com.miophas.singularity_iteration.common.armory.ArmoryRemoteService.PLAYER_RANGE,
                    com.miophas.singularity_iteration.common.armory.ArmoryRemoteService.ARMORY_RANGE).getString(), x0 + 8, yy + 6, DspUi.TEXT_DIM);
                case 1 -> {
                    int pieces = snap.pieces(r.index());
                    boolean hot = in(mouseX, mouseY, x0 + 3, yy, EW - 6, AROW - 1);
                    if (hot) g.fill(x0 + 3, yy, x0 + EW - 3, yy + AROW - 1, 0x305FD3F5);
                    g.drawString(font, font.plainSubstrByWidth(snap.names().get(r.index()), 70), x0 + 6, yy + 5, pieces > 0 ? DspUi.TEXT : DspUi.TEXT_DIM, false);
                    icons(g, snap.suits().get(r.index()), x0 + 80, yy + 1);
                    DspUi.small(g, font, pieces + "/6", x0 + 192, yy + 6, DspUi.TEXT_DIM);
                    boolean ok = pieces > 0 && !snap.busy();
                    if (deployTarget == 1) {
                        ok &= data.wingmen().size() < com.miophas.singularity_iteration.common.armory.WingmanService.MAX_PER_PLAYER;
                        dspButton(g, x0 + EW - 68, yy + 2, 64, 13, Component.translatable("gui.mio_icif.wingman.deploy").getString(),
                            in(mouseX, mouseY, x0 + EW - 68, yy + 2, 64, 13), DspUi.GREEN, ok);
                    } else if (deployTarget == 2) {
                        dspButton(g, x0 + EW - 68, yy + 2, 64, 13, Component.translatable("gui.mio_icif.companion.equip").getString(),
                            in(mouseX, mouseY, x0 + EW - 68, yy + 2, 64, 13), DspUi.ORANGE, ok);
                    } else {
                        dspButton(g, x0 + EW - 68, yy + 2, 64, 13, Component.translatable("gui.mio_icif.armory_console.summon").getString(),
                            in(mouseX, mouseY, x0 + EW - 68, yy + 2, 64, 13), DspUi.CYAN, ok);
                    }
                }
                case 3 -> {
                    var view = data.showcases().get(r.index());
                    boolean hot = in(mouseX, mouseY, x0 + 3, yy, EW - 6, AROW - 1);
                    if (hot) g.fill(x0 + 3, yy, x0 + EW - 3, yy + AROW - 1, 0x30F5A23C);
                    String label = view.label().isEmpty() ? Component.translatable("gui.mio_icif.armory_console.showcase").getString() : view.label();
                    g.drawString(font, font.plainSubstrByWidth(label, 70), x0 + 6, yy + 2, DspUi.TEXT, false);
                    double dist = minecraft != null && minecraft.player != null ? Math.sqrt(view.pos().distToCenterSqr(minecraft.player.position())) : 0;
                    DspUi.small(g, font, view.pos().toShortString() + "  " + Math.round(dist) + "m", x0 + 6, yy + 11, DspUi.TEXT_DIM);
                    icons(g, view.items(), x0 + 80, yy + 1);
                    dspButton(g, x0 + EW - 68, yy + 2, 64, 13, Component.translatable("gui.mio_icif.armory_console.exchange").getString(),
                        in(mouseX, mouseY, x0 + EW - 68, yy + 2, 64, 13), DspUi.ORANGE, !snap.busy());
                }
                default -> { }
            }
        }
        if (rows.size() > AROWS) {
            int barH = AROWS * AROW, knob = Math.max(8, barH * AROWS / rows.size());
            int ky = ly + (barH - knob) * armoryScroll / Math.max(1, rows.size() - AROWS);
            g.fill(x0 + EW - 3, ly, x0 + EW - 2, ly + barH, DspUi.FRAME);
            g.fill(x0 + EW - 3, ky, x0 + EW - 2, ky + knob, DspUi.CYAN_DIM);
        }
        String msg = data.message().getString();
        if (!msg.isEmpty()) DspUi.small(g, font, font.plainSubstrByWidth(msg, (int) ((EW - 10) / 0.75F)), x0 + 5, y0 + EH - 8, DspUi.TEXT);
    }

    private void icons(GuiGraphics g, List<ItemStack> items, int x, int y) {
        for (int c = 0; c < items.size(); c++) {
            int ix = x + c * 18;
            g.fill(ix, y, ix + 17, y + 16, 0x40000000);
            g.fill(ix, y + 15, ix + 17, y + 16, DspUi.FRAME);
            if (!items.get(c).isEmpty()) g.renderItem(items.get(c), ix, y);
        }
    }

    private void dspButton(GuiGraphics g, int x, int y, int w, int h, String label, boolean hot, int color, boolean enabled) {
        int c = enabled ? color : DspUi.TEXT_DIM;
        g.fill(x, y, x + w, y + h, (c & 0x00FFFFFF) | (hot && enabled ? 0x70000000 : 0x30000000));
        g.renderOutline(x, y, w, h, c);
        g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, (c & 0x00FFFFFF) | 0xA0000000);
        int tw = DspUi.smallWidth(font, label);
        DspUi.small(g, font, label, x + (w - tw) / 2, y + (h - 6) / 2, enabled ? DspUi.TEXT : DspUi.TEXT_DIM);
    }

    private boolean armoryClick(double mouseX, double mouseY) {
        var data = com.miophas.singularity_iteration.common.armory.ArmoryConsoleSyncPacket.latest();
        if (data == null || data.snapshot() == null) return false;
        int x0 = left + EX, y0 = top + EY;
        if (in(mouseX, mouseY, x0 + EW - 68, y0 + 4, 64, 14)) {
            sendArmory(com.miophas.singularity_iteration.common.armory.ArmoryRemoteService.RETURN, 0);
            return true;
        }
        List<ARow> rows = armoryRows(data);
        int ly = y0 + 24;
        for (int i = armoryScroll; i < Math.min(rows.size(), armoryScroll + AROWS); i++) {
            ARow r = rows.get(i);
            int yy = ly + (i - armoryScroll) * AROW;
            if (r.kind() == 0) {
                for (int t = 0; t < 3; t++) {
                    if (in(mouseX, mouseY, toggleX(t), yy + 4, TOGGLE_W, TOGGLE_H)) { deployTarget = t; sound(false); return true; }
                }
                continue;
            }
            if (r.kind() == 5 && in(mouseX, mouseY, x0 + EW - 136, yy + 2, 66, 13)) {
                wingmanMode = (wingmanMode + 1) % com.miophas.singularity_iteration.common.armory.WingmanMode.VALUES.length;
                if (!data.wingmen().isEmpty()) sendArmory(com.miophas.singularity_iteration.common.armory.ArmoryRemoteService.WINGMAN_MODE, wingmanMode);
                else sound(false);
                return true;
            }
            if (!in(mouseX, mouseY, x0 + EW - 68, yy + 2, 64, 13)) continue;
            if (r.kind() == 1 && deployTarget == 1) {
                sendArmory(com.miophas.singularity_iteration.common.armory.ArmoryRemoteService.WINGMAN_DEPLOY, r.index() | (wingmanMode << 8));
                return true;
            }
            if (r.kind() == 1 && deployTarget == 2) {
                sendArmory(com.miophas.singularity_iteration.common.armory.ArmoryRemoteService.COMPANION_EQUIP, r.index());
                return true;
            }
            if (r.kind() == 1) { sendArmory(com.miophas.singularity_iteration.common.armory.ArmoryRemoteService.SUMMON, r.index()); return true; }
            if (r.kind() == 3) { sendArmory(com.miophas.singularity_iteration.common.armory.ArmoryRemoteService.SHOWCASE, r.index()); return true; }
            if (r.kind() == 5) {
                sendArmory(com.miophas.singularity_iteration.common.armory.ArmoryRemoteService.WINGMAN_RECALL,
                    com.miophas.singularity_iteration.common.armory.ArmoryRemoteService.ALL);
                return true;
            }
            if (r.kind() == 6) { sendArmory(com.miophas.singularity_iteration.common.armory.ArmoryRemoteService.WINGMAN_RECALL, r.index()); return true; }
        }
        return false;
    }

    private void sendArmory(int action, int index) {
        PacketDistributor.sendToServer(new com.miophas.singularity_iteration.common.armory.ArmoryConsolePacket(action, index));
        refreshTimer = 10;
        sound(true);
    }

    private void armoryTooltips(GuiGraphics g, int mouseX, int mouseY) {
        var data = com.miophas.singularity_iteration.common.armory.ArmoryConsoleSyncPacket.latest();
        if (data == null || data.snapshot() == null) return;
        int x0 = left + EX, y0 = top + EY;
        if (in(mouseX, mouseY, x0 + EW - 68, y0 + 4, 64, 14)) {
            g.renderComponentTooltip(font, List.of(Component.translatable("gui.mio_icif.armory_console.return.tip").withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
            return;
        }
        List<ARow> rows = armoryRows(data);
        int ly = y0 + 24;
        for (int i = armoryScroll; i < Math.min(rows.size(), armoryScroll + AROWS); i++) {
            ARow r = rows.get(i);
            int yy = ly + (i - armoryScroll) * AROW;
            if (r.kind() == 5 && in(mouseX, mouseY, x0 + EW - 136, yy + 2, 66, 13)) {
                var mode = com.miophas.singularity_iteration.common.armory.WingmanMode.byId(wingmanMode);
                List<Component> lines = new ArrayList<>();
                lines.add(Component.translatable(mode.key()).withStyle(ChatFormatting.GREEN));
                for (var line : font.getSplitter().splitLines(Component.translatable(mode.key() + ".tip").getString(), 200, net.minecraft.network.chat.Style.EMPTY))
                    lines.add(Component.literal(line.getString()).withStyle(ChatFormatting.GRAY));
                lines.add(Component.translatable("gui.mio_icif.wingman.mode.cycle").withStyle(ChatFormatting.DARK_GRAY));
                g.renderComponentTooltip(font, lines, mouseX, mouseY);
                return;
            }
            if (r.kind() == 0) {
                for (int t = 1; t < 3; t++) {
                    if (!in(mouseX, mouseY, toggleX(t), yy + 4, TOGGLE_W, TOGGLE_H)) continue;
                    List<Component> lines = new ArrayList<>();
                    for (var line : font.getSplitter().splitLines(Component.translatable(TOGGLE_KEYS[t].replace(".toggle.", ".toggle.tip.")).getString(),
                            220, net.minecraft.network.chat.Style.EMPTY)) lines.add(Component.literal(line.getString()).withStyle(ChatFormatting.GRAY));
                    g.renderComponentTooltip(font, lines, mouseX, mouseY);
                    return;
                }
            }
            if (r.kind() != 1 && r.kind() != 3 && r.kind() != 6) continue;
            List<ItemStack> items = r.kind() == 1 ? data.snapshot().suits().get(r.index())
                : r.kind() == 6 ? data.wingmen().get(r.index()).items() : data.showcases().get(r.index()).items();
            for (int c = 0; c < items.size(); c++) {
                if (in(mouseX, mouseY, x0 + 80 + c * 18, yy + 1, 17, 16) && !items.get(c).isEmpty()) {
                    g.renderTooltip(font, items.get(c), mouseX, mouseY);
                    return;
                }
            }
            if (r.kind() == 3 && in(mouseX, mouseY, x0 + EW - 68, yy + 2, 64, 13)) {
                g.renderComponentTooltip(font, List.of(Component.translatable("gui.mio_icif.armory_console.exchange.tip").withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
                return;
            }
        }
    }


    // ================================================================== APPEARANCE: texture style
    private static final int AX = 7, AY = 18, AW = 286, AH = H - 18 - 7;
    private static final int CARD_W = 134, CARD_H = 96;

    private int cardX(int i) { return left + AX + 6 + i * (CARD_W + 6); }
    private int cardY() { return top + AY + 22; }

    // ---- 0.1.8.0: appearance pages: 0 = texture style, 1 = suit palette
    private int appearancePage;
    private static final int SUB_W = 52, SUB_H = 11;
    private int subX(int i) { return left + AX + AW - 4 - (2 - i) * (SUB_W + 2); }
    private int subY() { return top + AY + 4; }

    private void renderAppearanceTabs(GuiGraphics g, int mouseX, int mouseY) {
        String[] keys = {"texture", "palette"};
        for (int i = 0; i < 2; i++) {
            int x = subX(i), y = subY();
            boolean on = appearancePage == i, hot = in(mouseX, mouseY, x, y, SUB_W, SUB_H);
            g.fill(x, y, x + SUB_W, y + SUB_H, on ? 0xFF15293A : hot ? 0xFF112130 : DspUi.BG_HI);
            g.renderOutline(x, y, SUB_W, SUB_H, on ? DspUi.CYAN : DspUi.FRAME);
            DspUi.small(g, font, Component.translatable("gui.mio_icif.equipment_console.appearance.page." + keys[i]).getString(),
                x + 4, y + 3, on ? DspUi.TEXT : DspUi.TEXT_DIM);
        }
    }

    private void renderAppearance(GuiGraphics g, int mouseX, int mouseY) {
        if (appearancePage == 1) {
            renderPalette(g, mouseX, mouseY);
            renderAppearanceTabs(g, mouseX, mouseY);
            return;
        }
        int x0 = left + AX, y0 = top + AY;
        DspUi.screen(g, x0, y0, AW, AH);
        renderAppearanceTabs(g, mouseX, mouseY);
        DspUi.small(g, font, Component.translatable("gui.mio_icif.equipment_console.appearance.texture_style").getString(), x0 + 6, y0 + 6, DspUi.CYAN);
        DspUi.small(g, font, Component.translatable("gui.mio_icif.equipment_console.appearance.hint").getString(), x0 + 6, y0 + 13, DspUi.TEXT_DIM);
        // card 0: Default (classic IC2, ships in assets/), card 1: Experimental (built-in pack)
        boolean experimental = com.miophas.singularity_iteration.common.client.AppearanceStyle.isExperimental();
        boolean available = com.miophas.singularity_iteration.common.client.AppearanceStyle.available();
        for (int i = 0; i < 2; i++) {
            int x = cardX(i), y = cardY();
            boolean on = (i == 1) == experimental, hot = in(mouseX, mouseY, x, y, CARD_W, CARD_H);
            g.fill(x, y, x + CARD_W, y + CARD_H, on ? 0xFF15293A : hot ? 0xFF112130 : DspUi.BG_HI);
            g.renderOutline(x, y, CARD_W, CARD_H, on ? DspUi.CYAN : hot ? DspUi.CYAN_DIM : DspUi.FRAME);
            // swatch: a little machine front in each style
            int sx = x + 8, sy = y + 18;
            if (i == 1) {
                g.fill(sx, sy, sx + 34, sy + 34, 0xFF8C96A0);
                g.fill(sx + 2, sy + 2, sx + 32, sy + 7, 0xFF3A4652);
                g.fill(sx + 4, sy + 10, sx + 30, sy + 26, 0xFF1A232C);
                g.fill(sx + 6, sy + 12, sx + 28, sy + 24, 0xFF2E4E68);
                g.fill(sx + 8, sy + 29, sx + 26, sy + 31, (System.currentTimeMillis() / 500) % 2 == 0 ? DspUi.GREEN : 0xFF2E6B45);
                g.fill(sx + 29, sy + 3, sx + 31, sy + 5, DspUi.CYAN);
            } else {
                g.fill(sx, sy, sx + 34, sy + 34, 0xFF9AA1A8);
                g.renderOutline(sx, sy, 34, 34, 0xFF5E656C);
                g.fill(sx + 9, sy + 9, sx + 25, sy + 25, 0xFF6B7178);
                g.fill(sx + 11, sy + 11, sx + 23, sy + 23, 0xFF3C4146);
            }
            String key = i == 0 ? "default" : "experimental";
            DspUi.small(g, font, Component.translatable("gui.mio_icif.equipment_console.appearance." + key).getString(), x + 6, y + 6, on ? DspUi.TEXT : DspUi.TEXT_DIM);
            int ty = sy;
            for (String line : font.getSplitter().splitLines(Component.translatable("gui.mio_icif.equipment_console.appearance." + key + ".desc").getString(),
                    (int) ((CARD_W - 52) / 0.75F), net.minecraft.network.chat.Style.EMPTY).stream().map(net.minecraft.network.chat.FormattedText::getString).toList()) {
                if (ty > y + CARD_H - 20) break;
                DspUi.small(g, font, line, x + 48, ty, DspUi.TEXT_DIM);
                ty += 7;
            }
            String state = on ? "● " + Component.translatable("gui.mio_icif.equipment_console.appearance.active").getString()
                : "○ " + Component.translatable("gui.mio_icif.equipment_console.appearance.select").getString();
            DspUi.small(g, font, state, x + 6, y + CARD_H - 10, on ? DspUi.CYAN : (available ? DspUi.TEXT_DIM : DspUi.RED));
        }
        DspUi.small(g, font, Component.translatable(available ? "gui.mio_icif.equipment_console.appearance.reload"
            : "gui.mio_icif.equipment_console.appearance.missing").getString(), x0 + 6, y0 + AH - 10, available ? DspUi.TEXT_DIM : DspUi.RED);
        // 0.1.8.0: hand cover of the nano / quantum gauntlets
        DspUi.small(g, font, Component.translatable("gui.mio_icif.equipment_console.appearance.hands").getString(), x0 + 6, handY() - 9, DspUi.CYAN);
        boolean[] states = {com.miophas.singularity_iteration.common.client.suit.FcsClientConfig.handArmorFirstPerson(),
            com.miophas.singularity_iteration.common.client.suit.FcsClientConfig.handArmorThirdPerson()};
        String[] keys = {"first_person", "third_person"};
        for (int i = 0; i < 2; i++) {
            int bx = x0 + 6 + i * 140, by = handY();
            g.renderOutline(bx, by, 9, 9, DspUi.FRAME);
            if (states[i]) g.fill(bx + 2, by + 2, bx + 7, by + 7, DspUi.CYAN);
            DspUi.small(g, font, Component.translatable("gui.mio_icif.equipment_console.appearance.hands." + keys[i]).getString(), bx + 13, by + 2, DspUi.TEXT);
        }
        // 0.1.9.0: frame core attachments
        int cy = coreToggleY();
        g.renderOutline(x0 + 6, cy, 9, 9, DspUi.FRAME);
        if (com.miophas.singularity_iteration.common.client.suit.FcsClientConfig.coreAttachments()) g.fill(x0 + 8, cy + 2, x0 + 13, cy + 7, DspUi.CYAN);
        DspUi.small(g, font, Component.translatable("gui.mio_icif.equipment_console.appearance.core_attachments").getString(), x0 + 19, cy + 2, DspUi.TEXT);
    }

    private int coreToggleY() { return handY() + 12; }

    private int handY() { return cardY() + CARD_H + 16; }

    private boolean appearanceClick(double mouseX, double mouseY) {
        for (int i = 0; i < 2; i++) {
            if (in(mouseX, mouseY, subX(i), subY(), SUB_W, SUB_H)) {
                if (appearancePage != i) { appearancePage = i; sound(false); }
                return true;
            }
        }
        if (appearancePage == 1) return paletteClick(mouseX, mouseY);
        for (int i = 0; i < 2; i++) {
            if (in(mouseX, mouseY, left + AX + 6 + i * 140, handY(), 130, 9)) {
                boolean first = com.miophas.singularity_iteration.common.client.suit.FcsClientConfig.handArmorFirstPerson();
                boolean third = com.miophas.singularity_iteration.common.client.suit.FcsClientConfig.handArmorThirdPerson();
                if (i == 0) first = !first; else third = !third;
                com.miophas.singularity_iteration.common.client.suit.FcsClientConfig.setHandArmor(first, third);
                sound(true);
                return true;
            }
        }
        if (in(mouseX, mouseY, left + AX + 6, coreToggleY(), 200, 9)) {
            com.miophas.singularity_iteration.common.client.suit.FcsClientConfig.setCoreAttachments(
                !com.miophas.singularity_iteration.common.client.suit.FcsClientConfig.coreAttachments());
            sound(true);
            return true;
        }
        for (int i = 0; i < 2; i++) {
            if (in(mouseX, mouseY, cardX(i), cardY(), CARD_W, CARD_H)) {
                boolean experimental = i == 1;
                if (experimental != com.miophas.singularity_iteration.common.client.AppearanceStyle.isExperimental()) {
                    sound(true);
                    com.miophas.singularity_iteration.common.client.AppearanceStyle.setExperimental(experimental);
                }
                return true;
            }
        }
        return false;
    }

    // ================================================================== APPEARANCE: suit palette (0.1.8.0)
    private static final String[] PALETTE_SLOTS = {"head", "chest", "legs", "feet"};
    private static final net.minecraft.world.entity.EquipmentSlot[] PALETTE_EQUIP = {
        net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.entity.EquipmentSlot.CHEST,
        net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET};
    private static final int PAL_ROW = 17, PAL_LIST_W = 106, PAL_RX = 116, PAL_TRACK = 120;
    private int palettePiece;
    /** 0 = plates (primary), 1 = light strips (accent). */
    private int paletteTarget;
    private boolean paletteAll;
    /** Working colours of the editor: primary, accent. */
    private final int[] paletteWork = {0x3A3F46, 0x37E35A};
    private int paletteLoadedFor = -1;
    private int paletteDrag = -1;

    private ItemStack paletteStack(int piece) {
        return minecraft == null || minecraft.player == null ? ItemStack.EMPTY : minecraft.player.getItemBySlot(PALETTE_EQUIP[piece]);
    }

    /** Loads the working colours from the selected piece when the selection changes. */
    private void paletteLoad() {
        if (paletteLoadedFor == palettePiece) return;
        paletteLoadedFor = palettePiece;
        var palette = com.miophas.singularity_iteration.common.suit.SuitPalette.of(paletteStack(palettePiece));
        var first = com.miophas.singularity_iteration.common.suit.SuitPalette.PRESETS.get(0);
        paletteWork[0] = palette == null ? first.primary() : palette.primary();
        paletteWork[1] = palette == null ? first.accent() : palette.accent();
    }

    private int sliderY(int channel) { return top + AY + 70 + channel * 15; }
    private int sliderX() { return left + AX + PAL_RX + 14; }

    private void renderPalette(GuiGraphics g, int mouseX, int mouseY) {
        int x0 = left + AX, y0 = top + AY;
        DspUi.screen(g, x0, y0, AW, AH);
        paletteLoad();
        DspUi.small(g, font, Component.translatable("gui.mio_icif.palette.title").getString(), x0 + 6, y0 + 6, DspUi.CYAN);
        DspUi.small(g, font, Component.translatable("gui.mio_icif.palette.hint").getString(), x0 + 6, y0 + 14, DspUi.TEXT_DIM);
        // ---- piece list
        for (int i = 0; i < 4; i++) {
            int x = x0 + 6, y = y0 + 24 + i * PAL_ROW;
            ItemStack stack = paletteStack(i);
            boolean ok = com.miophas.singularity_iteration.common.suit.SuitPalette.supports(stack);
            boolean on = palettePiece == i, hot = ok && in(mouseX, mouseY, x, y, PAL_LIST_W, PAL_ROW - 2);
            g.fill(x, y, x + PAL_LIST_W, y + PAL_ROW - 2, on ? 0xFF15293A : hot ? 0xFF112130 : DspUi.BG_HI);
            g.renderOutline(x, y, PAL_LIST_W, PAL_ROW - 2, on ? DspUi.CYAN : DspUi.FRAME);
            if (!stack.isEmpty()) {
                g.pose().pushPose();
                g.pose().translate(x + 2, y + 1, 0);
                g.pose().scale(0.8F, 0.8F, 1);
                g.renderItem(stack, 0, 0);
                g.pose().popPose();
            }
            String label = Component.translatable("gui.mio_icif.palette.piece." + PALETTE_SLOTS[i]).getString();
            DspUi.small(g, font, label, x + 17, y + 3, ok ? DspUi.TEXT : DspUi.TEXT_DIM);
            if (ok) {
                var palette = com.miophas.singularity_iteration.common.suit.SuitPalette.of(stack);
                int p = palette == null ? 0xFF555A60 : 0xFF000000 | palette.primary(), a = palette == null ? 0xFF555A60 : 0xFF000000 | palette.accent();
                g.fill(x + PAL_LIST_W - 22, y + 3, x + PAL_LIST_W - 13, y + 11, p);
                g.fill(x + PAL_LIST_W - 12, y + 3, x + PAL_LIST_W - 3, y + 11, a);
                if (palette == null) DspUi.small(g, font, Component.translatable("gui.mio_icif.palette.original").getString(), x + 17, y + 9, DspUi.TEXT_DIM);
            } else {
                DspUi.small(g, font, Component.translatable("gui.mio_icif.palette.unsupported").getString(), x + 17, y + 9, DspUi.TEXT_DIM);
            }
        }
        // apply-to-all switch
        int ay = y0 + 24 + 4 * PAL_ROW + 2;
        g.renderOutline(x0 + 6, ay, 9, 9, DspUi.FRAME);
        if (paletteAll) g.fill(x0 + 8, ay + 2, x0 + 13, ay + 7, DspUi.CYAN);
        DspUi.small(g, font, Component.translatable("gui.mio_icif.palette.all").getString(), x0 + 18, ay + 2, DspUi.TEXT);
        // ---- right column: target, presets, sliders
        int rx = x0 + PAL_RX;
        for (int t = 0; t < 2; t++) {
            int bx = rx + t * 82, by = y0 + 24;
            boolean on = paletteTarget == t;
            g.fill(bx, by, bx + 80, by + 12, on ? 0xFF15293A : DspUi.BG_HI);
            g.renderOutline(bx, by, 80, 12, on ? DspUi.CYAN : DspUi.FRAME);
            g.fill(bx + 3, by + 3, bx + 9, by + 9, 0xFF000000 | paletteWork[t]);
            DspUi.small(g, font, Component.translatable(t == 0 ? "gui.mio_icif.palette.primary" : "gui.mio_icif.palette.accent").getString(),
                bx + 12, by + 3, on ? DspUi.TEXT : DspUi.TEXT_DIM);
        }
        var presets = com.miophas.singularity_iteration.common.suit.SuitPalette.PRESETS;
        for (int i = 0; i < presets.size(); i++) {
            int px = rx + i * 20, py = y0 + 42;
            var preset = presets.get(i);
            g.fill(px, py, px + 18, py + 14, DspUi.FRAME);
            g.fill(px + 1, py + 1, px + 12, py + 13, 0xFF000000 | preset.primary());
            g.fill(px + 12, py + 1, px + 17, py + 13, 0xFF000000 | preset.accent());
        }
        DspUi.small(g, font, Component.translatable("gui.mio_icif.palette.presets").getString(), rx, y0 + 58, DspUi.TEXT_DIM);
        int color = paletteWork[paletteTarget];
        String[] names = {"R", "G", "B"};
        int[] channelColor = {0xFFE04A4A, 0xFF4AD06A, 0xFF4A8AF0};
        for (int c = 0; c < 3; c++) {
            int value = (color >> (16 - c * 8)) & 255, sy = sliderY(c), sx = sliderX();
            DspUi.small(g, font, names[c], rx, sy + 1, channelColor[c]);
            g.fill(sx, sy + 2, sx + PAL_TRACK, sy + 5, 0xFF0A141C);
            g.fill(sx, sy + 2, sx + value * PAL_TRACK / 255, sy + 5, channelColor[c]);
            int knob = sx + value * PAL_TRACK / 255;
            g.fill(knob - 1, sy, knob + 2, sy + 8, DspUi.TEXT);
            DspUi.small(g, font, String.valueOf(value), sx + PAL_TRACK + 5, sy + 1, DspUi.TEXT);
        }
        int hy = sliderY(3) + 2;
        g.fill(rx, hy, rx + 26, hy + 12, 0xFF000000 | paletteWork[0]);
        g.fill(rx + 26, hy, rx + 40, hy + 12, 0xFF000000 | paletteWork[1]);
        g.renderOutline(rx, hy, 40, 12, DspUi.FRAME);
        DspUi.small(g, font, String.format("#%06X / #%06X", paletteWork[0], paletteWork[1]), rx + 46, hy + 3, DspUi.TEXT);
        // buttons: apply, reset
        int by = hy + 18;
        String[] buttons = {"gui.mio_icif.palette.apply", "gui.mio_icif.palette.reset"};
        for (int b = 0; b < 2; b++) {
            int bx = rx + b * 82;
            boolean hot = in(mouseX, mouseY, bx, by, 80, 13);
            g.fill(bx, by, bx + 80, by + 13, hot ? 0xFF15293A : DspUi.BG_HI);
            g.renderOutline(bx, by, 80, 13, b == 0 ? DspUi.CYAN : DspUi.FRAME);
            DspUi.small(g, font, Component.translatable(buttons[b]).getString(), bx + 6, by + 4, DspUi.TEXT);
        }
        DspUi.small(g, font, Component.translatable("gui.mio_icif.palette.note").getString(), x0 + 6, y0 + AH - 10, DspUi.TEXT_DIM);
    }

    private boolean paletteClick(double mouseX, double mouseY) {
        int x0 = left + AX, y0 = top + AY, rx = x0 + PAL_RX;
        for (int i = 0; i < 4; i++) {
            if (in(mouseX, mouseY, x0 + 6, y0 + 24 + i * PAL_ROW, PAL_LIST_W, PAL_ROW - 2)
                    && com.miophas.singularity_iteration.common.suit.SuitPalette.supports(paletteStack(i))) {
                if (palettePiece != i) { palettePiece = i; sound(false); }
                return true;
            }
        }
        int ay = y0 + 24 + 4 * PAL_ROW + 2;
        if (in(mouseX, mouseY, x0 + 6, ay, 90, 9)) { paletteAll = !paletteAll; sound(true); return true; }
        for (int t = 0; t < 2; t++) {
            if (in(mouseX, mouseY, rx + t * 82, y0 + 24, 80, 12)) { paletteTarget = t; sound(false); return true; }
        }
        var presets = com.miophas.singularity_iteration.common.suit.SuitPalette.PRESETS;
        for (int i = 0; i < presets.size(); i++) {
            if (in(mouseX, mouseY, rx + i * 20, y0 + 42, 18, 14)) {
                paletteWork[0] = presets.get(i).primary();
                paletteWork[1] = presets.get(i).accent();
                paletteSend(false);
                sound(true);
                return true;
            }
        }
        for (int c = 0; c < 3; c++) {
            if (in(mouseX, mouseY, sliderX() - 3, sliderY(c), PAL_TRACK + 6, 9)) {
                paletteDrag = c;
                paletteSlider(mouseX);
                return true;
            }
        }
        int by = sliderY(3) + 20;
        if (in(mouseX, mouseY, rx, by, 80, 13)) { paletteSend(false); sound(true); return true; }
        if (in(mouseX, mouseY, rx + 82, by, 80, 13)) { paletteSend(true); sound(true); return true; }
        return false;
    }

    private void paletteSlider(double mouseX) {
        int c = paletteDrag;
        int value = Mth.clamp((int) Math.round((mouseX - sliderX()) * 255.0 / PAL_TRACK), 0, 255);
        int shift = 16 - c * 8;
        int color = paletteWork[paletteTarget];
        paletteWork[paletteTarget] = (color & ~(255 << shift)) | (value << shift);
        // Live preview on the client copy; the server copy follows when the drag ends.
        for (int i : paletteTargets()) {
            com.miophas.singularity_iteration.common.suit.SuitPalette.set(paletteStack(i),
                new com.miophas.singularity_iteration.common.suit.SuitPalette(paletteWork[0], paletteWork[1]));
        }
    }

    private int[] paletteTargets() {
        if (!paletteAll) return new int[]{palettePiece};
        return java.util.stream.IntStream.range(0, 4)
            .filter(i -> com.miophas.singularity_iteration.common.suit.SuitPalette.supports(paletteStack(i))).toArray();
    }

    /** Sends the working palette (or a reset) for the selected piece, or all nano pieces. */
    private void paletteSend(boolean clear) {
        for (int i : paletteTargets()) {
            ItemStack stack = paletteStack(i);
            if (!com.miophas.singularity_iteration.common.suit.SuitPalette.supports(stack)) continue;
            com.miophas.singularity_iteration.common.suit.SuitPalette.set(stack,
                clear ? null : new com.miophas.singularity_iteration.common.suit.SuitPalette(paletteWork[0], paletteWork[1]));
            PacketDistributor.sendToServer(new com.miophas.singularity_iteration.common.network.SuitPalettePacket(
                PALETTE_SLOTS[i], paletteWork[0], paletteWork[1], clear));
        }
        if (clear) paletteLoadedFor = -1;
    }
}
