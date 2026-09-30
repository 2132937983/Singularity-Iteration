// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.item.tools;

import com.miophas.singularity_iteration.common.menu.tool.CropAnalyzerMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Independent electric crop analyzer entry point. */
public class CropAnalyzerItem extends mio_icif_tool_elc {
    public static final long MAX_ENERGY = 100_000L;

    public CropAnalyzerItem(Properties properties) {
        super(properties, MAX_ENERGY, 0L, 128L, 10L, 2);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        if (!player.isShiftKeyDown() && context.getLevel().getBlockEntity(context.getClickedPos())
                instanceof com.miophas.singularity_iteration.core.api.crop.IPlanter crop) {
            if (!context.getLevel().isClientSide && consumeEnergy(context.getItemInHand(), energyForLevel(2))) {
                var plant = crop.getPlant();
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.mio_icif.crop.diagnostic",
                    plant == null ? net.minecraft.network.chat.Component.translatable("message.mio_icif.crop_stick_empty")
                        : net.minecraft.network.chat.Component.translatable(plant.getTranslationKey()),
                    crop.getGrowthStage(), crop.getNutrients(), crop.getWater(), crop.getWeedControl(), crop.getProgress()));
            }
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
        }
        if (!context.getLevel().isClientSide && player instanceof ServerPlayer serverPlayer) {
            open(serverPlayer, context.getItemInHand(), context.getHand());
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers() {
        return ItemAttributeModifiers.EMPTY;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) open(serverPlayer, stack, hand);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private static void open(ServerPlayer player, ItemStack stack, InteractionHand hand) {
        player.openMenu(new CropAnalyzerMenu.Provider(stack, hand), buffer -> buffer.writeEnum(hand));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean selected) {
        super.inventoryTick(stack, level, entity, slotId, selected);
    }

    public static int energyForLevel(int level) {
        return switch (level) {
            case 1 -> 90;
            case 2 -> 900;
            case 3 -> 9000;
            default -> 10;
        };
    }
}
