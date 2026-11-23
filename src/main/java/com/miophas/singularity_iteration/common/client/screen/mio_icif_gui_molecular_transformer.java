package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.menu.producer.MolecularTransformerMenu;
import com.miophas.singularity_iteration.common.recipe.molecular_transformer.MolecularProcess;
import com.miophas.singularity_iteration.common.recipe.molecular_transformer.mio_icif_MolecularTransformerRecipe;
import com.miophas.singularity_iteration.common.recipe.molecular_transformer.mio_icif_MolecularTransformerRecipeInput;
import com.miophas.singularity_iteration.common.recipe.molecular_transformer.mio_icif_MolecularTransformerRecipes;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Molecular Transformer screen. 0.1.11.6: 248 x 204 with an observation window ({@link MolecularObservationWindow})
 * in place of the progress arrow; input and output in a column on the left with the energy gauge between them.
 *
 * <p>0.1.11.181: the vanilla 176 wide, merged (docs/gui_processing_machines.md, "合并布局"): the transformer's chamber drawn
 * on the vanilla panel with the observation window in its front (154 wide, was 207); the input port on its top at the left
 * and the output port at the right, the energy gauge along the top between them.
 */
@OnlyIn(Dist.CLIENT)
public class mio_icif_gui_molecular_transformer extends mio_icif_screen<MolecularTransformerMenu> {

    private static final int WINDOW_X = 11, WINDOW_Y = 37, WINDOW_W = 154, WINDOW_H = 74;
    private static final int ENERGY_X = 36, ENERGY_Y = 20, ENERGY_W = 104, ENERGY_H = 9;
    private static final int CHAMBER_X = 8, CHAMBER_Y = 34, CHAMBER_W = 160, CHAMBER_H = 80;

    private final MolecularObservationWindow window = new MolecularObservationWindow();
    private ItemStack lastInput = ItemStack.EMPTY;
    private mio_icif_MolecularTransformerRecipe lastRecipe;
    /** The machine drops its working flag on any tick its buffer is empty; the window holds the run for a second. */
    private long workingAt;
    private long lastDraw;

    public mio_icif_gui_molecular_transformer(MolecularTransformerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = MolecularTransformerMenu.GUI_WIDTH;
        this.imageHeight = MolecularTransformerMenu.GUI_HEIGHT;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos, y = this.topPos;
        // 0.1.11.181: the vanilla panel; the chamber with the window in its front, the two ports on its top, the gauge between
        VanillaFrame.panel(g, x, y, this.imageWidth, this.imageHeight);
        MachineArt.iron(g, x + CHAMBER_X, y + CHAMBER_Y, CHAMBER_W, CHAMBER_H);
        for (int port : new int[]{MolecularTransformerMenu.INPUT_X, MolecularTransformerMenu.OUTPUT_X})
            MachineArt.steel(g, x + port - 3, y + MolecularTransformerMenu.INPUT_Y - 3, 22, CHAMBER_Y - MolecularTransformerMenu.INPUT_Y + 4);
        for (var slot : menu.slots) {
            if (slot.x < 0 || slot.x > imageWidth) continue;
            VanillaFrame.slot(g, x + slot.x - 1, y + slot.y - 1);
        }
        int ex = x + ENERGY_X, ey = y + ENERGY_Y;
        MachineArt.sunk(g, ex, ey, ENERGY_W, ENERGY_H, MachineArt.GLASS);
        long energy = Integer.toUnsignedLong(menu.getEnergy()), max = Math.max(1, Integer.toUnsignedLong(menu.getMaxEnergy()));
        int filled = (int) Math.round((ENERGY_W - 2) * Math.min(1.0, energy / (double) max));
        if (filled > 0) g.fill(ex + 1, ey + 1, ex + 1 + filled, ey + ENERGY_H - 1, MachineArt.EU);
        MachineArt.sunk(g, x + WINDOW_X - 1, y + WINDOW_Y - 1, WINDOW_W + 2, WINDOW_H + 2, MachineArt.WINDOW);

        window.render(g, x + WINDOW_X, y + WINDOW_Y, WINDOW_W, WINDOW_H, snapshot());
    }

    private MolecularObservationWindow.Snapshot snapshot() {
        ItemStack input = menu.getSlot(MolecularTransformerMenu.INPUT_SLOT).getItem();
        mio_icif_MolecularTransformerRecipe recipe = recipeFor(input);
        long now = System.currentTimeMillis();
        if (menu.isWorking()) workingAt = now;
        if (menu.getEUPerTick() > 0) lastDraw = Integer.toUnsignedLong(menu.getEUPerTick());
        boolean working = recipe != null && now - workingAt < 1000;
        long recipeEU = recipe != null ? recipe.getEuCost() : Integer.toUnsignedLong(menu.getRecipeEU());
        Component inName = input.isEmpty() ? Component.literal("—") : input.getHoverName();
        Component outName = recipe != null ? recipe.getResult().getHoverName() : Component.literal("—");
        int inHash = input.isEmpty() ? 0 : input.getItem().hashCode();
        int outHash = recipe != null ? recipe.getResult().getItem().hashCode() : 0;
        return new MolecularObservationWindow.Snapshot(recipe != null ? recipe.getProcess() : MolecularProcess.UNKNOWN, recipe != null, working,
            menu.getProgress() / 100F, recipeEU, working ? lastDraw : 0, inName, outName, inHash, outHash);
    }

    /** The recipe for the input stack (the client has the recipes), looked up again only when the stack changes. */
    private mio_icif_MolecularTransformerRecipe recipeFor(ItemStack input) {
        if (input.isEmpty()) { lastInput = ItemStack.EMPTY; lastRecipe = null; return null; }
        if (ItemStack.isSameItemSameComponents(input, lastInput)) return lastRecipe;
        lastInput = input.copy();
        var level = Minecraft.getInstance().level;
        lastRecipe = level == null ? null : level.getRecipeManager()
            .getRecipeFor(mio_icif_MolecularTransformerRecipes.MOLECULAR_TRANSFORMER_TYPE.get(), new mio_icif_MolecularTransformerRecipeInput(input), level)
            .map(h -> h.value()).orElse(null);
        return lastRecipe;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int ex = leftPos + ENERGY_X, ey = topPos + ENERGY_Y;
        if (hoveredSlot == null && mouseX >= ex && mouseX < ex + ENERGY_W && mouseY >= ey && mouseY < ey + ENERGY_H) {
            g.renderComponentTooltip(this.font, List.of(
                Component.translatable("gui.mio_icif.mt.tip.energy", Integer.toUnsignedLong(menu.getEnergy()), Integer.toUnsignedLong(menu.getMaxEnergy())),
                Component.translatable("gui.mio_icif.mt.tip.draw", Integer.toUnsignedLong(menu.getEUPerTick()))), mouseX, mouseY);
        }
    }
}
