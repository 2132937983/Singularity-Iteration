package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.blockentity.producer.tower.LaserTowerStats;
import com.miophas.singularity_iteration.common.blockentity.producer.tower.TargetFilter;
import com.miophas.singularity_iteration.common.client.screen.widget.PieChart;
import com.miophas.singularity_iteration.common.client.screen.widget.SiButton;
import com.miophas.singularity_iteration.common.menu.producer.LaserTowerMenu;
import com.miophas.singularity_iteration.common.network.LaserTowerConfigPacket;
import com.miophas.singularity_iteration.common.registry.mio_icif_sounds;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Laser tower console with three tabs:
 * <ul>
 *   <li><b>Control</b>: status, adjustable &plusmn;X/Z and &plusmn;Y scan box with a live radar view.</li>
 *   <li><b>Statistics</b>: EU spent, volleys, hits, kills, damage; pie chart of hits (or kills)
 *       per target type with legend and a recent-target log.</li>
 *   <li><b>Targets</b>: whitelist/blacklist toggle and the editable list of entity ids,
 *       entity tags and player names.</li>
 * </ul>
 * Battery slot, energy bar and player inventory are shared by all tabs.
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class LaserTowerScreen extends mio_icif_screen<LaserTowerMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.parse("mio_icif:textures/gui/gui_laser_tower.png");
    private static final int TAB_CONTROL = 0, TAB_STATS = 1, TAB_TARGETS = 2;
    private static int lastTab = TAB_CONTROL;

    private static final int BAR_X = 30, BAR_Y = 95, BAR_W = 138, BAR_H = 5;
    private static final int RADAR_X = 108, RADAR_Y = 19, RADAR_S = 60;
    private static final int PIE_CX = 38, PIE_CY = 50, PIE_R = 29, PIE_IN = 12;
    private static final int LIST_X = 30, LIST_Y = 49, LIST_W = 139, LIST_ROWS = 4, ROW_H = 10;

    private int tab = lastTab;
    private final List<AbstractWidget> controlWidgets = new ArrayList<>();
    private final List<AbstractWidget> statsWidgets = new ArrayList<>();
    private final List<AbstractWidget> targetWidgets = new ArrayList<>();
    private final SiButton[] tabButtons = new SiButton[3];
    private EditBox entryBox;
    private SiButton modeButton;
    private boolean pieShowsKills;
    private int listScroll;

    // pie data cache, rebuilt when a sync arrives
    private int cachedSync = -1;
    private boolean cachedKills;
    private final long[] pieValues = new long[PieChart.PALETTE.length];
    private final String[] pieKeys = new String[PieChart.PALETTE.length];
    private int pieCount;

    public LaserTowerScreen(LaserTowerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = LaserTowerMenu.GUI_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        addUpgradeInfoDockButton();
        this.titleLabelX = 8;
        this.titleLabelY = 5;
        this.inventoryLabelY = 107;
        controlWidgets.clear(); statsWidgets.clear(); targetWidgets.clear();

        String[] tabKeys = {"gui.mio_icif.laser_tower.tab.control", "gui.mio_icif.laser_tower.tab.stats", "gui.mio_icif.laser_tower.tab.targets"};
        for (int i = 0; i < 3; i++) {
            int id = i;
            tabButtons[i] = addRenderableWidget(new SiButton(leftPos + 95 + i * 25, topPos + 4, 25, 11,
                Component.translatable(tabKeys[i]), b -> selectTab(id)));
        }

        // Control: range buttons (-8 -1 +1 +8) for X/Z and Y
        addRangeRow(41, 0);
        addRangeRow(66, 2);

        // Statistics
        SiButton reset = new SiButton(leftPos + 140, topPos + 83, 29, 10, Component.translatable("gui.mio_icif.laser_tower.reset"), b -> {
            if (hasShiftDown()) send(LaserTowerConfigPacket.RESET_STATS, 0, "");
            click();
        });
        reset.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("gui.mio_icif.laser_tower.reset.tip")));
        statsWidgets.add(addRenderableWidget(reset));

        // Targets
        modeButton = addRenderableWidget(new SiButton(leftPos + 8, topPos + 19, 70, 12, Component.empty(), b -> {
            send(LaserTowerConfigPacket.TOGGLE_MODE, 0, ""); toggleSound(); }));
        targetWidgets.add(modeButton);
        entryBox = new EditBox(font, leftPos + 32, topPos + 37, 115, 9, Component.translatable("gui.mio_icif.laser_tower.entry"));
        entryBox.setBordered(false);
        entryBox.setTextColor(0x2A2E33);
        entryBox.setMaxLength(TargetFilter.MAX_LENGTH);
        entryBox.setHint(Component.translatable("gui.mio_icif.laser_tower.entry.hint").withStyle(ChatFormatting.DARK_GRAY));
        addRenderableWidget(entryBox);
        targetWidgets.add(entryBox);
        targetWidgets.add(addRenderableWidget(new SiButton(leftPos + 151, topPos + 35, 18, 11, Component.literal("+"), b -> submitEntry())));

        selectTab(tab);
    }

    /** Buttons in display order -8, -1, +1, +8 mapped onto menu button ids. */
    private void addRangeRow(int y, int base) {
        int[] ids = {base + 4, base, base + 1, base + 5};
        String[] labels = {"-8", "-1", "+1", "+8"};
        for (int i = 0; i < 4; i++) {
            int id = ids[i];
            controlWidgets.add(addRenderableWidget(new SiButton(leftPos + 8 + i * 23, topPos + y, 22, 11, Component.literal(labels[i]), b -> pressRange(id))));
        }
    }

    private void selectTab(int newTab) {
        if (tab != newTab) click();
        tab = lastTab = newTab;
        controlWidgets.forEach(w -> w.visible = tab == TAB_CONTROL);
        statsWidgets.forEach(w -> w.visible = tab == TAB_STATS);
        targetWidgets.forEach(w -> w.visible = tab == TAB_TARGETS);
        for (int i = 0; i < 3; i++) tabButtons[i].selected(i == tab);
        if (tab != TAB_TARGETS) entryBox.setFocused(false);
    }

    private void pressRange(int id) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        click();
    }

    private void submitEntry() {
        String text = entryBox.getValue().trim();
        if (text.isEmpty()) return;
        if (TargetFilter.normalise(text) == null) {
            entryBox.setTextColor(0xC8423C);
            return;
        }
        send(LaserTowerConfigPacket.ADD, 0, text);
        entryBox.setValue("");
        entryBox.setTextColor(0x2A2E33);
        toggleSound();
    }

    private void send(int action, int index, String text) {
        PacketDistributor.sendToServer(new LaserTowerConfigPacket(menu.containerId, action, index, text));
    }

    private void click() {
        if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(mio_icif_sounds.UI_CLICK.get(), 1.0F, 0.6F));
    }

    private void toggleSound() {
        if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(mio_icif_sounds.UI_TOGGLE.get(), 1.0F, 0.6F));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (tab == TAB_TARGETS && entryBox.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) { submitEntry(); return true; }
            if (keyCode != GLFW.GLFW_KEY_ESCAPE) { entryBox.setTextColor(0x2A2E33); return entryBox.keyPressed(keyCode, scanCode, modifiers) || true; }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        TargetFilter filter = menu.filter();
        modeButton.setMessage(Component.translatable(filter.mode() == TargetFilter.Mode.WHITELIST
            ? "gui.mio_icif.laser_tower.mode.whitelist" : "gui.mio_icif.laser_tower.mode.blacklist"));
    }

    // ------------------------------------------------------------------ background
    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        long energy = menu.energy(), capacity = Math.max(1, menu.capacity());
        SiGuiTheme.bar(g, x + BAR_X, y + BAR_Y, BAR_W, BAR_H, (double) energy / capacity, SiGuiTheme.BAR_FROM, SiGuiTheme.BAR_TO);

        switch (tab) {
            case TAB_CONTROL -> renderControlBg(g, x, y, partialTick);
            case TAB_STATS -> renderStatsBg(g, x, y, mouseX, mouseY);
            default -> {
                SiGuiTheme.well(g, x + 30, y + 35, 119, 11);   // borderless entry box sits in a light well
                renderTargetsBg(g, x, y, mouseX, mouseY);
            }
        }
    }

    // ------------------------------------------------------------------ upgrade bay
    // The four upgrade slots live in the shared left utility dock; an info button under them
    // summarises what the installed upgrades currently do.
    private void addUpgradeInfoDockButton() {
        addDockButton(new DockButton((g, x, y, hov) -> {
            g.fill(x + 7, y + 3, x + 9, y + 5, hov ? SiGuiTheme.ACCENT : 0xFF2A2E33);
            g.fill(x + 7, y + 6, x + 9, y + 13, hov ? SiGuiTheme.ACCENT : 0xFF2A2E33);
        }, () -> List.of(
            Component.translatable("gui.mio_icif.laser_tower.upgrades").withStyle(ChatFormatting.WHITE),
            Component.translatable("gui.mio_icif.laser_tower.upgrades.interval", menu.volleyInterval()).withStyle(ChatFormatting.GRAY),
            Component.translatable("gui.mio_icif.laser_tower.upgrades.cost", menu.costPerTarget()).withStyle(ChatFormatting.GRAY),
            Component.translatable("gui.mio_icif.laser_tower.upgrades.tier", menu.effectiveTier()).withStyle(ChatFormatting.GRAY)),
            () -> { }));
    }

    private boolean renderBayTooltip(GuiGraphics g, int mouseX, int mouseY) {
        return false;
    }

    private void renderControlBg(GuiGraphics g, int x, int y, float partialTick) {
        // radar: scan box (inner square) relative to the maximum range, rotating sweep
        SiGuiTheme.well(g, x + RADAR_X, y + RADAR_Y, RADAR_S, RADAR_S);
        int cx = x + RADAR_X + RADAR_S / 2, cy = y + RADAR_Y + RADAR_S / 2;
        int half = Math.max(2, (RADAR_S / 2 - 3) * menu.horizontalRange() / Math.max(1, menu.maxHorizontal()));
        g.fill(cx - half, cy - half, cx + half, cy + half, 0x2A3A6EA5);
        g.renderOutline(cx - half, cy - half, half * 2, half * 2, SiGuiTheme.ACCENT);
        boolean live = menu.state() <= 1;
        if (live && minecraft != null && minecraft.level != null) {
            float t = (minecraft.level.getGameTime() + partialTick) * (menu.state() == 1 ? 0.35F : 0.12F);
            int r = RADAR_S / 2 - 2;
            for (int i = 0; i < 12; i++) {   // fading sweep trail
                float a = t - i * 0.05F;
                int ex = cx + (int) (Mth.cos(a) * r), ey = cy + (int) (Mth.sin(a) * r);
                int alpha = 200 - i * 16;
                line(g, cx, cy, ex, ey, (alpha << 24) | 0x3A6EA5);
            }
        }
        g.fill(cx - 1, cy - 1, cx + 2, cy + 2, SiGuiTheme.TEXT_SCREEN);
        // vertical range gauge on the radar's right edge
        int vh = (RADAR_S - 6) * menu.verticalRange() / Math.max(1, menu.maxVertical());
        g.fill(x + RADAR_X + RADAR_S - 4, y + RADAR_Y + RADAR_S - 3 - vh, x + RADAR_X + RADAR_S - 2, y + RADAR_Y + RADAR_S - 3, SiGuiTheme.WARN);

        // status LED
        int led = switch (menu.state()) { case 0 -> SiGuiTheme.GOOD; case 1 -> SiGuiTheme.BAD; case 2 -> SiGuiTheme.WARN; default -> SiGuiTheme.LED_OFF; };
        g.fill(x + 8, y + 19, x + 14, y + 25, SiGuiTheme.OUTLINE);
        g.fill(x + 9, y + 20, x + 13, y + 24, led);
    }

    /** Bresenham-free thin line made of 1px fills (short lines only). */
    private static void line(GuiGraphics g, int x0, int y0, int x1, int y1, int color) {
        int steps = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
        for (int i = 0; i <= steps; i++) {
            int px = x0 + (x1 - x0) * i / Math.max(1, steps), py = y0 + (y1 - y0) * i / Math.max(1, steps);
            g.fill(px, py, px + 1, py + 1, color);
        }
    }

    private void rebuildPie() {
        LaserTowerStats stats = menu.stats();
        if (cachedSync == menu.syncCount() && cachedKills == pieShowsKills) return;
        cachedSync = menu.syncCount();
        cachedKills = pieShowsKills;
        Object2LongMap<String> map = pieShowsKills ? stats.killsByType() : stats.hitsByType();
        List<Object2LongMap.Entry<String>> entries = new ArrayList<>(map.object2LongEntrySet());
        entries.sort((a, b) -> Long.compare(b.getLongValue(), a.getLongValue()));
        int max = PieChart.PALETTE.length;
        pieCount = 0;
        long rest = 0;
        for (int i = 0; i < entries.size(); i++) {
            if (pieCount < max - 1 || entries.size() == max) {
                pieKeys[pieCount] = entries.get(i).getKey();
                pieValues[pieCount++] = entries.get(i).getLongValue();
            } else {
                rest += entries.get(i).getLongValue();
            }
        }
        if (rest > 0) { pieKeys[pieCount] = LaserTowerStats.OTHER; pieValues[pieCount++] = rest; }
    }

    private void renderStatsBg(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        rebuildPie();
        SiGuiTheme.well(g, x + 7, y + 18, 63, 63);
        int hover = PieChart.hit(x + PIE_CX, y + PIE_CY, PIE_R, PIE_IN, pieValues, pieCount, mouseX, mouseY);
        PieChart.draw(g, x + PIE_CX, y + PIE_CY, PIE_R, PIE_IN, pieValues, pieCount, hover);
        SiGuiTheme.well(g, x + 73, y + 18, 96, 63);
    }

    private void renderTargetsBg(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        SiGuiTheme.well(g, x + LIST_X - 1, y + LIST_Y - 1, LIST_W + 2, LIST_ROWS * ROW_H + 2);
        List<String> entries = menu.filter().entries();
        for (int row = 0; row < LIST_ROWS; row++) {
            int i = row + listScroll;
            if (i >= entries.size()) break;
            int ry = y + LIST_Y + row * ROW_H;
            boolean hot = mouseX >= x + LIST_X && mouseX < x + LIST_X + LIST_W && mouseY >= ry && mouseY < ry + ROW_H;
            if (hot) g.fill(x + LIST_X, ry, x + LIST_X + LIST_W, ry + ROW_H, 0x403A6EA5);
        }
    }

    // ------------------------------------------------------------------ foreground
    @Override
    protected boolean showUpgradeHint() { return false; }

    /** The tower draws its own state lamp next to the status line. */
    @Override
    protected int statusLampColor() { return 0; }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        // the tabs start at x 92: long names shrink and end with an ellipsis instead of being cut
        drawFitted(g, font, title, titleLabelX, titleLabelY, 84, SiGuiTheme.TEXT, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, SiGuiTheme.TEXT, false);
        switch (tab) {
            case TAB_CONTROL -> renderControlLabels(g);
            case TAB_STATS -> renderStatsLabels(g);
            default -> renderTargetLabels(g);
        }
        g.drawString(font, Component.translatable("gui.mio_icif.laser_tower.cost_short", menu.costPerTarget()),
            BAR_X, 86, SiGuiTheme.TEXT, false);
    }

    private void renderControlLabels(GuiGraphics g) {
        String state = switch (menu.state()) {
            case 0 -> "standby"; case 1 -> "engaged"; case 2 -> "no_power"; default -> "disabled";
        };
        Component stateText = Component.translatable("gui.mio_icif.laser_tower.state." + state);
        int room = RADAR_X - 19;
        drawFitted(g, font, stateText, 17, 19, room, SiGuiTheme.TEXT, false);
        g.drawString(font, Component.translatable("gui.mio_icif.laser_tower.horizontal", menu.horizontalRange()), 8, 31, SiGuiTheme.TEXT, false);
        g.drawString(font, Component.translatable("gui.mio_icif.laser_tower.vertical", menu.verticalRange()), 8, 56, SiGuiTheme.TEXT, false);
        small(g, Component.translatable("gui.mio_icif.laser_tower.volley", menu.lastVolley(), menu.maxTargets()).getString(),
            RADAR_X + 3, RADAR_Y + RADAR_S - 8, SiGuiTheme.TEXT_SCREEN);
    }

    private void renderStatsLabels(GuiGraphics g) {
        LaserTowerStats st = menu.stats();
        int lx = 77;
        g.drawString(font, Component.translatable(pieShowsKills ? "gui.mio_icif.laser_tower.pie.kills" : "gui.mio_icif.laser_tower.pie.hits"),
            lx, 21, SiGuiTheme.ACCENT, false);
        for (int i = 0; i < Math.min(pieCount, 3); i++) {
            int ly = 31 + i * 8;
            g.fill(lx, ly + 1, lx + 5, ly + 6, PieChart.PALETTE[i]);
            small(g, font.plainSubstrByWidth(typeName(pieKeys[i]), 80), lx + 7, ly + 1, SiGuiTheme.TEXT_SCREEN);
            String n = compact(pieValues[i]);
            small(g, n, 167 - (int) (font.width(n) * 0.75F), ly + 1, SiGuiTheme.TEXT_SCREEN_DIM);
        }
        if (pieCount == 0) small(g, Component.translatable("gui.mio_icif.laser_tower.no_data").getString(), lx + 1, 32, SiGuiTheme.TEXT_SCREEN_DIM);
        int sy = 57;
        statPair(g, lx, sy, "gui.mio_icif.laser_tower.stat.eu", compact(st.energyUsed()));
        statPair(g, lx + 47, sy, "gui.mio_icif.laser_tower.stat.volleys_short", compact(st.volleys()));
        statPair(g, lx, sy + 7, "gui.mio_icif.laser_tower.stat.hits", compact(st.hits()));
        statPair(g, lx + 47, sy + 7, "gui.mio_icif.laser_tower.stat.kills_short", compact(st.kills()));
        statPair(g, lx, sy + 14, "gui.mio_icif.laser_tower.stat.damage", compact((long) st.damage()));
    }

    private void statPair(GuiGraphics g, int x, int y, String key, String value) {
        String label = Component.translatable(key).getString();
        small(g, label, x, y, SiGuiTheme.TEXT_SCREEN_DIM);
        small(g, value, x + (int) (font.width(label) * 0.75F) + 3, y, SiGuiTheme.TEXT_SCREEN);
    }

    /** Text at 3/4 scale for dense read-outs. */
    private void small(GuiGraphics g, String text, int x, int y, int color) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(0.75F, 0.75F, 1);
        g.drawString(font, text, 0, 0, color, false);
        g.pose().popPose();
    }

    private void renderTargetLabels(GuiGraphics g) {
        TargetFilter f = menu.filter();
        String desc = Component.translatable(f.mode() == TargetFilter.Mode.WHITELIST
            ? "gui.mio_icif.laser_tower.mode.whitelist.desc" : "gui.mio_icif.laser_tower.mode.blacklist.desc").getString();
        var lines = font.split(Component.literal(desc), 116);
        for (int i = 0; i < Math.min(2, lines.size()); i++) {
            g.pose().pushPose();
            g.pose().translate(82, 19 + i * 7, 0);
            g.pose().scale(0.75F, 0.75F, 1);
            g.drawString(font, lines.get(i), 0, 0, SiGuiTheme.TEXT, false);
            g.pose().popPose();
        }
        List<String> entries = f.entries();
        listScroll = Mth.clamp(listScroll, 0, Math.max(0, entries.size() - LIST_ROWS));
        for (int row = 0; row < LIST_ROWS; row++) {
            int i = row + listScroll;
            if (i >= entries.size()) break;
            int ry = LIST_Y + row * ROW_H + 1;
            String e = entries.get(i);
            int color = e.startsWith("#") ? SiGuiTheme.WARN : e.indexOf(':') >= 0 ? SiGuiTheme.TEXT_SCREEN : SiGuiTheme.GOOD;
            g.drawString(font, font.plainSubstrByWidth(entryLabel(e), LIST_W - 14), LIST_X + 2, ry, color, false);
            g.drawString(font, "\u00d7", LIST_X + LIST_W - 8, ry, SiGuiTheme.BAD, false);
        }
        if (entries.isEmpty()) {
            g.drawString(font, Component.translatable("gui.mio_icif.laser_tower.list_empty"), LIST_X + 2, LIST_Y + 2, SiGuiTheme.TEXT_SCREEN_DIM, false);
        }
        String count = entries.size() + "/" + TargetFilter.MAX_ENTRIES;
        g.drawString(font, count, 8, 37, SiGuiTheme.TEXT, false);
    }

    private String entryLabel(String e) {
        if (e.startsWith("#") || e.indexOf(':') < 0) return e;
        return typeName(e) + " (" + e + ")";
    }

    private static String typeName(String key) {
        if (LaserTowerStats.OTHER.equals(key)) return Component.translatable("gui.mio_icif.laser_tower.other").getString();
        if (LaserTowerStats.PLAYER.equals(key)) return Component.translatable("gui.mio_icif.laser_tower.player").getString();
        ResourceLocation id = ResourceLocation.tryParse(key);
        if (id == null) return key;
        return BuiltInRegistries.ENTITY_TYPE.getOptional(id).map(t -> t.getDescription().getString()).orElse(key);
    }

    private static String compact(long v) {
        if (v >= 1_000_000_000L) return String.format("%.1fG", v / 1e9);
        if (v >= 1_000_000L) return String.format("%.1fM", v / 1e6);
        if (v >= 10_000L) return String.format("%.1fk", v / 1e3);
        return Long.toString(v);
    }

    // ------------------------------------------------------------------ input
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (tab == TAB_TARGETS && button == 0) {
            int lx = leftPos + LIST_X, ly = topPos + LIST_Y;
            if (mouseX >= lx && mouseX < lx + LIST_W && mouseY >= ly && mouseY < ly + LIST_ROWS * ROW_H) {
                int i = (int) ((mouseY - ly) / ROW_H) + listScroll;
                if (mouseX >= lx + LIST_W - 11 && i < menu.filter().entries().size()) {
                    send(LaserTowerConfigPacket.REMOVE, i, "");
                    toggleSound();
                    return true;
                }
            }
        }
        if (tab == TAB_STATS && button == 0
                && PieChart.hit(leftPos + PIE_CX, topPos + PIE_CY, PIE_R, PIE_IN, pieValues, pieCount, mouseX, mouseY) >= 0) {
            pieShowsKills = !pieShowsKills;
            click();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (tab == TAB_TARGETS) {
            listScroll = Mth.clamp(listScroll - (int) Math.signum(scrollY), 0, Math.max(0, menu.filter().entries().size() - LIST_ROWS));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    protected void renderCustomTooltips(GuiGraphics g, int mouseX, int mouseY) {
        if (renderBayTooltip(g, mouseX, mouseY)) return;
        int x = leftPos, y = topPos;
        if (isHovering(mouseX, mouseY, x + BAR_X, y + BAR_Y - 1, BAR_W, BAR_H + 2)) {
            g.renderTooltip(font, Component.translatable("gui.mio_icif.laser_tower.energy",
                String.format("%,d", menu.energy()), String.format("%,d", menu.capacity())), mouseX, mouseY);
            return;
        }
        if (tab == TAB_CONTROL) {
            if (isHovering(mouseX, mouseY, x + 8, y + 31, 80, 9)) {
                g.renderTooltip(font, Component.translatable("gui.mio_icif.laser_tower.range_limit", menu.maxHorizontal()), mouseX, mouseY);
            } else if (isHovering(mouseX, mouseY, x + 8, y + 56, 80, 9)) {
                g.renderTooltip(font, Component.translatable("gui.mio_icif.laser_tower.range_limit", menu.maxVertical()), mouseX, mouseY);
            } else if (isHovering(mouseX, mouseY, x + RADAR_X, y + RADAR_Y, RADAR_S, RADAR_S)) {
                g.renderTooltip(font, Component.translatable("gui.mio_icif.laser_tower.radar"), mouseX, mouseY);
            }
        } else if (tab == TAB_STATS) {
            int i = PieChart.hit(x + PIE_CX, y + PIE_CY, PIE_R, PIE_IN, pieValues, pieCount, mouseX, mouseY);
            if (i >= 0) {
                long total = 0;
                for (int k = 0; k < pieCount; k++) total += pieValues[k];
                g.renderTooltip(font, Component.literal(typeName(pieKeys[i]) + ": " + pieValues[i]
                    + String.format(" (%.1f%%)", 100.0 * pieValues[i] / Math.max(1, total))), mouseX, mouseY);
            } else if (isHovering(mouseX, mouseY, x + 73, y + 18, 96, 36)) {
                List<Component> lines = new ArrayList<>();
                lines.add(Component.translatable("gui.mio_icif.laser_tower.recent").withStyle(ChatFormatting.AQUA));
                List<LaserTowerStats.Recent> recent = menu.stats().recent();
                for (int k = recent.size() - 1; k >= 0; k--) {
                    LaserTowerStats.Recent r = recent.get(k);
                    lines.add(Component.literal(String.format("%s  -%.1f❤", r.name(), r.damage()))
                        .withStyle(r.killed() ? ChatFormatting.RED : ChatFormatting.GRAY));
                }
                if (recent.isEmpty()) lines.add(Component.translatable("gui.mio_icif.laser_tower.no_data").withStyle(ChatFormatting.GRAY));
                g.renderComponentTooltip(font, lines, mouseX, mouseY);
            }
        } else if (isHovering(mouseX, mouseY, x + 8, y + 19, 70, 12)) {
            g.renderComponentTooltip(font, List.of(
                Component.translatable("gui.mio_icif.laser_tower.mode.help1"),
                Component.translatable("gui.mio_icif.laser_tower.mode.help2").withStyle(ChatFormatting.GRAY),
                Component.translatable("gui.mio_icif.laser_tower.mode.help3").withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
        }
    }
}
