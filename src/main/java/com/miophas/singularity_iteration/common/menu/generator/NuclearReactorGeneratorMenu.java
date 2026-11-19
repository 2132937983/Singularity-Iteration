package com.miophas.singularity_iteration.common.menu.generator;

import com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_nuclear_reactor_generator;
import com.miophas.singularity_iteration.common.registry.mio_icif_menus;
import com.miophas.singularity_iteration.common.item.reactor.mio_icif_reactor;
import com.miophas.singularity_iteration.common.menu.base.mio_icif_generator_menu;
import com.miophas.singularity_iteration.common.item.reactor.ReactorConfigCardItem;
import com.miophas.singularity_iteration.common.reactor.ReactorLayoutGuide;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

public class NuclearReactorGeneratorMenu extends mio_icif_generator_menu implements ReactorGuideMenu {
    public static final int REACTOR_SLOT_START = 0;
    public static final int MAX_REACTOR_SLOT_COUNT = 54;
    public static final int TOTAL_SLOT_COUNT = 54;

    // 0.1.11.21: the 300 x 284 control-room screen
    public static final int REACTOR_SLOTS_X = 14;
    public static final int REACTOR_SLOTS_Y = 20;
    private static final int REACTOR_SLOTS_ROWS = 6;
    private static final int SLOT_SIZE = 18;

    private static final int PLAYER_INV_START_X = 69;

    public NuclearReactorGeneratorMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, null, null);
    }

    // 0.1.11.76: the reactor config card (ReactorLayoutGuide), left of the player inventory, after all other slots
    public static final int CARD_SLOT_X = 27, CARD_SLOT_Y = 218;
    private final SimpleContainer cardSlot = new SimpleContainer(1);
    private final int cardSlotIndex;
    private final ReactorLayoutGuide.Data guide;

    public NuclearReactorGeneratorMenu(int containerId, Inventory playerInventory,
                                        @Nullable mio_icif_nuclear_reactor_generator blockEntity,
                                        @Nullable IItemHandler itemHandler,
                                        @Nullable ContainerData data) {
        super(mio_icif_menus.NUCLEAR_REACTOR_GENERATOR_MENU_TYPE.get(), containerId, TOTAL_SLOT_COUNT,
              playerInventory, itemHandler, data, mio_icif_nuclear_reactor_generator.CONTROL_DATA, blockEntity);
        this.cardSlotIndex = this.slots.size();
        this.addSlot(new ReactorLayoutGuide.CardSlot(cardSlot, CARD_SLOT_X, CARD_SLOT_Y));
        this.guide = new ReactorLayoutGuide.Data(blockEntity);
        this.addDataSlots(guide);
    }

    @Override public ReactorLayoutGuide.Data guideData() { return guide; }
    @Override public int cardSlotIndex() { return cardSlotIndex; }

    /** 0.1.11.76: the card goes back to the player (or drops at the player) when the screen closes. */
    @Override
    public void removed(Player player) {
        super.removed(player);
        if (player instanceof net.minecraft.server.level.ServerPlayer) clearContainer(player, cardSlot);
    }

    /** 0.1.11.76: Shift-click moves a config card between the player inventory and the card slot. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        ItemStack stack = slot.getItem();
        boolean fromCard = index == cardSlotIndex;
        if (stack.isEmpty() || !(fromCard || index >= TOTAL_SLOT_COUNT && ReactorConfigCardItem.isCard(stack))) return super.quickMoveStack(player, index);
        ItemStack before = stack.copy();
        boolean moved = fromCard ? moveItemStackTo(stack, TOTAL_SLOT_COUNT, cardSlotIndex, true) : moveItemStackTo(stack, cardSlotIndex, cardSlotIndex + 1, false);
        if (!moved) return ItemStack.EMPTY;
        slot.setByPlayer(stack.isEmpty() ? ItemStack.EMPTY : stack);
        return before;
    }

    @Override
    protected int getPlayerInventoryY() { return 200; }

    @Override
    protected int getPlayerHotbarY() { return 258; }

    @Override
    protected void addPlayerInventory(Inventory playerInventory, int startY, int hotbarY) {
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, PLAYER_INV_START_X + j * 18, startY + i * 18));
            }
        }
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, PLAYER_INV_START_X + i * 18, hotbarY));
        }
    }

    @Override
    protected void addMachineSlots() {
        for (int row = 0; row < REACTOR_SLOTS_ROWS; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = row * 9 + col;
                int slotX = REACTOR_SLOTS_X + col * SLOT_SIZE;
                int slotY = REACTOR_SLOTS_Y + row * SLOT_SIZE;
                final int slotCol = col;
                this.addSlot(new SlotItemHandler(itemHandler, slotIndex, slotX, slotY) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        if (slotCol >= getAvailableColumns()) return false;
                        return stack.getItem() instanceof mio_icif_reactor;
                    }
                    @Override
                    public int getMaxStackSize() { return 1; }
                    @Override
                    public int getMaxStackSize(ItemStack stack) { return 1; }
                });
            }
        }
    }

    public int getEnergy() { return data.get(0); }
    public int getMaxEnergy() { return data.get(1); }
    public int getHeat() { return data.get(2); }
    public int getMaxHeat() { return data.get(3); }
    public int getOutputPower() { return data.get(4); }
    public int getAvailableColumns() { return data.get(5); }
    public int getFeedRate() { return Math.max(1, data.get(6)); }
    // 0.1.11.4: data 7..10 (the removed liquid cooling) stay in the layout and are always 0
    /** 0.1.10.39: why the reactor makes nothing ({@code mio_icif_nuclear_reactor_generator.STATUS_*}). */
    public int getStatus() { return data.get(11); }
    /** 0.1.11.21: the control-room values (see the reactor's container data). */
    public int value(int i) { return data.get(i); }

    /** Feed-rate buttons: 0 = slower, 1 = faster, 2 = reset to 1x (shift = by 4). */
    @Override
    public boolean clickMenuButton(net.minecraft.world.entity.player.Player player, int id) {
        if (!(blockEntity instanceof mio_icif_nuclear_reactor_generator gen)) return false;
        if (ReactorLayoutGuide.isButton(id)) return ReactorLayoutGuide.click(id, player, gen, cardSlot, guide);   // 0.1.11.76
        int rate = gen.getFeedRate();
        switch (id) {
            case 10 -> gen.scram();                                            // 0.1.11.21: SCRAM, reset, setpoint
            case 11 -> gen.resetTrip();
            case 12 -> gen.setScramSetpoint(gen.getScramSetpoint() - 5);
            case 13 -> gen.setScramSetpoint(gen.getScramSetpoint() + 5);
            case 0 -> gen.setFeedRate(rate - 1);
            case 1 -> gen.setFeedRate(rate + 1);
            case 2 -> gen.setFeedRate(1);
            case 3 -> gen.setFeedRate(rate - 4);
            case 4 -> gen.setFeedRate(rate + 4);
            default -> { return false; }
        }
        if (gen.getLevel() != null) gen.getLevel().playSound(null, gen.getBlockPos(),
            com.miophas.singularity_iteration.common.registry.mio_icif_sounds.UI_CLICK.get(),
            net.minecraft.sounds.SoundSource.BLOCKS, 0.6F, 0.8F + gen.getFeedRate() / 20F);
        return true;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (blockEntity instanceof mio_icif_nuclear_reactor_generator gen) {
            setSyncData(0, (int) gen.getEnergyStorage().getAmount());
            setSyncData(1, (int) gen.getEnergyStorage().getCapacity());
            setSyncData(2, (int) gen.getHeatStorage().getHeatStored());
            setSyncData(3, (int) gen.getHeatStorage().getMaxHeatStored());
            setSyncData(4, gen.getCurrentOutput());
            setSyncData(5, gen.getAvailableColumns());
            setSyncData(6, gen.getFeedRate());
        }
    }
}