package com.miophas.singularity_iteration.core.prefab.item;

import com.miophas.singularity_iteration.core.api.item.ArmorFeatureInfo;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

/**
 * 装备特性 Tooltip 统一渲染。
 *
 * <p>为 {@link IElectricArmorItem#getFeatures(ItemStack) 声明的特性}生成可点击行：

 * <ul>
 *   <li>开关型：{@code 特性名: 开/关}，点击取反；</li>
 *   <li>模式型：{@code 特性名: 当前模式 [切换模式]}，点击切换到下一模式。</li>
 * </ul>
 *
 * <p>点击通过 {@code /mio_icif toggle <slot> <featureKey>} 命令执行，服务端由宿主统一处理，
 * 因此 core / common / 附属模组的物品共用同一交互路径。
 */
public final class ArmorFeatureTooltip {

    /** 命令根节点。 */
    public static final String COMMAND_ROOT = "mio_icif";
    /** 切换子命令。 */
    public static final String COMMAND_TOGGLE = "toggle";

    private static final String KEY_FEATURE_ON = "tooltip.mio_icif.armor.feature_on";
    private static final String KEY_FEATURE_OFF = "tooltip.mio_icif.armor.feature_off";
    private static final String KEY_CLICK_HINT = "tooltip.mio_icif.armor.click_toggle_hint";
    private static final String KEY_SWITCH_MODE = "tooltip.mio_icif.armor.switch_mode";

    private ArmorFeatureTooltip() {
    }

    /**
     * 追加全部特性行。
     */
    public static void appendAll(List<Component> tooltip, ItemStack stack, List<ArmorFeatureInfo> features) {
        for (ArmorFeatureInfo feature : features) {
            append(tooltip, stack, feature);
        }
    }

    /**
     * 追加单条特性行。
     */
    public static void append(List<Component> tooltip, ItemStack stack, ArmorFeatureInfo feature) {
        if (feature == null) {
            return;
        }
        if (feature.isMode()) {
            appendModeLine(tooltip, stack, feature);
        } else {
            appendToggleLine(tooltip, stack, feature);
        }
    }

    /** 生成点击命令。 */
    public static String toggleCommand(ArmorFeatureInfo feature) {
        String slot = feature.slot() == null ? "chest" : feature.slot().getName().toLowerCase();
        return "/" + COMMAND_ROOT + " " + COMMAND_TOGGLE + " " + slot + " " + feature.featureKey();
    }

    private static void appendToggleLine(List<Component> tooltip, ItemStack stack, ArmorFeatureInfo feature) {
        boolean enabled = ArmorFeatureState.isEnabled(stack, feature.featureKey());
        ChatFormatting statusColor = enabled ? ChatFormatting.GREEN : ChatFormatting.RED;
        String statusKey = enabled ? KEY_FEATURE_ON : KEY_FEATURE_OFF;

        MutableComponent featureName = Component.translatable(feature.featureNameKey());

        tooltip.add(Component.literal("  ")
            .append(featureName.withStyle(ChatFormatting.GRAY))
            .append(Component.literal(" "))
            .append(Component.translatable(statusKey).withStyle(style -> style
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, toggleCommand(feature)))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    Component.translatable(KEY_CLICK_HINT,
                        featureName,
                        Component.translatable(enabled ? KEY_FEATURE_OFF : KEY_FEATURE_ON))))
                .withUnderlined(true)
                .withColor(statusColor)
            ))
        );
    }

    private static void appendModeLine(List<Component> tooltip, ItemStack stack, ArmorFeatureInfo feature) {
        MutableComponent featureName = Component.translatable(feature.featureNameKey());
        MutableComponent modeName = feature.currentModeName() != null
            ? feature.currentModeName().copy()
            : Component.translatable(feature.featureNameKey() + ".default");

        tooltip.add(Component.literal("  ")
            .append(featureName.withStyle(ChatFormatting.GRAY))
            .append(Component.literal(": "))
            .append(modeName.withStyle(ChatFormatting.AQUA))
            .append(Component.literal(" "))
            .append(Component.translatable(KEY_SWITCH_MODE).withStyle(style -> style
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, toggleCommand(feature)))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    Component.translatable(KEY_CLICK_HINT,
                        featureName,
                        Component.translatable(KEY_SWITCH_MODE))))
                .withUnderlined(true)
                .withColor(ChatFormatting.YELLOW)
            ))
        );
    }
}
