package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.menu.MergedSlots;

import com.miophas.singularity_iteration.common.menu.producer.MetalFormerMenu;
import com.miophas.singularity_iteration.common.processing.ProcessStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 金属成型机界面. 0.1.11.115 (processing overhaul M8): the instrument screen (docs/gui_processing_machines.md). The figure is
 * the tool of the current mode from the side ({@link FormingFigure}); a flat button cycles the mode (rolling, cutting,
 * extruding). The readouts give the input and the recipe's energy, power and time.
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_metal_former extends ProcessInstrumentScreen<MetalFormerMenu> {

    private float spin;

    public mio_icif_gui_metal_former(MetalFormerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    private boolean running() { return menu.getStatus() == ProcessStatus.RUNNING; }

    @Override
    protected void drawFigure(GuiGraphics g, float cx, float cy, float dt) {
        if (running()) spin = (spin + dt * 2.5F) % (float) (2 * Math.PI);
        int max = menu.getMaxProgress();
        FormingFigure.draw(g, cx, cy, menu.getMode().name(), max > 0 ? (float) menu.getProgress() / max : 0, spin, running());
    }

    // ---- 0.1.11.178: the merged drawing (panel coordinates; the slots are MergedSlots.Former's); 0.1.11.180: 176 wide ----
    @Override protected int arrowX() { return 120; }
    @Override protected int arrowY() { return MergedSlots.Former.OUT_Y + 1; }
    @Override protected int[] machineBody() { return FormingFigure.BENCH_TOOL.clone(); }

    @Override
    protected void drawMachine(GuiGraphics g, float dt) {
        if (running()) spin = (spin + dt * 2.5F) % (float) (2 * Math.PI);
        FormingFigure.bench(g, menu.getMode().name(), (float) progress(), spin, running());
    }

    @Override
    protected Component headline() {
        return Component.translatable(K + "mode." + menu.getMode().name().toLowerCase(Locale.ROOT));
    }

    @Override
    protected int headlineColour() { return running() ? INK : DIM; }

    @Override
    protected List<Component> readouts() {
        List<Component> lines = new ArrayList<>();
        ItemStack input = menu.getSlot(MetalFormerMenu.INPUT_SLOT).getItem();
        lines.add(input.isEmpty() ? Component.translatable(K + "no_material") : input.getHoverName());
        lines.add(Component.translatable(K + "energy_line", (long) menu.getPower() * menu.getMaxProgress(), menu.getPower(), menu.getMaxProgress()));
        return lines;
    }

    @Override
    protected int status() { return menu.getStatus(); }

    @Override
    protected void drawExtras(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        // 0.1.11.143: an icon in the figure window's corner
        iconButton(g, 0, ICON_CYCLE, false, 0, mouseX, mouseY,
            List.of(Component.translatable(K + "switch_mode"), Component.translatable(K + "tip.switch_mode").withStyle(net.minecraft.ChatFormatting.GRAY)));
    }

    @Override
    protected int buttonCount() { return 1; }

    @Override
    protected List<Component> figureTooltip() {
        return List.of(Component.translatable(K + "tip.former.1"), Component.translatable(K + "tip.former.2"));
    }
}
