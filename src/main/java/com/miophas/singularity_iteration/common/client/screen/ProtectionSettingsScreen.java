package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.client.screen.panel.PanelCharts;
import com.miophas.singularity_iteration.common.client.screen.widget.SiButton;
import com.miophas.singularity_iteration.common.network.GridPackets;
import com.miophas.singularity_iteration.core.platform.neoforge.energy.IndependentSpecialCableBlockEntity;
import com.miophas.singularity_iteration.core.runtime.energy.engine.ProtectionRelay;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * 0.1.10.22 (G5): settings of a breaker or fuse cable: state, last cause, rated flow (EU/t), rated voltage (off or a
 * tier), delay (ticks at twice the rated flow) and, for a breaker, automatic reclosing; Apply and Reset. The values
 * come from the block entity on the client (the update tag carries them); the server checks the packet again.
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class ProtectionSettingsScreen extends Screen {
    private static final String K = "gui.mio_icif.protection.";
    private static final long[] VOLTAGES = {0, 32, 128, 512, 2048, 8192, 32768};
    private static final int W = 240, H = 178;

    private final BlockPos pos;
    private EditBox flowBox, delayBox;
    private SiButton voltageButton, recloseButton, resetButton;
    private int voltageIndex;
    private boolean reclose;

    private ProtectionSettingsScreen(BlockPos pos) {
        super(Component.translatable(K + "title"));
        this.pos = pos.immutable();
    }

    /** Opens the screen for the protective cable at {@code pos} (client thread). */
    public static void open(BlockPos pos) {
        Minecraft.getInstance().setScreen(new ProtectionSettingsScreen(pos));
    }

    private ProtectionRelay relay() {
        var level = Minecraft.getInstance().level;
        return level != null && level.getBlockEntity(pos) instanceof IndependentSpecialCableBlockEntity cable && cable.protective() ? cable.relay() : null;
    }

    @Override
    protected void init() {
        var relay = relay();
        if (relay == null) { onClose(); return; }
        int x = (width - W) / 2, y = (height - H) / 2;
        flowBox = new EditBox(font, x + W - 96, y + 52, 80, 14, Component.empty());
        flowBox.setMaxLength(10);
        flowBox.setFilter(s -> s.chars().allMatch(Character::isDigit));
        flowBox.setValue(Long.toString(relay.ratedFlow()));
        addRenderableWidget(flowBox);
        voltageIndex = 0;
        for (int i = 0; i < VOLTAGES.length; i++) if (VOLTAGES[i] == relay.ratedVoltage()) voltageIndex = i;
        voltageButton = addRenderableWidget(new SiButton(x + W - 96, y + 70, 80, 14, voltageLabel(), b -> {
            voltageIndex = (voltageIndex + 1) % VOLTAGES.length;
            b.setMessage(voltageLabel());
        }));
        delayBox = new EditBox(font, x + W - 96, y + 88, 80, 14, Component.empty());
        delayBox.setMaxLength(4);
        delayBox.setFilter(s -> s.chars().allMatch(Character::isDigit));
        delayBox.setValue(Integer.toString(relay.delay()));
        addRenderableWidget(delayBox);
        reclose = relay.autoReclose();
        if (relay.kind() == ProtectionRelay.Kind.BREAKER) {
            recloseButton = addRenderableWidget(new SiButton(x + W - 96, y + 106, 80, 14, recloseLabel(), b -> {
                reclose = !reclose;
                b.setMessage(recloseLabel());
            }));
        }
        addRenderableWidget(new SiButton(x + 8, y + H - 22, 70, 14, Component.translatable(K + "apply"), b -> apply(false)));
        resetButton = addRenderableWidget(new SiButton(x + 85, y + H - 22, 70, 14, Component.translatable(K + "reset"), b -> apply(true)));
        addRenderableWidget(new SiButton(x + W - 78, y + H - 22, 70, 14, Component.translatable("gui.done"), b -> onClose()));
    }

    private Component voltageLabel() {
        long v = VOLTAGES[voltageIndex];
        return v == 0 ? Component.translatable(K + "voltage_off") : Component.literal(v + " EU");
    }

    private Component recloseLabel() {
        return Component.translatable(reclose ? K + "reclose_on" : K + "reclose_off");
    }

    private void apply(boolean reset) {
        long flow = parse(flowBox.getValue(), 512);
        int delay = (int) parse(delayBox.getValue(), 20);
        PacketDistributor.sendToServer(new GridPackets.ProtectionSettings(pos, Math.max(1, flow), VOLTAGES[voltageIndex],
            Math.max(1, Math.min(ProtectionRelay.MAX_DELAY, delay)), reclose, reset));
        if (!reset) onClose();
    }

    private static long parse(String text, long fallback) {
        try { return text.isEmpty() ? fallback : Long.parseLong(text); } catch (NumberFormatException e) { return fallback; }
    }

    @Override
    public void tick() {
        super.tick();
        var relay = relay();
        var player = Minecraft.getInstance().player;
        if (relay == null || player == null || player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0) { onClose(); return; }
        if (resetButton != null) resetButton.active = relay.state() != ProtectionRelay.State.CLOSED
            && relay.kind() == ProtectionRelay.Kind.BREAKER;
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (getFocused() instanceof EditBox box && key != GLFW.GLFW_KEY_ESCAPE) return box.keyPressed(key, scan, modifiers) || true;
        return super.keyPressed(key, scan, modifiers);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    /** The panel goes with the background (one blur per frame), the widgets on top. */
    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        super.renderBackground(g, mx, my, pt);
        PanelCharts.panel(g, (width - W) / 2, (height - H) / 2, W, H);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        var relay = relay();
        int x = (width - W) / 2, y = (height - H) / 2;
        if (relay == null) return;
        Component name = Component.translatable(relay.kind() == ProtectionRelay.Kind.FUSE
            ? "block.mio_icif.wiring.block_eu_fuse_cable" : "block.mio_icif.wiring.block_eu_breaker_cable");
        g.drawString(font, name, x + 8, y + 7, PanelCharts.ACCENT, false);
        int colour = switch (relay.state()) {
            case CLOSED -> PanelCharts.up();
            case TRIPPED -> PanelCharts.WARN;
            default -> PanelCharts.down();
        };
        String state = Component.translatable(K + "state." + relay.state().name().toLowerCase(Locale.ROOT)).getString();
        if (relay.state() == ProtectionRelay.State.TRIPPED && relay.autoReclose())
            state += " · " + Component.translatable(K + "recloses", String.format(Locale.ROOT, "%.1f", relay.recloseIn() / 20.0)).getString();
        g.drawString(font, state, x + 8, y + 20, colour, false);
        if (relay.lastCause() != ProtectionRelay.Cause.NONE) {
            String cause = Component.translatable(K + "cause." + relay.lastCause().name().toLowerCase(Locale.ROOT)).getString();
            if (relay.kind() == ProtectionRelay.Kind.BREAKER && relay.recentTrips() > 0)
                cause += " · " + Component.translatable(K + "trips", relay.recentTrips()).getString();
            g.drawString(font, font.plainSubstrByWidth(Component.translatable(K + "last", cause).getString(), W - 16), x + 8, y + 32, PanelCharts.DIM, false);
        }
        g.drawString(font, Component.translatable(K + "flow"), x + 8, y + 55, PanelCharts.TEXT, false);
        g.drawString(font, Component.translatable(K + "voltage"), x + 8, y + 73, PanelCharts.TEXT, false);
        g.drawString(font, Component.translatable(K + "delay", String.format(Locale.ROOT, "%.2f", parse(delayBox.getValue(), 20) / 20.0)),
            x + 8, y + 91, PanelCharts.TEXT, false);
        if (recloseButton != null) g.drawString(font, Component.translatable(K + "reclose"), x + 8, y + 109, PanelCharts.TEXT, false);
        int ty = y + 126;
        for (var line : font.split(Component.translatable(relay.kind() == ProtectionRelay.Kind.FUSE ? K + "help_fuse" : K + "help"), W - 16)) {
            if (ty > y + H - 30) break;
            g.drawString(font, line, x + 8, ty, PanelCharts.DIM, false);
            ty += 9;
        }
    }
}
