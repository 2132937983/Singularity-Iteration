package com.singularity_iteration.mio_icif.Menu.Storage;

import com.singularity_iteration.mio_icif.Menu.Base.mio_icif_base_menu;
import com.singularity_iteration.mio_icif.Menu.mio_icif_menus;
import com.singularity_iteration.mio_icif.Blocks.entity.transformer.mio_icif_transformer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings({"null"})
public class TransformerMenu extends mio_icif_base_menu {

    private static final int DATA_SLOT_COUNT = 10;
    private final ContainerLevelAccess access;
    private final ContainerData data;
    final mio_icif_transformer blockEntity;
    private final int[] syncedData = new int[DATA_SLOT_COUNT];

    public TransformerMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null);
    }

    public TransformerMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_transformer blockEntity) {
        super(mio_icif_menus.TRANSFORMER_MENU_TYPE.get(), containerId, 0, 0);
        this.access = ContainerLevelAccess.create(playerInventory.player.level(),
            blockEntity != null ? blockEntity.getBlockPos() : playerInventory.player.blockPosition());
        this.blockEntity = blockEntity;

        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                if (index < 0 || index >= DATA_SLOT_COUNT) {
                    return 0;
                }

                mio_icif_transformer be = TransformerMenu.this.blockEntity;
                if (be == null) {
                    return syncedData[index];
                }

                int value = switch (index) {
                    case 0 -> be.getMode().ordinal();
                    case 1 -> be.hasRedstoneSignal() ? 1 : 0;
                    case 2 -> (int) (be.getInternalEnergy() & 0xFFFFFFFFL);
                    case 3 -> (int) ((be.getInternalEnergy() >> 32) & 0xFFFFFFFFL);
                    case 4 -> (int) (be.getBufferCapacity() & 0xFFFFFFFFL);
                    case 5 -> (int) ((be.getBufferCapacity() >> 32) & 0xFFFFFFFFL);
                    case 6 -> (int) (be.getLowSideLimit() & 0xFFFFFFFFL);
                    case 7 -> (int) ((be.getLowSideLimit() >> 32) & 0xFFFFFFFFL);
                    case 8 -> (int) (be.getHighSideLimit() & 0xFFFFFFFFL);
                    case 9 -> (int) ((be.getHighSideLimit() >> 32) & 0xFFFFFFFFL);
                    default -> 0;
                };
                syncedData[index] = value;
                return value;
            }

            @Override
            public void set(int index, int value) {
                if (index < 0 || index >= DATA_SLOT_COUNT) {
                    return;
                }
                syncedData[index] = value;
            }

            @Override
            public int getCount() {
                return DATA_SLOT_COUNT;
            }
        };

        addDataSlots(this.data);

        addPlayerInventorySlots(playerInventory);
    }

    protected void addPlayerInventorySlots(Inventory playerInventory) {
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 137 + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 195));
        }
    }

    public int getModeOrdinal() {
        return this.data.get(0);
    }

    public boolean hasRedstoneSignal() {
        return this.data.get(1) != 0;
    }

    public long getInternalEnergy() {
        long low = this.data.get(2) & 0xFFFFFFFFL;
        long high = this.data.get(3) & 0xFFFFFFFFL;
        return (high << 32) | low;
    }

    public long getBufferCapacity() {
        long low = this.data.get(4) & 0xFFFFFFFFL;
        long high = this.data.get(5) & 0xFFFFFFFFL;
        return (high << 32) | low;
    }

    public long getLowSideLimit() {
        long low = this.data.get(6) & 0xFFFFFFFFL;
        long high = this.data.get(7) & 0xFFFFFFFFL;
        return (high << 32) | low;
    }

    public long getHighSideLimit() {
        long low = this.data.get(8) & 0xFFFFFFFFL;
        long high = this.data.get(9) & 0xFFFFFFFFL;
        return (high << 32) | low;
    }

    public String getModeName() {
        mio_icif_transformer.TransformerMode[] modes = mio_icif_transformer.TransformerMode.values();
        int ordinal = getModeOrdinal();
        if (ordinal >= 0 && ordinal < modes.length) {
            return modes[ordinal].name().toLowerCase();
        }
        return "step_down";
    }

    public mio_icif_transformer.TransformerMode getEffectiveMode() {
        mio_icif_transformer.TransformerMode[] modes = mio_icif_transformer.TransformerMode.values();
        int ordinal = getModeOrdinal();
        if (ordinal < 0 || ordinal >= modes.length) {
            ordinal = 0;
        }
        mio_icif_transformer.TransformerMode currentMode = modes[ordinal];
        if (currentMode == mio_icif_transformer.TransformerMode.REDSTONE_CONTROL) {
            return hasRedstoneSignal() ? mio_icif_transformer.TransformerMode.STEP_UP : mio_icif_transformer.TransformerMode.STEP_DOWN;
        }
        return currentMode;
    }

    public Component getModeDisplayName() {
        return Component.translatable("gui.mio_icif.transformer.mode." + getModeName());
    }

    @Override
    public boolean stillValid(Player player) {
        return this.access.evaluate((level, pos) -> {
            if (level.getBlockEntity(pos) instanceof mio_icif_transformer) {
                return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
            }
            return false;
        }, true);
    }

    @Override
    protected boolean isBattery(net.minecraft.world.item.ItemStack stack) {
        return false;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (blockEntity == null) {
            return false;
        }

        switch (id) {
            case 0 -> blockEntity.setMode(mio_icif_transformer.TransformerMode.REDSTONE_CONTROL);
            case 1 -> blockEntity.setMode(mio_icif_transformer.TransformerMode.STEP_DOWN);
            case 2 -> blockEntity.setMode(mio_icif_transformer.TransformerMode.STEP_UP);
            default -> {
                return false;
            }
        }

        if (blockEntity.getLevel() != null) {
            blockEntity.getLevel().sendBlockUpdated(blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity.getBlockState(), 3);
        }
        return true;
    }
}

