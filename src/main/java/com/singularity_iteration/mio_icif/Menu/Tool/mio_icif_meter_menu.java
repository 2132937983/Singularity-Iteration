package com.singularity_iteration.mio_icif.Menu.Tool;

import com.singularity_iteration.mio_icif.Menu.Base.mio_icif_base_menu;
import com.singularity_iteration.mio_icif.energy.grid.EnergyNetGlobal;
import com.singularity_iteration.mio_icif.energy.grid.IEnergyTile;
import com.singularity_iteration.mio_icif.energy.grid.NodeStats;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.Level;

@SuppressWarnings("null")
public class mio_icif_meter_menu extends mio_icif_base_menu {

    private static final int DATA_SLOT_COUNT = 9;

    private final ContainerData data;
    private final Level level;
    private final BlockPos targetPos;
    private final IEnergyTile targetTile;
    private MeterMode mode = MeterMode.EnergyIn;

    private double resultAvg = 0;
    private double resultMin = 0;
    private double resultMax = 0;
    private int resultCount = 0;

    private final int[] syncedData = new int[DATA_SLOT_COUNT];

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

        IEnergyTile tile = null;
        if (!level.isClientSide) {
            tile = EnergyNetGlobal.getTile(level, targetPos);
            if (tile == null) {
                if (level.getBlockEntity(targetPos) instanceof IEnergyTile energyTile) {
                    tile = energyTile;
                }
            }
        }
        this.targetTile = tile;

        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                if (index < 0 || index >= DATA_SLOT_COUNT) {
                    return 0;
                }
                return syncedData[index];
            }

            @Override
            public void set(int index, int value) {
                if (index >= 0 && index < DATA_SLOT_COUNT) {
                    syncedData[index] = value;
                }
            }

            @Override
            public int getCount() {
                return DATA_SLOT_COUNT;
            }
        };

        addDataSlots(this.data);
        addPlayerInventorySlots(playerInventory);
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();

        if (!level.isClientSide && targetTile != null) {
            NodeStats stats = EnergyNetGlobal.getNodeStats(targetTile);
            if (stats == null) {
                return;
            }

            double result = switch (mode) {
                case EnergyIn -> stats.getEnergyIn();
                case EnergyOut -> stats.getEnergyOut();
                case EnergyGain -> stats.getEnergyIn() - stats.getEnergyOut();
                case Voltage -> stats.getVoltage();
            };

            if (resultCount == 0) {
                resultAvg = resultMin = resultMax = result;
            } else {
                if (result < resultMin) resultMin = result;
                if (result > resultMax) resultMax = result;
                resultAvg = (resultAvg * resultCount + result) / (resultCount + 1);
            }
            resultCount++;

            syncedData[0] = mode.ordinal();
            packDouble(syncedData, 1, resultAvg);
            packDouble(syncedData, 3, resultMin);
            packDouble(syncedData, 5, resultMax);
            syncedData[7] = resultCount;
            syncedData[8] = mode == MeterMode.Voltage ? 1 : 0;
        }
    }

    private void packDouble(int[] arr, int startIndex, double value) {
        long bits = Double.doubleToRawLongBits(value);
        arr[startIndex] = (int) (bits & 0xFFFFFFFFL);
        arr[startIndex + 1] = (int) ((bits >> 32) & 0xFFFFFFFFL);
    }

    public MeterMode getMode() {
        int ordinal = this.data.get(0);
        if (ordinal >= 0 && ordinal < MeterMode.values().length) {
            return MeterMode.values()[ordinal];
        }
        return MeterMode.EnergyIn;
    }

    public double getResultAvg() {
        return unpackDouble(this.data, 1);
    }

    public double getResultMin() {
        return unpackDouble(this.data, 3);
    }

    public double getResultMax() {
        return unpackDouble(this.data, 5);
    }

    public int getResultCount() {
        return this.data.get(7);
    }

    public boolean isVoltageMode() {
        return this.data.get(8) != 0;
    }

    private double unpackDouble(ContainerData data, int startIndex) {
        long low = data.get(startIndex) & 0xFFFFFFFFL;
        long high = data.get(startIndex + 1) & 0xFFFFFFFFL;
        return Double.longBitsToDouble((high << 32) | low);
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
        return true;
    }

    @Override
    protected boolean isBattery(net.minecraft.world.item.ItemStack stack) {
        return false;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= 0 && id < MeterMode.values().length) {
            this.mode = MeterMode.values()[id];
            this.resultCount = 0;
            this.resultAvg = 0;
            this.resultMin = 0;
            this.resultMax = 0;
            return true;
        }
        if (id == 100) {
            this.resultCount = 0;
            this.resultAvg = 0;
            this.resultMin = 0;
            this.resultMax = 0;
            return true;
        }
        return false;
    }

    public BlockPos getTargetPos() {
        return targetPos;
    }
}

