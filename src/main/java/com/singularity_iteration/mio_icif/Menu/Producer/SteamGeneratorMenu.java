package com.singularity_iteration.mio_icif.Menu.Producer;

import com.singularity_iteration.mio_icif.Menu.mio_icif_menus;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_steam_generator;
import com.singularity_iteration.mio_icif.Menu.Base.mio_icif_machine_menu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings({"null"}) public class SteamGeneratorMenu extends mio_icif_machine_menu {

    public SteamGeneratorMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, null);
    }

    public SteamGeneratorMenu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler) {
        this(containerId, playerInventory, itemHandler, null);
    }

    public SteamGeneratorMenu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler, @Nullable ContainerData data) {
        super(mio_icif_menus.STEAM_GENERATOR_MENU_TYPE.get(), containerId, 0, playerInventory, itemHandler, data, 12);
    }

    public SteamGeneratorMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_steam_generator blockEntity) {
        this(containerId, playerInventory,
              blockEntity != null ? blockEntity.getItemHandler() : null,
              blockEntity != null ? blockEntity.getContainerData() : null, blockEntity);
    }

    public SteamGeneratorMenu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler,
                              @Nullable ContainerData data, @Nullable mio_icif_steam_generator blockEntity) {
        super(mio_icif_menus.STEAM_GENERATOR_MENU_TYPE.get(), containerId, 0, playerInventory,
              itemHandler, data, 12, blockEntity);
    }

    @Override
    protected void addMachineSlots() {}

    // GUI高度220，按照原版IC2逻辑调整玩家物品栏位置?
    // 背包Y = 220 - 82 = 138, 快捷栏Y = 220 - 24 = 196
    @Override
    protected int getPlayerInventoryY() { return 138; }

    @Override
    protected int getPlayerHotbarY() { return 196; }

    @Override
    protected boolean shouldAddPlayerInventory() { return false; }

    @Override
    protected int getDataSlotCount() { return 12; }

    public int getWaterAmount() { return data.get(0); }
    public int getWaterCapacity() { return data.get(1); }
    public int getCalcification() { return data.get(2); }
    public int getMaxCalcification() { return data.get(3); }
    public int getLastHeatInput() { return data.get(4); }
    public float getSystemHeatFloat() { return data.get(5) / 10.0F; }
    public int getPressure() { return data.get(6); }
    public int getInputMB() { return data.get(7); }
    public int getOutputMB() { return data.get(8); }
    public int getGaugeHeatScaled76() { return data.get(9); }
    public int getGaugeCalcificationScaled58() { return data.get(10); }
    public int getGaugeLiquidScaled47() { return data.get(11); }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (blockEntity instanceof mio_icif_steam_generator generator) {
            setSyncData(0, generator.getWaterTank().getFluidAmount());
            setSyncData(1, generator.getWaterTank().getCapacity());
            setSyncData(2, generator.getCalcification());
            setSyncData(3, generator.getMaxCalcification());
            setSyncData(4, (int) generator.getLastHeatInput());
            setSyncData(5, Math.round(generator.getSystemHeat() * 10.0F));
            setSyncData(6, generator.getPressure());
            setSyncData(7, generator.getInputMB());
            setSyncData(8, generator.getOutputMB());
            setSyncData(9, generator.gaugeHeatScaled(76));
            setSyncData(10, generator.gaugeCalcificationScaled(58));
            setSyncData(11, generator.gaugeLiquidScaled(47, 0));
        }
    }

    public String getOutputFluidTranslationKey() {
        var be = getBlockEntity();
        if (be != null) {
            return be.getOutputFluidTranslationKey();
        }
        return "";
    }

    @Override
    public mio_icif_steam_generator getBlockEntity() {
        return this.blockEntity instanceof mio_icif_steam_generator be ? be : null;
    }

    public float getCalcificationPercent() {
        if (getMaxCalcification() <= 0) return 0;
        return Math.round(getCalcification() * 100.0F / getMaxCalcification() * 100.0F) / 100.0F;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (blockEntity instanceof mio_icif_steam_generator generator) {
            generator.onButtonEvent(id);
            return true;
        }
        return false;
    }
}