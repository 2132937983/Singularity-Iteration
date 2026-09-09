package com.singularity_iteration.mio_icif.Screen;

import com.singularity_iteration.mio_icif.Menu.Producer.BatchCrafterMenu;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_batch_crafter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.Optional;

@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_batch_crafter extends mio_icif_screen<BatchCrafterMenu> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.parse("mio_icif:textures/gui/gui_batch_crafter.png");

    private static final int ENERGY_GAUGE_X = 9;
    private static final int ENERGY_GAUGE_Y = 45;

    private static final int PROGRESS_ARROW_X = 90;
    private static final int PROGRESS_ARROW_Y = 35;

    private static final int RECIPE_OUTPUT_X = 94;
    private static final int RECIPE_OUTPUT_Y = 14;

    public mio_icif_gui_batch_crafter(BatchCrafterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = BatchCrafterMenu.GUI_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = this.imageHeight - 94;
    }

    private ItemStack computeRecipeOutputFromPhantomSlots() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) return ItemStack.EMPTY;

        BatchCrafterMenu menu = this.getMenu();
        TransientCraftingContainer container = new TransientCraftingContainer(
            new net.minecraft.world.inventory.AbstractContainerMenu(null, -1) {
                @Override
                public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player, int index) {
                    return ItemStack.EMPTY;
                }
                @Override
                public boolean stillValid(net.minecraft.world.entity.player.Player player) {
                    return true;
                }
            }, 3, 3);

        for (int i = 0; i < 9; i++) {
            int slotIndex = BatchCrafterMenu.MENU_PHANTOM_START + i;
            if (slotIndex < menu.slots.size()) {
                Slot slot = menu.slots.get(slotIndex);
                container.setItem(i, slot.getItem());
            }
        }

        Optional<RecipeHolder<CraftingRecipe>> recipe = mc.level.getRecipeManager()
            .getRecipeFor(RecipeType.CRAFTING, container.asCraftInput(), mc.level);

        return recipe.map(r -> r.value().assemble(container.asCraftInput(), mc.level.registryAccess()))
            .orElse(ItemStack.EMPTY);
    }

    private ItemStack getClientRecipeOutput() {
        mio_icif_batch_crafter be = this.getMenu().getBlockEntity();
        if (be != null) {
            ItemStack output = be.getRecipeOutput();
            if (!output.isEmpty()) return output;
        }

        BlockPos pos = this.getMenu().getBlockPos();
        if (pos != null && minecraft != null && minecraft.level != null) {
            var levelBe = minecraft.level.getBlockEntity(pos);
            if (levelBe instanceof mio_icif_batch_crafter crafter) {
                ItemStack output = crafter.getRecipeOutput();
                if (!output.isEmpty()) return output;
            }
        }

        return computeRecipeOutputFromPhantomSlots();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        BatchCrafterMenu menu = this.getMenu();
        if (menu != null) {
            drawLightningEnergy(guiGraphics, x + ENERGY_GAUGE_X, y + ENERGY_GAUGE_Y, menu.getEnergy(), menu.getMaxEnergy());

            guiGraphics.blit(ATLAS_TEXTURE, x + PROGRESS_ARROW_X, y + PROGRESS_ARROW_Y, 0,
                (float) PROGRESS_BAR_BG_U, (float) PROGRESS_BAR_BG_V,
                PROGRESS_BAR_BG_WIDTH, PROGRESS_BAR_BG_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);
            if (menu.isWorking()) {
                int progress = menu.getProgress();
                int maxProgress = menu.getMaxProgress();
                if (maxProgress > 0) {
                    int progressPixels = (progress * 24) / maxProgress;
                    guiGraphics.blit(ATLAS_TEXTURE, x + PROGRESS_ARROW_X, y + PROGRESS_ARROW_Y,
                        0, (float) ARROW_U, (float) ARROW_V, progressPixels, ARROW_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);
                }
            }

            ItemStack recipeOutput = getClientRecipeOutput();
            if (!recipeOutput.isEmpty()) {
                guiGraphics.renderItem(recipeOutput, x + RECIPE_OUTPUT_X, y + RECIPE_OUTPUT_Y);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderTooltip(guiGraphics, mouseX, mouseY);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        if (mouseX >= x + ENERGY_GAUGE_X && mouseX <= x + ENERGY_GAUGE_X + LIGHTNING_WIDTH &&
            mouseY >= y + ENERGY_GAUGE_Y && mouseY <= y + ENERGY_GAUGE_Y + LIGHTNING_HEIGHT) {
            BatchCrafterMenu menu = this.getMenu();
            if (menu != null) {
                guiGraphics.renderTooltip(this.font,
                    Component.literal("Energy: " + menu.getEnergy() + "/" + menu.getMaxEnergy() + " EU"),
                    mouseX, mouseY);
            }
        }

        if (mouseX >= x + RECIPE_OUTPUT_X && mouseX < x + RECIPE_OUTPUT_X + 16 &&
            mouseY >= y + RECIPE_OUTPUT_Y && mouseY < y + RECIPE_OUTPUT_Y + 16) {
            ItemStack recipeOutput = getClientRecipeOutput();
            if (!recipeOutput.isEmpty()) {
                guiGraphics.renderTooltip(this.font, recipeOutput, mouseX, mouseY);
            }
        }
    }
}