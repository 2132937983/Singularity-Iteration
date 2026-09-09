package com.singularity_iteration.mio_icif.Items.Tools;

import com.singularity_iteration.mio_icif.Menu.Tool.mio_icif_meter_menu_provider;
import com.singularity_iteration.mio_icif.energy.grid.EnergyNetGlobal;
import com.singularity_iteration.mio_icif.energy.grid.IEnergyTile;
import com.singularity_iteration.mio_icif.api.item.IEUMeterItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

@SuppressWarnings("null")
public class mio_icif_eu_meter extends Item implements IEUMeterItem {

    private static final String TAG_MODE = "meterMode";

    public mio_icif_eu_meter(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        if (player == null) return InteractionResult.PASS;

        if (player.isShiftKeyDown()) {
            cycleMode(stack, player);
            return InteractionResult.SUCCESS;
        }

        IEnergyTile energyTile = EnergyNetGlobal.getTile(level, pos);
        if (energyTile == null) {
            if (level.getBlockEntity(pos) instanceof IEnergyTile ieTile) {
                energyTile = ieTile;
            }
        }

        if (energyTile == null) {
            player.sendSystemMessage(Component.translatable("item.mio_icif.item_tool_meter.not_energy_tile")
                .withStyle(ChatFormatting.RED));
            return InteractionResult.SUCCESS;
        }

        int modeOrdinal = getMode(stack).ordinal();
        openMeterGui((ServerPlayer) player, pos, modeOrdinal);

        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide()) {
            return InteractionResultHolder.success(stack);
        }

        if (player.isShiftKeyDown()) {
            cycleMode(stack, player);
            return InteractionResultHolder.success(stack);
        }

        player.sendSystemMessage(Component.translatable("item.mio_icif.item_tool_meter.help")
            .withStyle(ChatFormatting.GRAY));

        return InteractionResultHolder.success(stack);
    }

    private void openMeterGui(ServerPlayer player, BlockPos targetPos, int modeOrdinal) {
        player.openMenu(new mio_icif_meter_menu_provider(targetPos, modeOrdinal), buf -> {
            buf.writeBlockPos(targetPos);
            buf.writeVarInt(modeOrdinal);
        });
    }

    private MeterMode getMode(ItemStack stack) {
        CompoundTag tag = getTag(stack);
        if (tag != null && tag.contains(TAG_MODE)) {
            try {
                return MeterMode.valueOf(tag.getString(TAG_MODE));
            } catch (IllegalArgumentException e) {
                return MeterMode.EnergyIn;
            }
        }
        return MeterMode.EnergyIn;
    }

    private void cycleMode(ItemStack stack, Player player) {
        MeterMode current = getMode(stack);
        MeterMode next = current.next();
        CompoundTag tag = getOrCreateTag(stack);
        tag.putString(TAG_MODE, next.name());
        saveTag(stack, tag);
        player.sendSystemMessage(Component.translatable("item.mio_icif.item_tool_meter.mode_switched", next.getDisplayName())
            .withStyle(ChatFormatting.YELLOW));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        MeterMode mode = getMode(stack);
        tooltipComponents.add(Component.translatable("item.mio_icif.item_tool_meter.tooltip_mode", mode.getDisplayName())
            .withStyle(ChatFormatting.YELLOW));
        tooltipComponents.add(Component.translatable("item.mio_icif.item_tool_meter.tooltip_usage")
            .withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("item.mio_icif.item_tool_meter.tooltip_shift")
            .withStyle(ChatFormatting.DARK_GRAY));
    }

    private CompoundTag getOrCreateTag(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData != null ? customData.copyTag() : new CompoundTag();
    }

    private CompoundTag getTag(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData != null ? customData.copyTag() : null;
    }

    private void saveTag(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public enum MeterMode {
        EnergyIn,
        EnergyOut,
        EnergyGain,
        Voltage;

        public MeterMode next() {
            MeterMode[] values = values();
            return values[(ordinal() + 1) % values.length];
        }

        public Component getDisplayName() {
            return Component.translatable("item.mio_icif.item_tool_meter.mode." + name());
        }
    }
}

