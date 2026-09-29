package com.miophas.singularity_iteration.common.menu.storage;

import com.miophas.singularity_iteration.common.menu.base.mio_icif_base_menu;
import com.miophas.singularity_iteration.common.registry.mio_icif_menus;
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
    @Nullable
    final TransformerView view;
    private final int[] syncedData = new int[DATA_SLOT_COUNT];

    public TransformerMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null);
    }

    public TransformerMenu(int containerId, Inventory playerInventory, @Nullable TransformerView view) {
        super(mio_icif_menus.TRANSFORMER_MENU_TYPE.get(), containerId, 0, 0);
        this.view = view;
        this.access = view == null
            ? ContainerLevelAccess.create(playerInventory.player.level(), playerInventory.player.blockPosition())
            : ContainerLevelAccess.create(view.viewLevel(), view.viewPos());

        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                if (index < 0 || index >= DATA_SLOT_COUNT) {
                    return 0;
                }
                TransformerView be = TransformerMenu.this.view;
                if (be == null) {
                    return syncedData[index];
                }

                int value = switch (index) {
                    case 0 -> be.modeOrdinal();
                    case 1 -> be.hasRedstoneSignal() ? 1 : 0;
                    case 2 -> (int) (be.storedEnergy() & 0xFFFFFFFFL);
                    case 3 -> (int) ((be.storedEnergy() >> 32) & 0xFFFFFFFFL);
                    case 4 -> (int) (be.bufferCapacity() & 0xFFFFFFFFL);
                    case 5 -> (int) ((be.bufferCapacity() >> 32) & 0xFFFFFFFFL);
                    case 6 -> (int) (be.lowSideLimit() & 0xFFFFFFFFL);
                    case 7 -> (int) ((be.lowSideLimit() >> 32) & 0xFFFFFFFFL);
                    case 8 -> (int) (be.highSideLimit() & 0xFFFFFFFFL);
                    case 9 -> (int) ((be.highSideLimit() >> 32) & 0xFFFFFFFFL);
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

    /** 模式序号：0 升压、1 降压、2 红石控制（与遗留枚举 ordinal 一致）。 */
    public String getModeName() {
        return switch (getModeOrdinal()) {
            case 0 -> "step_up";
            case 2 -> "redstone_control";
            default -> "step_down";
        };
    }

    /** 红石控制模式下按当前红石输入折算成实际模式。 */
    public String getEffectiveMode() {
        if (getModeOrdinal() == 2) {
            return hasRedstoneSignal() ? "step_up" : "step_down";
        }
        return getModeName();
    }

    public Component getModeDisplayName() {
        return Component.translatable("gui.mio_icif.transformer.mode." + getModeName());
    }

    @Override
    public boolean stillValid(Player player) {
        return this.access.evaluate((level, pos) -> {
            if (level.getBlockEntity(pos) instanceof com.miophas.singularity_iteration.common.menu.storage.TransformerView
                || level.getBlockEntity(pos) instanceof com.miophas.singularity_iteration.core.platform.neoforge.energy.IndependentTransformerBlockEntity) {
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
        // 屏幕按钮按位置编号：0 红石控制、1 降压、2 升压；模式 ordinal 与之相反
        // （0 升压、1 降压、2 红石控制），此处做一次显式换算，不共用同一序号。
        int[] buttonToOrdinal = {2, 1, 0};
        if (id < 0 || id >= buttonToOrdinal.length || this.view == null) {
            return false;
        }
        int ordinal = buttonToOrdinal[id];
        if (this.view.modeOrdinal() == ordinal) {
            return false;
        }
        this.view.setMode(ordinal);
        return true;
    }
}