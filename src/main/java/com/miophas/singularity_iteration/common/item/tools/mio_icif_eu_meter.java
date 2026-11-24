package com.miophas.singularity_iteration.common.item.tools;

import com.miophas.singularity_iteration.common.menu.tool.mio_icif_meter_menu_provider;
import com.miophas.singularity_iteration.core.runtime.energy.grid.EnergyNetGlobal;
import com.miophas.singularity_iteration.core.api.energy.grid.IEnergyTile;
import com.miophas.singularity_iteration.core.api.item.IEUMeterItem;
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
public class mio_icif_eu_meter extends Item implements IEUMeterItem, com.miophas.singularity_iteration.core.api.tool.IToolModeProvider {

    /**
     * 0.1.11.200 (T5c, decision D7): the multimeter's probe. The EU meter, the wind meter and the geomagnetic detector were
     * three tools that each read one number; the meter now carries all three probes (G switches). Readings need no energy,
     * as the EU meter's never did.
     */
    public enum Probe { ENERGY, WIND, GEOMAGNETIC }

    private static final String TAG_PROBE = "MeterProbe";

    public static Probe getProbe(ItemStack stack) {
        var data = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        int i = data == null ? 0 : data.copyTag().getInt(TAG_PROBE);
        return Probe.values()[Math.max(0, Math.min(Probe.values().length - 1, i))];
    }

    private static Component probeName(Probe probe) {
        return Component.translatable("item.mio_icif.item_tool_meter.probe." + probe.name().toLowerCase(java.util.Locale.ROOT));
    }

    @Override
    public void toggleActive(ItemStack stack, Player player) {
        Probe next = Probe.values()[(getProbe(stack).ordinal() + 1) % Probe.values().length];
        net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA, stack,
            tag -> tag.putInt(TAG_PROBE, next.ordinal()));
        player.displayClientMessage(Component.translatable("item.mio_icif.item_tool_meter.probe_switched", probeName(next)), true);
    }

    @Override
    public List<Component> toolModes(ItemStack stack) {
        return java.util.Arrays.stream(Probe.values()).map(mio_icif_eu_meter::probeName).toList();
    }

    @Override
    public int toolModeIndex(ItemStack stack) {
        return getProbe(stack).ordinal();
    }

    private static final String TAG_MODE = "meterMode";

    public mio_icif_eu_meter(Properties properties) {
        super(properties.stacksTo(1));
    }

    /**
     * 方块交互之前触发（对应 IC2 原版 {@code ItemToolMeter.onItemUseFirst}）。
     * <p>
     * 必须重写 {@code onItemUseFirst} 而不是 {@code useOn}：机器、电线、变压器等能量方块
     * 都会在 {@code useItemOn}/{@code useWithoutItem} 中打开自己的界面并消费掉交互，
     * 导致物品的 {@code useOn} 永远不会被调用。{@code onItemUseFirst} 位于方块交互之前，
     * 能保证电表始终优先测量被点击的能量节点。
     */
    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        // 0.1.11.200 (T5c): the wind and geomagnetic probes read the clicked block (a wind generator) or the place
        Probe probe = getProbe(stack);
        if (probe != Probe.ENERGY) {
            if (!level.isClientSide) {
                if (probe == Probe.WIND) {
                    if (level.getBlockEntity(context.getClickedPos()) instanceof com.miophas.singularity_iteration.common.blockentity.kuentity.kugenerator.mio_icif_Wind_Kinetic_Generator generator) {
                        MeterReadings.windOf(player, generator);
                    } else {
                        MeterReadings.windHere(player);
                    }
                } else {
                    MeterReadings.geomagnetic(player, level, context.getClickedPos());
                }
            }
            return InteractionResult.SUCCESS;
        }

        // 潜行右键：循环切换测量模式，不打开界面
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                cycleMode(stack, player);
            }
            return InteractionResult.SUCCESS;
        }

        BlockPos pos = context.getClickedPos();
        IEnergyTile energyTile = resolveEnergyTile(level, pos);
        var targetBe = level.getBlockEntity(pos);
        boolean networkTarget = com.miophas.singularity_iteration.common.blockentity.wiring.terminal.NetworkWalker.isConductor(targetBe)
            || com.miophas.singularity_iteration.common.blockentity.wiring.terminal.NetworkWalker.isTransformer(targetBe)
            || targetBe instanceof com.miophas.singularity_iteration.common.blockentity.wiring.terminal.EnergyTerminalBlockEntity;

        if (energyTile == null && !networkTarget) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("item.mio_icif.item_tool_meter.not_energy_tile")
                    .withStyle(ChatFormatting.RED), true);   // 0.1.11.200 (U-20)
            }
            // 与 IC2 一致：无论是否为能量节点都消费交互，避免误触方块自身界面
            return InteractionResult.SUCCESS;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            openMeterGui(serverPlayer, pos, getMode(stack).ordinal());
        }

        return InteractionResult.SUCCESS;
    }

    /**
     * 从前缀电网或方块实体两侧解析能量节点。
     * 客户端无法访问服务端电网（{@link EnergyNetGlobal#getTile} 返回 null），
     * 因此回退到方块实体接口判断；即使两侧结果不一致也只影响界面由服务端打开，不影响同步。
     */
    private static IEnergyTile resolveEnergyTile(Level level, BlockPos pos) {
        IEnergyTile energyTile = EnergyNetGlobal.getTile(level, pos);
        if (energyTile == null && level.getBlockEntity(pos) instanceof IEnergyTile blockEntityTile) {
            energyTile = blockEntityTile;
        }
        return energyTile;
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

        switch (getProbe(stack)) {
            case WIND -> MeterReadings.windHere(player);
            case GEOMAGNETIC -> MeterReadings.geomagnetic(player, level, player.blockPosition());
            case ENERGY -> player.displayClientMessage(Component.translatable("item.mio_icif.item_tool_meter.help")
                .withStyle(ChatFormatting.GRAY), true);   // 0.1.11.200 (U-20)
        }

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
        player.displayClientMessage(Component.translatable("item.mio_icif.item_tool_meter.mode_switched", next.getDisplayName())
            .withStyle(ChatFormatting.YELLOW), true);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        MeterMode mode = getMode(stack);
        tooltipComponents.add(Component.translatable("item.mio_icif.item_tool_meter.tooltip_probe", probeName(getProbe(stack)))
            .withStyle(ChatFormatting.AQUA));   // 0.1.11.200 (T5c)
        tooltipComponents.add(Component.translatable("item.mio_icif.item_tool_meter.tooltip_mode", mode.getDisplayName())
            .withStyle(ChatFormatting.YELLOW));
        tooltipComponents.add(Component.translatable("item.mio_icif.item_tool_meter.tooltip_usage")
            .withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("item.mio_icif.item_tool_meter.tooltip_shift")
            .withStyle(ChatFormatting.DARK_GRAY));
        tooltipComponents.add(Component.translatable("item.mio_icif.item_tool_meter.tooltip_measurement")
            .withStyle(ChatFormatting.GRAY));
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
