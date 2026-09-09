package com.singularity_iteration.mio_icif.Items.Armor;

import com.singularity_iteration.mio_icif.api.item.ArmorFeatureInfo;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

@SuppressWarnings("null")
public class ArmorFeatureToggle {

    private static final String FEATURE_PREFIX = "feat_";

    /**
     * @deprecated 使用 {@link ArmorFeatureInfo} 代替。
     * 此类型别名仅为内部向后兼容保留。
     */
    @Deprecated
    public static record FeatureInfo(EquipmentSlot slot, String featureKey, String featureNameKey, boolean isMode, Component currentModeName) {
        public FeatureInfo(EquipmentSlot slot, String featureKey, String featureNameKey) {
            this(slot, featureKey, featureNameKey, false, null);
        }

        public FeatureInfo(EquipmentSlot slot, String featureKey, String featureNameKey, Component currentModeName) {
            this(slot, featureKey, featureNameKey, true, currentModeName);
        }

        public ArmorFeatureInfo toApi() {
            return new ArmorFeatureInfo(slot, featureKey, featureNameKey, isMode, currentModeName);
        }
    }

    public static ArmorFeatureInfo toApi(FeatureInfo info) {
        return new ArmorFeatureInfo(info.slot, info.featureKey, info.featureNameKey, info.isMode, info.currentModeName);
    }

    public static FeatureInfo fromApi(ArmorFeatureInfo info) {
        return new FeatureInfo(info.slot(), info.featureKey(), info.featureNameKey(), info.isMode(), info.currentModeName());
    }

    public static boolean isEnabled(ItemStack stack, String featureKey) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null && !customData.isEmpty()) {
            CompoundTag tag = customData.copyTag();
            String fullKey = FEATURE_PREFIX + featureKey;
            // 如果NBT中明确设置了该特性，返回设置值
            if (tag.contains(fullKey)) {
                return tag.getBoolean(fullKey);
            }
        }
        // 默认启用（新物品或没有明确设置时）
        return true;
    }

    public static void setEnabled(ItemStack stack, String featureKey, boolean enabled) {
        CompoundTag tag = new CompoundTag();
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            tag = customData.copyTag();
        }
        tag.putBoolean(FEATURE_PREFIX + featureKey, enabled);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static boolean toggle(ItemStack stack, String featureKey) {
        boolean current = isEnabled(stack, featureKey);
        setEnabled(stack, featureKey, !current);
        return !current;
    }

    public static void addToggleLine(List<Component> tooltip, ItemStack stack, EquipmentSlot slot, String featureKey, String featureNameKey) {
        boolean enabled = isEnabled(stack, featureKey);
        String slotName = slot.getName().toLowerCase();
        MutableComponent featureName = Component.translatable(featureNameKey);
        MutableComponent statusText = enabled
            ? Component.translatable("tooltip.mio_icif.armor.feature_on")
            : Component.translatable("tooltip.mio_icif.armor.feature_off");
        ChatFormatting statusColor = enabled ? ChatFormatting.GREEN : ChatFormatting.RED;

        tooltip.add(Component.literal("  ")
            .append(featureName.withStyle(ChatFormatting.GRAY))
            .append(Component.literal(" "))
            .append(statusText.withStyle(style -> style
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                    "/mio_icif toggle " + slotName + " " + featureKey))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    Component.translatable("tooltip.mio_icif.armor.click_toggle_hint",
                        featureName,
                        enabled
                            ? Component.translatable("tooltip.mio_icif.armor.feature_off")
                            : Component.translatable("tooltip.mio_icif.armor.feature_on"))))
                .withUnderlined(true)
                .withColor(statusColor)
            ))
        );
    }

    public static void addModeLine(List<Component> tooltip, ItemStack stack, EquipmentSlot slot, String featureKey, String featureNameKey, Component currentModeName) {
        String slotName = slot.getName().toLowerCase();
        MutableComponent featureName = Component.translatable(featureNameKey);
        MutableComponent modeName = currentModeName.copy();

        tooltip.add(Component.literal("  ")
            .append(featureName.withStyle(ChatFormatting.GRAY))
            .append(Component.literal(": "))
            .append(modeName.withStyle(ChatFormatting.AQUA))
            .append(Component.literal(" "))
            .append(Component.translatable("tooltip.mio_icif.armor.switch_mode")
                .withStyle(style -> style
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                        "/mio_icif toggle " + slotName + " " + featureKey))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.translatable("tooltip.mio_icif.armor.click_toggle_hint",
                            featureName,
                            Component.translatable("tooltip.mio_icif.armor.switch_mode"))))
                    .withUnderlined(true)
                    .withColor(ChatFormatting.YELLOW)
                )
            )
        );
    }

    public static void addFeaturesToTooltip(List<Component> tooltip, ItemStack stack, List<FeatureInfo> features) {
        for (FeatureInfo feature : features) {
            if (feature.isMode) {
                addModeLine(tooltip, stack, feature.slot, feature.featureKey, feature.featureNameKey, feature.currentModeName);
            } else {
                addToggleLine(tooltip, stack, feature.slot, feature.featureKey, feature.featureNameKey);
            }
        }
    }
}