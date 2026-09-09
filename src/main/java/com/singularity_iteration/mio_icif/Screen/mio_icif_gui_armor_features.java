package com.singularity_iteration.mio_icif.Screen;

import com.singularity_iteration.mio_icif.Items.Armor.ArmorFeatureToggle;
import com.singularity_iteration.mio_icif.Singularity_Iteration;
import com.singularity_iteration.mio_icif.api.MioIcifAPI;
import com.singularity_iteration.mio_icif.api.item.ArmorFeatureInfo;
import com.singularity_iteration.mio_icif.network.mio_icif_Network;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * 装备特性管理 GUI
 * 按 Alt 键打开，用于管理玩家身上装备的特性开关
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
    private static final int[] ARMOR_Y = {14, 56, 98, 140};
    private static final int ARMOR_X = 14;

    // 特性按钮参数（水平排列）
    private static final int BUTTON_X = 44;
    private static final int BUTTON_WIDTH = 30;
    private static final int BUTTON_HEIGHT = 9;
    private static final int BUTTON_GAP = 2;

    // 装备槽位顺序
    private static final EquipmentSlot[] SLOTS = {
        EquipmentSlot.HEAD,
        EquipmentSlot.CHEST,
        EquipmentSlot.LEGS,
        EquipmentSlot.FEET
    };

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

    private void rebuildButtons() {
        featureButtons.clear();

        int guiLeft = (this.width - GUI_WIDTH) / 2;
        int guiTop = (this.height - GUI_HEIGHT) / 2;

        Player player = minecraft.player;
        if (player == null) return;

        // 为每个装备槽位创建特性按钮（水平排列）
        for (int slotIndex = 0; slotIndex < SLOTS.length; slotIndex++) {
            EquipmentSlot slot = SLOTS[slotIndex];
            ItemStack stack = player.getItemBySlot(slot);

            if (!stack.isEmpty()) {
                List<FeatureInfo> features = getFeaturesForSlot(slot, stack);
                int baseY = guiTop + ARMOR_Y[slotIndex];

                for (int featureIndex = 0; featureIndex < features.size(); featureIndex++) {
                    FeatureInfo feature = features.get(featureIndex);
                    int buttonX = guiLeft + BUTTON_X + featureIndex * (BUTTON_WIDTH + BUTTON_GAP);

                    featureButtons.add(new FeatureButton(
                        buttonX, baseY, BUTTON_WIDTH, BUTTON_HEIGHT,
                        slot, stack, feature.featureKey
                    ));
                }
            }
        }
    }

    /**
     * 获取指定装备槽位的所有特性
     */
    private List<FeatureInfo> getFeaturesForSlot(EquipmentSlot slot, ItemStack stack) {
        List<FeatureInfo> features = new ArrayList<>();

        List<ArmorFeatureInfo> itemFeatures = MioIcifAPI.instance().getItemAPI().getArmorFeatures(stack);
        for (ArmorFeatureInfo f : itemFeatures) {
            if (f.slot() == slot) {
                features.add(new FeatureInfo(f.featureKey(), f.featureNameKey(), f.isMode(), f.currentModeName()));
            }
        }

        return features;
    }

    /**
     * 切换特性状态
     */
    private void toggleFeature(EquipmentSlot slot, ItemStack stack, String featureKey) {
        if (featureKey != null) {
            boolean newState = ArmorFeatureToggle.toggle(stack, featureKey);
            mio_icif_Network.sendArmorFeatureToggle(slot, featureKey, newState);
            rebuildButtons();
        }
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

        Player player = minecraft.player;
        if (player != null) {
            // 绘制每个装备槽位
            for (int slotIndex = 0; slotIndex < SLOTS.length; slotIndex++) {
                EquipmentSlot slot = SLOTS[slotIndex];
                ItemStack stack = player.getItemBySlot(slot);
                int slotY = guiTop + ARMOR_Y[slotIndex];

                // 绘制装备图标
                guiGraphics.renderItem(stack, guiLeft + ARMOR_X, slotY);
                guiGraphics.renderItemDecorations(this.font, stack, guiLeft + ARMOR_X, slotY);

                if (!stack.isEmpty()) {
                    // 绘制特性按钮（水平排列）
                    List<FeatureInfo> features = getFeaturesForSlot(slot, stack);
                    for (int featureIndex = 0; featureIndex < features.size(); featureIndex++) {
                        FeatureInfo feature = features.get(featureIndex);
                        boolean enabled = ArmorFeatureToggle.isEnabled(stack, feature.featureKey);
                        Component featureName;

                        if (feature.isMode && feature.currentModeName != null) {
                            // 模式特性显示当前模式名称
                            featureName = feature.currentModeName;
                        } else {
                            featureName = Component.translatable(feature.featureNameKey);
                        }

                        FeatureButton btn = findButton(slot, feature.featureKey);
                        boolean hovered = btn != null && btn.isHovered(mouseX, mouseY);

                        int buttonX = guiLeft + BUTTON_X + featureIndex * (BUTTON_WIDTH + BUTTON_GAP);

                        // 绘制原版按钮纹理
                        ResourceLocation sprite = hovered ? BUTTON_HIGHLIGHTED_SPRITE : BUTTON_SPRITE;
                        guiGraphics.blitSprite(sprite, buttonX, slotY, BUTTON_WIDTH, BUTTON_HEIGHT);

                        // 绘制特性名称（居中，缩小字体）
                        float scale = 0.7f;
                        int textWidth = this.font.width(featureName);
                        int scaledWidth = (int)(textWidth * scale);
                        int textX = buttonX + (BUTTON_WIDTH - scaledWidth) / 2;
                        int textY = slotY + (BUTTON_HEIGHT - 6) / 2;
                        int textColor = feature.isMode ? 0x55FFFF : (enabled ? 0x55FF55 : 0xFF5555);

                        guiGraphics.pose().pushPose();
                        guiGraphics.pose().translate(textX, textY, 0);
                        guiGraphics.pose().scale(scale, scale, 1.0f);
                        guiGraphics.drawString(this.font, featureName, 0, 0, textColor, false);
                        guiGraphics.pose().popPose();
                    }
                } else {
                    // 空槽位显示灰色文字
                    Component emptyName = Component.translatable("gui.mio_icif.armor_features.empty_" + slot.getName().toLowerCase());
                    guiGraphics.drawString(this.font, emptyName, guiLeft + ARMOR_X + 20, slotY + 4, 0x808080, false);
                }
            }
        }
    }

    private FeatureButton findButton(EquipmentSlot slot, String featureKey) {
        for (FeatureButton btn : featureButtons) {
            if (btn.slot == slot && btn.featureKey.equals(featureKey)) {
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
                    toggleFeature(btn.slot, btn.stack, btn.featureKey);
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

    private static class FeatureInfo {
        final String featureKey;
        final String featureNameKey;
        final boolean isMode;
        final net.minecraft.network.chat.Component currentModeName;

        FeatureInfo(String key, String nameKey, boolean mode, net.minecraft.network.chat.Component modeName) {
            this.featureKey = key;
            this.featureNameKey = nameKey;
            this.isMode = mode;
            this.currentModeName = modeName;
        }
    }

    private static class FeatureButton {
        final int x, y, width, height;
        final EquipmentSlot slot;
        final ItemStack stack;
        final String featureKey;

        FeatureButton(int x, int y, int width, int height, EquipmentSlot slot, ItemStack stack, String featureKey) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.slot = slot;
            this.stack = stack;
            this.featureKey = featureKey;
        }

        boolean isHovered(int mouseX, int mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }
    }
}