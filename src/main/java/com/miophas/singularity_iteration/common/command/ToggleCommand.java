package com.miophas.singularity_iteration.common.command;

import com.miophas.singularity_iteration.common.item.armor.ArmorFeatureSlots;
import com.miophas.singularity_iteration.core.api.item.ArmorFeatureInfo;
import com.miophas.singularity_iteration.core.prefab.item.ArmorFeatures;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 装备特性切换命令。
 *
 * <p>{@code /mio_icif toggle <slot> [feature]}，其中 {@code slot} 为
 * {@code head/chest/legs/feet/back}（见 {@link ArmorFeatureSlots}）。
 *
 * <p>特性列表完全来自物品声明（{@link ArmorFeatures#features(ItemStack)}），
 * 不包含任何按物品类硬编码的映射，因此附属模组的装备与特性同样可被切换与补全。
 * 开关型取反、模式型切换到下一模式。
 */
@SuppressWarnings("null")
public class ToggleCommand {

    private static final String[] SLOT_NAMES = {"head", "chest", "legs", "feet", ArmorFeatureSlots.BACK};

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mio_icif")
            .then(Commands.literal("toggle")
                .then(Commands.argument("slot", StringArgumentType.word())
                    .suggests((context, builder) ->
                        SharedSuggestionProvider.suggest(SLOT_NAMES, builder))
                    .then(Commands.argument("feature", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            String slotId = StringArgumentType.getString(context, "slot");
                            if (context.getSource().getEntity() instanceof ServerPlayer player) {
                                ItemStack stack = ArmorFeatureSlots.stackById(player, slotId);
                                if (!stack.isEmpty()) {
                                    List<ArmorFeatureInfo> features = ArmorFeatures.features(stack);
                                    return SharedSuggestionProvider.suggest(
                                        features.stream().map(ArmorFeatureInfo::featureKey).toList(), builder);
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

    private static int toggleSlot(CommandSourceStack source, String slotId) {
        if (!(source.getEntity() instanceof ServerPlayer player)) return 0;
        ItemStack stack = ArmorFeatureSlots.stackById(player, slotId);
        List<ArmorFeatureInfo> features = ArmorFeatures.features(stack);
        if (features.isEmpty()) {
            player.sendSystemMessage(Component.translatable("command.mio_icif.toggle.no_item"));
            return 0;
        }
        return toggleFeature(source, slotId, features.get(0).featureKey());
    }

    private static int toggleFeature(CommandSourceStack source, String slotId, String featureKey) {
        if (!(source.getEntity() instanceof ServerPlayer player)) return 0;
        if (featureKey == null || featureKey.isEmpty()) return 0;

        // 先按槽位标识定位；若该槽位物品未声明此特性（例如背槽物品声明 CHEST），回退到全槽位查找
        String effectiveSlot = slotId;
        ItemStack stack = ArmorFeatureSlots.stackById(player, slotId);
        if (!ArmorFeatures.isDeclared(stack, null, featureKey)) {
            effectiveSlot = ArmorFeatureSlots.findDeclaringSlotId(player, featureKey);
            if (effectiveSlot == null) {
                player.sendSystemMessage(Component.translatable("command.mio_icif.toggle.no_item"));
                return 0;
            }
            stack = ArmorFeatureSlots.stackById(player, effectiveSlot);
        }
        if (!ArmorFeatures.isDeclared(stack, null, featureKey)) {
            player.sendSystemMessage(Component.translatable("command.mio_icif.toggle.no_item"));
            return 0;
        }

        return applyFeature(player, effectiveSlot, stack, featureKey) ? 1 : 0;
    }

    private static boolean applyFeature(ServerPlayer player, String slotId, ItemStack stack, String featureKey) {
        ArmorFeatureInfo info = ArmorFeatures.find(stack, featureKey);
        if (info == null) return false;

        boolean success;
        if (info.isMode()) {
            success = ArmorFeatures.cycleMode(stack, featureKey);
            if (!success) return false;
            ArmorFeatureSlots.write(player, slotId, stack);

            ArmorFeatureInfo updated = ArmorFeatures.find(stack, featureKey);
            Component modeName = updated != null && updated.currentModeName() != null
                ? updated.currentModeName()
                : Component.translatable(info.featureNameKey() + ".default");
            player.sendSystemMessage(Component.translatable("hud.mio_icif.jetpack.switch_mode", modeName));
            return true;
        }

        boolean newState = ArmorFeatures.toggle(stack, featureKey);
        ArmorFeatureSlots.write(player, slotId, stack);
        player.sendSystemMessage(Component.translatable("command.mio_icif.toggle.feature",
            Component.translatable(info.featureNameKey()),
            newState
                ? Component.translatable("tooltip.mio_icif.armor.feature_on")
                : Component.translatable("tooltip.mio_icif.armor.feature_off")));
        return true;
    }
}
