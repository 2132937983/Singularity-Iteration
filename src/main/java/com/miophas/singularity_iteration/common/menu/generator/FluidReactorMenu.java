package com.miophas.singularity_iteration.common.menu.generator;

import com.miophas.singularity_iteration.common.menu.base.mio_icif_base_menu;
import com.miophas.singularity_iteration.common.registry.mio_icif_menus;
import com.miophas.singularity_iteration.common.registry.mio_icif_blocks;
import com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_nuclear_reactor_generator;
import com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_fluid_reactor_handler;
import com.miophas.singularity_iteration.common.item.reactor.mio_icif_reactor;
import com.miophas.singularity_iteration.common.item.cell.mio_icif_cells;
import com.miophas.singularity_iteration.common.block.environment.fluid.mio_icif_fluids;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null")
public class FluidReactorMenu extends mio_icif_base_menu {

    private static final int INPUT_TANK_INPUT_SLOT_X = 8;
    private static final int INPUT_TANK_INPUT_SLOT_Y = 25;
    private static final int INPUT_TANK_OUTPUT_SLOT_X = 8;
    private static final int INPUT_TANK_OUTPUT_SLOT_Y = 115;
    private static final int OUTPUT_TANK_INPUT_SLOT_X = 188;
    private static final int OUTPUT_TANK_INPUT_SLOT_Y = 25;
    private static final int OUTPUT_TANK_OUTPUT_SLOT_X = 188;
    private static final int OUTPUT_TANK_OUTPUT_SLOT_Y = 115;

    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final mio_icif_nuclear_reactor_generator blockEntity;

    public static final int FLUID_REACTOR_SLOT_COUNT = 54 + 4;
    public static final int PLAYER_INVENTORY_START = FLUID_REACTOR_SLOT_COUNT;

    public FluidReactorMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null);
    }

    public FluidReactorMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_nuclear_reactor_generator blockEntity) {
        super(mio_icif_menus.FLUID_REACTOR_MENU_TYPE.get(), containerId, FLUID_REACTOR_SLOT_COUNT, PLAYER_INVENTORY_START);
        this.access = ContainerLevelAccess.create(playerInventory.player.level(),
            blockEntity != null ? blockEntity.getBlockPos() : playerInventory.player.blockPosition());
        this.blockEntity = blockEntity;

        if (blockEntity != null) {
            for (int row = 0; row < 6; row++) {
                for (int col = 0; col < 9; col++) {
                    int slotIndex = row * 9 + col;
                    int x = 26 + col * 18;
                    int y = 25 + row * 18;

                    this.addSlot(new SlotItemHandler(blockEntity.getReactorItemHandler(), slotIndex, x, y) {
                        @Override
                        public boolean mayPlace(ItemStack stack) {
                            return stack.getItem() instanceof mio_icif_reactor ||
                                   stack.getItem() instanceof com.miophas.singularity_iteration.common.item.reactor.mio_icif_heat_vent ||
                                   stack.getItem() instanceof com.miophas.singularity_iteration.common.item.reactor.mio_icif_neutron_reflector ||
                                   stack.getItem() instanceof com.miophas.singularity_iteration.common.item.reactor.mio_icif_reactor_plating;
                        }
                    });
                }
            }
        } else {
            for (int row = 0; row < 6; row++) {
                for (int col = 0; col < 9; col++) {
                    int x = 26 + col * 18;
                    int y = 25 + row * 18;
                    this.addSlot(new Slot(new SimpleContainer(54), row * 9 + col, x, y));
                }
            }
        }

        if (blockEntity != null && blockEntity.getFluidHandler() != null) {
            var fluidHandler = blockEntity.getFluidHandler();

            this.addSlot(new SlotItemHandler(fluidHandler.getItemHandler(),
                mio_icif_fluid_reactor_handler.INPUT_CELL_SLOT_1,
                INPUT_TANK_INPUT_SLOT_X, INPUT_TANK_INPUT_SLOT_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return fluidHandler.getItemHandler().isItemValid(this.getSlotIndex(), stack);
                }
            });

            this.addSlot(new SlotItemHandler(fluidHandler.getItemHandler(),
                mio_icif_fluid_reactor_handler.OUTPUT_EMPTY_SLOT_1,
                INPUT_TANK_OUTPUT_SLOT_X, INPUT_TANK_OUTPUT_SLOT_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });

            this.addSlot(new SlotItemHandler(fluidHandler.getItemHandler(),
                mio_icif_fluid_reactor_handler.INPUT_CELL_SLOT_2,
                OUTPUT_TANK_INPUT_SLOT_X, OUTPUT_TANK_INPUT_SLOT_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return fluidHandler.getItemHandler().isItemValid(this.getSlotIndex(), stack);
                }
            });

            this.addSlot(new SlotItemHandler(fluidHandler.getItemHandler(),
                mio_icif_fluid_reactor_handler.OUTPUT_EMPTY_SLOT_2,
                OUTPUT_TANK_OUTPUT_SLOT_X, OUTPUT_TANK_OUTPUT_SLOT_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        } else {
            SimpleContainer dummyFluidContainer = new SimpleContainer(4);
            this.addSlot(new Slot(dummyFluidContainer, 0, INPUT_TANK_INPUT_SLOT_X, INPUT_TANK_INPUT_SLOT_Y));
            this.addSlot(new Slot(dummyFluidContainer, 1, INPUT_TANK_OUTPUT_SLOT_X, INPUT_TANK_OUTPUT_SLOT_Y));
            this.addSlot(new Slot(dummyFluidContainer, 2, OUTPUT_TANK_INPUT_SLOT_X, OUTPUT_TANK_INPUT_SLOT_Y));
            this.addSlot(new Slot(dummyFluidContainer, 3, OUTPUT_TANK_OUTPUT_SLOT_X, OUTPUT_TANK_OUTPUT_SLOT_Y));
        }

        addCustomPlayerInventory(playerInventory, 26, 161, 219);

        if (blockEntity != null) {
            this.data = blockEntity.getFluidContainerData();
        } else {
            this.data = new ContainerData() {
                private final int[] data = new int[8];
                @Override
                public int get(int index) { return data[index]; }
                @Override
                public void set(int index, int value) { data[index] = value; }
                @Override
                public int getCount() { return 8; }
            };
        }
        this.addDataSlots(data);
    }

    @Override
    protected boolean isBattery(ItemStack stack) {
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, mio_icif_blocks.NUCLEAR_REACTOR_GENERATOR.get());
    }

    private void addCustomPlayerInventory(Inventory playerInventory, int startX, int startY, int hotbarY) {
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, startX + col * 18, startY + row * 18));
            }
        }

        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, startX + i * 18, hotbarY));
        }
    }

    public int getCurrentHeat() {
        return data.get(0);
    }

    public int getMaxHeat() {
        return data.get(1);
    }

    public int getHeatProgressPixels() {
        int heat = data.get(0);
        int maxHeat = data.get(1);
        if (maxHeat <= 0) return 0;
        return (int) ((float) heat / maxHeat * 100);
    }

    public int getCurrentTemperature() {
        return data.get(2);
    }

    public int getInputFluidAmount() {
        return data.get(3);
    }

    public int getOutputFluidAmount() {
        return data.get(4);
    }

    public int getInputFluidCapacity() {
        if (blockEntity != null && blockEntity.getFluidHandler() != null) {
            return blockEntity.getFluidHandler().getInputCapacity();
        }
        return 10000;
    }

    public int getOutputFluidCapacity() {
        if (blockEntity != null && blockEntity.getFluidHandler() != null) {
            return blockEntity.getFluidHandler().getOutputCapacity();
        }
        return 10000;
    }

    /** IC2 EmitHeat：20 tick 周期内被冷媒吸收的热量，显示单位为 HU/s。 */
    public int getHeatOutput() {
        return data.get(5);
    }

    /** 输入罐冷媒下标（0 = 冷却液，1 = 水，-1 = 空）。 */
    public int getInputCoolantKind() {
        return data.get(6);
    }

    /** 输出罐热媒下标（0 = 热冷却液，1 = 热水，-1 = 空）。 */
    public int getOutputCoolantKind() {
        return data.get(7);
    }

    @Nullable
    public mio_icif_nuclear_reactor_generator getBlockEntity() {
        return blockEntity;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack itemStack1 = slot.getItem();
            itemstack = itemStack1.copy();

            if (index < FLUID_REACTOR_SLOT_COUNT) {
                if (!this.moveItemStackTo(itemStack1, PLAYER_INVENTORY_START, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                boolean moved = false;

                if (isReactorComponent(itemStack1)) {
                    if (this.moveItemStackTo(itemStack1, 0, 54, false)) {
                        moved = true;
                    }
                } else if (isFluidCell(itemStack1)) {
                    if (this.moveItemStackTo(itemStack1, 54, 55, false) ||
                        this.moveItemStackTo(itemStack1, 56, 57, false)) {
                        moved = true;
                    }
                }

                if (!moved) {
                    if (!this.moveItemStackTo(itemStack1, PLAYER_INVENTORY_START, this.slots.size(), true)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (itemStack1.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setByPlayer(itemStack1);
            }
        }

        return itemstack;
    }

    private boolean isReactorComponent(ItemStack stack) {
        return stack.getItem() instanceof mio_icif_reactor ||
               stack.getItem() instanceof com.miophas.singularity_iteration.common.item.reactor.mio_icif_heat_vent ||
               stack.getItem() instanceof com.miophas.singularity_iteration.common.item.reactor.mio_icif_neutron_reflector ||
               stack.getItem() instanceof com.miophas.singularity_iteration.common.item.reactor.mio_icif_reactor_plating;
    }

    private boolean isFluidCell(ItemStack stack) {
        return mio_icif_cells.isCellContainingFluid(stack, mio_icif_fluids.COOLANT.get());
    }
}
