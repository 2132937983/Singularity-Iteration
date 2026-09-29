package com.miophas.singularity_iteration.common.menu.tool;

import com.miophas.singularity_iteration.common.menu.base.mio_icif_base_menu;
import com.miophas.singularity_iteration.core.runtime.energy.grid.EnergyNetGlobal;
import com.miophas.singularity_iteration.core.api.energy.grid.NodeStats;
import com.miophas.singularity_iteration.core.runtime.energy.MeterReadings;
import com.miophas.singularity_iteration.common.Singularity_Iteration;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.Level;

@SuppressWarnings("null")
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID)
public class mio_icif_meter_menu extends mio_icif_base_menu {

    private final MeterReadings data = new MeterReadings();
    private final Level level;
    private final BlockPos targetPos;
    private final BlockEntity targetOwner;
    private long lastSampleTick = Long.MIN_VALUE;
    private MeterMode mode = MeterMode.EnergyIn;

    private double resultAvg = 0;
    private double resultMin = 0;
    private double resultMax = 0;
    private int resultCount = 0;

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

    public mio_icif_meter_menu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, extraData.readBlockPos(), extraData.readVarInt());
    }

    public mio_icif_meter_menu(int containerId, Inventory playerInventory, BlockPos targetPos, int modeOrdinal) {
        super(mio_icif_tool_menus.EU_METER_MENU.get(), containerId, 0, 0);
        this.level = playerInventory.player.level();
        this.targetPos = targetPos;

        if (modeOrdinal >= 0 && modeOrdinal < MeterMode.values().length) {
            this.mode = MeterMode.values()[modeOrdinal];
        }

        this.targetOwner = level.isClientSide ? null : level.getBlockEntity(targetPos);
        data.update(mode.ordinal(), 0, 0, 0, 0);
        addDataSlots(this.data);
        addPlayerInventorySlots(playerInventory);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void sampleOpenMeters(ServerTickEvent.Post event) {
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            if (player.containerMenu instanceof mio_icif_meter_menu meter) {
                meter.sampleCompletedTick(player);
            }
        }
    }

    private void sampleCompletedTick(ServerPlayer player) {
        long tick = player.server.getTickCount();
        if (tick == lastSampleTick) return;
        lastSampleTick = tick;
        if (!stillValid(player)) { player.closeContainer(); return; }
        NodeStats stats = EnergyNetGlobal.getCurrentTickNodeStats(level, targetPos);
        // IC2 closes an instrument whose energy node no longer exists.
        if (stats == null) { player.closeContainer(); return; }
        double result = switch (mode) {
            case EnergyIn -> stats.getEnergyIn();
            case EnergyOut -> stats.getEnergyOut();
            case EnergyGain -> stats.getEnergyIn() - stats.getEnergyOut();
            // IC2 EnergyCalculatorLeg exposes the packet's tier in this mode.
            // Keep the core NodeStats packet magnitude API unchanged.
            case Voltage -> EnergyNetGlobal.getTierFromPower(stats.getVoltage());
        };

        if (resultCount == 0) {
            resultAvg = resultMin = resultMax = result;
        } else {
            if (result < resultMin) resultMin = result;
            if (result > resultMax) resultMax = result;
            resultAvg += (result - resultAvg) / ((double) resultCount + 1);
        }
        if (resultCount < Integer.MAX_VALUE) resultCount++;
        data.update(mode.ordinal(), resultAvg, resultMin, resultMax, resultCount);
        // Publish AFTER producing this tick's sample, not one broadcast later.
        super.broadcastChanges();
    }

    public MeterMode getMode() {
        int ordinal = data.mode();
        if (ordinal >= 0 && ordinal < MeterMode.values().length) {
            return MeterMode.values()[ordinal];
        }
        return MeterMode.EnergyIn;
    }

    public double getResultAvg() {
        return data.average();
    }

    public double getResultMin() {
        return data.minimum();
    }

    public double getResultMax() {
        return data.maximum();
    }

    public int getResultCount() {
        return data.samples();
    }

    public boolean isVoltageMode() {
        return getMode() == MeterMode.Voltage;
    }

    protected void addPlayerInventorySlots(Inventory playerInventory) {
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 136 + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 194));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        if (level.isClientSide) return true;
        return player.level() == level && targetOwner != null && !targetOwner.isRemoved()
            && level.getChunkSource().hasChunk(targetPos.getX() >> 4, targetPos.getZ() >> 4)
            && level.getBlockEntity(targetPos) == targetOwner;
    }

    @Override
    protected boolean isBattery(net.minecraft.world.item.ItemStack stack) {
        return false;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (level.isClientSide || !stillValid(player)) return false;
        if (id >= 0 && id < MeterMode.values().length) {
            this.mode = MeterMode.values()[id];
            resetReadings();
            return true;
        }
        if (id == 100) {
            resetReadings();
            return true;
        }
        return false;
    }

    private void resetReadings() {
        resultCount = 0;
        resultAvg = resultMin = resultMax = 0;
        lastSampleTick = level.getServer().getTickCount();
        data.update(mode.ordinal(), 0, 0, 0, 0);
        super.broadcastChanges();
    }

    public BlockPos getTargetPos() {
        return targetPos;
    }
}
