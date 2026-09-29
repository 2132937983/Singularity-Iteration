package com.miophas.singularity_iteration.common.menu.producer;

import com.miophas.singularity_iteration.common.registry.mio_icif_menus;
import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_pattern_storage;
import com.miophas.singularity_iteration.common.menu.base.mio_icif_machine_menu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;
import com.miophas.singularity_iteration.core.runtime.processing.PatternMenuData;
import com.miophas.singularity_iteration.core.runtime.processing.MachineMenuAccess;
import net.minecraft.world.entity.player.Player;

/**
 * 模式存储机的容器菜单独?
 */
@SuppressWarnings({"null"}) public class PatternStorageMenu extends mio_icif_machine_menu {

    public static final int MEMORY_SLOT = 0;
    public static final int SLOT_COUNT = 2;
    private net.minecraft.world.SimpleContainer preview;

    private static final int MEMORY_X = 80;
    private static final int MEMORY_Y = 35;

    private static final int DATA_COUNT = PatternMenuData.COUNT;

    public PatternStorageMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, null);
    }

    public PatternStorageMenu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler) {
        this(containerId, playerInventory, itemHandler, null);
    }

    public PatternStorageMenu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler, @Nullable ContainerData data) {
        super(mio_icif_menus.PATTERN_STORAGE_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory, itemHandler, data, DATA_COUNT);
    }

    public PatternStorageMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_pattern_storage blockEntity) {
        super(mio_icif_menus.PATTERN_STORAGE_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory,
              blockEntity != null ? blockEntity.getItemHandler() : null,
              blockEntity != null ? blockEntity.getContainerData() : null, DATA_COUNT, blockEntity);
    }

    @Override
    public mio_icif_pattern_storage getBlockEntity() {
        return this.blockEntity instanceof mio_icif_pattern_storage be ? be : null;
    }

    public int getCurrentIndex() {
        return this.data.get(PatternMenuData.INDEX);
    }

    public int getMaxIndex() {
        return this.data.get(PatternMenuData.SIZE);
    }

    public ItemStack getCurrentPattern() {
        return preview.getItem(0).copy();
    }

    public double getCurrentUuCost() {
        return Double.longBitsToDouble(PatternMenuData.read(data,PatternMenuData.UU,4));
    }

    public long getCurrentEuCost() {
        return PatternMenuData.read(data,PatternMenuData.EU,4);
    }

    public void setSyncData(int energy, int maxEnergy) {
        PatternMenuData.write(data,PatternMenuData.ENERGY,2,energy);
        PatternMenuData.write(data,PatternMenuData.CAPACITY,2,maxEnergy);
    }

    public int getEnergy() {
        return (int)PatternMenuData.read(data,PatternMenuData.ENERGY,2);
    }

    public int getMaxEnergy() {
        return (int)PatternMenuData.read(data,PatternMenuData.CAPACITY,2);
    }

    @Override public boolean stillValid(Player player){return player.level().isClientSide()||MachineMenuAccess.valid(player,getBlockEntity());}
    @Override public boolean clickMenuButton(Player player,int button){
        var storage=getBlockEntity();if(!MachineMenuAccess.action(player,this,storage))return false;
        switch(button){
            case 0:storage.previousPattern();return true;
            case 1:storage.nextPattern();return true;
            case 2:return storage.exportCurrentPattern();
            case 3:return storage.importMemoryPattern();
            default:return false;
        }
    }
    @Override public void broadcastChanges(){
        var storage=getBlockEntity();
        if(storage!=null){var item=storage.getCurrentPattern();if(!ItemStack.matches(item,preview.getItem(0)))preview.setItem(0,item);}
        super.broadcastChanges();
    }
    @Override public ItemStack quickMoveStack(Player player,int index){return index==1?ItemStack.EMPTY:super.quickMoveStack(player,index);}

    @Override
    protected void addMachineSlots() {
        this.addSlot(new SlotItemHandler(itemHandler, MEMORY_SLOT, MEMORY_X, MEMORY_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof com.miophas.singularity_iteration.common.item.resource.mio_icif_memory;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        // The vanilla slot packet carries the complete preview stack to an unbound client menu.
        // It is not part of the machine inventory and cannot be picked up or filled.
        preview=new net.minecraft.world.SimpleContainer(1);
        this.addSlot(new net.minecraft.world.inventory.Slot(preview,0,-1000,-1000){
            @Override public boolean mayPlace(ItemStack stack){return false;}
            @Override public boolean mayPickup(Player player){return false;}
        });
    }
}
