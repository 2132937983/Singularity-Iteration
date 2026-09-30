package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.item.armor.ArmorFeatureSlots;
import com.miophas.singularity_iteration.common.network.mio_icif_Network;
import com.miophas.singularity_iteration.core.api.item.ArmorFeatureInfo;
import com.miophas.singularity_iteration.core.prefab.item.ArmorFeatures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * 装备特性管理 GUI。
 *
 * <p>按 Alt 键打开，列出玩家身上所有"可管理特性"的装备槽
 * （原版四格 + 可选的 Curios 背槽，见 {@link ArmorFeatureSlots}），
 * 每个特性一个按钮：开关型点击取反，模式型点击切换到下一模式。
 *
 * <p>全部数据来自 {@link ArmorFeatures}（即物品声明的 {@link ArmorFeatureInfo}），
 * 不含任何物品类判断，因此附属模组的装备与自定义特性会自动出现在界面中。
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_armor_features extends Screen {

    // GUI 纹理
    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "textures/gui/gui_armour_feature_manager.png");

    // 原版按钮纹理
    private static final ResourceLocation BUTTON_SPRITE =
        ResourceLocation.withDefaultNamespace("widget/button");
    private static final ResourceLocation BUTTON_HIGHLIGHTED_SPRITE =
        ResourceLocation.withDefaultNamespace("widget/button_highlighted");

    // GUI 尺寸 (256x256 纹理)
    private static final int GUI_WIDTH = 200;
    private static final int GUI_HEIGHT = 180;

    // 装备图标位置
    private static final int ARMOR_X = 14;
    private static final int FIRST_ROW_Y = 14;
    private static final int STANDARD_ROW_GAP = 42;
    private static final int COMPACT_ROW_GAP = 30;

    // 特性按钮参数（水平排列）
    private static final int BUTTON_X = 44;
    private static final int BUTTON_WIDTH = 29;
    private static final int BUTTON_HEIGHT = 9;
    private static final int BUTTON_GAP = 1;

    // 点击区域列表
    private final List<FeatureButton> featureButtons = new ArrayList<>();

    public mio_icif_gui_armor_features() {
        super(Component.translatable("gui.mio_icif.armor_features.title"));
    }

    @Override
    protected void init() {
        super.init();
        rebuildButtons();
    }

    private static int rowGap(int rows) {
        return rows <= 4 ? STANDARD_ROW_GAP : COMPACT_ROW_GAP;
    }

    private void rebuildButtons() {
        featureButtons.clear();

        int guiLeft = (this.width - GUI_WIDTH) / 2;
        int guiTop = (this.height - GUI_HEIGHT) / 2;

        Player player = minecraft == null ? null : minecraft.player;
        if (player == null) return;

        List<ArmorFeatureSlots.EquippedSlot> slots = ArmorFeatureSlots.equipped(player);
        int gap = rowGap(slots.size());

        for (int slotIndex = 0; slotIndex < slots.size(); slotIndex++) {
            ArmorFeatureSlots.EquippedSlot slot = slots.get(slotIndex);
            List<ArmorFeatureInfo> features = ArmorFeatures.features(slot.stack());
            int baseY = guiTop + FIRST_ROW_Y + slotIndex * gap;

            for (int featureIndex = 0; featureIndex < features.size(); featureIndex++) {
                int buttonX = guiLeft + BUTTON_X + featureIndex * (BUTTON_WIDTH + BUTTON_GAP);
                featureButtons.add(new FeatureButton(
                    buttonX, baseY, BUTTON_WIDTH, BUTTON_HEIGHT,
                    slot.id(), slot.stack(), features.get(featureIndex).featureKey()
                ));
            }
        }
    }

    /**
     * 切换 / 循环一个特性（乐观更新 + 同步到服务端）。
     */
    private void toggleFeature(String slotId, String featureKey) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        String effectiveSlot = slotId;
        ItemStack stack = ArmorFeatureSlots.stackById(mc.player, effectiveSlot);
        if (ArmorFeatures.find(stack, featureKey) == null) {
            effectiveSlot = ArmorFeatureSlots.findDeclaringSlotId(mc.player, featureKey);
            if (effectiveSlot == null) return;
            stack = ArmorFeatureSlots.stackById(mc.player, effectiveSlot);
        }

        ArmorFeatureInfo info = ArmorFeatures.find(stack, featureKey);
        if (info == null) return;

        if (info.isMode()) {
            ArmorFeatures.cycleMode(stack, featureKey);
            mio_icif_Network.sendArmorFeatureCycle(effectiveSlot, featureKey);
        } else {
            boolean newState = ArmorFeatures.toggle(stack, featureKey);
            mio_icif_Network.sendArmorFeatureToggle(effectiveSlot, featureKey, newState);
        }
        rebuildButtons();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        int guiLeft = (this.width - GUI_WIDTH) / 2;
        int guiTop = (this.height - GUI_HEIGHT) / 2;

        // 绘制 GUI 纹理
        guiGraphics.blit(GUI_TEXTURE, guiLeft, guiTop, 0, 0, GUI_WIDTH, GUI_HEIGHT);

        // 绘制标题
        guiGraphics.drawString(this.font, this.title, guiLeft + 8, guiTop + 6, 0x404040, false);

        Player player = minecraft == null ? null : minecraft.player;
        if (player == null) return;

        List<ArmorFeatureSlots.EquippedSlot> slots = ArmorFeatureSlots.equipped(player);
        int gap = rowGap(slots.size());

        for (int slotIndex = 0; slotIndex < slots.size(); slotIndex++) {
            ArmorFeatureSlots.EquippedSlot slot = slots.get(slotIndex);
            ItemStack stack = slot.stack();
            int slotY = guiTop + FIRST_ROW_Y + slotIndex * gap;

            // 绘制装备图标
            guiGraphics.renderItem(stack, guiLeft + ARMOR_X, slotY);
            guiGraphics.renderItemDecorations(this.font, stack, guiLeft + ARMOR_X, slotY);

            if (stack.isEmpty()) {
                Component emptyName = Component.translatable("gui.mio_icif.armor_features.empty_" + slot.id());
                guiGraphics.drawString(this.font, emptyName, guiLeft + ARMOR_X + 20, slotY + 4, 0x808080, false);
                continue;
            }

            List<ArmorFeatureInfo> features = ArmorFeatures.features(stack);
            for (int featureIndex = 0; featureIndex < features.size(); featureIndex++) {
                ArmorFeatureInfo feature = features.get(featureIndex);
                boolean enabled = ArmorFeatures.isEnabled(stack, feature.featureKey());
                Component label = feature.isMode() && feature.currentModeName() != null
                    ? feature.currentModeName()
                    : Component.translatable(feature.featureNameKey());

                FeatureButton btn = findButton(slot.id(), feature.featureKey());
                boolean hovered = btn != null && btn.isHovered(mouseX, mouseY);

                int buttonX = guiLeft + BUTTON_X + featureIndex * (BUTTON_WIDTH + BUTTON_GAP);

                ResourceLocation sprite = hovered ? BUTTON_HIGHLIGHTED_SPRITE : BUTTON_SPRITE;
                guiGraphics.blitSprite(sprite, buttonX, slotY, BUTTON_WIDTH, BUTTON_HEIGHT);

                float scale = 0.7f;
                int textWidth = this.font.width(label);
                int scaledWidth = (int) (textWidth * scale);
                int textX = buttonX + (BUTTON_WIDTH - scaledWidth) / 2;
                int textY = slotY + (BUTTON_HEIGHT - 6) / 2;
                int textColor = feature.isMode() ? 0x55FFFF : (enabled ? 0x55FF55 : 0xFF5555);

                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(textX, textY, 0);
                guiGraphics.pose().scale(scale, scale, 1.0f);
                guiGraphics.drawString(this.font, label, 0, 0, textColor, false);
                guiGraphics.pose().popPose();
            }
        }
    }

    private FeatureButton findButton(String slotId, String featureKey) {
        for (FeatureButton btn : featureButtons) {
            if (btn.slotId.equals(slotId) && btn.featureKey.equals(featureKey)) {
                return btn;
            }
        }
        return null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (FeatureButton btn : featureButtons) {
                if (btn.isHovered((int) mouseX, (int) mouseY)) {
                    toggleFeature(btn.slotId, btn.featureKey);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // ESC键关闭
        if (keyCode == 256) {
            this.onClose();
            return true;
        }
        // 背包键（E键，keyCode 69）关闭
        if (keyCode == 69) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private static class FeatureButton {
        final int x, y, width, height;
        final String slotId;
        final ItemStack stack;
        final String featureKey;

        FeatureButton(int x, int y, int width, int height, String slotId, ItemStack stack, String featureKey) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.slotId = slotId;
            this.stack = stack;
            this.featureKey = featureKey;
        }

        boolean isHovered(int mouseX, int mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }
    }
}
