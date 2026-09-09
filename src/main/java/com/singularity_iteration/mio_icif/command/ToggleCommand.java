package com.singularity_iteration.mio_icif.command;

import com.singularity_iteration.mio_icif.Items.Armor.mio_icif_boots_quantum;
import com.singularity_iteration.mio_icif.Items.Armor.mio_icif_chestplate_advanced_jetpack;
import com.singularity_iteration.mio_icif.Items.Armor.mio_icif_chestplate_advanced_quantum;
import com.singularity_iteration.mio_icif.Items.Armor.mio_icif_chestplate_heavy_quantum;
import com.singularity_iteration.mio_icif.Items.Armor.mio_icif_chestplate_jetpack_elc;
import com.singularity_iteration.mio_icif.Items.Armor.mio_icif_chestplate_quantum;
import com.singularity_iteration.mio_icif.Items.Armor.mio_icif_diving_mask;
import com.singularity_iteration.mio_icif.Items.Armor.mio_icif_helmet_nano;
import com.singularity_iteration.mio_icif.Items.Armor.mio_icif_helmet_quantum;
import com.singularity_iteration.mio_icif.Items.Armor.mio_icif_leggings_nano;
import com.singularity_iteration.mio_icif.Items.Armor.mio_icif_leggings_quantum;
import com.singularity_iteration.mio_icif.api.MioIcifAPI;
import com.singularity_iteration.mio_icif.api.item.ArmorFeatureInfo;
import com.singularity_iteration.mio_icif.api.item.IJetpackItem;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("null")
public class ToggleCommand {

    private static final String[] SLOT_NAMES = {"head", "chest", "legs", "feet"};

    private static final Map<Class<?>, String[]> ARMOR_FEATURES;
    static {
        Map<Class<?>, String[]> map = new HashMap<>();
        map.put(mio_icif_helmet_quantum.class, new String[]{"night_vision", "water_breathing", "auto_food"});
        map.put(mio_icif_chestplate_quantum.class, new String[]{"flight", "fire_resistance", "jetpack_mode"});
        map.put(mio_icif_leggings_quantum.class, new String[]{"speed_boost"});
        map.put(mio_icif_boots_quantum.class, new String[]{"high_jump", "fire_resistance"});
        map.put(mio_icif_helmet_nano.class, new String[]{"night_vision"});
        map.put(mio_icif_leggings_nano.class, new String[]{"speed_boost"});
        map.put(mio_icif_chestplate_jetpack_elc.class, new String[]{"jetpack_mode"});
        map.put(mio_icif_diving_mask.class, new String[]{"oxygen_restore"});
        map.put(mio_icif_chestplate_advanced_jetpack.class, new String[]{"jetpack_mode"});
        map.put(mio_icif_chestplate_heavy_quantum.class, new String[]{"healing", "fire_resistance", "knockback_resistance"});
        map.put(mio_icif_chestplate_advanced_quantum.class, new String[]{"flight", "jetpack_mode", "fire_resistance", "healing", "knockback_resistance"});
        ARMOR_FEATURES = Map.copyOf(map);
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mio_icif")
            .then(Commands.literal("toggle")
                .then(Commands.argument("slot", StringArgumentType.word())
                    .suggests((context, builder) ->
                        SharedSuggestionProvider.suggest(SLOT_NAMES, builder))
                    .then(Commands.argument("feature", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            String slotName = StringArgumentType.getString(context, "slot");
                            if (context.getSource().getEntity() instanceof ServerPlayer player) {
                                EquipmentSlot slot = parseSlot(slotName);
                                if (slot != null) {
                                    ItemStack stack = player.getItemBySlot(slot);
                                    String[] features = ARMOR_FEATURES.get(stack.getItem().getClass());
                                    if (features != null) {
                                        return SharedSuggestionProvider.suggest(features, builder);
                                    }
                                }
                            }
                            return builder.buildFuture();
                        })
                        .executes(context -> toggleFeature(
                            context.getSource(),
                            StringArgumentType.getString(context, "slot"),
                            StringArgumentType.getString(context, "feature")))
                    )
                    .executes(context -> toggleSlot(context.getSource(), StringArgumentType.getString(context, "slot")))
                )
            )
        );
    }

    private static EquipmentSlot parseSlot(String slotName) {
        return switch (slotName.toLowerCase()) {
            case "head" -> EquipmentSlot.HEAD;
            case "chest" -> EquipmentSlot.CHEST;
            case "legs" -> EquipmentSlot.LEGS;
            case "feet" -> EquipmentSlot.FEET;
            default -> null;
        };
    }

    private static int toggleSlot(CommandSourceStack source, String slotName) {
        if (!(source.getEntity() instanceof ServerPlayer player)) return 0;
        EquipmentSlot slot = parseSlot(slotName);
        if (slot == null) {
            player.sendSystemMessage(Component.translatable("command.mio_icif.toggle.invalid_slot", slotName));
            return 0;
        }
        ItemStack stack = player.getItemBySlot(slot);
        if (handleArmorFeatureToggle(stack, player, slot, null)) return 1;
        player.sendSystemMessage(Component.translatable("command.mio_icif.toggle.no_item"));
        return 0;
    }

    private static int toggleFeature(CommandSourceStack source, String slotName, String featureKey) {
        if (!(source.getEntity() instanceof ServerPlayer player)) return 0;
        EquipmentSlot slot = parseSlot(slotName);
        if (slot == null) {
            player.sendSystemMessage(Component.translatable("command.mio_icif.toggle.invalid_slot", slotName));
            return 0;
        }
        ItemStack stack = player.getItemBySlot(slot);
        if (handleArmorFeatureToggle(stack, player, slot, featureKey)) return 1;
        player.sendSystemMessage(Component.translatable("command.mio_icif.toggle.no_item"));
        return 0;
    }

    private static boolean handleArmorFeatureToggle(ItemStack stack, ServerPlayer player, EquipmentSlot slot, String featureKey) {
        if (stack.isEmpty()) return false;

        // Handle jetpack mode toggle via IJetpackItem API
        if (stack.getItem() instanceof IJetpackItem jetpack) {
            if ("jetpack_mode".equals(featureKey) || featureKey == null) {
                IJetpackItem.JetpackMode currentMode = jetpack.getMode(stack);
                IJetpackItem.JetpackMode newMode = (currentMode == IJetpackItem.JetpackMode.HOVER)
                    ? IJetpackItem.JetpackMode.FLIGHT
                    : IJetpackItem.JetpackMode.HOVER;
                jetpack.setMode(stack, newMode);
                player.setItemSlot(slot, stack);
                Component modeName = newMode == IJetpackItem.JetpackMode.HOVER
                    ? Component.translatable("hud.mio_icif.jetpack.mode_hover")
                    : Component.translatable("hud.mio_icif.jetpack.mode_jetpack");
                player.sendSystemMessage(Component.translatable("hud.mio_icif.jetpack.switch_mode", modeName));
                return true;
            }
        }



        if (featureKey != null) {
            boolean newState = MioIcifAPI.instance().getItemAPI().toggleArmorFeature(stack, featureKey);
            player.setItemSlot(slot, stack);
            
            // Try to get feature name from armor's getFeatures() method
            String nameKey = "tooltip.mio_icif.armor.feature_unknown";
            List<ArmorFeatureInfo> features = MioIcifAPI.instance().getItemAPI().getArmorFeatures(stack);
            for (ArmorFeatureInfo info : features) {
                if (info.featureKey().equals(featureKey)) {
                    nameKey = info.featureNameKey();
                    break;
                }
            }
            // Fallback to hardcoded mapping if not found
            if (nameKey.equals("tooltip.mio_icif.armor.feature_unknown")) {
                nameKey = getFeatureNameKey(featureKey);
            }
            
            player.sendSystemMessage(Component.translatable("command.mio_icif.toggle.feature",
                Component.translatable(nameKey),
                newState ? Component.translatable("tooltip.mio_icif.armor.feature_on") : Component.translatable("tooltip.mio_icif.armor.feature_off")));
            return true;
        }

        // 使用 getFeatures() 方法获取特性列表
        List<ArmorFeatureInfo> apiFeatures = MioIcifAPI.instance().getItemAPI().getArmorFeatures(stack);
        if (!apiFeatures.isEmpty()) {
            return handleArmorFeatureToggle(stack, player, slot, apiFeatures.get(0).featureKey());
        }

        String[] features = ARMOR_FEATURES.get(stack.getItem().getClass());
        if (features != null && features.length > 0) {
            return handleArmorFeatureToggle(stack, player, slot, features[0]);
        }

        return false;
    }

    private static String getFeatureNameKey(String featureKey) {
        return switch (featureKey) {
            case "night_vision" -> "tooltip.mio_icif.armor.feature_night_vision";
            case "water_breathing" -> "tooltip.mio_icif.armor.feature_water_breathing";
            case "fire_resistance" -> "tooltip.mio_icif.armor.feature_fire_resistance";
            case "speed_boost" -> "tooltip.mio_icif.armor.feature_speed_boost";
            case "jump_boost" -> "tooltip.mio_icif.armor.feature_jump_boost";
            case "high_jump" -> "tooltip.mio_icif.armor.feature_high_jump";
            case "flight" -> "tooltip.mio_icif.armor.feature_flight";
            case "jetpack_mode" -> "tooltip.mio_icif.armor.feature_jetpack_mode";
            case "auto_food" -> "tooltip.mio_icif.armor.feature_auto_food";
            case "healing" -> "tooltip.mio_icif.armor.feature_healing";
            case "knockback_resistance" -> "tooltip.mio_icif.armor.feature_knockback_resistance";
            case "oxygen_restore" -> "tooltip.mio_icif.armor.feature_oxygen_restore";
            default -> "tooltip.mio_icif.armor.feature_unknown";
        };
    }
}