// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.integration.jei.category;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.client.multiblock.StructureView;
import com.miophas.singularity_iteration.common.registry.mio_icif_blocks;
import com.miophas.singularity_iteration.core.api.multiblock.StructureBlueprint;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.inputs.IJeiGuiEventListener;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.gui.widgets.IRecipeWidget;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.10.19: "Multiblock Structures" (like the structure viewer of Modern Industrialization): one page per
 * registered {@link StructureBlueprint} ({@code core.api.multiblock.StructureBlueprintRegistry}, so addon
 * structures appear too). The 3D view turns with a mouse drag and zooms with the wheel; a click on the layer
 * label shows one layer (left: up, right: down). The materials are input slots with the count of each
 * block, so looking up the uses of a block finds the structures that contain it.
 * <pre>
 *            Title
 *   +---------------------+
 *   |   3D view  [layer]  |
 *   +---------------------+
 *   [mat][mat][mat]...
 *   notes (scroll box)
 * </pre>
 */
@SuppressWarnings("null")
public class mio_icif_StructureCategory implements IRecipeCategory<StructureBlueprint> {
    public static final RecipeType<StructureBlueprint> TYPE =
        new RecipeType<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "structure"), StructureBlueprint.class);

    private static final int WIDTH = 168, HEIGHT = 190, VIEW_Y = 11, VIEW_H = 112, MATERIALS_Y = 125, PER_ROW = 9;
    private static final int TITLE_COLOR = 0xFF404040;

    private final IDrawable icon;

    public mio_icif_StructureCategory(IGuiHelper helper) {
        this.icon = helper.createDrawableItemLike(mio_icif_blocks.LARGE_FABRICATOR_CORE.get());
    }

    @Override public RecipeType<StructureBlueprint> getRecipeType() { return TYPE; }
    @Override public Component getTitle() { return Component.translatable("jei.mio_icif.category.structures"); }
    @Override public @Nullable IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return WIDTH; }
    @Override public int getHeight() { return HEIGHT; }
    @Override public ResourceLocation getRegistryName(StructureBlueprint blueprint) { return blueprint.id(); }

    /** The items of the permitted blocks of each key, with the count of the key. */
    private static List<List<ItemStack>> materials(StructureBlueprint blueprint) {
        List<List<ItemStack>> out = new ArrayList<>();
        for (Map.Entry<StructureBlueprint.Key, Integer> entry : blueprint.counts().entrySet()) {
            List<ItemStack> stacks = new ArrayList<>();
            for (StructureBlueprint.BlockRef ref : entry.getKey().blocks()) {
                BlockState state = ref.resolve();
                if (state == null) continue;
                ItemStack stack = new ItemStack(state.getBlock().asItem(), entry.getValue());
                if (!stack.isEmpty() && stacks.stream().noneMatch(s -> s.is(stack.getItem()))) stacks.add(stack);
            }
            out.add(stacks);
        }
        return out;
    }

    private static int rows(int materials) {
        return Math.max(1, (materials + PER_ROW - 1) / PER_ROW);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, StructureBlueprint blueprint, IFocusGroup focuses) {
        List<List<ItemStack>> materials = materials(blueprint);
        List<StructureBlueprint.Key> keys = new ArrayList<>(blueprint.counts().keySet());
        for (int i = 0; i < materials.size(); i++) {
            if (materials.get(i).isEmpty()) continue;
            int inRow = Math.min(PER_ROW, materials.size() - i / PER_ROW * PER_ROW);
            int x = (WIDTH - inRow * 18) / 2 + (i % PER_ROW) * 18 + 1, y = MATERIALS_Y + (i / PER_ROW) * 18 + 1;
            String note = keys.get(i).noteKey();
            var slot = SiJei.input(builder, x, y).addItemStacks(materials.get(i));
            if (note != null) slot.addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable(note).withStyle(ChatFormatting.GRAY)));
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, StructureBlueprint blueprint, IFocusGroup focuses) {
        builder.addText(Component.translatable(blueprint.titleKey()), 0, 0, WIDTH, 10)
            .alignHorizontalCenter().setColor(TITLE_COLOR).setShadow(false);
        Preview preview = new Preview(new StructureView(blueprint));
        builder.addWidget(preview);
        builder.addGuiEventListener(preview);
        int notesY = MATERIALS_Y + rows(materials(blueprint).size()) * 18 + 2;
        List<FormattedText> notes = new ArrayList<>();
        for (String key : blueprint.noteKeys()) notes.add(Component.translatable(key));
        if (!notes.isEmpty()) builder.addScrollBoxWidget(WIDTH, HEIGHT - notesY, 0, notesY).setContents(notes);
    }

    /** The 3D view and its input: drag turns, the wheel zooms, a click on the layer label changes the layer. */
    private static final class Preview implements IRecipeWidget, IJeiGuiEventListener {
        private final StructureView view;
        private final ScreenRectangle area = new ScreenRectangle(0, VIEW_Y, WIDTH, VIEW_H);
        private boolean dragged;

        Preview(StructureView view) {
            this.view = view;
        }

        @Override public ScreenPosition getPosition() { return new ScreenPosition(0, VIEW_Y); }
        @Override public ScreenRectangle getArea() { return area; }

        private Component layerLabel() {
            return view.layer() < 0 ? Component.translatable("jei.mio_icif.structure.layer_all", view.blueprint().sizeY())
                : Component.translatable("jei.mio_icif.structure.layer", view.layer() + 1, view.blueprint().sizeY());
        }

        private int labelWidth() {
            return Minecraft.getInstance().font.width(layerLabel()) + 6;
        }

        /** Over the layer label (coordinates of this widget). */
        private boolean onLabel(double x, double y) {
            return x >= WIDTH - labelWidth() && x < WIDTH && y >= 0 && y < 11;
        }

        @Override
        public void drawWidget(GuiGraphics graphics, double mouseX, double mouseY) {
            graphics.fill(0, 0, WIDTH, VIEW_H, 0x20000000);
            view.render(graphics, 0, 0, WIDTH, VIEW_H);
            int w = labelWidth();
            boolean hot = onLabel(mouseX, mouseY);
            graphics.fill(WIDTH - w, 0, WIDTH, 11, hot ? 0xC0405060 : 0x80303840);
            graphics.drawString(Minecraft.getInstance().font, layerLabel(), WIDTH - w + 3, 2, 0xFFFFFFFF, false);
        }

        @Override
        public void getTooltip(ITooltipBuilder tooltip, double mouseX, double mouseY) {
            // widget coordinates; some JEI versions ask every widget, so test the bounds here
            if (mouseX < 0 || mouseX >= WIDTH || mouseY < 0 || mouseY >= VIEW_H) return;
            if (onLabel(mouseX, mouseY)) tooltip.add(Component.translatable("jei.mio_icif.structure.layer_help"));
            else tooltip.add(Component.translatable("jei.mio_icif.structure.view_help").withStyle(ChatFormatting.GRAY));
        }

        // JEI gives the listener coordinates relative to getArea(); a click is a press (mouseClicked)
        // and then a release (mouseReleased), which does the action.
        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            dragged = false;
            return onLabel(mouseX, mouseY) || button == 0;
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            if (!dragged && onLabel(mouseX, mouseY)) {
                view.stepLayer(button == 1 ? -1 : 1);
                return true;
            }
            boolean was = dragged;
            dragged = false;
            return was;
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
            if (button != 0) return false;
            dragged = true;
            view.drag(dx, dy);
            return true;
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
            view.zoom(scrollY);
            return true;
        }
    }
}
