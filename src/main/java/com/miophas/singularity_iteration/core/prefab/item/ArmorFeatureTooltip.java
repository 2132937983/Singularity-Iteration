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
 * Shared tooltip rendering for equipment features.
 *
 * <p>Makes clickable lines for the {@link IElectricArmorItem#getFeatures(ItemStack) declared features}:

 * <ul>
 *   <li>Toggle type: {@code feature name: on/off}. A click inverts the value.</li>
 *   <li>Mode type: {@code feature name: current mode [switch mode]}. A click goes to the next mode.</li>
 * </ul>
 *
 * <p>A click runs the command {@code /mio_icif toggle <slot> <featureKey>}. The host mod handles it on the server.
 * Thus items from core, common and addon mods all use the same interaction path.
 */
public final class ArmorFeatureTooltip {

    /** The root command node. */
    public static final String COMMAND_ROOT = "mio_icif";
    /** The toggle subcommand. */
    public static final String COMMAND_TOGGLE = "toggle";

    private static final String KEY_FEATURE_ON = "tooltip.mio_icif.armor.feature_on";
    private static final String KEY_FEATURE_OFF = "tooltip.mio_icif.armor.feature_off";
    private static final String KEY_CLICK_HINT = "tooltip.mio_icif.armor.click_toggle_hint";
    private static final String KEY_SWITCH_MODE = "tooltip.mio_icif.armor.switch_mode";

    private ArmorFeatureTooltip() {
    }

    /**
     * Adds one line for each feature to the tooltip.
     */
    public static void appendAll(List<Component> tooltip, ItemStack stack, List<ArmorFeatureInfo> features) {
        for (ArmorFeatureInfo feature : features) {
            append(tooltip, stack, feature);
        }
    }

    /**
     * Adds the line for one feature to the tooltip. Does nothing if {@code feature} is null.
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

    /** Makes the click command. If the feature has no slot, the slot is "chest". */
    public static String toggleCommand(ArmorFeatureInfo feature) {
        String slot = feature.slot() == null ? "chest" : feature.slot().getName().toLowerCase(java.util.Locale.ROOT);
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
